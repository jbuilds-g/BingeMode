const app = {
    async init() {
        await API.init(); 
        await UI.renderList();
        const key = await DB.getSetting('tmdb_key');
        if (key) document.getElementById('apiKeyInput').value = key;
    },

    openModal() {
        document.getElementById('modal').classList.remove('hidden');
        UI.renderModalContent(null);
    },

    async openEdit(id) {
        document.getElementById('modal').classList.remove('hidden');
        const show = await DB.getShow(id);
        if (show) {
            await UI.renderModalContent(show);
            UI.fillForm(show);
        }
    },

    async openChecklist(id) {
        let show = await DB.getShow(id);
        if (!show) return;

        // Legacy Sync Logic for upgrades
        if (show.tmdbId && (!show.seasonData || show.seasonData.length === 0)) {
            const details = await API.getDetails(show.tmdbId);
            if (details && details.seasonData) {
                show.seasonData = details.seasonData;
                show.status = details.status; 
                show.rating = details.rating;
                await DB.saveShow(show);
                UI.renderList();
            } else {
                alert("Sync failed. Please check your API Key.");
                return;
            }
        }
        document.getElementById('modal').classList.remove('hidden');
        UI.renderChecklist(show);
    },

    closeModal() {
        document.getElementById('modal').classList.add('hidden');
        document.getElementById('modalBody').innerHTML = '';
        document.getElementById('convertId').value = ''; 
        document.getElementById('showId').value = '';
    },

    async selectApiShow(tmdbId) {
        const details = await API.getDetails(tmdbId);
        if (!details) return alert("Failed to fetch details");

        const convertId = document.getElementById('convertId').value;
        if (convertId) {
            await this.finalizeConversion(parseInt(convertId), details);
            return;
        }

        document.getElementById('title').value = details.title;
        document.getElementById('tmdbId').value = details.tmdbId;
        document.getElementById('apiPoster').value = details.poster;
        document.getElementById('apiStatus').value = details.status;
        document.getElementById('apiRating').value = details.rating;
        document.getElementById('apiSeasonData').value = JSON.stringify(details.seasonData);
        UI.populateSeasonSelect(details.seasonData);
    },

    async finalizeConversion(id, details) {
        const show = await DB.getShow(id);
        
        show.tmdbId = details.tmdbId;
        show.title = details.title; 
        show.poster = details.poster;
        show.status = details.status;
        show.rating = details.rating;
        show.seasonData = details.seasonData;
        show.updated = Date.now();

        await DB.saveShow(show);
        this.closeModal();
        UI.renderList();
        alert(`Upgraded "${show.title}" to Smart Tracking!`);
    },

    async saveShow() {
        const idInput = document.getElementById('showId');
        const editId = idInput && idInput.value ? parseInt(idInput.value) : null;
        const apiIdEl = document.getElementById('tmdbId');
        let showData = {};

        if (apiIdEl && apiIdEl.value) {
            const seasonSelect = document.getElementById('seasonSelect');
            showData = {
                title: document.getElementById('title').value,
                tmdbId: apiIdEl.value,
                poster: document.getElementById('apiPoster').value,
                status: document.getElementById('apiStatus').value,
                rating: document.getElementById('apiRating').value,
                seasonData: JSON.parse(document.getElementById('apiSeasonData').value),
                season: parseInt(seasonSelect.value) || 1,
                episode: 0, 
                updated: Date.now()
            };
        } else {
            showData = {
                title: document.getElementById('title').value,
                season: parseInt(document.getElementById('season').value) || 1,
                episode: parseInt(document.getElementById('episode').value) || 0,
                updated: Date.now()
            };
        }

        if (editId) showData.id = editId;

        if (editId && apiIdEl.value) {
            const old = await DB.getShow(editId);
            showData.episode = old.episode; 
        }

        await DB.saveShow(showData);
        this.closeModal();
        UI.renderList();
    },

    async deleteShow(id) {
        if (confirm("Delete this show?")) {
            await DB.deleteShow(id);
            UI.renderList();
        }
    },

    async quickUpdate(id, seasonDelta, episodeDelta) {
        const show = await DB.getShow(id);
        show.season += seasonDelta;
        show.episode += episodeDelta;
        if (show.episode < 0) show.episode = 0;
        if (show.season < 1) show.season = 1;
        show.updated = Date.now();
        await DB.saveShow(show);
        UI.renderList();
    },

    async setEpisode(id, epNum) {
        const show = await DB.getShow(id);
        show.episode = epNum;
        show.updated = Date.now();
        await DB.saveShow(show);
        this.openChecklist(id); 
        UI.renderList(); 
    },

    async startSeason(id, newSeason) {
        const show = await DB.getShow(id);
        show.season = newSeason;
        show.episode = 0;
        show.updated = Date.now();
        await DB.saveShow(show);
        this.openChecklist(id);
        UI.renderList();
    }
};

// Global exports for HTML handlers
window.app = app;
window.openModal = app.openModal;
window.closeSettings = () => document.getElementById('settingsModal').classList.add('hidden');
window.openSettings = () => {
    document.getElementById('settingsModal').classList.remove('hidden');
    const key = localStorage.getItem('tmdb_key'); 
};
window.saveSettings = async () => {
    const key = document.getElementById('apiKeyInput').value.trim();
    await DB.saveSetting('tmdb_key', key);
    API.key = key;
    closeSettings();
    alert("Settings Saved");
};

// Export/Import
window.exportData = async () => {
    const shows = await DB.getAllShows();
    const blob = new Blob([JSON.stringify(shows, null, 2)], {type: 'application/json'});
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `bingemode_backup_${new Date().toISOString().slice(0,10)}.json`;
    a.click();
};

window.importData = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = async (e) => {
        try {
            const data = JSON.parse(e.target.result);
            if (Array.isArray(data)) {
                for (const item of data) await DB.saveShow(item);
                UI.renderList();
                alert("Import Successful!");
            }
        } catch(err) { alert("Invalid Backup File"); }
    };
    reader.readAsText(file);
};

// Init
document.addEventListener('DOMContentLoaded', app.init);

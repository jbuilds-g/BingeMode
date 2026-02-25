const app = {
    async init() {
        await API.init(); 
        await UI.renderList();
        this.setupEventListeners();

        const key = await DB.getSetting('tmdb_key');
        if (key) {
            const input = document.getElementById('apiKeyInput');
            if (input) input.value = key;
        }
    },

    setupEventListeners() {
        // GLOBAL CLICK DELEGATION
        document.body.addEventListener('click', async (e) => {
            // 1. Check for Backdrops (closing modals)
            if (e.target.classList.contains('modal-backdrop')) {
                this.closeModal();
                this.closeSettings();
                return;
            }

            // 2. Find closest element with data-action
            const target = e.target.closest('[data-action]');
            if (!target) return;

            const action = target.dataset.action;
            const id = parseInt(target.dataset.id);

            // 3. Route Actions
            switch(action) {
                // Modals
                case 'open-add-show':
                    this.openModal();
                    break;
                case 'close-modal':
                    this.closeModal();
                    break;
                case 'open-settings':
                    this.openSettings();
                    break;
                case 'close-settings':
                    this.closeSettings();
                    break;
                
                // Settings Logic
                case 'save-settings':
                    this.saveSettings();
                    break;
                case 'export-data':
                    this.exportData();
                    break;

                // Show Actions
                case 'open-checklist':
                    this.openChecklist(id);
                    break;
                case 'open-edit':
                    this.openEdit(id);
                    break;
                case 'delete-show':
                    this.confirmDelete(id, target);
                    break;
                case 'set-episode':
                    this.setEpisode(id, parseInt(target.dataset.ep));
                    break;
                case 'start-season':
                    this.startSeason(id, parseInt(target.dataset.season));
                    break;
                case 'save-show':
                    this.saveShow();
                    break;
                case 'select-api-show':
                    this.selectApiShow(parseInt(target.dataset.tmdbId), target);
                    break;
            }
        });
    },

    openModal() {
        document.getElementById('modal').classList.remove('hidden');
        UI.renderModalContent(null);
    },

    openSettings() {
        document.getElementById('settingsModal').classList.remove('hidden');
        DB.getSetting('tmdb_key').then(k => {
            if(k) document.getElementById('apiKeyInput').value = k;
        });
    },

    closeSettings() {
        document.getElementById('settingsModal').classList.add('hidden');
    },

    async saveSettings() {
        const key = document.getElementById('apiKeyInput').value.trim();
        await DB.saveSetting('tmdb_key', key);
        API.key = key;
        this.closeSettings();
        alert("Settings Saved");
    },

    async exportData() {
        const shows = await DB.getAllShows();
        const blob = new Blob([JSON.stringify(shows, null, 2)], {type: 'application/json'});
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `bingemode_backup_${new Date().toISOString().slice(0,10)}.json`;
        a.click();
    },

    async openEdit(id) {
        document.getElementById('modal').classList.remove('hidden');
        const show = await DB.getShow(id);
        if (show) {
            UI.renderModalContent(show);
            UI.fillForm(show);
        }
    },

    async openChecklist(id) {
        let show = await DB.getShow(id);
        if (!show) return;

        // Auto-Heal: Fetch missing season data
        if (show.tmdbId && API.hasKey()) {
            let updated = false;
            
            // 1. Missing Season Data?
            if (!show.seasonData || !Array.isArray(show.seasonData)) {
                const details = await API.getDetails(show.tmdbId);
                if (details && details.seasonData) {
                    show.seasonData = details.seasonData;
                    updated = true;
                }
            }

            // 2. Missing Episode Names?
            const seasonIndex = (show.seasonData || []).findIndex(s => s.number === show.season);
            if (seasonIndex > -1) {
                if (!show.seasonData[seasonIndex].episodeList) {
                    const epList = await API.getSeasonEpisodes(show.tmdbId, show.season);
                    if (epList) {
                        show.seasonData[seasonIndex].episodeList = epList;
                        updated = true;
                    }
                }
            }

            if (updated) await DB.saveShow(show);
        }

        document.getElementById('modal').classList.remove('hidden');
        UI.renderChecklist(show);
    },

    closeModal() {
        document.getElementById('modal').classList.add('hidden');
        const body = document.getElementById('modalBody');
        if (body) body.innerHTML = '';
        
        const cvt = document.getElementById('convertId');
        if (cvt) cvt.value = ''; 
        const sid = document.getElementById('showId');
        if (sid) sid.value = '';
    },

    async selectApiShow(tmdbId, element) {
        if (element) {
            element.classList.add('flash-anim');
            await new Promise(r => setTimeout(r, 400));
        }

        const details = await API.getDetails(tmdbId);
        if (!details) return alert("Failed to fetch details.");

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
        Object.assign(show, {
            tmdbId: details.tmdbId,
            title: details.title,
            poster: details.poster,
            status: details.status,
            rating: details.rating,
            seasonData: details.seasonData,
            updated: Date.now()
        });
        await DB.saveShow(show);
        this.closeModal();
        UI.renderList();
    },

    async saveShow() {
        const idInput = document.getElementById('showId');
        const editId = idInput && idInput.value ? parseInt(idInput.value) : null;
        const apiIdEl = document.getElementById('tmdbId');
        let showData = {};

        if (apiIdEl && apiIdEl.value) {
            const seasonSelect = document.getElementById('seasonSelect');
            const selectedSeason = parseInt(seasonSelect.value) || 1;
            
            showData = {
                title: document.getElementById('title').value,
                tmdbId: apiIdEl.value,
                poster: document.getElementById('apiPoster').value,
                status: document.getElementById('apiStatus').value,
                rating: document.getElementById('apiRating').value,
                seasonData: JSON.parse(document.getElementById('apiSeasonData').value),
                season: selectedSeason,
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

        if (editId) {
            showData.id = editId;
            if (apiIdEl.value) {
                const old = await DB.getShow(editId);
                showData.episode = old.episode;
                if (old.season !== showData.season) showData.episode = 0; 
            }
        }

        await DB.saveShow(showData);
        this.closeModal();
        UI.renderList();
    },

    async confirmDelete(id, btn) {
        if (btn.getAttribute('data-confirm') !== 'true') {
            const originalText = btn.innerText;
            btn.innerText = "Sure?";
            btn.style.background = "var(--error)";
            btn.style.color = "white";
            btn.setAttribute('data-confirm', 'true');
            
            setTimeout(() => {
                if (btn && document.body.contains(btn)) {
                    btn.innerText = originalText;
                    btn.style.background = "";
                    btn.style.color = "";
                    btn.removeAttribute('data-confirm');
                }
            }, 3000);
        } else {
            await DB.deleteShow(id);
            this.closeModal();
            UI.renderList();
        }
    },

    async setEpisode(id, epNum) {
        const show = await DB.getShow(id);
        show.episode = epNum;
        show.updated = Date.now();
        await DB.saveShow(show);
        UI.renderChecklist(show); 
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

// Global export just for Import function called by onchange
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
                app.closeSettings();
            }
        } catch(err) { alert("Invalid Backup File"); }
    };
    reader.readAsText(file);
};

document.addEventListener('DOMContentLoaded', () => app.init());

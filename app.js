const app = {
    async init() {
        await API.init(); 
        await UI.renderList();
        
        // Setup Backdrop Clicks
        window.onclick = (event) => {
            if (event.target.classList.contains('modal-backdrop')) {
                app.closeModal();
                closeSettings();
            }
        };

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
            UI.renderModalContent(show);
            UI.fillForm(show);
        }
    },

    async openChecklist(id) {
        let show = await DB.getShow(id);
        if (!show) return;

        // Smart Update: Fetch episode names if missing and we have an API key
        if (show.tmdbId && API.hasKey()) {
            const seasonIndex = show.seasonData.findIndex(s => s.number === show.season);
            if (seasonIndex > -1) {
                // Check if we have detailed episodes for this season
                if (!show.seasonData[seasonIndex].episodeList) {
                    const epList = await API.getSeasonEpisodes(show.tmdbId, show.season);
                    if (epList) {
                        show.seasonData[seasonIndex].episodeList = epList;
                        await DB.saveShow(show); // Cache it
                    }
                }
            }
        }

        document.getElementById('modal').classList.remove('hidden');
        UI.renderChecklist(show);
    },

    closeModal() {
        document.getElementById('modal').classList.add('hidden');
        document.getElementById('settingsModal').classList.add('hidden');
        document.getElementById('modalBody').innerHTML = '';
        document.getElementById('convertId').value = ''; 
        document.getElementById('showId').value = '';
    },

    // TRIGGER: When user clicks a search result
    async selectApiShow(tmdbId, element) {
        // Animation
        if (element) {
            element.classList.add('flash-anim');
            await new Promise(r => setTimeout(r, 400));
        }

        const details = await API.getDetails(tmdbId);
        if (!details) return alert("Failed to fetch details");

        const convertId = document.getElementById('convertId').value;
        if (convertId) {
            await this.finalizeConversion(parseInt(convertId), details);
            return;
        }

        // Fill hidden fields for saving
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
            // API Mode
            const seasonSelect = document.getElementById('seasonSelect');
            const selectedSeason = parseInt(seasonSelect.value) || 1;
            
            // Smart History Logic:
            // If the user picked a season, we default to episode 0 (none watched)
            // But if they are "editing", we keep current.
            // *Requirement*: "Starting tracking a show when you are in the middle... started from zero but all other episodes already checked"
            // Implementation: The user usually just adds the show. If they want to be at Ep 5, they click Ep 5 in the checklist.
            // But if we want to support "Select Season -> Start at End of Season?" No, simplest is start at 0.
            // The Checklist UI handles the "Check one, check all previous" logic.

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
            // Manual Mode
            showData = {
                title: document.getElementById('title').value,
                season: parseInt(document.getElementById('season').value) || 1,
                episode: parseInt(document.getElementById('episode').value) || 0,
                updated: Date.now()
            };
        }

        if (editId) {
            showData.id = editId;
            // Preserve progress if just editing metadata
            if (apiIdEl.value) {
                const old = await DB.getShow(editId);
                showData.episode = old.episode;
            }
        }

        await DB.saveShow(showData);
        this.closeModal();
        UI.renderList();
    },

    // ASK TWICE DELETE
    async confirmDelete(id) {
        const btn = document.getElementById(`btn-del-${id}`);
        if (!btn) return;

        if (btn.innerText === "×") {
            // First Click
            btn.innerText = "?";
            btn.style.background = "var(--error)";
            btn.style.color = "white";
            setTimeout(() => {
                // Reset after 3 seconds if not clicked
                if (btn && document.body.contains(btn)) {
                    btn.innerText = "×";
                    btn.style.background = "#3f1a1a";
                    btn.style.color = "var(--error)";
                }
            }, 3000);
        } else {
            // Second Click (Confirmed)
            await DB.deleteShow(id);
            UI.renderList();
        }
    },

    // Checklist Logic: Set Episode
    async setEpisode(id, epNum) {
        const show = await DB.getShow(id);
        
        // Toggle logic: If clicking the exact current episode, maybe uncheck it? 
        // Standard "Binge" logic: Clicking 5 means "I watched 5". 
        // If 5 is already current, maybe they want to go back to 4?
        // Let's stick to: Click X -> Set progress to X.
        
        if (show.episode === epNum) {
             // Optional: Toggle off? Let's assume clicking again does nothing or unchecks.
             // Let's allow unchecking only the last one.
             show.episode = epNum - 1;
        } else {
            show.episode = epNum;
        }

        show.updated = Date.now();
        await DB.saveShow(show);
        // Re-render checklist inplace
        UI.renderChecklist(show); 
        UI.renderList(); // Update background list
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

// Global exports
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

document.addEventListener('DOMContentLoaded', app.init);

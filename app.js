const app = {
    async init() {
        await API.init(); 
        await UI.renderList();
        
        // Setup Backdrop Clicks (using event listener to be safe)
        window.addEventListener('click', (event) => {
            if (event.target.classList.contains('modal-backdrop')) {
                app.closeModal();
                // Also close settings if that's what was open
                if (!document.getElementById('settingsModal').classList.contains('hidden')) {
                    closeSettings();
                }
            }
        });

        const key = await DB.getSetting('tmdb_key');
        if (key) {
            const input = document.getElementById('apiKeyInput');
            if (input) input.value = key;
        }
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

        // --- SMART UPDATE & AUTO-HEAL ---
        // If we have an API ID but missing season data (migration from v1), fetch it now.
        if (show.tmdbId && API.hasKey()) {
            
            // 1. Auto-Heal: Missing seasonData entirely?
            if (!show.seasonData || !Array.isArray(show.seasonData)) {
                console.log("Migrating show data...");
                const details = await API.getDetails(show.tmdbId);
                if (details && details.seasonData) {
                    show.seasonData = details.seasonData;
                    // Save immediately so we don't fetch again next time
                    await DB.saveShow(show); 
                }
            }

            // 2. Smart Update: Missing episode names for current season?
            // Safe access using ( || [] ) to prevent crashes
            const seasonIndex = (show.seasonData || []).findIndex(s => s.number === show.season);
            
            if (seasonIndex > -1) {
                if (!show.seasonData[seasonIndex].episodeList) {
                    const epList = await API.getSeasonEpisodes(show.tmdbId, show.season);
                    if (epList) {
                        show.seasonData[seasonIndex].episodeList = epList;
                        await DB.saveShow(show);
                    }
                }
            }
        }

        // Open Modal
        document.getElementById('modal').classList.remove('hidden');
        UI.renderChecklist(show);
    },

    closeModal() {
        document.getElementById('modal').classList.add('hidden');
        document.getElementById('settingsModal').classList.add('hidden');
        const body = document.getElementById('modalBody');
        if (body) body.innerHTML = '';
        
        // Clear hidden inputs
        const cvt = document.getElementById('convertId');
        if (cvt) cvt.value = ''; 
        const sid = document.getElementById('showId');
        if (sid) sid.value = '';
    },

    // TRIGGER: When user clicks a search result
    async selectApiShow(tmdbId, element) {
        // Animation
        if (element) {
            element.classList.add('flash-anim');
            await new Promise(r => setTimeout(r, 400));
        }

        const details = await API.getDetails(tmdbId);
        if (!details) return alert("Failed to fetch details. Check internet or API Key.");

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
                // If switching seasons, maybe reset? Defaulting to keep episode count or 0 if season changed could be complex. 
                // For now, if season changed in dropdown, it overrides.
                if (old.season !== showData.season) {
                    showData.episode = 0; 
                }
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

        if (btn.getAttribute('data-confirm') !== 'true') {
            // First Click
            const originalText = btn.innerText;
            btn.innerText = "Sure?";
            btn.style.background = "var(--error)";
            btn.style.color = "white";
            btn.setAttribute('data-confirm', 'true');
            
            // Reset after 3 seconds
            setTimeout(() => {
                if (btn && document.body.contains(btn)) {
                    btn.innerText = originalText;
                    btn.style.background = ""; // revert to css
                    btn.style.color = "";
                    btn.removeAttribute('data-confirm');
                }
            }, 3000);
        } else {
            // Second Click (Confirmed)
            await DB.deleteShow(id);
            this.closeModal(); // Close if we were in the modal
            UI.renderList();
        }
    },

    async setEpisode(id, epNum) {
        const show = await DB.getShow(id);
        
        // Logic: Click X -> Set progress to X.
        // If clicking the current episode, toggle back one? 
        // Optional: Let's keep it simple. Click = Set.
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
        
        // Refresh the checklist for the new season
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
    // Pre-fill key if available logic is handled in init, but safe to check here too
    DB.getSetting('tmdb_key').then(k => {
        if(k) document.getElementById('apiKeyInput').value = k;
    });
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
                closeSettings();
            }
        } catch(err) { alert("Invalid Backup File"); }
    };
    reader.readAsText(file);
};

document.addEventListener('DOMContentLoaded', app.init);

const UI = {
    async renderList() {
        const list = document.getElementById('showList');
        const shows = await DB.getAllShows();
        list.innerHTML = '';

        if (!shows || shows.length === 0) {
            list.innerHTML = `<div style="grid-column:1/-1; text-align:center; padding:40px; color:#666;">
                <h3>No shows tracked</h3>
                <p>Click "+ Add Show" to start building your library.</p>
            </div>`;
            return;
        }

        shows.sort((a,b) => (b.updated || 0) - (a.updated || 0));

        shows.forEach(show => {
            const card = document.createElement('div');
            card.className = 'card';
            
            const imgHtml = show.poster 
                ? `<div class="poster-slot"><img src="${show.poster}" alt="${show.title}"></div>` 
                : `<div class="poster-slot"><div class="poster-placeholder">${show.title.substring(0,2).toUpperCase()}</div></div>`;

            let bottomSection = '';
            let badges = '';
            let clickAction = '';

            if (show.tmdbId) {
                // API MODE
                clickAction = `onclick="app.openChecklist(${show.id})"`;

                if (!show.seasonData || !Array.isArray(show.seasonData)) {
                    badges = `<div class="rating-badge">S${show.season}</div>`;
                    bottomSection = `<div class="card-api-hint" style="color:var(--warning); font-weight:bold;">⚠ Tap to Sync</div>`;
                } else {
                    const currentSeasonData = show.seasonData.find(s => s.number === show.season);
                    const totalEps = currentSeasonData ? currentSeasonData.episodes : '?';
                    let progressPct = 0;
                    if (typeof totalEps === 'number') progressPct = (show.episode / totalEps) * 100;

                    let statusTag = '';
                    if (show.episode >= totalEps && typeof totalEps === 'number') {
                        statusTag = `<span class="tag-finished">FINISHED</span>`;
                    } else {
                        statusTag = `<span class="tag-progress">${show.episode} / ${totalEps}</span>`;
                    }

                    badges = `<div class="rating-badge">S${show.season}</div> ${statusTag}`;
                    bottomSection = `
                        <div class="progress-container"><div class="progress-bar" style="width: ${Math.min(progressPct, 100)}%"></div></div>
                    `;
                }
            } else {
                // MANUAL MODE
                clickAction = ''; 
                badges = `<div class="status-badge manual">Manual</div>`;
                bottomSection = `
                    <div class="card-stats"><span>S${show.season}</span><span>E${show.episode}</span></div>
                    <div class="card-actions">
                        <button onclick="app.quickUpdate(${show.id}, 0, 1)">+Ep</button>
                        <button onclick="app.quickUpdate(${show.id}, 1, 0)">+Sz</button>
                        <button onclick="app.openEdit(${show.id})">✎</button>
                        <button class="danger" onclick="app.deleteShow(${show.id})">×</button>
                    </div>
                `;
            }

            card.innerHTML = `
                <div class="card-click-wrapper" ${clickAction}>
                    ${imgHtml}
                    <div class="card-content">
                        <div class="card-title">${show.title}</div>
                        <div class="meta-row">${badges}</div>
                        ${bottomSection}
                    </div>
                </div>
            `;
            list.appendChild(card);
        });
    },

    renderChecklist(show) {
        const body = document.getElementById('modalBody');
        const title = document.getElementById('modalTitle');
        title.innerText = `${show.title} - S${show.season}`;

        const safeSeasonData = show.seasonData || [];
        const sData = safeSeasonData.find(s => s.number === show.season);
        const totalEps = sData ? sData.episodes : 24; 

        let gridHtml = `<div class="checklist-grid">`;
        for (let i = 1; i <= totalEps; i++) {
            const isWatched = i <= show.episode;
            const isNext = i === show.episode + 1;
            let classList = "ep-box";
            if (isWatched) classList += " watched";
            if (isNext) classList += " next";

            gridHtml += `<div class="${classList}" onclick="app.setEpisode(${show.id}, ${i})">${i}</div>`;
        }
        gridHtml += `</div>`;

        let nextSeasonHtml = '';
        if (show.episode >= totalEps) {
            const nextS = safeSeasonData.find(s => s.number === show.season + 1);
            if (nextS) {
                nextSeasonHtml = `
                    <div class="season-complete-banner">
                        <p>Season ${show.season} Complete!</p>
                        <button class="next-season-btn" onclick="app.startSeason(${show.id}, ${show.season + 1})">Start Season ${show.season + 1}</button>
                    </div>`;
            } else {
                nextSeasonHtml = `<div class="season-complete-banner"><p>All caught up!</p></div>`;
            }
        }

        body.innerHTML = `
            ${gridHtml}
            ${nextSeasonHtml}
            <div class="modal-actions" style="margin-top:20px; text-align:right;">
                <button class="secondary" onclick="app.closeModal()">Close</button>
            </div>
        `;
    },

    renderModalContent(show) {
        const body = document.getElementById('modalBody');
        const title = document.getElementById('modalTitle');
        title.innerText = show ? "Edit Show" : "Add New Show";

        // Inject Form
        body.innerHTML = `
            <div class="form-group">
                <input type="text" id="searchInput" placeholder="Search TMDB (Type & Enter)" onchange="UI.handleSearch(this.value)">
                <div id="searchResults" class="search-results"></div>
            </div>

            <hr style="border:0; border-top:1px solid #333; margin: 15px 0;">

            <div id="manualForm">
                <div class="form-group">
                    <label>Title</label>
                    <input type="text" id="title" placeholder="Show Title">
                </div>
                
                <div id="apiFields" class="hidden">
                    <input type="hidden" id="tmdbId">
                    <input type="hidden" id="apiPoster">
                    <input type="hidden" id="apiStatus">
                    <input type="hidden" id="apiRating">
                    <input type="hidden" id="apiSeasonData">
                    <div class="form-group">
                        <label>Select Season to Start</label>
                        <select id="seasonSelect"></select>
                    </div>
                </div>

                <div id="manualFields">
                    <div class="form-group">
                        <label>Season</label>
                        <input type="number" id="season" value="1" min="1">
                    </div>
                    <div class="form-group">
                        <label>Episode</label>
                        <input type="number" id="episode" value="0" min="0">
                    </div>
                </div>

                <div class="modal-footer">
                    <button onclick="app.saveShow()" style="width:100%">Save</button>
                    ${show ? '<button onclick="app.closeModal()" class="secondary" style="width:100%; margin-top:8px;">Cancel</button>' : ''}
                </div>
            </div>
        `;
    },

    async handleSearch(query) {
        if (!query) return;
        const results = await API.search(query);
        const container = document.getElementById('searchResults');
        container.innerHTML = '';
        
        if (results.length === 0) {
            container.innerHTML = `<p style="color:#666">No results found.</p>`;
            return;
        }

        results.forEach(item => {
            const div = document.createElement('div');
            div.style.cssText = "padding:10px; border-bottom:1px solid #333; cursor:pointer; display:flex; gap:10px; align-items:center;";
            div.innerHTML = `
                <img src="${item.poster || ''}" style="width:30px; height:45px; object-fit:cover; background:#333;">
                <div>
                    <div style="font-weight:bold;">${item.name}</div>
                    <div style="font-size:0.8rem; color:#888;">${item.first_air_date ? item.first_air_date.substring(0,4) : ''}</div>
                </div>
            `;
            div.onclick = () => app.selectApiShow(item.id);
            container.appendChild(div);
        });
    },

    populateSeasonSelect(seasonData) {
        const sel = document.getElementById('seasonSelect');
        const manualFields = document.getElementById('manualFields');
        const apiFields = document.getElementById('apiFields');
        
        manualFields.classList.add('hidden');
        apiFields.classList.remove('hidden');
        
        sel.innerHTML = '';
        seasonData.forEach(s => {
            const opt = document.createElement('option');
            opt.value = s.number;
            opt.innerText = `Season ${s.number} (${s.episodes} eps)`;
            sel.appendChild(opt);
        });
    },

    fillForm(show) {
        document.getElementById('showId').value = show.id;
        document.getElementById('title').value = show.title;

        if (show.tmdbId) {
            // Convert mode: If editing a manual show, we might want to attach API data
            document.getElementById('convertId').value = show.id; 
            // We assume if you edit, you might be upgrading. 
        } else {
            // Manual edit
            document.getElementById('season').value = show.season;
            document.getElementById('episode').value = show.episode;
            document.getElementById('convertId').value = show.id; // Allow search to upgrade this
        }
    }
};

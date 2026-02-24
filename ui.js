const UI = {
    async renderList() {
        const list = document.getElementById('showList');
        const shows = await DB.getAllShows();
        list.innerHTML = '';

        if (!shows || shows.length === 0) {
            list.innerHTML = `<div style="grid-column:1/-1; text-align:center; padding:60px; color:var(--text-dim);">
                <h3>Library Empty</h3>
                <p>Tap the + button to add a show.</p>
            </div>`;
            return;
        }

        shows.sort((a,b) => (b.updated || 0) - (a.updated || 0));

        shows.forEach(show => {
            const card = document.createElement('div');
            card.className = 'card';
            
            // Poster
            const imgHtml = show.poster 
                ? `<div class="poster-slot"><img src="${show.poster}" alt="${show.title}"></div>` 
                : `<div class="poster-slot"><div class="poster-placeholder">${show.title.substring(0,2).toUpperCase()}</div></div>`;

            let metaInfo = '';
            let progressHtml = '';
            let clickAction = '';

            if (show.tmdbId) {
                // API Mode
                clickAction = `onclick="app.openChecklist(${show.id})"`;
                const sData = show.seasonData ? show.seasonData.find(s => s.number === show.season) : null;
                const totalEps = sData ? sData.episodes : 0;
                
                let pct = (totalEps > 0) ? (show.episode / totalEps) * 100 : 0;
                
                metaInfo = `<span>Season ${show.season}</span> <span>${show.episode}/${totalEps}</span>`;
                progressHtml = `<div class="progress-rail"><div class="progress-fill" style="width:${pct}%"></div></div>`;
                
                if (show.episode >= totalEps && totalEps > 0) {
                    metaInfo = `<span style="color:var(--success)">Finished S${show.season}</span>`;
                }

            } else {
                // Manual Mode
                // For manual mode, we render edit controls on the card or just basic info
                // Per design request: "Make it easy to delete... ask twice"
                // The delete button is best placed in the edit modal OR a dedicated small action area.
                // Let's put a small action row for manual items since they don't have a checklist.
                
                clickAction = `onclick="app.openEdit(${show.id})"`;
                metaInfo = `<span>S${show.season} • E${show.episode}</span>`;
                
                // Add mini delete button stopping propagation
                metaInfo += `<button id="btn-del-${show.id}" class="danger" style="padding:4px 8px; font-size:12px; border-radius:4px; margin-left:auto;" onclick="event.stopPropagation(); app.confirmDelete(${show.id})">×</button>`;
            }

            card.innerHTML = `
                <div onclick="(${clickAction})">
                    ${imgHtml}
                    <div class="card-content">
                        <div class="card-title">${show.title}</div>
                        <div class="card-meta">${metaInfo}</div>
                        ${progressHtml}
                    </div>
                </div>
            `;
            list.appendChild(card);
        });
    },

    renderChecklist(show) {
        const body = document.getElementById('modalBody');
        const title = document.getElementById('modalTitle');
        title.innerText = show.title;

        // Get Data
        const sData = show.seasonData ? show.seasonData.find(s => s.number === show.season) : null;
        const totalEps = sData ? sData.episodes : (show.episode + 5); // Fallback
        const episodesList = sData && sData.episodeList ? sData.episodeList : [];

        // Build List
        let listHtml = `<div class="checklist-list">`;
        
        for (let i = 1; i <= totalEps; i++) {
            const isWatched = i <= show.episode;
            
            // Try to find episode name
            let epName = `Episode ${i}`;
            let epObj = episodesList.find(e => e.number === i);
            if (epObj) epName = epObj.name;

            listHtml += `
                <div class="ep-row ${isWatched ? 'watched' : ''}" onclick="app.setEpisode(${show.id}, ${i})">
                    <div class="checkbox"></div>
                    <div class="ep-info">
                        <div class="ep-num">S${show.season} E${i}</div>
                        <div class="ep-title">${epName}</div>
                    </div>
                </div>
            `;
        }
        listHtml += `</div>`;

        // Next Season Button if finished
        let nextHtml = '';
        if (show.episode >= totalEps && totalEps > 0) {
            const nextS = show.seasonData.find(s => s.number === show.season + 1);
            if (nextS) {
                nextHtml = `<div style="text-align:center; margin-top:20px;">
                    <button onclick="app.startSeason(${show.id}, ${show.season+1})">Start Season ${show.season+1}</button>
                </div>`;
            }
        }

        // Action Footer
        const footerHtml = `
            <div style="margin-top:24px; display:flex; justify-content:space-between; border-top:1px solid #333; padding-top:16px;">
                <button class="secondary danger" id="btn-del-${show.id}" onclick="app.confirmDelete(${show.id})">Delete Show</button>
                <button class="secondary" onclick="app.openEdit(${show.id})">Edit</button>
            </div>
        `;

        body.innerHTML = `
            <div style="font-size:0.9rem; color:var(--text-dim); margin-bottom:10px;">Season ${show.season} Progress</div>
            ${listHtml}
            ${nextHtml}
            ${footerHtml}
        `;
    },

    renderModalContent(show) {
        const body = document.getElementById('modalBody');
        const title = document.getElementById('modalTitle');
        title.innerText = show ? "Edit Details" : "Add Show";

        body.innerHTML = `
            ${!show ? `
            <div class="form-group">
                <input type="text" id="searchInput" placeholder="Search TV Shows..." autocomplete="off">
                <div id="searchResults" class="search-results"></div>
            </div>
            <div style="text-align:center; font-size:0.8rem; color:#555; margin:10px 0;">— OR MANUAL ADD —</div>
            ` : ''}

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
                        <label>Select Season</label>
                        <select id="seasonSelect"></select>
                    </div>
                </div>

                <div id="manualFields">
                    <div class="row">
                        <div class="form-group" style="flex:1">
                            <label>Season</label>
                            <input type="number" id="season" value="1" min="1">
                        </div>
                        <div class="form-group" style="flex:1">
                            <label>Episode</label>
                            <input type="number" id="episode" value="0" min="0">
                        </div>
                    </div>
                </div>

                <div class="modal-footer" style="margin-top:20px;">
                    <button onclick="app.saveShow()" style="width:100%">Save Show</button>
                </div>
            </div>
        `;

        // Attach Search Listener with Debounce
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            let timeout = null;
            searchInput.addEventListener('input', (e) => {
                clearTimeout(timeout);
                timeout = setTimeout(() => UI.handleSearch(e.target.value), 400);
            });
            searchInput.focus();
        }
    },

    async handleSearch(query) {
        if (!query) {
            document.getElementById('searchResults').innerHTML = '';
            return;
        }
        const results = await API.search(query);
        const container = document.getElementById('searchResults');
        container.innerHTML = '';
        
        if (results.length === 0) return;

        results.forEach(item => {
            const div = document.createElement('div');
            div.className = 'search-item';
            div.innerHTML = `
                <img src="${item.poster || ''}" style="width:36px; height:54px; object-fit:cover; background:#333; border-radius:4px;">
                <div>
                    <div style="font-weight:bold;">${item.name}</div>
                    <div style="font-size:0.75rem; color:#888;">${item.first_air_date ? item.first_air_date.substring(0,4) : ''}</div>
                </div>
            `;
            // Pass 'div' to animate it on click
            div.onclick = () => app.selectApiShow(item.id, div);
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
            document.getElementById('convertId').value = show.id;
        } else {
            document.getElementById('season').value = show.season;
            document.getElementById('episode').value = show.episode;
            document.getElementById('convertId').value = show.id;
        }
    }
};

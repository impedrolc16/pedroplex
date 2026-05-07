const API_BASE = '/api';
const TMDB_IMG = 'https://image.tmdb.org/t/p/w500';
const PER_PAGE = 20;

const E = {
    COMPLETADA: 'completada',
    EN_PROCESO: 'en proceso',
    WATCHLIST:  'watchlist',
};

let ALL = [];
const pgState = {};
const dataCache = {
    home: null,
    series: null,
    peliculas: null,
    watchlist: null,
    loading: {}
};

async function init() {
    try {
        // Cargar SOLO datos de inicio
        const [completadas, enProceso, peliculas, watchlist] = await Promise.all([
            fetch(`${API_BASE}/series/completadas`).then(r => { if (!r.ok) throw new Error(); return r.json(); }),
            fetch(`${API_BASE}/series/enproceso`).then(r => { if (!r.ok) throw new Error(); return r.json(); }),
            fetch(`${API_BASE}/peliculas`).then(r => { if (!r.ok) throw new Error(); return r.json(); }),
            fetch(`${API_BASE}/watchlist`).then(r => { if (!r.ok) throw new Error(); return r.json(); })
        ]);

        ALL = [
            ...completadas.map(item => ({...item, estado: E.COMPLETADA})),
            ...enProceso.map(item => ({...item, estado: E.EN_PROCESO})),
            ...peliculas.map(item => ({...item, estado: E.COMPLETADA})),
            ...watchlist.map(item => ({...item, estado: E.WATCHLIST}))
        ];

        // Cachear todos los datos
        dataCache.home = ALL;
        dataCache.series = ALL.filter(i => i.tipo === 'serie');
        dataCache.peliculas = ALL.filter(i => i.tipo === 'pelicula');
        dataCache.watchlist = ALL.filter(i => i.estado === E.WATCHLIST);

        if (!ALL.length) {
            showError();
            return;
        }
    } catch {
        showError();
        return;
    }
    // Solo renderizar la página inicial
    renderHome();
}

function showError() {
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.getElementById('page-error').classList.add('active');
    document.getElementById('home-sub').textContent = 'No se pudo cargar la biblioteca';
}

function renderAll() {
    renderHome();
    renderSeries();
    renderPeliculas();
    renderWatchlist();
}

function renderHome() {
    const series = dataCache.series || ALL.filter(i => i.tipo === 'serie');
    const pelis  = dataCache.peliculas || ALL.filter(i => i.tipo === 'pelicula');
    const wl     = dataCache.watchlist || ALL.filter(i => i.estado === E.WATCHLIST);

    const seriesVistas = series.filter(i => i.estado !== E.WATCHLIST);
    const pelisVistas  = pelis.filter(i  => i.estado !== E.WATCHLIST);
    const wlSeries     = wl.filter(i    => i.tipo === 'serie');
    const wlPelis      = wl.filter(i    => i.tipo === 'pelicula');
    const favSeries    = series.filter(i => i.rating >= 10);
    const favPelis     = pelis.filter(i  => i.rating >= 10);

    document.getElementById('home-sub').textContent =
        `${seriesVistas.length + pelisVistas.length} títulos vistos · ${wl.length} en watchlist`;

    const defs = [
        { icon:'📺', label:'Series vistas',    num:seriesVistas.length, sub:[`✅ ${series.filter(i=>i.estado===E.COMPLETADA).length} completadas`,`⏳ ${series.filter(i=>i.estado===E.EN_PROCESO).length} en proceso`], cls:'red'    },
        { icon:'🎬', label:'Películas vistas', num:pelisVistas.length,  sub:[`Total en tu colección`],                                                                                                                   cls:'orange' },
        { icon:'📋', label:'Watchlist',        num:wl.length,           sub:[`📺 ${wlSeries.length} series`,`🎬 ${wlPelis.length} películas`],                                                                           cls:'blue'   },
        { icon:'⭐', label:'Series favoritas', num:favSeries.length || series.filter(i=>i.rating>=9).length, sub:[`Rating 10 · Top selección`],                                                                          cls:'gold'   },
        { icon:'⭐', label:'Pelis favoritas',  num:favPelis.length  || pelis.filter(i=>i.rating>=9).length,  sub:[`Rating 10 · Top selección`],                                                                          cls:'gold'   },
        { icon:'🎭', label:'Total catálogo',   num:ALL.length,          sub:[`${series.length} series · ${pelis.length} películas`],                                                                                      cls:'purple' },
    ];

    document.getElementById('statsGrid').innerHTML = defs.map(d => `
<div class="stat-card ${d.cls}">
  <span class="stat-icon">${d.icon}</span>
  <div class="stat-label">${d.label}</div>
  <div class="stat-num">${d.num}</div>
  <div class="stat-sub">${d.sub.map(s=>`<span>${s}</span>`).join('')}</div>
</div>`).join('');

    const bestS = (favSeries.length ? favSeries : series.filter(i=>i.rating>=8)).sort((a,b)=>b.rating-a.rating).slice(0,12);
    const bestP = (favPelis.length  ? favPelis  : pelis.filter(i=>i.rating>=8)).sort((a,b)=>b.rating-a.rating).slice(0,12);

    const favSeriesGrid = document.getElementById('favSeriesGrid');
    const favPelisGrid  = document.getElementById('favPelisGrid');
    if (favSeriesGrid) favSeriesGrid.innerHTML = bestS.length ? bestS.map(i => cardHTML(i, ALL.indexOf(i))).join('') : '<div class="empty">Sin series favoritas</div>';
    if (favPelisGrid)  favPelisGrid.innerHTML  = bestP.length ? bestP.map(i => cardHTML(i, ALL.indexOf(i))).join('') : '<div class="empty">Sin películas favoritas</div>';
}

function renderSeries() {
    const series = dataCache.series || ALL.filter(i => i.tipo === 'serie');
    const ep  = series.filter(i => i.estado===E.EN_PROCESO);
    const com = series.filter(i => i.estado===E.COMPLETADA);
    document.getElementById('series-sub').textContent = `${ep.length} en proceso · ${com.length} completadas`;
    setupGrid('enproceso', ep);
    setupGrid('completada', com);
}

function renderPeliculas() {
    const pelis = dataCache.peliculas || ALL.filter(i => i.tipo === 'pelicula');
    const p = pelis.filter(i => i.estado!==E.WATCHLIST);
    document.getElementById('peliculas-sub').textContent = `${p.length} películas en tu colección`;
    setupGrid('pelicula', p);
}

function renderWatchlist() {
    const watchlist = dataCache.watchlist || ALL.filter(i => i.estado === E.WATCHLIST);
    const ws = watchlist.filter(i => i.tipo==='serie');
    const wp = watchlist.filter(i => i.tipo==='pelicula');
    document.getElementById('watchlist-sub').textContent = `${ws.length + wp.length} títulos pendientes`;
    setupGrid('wlseries', ws);
    setupGrid('wlpelis',  wp);
}

function setupGrid(key, items) {
    pgState[key] = { items, page:1, sort:'title' };
    renderGrid(key);
}

function renderGrid(key) {
    const {items, page, sort} = pgState[key];
    let sortedItems = [...items];
    if (sort === 'rating-asc') {
        sortedItems.sort((a,b) => (a.rating || 0) - (b.rating || 0));
    } else if (sort === 'rating-desc') {
        sortedItems.sort((a,b) => (b.rating || 0) - (a.rating || 0));
    } else if (sort === 'title') {
        sortedItems.sort((a,b) => (a.titulo || '').localeCompare(b.titulo || ''));
    }
    const total  = sortedItems.length;
    const pages  = Math.max(1, Math.ceil(total / PER_PAGE));
    const start  = (page-1) * PER_PAGE;
    const slice  = sortedItems.slice(start, start + PER_PAGE);

    const cntEl = document.getElementById(`cnt-${key}`);
    if (cntEl) cntEl.textContent = total;

    const gridEl = document.getElementById(`grid-${key}`);
    if (!gridEl) return;

    if (!total) {
        gridEl.innerHTML = '<div class="empty">Sin títulos en esta categoría</div>';
        const pgEl = document.getElementById(`pg-${key}`);
        if (pgEl) pgEl.innerHTML = '';
        return;
    }
    gridEl.innerHTML = slice.map(i => cardHTML(i, ALL.indexOf(i))).join('');
    renderPagination(key, page, pages);
}

function renderPagination(key, cur, total) {
    const el = document.getElementById(`pg-${key}`);
    if (!el) return;
    if (total <= 1) { el.innerHTML = ''; return; }

    const range = pgRange(cur, total);
    let h = `<button class="pg-btn" onclick="changePg('${key}',${cur-1})" ${cur===1?'disabled':''}>‹</button>`;
    range.forEach(p => {
        if (p==='…') h += `<span class="pg-dots">…</span>`;
        else h += `<button class="pg-btn ${p===cur?'active':''}" onclick="changePg('${key}',${p})">${p}</button>`;
    });
    h += `<button class="pg-btn" onclick="changePg('${key}',${cur+1})" ${cur===total?'disabled':''}>›</button>`;
    el.innerHTML = h;
}

function pgRange(cur, total) {
    if (total<=7) return Array.from({length:total},(_,i)=>i+1);
    const r=[];
    if (cur<=4)          { for(let i=1;i<=5;i++) r.push(i); r.push('…'); r.push(total); }
    else if (cur>=total-3){ r.push(1); r.push('…'); for(let i=total-4;i<=total;i++) r.push(i); }
    else                  { r.push(1); r.push('…'); r.push(cur-1,cur,cur+1); r.push('…'); r.push(total); }
    return r;
}

function changePg(key, page) {
    pgState[key].page = page;
    renderGrid(key);
    const el = document.getElementById(`grid-${key}`);
    if (el) el.scrollIntoView({ behavior:'smooth', block:'start' });
}

function changeSort(key, sort) {
    pgState[key].sort = sort;
    pgState[key].page = 1;
    renderGrid(key);
}

function cardHTML(item, idx) {
    const img = getImg(item);
    const badge = getBadge(item);
    const imgTag = img
        ? `<img src="${img}" alt="${esc(item.titulo)}" onerror="this.style.display='none';this.nextElementSibling.style.display='flex'">`
        : '';
    const phStyle = img ? 'display:none;' : '';
    return `<div class="card" onclick="openModal(${idx})">
${badge}
${imgTag}
<div class="card-ph" style="${phStyle}">
  <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2">
    <rect x="2" y="2" width="20" height="20" rx="3"/>
    <circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/>
  </svg>
  <span>${esc(item.titulo)}</span>
</div>
<div class="card-overlay">
  <div class="card-name">${esc(item.titulo)}</div>
  <div class="card-rating">⭐ ${item.rating?.toFixed(1)??'N/A'}</div>
</div>
</div>`;
}

function getBadge(item) {
    if (item.estado===E.EN_PROCESO)  return `<span class="card-badge b-enproceso">En proceso</span>`;
    if (item.estado===E.COMPLETADA)  return `<span class="card-badge b-completada">✓ Completa</span>`;
    if (item.estado===E.WATCHLIST)   return `<span class="card-badge b-watchlist">Pendiente</span>`;
    if (item.tipo==='pelicula')      return `<span class="card-badge b-pelicula">Película</span>`;
    return '';
}

function openModal(idx) {
    const item = ALL[idx]; if (!item) return;
    const img = getImg(item);
    const mImg = document.getElementById('mImg');
    if (img) { mImg.src=img; mImg.style.display='block'; } else { mImg.style.display='none'; }
    document.getElementById('mTitle').textContent = item.titulo;
    document.getElementById('mDesc').textContent  = item.descripcion || 'Sin descripción disponible.';

    const tipoPill  = item.tipo==='serie' ? `<span class="pill pill-blue">Serie</span>` : `<span class="pill pill-red">Película</span>`;
    const estadoPill = item.estado===E.COMPLETADA ? `<span class="pill pill-green">✓ Completada</span>`
        : item.estado===E.EN_PROCESO  ? `<span class="pill pill-gold">⏳ En proceso</span>`
            : item.estado===E.WATCHLIST   ? `<span class="pill pill-orange">📋 Watchlist</span>` : '';
    const ratingSpan = `<span style="color:var(--gold);font-weight:600;font-size:.88rem;">⭐ ${item.rating?.toFixed(1)??'N/A'}</span>`;
    const tmdbA = item.tmdbId ? `<a href="https://www.themoviedb.org/${item.tipo==='serie'?'tv':'movie'}/${item.tmdbId}" target="_blank" class="tmdb-link">Ver en TMDB ↗</a>` : '';

    document.getElementById('mMeta').innerHTML = [tipoPill,estadoPill,ratingSpan,tmdbA].filter(Boolean).join('');
    document.getElementById('modal').classList.add('open');
    document.body.style.overflow='hidden';
}
function closeModal(e) {
    if (e && e.target!==document.getElementById('modal')) return;
    document.getElementById('modal').classList.remove('open');
    document.body.style.overflow='';
}

let currentPage = 'home';

function goPage(name) {
    document.querySelectorAll('.page').forEach(p=>p.classList.remove('active'));
    document.querySelectorAll('.nav-links button').forEach(b=>b.classList.remove('active'));
    document.getElementById('page-'+name).classList.add('active');
    const navBtn = document.getElementById('nav-'+name);
    if (navBtn) navBtn.classList.add('active');
    currentPage = name;
    window.scrollTo({top:0,behavior:'smooth'});
    clearSearch();

    // Renderizar solo la página solicitada
    if (name === 'home') {
        renderHome();
    } else if (name === 'series') {
        renderSeries();
    } else if (name === 'peliculas') {
        renderPeliculas();
    } else if (name === 'watchlist') {
        renderWatchlist();
    }
}

function clearSearch() {
    const inp1 = document.getElementById('searchInput');
    const inp2 = document.getElementById('searchInputMobile');
    if (inp1) inp1.value = '';
    if (inp2) inp2.value = '';
    const searchSection = document.getElementById('searchResultsSection');
    const normalContent = document.getElementById('homeNormalContent');
    if (searchSection) searchSection.style.display = 'none';
    if (normalContent) normalContent.style.display = '';
}

function handleSearch(q) {
    const inp1 = document.getElementById('searchInput');
    const inp2 = document.getElementById('searchInputMobile');
    if (inp1 && inp1.value !== q) inp1.value = q;
    if (inp2 && inp2.value !== q) inp2.value = q;

    q = q.trim().toLowerCase();

    if (!q) {
        const searchSection = document.getElementById('searchResultsSection');
        const normalContent = document.getElementById('homeNormalContent');
        if (searchSection) searchSection.style.display = 'none';
        if (normalContent) normalContent.style.display = '';
        return;
    }

    const f = ALL.filter(i => (i.titulo||'').toLowerCase().includes(q) || (i.descripcion||'').toLowerCase().includes(q));

    const searchSection = document.getElementById('searchResultsSection');
    const normalContent = document.getElementById('homeNormalContent');
    const searchGrid    = document.getElementById('searchResultsGrid');
    const searchTitle   = document.getElementById('searchResultsTitle');

    if (searchSection && searchGrid) {
        searchSection.style.display = '';
        if (normalContent) normalContent.style.display = 'none';
        if (searchTitle) searchTitle.textContent = `${f.length} resultado${f.length!==1?'s':''} para "${q}"`;
        searchGrid.innerHTML = f.length
            ? f.map(i => cardHTML(i, ALL.indexOf(i))).join('')
            : '<div class="empty">Sin resultados para esa búsqueda</div>';
    }
}

function getImg(item) {
    if (item.imagen && item.imagen.startsWith('http')) return item.imagen;
    if (item.imagen && item.imagen.startsWith('/'))    return TMDB_IMG + item.imagen;
    return '';
}

function toggleNav() {
    document.getElementById('mobileMenu').classList.toggle('open');
}

function esc(s) { return String(s??'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }

document.addEventListener('keydown', e=>{ if(e.key==='Escape') closeModal(); });
init();
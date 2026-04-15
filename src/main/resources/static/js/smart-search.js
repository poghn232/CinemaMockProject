(function () {
    'use strict';

    /* ── Config ──────────────────────────────────────────────────────── */
    var CFG = {
        searchEp: '/api/public/movies/search',
        historyEp: '/api/user/history',
        recommendEp: '/api/user/recommendations/maybe-you-want-to-watch',
        debounceDD: 200,   /* ms — autocomplete dropdown */
        debounceSearch: 400, /* ms — full result panel     */
        maxSuggest: 8,     /* max rows in dropdown       */
        maxRecommend: 16,
        maxFallback: 12,
    };

    /* ── CSS ─────────────────────────────────────────────────────────── */
    var CSS = [
        /* Wrapper giữ position context cho dropdown & panel */
        '.ss-wrap{position:relative;flex:1;}',
        '.ss-wrap .search-overlay-input{width:100%;box-sizing:border-box;}',

        /* Spinner trong ô input */
        '.ss-sp{display:none;position:absolute;right:13px;top:50%;',
        '  transform:translateY(-50%);width:15px;height:15px;border-radius:50%;',
        '  border:2px solid rgba(148,163,184,.18);border-top-color:#818cf8;',
        '  animation:ss-rot .65s linear infinite;pointer-events:none;}',
        '@keyframes ss-rot{to{transform:translateY(-50%) rotate(360deg)}}',

        /* Row */
        '.ss-row{display:flex;align-items:center;gap:11px;padding:10px 14px;',
        '  cursor:pointer;border-bottom:1px solid rgba(148,163,184,.07);transition:background .11s;}',
        '.ss-row:last-child{border-bottom:none;}',
        '.ss-row:hover,.ss-row.hi{background:rgba(99,102,241,.18);}',

        /* Thumbnail */
        '.ss-th{width:36px;height:52px;border-radius:6px;object-fit:cover;',
        '  background:rgba(148,163,184,.1);flex-shrink:0;}',
        '.ss-ph{width:36px;height:52px;border-radius:6px;flex-shrink:0;',
        '  background:rgba(148,163,184,.1);display:flex;align-items:center;',
        '  justify-content:center;font-size:17px;}',

        /* Info */
        '.ss-info{flex:1;min-width:0;}',
        '.ss-ttl{font-size:14px;font-weight:600;color:var(--text-main,#e2e8f0);',
        '  white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}',
        '.ss-ttl mark{background:transparent;color:#818cf8;font-weight:700;}',
        '.ss-met{font-size:11px;color:var(--text-subtle,#94a3b8);',
        '  margin-top:3px;display:flex;gap:7px;align-items:center;flex-wrap:wrap;}',
        '.ss-tag{font-size:9px;padding:1px 7px;border-radius:20px;font-weight:700;',
        '  text-transform:uppercase;letter-spacing:.04em;}',
        '.ss-t-seen{background:rgba(139,92,246,.25);color:#c4b5fd;}',
        '.ss-t-tv  {background:rgba(99,102,241,.2);color:#a5b4fc;}',
        '.ss-t-mv  {background:rgba(148,163,184,.15);color:var(--text-subtle,#94a3b8);}',
        '.ss-star  {color:#fbbf24;}',
        '.ss-seen-dot{width:6px;height:6px;border-radius:50%;background:#a78bfa;flex-shrink:0;}',

        /* Kbd hint */
        '.ss-kbd-row{display:flex;gap:5px;align-items:center;justify-content:flex-end;',
        '  padding:6px 14px 8px;border-top:1px solid rgba(148,163,184,.08);',
        '  font-size:10px;color:rgba(148,163,184,.4);}',
        '.ss-kbd{display:inline-block;padding:1px 5px;border-radius:4px;font-size:9px;',
        '  background:rgba(148,163,184,.1);border:1px solid rgba(148,163,184,.18);}',

        /* Empty row */
        '.ss-empty{padding:14px 16px;font-size:13px;text-align:center;',
        '  color:var(--text-subtle,#94a3b8);}',

        /* Section trong panel */
        '.ss-sec{margin-bottom:14px;}',
        '.ss-sec:last-child{margin-bottom:0;}',
        '.ss-sec-hdr{display:flex;align-items:center;gap:7px;margin-bottom:9px;',
        '  font-size:10px;font-weight:700;letter-spacing:.07em;',
        '  text-transform:uppercase;color:var(--text-subtle,#94a3b8);}',
        '.ss-sec-cnt{margin-left:auto;font-size:10px;color:rgba(148,163,184,.4);}',
        '.ss-sep{height:1px;background:rgba(148,163,184,.09);margin:12px 0;}',

        /* Horizontal strip */
        '.ss-strip{display:flex;gap:9px;overflow-x:auto;padding:2px 0 6px;scrollbar-width:thin;}',

        /* Mini card trong panel */
        '.ss-card{flex:0 0 auto;width:82px;border-radius:8px;overflow:hidden;cursor:pointer;',
        '  border:1px solid rgba(148,163,184,.12);background:rgba(15,23,42,.55);',
        '  transition:border-color .14s;position:relative;}',
        '.ss-card:hover{border-color:#818cf8;}',
        '.ss-cimg{width:82px;height:118px;object-fit:cover;display:block;',
        '  background:rgba(148,163,184,.1);}',
        '.ss-cph{width:82px;height:118px;display:flex;align-items:center;',
        '  justify-content:center;font-size:26px;background:rgba(148,163,184,.08);}',
        '.ss-cbdy{padding:5px 6px;}',
        '.ss-ctitle{font-size:11px;font-weight:600;line-height:1.3;',
        '  color:var(--text-main,#e2e8f0);display:-webkit-box;',
        '  -webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;}',
        '.ss-cmeta{font-size:10px;color:var(--text-subtle,#94a3b8);',
        '  margin-top:3px;display:flex;justify-content:space-between;}',

        /* Tiến trình xem (progress bar) dưới poster */
        '.ss-prog{position:absolute;bottom:0;left:0;right:0;height:3px;background:rgba(0,0,0,.4);}',
        '.ss-prog-bar{height:100%;background:#818cf8;border-radius:0 2px 2px 0;}',

        /* Ribbon "Đã xem" */
        '.ss-ribbon{position:absolute;top:4px;left:4px;z-index:2;',
        '  font-size:8px;padding:1px 5px;border-radius:3px;',
        '  background:rgba(139,92,246,.88);color:#fff;font-weight:700;}',

        /* Loading & note */
        '.ss-ld{padding:18px;text-align:center;font-size:12px;color:var(--text-subtle,#94a3b8);}',
        '.ss-note{font-size:11px;color:var(--text-subtle,#94a3b8);margin-bottom:10px;line-height:1.5;}',
        '.ss-note strong{color:var(--text-main,#e2e8f0);}',

        /* Guest prompt */
        '.ss-guest{padding:16px;text-align:center;font-size:12px;',
        '  color:var(--text-subtle,#94a3b8);line-height:1.7;}',
        '.ss-guest a{color:#818cf8;text-decoration:none;}',
        '.ss-guest a:hover{text-decoration:underline;}',

        /* ── Fallback section (0 kết quả) ── */
        '.ss-fb{display:none;margin-top:18px;padding:16px;border-radius:12px;',
        '  border:1px solid rgba(139,92,246,.2);background:rgba(139,92,246,.04);}',
        '.ss-fb.show{display:block;}',
        '.ss-fb-hdr{display:flex;align-items:center;gap:8px;margin-bottom:4px;',
        '  font-size:12px;font-weight:700;letter-spacing:.06em;text-transform:uppercase;',
        '  color:var(--text-subtle,#94a3b8);}',
        '.ss-fb-badge{font-size:10px;padding:2px 9px;border-radius:20px;font-weight:700;',
        '  background:rgba(139,92,246,.2);color:#c4b5fd;border:1px solid rgba(139,92,246,.28);}',
        '.ss-fb-note{font-size:12px;color:var(--text-subtle,#94a3b8);margin:4px 0 12px;font-style:italic;}',
    ].join('');

    /* ── State ───────────────────────────────────────────────────────── */
    var _ddItems = [], _idx = -1;
    var _tDD = null, _tSr = null, _tPanel = null;
    var _sp = null, _curPanelKey = null;

    /* User data cache (reset khi mở overlay) */
    var _history = null;   /* WatchHistoryDTO[] */
    var _recommend = null;   /* MovieItemDto[]    */
    var _historyMap = null;   /* key "movie:id" | "tv:id" → entry */
    var _loading = false;

    /* ── Utils ───────────────────────────────────────────────────────── */
    function esc(s) {
        return String(s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;')
            .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }
    function hilite(text, q) {
        if (!q) return esc(text);
        var i = text.toLowerCase().indexOf(q.toLowerCase());
        if (i < 0) return esc(text);
        return esc(text.slice(0, i))
            + '<mark>' + esc(text.slice(i, i + q.length)) + '</mark>'
            + esc(text.slice(i + q.length));
    }
    function token() { return localStorage.getItem('token'); }
    function authH() { var t = token(); return t ? { Authorization: 'Bearer ' + t } : {}; }
    function isAuth() { return !!token(); }
    function jGet(url) {
        return fetch(url, { headers: authH() })
            .then(function (r) { return r.ok ? r.json() : null; })
            .catch(function () { return null; });
    }
    function spin(on) { if (_sp) _sp.style.display = on ? 'block' : 'none'; }

    /* ── Preload lịch sử + gợi ý ────────────────────────────────────── */
    function preload() {
        if (!isAuth() || _loading || (_history !== null && _recommend !== null)) return;
        _loading = true;
        Promise.all([
            jGet(CFG.historyEp),
            jGet(CFG.recommendEp + '?limit=' + CFG.maxRecommend),
        ]).then(function (res) {
            _history = Array.isArray(res[0]) ? res[0] : [];
            _recommend = Array.isArray(res[1]) ? res[1] : [];
            _historyMap = {};
            _history.forEach(function (h) {
                if (h.movieId) _historyMap['movie:' + h.movieId] = h;
                if (h.tvSeriesId) _historyMap['tv:' + h.tvSeriesId] = h;
            });
            _loading = false;
        }).catch(function () { _loading = false; });
    }
    function resetCache() {
        _history = null; _recommend = null; _historyMap = null; _loading = false;
    }

    /* ── DOM bootstrap ───────────────────────────────────────────────── */
    function injectCSS() {
        if (document.getElementById('ss-style')) return;
        var s = document.createElement('style');
        s.id = 'ss-style'; s.textContent = CSS;
        document.head.appendChild(s);
    }

    function injectDOM() {
        var input = document.getElementById('overlaySearchInput');
        if (!input || document.getElementById('ss-dd')) return false;

        /* Bọc input trong wrapper */
        var wrap = document.createElement('div');
        wrap.className = 'ss-wrap';
        input.parentNode.insertBefore(wrap, input);
        wrap.appendChild(input);

        /* Spinner (sibling của input, bên trong wrap) */
        _sp = document.createElement('div');
        _sp.className = 'ss-sp';
        wrap.appendChild(_sp);

        /* Fallback section (sau overlayPagination) */
        var pag = document.getElementById('overlayPagination');
        if (pag) {
            var fb = document.createElement('div');
            fb.id = 'ss-fb'; fb.className = 'ss-fb';
            pag.parentNode.insertBefore(fb, pag.nextSibling);
        }
        return true;
    }


    function buildRow(item, q, i) {
        var row = document.createElement('div');
        row.className = 'ss-row';

        /* Kiểm tra user đã xem chưa */
        var seenEntry = _historyMap
            ? (_historyMap['movie:' + item.id] || _historyMap['tv:' + item.id])
            : null;

        /* Thumbnail */
        if (item.imageUrl) {
            var img = document.createElement('img');
            img.className = 'ss-th';
            img.src = item.imageUrl; img.alt = item.title; img.loading = 'lazy';
            row.appendChild(img);
        } else {
            var ph = document.createElement('div');
            ph.className = 'ss-ph';
            ph.textContent = item.type === 'tv' ? '📺' : '🎬';
            row.appendChild(ph);
        }

        /* Info */
        var info = document.createElement('div'); info.className = 'ss-info';
        var ttl = document.createElement('div'); ttl.className = 'ss-ttl';
        ttl.innerHTML = hilite(item.title, q);

        var met = document.createElement('div'); met.className = 'ss-met';

        if (seenEntry) {
            var dot = document.createElement('div'); dot.className = 'ss-seen-dot';
            var seenTag = document.createElement('span'); seenTag.className = 'ss-tag ss-t-seen';
            seenTag.textContent = 'Watched';
            met.appendChild(dot); met.appendChild(seenTag);
        } else {
            var typeTag = document.createElement('span');
            typeTag.className = 'ss-tag ' + (item.type === 'tv' ? 'ss-t-tv' : 'ss-t-mv');
            typeTag.textContent = item.type === 'tv' ? 'TV Series' : 'Movie';
            met.appendChild(typeTag);
        }

        if (item.year) {
            var yr = document.createElement('span'); yr.textContent = item.year;
            met.appendChild(yr);
        }
        if (item.rating) {
            var rt = document.createElement('span'); rt.className = 'ss-star';
            rt.textContent = '★ ' + item.rating.toFixed(1);
            met.appendChild(rt);
        }

        info.appendChild(ttl); info.appendChild(met);
        row.appendChild(info);

        /* Hover → side panel sau 200ms */
        row.addEventListener('mouseenter', function () {
            hlDD(i);
            clearTimeout(_tPanel);
            _tPanel = setTimeout(function () { loadPanel(item); }, 200);
        });
        row.addEventListener('click', function () { pickDD(i); });
        return row;
    }

    function closeDD() {
        var dd = document.getElementById('ss-dd');
        if (dd) { dd.classList.remove('open'); dd.innerHTML = ''; }
        _idx = -1; _ddItems = [];
    }

    function hlDD(idx) {
        _idx = idx;
        var rows = document.querySelectorAll('#ss-dd .ss-row');
        Array.prototype.forEach.call(rows, function (r, i) {
            r.classList.toggle('hi', i === idx);
        });
        if (idx >= 0 && rows[idx]) rows[idx].scrollIntoView({ block: 'nearest' });
    }

    function pickDD(i) {
        var item = _ddItems[i]; if (!item) return;
        var inp = document.getElementById('overlaySearchInput');
        if (inp) inp.value = item.title;
        closeDD(); closePanel();
        if (typeof handleMovieClick === 'function') handleMovieClick(item);
    }

    /* ── SIDE PANEL ──────────────────────────────────────────────────── */
    function loadPanel(item) {
        var key = item.type + ':' + item.id;
        if (_curPanelKey === key) return;
        _curPanelKey = key;

        var panel = document.getElementById('ss-panel'); if (!panel) return;
        panel.innerHTML = '<div class="ss-ld">Đang tải…</div>';
        panel.classList.add('open');

        /* Chờ user data sẵn (nếu đang preload) */
        var tries = 0;
        function tryRender() {
            if (_loading && tries++ < 20) { setTimeout(tryRender, 100); return; }
            renderPanel(panel, item);
        }
        tryRender();
    }

    function renderPanel(panel, item) {
        panel.innerHTML = '';

        if (!isAuth()) {
            var g = document.createElement('div'); g.className = 'ss-guest';
            g.innerHTML =
                '<div style="font-size:28px;margin-bottom:8px">👤</div>' +
                '<div><a href="login.html">Đăng nhập</a> để xem gợi ý<br>cá nhân hóa từ lịch sử xem</div>';
            panel.appendChild(g);
            return;
        }

        /* Lịch sử xem gần đây (loại trừ item đang hover) */
        var recent = [];
        if (_history && _history.length) {
            _history.forEach(function (h) {
                if (h.movieId && String(h.movieId) === String(item.id) && item.type === 'movie') return;
                if (h.tvSeriesId && String(h.tvSeriesId) === String(item.id) && item.type === 'tv') return;
                if (h.movieId && h.movieTitle) {
                    recent.push({
                        id: h.movieId, type: 'movie', title: h.movieTitle,
                        imageUrl: h.posterPath || '', progressSec: h.progressSec, durationSec: h.durationSec
                    });
                } else if (h.tvSeriesId && h.tvSeriesName) {
                    recent.push({
                        id: h.tvSeriesId, type: 'tv', title: h.tvSeriesName,
                        imageUrl: h.posterPath || '', progressSec: h.progressSec, durationSec: h.durationSec
                    });
                }
            });
            recent = recent.slice(0, 8);
        }

        /* Gợi ý từ recommendation engine (loại trừ item đang hover) */
        var recs = (_recommend || []).filter(function (m) {
            return !(String(m.id) === String(item.id) && m.type === item.type);
        }).slice(0, 8);

        if (!recent.length && !recs.length) {
            var none = document.createElement('div'); none.className = 'ss-ld';
            none.textContent = 'Chưa có lịch sử xem.';
            panel.appendChild(none); return;
        }

        /* Tiêu đề context */
        var note = document.createElement('div'); note.className = 'ss-note';
        note.innerHTML = 'For you while watching <strong>' + esc(item.title) + '</strong>';
        panel.appendChild(note);

        if (recent.length) {
            panel.appendChild(buildSection('Watched lately', 'ss-t-seen', recent, true));
        }
        if (recent.length && recs.length) {
            var sep = document.createElement('div'); sep.className = 'ss-sep';
            panel.appendChild(sep);
        }
        if (recs.length) {
            panel.appendChild(buildSection('Recommend for you', 'ss-t-seen', recs, false));
        }
    }

    function buildSection(label, tagCls, items, showProgress) {
        var sec = document.createElement('div'); sec.className = 'ss-sec';
        var hdr = document.createElement('div'); hdr.className = 'ss-sec-hdr';
        hdr.innerHTML = label
            + ' <span class="ss-tag ' + tagCls + '">' + items.length + '</span>'
            + '<span class="ss-sec-cnt">' + items.length + ' phim</span>';
        sec.appendChild(hdr);

        var strip = document.createElement('div'); strip.className = 'ss-strip';
        items.forEach(function (m) { strip.appendChild(buildCard(m, showProgress)); });
        sec.appendChild(strip);
        return sec;
    }

    function buildCard(m, showProgress) {
        var card = document.createElement('div'); card.className = 'ss-card';

        if (m.imageUrl) {
            var img = document.createElement('img');
            img.className = 'ss-cimg'; img.src = m.imageUrl; img.alt = m.title || ''; img.loading = 'lazy';
            card.appendChild(img);
        } else {
            var ph = document.createElement('div'); ph.className = 'ss-cph';
            ph.textContent = m.type === 'tv' ? '📺' : '🎬'; card.appendChild(ph);
        }

        if (showProgress) {
            var ribbon = document.createElement('div'); ribbon.className = 'ss-ribbon';
            ribbon.textContent = 'Watched'; card.appendChild(ribbon);
            if (m.progressSec && m.durationSec && m.durationSec > 0) {
                var pct = Math.min(100, Math.round(m.progressSec / m.durationSec * 100));
                var prog = document.createElement('div'); prog.className = 'ss-prog';
                var bar = document.createElement('div'); bar.className = 'ss-prog-bar';
                bar.style.width = pct + '%'; prog.appendChild(bar); card.appendChild(prog);
            }
        }

        var body = document.createElement('div'); body.className = 'ss-cbdy';
        var t = document.createElement('div'); t.className = 'ss-ctitle';
        t.textContent = m.title || ''; body.appendChild(t);

        var me = document.createElement('div'); me.className = 'ss-cmeta';
        var yr = document.createElement('span'); yr.textContent = m.year || '';
        var rt = document.createElement('span'); rt.className = 'ss-star';
        if (m.rating) rt.textContent = '★ ' + Number(m.rating).toFixed(1);
        me.appendChild(yr); me.appendChild(rt); body.appendChild(me);
        card.appendChild(body);

        card.addEventListener('click', function () {
            closeDD(); closePanel();
            if (typeof handleMovieClick === 'function') handleMovieClick(m);
        });
        return card;
    }

    function closePanel() {
        _curPanelKey = null;
        var p = document.getElementById('ss-panel');
        if (p) { p.classList.remove('open'); p.innerHTML = ''; }
    }

    /* ── FULL SEARCH ─────────────────────────────────────────────────── */
    function doSearch(q) {
        q = (q || '').trim();
        var grid = document.getElementById('overlayGrid');
        var countEl = document.getElementById('overlayResultCount');
        var pagEl = document.getElementById('overlayPagination');
        var fb = document.getElementById('ss-fb');
        var actSec = document.getElementById('overlayActorsSection');
        var actGrid = document.getElementById('overlayActorsGrid');
        if (!grid || !countEl) return;

        if (fb) fb.className = 'ss-fb'; /* hide */
        if (!q) {
            countEl.textContent = 'Nhập để tìm kiếm…';
            if (pagEl) pagEl.innerHTML = '';
            if (actSec) actSec.style.display = 'none';
            return;
        }

        spin(true);
        Promise.all([
            fetch(CFG.searchEp + '?q=' + encodeURIComponent(q) + '&type=all&page=1', { headers: authH() })
                .then(function (r) { return r.ok ? r.json() : { items: [] }; })
                .catch(function () { return { items: [] }; }),
            fetch('/api/public/people/page?page=1&q=' + encodeURIComponent(q), { headers: authH() })
                .then(function (r) { return r.ok ? r.json() : []; })
                .catch(function () { return []; }),
        ]).then(function (res) {
            spin(false);
            var items = (res[0] && res[0].items) || [];
            var people = Array.isArray(res[1]) ? res[1] : (res[1].items || []);
            var pTotal = Array.isArray(res[1]) ? res[1].length : (res[1].totalElements || people.length);

            countEl.textContent = items.length + ' phim · ' + (pTotal > 20 ? '20+' : pTotal) + ' diễn viên';

            if (actSec) actSec.style.display = people.length ? '' : 'none';
            if (actGrid && typeof createActorCard === 'function') {
                actGrid.innerHTML = '';
                people.slice(0, 20).forEach(function (p) { actGrid.appendChild(createActorCard(p)); });
            }

            renderGrid(grid, pagEl, items);
            if (items.length === 0) showFallback(q, fb);
        }).catch(function () {
            spin(false);
            countEl.textContent = 'Tìm kiếm thất bại.';
        });
    }

    function renderGrid(grid, pagEl, items) {
        var PAGE = 20, cur = 1, total = Math.max(1, Math.ceil(items.length / PAGE));
        function render(p) {
            var s = (p - 1) * PAGE; grid.innerHTML = '';
            items.slice(s, s + PAGE).forEach(function (m) {
                if (typeof createMovieCard === 'function') grid.appendChild(createMovieCard(m));
            });
            if (typeof applyFadeInAnimation === 'function') applyFadeInAnimation(grid);
            if (!pagEl) return; pagEl.innerHTML = '';
            if (total > 1) {
                var prev = document.createElement('button');
                prev.className = 'page-button' + (p === 1 ? ' disabled' : ''); prev.textContent = 'Trước';
                var info = document.createElement('div');
                info.style.cssText = 'min-width:110px;text-align:center;color:var(--text-subtle,#94a3b8)';
                info.textContent = 'Trang ' + p + ' / ' + total;
                var next = document.createElement('button');
                next.className = 'page-button' + (p === total ? ' disabled' : ''); next.textContent = 'Sau';
                prev.onclick = function () { if (cur > 1) { cur--; render(cur); } };
                next.onclick = function () { if (cur < total) { cur++; render(cur); } };
                pagEl.appendChild(prev); pagEl.appendChild(info); pagEl.appendChild(next);
            }
        }
        render(1);
    }

    /* ── FALLBACK khi 0 kết quả ─────────────────────────────────────── */
    function showFallback(q, fb) {
        if (!fb || !isAuth()) return;
        function tryShow() {
            var recs = _recommend || [];
            if (!recs.length) return;
            fb.innerHTML =
                '<div class="ss-fb-hdr">Recommend for you</div>' +
                '</div>' +
                '<div class="ss-fb-note">No results found for "' + esc(q) + '" — you might like:</div>' +
                '<div id="ss-fb-grid" class="movies-grid"></div>';
            fb.className = 'ss-fb show';
            var g = document.getElementById('ss-fb-grid'); if (!g) return;
            recs.slice(0, CFG.maxFallback).forEach(function (m) {
                if (typeof createMovieCard === 'function') g.appendChild(createMovieCard(m));
            });
            if (typeof applyFadeInAnimation === 'function') applyFadeInAnimation(g);
        }
        if (_loading) setTimeout(tryShow, 500); else tryShow();
    }

    /* ── WIRE EVENTS ─────────────────────────────────────────────────── */
    function wire() {
        var inp = document.getElementById('overlaySearchInput');
        if (!inp) return;

        /* QUAN TRỌNG: KHÔNG clone input — chỉ thêm listener mới.
           Clone sẽ làm mất reference trong ss-wrap và phá DOM. */

        inp.addEventListener('input', function () {
            var q = inp.value;
            clearTimeout(_tDD); clearTimeout(_tSr); closePanel();
            _tDD = setTimeout(function () { openDD(q); }, CFG.debounceDD);
            _tSr = setTimeout(function () { doSearch(q); }, CFG.debounceSearch);
        });

        inp.addEventListener('keydown', function (e) {
            var dd = document.getElementById('ss-dd');
            var open = dd && dd.classList.contains('open');
            if (e.key === 'Escape') {
                if (open) { closeDD(); closePanel(); }
                else if (typeof hideSearchOverlay === 'function') hideSearchOverlay();
                return;
            }
            if (!open) return;
            var rows = dd.querySelectorAll('.ss-row');
            if (e.key === 'ArrowDown') {
                e.preventDefault(); hlDD(Math.min(_idx + 1, rows.length - 1));
                if (_ddItems[_idx]) { clearTimeout(_tPanel); _tPanel = setTimeout(function () { loadPanel(_ddItems[_idx]); }, 300); }
            } else if (e.key === 'ArrowUp') {
                e.preventDefault(); hlDD(Math.max(_idx - 1, -1));
                if (_idx >= 0 && _ddItems[_idx]) { clearTimeout(_tPanel); _tPanel = setTimeout(function () { loadPanel(_ddItems[_idx]); }, 300); }
            } else if (e.key === 'Enter' && _idx >= 0) {
                e.preventDefault(); pickDD(_idx);
            }
        });

        /* Click ngoài → đóng dropdown + panel */
        document.addEventListener('click', function (e) {
            if (!e.target.closest('.ss-wrap')) { closeDD(); closePanel(); }
        });

        /* Patch showSearchOverlay để preload data khi mở */
        if (typeof showSearchOverlay === 'function') {
            var _origShow = showSearchOverlay;
            showSearchOverlay = function () {
                resetCache();
                preload();
                _origShow();
            };
        }

        /* Patch hideSearchOverlay để đóng dropdown + panel */
        if (typeof hideSearchOverlay === 'function') {
            var _origHide = hideSearchOverlay;
            hideSearchOverlay = function () {
                closeDD(); closePanel();
                _origHide();
            };
        }
    }

    /* ── BOOT ────────────────────────────────────────────────────────── */
    function boot() {
        injectCSS();
        if (!injectDOM()) return;
        wire();
        console.info('[smart-search v5] ✓ autocomplete=/search | suggest=watch-history');
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
    else setTimeout(boot, 0);

})();
/**
 * notification-bell.js
 * ─────────────────────────────────────────────────────────────────────────────
 * Tự render biểu tượng 🔔 vào bất kỳ element nào có id="notifBellMount"
 * và xử lý toàn bộ logic: fetch, badge, dropdown, đánh dấu đọc.
 *
 * Cách dùng trong HTML:
 *   1. Thêm <div id="notifBellMount"></div> vào navbar (nav-right)
 *   2. <script src="/js/notification-bell.js"></script> trước </body>
 * ─────────────────────────────────────────────────────────────────────────────
 */
(function () {
    'use strict';

    function getProfileId() { return localStorage.getItem('profileId') || ''; }

    function profileParam() {
        const pid = getProfileId();
        return pid ? 'profileId=' + pid : '';
    }

    const API = {
        list:    () => '/api/user/notifications' + (profileParam() ? '?' + profileParam() : ''),
        unread:  () => '/api/user/notifications/unread' + (profileParam() ? '?' + profileParam() : ''),
        readAll: () => '/api/user/notifications/read-all' + (profileParam() ? '?' + profileParam() : ''),
        readOne: (id) => `/api/user/notifications/${id}/read` + (profileParam() ? '?' + profileParam() : ''),
    };

    const POLL_MS = 30_000; // kiểm tra unread mỗi 30 giây

    // ── CSS ──────────────────────────────────────────────────────────────────
    const CSS = `
  .nb-wrap {
    position: relative;
    display: inline-flex;
    align-items: center;
  }
  .nb-btn {
    background: none;
    border: 1px solid transparent;
    border-radius: 999px;
    padding: 6px 10px;
    cursor: pointer;
    color: #9ca3af;
    font-size: 18px;
    line-height: 1;
    transition: color .15s, border-color .15s;
    display: flex;
    align-items: center;
    gap: 4px;
  }
  .nb-btn:hover {
    color: #e5e7eb;
    border-color: rgba(148,163,184,.5);
  }
  .nb-badge {
    position: absolute;
    top: 2px;
    right: 4px;
    background: #ef4444;
    color: #fff;
    font-size: 10px;
    font-weight: 700;
    min-width: 16px;
    height: 16px;
    border-radius: 999px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 4px;
    pointer-events: none;
    line-height: 1;
  }
  .nb-badge.hidden { display: none; }

  /* Dropdown */
  .nb-dropdown {
    position: absolute;
    top: calc(100% + 10px);
    right: 0;
    width: 340px;
    max-height: 480px;
    background: #0f172a;
    border: 1px solid rgba(148,163,184,.25);
    border-radius: 14px;
    box-shadow: 0 20px 50px rgba(0,0,0,.7);
    overflow: hidden;
    display: flex;
    flex-direction: column;
    z-index: 9999;
    opacity: 0;
    transform: translateY(-8px) scale(.97);
    pointer-events: none;
    transition: opacity .18s ease, transform .18s ease;
  }
  .nb-dropdown.open {
    opacity: 1;
    transform: translateY(0) scale(1);
    pointer-events: all;
  }

  .nb-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 16px 10px;
    border-bottom: 1px solid rgba(148,163,184,.15);
    flex-shrink: 0;
  }
  .nb-header-title {
    font-weight: 700;
    font-size: 15px;
    color: #e5e7eb;
  }
  .nb-read-all {
    font-size: 12px;
    color: #f97316;
    background: none;
    border: none;
    cursor: pointer;
    padding: 0;
  }
  .nb-read-all:hover { text-decoration: underline; }

  .nb-list {
    overflow-y: auto;
    flex: 1;
    scroll-behavior: smooth;
  }
  .nb-list::-webkit-scrollbar { width: 4px; }
  .nb-list::-webkit-scrollbar-track { background: transparent; }
  .nb-list::-webkit-scrollbar-thumb { background: rgba(148,163,184,.25); border-radius: 2px; }

  .nb-item {
    display: flex;
    gap: 12px;
    padding: 12px 16px;
    cursor: pointer;
    border-bottom: 1px solid rgba(148,163,184,.08);
    transition: background .12s;
    text-decoration: none;
    color: inherit;
  }
  .nb-item:hover { background: rgba(148,163,184,.07); }
  .nb-item.unread { background: rgba(249,115,22,.07); }
  .nb-item.unread:hover { background: rgba(249,115,22,.12); }

  .nb-poster {
    width: 40px;
    height: 60px;
    object-fit: cover;
    border-radius: 6px;
    flex-shrink: 0;
    background: #1e293b;
  }
  .nb-poster-placeholder {
    width: 40px;
    height: 60px;
    border-radius: 6px;
    background: #1e293b;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    flex-shrink: 0;
  }

  .nb-body { flex: 1; min-width: 0; }
  .nb-msg {
    font-size: 13px;
    color: #e5e7eb;
    line-height: 1.4;
    white-space: normal;
  }
  .nb-time {
    font-size: 11px;
    color: #6b7280;
    margin-top: 4px;
  }
  .nb-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #f97316;
    flex-shrink: 0;
    margin-top: 6px;
  }
  .nb-item.read .nb-dot { visibility: hidden; }

  .nb-empty {
    padding: 32px 16px;
    text-align: center;
    color: #6b7280;
    font-size: 13px;
  }
  .nb-loading {
    padding: 24px;
    text-align: center;
    color: #6b7280;
    font-size: 13px;
  }
  .nb-poster-placeholder {
  width: 40px;
  height: 60px;
  border-radius: 8px;
  background: #1e293b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;   /* 👈 to hơn */
}
  `;

    // ── Helpers ───────────────────────────────────────────────────────────────
    function getToken() { return localStorage.getItem('token') || ''; }
    function isLoggedIn() { return !!getToken(); }

    async function apiFetch(url, opts = {}) {
        const token = getToken();
        const headers = { ...(opts.headers || {}) };
        if (token) headers['Authorization'] = 'Bearer ' + token;
        console.log("TOKEN:", localStorage.getItem("token"));
        const res = await fetch(url, { ...opts, headers });

        if (!res.ok) {
            const text = await res.text();
            console.error("API ERROR:", res.status, text); // 👈 thêm dòng này
            throw new Error(res.status);
        }

        const text = await res.text();
        return text ? JSON.parse(text) : null;
    }

    function timeAgo(isoStr) {
        const diff = Date.now() - new Date(isoStr).getTime();
        const m = Math.floor(diff / 60000);
        if (m < 1)  return 'Just now';
        if (m < 60) return `${m}m ago`;
        const h = Math.floor(m / 60);
        if (h < 24) return `${h}h ago`;
        return `${Math.floor(h / 24)}d ago`;
    }

    function detailUrl(item) {
        if (item.eventType === "ACHIEVEMENT_UNLOCK" || item.contentType === "achievement") {
            return 'profile.html#achievements';
        }
        return `movie-detail.html?id=${item.contentId}&type=${item.contentType}`;
    }

    // ── Render ────────────────────────────────────────────────────────────────
    function renderItem(item) {
        const div = document.createElement('a');
        div.className = 'nb-item ' + (item.isRead ? 'read' : 'unread');
        div.href = detailUrl(item);

        // Poster
        const poster = (item.eventType === 'ACHIEVEMENT_UNLOCK' && item.iconUrl)
            ? `<div class="nb-poster-placeholder">${item.iconUrl}</div>`
            : item.posterUrl
                ? `<img class="nb-poster" src="${item.posterUrl}" alt="" loading="lazy">`
                : `<div class="nb-poster-placeholder">🎬</div>`;

        // 👉 Parse params an toàn
        let params = {};
        try {
            params = item.messageParams ? JSON.parse(item.messageParams) : {};
        } catch (e) {
            params = {};
        }

        // 👉 Dịch message từ i18n
        let msg;

        try {
            if (typeof t === 'function' && item.messageKey) {
                try {
                    msg = t(item.messageKey, params);
                    if (msg === item.messageKey) {
                        msg = item.contentTitle || "New notification";
                    }
                } catch(e) {
                    msg = item.contentTitle || "New notification";
                }
            } else {
                msg = item.contentTitle || "New notification";
            }
        } catch (e) {
            console.error("i18n error:", e);
            msg = item.contentTitle || "New notification";
        }

        // 👉 Render HTML
        div.innerHTML = `
        ${poster}
        <div class="nb-body">
            <div class="nb-msg">${msg}</div>
            <div class="nb-time">${timeAgo(item.createdAt)}</div>
        </div>
        <div class="nb-dot"></div>
    `;

        // Đánh dấu đã đọc
        div.addEventListener('click', () => {
            if (!item.isRead) {
                item.isRead = true;
                apiFetch(API.readOne(item.id), { method: 'PUT' }).catch(() => {});
                div.classList.replace('unread', 'read');
                div.querySelector('.nb-dot').style.visibility = 'hidden';

                // ⭐ update badge ngay
                if (window._notifBell) {
                    const unread = window._notifBell.items.filter(i => !i.isRead).length;
                    window._notifBell._updateBadge(unread);
                }
            }
        });
        return div;
    }

    // ── Main class ────────────────────────────────────────────────────────────
    class NotificationBell {
        constructor(mountEl) {
            this.mount    = mountEl;
            this.open     = false;
            this.items    = [];
            this.pollTimer = null;

            this._injectStyles();
            this._build();
            if (isLoggedIn()) {
                this._fetchUnread();
                this._startPolling();
            }
        }

        _injectStyles() {
            if (document.getElementById('nb-styles')) return;
            const s = document.createElement('style');
            s.id = 'nb-styles';
            s.textContent = CSS;
            document.head.appendChild(s);
        }

        _build() {
            this.wrap = document.createElement('div');
            this.wrap.className = 'nb-wrap';

            this.btn = document.createElement('button');
            this.btn.className = 'nb-btn';
            this.btn.setAttribute('aria-label', 'Notifications');
            this.btn.innerHTML = '🔔';

            this.badge = document.createElement('span');
            this.badge.className = 'nb-badge hidden';
            this.badge.textContent = '0';

            this.dropdown = document.createElement('div');
            this.dropdown.className = 'nb-dropdown';
            this.dropdown.innerHTML = `
        <div class="nb-header">
          <span class="nb-header-title" data-i18n="notification.title">Notifications</span>
          <button class="nb-read-all" data-i18n="notification.mark_all">Mark all read</button>
        </div>
        <div class="nb-list">
          <div class="nb-loading" data-i18n="notification.loading">Loading…</div>
        </div>
      `;

            this.wrap.appendChild(this.btn);
            this.wrap.appendChild(this.badge);
            this.wrap.appendChild(this.dropdown);
            this.mount.appendChild(this.wrap);

            if (typeof applyTranslations === "function") {
                applyTranslations();
            }

            // Events
            this.btn.addEventListener('click', (e) => { e.stopPropagation(); this._toggle(); });
            this.dropdown.querySelector('.nb-read-all').addEventListener('click', () => this._readAll());
            document.addEventListener('click', (e) => {
                if (!this.wrap.contains(e.target)) this._close();
            });
        }

        _toggle() {
            this.open ? this._close() : this._openDropdown();
        }

        _openDropdown() {
            this.open = true;
            this.dropdown.classList.add('open');
            this._fetchList();
        }

        _close() {
            this.open = false;
            this.dropdown.classList.remove('open');
        }

        async _fetchUnread() {
            if (!isLoggedIn()) return;
            try {
                const data = await apiFetch(API.unread());
                this._updateBadge(data.count);
            } catch { /* silent */ }
        }

        async _fetchList() {
            const list = this.dropdown.querySelector('.nb-list');

            list.innerHTML = `<div class="nb-loading">${t('notification.loading')}</div>`;

            try {
                this.items = await apiFetch(API.list());
                this._renderList();
                this._updateBadge(this.items.filter(i => !i.isRead).length);
            } catch {
                list.innerHTML = `<div class="nb-empty">${t('notification.failed')}</div>`;
            }
        }

        _renderList() {
            const list = this.dropdown.querySelector('.nb-list');
            list.innerHTML = '';

            if (!this.items || this.items.length === 0) {
                list.innerHTML = `<div class="nb-empty">${t('notification.empty')}</div>`;
                return;
            }

            this.items.forEach(item => list.appendChild(renderItem(item)));
        }

        async _readAll() {
            try {
                await apiFetch(API.readAll(), { method: 'PUT' });
                this._updateBadge(0);
                this.dropdown.querySelectorAll('.nb-item.unread').forEach(el => {
                    el.classList.replace('unread', 'read');
                    el.querySelector('.nb-dot').style.visibility = 'hidden';
                });
            } catch { /* silent */ }
        }

        _updateBadge(count) {
            if (count > 0) {
                this.badge.textContent = count > 99 ? '99+' : count;
                this.badge.classList.remove('hidden');
            } else {
                this.badge.classList.add('hidden');
            }
        }

        _startPolling() {
            this.pollTimer = setInterval(() => this._fetchUnread(), POLL_MS);
        }

        destroy() {
            clearInterval(this.pollTimer);
        }
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    function init() {
        const mount = document.getElementById('notifBellMount');
        if (!mount) return; // trang không có bell
        if (!isLoggedIn()) return; // chỉ hiển thị khi đã đăng nhập
        window._notifBell = new NotificationBell(mount);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
window.addEventListener("languageChanged", () => {
    if (window._notifBell) {
        window._notifBell._fetchList(); // reload lại để dịch
    }
});
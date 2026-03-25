/**
 * MovieZone AI Chat Support Widget
 * Inject this script into any HTML page to enable the AI assistant.
 * The widget will auto-initialize when the DOM is ready.
 *
 * Usage: <script src="js/ai-chat-support.js"></script>
 *
 * The widget calls /api/ai-chat on your backend, which proxies to Anthropic API.
 * See ai-chat-controller.java for the backend endpoint.
 */
(function () {
    'use strict';

    /* ─── CSS ─────────────────────────────────────────────────────────────── */
    const CSS = `
    @import url('https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,300;0,9..40,400;0,9..40,500;0,9..40,600;1,9..40,300&display=swap');

    .mz-chat-widget * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      font-family: 'DM Sans', system-ui, -apple-system, sans-serif;
    }

    /* ── Trigger Button ── */
   .mz-chat-trigger {
    position: fixed;
    /* ensure the trigger sits above system taskbars / docking areas by
      honoring the safe-area inset when available and adding a small offset */
  /* nudge up a bit more to avoid being covered by OS taskbars */
  bottom: calc(env(safe-area-inset-bottom, 0px) + 40px);
  right: calc(env(safe-area-inset-right, 0px) + 28px);
    z-index: 11000;
      width: 58px;
      height: 58px;
      border-radius: 999px;
      border: none;
      cursor: pointer;
      background: linear-gradient(135deg, #f97316, #ec4899);
      box-shadow:
        0 0 0 0 rgba(249,115,22,0.5),
        0 14px 28px rgba(236,72,153,0.45),
        0 0 0 1px rgba(249,115,22,0.8);
      display: flex;
      align-items: center;
      justify-content: center;
      transition: transform 0.2s ease, box-shadow 0.2s ease;
      animation: mz-pulse 3s ease infinite;
    }

    @keyframes mz-pulse {
      0%, 100% { box-shadow: 0 0 0 0 rgba(249,115,22,0.4), 0 14px 28px rgba(236,72,153,0.45), 0 0 0 1px rgba(249,115,22,0.8); }
      50%       { box-shadow: 0 0 0 10px rgba(249,115,22,0), 0 14px 28px rgba(236,72,153,0.45), 0 0 0 1px rgba(249,115,22,0.8); }
    }

    .mz-chat-trigger:hover {
      transform: translateY(-3px) scale(1.05);
      box-shadow: 0 0 0 0 rgba(249,115,22,0), 0 20px 40px rgba(236,72,153,0.6), 0 0 0 1px rgba(249,115,22,0.9);
      animation: none;
    }

    .mz-chat-trigger svg {
      transition: transform 0.25s cubic-bezier(0.34,1.56,0.64,1), opacity 0.15s;
    }

    .mz-chat-trigger.open .mz-icon-chat { transform: scale(0) rotate(-90deg); opacity: 0; position: absolute; }
    .mz-chat-trigger.open .mz-icon-close { transform: scale(1) rotate(0deg); opacity: 1; }
    .mz-chat-trigger .mz-icon-close { transform: scale(0) rotate(90deg); opacity: 0; position: absolute; }

    /* Unread badge */
    .mz-unread-badge {
      position: absolute;
      top: -4px;
      right: -4px;
      width: 18px;
      height: 18px;
      border-radius: 50%;
      background: #ef4444;
      color: white;
      font-size: 10px;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 2px solid #020617;
      opacity: 0;
      transform: scale(0);
      transition: all 0.2s cubic-bezier(0.34,1.56,0.64,1);
    }
    .mz-unread-badge.show {
      opacity: 1;
      transform: scale(1);
    }

    /* ── Chat Window ── */
    .mz-chat-window {
      position: fixed;
      /* position window above the trigger and respect safe-area inset */
      bottom: calc(env(safe-area-inset-bottom, 0px) + 98px);
      right: calc(env(safe-area-inset-right, 0px) + 28px);
      /* keep window z-index below full-screen overlays but above page content */
      z-index: 10990;
      width: 380px;
      max-height: 580px;
      height: 580px;
      border-radius: 20px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      background: #070d1a;
      border: 1px solid rgba(249,115,22,0.25);
      box-shadow:
        0 0 0 1px rgba(148,163,184,0.12),
        0 32px 64px rgba(2,6,23,0.9),
        0 0 80px rgba(249,115,22,0.08);
      transform: translateY(16px) scale(0.96);
      opacity: 0;
      pointer-events: none;
      transition: all 0.3s cubic-bezier(0.34,1.2,0.64,1);
      transform-origin: bottom right;
    }

    .mz-chat-window.open {
      transform: translateY(0) scale(1);
      opacity: 1;
      pointer-events: all;
    }

    /* ── Header ── */
    .mz-chat-header {
      padding: 16px 18px 14px;
      background: linear-gradient(135deg, rgba(249,115,22,0.15), rgba(236,72,153,0.1));
      border-bottom: 1px solid rgba(249,115,22,0.2);
      display: flex;
      align-items: center;
      gap: 12px;
      flex-shrink: 0;
      position: relative;
    }

    .mz-chat-header::before {
      content: '';
      position: absolute;
      inset: 0;
      background: radial-gradient(circle at 0 0, rgba(249,115,22,0.12), transparent 60%);
      pointer-events: none;
    }

    .mz-avatar {
      width: 38px;
      height: 38px;
      border-radius: 999px;
      background: linear-gradient(135deg, #f97316, #ec4899);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
      flex-shrink: 0;
      box-shadow: 0 0 16px rgba(249,115,22,0.4);
      position: relative;
    }

    .mz-avatar::after {
      content: '';
      position: absolute;
      bottom: 1px;
      right: 1px;
      width: 9px;
      height: 9px;
      border-radius: 50%;
      background: #22c55e;
      border: 2px solid #070d1a;
    }

    .mz-header-info {
      flex: 1;
    }

    .mz-header-name {
      font-size: 14px;
      font-weight: 600;
      color: #f1f5f9;
      letter-spacing: -0.01em;
    }

    .mz-header-status {
      font-size: 11px;
      color: #22c55e;
      display: flex;
      align-items: center;
      gap: 4px;
      margin-top: 1px;
    }

    .mz-status-dot {
      width: 5px;
      height: 5px;
      border-radius: 50%;
      background: #22c55e;
      animation: mz-blink 2s ease infinite;
    }

    @keyframes mz-blink { 0%,100%{opacity:1} 50%{opacity:0.3} }

    .mz-header-actions {
      display: flex;
      gap: 6px;
    }

    .mz-header-btn {
      width: 30px;
      height: 30px;
      border-radius: 8px;
      border: 1px solid rgba(148,163,184,0.15);
      background: rgba(15,23,42,0.6);
      color: #9ca3af;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.15s;
    }

    .mz-header-btn:hover {
      background: rgba(249,115,22,0.15);
      border-color: rgba(249,115,22,0.4);
      color: #f97316;
    }

    /* ── Suggestions ── */
    .mz-suggestions {
      padding: 10px 14px 6px;
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
      overflow-x: hidden;
      flex-shrink: 0;
      scrollbar-width: none;
      position: relative;
      z-index: 5;
    }

    .mz-suggestions::-webkit-scrollbar { display: none; }

    .mz-suggestion-chip {
      padding: 6px 12px;
      border-radius: 999px;
      border: 1px solid rgba(249,115,22,0.3);
      background: rgba(249,115,22,0.08);
      color: #fed7aa;
      font-size: 11px;
      font-weight: 500;
      cursor: pointer;
      white-space: nowrap;
      transition: all 0.15s;
      flex-shrink: 0;
    }

    .mz-suggestion-chip:hover {
      background: rgba(249,115,22,0.2);
      border-color: rgba(249,115,22,0.6);
      color: #fff;
      transform: translateY(-1px);
    }

    /* ── Messages ── */
    .mz-messages {
      flex: 1;
      overflow-y: auto;
      padding: 10px 14px 6px;
      display: flex;
      flex-direction: column;
      gap: 12px;
      scroll-behavior: smooth;
      position: relative;
      z-index: 1;
    }

    .mz-messages::-webkit-scrollbar {
      width: 4px;
    }
    .mz-messages::-webkit-scrollbar-track {
      background: transparent;
    }
    .mz-messages::-webkit-scrollbar-thumb {
      background: rgba(148,163,184,0.2);
      border-radius: 2px;
    }

    .mz-msg {
      display: flex;
      gap: 8px;
      align-items: flex-end;
      animation: mz-msg-in 0.28s cubic-bezier(0.34,1.2,0.64,1);
    }

    @keyframes mz-msg-in {
      from { opacity: 0; transform: translateY(10px) scale(0.95); }
      to   { opacity: 1; transform: translateY(0) scale(1); }
    }

    .mz-msg.user {
      flex-direction: row-reverse;
    }

    .mz-msg-avatar {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      flex-shrink: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: 700;
    }

    .mz-msg.bot .mz-msg-avatar {
      background: linear-gradient(135deg, #f97316, #ec4899);
      color: white;
    }

    .mz-msg.user .mz-msg-avatar {
      background: rgba(148,163,184,0.15);
      color: #9ca3af;
    }

    .mz-msg-bubble {
      max-width: 80%;
      padding: 10px 14px;
      border-radius: 16px;
      font-size: 13.5px;
      line-height: 1.5;
      position: relative;
    }

    .mz-msg.bot .mz-msg-bubble {
      background: rgba(15,23,42,0.85);
      border: 1px solid rgba(148,163,184,0.12);
      color: #e2e8f0;
      border-bottom-left-radius: 4px;
    }

    .mz-msg.user .mz-msg-bubble {
      background: linear-gradient(135deg, rgba(249,115,22,0.85), rgba(236,72,153,0.75));
      color: white;
      border-bottom-right-radius: 4px;
      font-weight: 400;
    }

    .mz-msg-time {
      font-size: 10px;
      color: #4b5563;
      margin-top: 4px;
      text-align: right;
    }

    .mz-msg.bot .mz-msg-time { text-align: left; }

    /* typing indicator */
    .mz-typing {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 10px 14px;
      background: rgba(15,23,42,0.85);
      border: 1px solid rgba(148,163,184,0.12);
      border-radius: 16px;
      border-bottom-left-radius: 4px;
      width: fit-content;
    }

    .mz-typing span {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: #f97316;
      animation: mz-dot 1.2s ease infinite;
    }

    .mz-typing span:nth-child(2) { animation-delay: 0.2s; background: #fb923c; }
    .mz-typing span:nth-child(3) { animation-delay: 0.4s; background: #ec4899; }

    @keyframes mz-dot {
      0%, 80%, 100% { transform: scale(0.7); opacity: 0.5; }
      40%           { transform: scale(1.1); opacity: 1; }
    }

    /* markdown inside bubble */
    .mz-msg-bubble strong { color: #f97316; font-weight: 600; }
    .mz-msg-bubble em { color: #c084fc; font-style: italic; }
    .mz-msg-bubble ul { padding-left: 16px; margin-top: 6px; }
    .mz-msg-bubble li { margin-bottom: 3px; }
    .mz-msg-bubble a { color: #f97316; text-decoration: underline; }
    .mz-msg-bubble code {
      background: rgba(249,115,22,0.12);
      color: #fb923c;
      padding: 1px 5px;
      border-radius: 4px;
      font-size: 12px;
      font-family: 'JetBrains Mono', monospace;
    }
    .mz-msg-bubble hr {
      border: none;
      border-top: 1px solid rgba(148,163,184,0.15);
      margin: 8px 0;
    }
    .mz-msg-bubble p + p { margin-top: 6px; }

    /* ── Input Area ── */
    .mz-input-area {
      padding: 12px 14px 14px;
      border-top: 1px solid rgba(148,163,184,0.1);
      flex-shrink: 0;
      background: rgba(7,13,26,0.95);
    }

    .mz-input-row {
      display: flex;
      gap: 8px;
      align-items: flex-end;
    }

    .mz-input-wrap {
      flex: 1;
      position: relative;
    }

    .mz-input {
      width: 100%;
      padding: 10px 14px;
      border-radius: 12px;
      border: 1px solid rgba(148,163,184,0.2);
      background: rgba(15,23,42,0.9);
      color: #e2e8f0;
      font-size: 13.5px;
      line-height: 1.4;
      resize: none;
      outline: none;
      font-family: inherit;
      transition: border-color 0.15s;
      min-height: 42px;
      max-height: 100px;
    }

    .mz-input:focus {
      border-color: rgba(249,115,22,0.5);
      box-shadow: 0 0 0 3px rgba(249,115,22,0.08);
    }

    .mz-input::placeholder { color: #4b5563; }

    .mz-send-btn {
      width: 42px;
      height: 42px;
      border-radius: 12px;
      border: none;
      background: linear-gradient(135deg, #f97316, #ec4899);
      color: white;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      transition: all 0.15s;
      box-shadow: 0 4px 14px rgba(249,115,22,0.35);
    }

    .mz-send-btn:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 20px rgba(249,115,22,0.5);
    }

    .mz-send-btn:disabled {
      opacity: 0.4;
      cursor: not-allowed;
      transform: none;
    }

    .mz-input-footer {
      font-size: 10px;
      color: #374151;
      text-align: center;
      margin-top: 8px;
    }

    .mz-input-footer span { color: #f97316; }

    /* ── Welcome ── */
    .mz-welcome {
      padding: 8px 14px 4px;
      flex-shrink: 0;
    }

    .mz-welcome-card {
      padding: 14px 16px;
      border-radius: 14px;
      background: linear-gradient(135deg, rgba(249,115,22,0.1), rgba(236,72,153,0.08));
      border: 1px solid rgba(249,115,22,0.2);
      position: relative;
      overflow: hidden;
    }

    .mz-welcome-card::before {
      content: '';
      position: absolute;
      top: -20px;
      right: -20px;
      width: 80px;
      height: 80px;
      border-radius: 50%;
      background: radial-gradient(circle, rgba(249,115,22,0.15), transparent 70%);
      pointer-events: none;
    }

    .mz-welcome-title {
      font-size: 13px;
      font-weight: 600;
      color: #f1f5f9;
      margin-bottom: 3px;
    }

    .mz-welcome-sub {
      font-size: 11.5px;
      color: #6b7280;
      line-height: 1.5;
    }

    /* Responsive */
    @media (max-width: 480px) {
      .mz-chat-window {
        right: 0;
        bottom: 0;
        width: 100vw;
        max-height: 100vh;
        height: 100vh;
        border-radius: 20px 20px 0 0;
      }
      .mz-chat-trigger {
        right: 16px;
        bottom: 16px;
      }
    }
  `;

    /* ─── HTML ────────────────────────────────────────────────────────────── */
    const HTML = `
    <div class="mz-chat-widget">
      <!-- Trigger Button -->
      <button class="mz-chat-trigger" id="mzTrigger" aria-label="Open AI Support">
        <svg class="mz-icon-chat" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          <circle cx="9" cy="10" r="1" fill="white" stroke="none"/>
          <circle cx="12" cy="10" r="1" fill="white" stroke="none"/>
          <circle cx="15" cy="10" r="1" fill="white" stroke="none"/>
        </svg>
        <svg class="mz-icon-close" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round">
          <line x1="18" y1="6" x2="6" y2="18"/>
          <line x1="6" y1="6" x2="18" y2="18"/>
        </svg>
        <div class="mz-unread-badge" id="mzBadge">1</div>
      </button>

      <!-- Chat Window -->
      <div class="mz-chat-window" id="mzWindow">
        <!-- Header -->
        <div class="mz-chat-header">
          <div class="mz-avatar">🎬</div>
          <div class="mz-header-info">
            <div class="mz-header-name" data-i18n="ai_chat.header.name">MovieZone AI</div>
            <div class="mz-header-status">
              <div class="mz-status-dot"></div>
              <span data-i18n="ai_chat.header.status"> Luôn sẵn sàng hỗ trợ </span>
            </div>
          </div>
          <div class="mz-header-actions">
            <button class="mz-header-btn" id="mzClearBtn" data-i18n-title="ai_chat.actions.clear">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
                <path d="M10 11v6M14 11v6"/>
              </svg>
            </button>
            <button class="mz-header-btn" id="mzMinimizeBtn" data-i18n-title="ai_chat.actions.minimize">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- Welcome card shown only on first open -->
        <div class="mz-welcome" id="mzWelcome">
          <div class="mz-welcome-card">
            <div class="mz-welcome-title" data-i18n="ai_chat.welcome_title">👋 Xin chào! Tôi là AI hỗ trợ của MovieZone</div>
            <div class="mz-welcome-sub" data-i18n="ai_chat.welcome_sub">Hỏi tôi về phim hot, gói Premium, hoặc bất kỳ điều gì về MovieZone nhé!</div>
          </div>
        </div>

        <!-- Quick suggestions -->
        <div class="mz-suggestions" id="mzSuggestions">
          <button class="mz-suggestion-chip" data-i18n="ai_chat.suggestions.premium">💎 Gói Premium</button>
          <button class="mz-suggestion-chip" data-i18n="ai_chat.suggestions.hot">🔥 Phim đang hot</button>
          <button class="mz-suggestion-chip" data-i18n="ai_chat.suggestions.action">🎬 Phim hành động</button>
          <button class="mz-suggestion-chip" data-i18n="ai_chat.suggestions.tv">📺 Series hay</button>
          <button class="mz-suggestion-chip" data-i18n="ai_chat.suggestions.high_rated">⭐ Đánh giá cao</button>
          <button class="mz-suggestion-chip" data-i18n="ai_chat.suggestions.new">🆕 Mới thêm gần đây</button>
        </div>

        <!-- Messages -->
        <div class="mz-messages" id="mzMessages"></div>

        <!-- Input -->
        <div class="mz-input-area">
          <div class="mz-input-row">
            <div class="mz-input-wrap">
              <textarea
                class="mz-input"
                id="mzInput"
                data-i18n-placeholder="ai_chat.input.placeholder"
                rows="1"
              ></textarea>
            </div>
            <button class="mz-send-btn" id="mzSendBtn" disabled>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <line x1="22" y1="2" x2="11" y2="13"/>
                <polygon points="22 2 15 22 11 13 2 9 22 2"/>
              </svg>
            </button>
          </div>
          <div class="mz-input-footer" data-i18n="ai_chat.input.footer">Powered by LLaMA AI · MovieZone</div>
        </div>
      </div>
    </div>
  `;

    /* ─── State ───────────────────────────────────────────────────────────── */
    let isOpen = false;
    let isLoading = false;
    let conversationHistory = JSON.parse(sessionStorage.getItem('mz_chat_history') || '[]');
    let hasOpened = sessionStorage.getItem('mz_chat_opened') === 'true';

    /* ─── Init ─────────────────────────────────────────────────────────────── */
    function init() {
        // Inject styles
        const style = document.createElement('style');
        style.textContent = CSS;
        document.head.appendChild(style);

        // Inject HTML
        const wrapper = document.createElement('div');
        wrapper.innerHTML = HTML;
        document.body.appendChild(wrapper.firstElementChild);

        // Bind events
        bindEvents();

        // Restore messages từ sessionStorage
        if (conversationHistory.length > 0) {
            document.getElementById('mzSuggestions').style.display = 'none';
            document.getElementById('mzWelcome').style.display = 'none';
            conversationHistory.forEach(msg => {
                if (msg.role === 'user') addUserMessage(msg.content);
                else if (msg.role === 'assistant') addBotMessage(msg.content);
            });
        }

        // Show badge after 3s to draw attention
        setTimeout(() => {
            const badge = document.getElementById('mzBadge');
            if (badge && conversationHistory.length === 0) badge.classList.add('show');
        }, 3000);
    }

    /* ─── Events ─────────────────────────────────────────────────────────── */
    function bindEvents() {
        const trigger = document.getElementById('mzTrigger');
        const window_ = document.getElementById('mzWindow');
        const input = document.getElementById('mzInput');
        const sendBtn = document.getElementById('mzSendBtn');
        const clearBtn = document.getElementById('mzClearBtn');
        const minimizeBtn = document.getElementById('mzMinimizeBtn');
        const suggestions = document.getElementById('mzSuggestions');

        trigger.addEventListener('click', toggleChat);
        sendBtn.addEventListener('click', sendMessage);
        clearBtn.addEventListener('click', clearChat);
        minimizeBtn.addEventListener('click', toggleChat);

        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                if (!sendBtn.disabled) sendMessage();
            }
        });

        input.addEventListener('input', () => {
            sendBtn.disabled = !input.value.trim() || isLoading;
            // auto-resize
            input.style.height = 'auto';
            input.style.height = Math.min(input.scrollHeight, 100) + 'px';
        });

        // Suggestion chips
        suggestions.addEventListener('click', (e) => {
            const chip = e.target.closest('.mz-suggestion-chip');
            if (!chip) return;
            input.value = chip.textContent.trim(); // giữ nguyên, kể cả emoji
            input.dispatchEvent(new Event('input'));
            sendMessage();
        });
    }

    /* ─── Toggle ─────────────────────────────────────────────────────────── */
    function toggleChat() {
        isOpen = !isOpen;
        const trigger = document.getElementById('mzTrigger');
        const window_ = document.getElementById('mzWindow');
        const badge = document.getElementById('mzBadge');

        trigger.classList.toggle('open', isOpen);
        window_.classList.toggle('open', isOpen);

        if (isOpen) {
            badge.classList.remove('show');
            document.getElementById('mzInput').focus();

            // Auto-send welcome message on first open
            if (!hasOpened) {
                hasOpened = true;
                sessionStorage.setItem('mz_chat_opened', 'true');
                setTimeout(() => {
                    addBotMessage('Xin chào! 🎬 Tôi là AI hỗ trợ của **MovieZone**. Tôi có thể giúp bạn:\n\n- 🔥 Tìm **phim đang hot** theo thể loại\n- 💎 Thông tin về **gói Premium**\n- 📊 Phim được **đánh giá cao nhất**\n- 🆕 Phim / Series **mới thêm** gần đây\n- ❓ Giải đáp mọi thắc mắc về MovieZone\n\nBạn muốn hỏi gì nào?');
                }, 400);
            }
        }
    }

    /* ─── Send Message ───────────────────────────────────────────────────── */
    async function sendMessage() {
        const input = document.getElementById('mzInput');
        const sendBtn = document.getElementById('mzSendBtn');
        const text = input.value.trim();
        if (!text || isLoading) return;

        // Clear input
        input.value = '';
        input.style.height = 'auto';
        sendBtn.disabled = true;

        // Hide suggestions after first message
        document.getElementById('mzSuggestions').style.display = 'none';
        document.getElementById('mzWelcome').style.display = 'none';

        // Add user message to UI
        addUserMessage(text);

        // Add to conversation history
        conversationHistory.push({role: 'user', content: text});
        sessionStorage.setItem('mz_chat_history', JSON.stringify(conversationHistory));
        // Show typing indicator
        isLoading = true;
        const typingId = showTyping();

        try {
            // Fetch context data from the app
            const contextData = await fetchContext();

            // Call backend API
            const response = await fetch('/api/ai-chat', {
                method: 'POST', headers: {
                    'Content-Type': 'application/json', ...(localStorage.getItem('token') ? {'Authorization': 'Bearer ' + localStorage.getItem('token')} : {})
                }, body: JSON.stringify({
                    messages: conversationHistory, context: contextData
                })
            });

            removeTyping(typingId);

            if (!response.ok) {
                throw new Error('API error ' + response.status);
            }

            const data = await response.json();
            const reply = data.reply || data.message || t("ai_chat.messages.busy");

            addBotMessage(reply);
            conversationHistory.push({role: 'assistant', content: reply});
            sessionStorage.setItem('mz_chat_history', JSON.stringify(conversationHistory));
        } catch (err) {
            removeTyping(typingId);
            console.error('[MovieZone AI]', err);
            addBotMessage(t("ai_chat.messages.error"));
        } finally {
            isLoading = false;
            sendBtn.disabled = !input.value.trim();
        }
    }

    /* ─── Fetch context (movies, packs) from existing APIs ───────────────── */
    async function fetchContext() {
        const ctx = {};
        try {
            const token = localStorage.getItem('token');
            const headers = token ? {Authorization: 'Bearer ' + token} : {};

            // Fetch movies
            const moviesRes = await fetch('/api/public/movies?type=all&page=1', {headers});
            if (moviesRes.ok) {
                const moviesData = await moviesRes.json();
                ctx.movies = (moviesData.items || []).slice(0, 20).map(m => ({
                    id: m.id, title: m.title, type: m.type, year: m.year, rating: m.rating
                }));
            }

            // Fetch subscription packs
            const packsRes = await fetch('/api/packs', {headers});
            if (packsRes.ok) {
                ctx.packs = await packsRes.json();
            }

            // Fetch watch history (chỉ khi đã login)
            if (token) {
                try {
                    const histRes = await fetch('/api/user/history', {headers});
                    if (histRes.ok) ctx.watchHistory = await histRes.json();
                } catch (e) { /* bỏ qua */
                }

                try {
                    const profileRes = await fetch('/api/user/profile', {headers});
                    if (profileRes.ok) ctx.userProfile = await profileRes.json();
                } catch (e) { /* bỏ qua */
                }
            }

        } catch (e) {
            console.warn('[MovieZone AI] Context fetch failed', e);
        }
        return ctx;
    }

    /* ─── UI Helpers ─────────────────────────────────────────────────────── */
    function addUserMessage(text) {
        const messages = document.getElementById('mzMessages');
        const time = getTime();

        const div = document.createElement('div');
        div.className = 'mz-msg user';
        div.innerHTML = `
      <div>
        <div class="mz-msg-bubble">${escapeHtml(text)}</div>
        <div class="mz-msg-time">${time}</div>
      </div>
      <div class="mz-msg-avatar">👤</div>
    `;
        messages.appendChild(div);
        scrollToBottom();
    }

    function addBotMessage(text) {
        const messages = document.getElementById('mzMessages');
        const time = getTime();

        const div = document.createElement('div');
        div.className = 'mz-msg bot';
        div.innerHTML = `
      <div class="mz-msg-avatar">MZ</div>
      <div>
        <div class="mz-msg-bubble">${renderMarkdown(text)}</div>
        <div class="mz-msg-time">${time}</div>
      </div>
    `;
        messages.appendChild(div);
        scrollToBottom();
    }

    function showTyping() {
        const messages = document.getElementById('mzMessages');
        const id = 'typing-' + Date.now();

        const div = document.createElement('div');
        div.className = 'mz-msg bot';
        div.id = id;
        div.innerHTML = `
      <div class="mz-msg-avatar">MZ</div>
      <div class="mz-typing">
        <span></span><span></span><span></span>
      </div>
    `;
        messages.appendChild(div);
        scrollToBottom();
        return id;
    }

    function removeTyping(id) {
        const el = document.getElementById(id);
        if (el) el.remove();
    }

    function clearChat() {
        conversationHistory = [];
        sessionStorage.removeItem('mz_chat_history');
        sessionStorage.removeItem('mz_chat_opened');
        document.getElementById('mzMessages').innerHTML = '';
        document.getElementById('mzSuggestions').style.display = '';
        document.getElementById('mzWelcome').style.display = '';
        hasOpened = false;
        setTimeout(() => {
            addBotMessage(t("ai_chat.messages.cleared"));
            hasOpened = true;
        }, 100);
    }

    function scrollToBottom() {
        const messages = document.getElementById('mzMessages');
        setTimeout(() => {
            messages.scrollTop = messages.scrollHeight;
        }, 50);
    }

    function getTime() {
        const lang = localStorage.getItem("lang") || "en";
        const localeMap = {
            vi: "vi-VN", en: "en-US", my: "my-MY", ja: "ja-JP", de: "de-DE"
        };
        return new Date().toLocaleTimeString(localeMap[lang] || "en-US", {
            hour: '2-digit', minute: '2-digit'
        });
    }

    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    /* Simple markdown renderer */
    function renderMarkdown(text) {
        return text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            // Bold
            .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
            // Italic
            .replace(/\*(.+?)\*/g, '<em>$1</em>')
            // Inline code
            .replace(/`(.+?)`/g, '<code>$1</code>')
            // HR
            .replace(/^---$/gm, '<hr>')
            // Unordered lists
            .replace(/^[-•]\s(.+)$/gm, '<li>$1</li>')
            .replace(/(<li>.*<\/li>)/gs, '<ul>$1</ul>')
            // Numbered lists
            .replace(/^\d+\.\s(.+)$/gm, '<li>$1</li>')
            // Line breaks
            .replace(/\n\n/g, '</p><p>')
            .replace(/\n/g, '<br>')
            // Wrap in paragraphs
            .replace(/^(.+)$/, '<p>$1</p>');
    }

    /* ─── Start ─────────────────────────────────────────────────────────── */
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
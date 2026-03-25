let translations = {};
let cache = {};

// lấy value từ key dạng nav.home
function getTranslation(key) {
    return key.split(".").reduce((obj, k) => obj?.[k], translations);
}

function t(key, params = {}) {
    let value = getTranslation(key);
    if (value == null) return key;

    // replace {{variable}}
    Object.keys(params).forEach(k => {
        value = value.replace(new RegExp(`{{${k}}}`, "g"), params[k]);
    });

    return value;
}

// áp dụng text
function applyTranslations() {

    document.querySelectorAll("[data-i18n]").forEach(el => {
        const key = el.getAttribute("data-i18n");
        const value = t(key);

        if (value) el.textContent = value;
    });

    document.querySelectorAll("[data-i18n-placeholder]").forEach(el => {
        const key = el.getAttribute("data-i18n-placeholder");
        const value = t(key);

        if (value) el.placeholder = value;
    });

    document.querySelectorAll("[data-i18n-title]").forEach(el => {
        const key = el.getAttribute("data-i18n-title");
        const value = t(key);
        if (value) el.title = value;
    });
}

// load json language
async function loadLanguage(lang) {

    try {
        if (!cache[lang]) {
            const res = await fetch(`/i18n/${lang}.json`);
            cache[lang] = await res.json();
        }

        translations = cache[lang];

        applyTranslations();
        updateLangFlag();
        renderUsername();

        window.dispatchEvent(new Event("languageChanged"));

    } catch (err) {
        console.error("Failed to load language:", err);
    }
}

// đổi language
function changeLanguage(lang) {
    // lưu ngôn ngữ
    localStorage.setItem("lang", lang);

    // load file ngôn ngữ rồi apply lại translation
    loadLanguage(lang).then(() => {

        // nếu đang ở trang admin thì reload fragment đang mở
        if (window.adminApi && window.adminApi.loadFragment) {
            const active = document.querySelector(".side-item.active");
            if (active) {
                const frag = active.getAttribute("data-fragment");
                if (frag) {
                    window.adminApi.loadFragment(frag);
                }
            }
        }
    });

    // đóng dropdown nếu có
    const dropdown = document.querySelector(".lang-dropdown");
    if (dropdown) dropdown.classList.remove("open");
}

// update flag
function updateLangFlag() {

    const flag = document.getElementById("langFlag");
    if (!flag) return;

    const lang = localStorage.getItem("lang") || "en";

    // if (lang === "vi") {
    //     flag.src = "https://flagcdn.com/w20/vn.png";
    // } else {
    //     flag.src = "https://flagcdn.com/w20/gb.png";
    // }
    const flags = {
        vi: "vn",
        en: "gb",
        my: "mm",
        ja: "jp",
        de: "de"
    };

    flag.src = `https://flagcdn.com/w20/${flags[lang] || "gb"}.png`;
}

// dropdown toggle
function initLangDropdown() {

    const btn = document.getElementById("langCurrent");
    const dropdown = document.querySelector(".lang-dropdown");

    if (!btn || !dropdown) return;

    btn.addEventListener("click", () => {
        dropdown.classList.toggle("open");
    });

    document.addEventListener("click", e => {
        if (!dropdown.contains(e.target)) {
            dropdown.classList.remove("open");
        }
    });
}

function renderUsername() {
    const usernameEl = document.getElementById("navUsername");
    if (!usernameEl) return;

    const username = localStorage.getItem("username") || "User";

    const text = t("nav.hello_user", {
        name: username
    });

    usernameEl.textContent = "👋 " + text;
}

function normalizeGenreKey(name) {
    return name
        .toLowerCase()
        .replace(/&/g, "and")        // Action & Adventure → action_and_adventure
        .replace(/[^a-z0-9]+/g, "_") // space, - → _
        .replace(/^_|_$/g, "");      // remove _ đầu/cuối
}

function translateGenre(name) {
    const key = normalizeGenreKey(name);
    const translated = t(`genres.${key}`);
    return translated === `genres.${key}` ? name : translated;
}

window.addEventListener("languageChanged", () => {
    // render lại genres menu
    if (genresFromServer) {
        const map = {};
        genresFromServer.forEach(g => {
            map[g.name] = g.items || [];
        });
        populateGenresMenu(map);
    } else {
        const genresMap = collectGenres(movies);
        populateGenresMenu(genresMap);
    }
});

// init khi load page
document.addEventListener("DOMContentLoaded", () => {

    const lang = localStorage.getItem("lang") || "en";

    loadLanguage(lang);

    initLangDropdown();
});
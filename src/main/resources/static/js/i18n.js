let translations = {};

// lấy value từ key dạng nav.home
function getTranslation(key) {
    const keys = key.split(".");
    let value = translations;

    keys.forEach(k => {
        if (value) value = value[k];
    });

    return value;
}

function t(key) {
    const value = getTranslation(key);
    return value || key;
}

// áp dụng text
function applyTranslations() {

    document.querySelectorAll("[data-i18n]").forEach(el => {
        const key = el.getAttribute("data-i18n");
        const value = getTranslation(key);

        if (value) el.textContent = value;
    });

    document.querySelectorAll("[data-i18n-placeholder]").forEach(el => {
        const key = el.getAttribute("data-i18n-placeholder");
        const value = getTranslation(key);

        if (value) el.placeholder = value;
    });
}

// load json language
async function loadLanguage(lang) {

    try {
        const res = await fetch(`/i18n/${lang}.json`);
        translations = await res.json();

        applyTranslations();
        updateLangFlag();
        if (window.renderMovies) {
            renderMovies();
        }

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

        // dịch lại toàn bộ DOM hiện tại
        if (typeof applyTranslations === "function") {
            applyTranslations();
        }

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

// init khi load page
document.addEventListener("DOMContentLoaded", () => {

    const lang = localStorage.getItem("lang") || "en";

    loadLanguage(lang);

    initLangDropdown();
});
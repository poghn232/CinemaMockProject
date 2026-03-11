//
// let translations = {};
//
// function applyTranslations() {
//
//     document.querySelectorAll("[data-i18n]").forEach(el => {
//         const key = el.getAttribute("data-i18n");
//
//         const keys = key.split(".");
//         let value = translations;
//
//         keys.forEach(k => {
//             if (value) value = value[k];
//         });
//
//         if (value) el.innerText = value;
//     });
//
//     document.querySelectorAll("[data-i18n-placeholder]").forEach(el => {
//         const key = el.getAttribute("data-i18n-placeholder");
//
//         const keys = key.split(".");
//         let value = translations;
//
//         keys.forEach(k => {
//             if (value) value = value[k];
//         });
//
//         if (value) el.placeholder = value;
//     });
// }
//
// async function loadLanguage(lang) {
//     const res = await fetch(`/i18n/${lang}.json`);
//     translations = await res.json();
//
//     applyTranslations();   // dùng lại function
// }
//
// function changeLanguage(lang) {
//     localStorage.setItem("lang", lang);
//     loadLanguage(lang);
// }
//
// document.addEventListener("DOMContentLoaded", () => {
//     const lang = localStorage.getItem("lang") || "en";
//     loadLanguage(lang);
// });
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

// áp dụng text
function applyTranslations() {

    document.querySelectorAll("[data-i18n]").forEach(el => {
        const key = el.getAttribute("data-i18n");
        const value = getTranslation(key);

        if (value) el.innerText = value;
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

    localStorage.setItem("lang", lang);

    loadLanguage(lang);

    // đóng dropdown nếu có
    const dropdown = document.querySelector(".lang-dropdown");
    if (dropdown) dropdown.classList.remove("open");
}

// update flag
function updateLangFlag() {

    const flag = document.getElementById("langFlag");
    if (!flag) return;

    const lang = localStorage.getItem("lang") || "en";

    if (lang === "vi") {
        flag.src = "https://flagcdn.com/w20/vn.png";
    } else {
        flag.src = "https://flagcdn.com/w20/gb.png";
    }
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
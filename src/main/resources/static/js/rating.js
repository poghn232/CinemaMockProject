/**
 * rating.js - Handles User Rating logic for MovieZone
 */

const RatingSystem = {
    async getStatus(type, id) {
        if (!id) return { hasRated: false, userRating: 0, average: 0, count: 0 };
        const token = localStorage.getItem("token");
        const headers = token ? { Authorization: "Bearer " + token } : {};
        try {
            const res = await fetch(`/api/reviews/status?type=${type}&id=${id}`, { headers });
            if (!res.ok) return null;
            return await res.json();
        } catch (e) {
            console.error("Failed to get rating status", e);
            return null;
        }
    },

    async saveRating(type, id, rating) {
        const token = localStorage.getItem("token");
        if (!token) {
            alert("Please login to rate!");
            return null;
        }
        try {
            const res = await fetch("/api/reviews/save", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: "Bearer " + token
                },
                body: JSON.stringify({ type, id, rating })
            });
            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.message || "Failed to save rating");
            }
            return await res.json();
        } catch (e) {
            console.error("Error saving rating:", e);
            alert(e.message);
            return null;
        }
    },

    renderStars(containerId, initialRating, onRate) {
        const container = document.getElementById(containerId);
        if (!container) return;

        container.innerHTML = "";
        container.style.display = "flex";
        container.style.gap = "5px";
        container.style.fontSize = "24px";
        container.style.cursor = "pointer";
        container.style.color = "#9ca3af";

        for (let i = 1; i <= 5; i++) {
            const star = document.createElement("span");
            star.innerHTML = "★";
            star.dataset.value = i;
            star.style.display = "inline-block";
            star.style.padding = "0 2px";
            star.style.userSelect = "none";
            star.style.WebkitUserSelect = "none";
            star.style.transition = "transform 0.1s ease, color 0.1s ease";
            
            if (i <= (initialRating / 2)) {
                star.style.color = "#f97316";
            }

            star.onclick = () => {
                if (onRate) onRate(i * 2); // Map 1-5 stars to 1-10 rating
            };

            star.onmouseover = () => {
                const stars = container.querySelectorAll("span");
                stars.forEach(s => {
                    if (parseInt(s.dataset.value) <= i) {
                        s.style.color = "#f97316";
                        s.style.transform = "scale(1.2)";
                    } else {
                        s.style.color = "#9ca3af";
                        s.style.transform = "scale(1)";
                    }
                });
            };

            star.onmouseout = () => {
                const stars = container.querySelectorAll("span");
                stars.forEach(s => {
                    s.style.transform = "scale(1)";
                    if (parseInt(s.dataset.value) <= (initialRating / 2)) {
                        s.style.color = "#f97316";
                    } else {
                        s.style.color = "#9ca3af";
                    }
                });
            };

            container.appendChild(star);
        }
    }
};

window.RatingSystem = RatingSystem;

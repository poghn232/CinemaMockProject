document.head.insertAdjacentHTML("beforeend", `
  <style>
    .ad-wrapper { position: fixed; z-index: 9999; }
    .ad-wrapper img { display: block; }
    .ad-close {
      position: fixed;
      z-index: 10000;
      width: 22px; height: 22px;
      background: #333; color: #fff;
      border: none; border-radius: 50%;
      cursor: pointer; font-size: 13px;
      line-height: 22px; text-align: center; padding: 0;
    }
    .ad-close:hover { background: #e00; }
    #leftColAd-wrap  { top: 50%; left: 0;    transform: translateY(-50%); }
    #rightColAd-wrap { top: 50%; right: 0;   transform: translateY(-50%); }
    #botRowAd-wrap   { bottom: 0; left: 50%; transform: translateX(-50%); }
  </style>
`);

const ads = [
    { id: "leftColAd",  src: "/api/image/2" },
    { id: "rightColAd", src: "/api/image/2" },
    { id: "botRowAd",   src: "/api/image/1" }
];

ads.forEach(ad => {
    const wrapper = document.createElement("div");
    wrapper.className = "ad-wrapper";
    wrapper.id = `${ad.id}-wrap`;

    const img = document.createElement("img");
    img.id = ad.id;
    img.alt = "advertisement";
    img.src = ad.src;

    wrapper.appendChild(img);
    document.body.appendChild(wrapper);

    const btn = document.createElement("button");
    btn.className = "ad-close";
    btn.textContent = "✕";
    document.body.appendChild(btn);

    function positionBtn() {
        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                const rect = wrapper.getBoundingClientRect();
                const topPos = Math.max(rect.top, 5);

                if (ad.id === "leftColAd") {
                    btn.style.left = (rect.right - 11) + "px";
                    btn.style.top  = topPos + "px";
                } else if (ad.id === "rightColAd") {
                    btn.style.left = (rect.left - 11) + "px";
                    btn.style.top  = topPos + "px";
                } else {
                    btn.style.left = (rect.right - 11) + "px";
                    btn.style.top  = (rect.top - 22) + "px";
                }
            });
        });
    }

    img.addEventListener("load", positionBtn);
    if (img.complete) positionBtn();

    btn.addEventListener("click", () => {
        wrapper.remove();
        btn.remove();
    });
});
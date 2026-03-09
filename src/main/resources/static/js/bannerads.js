document.head.insertAdjacentHTML("beforeend", `
  <style>
    #leftColAd  { position: fixed; top: 50%; left: 0;   transform: translateY(-50%); }
    #rightColAd { position: fixed; top: 50%; right: 0;  transform: translateY(-50%); }
    #botRowAd   { position: fixed; bottom: 0; left: 50%; transform: translateX(-50%); }
  </style>
`);
const ads = [
    {id: "leftColAd", src: "/api/image/2"},
    {id: "rightColAd", src: "/api/image/2"},
    {id: "botRowAd", src: "/api/image/1"}
];

ads.forEach(ad => {
    const img = document.createElement("img");
    img.id = ad.id;
    img.alt = "advertisement";
    img.src = ad.src;
    document.body.appendChild(img);
});
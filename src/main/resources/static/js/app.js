const $ = (id) => document.getElementById(id);
const TITLES = {
  OUTAGE_IN_AREA: "Not just you: outage in your area",
  LIKELY_JUST_YOU: "Probably just your property",
  NOT_ENOUGH_DATA: "Not enough data yet"
};
const COLORS = { CONFIRMED: "#ef4444", LIKELY: "#f59e0b", POSSIBLE: "#3b82f6" };

// ---- Map ----
const map = L.map("map").setView([parseFloat($("lat").value), parseFloat($("lon").value)], 12);
L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
  maxZoom: 19,
  attribution: "&copy; OpenStreetMap contributors"
}).addTo(map);
const outageLayer = L.layerGroup().addTo(map);
const youMarker = L.marker([parseFloat($("lat").value), parseFloat($("lon").value)]).addTo(map);
youMarker.bindTooltip("You", { permanent: false });

function moveYou(lat, lon, pan) {
  $("lat").value = Number(lat).toFixed(5);
  $("lon").value = Number(lon).toFixed(5);
  youMarker.setLatLng([lat, lon]);
  if (pan) map.setView([lat, lon], Math.max(map.getZoom(), 13));
}

map.on("click", (e) => {
  moveYou(e.latlng.lat, e.latlng.lng, false);
  $("locMsg").textContent = "Location set from the map.";
});

function syncFromInputs() {
  const lat = parseFloat($("lat").value), lon = parseFloat($("lon").value);
  if (!Number.isNaN(lat) && !Number.isNaN(lon)) {
    youMarker.setLatLng([lat, lon]);
    map.setView([lat, lon], map.getZoom());
  }
}
$("lat").addEventListener("change", syncFromInputs);
$("lon").addEventListener("change", syncFromInputs);

// ---- API helpers ----
function coords() {
  const lat = parseFloat($("lat").value), lon = parseFloat($("lon").value);
  if (Number.isNaN(lat) || Number.isNaN(lon)) throw new Error("Enter a valid latitude and longitude.");
  return { lat, lon };
}

async function api(path, options) {
  const res = await fetch(path, options);
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(body.message || "Request failed (" + res.status + ")");
  return body;
}

// ---- Is it just me? ----
async function checkJustMe() {
  try {
    const { lat, lon } = coords();
    const v = await api("/api/outages/just-me?lat=" + lat + "&lon=" + lon);
    $("verdict").hidden = false;
    $("verdict").className = "card " + v.verdict;
    $("vTitle").textContent = TITLES[v.verdict];
    $("vMsg").textContent = v.message;
    $("vStats").textContent = v.offReports + " OFF/UNSTABLE and " + v.onReports +
      " ON/RESTORED reports within " + v.radiusMeters + " m in the last " + v.minutes + " min.";
  } catch (e) { alert(e.message); }
}

// ---- Reporting ----
async function sendReport(status) {
  try {
    const { lat, lon } = coords();
    await api("/api/reports", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status, latitude: lat, longitude: lon })
    });
    $("reportMsg").textContent = "Report sent: " + status + ".";
    await Promise.all([checkJustMe(), loadOutages()]);
  } catch (e) { $("reportMsg").textContent = e.message; }
}

// ---- Outages: list + map ----
async function loadOutages() {
  const list = $("outages");
  try {
    const outages = await api("/api/outages");
    list.replaceChildren();
    outageLayer.clearLayers();

    $("sActive").textContent = outages.length;
    $("sConfirmed").textContent = outages.filter(o => o.status === "CONFIRMED").length;
    $("sReports").textContent = outages.reduce((n, o) => n + o.reportCount, 0);
    $("sUpdated").textContent = "updated " + new Date().toLocaleTimeString();

    if (outages.length === 0) {
      const li = document.createElement("li");
      li.className = "muted";
      li.textContent = "No active outages reported.";
      list.append(li);
      return;
    }

    const bounds = [];
    for (const o of outages) {
      const pct = Math.round(o.confidence * 100);

      const li = document.createElement("li");
      const tag = document.createElement("span");
      tag.className = "tag " + o.status;
      tag.textContent = o.status;
      li.append(tag, " " + o.reportCount + " reports, confidence " + pct +
        "%, radius " + Math.round(o.radiusMeters) + " m, near " +
        o.centerLatitude.toFixed(3) + ", " + o.centerLongitude.toFixed(3));
      list.append(li);

      const color = COLORS[o.status] || "#94a3b8";
      const circle = L.circle([o.centerLatitude, o.centerLongitude], {
        radius: Math.max(o.radiusMeters, 100),
        color, fillColor: color, fillOpacity: 0.15 + 0.3 * o.confidence, weight: 2
      });
      const popup = document.createElement("div");
      popup.textContent = o.status + ": " + o.reportCount + " reports, confidence " + pct +
        "%, radius " + Math.round(o.radiusMeters) + " m. Started " +
        new Date(o.startedAt).toLocaleString() + ", last activity " +
        new Date(o.updatedAt).toLocaleString() + ".";
      circle.bindPopup(popup);
      circle.addTo(outageLayer);
      bounds.push(circle.getBounds());
      li.style.cursor = "pointer";
      li.addEventListener("click", () => { map.fitBounds(circle.getBounds()); circle.openPopup(); });
    }
    if (!window.__fitted) {
      map.fitBounds(bounds.reduce((a, b) => a.extend(b)), { maxZoom: 14 });
      window.__fitted = true;
    }
  } catch (e) { list.textContent = e.message; }
}

// ---- Wiring ----
$("check").addEventListener("click", checkJustMe);
$("locate").addEventListener("click", () => {
  if (!navigator.geolocation) { $("locMsg").textContent = "Geolocation is not supported."; return; }
  navigator.geolocation.getCurrentPosition(
    (p) => { moveYou(p.coords.latitude, p.coords.longitude, true);
             $("locMsg").textContent = "Using your location."; },
    () => { $("locMsg").textContent = "Could not get your location. Enter it manually or click the map."; });
});
document.querySelectorAll("[data-status]").forEach((b) =>
  b.addEventListener("click", () => sendReport(b.dataset.status)));

loadOutages();
setInterval(loadOutages, 30000);

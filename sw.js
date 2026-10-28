// AETHER AI service worker. build.sh stamps VERSION; a new value makes the app offer "Update now".
const VERSION = "2.5.0";
const CACHE = "aether-" + VERSION;
const SHELL = [
  "./",
  "./index.html",
  "./manifest.webmanifest",
  "./icons/icon-192.png",
  "./icons/icon-512.png",
  "./icons/maskable-512.png",
  "./icons/apple-touch-icon.png",
  "./icons/favicon-64.png"
];

self.addEventListener("install", (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(SHELL)));
});

self.addEventListener("activate", (e) => {
  e.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k.startsWith("aether-") && k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("message", (e) => {
  if (e.data === "SKIP_WAITING") self.skipWaiting();
});

// Background alerts from the AETHER server. If the app is open and in front, it shows its own alert instead.
const RING_MS = 10000; // keep ringing about this long, unless the alert is tapped or dismissed
const wait = (ms) => new Promise((r) => setTimeout(r, ms));
self.addEventListener("push", (e) => {
  let d = {};
  try { d = e.data ? e.data.json() : {}; } catch { d = { body: e.data && e.data.text() }; }
  e.waitUntil((async () => {
    const list = await self.clients.matchAll({ type: "window", includeUncontrolled: true });
    if (list.some((c) => c.focused && c.visibilityState === "visible") && !d.force) return;
    const title = d.title || "AETHER alert", tag = d.tag || "aether";
    const isWa = /^https:\/\/wa\.me\//.test(d.url || "");
    const opts = {
      body: d.body || "",
      icon: "icons/icon-192.png",
      badge: "icons/favicon-64.png",
      tag,
      renotify: true,
      requireInteraction: true,
      silent: false,
      vibrate: [500, 200, 500, 200, 500, 200, 800],
      actions: isWa
        ? [{ action: "open", title: "Send on WhatsApp" }, { action: "snooze", title: "Snooze 5 min" }]
        : [{ action: "snooze", title: "Snooze 5 min" }, { action: "done", title: "Got it" }],
      data: { url: d.url || "./", api: d.api || "", dev: d.dev || "", title, body: d.body || "" },
    };
    await self.registration.showNotification(title, opts);
    // Ring again (sound + vibration) every few seconds while the alert is still on screen.
    for (let t = 3500; t < RING_MS; t += 3500) {
      await wait(3500);
      const still = await self.registration.getNotifications({ tag });
      if (!still.length) break;
      await self.registration.showNotification(title, opts);
    }
  })());
});

// "Got it" and closing the alert just stop it. "Snooze" asks the server to send it again in 5 minutes.
self.addEventListener("notificationclick", (e) => {
  e.notification.close();
  const data = e.notification.data || {};
  if (e.action === "done") return;
  if (e.action === "snooze") {
    if (!data.api || !data.dev) return;
    e.waitUntil(
      fetch(data.api + "snooze", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ deviceId: data.dev, title: data.title, body: data.body, url: data.url, minutes: 5 }),
      }).catch(() => {})
    );
    return;
  }
  const target = new URL(data.url || "./", self.registration.scope);
  e.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((list) => {
      if (/^https:\/\/wa\.me\//.test(target.href)) return self.clients.openWindow(target.href);
      // Opening the app from an alert: it shows the alert full screen and reads it out loud.
      const app = list.find((c) => c.url.startsWith(self.registration.scope));
      if (app) { app.postMessage({ type: "say", title: data.title || "", body: data.body || "", url: target.href }); return app.focus(); }
      target.searchParams.set("say", data.title || "");
      if (data.body) target.searchParams.set("sayb", data.body);
      return self.clients.openWindow(target.href);
    })
  );
});

self.addEventListener("fetch", (e) => {
  const req = e.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);

  // Pages: try the network first so updates show up, fall back to the cached app offline.
  if (req.mode === "navigate") {
    e.respondWith(
      fetch(req)
        .then((res) => { const copy = res.clone(); caches.open(CACHE).then((c) => c.put("./index.html", copy)); return res; })
        .catch(() => caches.match("./index.html"))
    );
    return;
  }

  // App files and fonts: serve from cache, refresh in the background.
  const cacheable = url.origin === location.origin || url.hostname.endsWith("fonts.googleapis.com") || url.hostname.endsWith("fonts.gstatic.com");
  if (!cacheable) return;
  e.respondWith(
    caches.match(req).then((hit) => {
      const net = fetch(req)
        .then((res) => { if (res.ok) { const copy = res.clone(); caches.open(CACHE).then((c) => c.put(req, copy)); } return res; })
        .catch(() => hit);
      return hit || net;
    })
  );
});

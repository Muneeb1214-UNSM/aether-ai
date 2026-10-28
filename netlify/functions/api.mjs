// AETHER API: the app sends its upcoming alerts here so they can arrive while the app is closed.
//   GET  /api/health       -> is the server set up?
//   POST /api/sync         -> save this device's push subscription and upcoming alerts
//   POST /api/test         -> send a test notification now
//   POST /api/unsubscribe  -> forget this device
import { devices, sendPush, pushReady, VAPID_PUBLIC_KEY } from "../lib/push.mjs";

const MAX_ITEMS = 500;

const json = (body, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: {
      "content-type": "application/json",
      "access-control-allow-origin": "*",
      "access-control-allow-headers": "content-type",
      "access-control-allow-methods": "GET, POST, OPTIONS",
    },
  });

const str = (v, max) => String(v ?? "").slice(0, max);

function cleanItem(it) {
  const at = Number(it && it.at);
  if (!it || !Number.isFinite(at) || !it.k) return null;
  const url = str(it.url, 1500);
  return {
    k: str(it.k, 120),
    at,
    title: str(it.title, 120) || "AETHER alert",
    body: str(it.body, 300),
    url: /^(https:\/\/|\.\/|#)/.test(url) ? url : "./",
  };
}

const validSubscription = (s) =>
  s && typeof s.endpoint === "string" && /^https:\/\//.test(s.endpoint) && s.keys && s.keys.p256dh && s.keys.auth;

export default async (req) => {
  if (req.method === "OPTIONS") return json({});
  const route = new URL(req.url).pathname.replace(/^\/api\/?/, "");

  if (route === "health") return json({ ok: true, push: pushReady(), publicKey: VAPID_PUBLIC_KEY });
  if (req.method !== "POST") return json({ error: "Use POST" }, 405);

  let body;
  try { body = await req.json(); } catch { return json({ error: "Bad JSON" }, 400); }
  const id = str(body.deviceId, 64);
  if (!/^[a-z0-9]{16,40}$/.test(id)) return json({ error: "Bad device id" }, 400);
  const store = devices();

  if (route === "sync") {
    const prev = (await store.get(id, { type: "json" })) || {};
    const subscription = validSubscription(body.subscription) ? body.subscription : prev.subscription;
    if (!subscription) return json({ error: "No push subscription" }, 400);
    const now = Date.now();
    const items = (Array.isArray(body.items) ? body.items : [])
      .map(cleanItem).filter(Boolean)
      .filter((i) => i.at > now - 60 * 1000)
      .sort((a, b) => a.at - b.at)
      .slice(0, MAX_ITEMS);
    const keys = new Set(items.map((i) => i.k));
    const doc = { subscription, items, sent: (prev.sent || []).filter((k) => keys.has(k)), updated: now };
    await store.setJSON(id, doc);
    return json({ ok: true, scheduled: items.length });
  }

  if (route === "test") {
    const doc = await store.get(id, { type: "json" });
    if (!doc || !doc.subscription) return json({ error: "This device is not registered yet" }, 404);
    try {
      await sendPush(doc.subscription, {
        title: "AETHER test alert",
        body: "Background alerts are working. You will get alerts even when the app is closed.",
        url: "./#alerts",
        tag: "aether-test",
        force: true,
      });
      return json({ ok: true });
    } catch (e) {
      return json({ error: e.message || "Push failed" }, 502);
    }
  }

  if (route === "unsubscribe") {
    await store.delete(id);
    return json({ ok: true });
  }

  return json({ error: "Not found" }, 404);
};

export const config = { path: "/api/*" };

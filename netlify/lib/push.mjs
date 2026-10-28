// Shared server helpers: device storage and Web Push sending.
import { getStore } from "@netlify/blobs";
import webpush from "web-push";

export const devices = () => getStore({ name: "aether-devices", consistency: "strong" });
const config = () => getStore({ name: "aether-config", consistency: "strong" });

// How late an alert may still be delivered (for example after a short outage).
export const LATE_LIMIT_MS = 15 * 60 * 1000;

// Push keys: taken from environment variables if both are set, otherwise created once
// on first use and kept in the site's private Netlify Blobs storage. No manual setup needed.
let keys = null;
export async function vapidKeys() {
  if (keys) return keys;
  if (process.env.VAPID_PUBLIC_KEY && process.env.VAPID_PRIVATE_KEY) {
    keys = { publicKey: process.env.VAPID_PUBLIC_KEY, privateKey: process.env.VAPID_PRIVATE_KEY };
  } else {
    const store = config();
    let k = await store.get("vapid", { type: "json" });
    if (!k) {
      await store.setJSON("vapid", webpush.generateVAPIDKeys(), { onlyIfNew: true });
      k = await store.get("vapid", { type: "json" }); // re-read so parallel first calls agree
    }
    keys = k;
  }
  webpush.setVapidDetails("https://github.com/Muneeb1214-UNSM/aether-ai", keys.publicKey, keys.privateKey);
  return keys;
}

export async function sendPush(subscription, payload) {
  await vapidKeys();
  return webpush.sendNotification(subscription, JSON.stringify(payload), { TTL: 3600, urgency: "high" });
}

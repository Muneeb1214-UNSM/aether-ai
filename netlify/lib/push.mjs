// Shared server helpers: device storage and Web Push sending.
import { getStore } from "@netlify/blobs";
import webpush from "web-push";

// The public key is safe to publish; the private key lives only in Netlify's environment variables.
export const VAPID_PUBLIC_KEY =
  process.env.VAPID_PUBLIC_KEY || "BIQ1AxZs3Wa1q1dw-YcqQQ7LUqRemwlX2uZRzCAcATHVivUYjdt-Q9JnQYLhd7iW8Ub0SNjUwD4bvBAzJaIRQWk";

let configured = false;
export function pushReady() {
  if (configured) return true;
  const priv = process.env.VAPID_PRIVATE_KEY;
  if (!priv) return false;
  webpush.setVapidDetails("https://github.com/Muneeb1214-UNSM/aether-ai", VAPID_PUBLIC_KEY, priv);
  configured = true;
  return true;
}

export async function sendPush(subscription, payload) {
  if (!pushReady()) throw new Error("VAPID_PRIVATE_KEY is not set");
  return webpush.sendNotification(subscription, JSON.stringify(payload), { TTL: 3600, urgency: "high" });
}

export const devices = () => getStore({ name: "aether-devices", consistency: "strong" });

// How late an alert may still be delivered (for example after a short outage).
export const LATE_LIMIT_MS = 15 * 60 * 1000;

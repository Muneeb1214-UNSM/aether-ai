// Runs every minute on Netlify and sends every alert that is due, to every registered device.
import { devices, sendPush, vapidKeys, LATE_LIMIT_MS } from "../lib/push.mjs";

export default async () => {
  await vapidKeys();
  const store = devices();
  const now = Date.now();
  const { blobs } = await store.list();
  let sentCount = 0;

  for (const { key } of blobs) {
    const doc = await store.get(key, { type: "json" });
    if (!doc || !doc.subscription) continue;
    const sent = new Set(doc.sent || []);
    let changed = false;
    let gone = false;

    for (const it of doc.items || []) {
      if (sent.has(it.k) || it.at > now) continue;
      sent.add(it.k);
      changed = true;
      if (now - it.at > LATE_LIMIT_MS) continue; // too old to be useful
      try {
        await sendPush(doc.subscription, { title: it.title, body: it.body, url: it.url, tag: it.k });
        sentCount++;
      } catch (e) {
        // 404/410: the browser dropped this subscription (app removed or permission revoked).
        if (e.statusCode === 404 || e.statusCode === 410) { gone = true; break; }
        console.log("push failed", key, e.statusCode || e.message);
      }
    }

    if (gone) { await store.delete(key); continue; }
    if (changed) {
      doc.items = (doc.items || []).filter((i) => i.at > now - LATE_LIMIT_MS);
      const keep = new Set(doc.items.map((i) => i.k));
      doc.sent = [...sent].filter((k) => keep.has(k));
      await store.setJSON(key, doc);
    }
  }
  console.log(`checked ${blobs.length} devices, sent ${sentCount} alerts`);
};

export const config = { schedule: "* * * * *" };

# AETHER AI

Your personal assistant app. Plan your day, track your health and study, improve your communication, reach your career goals, build habits and plan social media posts. Talk to it, and learn from three AI mentors.

**Open the app:** https://muneeb1214-unsm.github.io/aether-ai/

## Install it as an app

AETHER is a Progressive Web App (PWA): it installs like a normal app, opens full screen with its own icon, works offline and updates itself.

- **Android (Chrome):** open the link, tap **Install app** on the Home screen (or menu ⋮ → *Install app*).
- **iPhone / iPad (Safari):** open the link, tap **Share** → **Add to Home Screen**.
- **Windows / Mac (Chrome or Edge):** open the link, click the install icon in the address bar, or *Install app* in your profile menu.

When a new version is published, the app shows **"New version ready → Update now"**. One tap and you have it.

## Features

| Tab | What it does |
| --- | --- |
| Home | Greeting, day progress ring, key numbers, today's plan with a "Now" line, weekly chart, habits, upcoming posts and goals |
| Schedule | Add tasks with date, time, type and repeat. Filter by Today, Tomorrow, Next 7 days or All. A pop-up and sound remind you on time |
| Alerts | Alarms and reminders: once, every day, weekdays, or every few minutes/hours. Quick buttons (in 5, 15, 30 min, 1 hour). When an alert rings you get a full-screen pop-up, a repeating sound, Aether reads the message out loud, and you can tap **Got it** or **Snooze 5 min**. Also has **What's happening** (a feed of every reminder, alert, briefing and check-in, with an unread badge on the bell), **smart notifications** (heads-up before tasks, "did you finish?" nudges, spoken morning briefing and evening review) and **message check-ins** for Instagram, Facebook, LinkedIn, X and WhatsApp with one-tap links to each inbox |
| Health | **Health Mentor** chatbot, tap glasses to log water, sleep slider, steps, exercise switch, mood, and 7-day charts with goal lines |
| Study | **Study Buddy** chatbot, 25-minute focus timer with a live ring, subjects with weekly goals, manual study log and a weekly chart |
| Communication | **Communication Coach** chatbot, a daily speaking challenge that measures your words per minute and filler words, word of the day, and your practice history |
| Career | Big goals broken into small steps, with progress rings |
| Habits | 7-day habit grid with streaks |
| Social | Follower tracking with growth charts for Instagram, Facebook, LinkedIn, X and WhatsApp, plus a post planner |
| WhatsApp | Save contacts, use ready-made templates, write messages with AI, schedule messages (once or every day). At the right time Aether reminds you and opens WhatsApp with the message ready |

## AI mentors

Each mentor is a chatbot you can type to or **talk** to (tap **Talk** for a hands-free spoken conversation, say "stop" to end).

- **Study Buddy** (Study tab): explains any topic simply, quizzes you one question at a time, makes study plans, helps with focus and memory. Knows your subjects and study time.
- **Health Mentor** (Health tab): sleep, water, food, exercise and stress, personalised with your last 7 days of health data. It is not a doctor: it never diagnoses or suggests medicines, and for emergencies it tells you to call your local emergency number (1122 in Pakistan).
- **Communication Coach** (Communication tab): job interview practice, English correction, polite emails and messages, confidence and small talk role-plays. Tap **Get coach feedback** after a speaking challenge.

Without an AI key the mentors give quick built-in tips. With smart AI on (see below) they can answer anything.

## Your profile

On first open, enter your name and email to get your dashboard. Tap your profile picture (top right) to edit your profile, open AI settings or sign out. Your details stay on your device.

## Smart AI

Aether understands simple commands on its own. To ask it anything (advice, questions, writing messages, personal tips), turn on smart AI:

1. Tap your profile picture → **AI settings**
2. Paste a Claude API key from [console.anthropic.com](https://console.anthropic.com)
3. Ask anything, e.g. "Plan my evening", "Write a birthday message for Sara", "Give me a study tip"

The key is saved only in your browser and requests go straight from your browser to Claude (`claude-opus-5-5`). Smart AI can also act for you: add tasks, log water and sleep, add habits, open tabs and prepare WhatsApp messages.

## WhatsApp automation: what it does

Aether prepares messages and reminds you on time. When you tap **Send now**, WhatsApp opens with the message already typed, and you tap Send. Sending with no tap at all needs the WhatsApp Business API and a server, which is on the roadmap.

## Voice assistant

Tap the round button at the bottom right and just talk; it starts listening right away. You can also type. Aether answers out loud.

Speak naturally: "Hey Aether, can you add a task to go to the gym at 6 pm", "I need to call mom tomorrow", "dentist appointment tomorrow morning", "I'm going to study physics at 8 tonight", "kal 5 baje doctor ke paas jana hai". If Aether is not sure, it asks "Should I add this to your tasks?" with Yes / Set an alert / No buttons. After adding a task without a time, just say "at 5 pm" to set it.

**Voice settings** (gear icon in the assistant): pick a voice, change the speed, and choose your accent (Pakistani / Indian, American or British English) so Aether understands you better.

**Conversation mode:** tap **Start conversation** and just talk. Aether listens, answers out loud, and listens again, so you can talk back and forth without tapping. Say "stop" or "bye" to end it.

Try saying:

- "Remind me in 10 minutes to call mom"
- "Set an alarm for 6 am" / "Wake me up tomorrow at 7:30 am"
- "Remind me every hour to drink water"
- "What alerts do I have?" / "Cancel alert drink water"
- "What's happening?" / "What did I miss?" / "Brief me"

- "Add task gym at 6 pm"
- "Add task call mom at 8:30 pm tomorrow"
- "What is my plan today?" / "What is next?"
- "I drank water" / "I drank 2 glasses of water"
- "I slept 7 hours" / "I walked 5000 steps"
- "Mark gym done"
- "Start focus timer"
- "I feel good"
- "Open health"
- "Message Ali saying I am on my way" (for a saved WhatsApp contact)
- "How am I doing?"
- "Help"

Speaking works in Chrome and Edge. Every browser can use the type box.

## More

- Light and dark mode (sun/moon button)
- Works on phone, tablet and computer
- Tap "Show sample data" on first open to see it filled in

## How it works

- Your data is saved in your browser (`localStorage`) on the device you use.
- Reminders work while the app is open. Allow notifications in Alerts → Smart notifications.

## Project structure

| Path | What it is |
| --- | --- |
| `src/app.html` | The app's source: all screens, styles and logic. **Edit this file.** |
| `build.sh` | Builds `index.html` from `src/app.html` and stamps the version into `sw.js` |
| `index.html` | The built app (generated, don't edit by hand) |
| `manifest.webmanifest` | App name, icons, colours and shortcuts for installing |
| `sw.js` | Service worker: offline support and update detection |
| `icons/` | App icons (192, 512, maskable, Apple touch, favicon) |
| `netlify.toml` | Netlify hosting settings (publish folder, functions, cache and security headers) |
| `netlify/functions/api.mjs` | Server API: saves each device's upcoming alerts, sends a test notification |
| `netlify/functions/alert-cron.mjs` | Runs every minute and sends due alerts as push notifications |
| `netlify/lib/push.mjs` | Shared server code: storage and Web Push |

## How to release an update

1. Edit `src/app.html` and raise `APP_VERSION` (for example `2.0.0` → `2.1.0`).
2. Run `./build.sh`.
3. Commit and push. GitHub Pages publishes it in about a minute, and installed apps show **Update now**.

## Run locally

```bash
python -m http.server 8765
```

Then open http://localhost:8765 in Chrome or Edge.

## Host on Netlify

[![Deploy to Netlify](https://www.netlify.com/img/deploy/button.svg)](https://app.netlify.com/start/deploy?repository=https://github.com/Muneeb1214-UNSM/aether-ai)

The repo is ready for Netlify: `netlify.toml` publishes the folder as-is (no build step) and sets cache and security headers.

1. Sign up at [app.netlify.com](https://app.netlify.com) with your GitHub account.
2. **Add new site → Import an existing project → GitHub → aether-ai**. Netlify reads `netlify.toml`, so leave the build settings as they are and click **Deploy**.
3. **Site configuration → Change site name** to get a nicer link, for example `aether-ai.netlify.app`.
4. Optional: **Domain management → Add a domain** to use your own domain. Netlify adds free HTTPS.

Every push to `main` redeploys the site automatically. The in-app **Share** button always shares the link of the site it is opened on.

## Background alerts (even when the app is closed)

On the Netlify version, AETHER has a small server that sends alerts to your phone's lock screen even when the app is closed: alerts, task reminders (and the heads-up before them), morning and evening briefings, social check-ins, post reminders, and scheduled WhatsApp messages (tap the notification and WhatsApp opens with the message ready).

How it works: the app sends your upcoming 7 days of alerts to the server; `netlify/functions/alert-cron.mjs` runs every minute and sends whatever is due as a Web Push notification. Only alert times and texts are stored, with a random device id, never your name or email.

**One-time setup on Netlify**

1. Deploy the site to Netlify (see above). That's all the server needs: it creates its own push keys on first use and keeps them in the site's private Netlify Blobs storage.
2. Open the Netlify link on your phone, install the app, then go to **Alerts → Smart notifications → Alerts when the app is closed → Turn on**, and tap **Send test**.
3. Using the GitHub Pages link or an app installed from it? Paste your Netlify link into the **AETHER server link** box under that setting and tap **Connect**.

Optional: to use your own keys, set both `VAPID_PUBLIC_KEY` and `VAPID_PRIVATE_KEY` as Netlify environment variables.

Notes:
- Android (Chrome, Edge, Samsung Internet) works directly. On iPhone (iOS 16.4+), add AETHER to the Home Screen first and turn it on from the installed app.
- A closed app can't speak, so background alerts use your phone's notification sound and vibration. When the app is open, Aether still reads alerts out loud.
- Each background alert rings (sound + vibration) for about 10 seconds and has **Snooze 5 min** and **Got it** buttons. If it arrives silently, turn on Sound for AETHER (or Chrome) in your phone's notification settings.
- Alerts can arrive up to about a minute late, because the server checks once a minute.
- Open the app at least once a week so the next 7 days of alerts stay up to date.

## Share the app

Tap **Share app** on Home (or *Share with friends* in the profile menu). On phones it opens the phone's share sheet; on computers it shows the link with a Copy button, WhatsApp, Facebook, X, LinkedIn and Telegram buttons, and a QR code.

## Put it on the Google Play Store (optional)

Go to [pwabuilder.com](https://www.pwabuilder.com), enter the app link, and download the Android package. It wraps this same app, so every update you push to GitHub reaches the Play Store app too.

## Roadmap

- [ ] Live alerts for new social media messages and notifications (needs the AETHER server and each platform's API)
- [ ] Fully automatic WhatsApp sending through the WhatsApp Business API (needs a small server)
- [ ] Reminders on your phone when the page is closed
- [ ] Auto-posting and real analytics through social media APIs
- [ ] Real accounts and cloud sync across devices

## License

MIT

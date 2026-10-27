# AETHER AI

Your personal assistant dashboard. Plan your day, track your health and study, reach your career goals, build habits and plan social media posts. And you can talk to it.

**Live demo:** https://muneeb1214-unsm.github.io/aether-ai/

## Features

| Tab | What it does |
| --- | --- |
| Home | Greeting, day progress ring, key numbers, today's plan with a "Now" line, weekly chart, habits, upcoming posts and goals |
| Schedule | Add tasks with date, time, type and repeat. Filter by Today, Tomorrow, Next 7 days or All. A pop-up and sound remind you on time |
| Alerts | Alarms and reminders: once, every day, weekdays, or every few minutes/hours. Quick buttons (in 5, 15, 30 min, 1 hour). When an alert rings you get a full-screen pop-up, a repeating sound, Aether reads the message out loud, and you can tap **Got it** or **Snooze 5 min**. Also has **What's happening** (a feed of every reminder, alert, briefing and check-in, with an unread badge on the bell), **smart notifications** (heads-up before tasks, "did you finish?" nudges, spoken morning briefing and evening review) and **message check-ins** for Instagram, Facebook, LinkedIn, X and WhatsApp with one-tap links to each inbox |
| Health | Tap glasses to log water, sleep slider, steps, exercise switch, mood, and 7-day charts with goal lines |
| Study | 25-minute focus timer with a live ring, subjects with weekly goals, manual study log and a weekly chart |
| Career | Big goals broken into small steps, with progress rings |
| Habits | 7-day habit grid with streaks |
| Social | Follower tracking with growth charts for Instagram, Facebook, LinkedIn, X and WhatsApp, plus a post planner |
| WhatsApp | Save contacts, use ready-made templates, write messages with AI, schedule messages (once or every day). At the right time Aether reminds you and opens WhatsApp with the message ready |

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

Tap the round button at the bottom right and speak, or type. Aether answers out loud.

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

- One `index.html` file. No build step and no installs.
- Your data is saved in your browser (`localStorage`) on the device you use.
- Reminders work while the page is open. Tap the bell to allow browser notifications.

## Run locally

Download the repo and open `index.html` in Chrome or Edge.

## Roadmap

- [ ] Live alerts for new social media messages and notifications (needs the AETHER server and each platform's API)
- [ ] Fully automatic WhatsApp sending through the WhatsApp Business API (needs a small server)
- [ ] Reminders on your phone when the page is closed
- [ ] Auto-posting and real analytics through social media APIs
- [ ] Real accounts and cloud sync across devices

## License

MIT

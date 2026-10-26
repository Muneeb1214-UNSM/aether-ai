# AETHER AI

A personal assistant dashboard that keeps your whole day in one place: schedule and reminders, health, study, career goals, habits and social media planning.

**Live demo:** https://muneeb1214-unsm.github.io/aether-ai/

## Features

| Section | What it does |
| --- | --- |
| Overview | Daily briefing with a greeting, today's task count, your next task and four key numbers (tasks, water, study minutes, habits) |
| Schedule | Add tasks with a date, time and category (Study, Health, Work, Personal, Social). Repeat a task daily for 7 or 30 days. An alert and a sound play when a task is due |
| Health | Water glasses, sleep hours, steps, workout and mood, with 7-day water and sleep charts |
| Study | Subjects with weekly hour goals, progress bars, a 25-minute Pomodoro timer that logs sessions, and a 7-day chart |
| Career | Professional goals broken into milestones, with % progress |
| Habits | Daily habits with a 7-day grid and streak count |
| Social Media | Follower tracking with growth sparklines for Instagram, Facebook, LinkedIn, X and WhatsApp, plus a post planner (draft → scheduled → posted) with copy-caption |

Click **"Example data dekhein"** on first open to see the dashboard filled with sample data, then **"Example hatayein"** to start with your own.

## How it works

- A single `index.html` file with no build step and no dependencies (only Google Fonts).
- Data is saved in your browser's `localStorage`, so it stays on the device you use.
- Reminders fire while the page is open. Browser notifications work if you allow them.

## Run locally

Download the repo and open `index.html` in any browser.

## Roadmap

- [ ] Telegram / WhatsApp bot for reminders when the page is closed
- [ ] Auto-posting and real analytics through the Instagram, Facebook, LinkedIn and X APIs
- [ ] Cloud sync across devices
- [ ] AI daily briefing and voice commands

## License

MIT

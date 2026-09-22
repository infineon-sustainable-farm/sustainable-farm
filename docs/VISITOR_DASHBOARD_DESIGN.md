# Visitor Management Dashboard — Exact UI Reference

## Goal
Reproduce the dashboard shown in the reference image as closely as possible.
Do not redesign the layout or introduce additional colors/components unless explicitly requested.

## Overall visual style
- Application: Visitor Management
- Style: modern, minimal, clean enterprise dashboard
- Primary color: dark teal/green
- Secondary accent: very light mint/green
- Background: very light gray/blue
- Cards: white, rounded corners, subtle border/shadow
- Typography: clean modern sans-serif, bold dark navy/teal headings
- Avoid bright colors and gradients.
- Keep generous whitespace.

## Left sidebar
Fixed vertical sidebar on the left.
- Width: approximately 130px in the reference proportions.
- Background: dark teal/green.
- Circular white logo at the top.
- Text under logo: `VISITOR MANAGEMENT`
- Navigation items, in this exact order:
  1. Dashboard
  2. Scheduling
  3. Registration
  4. Visitors
  5. Education
  6. Safety
  7. Booking
  8. Feedback
  9. Events
  10. Staff
- Dashboard is the active item with a slightly lighter teal rounded background.
- Logout button at the bottom with the same subtle active-style background.

## Main header
At the top of the content area:
- Title: `Dashboard Overview`
- Subtitle: `Welcome back! Here’s what’s happening at your visitor center this week.`
- On the right: compact week selector card.
- Week selector:
  - Calendar icon
  - `Week 39`
  - `Sep 23 – Sep 29, 2025`
  - small dropdown chevron

## HERO CARD — IMPORTANT
Use the supplied asset:
`assets/visitor-dashboard-hero-card.png`

The hero card is the large card directly below the header.

Approximate structure:
- Full-width card inside the main content area.
- Height around 130–140px.
- Very light mint/green background.
- Rounded corners around 10–12px.
- No strong border.
- Very subtle shadow.
- Left side contains:
  - small uppercase label: `WELCOME BACK!`
  - main heading:
    `Manage visitors.`
    `Create great experiences.`
  - supporting text:
    `Track visits, schedule activities and ensure a safe
     and welcoming environment for everyone.`
  - rounded teal button:
    `View the guide  →`
- Right side contains a soft illustrated visitor-center scene:
  - visitor-center building
  - two visitors
  - staff member
  - trees/plants
- Illustration must remain soft and subtle and should not dominate the text.
- Keep the hero card's proportions and spacing close to the supplied asset.

## KPI cards
Immediately below the hero card: 5 equal cards in one horizontal row.

Cards:
1. Visitors this week
   - value: `12`
   - trend: `↑ +20%`
   - caption: `vs last week`
   - visitor/users icon

2. Slots booked
   - value: `4`
   - trend: `↑ +33%`
   - caption: `vs last week`
   - calendar icon

3. Safety briefings pending
   - value: `0`
   - status: `No change`
   - caption: `vs last week`
   - shield icon

4. Avg. satisfaction
   - value: `4.0/5`
   - trend: `↑ +0.2`
   - caption: `vs last week`
   - star icon

5. Upcoming events
   - value: `1`
   - trend: `↑ +1`
   - caption: `vs last week`
   - event/announcement icon

KPI card styling:
- White background.
- Rounded corners.
- Very subtle gray border/shadow.
- Small icon inside a pale mint rounded square.
- Label in muted gray.
- Main number large and dark.
- Trend in teal/green.
- Keep cards compact and equal height.

## Bottom section
Two cards side-by-side.

### Upcoming events
Large card on the left.
Header:
- `Upcoming events`
- `View all →` aligned right

Event row:
- Calendar/date icon.
- Date: `10 Oct`
- Event name: `Journée portes ouvertes d'automne`
- Time: `10 Oct 2026, 09:00`
- Right side:
  - `6/30`
  - `BOOKED`

### Upcoming tasks
Card on the right.
Header:
- clipboard/task icon
- `Upcoming tasks`

Empty state:
- clipboard illustration/icon in a pale mint circle
- `No pending tasks.`
- `You’re all caught up!`

## Layout hierarchy
1. Sidebar
2. Dashboard header
3. Hero introduction card
4. Five KPI cards
5. Upcoming events + Upcoming tasks

## Critical implementation instruction
The supplied hero image is a visual reference/asset, not a replacement for the rest of the UI.
Use the asset for the hero card if the project architecture allows it, while keeping the surrounding dashboard elements as real HTML/CSS/components.

Do not:
- add charts
- add extra dashboard sections
- change the navigation order
- add strong colors
- replace the hero with a different design
- significantly change card proportions
- invent additional metrics

The objective is visual fidelity to the provided reference.

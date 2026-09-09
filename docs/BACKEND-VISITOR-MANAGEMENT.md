# Visitor Management Backend — Technical Documentation

> Spring Boot 4.1.0 · Java 21 · PostgreSQL 17 · Hibernate 7.4

---

## 1. Architecture Overview

```
┌──────────────────────────────────────────────────────────────┐
│                        REST Controllers                       │
│  TimeSlotController · VisitorController                      │
│  RegistrationController · BriefingController                 │
│  EventController · FeedbackController                        │
│  EducationalProgramController                                │
├──────────────────────────────────────────────────────────────┤
│                         Services                              │
│  SchedulingServiceImpl · RegistrationServiceImpl             │
│  EventServiceImpl · FeedbackServiceImpl                     │
│  EducationalProgramServiceImpl                               │
│  SchedulingRules (constants)                                 │
├──────────────────────────────────────────────────────────────┤
│                     Repositories (JPA)                        │
│  TimeSlotRepository · VisitorRepository                      │
│  RegistrationRepository · BriefingRepository                 │
│  EventRepository · FeedbackRepository                        │
│  SurveySendRepository · TourStopRepository                   │
│  WorkshopRepository                                          │
├──────────────────────────────────────────────────────────────┤
│                      JPA Entities                             │
│  TimeSlot · Visitor · Registration · Briefing · Event        │
│  Feedback · SurveySend · TourStop · Workshop                │
│  (extend BaseEntity: id + createdAt + updatedAt)             │
├──────────────────────────────────────────────────────────────┤
│          core/ — shared infrastructure                        │
│  BaseEntity · GlobalExceptionHandler · ApiError              │
│  ResourceNotFoundException · ConflictException               │
│  BusinessRuleException                                       │
└──────────────────────────────────────────────────────────────┘
```

**Packages:**
- `core/` — shared infra only (BaseEntity, exceptions, handler)
- `modules/visitormanagement/` — Scheduling + Registration sub-modules
- Enums live in the module package (not `core/`)

---

## 2. Database Schema

### Entity-Relationship Diagram

```
┌─────────────────────┐         ┌─────────────────────────┐
│     time_slot        │         │        visitor           │
├─────────────────────┤         ├─────────────────────────┤
│ id            BIGINT│ PK      │ id            BIGINT│ PK │
│ slot_date     DATE  │ NOT NULL│ full_name  VARCHAR(150)  │ NOT NULL
│ start_time    TIME  │ NOT NULL│ group_size     INT       │ NOT NULL, DEFAULT 1
│ end_time      TIME  │ NOT NULL│ email     VARCHAR(200)   │ UNIQUE
│ max_capacity  INT   │ NOT NULL│ phone     VARCHAR(30)    │
│ status   VARCHAR(20)│ NOT NULL│ language  VARCHAR(40)    │
│ guide_id    BIGINT  │         │ visitor_type VARCHAR(20) │ NOT NULL
│ created_at INSTANT │ NOT NULL│ special_needs VARCHAR(500)│
│ updated_at INSTANT │ NOT NULL│ created_at INSTANT│ NOT NULL
└─────────┬───────────┘         │ updated_at INSTANT│ NOT NULL
          │                     └──────────┬──────────────┘
          │ 1                            1 │
          │                                │
          │ *                        1 │ *
┌─────────┴───────────────────────────────┴──────────────┐
│                    registration                          │
├─────────────────────────────────────────────────────────┤
│ id            BIGINT│ PK                                 │
│ visitor_id    BIGINT│ FK → visitor (NOT NULL)            │
│ time_slot_id  BIGINT│ FK → time_slot (nullable, NULL for    │
│                   │    event-only registrations)           │
│ event_id      BIGINT│                                    │
│ visit_purpose VARCHAR(20)│ NOT NULL                      │
│ is_prospect   BOOLEAN│ NOT NULL, DEFAULT false           │
│ status   VARCHAR(20)│ NOT NULL, DEFAULT 'PENDING'       │
│ created_at    INSTANT│ NOT NULL                          │
│ updated_at    INSTANT│ NOT NULL                          │
│ UNIQUE (visitor_id, time_slot_id)                        │
└─────────────────────┬───────────────────────────────────┘
                      │ 1
                      │
                      │ 1
              ┌───────┴──────────┐
              │    briefing       │
              ├──────────────────┤
              │ id          BIGINT│ PK                    │
              │ registration_id   │ FK → registration    │
              │                   │    (NOT NULL, UNIQUE) │
              │ delivered_at INSTANT│                     │
              │ staff_member VARCHAR(150)                  │
              │ signature   VARCHAR(500)                   │
              │ status  VARCHAR(20)│ NOT NULL, 'PENDING'  │
              │ created_at  INSTANT│ NOT NULL              │
│ updated_at  INSTANT│ NOT NULL              │
               └──────────────────┘

      registration.event_id → event.id (no FK constraint, soft link)

       ┌───────────────────────────────────┐
       │                event                │
       ├───────────────────────────────────┤
       │ id            BIGINT│ PK             │
       │ title     VARCHAR(150)│ NOT NULL   │
       │ event_type VARCHAR(30)│ NOT NULL   │
       │ start_date_time TIMESTAMP│ NOT NULL│
       │ end_date_time   TIMESTAMP│ NOT NULL│
       │ max_capacity  INT│ NOT NULL, DEFAULT 10│
       │ status   VARCHAR(20)│ NOT NULL, 'DRAFT'│
       │ description VARCHAR(1000)│          │
       │ location   VARCHAR(150)│            │
       │ created_at   INSTANT│ NOT NULL       │
       │ updated_at   INSTANT│ NOT NULL       │
       └───────────────────────────────────┘

       ┌───────────────────────────────────┐
       │             feedback               │
       ├───────────────────────────────────┤
       │ id            BIGINT│ PK             │
       │ visitor_id    BIGINT│ FK → visitor (NOT NULL)│
       │ survey_send_id BIGINT│ FK → survey_send (nullable)│
       │ origin   VARCHAR(20)│ NOT NULL     │
       │ rating       INT│ NOT NULL (1..5)    │
       │ briefing_clear VARCHAR(1000)│       │
       │ educational_value VARCHAR(1000)│    │
       │ recommend    VARCHAR(1000)│         │
       │ comment     VARCHAR(1000)│          │
       │ routed_to   VARCHAR(60)│            │
       │ submitted_at INSTANT│ NOT NULL       │
       │ created_at   INSTANT│ NOT NULL       │
       │ updated_at   INSTANT│ NOT NULL       │
       └───────────────────────────────────┘

       ┌───────────────────────────────────┐
       │            survey_send              │
       ├───────────────────────────────────┤
       │ id            BIGINT│ PK             │
       │ visitor_id    BIGINT│ FK → visitor (NOT NULL)│
       │ channel   VARCHAR(20)│ NOT NULL    │
       │ message_template VARCHAR(500)│      │
       │ sent_at    INSTANT│ NOT NULL         │
       │ status   VARCHAR(20)│ NOT NULL, 'SENT'│
       │ created_at   INSTANT│ NOT NULL       │
       │ updated_at   INSTANT│ NOT NULL       │
       └───────────────────────────────────┘

       ┌───────────────────────────────────┐
       │             tour_stop              │
       ├───────────────────────────────────┤
       │ id            BIGINT│ PK             │
       │ name     VARCHAR(150)│ NOT NULL    │
       │ position      INT│ NOT NULL         │
       │ description VARCHAR(2000)│          │
       │ duration_minutes INT│ NOT NULL      │
       │ max_capacity INT│                   │
       │ location   VARCHAR(150)│            │
       │ demo      VARCHAR(1000)│            │
       │ safety_notes VARCHAR(1000)│         │
       │ active      BOOLEAN│ NOT NULL, true │
       │ created_at   INSTANT│ NOT NULL       │
       │ updated_at   INSTANT│ NOT NULL       │
       └───────────────────────────────────┘

       ┌───────────────────────────────────┐
       │             workshop               │
       ├───────────────────────────────────┤
       │ id            BIGINT│ PK             │
       │ name     VARCHAR(150)│ NOT NULL    │
       │ duration_minutes INT│ NOT NULL      │
       │ target_group VARCHAR(60)│ NOT NULL │
       │ facilitator VARCHAR(100)│           │
       │ description VARCHAR(1000)│          │
       │ status   VARCHAR(20)│ NOT NULL,'DRAFT'│
       │ created_at   INSTANT│ NOT NULL       │
       │ updated_at   INSTANT│ NOT NULL       │
       └───────────────────────────────────┘
```

### Table Summary

| Table | Rows (design) | Purpose |
|-------|--------------|---------|
| `time_slot` | ~500/year | Farm tour calendar slots (2/day max) |
| `visitor` | ~2,000 | Visitor database (individuals/groups) |
| `registration` | ~4,000 | Links visitor → time slot **or** event |
| `briefing` | ~3,000 | Safety briefing per confirmed registration |
| `event` | ~50/year | Open days, buyer visits, school/community days |
| `feedback` | ~3,000 | Satisfaction responses (tablet + survey links) |
| `survey_send` | ~8,000 | Post-visit surveys sent by email/SMS/WhatsApp |
| `tour_stop` | ~8 | Standard guided-tour stops (seeded via API) |
| `workshop` | ~10 | Tour templates / workshops (Active / Draft / Inactive) |

### Enums

| Enum | Values | Table Column |
|------|--------|--------------|
| `TimeSlotStatus` | `AVAILABLE`, `RESERVED`, `FULL`, `CANCELLED`, `COMPLETED` | `time_slot.status` |
| `RegistrationStatus` | `PENDING`, `CONFIRMED`, `REJECTED`, `CHECKED_IN`, `CANCELLED` | `registration.status` |
| `BriefingStatus` | `PENDING`, `DONE` | `briefing.status` |
| `VisitorType` | `INDIVIDUAL`, `GROUP`, `SCHOOL`, `PARTNER` | `visitor.visitor_type` |
| `VisitPurpose` | `TOURISM`, `PURCHASE`, `PARTNERSHIP`, `INVESTMENT`, `EDUCATION`, `OTHER` | `registration.visit_purpose` |
| `EventType` | `OPEN_DAY`, `PARTNER_BUYER`, `SCHOOL`, `COMMUNITY`, `OTHER` | `event.event_type` |
| `EventStatus` | `DRAFT`, `PUBLISHED`, `CANCELLED`, `COMPLETED` | `event.status` |
| `FeedbackChannel` | `ON_SITE`, `EMAIL`, `SMS`, `WHATSAPP` | `feedback.origin`, `survey_send.channel` |
| `SurveyStatus` | `SENT`, `RECEIVED` | `survey_send.status` |
| `WorkshopStatus` | `DRAFT`, `ACTIVE`, `INACTIVE` | `workshop.status` |

---

## 3. REST API Endpoints

Base URL: `http://localhost:8080/api/v1`

### 3.1 Time Slots — `/api/v1/time-slots`

| Method | Path | Description | Status Codes |
|--------|------|-------------|-------------|
| `GET` | `/time-slots` | List all slots (optional `?date=YYYY-MM-DD`) | 200 |
| `GET` | `/time-slots/{id}` | Get slot by ID | 200, 404 |
| `POST` | `/time-slots` | Create a new slot | 201, 400, 409, 422 |
| `PUT` | `/time-slots/{id}` | Update slot details | 200, 404, 422 |
| `DELETE` | `/time-slots/{id}` | Cancel a slot (soft-delete) | 204, 404, 422 |
| `POST` | `/time-slots/{id}/assign-guide` | Assign a guide (`?guideId=N`) | 200, 404, 422 |
| `GET` | `/time-slots/availability?date=...` | Availability view for a date | 200 |

**Create/Update Request Body:**
```json
{
  "date": "2026-09-08",
  "startTime": "09:00",
  "endTime": "11:00",
  "maxCapacity": 10,
  "guideId": 5
}
```

**TimeSlotResponse:**
```json
{
  "id": 1,
  "date": "2026-09-08",
  "startTime": "09:00",
  "endTime": "11:00",
  "maxCapacity": 10,
  "booked": 3,
  "status": "RESERVED",
  "guideId": 5,
  "createdAt": "2026-09-01T10:00:00Z",
  "updatedAt": "2026-09-01T10:00:00Z"
}
```

**AvailabilityResponse:**
```json
{
  "id": 1,
  "date": "2026-09-08",
  "startTime": "09:00",
  "endTime": "11:00",
  "maxCapacity": 10,
  "remaining": 7,
  "status": "RESERVED",
  "guideId": 5
}
```

### 3.2 Visitors — `/api/v1/visitors`

| Method | Path | Description | Status Codes |
|--------|------|-------------|-------------|
| `GET` | `/visitors` | List all visitors | 200 |
| `GET` | `/visitors/{id}` | Get visitor by ID | 200, 404 |
| `POST` | `/visitors` | Create visitor | 201, 400, 409 |
| `PUT` | `/visitors/{id}` | Update visitor | 200, 404, 409 |

**VisitorRequest:**
```json
{
  "fullName": "Alice Dupont",
  "groupSize": 2,
  "email": "alice@example.com",
  "phone": "+241 06 12 34 56",
  "language": "French",
  "type": "INDIVIDUAL",
  "specialNeeds": "Wheelchair access required"
}
```

### 3.3 Registrations — `/api/v1/registrations`

| Method | Path | Description | Status Codes |
|--------|------|-------------|-------------|
| `GET` | `/registrations` | List (filter by `?timeSlotId=`, `?date=`, or `?prospect=true`) | 200 |
| `GET` | `/registrations/{id}` | Get registration by ID | 200, 404 |
| `POST` | `/registrations` | Create registration (visitor→slot, requires `visitPurpose`) | 201, 400, 409, 422 |
| `PATCH` | `/registrations/{id}/approve` | Approve → triggers briefing creation | 200, 404, 422 |
| `PATCH` | `/registrations/{id}/reject` | Reject a pending registration | 200, 404, 422 |
| `PATCH` | `/registrations/{id}/check-in` | Check in a confirmed visitor | 200, 404, 422 |
| `PATCH` | `/registrations/{id}/cancel` | Cancel a registration | 200, 404, 422 |

**RegistrationRequest:**
```json
{
  "visitorId": 1,
  "timeSlotId": 10,
  "visitPurpose": "PURCHASE",
  "eventId": null
}
```

**RegistrationResponse (new fields):**
```json
{
  "id": 1,
  "visitorId": 1,
  "visitorName": "Alice Dupont",
  "groupSize": 2,
  "timeSlotId": 10,
  "slotDate": "2026-09-08",
  "slotStart": "09:00",
  "eventId": null,
  "visitPurpose": "PURCHASE",
  "prospect": true,
  "status": "PENDING",
  "briefingId": null,
  "createdAt": "2026-09-01T10:00:00Z",
  "updatedAt": "2026-09-01T10:00:00Z"
}
```

### 3.4 Briefings — `/api/v1`

| Method | Path | Description | Status Codes |
|--------|------|-------------|-------------|
| `GET` | `/registrations/{registrationId}/briefing` | Get briefing for registration | 200, 404 |
| `PATCH` | `/registrations/{registrationId}/briefing/deliver` | Mark briefing delivered | 200, 422 |

**BriefingDeliverRequest:**
```json
{
  "staffMember": "Guard Camille",
  "signature": "digital-signature-or-name"
}
```

### 3.5 Events — `/api/v1/events`

| Method | Path | Description | Status Codes |
|--------|------|-------------|-------------|
| `GET` | `/events` | List events (`?type=`, `?date=YYYY-MM-DD`) | 200 |
| `GET` | `/events/{id}` | Get event by ID | 200, 404 |
| `POST` | `/events` | Create event (default status `DRAFT`) | 201, 400, 422 |
| `PUT` | `/events/{id}` | Update event (not allowed when CANCELLED/COMPLETED) | 200, 404, 422 |
| `DELETE` | `/events/{id}` | Cancel event (soft-delete) | 204, 404, 422 |
| `POST` | `/events/{id}/publish` | Publish a DRAFT event | 200, 404, 422 |
| `GET` | `/events/{id}/registrations` | List registrations for the event | 200, 404 |
| `POST` | `/events/{id}/register` | Register a visitor for the event | 201, 400, 404, 409, 422 |

**EventRequest:**
```json
{
  "title": "Open Farm Day",
  "type": "OPEN_DAY",
  "startDateTime": "2026-10-04T10:00:00",
  "endDateTime": "2026-10-04T16:00:00",
  "maxCapacity": 60,
  "description": "Discover the farm, meet the team, taste our products",
  "location": "Farm entrance hall"
}
```

**EventResponse:**
```json
{
  "id": 1,
  "title": "Open Farm Day",
  "type": "OPEN_DAY",
  "startDateTime": "2026-10-04T10:00:00",
  "endDateTime": "2026-10-04T16:00:00",
  "maxCapacity": 60,
  "booked": 3,
  "status": "PUBLISHED",
  "description": "Discover the farm, meet the team, taste our products",
  "location": "Farm entrance hall",
  "createdAt": "2026-09-01T10:00:00Z",
  "updatedAt": "2026-09-01T10:00:00Z"
}
```

**EventRegistrationRequest:**
```json
{
  "visitorId": 1,
  "visitPurpose": "PURCHASE"
}
```

The registration created for an event has `timeSlotId: null` and carries the visitor's `visitPurpose`. Commercial purposes (`PURCHASE`, `PARTNERSHIP`, `INVESTMENT`) automatically mark the visitor as a prospect, exactly like time-slot registrations.

### 3.6 Feedback & Surveys — `/api/v1`

| Method | Path | Description | Status Codes |
|--------|------|-------------|-------------|
| `GET` | `/feedback` | List responses (`?visitorId=`, `?from=`, `?to=`) | 200 |
| `GET` | `/feedback/summary` | Compiled stats (`?from=`, `?to=`) | 200 |
| `POST` | `/feedback` | Submit response (on-site or linked to a sent survey) | 201, 400, 404, 422 |
| `PATCH` | `/feedback/{id}/route` | Route a response to a module owner | 200, 404, 400 |
| `GET` | `/surveys` | List sent surveys (`?visitorId=`, `?status=`) | 200 |
| `POST` | `/surveys` | Send a post-visit survey (email/SMS/WhatsApp) | 201, 400, 404, 422 |

**FeedbackRequest:** `visitorId` (required), `surveyId` (optional — links the response to a sent survey and marks it RECEIVED), `rating` (1–5, required), `briefingClear`, `educationalValue`, `recommend`, `comment`.
```json
{
  "visitorId": 1,
  "surveyId": 12,
  "rating": 4,
  "briefingClear": "Yes, very clear",
  "educationalValue": "The solar tracking was interesting",
  "recommend": "Definitely",
  "comment": "Would love a German-language brochure"
}
```

**FeedbackResponse:**
```json
{
  "id": 1,
  "visitorId": 1,
  "visitorName": "Alice Dupont",
  "surveyId": 12,
  "origin": "EMAIL",
  "rating": 4,
  "briefingClear": "Yes, very clear",
  "educationalValue": "The solar tracking was interesting",
  "recommend": "Definitely",
  "comment": "Would love a German-language brochure",
  "routedTo": "Sales (Mariata)",
  "submittedAt": "2026-08-26T13:00:00Z",
  "createdAt": "2026-08-26T13:00:00Z",
  "updatedAt": "2026-08-26T13:00:00Z"
}
```

**FeedbackSummaryResponse** (compiled weekly report):
```json
{
  "averageRating": 4.2,
  "total": 18,
  "recommendPct": 92.0,
  "distribution": { "1": 0, "2": 0, "3": 3, "4": 6, "5": 9 }
}
```

**SurveyRequest:**
```json
{
  "visitorId": 3,
  "channel": "EMAIL",
  "messageTemplate": "Thank you for visiting Sustainable Farm! Share your feedback: [link]"
}
```

**SurveyResponse:** id, visitorId, visitorName, channel, messageTemplate, sentAt, status (`SENT`/`RECEIVED`), `rating` (once the visitor responded), createdAt, updatedAt.

On-site tablet submissions use `POST /feedback` without `surveyId` → `origin: "ON_SITE"`. A sent survey becomes `RECEIVED` automatically when a response is linked to it.

### 3.7 Educational Program — `/api/v1`

| Method | Path | Description | Status Codes |
|--------|------|-------------|-------------|
| `GET` | `/tour-stops` | List guided-tour stops (ordered by position) | 200 |
| `POST` | `/tour-stops` | Create a tour stop | 201, 400, 409 |
| `PUT` | `/tour-stops/{id}` | Update a stop (position must stay unique among active stops) | 200, 404, 409, 400 |
| `DELETE` | `/tour-stops/{id}` | Deactivate a stop (soft-delete) | 204, 404 |
| `GET` | `/workshops` | List workshops/templates (`?status=DRAFT|ACTIVE|INACTIVE`) | 200 |
| `POST` | `/workshops` | Create a workshop (status `DRAFT`) | 201, 400 |
| `PUT` | `/workshops/{id}` | Update a workshop | 200, 404, 400 |
| `POST` | `/workshops/{id}/publish` | Publish a DRAFT workshop → ACTIVE | 200, 404, 422 |
| `POST` | `/workshops/{id}/deactivate` | Deactivate an ACTIVE workshop → INACTIVE | 200, 404, 422 |

**TourStopRequest / TourStopResponse:** `name` (required), `position` (required, ≥1, unique among active stops), `description`, `durationMinutes` (required, ≥1), `maxCapacity` (optional, ≥1), `location`, `demo`, `safetyNotes`, `active`.
```json
{
  "name": "Solar plant & tracking system",
  "position": 4,
  "description": "86.4 kWp installed, +20% yield with tracking",
  "durationMinutes": 15,
  "maxCapacity": 15,
  "location": "Solar field — observation path",
  "demo": "Live dashboard screen (Infineon sensors)",
  "safetyNotes": "Observation path only; tracker, battery/inverter and running generator restricted"
}
```

**WorkshopRequest / WorkshopResponse:** `name` (required), `durationMinutes` (required, ≥1), `targetGroup` (required, e.g. `All`, `Professional`, `Schools`), `facilitator`, `description`, `status` (`DRAFT` by default).

**Confirmed reference content** (module owners: Aida, Jean-Louis, Delwende, Phares, Abdoul): standard tour stops — Welcome & safety briefing (5-10 min, Front Desk), Mango orchard (20 min, Keitt 200 trees/2 ha), Smart drip irrigation (10-15 min, max 10), Solar plant & tracking (15 min, max 15), Processing unit (20 min, max 10), Wrap-up & questions (15 min) → **max 10 pers/slot** (limiting stops: irrigation + processing). Reference workshops: Standard farm tour (~100 min, All, Alix), Solar workshop (45 min, Professional, Guest Aida), Mango tasting & processing (40 min, General public, Guest Abdoul), School discovery day (120 min, Schools, P. Nikiéma).

---

## 4. Business Rules

### Scheduling Rules (`SchedulingRules.java`)

| Rule | Value | Source |
|------|-------|--------|
| Max visitors per slot | **10** | Confirmed with module owners (Abdoul) |
| Max slots per day | **2** | Mockup: morning + afternoon |
| Closed day | **Sunday** | All module owners |
| Morning slot | 09:00–11:00 | Default |
| Afternoon slot | 14:00–16:00 | Default |

### Registration Rules

| Rule | Enforcement |
|------|-------------|
| Cannot register on a **CANCELLED** slot | `BusinessRuleException` |
| No duplicate registration (visitor + slot) | `ConflictException` (409) |
| Group size must fit remaining capacity | `BusinessRuleException` |
| Only **PENDING** → approve/reject | `BusinessRuleException` |
| Only **CONFIRMED** → check-in | `BusinessRuleException` |
| Cannot cancel after **CHECKED_IN** | `BusinessRuleException` |
| **CONFIRMED** → auto-creates `Briefing` (PENDING) | Service layer |
| Briefing delivery requires PENDING status | `BusinessRuleException` |

### Event Rules

| Rule | Enforcement |
|------|-------------|
| `endDateTime` must be after `startDateTime` | `BusinessRuleException` |
| Only **DRAFT** events can be published | `BusinessRuleException` |
| Cannot publish an event that has already ended | `BusinessRuleException` |
| Cannot update **CANCELLED** or **COMPLETED** events | `BusinessRuleException` |
| Cannot cancel a **COMPLETED** event | `BusinessRuleException` |
| Registration only on **PUBLISHED** events | `BusinessRuleException` |
| No duplicate registration (visitor + event) | `ConflictException` (409) |
| `booked + groupSize > maxCapacity` → reject | `BusinessRuleException` (422) |
| Booked count excludes `REJECTED` / `CANCELLED` registrations | Repository query |

### Feedback & Survey Rules

| Rule | Enforcement |
|------|-------------|
| Rating must be between 1 and 5 | Bean validation (400) |
| Response linked to a survey must match the survey's visitor | `BusinessRuleException` (422) |
| A survey can only be answered once (RECEIVED is final) | `BusinessRuleException` (422) |
| Survey channel cannot be `ON_SITE` (tablet responses are direct) | `BusinessRuleException` (422) |
| Response without a survey → recorded as `ON_SITE` | Service layer |
| Linking a response marks the survey `RECEIVED` | Service layer |
| Routing targets are free-text tags (no coupling to other modules) | Data model |

### Educational Program Rules

| Rule | Enforcement |
|------|-------------|
| Position must be ≥ 1 | Bean validation (400) |
| Position must be unique among **active** stops | `ConflictException` (409) |
| Deactivating a stop frees its position (soft-delete) | `DELETE /tour-stops/{id}` |
| Create → status `DRAFT` | Service layer |
| Only **DRAFT** workshops can be published | `BusinessRuleException` (422) |
| Only **ACTIVE** workshops can be deactivated | `BusinessRuleException` (422) |

### Visitor Rules

| Rule | Enforcement |
|------|-------------|
| Duplicate email → `ConflictException` (409) | On create and update |
| `@NotBlank` on fullName | Bean validation (400) |
| `@Email` on email | Bean validation (400) |

### Lead Generation (Prospect Detection)

The visitor management app doubles as a **marketing tool** for the Sales & Marketing team. When a visitor registers with a commercial purpose, the system automatically flags them as a prospect.

**How it works:**
1. During registration, the visitor **must** select a `visitPurpose` (mandatory field)
2. If the purpose is `PURCHASE`, `PARTNERSHIP`, or `INVESTMENT`, the system sets `isProspect = true` automatically
3. Sales & Marketing can query all prospects via `GET /api/v1/registrations?prospect=true`

**VisitPurpose values:**

| Purpose | Prospect? | Description |
|---------|-----------|-------------|
| `TOURISM` | No | Leisure visit, tourism |
| `PURCHASE` | **Yes** | Interested in buying products |
| `PARTNERSHIP` | **Yes** | Business partnership opportunity |
| `INVESTMENT` | **Yes** | Investment opportunity |
| `EDUCATION` | No | Training, educational visit |
| `OTHER` | No | Other reasons |

**Prospect query example:**
```
GET /api/v1/registrations?prospect=true
```
Returns all registrations where `isProspect = true`, including visitor name, email, phone, and visit purpose.

---

## 5. Status Flow Diagrams

### TimeSlot Status Flow
```
                ┌──────────┐
     create     │ AVAILABLE│
   ──────────►  └────┬─────┘
                     │
           ┌─────────┼──────────┐
           │         │          │
      register   assign     cancel
           │      guide         │
           ▼         │          ▼
     ┌──────────┐   │   ┌──────────┐
     │ RESERVED │   │   │ CANCELLED│
     └────┬─────┘   │   └──────────┘
          │         │
     full │         │
          ▼         │
     ┌──────┐      │
     │ FULL │◄─────┘ (booked >= capacity)
     └──┬───┘
        │
   end_time passes
        ▼
  ┌───────────┐
  │ COMPLETED │
  └───────────┘
```

### Registration Status Flow
```
                ┌─────────┐
    register    │ PENDING │
   ──────────►  └────┬────┘
                     │
            ┌────────┼──────────┐
            │        │          │
        approve   reject     cancel
            │        │          │
            ▼        ▼          ▼
     ┌──────────┐ ┌──────────┐ ┌───────────┐
     │CONFIRMED │ │ REJECTED │ │ CANCELLED │
     └────┬─────┘ └──────────┘ └───────────┘
          │
     check-in
          │
          ▼
   ┌───────────┐
   │CHECKED_IN │
   └─────┬─────┘
         │
    cancel blocked
         │
    (final state)
```

### Briefing Status Flow
```
         ┌─────────┐
  auto   │ PENDING │  ← created when registration CONFIRMED
 ──────► └────┬────┘
              │
         deliver briefing
              │
              ▼
        ┌──────────┐
        │   DONE   │  ← staffMember + signature + deliveredAt
        └──────────┘
```

### Event Status Flow
```
               ┌─────────┐
   create      │  DRAFT  │
  ─────────►   └────┬────┘
                    │
              publish (not ended)
                    │
                    ▼
            ┌───────────┐      cancel from any non-completed
            │ PUBLISHED │◄───────────┐
            └─────┬─────┘            │
                  │            ┌─────┴─────┐
          ended   │            │ CANCELLED │
                  │            └───────────┘
                  ▼
          ┌───────────┐
          │ COMPLETED │
          └───────────┘
```

### Survey Status Flow
```
              ┌─────────┐
   POST  │ SENT    │  ← survey link dispatched by email/SMS/WhatsApp
  ─────► └────┬────┘
              │
         response linked
              │
              ▼
        ┌───────────┐
        │ RECEIVED  │  ← linked Feedback (rating visible)
        └───────────┘
```

---

## 6. Error Handling

All errors return a consistent JSON envelope via `GlobalExceptionHandler`:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Time slot 999 not found",
  "path": "/api/v1/time-slots/999"
}
```

| Exception | HTTP Status | When |
|-----------|-------------|------|
| `ResourceNotFoundException` | 404 | Entity not found |
| `ConflictException` | 409 | Duplicate email, duplicate registration, overlapping slot |
| `BusinessRuleException` | 422 | Invalid state transition, capacity exceeded, Sunday creation |
| `MethodArgumentNotValidException` | 400 | Bean validation failures (`@NotBlank`, `@Email`, etc.) |

---

## 7. Test Coverage Summary

| Layer | Annotation | Tests | Framework |
|-------|-----------|-------|-----------|
| Repository | `@DataJpaTest` + H2 | 32 | Spring Data + AssertJ |
| Service | `@ExtendWith(MockitoExtension.class)` | 74 | Mockito + AssertJ |
| Controller | `@WebMvcTest` + MockMvc | 69 | MockMvc + Mockito |
| Context | `@SpringBootTest` | 1 | Spring Boot |
| **Total** | | **176** | |

### Test Files

```
src/test/java/com/infineonbit/sustainablefarm/
├── SustainableFarmApplicationTests.java
└── modules/visitormanagement/
    ├── repository/
    │   ├── TimeSlotRepositoryTest.java       (5 tests)
    │   ├── VisitorRepositoryTest.java        (4 tests)
    │   ├── RegistrationRepositoryTest.java   (5 tests)
    │   ├── BriefingRepositoryTest.java       (2 tests)
    │   ├── EventRepositoryTest.java          (4 tests)
    │   ├── FeedbackRepositoryTest.java       (7 tests)
    │   └── EducationalProgramRepositoryTest.java (5 tests)
    ├── service/
    │   ├── SchedulingServiceImplTest.java    (13 tests)
    │   ├── RegistrationServiceImplTest.java  (21 tests)
    │   ├── EventServiceImplTest.java         (16 tests)
    │   ├── FeedbackServiceImplTest.java      (12 tests)
    │   └── EducationalProgramServiceImplTest.java (12 tests)
    └── controller/
        ├── TimeSlotControllerTest.java       (9 tests)
        ├── VisitorControllerTest.java        (7 tests)
        ├── RegistrationControllerTest.java   (12 tests)
        ├── BriefingControllerTest.java       (5 tests)
        ├── EventControllerTest.java          (13 tests)
        ├── FeedbackControllerTest.java       (10 tests)
        └── EducationalProgramControllerTest.java (12 tests)
```

---

## 8. Running the Backend

```bash
# Start PostgreSQL + Backend
docker compose up -d backend

# Backend available at
http://localhost:8080

# Swagger UI
http://localhost:8080/swagger-ui/index.html

# Actuator health
http://localhost:8080/actuator/health

# Run tests (Docker, no local JDK required)
docker run --rm -v "$PWD":/src -w /src \
  -v "$HOME/.m2":/root/.m2 \
  maven:3.9-eclipse-temurin-21 ./mvnw test

# Compile only
docker run --rm -v "$PWD":/src -w /src \
  -v "$HOME/.m2":/root/.m2 \
  maven:3.9-eclipse-temurin-21 ./mvnw compile
```

---

## 9. Technology Stack

| Component | Version | Notes |
|-----------|---------|-------|
| Java | 21 | LTS |
| Spring Boot | 4.1.0 | Modular monolith |
| Spring Data JPA | 4.1.0 | Derived queries + `@Query` |
| Hibernate ORM | 7.4.1 | `ddl-auto=update` (no Flyway) |
| PostgreSQL | 17-alpine | Production database |
| H2 | 2.x | Test-only in-memory DB |
| SpringDoc OpenAPI | 2.8.5 | Swagger UI auto-generated |
| Lombok | 1.18.46 | Boilerplate reduction |
| Jackson | 3.1.4 | JSON serialization |
| Mockito | 5.23.0 | Service + controller tests |
| AssertJ | 3.27.7 | Fluent assertions |

---

*Last updated: 2026-09-09*

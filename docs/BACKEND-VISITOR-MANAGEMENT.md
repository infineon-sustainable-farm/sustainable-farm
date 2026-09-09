# Visitor Management Backend — Technical Documentation

> Spring Boot 4.1.0 · Java 21 · PostgreSQL 17 · Hibernate 7.4

---

## 1. Architecture Overview

```
┌──────────────────────────────────────────────────────────────┐
│                        REST Controllers                       │
│  TimeSlotController · VisitorController                      │
│  RegistrationController · BriefingController                 │
│  EventController                                            │
├──────────────────────────────────────────────────────────────┤
│                         Services                              │
│  SchedulingServiceImpl · RegistrationServiceImpl             │
│  EventServiceImpl                                          │
│  SchedulingRules (constants)                                 │
├──────────────────────────────────────────────────────────────┤
│                     Repositories (JPA)                        │
│  TimeSlotRepository · VisitorRepository                      │
│  RegistrationRepository · BriefingRepository                 │
│  EventRepository                                            │
├──────────────────────────────────────────────────────────────┤
│                      JPA Entities                             │
│  TimeSlot · Visitor · Registration · Briefing · Event       │
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
```

### Table Summary

| Table | Rows (design) | Purpose |
|-------|--------------|---------|
| `time_slot` | ~500/year | Farm tour calendar slots (2/day max) |
| `visitor` | ~2,000 | Visitor database (individuals/groups) |
| `registration` | ~4,000 | Links visitor → time slot **or** event |
| `briefing` | ~3,000 | Safety briefing per confirmed registration |
| `event` | ~50/year | Open days, buyer visits, school/community days |

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
| Repository | `@DataJpaTest` + H2 | 20 | Spring Data + AssertJ |
| Service | `@ExtendWith(MockitoExtension.class)` | 50 | Mockito + AssertJ |
| Controller | `@WebMvcTest` + MockMvc | 47 | MockMvc + Mockito |
| Context | `@SpringBootTest` | 1 | Spring Boot |
| **Total** | | **118** | |

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
    │   └── EventRepositoryTest.java          (4 tests)
    ├── service/
    │   ├── SchedulingServiceImplTest.java    (13 tests)
    │   ├── RegistrationServiceImplTest.java  (21 tests)
    │   └── EventServiceImplTest.java         (16 tests)
    └── controller/
        ├── TimeSlotControllerTest.java       (9 tests)
        ├── VisitorControllerTest.java        (7 tests)
        ├── RegistrationControllerTest.java   (12 tests)
        ├── BriefingControllerTest.java       (5 tests)
        └── EventControllerTest.java          (13 tests)
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

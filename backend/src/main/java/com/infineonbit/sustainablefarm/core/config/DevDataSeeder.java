package com.infineonbit.sustainablefarm.core.config;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.AgriActivity;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Event;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventType;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Registration;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.RegistrationStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlot;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlotStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TourStop;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.VisitPurpose;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Visitor;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.VisitorType;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Workshop;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.WorkshopStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.AgriActivityRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.EventRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.RegistrationRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.TimeSlotRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.TourStopRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.VisitorRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.WorkshopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Dev-only reference data. Enable with the {@code dev} Spring profile
 * (e.g. {@code SPRING_PROFILES_ACTIVE=dev}). Never active in production.
 * Each entity type is seeded only when its table is empty (idempotent).
 */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final TourStopRepository tourStopRepository;
    private final WorkshopRepository workshopRepository;
    private final AgriActivityRepository activityRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final EventRepository eventRepository;
    private final VisitorRepository visitorRepository;
    private final RegistrationRepository registrationRepository;

    public DevDataSeeder(TourStopRepository tourStopRepository,
                         WorkshopRepository workshopRepository,
                         AgriActivityRepository activityRepository,
                         TimeSlotRepository timeSlotRepository,
                         EventRepository eventRepository,
                         VisitorRepository visitorRepository,
                         RegistrationRepository registrationRepository) {
        this.tourStopRepository = tourStopRepository;
        this.workshopRepository = workshopRepository;
        this.activityRepository = activityRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.eventRepository = eventRepository;
        this.visitorRepository = visitorRepository;
        this.registrationRepository = registrationRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedTourStops();
        seedWorkshops();
        seedActivities();
        seedTimeSlots();
        seedEvent();
        seedSampleVisitor();
        log.info("Dev data seeding complete");
    }

    private void seedTourStops() {
        if (tourStopRepository.count() > 0) {
            return;
        }
        tourStopRepository.save(stop("Welcome & safety briefing", 1, "Introduction to the farm, safety rules and visit flow",
                10, 10, "Front desk", null, "Follow the marked path, use provided sunscreen and water"));
        tourStopRepository.save(stop("Mango orchard", 2, "Keitt and Brooks mango trees, 200 trees over 2 ha, orchard management",
                20, 10, "Orchard", null, "Stay on the paths, do not pick fruit without a guide"));
        tourStopRepository.save(stop("Smart drip irrigation", 3, "Solar-powered drip irrigation keeping water use under control",
                15, 10, "Irrigation plots", "Drip line layout", "Keep hands off the drip lines when running"));
        tourStopRepository.save(stop("Solar plant & tracking", 4, "On-grid solar field with single-axis tracking, energy dashboard",
                15, 15, "Solar field", "Energy dashboard", "Observation path only, keep distance from panels"));
        tourStopRepository.save(stop("Processing unit", 5, "Mango drying and juicing unit, small-scale processing setup",
                20, 10, "Processing unit", "Solar dryer", "No loose clothing near machinery, hair tied back"));
        tourStopRepository.save(stop("Wrap-up & questions", 6, "Final Q&A, memento and exit survey",
                15, 15, "Visitor center", null, null));
        log.info("Seeded {} tour stops", tourStopRepository.count());
    }

    private TourStop stop(String name, int position, String description, int duration,
                          Integer maxCapacity, String location, String demo, String safetyNotes) {
        TourStop stop = new TourStop();
        stop.setName(name);
        stop.setPosition(position);
        stop.setDescription(description);
        stop.setDurationMinutes(duration);
        stop.setMaxCapacity(maxCapacity);
        stop.setLocation(location);
        stop.setDemo(demo);
        stop.setSafetyNotes(safetyNotes);
        stop.setActive(true);
        return stop;
    }

    private void seedWorkshops() {
        if (workshopRepository.count() > 0) {
            return;
        }
        workshopRepository.save(workshop("Standard farm tour", 100, "All", "Alix",
                "Guided visit of the whole farm", WorkshopStatus.ACTIVE));
        workshopRepository.save(workshop("Solar workshop", 45, "Professional", "Guest Aida",
                "Solar farm operation and energy dashboard", WorkshopStatus.ACTIVE));
        workshopRepository.save(workshop("Mango tasting & processing", 40, "General public", "Guest Abdoul",
                "Tasting and small processing demo", WorkshopStatus.DRAFT));
        workshopRepository.save(workshop("School discovery day", 120, "Schools", "P. Nikiéma",
                "Half-day educational program for school groups", WorkshopStatus.ACTIVE));
        log.info("Seeded {} workshops", workshopRepository.count());
    }

    private Workshop workshop(String name, int duration, String targetGroup, String facilitator,
                              String description, WorkshopStatus status) {
        Workshop workshop = new Workshop();
        workshop.setName(name);
        workshop.setDurationMinutes(duration);
        workshop.setTargetGroup(targetGroup);
        workshop.setFacilitator(facilitator);
        workshop.setDescription(description);
        workshop.setStatus(status);
        return workshop;
    }

    private void seedActivities() {
        if (activityRepository.count() > 0) {
            return;
        }
        activityRepository.save(activity("Standard farm tour", "5000", 10, 100,
                "Guided tour across orchard, irrigation, solar and processing areas"));
        activityRepository.save(activity("Mango tasting & processing", "3000", 12, 40,
                "Tasting session with a small processing demo"));
        activityRepository.save(activity("Solar workshop", "5000", 15, 45,
                "On-site visit of the solar field with energy dashboard"));
        log.info("Seeded {} activities", activityRepository.count());
    }

    private AgriActivity activity(String name, String price, int capacity, int duration, String description) {
        AgriActivity activity = new AgriActivity();
        activity.setName(name);
        activity.setPrice(new BigDecimal(price));
        activity.setCapacity(capacity);
        activity.setDurationMinutes(duration);
        activity.setDescription(description);
        activity.setActive(true);
        return activity;
    }

    private void seedTimeSlots() {
        if (timeSlotRepository.count() > 0) {
            return;
        }
        LocalDate day = LocalDate.now().plusDays(1);
        for (int i = 0; i < 7; i++) {
            if (day.getDayOfWeek() == DayOfWeek.SUNDAY) {
                day = day.plusDays(1);
                continue;
            }
            timeSlotRepository.save(slot(day, LocalTime.of(9, 0), LocalTime.of(11, 0)));
            timeSlotRepository.save(slot(day, LocalTime.of(14, 0), LocalTime.of(16, 0)));
            day = day.plusDays(1);
        }
        log.info("Seeded {} time slots", timeSlotRepository.count());
    }

    private TimeSlot slot(LocalDate date, LocalTime start, LocalTime end) {
        TimeSlot slot = new TimeSlot();
        slot.setDate(date);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setMaxCapacity(10);
        slot.setStatus(TimeSlotStatus.AVAILABLE);
        return slot;
    }

    private void seedEvent() {
        if (eventRepository.count() > 0) {
            return;
        }
        Event event = new Event();
        event.setTitle("Journée portes ouvertes");
        event.setType(EventType.OPEN_DAY);
        LocalDate openDay = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).plusWeeks(2);
        event.setStartDateTime(openDay.atTime(9, 0));
        event.setEndDateTime(openDay.atTime(17, 0));
        event.setMaxCapacity(50);
        event.setLocation("Sustainable Farm");
        event.setDescription("Discovery day with guided tours, workshops and local products.");
        event.setStatus(EventStatus.PUBLISHED);
        eventRepository.save(event);
        log.info("Seeded event '{}'", event.getTitle());
    }

    private void seedSampleVisitor() {
        if (visitorRepository.count() > 0) {
            return;
        }
        Visitor visitor = new Visitor();
        visitor.setFullName("Awa Ouédraogo");
        visitor.setGroupSize(8);
        visitor.setEmail("awa.ouedraogo@example.com");
        visitor.setPhone("+226 70 00 00 00");
        visitor.setLanguage("French");
        visitor.setType(VisitorType.GROUP);
        visitor.setSpecialNeeds("Accès PMR");
        visitorRepository.save(visitor);

        timeSlotRepository.findByDateGreaterThanEqualOrderByDateAscStartTimeAsc(LocalDate.now())
                .stream()
                .findFirst()
                .ifPresent(slot -> {
                    Registration registration = new Registration();
                    registration.setVisitor(visitor);
                    registration.setTimeSlot(slot);
                    registration.setVisitPurpose(VisitPurpose.PURCHASE);
                    registration.setProspect(true);
                    registration.setStatus(RegistrationStatus.PENDING);
                    registrationRepository.save(registration);
                });
        log.info("Seeded sample visitor + pending registration");
    }
}
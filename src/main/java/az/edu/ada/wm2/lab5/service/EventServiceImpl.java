package az.edu.ada.wm2.lab5.service;

import az.edu.ada.wm2.lab5.model.Event;
import az.edu.ada.wm2.lab5.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    @Autowired
    public EventServiceImpl(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public Event createEvent(Event event) {
        if (event.getId() == null) {
            event.setId(UUID.randomUUID());
        }
        return eventRepository.save(event);
    }

    @Override
    public Event getEventById(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
    }

    @Override
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @Override
    public Event updateEvent(UUID id, Event event) {
        if (!eventRepository.existsById(id)) {
            throw new RuntimeException("Event not found with id: " + id);
        }
        event.setId(id);
        return eventRepository.save(event);
    }

    @Override
    public void deleteEvent(UUID id) {
        if (!eventRepository.existsById(id)) {
            throw new RuntimeException("Event not found with id: " + id);
        }
        eventRepository.deleteById(id);
    }

    @Override
    public Event partialUpdateEvent(UUID id, Event partialEvent) {
        Event existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));

        // Update only non-null fields
        if (partialEvent.getEventName() != null) {
            existingEvent.setEventName(partialEvent.getEventName());
        }
        if (partialEvent.getTags() != null && !partialEvent.getTags().isEmpty()) {
            existingEvent.setTags(partialEvent.getTags());
        }
        if (partialEvent.getTicketPrice() != null) {
            existingEvent.setTicketPrice(partialEvent.getTicketPrice());
        }
        if (partialEvent.getEventDateTime() != null) {
            existingEvent.setEventDateTime(partialEvent.getEventDateTime());
        }
        if (partialEvent.getDurationMinutes() > 0) {
            existingEvent.setDurationMinutes(partialEvent.getDurationMinutes());
        }

        return eventRepository.save(existingEvent);
    }

    // Custom methods
    @Override
    public List<Event> getEventsByTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return List.of();
        }
        String normalized = tag.trim().toLowerCase();

        return eventRepository.findAll()
                .stream()
                .filter(e -> e != null && e.getTags() != null)
                .filter(e -> e.getTags().stream()
                        .filter(t -> t != null && !t.trim().isEmpty())
                        .map(t -> t.trim().toLowerCase())
                        .anyMatch(t -> t.equals(normalized)))
                .collect(Collectors.toList());
    }

    @Override
    public List<Event> getUpcomingEvents() {
        LocalDateTime now = LocalDateTime.now();

        return eventRepository.findAll()
                .stream()
                .filter(e -> e != null && e.getEventDateTime() != null)
                .filter(e -> e.getEventDateTime().isAfter(now))
                .sorted((a, b) -> a.getEventDateTime().compareTo(b.getEventDateTime()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Event> getEventsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice == null && maxPrice == null) {
            return getAllEvents();
        }

        BigDecimal min = minPrice;
        BigDecimal max = maxPrice;
        if (min == null) min = BigDecimal.valueOf(Long.MIN_VALUE);
        if (max == null) max = BigDecimal.valueOf(Long.MAX_VALUE);

        if (min.compareTo(max) > 0) {
            BigDecimal tmp = min;
            min = max;
            max = tmp;
        }

        BigDecimal finalMin = min;
        BigDecimal finalMax = max;

        return eventRepository.findAll()
                .stream()
                .filter(e -> e != null && e.getTicketPrice() != null)
                .filter(e -> e.getTicketPrice().compareTo(finalMin) >= 0
                        && e.getTicketPrice().compareTo(finalMax) <= 0)
                .collect(Collectors.toList());
    }

    @Override
    public List<Event> getEventsByDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return getAllEvents();
        }

        LocalDateTime s = start;
        LocalDateTime e = end;

        if (s == null) s = LocalDateTime.MIN;
        if (e == null) e = LocalDateTime.MAX;

        if (s.isAfter(e)) {
            LocalDateTime tmp = s;
            s = e;
            e = tmp;
        }

        LocalDateTime finalStart = s;
        LocalDateTime finalEnd = e;

        return eventRepository.findAll()
                .stream()
                .filter(ev -> ev != null && ev.getEventDateTime() != null)
                .filter(ev -> !ev.getEventDateTime().isBefore(finalStart)
                        && !ev.getEventDateTime().isAfter(finalEnd))
                .sorted((a, b) -> a.getEventDateTime().compareTo(b.getEventDateTime()))
                .collect(Collectors.toList());
    }

    @Override
    public Event updateEventPrice(UUID id, BigDecimal newPrice) {
        if (id == null) {
            throw new IllegalArgumentException("Event id cannot be null");
        }
        if (newPrice == null) {
            throw new IllegalArgumentException("New price cannot be null");
        }
        if (newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("New price cannot be negative");
        }

        Event existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));

        existingEvent.setTicketPrice(newPrice);
        return eventRepository.save(existingEvent);

    }
}
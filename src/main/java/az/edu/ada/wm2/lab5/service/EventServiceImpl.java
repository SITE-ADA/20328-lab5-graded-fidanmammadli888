package az.edu.ada.wm2.lab5.service;

import az.edu.ada.wm2.lab5.model.Event;
import az.edu.ada.wm2.lab5.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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

        return eventRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(e -> e.getEventDateTime() != null)
                .filter(e -> !e.getEventDateTime().isBefore(now)) // eventDateTime >= now
                .sorted((a, b) -> a.getEventDateTime().compareTo(b.getEventDateTime()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Event> getEventsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice == null && maxPrice == null) {
            return eventRepository.findAll();
        }

        BigDecimal min = (minPrice == null) ? BigDecimal.ZERO : minPrice;
        BigDecimal max = (maxPrice == null) ? new BigDecimal("999999999") : maxPrice;

        if (min.compareTo(BigDecimal.ZERO) < 0 || max.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price range cannot be negative.");
        }
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice.");
        }

        return eventRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(e -> e.getTicketPrice() != null)
                .filter(e -> e.getTicketPrice().compareTo(min) >= 0 && e.getTicketPrice().compareTo(max) <= 0) // inclusive
                .sorted((a, b) -> a.getTicketPrice().compareTo(b.getTicketPrice()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Event> getEventsByDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return eventRepository.findAll();
        }

        LocalDateTime s = (start == null) ? LocalDateTime.MIN : start;
        LocalDateTime e = (end == null) ? LocalDateTime.MAX : end;

        if (s.isAfter(e)) {
            throw new IllegalArgumentException("start cannot be after end.");
        }

        return eventRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(ev -> ev.getEventDateTime() != null)
                .filter(ev -> !ev.getEventDateTime().isBefore(s) && !ev.getEventDateTime().isAfter(e)) // inclusive
                .sorted((a, b) -> a.getEventDateTime().compareTo(b.getEventDateTime()))
                .collect(Collectors.toList());
    }

    @Override
    public Event updateEventPrice(UUID id, BigDecimal newPrice) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null.");
        }
        if (newPrice == null) {
            throw new IllegalArgumentException("newPrice cannot be null.");
        }
        if (newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("newPrice cannot be negative.");
        }

        Event existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));

        existingEvent.setTicketPrice(newPrice);
        return eventRepository.save(existingEvent);
    }

    private String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t.toLowerCase(Locale.ROOT);
    }
}
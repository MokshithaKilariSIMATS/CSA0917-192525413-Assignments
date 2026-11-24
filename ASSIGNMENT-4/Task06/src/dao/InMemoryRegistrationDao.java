package dao;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import model.Event;
import model.Registration;

/** Test double with the same contract and the same UNIQUE(student, event) rule as the MySQL table. */
public class InMemoryRegistrationDao implements RegistrationDao {
    private final List<Event> events = Arrays.asList(
        new Event(1, "Hackathon 2026", "2026-12-05"), new Event(2, "Robotics Workshop", "2026-12-12"),
        new Event(3, "Cultural Night", "2026-12-20"), new Event(4, "Guest Lecture: AI", "2026-12-28"), new Event(5, "Sports Meet", "2027-01-10"));
    private final List<Registration> rows = new ArrayList<>();
    private final Set<String> keys = new HashSet<>();
    private int nextId = 1;
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String eventName(int id) { for (Event e : events) if (e.getId() == id) return e.getName(); throw new IllegalArgumentException("No event " + id); }

    public List<Event> findEvents() { return events; }
    public synchronized boolean exists(String s, int e) { return keys.contains(s + "|" + e); }
    public synchronized void insert(String s, String name, String email, int eventId) throws DuplicateRegistrationException {
        if (!keys.add(s + "|" + eventId)) throw new DuplicateRegistrationException("Student " + s + " is already registered for this event.");
        rows.add(new Registration(nextId++, s, name, email, eventId, eventName(eventId), LocalDateTime.now().format(F)));
    }
    /** Bulk loader used by the benchmark (mirrors the CTE in sql/schema.sql). */
    public synchronized void seed(int n) {
        for (int i = 1; i <= n; i++) {
            String sid = String.valueOf(192000000 + i); int ev = 1 + (i % 5);
            keys.add(sid + "|" + ev);
            rows.add(new Registration(nextId++, sid, "Student " + i, "s" + i + "@campus.edu", ev, eventName(ev), "2026-11-01 10:00:00"));
        }
    }
    public synchronized int count(Integer ev) { return ev == null ? rows.size() : (int) rows.stream().filter(r -> r.getEventId() == ev).count(); }
    public synchronized List<Registration> findPage(Integer ev, int limit, int offset) {
        List<Registration> out = new ArrayList<>(limit); int skipped = 0;
        for (Registration r : rows) {
            if (ev != null && r.getEventId() != ev) continue;
            if (skipped++ < offset) continue;
            out.add(r); if (out.size() == limit) break;
        }
        return out;
    }
    public synchronized List<Registration> findAll(Integer ev) {
        if (ev == null) return new ArrayList<>(rows);
        List<Registration> out = new ArrayList<>(); for (Registration r : rows) if (r.getEventId() == ev) out.add(r); return out;
    }
}

package dao;
import java.sql.*;
import java.util.*;
import model.Event;
import model.Registration;

/** MySQL implementation. Every statement is a PreparedStatement; every resource is closed by try-with-resources. */
public class JdbcRegistrationDao implements RegistrationDao {
    private final String url, user, pass;
    public JdbcRegistrationDao(String url, String user, String pass) { this.url = url; this.user = user; this.pass = pass; }
    private Connection open() throws SQLException { return DriverManager.getConnection(url, user, pass); }

    private static final String SELECT =
        "SELECT r.id, r.student_id, r.student_name, r.email, r.event_id, e.name, r.registered_at " +
        "FROM registrations r JOIN events e ON e.id = r.event_id ";

    public List<Event> findEvents() throws SQLException {
        List<Event> out = new ArrayList<>();
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement("SELECT id, name, event_date FROM events ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(new Event(rs.getInt(1), rs.getString(2), rs.getString(3)));
        }
        return out;
    }
    public boolean exists(String studentId, int eventId) throws SQLException {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement("SELECT 1 FROM registrations WHERE student_id = ? AND event_id = ?")) {
            ps.setString(1, studentId); ps.setInt(2, eventId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }
    public void insert(String studentId, String name, String email, int eventId) throws SQLException, DuplicateRegistrationException {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO registrations(student_id, student_name, email, event_id) VALUES(?,?,?,?)")) {
            ps.setString(1, studentId); ps.setString(2, name); ps.setString(3, email); ps.setInt(4, eventId);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException dup) {        // UNIQUE(student_id, event_id)
            throw new DuplicateRegistrationException("Student " + studentId + " is already registered for this event.");
        }
    }
    public int count(Integer eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM registrations" + (eventId == null ? "" : " WHERE event_id = ?");
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (eventId != null) ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }
    public List<Registration> findPage(Integer eventId, int limit, int offset) throws SQLException {
        String sql = SELECT + (eventId == null ? "" : "WHERE r.event_id = ? ") + "ORDER BY r.id LIMIT ? OFFSET ?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            if (eventId != null) ps.setInt(i++, eventId);
            ps.setInt(i++, limit); ps.setInt(i, offset);
            return read(ps);
        }
    }
    public List<Registration> findAll(Integer eventId) throws SQLException {
        String sql = SELECT + (eventId == null ? "" : "WHERE r.event_id = ? ") + "ORDER BY r.id";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (eventId != null) ps.setInt(1, eventId);
            return read(ps);
        }
    }
    private List<Registration> read(PreparedStatement ps) throws SQLException {
        List<Registration> out = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(new Registration(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5), rs.getString(6), rs.getString(7)));
        }
        return out;
    }
}

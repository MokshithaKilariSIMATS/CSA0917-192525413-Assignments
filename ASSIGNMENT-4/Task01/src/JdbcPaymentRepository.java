import java.sql.*;

/**
 * MySQL implementation (PreparedStatement + try-with-resources).
 * Table: payment(student_id VARCHAR(15), amount DECIMAL(10,2)); student(student_id, due DECIMAL(10,2)).
 */
public class JdbcPaymentRepository implements PaymentRepository {
    private final String url, user, pass;
    public JdbcPaymentRepository(String url, String user, String pass) { this.url = url; this.user = user; this.pass = pass; }

    public boolean studentExists(String id) {
        try (Connection c = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM student WHERE student_id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new IllegalStateException("Database error: " + e.getMessage(), e); }
    }
    public double outstanding(String id) {
        try (Connection c = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = c.prepareStatement("SELECT due FROM student WHERE student_id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getDouble(1) : 0; }
        } catch (SQLException e) { throw new IllegalStateException("Database error: " + e.getMessage(), e); }
    }
    public void savePayment(String id, double amount) throws SQLException {
        try (Connection c = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = c.prepareStatement("INSERT INTO payment(student_id, amount) VALUES(?, ?)")) {
            ps.setString(1, id);
            ps.setDouble(2, amount);
            ps.executeUpdate();
        }
    }
}

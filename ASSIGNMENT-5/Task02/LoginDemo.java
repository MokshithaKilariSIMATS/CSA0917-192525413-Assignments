import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Task B2 : unsafe (string-concatenation) login vs PreparedStatement login.
 * Runs ONLY against a throw-away local test database created by this program.
 */
public class LoginDemo {

    /** UNSAFE: user input becomes part of the SQL text */
    static boolean unsafeLogin(Connection c, String user, String pass) throws SQLException {
        String sql = "SELECT username FROM users WHERE username = '" + user + "' AND password = '" + pass + "'";
        System.out.println("      SQL sent : " + sql);
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next();
        } catch (SQLException syntax) {
            System.out.println("      SQL error: " + syntax.getMessage());
            return false;
        }
    }

    /** SAFE: input is bound as DATA to ? placeholders and can never change the query structure */
    static boolean safeLogin(Connection c, String user, String pass) throws SQLException {
        String sql = "SELECT username FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user);
            ps.setString(2, pass);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        try (Connection c = Db.open()) {
            Db.dropIfExists(c, "users");
            try (Statement st = c.createStatement()) {
                st.executeUpdate("CREATE TABLE users (username VARCHAR(30) NOT NULL PRIMARY KEY, password VARCHAR(60) NOT NULL)");
                st.executeUpdate("INSERT INTO users VALUES ('admin','Admin@123')");
                st.executeUpdate("INSERT INTO users VALUES ('student','pass123')");
            }
            System.out.println("=== Task B2 : SQL-injection experiment (local test DB, users: admin / student) ===");

            String[][] cases = {
                    {"1 valid credentials",          "admin",          "Admin@123",    "ACCEPT"},
                    {"2 wrong password",             "admin",          "wrong",        "REJECT"},
                    {"3 comment-out attack",         "admin' --",      "anything",     "REJECT"},
                    {"4 always-true attack (user)",  "' OR '1'='1",    "' OR '1'='1",  "REJECT"},
                    {"5 always-true attack (pass)",  "admin",          "x' OR 'a'='a", "REJECT"},
            };
            System.out.printf("%n%-32s %-9s %-9s %-9s%n", "Case", "Expected", "UNSAFE", "SAFE(PS)");
            StringBuilder table = new StringBuilder();
            for (String[] cs : cases) {
                System.out.println();
                System.out.println("[" + cs[0] + "]  user=\"" + cs[1] + "\"  pass=\"" + cs[2] + "\"");
                boolean u = unsafeLogin(c, cs[1], cs[2]);
                boolean s = safeLogin(c, cs[1], cs[2]);
                System.out.println("      UNSAFE -> " + (u ? "ACCEPTED" : "rejected") + "   |   PreparedStatement -> " + (s ? "ACCEPTED" : "rejected"));
                String flag = (u && cs[3].equals("REJECT")) ? "  <-- BYPASS" : "";
                table.append(String.format("%-32s %-9s %-9s %-9s%s%n", cs[0], cs[3], u ? "ACCEPT" : "reject", s ? "ACCEPT" : "reject", flag));
            }
            System.out.println();
            System.out.printf("%-32s %-9s %-9s %-9s%n", "Case", "Expected", "UNSAFE", "SAFE(PS)");
            System.out.print(table);
        } finally {
            Db.shutdownDerby();
        }
    }
}

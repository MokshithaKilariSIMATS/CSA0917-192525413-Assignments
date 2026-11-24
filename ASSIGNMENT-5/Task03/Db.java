import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Connection helper shared by Task02-Task04.
 *
 * Default: embedded Apache Derby (self-contained, no server needed) so the code runs anywhere.
 * MySQL:   run with  -Ddb.url=jdbc:mysql://localhost:3306/busdb -Ddb.user=root -Ddb.pass=secret
 *          and put mysql-connector-j on the class path. The SQL used is portable between both.
 */
public final class Db {
    private Db() {}

    public static final String DERBY_DIR = System.getProperty("derby.dir", "testdb");

    public static boolean isMySql() {
        return System.getProperty("db.url", "").startsWith("jdbc:mysql");
    }

    public static Connection open() throws SQLException {
        String url = System.getProperty("db.url", "jdbc:derby:" + DERBY_DIR + ";create=true");
        String user = System.getProperty("db.user", "app");
        String pass = System.getProperty("db.pass", "app");
        return DriverManager.getConnection(url, user, pass);
    }

    public static void shutdownDerby() {
        if (isMySql()) return;
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException expected) {
            // Derby reports a successful shutdown by throwing SQLState XJ015
        }
    }

    /** same DDL as Task03/schema.sql (portable between Derby and MySQL) */
    public static final String BUS_PASS_DDL =
            "CREATE TABLE bus_pass (" +
            " pass_id       VARCHAR(10)   NOT NULL PRIMARY KEY," +
            " reg_no        VARCHAR(15)   NOT NULL," +
            " name          VARCHAR(60)   NOT NULL," +
            " route_no      INT           NOT NULL," +
            " boarding_stop VARCHAR(60)   NOT NULL," +
            " validity_date DATE          NOT NULL," +
            " amount        DECIMAL(8,2)  NOT NULL CHECK (amount >= 0))";

    public static void dropIfExists(Connection c, String table) {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("DROP TABLE " + table);
        } catch (SQLException ignored) {
            // table did not exist
        }
    }

    public static void recreateBusPass(Connection c) throws SQLException {
        boolean ac = c.getAutoCommit();
        c.setAutoCommit(true);
        dropIfExists(c, "bus_pass");
        try (Statement st = c.createStatement()) {
            st.executeUpdate(BUS_PASS_DDL);
        }
        c.setAutoCommit(ac);
    }

    public static int count(Connection c, String table) throws SQLException {
        try (Statement st = c.createStatement();
             var rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}

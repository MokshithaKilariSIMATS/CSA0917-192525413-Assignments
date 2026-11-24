import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Task B3 : three ways of moving bus-pass records into the database.
 *   TODO 1 -> loadStatement()          plain Statement, SQL text built per row
 *   TODO 2 -> loadPrepared()           PreparedStatement, one executeUpdate per row
 *   TODO 3 -> loadBatch()              PreparedStatement.addBatch / executeBatch, 1,000 rows per batch
 * plus loadBatchTransaction() (batches inside ONE transaction) and loadMultiRow().
 */
public class PassLoader {

    public static final int BATCH_SIZE = 1000;
    static final String INSERT_SQL =
            "INSERT INTO bus_pass (pass_id, reg_no, name, route_no, boarding_stop, validity_date, amount) VALUES (?,?,?,?,?,?,?)";

    /** synthetic but realistic records */
    public static List<BusPass> generate(int n, int firstId) {
        List<BusPass> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int id = firstId + i;
            list.add(new BusPass("P" + id, "22CS" + String.format("%05d", id % 90000),
                    "Student " + id, 1 + (id % 25), "Stop " + (id % 40),
                    LocalDate.parse("2027-03-31"), new BigDecimal("1500.00")));
        }
        return list;
    }

    // ---------------------------------------------------------------- TODO 1
    public static void loadStatement(Connection c, List<BusPass> passes) throws SQLException {
        try (Statement st = c.createStatement()) {
            for (BusPass p : passes) {
                String sql = "INSERT INTO bus_pass VALUES ('" + p.passId() + "','" + p.regNo() + "','"
                        + p.name() + "'," + p.route() + ",'" + p.stop() + "','" + p.validity() + "',"
                        + p.amount().toPlainString() + ")";
                st.executeUpdate(sql);                   // parsed + planned again for every row
            }
        }
    }

    // ---------------------------------------------------------------- TODO 2
    public static void loadPrepared(Connection c, List<BusPass> passes) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(INSERT_SQL)) {   // parsed once
            for (BusPass p : passes) {
                bind(ps, p);
                ps.executeUpdate();                      // one round trip per row
            }
        }
    }

    // ---------------------------------------------------------------- TODO 3
    public static void loadBatch(Connection c, List<BusPass> passes) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(INSERT_SQL)) {
            int pending = 0;
            for (BusPass p : passes) {
                bind(ps, p);
                ps.addBatch();
                if (++pending == BATCH_SIZE) {           // execute batches of 1,000 rows
                    ps.executeBatch();
                    pending = 0;
                }
            }
            if (pending > 0) ps.executeBatch();          // remaining rows
        }
    }

    /** batches inside a single transaction: all rows or none */
    public static void loadBatchTransaction(Connection c, List<BusPass> passes) throws SQLException {
        boolean old = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            loadBatch(c, passes);
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(old);
        }
    }

    /** one INSERT ... VALUES (..),(..),(..) per 100 rows */
    public static void loadMultiRow(Connection c, List<BusPass> passes) throws SQLException {
        final int rows = 100;
        boolean old = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            for (int from = 0; from < passes.size(); from += rows) {
                int to = Math.min(from + rows, passes.size());
                StringBuilder sb = new StringBuilder("INSERT INTO bus_pass VALUES ");
                for (int i = from; i < to; i++) sb.append(i > from ? ",(?,?,?,?,?,?,?)" : "(?,?,?,?,?,?,?)");
                try (PreparedStatement ps = c.prepareStatement(sb.toString())) {
                    int k = 1;
                    for (int i = from; i < to; i++) {
                        BusPass p = passes.get(i);
                        ps.setString(k++, p.passId());
                        ps.setString(k++, p.regNo());
                        ps.setString(k++, p.name());
                        ps.setInt(k++, p.route());
                        ps.setString(k++, p.stop());
                        ps.setDate(k++, Date.valueOf(p.validity()));
                        ps.setBigDecimal(k++, p.amount());
                    }
                    ps.executeUpdate();
                }
            }
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(old);
        }
    }

    static void bind(PreparedStatement ps, BusPass p) throws SQLException {
        ps.setString(1, p.passId());
        ps.setString(2, p.regNo());
        ps.setString(3, p.name());
        ps.setInt(4, p.route());
        ps.setString(5, p.stop());
        ps.setDate(6, Date.valueOf(p.validity()));
        ps.setBigDecimal(7, p.amount());
    }

    // ------------------------------------------------------------------ demo
    static void showTable(Connection c, String label) throws SQLException {
        System.out.println("-- SELECT * FROM bus_pass  (" + label + ")");
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM bus_pass ORDER BY pass_id")) {
            int shown = 0, total = 0;
            while (rs.next()) {
                total++;
                if (shown++ < 12) {                      // print only the first rows to keep the output readable
                    System.out.printf("   %s | %s | %-12s | route %2d | %-8s | %s | %s%n",
                            rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4),
                            rs.getString(5), rs.getDate(6), rs.getBigDecimal(7));
                }
            }
            System.out.println("   ... (" + total + " rows in total)");
        }
    }

    public static void main(String[] args) throws Exception {
        try (Connection c = Db.open()) {
            System.out.println("=== Task B3 : insert 10 records with three techniques ===");
            System.out.println("Database : " + c.getMetaData().getDatabaseProductName() + " " + c.getMetaData().getDatabaseProductVersion());
            Db.recreateBusPass(c);

            loadStatement(c, generate(10, 2001));
            loadPrepared(c, generate(10, 2101));
            loadBatch(c, generate(10, 2201));
            System.out.println("Rows inserted: Statement=10, PreparedStatement=10, Batch=10 -> table now holds " + Db.count(c, "bus_pass"));
            showTable(c, "first 12 of 30 rows");

            System.out.println();
            System.out.println("-- ResultSetMetaData of SELECT * FROM bus_pass");
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM bus_pass")) {
                ResultSetMetaData md = rs.getMetaData();
                System.out.printf("   %-3s %-14s %-12s %-5s %-5s %-8s%n", "#", "column", "type", "size", "scale", "nullable");
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    System.out.printf("   %-3d %-14s %-12s %-5d %-5d %-8s%n", i, md.getColumnName(i),
                            md.getColumnTypeName(i), md.getPrecision(i), md.getScale(i),
                            md.isNullable(i) == ResultSetMetaData.columnNoNulls ? "NO" : "YES");
                }
            }

            System.out.println();
            System.out.println("=== Transaction experiment : invalid amount halfway through 2,000 records ===");
            List<BusPass> input = generate(2000, 5001);
            input.set(1000, new BusPass("P6001", "22CS06001", "Bad Amount", 3, "Stop 1",
                    LocalDate.parse("2027-03-31"), new BigDecimal("-500.00")));      // violates CHECK (amount >= 0)

            Db.recreateBusPass(c);
            try {
                loadBatch(c, input);                       // auto-commit ON: every executeBatch commits
            } catch (SQLException e) {
                System.out.println("[auto-commit batches] failure at record #1001 : " + shortMsg(e));
            }
            System.out.println("   rows left in table after failure = " + Db.count(c, "bus_pass") + "   (partial load - inconsistent)");

            Db.recreateBusPass(c);
            try {
                loadBatchTransaction(c, input);
            } catch (SQLException e) {
                System.out.println("[single transaction ] failure at record #1001 : " + shortMsg(e));
            }
            System.out.println("   rows left in table after failure = " + Db.count(c, "bus_pass") + "   (rolled back - all or nothing)");
        } finally {
            Db.shutdownDerby();
        }
    }

    static String shortMsg(SQLException e) {
        SQLException x = e;
        while (x.getNextException() != null) x = x.getNextException();
        String m = x.getMessage();
        return m.length() > 110 ? m.substring(0, 110) + "..." : m;
    }
}

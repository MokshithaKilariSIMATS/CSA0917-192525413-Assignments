import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Task B4 : migrate the legacy text file into the database.
 *   - validate every line (damaged / duplicate lines are REPORTED, not loaded)
 *   - load the valid ones with PreparedStatement batches (1,000 rows) inside ONE transaction
 *   - print DatabaseMetaData: product, driver version, tables
 */
public class Migrate {

    public static void main(String[] args) throws Exception {
        Path src = Paths.get(args.length > 0 ? args[0] : "Task04/data/legacy_passes.txt");
        Path rejectFile = Paths.get("Task04/output/rejected_records.txt");

        List<BusPass> valid = new ArrayList<>();
        List<String> rejects = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int lineNo = 0;
        try (BufferedReader br = Files.newBufferedReader(src)) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                String reason = BusPass.check(line);
                if (reason != null) {
                    rejects.add("line " + lineNo + " | " + reason + " | " + (line.isEmpty() ? "<empty>" : line));
                    continue;
                }
                BusPass p = BusPass.parse(line);
                if (!seen.add(p.passId())) {
                    rejects.add("line " + lineNo + " | duplicate pass id " + p.passId() + " | " + line);
                    continue;
                }
                valid.add(p);
            }
        }

        System.out.println("=== Task B4 : migration  " + src + "  ->  database table bus_pass ===");
        System.out.println("Lines read      : " + lineNo);
        System.out.println("Valid records   : " + valid.size());
        System.out.println("Rejected records: " + rejects.size());
        try (BufferedWriter w = Files.newBufferedWriter(rejectFile)) {
            for (String r : rejects) { w.write(r); w.newLine(); }
        }
        System.out.println("Rejected-record report (" + rejectFile + "):");
        for (String r : rejects) System.out.println("   " + r);

        try (Connection c = Db.open()) {
            Db.recreateBusPass(c);
            long t0 = System.nanoTime();
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(PassLoader.INSERT_SQL)) {
                int pending = 0;
                int batches = 0;
                for (BusPass p : valid) {
                    PassLoader.bind(ps, p);
                    ps.addBatch();
                    if (++pending == PassLoader.BATCH_SIZE) { ps.executeBatch(); pending = 0; batches++; }
                }
                if (pending > 0) { ps.executeBatch(); batches++; }
                c.commit();
                System.out.printf("%nLoaded in %d batches inside ONE transaction, committed. (%.1f ms)%n", batches, (System.nanoTime() - t0) / 1e6);
            } catch (SQLException e) {
                c.rollback();
                System.out.println("Migration FAILED, transaction rolled back: " + e.getMessage());
                throw e;
            }
            c.setAutoCommit(true);

            int rows = Db.count(c, "bus_pass");
            System.out.println("Verification    : rows in table = " + rows + "   valid records read = " + valid.size()
                    + "   -> " + (rows == valid.size() ? "MATCH" : "MISMATCH"));

            DatabaseMetaData md = c.getMetaData();
            System.out.println();
            System.out.println("-- DatabaseMetaData");
            System.out.println("   Database product : " + md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
            System.out.println("   JDBC driver      : " + md.getDriverName() + " " + md.getDriverVersion());
            System.out.println("   JDBC version     : " + md.getJDBCMajorVersion() + "." + md.getJDBCMinorVersion());
            System.out.println("   URL              : " + md.getURL());
            System.out.println("   Tables (user)    :");
            try (ResultSet rs = md.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    System.out.println("      " + rs.getString("TABLE_SCHEM") + "." + rs.getString("TABLE_NAME"));
                }
            }
        } finally {
            Db.shutdownDerby();
        }
    }
}

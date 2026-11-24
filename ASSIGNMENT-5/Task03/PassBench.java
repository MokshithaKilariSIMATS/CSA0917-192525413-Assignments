import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.List;

/**
 * Task B3 : insertion time for 1,000 / 10,000 / 100,000 records using
 * Statement, PreparedStatement and PreparedStatement batches (auto-commit ON), plus
 * the same batches inside ONE transaction and multi-row INSERT.
 */
public class PassBench {

    interface Loader { void run(Connection c, List<BusPass> l) throws Exception; }

    static double time(Connection c, int n, Loader loader) throws Exception {
        Db.recreateBusPass(c);
        List<BusPass> data = PassLoader.generate(n, 100000);
        long t0 = System.nanoTime();
        loader.run(c, data);
        double ms = (System.nanoTime() - t0) / 1e6;
        int rows = Db.count(c, "bus_pass");
        if (rows != n) throw new IllegalStateException("expected " + n + " rows, found " + rows);
        return ms;
    }

    public static void main(String[] args) throws Exception {
        int[] sizes = {1000, 10000, 100000};
        // Statement / PreparedStatement with auto-commit are very slow at 100,000 rows; the cap keeps the run finite.
        long capMs = Long.getLong("cap.ms", 600_000L);

        String[] names = {"Statement (auto-commit)", "PreparedStatement (auto-commit)",
                "Batch 1000 (auto-commit)", "Batch 1000 (ONE transaction)", "Multi-row INSERT x100 (txn)"};
        PassLoader.class.getName();
        try (Connection c = Db.open(); PrintWriter csv = new PrintWriter(Files.newBufferedWriter(Paths.get("Task03/output/insert_results.csv")))) {
            csv.println("rows,method,timeMs");
            System.out.println("=== Task B3 : insertion time (ms) on " + c.getMetaData().getDatabaseProductName() + " " + c.getMetaData().getDatabaseProductVersion() + " ===");
            System.out.printf("%-34s", "Method");
            for (int n : sizes) System.out.printf("%14s", String.format("%,d rows", n));
            System.out.println();

            // warm-up so class loading / JIT is not charged to the first row of the table
            time(c, 2000, PassLoader::loadBatch);
            time(c, 2000, PassLoader::loadPrepared);
            time(c, 500, PassLoader::loadStatement);

            double[][] t = new double[names.length][sizes.length];
            for (int m = 0; m < names.length; m++) {
                for (int s = 0; s < sizes.length; s++) {
                    int n = sizes[s];
                    Loader l = switch (m) {
                        case 0 -> PassLoader::loadStatement;
                        case 1 -> PassLoader::loadPrepared;
                        case 2 -> PassLoader::loadBatch;
                        case 3 -> PassLoader::loadBatchTransaction;
                        default -> PassLoader::loadMultiRow;
                    };
                    // skip a size if the previous (10x smaller) run already projects beyond the cap
                    if (s > 0 && t[m][s - 1] * 10 > capMs) {
                        t[m][s] = Double.NaN;
                        continue;
                    }
                    t[m][s] = time(c, n, l);
                    csv.printf("%d,%s,%.1f%n", n, names[m], t[m][s]);
                }
                System.out.printf("%-34s", names[m]);
                for (int s = 0; s < sizes.length; s++)
                    System.out.print(Double.isNaN(t[m][s]) ? String.format("%14s", "skipped(cap)") : String.format("%,14.1f", t[m][s]));
                System.out.println();
            }

            System.out.println();
            System.out.println("Single-pass counter case: one PreparedStatement INSERT (auto-commit) =");
            Db.recreateBusPass(c);
            List<BusPass> one = PassLoader.generate(200, 900000);
            long t0 = System.nanoTime();
            try (var ps = c.prepareStatement(PassLoader.INSERT_SQL)) {
                for (BusPass p : one) { PassLoader.bind(ps, p); ps.executeUpdate(); }
            }
            System.out.printf("   %.3f ms per pass (mean of 200 individual commits)%n", (System.nanoTime() - t0) / 1e6 / 200);
        } finally {
            Db.shutdownDerby();
        }
    }
}

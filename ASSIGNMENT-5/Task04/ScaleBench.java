import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Task B4 : final engineering decision - measure the expanded system.
 * 12 campuses, 100x the current 8,000 passes/year  =>  800,000 passes (66,667 per campus).
 * Compares text file, Java serialization (one file per campus) and one shared database.
 */
public class ScaleBench {

    static final int N = 800_000, CAMPUSES = 12;

    static long usedHeap() {
        for (int i = 0; i < 3; i++) System.gc();
        Runtime r = Runtime.getRuntime();
        return r.totalMemory() - r.freeMemory();
    }

    static BusPass pass(int i) {
        return new BusPass("P" + (1_000_000 + i), "22CS" + String.format("%05d", i % 90000),
                "Student " + i, 1 + (i % 25), "Stop " + (i % 40),
                LocalDate.parse("2027-03-31"), new BigDecimal("1500.00"));
    }

    static double ms(long t0) { return (System.nanoTime() - t0) / 1e6; }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Task B4 : scale test, " + String.format("%,d", N) + " passes, " + CAMPUSES + " campuses ===");
        System.out.println("JVM max heap = " + Runtime.getRuntime().maxMemory() / 1048576 + " MB");
        Random rnd = new Random(7);
        int Q = 200;
        String[] ids = new String[Q];
        for (int i = 0; i < Q; i++) ids[i] = "P" + (1_000_000 + 1 + rnd.nextInt(N));

        // ------------------------------------------------------------ text file
        Path txt = Paths.get("Task04/data/scale_800k.txt");
        long t0 = System.nanoTime();
        try (BufferedWriter w = Files.newBufferedWriter(txt)) {
            for (int i = 1; i <= N; i++) { w.write(pass(i).toLine()); w.write('\n'); }
        }
        System.out.printf("%n[TEXT]  write %,d records            : %10.1f ms   file = %.1f MB%n", N, ms(t0), Files.size(txt) / 1048576.0);
        PassStore store = new PassStore(txt);
        store.find("P1000001");                                  // warm-up
        t0 = System.nanoTime();
        for (int i = 0; i < 5; i++) store.find(ids[i]);
        double scanAvg = ms(t0) / 5;
        System.out.printf("[TEXT]  scan find(), mean of 5          : %10.1f ms per search%n", scanAvg);
        System.out.printf("[TEXT]  %d searches by scanning (proj.)  : %10.1f s   (= %d x %.1f ms)%n", Q, Q * scanAvg / 1000, Q, scanAvg);
        long base = usedHeap();
        t0 = System.nanoTime();
        PassStoreMap map = new PassStoreMap(txt);
        double mapLoad = ms(t0);
        double mapHeap = (usedHeap() - base) / 1048576.0;
        t0 = System.nanoTime();
        for (String id : ids) map.find(id);
        System.out.printf("[TEXT]  HashMap load                    : %10.1f ms   heap = %.0f MB   then %d lookups = %.2f ms%n", mapLoad, mapHeap, Q, ms(t0));
        map = null;

        // ------------------------------------------------------------ serialization, one file per campus
        List<BusPass> all = new ArrayList<>(N);
        for (int i = 1; i <= N; i++) all.add(pass(i));
        Path serDir = Paths.get("Task04/data/ser");
        Files.createDirectories(serDir);
        int per = N / CAMPUSES;
        t0 = System.nanoTime();
        long serBytes = 0;
        for (int c = 0; c < CAMPUSES; c++) {
            Path f = serDir.resolve("campus" + c + ".ser");
            try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(f)))) {
                oos.writeObject(new ArrayList<>(all.subList(c * per, (c + 1) * per)));
            }
            serBytes += Files.size(f);
        }
        System.out.printf("%n[SERIAL] write %d files (one per campus): %10.1f ms   total = %.1f MB%n", CAMPUSES, ms(t0), serBytes / 1048576.0);
        all = null;
        t0 = System.nanoTime();
        int routeHits = 0;
        for (int c = 0; c < CAMPUSES; c++) {
            try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(serDir.resolve("campus" + c + ".ser"))))) {
                @SuppressWarnings("unchecked")
                List<BusPass> part = (List<BusPass>) ois.readObject();
                for (BusPass p : part) if (p.route() == 12) routeHits++;
            }
        }
        System.out.printf("[SERIAL] cross-campus query 'route 12'    : %10.1f ms   (must load all %d files)  hits = %,d%n", ms(t0), CAMPUSES, routeHits);

        // ------------------------------------------------------------ database
        try (Connection c = Db.open()) {
            Db.recreateBusPass(c);
            List<BusPass> data = new ArrayList<>(N);
            for (int i = 1; i <= N; i++) data.add(pass(i));
            t0 = System.nanoTime();
            PassLoader.loadBatchTransaction(c, data);
            double load = ms(t0);
            data = null;
            System.out.printf("%n[DB]     batch(1000) + 1 transaction load  : %10.1f ms   rows = %,d%n", load, Db.count(c, "bus_pass"));

            // PK lookups
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM bus_pass WHERE pass_id = ?")) {
                for (int i = 0; i < 20; i++) { ps.setString(1, ids[i]); try (ResultSet rs = ps.executeQuery()) { rs.next(); } }   // warm-up
                t0 = System.nanoTime();
                for (String id : ids) {
                    ps.setString(1, id);
                    try (ResultSet rs = ps.executeQuery()) { rs.next(); }
                }
                System.out.printf("[DB]     %d primary-key lookups            : %10.2f ms total = %.3f ms per search%n", Q, ms(t0), ms(t0) / Q);
            }
            // route query: no index, then with index
            String q = "SELECT COUNT(*) FROM bus_pass WHERE route_no = 12";
            try (Statement st = c.createStatement()) {
                t0 = System.nanoTime();
                ResultSet rs = st.executeQuery(q); rs.next();
                System.out.printf("[DB]     'route 12' without index          : %10.1f ms   hits = %,d%n", ms(t0), rs.getInt(1));
                st.executeUpdate("CREATE INDEX idx_route ON bus_pass(route_no)");
                t0 = System.nanoTime();
                rs = st.executeQuery(q); rs.next();
                System.out.printf("[DB]     'route 12' with index             : %10.1f ms   hits = %,d%n", ms(t0), rs.getInt(1));
            }
        } finally {
            Db.shutdownDerby();
        }
    }
}

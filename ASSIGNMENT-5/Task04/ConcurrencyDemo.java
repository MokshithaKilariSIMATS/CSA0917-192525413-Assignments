import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Task B4 : concurrent counters. 8 "counter" threads (think: 8 campuses) each issue 200 passes.
 *   File : naive read-increment-write of a counter file  -> lost updates, duplicate pass numbers
 *   DB   : UPDATE ... SET next_no = next_no + 1 inside a transaction -> every number unique
 */
public class ConcurrencyDemo {

    static final int THREADS = 8, PER_THREAD = 200, TOTAL = THREADS * PER_THREAD;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Task B4 : concurrent pass-number counters (" + THREADS + " threads x " + PER_THREAD + " = " + TOTAL + " passes) ===");
        fileCounter();
        dbCounter();
    }

    static void fileCounter() throws Exception {
        Path counter = Paths.get("Task04/data/counter.txt");
        Path log = Paths.get("Task04/data/issued_file.txt");
        Files.writeString(counter, "0");
        Files.deleteIfExists(log);
        Files.createFile(log);
        List<Integer> issued = java.util.Collections.synchronizedList(new ArrayList<>());

        Runnable job = () -> {
            for (int i = 0; i < PER_THREAD; i++) {
                try {
                    int n = Integer.parseInt(Files.readString(counter).trim());   // read
                    n++;                                                          // increment
                    Files.writeString(counter, Integer.toString(n));              // write (another thread may have run in between)
                    issued.add(n);
                } catch (IOException | NumberFormatException e) {
                    issued.add(-1);   // torn read of a half-written counter file
                }
            }
        };
        run(job);
        int distinct = (int) issued.stream().filter(x -> x > 0).distinct().count();
        int failed = (int) issued.stream().filter(x -> x < 0).count();
        System.out.println();
        System.out.println("[Text counter file, no locking]");
        System.out.println("   passes issued        : " + issued.size());
        System.out.println("   distinct pass numbers: " + distinct);
        System.out.println("   duplicate numbers    : " + (issued.size() - failed - distinct));
        System.out.println("   unreadable (torn) reads: " + failed);
        System.out.println("   final counter value  : " + Files.readString(counter).trim() + "   (should be " + TOTAL + ")");
        Files.deleteIfExists(counter);
        Files.deleteIfExists(log);
    }

    static void dbCounter() throws Exception {
        try (Connection setup = Db.open()) {
            Db.dropIfExists(setup, "pass_counter");
            Db.dropIfExists(setup, "issued_pass");
            try (Statement st = setup.createStatement()) {
                st.executeUpdate("CREATE TABLE pass_counter (id INT NOT NULL PRIMARY KEY, next_no INT NOT NULL)");
                st.executeUpdate("INSERT INTO pass_counter VALUES (1, 0)");
                st.executeUpdate("CREATE TABLE issued_pass (pass_no INT NOT NULL PRIMARY KEY)");   // PK = uniqueness guarantee
            }
        }
        AtomicInteger errors = new AtomicInteger();
        Runnable job = () -> {
            try (Connection c = Db.open()) {
                c.setAutoCommit(false);
                for (int i = 0; i < PER_THREAD; i++) {
                    while (true) {
                        try {
                            try (Statement st = c.createStatement()) {
                                st.executeUpdate("UPDATE pass_counter SET next_no = next_no + 1 WHERE id = 1");   // row lock
                            }
                            int n;
                            try (PreparedStatement ps = c.prepareStatement("SELECT next_no FROM pass_counter WHERE id = 1");
                                 ResultSet rs = ps.executeQuery()) {
                                rs.next();
                                n = rs.getInt(1);
                            }
                            try (PreparedStatement ps = c.prepareStatement("INSERT INTO issued_pass VALUES (?)")) {
                                ps.setInt(1, n);
                                ps.executeUpdate();
                            }
                            c.commit();
                            break;
                        } catch (java.sql.SQLException e) {
                            c.rollback();                       // deadlock victim / lock timeout -> retry the transaction
                            if (!e.getSQLState().startsWith("40") && !"XJ208".equals(e.getSQLState())) {
                                errors.incrementAndGet();
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                errors.incrementAndGet();
            }
        };
        long t0 = System.nanoTime();
        run(job);
        double ms = (System.nanoTime() - t0) / 1e6;
        try (Connection c = Db.open(); Statement st = c.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT COUNT(*), COUNT(DISTINCT pass_no), MAX(pass_no) FROM issued_pass");
            rs.next();
            int total = rs.getInt(1), distinct = rs.getInt(2), max = rs.getInt(3);
            rs = st.executeQuery("SELECT next_no FROM pass_counter WHERE id = 1");
            rs.next();
            System.out.println();
            System.out.println("[Database counter row, transaction + row lock]  (" + String.format("%.0f", ms) + " ms)");
            System.out.println("   passes issued        : " + total);
            System.out.println("   distinct pass numbers: " + distinct);
            System.out.println("   duplicate numbers    : " + (total - distinct));
            System.out.println("   unexpected errors    : " + errors.get());
            System.out.println("   final counter value  : " + rs.getInt(1) + "   (should be " + TOTAL + ", max issued " + max + ")");
        } finally {
            Db.shutdownDerby();
        }
    }

    static void run(Runnable job) throws InterruptedException {
        Thread[] ts = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) { ts[i] = new Thread(job); ts[i].start(); }
        for (Thread t : ts) t.join();
    }
}

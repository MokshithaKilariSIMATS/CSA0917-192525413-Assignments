import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;

/** A reader that counts how many instances are currently open (our "file handle" meter). */
class TrackedReader extends BufferedReader {
    static final AtomicInteger OPEN = new AtomicInteger();
    final String name; private boolean closed;
    TrackedReader(String path) throws FileNotFoundException {
        super(new FileReader(path)); name = path; OPEN.incrementAndGet();
    }
    @Override public void close() throws IOException {
        if (!closed) { closed = true; OPEN.decrementAndGet(); }
        super.close();
    }
}

public class ReportGen {
    static final Logger LOG = Logger.getLogger("report");
    static String lastFailedFile = "-";

    static void makeData(Path dir, boolean corrupt) throws IOException {
        Files.createDirectories(dir);
        for (int s = 1; s <= 5; s++) {
            List<String> lines = new ArrayList<>();
            for (int i = 1; i <= 4; i++) lines.add("item" + i + "," + (s + i) + "," + (10 * i));
            if (corrupt && s == 2) lines.add(2, "item9,abc,10");          // bad quantity  -> NumberFormatException
            if (corrupt && s == 4) lines.add(1, "item8");                 // missing column -> ArrayIndexOutOfBounds
            Files.write(dir.resolve("store" + s + ".txt"), lines);
        }
    }

    static int sumFile(BufferedReader br) throws IOException {
        int total = 0; String line;
        while ((line = br.readLine()) != null) {
            String[] p = line.split(",");
            total += Integer.parseInt(p[1]) * Integer.parseInt(p[2]);
        }
        return total;
    }

    // V1: manual close(), but close() is NOT in finally  -> leaks when sumFile() throws
    static String manualLeaky(Path dir, Path out) throws IOException {
        StringBuilder sb = new StringBuilder(); int grand = 0;
        for (int s = 1; s <= 5; s++) {
            String f = dir.resolve("store" + s + ".txt").toString();
            try {
                TrackedReader br = new TrackedReader(f);
                int t = sumFile(br);
                br.close();                                               // skipped when an exception occurs
                sb.append("store").append(s).append(": ").append(t).append('\n'); grand += t;
            } catch (RuntimeException e) {
                lastFailedFile = f; LOG.warning("FAILED file=" + f + " cause=" + e);
                sb.append("store").append(s).append(": ERROR\n");
            }
        }
        sb.append("TOTAL: ").append(grand).append('\n');
        FileWriter fw = new FileWriter(out.toFile()); fw.write(sb.toString()); fw.close();
        return sb.toString();
    }

    // V2: try-catch-finally with correct manual close()
    static String manualFinally(Path dir, Path out) throws IOException {
        StringBuilder sb = new StringBuilder(); int grand = 0;
        for (int s = 1; s <= 5; s++) {
            String f = dir.resolve("store" + s + ".txt").toString();
            TrackedReader br = null;
            try {
                br = new TrackedReader(f);
                int t = sumFile(br);
                sb.append("store").append(s).append(": ").append(t).append('\n'); grand += t;
            } catch (RuntimeException e) {
                lastFailedFile = f; LOG.warning("FAILED file=" + f + " cause=" + e);
                sb.append("store").append(s).append(": ERROR\n");
            } finally {
                if (br != null) { try { br.close(); } catch (IOException ignore) { } }
            }
        }
        sb.append("TOTAL: ").append(grand).append('\n');
        FileWriter fw = null;
        try { fw = new FileWriter(out.toFile()); fw.write(sb.toString()); }
        finally { if (fw != null) fw.close(); }
        return sb.toString();
    }

    // V3: try-with-resources
    static String twr(Path dir, Path out) throws IOException {
        StringBuilder sb = new StringBuilder(); int grand = 0;
        for (int s = 1; s <= 5; s++) {
            String f = dir.resolve("store" + s + ".txt").toString();
            try (TrackedReader br = new TrackedReader(f)) {
                int t = sumFile(br);
                sb.append("store").append(s).append(": ").append(t).append('\n'); grand += t;
            } catch (RuntimeException e) {
                lastFailedFile = f; LOG.warning("FAILED file=" + f + " cause=" + e);
                sb.append("store").append(s).append(": ERROR\n");
            }
        }
        sb.append("TOTAL: ").append(grand).append('\n');
        try (FileWriter fw = new FileWriter(out.toFile())) { fw.write(sb.toString()); }
        return sb.toString();
    }

    interface Gen { String run(Path d, Path o) throws IOException; }

    static void measure(String label, Gen g, Path dir) throws IOException {
        TrackedReader.OPEN.set(0);
        String r = g.run(dir, Paths.get("summary_tmp.txt"));
        System.out.printf("  %-26s files left open = %d%n", label, TrackedReader.OPEN.get());
    }

    // ---- suppressed exception + closing order demo ----
    static class Res implements AutoCloseable {
        final String n; final boolean failOnClose;
        Res(String n, boolean f) { this.n = n; failOnClose = f; System.out.println("  open  " + n); }
        public void close() { System.out.println("  close " + n); if (failOnClose) throw new IllegalStateException("close failed: " + n); }
    }

    public static void main(String[] args) throws Exception {
        LOG.setUseParentHandlers(false);
        Handler h = new StreamHandler(System.out, new SimpleFormatter() {
            public String format(LogRecord r) { return "  [LOG " + r.getLevel() + "] " + r.getMessage() + "\n"; }
        }) { public synchronized void publish(LogRecord r) { super.publish(r); flush(); } };
        LOG.addHandler(h);

        Path good = Paths.get("data_good"), bad = Paths.get("data_bad");
        makeData(good, false); makeData(bad, true);

        System.out.println("=== 1. Same report from all three versions (good data) ===");
        String a = manualLeaky(good, Paths.get("sum_a.txt"));
        String b = manualFinally(good, Paths.get("sum_b.txt"));
        String c = twr(good, Paths.get("sum_c.txt"));
        System.out.print(c);
        System.out.println("leaky == finally : " + a.equals(b));
        System.out.println("finally == twr   : " + b.equals(c));
        System.out.println("summary files identical: " + (Files.mismatch(Paths.get("sum_a.txt"), Paths.get("sum_c.txt")) == -1));

        System.out.println("\n=== 2. Failing input (store2 and store4 are corrupt): logging + open-file count ===");
        System.out.println("  version 1 - manual close() outside finally:");
        measure("manualLeaky", ReportGen::manualLeaky, bad);
        System.out.println("  version 2 - manual close() in finally:");
        measure("manualFinally", ReportGen::manualFinally, bad);
        System.out.println("  version 3 - try-with-resources:");
        measure("try-with-resources", ReportGen::twr, bad);
        System.out.println("  Output of the try-with-resources version on corrupt data:");
        System.out.print("  " + twr(bad, Paths.get("sum_bad.txt")).replace("\n", "\n  "));
        System.out.println();

        System.out.println("=== 3. Closing order and suppressed exceptions ===");
        try (Res first = new Res("A (opened first)", false); Res second = new Res("B (opened second)", true)) {
            System.out.println("  body runs and throws");
            throw new RuntimeException("body failed");
        } catch (RuntimeException e) {
            System.out.println("  caught: " + e.getMessage());
            for (Throwable s : e.getSuppressed()) System.out.println("  suppressed: " + s.getMessage());
        }
    }
}

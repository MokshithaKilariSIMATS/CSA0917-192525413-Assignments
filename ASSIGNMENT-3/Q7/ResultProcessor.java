import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

class CorruptFileException extends Exception {                     // checked: expected, per-file failure
    CorruptFileException(String m, Throwable c) { super(m, c); }
    CorruptFileException(String m) { super(m); }
}
record Result(String file, String student, int total, boolean pass) { }

public class ResultProcessor {
    static long LATENCY_MS = 1;                                     // simulated storage latency per file

    // ---------- pure business logic: read -> validate -> compute ----------
    static Result process(Path p) throws IOException, CorruptFileException {
        if (LATENCY_MS > 0) { try { Thread.sleep(LATENCY_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException("interrupted", e); } }
        List<String> lines = Files.readAllLines(p);                 // NoSuchFileException (IOException) if missing
        if (lines.isEmpty()) throw new CorruptFileException("empty file");
        String student = null; int total = 0, subjects = 0;
        for (String line : lines) {
            String[] f = line.split(",");
            if (f.length != 3) throw new CorruptFileException("expected 3 fields but found " + f.length);
            try {
                int marks = Integer.parseInt(f[2].trim());
                if (marks < 0 || marks > 100) throw new CorruptFileException("marks out of range: " + marks);
                if (student == null) student = f[0]; else if (!student.equals(f[0])) throw new CorruptFileException("mixed student ids");
                total += marks; subjects++;
            } catch (NumberFormatException e) { throw new CorruptFileException("marks not numeric: " + f[2], e); }
        }
        return new Result(p.getFileName().toString(), student, total, total / subjects >= 40);
    }

    // ---------- the design: ExecutorService + Callable + Future + concurrent collections ----------
    static class Summary {
        final ConcurrentHashMap<String, Result> ok = new ConcurrentHashMap<>();       // thread-safe, no global lock
        final ConcurrentHashMap<String, String> failed = new ConcurrentHashMap<>();   // file -> reason
        int submitted;
        long millis;
        void print() {
            Map<String, Long> byReason = failed.values().stream().collect(Collectors.groupingBy(s -> s.split(":")[0], TreeMap::new, Collectors.counting()));
            long passed = ok.values().stream().filter(Result::pass).count();
            System.out.println("  submitted : " + submitted);
            System.out.println("  succeeded : " + ok.size() + "  (pass " + passed + ", fail " + (ok.size() - passed) + ")");
            System.out.println("  failed    : " + failed.size() + "  " + byReason);
            System.out.println("  check     : succeeded + failed == submitted  ->  " + (ok.size() + failed.size() == submitted));
        }
    }

    static Summary runPool(List<Path> files, int threads) throws Exception {
        Summary s = new Summary(); s.submitted = files.size();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        long t0 = System.nanoTime();
        List<Future<Boolean>> futures = new ArrayList<>();
        for (Path p : files) {
            futures.add(pool.submit(() -> {                          // Callable<Boolean>: true = ok, false = failed
                try { Result r = process(p); s.ok.put(p.toString(), r); return true; }
                catch (NoSuchFileException e)       { s.failed.put(p.toString(), "MISSING: " + e.getMessage()); }
                catch (CorruptFileException e)      { s.failed.put(p.toString(), "CORRUPT: " + e.getMessage()); }
                catch (IOException e)               { s.failed.put(p.toString(), "IO: " + e.getMessage()); }
                catch (RuntimeException e)          { s.failed.put(p.toString(), "BUG: " + e); }   // nothing escapes the task
                return false;
            }));
        }
        for (int i = 0; i < futures.size(); i++) {
            try { futures.get(i).get(); }
            catch (ExecutionException e) { s.failed.put(files.get(i).toString(), "UNEXPECTED: " + e.getCause()); }   // safety net (Errors)
        }
        pool.shutdown();
        s.millis = (System.nanoTime() - t0) / 1_000_000;
        return s;
    }

    // ---------- data generation ----------
    static List<Path> generate(Path dir, int n, double pMissing, double pCorrupt) throws IOException {
        Files.createDirectories(dir);
        Random r = new Random(42); List<Path> files = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Path p = dir.resolve(String.format("script_%05d.txt", i)); files.add(p);
            double x = r.nextDouble();
            if (x < pMissing) continue;                                              // never created
            List<String> lines = new ArrayList<>();
            for (int s = 1; s <= 5; s++) lines.add("S" + i + ",SUB" + s + "," + (20 + r.nextInt(81)));
            if (x < pMissing + pCorrupt) {
                switch (r.nextInt(4)) {
                    case 0 -> lines.set(2, "S" + i + ",SUB3,abc");
                    case 1 -> lines.set(1, "S" + i + ",SUB2,150");
                    case 2 -> lines.set(3, "garbage");
                    default -> lines.clear();
                }
            }
            Files.write(p, lines);
        }
        return files;
    }

    static List<Path> names(Path dir, int n) {
        return IntStream.rangeClosed(1, n).mapToObj(i -> dir.resolve(String.format("script_%05d.txt", i))).collect(Collectors.toList());
    }

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "small";
        if (mode.equals("small")) {
            System.out.println("=== Verification on a hand-checked set of 10 files ===");
            Path d = Paths.get("small"); Files.createDirectories(d);
            String[][] spec = {   // name, content (null = missing)
                {"a1.txt", "S1,M,80\nS1,P,70"}, {"a2.txt", "S2,M,30\nS2,P,20"}, {"a3.txt", "S3,M,100\nS3,P,90"},
                {"a4.txt", "S4,M,55\nS4,P,45"}, {"a5.txt", "S5,M,60\nS5,P,60"}, {"a6.txt", "S6,M,40\nS6,P,40"},
                {"b1.txt", "S7,M,abc\nS7,P,50"}, {"b2.txt", "S8,M,150\nS8,P,50"},
                {"m1.txt", null}, {"m2.txt", null}};
            List<Path> files = new ArrayList<>();
            for (String[] s : spec) { Path p = d.resolve(s[0]); files.add(p); if (s[1] != null) Files.writeString(p, s[1]); else Files.deleteIfExists(p); }
            LATENCY_MS = 0;
            Summary s = runPool(files, 4);
            s.print();
            long pass = s.ok.values().stream().filter(Result::pass).count();
            boolean good = s.submitted == 10 && s.ok.size() == 6 && s.failed.size() == 4 && pass == 5;
            System.out.println("  hand-checked expectation: 6 succeeded (5 pass, 1 fail), 2 CORRUPT, 2 MISSING");
            System.out.println("  failed detail:"); new TreeMap<>(s.failed).forEach((k, v) -> System.out.println("    " + Paths.get(k).getFileName() + " -> " + v));
            System.out.println("  RESULT: " + (good ? "PASS - counts match" : "FAIL"));
        } else if (mode.equals("gen")) {
            List<Path> f = generate(Paths.get("data"), 10000, 0.02, 0.03);
            System.out.println("generated " + f.size() + " names; files present: " + Files.list(Paths.get("data")).count());
        } else if (mode.equals("bench")) {
            List<Path> files = names(Paths.get("data"), 10000);
            LATENCY_MS = 0; runPool(files.subList(0, 2000), 4);          // warm-up
            LATENCY_MS = 1;
            System.out.println("10,000 files, 1 ms simulated storage latency per file, " + Runtime.getRuntime().availableProcessors() + " CPU core(s)");
            System.out.printf("%-10s %-12s %-12s %-12s %-10s%n", "threads", "run 1 (ms)", "run 2 (ms)", "mean (ms)", "speed-up");
            double base = 0; Summary last = null;
            for (int t : new int[]{1, 2, 4, 8, 16, 32, 64}) {
                Summary a = runPool(files, t); Summary b = (t == 1) ? a : runPool(files, t);
                double mean = (a.millis + b.millis) / 2.0; if (t == 1) base = mean; last = b;
                System.out.printf("%-10d %-12d %-12d %-12.0f %-10.2f%n", t, a.millis, b.millis, mean, base / mean);
            }
            System.out.println("\nFull run summary (identical for every pool size):"); last.print();
        }
    }
}

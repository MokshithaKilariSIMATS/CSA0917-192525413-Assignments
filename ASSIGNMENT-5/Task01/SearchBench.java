import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Random;

/** Task B1: pass-search time as the file grows 1,000 -> 100,000 records; scan-per-find vs HashMap. */
public class SearchBench {

    static void generate(Path f, int n) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(f)) {
            for (int i = 1; i <= n; i++) {
                BusPass p = new BusPass("P" + (100000 + i), "22CS" + String.format("%05d", i % 90000),
                        "Student " + i, 1 + (i % 25), "Stop " + (i % 40),
                        LocalDate.parse("2027-03-31"), new BigDecimal("1500.00"));
                w.write(p.toLine());
                w.write('\n');
            }
        }
    }

    static long usedHeap() {
        for (int i = 0; i < 3; i++) System.gc();
        Runtime r = Runtime.getRuntime();
        return r.totalMemory() - r.freeMemory();
    }

    public static void main(String[] args) throws IOException {
        int[] sizes = {1000, 5000, 10000, 25000, 50000, 100000};
        int q = 100;                                   // number of searches in the multi-query experiment
        Random rnd = new Random(42);
        PrintWriter csv = new PrintWriter(Files.newBufferedWriter(Paths.get("Task01/output/search_results.csv")));
        csv.println("n,scanWorstMs,scanAvgMs,mapLoadMs,mapQueryTotalMs,scanQTotalMs,mapHeapMB");

        System.out.println("=== Task B1 : search time vs file size (q = " + q + " random searches) ===");
        System.out.printf("%8s | %12s %12s | %12s %12s %12s | %9s%n",
                "n", "scan worst", "scan avg", "scan q total", "map load", "map q total", "map heap");
        System.out.printf("%8s | %12s %12s | %12s %12s %12s | %9s%n", "", "(ms)", "(ms)", "(ms)", "(ms)", "(ms)", "(MB)");

        // warm-up
        Path warm = Paths.get("Task01/data/bench_warm.txt");
        generate(warm, 20000);
        new PassStore(warm).find("P120000");
        Files.delete(warm);

        for (int n : sizes) {
            Path f = Paths.get("Task01/data/bench_" + n + ".txt");
            generate(f, n);
            PassStore store = new PassStore(f);
            String last = "P" + (100000 + n);

            // q random searches by scanning
            String[] ids = new String[q];
            for (int i = 0; i < q; i++) ids[i] = "P" + (100000 + 1 + rnd.nextInt(n));
            long t0 = System.nanoTime();
            for (String id : ids) store.find(id);
            double scanQ = (System.nanoTime() - t0) / 1e6;

            // worst case (last record) is measured AFTER the q scans, when the scan code is fully JIT-compiled

            // worst case: last record
            double[] w = new double[5];
            for (int r = 0; r < w.length; r++) {
                long tw = System.nanoTime();
                store.find(last);
                w[r] = (System.nanoTime() - tw) / 1e6;
            }
            java.util.Arrays.sort(w);
            double worst = w[w.length / 2];                  // median of 5 runs

            // HashMap: load once, then q lookups
            long base = usedHeap();
            t0 = System.nanoTime();
            PassStoreMap map = new PassStoreMap(f);
            double load = (System.nanoTime() - t0) / 1e6;
            double heapMB = (usedHeap() - base) / 1048576.0;
            t0 = System.nanoTime();
            for (String id : ids) map.find(id);
            double mapQ = (System.nanoTime() - t0) / 1e6;

            System.out.printf("%,8d | %12.2f %12.2f | %12.1f %12.1f %12.3f | %9.1f%n",
                    n, worst, scanQ / q, scanQ, load, mapQ, heapMB);
            csv.printf("%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.2f%n", n, worst, scanQ / q, load, mapQ, scanQ, heapMB);
            Files.delete(f);
            if (map.size() != n) System.out.println("   !! unexpected map size " + map.size());
        }
        csv.close();
        System.out.println();
        System.out.println("scan worst = find() of the LAST record; scan avg = mean of q random scans.");
        System.out.println("Break-even: HashMap wins once (q x scan avg) exceeds its one-time load time.");
    }
}

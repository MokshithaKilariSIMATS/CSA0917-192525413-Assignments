import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.BufferedWriter;
import java.util.List;
import java.util.Arrays;

/**
 * Tasks A2 + B1: count lines of an attendance log with different I/O strategies and measure
 * time, number of read() calls that reach the underlying FileInputStream, and retained heap.
 */
public class LineCountBench {

    /** counts how many read calls actually reach the underlying file stream */
    static class CountingIn extends FilterInputStream {
        long calls;
        CountingIn(InputStream in) { super(in); }
        @Override public int read() throws IOException { calls++; return super.read(); }
        @Override public int read(byte[] b, int o, int l) throws IOException { calls++; return super.read(b, o, l); }
    }

    record Result(long lines, long readCalls, double ms) {}

    static Result unbuffered(Path f) throws IOException {
        long t0 = System.nanoTime();
        long lines = 0;
        try (CountingIn in = new CountingIn(new FileInputStream(f.toFile()))) {
            int b;
            while ((b = in.read()) != -1) if (b == '\n') lines++;
            return new Result(lines, in.calls, (System.nanoTime() - t0) / 1e6);
        }
    }

    static Result bufferedStream(Path f) throws IOException {
        long t0 = System.nanoTime();
        long lines = 0;
        CountingIn raw = new CountingIn(new FileInputStream(f.toFile()));
        try (BufferedInputStream in = new BufferedInputStream(raw)) {   // default 8 KB buffer
            int b;
            while ((b = in.read()) != -1) if (b == '\n') lines++;
        }
        return new Result(lines, raw.calls, (System.nanoTime() - t0) / 1e6);
    }

    static Result byteArray(Path f, int size) throws IOException {
        long t0 = System.nanoTime();
        long lines = 0;
        byte[] buf = new byte[size];
        try (CountingIn in = new CountingIn(new FileInputStream(f.toFile()))) {
            int n;
            while ((n = in.read(buf)) > 0) for (int i = 0; i < n; i++) if (buf[i] == '\n') lines++;
            return new Result(lines, in.calls, (System.nanoTime() - t0) / 1e6);
        }
    }

    static Result bufferedReader(Path f) throws IOException {
        long t0 = System.nanoTime();
        long lines = 0;
        try (BufferedReader br = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            while (br.readLine() != null) lines++;
        }
        return new Result(lines, -1, (System.nanoTime() - t0) / 1e6);
    }

    static long usedHeap() {
        Runtime rt = Runtime.getRuntime();
        for (int i = 0; i < 3; i++) { System.gc(); }
        return rt.totalMemory() - rt.freeMemory();
    }

    static void generate(Path f, long targetBytes) throws IOException {
        if (Files.exists(f) && Files.size(f) >= targetBytes) return;
        try (BufferedWriter w = Files.newBufferedWriter(f)) {
            long written = 0;
            int i = 0;
            while (written < targetBytes) {
                String line = "2026-11-19,22CS" + String.format("%05d", i % 90000) + ",P" + (i % 3) + ",PRESENT";
                w.write(line);
                w.write('\n');
                written += line.length() + 1;
                i++;
            }
        }
    }

    static double median(double[] v) {
        double[] c = v.clone();
        Arrays.sort(c);
        return c[c.length / 2];
    }

    public static void main(String[] args) throws IOException {
        Path dir = Paths.get("Task01/data");
        int[] sizesMB = {1, 10, 50};
        PrintWriter csv = new PrintWriter(Files.newBufferedWriter(Paths.get("Task01/output/linecount_results.csv")));
        csv.println("sizeMB,method,lines,readCalls,timeMs");

        System.out.println("=== Tasks A2/B1 : line-counting benchmark (median of 3 runs; unbuffered 50 MB = 1 run) ===");
        System.out.printf("%-6s %-26s %12s %14s %10s%n", "SizeMB", "Method", "Lines", "read() calls", "Time(ms)");
        for (int mb : sizesMB) {
            Path f = dir.resolve("attendance_" + mb + "MB.log");
            generate(f, mb * 1024L * 1024L);
            long bytes = Files.size(f);
            // warm-up so the JIT is not charged to the first measured method
            byteArray(f, 65536);

            String[] names = {"unbuffered FileInputStream", "BufferedInputStream(8KB)", "byte[] 8 KB", "byte[] 64 KB", "BufferedReader.readLine"};
            for (String name : names) {
                int runs = (name.startsWith("unbuffered") && mb == 50) ? 1 : 3;
                double[] t = new double[runs];
                Result last = null;
                for (int r = 0; r < runs; r++) {
                    last = switch (name) {
                        case "unbuffered FileInputStream" -> unbuffered(f);
                        case "BufferedInputStream(8KB)" -> bufferedStream(f);
                        case "byte[] 8 KB" -> byteArray(f, 8192);
                        case "byte[] 64 KB" -> byteArray(f, 65536);
                        default -> bufferedReader(f);
                    };
                    t[r] = last.ms();
                }
                double med = median(t);
                System.out.printf("%-6d %-26s %,12d %,14d %10.1f%n", mb, name, last.lines(), last.readCalls(), med);
                csv.printf("%d,%s,%d,%d,%.1f%n", mb, name, last.lines(), last.readCalls(), med);
            }

            // Files.readAllLines : time + memory retained by the List<String>
            double[] t = new double[3];
            long retained = 0;
            long lines = 0;
            for (int r = 0; r < 3; r++) {
                long base = usedHeap();
                long t0 = System.nanoTime();
                List<String> all = Files.readAllLines(f);
                t[r] = (System.nanoTime() - t0) / 1e6;
                lines = all.size();
                retained = usedHeap() - base;
                if (all.isEmpty()) System.out.println();
            }
            System.out.printf("%-6d %-26s %,12d %14s %10.1f   retained heap = %.1f MB%n",
                    mb, "Files.readAllLines", lines, "n/a", median(t), retained / 1048576.0);
            csv.printf("%d,Files.readAllLines,%d,-1,%.1f%n", mb, lines, median(t));
            csv.printf("%d,readAllLines_heapMB,%d,-1,%.1f%n", mb, lines, retained / 1048576.0);

            // streaming count for comparison of memory
            long base = usedHeap();
            long t0 = System.nanoTime();
            long cnt;
            try (var s = Files.lines(f)) { cnt = s.count(); }
            double ms = (System.nanoTime() - t0) / 1e6;
            System.out.printf("%-6d %-26s %,12d %14s %10.1f   retained heap = %.1f MB%n",
                    mb, "Files.lines().count()", cnt, "n/a", ms, (usedHeap() - base) / 1048576.0);
            csv.printf("%d,Files.lines.count,%d,-1,%.1f%n", mb, cnt, ms);
            System.out.println("       (file size " + bytes + " bytes; predicted unbuffered read() calls = bytes + 1 = " + String.format("%,d", bytes + 1) + ")");
            System.out.println();
        }
        csv.close();
    }
}

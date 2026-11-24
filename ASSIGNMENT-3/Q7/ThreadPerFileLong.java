import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class ThreadPerFileLong {
    public static void main(String[] a) throws Exception {
        ResultProcessor.LATENCY_MS = 200;                    // realistic network-disk latency
        AtomicInteger ok = new AtomicInteger(), bad = new AtomicInteger();
        Thread[] ts = new Thread[10000]; int started = 0; long t0 = System.nanoTime();
        try {
            for (int i = 0; i < ts.length; i++) {
                final Path p = Paths.get("data", String.format("script_%05d.txt", i + 1));
                ts[i] = new Thread(() -> { try { ResultProcessor.process(p); ok.incrementAndGet(); } catch (Exception e) { bad.incrementAndGet(); } });
                ts[i].start(); started++;
            }
        } catch (OutOfMemoryError e) { System.out.println("OutOfMemoryError after " + started + " threads: " + e.getMessage()); }
        for (int i = 0; i < started; i++) ts[i].join();
        System.out.println("200 ms latency, thread-per-file: " + (System.nanoTime() - t0) / 1_000_000 + " ms, started=" + started
            + ", ok=" + ok + ", failed=" + bad + ", peak live threads=" + ManagementFactory.getThreadMXBean().getPeakThreadCount());
        Runtime r = Runtime.getRuntime();
        System.out.println("heap used = " + (r.totalMemory() - r.freeMemory()) / 1_000_000 + " MB; reserved thread stacks ~ peak x 1 MB (virtual)");
    }
}

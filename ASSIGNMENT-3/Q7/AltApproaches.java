import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

/** Alternative concepts for Q7: parallelStream() and one thread per file. */
public class AltApproaches {
    public static void main(String[] args) throws Exception {
        List<Path> files = IntStream.rangeClosed(1, 10000).mapToObj(i -> Paths.get("data", String.format("script_%05d.txt", i))).collect(Collectors.toList());
        ResultProcessor.LATENCY_MS = 1;
        System.out.println("CPU cores = " + Runtime.getRuntime().availableProcessors() + ", ForkJoin common pool parallelism = " + ForkJoinPool.commonPool().getParallelism());

        // ---- parallel stream: failure handled INSIDE the lambda (an escaping exception would abort the whole stream) ----
        AtomicInteger ok = new AtomicInteger(), bad = new AtomicInteger();
        long t0 = System.nanoTime();
        files.parallelStream().forEach(p -> {
            try { ResultProcessor.process(p); ok.incrementAndGet(); } catch (Exception e) { bad.incrementAndGet(); }
        });
        long ps = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("\nparallelStream()   : " + ps + " ms, ok=" + ok + ", failed=" + bad + ", threads used = common pool (" + ForkJoinPool.commonPool().getParallelism() + " + caller)");

        // ---- parallel stream, unprotected: one bad file aborts the stream ----
        AtomicInteger seen = new AtomicInteger();
        try {
            files.parallelStream().forEach(p -> { try { ResultProcessor.process(p); seen.incrementAndGet(); } catch (Exception e) { throw new RuntimeException(e); } });
        } catch (RuntimeException e) { System.out.println("parallelStream() with exception NOT caught inside lambda -> stream aborted after " + seen + " of 10000 files: " + e.getCause().getClass().getSimpleName()); }

        // ---- one thread per file ----
        ok.set(0); bad.set(0);
        Thread[] ts = new Thread[files.size()];
        long t1 = System.nanoTime();
        int started = 0;
        try {
            for (int i = 0; i < ts.length; i++) {
                final Path p = files.get(i);
                ts[i] = new Thread(() -> { try { ResultProcessor.process(p); ok.incrementAndGet(); } catch (Exception e) { bad.incrementAndGet(); } });
                ts[i].start(); started++;
            }
        } catch (OutOfMemoryError e) { System.out.println("OutOfMemoryError after starting " + started + " threads: " + e.getMessage()); }
        for (int i = 0; i < started; i++) ts[i].join();
        long tt = (System.nanoTime() - t1) / 1_000_000;
        int peak = ManagementFactory.getThreadMXBean().getPeakThreadCount();
        System.out.println("thread-per-file    : " + tt + " ms, ok=" + ok + ", failed=" + bad + ", started=" + started + ", peak live threads = " + peak);
        System.out.println("                     reserved stack = peak x 1 MB (default -Xss) ~ " + peak + " MB virtual; fixed pool of 8 = ~8 MB");
    }
}

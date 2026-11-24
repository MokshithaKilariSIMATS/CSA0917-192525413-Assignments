import java.util.concurrent.*;
import java.util.*;

public class PoolStations {
    static long run(int stations, int pool, int scripts) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(pool);
        long t0 = System.nanoTime();
        List<Future<Integer>> f = new ArrayList<>();
        for (int s = 0; s < stations; s++)
            f.add(ex.submit(() -> { for (int i = 0; i < scripts; i++) Thread.sleep(5); return scripts; }));
        int total = 0; for (Future<Integer> x : f) total += x.get();
        ex.shutdown();
        return (System.nanoTime() - t0) / 1_000_000;
    }
    public static void main(String[] args) throws Exception {
        System.out.println("4 stations x 25 scripts, 5 ms per script (sleep)");
        System.out.printf("%-34s %s%n", "approach", "time (ms)");
        long t0 = System.nanoTime();
        Thread[] th = new Thread[4];
        for (int i = 0; i < 4; i++) { th[i] = new Thread(() -> { try { for (int k = 0; k < 25; k++) Thread.sleep(5); } catch (InterruptedException e) { } }); th[i].start(); }
        for (Thread t : th) t.join();
        System.out.printf("%-34s %d%n", "4 hand-made Thread objects", (System.nanoTime() - t0) / 1_000_000);
        for (int p : new int[]{1, 2, 4}) System.out.printf("%-34s %d%n", "ExecutorService, pool = " + p, run(4, p, 25));
    }
}

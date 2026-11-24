import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;

/** Reliable reproduction of double booking + the strategies compared. */
public class Race {
    interface Booker { boolean book(int seat); int sold(); }

    static class Unsafe implements Booker {                 // widened race window (sleep between check and set)
        boolean[] b = new boolean[10]; int sold; final boolean widen;
        Unsafe(boolean w) { widen = w; }
        public boolean book(int s) {
            if (!b[s]) { if (widen) { try { Thread.sleep(1); } catch (InterruptedException e) { } } b[s] = true; sold++; return true; }
            return false;
        }
        public int sold() { return sold; }
    }
    static class SyncMethod implements Booker {
        boolean[] b; int sold; SyncMethod(int n) { b = new boolean[n]; }
        public synchronized boolean book(int s) { if (!b[s]) { b[s] = true; sold++; return true; } return false; }
        public int sold() { return sold; }
    }
    static class SyncBlock implements Booker {
        boolean[] b; int sold; final Object lock = new Object(); SyncBlock(int n) { b = new boolean[n]; }
        public boolean book(int s) {
            // work that needs no lock (validation, logging) stays outside
            if (s < 0 || s >= b.length) return false;
            synchronized (lock) { if (!b[s]) { b[s] = true; sold++; return true; } }
            return false;
        }
        public int sold() { return sold; }
    }
    static class Atomic implements Booker {
        AtomicBoolean[] b; AtomicInteger sold = new AtomicInteger();
        Atomic(int n) { b = new AtomicBoolean[n]; for (int i = 0; i < n; i++) b[i] = new AtomicBoolean(); }
        public boolean book(int s) { if (b[s].compareAndSet(false, true)) { sold.incrementAndGet(); return true; } return false; }
        public int sold() { return sold.get(); }
    }
    static class Locked implements Booker {
        boolean[] b; int sold; final ReentrantLock l = new ReentrantLock(); Locked(int n) { b = new boolean[n]; }
        public boolean book(int s) { l.lock(); try { if (!b[s]) { b[s] = true; sold++; return true; } return false; } finally { l.unlock(); } }
        public int sold() { l.lock(); try { return sold; } finally { l.unlock(); } }
    }
    static class CHM implements Booker {
        ConcurrentHashMap<Integer, Boolean> m = new ConcurrentHashMap<>();
        public boolean book(int s) { return m.putIfAbsent(s, Boolean.TRUE) == null; }
        public int sold() { return m.size(); }
    }

    static int storm(Booker bk, int users, int seats) throws InterruptedException {
        CountDownLatch go = new CountDownLatch(1);
        Thread[] t = new Thread[users];
        for (int i = 0; i < users; i++) { final int seat = i % seats; t[i] = new Thread(() -> { try { go.await(); } catch (InterruptedException e) { } bk.book(seat); }); t[i].start(); }
        go.countDown();
        for (Thread x : t) x.join();
        return bk.sold();
    }

    static long bench(Booker bk, int threads, int perThread, int seats) throws InterruptedException {
        Thread[] t = new Thread[threads]; long t0 = System.nanoTime();
        for (int i = 0; i < threads; i++) { final int id = i; t[i] = new Thread(() -> { for (int k = 0; k < perThread; k++) bk.book((id * 31 + k) % seats); }); t[i].start(); }
        for (Thread x : t) x.join();
        return (System.nanoTime() - t0) / ((long) threads * perThread);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== A. Reliable reproduction: 100 users, 10 seats, 1 ms race window, 10 runs ===");
        int bad = 0;
        for (int r = 1; r <= 10; r++) { int s = storm(new Unsafe(true), 100, 10); if (s != 10) bad++; System.out.printf("  run %2d: seats sold = %3d  (double bookings = %d)%n", r, s, s - 10); }
        System.out.println("  runs with double booking: " + bad + " / 10");
        System.out.println("\n=== B. Same storm on every synchronized/atomic strategy (10 runs each) ===");
        Booker[] ignore = {};
        String[] names = {"synchronized method", "synchronized block", "AtomicBoolean[] CAS", "ReentrantLock", "ConcurrentHashMap"};
        for (int k = 0; k < 5; k++) {
            int wrong = 0, mx = 0;
            for (int r = 0; r < 10; r++) {
                Booker bk = k == 0 ? new SyncMethod(10) : k == 1 ? new SyncBlock(10) : k == 2 ? new Atomic(10) : k == 3 ? new Locked(10) : new CHM();
                int s = storm(bk, 100, 10); mx = Math.max(mx, s); if (s != 10) wrong++;
            }
            System.out.printf("  %-22s max sold = %d, runs with wrong count = %d / 10%n", names[k], mx, wrong);
        }
        System.out.println("\n=== C. Time per booking under contention (ns), 8 threads x 200,000 bookings, 1000 seats ===");
        for (int w = 0; w < 2; w++) { bench(new SyncMethod(1000), 8, 100000, 1000); bench(new Atomic(1000), 8, 100000, 1000); }
        for (int k = 0; k < 5; k++) {
            long best = Long.MAX_VALUE;
            for (int r = 0; r < 3; r++) {
                Booker bk = k == 0 ? new SyncMethod(1000) : k == 1 ? new SyncBlock(1000) : k == 2 ? new Atomic(1000) : k == 3 ? new Locked(1000) : new CHM();
                best = Math.min(best, bench(bk, 8, 200000, 1000));
            }
            System.out.printf("  %-22s %5d ns/booking%n", names[k], best);
        }
        System.out.println("  (JVM sees " + Runtime.getRuntime().availableProcessors() + " CPU core(s))");
    }
}

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class StationThread extends Thread {                       // way 1: extend Thread
    static final AtomicInteger TOTAL = new AtomicInteger();
    static final Queue<String> ORDER = new ConcurrentLinkedQueue<>();
    final int scripts; int scanned;
    StationThread(String name, int scripts) { super(name); this.scripts = scripts; }
    @Override public void run() {
        try { for (int i = 0; i < scripts; i++) { Thread.sleep(5); scanned++; TOTAL.incrementAndGet(); } }
        catch (InterruptedException e) { return; }
        ORDER.add(getName());
    }
}

class StationTask implements Runnable {                    // way 2: implement Runnable
    final int scripts; int scanned;
    StationTask(int scripts) { this.scripts = scripts; }
    @Override public void run() {
        try { for (int i = 0; i < scripts; i++) { Thread.sleep(5); scanned++; StationThread.TOTAL.incrementAndGet(); } }
        catch (InterruptedException e) { return; }
        StationThread.ORDER.add(Thread.currentThread().getName());
    }
}

public class Stations {
    public static void main(String[] args) throws InterruptedException {
        boolean verbose = args.length == 0;
        int n = 25;
        // ---- Version 1: extends Thread ----
        StationThread[] st = new StationThread[4];
        for (int i = 0; i < 4; i++) st[i] = new StationThread("Station-" + (i + 1), n);
        if (verbose) System.out.println("-- extends Thread --\nstate before start : " + st[0].getState() + ", isAlive=" + st[0].isAlive());
        for (StationThread t : st) t.start();
        if (verbose) { Thread.sleep(20); System.out.println("state while running: " + st[0].getState() + ", isAlive=" + st[0].isAlive()); }
        for (StationThread t : st) t.join();
        if (verbose) System.out.println("state after join   : " + st[0].getState() + ", isAlive=" + st[0].isAlive());
        int v1 = StationThread.TOTAL.get();
        String order = String.join(" ", StationThread.ORDER);
        if (!verbose) { System.out.println(order); return; }
        System.out.println("finish order: " + order + "\nTotal scanned (after join): " + v1);

        // ---- Version 2: implements Runnable ----
        StationThread.TOTAL.set(0); StationThread.ORDER.clear();
        StationTask[] tasks = new StationTask[4]; Thread[] th = new Thread[4];
        for (int i = 0; i < 4; i++) { tasks[i] = new StationTask(n); th[i] = new Thread(tasks[i], "Runnable-" + (i + 1)); }
        System.out.println("\n-- implements Runnable --");
        for (Thread t : th) t.start();
        for (Thread t : th) t.join();
        System.out.println("finish order: " + String.join(" ", StationThread.ORDER) + "\nTotal scanned (after join): " + StationThread.TOTAL.get());

        // ---- without join: total printed too early ----
        StationThread.TOTAL.set(0);
        StationThread[] x = new StationThread[4];
        for (int i = 0; i < 4; i++) { x[i] = new StationThread("X-" + i, n); x[i].start(); }
        System.out.println("\n-- WITHOUT join() --\nTotal printed immediately: " + StationThread.TOTAL.get() + "  (wrong, expected " + 4 * n + ")");
        for (StationThread t : x) t.join();

        // ---- run() vs start() ----
        System.out.println("\n-- run() vs start() --");
        Thread probe = new Thread(() -> System.out.println("  body executed on thread: " + Thread.currentThread().getName()), "Probe-1");
        probe.run();
        Thread probe2 = new Thread(() -> System.out.println("  body executed on thread: " + Thread.currentThread().getName()), "Probe-2");
        probe2.start(); probe2.join();
        System.out.println("  (run() executed on main - no new thread was created)");

        // ---- life cycle states observed ----
        System.out.println("\n-- life-cycle states of one station --");
        Thread life = new Thread(() -> { try { Thread.sleep(50); } catch (InterruptedException e) { } }, "Life");
        System.out.println("  after new       : " + life.getState());
        life.start(); Thread.sleep(10);
        System.out.println("  inside sleep()  : " + life.getState());
        Object lock = new Object();
        Thread blocked;
        synchronized (lock) {
            blocked = new Thread(() -> { synchronized (lock) { } }, "Blk");
            blocked.start(); Thread.sleep(20);
            System.out.println("  waiting on lock : " + blocked.getState());
        }
        life.join(); blocked.join();
        System.out.println("  after finishing : " + life.getState());
    }
}

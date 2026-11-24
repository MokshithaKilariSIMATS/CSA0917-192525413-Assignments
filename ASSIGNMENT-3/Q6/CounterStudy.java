import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/** Student A experiments: size trace, notify() vs notifyAll(), and the BlockingQueue alternative. */
public class CounterStudy {
    // generic bounded counter; 'all' selects notifyAll() or notify()
    static class Counter {
        final int[] buf; int count, in, out, min = 99, max = -1; final boolean all; final List<Integer> trace = new ArrayList<>();
        Counter(int cap, boolean all) { buf = new int[cap]; this.all = all; }
        synchronized void put(int v) throws InterruptedException {
            while (count == buf.length) wait();
            buf[in] = v; in = (in + 1) % buf.length; count++; rec();
            if (all) notifyAll(); else notify();
        }
        synchronized int take() throws InterruptedException {
            while (count == 0) wait();
            int v = buf[out]; out = (out + 1) % buf.length; count--; rec();
            if (all) notifyAll(); else notify();
            return v;
        }
        void rec() { min = Math.min(min, count); max = Math.max(max, count); if (trace.size() < 4000) trace.add(count); }
    }

    // returns true if every thread finished within the timeout
    static boolean run(Counter c, int producers, int consumers, int perProducer, long timeoutMs) throws Exception {
        List<Thread> ts = new ArrayList<>();
        int totalTokens = producers * perProducer, perConsumer = totalTokens / consumers;
        for (int p = 0; p < producers; p++) { Thread t = new Thread(() -> { try { for (int i = 0; i < perProducer; i++) c.put(i); } catch (InterruptedException e) { } }); t.setDaemon(true); ts.add(t); }
        for (int q = 0; q < consumers; q++) { Thread t = new Thread(() -> { try { for (int i = 0; i < perConsumer; i++) c.take(); } catch (InterruptedException e) { } }); t.setDaemon(true); ts.add(t); }
        for (Thread t : ts) t.start();
        long end = System.currentTimeMillis() + timeoutMs;
        for (Thread t : ts) t.join(Math.max(1, end - System.currentTimeMillis()));
        return ts.stream().noneMatch(Thread::isAlive);
    }

    static long benchQueue(int total) throws Exception {
        ArrayBlockingQueue<Integer> q = new ArrayBlockingQueue<>(5);
        Thread p = new Thread(() -> { try { for (int i = 0; i < total; i++) q.put(i); } catch (InterruptedException e) { } });
        Thread[] c = new Thread[2];
        for (int i = 0; i < 2; i++) { c[i] = new Thread(() -> { try { for (int k = 0; k < total / 2; k++) q.take(); } catch (InterruptedException e) { } }); }
        long t0 = System.nanoTime(); p.start(); for (Thread t : c) t.start(); p.join(); for (Thread t : c) t.join();
        return (System.nanoTime() - t0) / total;
    }
    static long benchWait(int total) throws Exception {
        Counter k = new Counter(5, true); long t0 = System.nanoTime();
        run(k, 1, 2, total, 60000);
        return (System.nanoTime() - t0) / total;
    }

    // tryLock-with-timeout transfer (alternative to lock ordering)
    static class Acct { final ReentrantLock l = new ReentrantLock(); int bal = 1000; }
    static int retries;
    static synchronized void addRetry() { retries++; }
    static void transferTry(Acct a, Acct b, int amt) throws InterruptedException {
        while (true) {
            if (a.l.tryLock(1, TimeUnit.MILLISECONDS)) {
                try {
                    if (b.l.tryLock(1, TimeUnit.MILLISECONDS)) {
                        try { a.bal -= amt; b.bal += amt; return; } finally { b.l.unlock(); }
                    }
                } finally { a.l.unlock(); }
            }
            addRetry(); Thread.sleep(ThreadLocalRandom.current().nextInt(1, 3));   // back off, then retry
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== A. Counter size over time (1 cook, 2 students, 20,000 tokens, capacity 5) ===");
        Counter c = new Counter(5, true);
        System.out.println("finished: " + run(c, 1, 2, 20000, 20000));
        System.out.println("operations recorded : " + c.trace.size() + " (first 4000 samples kept)");
        System.out.println("minimum count seen  : " + c.min);
        System.out.println("maximum count seen  : " + c.max);
        System.out.println("first 40 samples    : " + c.trace.subList(0, 40));
        System.out.println("end state count     : " + c.count + " (all tokens consumed)");

        System.out.println("\n=== B. notify() vs notifyAll() ===");
        System.out.println("B1 design as submitted: 1 cook, 2 students, capacity 5, notify():");
        int ok = 0; for (int r = 0; r < 20; r++) if (run(new Counter(5, false), 1, 2, 2000, 3000)) ok++;
        System.out.println("   completed " + ok + " / 20 runs (the waiting set holds only cooks OR only students at any moment)");
        System.out.println("B2 stress design: 2 cooks, 2 students, capacity 1, notify():");
        ok = 0; for (int r = 0; r < 20; r++) if (run(new Counter(1, false), 2, 2, 2000, 3000)) ok++;
        System.out.println("   completed " + ok + " / 20 runs, hung " + (20 - ok) + " / 20 runs");
        System.out.println("B3 same stress design with notifyAll():");
        ok = 0; for (int r = 0; r < 20; r++) if (run(new Counter(1, true), 2, 2, 2000, 3000)) ok++;
        System.out.println("   completed " + ok + " / 20 runs, hung " + (20 - ok) + " / 20 runs");

        System.out.println("\n=== C. Alternative designs ===");
        for (int w = 0; w < 2; w++) { benchWait(50000); benchQueue(50000); }
        long w1 = Long.MAX_VALUE, q1 = Long.MAX_VALUE;
        for (int r = 0; r < 3; r++) { w1 = Math.min(w1, benchWait(200000)); q1 = Math.min(q1, benchQueue(200000)); }
        System.out.println("wait()/notifyAll() counter : " + w1 + " ns per token (put+take)");
        System.out.println("ArrayBlockingQueue(5)      : " + q1 + " ns per token (put+take)");

        Acct a = new Acct(), b = new Acct();
        Thread[] t = new Thread[4];
        for (int i = 0; i < 4; i++) { final int id = i; t[i] = new Thread(() -> { try { for (int k = 0; k < 500; k++) { if (id % 2 == 0) transferTry(a, b, 1); else transferTry(b, a, 1); } } catch (InterruptedException e) { } }); }
        long t0 = System.nanoTime(); for (Thread x : t) x.start(); for (Thread x : t) x.join(30000);
        boolean done = Arrays.stream(t).noneMatch(Thread::isAlive);
        System.out.println("tryLock(1 ms) transfers, opposite directions, 4 threads x 500: finished=" + done + ", total=" + (a.bal + b.bal)
                + " (expected 2000), retries=" + retries + ", time=" + (System.nanoTime() - t0) / 1_000_000 + " ms");
    }
}

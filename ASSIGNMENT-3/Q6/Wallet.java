import java.lang.management.*;
import java.util.*;

class Account {
    final int id; int balance;
    Account(int id, int b) { this.id = id; balance = b; }
}

public class Wallet {
    // DEADLOCK-PRONE: locks 'from' then 'to' in the order given by the caller
    static void transferUnsafe(Account from, Account to, int amt, long pauseMs) throws InterruptedException {
        synchronized (from) {
            Thread.sleep(pauseMs);                     // makes the bad interleaving certain
            synchronized (to) { from.balance -= amt; to.balance += amt; }
        }
    }
    // DEADLOCK-FREE: always lock the account with the smaller id first
    static void transferSafe(Account from, Account to, int amt) {
        Account first = from.id < to.id ? from : to, second = first == from ? to : from;
        synchronized (first) { synchronized (second) { from.balance -= amt; to.balance += amt; } }
    }

    public static void main(String[] args) throws Exception {
        Account a = new Account(1, 1000), b = new Account(2, 1000);
        System.out.println("=== Part A1: transfers that deadlock ===");
        Thread t1 = new Thread(() -> { try { transferUnsafe(a, b, 100, 200); } catch (InterruptedException e) { } }, "T1 (A->B)");
        Thread t2 = new Thread(() -> { try { transferUnsafe(b, a, 50, 200); } catch (InterruptedException e) { } }, "T2 (B->A)");
        t1.setDaemon(true); t2.setDaemon(true);
        t1.start(); t2.start();
        t1.join(1500); t2.join(1500);
        System.out.println("After 1.5 s: T1 alive=" + t1.isAlive() + " state=" + t1.getState() + " | T2 alive=" + t2.isAlive() + " state=" + t2.getState());
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        long[] dead = mx.findDeadlockedThreads();
        System.out.println("ThreadMXBean.findDeadlockedThreads(): " + (dead == null ? "none" : dead.length + " threads deadlocked"));
        if (dead != null) for (ThreadInfo ti : mx.getThreadInfo(dead))
            System.out.println("  " + ti.getThreadName() + " is " + ti.getThreadState() + ", waiting for " + ti.getLockName().replaceAll("@[0-9a-f]+", "@..") + " held by " + ti.getLockOwnerName());
        System.out.println("Balances unchanged: A=" + a.balance + " B=" + b.balance);

        System.out.println("\n=== Part A2: same two transfers with lock ordering ===");
        Account c = new Account(1, 1000), d = new Account(2, 1000);
        Thread s1 = new Thread(() -> transferSafe(c, d, 100)), s2 = new Thread(() -> transferSafe(d, c, 50));
        s1.start(); s2.start(); s1.join(); s2.join();
        System.out.println("Both completed. A=" + c.balance + " B=" + d.balance + " (expected 950 / 1050)");

        System.out.println("\n=== Part A3: stress - 8 threads x 50,000 random transfers, 5 accounts ===");
        Account[] acc = new Account[5];
        for (int i = 0; i < 5; i++) acc[i] = new Account(i, 1000);
        Thread[] w = new Thread[8];
        for (int i = 0; i < w.length; i++) {
            final long seed = i;
            w[i] = new Thread(() -> { Random r = new Random(seed); for (int k = 0; k < 50000; k++) { int x = r.nextInt(5), y = r.nextInt(5); if (x != y) transferSafe(acc[x], acc[y], 1); } });
            w[i].start();
        }
        long t0 = System.currentTimeMillis();
        for (Thread t : w) t.join(20000);
        int sum = 0; for (Account x : acc) sum += x.balance;
        boolean allDone = Arrays.stream(w).noneMatch(Thread::isAlive);
        System.out.println("All threads finished: " + allDone + " in " + (System.currentTimeMillis() - t0) + " ms; total money = " + sum + " (expected 5000)");
    }
}

public class PriorityDaemon {
    static volatile long counter;
    static class Burner extends Thread {
        long done; Burner(String n, int p) { super(n); setPriority(p); }
        public void run() { long end = System.nanoTime() + 300_000_000L; while (System.nanoTime() < end) done++; }
    }
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("daemon")) {
            Thread d = new Thread(() -> {
                try { for (int i = 1; i <= 10; i++) { Thread.sleep(100); System.out.println("  daemon station scanned batch " + i); } }
                catch (InterruptedException e) { }
                System.out.println("  daemon station FINISHED");
            }, "Daemon-Station");
            d.setDaemon(true);
            Thread u = new Thread(() -> {
                try { Thread.sleep(350); } catch (InterruptedException e) { }
                System.out.println("  user station finished");
            }, "User-Station");
            d.start(); u.start();
            System.out.println("  isDaemon = " + d.isDaemon());
            u.join();
            System.out.println("  main() ends now");
            return;
        }
        System.out.println("CPU-bound work in 300 ms, priorities MIN(1) vs MAX(10), 4 threads each:");
        Burner[] b = { new Burner("low-1", 1), new Burner("high-1", 10), new Burner("low-2", 1), new Burner("high-2", 10) };
        for (Burner t : b) t.start();
        for (Burner t : b) t.join();
        for (Burner t : b) System.out.printf("  %-7s priority=%2d  iterations=%,d%n", t.getName(), t.getPriority(), t.done);
    }
}

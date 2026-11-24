public class ExceptionCost {
    static class Plain extends Exception {                    // normal: captures stack trace
        Plain(String m) { super(m); }
    }
    static class NoTrace extends Exception {                  // writableStackTrace = false
        NoTrace(String m) { super(m, null, false, false); }
    }
    static int depth(int d, int kind) throws Exception {
        if (d > 0) return depth(d - 1, kind) + 1;
        if (kind == 0) throw new Plain("x");
        if (kind == 1) throw new NoTrace("x");
        return 0;
    }
    static long run(int d, int kind, int n) {
        long t0 = System.nanoTime(); int s = 0;
        for (int i = 0; i < n; i++) { try { s += depth(d, kind); } catch (Exception e) { s++; } }
        if (s == -1) System.out.print("");
        return (System.nanoTime() - t0) / n;
    }
    public static void main(String[] a) {
        int n = 200_000;
        for (int w = 0; w < 3; w++) for (int d : new int[]{5, 50, 500}) { run(d, 0, 20000); run(d, 1, 20000); }
        System.out.printf("%-14s %18s %18s %18s%n", "stack depth", "with trace (ns)", "no trace (ns)", "plain return (ns)");
        for (int d : new int[]{5, 50, 500})
            System.out.printf("%-14d %18d %18d %18d%n", d, run(d, 0, n), run(d, 1, n), run(d, 2, n));
        Throwable root = new java.io.IOException("Disk quota exceeded");
        Throwable w1 = new Exception("level1", root), w2 = new Exception("level2", w1);
        int frames = 0; for (Throwable t = w2; t != null; t = t.getCause()) frames += t.getStackTrace().length;
        System.out.println("\nStack frames held by a 3-link chain created here: " + frames + " (each link keeps its own trace)");
    }
}

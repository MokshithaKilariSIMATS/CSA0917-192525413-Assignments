import java.util.*;

/** Alternative design: no exceptions, pre-validation + error codes (-1, -2, -3). */
public class ErrorCodeAtm {
    static final int BAD_NUMBER = -1, NOT_POSITIVE = -2, NO_FUNDS = -3;
    static boolean isInt(String s) {
        if (s.isEmpty()) return false;
        int i = (s.charAt(0) == '-') ? 1 : 0;
        if (i == s.length()) return false;
        for (; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return false;
        return true;
    }
    static int withdrawCode(int balance, String text) {
        if (!isInt(text)) return BAD_NUMBER;
        int amount = Integer.parseInt(text);
        if (amount <= 0) return NOT_POSITIVE;
        if (amount > balance) return NO_FUNDS;
        return balance - amount;
    }
    static int withdrawEx(int balance, String text) {
        try {
            int amount = Integer.parseInt(text);
            if (amount <= 0 || amount > balance) throw new IllegalArgumentException("bad");
            return balance - amount;
        } catch (IllegalArgumentException e) { return -1; }
    }
    static long time(boolean useCodes, String text, int n) {
        long sink = 0, t0 = System.nanoTime();
        for (int i = 0; i < n; i++) sink += useCodes ? withdrawCode(5000, text) : withdrawEx(5000, text);
        long t = System.nanoTime() - t0;
        if (sink == 42) System.out.print("");
        return t / n;
    }
    public static void main(String[] args) {
        // Scanner.hasNextInt() style check, shown on a scripted input
        Scanner sc = new Scanner("500 abc -20 99999");
        System.out.println("Scanner.hasNextInt() version on input \"500 abc -20 99999\":");
        while (sc.hasNext()) {
            if (!sc.hasNextInt()) { System.out.println("  token '" + sc.next() + "' -> code " + BAD_NUMBER); continue; }
            int amt = sc.nextInt();
            int r = amt <= 0 ? NOT_POSITIVE : amt > 5000 ? NO_FUNDS : 5000 - amt;
            System.out.println("  amount " + amt + " -> " + r);
        }
        int n = 2_000_000;
        for (int w = 0; w < 3; w++) { time(true, "abc", 200000); time(false, "abc", 200000); }   // warm-up
        System.out.println("\nCost per withdrawal attempt (ns), n = " + n + ", JIT-warmed:");
        System.out.printf("%-34s %12s %12s%n", "input", "error code", "exception");
        for (String in : new String[]{"500", "abc"})
            System.out.printf("%-34s %12d %12d%n", in.equals("500") ? "valid (\"500\")" : "invalid (\"abc\")",
                              time(true, in, n), time(false, in, n));
    }
}

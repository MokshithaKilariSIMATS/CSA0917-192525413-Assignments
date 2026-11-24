/** Console tests for the service layer: no GUI needed - proves separation of concerns. */
public class FeeServiceTest {
    static int pass = 0, fail = 0;
    static void run(FeeService s, String name, String id, String amt, boolean expectOk) {
        String out;
        boolean ok;
        try { out = s.pay(id, amt).toString(); ok = true; }
        catch (Exception e) { out = e.getMessage(); ok = false; }
        boolean good = ok == expectOk;
        if (good) pass++; else fail++;
        System.out.printf("[%s] %-28s -> %s%n", good ? "PASS" : "FAIL", name, out);
    }
    public static void main(String[] args) {
        FeeService s = new FeeService(new InMemoryPaymentRepository());
        run(s, "valid payment",            "192525413", "10000",  true);
        run(s, "non-numeric amount",       "192525413", "abc",    false);
        run(s, "negative amount",          "192525413", "-50",    false);
        run(s, "short student id",         "1234",      "500",    false);
        run(s, "SQL-injection style id",   "1' OR '1'='1", "500", false);
        run(s, "unknown student",          "999999999", "500",    false);
        run(s, "amount above due",         "192525001", "99999",  false);
        run(s, "pay remaining exactly",    "192525001", "20000",  true);
        System.out.println("Summary: " + pass + " passed, " + fail + " failed");
    }
}

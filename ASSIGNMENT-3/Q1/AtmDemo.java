import java.io.*;
import java.util.*;

class InsufficientFundsException extends Exception {          // checked, business rule
    InsufficientFundsException(String msg) { super(msg); }
}

public class AtmDemo {
    static final Map<String, Integer> balances = new HashMap<>();

    // Failure point 1: card-reader file missing -> FileNotFoundException (checked IOException)
    static void loadCardFile(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                balances.put(p[0].trim(), Integer.parseInt(p[1].trim()));
            }
        }
    }

    static int withdraw(String acct, String amountText) throws InsufficientFundsException {
        int amount = Integer.parseInt(amountText.trim());                   // F2: letters -> NumberFormatException
        if (amount <= 0)                                                    // F3: negative / zero
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        Integer bal = balances.get(acct);
        if (bal == null)                                                    // F4: unknown account
            throw new NoSuchElementException("Unknown account " + acct);
        if (amount > bal)                                                   // F5: more than balance
            throw new InsufficientFundsException("Requested " + amount + " but balance is " + bal);
        balances.put(acct, bal - amount);
        return bal - amount;
    }

    static void attempt(int no, String acct, String amount) {
        String path = "no exception";
        System.out.printf("Test %d: account=%s amount=\"%s\"%n", no, acct, amount);
        try {
            int nb = withdraw(acct, amount);
            System.out.println("  Cash dispensed. New balance = " + nb);
        } catch (NumberFormatException e) {                // narrowest first (subclass of IllegalArgumentException)
            path = "catch NumberFormatException";
            System.out.println("  Please type digits only. (" + e.getMessage() + ")");
        } catch (IllegalArgumentException e) {             // broader sibling AFTER its subclass
            path = "catch IllegalArgumentException";
            System.out.println("  Invalid amount. (" + e.getMessage() + ")");
        } catch (InsufficientFundsException e) {           // checked, custom
            path = "catch InsufficientFundsException";
            System.out.println("  Declined: " + e.getMessage());
        } catch (NoSuchElementException | NullPointerException e) {   // multi-catch of unrelated types
            path = "catch NoSuchElementException | NullPointerException";
            System.out.println("  Card rejected: " + e.getMessage());
        } catch (Exception e) {                            // last-resort safety net
            path = "catch Exception";
            System.out.println("  Unexpected: " + e);
        } finally {
            System.out.println("  [finally] card returned, session closed");
        }
        System.out.println("  -> path taken: " + path);
    }

    public static void main(String[] args) {
        System.out.println("=== Test 0: card-reader file missing ===");
        try {
            loadCardFile("cards_missing.txt");
        } catch (FileNotFoundException e) {
            System.out.println("  catch FileNotFoundException: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("  catch IOException: " + e.getMessage());
        } finally {
            System.out.println("  [finally] card reader released");
        }
        try { loadCardFile("cards.txt"); System.out.println("\nCard file loaded: " + balances.size() + " accounts"); }
        catch (IOException e) { System.out.println("Fatal: " + e); return; }

        String[][] tests = {
            {"1001", "500"},        // valid
            {"1001", "abc"},        // letters
            {"1001", "-200"},       // negative
            {"1001", "99999"},      // more than balance
            {"9999", "100"},        // unknown account
            {"1002", "750"},        // exactly the balance (boundary, valid)
            {"1002", "12.5"},       // decimal text
        };
        System.out.println();
        for (int i = 0; i < tests.length; i++) attempt(i + 1, tests[i][0], tests[i][1]);
    }
}

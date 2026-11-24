import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;

/** Task B1: normal / non-existent / damaged / missing-file / torn-write test cases for PassStore. */
public class StoreDemo {

    public static void main(String[] args) throws IOException {
        Path sample = Paths.get("Task01/data/passes_sample.txt");
        System.out.println("=== Task B1 : PassStore functional tests ===");
        System.out.println("Input file " + sample + ":");
        for (String l : Files.readAllLines(sample)) System.out.println("   | " + (l.isEmpty() ? "<empty line>" : l));
        System.out.println();

        PassStore store = new PassStore(sample);

        System.out.println("TEST 1  normal case      find(\"P1003\")");
        System.out.println("   -> " + store.find("P1003") + "   [damaged lines skipped: " + store.damagedSeen + "]");

        System.out.println("TEST 2  damaged line     find(\"P1002\")  (line has only 5 fields)");
        System.out.println("   -> " + store.find("P1002") + "   [damaged lines skipped: " + store.damagedSeen + "]");

        System.out.println("TEST 3  absent id        find(\"P9999\")");
        System.out.println("   -> " + store.find("P9999"));

        System.out.println("TEST 4  list(route 12)");
        List<BusPass> r12 = store.list(12);
        r12.forEach(p -> System.out.println("   -> " + p.passId() + " " + p.name() + " stop=" + p.stop()));
        System.out.println("   [damaged lines skipped: " + store.damagedSeen + "]");

        System.out.println("TEST 5  missing file     find(\"P1001\") on data/does_not_exist.txt");
        System.out.println("   -> " + new PassStore(Paths.get("Task01/data/does_not_exist.txt")).find("P1001"));

        System.out.println("TEST 6  add() then find()");
        Path work = Paths.get("Task01/data/passes_work.txt");
        Files.copy(sample, work, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        PassStore ws = new PassStore(work);
        ws.add(new BusPass("P1004", "22CS104", "Kiran P", 12, "Bus Stand",
                LocalDate.parse("2027-03-31"), new BigDecimal("1500.00")));
        System.out.println("   -> " + ws.find("P1004"));

        System.out.println("TEST 7  torn write (power failure leaves last line without newline), then add()");
        Files.write(work, "P1005,22CS105,Torn Reco".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        ws.add(new BusPass("P1006", "22CS106", "Leena M", 15, "Station Road",
                LocalDate.parse("2027-03-31"), new BigDecimal("1800.00")));
        System.out.println("   find(P1005) torn record -> " + ws.find("P1005") + "   (damaged lines skipped: " + ws.damagedSeen + ")");
        System.out.println("   find(P1006) new record  -> " + ws.find("P1006"));
        System.out.println("   (the torn line was isolated, so the new record was NOT glued onto it)");
        Files.deleteIfExists(work);
    }
}

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/** Task B2 : adversarial bus-pass data - wrong field count, non-numeric amount, empty line (and more). */
public class DamagedDataTest {

    public static void main(String[] args) throws IOException {
        Path f = Paths.get("Task02/data/passes_damaged.txt");
        List<String> lines = Files.readAllLines(f);
        System.out.println("=== Task B2 : damaged bus-pass data ===");
        System.out.println("Input file: " + f + "  (" + lines.size() + " lines)");
        System.out.println();
        System.out.printf("%-4s %-9s %-34s %s%n", "Line", "Verdict", "Reason", "Content");
        int ok = 0, bad = 0;
        for (int i = 0; i < lines.size(); i++) {
            String reason = BusPass.check(lines.get(i));
            if (reason == null) ok++; else bad++;
            System.out.printf("%-4d %-9s %-34s %s%n", i + 1, reason == null ? "VALID" : "DAMAGED",
                    reason == null ? "-" : reason, lines.get(i).isEmpty() ? "<empty line>" : lines.get(i));
        }
        System.out.println();
        System.out.println("Summary: valid = " + ok + ", damaged = " + bad);

        PassStore store = new PassStore(f);
        System.out.println();
        System.out.println("Search must survive the damage:");
        System.out.println("  find(P2001) -> " + (store.find("P2001") != null ? "found" : "null") + "   (before the damage)");
        System.out.println("  find(P2009) -> " + (store.find("P2009") != null ? "found" : "null") + "   (after ALL damaged lines; skipped " + store.damagedSeen + " damaged lines)");
        System.out.println("  find(P2002) -> " + store.find("P2002") + "   (record itself is damaged)");
    }
}

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/** Delimited text-file store for bus passes (Task A1 design, Task B1 implementation). */
public class PassStore {

    private final Path file;
    /** number of damaged lines skipped by the most recent find()/list() call */
    public int damagedSeen;

    public PassStore(Path file) {
        this.file = file;
    }

    /** ADD: append one record. Repairs a missing trailing newline first (partial write after power failure). */
    public void add(BusPass p) throws IOException {
        String line = p.toLine();
        boolean needNewline = false;
        if (Files.exists(file) && Files.size(file) > 0) {
            try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
                raf.seek(raf.length() - 1);
                needNewline = raf.read() != '\n';
            }
        }
        try (BufferedWriter w = Files.newBufferedWriter(file,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (needNewline) w.newLine();          // isolate the torn line so it cannot merge with the new one
            w.write(line);
            w.newLine();
        }
    }

    /**
     * FIND: read line by line, skip damaged lines, return the matching record (or null).
     * (TODO 1 of Task B1 - completed.)
     */
    public BusPass find(String passId) throws IOException {
        damagedSeen = 0;
        try (BufferedReader br = Files.newBufferedReader(file)) {
            String line;
            while ((line = br.readLine()) != null) {
                BusPass p;
                try {
                    p = BusPass.parse(line);
                } catch (IllegalArgumentException damaged) {
                    damagedSeen++;                 // a damaged line never stops the search
                    continue;
                }
                if (p.passId().equals(passId)) {
                    return p;
                }
            }
        } catch (NoSuchFileException missing) {
            System.out.println("  [store] file not found: " + file + " -> treated as empty store");
        }
        return null;
    }

    /** LIST: all valid passes on a route. */
    public List<BusPass> list(int route) throws IOException {
        damagedSeen = 0;
        List<BusPass> out = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(file)) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    BusPass p = BusPass.parse(line);
                    if (p.route() == route) out.add(p);
                } catch (IllegalArgumentException damaged) {
                    damagedSeen++;
                }
            }
        } catch (NoSuchFileException missing) {
            System.out.println("  [store] file not found: " + file + " -> empty list");
        }
        return out;
    }
}

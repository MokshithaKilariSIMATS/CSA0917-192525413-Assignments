import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Alternative design for Task B1: load every valid record into a HashMap once, then answer find() in O(1). */
public class PassStoreMap {

    private final Map<String, BusPass> index = new HashMap<>();
    public int damaged;

    public PassStoreMap(Path file) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(file)) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    BusPass p = BusPass.parse(line);
                    index.put(p.passId(), p);
                } catch (IllegalArgumentException e) {
                    damaged++;
                }
            }
        }
    }

    public BusPass find(String passId) {
        return index.get(passId);
    }

    public int size() {
        return index.size();
    }
}

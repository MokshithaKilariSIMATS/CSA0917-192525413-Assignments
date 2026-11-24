import java.util.*;

/** Simulated database access: every call costs ~latency ms (stand-in for a JDBC round trip). */
public class DoctorDao {
    private final int latencyMs;
    private int calls = 0;
    private static final Map<String, List<String>> DATA = new LinkedHashMap<>();
    static {
        DATA.put("Cardiology",  Arrays.asList("Dr. Rao", "Dr. Mehta", "Dr. Iyer"));
        DATA.put("Orthopedics", Arrays.asList("Dr. Khan", "Dr. Nair"));
        DATA.put("Paediatrics", Arrays.asList("Dr. Sen", "Dr. Das", "Dr. Pillai", "Dr. Joshi"));
        DATA.put("Neurology",   Arrays.asList("Dr. Bose"));
    }
    public DoctorDao(int latencyMs) { this.latencyMs = latencyMs; }
    public List<String> departments() { return new ArrayList<>(DATA.keySet()); }
    public List<String> doctorsIn(String dept) {
        calls++;
        try { Thread.sleep(latencyMs); } catch (InterruptedException ignored) { }
        return DATA.getOrDefault(dept, Collections.emptyList());
    }
    public int getCalls() { return calls; }
}

import java.util.HashMap;
import java.util.Map;

/** Stand-in repository used for testing the service without a database. */
public class InMemoryPaymentRepository implements PaymentRepository {
    private final Map<String, Double> dues = new HashMap<>();
    public InMemoryPaymentRepository() {
        dues.put("192525413", 50000.0);
        dues.put("192525001", 20000.0);
        dues.put("192525002", 0.0);
    }
    public boolean studentExists(String id) { return dues.containsKey(id); }
    public double outstanding(String id) { return dues.get(id); }
    public void savePayment(String id, double amount) { dues.merge(id, -amount, Double::sum); }
}

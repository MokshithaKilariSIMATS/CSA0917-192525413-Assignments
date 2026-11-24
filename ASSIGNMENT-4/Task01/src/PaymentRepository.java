/** Data-access contract. The service depends on this interface, not on JDBC. */
public interface PaymentRepository {
    boolean studentExists(String studentId);
    double outstanding(String studentId);
    void savePayment(String studentId, double amount) throws Exception;
}

import java.util.regex.Pattern;

/** Business layer: validates input and processes a payment. No Swing classes here. */
public class FeeService {
    private static final Pattern ID = Pattern.compile("\\d{9}");
    private final PaymentRepository repo;
    public FeeService(PaymentRepository repo) { this.repo = repo; }

    /** @throws IllegalArgumentException with a user-readable message for every invalid input */
    public Receipt pay(String studentId, String amountText) throws Exception {
        if (studentId == null || !ID.matcher(studentId.trim()).matches())
            throw new IllegalArgumentException("Student ID must be exactly 9 digits.");
        String id = studentId.trim();
        double amount;
        try { amount = Double.parseDouble(amountText == null ? "" : amountText.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Amount must be a number."); }
        if (amount <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
        if (!repo.studentExists(id)) throw new IllegalArgumentException("Unknown student ID: " + id);
        double due = repo.outstanding(id);
        if (amount > due) throw new IllegalArgumentException(
                String.format("Amount exceeds outstanding fee of Rs.%.2f.", due));
        repo.savePayment(id, amount);
        return new Receipt(id, amount);
    }
}

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Immutable value object produced by FeeService after a successful payment. */
public final class Receipt {
    private static int counter = 5000;
    private final int number;
    private final String studentId;
    private final double amount;
    private final String time;

    public Receipt(String studentId, double amount) {
        this.number = ++counter;
        this.studentId = studentId;
        this.amount = amount;
        this.time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
    }
    public int getNumber() { return number; }
    public double getAmount() { return amount; }
    @Override public String toString() {
        return String.format("Receipt #%d | Student %s | Paid Rs.%.2f | %s", number, studentId, amount, time);
    }
}

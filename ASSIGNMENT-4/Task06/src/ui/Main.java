package ui;
import dao.*;
import javax.swing.SwingUtilities;
import service.RegistrationService;

/** Entry point. With DB_URL/DB_USER/DB_PASS set it uses MySQL; otherwise it runs on the in-memory test double. */
public class Main {
    public static RegistrationDao createDao() {
        String url = System.getenv("DB_URL");
        if (url != null) return new JdbcRegistrationDao(url, System.getenv().getOrDefault("DB_USER", "root"), System.getenv().getOrDefault("DB_PASS", ""));
        InMemoryRegistrationDao d = new InMemoryRegistrationDao();
        try { d.insert("192525001", "Ravi Kumar", "ravi@campus.edu", 1); d.insert("192525002", "Meena Iyer", "meena@campus.edu", 2); d.insert("192525003", "Arjun Das", "arjun@campus.edu", 1); } catch (Exception ignored) { }
        return d;
    }
    public static void main(String[] a) {
        System.out.println("Data source: " + (System.getenv("DB_URL") != null ? "MySQL (JDBC)" : "in-memory test double"));
        SwingUtilities.invokeLater(() -> new RegistrationFrame(new RegistrationService(createDao())).setVisible(true));
    }
}

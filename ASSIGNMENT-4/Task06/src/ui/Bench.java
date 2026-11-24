package ui;
import dao.*;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import model.Registration;

/** All-at-once vs paginated loading of 100,000 registrations: time, retained memory, and the work done before the UI can respond. */
public class Bench {
    static long usedMb() { System.gc(); System.gc(); Runtime r = Runtime.getRuntime(); return (r.totalMemory() - r.freeMemory()) / (1024 * 1024); }
    static DefaultTableModel fill(List<Registration> rows) {
        DefaultTableModel m = new DefaultTableModel(new String[]{"ID", "Student", "Name", "Email", "Event", "At"}, 0);
        for (Registration x : rows) m.addRow(new Object[]{x.getId(), x.getStudentId(), x.getStudentName(), x.getEmail(), x.getEventName(), x.getRegisteredAt()});
        return m;
    }
    public static void main(String[] a) throws Exception {
        RegistrationDao dao = Main.createDao();
        if (dao instanceof InMemoryRegistrationDao) ((InMemoryRegistrationDao) dao).seed(100_000);
        System.out.println("Rows in data source: " + dao.count(null) + "   (source: " + (dao instanceof InMemoryRegistrationDao ? "in-memory stand-in" : "MySQL") + ")");
        for (int warm = 0; warm < 3; warm++) { fill(dao.findAll(null)); fill(dao.findPage(null, 50, 0)); }   // JIT warm-up
        long base = usedMb();
        long t0 = System.nanoTime(); DefaultTableModel all = fill(dao.findAll(null)); double allMs = (System.nanoTime() - t0) / 1e6;
        long allMb = usedMb() - base;
        System.out.printf("ALL AT ONCE : %8.1f ms | rows in table model %,7d | retained heap ~ %d MB%n", allMs, all.getRowCount(), allMb);
        all = null; base = usedMb();
        double sum = 0; int pageNo = 0; DefaultTableModel pg = null; double worst = 0;
        for (int p = 0; p < 5; p++) { long s = System.nanoTime(); pg = fill(dao.findPage(null, 50, p * 50)); double ms = (System.nanoTime() - s) / 1e6; sum += ms; worst = Math.max(worst, ms); pageNo++; }
        long pgMb = usedMb() - base;
        System.out.printf("PAGINATED   : %8.2f ms per page (avg of %d, worst %.2f) | rows in table model %d | retained heap ~ %d MB%n", sum / pageNo, pageNo, worst, pg.getRowCount(), pgMb);
        long s = System.nanoTime(); pg = fill(dao.findPage(null, 50, 99_900)); System.out.printf("LAST PAGE   : %8.2f ms  (OFFSET 99900)%n", (System.nanoTime() - s) / 1e6);
    }
}

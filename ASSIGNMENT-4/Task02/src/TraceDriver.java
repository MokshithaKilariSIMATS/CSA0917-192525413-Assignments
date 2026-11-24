import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Drives the window through a full life cycle and prints the real method-call sequence. */
public class TraceDriver {
    public static void main(String[] a) throws Exception {
        JFrame.setDefaultLookAndFeelDecorated(true);
        final AppointmentFrame[] f = new AppointmentFrame[1];
        System.out.println("== 1. Create window");
        SwingUtilities.invokeAndWait(() -> { f[0] = new AppointmentFrame(new AppointmentService(new DoctorDao(0))); f[0].setLocation(150, 100); });
        System.out.println("== 2. setVisible(true)");
        SwingUtilities.invokeAndWait(() -> f[0].setVisible(true));
        Shot.pause(700);
        System.out.println("== 3. Change department to Paediatrics");
        SwingUtilities.invokeAndWait(() -> f[0].cmbDept.setSelectedItem("Paediatrics"));
        Shot.pause(300); Shot.take("../output/task2_department_changed.png");
        System.out.println("== 4. Book with invalid phone");
        SwingUtilities.invokeAndWait(() -> { f[0].txtPatient.setText("Asha Reddy"); f[0].txtPhone.setText("12345"); f[0].btnBook.doClick(); });
        Shot.pause(300); Shot.take("../output/task2_invalid_phone.png");
        System.out.println("== 5. Book valid appointment");
        SwingUtilities.invokeAndWait(() -> { f[0].txtPhone.setText("9876543210"); f[0].btnBook.doClick(); });
        Shot.pause(300); Shot.take("../output/task2_booked.png");
        System.out.println("== 6. Book same doctor/slot again (conflict)");
        SwingUtilities.invokeAndWait(() -> f[0].btnBook.doClick());
        Shot.pause(300); Shot.take("../output/task2_conflict.png");
        System.out.println("== 7. Close window (WINDOW_CLOSING event)");
        SwingUtilities.invokeAndWait(() -> f[0].dispatchEvent(new WindowEvent(f[0], WindowEvent.WINDOW_CLOSING)));
        Shot.pause(500);
        System.exit(0);
    }
}

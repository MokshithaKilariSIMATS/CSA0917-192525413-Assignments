package ui;
import java.awt.*;
import java.io.File;
import javax.swing.*;
import service.RegistrationService;

/** Drives the real window (normal, invalid, duplicate registration; filter; paging; export) and captures evidence. */
public class ScreenshotDriver {
    static void ui(Runnable r) throws Exception { SwingUtilities.invokeAndWait(r); }
    public static void main(String[] args) throws Exception {
        JFrame.setDefaultLookAndFeelDecorated(true); JDialog.setDefaultLookAndFeelDecorated(true);
        final RegistrationFrame[] f = new RegistrationFrame[1];
        ui(() -> { f[0] = new RegistrationFrame(new RegistrationService(Main.createDao())); f[0].setLocation(10, 10); f[0].setVisible(true); });
        Shot.pause(900);
        // 1 normal
        ui(() -> { f[0].txtId.setText("192525413"); f[0].txtName.setText("Mokshitha Kilari"); f[0].txtEmail.setText("mokshitha@campus.edu"); f[0].cmbEvent.setSelectedIndex(0); f[0].btnRegister.doClick(); });
        Shot.pause(900); Shot.take("../output/task6_1_normal_registration.png"); System.out.println("NORMAL    -> " + f[0].lblStatus.getText());
        // 2 invalid
        ui(() -> { f[0].txtId.setText("12AB"); f[0].txtEmail.setText("not-an-email"); f[0].btnRegister.doClick(); });
        Shot.pause(700); Shot.take("../output/task6_2_invalid_input.png"); System.out.println("INVALID   -> " + f[0].lblStatus.getText());
        ui(() -> { f[0].txtId.setText("192525413"); f[0].btnRegister.doClick(); });
        Shot.pause(700); System.out.println("INVALID   -> " + f[0].lblStatus.getText());
        // 3 duplicate
        ui(() -> { f[0].txtEmail.setText("mokshitha@campus.edu"); f[0].btnRegister.doClick(); });
        Shot.pause(700); Shot.take("../output/task6_3_duplicate_registration.png"); System.out.println("DUPLICATE -> " + f[0].lblStatus.getText());
        // 4 same student, different event is allowed
        ui(() -> { f[0].cmbEvent.setSelectedIndex(2); f[0].btnRegister.doClick(); });
        Shot.pause(700); System.out.println("OTHER EVT -> " + f[0].lblStatus.getText());
        // 5 table + filter
        ui(() -> { f[0].tabs.setSelectedIndex(1); f[0].cmbFilter.setSelectedIndex(0); });
        Shot.pause(800); Shot.take("../output/task6_4_all_registrations.png");
        ui(() -> f[0].cmbFilter.setSelectedIndex(1));
        Shot.pause(800); Shot.take("../output/task6_5_filtered_by_event.png"); System.out.println("FILTER    -> " + f[0].lblPage.getText() + " | rows in table: " + f[0].model.getRowCount());
        // 6 export
        File out = new File("../output/registrations_export.csv");
        ui(() -> f[0].exportTo(out)); Shot.pause(900); System.out.println("EXPORT    -> " + f[0].lblStatus.getText());
        System.exit(0);
    }
}

import java.awt.*;
import javax.swing.*;

/** Drives the real GUI with Robot input and captures screenshots (evidence). */
public class ScreenshotDriver {
    public static void main(String[] a) throws Exception {
        JFrame.setDefaultLookAndFeelDecorated(true); JDialog.setDefaultLookAndFeelDecorated(true);
        final FeePaymentFrame[] f = new FeePaymentFrame[1];
        SwingUtilities.invokeAndWait(() -> { f[0] = new FeePaymentFrame(new FeeService(new InMemoryPaymentRepository())); f[0].setLocation(200, 150); f[0].setVisible(true); });
        Shot.pause(800);
        SwingUtilities.invokeAndWait(() -> { f[0].txtId.setText("192525413"); f[0].txtAmount.setText("abc"); f[0].btnPay.doClick(); });
        Shot.pause(700); Shot.take("../output/task1_invalid_amount.png");
        SwingUtilities.invokeAndWait(() -> { f[0].txtAmount.setText("10000"); f[0].btnPay.doClick(); });
        Shot.pause(700); Shot.take("../output/task1_valid_payment.png");
        SwingUtilities.invokeLater(() -> f[0].btnReceipt.doClick());
        Shot.pause(900); Shot.take("../output/task1_receipt_dialog.png");
        System.exit(0);
    }
}

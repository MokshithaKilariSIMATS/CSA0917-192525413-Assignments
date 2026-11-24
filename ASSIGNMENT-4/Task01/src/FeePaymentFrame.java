import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

/** View layer: collects input and shows results. All processing is delegated to FeeService. */
public class FeePaymentFrame extends JFrame {
    final JTextField txtId = new JTextField(12);
    final JTextField txtAmount = new JTextField(12);
    final JLabel lblStatus = new JLabel("Enter details and press Pay.");
    final JButton btnPay, btnReceipt, btnClear;
    private final FeeService service;
    private Receipt lastReceipt;

    // one Action object per button: separate listener, own name, own enabled state
    private class PayAction extends AbstractAction {
        PayAction() { super("Pay"); }
        public void actionPerformed(ActionEvent e) {
            setEnabled(false);
            lblStatus.setText("Processing payment...");
            new SwingWorker<Receipt, Void>() {           // keeps the EDT free
                protected Receipt doInBackground() throws Exception {
                    return service.pay(txtId.getText(), txtAmount.getText());
                }
                protected void done() {
                    try {
                        lastReceipt = get();
                        lblStatus.setForeground(new Color(0, 110, 0));
                        lblStatus.setText("Payment successful. Receipt #" + lastReceipt.getNumber());
                        btnReceipt.setEnabled(true);
                    } catch (Exception ex) {
                        Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                        lblStatus.setForeground(Color.RED);
                        lblStatus.setText(c.getMessage());
                    } finally { PayAction.this.setEnabled(true); }
                }
            }.execute();
        }
    }
    private class ReceiptAction extends AbstractAction {
        ReceiptAction() { super("Receipt"); setEnabled(false); }
        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(FeePaymentFrame.this, lastReceipt.toString(), "Fee Receipt", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    private class ClearAction extends AbstractAction {
        ClearAction() { super("Clear"); }
        public void actionPerformed(ActionEvent e) {
            txtId.setText(""); txtAmount.setText(""); lblStatus.setForeground(Color.DARK_GRAY);
            lblStatus.setText("Enter details and press Pay.");
        }
    }

    public FeePaymentFrame(FeeService service) {
        super("Student Fee Payment");
        this.service = service;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(new JLabel("Student ID")); form.add(txtId);
        form.add(new JLabel("Amount (Rs.)")); form.add(txtAmount);
        btnPay = new JButton(new PayAction());
        btnReceipt = new JButton(new ReceiptAction());
        btnClear = new JButton(new ClearAction());
        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(btnPay); buttons.add(btnReceipt); buttons.add(btnClear);
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(form, BorderLayout.NORTH);
        root.add(buttons, BorderLayout.CENTER);
        root.add(lblStatus, BorderLayout.SOUTH);
        setContentPane(root);
        pack(); setMinimumSize(new Dimension(420, 200));
        getRootPane().setDefaultButton(btnPay);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FeePaymentFrame(new FeeService(new InMemoryPaymentRepository())).setVisible(true));
    }
}

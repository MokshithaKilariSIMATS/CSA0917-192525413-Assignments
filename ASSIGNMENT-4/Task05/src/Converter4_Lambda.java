import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Style 4: lambda expression (ActionListener is a functional interface). TODO 1 and TODO 2 completed. */
public class Converter4_Lambda extends JFrame {
    final JTextField txtAmount = new JTextField(10);
    final JComboBox<String> cmbFrom = new JComboBox<>(ConversionLogic.PER_USD.keySet().toArray(new String[0]));
    final JComboBox<String> cmbTo = new JComboBox<>(ConversionLogic.PER_USD.keySet().toArray(new String[0]));
    final JButton btnConvert = new JButton("Convert");
    final JLabel lblResult = new JLabel("Result will appear here");

    public Converter4_Lambda() {
        super("Currency Converter - Lambda expression");
        cmbTo.setSelectedItem("INR");
        JPanel p = new JPanel(new GridLayout(4, 2, 6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(new JLabel("Amount")); p.add(txtAmount);
        p.add(new JLabel("From")); p.add(cmbFrom);
        p.add(new JLabel("To")); p.add(cmbTo);
        p.add(btnConvert); p.add(lblResult);
        add(p);
        btnConvert.addActionListener(e -> doConvert());   // TODO 1: Convert button
        txtAmount.addActionListener(e -> doConvert());    // TODO 2: Enter key
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { System.out.println("windowClosing: disposing Converter4_Lambda"); dispose(); }
        });
        pack();
    }
    private void doConvert() {
        try {
            String r = ConversionLogic.convert(txtAmount.getText(), (String) cmbFrom.getSelectedItem(), (String) cmbTo.getSelectedItem());
            lblResult.setText(r);
            JOptionPane.showMessageDialog(this, r, "Conversion result", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            lblResult.setText("Invalid amount");
            JOptionPane.showMessageDialog(this, "Please enter a positive number.", "Invalid amount", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static JFrame create() { return new Converter4_Lambda(); }
    public static void main(String[] a) { SwingUtilities.invokeLater(() -> create().setVisible(true)); }
}

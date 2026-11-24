import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Style 2: a named inner class implements ActionListener and can reach the frame's fields directly. */
public class Converter2_NamedInner extends JFrame {
    final JTextField txtAmount = new JTextField(10);
    final JComboBox<String> cmbFrom = new JComboBox<>(ConversionLogic.PER_USD.keySet().toArray(new String[0]));
    final JComboBox<String> cmbTo = new JComboBox<>(ConversionLogic.PER_USD.keySet().toArray(new String[0]));
    final JButton btnConvert = new JButton("Convert");
    final JLabel lblResult = new JLabel("Result will appear here");

    public Converter2_NamedInner() {
        super("Currency Converter - Named inner class");
        cmbTo.setSelectedItem("INR");
        JPanel p = new JPanel(new GridLayout(4, 2, 6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(new JLabel("Amount")); p.add(txtAmount);
        p.add(new JLabel("From")); p.add(cmbFrom);
        p.add(new JLabel("To")); p.add(cmbTo);
        p.add(btnConvert); p.add(lblResult);
        add(p);
        ConvertListener l = new ConvertListener();
        btnConvert.addActionListener(l);
        txtAmount.addActionListener(l);         // Enter key
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { System.out.println("windowClosing: disposing Converter2_NamedInner"); dispose(); }
        });
        pack();
    }
    private class ConvertListener implements ActionListener {
        @Override public void actionPerformed(ActionEvent e) { doConvert(); }
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

    public static JFrame create() { return new Converter2_NamedInner(); }
    public static void main(String[] a) { SwingUtilities.invokeLater(() -> create().setVisible(true)); }
}

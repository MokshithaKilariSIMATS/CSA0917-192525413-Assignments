import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;

/**
 * TASK 1 - ORIGINAL (FAULTY) DESIGN, kept only for the diagnosis.
 * One listener for every button, all logic inside the handler.
 * (Reconstruction of the scenario described in the assignment brief.)
 */
public class BadFeeWindow extends JFrame implements ActionListener {
    JTextField txtId = new JTextField(10), txtAmount = new JTextField(10);
    JButton btnPay = new JButton("Pay"), btnReceipt = new JButton("Receipt"), btnClear = new JButton("Clear");
    JLabel lblStatus = new JLabel();

    BadFeeWindow() {
        setLayout(new FlowLayout());
        add(new JLabel("Student ID")); add(txtId);
        add(new JLabel("Amount")); add(txtAmount);
        add(btnPay); add(btnReceipt); add(btnClear); add(lblStatus);
        btnPay.addActionListener(this);        // the SAME listener ...
        btnReceipt.addActionListener(this);    // ... for every button
        btnClear.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd == "Pay") {                                   // (1) String compared with ==
            try {
                double amt = Double.parseDouble(txtAmount.getText());   // (2) no validation
                Connection con = DriverManager.getConnection("jdbc:mysql://localhost/college", "root", "root");
                Statement st = con.createStatement();
                st.executeUpdate("INSERT INTO payment VALUES('" + txtId.getText() + "'," + amt + ")"); // (3) SQL injection
                Thread.sleep(3000);                                      // (4) freezes the EDT
                // (5) no feedback, (6) connection never closed
            } catch (Exception ex) { }                                   // (7) exception swallowed
        } else if (cmd == "Receipt") {
            // (8) Receipt action missing - nothing happens
        } else if (cmd == "Clear") {
            txtId.setText(""); txtAmount.setText("");
        }
    }
    public static void main(String[] a) {
        BadFeeWindow w = new BadFeeWindow();
        w.setSize(400, 150); w.setVisible(true);
    }
}

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Outpatient appointment desk. Logs every window event so the call sequence can be traced. */
public class AppointmentFrame extends JFrame {
    final JComboBox<String> cmbDept = new JComboBox<>();
    final JComboBox<String> cmbDoctor = new JComboBox<>();
    final JComboBox<String> cmbSlot = new JComboBox<>(new String[]{"09:00", "09:30", "10:00", "10:30", "11:00"});
    final JTextField txtPatient = new JTextField(16), txtPhone = new JTextField(16);
    final JButton btnBook = new JButton("Book Appointment"), btnClear = new JButton("Clear");
    final JLabel lblStatus = new JLabel("Ready.");
    private final AppointmentService service;

    static void log(String s) { System.out.println("  -> " + s); }

    public AppointmentFrame(AppointmentService service) {
        super("Outpatient Appointment Desk");
        this.service = service;
        log("constructor: building components");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);      // windowClosing decides what happens
        JPanel form = new JPanel(new GridLayout(5, 2, 6, 6));
        form.add(new JLabel("Department")); form.add(cmbDept);
        form.add(new JLabel("Doctor"));     form.add(cmbDoctor);
        form.add(new JLabel("Time slot"));  form.add(cmbSlot);
        form.add(new JLabel("Patient name")); form.add(txtPatient);
        form.add(new JLabel("Phone"));      form.add(txtPhone);
        JPanel south = new JPanel(new FlowLayout());
        south.add(btnBook); south.add(btnClear);
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(form, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(south, BorderLayout.NORTH); bottom.add(lblStatus, BorderLayout.SOUTH);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);

        for (String d : service.departments()) cmbDept.addItem(d);
        refreshDoctors();

        cmbDept.addItemListener(e -> {                      // ItemEvent -> ItemListener
            if (e.getStateChange() == ItemEvent.SELECTED) { log("ItemEvent(SELECTED) on cmbDept -> refreshDoctors()"); refreshDoctors(); }
        });
        btnBook.addActionListener(e -> { log("ActionEvent on btnBook -> onBook()"); onBook(); });
        btnClear.addActionListener(e -> { log("ActionEvent on btnClear -> clear fields"); txtPatient.setText(""); txtPhone.setText(""); lblStatus.setText("Cleared."); });

        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e)      { log("windowOpened"); }
            public void windowActivated(WindowEvent e)   { log("windowActivated"); }
            public void windowDeactivated(WindowEvent e) { log("windowDeactivated"); }
            public void windowClosing(WindowEvent e)     { log("windowClosing -> dispose()"); dispose(); }
            public void windowClosed(WindowEvent e)      { log("windowClosed"); }
        });
        pack();
    }
    private void refreshDoctors() {
        cmbDoctor.removeAllItems();
        for (String d : service.doctors((String) cmbDept.getSelectedItem())) cmbDoctor.addItem(d);
    }
    private void onBook() {
        try {
            String msg = service.book(txtPatient.getText(), txtPhone.getText(), (String) cmbDept.getSelectedItem(),
                    (String) cmbDoctor.getSelectedItem(), (String) cmbSlot.getSelectedItem());
            lblStatus.setForeground(new Color(0, 110, 0)); lblStatus.setText(msg);
        } catch (RuntimeException ex) {
            lblStatus.setForeground(Color.RED); lblStatus.setText(ex.getMessage());
        }
    }
    public static void main(String[] a) {
        SwingUtilities.invokeLater(() -> new AppointmentFrame(new AppointmentService(new DoctorDao(0))).setVisible(true));
    }
}

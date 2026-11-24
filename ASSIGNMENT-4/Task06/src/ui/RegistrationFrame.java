package ui;
import dao.DuplicateRegistrationException;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Event;
import model.Registration;
import service.RegistrationService;

/** View layer only. Anything that may take time runs in a SwingWorker, never on the Event Dispatch Thread. */
public class RegistrationFrame extends JFrame {
    final JTextField txtId = new JTextField(14), txtName = new JTextField(14), txtEmail = new JTextField(14);
    final JComboBox<Event> cmbEvent = new JComboBox<>();
    final JComboBox<Object> cmbFilter = new JComboBox<>();
    final JButton btnRegister = new JButton("Register"), btnClear = new JButton("Clear");
    final JButton btnPrev = new JButton("< Prev"), btnNext = new JButton("Next >");
    final JLabel lblStatus = new JLabel("Ready."), lblPage = new JLabel("Page 1");
    final JTabbedPane tabs = new JTabbedPane();
    final DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Student ID", "Name", "E-mail", "Event", "Registered at"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final RegistrationService service;
    private int page = 0, pages = 1;

    public RegistrationFrame(RegistrationService service) {
        super("Campus Event Registration");
        this.service = service;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setJMenuBar(buildMenu());

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8); g.anchor = GridBagConstraints.WEST;
        String[] labels = {"Student ID", "Full name", "E-mail", "Event"};
        JComponent[] fields = {txtId, txtName, txtEmail, cmbEvent};
        for (int i = 0; i < labels.length; i++) {
            g.gridx = 0; g.gridy = i; form.add(new JLabel(labels[i]), g);
            g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; form.add(fields[i], g); g.fill = GridBagConstraints.NONE;
        }
        JPanel btns = new JPanel(); btns.add(btnRegister); btns.add(btnClear);
        g.gridx = 1; g.gridy = 4; form.add(btns, g);
        JPanel regTab = new JPanel(new BorderLayout()); regTab.add(form, BorderLayout.NORTH);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Filter by event:")); top.add(cmbFilter);
        JPanel nav = new JPanel(); nav.add(btnPrev); nav.add(lblPage); nav.add(btnNext);
        JPanel viewTab = new JPanel(new BorderLayout());
        viewTab.add(top, BorderLayout.NORTH); viewTab.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER); viewTab.add(nav, BorderLayout.SOUTH);

        tabs.addTab("Register", regTab); tabs.addTab("Registrations", viewTab);
        add(tabs, BorderLayout.CENTER);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8)); add(lblStatus, BorderLayout.SOUTH);

        btnRegister.addActionListener(e -> doRegister());
        txtEmail.addActionListener(e -> doRegister());
        btnClear.addActionListener(e -> { txtId.setText(""); txtName.setText(""); txtEmail.setText(""); setStatus("Cleared.", false); });
        cmbFilter.addActionListener(e -> { page = 0; loadPage(); });
        btnPrev.addActionListener(e -> { if (page > 0) { page--; loadPage(); } });
        btnNext.addActionListener(e -> { if (page + 1 < pages) { page++; loadPage(); } });
        setMinimumSize(new Dimension(760, 460)); setSize(820, 480);
        loadEvents();
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File"), view = new JMenu("View"), help = new JMenu("Help");
        JMenuItem export = new JMenuItem("Export registrations to CSV...");
        JMenuItem exit = new JMenuItem("Exit"), refresh = new JMenuItem("Refresh"), about = new JMenuItem("About");
        export.addActionListener(e -> chooseAndExport());
        exit.addActionListener(e -> dispose());
        refresh.addActionListener(e -> loadPage());
        about.addActionListener(e -> JOptionPane.showMessageDialog(this, "Campus Event Registration\nSwing + JDBC (MySQL)\nCSA09 Assignment 4", "About", JOptionPane.INFORMATION_MESSAGE));
        file.add(export); file.addSeparator(); file.add(exit); view.add(refresh); help.add(about);
        bar.add(file); bar.add(view); bar.add(help);
        return bar;
    }

    void setStatus(String s, boolean error) { lblStatus.setForeground(error ? Color.RED : new Color(0, 110, 0)); lblStatus.setText(s); }
    private Integer selectedEventId() { Object o = cmbFilter.getSelectedItem(); return o instanceof Event ? ((Event) o).getId() : null; }

    private void loadEvents() {
        new SwingWorker<List<Event>, Void>() {
            protected List<Event> doInBackground() throws Exception { return service.events(); }
            protected void done() {
                try {
                    cmbFilter.addItem("All events");
                    for (Event e : get()) { cmbEvent.addItem(e); cmbFilter.addItem(e); }
                } catch (Exception ex) { setStatus("Could not load events: " + cause(ex), true); }
            }
        }.execute();
    }

    void doRegister() {
        btnRegister.setEnabled(false); setStatus("Saving...", false);
        final String id = txtId.getText(), name = txtName.getText(), mail = txtEmail.getText(); final Event ev = (Event) cmbEvent.getSelectedItem();
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception { service.register(id, name, mail, ev); return null; }
            protected void done() {
                try { get(); loadPage("Registered " + id.trim() + " for " + ev.getName() + "."); }
                catch (Exception ex) {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    setStatus((c instanceof DuplicateRegistrationException ? "DUPLICATE: " : c instanceof IllegalArgumentException ? "INVALID: " : "ERROR: ") + c.getMessage(), true);
                } finally { btnRegister.setEnabled(true); }
            }
        }.execute();
    }

    void loadPage() { loadPage(null); }
    void loadPage(String okMessage) {
        final Integer ev = selectedEventId(); final int p = page;
        setStatus("Loading page " + (p + 1) + "...", false);
        new SwingWorker<Object[], Void>() {
            protected Object[] doInBackground() throws Exception { return new Object[]{service.page(ev, p), service.totalPages(ev), service.totalRows(ev)}; }
            @SuppressWarnings("unchecked") protected void done() {
                try {
                    Object[] r = get(); pages = (Integer) r[1];
                    model.setRowCount(0);
                    for (Registration x : (List<Registration>) r[0])
                        model.addRow(new Object[]{x.getId(), x.getStudentId(), x.getStudentName(), x.getEmail(), x.getEventName(), x.getRegisteredAt()});
                    lblPage.setText("Page " + (p + 1) + " of " + pages + "  (" + r[2] + " rows)");
                    btnPrev.setEnabled(p > 0); btnNext.setEnabled(p + 1 < pages);
                    setStatus(okMessage != null ? okMessage : "Loaded " + model.getRowCount() + " row(s).", false);
                } catch (Exception ex) { setStatus("Load failed: " + cause(ex), true); }
            }
        }.execute();
    }

    private void chooseAndExport() {
        JFileChooser fc = new JFileChooser(); fc.setSelectedFile(new File("registrations.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) exportTo(fc.getSelectedFile());
    }
    void exportTo(File f) {
        final Integer ev = selectedEventId();
        setStatus("Exporting...", false);
        new SwingWorker<Integer, Void>() {
            protected Integer doInBackground() throws Exception { try (Writer w = new FileWriter(f)) { return service.exportCsv(ev, w); } }
            protected void done() { try { setStatus("Exported " + get() + " rows to " + f.getName(), false); } catch (Exception ex) { setStatus("Export failed: " + cause(ex), true); } }
        }.execute();
    }
    private static String cause(Exception e) { return e.getCause() != null ? e.getCause().getMessage() : e.getMessage(); }
}

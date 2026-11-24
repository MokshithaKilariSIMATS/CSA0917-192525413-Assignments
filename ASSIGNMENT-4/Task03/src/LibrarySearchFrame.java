import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Campus library search screen.
 * Layout map:
 *  JFrame content (BorderLayout)
 *   NORTH  : titleLabel (centred, in a FlowLayout panel)
 *   CENTER : body panel (BorderLayout)
 *              NORTH  : searchRow (FlowLayout)  [Search for][field][Category][Search]
 *              CENTER : JScrollPane(JTable)      <- grows with window
 *   SOUTH  : footer panel (BorderLayout)
 *              CENTER : buttonRow (FlowLayout)   [Prev][Next][Reserve]
 *              SOUTH  : status label
 */
public class LibrarySearchFrame extends JFrame {
    final JTextField txtSearch = new JTextField(22);
    final JComboBox<String> cmbCategory = new JComboBox<>(new String[]{"All", "Computer Science", "Mathematics", "Electronics", "Literature"});
    final JButton btnSearch = new JButton("Search"), btnPrev = new JButton("Prev"), btnNext = new JButton("Next"), btnReserve = new JButton("Reserve");
    final JLabel lblStatus = new JLabel("Enter a keyword and press Search.");
    final DefaultTableModel model = new DefaultTableModel(new String[]{"Title", "Author", "Category", "Available"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    final JTable table = new JTable(model);
    private static final int PAGE = 5;
    private int page = 0;
    private List<String[]> hits = new ArrayList<>();

    private static final String[][] CATALOG = {
        {"Programming in Java", "Herbert Schildt", "Computer Science", "Yes"},
        {"Core Java Volume I", "Cay Horstmann", "Computer Science", "Yes"},
        {"Effective Java", "Joshua Bloch", "Computer Science", "No"},
        {"Computer Vision: Algorithms", "Richard Szeliski", "Computer Science", "Yes"},
        {"Data Structures in Java", "Mark Weiss", "Computer Science", "Yes"},
        {"Operating System Concepts", "Silberschatz", "Computer Science", "No"},
        {"Linear Algebra Done Right", "Sheldon Axler", "Mathematics", "Yes"},
        {"Discrete Mathematics", "Kenneth Rosen", "Mathematics", "Yes"},
        {"Probability and Statistics", "Walpole", "Mathematics", "Yes"},
        {"Digital Design", "Morris Mano", "Electronics", "Yes"},
        {"Microelectronic Circuits", "Sedra Smith", "Electronics", "No"},
        {"Godan", "Premchand", "Literature", "Yes"},
        {"The Guide", "R. K. Narayan", "Literature", "Yes"},
    };

    public LibrarySearchFrame() {
        super("Campus Library Search");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JLabel title = new JLabel("CAMPUS LIBRARY CATALOGUE");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.add(title);

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchRow.add(new JLabel("Search for:")); searchRow.add(txtSearch);
        searchRow.add(new JLabel("Category:")); searchRow.add(cmbCategory); searchRow.add(btnSearch);

        JPanel body = new JPanel(new BorderLayout(0, 6));
        body.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        body.add(searchRow, BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        buttonRow.add(btnPrev); buttonRow.add(btnNext); buttonRow.add(btnReserve);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(2, 10, 6, 10));
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(buttonRow, BorderLayout.CENTER); footer.add(lblStatus, BorderLayout.SOUTH);

        add(titlePanel, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        btnSearch.addActionListener(e -> search());
        txtSearch.addActionListener(e -> search());
        btnPrev.addActionListener(e -> { if (page > 0) { page--; showPage(); } });
        btnNext.addActionListener(e -> { if ((page + 1) * PAGE < hits.size()) { page++; showPage(); } });
        btnReserve.addActionListener(e -> reserve());
        setMinimumSize(new Dimension(520, 320));
        setSize(800, 480);
        search();
    }

    private void search() {
        String kw = txtSearch.getText().trim().toLowerCase();
        String cat = (String) cmbCategory.getSelectedItem();
        hits = new ArrayList<>();
        for (String[] b : CATALOG)
            if ((cat.equals("All") || b[2].equals(cat)) && (b[0].toLowerCase().contains(kw) || b[1].toLowerCase().contains(kw))) hits.add(b);
        page = 0; showPage();
    }
    private void showPage() {
        model.setRowCount(0);
        int from = page * PAGE, to = Math.min(from + PAGE, hits.size());
        for (int i = from; i < to; i++) model.addRow(hits.get(i));
        btnPrev.setEnabled(page > 0); btnNext.setEnabled(to < hits.size());
        lblStatus.setText(hits.isEmpty() ? "No books found." : "Showing " + (from + 1) + "-" + to + " of " + hits.size() + " result(s).");
    }
    private void reserve() {
        int r = table.getSelectedRow();
        if (r < 0) { lblStatus.setText("Select a book first."); return; }
        if (model.getValueAt(r, 3).equals("No")) lblStatus.setText("'" + model.getValueAt(r, 0) + "' is not available.");
        else lblStatus.setText("Reserved: " + model.getValueAt(r, 0));
    }
    public static void main(String[] a) { SwingUtilities.invokeLater(() -> new LibrarySearchFrame().setVisible(true)); }
}

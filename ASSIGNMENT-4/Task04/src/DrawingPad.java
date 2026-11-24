import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/** TASK 4 - REPAIRED: points are stored, label updated, repaint() requested, paint() redraws everything. */
public class DrawingPad extends JFrame {
    private final List<Point> points = new ArrayList<>();        // TODO 1 (done): store clicked points
    private final JLabel lblCount = new JLabel("Clicks: 0");
    final JPanel canvas = new JPanel() {
        @Override public void paint(Graphics g) {                // TODO 2 (done): draw stored points
            super.paint(g);                                      // clears the background first
            g.setColor(Color.RED);
            for (Point p : points) g.fillOval(p.x - 5, p.y - 5, 10, 10);
        }
    };

    public DrawingPad() {
        super("Drawing Pad - FIXED");
        canvas.setBackground(Color.WHITE);
        add(canvas, BorderLayout.CENTER);
        add(lblCount, BorderLayout.SOUTH);
        canvas.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {   // @Override => compile-time check of the name
                points.add(e.getPoint());
                lblCount.setText("Clicks: " + points.size());
                canvas.repaint();                                // ask Swing to call paint() again
            }
        });
        setSize(420, 320);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
    public static void main(String[] a) { SwingUtilities.invokeLater(() -> new DrawingPad().setVisible(true)); }
}

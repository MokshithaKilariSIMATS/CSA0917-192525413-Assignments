import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Intermediate step: handler now runs and the label updates, but dots are painted with getGraphics(). */
public class HalfFixedPad extends JFrame {
    int count = 0;
    JLabel lblCount = new JLabel("Clicks: 0");
    JPanel canvas = new JPanel();

    HalfFixedPad() {
        super("Drawing Pad - HALF FIXED (getGraphics)");
        canvas.setBackground(Color.WHITE);
        add(canvas, BorderLayout.CENTER);
        add(lblCount, BorderLayout.SOUTH);
        canvas.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                count++; lblCount.setText("Clicks: " + count);
                Graphics g = canvas.getGraphics();
                g.setColor(Color.RED); g.fillOval(e.getX() - 5, e.getY() - 5, 10, 10);
                g.dispose();                       // dot exists only on screen - not in any data structure
            }
        });
        setSize(420, 320);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
    public static void main(String[] a) { new HalfFixedPad().setVisible(true); }
}

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** TASK 4 - AS SUPPLIED (reconstruction): the dot never appears and the counter never changes. */
public class BrokenDrawingPad extends JFrame {
    int count = 0;
    JLabel lblCount = new JLabel("Clicks: 0");
    JPanel canvas = new JPanel();

    BrokenDrawingPad() {
        super("Drawing Pad - BROKEN");
        canvas.setBackground(Color.WHITE);
        add(canvas, BorderLayout.CENTER);
        add(lblCount, BorderLayout.SOUTH);
        // BUG 1: no canvas.addMouseListener(...) call anywhere
        setSize(420, 320);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    class ClickHandler extends MouseAdapter {
        // BUG 2: wrong method name - MouseAdapter declares mouseClicked(), so this is just an
        //        unrelated method. It compiles silently and is never called by Swing.
        public void mouseClick(MouseEvent e) {
            Graphics g = canvas.getGraphics();      // BUG 3: draws directly, never stored
            g.fillOval(e.getX(), e.getY(), 10, 10);
            // BUG 4: count never incremented, label never updated
        }
    }
    public static void main(String[] a) { new BrokenDrawingPad().setVisible(true); }
}

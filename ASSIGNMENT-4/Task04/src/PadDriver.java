import java.awt.*;
import java.awt.event.InputEvent;
import javax.swing.*;

/** Clicks with a real Robot mouse, then simulates minimise/restore by hiding and re-showing the window. */
public class PadDriver {
    static Robot robot;
    static void click(JComponent c, int dx, int dy) throws Exception {
        Point p = c.getLocationOnScreen();
        robot.mouseMove(p.x + dx, p.y + dy); robot.delay(150);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK); robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK); robot.delay(250);
    }
    static void clicks(JComponent c) throws Exception { click(c, 60, 50); click(c, 150, 120); click(c, 250, 80); click(c, 320, 160); }

    static void restoreCycle(JFrame f) throws Exception {
        SwingUtilities.invokeAndWait(() -> f.setVisible(false)); Shot.pause(600);   // "minimise"
        SwingUtilities.invokeAndWait(() -> f.setVisible(true));  Shot.pause(900);   // "restore"
    }

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        JFrame.setDefaultLookAndFeelDecorated(true);
        // --- 1. broken pad
        BrokenDrawingPad b = new BrokenDrawingPad();
        SwingUtilities.invokeAndWait(() -> { b.setLocation(30, 30); b.setVisible(true); });
        Shot.pause(700); clicks(b.canvas);
        System.out.println("BrokenDrawingPad : 4 real mouse clicks sent, label shows -> " + b.lblCount.getText());
        Shot.take("../output/task4_1_broken_after_clicks.png");
        SwingUtilities.invokeAndWait(b::dispose);
        // --- 2. half fixed
        HalfFixedPad h = new HalfFixedPad();
        SwingUtilities.invokeAndWait(() -> { h.setLocation(30, 30); h.setVisible(true); });
        Shot.pause(700); clicks(h.canvas);
        System.out.println("HalfFixedPad     : before minimise, label -> " + h.lblCount.getText());
        Shot.take("../output/task4_2_halffixed_before_minimise.png");
        restoreCycle(h);
        System.out.println("HalfFixedPad     : after  restore , label -> " + h.lblCount.getText() + "  (dots lost: repaint cleared them)");
        Shot.take("../output/task4_3_halffixed_after_restore.png");
        SwingUtilities.invokeAndWait(h::dispose);
        // --- 3. fixed
        DrawingPad d = new DrawingPad();
        SwingUtilities.invokeAndWait(() -> { d.setLocation(30, 30); d.setVisible(true); });
        Shot.pause(700); clicks(d.canvas);
        System.out.println("DrawingPad       : before minimise, label -> " + ((JLabel) ((BorderLayout) d.getContentPane().getLayout()).getLayoutComponent(BorderLayout.SOUTH)).getText());
        Shot.take("../output/task4_4_fixed_before_minimise.png");
        restoreCycle(d);
        System.out.println("DrawingPad       : after  restore , dots still drawn (stored in List<Point>)");
        Shot.take("../output/task4_5_fixed_after_restore.png");
        System.exit(0);
    }
}

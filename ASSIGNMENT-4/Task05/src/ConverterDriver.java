import java.awt.*;
import javax.swing.*;

/** Runs all four versions: valid conversion, invalid amount, Enter key, window closing. */
public class ConverterDriver {
    static void closeDialogs() throws Exception {
        SwingUtilities.invokeAndWait(() -> { for (Window w : Window.getWindows()) if (w instanceof JDialog && w.isShowing()) w.dispose(); });
    }
    static void moveDialogs() throws Exception {
        SwingUtilities.invokeAndWait(() -> { for (Window w : Window.getWindows()) if (w instanceof JDialog && w.isShowing()) w.setLocation(w.getX() + 20, w.getY() + 130); });
        Shot.pause(300);
    }
    static void run(String name, JFrame f, boolean shots) throws Exception {
        SwingUtilities.invokeAndWait(() -> { f.setLocation(40, 40); f.setVisible(true); });
        Shot.pause(600);
        SwingUtilities.invokeAndWait(() -> { try {
            java.lang.reflect.Field fa = f.getClass().getDeclaredField("txtAmount"); fa.setAccessible(true);
            ((JTextField) fa.get(f)).setText("250"); } catch (Exception e) { throw new RuntimeException(e); } });
        // click via the button -> modal dialog opens, so use invokeLater
        JButton btn = (JButton) f.getClass().getDeclaredField("btnConvert").get(f);
        SwingUtilities.invokeLater(btn::doClick);
        Shot.pause(800);
        if (shots) { moveDialogs(); Shot.take("../output/task5_" + name + "_valid.png"); }
        closeDialogs();
        JTextField t = (JTextField) f.getClass().getDeclaredField("txtAmount").get(f);
        SwingUtilities.invokeAndWait(() -> t.setText("abc"));
        SwingUtilities.invokeLater(() -> t.postActionEvent());      // Enter key path
        Shot.pause(800);
        if (shots) { moveDialogs(); Shot.take("../output/task5_" + name + "_invalid.png"); }
        closeDialogs();
        JLabel lbl = (JLabel) f.getClass().getDeclaredField("lblResult").get(f);
        System.out.printf("%-26s after Enter with 'abc' -> label: %s%n", name, lbl.getText());
        SwingUtilities.invokeAndWait(() -> f.dispatchEvent(new java.awt.event.WindowEvent(f, java.awt.event.WindowEvent.WINDOW_CLOSING)));
        Shot.pause(300);
    }
    public static void main(String[] a) throws Exception {
        JFrame.setDefaultLookAndFeelDecorated(true); JDialog.setDefaultLookAndFeelDecorated(true);
        System.out.println(ConversionLogic.convert("250", "USD", "INR"));
        run("1_frame_listener", Converter1_FrameListener.create(), true);
        run("2_named_inner", Converter2_NamedInner.create(), true);
        run("3_anonymous", Converter3_Anonymous.create(), true);
        run("4_lambda", Converter4_Lambda.create(), true);
        System.exit(0);
    }
}

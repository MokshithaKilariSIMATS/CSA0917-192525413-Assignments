import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Test-harness helper: captures the real (virtual) screen with java.awt.Robot. */
public class Shot {
    public static void take(String path) throws Exception {
        Robot r = new Robot();
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        BufferedImage img = r.createScreenCapture(new Rectangle(0, 0, d.width, d.height));
        File f = new File(path);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
    }
    public static void pause(int ms) throws Exception { Thread.sleep(ms); }
}

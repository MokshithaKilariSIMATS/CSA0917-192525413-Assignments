import java.awt.event.*;

/** Compile-time demonstration: @Override exposes the misspelt listener method. */
public class OverrideDemo extends MouseAdapter {
    @Override
    public void mouseClick(MouseEvent e) {     // should be mouseClicked
        System.out.println("never reached");
    }
}

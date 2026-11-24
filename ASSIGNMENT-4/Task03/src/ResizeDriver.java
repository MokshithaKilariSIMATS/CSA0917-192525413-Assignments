import javax.swing.*;

/** Opens the screen at three sizes and captures each to show the resizing behaviour. */
public class ResizeDriver {
    public static void main(String[] a) throws Exception {
        JFrame.setDefaultLookAndFeelDecorated(true);
        final LibrarySearchFrame[] f = new LibrarySearchFrame[1];
        SwingUtilities.invokeAndWait(() -> { f[0] = new LibrarySearchFrame(); f[0].setLocation(20, 20); f[0].setVisible(true); });
        int[][] sizes = {{560, 360}, {800, 480}, {1100, 640}};
        String[] names = {"small", "medium", "large"};
        for (int i = 0; i < 3; i++) {
            final int w = sizes[i][0], h = sizes[i][1];
            SwingUtilities.invokeAndWait(() -> { f[0].setSize(w, h); f[0].txtSearch.setText("java"); f[0].btnSearch.doClick(); });
            Shot.pause(700);
            System.out.printf("%-6s window %dx%d  | table viewport %dx%d | search row height %d | footer buttons height %d%n", names[i], w, h,
                f[0].table.getParent().getWidth(), f[0].table.getParent().getHeight(),
                f[0].btnSearch.getParent().getHeight(), f[0].btnReserve.getParent().getHeight());
            Shot.take("../output/task3_" + names[i] + "_" + w + "x" + h + ".png");
        }
        SwingUtilities.invokeAndWait(() -> { f[0].txtSearch.setText(""); f[0].btnSearch.doClick(); f[0].btnNext.doClick(); f[0].table.setRowSelectionInterval(0, 0); f[0].btnReserve.doClick(); });
        Shot.pause(500); Shot.take("../output/task3_reserve_paging.png");
        System.exit(0);
    }
}

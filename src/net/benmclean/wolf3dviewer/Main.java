package net.benmclean.wolf3dviewer;

import javax.swing.JFrame;

public class Main extends JFrame {
    public Main() {
        setTitle("WOLF3D-Viewer");
        setSize(300, 200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    public static void main(String[] args) throws Exception {
        DownloadShareware.WOLF3DShareware();
        Main ex = new Main();
        ex.setVisible(true);
    }
}

package com.albert.wolf3d.core.io.file;

import java.awt.image.IndexColorModel;
import java.io.*;

public class PaletteFileReader {
    //region Static variables
    private static PaletteFileReader instance;
    private static final int COLORS = 256;
    //endregion

    //region Constructors
    public PaletteFileReader() {
    }

    //endregion

    //region Static methods
    public static PaletteFileReader get() {
        if (instance == null) {
            instance = new PaletteFileReader();
        }
        return instance;
    }

    private static IndexColorModel colorModelFromPAL(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        if (!in.readLine().equals("JASC-PAL") || !in.readLine().equals("0100")) {
            throw new UnsupportedEncodingException("Palette '" + file.getName() + "' is an incorrectly formatted JASC palette.");
        }
        int numColors = Integer.parseInt(in.readLine());
        if (numColors != COLORS) {
            throw new UnsupportedEncodingException("Palette '" + file.getName() + "' does not contain exactly 256 colors.");
        }
        String line;
        String[] tokens = null;
        byte[] r = new byte[COLORS];
        byte[] g = new byte[COLORS];
        byte[] b = new byte[COLORS];

        for (int x = 0; x < COLORS; x++) {
            line = in.readLine();
            if (line != null) {
                tokens = line.split("\\s");
            }
            if (tokens == null || tokens.length != 3) {
                throw new UnsupportedEncodingException("Palette '" + file.getName() + "' is an incorrectly formatted JASC palette.");
            }

            r[x] = (byte) Integer.parseInt(tokens[0]);
            g[x] = (byte) Integer.parseInt(tokens[1]);
            b[x] = (byte) Integer.parseInt(tokens[2]);
        }
        return new IndexColorModel(8, COLORS, r, g, b, 255);
    }
    //endregion

    //region Instance methods
    public IndexColorModel read(File file) throws IOException {
        String fileName = file.getName();
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

        switch (extension) {
            case "pal":
                return colorModelFromPAL(file);
            default:
                throw new UnsupportedOperationException("Unsupported palette type '" + extension + "'");
        }
    }
    //endregion
}

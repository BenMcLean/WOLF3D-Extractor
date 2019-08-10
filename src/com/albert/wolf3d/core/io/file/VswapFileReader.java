package com.albert.wolf3d.core.io.file;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VswapFileReader {
    //region Static variables
    private static final int NUM_DATA_OFS = 64;
    //endregion

    //region Inner classes
    public static class VswapFileData {
        private List<BufferedImage> graphics;
        private int wallStartIndex, wallEndIndex;
        private int spriteStartIndex, spriteEndIndex;

        public VswapFileData() {
        }

        public List<BufferedImage> getGraphics() {
            return graphics;
        }

        public void setGraphics(List<BufferedImage> graphics) {
            this.graphics = graphics;
        }

        public int getWallStartIndex() {
            return wallStartIndex;
        }

        public void setWallStartIndex(int wallStartIndex) {
            this.wallStartIndex = wallStartIndex;
        }

        public int getWallEndIndex() {
            return wallEndIndex;
        }

        public void setWallEndIndex(int wallEndIndex) {
            this.wallEndIndex = wallEndIndex;
        }

        public int getSpriteStartIndex() {
            return spriteStartIndex;
        }

        public void setSpriteStartIndex(int spriteStartIndex) {
            this.spriteStartIndex = spriteStartIndex;
        }

        public int getSpriteEndIndex() {
            return spriteEndIndex;
        }

        public void setSpriteEndIndex(int spriteEndIndex) {
            this.spriteEndIndex = spriteEndIndex;
        }
    }
    //endregion

    //region Constructors
    private VswapFileReader() { }
    //endregion

    //region Static methods
    public static VswapFileData read(final File file, final int dimension, final IndexColorModel colorModel) throws IOException {
        // parse header info
        final WolfFileWrapper in = new WolfFileWrapper(file);
        final int pageSize = dimension * dimension;
        final int chunks = in.readWord();
        final int spritePageOffset = in.readWord();
        final int soundPageOffset = in.readWord();
        final int graphicChunks = soundPageOffset;
        final int[] pageOffsets = new int[graphicChunks];
        int dataStart = 0;

        for (int x = 0; x < graphicChunks; x++) {
            pageOffsets[x] = in.readDWord();
            if (x == 0) {
                dataStart = pageOffsets[0];
            }
            if (pageOffsets[x] != 0 && (pageOffsets[x] < dataStart || pageOffsets[x] > in.getTotalSpace())) {
                throw new UnsupportedEncodingException("VSWAP file '" + in.getName() + "' contains invalid page offsets.");
            }
        }

        // parse graphic data
        List<BufferedImage> graphics = new ArrayList<>();
        DataBufferByte dbuf = new DataBufferByte(pageSize);
        final int[] bitMasks = new int[]{ (byte) 0xFF };
        SampleModel sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_BYTE, dimension, dimension, bitMasks);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dbuf, null);

        int page;
        // read in walls
        for (page = 0; page < spritePageOffset; page++) {
            in.seek(pageOffsets[page]);
            for (int col = 0; col < dimension; col++) {
                for (int row = 0; row < dimension; row++) {
                    dbuf.setElem(dimension * row + col, (byte) in.readByte());
                }
            }
            BufferedImage img = new BufferedImage(dimension, dimension, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
            img.setData(raster);
            graphics.add(img);
        }
        
        // read in sprites
        for ( ; page < graphicChunks; page++) {
            int leftpix, rightpix, starty, endy, newstart;
            int offset = pageOffsets[page];
            int[] dataOfs = new int[NUM_DATA_OFS]; // 64 even in hires?
            long oldpos;
            byte col;

            // Clear the byte buffer
            Arrays.fill(dbuf.getData(), (byte) 0xFF);

            in.seek(offset);

            leftpix = in.readWord();
            rightpix = in.readWord();
            int totalOfs = rightpix - leftpix + 1;
            for (int j = 0; j < totalOfs; j++) {
                dataOfs[j] = in.readWord();
            }

            for (int spot = 0; leftpix <= rightpix; leftpix++, spot++) {
                in.seek(offset + dataOfs[spot]);

                while((endy = in.readWord()) != 0) {
                    endy >>= 1;
                    newstart = in.readWord();
                    starty = in.readWord() >> 1;
                    oldpos = in.getPosition(); // reading in the colors jumps to a new spot

                    in.seek(offset + newstart + starty);
                    for ( ; starty < endy; starty++) {
                        col = (byte) in.readByte();
                        dbuf.setElem(starty * dimension + leftpix, col);
                    }
                    in.seek(oldpos); // go back to "endy data"
                }
            }

            BufferedImage img = new BufferedImage(dimension, dimension, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
            img.setData(raster);

            graphics.add(img);
        }

        // package results
        VswapFileData fileData = new VswapFileData();
        fileData.setGraphics(graphics);
        fileData.setWallStartIndex(0);
        fileData.setWallEndIndex(spritePageOffset);
        fileData.setSpriteStartIndex(spritePageOffset);
        fileData.setSpriteEndIndex(soundPageOffset);
        return fileData;
    }

    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);
        VswapFileData data = VswapFileReader.read(file, 64, PaletteFileReader.get().read(new File(args[1])));
        
        for (int i = data.getWallStartIndex(), x = 0; i < data.getWallEndIndex(); i++) {
            boolean even = i % 2 == 0;
            if (even) {
                x++;
            }
            ImageIO.write(data.getGraphics().get(i), "png", new File(String.format("%s/%s.png", args[2], x + (even ? "_1" : "_2"))));
        }
    }
    //endregion
}

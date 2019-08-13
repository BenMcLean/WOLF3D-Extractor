package com.albert.wolf3d.core.io.file;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

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
    private VswapFileReader() {
    }
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
        final int[] bitMasks = new int[]{(byte) 0xFF};
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
        for (; page < graphicChunks; page++) {
            in.seek(pageOffsets[page]);
            // https://devinsmith.net/backups/bruce/wolf3d.html
            // Each sprite is a 64 texel wide and 64 texel high block.
            int[][] sprite = new int[64][64];
            for (int[] row : sprite)
                Arrays.fill(row, 255);
            // It is a sparse array and is packed as RLE columns.
            // The first part of the sprite is two short integers (two bytes each) that tell the left and right extents of the sprite. By extents I mean that the left extent is the first column on the left with a colored texel in it. And the right extent is the last column on the right that has a colored texel in it.
            int leftExtent = in.readWord(),
                rightExtent = in.readWord();
            // Immediately after this data (four bytes into the sprite) is a list of two byte offsets into the file that has the drawing information for each of those columns. The first is the offset for the left extent column and the last is the offset for the right extent column with all the other column offsets stored sequentially between them.
            int[] columnOffsets = new int[rightExtent - leftExtent];
            for (int x = 0; x < columnOffsets.length; x++)
                columnOffsets[x] = in.readWord();
            long trexels = in.getPosition(); // The area between the end of the column segment offset list and the first column drawing instructions is the actual texels of the sprite.
            // Now comes the interesting part. Each of these offsets points to a possible list of drawing commands for the scalers to use to draw the sprite. Each column segment instruction is a series of 6 bytes. If the first two bytes of the column segment instructions is zero, then that is the end of that column and we can move on to the next column.
            Map<Integer, List<int[]>> commands = new HashMap<>();
            for(int x=0; x < columnOffsets.length; x++) {
                in.seek(columnOffsets[x]);
                List<int[]> list = new ArrayList<>();
                int a;
                do {
                    a = in.readWord();
                    list.add(new int[] {a, in.readWord(), in.readWord()});
                } while (a != 0);
                commands.put(x, list);
            }
            // To interpret these columns was the tricky part. Each of these offsets points to an offset into an array of short unsigned integers in the original game which are the offsets of individual rows in the unwound column drawers.
            in.seek(trexels);
            for(int column=0; column < columnOffsets.length; column++) {
                int x = leftExtent + column;
                for (int[] command : commands.get(column)) {
                    // So if we take the starting position (which is the first two bytes) and divide it by two, we have one end of the column segment.
                    int startingPosition = command[0] / 2;
                    // The other end of that segment is the last two bytes (of the six byte instruction) and we also divide that by two to get the ending position of that column segment.
                    int endingPosition = command[2] / 2;
                    // But where do we get the texels to draw from?
                    // The area between the end of the column segment offset list and the first column drawing instructions is the actual texels of the sprite. Only the colored texels are stored here and they are stored sequentially as are the column drawing instructions. There is a one to one correspondence between each drawing instruction and the texels stored here. Each column segment's height uses that many texels from this pool of texels.
                    for (int y = startingPosition; y < endingPosition; y++)
                        sprite[y][x] = in.readByte();
                }
            }

            for (int y=0; y < sprite.length; y++) {
                for (int x = 0; x < sprite[0].length; x++)
                    System.out.print(String.format ("%03d", sprite[y][x]) + ", ");
                System.out.println();
            }
            System.out.println();
            System.out.println();

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

    /**
     * @param args 0 is VSWAP.WL1, 1 is the Wolf3D JASC palette from WDC (the Wolf Data Compiler at http://winwolf3d.dugtrio17.com ), 2 is output folder
     * @throws Exception
     */
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

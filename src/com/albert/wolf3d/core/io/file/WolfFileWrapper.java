package com.albert.wolf3d.core.io.file;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WolfFileWrapper implements Closeable {
    //region Static variables
    private static final int CARMACK_NEAR = 0xA7;
    private static final int CARMACK_FAR = 0xA8;
    private static final int WORD_LENGTH = 2;
    private static final int DWORD_LENGTH = WORD_LENGTH * 2;
    //endregion

    //region Instance variables
    private final File file;
    private RandomAccessFile raf;
    private boolean closed;
    //endregion

    //region Inner classes
    //endregion

    //region Constructors
    public WolfFileWrapper(File file) throws IOException {
        this.file = file;
        reopen();
    }
    //endregion

    //region Static methods
    public static int[] rlewExpand(int[] carmackExpanded, int length, int tag) {
        int[] rawMapData = new int[length];
        int value, count, i, src_index ,dest_index;

        src_index = 1;
        dest_index = 0;

        do {
            value = carmackExpanded[src_index++]; // WORDS!!
            if (value != tag) {
                // uncompressed
                rawMapData[dest_index++] = value;
            } else {
                // compressed string
                count = carmackExpanded[src_index++];
                value = carmackExpanded[src_index++];
                for (i = 1; i <= count; i++) {
                    rawMapData[dest_index++] = value;
                }
            }

        } while (dest_index < length);

        return rawMapData;
    }
    //endregion

    //region Instance methods
    public int readByte() throws IOException {
        return raf.read();
    }

    public int readWord() throws IOException {
        byte[] buffer = new byte[Integer.BYTES];
        raf.read(buffer, 0, WORD_LENGTH);
        return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public int readDWord() throws IOException {
        byte[] buffer = new byte[Integer.BYTES];
        raf.read(buffer, 0, DWORD_LENGTH);
        return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public String getName() {
        return file.getName();
    }

    public long getPosition() throws IOException {
        return raf.getFilePointer();
    }

    public long getTotalSpace() { return file.getTotalSpace(); }

    public void reopen() throws IOException {
        if (raf == null) {
            raf = new RandomAccessFile(file, "r");
            closed = false;
        }
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        try {
            raf.close();
            raf = null;
            closed = true;
        } catch (Exception ignore) { }
    }

    public void seek(long position) throws IOException {
        raf.seek(position);
    }

    public int[] carmackExpand(long position) throws IOException {
        ////////////////////////////
        // Get to the correct chunk
        int length;
        int ch, chhigh, count, offset, index;

        seek(position);

        // First word is expanded length
        length = readWord();
        int[] expandedWords = new int[length]; // array of WORDS

        length /= 2;

        index = 0;

        while (length > 0) {
            ch = readWord();
            chhigh = ch >> 8;

            if (chhigh == CARMACK_NEAR) {
                count = (ch & 0xFF);

                if (count == 0) {
                    ch |= readByte();
                    expandedWords[index++] = ch;
                    length--;
                } else {
                    offset = readByte();
                    length -= count;
                    if (length < 0) {
                        return expandedWords;
                    }
                    while ((count--) > 0) {
                        expandedWords[index] = expandedWords[index - offset];
                        index++;
                    }
                }
            } else if (chhigh == CARMACK_FAR) {
                count = (ch & 0xFF);

                if (count == 0) {
                    ch |= readByte();
                    expandedWords[index++] = ch;
                    length--;
                } else {
                    offset = readWord();
                    length -= count;
                    if (length < 0) {
                        return expandedWords;
                    }
                    while ((count--) > 0) {
                        expandedWords[index++] = expandedWords[offset++];
                    }
                }
            } else {
                expandedWords[index++] = ch;
                length--;
            }
        }

        return expandedWords;
    }
    //endregion
}

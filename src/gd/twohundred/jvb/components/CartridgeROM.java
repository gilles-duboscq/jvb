package gd.twohundred.jvb.components;

import gd.twohundred.jvb.components.interfaces.ReadOnlyMemory;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;

import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardOpenOption.READ;

public class CartridgeROM implements ReadOnlyMemory {
    public static final int MIN_SIZE = 0x400;
    public static final int MAX_SIZE = 0x100_0000;
    public static final int START = 0x07000000;
    private final MappedByteBuffer cartridgeData;
    private final int addressMask;

    public CartridgeROM(Path cartridgePath) throws IOException {
        System.out.println("Opening " + cartridgePath + "...");
        FileChannel fileChannel = FileChannel.open(cartridgePath, READ);
        long size = fileChannel.size();
        assert size >= MIN_SIZE && size <= MAX_SIZE && Long.bitCount(size) == 1;
        cartridgeData = fileChannel.map(READ_ONLY, 0, size);
        cartridgeData.order(ByteOrder.LITTLE_ENDIAN);
        addressMask = (int) (size - 1);
    }

    @Override
    public int getByte(int address) {
        int effectiveAddress = address & addressMask;
        return cartridgeData.get(effectiveAddress) & 0xff;
    }

    @Override
    public int getHalfWord(int address) {
        int effectiveAddress = address & addressMask;
        return cartridgeData.getShort(effectiveAddress) & 0xffff;
    }

    @Override
    public int getWord(int address) {
        int effectiveAddress = address & addressMask;
        return cartridgeData.getInt(effectiveAddress);
    }

    @Override
    public int getStart() {
        return START;
    }

    @Override
    public int getSize() {
        return MAX_SIZE;
    }

    private static final Charset SHIFT_JIS = Charset.forName("SJIS");
    private static final int GAME_TITLE_START = 0xFFFFFDE0;
    private static final int GAME_TITLE_SIZE = 0x14;
    private static final int MAKER_CODE_START = 0xFFFFFDF9;
    private static final int MAKER_CODE_SIZE = 2;
    private static final int GAME_CODE_START = 0xFFFFFDFB;
    private static final int GAME_CODE_SIZE = 4;
    private static final int GAME_VERSION_POSITION = 0xFFFFFDFF;

    public String readString(int start, int length, Charset charset) {
        byte[] data = new byte[length];
        int strLength = length;
        for (int i = 0; i < length; i++) {
            data[i] = (byte) this.getByte(start + i);
            if (strLength == length && data[i] == 0) {
                strLength = i;
            }
        }
        return new String(data, 0, strLength, charset);
    }

    public String getGameTitle() {
        return readString(GAME_TITLE_START, GAME_TITLE_SIZE, SHIFT_JIS);
    }

    public String getMakerCode() {
        return readString(MAKER_CODE_START, MAKER_CODE_SIZE, US_ASCII);
    }

    public String getGameCode() {
        return readString(GAME_CODE_START, GAME_CODE_SIZE, US_ASCII);
    }

    public int getGameVersion() {
        return getByte(GAME_VERSION_POSITION);
    }
}

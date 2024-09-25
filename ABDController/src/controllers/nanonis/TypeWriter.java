package controllers.nanonis;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class TypeWriter {
    private OutputStream out;

    public TypeWriter(OutputStream out) {
        super();

        this.out = out;
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void writeHeader(String commandName, int bodyLen) throws IOException {
        out.write(commandName.getBytes());
        out.write(new byte[32 - commandName.getBytes().length]);
        try {
            writeUInt32(bodyLen);
            writeUInt16(1); // wants response
            writeUInt16(0); // padding
        } catch (UnsignedException e) {
            e.printStackTrace();
        }
    }

    public void writeInt(int value) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(value);
        out.write(bb.array());
    }

    public void writeString(String string) throws IOException {
        writeInt(string.getBytes().length);
        out.write(string.getBytes());

    }

    public void writeUInt16(int value) throws IOException, UnsignedException {
        if (value < 0) {
            throw new UnsignedException("writeUInt16 got negative number");
        }
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.putShort((short) (value & 0xFFFF));
        out.write(bb.array());
    }

    public void writeUInt32(long value) throws IOException, UnsignedException {
        if (value < 0) {
            throw new UnsignedException("writeUInt32 got negative number");
        }
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt((int) (value & 0xFFFFFFFF));
        out.write(bb.array());
    }

    public void writeFloat32(float value) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putFloat(value);
        out.write(bb.array());
    }

    public void writeFloat64(double value) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putDouble(value);
        out.write(bb.array());
    }

    public void writeBool(boolean value) throws IOException {
        try {
            writeUInt32(value ? 1 : 0);
        } catch (UnsignedException e) {
            // can never happen
        }
    }

    public void writeArrayInt1D(int[] vals) throws IOException {
        writeInt(vals.length);
        for (int val : vals) {
            writeInt(val);
        }
    }

    public void writeArrayFloat64_1D(double[] vals) throws IOException {
        writeInt(vals.length);
        for (double val : vals) {
            writeFloat64(val);
        }
    }

    public void writeArrayString1D(String[] strings) throws IOException {
        int size = 0;
        for (String s : strings) {
            size += s.getBytes().length + 4;
        }
        writeInt(size);
        writeInt(strings.length);
        for (String s : strings) {
            writeString(s);
        }
    }
}

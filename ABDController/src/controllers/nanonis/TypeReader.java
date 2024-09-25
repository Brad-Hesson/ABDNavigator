package controllers.nanonis;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class TypeReader {
    private DataInputStream in;

    public TypeReader(DataInputStream in) {
        super();
        this.in = in;
    }

    public String readHeader() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(32);
        in.readFully(bb.array());
        String name = new String(bb.array()).split("\0")[0];
        ByteBuffer _bb = ByteBuffer.allocate(8);
        in.readFully(_bb.array());
        return name;
    }

    public int readInt() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(4);
        in.readFully(bb.array());
        return bb.getInt();
    }

    public int readUInt16() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(2);
        in.readFully(bb.array());
        return bb.getShort();
    }

    public long readUInt32() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(4);
        in.readFully(bb.array());
        return bb.getInt();
    }

    public float readFloat32() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(4);
        in.readFully(bb.array());
        return bb.getFloat();
    }

    public double readFloat64() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(8);
        in.readFully(bb.array());
        return bb.getDouble();
    }

    public boolean readBool() throws IOException {
        return readUInt32() != 0;
    }

    public String readString() throws IOException {
        int len = readInt();
        ByteBuffer bb = ByteBuffer.allocate(len);
        in.readFully(bb.array());
        return new String(bb.array(), StandardCharsets.UTF_8);
    }

    public Optional<String> readError() throws IOException {
        boolean status = readBool();
        String desc = readString();
        if (status) {
            return Optional.of(desc);
        } else {
            return Optional.empty();
        }
    }

    public int[] readArrayInt1D() throws IOException {
        int len = readInt();
        int[] vals = new int[len];
        for (int i = 0; i < len; i++) {
            vals[i] = readInt();
        }
        return vals;
    }

    public float[][] readArrayFloat32_2D() throws IOException {
        int rows = readInt();
        int cols = readInt();
        float[][] vals = new float[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                vals[r][c] = readFloat32();
            }
        }
        return vals;
    }
}

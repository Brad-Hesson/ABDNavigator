package navigator;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class NanonisSXMFile {
    public HashMap<String, String> params = new HashMap<>();
    public HashMap<String, float[][][]> images = new HashMap<>();

    public int xPixels;
    public int yPixels;

    public NanonisSXMFile(String filePath) throws Exception {
        super();
        File file = new File(filePath);
        FileInputStream fin = new FileInputStream(file);
        DataInputStream in = new DataInputStream(fin);

        String currentParam = "";
        while (true) {
            String line = "";
            while (true) {
                char c = (char) in.readByte();
                if (c == '\n') {
                    break;
                }
                line += c;
            }
            if (line.contains(":SCANIT_END:")) {
                break;
            } else if (line.startsWith(":")) {
                currentParam = line.replaceAll(":", "").trim();
                params.put(currentParam, "");
            } else {
                String data = params.get(currentParam);
                data += line + "\n";
                params.put(currentParam, data);
            }
        }
        String[] datatype = params.get("SCANIT_TYPE").trim().split("\s+");
        if (!datatype[0].equals("FLOAT") || !datatype[1].equals("MSBFIRST")) {
            in.close();
            throw new Exception("Unsupported SXM file datatype: " + datatype[0] + " " + datatype[1]);
        }
        List<String> channelNames = params
                .get("DATA_INFO")
                .trim()
                .lines()
                .skip(1)
                .map(s -> s.trim().split("\t+")[1])
                .collect(Collectors.toList());
        String[] pixelsString = params.get("SCAN_PIXELS").trim().split("\s+");
        xPixels = Integer.parseInt(pixelsString[0]);
        yPixels = Integer.parseInt(pixelsString[1]);
        System.out.println(xPixels);
        System.out.println(yPixels);
        // get the last couple \n's
        in.read();
        in.read();
        in.read();
        in.read();
        for (String channelName : channelNames) {
            float[][][] data = new float[2][yPixels][xPixels];
            for (int row = 0; row < yPixels; row++) {
                for (int col = 0; col < xPixels; col++) {
                    data[0][row][col] = in.readFloat();
                }
            }
            for (int row = 0; row < yPixels; row++) {
                for (int col = 0; col < xPixels; col++) {
                    data[1][row][col] = in.readFloat();
                }
            }
            images.put(channelName, data);
        }
        if (in.read() != -1) {
            in.close();
            throw new Exception("SXM Read Error: Not all bytes were read");
        }
        in.close();
    }
}

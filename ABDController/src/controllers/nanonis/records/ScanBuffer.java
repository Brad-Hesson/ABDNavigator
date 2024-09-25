package controllers.nanonis.records;

import java.util.Arrays;

public record ScanBuffer(int[] channelIndexes, int pixelsPerLine, int linesPerScan) {
    public String toString() {
        String ret = "";
        ret += "ScanBuffer[channelIndexes=";
        ret += Arrays.toString(channelIndexes);
        ret += ", pixelsPerLine=";
        ret += pixelsPerLine;
        ret += ", linesPerScan=";
        ret += linesPerScan;
        ret += "]";
        return ret;
    }
}

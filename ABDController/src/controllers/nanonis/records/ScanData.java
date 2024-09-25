package controllers.nanonis.records;

import java.util.Arrays;

public record ScanData(
        String channelName,
        float[][] data,
        boolean scanDirUp) {
    public String toString() {
        String ret = "";
        ret += "ScanFrameData[channelName=";
        ret += channelName;
        ret += ", scanData=";
        if (data.length * data[0].length > 16) {
            ret += "[rows=" + data.length + ", cols=" + data[0].length + "]";
        } else {
            ret += Arrays.deepToString(data);
        }
        ret += ", scanDirUp=";
        ret += scanDirUp;
        ret += "]";
        return ret;
    }
}

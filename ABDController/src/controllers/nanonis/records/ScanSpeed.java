package controllers.nanonis.records;

public record ScanSpeed(float fSpeedMps,
        float bSpeedMps,
        float fLineTimeS,
        float bLineTimeS,
        int whichParamConst,
        float fbRatio) {

}

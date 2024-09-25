package controllers.nanonis.records;

public record ScanProps(boolean continuousScan,
        boolean bouncyScan,
        boolean autoSave,
        String seriesName,
        String comment) {

}

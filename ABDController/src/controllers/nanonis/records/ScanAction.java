package controllers.nanonis.records;

public enum ScanAction {
    START,
    STOP,
    PAUSE,
    RESUME,
    FREEZE,
    UNFREEZE,
    GOTO_CENTER;

    public int toInt() {
        switch (this) {
            case ScanAction.START:
                return 0;
            case ScanAction.STOP:
                return 1;
            case ScanAction.PAUSE:
                return 2;
            case ScanAction.RESUME:
                return 3;
            case ScanAction.FREEZE:
                return 4;
            case ScanAction.UNFREEZE:
                return 5;
            case ScanAction.GOTO_CENTER:
                return 6;
            default:
                return -1;
        }
    }
}

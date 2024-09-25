package controllers.nanonis.records;

public enum Direction {
    X_POS,
    X_NEG,
    Y_POS,
    Y_NEG,
    Z_POS,
    Z_NEG;

    public int toInt(){
        switch (this) {
            case Direction.X_POS:
                return 0;
            case Direction.X_NEG:
                return 1;
            case Direction.Y_POS:
                return 2;
            case Direction.Y_NEG:
                return 3;
            case Direction.Z_POS:
                return 4;
            case Direction.Z_NEG:
                return 5;
            default:
                return -1;
        }
    }
}

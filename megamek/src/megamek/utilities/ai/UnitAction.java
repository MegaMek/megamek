package megamek.utilities.ai;

import megamek.common.Coords;

public record UnitAction(int id, int facing, int fromX, int fromY, int toX, int toY, int hexesMoved, int distance, int mpUsed,
                         int maxMp, double mpP, double heatP, double armorP, double internalP, boolean jumping, boolean prone,
                         boolean legal) {
    public double chanceOfFailure() {
        return 0.0;
    }

    public Coords currentPosition() {
        return new Coords(fromX, fromY);
    }

    public Coords finalPosition() {
        return new Coords(toX, toY);
    }
}

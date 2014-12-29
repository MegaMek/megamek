package megamek.client.ui.swing.boardview;

import megamek.common.Coords;

class EcmBubble extends Coords {
    /**
     *
     */
    private static final long serialVersionUID = 3304636458460529324L;

    int range;
    int tint;
    int direction;

    public EcmBubble(Coords c, int range, int tint) {
        super(c);
        this.range = range;
        this.tint = tint;
        direction = -1;
    }

    public EcmBubble(Coords c, int range, int tint, int direction) {
        super(c);
        this.range = range;
        this.tint = tint;
        this.direction = direction;
    }

}
package megamek.client.ui.swing.boardview;

import megamek.common.Coords;

public class ScorchedGroundSprite extends BaseScorchedGroundSprite {
    public ScorchedGroundSprite(BoardView boardView, Coords coords, Coords offset, int radius) {
        super(boardView, coords, offset, radius);
    }

    public ScorchedGroundSprite(BoardView boardView, MultiHexScorchDecalPosition decal) {
        this(boardView, decal.position(), decal.offset(), decal.radius());
    }
}

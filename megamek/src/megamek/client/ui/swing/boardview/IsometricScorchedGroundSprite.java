package megamek.client.ui.swing.boardview;

import megamek.common.Coords;

import java.awt.*;
import java.awt.image.ImageObserver;

public class IsometricScorchedGroundSprite extends BaseScorchedGroundSprite {
    public IsometricScorchedGroundSprite(BoardView boardView, Coords coords, Coords offset, int radius) {
        super(boardView, coords, offset, radius);
    }

    public IsometricScorchedGroundSprite(BoardView boardView, MultiHexScorchDecalPosition decal) {
        this(boardView, decal.position(), decal.offset(), decal.radius());
    }

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer, boolean makeTranslucent) {
        if (isReady()) {
            Graphics2D g2 = (Graphics2D) g;
            if (makeTranslucent) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                g2.drawImage(image, x, y, observer);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            } else {
                g.drawImage(image, x, y, observer);
            }
        } else {
            prepare();
        }
    }
}

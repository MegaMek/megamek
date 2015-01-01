package megamek.client.ui.swing.boardview;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Stroke;
import java.awt.image.FilteredImageSource;

import megamek.client.ui.swing.util.KeyAlphaFilter;
import megamek.common.Coords;

/**
 * Sprite for displaying information about where a unit can move to.
 */
class MovementEnvelopeSprite extends HexSprite {

    Color drawColor;
    public MovementEnvelopeSprite(BoardView1 boardView1, Color c, Coords l) {
        super(boardView1, l);
        drawColor = c;
    }

    @Override
    public void prepare() {
        updateBounds();
        // create image for buffer
        Image tempImage = bv.createImage(bounds.width, bounds.height);
        Graphics graph = tempImage.getGraphics();

        // fill with key color
        graph.setColor(new Color(BoardView1.TRANSPARENT));
        graph.fillRect(0, 0, bounds.width, bounds.height);
        // draw attack poly
        graph.setColor(drawColor);
        Stroke st = ((Graphics2D) graph).getStroke();
        ((Graphics2D) graph).setStroke(new BasicStroke(2));
        graph.drawPolygon(bv.hexPoly);
        ((Graphics2D) graph).setStroke(st);

        // create final image
        image = bv.getScaledImage(bv.createImage(new FilteredImageSource(
                tempImage.getSource(), new KeyAlphaFilter(
                        BoardView1.TRANSPARENT))), false);
        graph.dispose();
        tempImage.flush();
    }
}
package megamek.client.ui.swing.boardview;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Coords;

/**
 * Sprite for displaying information about where a unit can move to.
 */
class MovementEnvelopeSprite extends HexSprite {

    private final Color drawColor;
    private static Shape drawShape; 
    
    static {
        // creates the Shape that is drawn in each hex of the movement envelope
        AffineTransform scaleCenter = new AffineTransform();
        scaleCenter.translate(BoardView1.HEX_W/2, BoardView1.HEX_H/2);
        scaleCenter.scale(0.9, 0.9);
        scaleCenter.translate(-BoardView1.HEX_W/2, -BoardView1.HEX_H/2);
        drawShape = scaleCenter.createTransformedShape(BoardView1.hexPoly);
    }

    public MovementEnvelopeSprite(BoardView1 boardView1, Color c, Coords l) {
        super(boardView1, l);
        drawColor = c;
    }

    @Override
    public void prepare() {
        // adjust bounds (image size) to board zoom
        updateBounds();
        
        // create image for buffer
        createNewImage();
        Graphics2D graph = (Graphics2D)image.getGraphics();
        GUIPreferences.AntiAliasifSet(graph);

        // scale the following draws according to board zoom
        graph.scale(bv.scale, bv.scale);

        // colored hex border
        graph.setColor(drawColor);
        graph.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10.0f, new float[] { 5.0f } , 0.0f));
        graph.draw(drawShape);

        graph.dispose();
    }
}
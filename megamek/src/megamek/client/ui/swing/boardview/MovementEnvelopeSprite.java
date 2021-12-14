package megamek.client.ui.swing.boardview;

import static megamek.client.ui.swing.boardview.HexDrawUtilities.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Coords;

/**
 * Sprite for displaying information about where a unit can move to.
 */
class MovementEnvelopeSprite extends HexSprite {

    // control values
    private static final int borderThickness = 10;
    private static final int borderOpacity = 60;
    private static final float lineThickness = 2;
    
    // sprite settings
    protected final Color drawColor;
    protected final int borders;

    public MovementEnvelopeSprite(BoardView boardView1, Color c, Coords l, int borders) {
        super(boardView1, l);
        drawColor = c;
        this.borders = borders;
    }
    
    @Override
    public void prepare() {
        // adjust bounds (image size) to board zoom
        updateBounds();
        
        // create image for buffer
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        GUIPreferences.AntiAliasifSet(graph);

        // scale the following draws according to board zoom
        graph.scale(bv.scale, bv.scale);

        graph.setStroke(new BasicStroke(lineThickness, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10f, new float[] { 5f, 3f } , 0f));

        // cycle through directions
        for (int i=0;i<6;i++) {
            if ((borders & (1<<i))>0) {
                // 1) thick transparent border
                int cut = ((borders & (1 << ((i + 1) % 6))) == 0) ? CUT_RBORDER : CUT_RINSIDE;
                cut |= ((borders & (1 << ((i + 5) % 6))) == 0) ? CUT_LBORDER : CUT_LINSIDE;

                graph.setColor(new Color(drawColor.getRed(), drawColor.getGreen(),
                        drawColor.getBlue(), borderOpacity));
                graph.fill(getHexBorderArea(i, cut, borderThickness));
                
                // 2) thin dashed line border
                graph.setColor(drawColor);
                graph.draw(getHexBorderLine(i, cut, lineThickness / 2));
            }
        }
        graph.dispose();
    }
    
}
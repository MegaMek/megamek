package megamek.client.ui.swing.boardview;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Coords;

/**
 * Sprite for displaying information about where a unit can move to.
 */
class MovementEnvelopeSprite extends HexSprite {

    private final Color drawColor;
    private final int borders;

    // Control settings
    private final static int inset = 0;
    private final static int transparentLineThickness = 10;
    private final static int transparentLineOpacity = 60;
    private final static float dashedLineThickness = 2;
    
    private static final float hexFormDelta = 42.0f/72.746f;
    private static Polygon borderShape;
    private static Polygon cornerShapeL;
    private static Polygon cornerShapeR;
    
    static {
        // creates the Shape that is drawn in each hex of the movement envelope
        
        int xd1 = (int)(hexFormDelta*inset);
        int xd2 = (int)(hexFormDelta*(inset+transparentLineThickness));
        
        borderShape = new Polygon();
        borderShape.addPoint(21+xd1, inset);
        borderShape.addPoint(63-xd1, inset);
        borderShape.addPoint(63-xd2, inset+transparentLineThickness);
        borderShape.addPoint(21+xd2, inset+transparentLineThickness);
        
        cornerShapeL = new Polygon();
        cornerShapeL.addPoint(21-xd1, inset);
        cornerShapeL.addPoint(21+xd1, inset);
        cornerShapeL.addPoint(21+xd2, inset+transparentLineThickness);
        cornerShapeL.addPoint(21-xd2, inset+transparentLineThickness);
        
        cornerShapeR = new Polygon();
        cornerShapeR.addPoint(63-xd1, inset);
        cornerShapeR.addPoint(63+xd1, inset);
        cornerShapeR.addPoint(63+xd2, inset+transparentLineThickness);
        cornerShapeR.addPoint(63-xd2, inset+transparentLineThickness);
    }

    public MovementEnvelopeSprite(BoardView1 boardView1, Color c, Coords l, int borders) {
        super(boardView1, l);
        drawColor = c;
        this.borders = borders;
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
        graph.scale(1, 72/72.746); // adjust to non-perfect hex
        
        // 1) thick transparent border
        graph.setColor(new Color(drawColor.getRed(), drawColor.getGreen(),
                drawColor.getBlue(), transparentLineOpacity));
        
        // cycle through directions
        for (int i=0;i<6;i++) {
            if ((borders & (1<<i))>0) {
                graph.fillPolygon(borderShape);
                
                // add corner shapes to connect across hexes
                if ((borders & (1 << ((i + 1) % 6))) == 0) 
                    graph.fillPolygon(cornerShapeR);    
                if ((borders & (1 << ((i + 5) % 6))) == 0) 
                    graph.fillPolygon(cornerShapeL);    
            }
            graph.rotate(Math.toRadians(60), 42, 72.746/2);
        }

        // 2) thin dashed line border
        graph.setColor(drawColor);
        graph.setStroke(new BasicStroke(dashedLineThickness, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10f, new float[] { 5f, 3f } , 0f));

        // cycle through directions
        for (int i=0;i<6;i++) {
            if ((borders & (1<<i))>0) {
                int EndX_L = 21+(int)(hexFormDelta*inset);
                int EndX_R = 63-(int)(hexFormDelta*inset);                
                
                // elongate line to connect across hexes
                if ((borders & (1 << ((i + 1) % 6))) == 0) 
                    EndX_R += (int)(hexFormDelta*inset)*2;
                if ((borders & (1 << ((i + 5) % 6))) == 0) 
                    EndX_L -= (int)(hexFormDelta*inset)*2;   
                
                graph.drawLine(EndX_L, (int) (inset + dashedLineThickness / 2), 
                        EndX_R, (int) (inset + dashedLineThickness / 2));
                
            }
            graph.rotate(Math.toRadians(60), 42, 72.746/2);
        }

        graph.dispose();
    }
}
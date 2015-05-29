package megamek.client.ui.swing.boardview;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.util.EnumMap;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Compute;
import megamek.common.Facing;
import megamek.common.MovePath;
import megamek.common.VTOL;

/**
 * Sprite for displaying information about movement modifier that can be
 * achieved by provided MovePath. Multiple MovementModifierEnvelopeSprite can be
 * drawn on a single hex, one for each final facing.
 * 
 * @author Saginatio
 * 
 */
public class MovementModifierEnvelopeSprite extends HexSprite {
    
    private final static Color fontColor = Color.BLACK;
    private final static float fontSize = 9;

    private final Color color;
    private final Point.Float textPos;
    private final Facing facing;
    private final String modifier;

    static EnumMap<Facing, Polygon> borders = new EnumMap<>(Facing.class);
    static EnumMap<Facing, Point> borderMidPoints = new EnumMap<>(Facing.class);
    
    static {
        initBorderPolygons();
    }

    private static void initBorderPolygons() {
        Polygon hexPoly = new Polygon();
        hexPoly.addPoint(21, 0);
        hexPoly.addPoint(62, 0);
        hexPoly.addPoint(83, 35);
        hexPoly.addPoint(62, 71);
        hexPoly.addPoint(21, 71);
        hexPoly.addPoint(0, 35);

        int y[] = hexPoly.ypoints;
        int x[] = hexPoly.xpoints;
        for (Facing f : Facing.values()) {
            Polygon poly = new Polygon();
            Point p1 = new Point(x[f.getIntValue()], y[f.getIntValue()]);
            Facing f2 = f.getNextClockwise();
            Point p2 = new Point(x[f2.getIntValue()], y[f2.getIntValue()]);
            poly.addPoint(p1.x, p1.y);
            poly.addPoint(p2.x, p2.y);

            Point hmp = new Point(41, 35);
            Point p3 = new Point((p2.x * 6 + hmp.x * 4) / 10, (p2.y * 6 + hmp.y * 4) / 10);
            Point p4 = new Point((p1.x * 6 + hmp.x * 4) / 10, (p1.y * 6 + hmp.y * 4) / 10);
            poly.addPoint(p3.x, p3.y);
            poly.addPoint(p4.x, p4.y);
            borders.put(f, poly);

            //calculation of middle point of the polygon
            int pmpX = 0;
            for (int i = 0; i < poly.npoints; i++)
                pmpX += poly.xpoints[i];
            pmpX /= poly.npoints;
            int pmpY = 0;
            for (int i = 0; i < poly.npoints; i++)
                pmpY += poly.ypoints[i];
            pmpY /= poly.npoints;
            borderMidPoints.put(f, new Point(pmpX, pmpY));
        }
    }

    /**
     * @param boardView1
     * @param mp
     */
    public MovementModifierEnvelopeSprite(BoardView1 boardView1, MovePath mp) {
        super(boardView1, mp.getFinalCoords());

        facing = Facing.valueOfInt(mp.getFinalFacing());
        
        int modi = Compute.getTargetMovementModifier(mp.getHexesMoved(),
                mp.isJumping(),
                mp.getEntity() instanceof VTOL,
                boardView1.game).getValue();
        float hue = 0.7f - 0.15f * modi;
        color = new Color(Color.HSBtoRGB(hue, 1, 1));
        modifier = String.format("%+d", modi);

        Point sp = borderMidPoints.get(facing);
        textPos = new Point.Float(sp.x-fontSize/2, sp.y+fontSize/2-1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.client.ui.swing.boardview.Sprite#prepare()
     */
    @Override
    public void prepare() {
        // adjust bounds (image size) to board zoom
        updateBounds();
        
        // create image for buffer
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D)image.getGraphics();
        GUIPreferences.AntiAliasifSet(graph);

        // scale the following draws according to board zoom
        graph.scale(bv.scale, bv.scale);

        // colored polygon at the hex border
        graph.setColor(color);
        graph.fillPolygon(borders.get(facing));

        // draw the movement modifier if it's readable
        if (fontSize * bv.scale > 4) {
            graph.setColor(fontColor);
            graph.setFont(graph.getFont().deriveFont(fontSize));
            graph.drawString(modifier, textPos.x, textPos.y);
        }

        graph.dispose();
    }
}

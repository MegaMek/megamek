/**
 * 
 */
package megamek.client.ui.swing.boardview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.FilteredImageSource;
import java.util.EnumMap;

import megamek.client.ui.swing.util.KeyAlphaFilter;
import megamek.common.Compute;
import megamek.common.Facing;
import megamek.common.MovePath;
import megamek.common.VTOL;

/**
 * @author Saginatio
 * 
 */
public class MovementModifierEnvelopeSprite extends HexSprite {

    private Color color;
    private Color fillColor;
    private Color fontColor;
    private Facing facing;
    static EnumMap<Facing, Polygon> borders = new EnumMap<>(Facing.class);
    static EnumMap<Facing, Point> borderMidPoints = new EnumMap<>(Facing.class);
    private int modifier;
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

            //calculation of middle of the polygon
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
        bounds = new Rectangle(this.boardView1.getHexLocation(loc), this.boardView1.hex_size);
        modifier = Compute.getTargetMovementModifier(mp.getHexesMoved(),
                mp.isJumping(),
                mp.getEntity() instanceof VTOL,
                boardView1.game).getValue();
        float hue = 0.7f - 0.15f * modifier;
        color = new Color(Color.HSBtoRGB(hue, 1, 1));
        fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
        fontColor = Color.BLACK;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.client.ui.swing.boardview.Sprite#prepare()
     */
    @Override
    public void prepare() {
        // create image for buffer
        Image tempImage = this.boardView1.createImage(bounds.width, bounds.height);
        Graphics graph = tempImage.getGraphics();

        // fill with key color
        graph.setPaintMode();
        graph.setColor(new Color(BoardView1.TRANSPARENT));
        graph.fillRect(0, 0, bounds.width, bounds.height);

        // draw polygon at the border
        graph.setColor(fillColor);
        graph.fillPolygon(borders.get(facing));

        // draw a movement modifier
        Point sp = borderMidPoints.get(facing);
        int fontSize = 8;
        sp = (Point) (sp.clone());
        sp.translate(-fontSize / 2, fontSize / 2);
        String s = String.format("%+d", modifier);
        graph.setColor(fontColor);
        graph.setFont(graph.getFont().deriveFont(((Integer) fontSize).floatValue()));
        graph.drawString(s, sp.x, sp.y);

        // create final image
        if (this.boardView1.zoomIndex == BoardView1.BASE_ZOOM_INDEX) {
            image = this.boardView1.createImage(new FilteredImageSource(
                    tempImage.getSource(), new KeyAlphaFilter(BoardView1.TRANSPARENT)));
        } else {
            image = this.boardView1.getScaledImage(this.boardView1.createImage(new FilteredImageSource(
                    tempImage.getSource(), new KeyAlphaFilter(BoardView1.TRANSPARENT))), false);
        }
        graph.dispose();
        tempImage.flush();

    }

}

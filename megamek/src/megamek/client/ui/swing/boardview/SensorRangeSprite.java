package megamek.client.ui.swing.boardview;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.image.ImageObserver;
import static megamek.client.ui.swing.boardview.HexDrawUtilities.*;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Coords;
import megamek.common.RangeType;

/**
 * This sprite is used to paint the field of fire 
 * for weapons. 
 * 
 * <BR><BR>Extends {@link MovementEnvelopeSprite}
 * 
 * @author Simon
 */
public class SensorRangeSprite extends MovementEnvelopeSprite {
    // ### Control values
    
    // thick border
    private static final int borderW = 10;
    private static final int borderOpac = 120;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    // thin line
    private static final float lineThickness = 1.4f;
    private static final Color lineColor = Color.WHITE;
    private static final Stroke lineStroke = new BasicStroke(lineThickness, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10f, new float[] { 2f, 2f }, 0f);
    // ### -------------
    
    // the fields control when and how borders are drawn 
    // across a hex instead of along its borders
    private static final int[] bDir = {
        0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 2, 2, 1, 0, 0, 0, 0, 0,
        0, 0, 1, 0, 3, 3, 3, 0, 2, 2, 1, 0, 2, 5, 4, 5, 6, 5, 1, 5,
        0, 5, 2, 5, 2, 2, 1, 5, 4, 4, 4, 4, 4, 4, 1, 4, 3, 3, 3, 3, 2, 2, 1, 0
    };
    private static final int[] bTypes = {
        0, 0, 0, 1, 0, 0, 1, 2, 0, 0, 0, 6, 1, 7, 2, 3, 0, 0, 0, 7,
        0, 1, 6, 5, 1, 6, 7, 4, 2, 5, 3, 8, 0, 1, 4, 2, 6, 6, 7, 3,
        0, 7, 2, 5, 6, 4, 5, 8, 1, 2, 6, 3, 7, 5, 4, 8, 2, 3, 5, 8, 3, 8, 8, 0
    };
    
    // in this sprite type, the images are very repetitive
    // therefore they get saved in a static array
    // they will be painted only once for each border
    // arrangement and color and repainted only when
    // the board is zoomed
    private static Image[][] images = new Image[64][5];
    private static float oldZoom;
    
    // individual sprite values
    private final Color fillColor;
    private final int sensorType;
    public final static int SENSORS = 0;
    public final static int VISUAL = 1;
    
    public SensorRangeSprite(BoardView boardView1, int sensorType, Coords l,
                             int borders) {
        // the color of the super doesn't matter
        super(boardView1, Color.BLACK, l, borders);
        Color c = getColor(sensorType);
        fillColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), borderOpac);
        this.sensorType = sensorType;
    }

    public static Color getColor(int sensorType) {
        switch (sensorType) {
            case SENSORS:
                return UIUtil.uiLightBlue();
            case VISUAL:
                return UIUtil.uiDarkBlue();
            default:
                return new Color(0,0,0);
        }
    }

    @Override
    public void prepare() {
        // adjust bounds (image size) to board zoom
        updateBounds();
        
        // when the zoom hasn't changed and there is already
        // a prepared image for these borders, then do nothing more
        if ((bv.scale == oldZoom) && isReady()) {
            return;
        }
        
        // when the board is rezoomed, ditch all images
        if (bv.scale != oldZoom) {
            oldZoom = bv.scale;
            images = new Image[64][5];
        }

        // create image for buffer
        images[borders][sensorType] = createNewHexImage();
        Graphics2D graph = (Graphics2D) images[borders][sensorType].getGraphics();
        UIUtil.setHighQualityRendering(graph);

        // scale the following draws according to board zoom
        graph.scale(bv.scale, bv.scale);
        
        graph.setStroke(lineStroke);

        // this will take the right way to paint the borders
        // from the static arrays; depends on the exact
        // borders that are present
        switch (bTypes[borders]) {
            case 1: // 2 adjacent borders
                drawBorderXC(graph, getHexCrossArea01(bDir[borders], borderW),
                        getHexCrossLine01(bDir[borders], borderW));
                break;
            case 2: // 3 adjacent borders
                drawBorderXC(graph, getHexCrossArea012(bDir[borders], borderW),
                        getHexCrossLine012(bDir[borders], borderW));
                break;
            case 3: // 4 adjacent borders
                drawBorderXC(graph, getHexCrossArea0123(bDir[borders], borderW),
                        getHexCrossLine0123(bDir[borders], borderW));
                break;
            case 4: // twice two adjacent borders
                drawBorderXC(graph, getHexCrossArea01(bDir[borders], borderW),
                        getHexCrossLine01(bDir[borders], borderW));
                drawBorderXC(graph, getHexCrossArea01(bDir[borders]+3, borderW),
                        getHexCrossLine01(bDir[borders]+3, borderW));
                break;
            case 5: // three adjacent borders and one lone
                drawBorderXC(graph, getHexCrossArea012(bDir[borders], borderW),
                        getHexCrossLine012(bDir[borders], borderW));
                drawLoneBorder(graph, bDir[borders] + 4);
                break;
            case 6: // two adjacent borders and one lone
                drawBorderXC(graph, getHexCrossArea01(bDir[borders], borderW),
                        getHexCrossLine01(bDir[borders], borderW));
                drawLoneBorder(graph, bDir[borders] + 3);
                break;
            case 7: // two adjacent borders and one lone (other hexface)
                drawBorderXC(graph, getHexCrossArea01(bDir[borders], borderW),
                        getHexCrossLine01(bDir[borders], borderW));
                drawLoneBorder(graph, bDir[borders] + 4);
                break;
            case 8:
                drawBorderXC(graph, getHexCrossArea01234(bDir[borders], borderW),
                        getHexCrossLine01234(bDir[borders], borderW));
                break;
            default:
                drawNormalBorders(graph);
        }

        graph.dispose();
    }
    
    private void drawBorderXC(Graphics2D graph, Shape fillShape, Shape lineShape) {
        // 1) thick transparent border
        graph.setColor(fillColor);
        graph.fill(fillShape);

        // 2) thin dashed line border
        graph.setColor(lineColor);
        graph.draw(lineShape);
    }
    
    private void drawLoneBorder(Graphics2D graph, int dir) {
        // 1) thick transparent border
        graph.setColor(fillColor);
        graph.fill(getHexBorderArea(dir, CUT_BORDER, borderW));

        // 2) thin dashed line border
        graph.setColor(lineColor);
        graph.draw(getHexBorderLine(dir));
    }
    
    private void drawNormalBorders(Graphics2D graph) {
        // cycle through directions
        for (int i = 0; i < 6; i++) {
            if ((borders & (1 << i)) != 0) {
                // 1) thick transparent border
                int cut = ((borders & (1 << ((i + 1) % 6))) == 0) ? CUT_RBORDER : CUT_RINSIDE;
                cut |= ((borders & (1 << ((i + 5) % 6))) == 0) ? CUT_LBORDER : CUT_LINSIDE;

                graph.setColor(fillColor);
                graph.fill(getHexBorderArea(i, cut, borderW));
                
                // 2) thin dashed line border
                graph.setColor(lineColor);
                graph.draw(getHexBorderLine(i, cut, lineThickness/2));
            }
        } 
    }
    
    @Override
    public boolean isReady() {
        return (bv.scale == oldZoom) && (images[borders][sensorType] != null);
    }

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer, boolean makeTranslucent) {
        if (isReady()) {
            if (makeTranslucent) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                g2.drawImage(images[borders][sensorType], x, y, observer);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            } else {
                g.drawImage(images[borders][sensorType], x, y, observer);
            }
        } else {
            prepare();
        }
    }
}

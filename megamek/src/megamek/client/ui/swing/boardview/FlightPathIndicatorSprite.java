/**
 *
 */
package megamek.client.ui.swing.boardview;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.StringDrawer;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Coords;
import megamek.common.MoveStep;

/**
 * The Flight Path Indicator Sprite represents the status of a hex in the trajectory of an
 * aerospace unit going in a straight line until it expends its velocity.
 *
 * A green solid circle represents required trajectory.
 * A green non-filled circle represents a point on the path where a turn is allowed.
 */
public class FlightPathIndicatorSprite extends HexSprite {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final int TEXT_SIZE = 30;
    //private static final Color TEXT_COLOR = new Color(255, 255, 40, 128);
    private static final Color TEXT_COLOR = Color.GREEN;
    private static final Color OUTLINE_COLOR = new Color(40, 40,40,200);

    private static final int HEX_CENTER_X = BoardView.HEX_W / 2;
    private static final int HEX_CENTER_Y = BoardView.HEX_H / 2;

    private MoveStep step = null;
    private boolean isLast = false;

    //U+26AA  MEDIUM WHITE CIRCLE
    //U+26AB  MEDIUM BLACK CIRCLE

    //Draw a special character 'circle'.
    private final StringDrawer xWriter = new StringDrawer("\u26AB")
            .at(HEX_CENTER_X, HEX_CENTER_Y)
            .color(TEXT_COLOR)
            .fontSize(TEXT_SIZE)
            .center().outline(OUTLINE_COLOR, 1.5f);

    /**
     * @param boardView - BoardView associated with the sprite.
     * @param loc - hex coordinate to place the sprite.
     * @param step - the MoveStep object that backs the flight indicator state at that hex.
     * @param last - true if the sprite represents the last indicator on the board.
     */
    public FlightPathIndicatorSprite(BoardView boardView, Coords loc, final MoveStep step, boolean last) {
        super(boardView, loc);
        this.step = step;
        this.isLast = last;
    }

    @Override
    public void prepare() {
        Graphics2D graph = spriteSetup();
        xWriter.draw(graph);
        graph.dispose();
    }

    /*
     * Standard Hex Sprite 2D Graphics setup.  Creates the context, base hex image
     * settings, scale, and fonts.
     */
    private Graphics2D spriteSetup() {
        updateBounds();
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);
        graph.scale(bv.scale, bv.scale);

        fontSetup(graph);

        return graph;
    }

    /*
     * Sets the font name, style, and size from configured default parameters.
     */
    private void fontSetup(Graphics2D graph) {
        String fontName = GUIP.getMoveFontType();
        int fontStyle = GUIP.getMoveFontStyle();
        graph.setFont(new Font(fontName, fontStyle, TEXT_SIZE));
    }

    /*
     * Return true if this sprite/step is the last of the flight path indicators in the
     * flight path.
     */
    private boolean isLastIndicator() {
        return isLast;
    }
}
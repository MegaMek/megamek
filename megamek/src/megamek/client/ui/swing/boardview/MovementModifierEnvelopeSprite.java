package megamek.client.ui.swing.boardview;

import static megamek.client.ui.swing.boardview.HexDrawUtilities.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Compute;
import megamek.common.CrewType;
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
    
    private static final Color fontColor = Color.BLACK;
    private static final float fontSize = 9;
    private static final double borderW = 15;
    private static final double inset = 1;

    private final Color color;
    private final Facing facing;
    private final String modifier;

    /**
     * @param boardView1
     * @param mp
     */
    public MovementModifierEnvelopeSprite(BoardView boardView1, MovePath mp) {
        super(boardView1, mp.getFinalCoords());

        facing = Facing.valueOfInt(mp.getFinalFacing());
        
        int modi = Compute.getTargetMovementModifier(mp.getHexesMoved(),
                mp.isJumping(),
                mp.getEntity() instanceof VTOL,
                boardView1.game).getValue();
        //Add evasion bonus for 'Mech with dual cockpit
        if (mp.getEntity().getCrew().getCrewType().equals(CrewType.DUAL)
                && mp.getEntity().getCrew().hasDedicatedPilot()
                && !mp.isJumping() && mp.getHexesMoved() > 0) {
            modi++;
        }
        float hue = 0.7f - 0.15f * modi;
        color = new Color(Color.HSBtoRGB(hue, 1, 1));
        modifier = String.format("%+d", modi);
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
        Graphics2D graph = (Graphics2D) image.getGraphics();
        GUIPreferences.AntiAliasifSet(graph);

        // scale the following draws according to board zoom
        graph.scale(bv.scale, bv.scale);

        // colored polygon at the hex border
        graph.setColor(color);
        graph.fill(getHexBorderArea(facing.getIntValue(), CUT_INSIDE, borderW, inset));

        // draw the movement modifier if it's readable
        if (fontSize * bv.scale > 4) {
            graph.setFont(graph.getFont().deriveFont(fontSize));
            Point2D.Double pos = getHexBorderAreaMid(facing.getIntValue(), borderW, inset);
            bv.drawCenteredText(graph, modifier, (float) pos.x, (float) pos.y, fontColor, false);
        }

        graph.dispose();
    }
}

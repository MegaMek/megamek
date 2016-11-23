package megamek.client.ui.swing.boardview;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.TargetRoll;
import megamek.common.util.FiringSolution;

/**
 * Sprite for displaying generic firing information. This is used for
 * the firing phase and displays either range and target modifier or
 * a big red X if the target cannot be hit.
 */
class FiringSolutionSprite extends HexSprite {
    
    // control values
    // for modifier and range
    private static final int fontSizeSmall = 25;
    private static final int fontSizeRange = 20;
    private static final Color fontColor = Color.CYAN;
    // for the big X
    private static final int fontSizeLarge = 40;
    private static final Color xColor = Color.RED;

    private static final Color hexIconColor = new Color(80,80,80,140);
    private static final Stroke hexIconStroke = new BasicStroke(1.5f);

    private static final Color indirectDashColor1 = new Color(255,  0, 0, 140);
    private static final Color indirectDashColor2 = new Color(255,255, 0, 140);
    private static final float dashPeriod[] = { 10.0f };
    private static final BasicStroke indirectStroke1 = new BasicStroke(3.0f, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND, 10.0f, dashPeriod, 0.0f);
    private static final BasicStroke indirectStroke2 = new BasicStroke(3.0f, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND, 10.0f, dashPeriod, 10.0f);
    
    // calculated statics
    // text positions
    private static Point centerHex = new Point(BoardView1.HEX_W / 2,
            BoardView1.HEX_H / 2);
    private static Point firstLine = new Point(BoardView1.HEX_W / 2 - 2,
            BoardView1.HEX_H / 4 + 2);
    private static Point secondLine = new Point(BoardView1.HEX_W / 2 + 9,
            BoardView1.HEX_H * 3 / 4 - 2);

    // sprite object data
    private FiringSolution fsoln;
    private String range;
    private String toHitMod;
    private boolean noHitPossible = false;
    private Shape finalHex;

    public FiringSolutionSprite(BoardView1 boardView1, final FiringSolution fsoln) {
        super(boardView1, fsoln.getToHitData().getLocation());
        updateBounds();
        
        this.fsoln = fsoln;
        // modifier
        int thm = fsoln.getToHitData().getValue();
        toHitMod = Integer.toString(thm);
        if (thm >= 0) toHitMod = "+" + toHitMod;
        if ((thm == TargetRoll.IMPOSSIBLE)
                || (thm == TargetRoll.AUTOMATIC_FAIL)) 
            noHitPossible = true;
        
        // range
        int r = fsoln.getToHitData().getRange();
        range = Integer.toString(r);

        // create the small hex shape
        AffineTransform at = AffineTransform.getTranslateInstance((r > 9) ? 25 : 30, secondLine.y + 2);
        at.scale(0.17, 0.17);
        at.translate(-BoardView1.HEX_W/2, -BoardView1.HEX_H/2);
        finalHex = at.createTransformedShape(BoardView1.hexPoly);
    }

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
        
        // get the right font
        String fontName = GUIPreferences.getInstance().getString(
                GUIPreferences.ADVANCED_MOVE_FONT_TYPE);
        int fontStyle = GUIPreferences.getInstance().getInt(
                GUIPreferences.ADVANCED_MOVE_FONT_STYLE);
        
        if (noHitPossible) {  
            // write big red X
            graph.setFont(new Font(fontName, fontStyle, (int)(fontSizeLarge)));
            if (bv.scale > 0.7) {
                // better translucent, the X is so big
                bv.drawOutlineText(graph, "X", centerHex, 
                        fontSizeLarge, xColor, true, Color.BLACK);
            } else {
                // better readable at small scale
                bv.drawCenteredText(graph, "X", centerHex, xColor, false);
            }
        } else {    
            // hittable: write modifier and range
            Font textFont = new Font(fontName, fontStyle, fontSizeSmall);
            Font rangeFont = new Font(fontName, fontStyle, fontSizeRange);
            
            // shadows
            bv.drawTextShadow(graph, toHitMod, firstLine, textFont);
            bv.drawTextShadow(graph, range, secondLine, rangeFont);
            
            // text
            bv.drawCenteredText(graph, toHitMod, firstLine, fontColor, false, textFont);
            bv.drawCenteredText(graph, range, secondLine, fontColor, false, rangeFont);

            // a small hex shape for distance
            // fill blueish
            graph.setColor(hexIconColor);
            graph.fill(finalHex);
            // hex border
            graph.setStroke(hexIconStroke);
            graph.setColor(fontColor);
            graph.draw(finalHex);
        }
        
        if (fsoln.isTargetSpotted()) {
            graph.setColor(indirectDashColor1);
            graph.setStroke(indirectStroke1);
            graph.draw(BoardView1.hexPoly);

            graph.setColor(indirectDashColor2);
            graph.setStroke(indirectStroke2);
            graph.draw(BoardView1.hexPoly);
        }

        graph.dispose();
    }

}
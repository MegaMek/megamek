package megamek.client.ui.swing.boardview;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Coords;
import megamek.common.TargetRoll;

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
    
    // calculated statics
    // text positions
    private static Point centerHex = new Point(BoardView1.HEX_W / 2,
            BoardView1.HEX_H / 2);
    private static Point firstLine = new Point(BoardView1.HEX_W / 2 - 2,
            BoardView1.HEX_H / 4 + 2);
    private static Point secondLine = new Point(BoardView1.HEX_W / 2 + 9,
            BoardView1.HEX_H * 3 / 4 - 2);

    // sprite object data
    private String range;
    private String toHitMod;
    private boolean noHitPossible = false;
    private Shape finalHex;

    public FiringSolutionSprite(BoardView1 boardView1, final int thm,
            final int r, final Coords l) {
        super(boardView1, l);
        updateBounds();
        
        // modifier
        toHitMod = Integer.toString(thm);
        if (thm >= 0) toHitMod = "+" + toHitMod;
        if ((thm == TargetRoll.IMPOSSIBLE)
                || (thm == TargetRoll.AUTOMATIC_FAIL)) 
            noHitPossible = true;
        
        // range
        range = Integer.toString(r);

        // create the small hex shape
        AffineTransform at = AffineTransform.getTranslateInstance((r > 9) ? 25
                : 30, secondLine.y + 2);
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
            bv.drawCenteredText(graph, toHitMod, firstLine, fontColor, false,
                    textFont);
            bv.drawCenteredText(graph, range, secondLine, fontColor, false,
                    rangeFont);

            // a small hex shape for distance
            // fill blueish
            graph.setColor(new Color(80,80,80,140));
            graph.fill(finalHex);
            // hex border
            graph.setStroke(new BasicStroke(1.5f));
            graph.setColor(fontColor);
            graph.draw(finalHex);
        }
        
        graph.dispose();
    }

}
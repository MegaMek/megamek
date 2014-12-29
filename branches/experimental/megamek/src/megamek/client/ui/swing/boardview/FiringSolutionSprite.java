package megamek.client.ui.swing.boardview;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.image.FilteredImageSource;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.KeyAlphaFilter;
import megamek.common.Coords;
import megamek.common.TargetRoll;

/**
 * Sprite for displaying generic firing information. This is used for
 */
class FiringSolutionSprite extends Sprite {

    private int toHitMod, range;
    private Coords loc;
    private Image baseScaleImage;

    public FiringSolutionSprite(BoardView1 boardView1, final int thm, final int r, final Coords l) {
        super(boardView1);
        toHitMod = thm;
        loc = l;
        range = r;
        bounds = new Rectangle(this.boardView1.getHexLocation(loc), this.boardView1.hex_size);
        image = null;
        baseScaleImage = null;
    }

    /**
     * Refreshes this StepSprite's image to handle changes in the zoom
     * level.
     */
    public void refreshZoomLevel() {

        if (baseScaleImage == null) {
            return;
        }

        if (this.boardView1.zoomIndex == BoardView1.BASE_ZOOM_INDEX) {
            image = this.boardView1.createImage(new FilteredImageSource(
                    baseScaleImage.getSource(), new KeyAlphaFilter(
                            BoardView1.TRANSPARENT)));
        } else {
            image = this.boardView1.getScaledImage(this.boardView1.createImage(new FilteredImageSource(
                    baseScaleImage.getSource(), new KeyAlphaFilter(
                            BoardView1.TRANSPARENT))),false);
        }
    }

    @Override
    public void prepare() {
        // create image for buffer
        Image tempImage = this.boardView1.createImage(bounds.width, bounds.height);
        Graphics graph = tempImage.getGraphics();

        // fill with key color
        graph.setColor(new Color(BoardView1.TRANSPARENT));
        graph.fillRect(0, 0, bounds.width, bounds.height);

        // Draw firing information
        Point p = this.boardView1.getHexLocation(loc);
        p.translate(-bounds.x, -bounds.y);
        graph.setFont(getFiringFont());

        if ((toHitMod != TargetRoll.IMPOSSIBLE)
                && (toHitMod != TargetRoll.AUTOMATIC_FAIL)) {
            int xOffset = 25;
            int yOffset = 30;
            FontMetrics metrics = graph.getFontMetrics();
            // Draw to-hit modifier

            String modifier;
            if (toHitMod >= 0) {
                modifier = "+" + toHitMod;
            } else {
                modifier = "" + toHitMod;
            }
            Graphics2D g2 = (Graphics2D) graph;
            GlyphVector gv = getFiringFont().createGlyphVector(
                    g2.getFontRenderContext(), modifier);
            g2.translate(xOffset, yOffset);
            for (int i = 0; i < modifier.length(); i++){
                Shape gs = gv.getGlyphOutline(i);
                g2.setPaint(GUIPreferences.getInstance().getColor(
                        GUIPreferences.ADVANCED_FIRE_SOLN_CANSEE_COLOR));
                g2.fill(gs); // Fill the shape
                g2.setPaint(Color.black); // Switch to solid black
                g2.draw(gs); // And draw the outline
            }
            g2.translate(-13, metrics.getHeight());
            yOffset += metrics.getHeight();
            modifier = "rng: " + range;
            gv = getFiringFont().createGlyphVector(
                    g2.getFontRenderContext(), modifier);
            for (int i = 0; i < modifier.length(); i++){
                Shape gs = gv.getGlyphOutline(i);
                g2.setPaint(GUIPreferences.getInstance().getColor(
                        GUIPreferences.ADVANCED_FIRE_SOLN_CANSEE_COLOR));
                g2.fill(gs); // Fill the shape
                g2.setPaint(Color.black); // Switch to solid black
                g2.draw(gs); // And draw the outline
            }
        } else {
            String modifier = "X";
            Graphics2D g2 = (Graphics2D) graph;
            GlyphVector gv = getFiringFont().createGlyphVector(
                    g2.getFontRenderContext(), modifier);
            g2.translate(35, 39);
            for (int i = 0; i < modifier.length(); i++){
                Shape gs = gv.getGlyphOutline(i);
                g2.setPaint(GUIPreferences.getInstance().getColor(
                        GUIPreferences.ADVANCED_FIRE_SOLN_NOSEE_COLOR));
                g2.fill(gs); // Fill the shape
                g2.setPaint(Color.black); // Switch to solid black
                g2.draw(gs); // And draw the outline
            }
        }

        // graph.setColor(drawColor);
        // graph.drawPolygon(hexPoly);

        baseScaleImage = this.boardView1.createImage(new FilteredImageSource(
                tempImage.getSource(), new KeyAlphaFilter(BoardView1.TRANSPARENT)));
        // create final image
        if (this.boardView1.zoomIndex == BoardView1.BASE_ZOOM_INDEX) {
            image = this.boardView1.createImage(new FilteredImageSource(
                    tempImage.getSource(), new KeyAlphaFilter(BoardView1.TRANSPARENT)));
        } else {
            image = this.boardView1.getScaledImage(this.boardView1.createImage(new FilteredImageSource(
                    tempImage.getSource(), new KeyAlphaFilter(BoardView1.TRANSPARENT))),false);
        }
        graph.dispose();
        tempImage.flush();
    }

    @Override
    public Rectangle getBounds() {
        bounds = new Rectangle(this.boardView1.getHexLocation(loc), this.boardView1.hex_size);
        return bounds;
    }

    public Font getFiringFont() {

        String fontName = GUIPreferences.getInstance().getString(
                GUIPreferences.ADVANCED_MOVE_FONT_TYPE);
        int fontStyle = GUIPreferences.getInstance().getInt(
                GUIPreferences.ADVANCED_MOVE_FONT_STYLE);
        int fontSize = 16;

        return new Font(fontName, fontStyle, fontSize);
    }
}
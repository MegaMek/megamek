package megamek.client.ui.swing.boardview;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Coords;

public class TextMarkerSprite extends HexSprite {
    
    private String spriteText;
    private Color spriteColor;

    public TextMarkerSprite(BoardView boardView1, Coords loc, String text, Color color) {
        super(boardView1, loc);
        spriteText = text;
        spriteColor = color;
    }

    @Override
    public void prepare() {
        // adjust bounds (image size) to board zoom
        updateBounds();

        // create image for buffer
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        GUIPreferences.AntiAliasifSet(graph);

        // get a big font and test to see which font size will fit
        // the hex shape
        Font textFont = new Font("SansSerif", Font.PLAIN, 1000);
        graph.setFont(textFont);
        FontMetrics fm = graph.getFontMetrics(graph.getFont());
        Rectangle2D rect = fm.getStringBounds(spriteText, graph);

        float factor = 1;
        if (rect.getHeight() > bounds.getHeight()) {
            factor = (float) bounds.getHeight() / (float) rect.getHeight();
        }

        if ((rect.getWidth() * factor) > bounds.getWidth()) {
            factor = Math.min(factor, ((float) bounds.getWidth() / (float) rect.getWidth()));
        }
        // make smaller to actually fit the hex shape
        factor = factor * 0.7f;
        
        // set the font and draw the text
        Font textFontS = new Font("SansSerif", Font.PLAIN, (int) (factor * 1000));
        graph.setFont(textFontS);
        Point pos = new Point((int) (bounds.getWidth() / 2),(int) (bounds.getHeight() / 2));
        bv.drawTextShadow(graph, spriteText, pos, textFontS);
        BoardView.drawCenteredText(graph, spriteText, pos, spriteColor, false);
    }
}

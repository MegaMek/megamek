package megamek.client.ui.swing.widget;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a label that can stretch across multiple lines
 * @author NickAragua
 *
 */
public class PMMultiLineLabel extends PMSimpleLabel {
    private List<String> labels = new ArrayList<>();
    
    /**
     * Constructs a new multi-line label
     * @param fm Font metrics object
     * @param c Color for the text on this label
     */
    public PMMultiLineLabel(FontMetrics fm, Color c) {
        super("", fm, c);
    }
    
    /**
     * Clear the contents of this multi-line label
     */
    public void clear() {
        labels.clear();
        height = 0;
        width = 0;
    }
    
    /**
     * Add a string to this multi-line label
     * @param s The string to add
     */
    public void addString(String s) {
        labels.add(s);
        
        int newWidth = fm.stringWidth(s);
        if (newWidth > width) {
            width = newWidth;
        }
        
        height += fm.getHeight();
    }
    
    /*
     * Draw the label.
     */
    @Override
    public void drawInto(Graphics g) {
        if (!visible) {
            return;
        }
        Font font = g.getFont();
        Color temp = g.getColor();
        g.setColor(color);
        g.setFont(fm.getFont());
        
        int currentY = y;

        for (String s : labels) {
            g.drawString(s, x, currentY);
            currentY += fm.getHeight();
        }
        
        g.setColor(temp);
        g.setFont(font);
    }
    
}

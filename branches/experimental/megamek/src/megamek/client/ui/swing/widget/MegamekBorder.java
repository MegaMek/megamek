package megamek.client.ui.swing.widget;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.border.EmptyBorder;

import megamek.common.Configuration;

/**
 * A Border that has an image for each corner as well as an image for the line
 * inbetween each corner.  The image for the line between two corners will be 
 * tiled if it is not large enough to fill the whole space.
 * 
 * @author walczak
 *
 */
public class MegamekBorder extends EmptyBorder {

    protected ImageIcon tl_corner, tr_corner, bl_corner, br_corner;
    protected ImageIcon left_line, top_line, right_line, bottom_line;
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public MegamekBorder(Insets i){
        super(i);
        loadIcons(SkinXMLHandler.getSkin(SkinXMLHandler.defaultUIElement));
    }
    
    public MegamekBorder(int top, int left, int bottom, int right){
        super(top,left,bottom,right);
        loadIcons(SkinXMLHandler.getSkin(SkinXMLHandler.defaultUIElement));        
    }
    
    public MegamekBorder(){
    	super(0,0,0,0);
    	loadIcons(SkinXMLHandler.getSkin(SkinXMLHandler.defaultUIElement));
    }
    
    public MegamekBorder(String component){
    	super(0,0,0,0);
    	loadIcons(SkinXMLHandler.getSkin(component));
    }
    
    public void loadIcons(SkinSpecification skin){
        try {
            java.net.URI imgURL = 
                    new File(Configuration.widgetsDir(),
                    		skin.tl_corner).toURI();
            tl_corner = new ImageIcon(imgURL.toURL());
            imgURL = 
                    new File(Configuration.widgetsDir(),
                    		skin.tr_corner).toURI();
            tr_corner = new ImageIcon(imgURL.toURL());
            imgURL = 
                    new File(Configuration.widgetsDir(),
                    		skin.bl_corner).toURI();
            bl_corner = new ImageIcon(imgURL.toURL());
            imgURL = 
                    new File(Configuration.widgetsDir(),
                    		skin.br_corner).toURI();
            br_corner = new ImageIcon(imgURL.toURL());
            
            imgURL = 
                    new File(Configuration.widgetsDir(),
                    		skin.left_line).toURI();
            left_line = new ImageIcon(imgURL.toURL());
            imgURL = 
                    new File(Configuration.widgetsDir(),
                    		skin.right_line).toURI();
            right_line = new ImageIcon(imgURL.toURL()); 
            imgURL = 
                    new File(Configuration.widgetsDir(),
                    		skin.top_line).toURI();
            top_line = new ImageIcon(imgURL.toURL()); 
            imgURL =
                    new File(Configuration.widgetsDir(),
                    		skin.bottom_line).toURI();
            bottom_line = new ImageIcon(imgURL.toURL()); 
        } catch (Exception e){
        	System.out.println("Error: loading icons for " +
        			"a MegamekBorder!");
        	System.out.println("Error: " + e.getMessage());
        }      
    }
    
    
    public void paintBorder(Component c, Graphics g, int x, int y, int width, 
    		int height) {
        g.translate(x, y);
        
        if (tl_corner.getImageLoadStatus() == MediaTracker.COMPLETE){
        	paintCorner(c, g, 0, 0, tl_corner);
        }
        if (bl_corner.getImageLoadStatus() == MediaTracker.COMPLETE){
        	paintCorner(c, g, 0, height - bl_corner.getIconHeight(), bl_corner);
        }
        if (tr_corner.getImageLoadStatus() == MediaTracker.COMPLETE){
        	paintCorner(c, g, width-tr_corner.getIconWidth(), 0, tr_corner);
    	}
        if (br_corner.getImageLoadStatus() == MediaTracker.COMPLETE){
        paintCorner(c, g, width-br_corner.getIconWidth(), 
        		height-br_corner.getIconHeight(), br_corner);
        }
        
        if (top_line.getImageLoadStatus() == MediaTracker.COMPLETE){
	        paintEdge(c, g, tl_corner.getIconWidth(), 0, 
	        		width - 
	        			(tl_corner.getIconWidth() + tr_corner.getIconWidth()), 
	        		top_line.getIconHeight(), top_line);
        }
        if (left_line.getImageLoadStatus() == MediaTracker.COMPLETE){
	        paintEdge(c, g, 0, tl_corner.getIconHeight(), 
	        		left_line.getIconWidth(), 
	        		height - 
	        			(tl_corner.getIconHeight() + bl_corner.getIconHeight()), 
	        		left_line);
        }
        if (bottom_line.getImageLoadStatus() == MediaTracker.COMPLETE){
	        paintEdge(c, g, bl_corner.getIconWidth(), 
	        		height - bottom_line.getIconHeight(), 
	        		width - 
	        			(bl_corner.getIconWidth() + br_corner.getIconWidth()), 
	        		bottom_line.getIconHeight(), bottom_line);
        }
        if (right_line.getImageLoadStatus() == MediaTracker.COMPLETE){
	        paintEdge(c, g, width-right_line.getIconWidth(), 
	        		tr_corner.getIconHeight(), right_line.getIconWidth(), 
	        		height - 
	        			(tr_corner.getIconHeight() + br_corner.getIconHeight()), 
	        		right_line); 
        }
        
    
        g.translate(-x, -y);
    }
    
    private void paintCorner(Component c, Graphics g, int x, int y, 
            ImageIcon icon) {
        
        int tileW = icon.getIconWidth();
        int tileH = icon.getIconHeight();
        g = g.create(x, y, x+tileW, y+tileH);
        icon.paintIcon(c,g,0,0);
        g.dispose();        
    }
    
    private void paintEdge(Component c, Graphics g, int x, int y, int width, 
            int height, ImageIcon icon) {
        g = g.create(x, y, width, height);
        int tileW = icon.getIconWidth();
        int tileH = icon.getIconHeight();
        for (x = 0; x < width; x += tileW) {
            for (y = 0; y < height; y += tileH) {
                icon.paintIcon(c, g, x, y);
            }
        }
        g.dispose();
    }
    
    public Insets getBorderInsets(Component c, Insets insets) {
        return computeInsets(insets);
    }
    
    public Insets getBorderInsets() {
        return computeInsets(new Insets(0,0,0,0));
    }
    
    private Insets computeInsets(Insets insets) {
        insets.top = Math.min(tl_corner.getIconHeight(),
        		Math.min(top_line.getIconHeight(),tr_corner.getIconHeight()));
        insets.right = Math.min(tr_corner.getIconWidth(),
        		Math.min(right_line.getIconWidth(),br_corner.getIconWidth()));
        insets.bottom = Math.min(bl_corner.getIconHeight(),
        		Math.min(bottom_line.getIconHeight(),br_corner.getIconHeight()));
        insets.left = Math.min(tl_corner.getIconWidth(),
        		Math.min(left_line.getIconWidth(),bl_corner.getIconWidth()));

        return insets;
    }
    
    public boolean isBorderOpaque() {
        return true;
    }

}

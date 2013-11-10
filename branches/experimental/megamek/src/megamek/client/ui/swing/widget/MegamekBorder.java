package megamek.client.ui.swing.widget;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.border.EmptyBorder;

import megamek.common.Configuration;

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
        
        }
    }
    
    
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        g.translate(x, y);

        int tileW = tl_corner.getIconWidth();
        int tileH = tl_corner.getIconHeight();
        
        paintCorner(c, g, 0, 0, tl_corner);
        paintCorner(c, g, 0, height-tileH, bl_corner);
        paintCorner(c, g, width-tileW, 0, tr_corner);        
        paintCorner(c, g, width-tileW, height-tileH, br_corner);
     
        paintEdge(c, g, tileW, 0, width - 2*tileW, tileH, top_line);
        paintEdge(c, g, 0, tileH, tileW, height-2*tileH, left_line);        
        paintEdge(c, g, tileW, height-tileH, width-2*tileW, tileH, bottom_line);       
        paintEdge(c, g, width-tileW, tileH, tileW, height-2*tileH, right_line);        
    
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

    /* should be protected once api changes area allowed */
    private Insets computeInsets(Insets insets) {

        insets.top = tl_corner.getIconHeight();
        insets.right = tl_corner.getIconWidth();
        insets.bottom = tl_corner.getIconHeight();
        insets.left = tl_corner.getIconWidth();

        return insets;
    }
    
    public boolean isBorderOpaque() {
        return true;
    }

}

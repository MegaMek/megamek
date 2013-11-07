package megamek.client.ui.swing.widget;

import java.awt.Insets;
import java.io.File;

import javax.swing.ImageIcon;

import megamek.common.Configuration;

public class MegamekButtonBorder extends MegamekBorder {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public MegamekButtonBorder(Insets i){
        super(i);
        loadIcons();
    }
    
    public MegamekButtonBorder(int top, int left, int bottom, int right){
        super(top,left,bottom,right);
        loadIcons();        
    }
    
    public void loadIcons(){
        try {
            java.net.URI imgURL = 
                    new File(Configuration.widgetsDir(),
                    "button_top_left.png").toURI();
            tl_corner = new ImageIcon(imgURL.toURL());
            imgURL = 
                    new File(Configuration.widgetsDir(),
                    "button_top_right.png").toURI();
            tr_corner = new ImageIcon(imgURL.toURL());
            imgURL = 
                    new File(Configuration.widgetsDir(),
                    "button_bottom_left.png").toURI();
            bl_corner = new ImageIcon(imgURL.toURL());
            imgURL = 
                    new File(Configuration.widgetsDir(),
                    "button_bottom_right.png").toURI();
            br_corner = new ImageIcon(imgURL.toURL());
            
            imgURL = 
                    new File(Configuration.widgetsDir(),
                    "button_left_line.png").toURI();
            left_line = new ImageIcon(imgURL.toURL());
            imgURL = 
                    new File(Configuration.widgetsDir(),
                    "button_right_line.png").toURI();
            right_line = new ImageIcon(imgURL.toURL()); 
            imgURL = 
                    new File(Configuration.widgetsDir(),
                    "button_top_line.png").toURI();
            top_line = new ImageIcon(imgURL.toURL()); 
            imgURL =
                    new File(Configuration.widgetsDir(),
                    "button_bottom_line.png").toURI();
            bottom_line = new ImageIcon(imgURL.toURL()); 
        } catch (Exception e){
        
        }
    }

}

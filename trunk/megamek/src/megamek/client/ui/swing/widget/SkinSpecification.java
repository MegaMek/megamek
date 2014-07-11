package megamek.client.ui.swing.widget;

import java.awt.Color;
import java.util.ArrayList;

/**
 * A class that contains state information that specifies a skin.
 * 
 * @author walczak
 *
 */
public class SkinSpecification {
    
    public boolean allIconsSpecified = true;
    
    public String tl_corner;
    public String tr_corner;
    public String bl_corner;
    public String br_corner;
    
    public ArrayList<String> topEdge;
    public ArrayList<Boolean> topShouldTile;
    public ArrayList<String> rightEdge;
    public ArrayList<Boolean> rightShouldTile;
    public ArrayList<String> bottomEdge;
    public ArrayList<Boolean> bottomShouldTile;
    public ArrayList<String> leftEdge;
    public ArrayList<Boolean> leftShouldTile;
    
    public Color fontColor = Color.black;
    
    public ArrayList<String> backgrounds;
    
    boolean noBorder = false;
    
    boolean tileBackground = true;
    
    public SkinSpecification(){
        tl_corner = tr_corner = bl_corner = br_corner = "";
        topEdge = new ArrayList<String>();
        rightEdge = new ArrayList<String>();
        bottomEdge = new ArrayList<String>();
        leftEdge = new ArrayList<String>();
        backgrounds = new ArrayList<String>();
        topShouldTile = new ArrayList<Boolean>();
        rightShouldTile = new ArrayList<Boolean>();
        bottomShouldTile = new ArrayList<Boolean>();
        leftShouldTile = new ArrayList<Boolean>();
    }
    
    public boolean hasBorder() {
        // Return false if any corner doesn't exsit
        if (tl_corner.equals("") || tr_corner.equals("")
                || bl_corner.equals("") || br_corner.equals("")) {
            return false;
        }
        
        // Return false if any edge doesn't exsit
        if (topEdge.size() == 0 || rightEdge.size() == 0
                || bottomEdge.size() == 0 || leftEdge.size() == 0) {
            return false;
        }
        
        // Make sure edges don't contain empty strings
        for (String edge : topEdge) {
            if (edge.equals("")) {
                return false;
            }
        }
        
        for (String edge : rightEdge) {
            if (edge.equals("")) {
                return false;
            }
        }
        
        for (String edge : bottomEdge) {
            if (edge.equals("")) {
                return false;
            }
        }
        
        for (String edge : leftEdge) {
            if (edge.equals("")) {
                return false;
            }
        }
        
        return true;
    }
    
    public boolean hasBackgrounds() {
        if (backgrounds.size() == 0) {
            return false;
        }
        
        for (String bg : backgrounds) {
            if (bg.equals("")) {
                return false;
            }
        }
        return true;
    }


}

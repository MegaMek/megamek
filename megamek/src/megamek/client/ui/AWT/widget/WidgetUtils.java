/*
 * Created on 20.06.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package megamek.client.util.widget;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;

public class WidgetUtils {
    public static void setAreaColor(PMSimplePolygonArea ha, PMValueLabel l, double percentRemaining) {
        if ( percentRemaining <= 0 ){
            ha.backColor = Color.darkGray.brighter();
            l.setColor(Color.blue);
            ha.highlightBorderColor = Color.green;          
        } else if ( percentRemaining <= .25 ){
            ha.backColor = Color.red.brighter();
            l.setColor(Color.blue);
            ha.highlightBorderColor = Color.green;          
        } else if ( percentRemaining <= .75 ){
            ha.backColor = Color.yellow;
            l.setColor(Color.blue);
            ha.highlightBorderColor = Color.green;
        } else {
            ha.backColor = Color.gray.brighter();
            l.setColor(Color.red);
            ha.highlightBorderColor = Color.red;
        }
    }
    
    public static PMSimpleLabel createLabel(String s, FontMetrics fm,Color color, int x, int y){
        PMSimpleLabel l = new PMSimpleLabel(s, fm, color);
        centerLabelAt(l,x,y);
        return l; 
    }
    
    public static PMValueLabel createValueLabel(int x, int y, String v, FontMetrics fm){
        PMValueLabel l = new PMValueLabel(fm, Color.red);
        centerLabelAt(l, x, y);
        l.setValue(v);
        return l;
    }
    
    public static void centerLabelAt(PMSimpleLabel l, int x, int y){
        if (l == null) return;
        Dimension d = l.getSize();
        l.moveTo( x - d.width/2, y + d.height/2); 
    }
}

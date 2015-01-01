/**
 * MegaMek - Copyright (C) 2004,2006 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */

package megamek.client.ui.AWT.widget;

import java.awt.*;

public class TransparentLabel extends PicMap{
    static final long serialVersionUID = 570936407477398505L;
    
    public final static int LEFT = -1;
    public final static int CENTER = 0;
    public final static int RIGHT = 1;
    
    private int align = 0;
    PMSimpleLabel l;
    
    public TransparentLabel( String s, FontMetrics fm, Color c, int al){
        super();
        l = new PMSimpleLabel(s, fm, c);
        addElement(l);
        l.moveTo(0, l.getSize().width - l.getDescent());
        setBackgroundOpaque(false);
        align = al;
        onResize();
    }
    
    public void setText(String s){
        l.setString(s);
        onResize();
        repaint();
    }
    
    public void onResize(){
        Rectangle r = getContentBounds();
        Dimension d = getSize();
        if(align < 0){
            setContentMargins(0,0,(d.width - r.width),0);
        } else if(align == 0) {
            setContentMargins((d.width - r.width)/2,0,(d.width - r.width)/2,0);
        } else if(align > 0) {
            setContentMargins((d.width - r.width),0,0,0);
        }
        
        r = getContentBounds();
        d = getSize();
    }
}

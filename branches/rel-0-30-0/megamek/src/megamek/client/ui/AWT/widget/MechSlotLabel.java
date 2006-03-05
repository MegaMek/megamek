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

public class MechSlotLabel extends PicMap {
    static final long serialVersionUID = -6902502003022222382L;
    
    //Color Of background
    private Color bgColor;
    //Margins - used to draw 3D box
    private static final int MARGIN_WIDTH = 2;
    
    private BackGroundDrawer bgd = new BackGroundDrawer (null);
    
    public MechSlotLabel (String s, FontMetrics fm, Image im, Color textColor, Color bgColor){
        super();
        PMPicArea pa = new PMPicArea(im);
        pa.setCursor(Cursor.getDefaultCursor());
        addElement(pa);
        PMSimpleLabel l = new PMSimpleLabel(s, fm, textColor);
        addElement(l);
        l.moveTo(pa.getBounds().width + 5, (pa.getBounds().height - l.getBounds().height)/2 + l.getSize().height - l.getDescent());
        setContentMargins(MARGIN_WIDTH, MARGIN_WIDTH, MARGIN_WIDTH, MARGIN_WIDTH);
        this.bgColor = bgColor;
        addBgDrawer(bgd);
        drawBGImage();
    }
    
    private void drawBGImage(){
        Dimension  d = getSize();
        int w = d.width;
        int h = d.height;
        Image BGImage = createImage(w, h);
        if (BGImage == null) return;
        Graphics g = BGImage.getGraphics();
        g.setColor(Color.green.darker().darker());
        g.fillRect(0, 0, w, h);
        g.setColor(Color.green.darker());
        g.fillRect(w-2, 0, 2, h);
        g.fillRect(0,h-2, w, 2);
        g.setColor(Color.green.darker().darker().darker());
        g.fillRect(0, 0, w, 2);
        g.fillRect(0, 0, 2, h);
        g.dispose();
        bgd.setImage(BGImage);
    }
    
    public void onResize(){
        drawBGImage();
    }
}

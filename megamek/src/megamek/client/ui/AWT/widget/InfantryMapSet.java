/**
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.client.util.widget;

import java.awt.*;
import java.util.*;
import megamek.client.*;
import megamek.common.*;

/**
 * Set of areas for PicMap to represent infantry platoon in MechDisplay 
 */

public class InfantryMapSet implements DisplayMapSet{
	//Picture to represent single trooper
	private Image infImage;
    //Reference to Component class required to handle images and fonts
	private Component comp;
	 // Assuming that it will be no more that 28 men in platoon
	private PMPicArea[] areas = new PMPicArea[28];
	// Main areas group that will be passing to PicMap
	private PMAreasGroup content = new PMAreasGroup();
	//Label
	private PMValueLabel label;
	//Set of Backgrownd drawers
	private Vector    bgDrawers = new Vector();

	
	private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN, 10);	

	
	public InfantryMapSet(Component c){
		comp = c;
		setAreas();
		setBackGround();
	}
	
	public PMAreasGroup getContentGroup(){
        return content;
    }
    
    public Vector getBackgroundDrawers(){
    	return bgDrawers;
    };
    
    public void setEntity(Entity e){
       Infantry inf = (Infantry) e;
       int men = inf.getInternal(0);
       for (int i = 0; i < men; i++){
       	   areas[i].setVisible(true);
       }
       for (int i = men; i < 28; i++){
       	   areas[i].setVisible(false);
       }
       
       label.setValue("Infantry Platoon: " + Integer.toString(men) + " men");
    }
	
	private void setAreas(){
		int stepX = 30;
	    int stepY = 42;
		infImage = comp.getToolkit().getImage("data/widgets/inf.gif");
		PMUtil.setImage(infImage, comp);
		for(int i = 0; i < 28; i++){
			int shiftX = (i % 5) * stepX;
			int shiftY = (i / 5) * stepY;
			areas[i] = new PMPicArea(infImage);
			areas[i].translate(shiftX, shiftY);
			content.addArea((PMElement) areas[i]);
		}
		
		FontMetrics fm = comp.getFontMetrics(FONT_VALUE);
		label = new PMValueLabel(fm, Color.white);
		label.setValue("Infantry Platoon: 00 men");
		Dimension d = label.getSize();
		content.translate(0, d.height + 5);
		label.moveTo(d.width / 2, d.height);
		content.addArea((PMElement) label);
	}
	
	private void setBackGround(){
        Image tile = comp.getToolkit().getImage("data/widgets/tile.gif");
        PMUtil.setImage(tile, comp);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_HORIZONTAL | 
                BackGroundDrawer.VALIGN_TOP;
        tile = comp.getToolkit().getImage("data/widgets/h_line.gif");
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));                
        
            b = BackGroundDrawer.TILING_HORIZONTAL | 
                BackGroundDrawer.VALIGN_BOTTOM;
        tile = comp.getToolkit().getImage("data/widgets/h_line.gif");
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_VERTICAL | 
                BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage("data/widgets/v_line.gif");
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_VERTICAL | 
                BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage("data/widgets/v_line.gif");
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
                
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage("data/widgets/tl_corner.gif");
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage("data/widgets/bl_corner.gif");
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage("data/widgets/tr_corner.gif");
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage("data/widgets/br_corner.gif");
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
         
    }
	
}
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

import megamek.client.GUIPreferences;
import megamek.client.Messages;
import megamek.common.*;

/**
 * Class which keeps set of all areas required to 
 * represent Battle Armor unit in MechDsiplay.ArmorPanel class.
 */


public class BattleArmorMapSet implements DisplayMapSet{
    
    //Picture with figure
	private Image battleArmorImage;
	//Images that shows how much armor + 1 internal damage left.
	private Image[] armorImage = new Image[5];
	//Reference to Component (required for Image handling)
	private Component comp;
	//Set of areas to show BA figures
	private PMPicArea[] unitAreas =  new PMPicArea[5];
	//Set of areas to show BA armor left
	private PMPicArea[] armorAreas = new PMPicArea[5];
	//Set of labels to show BA armor left
	private PMValueLabel[]    armorLabels = new PMValueLabel[5];
	//Content group which will be sent to PicMap component
	private PMAreasGroup content = new PMAreasGroup();
	//Set of Backgrpund drawers which will be sent to PicMap component
	private Vector    bgDrawers = new Vector();
	
	private int stepY = 53;
	
	private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN, //$NON-NLS-1$ 
	        GUIPreferences.getInstance().getMechDisplayArmorLargeFontSize());
	
	/**
     * This constructor have to be called anly from addNotify() method
     */
	public BattleArmorMapSet( Component c){
		comp = c;
		setAreas();
		setBackGround();
	}

	private void setAreas(){
		FontMetrics fm = comp.getFontMetrics(FONT_VALUE);
		
		battleArmorImage = comp.getToolkit().getImage("data/widgets/battle_armor.gif"); //$NON-NLS-1$
		PMUtil.setImage(battleArmorImage, comp);
		for(int i = 0; i < 5; i++){
		    int shiftY = i * stepY;
		    unitAreas[i] = new PMPicArea(battleArmorImage);
		    unitAreas[i].translate(0, shiftY);
			content.addArea((PMElement) unitAreas[i]);
			
			
			armorImage[i] = comp.createImage(105, 12);
		    armorAreas[i] = new PMPicArea(armorImage[i]);
		    armorAreas[i].translate(45, shiftY + 12);
		    content.addArea((PMElement) armorAreas[i]);
		    
		    armorLabels[i] = new PMValueLabel(fm, Color.red.brighter());
		    armorLabels[i]. moveTo(160, shiftY + 24);
		    content.addArea((PMElement) armorLabels[i]);
		}
	}
	
	public PMAreasGroup getContentGroup(){
        return content;
    }
    
    public Vector getBackgroundDrawers(){
    	return bgDrawers;
    }
    
    
    public void setEntity(Entity e){
    	BattleArmor ba = (BattleArmor) e;
    	int armor = 0;
    	int internal =0;
        int men = 5;
        
        if (ba.isClan()){
        	 men = 5;
        	 armorAreas[4].setVisible(true);
        	 armorLabels[4].setVisible(true);
        	 unitAreas[4].setVisible(true);
        } else{
        	 men = 4;
        	 armorAreas[4].setVisible(false);
        	 armorLabels[4].setVisible(false);
        	 unitAreas[4].setVisible(false);
        }
        
        for(int i = 0; i < men ; i++){
       	    armor = (ba.getArmor(i+1, false) < 0) ? 0: ba.getArmor(i+1, false);
       	    internal =  (ba.getInternal(i+1) < 0) ? 0: ba.getInternal(i+1);
       	    if((armor+internal) == 0){
       	    	armorAreas[i].setVisible(false);
       	    	armorLabels[i].setValue(Messages.getString("BattleArmorMapSet.Killed")); //$NON-NLS-1$
       	    } else {
       	    	drawArmorImage(armorImage[i], armor+internal);
       	    	armorLabels[i].setValue(Integer.toString(armor+internal));
                armorAreas[i].setVisible(true);
       	    }
        }
    }

	private void setBackGround(){
        Image tile = comp.getToolkit().getImage("data/widgets/tile.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_HORIZONTAL | 
                BackGroundDrawer.VALIGN_TOP;
        tile = comp.getToolkit().getImage("data/widgets/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));                
        
            b = BackGroundDrawer.TILING_HORIZONTAL | 
                BackGroundDrawer.VALIGN_BOTTOM;
        tile = comp.getToolkit().getImage("data/widgets/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_VERTICAL | 
                BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage("data/widgets/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_VERTICAL | 
                BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage("data/widgets/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
                
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage("data/widgets/tl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage("data/widgets/bl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage("data/widgets/tr_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage("data/widgets/br_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
         
    }
    
    //Redraws armor images
    private void drawArmorImage(Image im, int a){
        int x = 0;
        int w = im.getWidth(null);
        int h= im.getHeight(null);
        Graphics g = im.getGraphics();
        g.setColor(Color.black);
        g.fillRect(0,0,w,h);
        for (int i = 0; i< a; i++){
            x = i*7;
            g.setColor(Color.green.darker());
            g.fillRect(x, 0, 5, 12);
        }
    }

}

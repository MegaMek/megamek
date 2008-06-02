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





package megamek.client.ui.AWT.widget;

import java.awt.*;
import java.util.*;

import megamek.client.ui.AWT.GUIPreferences;
import megamek.client.ui.AWT.Messages;
import megamek.common.*;

/**
 * Class which keeps set of all areas required to 
 * represent Battle Armor unit in MechDsiplay.ArmorPanel class.
 */


public class SquadronMapSet implements DisplayMapSet{

    private static final String IMAGE_DIR = "data/images/units";
    
    //Picture with figure
    private Image fighterImage;
    //Images that shows how much armor + 1 internal damage left.
    //private Image[] armorImage = new Image[BattleArmor.BA_MAX_MEN];
    private Image armorImage;
    //Reference to Component (required for Image handling)
    private Component comp;
    //Set of areas to show BA figures
    private PMPicArea[] unitAreas =  new PMPicArea[FighterSquadron.MAX_SIZE];
    private PMSimpleLabel[] deadUnit = new PMSimpleLabel[FighterSquadron.MAX_SIZE];
    //Set of areas to show BA armor left
    //private PMPicArea[] armorAreas = new PMPicArea[BattleArmor.BA_MAX_MEN];
    private PMPicArea armorAreas;
    //Set of labels to show BA armor left
    private PMValueLabel   armorLabels;
    private PMValueLabel   armorNameLabel;
    
    private PMSimpleLabel[] labels = new PMSimpleLabel[4];
    private PMValueLabel[] vLabels = new PMValueLabel[4];
    
    //Content group which will be sent to PicMap component
    private PMAreasGroup content = new PMAreasGroup();
    //Set of Backgrpund drawers which will be sent to PicMap component
    private Vector<BackGroundDrawer>    bgDrawers = new Vector<BackGroundDrawer>();
    
    private int stepY = 40;
    private int stepX = 40;
    
    private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN, //$NON-NLS-1$ 
            GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorLargeFontSize"));
    
    private static final Font BIG_FONT_VALUE = new Font("SansSerif", Font.PLAIN, //$NON-NLS-1$ 
            24);
    
    /**
     * This constructor have to be called anly from addNotify() method
     */
    public SquadronMapSet( Component c){
        comp = c;
        setAreas();
        setBackGround();
    }

    private void setAreas(){
        FontMetrics fm = comp.getFontMetrics(FONT_VALUE);
        FontMetrics fmBig = comp.getFontMetrics(BIG_FONT_VALUE);
        
        fighterImage = comp.getToolkit().getImage(IMAGE_DIR+"/asf.gif"); //$NON-NLS-1$
        PMUtil.setImage(fighterImage, comp);
        armorImage = comp.createImage(120, 12);
        armorAreas = new PMPicArea(armorImage);
        armorAreas.translate(30, 0);
        content.addArea(armorAreas);
        armorLabels = new PMValueLabel(fm, Color.red.brighter());
        armorLabels. moveTo(160, 12);
        content.addArea(armorLabels);
        armorNameLabel = new PMValueLabel(fm, Color.white);
        armorNameLabel. moveTo(0, 12);
        content.addArea(armorNameLabel);
        for(int i = 0; i < FighterSquadron.MAX_SIZE; i++){
            int shiftX = (i % 3) * stepX;
        	int shiftY = (i / 3) * stepY + 110;
            unitAreas[i] = new PMPicArea(fighterImage);
            unitAreas[i].translate(shiftX, shiftY);
            content.addArea(unitAreas[i]);
            int newx = shiftX + (stepX/2);
            int newy = shiftY + (stepY/2);
            deadUnit[i] = WidgetUtils.createLabel("X", fmBig, Color.red.brighter(),newx+10,newy); //$NON-NLS-1$
            content.addArea(deadUnit[i]);
        }
        
        labels[0] = WidgetUtils.createLabel("Avionics:", fm, Color.white,10,30); //$NON-NLS-1$
        labels[1] = WidgetUtils.createLabel("Engine:", fm, Color.white,10,45); //$NON-NLS-1$
        labels[2] = WidgetUtils.createLabel("FCS:", fm, Color.white,10,60); //$NON-NLS-1$
        labels[3] = WidgetUtils.createLabel("Sensors:", fm, Color.white,10,75); //$NON-NLS-1$
        
        vLabels[0] = WidgetUtils.createValueLabel(40, 30, "", fm); //$NON-NLS-1$
        vLabels[1] = WidgetUtils.createValueLabel(40, 45, "", fm); //$NON-NLS-1$
        vLabels[2] = WidgetUtils.createValueLabel(40, 60, "", fm); //$NON-NLS-1$
        vLabels[3] = WidgetUtils.createValueLabel(40, 75, "", fm); //$NON-NLS-1$
        content.addArea(labels[0]);
        content.addArea(vLabels[0]);
        content.addArea(labels[1]);
        content.addArea(vLabels[1]);
        content.addArea(labels[2]);
        content.addArea(vLabels[2]);
        content.addArea(labels[3]);
        content.addArea(vLabels[3]);
        
        
    }
    
    public PMAreasGroup getContentGroup(){
        return content;
    }
    
    public Vector<BackGroundDrawer> getBackgroundDrawers(){
        return bgDrawers;
    }
    
    
    public void setEntity(Entity e){
        FighterSquadron fs = (FighterSquadron) e;
        int armor = 0;
        int orig = 1;
        int fighters = fs.getN0Fighters();
        int active = fs.getNFighters();

        armorAreas.setVisible(true);
        armorLabels.setVisible(true);
        armorNameLabel.setVisible(true);
        
        for (int x=0; x<fighters; x++) {
             unitAreas[x].setVisible(true);
             deadUnit[x].setVisible(false);
        }
        for (int x=fighters; x<FighterSquadron.MAX_SIZE; x++) {
             unitAreas[x].setVisible(false);
             deadUnit[x].setVisible(false);
        }

        armor = (fs.getArmor() < 0) ? 0: fs.getArmor();
        orig = fs.getTotalOArmor();
        armorLabels.setValue(Integer.toString(armor));
        //TODO: put this into messages
        armorNameLabel.setValue("Armor:");
        //      now for the vitals
        vLabels[0].setValue(getCriticalHitTally(fs.getAvionicsHits(),3));
        vLabels[1].setValue(getCriticalHitTally(fs.getEngineHits(),fs.getMaxEngineHits()));
        vLabels[2].setValue(getCriticalHitTally(fs.getFCSHits(),3));
        vLabels[3].setValue(getCriticalHitTally(fs.getSensorHits(),3));
        
        drawArmorImage(armorImage, armor, orig);
        for(int i = 0; i < fighters ; i++) {
            if(i >= active){
            	//then this one is dead (put an X through it?)
            	deadUnit[i].setVisible(true);
            }
        }
    }

    private void setBackGround(){
        Image tile = comp.getToolkit().getImage(IMAGE_DIR+"/tile.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_HORIZONTAL | 
                BackGroundDrawer.VALIGN_TOP;
        tile = comp.getToolkit().getImage(IMAGE_DIR+"/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));                
        
            b = BackGroundDrawer.TILING_HORIZONTAL | 
                BackGroundDrawer.VALIGN_BOTTOM;
        tile = comp.getToolkit().getImage(IMAGE_DIR+"/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_VERTICAL | 
                BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(IMAGE_DIR+"/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_VERTICAL | 
                BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(IMAGE_DIR+"/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
                
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(IMAGE_DIR+"/tl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(IMAGE_DIR+"/bl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(IMAGE_DIR+"/tr_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(IMAGE_DIR+"/br_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
         
    }
    
    //Redraws armor images
    private void drawArmorImage(Image im, int a, int orig){
        int w = im.getWidth(null);
        int h= im.getHeight(null);
        Graphics g = im.getGraphics();
        g.setColor(Color.black);
        g.fillRect(0,0,w,h);
        int percent = (int)Math.ceil(120.0 * ((double)a)/((double)orig));
        g.setColor(Color.green.darker());
        g.fillRect(0, 0, percent, 12);
    }
    
    private String getCriticalHitTally(int tally, int max) {
    	
    	String marks = "";
    	
    	if(tally < 1) {
    		return marks;
    	}
    	
    	if(tally >= max) {
    		marks = "Out";
    		return marks;
    	}
    	
    	while(tally > 0) {
    		marks = marks + "X";
    		tally--;
    	}
    	
    	return marks;
    }

}

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
 * Set of elements to reperesent general unit information in MechDisplay
 */

public class GeneralInfoMapSet implements DisplayMapSet{
    private Component comp;
    private PMAreasGroup content = new PMAreasGroup();
    private PMSimpleLabel mechTypeL0, mechTypeL1, statusL, playerL, teamL, weightL, pilotL, mpL0, mpL1, mpL2, mpL3, curMoveL, heatL;
	private PMSimpleLabel  statusR, playerR, teamR, weightR, pilotR, mpR0, mpR1, mpR2, mpR3,  curMoveR, heatR;
	private Vector    bgDrawers = new Vector();
    private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN, 12);	
    private static final Font FONT_TITLE = new Font("SansSerif", Font.ITALIC, 12);	
	private final static int MAX_STR_LENGTH = 18;
	
	
	/**
     * This constructor have to be called anly from addNotify() method
     */
	public GeneralInfoMapSet( Component c){
		comp = c;
		setAreas();
		setBackGround();
	}



	private void setAreas(){
		FontMetrics fm = comp.getFontMetrics(FONT_TITLE);
		
		mechTypeL0 = createLabel("Loc0st LCT-1L", fm, 0, 10);
		mechTypeL0.setColor(Color.yellow);
		content.addArea(mechTypeL0);
		
		
		mechTypeL1 = createLabel("***", fm, 0, 25);
		mechTypeL1.setColor(Color.yellow);
		content.addArea(mechTypeL1);
		
		fm = comp.getFontMetrics(FONT_VALUE);

		playerL = createLabel("Player:", fm, 0, 40);
		content.addArea(playerL);
		
		playerR = createLabel("?", fm, playerL.getSize().width + 10, 40);
		content.addArea(playerR);		
		
		teamL = createLabel("Team:", fm, 0, 55);
		content.addArea(teamL);
		
		teamR = createLabel("?", fm, teamL.getSize().width + 10, 55);
		content.addArea(teamR);
		
				
		statusL = createLabel("Status:", fm, 0, 70);
		content.addArea(statusL);

		statusR = createLabel("***", fm, statusL.getSize().width + 10, 70);
		content.addArea(statusR);

		weightL = createLabel("Weight:", fm, 0, 85);
		content.addArea(weightL);
		
		weightR = createLabel("***", fm, weightL.getSize().width + 10, 85);
		content.addArea(weightR);
		
		
		pilotL = createLabel("Pilot:", fm, 0, 100);
		content.addArea(pilotL);
		
		pilotR = createLabel("***", fm, pilotL.getSize().width + 10, 100);
		content.addArea(pilotR);
		
		mpL0 = createLabel("Movement:", fm, 0, 115);
		content.addArea(mpL0);
		
		mpL1 = createLabel("Walk:", fm, 0 , 130);
		mpL1.moveTo( mpL0.getSize().width - mpL1.getSize().width, 130);
		content.addArea(mpL1);
		
		mpL2 = createLabel("Run:", fm, 0 , 145);
		mpL2.moveTo( mpL0.getSize().width - mpL2.getSize().width, 145);
		content.addArea(mpL2);
		
		mpL3 = createLabel("Jump:", fm, 0 , 160);
		mpL3.moveTo( mpL0.getSize().width - mpL3.getSize().width, 160);
		content.addArea(mpL3);
		
		
		mpR0 = createLabel("", fm, mpL0.getSize().width + 10, 115);
		content.addArea(mpR0);
		
		mpR1 = createLabel("***", fm, mpL0.getSize().width + 10, 130);
		content.addArea(mpR1);
		
		mpR2 = createLabel("***", fm, mpL0.getSize().width + 10, 145);
		content.addArea(mpR2);
				
		mpR3 = createLabel("***", fm, mpL0.getSize().width + 10, 160);
		content.addArea(mpR3);
		
		curMoveL = createLabel("Currently:", fm, 0, 175);
		content.addArea(curMoveL);
		
		curMoveR = createLabel("***", fm, curMoveL.getSize().width + 10, 175);
		content.addArea(curMoveR);
		
				
		heatL = createLabel("Heat:", fm, 0, 190);
		content.addArea(heatL);
		
		heatR = createLabel("***", fm, heatL.getSize().width + 10, 190);
		content.addArea(heatR);
	}
	
	 /**
     * updates fields for the unit
     */
    public void setEntity(Entity en) {
    	
    	String s = en.getShortName();
    	mechTypeL1.setVisible(false);
    	
    	if(s.length() > MAX_STR_LENGTH){
    		int i = s.lastIndexOf(" ", MAX_STR_LENGTH);
    		mechTypeL0.setString(s.substring(0,i));
            mechTypeL1.setString(s.substring(i).trim());
            mechTypeL1.setVisible(true);
    	} else {
    		mechTypeL0.setString(s);
    		mechTypeL1.setString("");
    	}
    	
    	
        statusR.setString(en.isProne() ? "prone" : "normal");
        playerR.setString(en.getOwner().getName());
        teamR.setString(en.getOwner().getTeam() == 0 ? "No Team" : "Team " + en.getOwner().getTeam());
        weightR.setString(Integer.toString((int)en.getWeight()));
        pilotR.setString(en.crew.getDesc());
        
        if (en.mpUsed > 0) {
        	mpR0.setString("("+ en.mpUsed + " used)");
        } else {
        	mpR0.setString("");
        }
        mpR1.setString(Integer.toString(en.getWalkMP()));
        mpR2.setString(en.getRunMPasString());
        mpR3.setString(Integer.toString(en.getJumpMPWithTerrain()));

        curMoveR.setString(en.getMovementString(en.moved) + (en.moved == en.MOVE_NONE ? "" : " " + en.delta_distance));
        
        int heatCap = en.getHeatCapacity();
        int heatCapWater = en.getHeatCapacityWithWater();
        String heatCapacityStr = Integer.toString(heatCap);
        
        if ( heatCap < heatCapWater ) {
          heatCapacityStr = heatCap + " [" + heatCapWater + "]";
        }
        
        heatR.setString(Integer.toString(en.heat) + " (" + heatCapacityStr + " capacity)");
        
        if (en instanceof Infantry){
        	heatL.setVisible(false);
        	heatR.setVisible(false);
        } else {
        	heatL.setVisible(true);
        	heatR.setVisible(true);
        }
    
    
    }
        
	public PMAreasGroup getContentGroup(){
        return content;
    }
    
    public Vector getBackgroundDrawers(){
    	return bgDrawers;
    }
    
	private void setBackGround(){
        Image tile = comp.getToolkit().getImage("data/widgets/tile.gif");
        PMUtil.setImage(tile,comp);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_HORIZONTAL | 
                BackGroundDrawer.VALIGN_TOP;
        tile = comp.getToolkit().getImage("data/widgets/h_line.gif");
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));                
        
            b = BackGroundDrawer.TILING_HORIZONTAL | 
                BackGroundDrawer.VALIGN_BOTTOM;
        tile = comp.getToolkit().getImage("data/widgets/h_line.gif");
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_VERTICAL | 
                BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage("data/widgets/v_line.gif");
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_VERTICAL | 
                BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage("data/widgets/v_line.gif");
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
                
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage("data/widgets/tl_corner.gif");
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage("data/widgets/bl_corner.gif");
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage("data/widgets/tr_corner.gif");
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage("data/widgets/br_corner.gif");
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
         
    }
    
    private PMSimpleLabel createLabel(String s, FontMetrics fm, int x, int y){
        PMSimpleLabel l = new PMSimpleLabel(s, fm, Color.white);
        l.moveTo(x, y);
        return l; 
    }   
    
	
}

/**
 * MegaMek - Copyright (C) 2003,2004 Ben Mazur (bmazur@sev.org)
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
import megamek.common.*;
import megamek.common.options.GameOption;

/**
 * Set of elements to reperesent general unit information in MechDisplay
 */

public class GeneralInfoMapSet implements DisplayMapSet{
    private Component comp;
    private PMAreasGroup content = new PMAreasGroup();
    private PMSimpleLabel mechTypeL0, mechTypeL1, statusL, playerL, teamL,
        weightL, pilotL, mpL0, mpL1, mpL2, mpL3, curMoveL, heatL,
        movementTypeL, ejectL;
    private PMSimpleLabel statusR, playerR, teamR, weightR, pilotR,
        mpR0, mpR1, mpR2, mpR3, curMoveR, heatR, movementTypeR, ejectR;
    private PMSimpleLabel[] advantagesR;
    private Vector    bgDrawers = new Vector();
    private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN, Settings.mechDisplayLargeFontSize);
    private static final Font FONT_TITLE = new Font("SansSerif", Font.ITALIC, Settings.mechDisplayLargeFontSize);
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
		
		
        mpL0 = createLabel("Movement:", fm, 0, 100);
        content.addArea(mpL0);
		
        mpL1 = createLabel("Walk:", fm, 0 , 115);
        mpL1.moveTo( mpL0.getSize().width - mpL1.getSize().width, 115);
        content.addArea(mpL1);
		
        mpL2 = createLabel("Run:", fm, 0 , 130);
        mpL2.moveTo( mpL0.getSize().width - mpL2.getSize().width, 130);
        content.addArea(mpL2);
		
        mpL3 = createLabel("Jump:", fm, 0 , 145);
        mpL3.moveTo( mpL0.getSize().width - mpL3.getSize().width, 145);
        content.addArea(mpL3);
		
		
        mpR0 = createLabel("", fm, mpL0.getSize().width + 10, 100);
        content.addArea(mpR0);
		
        mpR1 = createLabel("***", fm, mpL0.getSize().width + 10, 115);
        content.addArea(mpR1);
		
        mpR2 = createLabel("***", fm, mpL0.getSize().width + 10, 130);
        content.addArea(mpR2);
				
        mpR3 = createLabel("***", fm, mpL0.getSize().width + 10, 145);
        content.addArea(mpR3);
		
        curMoveL = createLabel("Currently:", fm, 0, 160);
        content.addArea(curMoveL);
		
        curMoveR = createLabel("***", fm, curMoveL.getSize().width + 10, 160);
        content.addArea(curMoveR);
		
				
        heatL = createLabel("Heat:", fm, 0, 175);
        content.addArea(heatL);
		
        heatR = createLabel("***", fm, heatL.getSize().width + 10, 175);
        content.addArea(heatR);

        movementTypeL = createLabel("Type:", fm, 0, 190);
        content.addArea(movementTypeL);
        movementTypeR = createLabel("***", fm, movementTypeL.getSize().width + 10, 190);
        content.addArea(movementTypeR);

        pilotL = createLabel("Pilot:", fm, 0, 215);
        content.addArea(pilotL);
        pilotR = createLabel("***", fm, pilotL.getSize().width + 10, 215);
        content.addArea(pilotR);

        ejectL = createLabel( "Autoeject Status:", fm, 0, 230);
        content.addArea( ejectL );
        ejectR = createLabel("***", fm, ejectL.getSize().width + 10, 230);
        content.addArea( ejectR );

        int vSpace = 230;
        advantagesR = new PMSimpleLabel[24];
        for (int i=0; i < advantagesR.length; i++) {
            advantagesR[i] = createLabel(new Integer(i).toString(), fm, pilotL.getSize().width + 10, vSpace);
            content.addArea(advantagesR[i]);
            vSpace += 15;
        };
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
        if (en.getOwner().getTeam() == 0) {
            teamL.setVisible(false);
            teamR.setVisible(false);
        } else {
            teamL.setVisible(true);
            teamR.setString("Team " + en.getOwner().getTeam());
            teamR.setVisible(true);
        }
        weightR.setString(Integer.toString((int)en.getWeight()));
        
        pilotR.setString(en.crew.getDesc() + " (" + en.crew.getGunnery() + "/" + en.crew.getPiloting() + ")" );

        ejectR.setString( "N/A" );
        if (en instanceof Mech) {
            if (((Mech)en).isAutoEject()) {
                ejectR.setString( "Active" );
            } else {
                ejectR.setString( "Inactive" );
            }
        }                

        for (int i=0; i < advantagesR.length; i++ ) {
            advantagesR[i].setString("");
        }
        if (en.crew.countAdvantages() > 0) {
            int i=0;
            for (Enumeration advantages = en.crew.getAdvantages(); advantages.hasMoreElements();) {
                GameOption option = (GameOption)advantages.nextElement();
                if (option.booleanValue()) {
                    advantagesR[i++].setString(option.getFullName());
                };
            };
        };
        
        if (en.mpUsed > 0) {
        	mpR0.setString("("+ en.mpUsed + " used)");
        } else {
        	mpR0.setString("");
        }
        mpR1.setString(Integer.toString(en.getWalkMP()));
        mpR2.setString(en.getRunMPasString());
        mpR3.setString(Integer.toString(en.getJumpMPWithTerrain()));

        curMoveR.setString(en.getMovementString(en.moved) + (en.moved == Entity.MOVE_NONE ? "" : " " + en.delta_distance));
        
        int heatCap = en.getHeatCapacity();
        int heatCapWater = en.getHeatCapacityWithWater();
        String heatCapacityStr = Integer.toString(heatCap);
        
        if ( heatCap < heatCapWater ) {
          heatCapacityStr = heatCap + " [" + heatCapWater + "]";
        }
        
        heatR.setString(Integer.toString(en.heat) + " (" + heatCapacityStr + " capacity)");
        
        if (en instanceof Mech){
        	heatL.setVisible(true);
        	heatR.setVisible(true);
        } else {
        	heatL.setVisible(false);
        	heatR.setVisible(false);
        }

        if (en instanceof Tank) {
            movementTypeL.setVisible(true);
            movementTypeR.setString(en.getMovementTypeAsString());
            movementTypeR.setVisible(true);
        } else {
            movementTypeL.setVisible(false);
            movementTypeR.setVisible(false);
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

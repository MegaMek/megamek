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

import megamek.client.GUIPreferences;
import megamek.client.Messages;
import megamek.common.*;
import megamek.common.options.IOption;

/**
 * Set of elements to reperesent general unit information in MechDisplay
 */

public class GeneralInfoMapSet implements DisplayMapSet{

    private static String STAR3 = "***";  //$NON-NLS-1$
    private Component comp;
    private PMAreasGroup content = new PMAreasGroup();
    private PMSimpleLabel mechTypeL0, mechTypeL1, statusL, playerL, teamL,
        weightL, bvL, pilotL, mpL0, mpL1, mpL2, mpL3, curMoveL, heatL,
        movementTypeL, ejectL;
    private PMSimpleLabel statusR, playerR, teamR, weightR, bvR, pilotR,
        mpR0, mpR1, mpR2, mpR3, curMoveR, heatR, movementTypeR, ejectR;
    private PMSimpleLabel[] advantagesR;
    private Vector    bgDrawers = new Vector();
    private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayLargeFontSize")); //$NON-NLS-1$
    private static final Font FONT_TITLE = new Font("SansSerif", Font.ITALIC, GUIPreferences.getInstance().getInt("AdvancedMechDisplayLargeFontSize")); //$NON-NLS-1$
    private int yCoord = 1;

    /**
     * This constructor have to be called anly from addNotify() method
     */
    public GeneralInfoMapSet( Component c){
        comp = c;
        setAreas();
        setBackGround();
    }

    //These two methods are used to vertically position new labels on the
    // display.
    private int getYCoord() {
        return yCoord * 15 - 5;
    }
    private int getNewYCoord() {
        yCoord++;
        return getYCoord();
    }

    private void setAreas(){
        FontMetrics fm = comp.getFontMetrics(FONT_TITLE);

        mechTypeL0 = createLabel(Messages.getString("GeneralInfoMapSet.LocOstLCT"), fm, 0, getYCoord()); //$NON-NLS-1$
        mechTypeL0.setColor(Color.yellow);
        content.addArea(mechTypeL0);
        
        mechTypeL1 = createLabel(STAR3, fm, 0, getNewYCoord());
        mechTypeL1.setColor(Color.yellow);
        content.addArea(mechTypeL1);
        
        fm = comp.getFontMetrics(FONT_VALUE);

        playerL = createLabel(Messages.getString("GeneralInfoMapSet.playerL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(playerL);
        
        playerR = createLabel(Messages.getString("GeneralInfoMapSet.playerR"), fm, playerL.getSize().width + 10, getYCoord()); //$NON-NLS-1$
        content.addArea(playerR);
        
        teamL = createLabel(Messages.getString("GeneralInfoMapSet.teamL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(teamL);
        
        teamR = createLabel(Messages.getString("GeneralInfoMapSet.teamR"), fm, teamL.getSize().width + 10, getYCoord()); //$NON-NLS-1$
        content.addArea(teamR);
        
                
        statusL = createLabel(Messages.getString("GeneralInfoMapSet.statusL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(statusL);

        statusR = createLabel(STAR3, fm, statusL.getSize().width + 10, getYCoord());
        content.addArea(statusR);

        weightL = createLabel(Messages.getString("GeneralInfoMapSet.weightL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(weightL);
        
        weightR = createLabel(STAR3, fm, weightL.getSize().width + 10, getYCoord());
        content.addArea(weightR);

        bvL = createLabel( Messages.getString("GeneralInfoMapSet.bvL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea( bvL );

        bvR = createLabel(STAR3, fm, bvL.getSize().width + 10, getYCoord());
        content.addArea( bvR );

        mpL0 = createLabel(Messages.getString("GeneralInfoMapSet.mpL0"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(mpL0);

        mpR0 = createLabel("", fm, mpL0.getSize().width + 10, getYCoord()); //$NON-NLS-1$
        content.addArea(mpR0);

        mpL1 = createLabel(Messages.getString("GeneralInfoMapSet.mpL1"), fm, 0 , getNewYCoord()); //$NON-NLS-1$
        mpL1.moveTo( mpL0.getSize().width - mpL1.getSize().width, getYCoord());
        content.addArea(mpL1);

        mpR1 = createLabel(STAR3, fm, mpL0.getSize().width + 10, getYCoord());
        content.addArea(mpR1);

        mpL2 = createLabel(Messages.getString("GeneralInfoMapSet.mpL2"), fm, 0 , getNewYCoord()); //$NON-NLS-1$
        mpL2.moveTo( mpL0.getSize().width - mpL2.getSize().width, getYCoord());
        content.addArea(mpL2);

        mpR2 = createLabel(STAR3, fm, mpL0.getSize().width + 10, getYCoord());
        content.addArea(mpR2);

        mpL3 = createLabel(Messages.getString("GeneralInfoMapSet.mpL3"), fm, 0 , getNewYCoord()); //$NON-NLS-1$
        mpL3.moveTo( mpL0.getSize().width - mpL3.getSize().width, getYCoord());
        content.addArea(mpL3);

        mpR3 = createLabel(STAR3, fm, mpL0.getSize().width + 10, getYCoord());
        content.addArea(mpR3);

        curMoveL = createLabel(Messages.getString("GeneralInfoMapSet.curMoveL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(curMoveL);
        
        curMoveR = createLabel(STAR3, fm, curMoveL.getSize().width + 10, getYCoord());
        content.addArea(curMoveR);
        
        heatL = createLabel(Messages.getString("GeneralInfoMapSet.heatL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(heatL);
        
        heatR = createLabel(STAR3, fm, heatL.getSize().width + 10, getYCoord());
        content.addArea(heatR);

        movementTypeL = createLabel(Messages.getString("GeneralInfoMapSet.movementTypeL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(movementTypeL);
        movementTypeR = createLabel(STAR3, fm, movementTypeL.getSize().width + 10, getYCoord());
        content.addArea(movementTypeR);

        pilotL = createLabel(Messages.getString("GeneralInfoMapSet.pilotL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(pilotL);
        pilotR = createLabel(STAR3, fm, pilotL.getSize().width + 10, getYCoord());
        content.addArea(pilotR);

        ejectL = createLabel( Messages.getString("GeneralInfoMapSet.ejectL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea( ejectL );
        ejectR = createLabel(STAR3, fm, ejectL.getSize().width + 10, getYCoord());
        content.addArea( ejectR );

        advantagesR = new PMSimpleLabel[24];
        for (int i=0; i < advantagesR.length; i++) {
            advantagesR[i] = createLabel(new Integer(i).toString(), fm, pilotL.getSize().width + 10, getNewYCoord());
            content.addArea(advantagesR[i]);
        };
        //DO NOT PLACE ANY MORE LABELS BELOW HERE.  They will get
        //pushed off the bottom of the screen by the pilot advantage
        //labels.  Why not just allocate the number of pilot advantage
        //labels required instead of a hard 24?  Because we don't have
        //an entity at this point.  Bleh.
    }

     /**
     * updates fields for the unit
     */
    public void setEntity(Entity en) {
        
        String s = en.getShortName();
        mechTypeL1.setVisible(false);
        
        if(s.length() > GUIPreferences.getInstance().getInt("AdvancedMechDisplayWrapLength")){
            mechTypeL1.setColor(Color.yellow);
            int i = s.lastIndexOf(" ", GUIPreferences.getInstance().getInt("AdvancedMechDisplayWrapLength")); //$NON-NLS-1$
            mechTypeL0.setString(s.substring(0,i));
            mechTypeL1.setString(s.substring(i).trim());
            mechTypeL1.setVisible(true);
        } else {
            mechTypeL0.setString(s);
            mechTypeL1.setString(""); //$NON-NLS-1$
        }

        if (!en.isDesignValid()) {
            //If this is the case, we will just overwrite the name-overflow
            // area, since this info is more important.
            mechTypeL1.setColor(Color.red);
            mechTypeL1.setString(Messages.getString("GeneralInfoMapSet.invalidDesign"));
            mechTypeL1.setVisible(true);
        }

        statusR.setString(en.isProne() ? Messages.getString("GeneralInfoMapSet.prone") : Messages.getString("GeneralInfoMapSet.normal")); //$NON-NLS-1$ //$NON-NLS-2$
        playerR.setString(en.getOwner().getName());
        if (en.getOwner().getTeam() == 0) {
            teamL.setVisible(false);
            teamR.setVisible(false);
        } else {
            teamL.setVisible(true);
            teamR.setString(Messages.getString("GeneralInfoMapSet.Team") + en.getOwner().getTeam()); //$NON-NLS-1$
            teamR.setVisible(true);
        }
        weightR.setString(Integer.toString((int)en.getWeight()));
        
        pilotR.setString(en.crew.getDesc() + " (" + en.crew.getGunnery() + "/" + en.crew.getPiloting() + ")" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        ejectR.setString( Messages.getString("GeneralInfoMapSet.NA") ); //$NON-NLS-1$
        if (en instanceof Mech) {
            if (((Mech)en).isAutoEject()) {
                ejectR.setString( Messages.getString("GeneralInfoMapSet.Operational") ); //$NON-NLS-1$
            } else {
                ejectR.setString( Messages.getString("GeneralInfoMapSet.Disabled") ); //$NON-NLS-1$
            }
        }                

        for (int i=0; i < advantagesR.length; i++ ) {
            advantagesR[i].setString(""); //$NON-NLS-1$
        }
        if (en.crew.countAdvantages() > 0) {
            int i=0;
            for (Enumeration advantages = en.crew.getAdvantages(); advantages.hasMoreElements();) {
                IOption option = (IOption)advantages.nextElement();
                if (option.booleanValue()) {
                    advantagesR[i++].setString(option.getDisplayableName());
                };
            };
        };
        
        if (en.mpUsed > 0) {
            mpR0.setString("("+ en.mpUsed + " used)"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            mpR0.setString(""); //$NON-NLS-1$
        }
        mpR1.setString(Integer.toString(en.getWalkMP()));
        mpR2.setString(en.getRunMPasString());
        mpR3.setString(Integer.toString(en.getJumpMPWithTerrain()));

        curMoveR.setString(en.getMovementString(en.moved) + (en.moved == Entity.MOVE_NONE ? "" : " " + en.delta_distance)); //$NON-NLS-1$ //$NON-NLS-2$
        
        int heatCap = en.getHeatCapacity();
        int heatCapWater = en.getHeatCapacityWithWater();
        String heatCapacityStr = Integer.toString(heatCap);
        
        if ( heatCap < heatCapWater ) {
          heatCapacityStr = heatCap + " [" + heatCapWater + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        heatR.setString(Integer.toString(en.heat) + " (" + heatCapacityStr + " "+Messages.getString("GeneralInfoMapSet.capacity")+")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        
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
        bvR.setString(new Integer(en.calculateBattleValue()).toString());
    }
        
    public PMAreasGroup getContentGroup(){
        return content;
    }
    
    public Vector getBackgroundDrawers(){
        return bgDrawers;
    }
    
    private void setBackGround(){
        Image tile = comp.getToolkit().getImage("data/widgets/tile.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile,comp);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_HORIZONTAL | 
                BackGroundDrawer.VALIGN_TOP;
        tile = comp.getToolkit().getImage("data/widgets/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));                
        
            b = BackGroundDrawer.TILING_HORIZONTAL | 
                BackGroundDrawer.VALIGN_BOTTOM;
        tile = comp.getToolkit().getImage("data/widgets/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_VERTICAL | 
                BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage("data/widgets/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_VERTICAL | 
                BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage("data/widgets/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
                
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage("data/widgets/tl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage("data/widgets/bl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage("data/widgets/tr_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage("data/widgets/br_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
         
    }
    
    private PMSimpleLabel createLabel(String s, FontMetrics fm, int x, int y){
        PMSimpleLabel l = new PMSimpleLabel(s, fm, Color.white);
        l.moveTo(x, y);
        return l; 
    }   
    
    
}

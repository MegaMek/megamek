/**
 * MegaMek - Copyright (C) 2003,2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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

package megamek.client.ui.swing.widget;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JComponent;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Aero;
import megamek.common.Configuration;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.GunEmplacement;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.Tank;
import megamek.common.Warship;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;

/**
 * Set of elements to reperesent general unit information in MechDisplay
 */

public class GeneralInfoMapSet implements DisplayMapSet {

    private static String STAR3 = "***"; //$NON-NLS-1$
    private JComponent comp;
    private PMAreasGroup content = new PMAreasGroup();
    private PMSimpleLabel mechTypeL0, mechTypeL1, statusL, pilotL, playerL,
            teamL, weightL, bvL, mpL0, mpL1, mpL2, mpL3, mpL4, curMoveL, heatL,
            movementTypeL, ejectL, elevationL, fuelL, curSensorsL,
            visualRangeL;
    private PMSimpleLabel statusR, pilotR, playerR, teamR, weightR, bvR, mpR0,
            mpR1, mpR2, mpR3, mpR4, curMoveR, heatR, movementTypeR, ejectR,
            elevationR, fuelR, curSensorsR, visualRangeR;
    private PMSimpleLabel[] quirksR;
    private PMSimpleLabel[] partRepsR;
    private Vector<BackGroundDrawer> bgDrawers = new Vector<BackGroundDrawer>();
    private static final Font FONT_VALUE = new Font(
            "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayLargeFontSize")); //$NON-NLS-1$
    private static final Font FONT_TITLE = new Font(
            "SansSerif", Font.ITALIC, GUIPreferences.getInstance().getInt("AdvancedMechDisplayLargeFontSize")); //$NON-NLS-1$
    private int yCoord = 1;

    /**
     * This constructor have to be called anly from addNotify() method
     */
    public GeneralInfoMapSet(JComponent c) {
        comp = c;
        setAreas();
        setBackGround();
    }

    // These two methods are used to vertically position new labels on the
    // display.
    private int getYCoord() {
        return (yCoord * 15) - 5;
    }

    private int getNewYCoord() {
        yCoord++;
        return getYCoord();
    }

    private void setAreas() {
        FontMetrics fm = comp.getFontMetrics(FONT_TITLE);

        mechTypeL0 = createLabel(
                Messages.getString("GeneralInfoMapSet.LocOstLCT"), fm, 0, getYCoord()); //$NON-NLS-1$
        mechTypeL0.setColor(Color.yellow);
        content.addArea(mechTypeL0);

        mechTypeL1 = createLabel(STAR3, fm, 0, getNewYCoord());
        mechTypeL1.setColor(Color.yellow);
        content.addArea(mechTypeL1);

        fm = comp.getFontMetrics(FONT_VALUE);

        pilotL = createLabel(
                Messages.getString("GeneralInfoMapSet.pilotL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(pilotL);
        
        pilotR = createLabel(
                Messages.getString("GeneralInfoMapSet.playerR"), fm, pilotL.getSize().width + 10, getYCoord()); //$NON-NLS-1$
        content.addArea(pilotR);
        
        playerL = createLabel(
                Messages.getString("GeneralInfoMapSet.playerL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(playerL);

        playerR = createLabel(
                Messages.getString("GeneralInfoMapSet.playerR"), fm, playerL.getSize().width + 10, getYCoord()); //$NON-NLS-1$
        content.addArea(playerR);

        teamL = createLabel(
                Messages.getString("GeneralInfoMapSet.teamL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(teamL);

        teamR = createLabel(
                Messages.getString("GeneralInfoMapSet.teamR"), fm, teamL.getSize().width + 10, getYCoord()); //$NON-NLS-1$
        content.addArea(teamR);

        statusL = createLabel(
                Messages.getString("GeneralInfoMapSet.statusL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(statusL);

        statusR = createLabel(STAR3, fm, statusL.getSize().width + 10,
                getYCoord());
        content.addArea(statusR);

        weightL = createLabel(
                Messages.getString("GeneralInfoMapSet.weightL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(weightL);

        weightR = createLabel(STAR3, fm, weightL.getSize().width + 10,
                getYCoord());
        content.addArea(weightR);

        bvL = createLabel(
                Messages.getString("GeneralInfoMapSet.bvL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(bvL);

        bvR = createLabel(STAR3, fm, bvL.getSize().width + 10, getYCoord());
        content.addArea(bvR);

        mpL0 = createLabel(
                Messages.getString("GeneralInfoMapSet.mpL0"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(mpL0);

        mpR0 = createLabel("", fm, mpL0.getSize().width + 10, getYCoord()); //$NON-NLS-1$
        content.addArea(mpR0);

        mpL1 = createLabel(
                Messages.getString("GeneralInfoMapSet.mpL1"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        mpL1.moveTo(mpL0.getSize().width - mpL1.getSize().width, getYCoord());
        content.addArea(mpL1);

        mpR1 = createLabel(STAR3, fm, mpL0.getSize().width + 10, getYCoord());
        content.addArea(mpR1);

        mpL2 = createLabel(
                Messages.getString("GeneralInfoMapSet.mpL2"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        mpL2.moveTo(mpL0.getSize().width - mpL2.getSize().width, getYCoord());
        content.addArea(mpL2);

        mpR2 = createLabel(STAR3, fm, mpL0.getSize().width + 10, getYCoord());
        content.addArea(mpR2);

        mpL3 = createLabel(
                Messages.getString("GeneralInfoMapSet.mpL3"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        mpL3.moveTo(mpL0.getSize().width - mpL3.getSize().width, getYCoord());
        content.addArea(mpL3);

        mpR3 = createLabel(STAR3, fm, mpL0.getSize().width + 10, getYCoord());
        content.addArea(mpR3);

        mpL4 = createLabel(
                Messages.getString("GeneralInfoMapSet.mpL4"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        mpL4.moveTo(mpL0.getSize().width - mpL3.getSize().width, getYCoord());
        content.addArea(mpL4);

        mpR4 = createLabel("", fm, mpL0.getSize().width + 10, getYCoord());
        content.addArea(mpR4);

        curMoveL = createLabel(
                Messages.getString("GeneralInfoMapSet.curMoveL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(curMoveL);

        curMoveR = createLabel(STAR3, fm, curMoveL.getSize().width + 10,
                getYCoord());
        content.addArea(curMoveR);

        heatL = createLabel(
                Messages.getString("GeneralInfoMapSet.heatL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(heatL);

        heatR = createLabel(STAR3, fm, heatL.getSize().width + 10, getYCoord());
        content.addArea(heatR);

        fuelL = createLabel(
                Messages.getString("GeneralInfoMapSet.fuelL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(fuelL);
        fuelR = createLabel(STAR3, fm, fuelL.getSize().width + 10, getYCoord());
        content.addArea(fuelR);

        movementTypeL = createLabel(
                Messages.getString("GeneralInfoMapSet.movementTypeL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(movementTypeL);
        movementTypeR = createLabel(STAR3, fm,
                movementTypeL.getSize().width + 10, getYCoord());
        content.addArea(movementTypeR);

        ejectL = createLabel(
                Messages.getString("GeneralInfoMapSet.ejectL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(ejectL);
        ejectR = createLabel(STAR3, fm, ejectL.getSize().width + 10,
                getYCoord());
        content.addArea(ejectR);

        elevationL = createLabel(
                Messages.getString("GeneralInfoMapSet.elevationL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(elevationL);
        elevationR = createLabel(STAR3, fm, elevationL.getSize().width + 10,
                getYCoord());
        content.addArea(elevationR);

        curSensorsL = createLabel(
                Messages.getString("GeneralInfoMapSet.currentSensorsL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(curSensorsL);
        curSensorsR = createLabel(STAR3, fm, curSensorsL.getSize().width + 10,
                getYCoord());
        content.addArea(curSensorsR);

        visualRangeL = createLabel(
                Messages.getString("GeneralInfoMapSet.visualRangeL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(visualRangeL);
        visualRangeR = createLabel(STAR3, fm,
                visualRangeL.getSize().width + 10, getYCoord());
        content.addArea(visualRangeR);

        quirksR = new PMSimpleLabel[40];
        for (int i = 0; i < quirksR.length; i++) {
            quirksR[i] = createLabel(new Integer(i).toString(), fm, 0,
                    getNewYCoord());
            content.addArea(quirksR[i]);
        }

        partRepsR = new PMSimpleLabel[20];
        for (int i = 0; i < partRepsR.length; i++) {
            partRepsR[i] = createLabel(new Integer(i).toString(), fm, 0,
                    getNewYCoord());
            content.addArea(partRepsR[i]);
        }

    }

    /**
     * updates fields for the unit
     */
    public void setEntity(Entity en) {

        String s = en.getShortName();
        mechTypeL1.setVisible(false);

        if (s.length() > GUIPreferences.getInstance().getInt(
                "AdvancedMechDisplayWrapLength")) {
            mechTypeL1.setColor(Color.yellow);
            int i = s
                    .lastIndexOf(
                            " ", GUIPreferences.getInstance().getInt("AdvancedMechDisplayWrapLength")); //$NON-NLS-1$
            mechTypeL0.setString(s.substring(0, i));
            mechTypeL1.setString(s.substring(i).trim());
            mechTypeL1.setVisible(true);
        } else {
            mechTypeL0.setString(s);
            mechTypeL1.setString(""); //$NON-NLS-1$
        }

        if (!en.isDesignValid()) {
            // If this is the case, we will just overwrite the name-overflow
            // area, since this info is more important.
            mechTypeL1.setColor(Color.red);
            mechTypeL1.setString(Messages
                    .getString("GeneralInfoMapSet.invalidDesign"));
            mechTypeL1.setVisible(true);
        }

        statusR.setString(en.isProne() ? Messages
                .getString("GeneralInfoMapSet.prone") : Messages.getString("GeneralInfoMapSet.normal")); //$NON-NLS-1$ //$NON-NLS-2$
        if (en.getOwner() != null) {
            playerR.setString(en.getOwner().getName());
            if (en.getOwner().getTeam() == 0) {
                teamL.setVisible(false);
                teamR.setVisible(false);
            } else {
                teamL.setVisible(true);
                teamR.setString(Messages.getString("GeneralInfoMapSet.Team") + en.getOwner().getTeam()); //$NON-NLS-1$
                teamR.setVisible(true);
            }
        }
        
        if (en.getCrew() != null) {
            Crew c = en.getCrew();
            String pilotString = c.getDesc() + " (";
            pilotString += c.getGunnery() + "/" + c.getPiloting();
            int crewAdvCount = c.countOptions(PilotOptions.LVL3_ADVANTAGES);
            if (crewAdvCount > 0) {
                pilotString += ", +" + crewAdvCount;
            }
            pilotString += ")";
            pilotR.setString(pilotString);
        } else {
            pilotR.setString("");
        }

        if (en instanceof Infantry) {
            weightR.setString(Double.toString(en.getWeight()));
        } else {
            weightR.setString(Integer.toString((int) en.getWeight()));
        }

        ejectR.setString(Messages.getString("GeneralInfoMapSet.NA")); //$NON-NLS-1$
        if (en instanceof Mech) {
            if (((Mech) en).isAutoEject()) {
                ejectR.setString(Messages
                        .getString("GeneralInfoMapSet.Operational")); //$NON-NLS-1$
            } else {
                ejectR.setString(Messages
                        .getString("GeneralInfoMapSet.Disabled")); //$NON-NLS-1$
            }
        }
        elevationR.setString(Integer.toString(en.getElevation()));
        if (en.isAirborne()) {
            elevationL.setString(Messages
                    .getString("GeneralInfoMapSet.altitudeL"));
            elevationR.setString(Integer.toString(en.getAltitude()));
        } else {
            elevationL.setString(Messages
                    .getString("GeneralInfoMapSet.elevationL"));
        }

        for (PMSimpleLabel element : quirksR) {
            element.setString(""); //$NON-NLS-1$
        }
        for (PMSimpleLabel element : partRepsR) {
            element.setString(""); //$NON-NLS-1$
        }

        int i = 0;
        if ((null != en.getGame())
                && en.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            for (Enumeration<IOptionGroup> qGroups = en.getQuirks().getGroups(); qGroups
                    .hasMoreElements();) {
                IOptionGroup qGroup = qGroups.nextElement();
                if (en.countQuirks(qGroup.getKey()) > 0) {
                    quirksR[i++].setString(qGroup.getDisplayableName());
                    for (Enumeration<IOption> quirks = qGroup.getOptions(); quirks
                            .hasMoreElements();) {
                        IOption quirk = quirks.nextElement();
                        if (quirk.booleanValue()) {
                            quirksR[i++].setString("  "
                                    + quirk.getDisplayableNameWithValue());
                        }
                    }
                }
            }
        }
        for (Enumeration<IOptionGroup> repGroups = en.getPartialRepairs()
                .getGroups(); repGroups.hasMoreElements();) {
            IOptionGroup repGroup = repGroups.nextElement();
            if (en.countPartialRepairs() > 0) {
                partRepsR[i++].setString(repGroup.getDisplayableName());
                for (Enumeration<IOption> partreps = repGroup.getOptions(); partreps
                        .hasMoreElements();) {
                    IOption partrep = partreps.nextElement();
                    if (partrep.booleanValue()) {
                        partRepsR[i++].setString("  "
                                + partrep.getDisplayableNameWithValue());
                    }
                }
            }
        }

        if (en.mpUsed > 0) {
            mpR0.setString("(" + en.mpUsed + " used)"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            mpR0.setString(""); //$NON-NLS-1$
        }
        mpR1.setString(Integer.toString(en.getWalkMP()));
        mpR2.setString(en.getRunMPasString());

        if ((en instanceof Jumpship) && !(en instanceof Warship)) {
            mpR2.setString(en.getRunMPasString() + " ("
                    + Double.toString(((Jumpship) en).getAccumulatedThrust())
                    + ")");
        }

        mpR3.setString(Integer.toString(en.getJumpMPWithTerrain()));

        if (en.hasUMU()) {
            mpL4.setVisible(true);
            mpR4.setVisible(true);
            mpR4.setString(Integer.toString(en.getActiveUMUCount()));
        } else {
            mpL4.setVisible(false);
            mpR4.setVisible(false);
        }

        if (en instanceof Aero) {
            Aero a = (Aero) en;
            curMoveR.setString(Integer.toString(a.getCurrentVelocity())
                    + Messages.getString("GeneralInfoMapSet.velocity"));
            int currentFuel = a.getFuel();
            int safeThrust = a.getWalkMP();
            fuelR.setString(Integer.toString(a.getFuel()));
            if (currentFuel < (5 * safeThrust)) {
                fuelR.setColor(Color.red);
            } else if (currentFuel < (10 * safeThrust)) {
                fuelR.setColor(Color.yellow);
            } else {
                fuelR.setColor(Color.white);
            }
        } else {
            curMoveR.setString(en.getMovementString(en.moved)
                    + (en.moved == EntityMovementType.MOVE_NONE ? "" : " " + en.delta_distance)); //$NON-NLS-1$ //$NON-NLS-2$
        }

        int heatCap = en.getHeatCapacity();
        int heatCapWater = en.getHeatCapacityWithWater();
        if(en.getCoolantFailureAmount() > 0) {
            heatCap -= en.getCoolantFailureAmount();
            heatCapWater -= en.getCoolantFailureAmount();
        }
        String heatCapacityStr = Integer.toString(heatCap);

        if (heatCap < heatCapWater) {
            heatCapacityStr = heatCap + " [" + heatCapWater + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        }

        heatR.setString(Integer.toString(en.heat)
                + " (" + heatCapacityStr + " " + Messages.getString("GeneralInfoMapSet.capacity") + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        if (en instanceof Mech) {
            heatL.setVisible(true);
            heatR.setVisible(true);
        } else {
            heatL.setVisible(false);
            heatR.setVisible(false);
        }

        if (en instanceof Tank) {
            movementTypeL.setVisible(true);
            movementTypeR.setString(Messages.getString("MovementType."
                    + en.getMovementModeAsString()));
            movementTypeR.setVisible(true);
        } else {
            movementTypeL.setVisible(false);
            movementTypeR.setVisible(false);
        }

        if ((en.getGame() != null) && en.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
            curSensorsR.setVisible(true);
            visualRangeR.setVisible(true);
            curSensorsL.setVisible(true);
            visualRangeL.setVisible(true);
            curSensorsR.setString(en.getSensorDesc());
            visualRangeR.setString(Integer.toString(en.getGame()
                    .getPlanetaryConditions().getVisualRange(en, false)));
        } else {
            curSensorsR.setVisible(false);
            visualRangeR.setVisible(false);
            curSensorsR.setVisible(false);
            visualRangeR.setVisible(false);
        }

        if (en instanceof GunEmplacement) {
            weightL.setVisible(false);
            weightR.setVisible(false);
            mpL0.setVisible(false);
            mpR0.setVisible(false);
            mpL1.setVisible(false);
            mpR1.setVisible(false);
            mpL2.setVisible(false);
            mpR2.setVisible(false);
            mpL3.setVisible(false);
            mpR3.setVisible(false);
            curMoveL.setVisible(false);
            curMoveR.setVisible(false);
        } else {
            weightL.setVisible(true);
            weightR.setVisible(true);
            mpL0.setVisible(true);
            mpR0.setVisible(true);
            mpL1.setVisible(true);
            mpR1.setVisible(true);
            mpL2.setVisible(true);
            mpR2.setVisible(true);
            mpL3.setVisible(true);
            mpR3.setVisible(true);
            curMoveL.setVisible(true);
            curMoveR.setVisible(true);
        }

        if (en instanceof Aero) {
            heatL.setVisible(true);
            heatR.setVisible(true);
            mpR3.setVisible(false);
            mpL3.setVisible(false);
            curMoveL.setVisible(true);
            curMoveR.setVisible(true);
            fuelL.setVisible(true);
            fuelR.setVisible(true);
            mpL0.setString(Messages.getString("GeneralInfoMapSet.thrust"));
            mpL1.setString(Messages.getString("GeneralInfoMapSet.safe"));
            mpL2.setString(Messages.getString("GeneralInfoMapSet.over"));
        } else {
            mpL0.setString(Messages.getString("GeneralInfoMapSet.mpL0"));
            mpL1.setString(Messages.getString("GeneralInfoMapSet.mpL1"));
            mpL2.setString(Messages.getString("GeneralInfoMapSet.mpL2"));
            fuelL.setVisible(false);
            fuelR.setVisible(false);
        }
        if ((en.getGame() != null) && en.getGame().getBoard().inSpace()) {
            elevationL.setVisible(false);
            elevationR.setVisible(false);
        }
        bvL.setVisible(true);
        bvR.setVisible(true);
        bvR.setString(Integer.toString(en.calculateBattleValue()));

    }

    public PMAreasGroup getContentGroup() {
        return content;
    }

    public Vector<BackGroundDrawer> getBackgroundDrawers() {
        return bgDrawers;
    }

    private void setBackGround() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler
                .getUnitDisplaySkin();

        Image tile = comp.getToolkit().getImage(new File(Configuration.widgetsDir(), udSpec.getBackgroundTile()).toString()); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_TOP;
        tile = comp.getToolkit().getImage(new File(Configuration.widgetsDir(), udSpec.getTopLine()).toString()); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_BOTTOM;
        tile = comp.getToolkit().getImage(new File(Configuration.widgetsDir(), udSpec.getBottomLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(new File(Configuration.widgetsDir(), udSpec.getLeftLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(new File(Configuration.widgetsDir(), udSpec.getRightLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(new File(Configuration.widgetsDir(), udSpec.getTopLeftCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(new File(Configuration.widgetsDir(), udSpec.getBottomLeftCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(new File(Configuration.widgetsDir(), udSpec.getTopRightCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(new File(Configuration.widgetsDir(), udSpec.getBottomRightCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

    }

    private PMSimpleLabel createLabel(String s, FontMetrics fm, int x, int y) {
        PMSimpleLabel l = new PMSimpleLabel(s, fm, Color.white);
        l.moveTo(x, y);
        return l;
    }

}

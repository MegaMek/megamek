/*
* Copyright (c) 2003-2004 - Ben Mazur (bmazur@sev.org).
* Copyright (c) 2013 - Edward Cullen (eddy@obsessedcomputers.co.uk).
* Copyright (c) 2018-2022 - The MegaMek Team. All Rights Reserved.
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.client.ui.swing.widget;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.*;
import megamek.common.options.*;
import megamek.common.util.fileUtils.MegaMekFile;

import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Set of elements to represent general unit information in MechDisplay
 */
public class GeneralInfoMapSet implements DisplayMapSet {

    private static String STAR3 = "***";
    private JComponent comp;
    private PMAreasGroup content = new PMAreasGroup();
    private PMSimpleLabel mechTypeL0, statusL, pilotL, playerL,
            teamL, weightL, bvL, mpL0, mpL1, mpL2, mpL3, mpL4, curMoveL, heatL,
            movementTypeL, ejectL, elevationL, fuelL, curSensorsL,
            visualRangeL;
    private PMSimpleLabel statusR, pilotR, playerR, teamR, weightR, bvR, mpR0,
            mpR1, mpR2, mpR3, mpR4, curMoveR, heatR, movementTypeR, ejectR,
            elevationR, fuelR, curSensorsR, visualRangeR;
    private PMMultiLineLabel quirksAndPartReps;
    private Vector<BackGroundDrawer> bgDrawers = new Vector<>();
    private static final Font FONT_VALUE = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
            GUIPreferences.getInstance().getInt("AdvancedMechDisplayLargeFontSize"));
    private static final Font FONT_TITLE = new Font(MMConstants.FONT_SANS_SERIF, Font.ITALIC,
            GUIPreferences.getInstance().getInt("AdvancedMechDisplayLargeFontSize"));
    private int yCoord = 1;

    /**
     * This constructor can only be called from the addNotify method
     */
    public GeneralInfoMapSet(JComponent c) {
        comp = c;
        setAreas();
        setBackGround();
    }

    // These two methods are used to vertically position new labels on the display.
    private int getYCoord() {
        return (yCoord * 15) - 5;
    }

    private int getNewYCoord() {
        yCoord++;
        return getYCoord();
    }

    private void setAreas() {
        FontMetrics fm = comp.getFontMetrics(FONT_TITLE);

        mechTypeL0 = createLabel(Messages.getString("GeneralInfoMapSet.LocOstLCT"), fm, 0, getYCoord());
        mechTypeL0.setVisible(false);
        mechTypeL0.setColor(Color.RED);
        content.addArea(mechTypeL0);

        fm = comp.getFontMetrics(FONT_VALUE);

        pilotL = createLabel(Messages.getString("GeneralInfoMapSet.pilotL"), fm, 0, getNewYCoord());
        content.addArea(pilotL);

        pilotR = createLabel(Messages.getString("GeneralInfoMapSet.playerR"), fm,
                pilotL.getSize().width + 10, getYCoord());
        content.addArea(pilotR);

        playerL = createLabel(Messages.getString("GeneralInfoMapSet.playerL"), fm, 0, getNewYCoord());
        content.addArea(playerL);

        playerR = createLabel(Messages.getString("GeneralInfoMapSet.playerR"), fm,
                playerL.getSize().width + 10, getYCoord());
        content.addArea(playerR);

        teamL = createLabel(Messages.getString("GeneralInfoMapSet.teamL"), fm, 0, getNewYCoord());
        content.addArea(teamL);

        teamR = createLabel(Messages.getString("GeneralInfoMapSet.teamR"), fm,
                teamL.getSize().width + 10, getYCoord());
        content.addArea(teamR);

        statusL = createLabel(Messages.getString("GeneralInfoMapSet.statusL"), fm, 0, getNewYCoord());
        content.addArea(statusL);

        statusR = createLabel(STAR3, fm, statusL.getSize().width + 10, getYCoord());
        content.addArea(statusR);

        weightL = createLabel(Messages.getString("GeneralInfoMapSet.weightL"), fm, 0, getNewYCoord());
        content.addArea(weightL);

        weightR = createLabel(STAR3, fm, weightL.getSize().width + 10, getYCoord());
        content.addArea(weightR);

        bvL = createLabel(Messages.getString("GeneralInfoMapSet.bvL"), fm, 0, getNewYCoord());
        content.addArea(bvL);

        bvR = createLabel(STAR3, fm, bvL.getSize().width + 10, getYCoord());
        content.addArea(bvR);

        mpL0 = createLabel(Messages.getString("GeneralInfoMapSet.mpL0"), fm, 0, getNewYCoord());
        content.addArea(mpL0);

        mpR0 = createLabel("", fm, mpL0.getSize().width + 10, getYCoord());
        content.addArea(mpR0);

        mpL1 = createLabel(Messages.getString("GeneralInfoMapSet.mpL1"), fm, 0, getNewYCoord());
        mpL1.moveTo(mpL0.getSize().width - mpL1.getSize().width, getYCoord());
        content.addArea(mpL1);

        mpR1 = createLabel(STAR3, fm, mpL0.getSize().width + 10, getYCoord());
        content.addArea(mpR1);

        mpL2 = createLabel(Messages.getString("GeneralInfoMapSet.mpL2"), fm, 0, getNewYCoord());
        mpL2.moveTo(mpL0.getSize().width - mpL2.getSize().width, getYCoord());
        content.addArea(mpL2);

        mpR2 = createLabel(STAR3, fm, mpL0.getSize().width + 10, getYCoord());
        content.addArea(mpR2);

        mpL3 = createLabel(Messages.getString("GeneralInfoMapSet.mpL3"), fm, 0, getNewYCoord());
        mpL3.moveTo(mpL0.getSize().width - mpL3.getSize().width, getYCoord());
        content.addArea(mpL3);

        mpR3 = createLabel(STAR3, fm, mpL0.getSize().width + 10, getYCoord());
        content.addArea(mpR3);

        mpL4 = createLabel(Messages.getString("GeneralInfoMapSet.mpL4"), fm, 0, getNewYCoord());
        mpL4.moveTo(mpL0.getSize().width - mpL3.getSize().width, getYCoord());
        content.addArea(mpL4);

        mpR4 = createLabel("", fm, mpL0.getSize().width + 10, getYCoord());
        content.addArea(mpR4);

        curMoveL = createLabel(Messages.getString("GeneralInfoMapSet.curMoveL"), fm, 0, getNewYCoord());
        content.addArea(curMoveL);

        curMoveR = createLabel(STAR3, fm, curMoveL.getSize().width + 10, getYCoord());
        content.addArea(curMoveR);

        heatL = createLabel(Messages.getString("GeneralInfoMapSet.heatL"), fm, 0, getNewYCoord());
        content.addArea(heatL);

        heatR = createLabel(STAR3, fm, heatL.getSize().width + 10, getYCoord());
        content.addArea(heatR);

        fuelL = createLabel(Messages.getString("GeneralInfoMapSet.fuelL"), fm, 0, getNewYCoord());
        content.addArea(fuelL);
        fuelR = createLabel(STAR3, fm, fuelL.getSize().width + 10, getYCoord());
        content.addArea(fuelR);

        movementTypeL = createLabel(Messages.getString("GeneralInfoMapSet.movementTypeL"), fm,
                0, getNewYCoord());
        content.addArea(movementTypeL);
        movementTypeR = createLabel(STAR3, fm, movementTypeL.getSize().width + 10, getYCoord());
        content.addArea(movementTypeR);

        ejectL = createLabel(Messages.getString("GeneralInfoMapSet.ejectL"), fm, 0,
                getNewYCoord());
        content.addArea(ejectL);
        ejectR = createLabel(STAR3, fm, ejectL.getSize().width + 10, getYCoord());
        content.addArea(ejectR);

        elevationL = createLabel(Messages.getString("GeneralInfoMapSet.elevationL"), fm, 0,
                getNewYCoord());
        content.addArea(elevationL);
        elevationR = createLabel(STAR3, fm, elevationL.getSize().width + 10, getYCoord());
        content.addArea(elevationR);

        curSensorsL = createLabel(Messages.getString("GeneralInfoMapSet.currentSensorsL"), fm,
                0, getNewYCoord());
        content.addArea(curSensorsL);
        curSensorsR = createLabel(STAR3, fm, curSensorsL.getSize().width + 10, getYCoord());
        content.addArea(curSensorsR);

        visualRangeL = createLabel(Messages.getString("GeneralInfoMapSet.visualRangeL"), fm,
                0, getNewYCoord());
        content.addArea(visualRangeL);
        visualRangeR = createLabel(STAR3, fm, visualRangeL.getSize().width + 10, getYCoord());
        content.addArea(visualRangeR);

        getNewYCoord(); // skip a line for readability

        quirksAndPartReps = new PMMultiLineLabel(fm, Color.white);
        quirksAndPartReps.moveTo(0, getNewYCoord());
        content.addArea(quirksAndPartReps);
    }

    /**
     * updates fields for the unit
     */
    @Override
    public void setEntity(Entity en) {

        mechTypeL0.setVisible(false);
        if (!en.isDesignValid()) {
            // If this is the case, we will just overwrite the name-overflow
            // area, since this info is more important.
            mechTypeL0.setString(Messages
                    .getString("GeneralInfoMapSet.invalidDesign"));
            mechTypeL0.setVisible(true);
        }

        statusR.setString(en.isProne() ? Messages.getString("GeneralInfoMapSet.prone")
                : Messages.getString("GeneralInfoMapSet.normal"));
        if (en.getOwner() != null) {
            playerR.setString(en.getOwner().getName());
            if (en.getOwner().getTeam() == 0) {
                teamL.setVisible(false);
                teamR.setVisible(false);
            } else {
                teamL.setVisible(true);
                teamR.setString(Messages.getString("GeneralInfoMapSet.Team") + en.getOwner().getTeam());
                teamR.setVisible(true);
            }
        }

        if (en.getCrew() != null) {
            Crew c = en.getCrew();
            String pilotString = c.getDesc(c.getCurrentPilotIndex()) + " (";
            pilotString += c.getSkillsAsString(
                    en.getGame().getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY));
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

        ejectR.setString(Messages.getString("GeneralInfoMapSet.NA"));
        if (en instanceof Mech) {
            if (((Mech) en).isAutoEject()) {
                ejectR.setString(Messages.getString("GeneralInfoMapSet.Operational"));
            } else {
                ejectR.setString(Messages.getString("GeneralInfoMapSet.Disabled"));
            }
        }
        elevationR.setString(Integer.toString(en.getElevation()));
        if (en.isAirborne()) {
            elevationL.setString(Messages.getString("GeneralInfoMapSet.altitudeL"));
            elevationR.setString(Integer.toString(en.getAltitude()));
        } else {
            elevationL.setString(Messages.getString("GeneralInfoMapSet.elevationL"));
        }

        quirksAndPartReps.clear();

        if ((null != en.getGame())
                && en.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            addOptionsToList(en.getQuirks(), quirksAndPartReps);
        }

        if ((null != en.getGame())
                && en.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_PARTIALREPAIRS)) {
            // skip a line for readability
            quirksAndPartReps.addString("");

            addOptionsToList(en.getPartialRepairs(), quirksAndPartReps);
        }

        if (en.mpUsed > 0) {
            mpR0.setString("(" + en.mpUsed + " used)");
        } else {
            mpR0.setString("");
        }
        mpR1.setString(Integer.toString(en.getWalkMP()));
        mpR2.setString(en.getRunMPasString());

        if ((en instanceof Jumpship) && !(en instanceof Warship)) {
            mpR2.setString(en.getRunMPasString() + " (" + ((Jumpship) en).getAccumulatedThrust() + ")");
        }

        mpR3.setString(Integer.toString(en.getJumpMPWithTerrain()));

        if (en.hasUMU()) {
            mpL4.setVisible(true);
            mpR4.setVisible(true);
            mpR4.setString(Integer.toString(en.getActiveUMUCount()));
        } else if (en instanceof LandAirMech
                && en.getMovementMode() == EntityMovementMode.WIGE) {
            mpL4.setVisible(true);
            mpR4.setVisible(true);
            mpR1.setString(Integer.toString(((LandAirMech) en).getAirMechWalkMP()));
            mpR2.setString(Integer.toString(((LandAirMech) en).getAirMechRunMP()));
            mpR3.setString(Integer.toString(((LandAirMech) en).getAirMechCruiseMP()));
            mpR4.setString(Integer.toString(((LandAirMech) en).getAirMechFlankMP()));
        } else {
            mpL4.setVisible(false);
            mpR4.setVisible(false);
        }

        if (en.isAero()) {
            IAero a = (IAero) en;
            curMoveR.setString(a.getCurrentVelocity() + Messages.getString("GeneralInfoMapSet.velocity"));
            int currentFuel = a.getCurrentFuel();
            int safeThrust = en.getWalkMP();
            fuelR.setString(Integer.toString(a.getCurrentFuel()));
            if (currentFuel < (5 * safeThrust)) {
                fuelR.setColor(Color.red);
            } else if (currentFuel < (10 * safeThrust)) {
                fuelR.setColor(Color.yellow);
            } else {
                fuelR.setColor(Color.white);
            }
        } else {
            curMoveR.setString(en.getMovementString(en.moved)
                    + (en.moved == EntityMovementType.MOVE_NONE ? "" : " " + en.delta_distance));
        }

        int heatCap = en.getHeatCapacity();
        int heatCapWater = en.getHeatCapacityWithWater();
        if (en.getCoolantFailureAmount() > 0) {
            heatCap -= en.getCoolantFailureAmount();
            heatCapWater -= en.getCoolantFailureAmount();
        }
        String heatCapacityStr = Integer.toString(heatCap);

        if (heatCap < heatCapWater) {
            heatCapacityStr = heatCap + " [" + heatCapWater + "]";
        }

        heatR.color = GUIPreferences.getInstance().getColorForHeat(en.heat, Color.WHITE);
        heatR.setString(en.heat
                + " (" + heatCapacityStr + " " + Messages.getString("GeneralInfoMapSet.capacity") + ")");

        if (en instanceof Mech) {
            heatL.setVisible(true);
            heatR.setVisible(true);
        } else {
            heatL.setVisible(false);
            heatR.setVisible(false);
        }

        if (en instanceof Tank) {
            movementTypeL.setString(Messages.getString("GeneralInfoMapSet.movementTypeL"));
            movementTypeL.setVisible(true);
            movementTypeR.setString(Messages.getString("MovementType."
                    + en.getMovementModeAsString()));
            movementTypeR.setVisible(true);
        } else if (en instanceof QuadVee || en instanceof LandAirMech
                || (en instanceof Mech && ((Mech) en).hasTracks())) {
            movementTypeL.setString(Messages.getString("GeneralInfoMapSet.movementModeL"));
            if (en.getMovementMode() == EntityMovementMode.AERODYNE) {
                // Show "Fighter/AirMech" instead of "Aerodyne/WiGE"
                movementTypeR.setString(Messages.getString("BoardView1.ConversionMode.AERODYNE"));
            } else if (en.getMovementMode() == EntityMovementMode.WIGE) {
                movementTypeR.setString(Messages.getString("BoardView1.ConversionMode.WIGE"));
            } else {
                movementTypeR.setString(Messages.getString("MovementType."
                        + en.getMovementModeAsString()));
            }
            movementTypeL.setVisible(true);
            movementTypeR.setVisible(true);
        } else {
            movementTypeL.setVisible(false);
            movementTypeR.setVisible(false);
        }

        if ((en.getGame() != null) && en.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_SENSORS)) {
            curSensorsR.setVisible(true);
            visualRangeR.setVisible(true);
            curSensorsL.setVisible(true);
            visualRangeL.setVisible(true);
            curSensorsR.setString(en.getSensorDesc());
            visualRangeR.setString(Integer.toString(Compute.getMaxVisualRange(en, false)));
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

        if (en.isAero()) {
            heatL.setVisible(true);
            heatR.setVisible(true);
            curMoveL.setVisible(true);
            curMoveR.setVisible(true);
            fuelL.setVisible(true);
            fuelR.setVisible(true);
            mpL0.setString(Messages.getString("GeneralInfoMapSet.thrust"));
            mpL1.setString(Messages.getString("GeneralInfoMapSet.safe"));
            mpL2.setString(Messages.getString("GeneralInfoMapSet.over"));
            if (en.getMovementMode() == EntityMovementMode.WHEELED) {
                mpR1.setString(Integer.toString(((IAero) en).getCurrentThrust()));
                mpR2.setString(Integer.toString((int) Math.ceil(((IAero) en).getCurrentThrust() * 1.5)));
                mpL3.setString(Messages.getString("GeneralInfoMapSet.vehicle.mpL1"));
                mpR3.setString(Integer.toString(en.getWalkMP()));
                mpR3.setVisible(true);
                mpL3.setVisible(true);
            } else {
                mpR3.setVisible(false);
                mpL3.setVisible(false);
            }
        } else if (en instanceof Tank
                || (en instanceof QuadVee && en.getConversionMode() == QuadVee.CONV_MODE_VEHICLE)) {
            mpL0.setString(Messages.getString("GeneralInfoMapSet.mpL0"));
            mpL1.setString(Messages.getString("GeneralInfoMapSet.vehicle.mpL1"));
            mpL2.setString(Messages.getString("GeneralInfoMapSet.vehicle.mpL2"));
            mpL3.setString(Messages.getString("GeneralInfoMapSet.mpL3"));
            fuelL.setVisible(false);
            fuelR.setVisible(false);
        } else if (en instanceof LandAirMech
                && en.getMovementMode() == EntityMovementMode.WIGE) {
            mpL0.setString(Messages.getString("GeneralInfoMapSet.mpL0"));
            mpL1.setString(Messages.getString("GeneralInfoMapSet.mpL1"));
            mpL2.setString(Messages.getString("GeneralInfoMapSet.mpL2"));
            mpL3.setString(Messages.getString("GeneralInfoMapSet.vehicle.mpL1"));
            mpL4.setString(Messages.getString("GeneralInfoMapSet.vehicle.mpL2"));
            fuelL.setVisible(false);
            fuelR.setVisible(false);
        } else {
            mpL0.setString(Messages.getString("GeneralInfoMapSet.mpL0"));
            mpL1.setString(Messages.getString("GeneralInfoMapSet.mpL1"));
            mpL2.setString(Messages.getString("GeneralInfoMapSet.mpL2"));
            mpL3.setString(Messages.getString("GeneralInfoMapSet.mpL3"));
            fuelL.setVisible(false);
            fuelR.setVisible(false);
            if (en instanceof LandAirMech
                    && en.getMovementMode() == EntityMovementMode.WIGE) {
                mpL3.setString(Messages.getString("GeneralInfoMapSet.vehicle.mpL1"));
                mpL4.setString(Messages.getString("GeneralInfoMapSet.vehicle.mpL2"));
            } else {
                mpL3.setString(Messages.getString("GeneralInfoMapSet.mpL3"));
            }
        }
        if ((en.getGame() != null) && en.getGame().getBoard().inSpace()) {
            elevationL.setVisible(false);
            elevationR.setVisible(false);
        }
        bvL.setVisible(true);
        bvR.setVisible(true);
        bvR.setString(Integer.toString(en.calculateBattleValue()));

    }

    /**
     * Add all options from the given AbstractOptions instance into an array of PMSimpleLabel elements.
     * @param optionsInstance AbstractOptions instance
     * @param quirksAndPartReps
     */
    public void addOptionsToList(AbstractOptions optionsInstance, PMMultiLineLabel quirksAndPartReps) {
        for (Enumeration<IOptionGroup> optionGroups = optionsInstance.getGroups(); optionGroups.hasMoreElements();) {
            IOptionGroup group = optionGroups.nextElement();
            if (optionsInstance.count(group.getKey()) > 0) {
                quirksAndPartReps.addString(group.getDisplayableName());

                for (Enumeration<IOption> options = group.getOptions(); options.hasMoreElements();) {
                    IOption option = options.nextElement();
                    if (option != null && option.booleanValue()) {
                        quirksAndPartReps.addString("  " + option.getDisplayableNameWithValue());
                    }
                }
            }
        }
    }

    @Override
    public PMAreasGroup getContentGroup() {
        return content;
    }

    @Override
    public Vector<BackGroundDrawer> getBackgroundDrawers() {
        return bgDrawers;
    }

    private void setBackGround() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler.getUnitDisplaySkin();

        Image tile = comp.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
                udSpec.getBackgroundTile()).toString());
        PMUtil.setImage(tile, comp);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_TOP;
        tile = comp.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
                udSpec.getTopLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_BOTTOM;
        tile = comp.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
                udSpec.getBottomLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
                udSpec.getLeftLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
                udSpec.getRightLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
                udSpec.getTopLeftCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
                udSpec.getBottomLeftCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
                udSpec.getTopRightCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
                udSpec.getBottomRightCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

    }

    private PMSimpleLabel createLabel(String s, FontMetrics fm, int x, int y) {
        PMSimpleLabel l = new PMSimpleLabel(s, fm, Color.white);
        l.moveTo(x, y);
        return l;
    }
}

/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2006 Ben Mazur (bmazur@sev.org)
 * Copyright © 2015 Nicholas Walczak (walczak@cs.umn.edu)
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

/**
 * A class that contains state information that specifies a skin for the
 * UnitDisplay.
 * 
 * @author arlith
 *
 */
public class UnitDisplaySkinSpecification {

    private String generalTabIdle = "tab_general_idle.gif"; //$NON-NLS-1$
    private String pilotTabIdle = "tab_pilot_idle.gif"; //$NON-NLS-1$
    private String armorTabIdle = "tab_armor_idle.gif"; //$NON-NLS-1$
    private String systemsTabIdle = "tab_systems_idle.gif"; //$NON-NLS-1$
    private String weaponsTabIdle = "tab_weapon_idle.gif"; //$NON-NLS-1$
    private String extrasTabIdle = "tab_extras_idle.gif"; //$NON-NLS-1$
    private String generalTabActive = "tab_general_active.gif"; //$NON-NLS-1$
    private String pilotTabActive = "tab_pilot_active.gif"; //$NON-NLS-1$
    private String armorTabActive = "tab_armor_active.gif"; //$NON-NLS-1$
    private String systemsTabActive = "tab_systems_active.gif"; //$NON-NLS-1$
    private String weaponsTabActive = "tab_weapon_active.gif"; //$NON-NLS-1$
    private String extraTabActive = "tab_extras_active.gif"; //$NON-NLS-1$
    private String cornerIdle = "idle_corner.gif"; //$NON-NLS-1$
    private String cornerActive = "active_corner.gif"; //$NON-NLS-1$

    private String backgroundTile = "tile.gif"; //$NON-NLS-1$
    private String topLine = "h_line.gif"; //$NON-NLS-1$
    private String bottomLine = "h_line.gif"; //$NON-NLS-1$
    private String leftLine = "v_line.gif"; //$NON-NLS-1$
    private String rightLine = "v_line.gif"; //$NON-NLS-1$
    private String topLeftCorner = "tl_corner.gif"; //$NON-NLS-1$
    private String bottomLeftCorner = "bl_corner.gif"; //$NON-NLS-1$
    private String topRightCorner = "tr_corner.gif"; //$NON-NLS-1$
    private String bottomRightCorner = "br_corner.gif"; //$NON-NLS-1$

    private String mechOutline = "bg_mech.gif"; //$NON-NLS-1$

    public String getGeneralTabIdle() {
        return generalTabIdle;
    }

    public void setGeneralTabIdle(String generalTabIdle) {
        this.generalTabIdle = generalTabIdle;
    }

    public String getPilotTabIdle() {
        return pilotTabIdle;
    }

    public void setPilotTabIdle(String pilotTabIdle) {
        this.pilotTabIdle = pilotTabIdle;
    }

    public String getArmorTabIdle() {
        return armorTabIdle;
    }

    public void setArmorTabIdle(String armorTabIdle) {
        this.armorTabIdle = armorTabIdle;
    }

    public String getSystemsTabIdle() {
        return systemsTabIdle;
    }

    public void setSystemsTabIdle(String systemsTabIdle) {
        this.systemsTabIdle = systemsTabIdle;
    }

    public String getWeaponsTabIdle() {
        return weaponsTabIdle;
    }

    public void setWeaponsTabIdle(String weaponsTabIdle) {
        this.weaponsTabIdle = weaponsTabIdle;
    }

    public String getExtrasTabIdle() {
        return extrasTabIdle;
    }

    public void setExtrasTabIdle(String extrasTabIdle) {
        this.extrasTabIdle = extrasTabIdle;
    }

    public String getGeneralTabActive() {
        return generalTabActive;
    }

    public void setGeneralTabActive(String generalTabActive) {
        this.generalTabActive = generalTabActive;
    }

    public String getPilotTabActive() {
        return pilotTabActive;
    }

    public void setPilotTabActive(String generaTabActive) {
        this.pilotTabActive = generaTabActive;
    }

    public String getArmorTabActive() {
        return armorTabActive;
    }

    public void setArmorTabActive(String armorTabActive) {
        this.armorTabActive = armorTabActive;
    }

    public String getSystemsTabActive() {
        return systemsTabActive;
    }

    public void setSystemsTabActive(String systemsTabActive) {
        this.systemsTabActive = systemsTabActive;
    }

    public String getWeaponsTabActive() {
        return weaponsTabActive;
    }

    public void setWeaponsTabActive(String weaponsTabActive) {
        this.weaponsTabActive = weaponsTabActive;
    }

    public String getExtraTabActive() {
        return extraTabActive;
    }

    public void setExtraTabActive(String extraTabActive) {
        this.extraTabActive = extraTabActive;
    }

    public String getCornerIdle() {
        return cornerIdle;
    }

    public void setCornerIdle(String cornerIdle) {
        this.cornerIdle = cornerIdle;
    }

    public String getCornerActive() {
        return cornerActive;
    }

    public void setCornerActive(String cornerActive) {
        this.cornerActive = cornerActive;
    }

    public String getBackgroundTile() {
        return backgroundTile;
    }

    public void setBackgroundTile(String backgroundTile) {
        this.backgroundTile = backgroundTile;
    }

    public String getTopLine() {
        return topLine;
    }

    public void setTopLine(String topLine) {
        this.topLine = topLine;
    }

    public String getBottomLine() {
        return bottomLine;
    }

    public void setBottomLine(String bottomLine) {
        this.bottomLine = bottomLine;
    }

    public String getLeftLine() {
        return leftLine;
    }

    public void setLeftLine(String leftLine) {
        this.leftLine = leftLine;
    }

    public String getRightLine() {
        return rightLine;
    }

    public void setRightLine(String rightLine) {
        this.rightLine = rightLine;
    }

    public String getTopLeftCorner() {
        return topLeftCorner;
    }

    public void setTopLeftCorner(String topLeftCorner) {
        this.topLeftCorner = topLeftCorner;
    }

    public String getBottomLeftCorner() {
        return bottomLeftCorner;
    }

    public void setBottomLeftCorner(String bottomLeftCorner) {
        this.bottomLeftCorner = bottomLeftCorner;
    }

    public String getTopRightCorner() {
        return topRightCorner;
    }

    public void setTopRightCorner(String topRightCorner) {
        this.topRightCorner = topRightCorner;
    }

    public String getBottomRightCorner() {
        return bottomRightCorner;
    }

    public void setBottomRightCorner(String bottomRightCorner) {
        this.bottomRightCorner = bottomRightCorner;
    }

    public String getMechOutline() {
        return mechOutline;
    }

    public void setMechOutline(String mechOutline) {
        this.mechOutline = mechOutline;
    }

}

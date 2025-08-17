/*
 * Copyright (C) 2000-2004, 2006 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2015 Nicholas Walczak (walczak@cs.umn.edu)
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.client.ui.widget;

/**
 * A class that contains state information that specifies a skin for the UnitDisplay.
 *
 * @author arlith
 */
public class UnitDisplaySkinSpecification {

    private String generalTabIdle = "tab_general_idle.gif";
    private String pilotTabIdle = "tab_pilot_idle.gif";
    private String armorTabIdle = "tab_armor_idle.gif";
    private String systemsTabIdle = "tab_systems_idle.gif";
    private String weaponsTabIdle = "tab_weapon_idle.gif";
    private String extrasTabIdle = "tab_extras_idle.gif";
    private String generalTabActive = "tab_general_active.gif";
    private String pilotTabActive = "tab_pilot_active.gif";
    private String armorTabActive = "tab_armor_active.gif";
    private String systemsTabActive = "tab_systems_active.gif";
    private String weaponsTabActive = "tab_weapon_active.gif";
    private String extraTabActive = "tab_extras_active.gif";
    private String cornerIdle = "idle_corner.gif";
    private String cornerActive = "active_corner.gif";

    private String backgroundTile = "tile.gif";
    private String topLine = "h_line.gif";
    private String bottomLine = "h_line.gif";
    private String leftLine = "v_line.gif";
    private String rightLine = "v_line.gif";
    private String topLeftCorner = "tl_corner.gif";
    private String bottomLeftCorner = "bl_corner.gif";
    private String topRightCorner = "tr_corner.gif";
    private String bottomRightCorner = "br_corner.gif";

    private String mekOutline = "bg_mek.gif";

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

    public String getMekOutline() {
        return mekOutline;
    }

    public void setMekOutline(String mekOutline) {
        this.mekOutline = mekOutline;
    }

}

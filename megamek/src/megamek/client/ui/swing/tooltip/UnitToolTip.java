/*  
* MegaMek - Copyright (C) 2020 - The MegaMek Team  
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
package megamek.client.ui.swing.tooltip;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import megamek.common.templates.TROView;
import megamek.common.weapons.LegAttack;
import megamek.common.weapons.StopSwarmAttack;
import megamek.common.weapons.SwarmAttack;
import megamek.common.weapons.SwarmWeaponAttack;
import org.apache.logging.log4j.LogManager;

import java.awt.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static megamek.client.ui.swing.tooltip.TipUtil.*;
import static megamek.client.ui.swing.util.UIUtil.*;

public final class UnitToolTip {
    
    /** The font size reduction for Quirks */
    final static float TT_SMALLFONT_DELTA = -0.2f;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    /** Returns the unit tooltip with values that are relevant in the lobby. */
    public static StringBuilder getEntityTipLobby(Entity entity, Player localPlayer,
            MapSettings mapSettings) {
        return getEntityTipTable(entity, localPlayer, true, false, mapSettings);
    }
    
    /** Returns the unit tooltip with values that are relevant in-game. */
    public static StringBuilder getEntityTipGame(Entity entity, Player localPlayer) {
        return getEntityTipTable(entity, localPlayer, false, true, null);
    }

    /** Returns the unit tooltip with values that are relevant in-game without the Pilot info. */
    public static StringBuilder getEntityTipUnitDisplay(Entity entity, Player localPlayer) {
        return getEntityTipTable(entity, localPlayer, true, false, null);
    }

    // PRIVATE
    
    /** Assembles the whole unit tooltip. */
    private static StringBuilder getEntityTipTable(Entity entity, Player localPlayer,
           boolean details, boolean pilotInfo, @Nullable MapSettings mapSettings) {
        
        // Tooltip info for a sensor blip
        if (EntityVisibilityUtils.onlyDetectedBySensors(localPlayer, entity)) {
            String msg_senorreturn = Messages.getString("BoardView1.sensorReturn");
            return new StringBuilder(msg_senorreturn);
        }

        String result = "";
        String f = "";
        String p = "";
        Game game = entity.getGame();

        // Unit Chassis and Player
        Player owner = game.getPlayer(entity.getOwnerId());
        String msg_clanbrackets =Messages.getString("BoardView1.Tooltip.ClanBrackets");
        String clanStr = entity.isClan() && !entity.isMixedTech() ? " " + msg_clanbrackets + " " : "";
        f = entity.getChassis() + clanStr;
        f += " (" + (int) entity.getWeight() + "t)";
        f += "&nbsp;&nbsp;" + entity.getEntityTypeName(entity.getEntityType());
        f += "<BR>" + owner.getName();
        String msg_id = MessageFormat.format(" [ID: {0}]", entity.getId());
        f += UIUtil.guiScaledFontHTML(UIUtil.uiGray()) + msg_id + "</FONT>";
        p += guiScaledFontHTML(entity.getOwner().getColour().getColour()) + f +  "</FONT>";

        // Pilot; in the lounge the pilot is separate so don't add it there
        if (details && (mapSettings != null)) {
            p += deploymentWarnings(entity, localPlayer, mapSettings) + "<BR>";
        } else {
            if (pilotInfo) {
                p += forceEntry(entity, localPlayer);
            }
            p += inGameValues(entity, localPlayer);
            if (pilotInfo) {
                p += PilotToolTip.getPilotTipShort(entity, GUIP.getBoolean(GUIPreferences.SHOW_PILOT_PORTRAIT_TT));
            } else {
                p += "<BR>";
            }
        }

        result += p;
        p = "";

        // An empty squadron should not show any info
        if (entity instanceof FighterSquadron && entity.getLoadedUnits().isEmpty()) {
            String col = "<TD>" + result + "</TD>";
            String row = "<TR>" + col + "</TR>";
            String table = "<TABLE BGCOLOR=#313131 width=100%>" + row + "</TABLE>";
            return new StringBuilder().append(table);
        }

        // Static entity values like move capability
        f = entityValues(entity).toString();
        result += guiScaledFontHTML() + f + "</FONT>";

        // Status bar visual representation of armor and IS 
        if (GUIP.getBoolean(GUIPreferences.SHOW_ARMOR_MINIVIS_TT)) {
            result += addArmorMiniVisToTT(entity);
        }

        // Weapon List
        if (GUIP.getBoolean(GUIPreferences.SHOW_WPS_IN_TT)) {
            f = weaponList(entity).toString();
            f += ecmInfo(entity).toString();
            result += guiScaledFontHTML() + f + "</FONT>";
        }

        // Bomb List
        result += bombList(entity);

        // StratOps quirks, chassis and weapon
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            f = "<BR>";
            String quirksList = getOptionList(entity.getQuirks().getGroups(), entity::countQuirks, details);
            if (!quirksList.isEmpty()) {
                f += quirksList;
            }
            for (Mounted weapon: entity.getWeaponList()) {
                String wpQuirksList = getOptionList(weapon.getQuirks().getGroups(), 
                        grp -> weapon.countQuirks(), (e) -> weapon.getDesc(), details);
                if (!wpQuirksList.isEmpty()) {
                    // Line break after weapon name not useful here
                    f += wpQuirksList.replace(":</I><BR>", ":</I>");
                }
            }
            result += guiScaledFontHTML(uiQuirksColor(), TT_SMALLFONT_DELTA) + f + "</FONT>";
        }

        // Partial repairs
        String partialList = getOptionList(entity.getPartialRepairs().getGroups(), 
                grp -> entity.countPartialRepairs(), details);
        if (!partialList.isEmpty()) {
            f = partialList;
            result += guiScaledFontHTML(uiPartialRepairColor(), TT_SMALLFONT_DELTA) + f + "</FONT>";
        }
        
        if (!entity.getLoadedUnits().isEmpty()) {
            result += carriedUnits(entity);
        }
        
        if (details && entity.hasAnyC3System()) {
            result += c3Info(entity);
        }

        String col = "<TD>" + result + "</TD>";
        String row = "<TR>" + col + "</TR>";
        String table = "<TABLE BGCOLOR=#313131 width=100%>" + row + "</TABLE>";

        return new StringBuilder().append(table);
    }

    private static boolean hideArmorLocation(Entity entity, int location) {
        return ((entity.getOArmor(location) <= 0) && (entity.getOInternal(location) <= 0) && !entity.hasRearArmor(location))
                || (entity.isConventionalInfantry() && (location != Infantry.LOC_INFANTRY));
    }

    private static String locationHeader(Entity entity, int location) {
        String msg_activetroopers =Messages.getString("BoardView1.Tooltip.ActiveTroopers");
        return entity.isConventionalInfantry() ? ((Infantry) entity).getShootingStrength() + " " + msg_activetroopers : entity.getLocationAbbr(location);
    }

    private static StringBuilder sysCrits(Entity entity, int type, int index, int loc, String l) {
        String result = "";
        String f = "";
        int total = entity.getNumberOfCriticals(type, index, loc);
        int hits = entity.getHitCriticals(type, index, loc);
        int good = total - hits;
        boolean bad = (entity.getBadCriticals(type,index, loc) > 0);

        if ((good + hits) > 0) {
            f = "&nbsp;&nbsp;" + l + ":&nbsp;";
            result = guiScaledFontHTML(TT_SMALLFONT_DELTA) + f + "</FONT>";
            result += systemBar(good, hits, bad);
        }

        return new StringBuilder().append(result);
    }

    /** Returns the graphical Armor representation. */
    private static StringBuilder addArmorMiniVisToTT(Entity entity) {
        String armorChar = GUIP.getString(GUIPreferences.ADVANCED_ARMORMINI_ARMOR_CHAR);
        if (entity.isCapitalScale()) {
            armorChar = GUIP.getString(GUIPreferences.ADVANCED_ARMORMINI_CAP_ARMOR_CHAR);
        }
        String internalChar = GUIP.getString(GUIPreferences.ADVANCED_ARMORMINI_IS_CHAR);
        String f = "";
        String col1 = "";
        String col2 = "";
        String col3 = "";
        String row = "";
        String rows = "";

        for (int loc = 0 ; loc < entity.locations(); loc++) {
            // do not show locations that do not support/have armor/internals like HULL on Aero
            if (hideArmorLocation(entity, loc)) {
                continue;
            }

            boolean locDestroyed = (entity.getInternal(loc) == IArmorState.ARMOR_DOOMED || entity.getInternal(loc) == IArmorState.ARMOR_DESTROYED);

            if (locDestroyed) {
                // Destroyed location
                col1 = "";
                f = "&nbsp;&nbsp;" + locationHeader(entity, loc) + ":&nbsp;";
                col2 = guiScaledFontHTML(TT_SMALLFONT_DELTA) + f + "</FONT>";
                col2 += destroyedLocBar(entity.getOArmor(loc, true)).toString();
            } else {
                // Rear armor
                if (entity.hasRearArmor(loc)) {
                    String msg_abbr_rear = Messages.getString("BoardView1.Tooltip.AbbreviationRear");
                    f = "&nbsp;&nbsp;" + locationHeader(entity, loc) + msg_abbr_rear + "&nbsp;";
                    col1 = guiScaledFontHTML(TT_SMALLFONT_DELTA) + f + "</FONT>";
                    col1 += intactLocBar(entity.getOArmor(loc, true), entity.getArmor(loc, true), armorChar).toString();
                } else {
                    // No rear armor: empty table cells instead
                    // At small font sizes, writing one character at the correct font size is 
                    // necessary to prevent the table rows from being spaced non-beautifully
                    col1 = guiScaledFontHTML(TT_SMALLFONT_DELTA) + "&nbsp;" + "</FONT>";
                }
                // Front armor
                f = "&nbsp;&nbsp;" + locationHeader(entity, loc) + ":&nbsp;";
                col2 = guiScaledFontHTML(TT_SMALLFONT_DELTA) + f + "</FONT>";
                col2 += intactLocBar(entity.getOInternal(loc), entity.getInternal(loc), internalChar).toString();
                col2 += intactLocBar(entity.getOArmor(loc), entity.getArmor(loc), armorChar).toString();
            }

            String msg_abbr_sensors = Messages.getString("BoardView1.Tooltip.AbbreviationSensors");
            String msg_abbr_lifesupport = Messages.getString("BoardView1.Tooltip.AbbreviationLifeSupport");
            String msg_abbr_engine = Messages.getString("BoardView1.Tooltip.AbbreviationEngine");
            String msg_abbr_gyro = Messages.getString("BoardView1.Tooltip.AbbreviationGyro");
            String msg_abbr_shoulder = Messages.getString("BoardView1.Tooltip.AbbreviationShoulder");
            String msg_abbr_upperarm = Messages.getString("BoardView1.Tooltip.AbbreviationUpperArm");
            String msg_abbr_lowerarm = Messages.getString("BoardView1.Tooltip.AbbreviationLowerArm");
            String msg_abbr_hand = Messages.getString("BoardView1.Tooltip.AbbreviationHand");
            String msg_abbr_hip = Messages.getString("BoardView1.Tooltip.AbbreviationHip");
            String msg_abbr_upperleg = Messages.getString("BoardView1.Tooltip.AbbreviationUpperLeg");
            String msg_abbr_lowerleg = Messages.getString("BoardView1.Tooltip.AbbreviationLowerLeg");
            String msg_abbr_foot = Messages.getString("BoardView1.Tooltip.AbbreviationLowerFoot");

            switch (loc) {
                case 0:
                    col3 = sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, loc, msg_abbr_sensors).toString();
                    col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, loc, msg_abbr_lifesupport).toString();
                    break;
                case 1:
                    col3 = sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, loc, msg_abbr_engine).toString();
                    col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, loc, msg_abbr_gyro).toString();
                    col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, loc, msg_abbr_sensors).toString();
                    col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, loc, msg_abbr_lifesupport).toString();
                    break;
                case 2:
                case 3:
                    col3 = sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, loc, msg_abbr_engine).toString();
                    col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, loc, msg_abbr_gyro).toString();
                    break;
                case 4:
                case 5:
                    col3 = sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER, loc, msg_abbr_shoulder).toString();
                    col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_ARM, loc, msg_abbr_upperarm).toString();
                    col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_ARM, loc, msg_abbr_lowerarm).toString();
                    col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HAND, loc, msg_abbr_hand).toString();
                    col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc, msg_abbr_hip).toString();
                    col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, loc, msg_abbr_upperleg).toString();
                    col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, loc, msg_abbr_lowerleg).toString();
                    col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, loc, msg_abbr_foot).toString();
                    break;
                case 6:
                case 7:                
                case 8:
                    col3 = sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc, msg_abbr_hip).toString();
                    col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, loc, msg_abbr_upperleg).toString();
                    col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, loc, msg_abbr_lowerleg).toString();
                    col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, loc, msg_abbr_foot).toString();
                    break;
            }

            col1 = "<TD>" + col1 + "</TD>";
            col2 = "<TD>" + col2 + "</TD>";
            col3 = "<TD>" + col3 + "</TD>";
            row = "<TR>" + col1 + col2 + col3 + "</TR>";
            rows += row;
        }

        String tbody = "<TBODY>" + rows + "</TBODY>";
        String table = "<TABLE CELLSPACING=0 CELLPADDING=0>" + tbody + "</TABLE>";

        return new StringBuilder().append(table);
    }
    
    /** 
     * Used for destroyed locations.
     * Returns a string representing armor or internal structure of the location.
     * The location has the given orig original Armor/IS. 
     */
    private static StringBuilder destroyedLocBar(int orig) {
        String destroyedChar = GUIP.getString(GUIPreferences.ADVANCED_ARMORMINI_DESTROYED_CHAR);
        return locBar(orig, orig, destroyedChar, true);
    }

    /** 
     * Used for intact locations.
     * Returns a string representing armor or internal structure of the location.
     * The location has the given orig original Armor/IS. 
     */
    private static StringBuilder intactLocBar(int orig, int curr, String dChar) {
        return locBar(orig, curr, dChar, false);
    }
    
    /** 
     * Returns a string representing armor or internal structure of one location.
     * The location has the given orig original Armor/IS and the given curr current
     * Armor/IS. The character dChar will be repeated at appropriate colors depending
     * on the value of curr, orig and the static visUnit which gives the amount of 
     * Armor/IS per single character. 
     */
    private static StringBuilder locBar(int orig, int curr, String dChar, boolean destroyed) {
        // Internal Structure can be zero, e.g. in Aero
        if (orig == 0) {
            return new StringBuilder("");
        }

        String result = "";
        String f = "";
        Color colorIntact = GUIP.getColor(GUIPreferences.ADVANCED_ARMORMINI_COLOR_INTACT);
        Color colorPartialDmg = GUIP.getColor(GUIPreferences.ADVANCED_ARMORMINI_COLOR_PARTIAL_DMG);
        Color colorDamaged = GUIP.getColor(GUIPreferences.ADVANCED_ARMORMINI_COLOR_DAMAGED);
        int visUnit = GUIP.getInt(GUIPreferences.ADVANCED_ARMORMINI_UNITS_PER_BLOCK);
        
        if (destroyed) {
            colorIntact = colorDamaged;
            colorPartialDmg = colorDamaged;
        }
        
        int numPartial = ((curr != orig) && (curr % visUnit) > 0) ? 1 : 0;
        int numIntact = (curr - 1) / visUnit + 1 - numPartial;
        int numDmgd = (orig - 1) / visUnit + 1 - numPartial - numIntact;

        if (curr <= 0) {
            numPartial = 0;
            numIntact = 0;
            numDmgd = (orig - 1) / visUnit + 1;
        }
        
        if (numIntact > 0) {
            if (numIntact > 15 && numIntact + numDmgd > 30) {
                int tensIntact = (numIntact - 1) / 10;
                String msg_x = Messages.getString("BoardView1.Tooltip.X");
                f = dChar + msg_x + tensIntact * 10;
                f += repeat(dChar, numIntact - 10 * tensIntact);
                result += guiScaledFontHTML(colorIntact, TT_SMALLFONT_DELTA) + f + "</FONT>";
            } else {
                f = repeat(dChar, numIntact);
                result += guiScaledFontHTML(colorIntact, TT_SMALLFONT_DELTA) + f  + "</FONT>";
            }
        }
        if (numPartial > 0) {
            f = repeat(dChar, numPartial);
            result += guiScaledFontHTML(colorPartialDmg, TT_SMALLFONT_DELTA) + f + "</FONT>";
        }
        if (numDmgd > 0) {
            if (numDmgd > 15 && numIntact + numDmgd > 30) {
                int tensDmgd = (numDmgd - 1) / 10;
                String msg_x = Messages.getString("BoardView1.Tooltip.X");
                f = dChar + msg_x + tensDmgd * 10;
                f += repeat(dChar, numDmgd - 10 * tensDmgd);
                result +=  guiScaledFontHTML(colorIntact, TT_SMALLFONT_DELTA) + f + "</FONT>";
            } else {
                f = repeat(dChar, numDmgd);
                result += guiScaledFontHTML(colorDamaged, TT_SMALLFONT_DELTA) + f + "</FONT>";
            }
        }
        return new StringBuilder().append(result);
    }

    private static StringBuilder systemBar(int good, int bad, boolean destroyed) {
        // Internal Structure can be zero, e.g. in Aero
        if ((good + bad) == 0) {
            return new StringBuilder("");
        }

        String result = "";
        String f = "";
        Color colorIntact = GUIP.getColor(GUIPreferences.ADVANCED_ARMORMINI_COLOR_INTACT);
        Color colorDamaged = GUIP.getColor(GUIPreferences.ADVANCED_ARMORMINI_COLOR_DAMAGED);
        String dChar =  GUIP.getString(GUIPreferences.ADVANCED_ARMORMINI_DESTROYED_CHAR);
        String iChar = GUIP.getString(GUIPreferences.ADVANCED_ARMORMINI_IS_CHAR);

        if (good > 0)  {
            if (!destroyed) {
                f = repeat(iChar, good);
                result = guiScaledFontHTML(colorIntact, TT_SMALLFONT_DELTA) + f + "</FONT>";
            } else {
                f = repeat(iChar, good);
                result = guiScaledFontHTML(colorDamaged, TT_SMALLFONT_DELTA) + f + "</FONT>";
            }
        }
        if (bad > 0) {
            f =repeat(dChar, bad);
            result = guiScaledFontHTML(colorDamaged, TT_SMALLFONT_DELTA) + f + "</FONT>";
        }
        return new StringBuilder().append(result);
    }
    
    private static class WeaponInfo {
        String name;
        String sortString;
        String range = "";
        int count = 1;
        boolean isClan;
        boolean isHotloaded = false;
        boolean isRapidFire = false;
        HashMap<String, Integer> ammos = new HashMap<>();
        int ammoActiveWeaponCount;
    }
    
    /** 
     * Returns true if the weapontype should be excluded from the Tooltip. 
     * This is true for C3 computers (only Masters are weapons) and 
     * special Infantry attacks (Swarm Attacks and the like).
     */ 
    private static boolean isNotTTRelevant(WeaponType wtype) {
        return wtype.hasFlag(WeaponType.F_C3M) || wtype.hasFlag(WeaponType.F_C3MBS)
                || wtype instanceof LegAttack || wtype instanceof SwarmAttack
                || wtype instanceof StopSwarmAttack || wtype instanceof SwarmWeaponAttack;
    }
    
    private static final String RAPIDFIRE = "|RF|";
    private static final String CLANWP = "|CL|";

    /** Returns the assembled weapons with ranges etc. */
    private static StringBuilder weaponList(Entity entity) {
        ArrayList<Mounted> weapons = entity.getWeaponList();
        HashMap<String, WeaponInfo> wpInfos = new HashMap<>();
        // Gather names, counts, Clan/IS
        WeaponInfo currentWp;
        for (Mounted curWp: weapons) {
            WeaponType wtype = (WeaponType) curWp.getType();
            if (isNotTTRelevant(wtype)) {
                continue;
            }
            String weapDesc = curWp.getDesc();
            // Distinguish equal weapons with and without rapid fire
            if (isRapidFireActive(entity.getGame()) && curWp.isRapidfire() && !curWp.isDestroyed()) {
                weapDesc += RAPIDFIRE;
            }
            if (weapDesc.startsWith("+")) {
                weapDesc = weapDesc.substring(1);
            }
            if (curWp.getType().isClan()) {
                weapDesc += CLANWP;
            }

            String msg_clanbrackets = Messages.getString("BoardView1.Tooltip.ClanBrackets");
            String msg_clanparens = Messages.getString("BoardView1.Tooltip.ClanParens");
            weapDesc = weapDesc.replace(msg_clanbrackets, "").replace(msg_clanparens, "").trim();
            if (wpInfos.containsKey(weapDesc)) {
                currentWp = wpInfos.get(weapDesc);
                currentWp.count++;
                wpInfos.put(weapDesc, currentWp);
                String msg_ammo = Messages.getString("BoardView1.Tooltip.Ammo");
                if (!curWp.isDestroyed() && wpInfos.containsKey(curWp.getName() + msg_ammo)) {
                    WeaponInfo currAmmo = wpInfos.get(curWp.getName() + msg_ammo);
                    currAmmo.ammoActiveWeaponCount++;
                } 
            } else {
                currentWp = new WeaponInfo();
                currentWp.name = weapDesc;
                currentWp.sortString = curWp.getName();
                // Sort active weapons below destroyed to keep them close to their ammo
                if (!curWp.isDestroyed()) {
                    currentWp.sortString += "1";
                }
                currentWp.isRapidFire = weapDesc.contains(RAPIDFIRE);

                // Create the ranges String
                int[] ranges;
                if (entity.isAero()) {
                    ranges = wtype.getATRanges();
                } else {
                    ranges = wtype.getRanges(curWp);
                }
                String rangeString = " \u22EF ";
                if (ranges[RangeType.RANGE_MINIMUM] > 0) {
                    rangeString += "(" + ranges[RangeType.RANGE_MINIMUM] + ") ";
                }
                int maxRange = RangeType.RANGE_LONG;
                if (entity.getGame().getOptions().booleanOption(
                        OptionsConstants.ADVCOMBAT_TACOPS_RANGE)) {
                    maxRange = RangeType.RANGE_EXTREME;
                }
                for (int i = RangeType.RANGE_SHORT; i <= maxRange; i++) {
                    rangeString += ranges[i];
                    if (i != maxRange) {
                        rangeString += "\u2B1D";
                    }
                }
                WeaponType wpT = ((WeaponType) curWp.getType());
                if (!wpT.hasFlag(WeaponType.F_AMS)
                        || entity.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_MANUAL_AMS)) {
                    currentWp.range = rangeString;
                }

                currentWp.isClan = wpT.isClan();
                wpInfos.put(weapDesc, currentWp);

                // Add ammo info if the weapon has ammo 
                // Check wpInfos for dual entries to avoid displaying ammo twice for non/rapid-fire  
                if ((wtype.getAmmoType() != AmmoType.T_NA)
                        && (!wtype.hasFlag(WeaponType.F_ONESHOT) || wtype.hasFlag(WeaponType.F_BA_INDIVIDUAL))
                        && (wtype.getAmmoType() != AmmoType.T_INFANTRY)) {

                    String msg_ammo = Messages.getString("BoardView1.Tooltip.Ammo");
                    if (wpInfos.containsKey(curWp.getName() + msg_ammo)) {
                        if (!curWp.isDestroyed()) {
                            WeaponInfo currAmmo = wpInfos.get(curWp.getName() + msg_ammo);
                            currAmmo.ammoActiveWeaponCount++;
                        }
                    } else {
                        WeaponInfo currAmmo = new WeaponInfo();

                        String msg_zz = Messages.getString("BoardView1.Tooltip.ZZ");
                        currAmmo.sortString = curWp.getName() + msg_zz; // Sort ammo after the weapons
                        currAmmo.ammoActiveWeaponCount = curWp.isDestroyed() ? 0 : 1;
                        for (Mounted amounted : entity.getAmmo()) {
                            boolean canSwitchToAmmo = AmmoType.canSwitchToAmmo(curWp, (AmmoType) amounted.getType());
                            if (canSwitchToAmmo && !amounted.isDumping()) {
                                String msg_antipersonnel = Messages.getString("BoardView1.Tooltip.AntiPersonnel");
                                String msg_ap = Messages.getString("BoardView1.Tooltip.AP");
                                String msg_isbracket = Messages.getString("BoardView1.Tooltip.ISBracket");
                                String msg_halfbrackets = Messages.getString("BoardView1.Tooltip.HalfBrackets");
                                String msg_half = Messages.getString("BoardView1.Tooltip.Half");
                                String msg_standard = Messages.getString("BoardView1.Tooltip.Standard");
                                String msg_hotloadedparens = Messages.getString("BoardView1.Tooltip.HotLoadedParens");

                                String name = amounted.getName().replace(msg_antipersonnel, msg_ap)
                                        .replace(msg_ammo, "").replace(msg_isbracket, "").replace(msg_clanbrackets, "")
                                        .replace(msg_clanparens, "").replace(msg_halfbrackets, "").replace(msg_half, "")
                                        .replace(curWp.getDesc(), "").trim();
                                if (name.isBlank()) {
                                    name = msg_standard;
                                }

                                if (amounted.isHotLoaded()) {
                                    name += " " + msg_hotloadedparens;
                                }
                                int count = amounted.getUsableShotsLeft();
                                count += currAmmo.ammos.getOrDefault(name, 0);
                                currAmmo.ammos.put(name, count);
                            }
                        }
                        wpInfos.put(curWp.getName() + msg_ammo, currAmmo);
                    }
                }
            }
        }
        
        // Print to Tooltip
        String col1 = "";
        String col2 = "";
        String row = "";
        String rows = "";
        boolean subsequentLine = false; 
        // Display sorted by weapon name
        var wps = new ArrayList<>(wpInfos.values());
        wps.sort(Comparator.comparing(w -> w.sortString));
        int totalWeaponCount = wpInfos.values().stream().filter(i -> i.ammos.isEmpty()).mapToInt(wp -> wp.count).sum();
        boolean hasMultiples = wpInfos.values().stream().mapToInt(wp -> wp.count).anyMatch(c -> c > 1);
        for (WeaponInfo currentEquip : wps) {
            // This WeaponInfo is ammo
            if (!currentEquip.ammos.isEmpty()) {
                row = createAmmoEntry(currentEquip).toString();
            } else {
                // This WeaponInfo is a weapon
                // Check if weapon is destroyed, text gray and strikethrough if so, remove the "x "/"*"
                boolean isDestroyed = false;
                String nameStr = currentEquip.name;
                if (nameStr == null) {
                    String msg_nullweaponname = Messages.getString("BoardView1.Tooltip.NullWeaponName");
                    nameStr = msg_nullweaponname; // Happens with Vehicle Flamers!
                }
                if (nameStr.startsWith("x ")) {
                    nameStr = nameStr.substring(2);
                    isDestroyed = true;
                }

                if (nameStr.startsWith("*")) {
                    nameStr = nameStr.substring(1);
                    isDestroyed = true;
                }

                // Remove the rapid fire marker (used only to distinguish weapons set to different modes)
                nameStr = nameStr.replace(RAPIDFIRE, "");
                nameStr = nameStr.replace(CLANWP, "");
                nameStr += currentEquip.range;
                String techBase = "";

                if (entity.isMixedTech()) {
                    String msg_clan = Messages.getString("BoardView1.Tooltip.Clan");
                    String msg_is = Messages.getString("BoardView1.Tooltip.IS");
                    techBase = currentEquip.isClan ? msg_clan : msg_is;
                    techBase += " ";
                }

                String destStr = "";

                if (totalWeaponCount > 5 && hasMultiples) {
                    // more than 5 weapons: group and list with a multiplier "4 x Small Laser"
                    if (currentEquip.count > 1) {
                        String msg_x = Messages.getString("BoardView1.Tooltip.X");
                        col1 = currentEquip.count + " " + msg_x + " ";
                    } else {
                        col1 = "";
                    }

                    col2 = addToTT("Weapon", false, currentEquip.count, techBase, nameStr, destStr).toString();
                    col2 += weaponModifier(isDestroyed, currentEquip);
                    if (isDestroyed) {
                        col2 = "<S>" + col2 + "</S>";
                    }
                } else {
                    col1 = "";
                    col2 = "";
                    // few weapons: list each weapon separately
                    for (int i = 0; i < currentEquip.count; i++) {
                        col2 += addToTT("Weapon", false, currentEquip.count, techBase, nameStr, destStr).toString();
                        col2 += weaponModifier(isDestroyed, currentEquip);
                        if (isDestroyed) {
                            col2 = "<S>" + col2 + "</S>";
                        }
                        col2 += "<BR>";
                    }
                };

                col1 = guiScaledFontHTML(uiTTWeaponColor()) + col1 + "</FONT>";
                col1 = "<TD>" + col1 + "</TD>";
                col2 = guiScaledFontHTML(uiTTWeaponColor()) + col2 + "</FONT>";
                col2 = "<TD>" + col2 + "</TD>";
                row = "<TR>" + col1 + col2 + "</TR>";
            }

            rows += row;
        }

        String tbody = "<TBODY>" + rows + "</TBODY>";
        String table = "<TABLE CELLSPACING=0 CELLPADDING=0>" + tbody + "</TABLE>";

        return new StringBuilder().append(table);
    }

    private static StringBuilder bombList(Entity entity) {
        String col1 = "";
        String col2 = "";
        String col3= "";
        String row = "";
        String rows = "";
        String table = "";

        if (entity.isBomber()) {
            int[] loadout = { };

            if (entity.getGame().getPhase().isLounge()) {
                loadout = ((IBomber) entity).getBombChoices();
            } else {
                loadout = entity.getBombLoadout();
            }

            for (int i = 0; i < loadout.length; i++) {
                int count = loadout[i];

                if (count > 0) {
                    col1 = String.valueOf(count);
                    col2 = "&nbsp;x&nbsp;";
                    col3 = BombType.getBombName(i);

                    col1 = guiScaledFontHTML(uiTTWeaponColor()) + col1 + "</FONT>";
                    col1 = "<TD>" + col1 + "</TD>";
                    col2 = guiScaledFontHTML(uiTTWeaponColor()) + col2 + "</FONT>";
                    col2 = "<TD>" + col2 + "</TD>";
                    col3 = guiScaledFontHTML(uiTTWeaponColor()) + col3 + "</FONT>";
                    col3 = "<TD>" + col3 + "</TD>";
                    row = "<TR>" + col1  + col2 + col3 + "</TR>";
                } else {
                    row = "";
                }

                rows += row;
            }

            String tbody = "<TBODY>" + rows + "</TBODY>";
            table = "<TABLE CELLSPACING=0 CELLPADDING=0>" + tbody + "</TABLE>";
        }

        return new StringBuilder().append(table);
    }

    private static String weaponModifier(boolean isDestroyed, WeaponInfo currentEquip) {
        if (isDestroyed) {
            // Ends the strikethrough that is added for destroyed weapons
            return "";
        } else if (currentEquip.isHotloaded) {
            String msg_hotloaded = Messages.getString("BoardView1.Tooltip.HotLoaded");
            String s = "<I> " + msg_hotloaded + "</I>";
            return " \u22EF" + s;
        } else if (currentEquip.isRapidFire) {
            String msg_rapidfire = Messages.getString("BoardView1.Tooltip.Rapidfire");
            String s = "<I> " + msg_rapidfire + "</I>";
            return " \u22EF" + s;
        }
        return "";
    }
    
    /** Returns the ammo line(s) for the ammo of one weapon type. */
    private static StringBuilder createAmmoEntry(WeaponInfo ammoInfo) {
        String col1 = "";
        String col2 = "";
        String row = "";
        String rows = "";

        int totalAmmo = ammoInfo.ammos.values().stream().mapToInt(n -> n).sum();
        if (totalAmmo == 0 && ammoInfo.ammoActiveWeaponCount > 0) {
            String msg_outofammo = Messages.getString("BoardView1.Tooltip.OutOfAmmo");
            col2 = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + msg_outofammo;
            col1 = "<TD>" + col1 + "</TD>";
            col2 = guiScaledFontHTML(uiYellow(), -0.2f) + col2 + "</FONT>";
            col2 = "<TD>" + col2 + "</TD>";
            row = "<TR>" + col1 + col2 + "</TR>";
            rows += row;
        } else {
            for (Entry<String, Integer> ammo: ammoInfo.ammos.entrySet()) {
                String msg_standard = Messages.getString("BoardView1.Tooltip.Standard");
                String ammoName = ammo.getKey().equals(msg_standard) && ammoInfo.ammos.size() == 1 ? "" : ammo.getKey() + ": ";
                // No entry when no ammo of this type left but some other type left
                if (ammo.getValue() == 0) {
                    continue;
                }
                col1 = "";
                col2 = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
                String msg_shots = Messages.getString("BoardView1.Tooltip.Shots");
                if (ammoInfo.ammoActiveWeaponCount > 1) { 
                    // Remaining ammo and multiple weapons using it
                    col2 += ammoName;
                    String msg_turns = Messages.getString("BoardView1.Tooltip.Turns");
                    col2 += ammo.getValue() / ammoInfo.ammoActiveWeaponCount + " " + msg_turns;
                    col2 += " (" + ammo.getValue() + " " + msg_shots + ")";
                } else { 
                    // Remaining ammo and only one weapon using it
                    col2 += ammoName + ammo.getValue() + " " + msg_shots;
                }

                col1 = "<TD>" + col1 + "</TD>";
                col2 = guiScaledFontHTML(uiYellow(), -0.2f) + col2 + "</FONT>";
                col2 = "<TD>" + col2 + "</TD>";
                row = "<TR>" + col1 + col2 + "</TR>";
                rows += row;
            }
        }

        return new StringBuilder().append(rows);
    }

    /** Returns a line showing ECM / ECCM. */
    private static StringBuilder ecmInfo(Entity entity) {
        String f = "";
        String result = "";
        if (entity.hasActiveECM()) {
            String msg_ecmsource = Messages.getString("BoardView1.ecmSource");
            f += ECM_SIGN + " " + msg_ecmsource;
        }
        if (entity.hasActiveECCM()) {
            String msg_eccmsource = Messages.getString("BoardView1.eccmSource");
            f += ECM_SIGN+ " " +msg_eccmsource;
        }

        result = guiScaledFontHTML() + f + "</FONT>";

        return new StringBuilder().append(result);
    }

    /** Returns values that only are relevant when in-game such as heat. */
    private static StringBuilder inGameValues(Entity entity, Player localPlayer) {
        Game game = entity.getGame();
        boolean isGunEmplacement = entity instanceof GunEmplacement;
        String result = "";
        String f = "";
        String i = "";
        
        // BV Info
        // Hidden for invisible units when in double blind and hide enemy bv is selected
        // Also not shown in the lobby as BV is shown there outside the tooltip
        boolean showEnemyBV = !(game.getOptions().booleanOption(OptionsConstants.ADVANCED_SUPPRESS_DB_BV) &&
                game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND));
        boolean isVisible = EntityVisibilityUtils.trackThisEntitiesVisibilityInfo(localPlayer, entity);

        if (isVisible || showEnemyBV) {
            int currentBV = entity.calculateBattleValue(false, false);
            int initialBV = entity.getInitialBV();
            double percentage = (double) currentBV / initialBV;
            result += addToTT("BV", BR, currentBV, initialBV, percentage).toString();
        }

        String damageLevel;
        switch (entity.getDamageLevel()) {
            case Entity.DMG_CRIPPLED:
                String msg_crippled = Messages.getString("BoardView1.Tooltip.Crippled");
                f = "&nbsp;&nbsp;" + msg_crippled;
                damageLevel = guiScaledFontHTML(GUIP.getWarningColor()) + f + "</FONT>";
                break;
            case Entity.DMG_HEAVY:
                String msg_heavydmg = Messages.getString("BoardView1.Tooltip.HeavyDmg");
                f = "&nbsp;&nbsp;" + msg_heavydmg;
                damageLevel = guiScaledFontHTML(GUIP.getWarningColor()) + f + "</FONT>";
                break;
            case Entity.DMG_MODERATE:
                String msg_moderatedmg = Messages.getString("BoardView1.Tooltip.ModerateDmg");
                damageLevel = "&nbsp;&nbsp;"+ msg_moderatedmg;
                break;
            case Entity.DMG_LIGHT:
                String msg_lightdmg = Messages.getString("BoardView1.Tooltip.LightDmg");
                damageLevel = "&nbsp;&nbsp;" + msg_lightdmg ;
                break;
            default:
                String msg_undamaged = Messages.getString("BoardView1.Tooltip.Undamaged");
                damageLevel = "&nbsp;&nbsp;" + msg_undamaged;
        }
        result += damageLevel;

        // Actual Movement
        if (!isGunEmplacement) {
            // "Has not yet moved" only during movement phase
            if (!entity.isDone() && game.getPhase().isMovement()) {
                i = addToTT("NotYetMoved", BR).toString();
                f = "<I>" + i + "</I>";
                result += guiScaledFontHTML(GUIP.getColorForMovement(entity.moved)) + f + "</FONT>";
            } else if ((entity.isDone() && game.getPhase().isMovement())
                    || (game.getPhase().isMovementReport())
                    || (game.getPhase().isFiring())
                    || (game.getPhase().isFiringReport())
                    || (game.getPhase().isPhysical())
                    || (game.getPhase().isPhysicalReport())) {
                int tmm = Compute.getTargetMovementModifier(game, entity.getId()).getValue();
                if (entity.moved == EntityMovementType.MOVE_NONE) {
                    i = addToTT("NoMove", BR, tmm).toString().toString();
                    f = "<I>" + i + "</I>";
                } else {
                    i = addToTT("MovementF", BR, entity.getMovementString(entity.moved),
                            entity.delta_distance, tmm).toString();
                    f = "<I>" + i + "</I>";
                }
                // Special Moves
                if (entity.isEvading()) {
                    i = addToTT("Evade", BR).toString();
                    f = "<I>" + i + "</I>";
                    f = guiScaledFontHTML(GUIP.getWarningColor()) + f + "</FONT>";
                }

                if ((entity instanceof Infantry) && ((Infantry) entity).isTakingCover()) {
                    i = addToTT("TakingCover", BR).toString();
                    f = "<I>" + i + "</I>";
                    f = guiScaledFontHTML(GUIP.getWarningColor()) + f + "</FONT>";
                }

                if (entity.isCharging()) {
                    f = addToTT("Charging", BR).toString();
                }

                if (entity.isMakingDfa()) {
                    i = addToTT("DFA", BR).toString();
                    f = "<I>" + i + "</I>";
                    f = guiScaledFontHTML(GUIP.getWarningColor()) + f + "</FONT>";
                }

                if (entity.isUnjammingRAC()) {
                    f = "<BR>";
                    String msg_unjammingrac = Messages.getString("BoardView1.Tooltip.UnjammingRAC");
                    f += msg_unjammingrac;
                    if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_UNJAM_UAC)) {
                        String msg_andac = Messages.getString("BoardView1.Tooltip.AndAC");
                        f += msg_andac;
                    }
                }

                result += guiScaledFontHTML(GUIP.getColorForMovement(entity.moved)) + f + "</FONT>";
            }
        }

        i = "";
        f = "";

        if (entity.isAero()) {
            // Velocity, Altitude, Elevation, Fuel
            IAero aero = (IAero) entity;
            i = addToTT("AeroVelAltFuel", BR, aero.getCurrentVelocity(), aero.getAltitude(), aero.getFuel()).toString();
        } else if (entity.getElevation() != 0) {
            // Elevation only
            i = addToTT("Elev", BR, entity.getElevation()).toString();
        }
        f = "<I>" + i + "</I>";
        result += guiScaledFontHTML(uiLightViolet()) + f + "</FONT>";

        result += "<BR>";
        String msg_facing = Messages.getString("BoardView1.Tooltip.Facing");
        f = "&nbsp;&nbsp;" + msg_facing + ":&nbsp;" + PlanetaryConditions.getWindDirDisplayableName(entity.getFacing());
        if (entity.getFacing() != entity.getSecondaryFacing()) {
            String msg_twist = Messages.getString("BoardView1.Tooltip.Twist");
            f = "&nbsp;&nbsp;" + msg_twist + ":&nbsp;" + PlanetaryConditions.getWindDirDisplayableName(entity.getSecondaryFacing());
        }
        result += guiScaledFontHTML() + f + "</FONT>";

        // Heat, not shown for units with 999 heat sinks (vehicles)
        if (entity.getHeatCapacity() != 999) {
            int heat = entity.heat;
            f = "";
            if (heat == 0) {
                f += addToTT("Heat0", BR).toString();
            } else {
                f += addToTT("Heat", BR, heat).toString();
            }
            f += " / "+entity.getHeatCapacity();
            result += guiScaledFontHTML(GUIP.getColorForHeat(heat)) + f + "</FONT>";
        }

        // Gun Emplacement Status
        if (isGunEmplacement) {
            GunEmplacement emp = (GunEmplacement) entity; 
            if (emp.isTurret() && emp.isTurretLocked(emp.getLocTurret())) {
                i = addToTT("TurretLocked", BR).toString();
                f = "<I>" + i + "</I>";
                result += guiScaledFontHTML(GUIP.getWarningColor()) + f + "</FONT>";
            }
        }

        // Unit Immobile
        if (!isGunEmplacement && entity.isImmobile()) {
            f = addToTT("Immobile", BR).toString();
            result += guiScaledFontHTML(GUIP.getWarningColor()) + f + "</FONT>";
        }

        // Unit Prone
        if (!isGunEmplacement && entity.isProne()) {
            f = addToTT("Prone", BR).toString();
            result += guiScaledFontHTML(GUIP.getWarningColor()) + f + "</FONT>";
        }


        if (!entity.getHiddenActivationPhase().isUnknown()) {
            result += addToTT("HiddenActivating", BR, entity.getHiddenActivationPhase().toString()).toString();
        } else if (entity.isHidden()) {
            result += addToTT("Hidden", BR).toString();
        }

        // Jammed by ECM - don't know how to replicate this correctly from the boardview
        //      if (isAffectedByECM()) {
        //          addToTT("Jammed", BR);
        //      }

        // Swarmed
        if (entity.getSwarmAttackerId() != Entity.NONE) {
            final Entity swarmAttacker = game.getEntity(entity.getSwarmAttackerId());
            if (swarmAttacker == null) {
                LogManager.getLogger().error(String.format(
                        "Entity %s is currently swarmed by an unknown attacker with id %s",
                        entity.getId(), entity.getSwarmAttackerId()));
            }
            String msg_error = Messages.getString("ERROR");
            String sa = (swarmAttacker == null) ? msg_error : swarmAttacker.getDisplayName();
            f = addToTT("Swarmed", BR, sa).toString();
            result += guiScaledFontHTML(GUIP.getWarningColor()) + f + "</FONT>";
        }

        // Spotting
        if (entity.isSpotting() && game.hasEntity(entity.getSpotTargetId())) {
            f = addToTT("Spotting", BR, game.getEntity(entity.getSpotTargetId()).getDisplayName()).toString();
            result += guiScaledFontHTML() + f + "</FONT>";
        }

        // If Double Blind, add information about who sees this Entity
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
            StringBuffer tempList = new StringBuffer();
            boolean teamVision = game.getOptions().booleanOption(
                    OptionsConstants.ADVANCED_TEAM_VISION);
            int seenByResolution = GUIP.getAdvancedUnitToolTipSeenByResolution();
            String tmpStr = "";

            dance: for (Player player :  entity.getWhoCanSee()) {
                if (player.isEnemyOf(entity.getOwner()) || !teamVision) {
                    switch (seenByResolution) {
                        case 1:
                            String msg_someone = Messages.getString("BoardView1.Tooltip.Someone");
                            tempList.append(msg_someone);
                            tempList.append(", ");
                            break dance;
                        case 2:
                            tmpStr = game.getTeamForPlayer(player).toString();
                            break;
                        case 3:
                            tmpStr = player.getName();
                            break;
                        case 4:
                            tmpStr = player.toString();
                            break;
                        default:
                            break dance;
                    }

                    if (tempList.indexOf(tmpStr) == -1) {
                        tempList.append(tmpStr);
                        tempList.append(", ");
                    }
                }
            }
            if (tempList.length() > 1) {
                tempList.delete(tempList.length() - 2, tempList.length());
                f = addToTT("SeenBy", BR, tempList.toString()).toString();
                result += guiScaledFontHTML() + f + "</FONT>";
            }            
        }

        // If sensors, display what sensors this unit is using
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_SENSORS)
                || game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS)) {
            result += addToTT("Sensors", BR, entity.getSensorDesc(), Compute.getMaxVisualRange(entity, false)).toString();
        }

        if (entity.hasAnyTypeNarcPodsAttached()) {
            f = addToTT(entity.hasNarcPodsAttached() ? "Narced" : "INarced", BR).toString();
            result += guiScaledFontHTML(uiLightRed()) + f + "</FONT>";
        }
        
        // Towing
        if (!entity.getAllTowedUnits().isEmpty()) {
            String unitList = entity.getAllTowedUnits().stream()
                    .map(id -> entity.getGame().getEntity(id).getShortName())
                    .collect(Collectors.joining(", "));
            if (unitList.length() > 1) {
                result += addToTT("Towing", BR, unitList).toString();
            }
        }

        // Coloring and italic to make these transient entries stand out
        result = guiScaledFontHTML(uiLightViolet()) + result + "</FONT>";
        result = "<I>" + result + "</I>";

        return new StringBuilder().append(result);
    }
    
    /** Returns unit values that are relevant in-game and in the lobby such as movement ability. */
    private static StringBuilder entityValues(Entity entity) {
        boolean isGunEmplacement = entity instanceof GunEmplacement;
        String result = "";
        String f = "";
        String d = "";
        String l1= "";
        String l2 = "";
        String l3 = "";
        String l4 = "";

        // Unit movement ability
        if (!isGunEmplacement) {
            int hipHits = 0;
            int actuatorHits = 0;
            int legsDestroyed = 0;

            if (entity instanceof Mech) {
                if (entity.getMovementMode() == EntityMovementMode.TRACKED) {
                    for (Mounted m : entity.getMisc()) {
                        if (m.getType().hasFlag(MiscType.F_TRACKS)) {
                            if (m.isHit() || entity.isLocationBad(m.getLocation())) {
                                legsDestroyed++;
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < entity.locations(); i++) {
                        if (entity.locationIsLeg(i)) {
                            if (!entity.isLocationBad(i)) {
                                if (((Mech) entity).legHasHipCrit(i)) {
                                    hipHits++;
                                    if ((entity.getGame() == null) || (!entity.getGame().getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEG_DAMAGE))) {
                                        continue;
                                    }
                                }
                                actuatorHits += ((Mech) entity).countLegActuatorCrits(i);
                            }
                            else {
                                legsDestroyed++;
                            }
                        }
                    }
                }
            }

            int jumpJet = 0;
            int jumpJetDistroyed = 0;
            int jumpBooster = 0;
            int jumpBoosterDistroyed = 0;
            int paritalWing = 0;
            int paritalWingDistroyed = 0;
            int partialWingWeaterMod = 0;

            if ((entity instanceof Mech) || (entity instanceof Tank)) {
                for (Mounted mounted : entity.getMisc()) {
                    if (mounted.getType().hasFlag(MiscType.F_JUMP_JET)) {
                        jumpJet++;
                        if (mounted.isDestroyed() || mounted.isBreached()) {
                            jumpJetDistroyed++;
                        }
                    }
                    if (mounted.getType().hasFlag(MiscType.F_JUMP_BOOSTER)) {
                        jumpBooster++;
                        if (mounted.isDestroyed() || mounted.isBreached()) {
                            jumpBoosterDistroyed++;
                        }
                    }
                    if (mounted.getType().hasFlag(MiscType.F_PARTIAL_WING)) {
                        int eNum = entity.getEquipmentNum(mounted);
                        paritalWing += entity.getGoodCriticals(CriticalSlot.TYPE_EQUIPMENT, eNum, Mech.LOC_RT);
                        paritalWing += entity.getGoodCriticals(CriticalSlot.TYPE_EQUIPMENT, eNum, Mech.LOC_LT);
                        paritalWingDistroyed += entity.getBadCriticals(CriticalSlot.TYPE_EQUIPMENT, eNum, Mech.LOC_RT);
                        paritalWingDistroyed += entity.getBadCriticals(CriticalSlot.TYPE_EQUIPMENT, eNum, Mech.LOC_LT);

                        partialWingWeaterMod = ((Mech) entity).getPartialWingJumpAtmoBonus() - ((Mech) entity).getPartialWingJumpWeightClassBonus();
                    }
                }

                paritalWing += paritalWingDistroyed;
            }

            int walkMP = entity.getOriginalWalkMP();
            int runMP = entity.getOriginalRunMP();
            int jumpMP = entity.getOriginalJumpMP();
            d = addToTT("Movement", NOBR , walkMP,  runMP).toString();

            if (jumpMP > 0) {
                d += "/" + jumpMP;
            }

            int walkMPModified = entity.getWalkMP(true, false,false);
            int runMPModified = entity.getRunMP(true, false, false);
            int jumpMPModified = entity.getJumpMP(true);

            if ((walkMP != walkMPModified) || (runMP != runMPModified) || (jumpMP != jumpMPModified)) {
                d += DOT_SPACER + walkMPModified + "/" + runMPModified;
                if (jumpMPModified > 0) {
                    d += "/" + jumpMPModified;
                }
                if (entity.getGame().getPlanetaryConditions().getGravity()  != 1.0) {
                    d += DOT_SPACER;
                    f =  entity.getGame().getPlanetaryConditions().getGravity() + "g";
                    d += guiScaledFontHTML(GUIP.getWarningColor()) + f + "</FONT>";
                }
                int walkMPNoHeat = entity.getWalkMP(true, true,false);
                int runMPNoHeat = entity.getRunMP(true, true, false);
                if ((walkMPNoHeat != walkMPModified) || (runMPNoHeat != runMPModified)) {
                    d += DOT_SPACER;
                    f = "\uD83D\uDD25";
                    d += guiScaledFontHTML(GUIP.getWarningColor()) + f + "</FONT>";
                }
            }

            d += DOT_SPACER;
            f = entity.getMovementModeAsString();
            d += guiScaledFontHTML(uiWhite()) + f + "</FONT>";

            if (entity instanceof IBomber) {
                int bombMod = 0;
                bombMod = ((IBomber) entity).reduceMPByBombLoad(walkMP);
                if (bombMod != walkMP) {
                    d += DOT_SPACER;
                    f = "\uD83D\uDCA3";
                    d+= guiScaledFontHTML(GUIP.getWarningColor()) + f + "</FONT>";
                }
            }

            int weatherMod = entity.getGame().getPlanetaryConditions().getMovementMods(entity);

            if ((weatherMod != 0) || (partialWingWeaterMod != 0)) {
                d += DOT_SPACER;
                f = "\u2602";
                d += guiScaledFontHTML(GUIP.getWarningColor()) + f + "</FONT>";
            }

            if ((legsDestroyed > 0) || (hipHits > 0) || (actuatorHits > 0) || (jumpJetDistroyed > 0) || (paritalWingDistroyed > 0)
                    || (jumpBoosterDistroyed > 0) || (entity.isImmobile()) || (entity.isGyroDestroyed())) {
                d += DOT_SPACER;
                f = "\uD83D\uDD27";
                d += guiScaledFontHTML(GUIP.getWarningColor()) + f + "</FONT>";
            }

            if ((entity instanceof BipedMech) || (entity instanceof TripodMech)) {
                int shieldMod = 0;
                if (entity.hasShield()) {
                    shieldMod -= entity.getNumberOfShields(MiscType.S_SHIELD_LARGE);
                    shieldMod -= entity.getNumberOfShields(MiscType.S_SHIELD_MEDIUM);
                }

                if (shieldMod != 0) {
                    d += DOT_SPACER;
                    f = "\u26E8";
                    d += guiScaledFontHTML(GUIP.getWarningColor()) + f + "</FONT>";
                }
            }

            if (entity.hasModularArmor()) {
                d += DOT_SPACER;
                f = "\u27EC\u25AE";
                d += DOT_SPACER + guiScaledFontHTML(GUIP.getWarningColor()) + f + "</FONT>";
            }

            l1 = "<Li style=\"list-style-type: none; list-style-image: none; margin: 0; padding: 0; width: 300px;\">" + d + "</Li>";
            d = "";

            if ((jumpJetDistroyed > 0) || (jumpBoosterDistroyed > 0) || (paritalWingDistroyed > 0)) {
                String jj = "";
                if (jumpJetDistroyed > 0) {
                    String msg_jumpjets = Messages.getString("BoardView1.Tooltip.JumpJets");
                    jj = msg_jumpjets + ": " + (jumpJet - jumpJetDistroyed) + "/" + jumpJet;
                }
                if (jumpBoosterDistroyed > 0)  {
                    String msg_jumpBoosters = Messages.getString("BoardView1.Tooltip.JumpBoosters");
                    jj += "; " + msg_jumpBoosters + ": " + (jumpBooster - jumpBoosterDistroyed) + "/" + jumpBooster;
                }
                if (paritalWingDistroyed > 0)  {
                    String msg_partialwing = Messages.getString("BoardView1.Tooltip.PartialWing");
                    jj += "; " + msg_partialwing + ": " + (paritalWing - paritalWingDistroyed) + "/" + paritalWing;
                }
                if (jj.startsWith(";")) {
                    jj = jj.substring(2);
                }
                l2 = "<Li style=\"list-style-type: none; list-style-image: none; margin: 0; padding: 0;\">" + jj + "</Li>";
            }
        }
        
        // Infantry specialization like SCUBA
        if (entity instanceof Infantry) {
            Infantry inf = (Infantry) entity;
            int spec = inf.getSpecializations();
            if (spec > 0) {
                d += addToTT("InfSpec", NOBR, Infantry.getSpecializationName(spec)).toString();
                l3 = "<Li style=\"list-style-type: none; list-style-image: none; margin: 0; padding: 0;\">" + d + "</Li>";
                d = "";
            }
        }

        // Armor and Internals
        if (entity.isConventionalInfantry()) {
            d += "";
        } else if (!isGunEmplacement) {
            String msg_unknown = Messages.getString("BoardView1.Tooltip.Unknown");
            String armorType = TROView.formatArmorType(entity, true).replace(msg_unknown, "");
            if (!armorType.isBlank()) {
                String msg_armorcapital = Messages.getString("BoardView1.Tooltip.ArmorCapital");
                armorType = (entity.isCapitalScale() ? msg_armorcapital + " " : "") + armorType;
                armorType = " (" + armorType + ")";
            }
            String armorStr = " " + entity.getTotalArmor() + armorType;
            d += addToTT("ArmorInternals", NOBR, armorStr, entity.getTotalInternal()).toString();
            l4 = "<Li style=\"list-style-type: none; list-style-image: none; margin: 0; padding: 0;\">" + d + "</Li>";
            d = "";
        }

        String ul = "<UL style=\"list-style-type: none; list-style-image: none; margin: 0; padding: 0;\">" + l1 + l2 + l3 + l4 + "</UL>";
        result += ul;

        return new StringBuilder().append(result);
    }
    
    /** Returns warnings about problems that should be solved before deploying. */
    private static StringBuilder deploymentWarnings(Entity entity, Player localPlayer,
                                                    MapSettings mapSettings) {
        String result = "";
        String f = "";
        // Critical (red) warnings
        if (entity.getGame().getPlanetaryConditions().whyDoomed(entity, entity.getGame()) != null) {
            String msg_cannotsurvive = Messages.getString("BoardView1.Tooltip.CannotSurvive");
            f = "<BR>" + msg_cannotsurvive + " " + entity.getGame().getPlanetaryConditions().whyDoomed(entity, entity.getGame());
        }
        if (entity.doomedInAtmosphere() && mapSettings.getMedium() == MapSettings.MEDIUM_ATMOSPHERE) {
            String msg_cannotsurviveatmo = Messages.getString("BoardView1.Tooltip.CannotSurviveAtmo");
            f = "<BR>" + msg_cannotsurviveatmo;
        }
        if (entity.doomedOnGround() && mapSettings.getMedium() == MapSettings.MEDIUM_GROUND) {
            String msg_cannotsurviveground = Messages.getString("BoardView1.Tooltip.CannotSurviveGround");
            f = "<BR>" + msg_cannotsurviveground;
        }
        if (entity.doomedInSpace() && mapSettings.getMedium() == MapSettings.MEDIUM_SPACE) {
            String msg_cannotsurvivespace = Messages.getString("BoardView1.Tooltip.CannotSurviveSpace");
            f = "<BR>" + msg_cannotsurvivespace;
        }
        result += guiScaledFontHTML(GUIP.getWarningColor()) + f + "</FONT>";

        f = "";
        // Non-critical (yellow) warnings
        if (((entity.hasC3i() || entity.hasNavalC3()) && (entity.calculateFreeC3Nodes() == 5))
                || ((entity.getC3Master() == null) && entity.hasC3S())
                || (entity.hasNovaCEWS() && (entity.calculateFreeC3Nodes() == 2))) {
            String msg_unconnectedc3computer = Messages.getString("BoardView1.Tooltip.UnconnectedC3Computer");
            f = "<BR>" + msg_unconnectedc3computer;
        }
        
        // Non-critical (yellow) warnings
        if (entity instanceof FighterSquadron && entity.getLoadedUnits().isEmpty()) {
            String msg_fightersquadronempty = Messages.getString("BoardView1.Tooltip.FighterSquadronEmpty");
            f = "<BR>" + msg_fightersquadronempty;
        }

        result += guiScaledFontHTML(uiYellow()) + f + "</FONT>";

        return new StringBuilder().append(result);
    }
    
    /** Returns a list of units loaded onto this unit. */
    private static StringBuilder carriedUnits(Entity entity) {
        String result = "";
        String f = "";

        if (entity instanceof FighterSquadron) {
            String msg_fighter = Messages.getString("BoardView1.Tooltip.Fighters");
            f += msg_fighter + ":";
        } else {
            String msg_carriedunits = Messages.getString("BoardView1.Tooltip.CarriedUnits");
            f += msg_carriedunits + ":";
        }
        for (Entity carried: entity.getLoadedUnits()) {
            f += "<BR>&nbsp;&nbsp;" + carried.getShortNameRaw();
            if (PreferenceManager.getClientPreferences().getShowUnitId()) {
                f += " [" + carried.getId() + "]";
            }
        }

        result = guiScaledFontHTML() + f + "</FONT>";
        return new StringBuilder().append(result);
    }

    /** Returns the full force chain the entity is in as one text line. */
    private static StringBuilder forceEntry(Entity entity, Player localPlayer) {
        String result = "";
        String f = "";

        if (entity.partOfForce()) {
            // Get the my / ally / enemy color and desaturate it
            Color color = GUIP.getEnemyUnitColor();
            if (entity.getOwnerId() == localPlayer.getId()) {
                color = GUIP.getMyUnitColor();
            } else if (!localPlayer.isEnemyOf(entity.getOwner())) {
                color = GUIP.getAllyUnitColor();
            }
            color = addGray(color, 128).brighter();
            f = "<BR>";
            var forceChain = entity.getGame().getForces().forceChain(entity);
            for (int i = forceChain.size() - 1; i >= 0; i--) {
                f += forceChain.get(i).getName();
                f += i != 0 ? ", " : "";
            }
            result = guiScaledFontHTML(color) + f + "</FONT>";
        }

        return new StringBuilder().append(result);
    }
    
    /** Returns an overview of the C3 system the unit is in. */
    private static StringBuilder c3Info(Entity entity) {
        String result = "";
        String f = "";

        List<String> members = entity.getGame().getEntitiesVector().stream()
                .filter(e -> e.onSameC3NetworkAs(entity))
                .sorted(Comparator.comparingInt(Entity::getId))
                .map(e -> c3UnitName(e, entity)).collect(Collectors.toList());
        if (members.size() > 1) {
            if (entity.hasNhC3()) {
                String msg_c3i = Messages.getString("BoardView1.Tooltip.C3i");
                String msg_nc3 = Messages.getString("BoardView1.Tooltip.NC3");
                f = entity.hasC3i() ? msg_c3i : msg_nc3;
            } else {
                String msg_c3 = Messages.getString("BoardView1.Tooltip.C3");
                f = msg_c3;
            }
            String msg_network = Messages.getString("BoardView1.Tooltip.Network");
            f += " " + msg_network + ": <BR>&nbsp;&nbsp;";
            f += String.join("<BR>&nbsp;&nbsp;", members);
            f += "<BR>";
        }

        result = guiScaledFontHTML(uiC3Color(), -0.2f) + f + "</FONT>";

        return new StringBuilder().append(result);
    }

    private static String c3UnitName(Entity c3member, Entity entity) {
        String result = "";
        String it = "";
        String f = "";
        String tmp = "";

        f = " [" + c3member.getId() + "] ";

        if (c3member.isC3CompanyCommander()) {
            String msg_c3cc = Messages.getString("BoardView1.Tooltip.C3CC");
            it += msg_c3cc + " ";
        } else if (c3member.hasC3M()) {
            String msg_c3m = Messages.getString("BoardView1.Tooltip.C3M");
            it += msg_c3m + " ";
        }

        f += "<I>" + it + "</I>";
        result += guiScaledFontHTML(uiGray(), -0.2f) + f + "</FONT>";
        result += c3member.getShortNameRaw();

        String msg_thisunit = Messages.getString("BoardView1.Tooltip.ThisUnit");
        it = " (" + msg_thisunit + ")";
        tmp = "<I>" + it + "</I>";
        f = c3member.equals(entity) ? tmp : "";
        result += guiScaledFontHTML(uiGray(), -0.2f) + f + "</FONT>";

        return result;
    }
    
    /** Helper method to shorten repetitive calls. */
    private static StringBuilder addToTT(String tipName, boolean startBR, Object... ttO) {
        StringBuilder result = new StringBuilder();
        if (startBR == BR) {
            result.append("<BR>");
        }
        if (ttO != null) {
            result.append(Messages.getString("BoardView1.Tooltip." + tipName, ttO));
        } else {
            result.append(Messages.getString("BoardView1.Tooltip." + tipName));
        }
        return result;
    }
    
    /** Returns true when Hot-Loading LRMs is on. */
    static boolean isHotLoadActive(Game game) {
        return game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD);
    }
    
    /** Returns true when Hot-Loading LRMs is on. */
    static boolean isRapidFireActive(Game game) {
        return game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_BURST);
    }

}

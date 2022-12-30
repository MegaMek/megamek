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

import static megamek.client.ui.Messages.getString;
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
            return new StringBuilder(Messages.getString("BoardView1.sensorReturn"));
        }

        StringBuilder result = new StringBuilder();
        result.append("<TABLE BORDER=0 BGCOLOR=" + BGCOLOR + " width=100%><TR><TD>");
        Game game = entity.getGame();

        // Unit Chassis and Player
        Player owner = game.getPlayer(entity.getOwnerId());
        result.append(guiScaledFontHTML(entity.getOwner().getColour().getColour()));
        String clanStr = entity.isClan() && !entity.isMixedTech() ? " [Clan] " : "";
        result.append(entity.getChassis()).append(clanStr);
        result.append(" (").append((int)entity.getWeight()).append("t)");
        result.append("&nbsp;&nbsp;" + entity.getEntityTypeName(entity.getEntityType()));
        result.append("<BR>").append(owner.getName());
        result.append(UIUtil.guiScaledFontHTML(UIUtil.uiGray()));
        result.append(MessageFormat.format(" [ID: {0}] </FONT>", entity.getId()));
        result.append("</FONT>");

        // Pilot; in the lounge the pilot is separate so don't add it there
        if (details && (mapSettings != null)) {
            result.append(deploymentWarnings(entity, localPlayer, mapSettings));
            result.append("<BR>");
        } else {
            if (pilotInfo) {
                result.append(forceEntry(entity, localPlayer));
            }
            result.append(inGameValues(entity, localPlayer));
            if (pilotInfo) {
                result.append(PilotToolTip.getPilotTipShort(entity, GUIP.getBoolean(GUIPreferences.SHOW_PILOT_PORTRAIT_TT)));
            } else {
                result.append("<BR>");
            }
        }
        
        // An empty squadron should not show any info
        if (entity instanceof FighterSquadron && entity.getLoadedUnits().isEmpty()) {
            return result;
        }

        // Static entity values like move capability
        result.append(guiScaledFontHTML());
        result.append(entityValues(entity));
        result.append("</FONT>");

        // Status bar visual representation of armor and IS 
        if (GUIP.getBoolean(GUIPreferences.SHOW_ARMOR_MINIVIS_TT)) {
            result.append(scaledHTMLSpacer(3));
            result.append(addArmorMiniVisToTT(entity));
        }

        // Weapon List
        if (GUIP.getBoolean(GUIPreferences.SHOW_WPS_IN_TT)) {
            result.append(scaledHTMLSpacer(3));
            result.append(guiScaledFontHTML());
            result.append(weaponList(entity));
            result.append(ecmInfo(entity));
            result.append("</FONT>");
        }

        // Bomb List
        result.append(bombList(entity));

        // StratOps quirks, chassis and weapon
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            result.append(scaledHTMLSpacer(3));
            result.append(guiScaledFontHTML(uiQuirksColor(), TT_SMALLFONT_DELTA));
            String quirksList = getOptionList(entity.getQuirks().getGroups(), entity::countQuirks, details);
            if (!quirksList.isEmpty()) {
                result.append(quirksList);
            }
            for (Mounted weapon: entity.getWeaponList()) {
                String wpQuirksList = getOptionList(weapon.getQuirks().getGroups(), 
                        grp -> weapon.countQuirks(), (e) -> weapon.getDesc(), details);
                if (!wpQuirksList.isEmpty()) {
                    // Line break after weapon name not useful here
                    result.append(wpQuirksList.replace(":</I><BR>", ":</I>"));
                }
            }
            result.append("</FONT>");
        }

        // Partial repairs
        String partialList = getOptionList(entity.getPartialRepairs().getGroups(), 
                grp -> entity.countPartialRepairs(), details);
        if (!partialList.isEmpty()) {
            result.append(scaledHTMLSpacer(3));
            result.append(guiScaledFontHTML(uiPartialRepairColor(), TT_SMALLFONT_DELTA));
            result.append(partialList);
            result.append("</FONT>");
        }
        
        if (!entity.getLoadedUnits().isEmpty()) {
            result.append(scaledHTMLSpacer(3));
            result.append(carriedUnits(entity));
        }
        
        if (details && entity.hasAnyC3System()) {
            result.append(scaledHTMLSpacer(3));
            result.append(c3Info(entity));
        }
        result.append("</TD></TR></TABLE>");

        return result;
    }

    private static boolean hideArmorLocation(Entity entity, int location) {
        return ((entity.getOArmor(location) <= 0) && (entity.getOInternal(location) <= 0) && !entity.hasRearArmor(location))
                || (entity.isConventionalInfantry() && (location != Infantry.LOC_INFANTRY));
    }

    private static String locationHeader(Entity entity, int location) {
        return entity.isConventionalInfantry() ?
                ((Infantry) entity).getShootingStrength() + " Active Troopers" : entity.getLocationAbbr(location);
    }

    /** Returns the graphical Armor representation. */
    private static StringBuilder addArmorMiniVisToTT(Entity entity) {
        String armorChar = GUIP.getString(GUIPreferences.ADVANCED_ARMORMINI_ARMOR_CHAR);
        if (entity.isCapitalScale()) {
            armorChar = GUIP.getString(GUIPreferences.ADVANCED_ARMORMINI_CAP_ARMOR_CHAR);
        }
        String internalChar = GUIP.getString(GUIPreferences.ADVANCED_ARMORMINI_IS_CHAR);
        StringBuilder result = new StringBuilder();
        result.append("<TABLE CELLSPACING=0 CELLPADDING=0>\n<TBODY>\n");
        for (int loc = 0 ; loc < entity.locations(); loc++) {
            // do not show locations that do not support/have armor/internals like HULL on Aero
            if (hideArmorLocation(entity, loc)) {
                continue;
            }

            boolean locDestroyed = (entity.getInternal(loc) == IArmorState.ARMOR_DOOMED || entity.getInternal(loc) == IArmorState.ARMOR_DESTROYED);
            result.append("<TR>\n<TD>\n");
            if (locDestroyed) {
                // Destroyed location
                result.append("</TD>\n<TD>\n</TD>\n<TD>\n");
                result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                result.append("&nbsp;&nbsp;" + locationHeader(entity, loc) + ":&nbsp;");
                result.append("</FONT>\n</TD>\n<TD>\n");
                result.append(destroyedLocBar(entity.getOArmor(loc, true)));
                result.append("</TD>\n<TD>\n");
            } else {
                // Rear armor
                if (entity.hasRearArmor(loc)) {
                    result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                    result.append("&nbsp;&nbsp;" + locationHeader(entity, loc) + "R:&nbsp;");
                    result.append("</FONT>\n</TD>\n<TD>\n");
                    result.append(intactLocBar(entity.getOArmor(loc, true), entity.getArmor(loc, true), armorChar));
                    result.append("</TD>\n<TD>\n");
                } else {
                    // No rear armor: empty table cells instead
                    // At small font sizes, writing one character at the correct font size is 
                    // necessary to prevent the table rows from being spaced non-beautifully
                    result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA) + "&nbsp;</FONT>\n</TD>\n<TD>\n");
                    result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA) + "&nbsp;</FONT>\n</TD>\n<TD>\n");
                }
                // Front armor
                result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                result.append("&nbsp;&nbsp;" + locationHeader(entity, loc) + ":&nbsp;");
                result.append("</FONT>\n</TD>\n<TD>\n");
                result.append(intactLocBar(entity.getOInternal(loc), entity.getInternal(loc), internalChar));
                result.append(intactLocBar(entity.getOArmor(loc), entity.getArmor(loc), armorChar));
                result.append("</TD>\n<TD>\n");
            }

            int hits = 0;
            int good = 0;
            switch (loc) {
                case 0:
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, loc);
                    if ((good + hits) > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;S:&nbsp;");
                        result.append("</FONT>\n<");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, loc);
                    if ((good + hits) > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;L:&nbsp;");
                        result.append("</FONT>\n<");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    result.append("</TD>\n<TD>\n");
                    break;
                case 1:
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, loc);
                    if ((good + hits)  > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;E:&nbsp;");
                        result.append("</FONT>\n<");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, loc);
                    if ((good + hits)  > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;G:&nbsp;");
                        result.append("</FONT>\n");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, loc);
                    if ((good + hits) > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;S:&nbsp;");
                        result.append("</FONT>\n<");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, loc);
                    if ((good + hits) > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;L:&nbsp;");
                        result.append("</FONT>\n<");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    result.append("</TD>\n<TD>\n");
                    break;
                case 2:
                case 3:
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, loc);
                    if ((good + hits)  > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;E:&nbsp;");
                        result.append("</FONT>\n");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, loc);
                    if ((good + hits) > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;L:&nbsp;");
                        result.append("</FONT>\n<");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    break;
                case 4:
                case 5:
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER, loc);
                    if ((good + hits)  > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;S:&nbsp;");
                        result.append("</FONT>\n");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_ARM, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_ARM, loc);
                    if ((good + hits)  > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;UA:&nbsp;");
                        result.append("</FONT>\n");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_ARM, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_ARM, loc);
                    if ((good + hits)  > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;LA:&nbsp;");
                        result.append("</FONT>\n");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HAND, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HAND, loc);
                    if ((good + hits)  > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;H:&nbsp;");
                        result.append("</FONT>\n");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc);
                    if ((good + hits)  > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;H:&nbsp;");
                        result.append("</FONT>\n");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, loc);
                    if ((good + hits)  > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;UL:&nbsp;");
                        result.append("</FONT>\n");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, loc);
                    if ((good + hits)  > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;LL:&nbsp;");
                        result.append("</FONT>\n");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, loc);
                    if ((good + hits)  > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;F:&nbsp;");
                        result.append("</FONT>\n");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    result.append("</TD>\n<TD>\n");
                    result.append("</TD>\n<TD>\n");
                    break;
                case 6:
                case 7:
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc);
                    if ((good + hits)  > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;H:&nbsp;");
                        result.append("</FONT>\n");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, loc);
                    if ((good + hits)  > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;UL:&nbsp;");
                        result.append("</FONT>\n");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, loc);
                    if ((good + hits)  > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;LL:&nbsp;");
                        result.append("</FONT>\n");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    good = entity.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, loc);
                    hits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, loc);
                    if ((good + hits)  > 0) {
                        result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                        result.append("&nbsp;&nbsp;F:&nbsp;");
                        result.append("</FONT>\n");
                        result.append(systemBar(good, hits, locDestroyed));
                        result.append("</FONT>\n");
                    }
                    result.append("</TD>\n<TD>\n");
                    break;
            }
            result.append("</TD>\n</TR>\n\n");
        }
        result.append("</TBODY>\n</TABLE>\n");
        return result;
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
        
        StringBuilder result = new StringBuilder();
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
            result.append(guiScaledFontHTML(colorIntact, TT_SMALLFONT_DELTA));
            if (numIntact > 15 && numIntact + numDmgd > 30) {
                int tensIntact = (numIntact - 1) / 10;
                result.append(dChar + "x" + tensIntact * 10 + " ");
                result.append(repeat(dChar, numIntact - 10 * tensIntact) + "</FONT>\n");
            } else {
                result.append(repeat(dChar, numIntact) + "</FONT>\n");
            }
        }
        if (numPartial > 0) {
            result.append(guiScaledFontHTML(colorPartialDmg, TT_SMALLFONT_DELTA));
            result.append(repeat(dChar, numPartial) + "</FONT>\n");
        }
        if (numDmgd > 0) {
            result.append(guiScaledFontHTML(colorDamaged, TT_SMALLFONT_DELTA));
            if (numDmgd > 15 && numIntact + numDmgd > 30) {
                int tensDmgd = (numDmgd - 1) / 10;
                result.append(dChar + "x" + tensDmgd * 10 + " ");
                result.append(repeat(dChar, numDmgd - 10 * tensDmgd) + "</FONT>\n");
            } else {
                result.append(repeat(dChar, numDmgd) + "</FONT>\n");
            }
        }
        return result;
    }

    private static StringBuilder systemBar(int good, int bad, boolean destroyed) {
        // Internal Structure can be zero, e.g. in Aero
        if ((good + bad) == 0) {
            return new StringBuilder("");
        }

        StringBuilder result = new StringBuilder();
        Color colorIntact = GUIP.getColor(GUIPreferences.ADVANCED_ARMORMINI_COLOR_INTACT);
        Color colorDamaged = GUIP.getColor(GUIPreferences.ADVANCED_ARMORMINI_COLOR_DAMAGED);
        String dChar =  GUIP.getString(GUIPreferences.ADVANCED_ARMORMINI_DESTROYED_CHAR);
        String iChar = GUIP.getString(GUIPreferences.ADVANCED_ARMORMINI_IS_CHAR);

        if (good > 0)  {
            if (!destroyed) {
                result.append(guiScaledFontHTML(colorIntact, TT_SMALLFONT_DELTA));
                result.append(repeat(iChar, good));
                result.append("</FONT>\n");
            } else {
                result.append(guiScaledFontHTML(colorDamaged, TT_SMALLFONT_DELTA));
                result.append(repeat(dChar, good));
                result.append("</FONT>\n");
            }
        }
        if (bad > 0) {
            result.append(guiScaledFontHTML(colorDamaged, TT_SMALLFONT_DELTA));
            result.append(repeat(dChar, bad));
            result.append("</FONT>\n");
        }
        return result;
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
            weapDesc = weapDesc.replace("[Clan]", "").replace("(Clan)", "").trim();
            if (wpInfos.containsKey(weapDesc)) {
                currentWp = wpInfos.get(weapDesc);
                currentWp.count++;
                wpInfos.put(weapDesc, currentWp);
                if (!curWp.isDestroyed() && wpInfos.containsKey(curWp.getName() + "Ammo")) {
                    WeaponInfo currAmmo = wpInfos.get(curWp.getName() + "Ammo");
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
                    
                    if (wpInfos.containsKey(curWp.getName() + "Ammo")) {
                        if (!curWp.isDestroyed()) {
                            WeaponInfo currAmmo = wpInfos.get(curWp.getName() + "Ammo");
                            currAmmo.ammoActiveWeaponCount++;
                        }
                    } else {
                        WeaponInfo currAmmo = new WeaponInfo();
                        currAmmo.sortString = curWp.getName() + "ZZ"; // Sort ammo after the weapons
                        currAmmo.ammoActiveWeaponCount = curWp.isDestroyed() ? 0 : 1;
                        for (Mounted amounted : entity.getAmmo()) {
                            boolean canSwitchToAmmo = AmmoType.canSwitchToAmmo(curWp, (AmmoType) amounted.getType());
                            if (canSwitchToAmmo && !amounted.isDumping()) {
                                String name = amounted.getName().replace("Anti-Personnel", "AP")
                                        .replace("Ammo", "").replace("[IS]", "").replace("[Clan]", "")
                                        .replace("(Clan)", "").replace("[Half]", "").replace("Half", "")
                                        .replace(curWp.getDesc(), "").trim();
                                if (name.isBlank()) {
                                    name = "Standard";
                                }

                                if (amounted.isHotLoaded()) {
                                    name += " (Hot-Loaded)";
                                }
                                int count = amounted.getUsableShotsLeft();
                                count += currAmmo.ammos.getOrDefault(name, 0);
                                currAmmo.ammos.put(name, count);
                            }
                        }
                        wpInfos.put(curWp.getName() + "Ammo", currAmmo);
                    }
                }
            }

        }
        
        // Print to Tooltip
        StringBuilder result = new StringBuilder();
        boolean subsequentLine = false; 
        // Display sorted by weapon name
        var wps = new ArrayList<>(wpInfos.values());
        wps.sort(Comparator.comparing(w -> w.sortString));
        int totalWeaponCount = wpInfos.values().stream().filter(i -> i.ammos.isEmpty()).mapToInt(wp -> wp.count).sum();
        boolean hasMultiples = wpInfos.values().stream().mapToInt(wp -> wp.count).anyMatch(c -> c > 1);
        result.append("<TABLE CELLSPACING=0 CELLPADDING=0 " + guiScaledFontHTML(uiTTWeaponColor()).substring(1));
        for (WeaponInfo currentEquip : wps) {
            // This WeaponInfo is ammo
            if (!currentEquip.ammos.isEmpty()) {
                result.append(createAmmoEntry(currentEquip));
            } else {
                // This WeaponInfo is a weapon
                // Check if weapon is destroyed, text gray and strikethrough if so, remove the "x "/"*"
                boolean isDestroyed = false;
                String nameStr = currentEquip.name;
                if (nameStr == null) {
                    nameStr = "NULL Weapon Name!"; // Happens with Vehicle Flamers!
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

                result.append(guiScaledFontHTML(uiTTWeaponColor()));
                String techBase = "";
                if (entity.isMixedTech()) {
                    techBase = currentEquip.isClan ? Messages.getString("BoardView1.Tooltip.Clan") :
                            Messages.getString("BoardView1.Tooltip.IS");
                    techBase += " ";
                }
                String destStr = isDestroyed ? "<S>" : "";

                if (totalWeaponCount > 5 && hasMultiples) {
                    // more than 5 weapons: group and list with a multiplier "4 x Small Laser"
                    result.append("<TR><TD>");
                    if (currentEquip.count > 1) {
                        result.append(currentEquip.count + " x ");
                    }
                    result.append("</TD><TD>");
                    result.append(addToTT("Weapon", false, currentEquip.count, techBase, nameStr, destStr));
                    result.append(weaponModifier(isDestroyed, currentEquip));
                    result.append("</TD></TR>");
                } else {
                    // few weapons: list each weapon separately
                    for (int i = 0; i < currentEquip.count; i++) {
                        result.append("<TR><TD></TD><TD>");
                        result.append(addToTT("Weapon", false, currentEquip.count, techBase, nameStr, destStr));
                        result.append(weaponModifier(isDestroyed, currentEquip));
                        result.append("</TD></TR>");
                    }
                }
            }
        }
        result.append("</TABLE>");
        return result;
    }

    private static StringBuilder bombList(Entity entity) {
        StringBuilder result = new StringBuilder();

        if (entity.isBomber()) {
            int[] loadout = { };

            if (entity.getGame().getPhase().isLounge()) {
                if (entity instanceof Aero) {
                    loadout = ((Aero) entity).getBombChoices();
                } else if (entity instanceof LandAirMech) {
                    loadout = ((LandAirMech) entity).getBombChoices();
                } else if (entity instanceof VTOL) {
                    loadout = ((VTOL) entity).getBombChoices();
                } else {
                    return result;
                }
            } else {
                loadout = entity.getBombLoadout();
            }
            result.append("<TABLE CELLSPACING=0 CELLPADDING=0 " + guiScaledFontHTML(uiTTWeaponColor()).substring(1) + "\n");
            for (int i = 0; i < loadout.length; i++) {
                int count = loadout[i];

                if (count > 0) {
                    result.append("<TR>\n<TD>\n");
                    result.append(count + "\n");
                    result.append("</TD>\n<TD>\n");
                    result.append("&nbsp;x&nbsp;\n");
                    result.append("</TD>\n<TD>\n");
                    result.append(BombType.getBombName(i) + "\n");
                    result.append("</TD>\n</TR>\n");
                }
            }
            result.append("</TABLE>\n");
        }

        return result;
    }

    private static String weaponModifier(boolean isDestroyed, WeaponInfo currentEquip) {
        if (isDestroyed) {
            // Ends the strikethrough that is added for destroyed weapons
            return "</S>";
        } else if (currentEquip.isHotloaded) {
            return " \u22EF<I> Hot-loaded</I>";
        } else if (currentEquip.isRapidFire) {
            return " \u22EF<I> Rapid-fire</I>";
        }
        return "";
    }
    
    /** Returns the ammo line(s) for the ammo of one weapon type. */
    private static StringBuilder createAmmoEntry(WeaponInfo ammoInfo) {
        StringBuilder result = new StringBuilder();
        int totalAmmo = ammoInfo.ammos.values().stream().mapToInt(n -> n).sum();
        if (totalAmmo == 0 && ammoInfo.ammoActiveWeaponCount > 0) {
            result.append(guiScaledFontHTML(uiYellow(), -0.2f));
            result.append("<TR><TD></TD><TD>");
            result.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Out of Ammo!");
            result.append("</TD></TR>");
        } else {
            for (Entry<String, Integer> ammo: ammoInfo.ammos.entrySet()) {
                String ammoName = ammo.getKey().equals("Standard") && ammoInfo.ammos.size() == 1 ? "" : ammo.getKey() + ": ";
                // No entry when no ammo of this type left but some other type left
                if (ammo.getValue() == 0) {
                    continue;
                }
                result.append("<TR><TD></TD><TD>");
                result.append(guiScaledFontHTML(uiGreen(), -0.2f));
                result.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
                if (ammoInfo.ammoActiveWeaponCount > 1) { 
                    // Remaining ammo and multiple weapons using it
                    result.append(ammoName);
                    result.append(ammo.getValue() / ammoInfo.ammoActiveWeaponCount).append(" turns");
                    result.append(" (" + ammo.getValue() + " shots)");
                } else { 
                    // Remaining ammo and only one weapon using it
                    result.append(ammoName).append(ammo.getValue()).append(" shots");
                }
                result.append("</TD></TR>");
            }
        }
        return result;
    }

    /** Returns a line showing ECM / ECCM. */
    private static StringBuilder ecmInfo(Entity entity) {
        StringBuilder result = new StringBuilder();
        result.append(guiScaledFontHTML());
        if (entity.hasActiveECM()) {
            result.append(ECM_SIGN).append(" ");
            result.append(Messages.getString("BoardView1.ecmSource"));
        }
        if (entity.hasActiveECCM()) {
            result.append(ECM_SIGN).append(" ");
            result.append(Messages.getString("BoardView1.eccmSource"));
        }
        result.append("</FONT>");
        return result;
    }

    /** Returns values that only are relevant when in-game such as heat. */
    private static StringBuilder inGameValues(Entity entity, Player localPlayer) {
        StringBuilder result = new StringBuilder();
        Game game = entity.getGame();
        boolean isGunEmplacement = entity instanceof GunEmplacement;
        
        // Coloring and italic to make these transient entries stand out
        result.append(guiScaledFontHTML(uiLightViolet()) + "<I>");
        
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
            result.append(addToTT("BV", BR, currentBV, initialBV, percentage));
        }

        String damageLevel;
        switch (entity.getDamageLevel()) {
            case Entity.DMG_CRIPPLED:
                damageLevel = guiScaledFontHTML(GUIP.getWarningColor());
                damageLevel += "&nbsp;&nbsp;CRIPPLED";
                damageLevel += "</FONT>";
                break;
            case Entity.DMG_HEAVY:
                damageLevel = guiScaledFontHTML(GUIP.getWarningColor());
                damageLevel += "&nbsp;&nbsp;HEAVY DMG";
                damageLevel += "</FONT>";
                break;
            case Entity.DMG_MODERATE:
                damageLevel = "&nbsp;&nbsp;MODERATE DMG";
                break;
            case Entity.DMG_LIGHT:
                damageLevel = "&nbsp;&nbsp;LIGHT DMG";
                break;
            default:
                damageLevel = "&nbsp;&nbsp;UNDAMAGED";
        }
        result.append(damageLevel);

        // Actual Movement
        if (!isGunEmplacement) {
            // "Has not yet moved" only during movement phase
            if (!entity.isDone() && game.getPhase().isMovement()) {
                result.append(addToTT("NotYetMoved", BR));
            } else if ((entity.isDone() && game.getPhase().isMovement())
                    || game.getPhase().isFiring()) {
                result.append(guiScaledFontHTML(GUIP.getColorForMovement(entity.moved)));
                int tmm = Compute.getTargetMovementModifier(game, entity.getId()).getValue();
                if (entity.moved == EntityMovementType.MOVE_NONE) {
                    result.append(addToTT("NoMove", BR, tmm));
                } else {
                    result.append(addToTT("MovementF", BR, entity.getMovementString(entity.moved),
                            entity.delta_distance, tmm));
                }
                // Special Moves
                if (entity.isEvading()) { 
                    result.append(addToTT("Evade", NOBR));
                }

                if ((entity instanceof Infantry) && ((Infantry) entity).isTakingCover()) { 
                    result.append(addToTT("TakingCover", NOBR));
                }

                if (entity.isCharging()) { 
                    result.append(addToTT("Charging", BR));
                }

                if (entity.isMakingDfa()) { 
                    result.append(addToTT("DFA", NOBR));
                }

                if (entity.isUnjammingRAC()) {
                    result.append("<BR>");
                    result.append("Unjamming RAC");
                    if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_UNJAM_UAC)) {
                        result.append("/AC");
                    }
                }
            }
        }

        if (entity.isAero()) {
            // Velocity, Altitude, Elevation, Fuel
            result.append(guiScaledFontHTML(uiLightViolet()));
            IAero aero = (IAero) entity;
            result.append(addToTT("AeroVelAltFuel", BR, aero.getCurrentVelocity(), aero.getAltitude(), aero.getFuel()));
            result.append("</FONT>");
        } else if (entity.getElevation() != 0) {
            // Elevation only
            result.append(guiScaledFontHTML(uiLightViolet()));
            result.append(addToTT("Elev", BR, entity.getElevation()));
            result.append("</FONT>");
        }

        // Heat, not shown for units with 999 heat sinks (vehicles)
        if (entity.getHeatCapacity() != 999) {
            int heat = entity.heat;
            result.append(guiScaledFontHTML(GUIP.getColorForHeat(heat)));
            if (heat == 0) {
                result.append(addToTT("Heat0", BR));
            } else {
                result.append(addToTT("Heat", BR, heat));
            }
            result.append(" / "+entity.getHeatCapacity());
            result.append("</FONT>");
        }

        // Gun Emplacement Status
        if (isGunEmplacement) {
            GunEmplacement emp = (GunEmplacement) entity; 
            if (emp.isTurret() && emp.isTurretLocked(emp.getLocTurret())) {
                result.append(guiScaledFontHTML(GUIP.getWarningColor()));
                result.append(addToTT("TurretLocked", BR));
                result.append("</FONT>");
            }
        }

        // Unit Immobile
        if (!isGunEmplacement && entity.isImmobile()) {
            result.append(guiScaledFontHTML(GUIP.getWarningColor()));
            result.append(addToTT("Immobile", BR));
            result.append("</FONT>");
        }

        // Unit Prone
        if (!isGunEmplacement && entity.isProne()) {
            result.append(guiScaledFontHTML(GUIP.getWarningColor()));
            result.append(addToTT("Prone", BR));
            result.append("</FONT>");
        }


        if (!entity.getHiddenActivationPhase().isUnknown()) {
            result.append(addToTT("HiddenActivating", BR, entity.getHiddenActivationPhase().toString()));
        } else if (entity.isHidden()) {
            result.append(addToTT("Hidden", BR));
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
            result.append(addToTT("Swarmed", BR,
                    (swarmAttacker == null) ? "ERROR" : swarmAttacker.getDisplayName()));
        }

        // Spotting
        if (entity.isSpotting() && game.hasEntity(entity.getSpotTargetId())) {
            result.append(addToTT("Spotting", BR, game.getEntity(entity.getSpotTargetId()).getDisplayName()));
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
                            tmpStr = "Someone";
                            tempList.append(tmpStr);
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
                            tmpStr ="";
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
                result.append(addToTT("SeenBy", BR, tempList.toString()));
            }            
        }

        // If sensors, display what sensors this unit is using
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_SENSORS)
                || game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS)) {
            result.append(addToTT("Sensors", BR, entity.getSensorDesc(), Compute.getMaxVisualRange(entity, false)));
        }

        if (entity.hasAnyTypeNarcPodsAttached()) {
            result.append(guiScaledFontHTML(uiLightRed()));
            result.append(addToTT(entity.hasNarcPodsAttached() ? "Narced" : "INarced", BR));
            result.append("</FONT>");
        }
        
        // Towing
        if (!entity.getAllTowedUnits().isEmpty()) {
            String unitList = entity.getAllTowedUnits().stream()
                    .map(id -> entity.getGame().getEntity(id).getShortName())
                    .collect(Collectors.joining(", "));
            if (unitList.length() > 1) {
                result.append(addToTT("Towing", BR, unitList));
            }
        }
        result.append("</I></FONT>");
        result.append("</I></FONT>");
        return result;
    }
    
    /** Returns unit values that are relevant in-game and in the lobby such as movement ability. */
    private static StringBuilder entityValues(Entity entity) {
        StringBuilder result = new StringBuilder();
        boolean isGunEmplacement = entity instanceof GunEmplacement;
        int hipHits = 0;
        int actuatorHits = 0;
        int legsDestroyed = 0;

        if ((!(entity instanceof Infantry)) && (!(entity instanceof FighterSquadron)) && (!(entity instanceof GunEmplacement))) {
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
        }

        int jumpJet = 0;
        int jumpJetDistroyed = 0;
        int jumpBooster = 0;
        int jumpBoosterDistroyed = 0;

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
            }
        }

        // Unit movement ability
        if (!isGunEmplacement) {
            int walkMP = entity.getOriginalWalkMP();
            int runMP = entity.getOriginalRunMP();
            int jumpMP = entity.getOriginalJumpMP();
            result.append(addToTT("Movement", NOBR , walkMP,  runMP));

            if (jumpMP > 0) {
                result.append("/" + jumpMP);
            }

            int walkMPModified = entity.getWalkMP(true, false,false);
            int runMPModified = entity.getRunMP(true, false, false);
            int jumpMPModified = entity.getJumpMP(true);

            if ((walkMP != walkMPModified) || (runMP != runMPModified) || (jumpMP != jumpMPModified)) {
                result.append(DOT_SPACER);
                result.append(walkMPModified + "/" + runMPModified);
                if (jumpMPModified > 0) {
                    result.append("/" + jumpMPModified);
                }
                if (entity.getGame().getPlanetaryConditions().getGravity()  != 1.0) {
                    result.append(DOT_SPACER + entity.getGame().getPlanetaryConditions().getGravity() + "g");
                }
                int walkMPNoHeat = entity.getWalkMP(true, true,false);
                int runMPNoHeat = entity.getRunMP(true, true, false);
                if ((walkMPNoHeat != walkMPModified) || (runMPNoHeat != runMPModified)) {
                    result.append(DOT_SPACER + guiScaledFontHTML(GUIP.getWarningColor()) + "\uD83D\uDD25</FONT>");
                }
            }

            if (entity instanceof Tank) {
                result.append(DOT_SPACER + entity.getMovementModeAsString());
            }

            int weatherMod = entity.getGame().getPlanetaryConditions().getMovementMods(entity);

            if (weatherMod != 0) {
                result.append(DOT_SPACER + guiScaledFontHTML(GUIP.getWarningColor()) + "\u2602" + "</FONT>");
            }

            if ((legsDestroyed > 0) || (hipHits > 0) || (actuatorHits > 0) || (jumpJetDistroyed > 0) || (jumpBoosterDistroyed > 0) || (entity.isImmobile()) || (entity.isGyroDestroyed())) {
                result.append(DOT_SPACER + guiScaledFontHTML(GUIP.getWarningColor()) + "\uD83D\uDD27" + "</FONT>");
            }
        }
        
        // Infantry specialization like SCUBA
        if (entity instanceof Infantry) {
            Infantry inf = (Infantry) entity;
            int spec = inf.getSpecializations();
            if (spec > 0) {
                result.append(addToTT("InfSpec", BR, Infantry.getSpecializationName(spec)));
            }
        }

        if ((jumpJetDistroyed > 0) || (jumpBoosterDistroyed > 0)) {
            result.append("<BR>");
            String jj = "";
            if (jumpJetDistroyed > 0) {
                jj = "Jump Jets " + (jumpJet - jumpJetDistroyed) + "/" + jumpJet;
            }
            if (jumpBoosterDistroyed > 0)  {
                jj += "; Jump Booster " + (jumpBooster - jumpBoosterDistroyed) + "/" + jumpBooster;
            }
            if (jj.startsWith(";")) {
                jj = jj.substring(2);
            }
            result.append(jj);
        }

        // Armor and Internals
        if (entity.isConventionalInfantry()) {
            result.append("<BR>");
        } else if (!isGunEmplacement) {
            String armorType = TROView.formatArmorType(entity, true).replace("UNKNOWN", "");
            if (!armorType.isBlank()) {
                armorType = (entity.isCapitalScale() ? getString("BoardView1.Tooltip.ArmorCapital") + " " : "") + armorType;
                armorType = " (" + armorType + ")";
            }
            String armorStr = " " + entity.getTotalArmor() + armorType;
            result.append(addToTT("ArmorInternals", BR, armorStr, entity.getTotalInternal()));
        }
        return result;
    }
    
    /** Returns warnings about problems that should be solved before deploying. */
    private static StringBuilder deploymentWarnings(Entity entity, Player localPlayer,
                                                    MapSettings mapSettings) {
        StringBuilder result = new StringBuilder();
        // Critical (red) warnings
        result.append(guiScaledFontHTML(GUIP.getWarningColor()));
        if (entity.getGame().getPlanetaryConditions().whyDoomed(entity, entity.getGame()) != null) {
            result.append("<BR>Cannot survive " + entity.getGame().getPlanetaryConditions().whyDoomed(entity, entity.getGame()));
        }
        if (entity.doomedInAtmosphere() && mapSettings.getMedium() == MapSettings.MEDIUM_ATMOSPHERE) {
            result.append("<BR>Cannot survive on a low/high atmosphere map!");
        }
        if (entity.doomedOnGround() && mapSettings.getMedium() == MapSettings.MEDIUM_GROUND) {
            result.append("<BR>Cannot survive on a ground map!");
        }
        if (entity.doomedInSpace() && mapSettings.getMedium() == MapSettings.MEDIUM_SPACE) {
            result.append("<BR>Cannot survive in space!");
        }
        result.append("</FONT>");
        
        // Non-critical (yellow) warnings
        result.append(guiScaledFontHTML(uiYellow())); 
        if (((entity.hasC3i() || entity.hasNavalC3()) && (entity.calculateFreeC3Nodes() == 5))
                || ((entity.getC3Master() == null) && entity.hasC3S())
                || (entity.hasNovaCEWS() && (entity.calculateFreeC3Nodes() == 2))) {
            result.append("<BR>Unconnected C3 Computer");
        }
        
        // Non-critical (yellow) warnings
        if (entity instanceof FighterSquadron && entity.getLoadedUnits().isEmpty()) {
            result.append("<BR>This Fighter Squadron is empty");
        }
        result.append("</FONT>");
        return result;
    }
    
    /** Returns a list of units loaded onto this unit. */
    private static StringBuilder carriedUnits(Entity entity) {
        StringBuilder result = new StringBuilder();
        
        result.append(guiScaledFontHTML());
        if (entity instanceof FighterSquadron) {
            result.append("Fighters:");
        } else {
            result.append("Carried Units:");
        }
        for (Entity carried: entity.getLoadedUnits()) {
            result.append("<BR>&nbsp;&nbsp;").append(carried.getShortNameRaw());
            if (PreferenceManager.getClientPreferences().getShowUnitId()) {
                result.append(" [" + carried.getId() + "]");
            }

        }
        result.append("</FONT>");
        return result;
    }

    /** Returns the full force chain the entity is in as one text line. */
    private static StringBuilder forceEntry(Entity entity, Player localPlayer) {
        StringBuilder result = new StringBuilder();

        if (entity.partOfForce()) {
            // Get the my / ally / enemy color and desaturate it
            Color color = GUIP.getEnemyUnitColor();
            if (entity.getOwnerId() == localPlayer.getId()) {
                color = GUIP.getMyUnitColor();
            } else if (!localPlayer.isEnemyOf(entity.getOwner())) {
                color = GUIP.getAllyUnitColor();
            }
            color = addGray(color, 128).brighter();
            result.append(guiScaledFontHTML(color)).append("<BR>");
            var forceChain = entity.getGame().getForces().forceChain(entity);
            for (int i = forceChain.size() - 1; i >= 0; i--) {
                result.append(forceChain.get(i).getName());
                result.append(i != 0 ? ", " : "");
            }
            result.append("</FONT>");
        }
        return result;
    }
    
    /** Returns an overview of the C3 system the unit is in. */
    private static StringBuilder c3Info(Entity entity) {
        StringBuilder result = new StringBuilder();

        result.append(guiScaledFontHTML());
        List<String> members = entity.getGame().getEntitiesVector().stream()
                .filter(e -> e.onSameC3NetworkAs(entity))
                .sorted(Comparator.comparingInt(Entity::getId))
                .map(e -> c3UnitName(e, entity)).collect(Collectors.toList());
        if (members.size() > 1) {
            result.append(guiScaledFontHTML(uiC3Color(), -0.2f));
            if (entity.hasNhC3()) {
                result.append(entity.hasC3i() ? "C3i" : "NC3");
            } else {
                result.append("C3");
            }
            result.append(" Network: <BR>&nbsp;&nbsp;");
            result.append(String.join("<BR>&nbsp;&nbsp;", members));
            result.append("<BR>");
        }

        result.append("</FONT>");
        return result;
    }

    private static String c3UnitName(Entity c3member, Entity entity) {
        StringBuilder result = new StringBuilder();
        result.append(guiScaledFontHTML(uiGray(), -0.2f));
        result.append(" [" + c3member.getId() + "] <I>");
        if (c3member.isC3CompanyCommander()) {
            result.append("C3CC");
        } else if (c3member.hasC3M()) {
            result.append("C3M");
        }
        result.append("</I></FONT> ");
        result.append(c3member.getShortNameRaw());
        result.append(guiScaledFontHTML(uiGray(), -0.2f));
        result.append(c3member.equals(entity) ? "<I> (This unit)</I>" : "");
        result.append("</FONT>");
        return result.toString();    
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

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.*;
import megamek.common.IGame.Phase;
import megamek.common.options.*;
import static megamek.client.ui.swing.tooltip.TipUtil.*;

public final class UnitToolTip {
    
    // Unify tooltips
    // remove unnecessary elements in lobby
    //TODO: correct color of other players in lobby
    // Mark capital weapns and capital armor - not necessary, alreedy marked
    // Mark capital armor
    // line break after name
    // pilot table
    //TODO: multi units horizontal stacking?
    // boardv use settooltip?
    // remove armor locs on aero that have no armor
    // make quirks multi per line
    // add settings for text size (overall?)
    // in lobby, change current vel to starting vel, starting alt / elev ?
    //TODO: on hex edge, all adjacent units in both hexes are shown ????!??!?!??
    // test board editor names (clifftop, bldgs)
    //TODO: GUIScale to 0...10, 1 middle
    // Pilot: only 1 G/P for all, arrange portraits in 1 row
    //TODO: Summary of Quirks in game
    //TODO: wps / armor 1 size smaller?
    
    
    
    private static StringBuffer result;
    private static boolean skipBRAfterTable;
    private static Entity entity;
    private static String fontSize;
    private static String armorChar;
    private static String internalChar;
    
    private static String destroyedChar;
    private static String colorIntact;
    private static String colorPartialDmg;
    private static String colorDamaged;
    private static int visUnit;

    public static synchronized StringBuffer getEntityTooltip(Entity en, IPlayer localPlayer) {
        // Tooltip info for a sensor blip
        if (EntityVisibilityUtils.onlyDetectedBySensors(localPlayer, en)) {
            return new StringBuffer(Messages.getString("BoardView1.sensorReturn"));
        }

        result = new StringBuffer();
        entity = en;
        IGame game = en.getGame();
        GUIPreferences guip = GUIPreferences.getInstance();
        skipBRAfterTable = false;
        
        boolean isGunEmplacement = entity instanceof GunEmplacement;
        boolean inLounge = game.getPhase() == Phase.PHASE_LOUNGE;

        // Unit Chassis and Player
        result.append(getFontHTML());
        addToTT("ChassisPlayer", NOBR,
                Integer.toHexString(PlayerColors.getColorRGB(entity.getOwner().getColorIndex())), 
                entity.getChassis(), entity.getOwner().getName());
        result.append("</FONT>");

        // Pilot; in the lounge the pilot is separate so don't add it there
        if (!inLounge) {
            result.append(PilotToolTip.getPilotTipShort(en));
        } else {
            result.append("<BR>");
        }

        result.append(getFontHTML());

        // Unit movement ability
        if (!isGunEmplacement) {
            addToTT("Movement", NOBR, entity.getWalkMP(), entity.getRunMPasString());
            if (entity.getJumpMP() > 0) {
                result.append("/" + entity.getJumpMP());
            }
        }

        // Armor and Internals
        addToTT("ArmorInternals", BR, entity.getTotalArmor(), entity.getTotalInternal());
        if (entity.isCapitalScale()) {
            addToTT("ArmorCapital", BR);
        }
        
        result.append("</FONT>");
        
        // Build a "status bar" visual representation of armor
        // and IS of the unit using block element characters.
        if (guip.getBoolean(GUIPreferences.SHOW_ARMOR_MINIVIS_TT)) {
            addArmorMiniVisToTT();
            skipBRAfterTable = true;
        }
        
        result.append(getFontHTML());
        
        // BV Info
        // Hidden for invisible units when in double blind and hide enemy bv is selected
        // Also not shown in the lobby as BV is shown there outside the tooltip
        boolean showEnemyBV = !(game.getOptions().booleanOption(OptionsConstants.ADVANCED_SUPPRESS_DB_BV) &&
                game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND));
        boolean isVisible = EntityVisibilityUtils.trackThisEntitiesVisibilityInfo(localPlayer, entity);

        if (!inLounge && (isVisible || showEnemyBV)) {
            int currentBV = entity.calculateBattleValue(false, false);
            int initialBV = entity.getInitialBV();
            double percentage = (double) currentBV / initialBV;
            addToTT("BV", BR, currentBV, initialBV, percentage);
        }

        // Heat, not shown in the lobby and for units with 999 heat sinks (vehicles)
        if (!inLounge && (entity.getHeatCapacity() != 999)) {
            if (entity.heat == 0) {
                addToTT("Heat0", BR);
            } else { 
                addToTT("Heat", BR, entity.heat);
            }
        }

        // Actual Movement
        if (!isGunEmplacement) {
            // In the Movement Phase, unit not done
            if (!entity.isDone() && game.getPhase() == Phase.PHASE_MOVEMENT) {
                // "Has not yet moved" only during movement phase
                addToTT("NotYetMoved", BR);
                // In the Movement Phase, unit is done - or in the Firing Phase
            } else if ((entity.isDone() && game.getPhase() == Phase.PHASE_MOVEMENT) 
                    || game.getPhase() == Phase.PHASE_FIRING) {
                int tmm = Compute.getTargetMovementModifier(game, entity.getId()).getValue();
                if (entity.moved == EntityMovementType.MOVE_NONE) {
                    addToTT("NoMove", BR, tmm);
                } else {
                    addToTT("MovementF", BR, entity.getMovementString(entity.moved),
                            entity.delta_distance, tmm);
                }
                // Special Moves
                if (entity.isEvading()) { 
                    addToTT("Evade", NOBR);
                }

                if ((entity instanceof Infantry) && ((Infantry)entity).isTakingCover()) { 
                    addToTT("TakingCover", NOBR);
                }

                if (entity.isCharging()) { 
                    addToTT("Charging", NOBR);
                }

                if (entity.isMakingDfa()) { 
                    addToTT("DFA", NOBR);
                }
            }
        }

        // Velocity, Altitude, Elevation
        if (entity.isAero()) {
            Aero aero = (Aero) entity;
            if (inLounge) {
                addToTT("AeroStartingVelAlt", BR, aero.getCurrentVelocity(), aero.getAltitude());
            } else {
                addToTT("AeroVelAlt", BR, aero.getCurrentVelocity(), aero.getAltitude());
            }
        } else if (entity.getElevation() != 0) {
            if (inLounge) {
                addToTT("StartingElev", BR, entity.getElevation());
            } else {
                addToTT("Elev", BR, entity.getElevation());
            }
        }

        // Gun Emplacement Status
        if (isGunEmplacement) {
            GunEmplacement emp = (GunEmplacement) entity; 
            if (emp.isTurret() && emp.isTurretLocked(emp.getLocTurret())) {
                addToTT("TurretLocked", BR);
            }
        }
        
        if (entity instanceof Infantry) {
            Infantry inf = (Infantry) entity;
            int spec = inf.getSpecializations();
            if (spec > 0) {
                addToTT("InfSpec", BR, Infantry.getSpecializationName(spec));
            }
        }

        // Unit Immobile
        if (!isGunEmplacement && (entity.isImmobile())) {
            addToTT("Immobile", BR);
        }

        if (entity.isHiddenActivating()) {
            addToTT("HiddenActivating", BR,
                    IGame.Phase.getDisplayableName(entity.getHiddenActivationPhase()));
        } else if (entity.isHidden()) {
            addToTT("Hidden", BR);
        }

        // Jammed by ECM - don't know how to replicate this correctly from the boardview
//        if (isAffectedByECM()) {
//            addToTT("Jammed", BR);
//        }

        // Swarmed
        if (entity.getSwarmAttackerId() != Entity.NONE) {
            addToTT("Swarmed", BR, game.getEntity(entity.getSwarmAttackerId()).getDisplayName());
        }

        // Spotting
        if (entity.isSpotting()) {
            addToTT("Spotting", BR, game.getEntity(entity.getSpotTargetId()).getDisplayName());
        }

        // If DB, add information about who sees this Entity
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
            StringBuffer playerList = new StringBuffer();
            boolean teamVision = game.getOptions().booleanOption(
                    OptionsConstants.ADVANCED_TEAM_VISION);
            for (IPlayer player : entity.getWhoCanSee()) {
                if (player.isEnemyOf(entity.getOwner()) || !teamVision) {
                    playerList.append(player.getName());
                    playerList.append(", ");
                }
            }
            if (playerList.length() > 1) {
                playerList.delete(playerList.length() - 2, playerList.length());
                addToTT("SeenBy", BR, playerList.toString());
            }            
        }

        // If sensors, display what sensors this unit is using
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_SENSORS)
                || game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS)) {
            addToTT("Sensors", BR, entity.getSensorDesc());
        }

        // Towing
        if (entity.getAllTowedUnits().size() > 0) {
            String unitList = entity.getAllTowedUnits().stream()
                    .map(id -> entity.getGame().getEntity(id).getDisplayName())
                    .collect(Collectors.joining(", "));
            if (unitList.length() > 1) {
                addToTT("Towing", BR, unitList);
            }
        }

        // Weapon List
        if (guip.getBoolean(GUIPreferences.SHOW_WPS_IN_TT)) {

            ArrayList<Mounted> weapons = entity.getWeaponList();
            HashMap<String, Integer> wpNames = new HashMap<String,Integer>();

            // Gather names, counts, Clan/IS
            // When clan then the number will be stored as negative
            for (Mounted curWp: weapons) {
                String weapDesc = curWp.getDesc();
                // Append ranges
                WeaponType wtype = (WeaponType)curWp.getType();
                int ranges[];
                if (entity.isAero()) {
                    ranges = wtype.getATRanges();
                } else {
                    ranges = wtype.getRanges(curWp);
                } 
                String rangeString = " \u22EF ";
                if ((ranges[RangeType.RANGE_MINIMUM] != WeaponType.WEAPON_NA) 
                        && (ranges[RangeType.RANGE_MINIMUM] != 0)) {
                    rangeString += "(" + ranges[RangeType.RANGE_MINIMUM] + ") ";
                }
                int maxRange = RangeType.RANGE_LONG;
                if (game.getOptions().booleanOption(
                        OptionsConstants.ADVCOMBAT_TACOPS_RANGE)) {
                    maxRange = RangeType.RANGE_EXTREME;
                }
                for (int i = RangeType.RANGE_SHORT; i <= maxRange; i++) {
                    rangeString += ranges[i];
                    if (i != maxRange) {
                        rangeString += "\u2B1D";
                    }
                }
                weapDesc += rangeString;
                if (wpNames.containsKey(weapDesc)) {
                    int number = wpNames.get(weapDesc);
                    if (number > 0) 
                        wpNames.put(weapDesc, number + 1);
                    else 
                        wpNames.put(weapDesc, number - 1);
                } else {
                    WeaponType wpT = ((WeaponType)curWp.getType());

                    if (entity.isClan() && TechConstants.isClan(wpT.getTechLevel(entity.getYear()))) 
                        wpNames.put(weapDesc, -1);
                    else
                        wpNames.put(weapDesc, 1);
                }
            }

            // Print to Tooltip
            for (Entry<String, Integer> entry : wpNames.entrySet()) {
                // Check if weapon is destroyed, text gray and strikethrough if so, remove the "x "/"*"
                // Also remove "+", means currently selected for firing
                boolean wpDest = false;
                String nameStr = entry.getKey();
                if (entry.getKey().startsWith("x ")) { 
                    nameStr = entry.getKey().substring(2, entry.getKey().length());
                    wpDest = true;
                }

                if (entry.getKey().startsWith("*")) { 
                    nameStr = entry.getKey().substring(1, entry.getKey().length());
                    wpDest = true;
                }

                if (entry.getKey().startsWith("+")) { 
                    nameStr = entry.getKey().substring(1, entry.getKey().length());
                    nameStr = nameStr.concat(" <I>(Firing)</I>");
                }

                // normal coloring 
                result.append("<FONT COLOR=#8080FF>");
                // but: color gray and strikethrough when weapon destroyed
                if (wpDest) result.append("<FONT COLOR=#a0a0a0><S>");

                String clanStr = "";
                if (entry.getValue() < 0) clanStr = Messages.getString("BoardView1.Tooltip.Clan");

                // when more than 5 weapons are present, they will be grouped
                // and listed with a multiplier
                if (weapons.size() > 5) {
                    addToTT("WeaponN", BR, Math.abs(entry.getValue()), clanStr, nameStr);

                } else { // few weapons: list each weapon separately
                    for (int i = 0; i < Math.abs(entry.getValue()); i++) {
                        addToTT("Weapon", BR, Math.abs(entry.getValue()), clanStr, nameStr);
                    }
                }
                // Weapon destroyed? End strikethrough
                if (wpDest) result.append("</S>");
                result.append("</FONT>"); 
            }
        }
        result.append("<BR>");
        result.append("</FONT>");
        
        // StratOps quirks, chassis and weapon
        result.append(getSmallFontHTML());
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            String quirksList = getOptionList(entity.getQuirks().getGroups(), 
                    grp -> entity.countQuirks(grp), inLounge);
            if (!quirksList.isEmpty()) {
                result.append(quirksList + "<BR>");
            }
            for (Mounted weapon : entity.getWeaponList()) {
                String wpQuirksList = getOptionList(weapon.getQuirks().getGroups(), 
                        grp -> weapon.countQuirks(), (e) -> weapon.getDesc(), inLounge);
                if (!wpQuirksList.isEmpty()) {
                    result.append(wpQuirksList + "<BR>");
                }
            }
        }

        // Partial repairs
        String partialList = getOptionList(entity.getPartialRepairs().getGroups(), 
                grp -> entity.countPartialRepairs(), inLounge);
        if (!partialList.isEmpty()) {
            result.append(partialList + "<BR>");
        }
        
        result.append("</FONT>");
        return result;
    }
    
    private static void addArmorMiniVisToTT() {
        GUIPreferences guip = GUIPreferences.getInstance();
        armorChar = guip.getString(GUIPreferences.ADVANCED_ARMORMINI_ARMOR_CHAR);
        if (entity.isCapitalScale()) {
            armorChar = guip.getString(GUIPreferences.ADVANCED_ARMORMINI_CAP_ARMOR_CHAR);
        }
        internalChar = guip.getString(GUIPreferences.ADVANCED_ARMORMINI_IS_CHAR);
        destroyedChar = guip.getString(GUIPreferences.ADVANCED_ARMORMINI_DESTROYED_CHAR);
        colorIntact = Integer.toHexString(
                guip.getColor(GUIPreferences.ADVANCED_ARMORMINI_COLOR_INTACT).getRGB() & 0xFFFFFF);
        colorPartialDmg = Integer.toHexString(
                guip.getColor(GUIPreferences.ADVANCED_ARMORMINI_COLOR_PARTIAL_DMG).getRGB() & 0xFFFFFF);
        colorDamaged = Integer.toHexString(
                guip.getColor(GUIPreferences.ADVANCED_ARMORMINI_COLOR_DAMAGED).getRGB() & 0xFFFFFF);
        visUnit = guip.getInt(GUIPreferences.ADVANCED_ARMORMINI_UNITS_PER_BLOCK);
        addToTT("ArmorMiniPanelStart", NOBR);
        for (int loc = 0 ; loc < entity.locations(); loc++) {
            // do not show locations that do not support/have armor/internals like HULL on Aero
            if (entity.getOArmor(loc) <= 0 && entity.getOInternal(loc) <= 0 && !entity.hasRearArmor(loc)) {
                continue;
            }
            result.append("<TR><TD>");
            if (entity.getInternal(loc) == IArmorState.ARMOR_DOOMED ||
                    entity.getInternal(loc) == IArmorState.ARMOR_DESTROYED) {
                // Destroyed location
                result.append("</TD><TD></TD><TD>");
                addToTT("AMPLoc", NOBR, fontSize, "&nbsp;&nbsp;" + entity.getLocationAbbr(loc)+ ":");
                int origIS = (entity.getOInternal(loc) - 1) / visUnit + 1;
                addToTT("AMPItem", NOBR, colorDamaged, fontSize, destroyedChar.repeat(origIS));
            } else {
                // Put rear armor blocks first, with some spacing, if unit has any.
                if (entity.hasRearArmor(loc)) {
                    addToTT("AMPLoc", NOBR, fontSize, entity.getLocationAbbr(loc) + "R:");
                    result.append(buildAMP(entity.getOArmor(loc, true), entity.getArmor(loc, true), armorChar));
                    result.append("</TD><TD>");
                } else {
                    // Add empty table cells instead
                    // At small font sizes, writing one character at the correct font size is 
                    // necessary to prevent the table rows from being spaced non-beautifully
                    addToTT("AMPLoc", NOBR, fontSize, "&nbsp;");
                    addToTT("AMPLoc", NOBR, fontSize, "&nbsp;");
                }
                addToTT("AMPLoc", NOBR, fontSize, "&nbsp;&nbsp;" + entity.getLocationAbbr(loc)+ ":");
                // Add IS shade blocks.
                result.append(buildAMP(entity.getOInternal(loc), entity.getInternal(loc), internalChar));
                // Add main armor blocks.
                result.append(buildAMP(entity.getOArmor(loc), entity.getArmor(loc), armorChar));
                result.append("</TD></TR>");
            }
        }
        addToTT("ArmorMiniPanelEnd", NOBR);
    }
    
    /** 
     * Returns a string representing armor or internal structure of one location.
     * The location has the given orig original Armor/IS and the given curr current
     * Armor/IS. The character dChar will be repeated at appropriate colors depending
     * on the value of curr, orig and the static visUnit which gives the amount of 
     * Armor/IS per single character. 
     */
    private static String buildAMP(int orig, int curr, String dChar) {
        // Internal Structure can be zero, e.g. in Aero
        if (orig == 0) {
            return "";
        }
        
        String result = "";
        int numPartial = ((curr != orig) && (curr % visUnit) > 0) ? 1 : 0;
        int numIntact = (curr - 1) / visUnit + 1 - numPartial;
        int numDmgd = (orig - 1) / visUnit + 1 - numPartial - numIntact;
        if (numIntact > 0) {
            addToTT("AMPItem", NOBR, colorIntact, fontSize, dChar.repeat(numIntact));
        }
        if (numPartial > 0) {
            addToTT("AMPItem", NOBR, colorPartialDmg, fontSize, dChar.repeat(numPartial));
        }
        if (numDmgd > 0) {
            addToTT("AMPItem", NOBR, colorDamaged, fontSize, dChar.repeat(numDmgd));
        }
        return result;
    }
    
    /** Helper method to shorten repetitive calls. */
    protected static void addToTT(String tipName, boolean startBR, Object... ttO) {
        if (startBR == BR) {
            if (skipBRAfterTable) {
                skipBRAfterTable = false;
            } else {
                result.append("<BR>");
            }
        }
        if (ttO != null) {
            result.append(Messages.getString("BoardView1.Tooltip." + tipName, ttO));
        } else {
            result.append(Messages.getString("BoardView1.Tooltip." + tipName));
        }
    }

}

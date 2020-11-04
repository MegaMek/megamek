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

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.IGame.Phase;
import megamek.common.annotations.Nullable;
import megamek.common.options.*;
import static megamek.client.ui.swing.tooltip.TipUtil.*;
import static megamek.client.ui.swing.util.UIUtil.*;

public final class UnitToolTip {
    
    // Unify tooltips
    // remove unnecessary elements in lobby
    // correct color of other players in lobby
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
    // GUIScale to 0.7..2.4, 1 middle
    // Pilot: only 1 G/P for all, arrange portraits in 1 row
    //TODO: Summary of Quirks in game
    // wps / armor 1 size smaller?
    // blind drop tooltips!
    // compact mode: partial repairs/damaged, C3 complete, 
    // remove load label from lobby
    //TODO: allow disconnecting C3 in lobby
    // show doomed status in lobby
    // show doomed status explicit in tooltip #2322
    //TODO: better ECM source
    
    
    public static StringBuilder getEntityTipLobby(Entity entity, IPlayer localPlayer, 
            MapSettings mapSettings) {
        return getEntityTip(entity, localPlayer, true, mapSettings);
    }
    
    public static StringBuilder getEntityTipGame(Entity entity, IPlayer localPlayer) {
        return getEntityTip(entity, localPlayer, false, null);
    }

    // PRIVATE
    
    private static StringBuilder getEntityTip(Entity entity, IPlayer localPlayer, 
            boolean inLobby, @Nullable MapSettings mapSettings) {
        // Tooltip info for a sensor blip
        if (EntityVisibilityUtils.onlyDetectedBySensors(localPlayer, entity)) {
            return new StringBuilder(Messages.getString("BoardView1.sensorReturn"));
        }

        StringBuilder result = new StringBuilder();
        IGame game = entity.getGame();
        GUIPreferences guip = GUIPreferences.getInstance();
        
        // Unit Chassis and Player
        IPlayer owner = game.getPlayer(entity.getOwnerId());
        result.append(getFontHTML(PlayerColors.getColor(owner.getColorIndex())));
        result.append(addToTT("ChassisPlayer", NOBR, entity.getChassis(), owner.getName()));
        result.append("</FONT>");

        // Pilot; in the lounge the pilot is separate so don't add it there
        if (inLobby && (mapSettings != null)) {
            result.append(deploymentWarnings(entity, localPlayer, mapSettings));
            result.append(deploymentInfo(entity));
            result.append("<BR>");
        } else {
            result.append(inGameValues(entity, localPlayer));
            result.append(PilotToolTip.getPilotTipShort(entity));
        }

        // Static entity values like move capability
        result.append(getFontHTML());
        result.append(entityValues(entity));
        result.append("</FONT>");

        // Status bar visual representation of armor and IS 
        if (guip.getBoolean(GUIPreferences.SHOW_ARMOR_MINIVIS_TT)) {
            result.append(addArmorMiniVisToTT(entity));
        }

        // Weapon List
        if (guip.getBoolean(GUIPreferences.SHOW_WPS_IN_TT)) {
            result.append(getSmallFontHTML());
            result.append(weaponList(entity));
            result.append("</FONT>");
        }

        // StratOps quirks, chassis and weapon
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            result.append(getSmallFontHTML());
            String quirksList = getOptionList(entity.getQuirks().getGroups(), 
                    grp -> entity.countQuirks(grp), inLobby);
            if (!quirksList.isEmpty()) {
                result.append(quirksList);
            }
            for (Mounted weapon : entity.getWeaponList()) {
                String wpQuirksList = getOptionList(weapon.getQuirks().getGroups(), 
                        grp -> weapon.countQuirks(), (e) -> weapon.getDesc(), inLobby);
                if (!wpQuirksList.isEmpty()) {
                    result.append("Weapon Quirk: " + wpQuirksList);
                }
            }
            result.append("</FONT>");
        }

        // Partial repairs
        String partialList = getOptionList(entity.getPartialRepairs().getGroups(), 
                grp -> entity.countPartialRepairs(), inLobby);
        if (!partialList.isEmpty()) {
            result.append(getSmallFontHTML());
            result.append(partialList + "<BR>");
            result.append("</FONT>");
        }
        

        return result;
    }
    
    private static StringBuilder addArmorMiniVisToTT(Entity entity) {
        GUIPreferences guip = GUIPreferences.getInstance();
        String armorChar = guip.getString(GUIPreferences.ADVANCED_ARMORMINI_ARMOR_CHAR);
        if (entity.isCapitalScale()) {
            armorChar = guip.getString(GUIPreferences.ADVANCED_ARMORMINI_CAP_ARMOR_CHAR);
        }
        String internalChar = guip.getString(GUIPreferences.ADVANCED_ARMORMINI_IS_CHAR);
        String destroyedChar = guip.getString(GUIPreferences.ADVANCED_ARMORMINI_DESTROYED_CHAR);
        Color colorDamaged = guip.getColor(GUIPreferences.ADVANCED_ARMORMINI_COLOR_DAMAGED);
        int visUnit = guip.getInt(GUIPreferences.ADVANCED_ARMORMINI_UNITS_PER_BLOCK);
        StringBuilder result = new StringBuilder();
        result.append("<TABLE CELLSPACING=0 CELLPADDING=0><TBODY>");
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
                result.append(getSmallFontHTML());
                result.append("&nbsp;&nbsp;" + entity.getLocationAbbr(loc)+ ":&nbsp;");
                result.append("</FONT></TD><TD>");
                result.append(getSmallFontHTML(colorDamaged));
                int origIS = (entity.getOInternal(loc) - 1) / visUnit + 1;
                result.append(destroyedChar.repeat(origIS));
                result.append("</FONT>");
            } else {
                // Put rear armor blocks first, with some spacing, if unit has any.
                if (entity.hasRearArmor(loc)) {
                    result.append(getSmallFontHTML());
                    result.append("&nbsp;&nbsp;" + entity.getLocationAbbr(loc)+ "R:&nbsp;");
                    result.append("</FONT></TD><TD>");
                    result.append(buildAMP(entity.getOArmor(loc, true), entity.getArmor(loc, true), armorChar));
                    result.append("</TD><TD>");
                } else {
                    // Add empty table cells instead
                    // At small font sizes, writing one character at the correct font size is 
                    // necessary to prevent the table rows from being spaced non-beautifully
                    result.append(getSmallFontHTML() + "&nbsp;</FONT></TD><TD>");
                    result.append(getSmallFontHTML() + "&nbsp;</FONT></TD><TD>");
                }
                result.append(getSmallFontHTML());
                result.append("&nbsp;&nbsp;" + entity.getLocationAbbr(loc)+ ":&nbsp;");
                result.append("</FONT></TD><TD>");
                // Add IS shade blocks.
                result.append(buildAMP(entity.getOInternal(loc), entity.getInternal(loc), internalChar));
                // Add main armor blocks.
                result.append(buildAMP(entity.getOArmor(loc), entity.getArmor(loc), armorChar));
                result.append("</TD></TR>");
            }
        }
        result.append("</TBODY></TABLE>");
        return result;
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
        
        StringBuilder result = new StringBuilder();
        GUIPreferences guip = GUIPreferences.getInstance();
        Color colorIntact = guip.getColor(GUIPreferences.ADVANCED_ARMORMINI_COLOR_INTACT);
        Color colorPartialDmg = guip.getColor(GUIPreferences.ADVANCED_ARMORMINI_COLOR_PARTIAL_DMG);
        Color colorDamaged = guip.getColor(GUIPreferences.ADVANCED_ARMORMINI_COLOR_DAMAGED);
        int visUnit = guip.getInt(GUIPreferences.ADVANCED_ARMORMINI_UNITS_PER_BLOCK);
        int numPartial = ((curr != orig) && (curr % visUnit) > 0) ? 1 : 0;
        int numIntact = (curr - 1) / visUnit + 1 - numPartial;
        int numDmgd = (orig - 1) / visUnit + 1 - numPartial - numIntact;
        if (numIntact > 0) {
            result.append(getSmallFontHTML(colorIntact));
            result.append(dChar.repeat(numIntact));
            result.append("</FONT>");
        }
        if (numPartial > 0) {
            result.append(getSmallFontHTML(colorPartialDmg));
            result.append(dChar.repeat(numPartial));
            result.append("</FONT>");
        }
        if (numDmgd > 0) {
            result.append(getSmallFontHTML(colorDamaged));
            result.append(dChar.repeat(numDmgd));
            result.append("</FONT>");
        }
        return result.toString();
    }
    
    private static StringBuilder weaponList(Entity entity) {
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
        StringBuilder result = new StringBuilder();
        boolean subsequentLine = false;
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
                result.append(addToTT("WeaponN", subsequentLine, Math.abs(entry.getValue()), clanStr, nameStr));
                subsequentLine = true;
            } else { // few weapons: list each weapon separately
                for (int i = 0; i < Math.abs(entry.getValue()); i++) {
                    result.append(addToTT("Weapon", subsequentLine, Math.abs(entry.getValue()), clanStr, nameStr));
                    subsequentLine = true;
                }
            }
            // Weapon destroyed? End strikethrough
            if (wpDest) result.append("</S>");
            result.append("</FONT>"); 
        }
        result.append("<BR>");
        return result;
    }
    
    private static StringBuilder inGameValues(Entity entity, IPlayer localPlayer) {
        StringBuilder result = new StringBuilder();
        IGame game = entity.getGame();
        boolean isGunEmplacement = entity instanceof GunEmplacement;
        
        // Coloring and italic to make these transient entries stand out
        result.append(getFontHTML(UIUtil.uiLightViolet()) + "<I>");
        
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

        // Heat, not shown in the lobby and for units with 999 heat sinks (vehicles)
        if (entity.getHeatCapacity() != 999) {
            if (entity.heat == 0) {
                result.append(getFontHTML(UIUtil.uiGreen()));
                result.append(addToTT("Heat0", BR));
            } else { 
                result.append(getFontHTML(UIUtil.uiLightRed()));
                result.append(addToTT("Heat", BR, entity.heat));
            }
            result.append("</FONT>");
        }

        // Actual Movement
        if (!isGunEmplacement) {
            // "Has not yet moved" only during movement phase
            if (!entity.isDone() && game.getPhase() == Phase.PHASE_MOVEMENT) {
                result.append(addToTT("NotYetMoved", BR));
            } else if ((entity.isDone() && game.getPhase() == Phase.PHASE_MOVEMENT) 
                    || game.getPhase() == Phase.PHASE_FIRING) {
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

                if ((entity instanceof Infantry) && ((Infantry)entity).isTakingCover()) { 
                    result.append(addToTT("TakingCover", NOBR));
                }

                if (entity.isCharging()) { 
                    result.append(addToTT("Charging", BR));
                }

                if (entity.isMakingDfa()) { 
                    result.append(addToTT("DFA", NOBR));
                }
            }
        }

        // Velocity, Altitude, Elevation
        if (entity.isAero()) {
            result.append(getFontHTML(UIUtil.uiLightViolet()));
            Aero aero = (Aero) entity;
//            if (inLounge) {
//                addToTT("AeroStartingVelAlt", BR, aero.getCurrentVelocity(), aero.getAltitude());
//            } else {
            result.append(addToTT("AeroVelAlt", BR, aero.getCurrentVelocity(), aero.getAltitude()));
            result.append("</FONT>");
//            }
        } else if (entity.getElevation() != 0) {
            result.append(getFontHTML(UIUtil.uiLightViolet()));
//            if (inLounge) {
//                addToTT("StartingElev", BR, entity.getElevation());
//            } else {
            result.append(addToTT("Elev", BR, entity.getElevation()));
            result.append("</FONT>");
//            }
        }

        // Gun Emplacement Status
        if (isGunEmplacement) {
            GunEmplacement emp = (GunEmplacement) entity; 
            if (emp.isTurret() && emp.isTurretLocked(emp.getLocTurret())) {
                result.append(getFontHTML(GUIPreferences.getInstance().getWarningColor()));
                result.append(addToTT("TurretLocked", BR));
                result.append("</FONT>");
            }
        }

        // Unit Immobile
        if (!isGunEmplacement && (entity.isImmobile())) {
            result.append(getFontHTML(GUIPreferences.getInstance().getWarningColor()));
            result.append(addToTT("Immobile", BR));
            result.append("</FONT>");
        }

        if (entity.isHiddenActivating()) {
            result.append(addToTT("HiddenActivating", BR,
                    IGame.Phase.getDisplayableName(entity.getHiddenActivationPhase())));
        } else if (entity.isHidden()) {
            result.append(addToTT("Hidden", BR));
        }

        // Jammed by ECM - don't know how to replicate this correctly from the boardview
        //      if (isAffectedByECM()) {
        //          addToTT("Jammed", BR);
        //      }

        // Swarmed
        if (entity.getSwarmAttackerId() != Entity.NONE) {
            result.append(addToTT("Swarmed", BR, game.getEntity(entity.getSwarmAttackerId()).getDisplayName()));
        }

        // Spotting
        if (entity.isSpotting()) {
            result.append(addToTT("Spotting", BR, game.getEntity(entity.getSpotTargetId()).getDisplayName()));
        }

        // If Double Blind, add information about who sees this Entity
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
                result.append(addToTT("SeenBy", BR, playerList.toString()));
            }            
        }

        // If sensors, display what sensors this unit is using
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_SENSORS)
                || game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS)) {
            result.append(addToTT("Sensors", BR, entity.getSensorDesc()));
        }

        // Towing
        if (entity.getAllTowedUnits().size() > 0) {
            String unitList = entity.getAllTowedUnits().stream()
                    .map(id -> entity.getGame().getEntity(id).getShortName())
                    .collect(Collectors.joining(", "));
            if (unitList.length() > 1) {
                result.append(addToTT("Towing", BR, unitList));
            }
        }
        result.append("</I></FONT>");
        return result;
    }
    
    private static StringBuilder entityValues(Entity entity) {
        StringBuilder result = new StringBuilder();
        boolean isGunEmplacement = entity instanceof GunEmplacement;
        // Unit movement ability
        if (isGunEmplacement) {
            result.append(addToTT("Immobile", NOBR));
        } else {
            result.append(addToTT("Movement", NOBR, entity.getWalkMP(), entity.getRunMPasString()));
            if (entity.getJumpMP() > 0) {
                result.append("/" + entity.getJumpMP());
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

        // Armor and Internals
        result.append(addToTT("ArmorInternals", BR, entity.getTotalArmor(), entity.getTotalInternal()));
        if (entity.isCapitalScale()) {
            addToTT("ArmorCapital", BR);
        }
        return result;
    }
    
    /** Returns warnings about problems that should be solved before deploying. */
    private static StringBuilder deploymentWarnings(Entity entity, IPlayer localPlayer,
            MapSettings mapSettings) {
        StringBuilder result = new StringBuilder();
        // Warning sign
        result.append(UIUtil.getFontHTML(GUIPreferences.getInstance().getWarningColor())); 
        if (((entity.hasC3i() || entity.hasNavalC3()) && (entity.calculateFreeC3Nodes() == 5))
                || ((entity.getC3Master() == null) && entity.hasC3S())) {
            result.append("<BR>Unconnected C3 Computer");
        }
        if (entity.getGame().getPlanetaryConditions().whyDoomed(entity, entity.getGame()) != null) {
            result.append("<BR>Cannot survive " + entity.getGame().getPlanetaryConditions().whyDoomed(entity, entity.getGame()));
        }
        if (entity.doomedInAtmosphere() && mapSettings.getMedium() == MapSettings.MEDIUM_ATMOSPHERE) {
            result.append("<BR>Cannot survive on a low/high atmosphere map!");
        }
        if (entity.doomedOnGround() && mapSettings.getMedium() == MapSettings.MEDIUM_GROUND) {
            result.append("<BR>Cannot survive on a ground map!");
        }
        if  (entity.doomedInSpace() && mapSettings.getMedium() == MapSettings.MEDIUM_SPACE) {
            result.append("<BR>Cannot survive in space!");
        }
        result.append("</FONT>");
        return result;
    }
    
    /** Returns information about deployment settings. */
    private static StringBuilder deploymentInfo(Entity entity) {
        StringBuilder result = new StringBuilder();
        result.append(UIUtil.getFontHTML(UIUtil.uiGreen()));
        
        // Loaded onto another unit
        if (entity.getTransportId() != Entity.NONE) {
            Entity loader = entity.getGame().getEntity(entity.getTransportId());
            result.append("<BR>Deploys aboard " + loader.getShortName());
        }

        // Hidden deployment
        if (entity.isHidden()) {
            result.append("<BR>Deploys hidden");
        }
        
        if (entity.isHullDown()) {
            result.append("<BR>Deploys hull down");
        }
        
        if (entity.isProne()) {
            result.append("<BR>Deploys prone");
        }
        
        // Offboard deployment
        if (entity.isOffBoard()) {
            result.append("<BR>" + Messages.getString("ChatLounge.deploysOffBoard"));
        } else if (entity.getDeployRound() > 0) {
            result.append("<BR>" + Messages.getString("ChatLounge.deploysAfterRound") + entity.getDeployRound());
            if (entity.getStartingPos(false) != Board.START_NONE) {
                result.append(Messages.getString("ChatLounge.deploysAfterZone"));
                result.append(IStartingPositions.START_LOCATION_NAMES[entity.getStartingPos(false)]);
            }
        }
    
        result.append("</FONT>");
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

}

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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.lobby.ChatLounge;
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
    // boardv use settooltip?
    // remove armor locs on aero that have no armor
    // make quirks multi per line
    // add settings for text size (overall?)
    // in lobby, change current vel to starting vel, starting alt / elev ?
    //TODO: on hex edge, all adjacent units in both hexes are shown ????!??!?!??
    // test board editor names (clifftop, bldgs)
    // GUIScale to 0.7..2.4, 1 middle
    // Pilot: only 1 G/P for all, arrange portraits in 1 row
    // Summary of Quirks in game
    // wps / armor 1 size smaller?
    // blind drop tooltips!
    // compact mode: partial repairs/damaged, C3 complete, 
    // remove load label from lobby
    //TODO: allow disconnecting C3 in lobby
    //TODO: config dialog de-crap
    // show doomed status in lobby
    // show doomed status explicit in tooltip #2322
    //TODO: better ECM source
    //TODO: portraits after loading a mul are there in the mektable but not in the TT
    // reduce portrait and unit image to make the lines smaller in full mode
    // save column width
    // make gameyearlabels etc. follow the guiScale
    // remove the button "DeleteAll"
    //TODO: Externalize strings
    //TODO: Restrict Hidden deploy to units that can deploy hidden: VTOL: only on elev 0
    //TODO: is hidden exclusive with late deploy?
    // Add toggle button show ID
    // Dont show Altitude in space
    //TODO: Loaded units have altitude?? Dont show alt/vel for loaded
    //TODO: Show Hidden only when hidden rule active -- no, remove hidden when inactive
    // Allow hiding unofficial and legacy options in Game options
    // No Altitude in space
    // No hidden in space
    //TODO: Add searchlight to entitysprite name label
    // add searchlights automatically - add searchlights always, remove popup menu, add note in PlanetryC
    // make the randommapdialog scrolling more responsive
    // gui scale various dialogs: connect/host/planetary conditions/randommap/mapsize
    // gui scale popups: player list, mek list
    // Player toolrip notes You and Your Bot
    // same team should see through blind drop
    // renew the TT when mouseing over the same units... -> shows ID in unit TT
    // in the unit selector, the text filter gets cursor directly and content is marked
    // constant tooltip recalc calling BV() all the time in the mek list -> bad
    // tables no line breaks
    //TODO: Show ID for loader
    //TODO: When resizing table columns, dont switch sorter
    // extract Mektablemodel, mektablepopup, mektableformatter, package lobby
    // Search Game Options switch legacy unoff.
    //TODO: debug hotloading improved lrms
    //TODO: Squadron Pilot name?
    // Add developer quicksave and quickload Ctrl-S/L. Will save to quicksave.sav.gz, assumes no PW, no registering on load
    // Tooltip shows Hotload and Burst MG mode (format better!)
    //TODO: squadron show actual fighters in them (icon)?
    // Button Player Settings, Bot Settings, 
    // remove popup on players
    //TODO: add init and mindefields to table?
    // dont show hotload as valid in popup for clan wps
    // Add confirm dlg for delete
    //TODO: TacOps/BMM minefields setting should affect lobby player table and player config
    //TODO: remove path from board list
    // unit selector remember sizes #2428
    // add dialog before delete
    // hide deployment info for loaded units
    // blind drop unit display "Medium Support Vehcles Vehicle...correction, generall< more specific (Light Vehicle)
    // corrected conventional fighter weight classes, use the same as ASF (50t = medium)
    //TODO: Mark a carrier and an unrelated unit, select offload -> doesnt work; mark carrier alone: works
    // Remove bot with loaded units breaks...
    //TODO: Player removal with loaded units rbeaks the game
    //TODO: Table sorter not updated when it was prohibited and changed 
    //TODO: sorting use the UIManager char for ascending/descending
    
    /** The font size reduction for Quirks */
    final static float TT_SMALLFONT_DELTA = -0.2f;
    
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
        result.append(guiScaledFontHTML(PlayerColors.getColor(owner.getColorIndex())));
        result.append(addToTT("ChassisPlayer", NOBR, entity.getChassis(), owner.getName()));
        result.append(UIUtil.guiScaledFontHTML(UIUtil.uiGray()));
        result.append(MessageFormat.format(" [ID: {0}] </FONT>", entity.getId()));
        result.append("</FONT>");

        // Pilot; in the lounge the pilot is separate so don't add it there
        if (inLobby && (mapSettings != null)) {
            result.append(deploymentWarnings(entity, localPlayer, mapSettings));
            result.append("<BR>");
        } else {
            result.append(inGameValues(entity, localPlayer));
            result.append(PilotToolTip.getPilotTipShort(entity));
        }

        // Static entity values like move capability
        result.append(guiScaledFontHTML());
        result.append(entityValues(entity));
        result.append("</FONT>");

        // Status bar visual representation of armor and IS 
        if (guip.getBoolean(GUIPreferences.SHOW_ARMOR_MINIVIS_TT)) {
            result.append(scaledHTMLSpacer(3));
            result.append(addArmorMiniVisToTT(entity));
        }

        // Weapon List
        if (guip.getBoolean(GUIPreferences.SHOW_WPS_IN_TT)) {
            result.append(scaledHTMLSpacer(3));
            result.append(guiScaledFontHTML());
            result.append(weaponList(entity));
            result.append("</FONT>");
        }

        // StratOps quirks, chassis and weapon
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            result.append(scaledHTMLSpacer(3));
            result.append(guiScaledFontHTML(uiQuirksColor(), TT_SMALLFONT_DELTA));
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
            result.append(scaledHTMLSpacer(3));
            result.append(guiScaledFontHTML(uiPartialRepairColor(), TT_SMALLFONT_DELTA));
            result.append(partialList);
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
        Color colorDamaged = guip.getColor(GUIPreferences.ADVANCED_ARMORMINI_COLOR_DAMAGED);
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
                result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                result.append("&nbsp;&nbsp;" + entity.getLocationAbbr(loc)+ ":&nbsp;");
                result.append("</FONT></TD><TD>");
                result.append(guiScaledFontHTML(colorDamaged, TT_SMALLFONT_DELTA));
                result.append(destroyedLocBar(entity.getOArmor(loc, true)));
                result.append("</FONT>");
            } else {
                // Rear armor
                if (entity.hasRearArmor(loc)) {
                    result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                    result.append("&nbsp;&nbsp;" + entity.getLocationAbbr(loc)+ "R:&nbsp;");
                    result.append("</FONT></TD><TD>");
                    result.append(intactLocBar(entity.getOArmor(loc, true), entity.getArmor(loc, true), armorChar));
                    result.append("</TD><TD>");
                } else {
                    // No rear armor: empty table cells instead
                    // At small font sizes, writing one character at the correct font size is 
                    // necessary to prevent the table rows from being spaced non-beautifully
                    result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA) + "&nbsp;</FONT></TD><TD>");
                    result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA) + "&nbsp;</FONT></TD><TD>");
                }
                // Front armor
                result.append(guiScaledFontHTML(TT_SMALLFONT_DELTA));
                result.append("&nbsp;&nbsp;" + entity.getLocationAbbr(loc)+ ":&nbsp;");
                result.append("</FONT></TD><TD>");
                result.append(intactLocBar(entity.getOInternal(loc), entity.getInternal(loc), internalChar));
                result.append(intactLocBar(entity.getOArmor(loc), entity.getArmor(loc), armorChar));
                result.append("</TD></TR>");
            }
        }
        result.append("</TBODY></TABLE>");
        return result;
    }
    
    /** 
     * Used for destroyed locations.
     * Returns a string representing armor or internal structure of the location.
     * The location has the given orig original Armor/IS. 
     */
    private static StringBuilder destroyedLocBar(int orig) {
        GUIPreferences guip = GUIPreferences.getInstance();
        String destroyedChar = guip.getString(GUIPreferences.ADVANCED_ARMORMINI_DESTROYED_CHAR);
        return locBar(orig, orig, destroyedChar, true);
    }
    
    /** 
     * Used for intact locations.
     * Returns a string representing armor or internal structure of the location.
     * The location has the given orig original Armor/IS. 
     */
    private static StringBuilder intactLocBar(int orig, int curr, String dChar) {
        return locBar(orig, orig, dChar, false);
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
        GUIPreferences guip = GUIPreferences.getInstance();
        Color colorIntact = guip.getColor(GUIPreferences.ADVANCED_ARMORMINI_COLOR_INTACT);
        Color colorPartialDmg = guip.getColor(GUIPreferences.ADVANCED_ARMORMINI_COLOR_PARTIAL_DMG);
        Color colorDamaged = guip.getColor(GUIPreferences.ADVANCED_ARMORMINI_COLOR_DAMAGED);
        int visUnit = guip.getInt(GUIPreferences.ADVANCED_ARMORMINI_UNITS_PER_BLOCK);
        
        if (destroyed) {
            colorIntact = colorDamaged;
            colorPartialDmg = colorDamaged;
        }
        
        int numPartial = ((curr != orig) && (curr % visUnit) > 0) ? 1 : 0;
        int numIntact = (curr - 1) / visUnit + 1 - numPartial;
        int numDmgd = (orig - 1) / visUnit + 1 - numPartial - numIntact;
        if (numIntact > 0) {
            result.append(guiScaledFontHTML(colorIntact, TT_SMALLFONT_DELTA));
            result.append(repeat(dChar, numIntact) + "</FONT>");
        }
        if (numPartial > 0) {
            result.append(guiScaledFontHTML(colorPartialDmg, TT_SMALLFONT_DELTA));
            result.append(repeat(dChar, numPartial) + "</FONT>");
        }
        if (numDmgd > 0) {
            result.append(guiScaledFontHTML(colorDamaged, TT_SMALLFONT_DELTA));
            result.append(repeat(dChar, numDmgd) + "</FONT>");
        }
        return result;
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
            if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD)
                    && curWp.isHotLoaded()) {
                weapDesc += " \u22EF<I> Hot-loaded</I>";
            }
            if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_BURST)
                    && curWp.isRapidfire()) {
                weapDesc += " \u22EF<I> Rapid-fire</I>";
            }
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

            result.append(guiScaledFontHTML(uiTTWeaponColor()));
            if (wpDest) {
                result.append("<S>");
            }

            String clanStr = "";
            if (entry.getValue() < 0) { 
                clanStr = Messages.getString("BoardView1.Tooltip.Clan");
            }

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
            if (wpDest) {
                result.append("</S>");
            }
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

        // Heat, not shown for units with 999 heat sinks (vehicles)
        if (entity.getHeatCapacity() != 999) {
            if (entity.heat == 0) {
                result.append(guiScaledFontHTML(uiGreen()));
                result.append(addToTT("Heat0", BR));
            } else { 
                result.append(guiScaledFontHTML(uiLightRed()));
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
            result.append(guiScaledFontHTML(uiLightViolet()));
            Aero aero = (Aero) entity;
//            if (inLounge) {
//                addToTT("AeroStartingVelAlt", BR, aero.getCurrentVelocity(), aero.getAltitude());
//            } else {
            result.append(addToTT("AeroVelAlt", BR, aero.getCurrentVelocity(), aero.getAltitude()));
            result.append("</FONT>");
//            }
        } else if (entity.getElevation() != 0) {
            result.append(guiScaledFontHTML(uiLightViolet()));
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
                result.append(guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor()));
                result.append(addToTT("TurretLocked", BR));
                result.append("</FONT>");
            }
        }

        // Unit Immobile
        if (!isGunEmplacement && (entity.isImmobile())) {
            result.append(guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor()));
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
            if (entity instanceof Tank) {
                result.append(ChatLounge.DOT_SPACER + entity.getMovementModeAsString());
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
        // Critical (red) warnings
        result.append(guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor())); 
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
        
        // Non-critical (yellow) warnings
        result.append(guiScaledFontHTML(uiYellow())); 
        if (((entity.hasC3i() || entity.hasNavalC3()) && (entity.calculateFreeC3Nodes() == 5))
                || ((entity.getC3Master() == null) && entity.hasC3S())) {
            result.append("<BR>Unconnected C3 Computer");
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

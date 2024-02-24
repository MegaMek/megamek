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

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import megamek.common.options.GameOptions;
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

    public static StringBuilder lobbyTip(InGameObject unit, Player localPlayer, MapSettings mapSettings) {
        if (unit instanceof Entity) {
            return getEntityTipTable((Entity) unit, localPlayer, true, false, false, mapSettings,
                    true, false, false, false, false, false);
        } else if (unit instanceof AlphaStrikeElement) {
            // TODO : Provide a suitable tip
            return new StringBuilder("AlphaStrikeElement " + ((AlphaStrikeElement) unit).getName());
        } else {
            return new StringBuilder("This type of object has currently no table entry.");
        }
    }

    /** Returns the unit tooltip with values that are relevant in the lobby. */
    public static StringBuilder getEntityTipLobby(Entity entity, Player localPlayer,
                                                  MapSettings mapSettings) {
        return getEntityTipTable(entity, localPlayer, true, true, false, mapSettings,
                true, false, false, false, false,false);
    }

    /** Returns the unit tooltip with values that are relevant in-game. */
    public static StringBuilder getEntityTipGame(Entity entity, Player localPlayer) {
        return getEntityTipTable(entity, localPlayer, false, true, true, null,
                true, true, true, true, true,false);
    }

    /** Returns the unit tooltip with values that are relevant in-game without the Pilot info. */
    public static StringBuilder getEntityTipUnitDisplay(Entity entity, Player localPlayer) {
        return getEntityTipTable(entity, localPlayer, true, false, false, null,
                true, true, true, true, true, false);
    }

    /** Returns the unit tooltip with minimal but useful information */
    public static StringBuilder getEntityTipAsTarget(Entity entity, Player localPlayer) {
        return getEntityTipTable(entity, localPlayer, false, true, false, null,
                true, true, false, false, false, false);
    }

    /** Returns the unit tooltip with minimal but useful information */
    public static StringBuilder getEntityTipReport(Entity entity) {
        return getEntityTipTable(entity, null, true, false, true, null,
                false, true, false, false, false, true);
    }

    public static String wrapWithHTML(String text) {
        String fgColor = GUIP.hexColor(GUIP.getUnitToolTipFGColor());
        String bgColor = GUIP.hexColor(GUIP.getUnitToolTipBGColor());
        String format = "<html><body style=\"color:%s; background-color:%s;\" >%s</body></html>";
        return String.format(format, fgColor, bgColor, text);
    }

    // PRIVATE

    /** Assembles the whole unit tooltip. */
    private static StringBuilder getEntityTipTable(Entity entity, Player localPlayer,
           boolean details, boolean pilotInfoShow, boolean pilotInfoStandard, @Nullable MapSettings mapSettings, boolean showName,
           boolean inGameValue, boolean showBV, boolean showSensors, boolean showSeenBy, boolean report) {
        // Tooltip info for a sensor blip
        if (EntityVisibilityUtils.onlyDetectedBySensors(localPlayer, entity)) {
            String msg_senorreturn = Messages.getString("BoardView1.sensorReturn");
            return new StringBuilder(msg_senorreturn);
        }

        String result = "";
        Game game = entity.getGame();

        // unit name, player name
        result += getDisplayNames(entity, game, showName);

        // Force
        result += forceEntry(entity, localPlayer);

        // In Game Values
        result += inGameValues(entity, localPlayer, inGameValue, showBV, showSensors, showSeenBy);

        // Deployment Warnings
        result += deploymentWarnings(entity, mapSettings, details);

        // Pilot
        result += getPilotInfo(entity, pilotInfoShow, pilotInfoStandard, report);

        // An empty squadron should not show any info
        if (entity instanceof FighterSquadron && entity.getLoadedUnits().isEmpty()) {
            String col = "<TD>" + result + "</TD>";
            String row = "<TR>" + col + "</TR>";
            String table = "<TABLE width=100%>" + row + "</TABLE>";
            return new StringBuilder().append(table);
        }

        // Static entity values like move capability
        result += getMovement(entity).toString();

        // Armor
        result += getArmor(entity).toString();

        // Status bar visual representation of armor and IS
        result += addArmorMiniVisToTT(entity);

        // Weapon List
        result += weaponList(entity).toString();

        // ECM Info
        result += ecmInfo(entity).toString();

        // Bomb List
        result += bombList(entity);

        // StratOps quirks, chassis and weapon
        result += getQuirks(entity, game, details);

        // Partial repairs
        result += getPartialRepairs(entity, details);

        // Carried Units
        result += carriedUnits(entity);

        // C3 Info
        result += c3Info(entity, details);

        String col = "<TD>" + result + "</TD>";
        String row = "<TR>" + col + "</TR>";
        String table = "<TABLE width=100%>" + row + "</TABLE>";

        return new StringBuilder().append(table);
    }

    public static String getTargetTipDetail(Targetable target, @Nullable Client client) {
        if (target instanceof Entity) {
            return UnitToolTip.getEntityTipAsTarget((Entity) target, (client != null) ? client.getLocalPlayer() : null).toString();
        } else if (target instanceof BuildingTarget) {
            return HexTooltip.getBuildingTargetTip((BuildingTarget) target, (client != null) ? client.getBoard() : null, GUIP);
        } else if (target instanceof Hex) {
            return HexTooltip.getHexTip((Hex) target, client, GUIP);
        } else {
            return getTargetTipSummary(target, client);
        }
    }

    public static String getTargetTipSummary(Targetable target, @Nullable Client client) {
        if (target == null) {
            return Messages.getString("BoardView1.Tooltip.NoTarget");
        } else if (target instanceof Entity) {
            return getTargetTipSummaryEntity((Entity) target, client);
        } else if (target instanceof BuildingTarget) {
            return HexTooltip.getOneLineSummary((BuildingTarget) target, (client != null) ? client.getBoard() : null);
        }
        return target.getDisplayName();
    }

    public static String getTargetTipSummaryEntity(Entity entity, @Nullable Client client ) {
        if (entity == null) {
            return Messages.getString("BoardView1.Tooltip.NoTarget");
        }

        // Tooltip info for a sensor blip
        if ((client != null) && EntityVisibilityUtils.onlyDetectedBySensors(client.getLocalPlayer(), entity)) {
            return Messages.getString("BoardView1.sensorReturn");
        }

        String result = getDisplayNames(entity, (client != null) ? client.getGame() : null, true);
        result += "<BR>";
        result += UnitToolTip.getOneLineSummary(entity);
        return result;
    }

    private static String getDisplayNames(Entity entity, @Nullable Game game, boolean showName) {
        String result = "";

        if (showName) {
            // Unit Chassis and Player
            Player owner = (game != null) ? game.getPlayer(entity.getOwnerId()) : null;
            Color ownerColor = (owner != null) ? owner.getColour().getColour() : GUIP.getUnitToolTipFGColor();
            String ownerName = (owner != null) ? owner.getName() : ReportMessages.getString("BoardView1.Tooltip.unknownOwner");
            String msg_clanbrackets = Messages.getString("BoardView1.Tooltip.ClanBrackets");
            String clanStr = entity.isClan() && !entity.isMixedTech() ? " " + msg_clanbrackets + " " : "";
            result = entity.getFullChassis() + clanStr;
            result += " (" + (int) entity.getWeight() + "t)";
            result += "&nbsp;&nbsp;" + entity.getEntityTypeName(entity.getEntityType());
            result += "<BR>" + ownerName;
            String msg_id = MessageFormat.format(" [ID: {0}]", entity.getId());
            result += UIUtil.guiScaledFontHTML(GUIP.getUnitToolTipFGColor()) + msg_id + "</FONT>";
            result = guiScaledFontHTML(ownerColor) + result + "</FONT>";
        }

        return result;
    }

    private static String getPilotInfo(Entity entity, boolean pilotInfoShow, boolean pilotInfoStandard, boolean report) {
        if (!pilotInfoShow) {
            return "";
        }

        if (pilotInfoStandard) {
            return PilotToolTip.getPilotTipShort(entity, GUIP.getshowPilotPortraitTT(), report).toString();
        } else {
            return "<BR>" + PilotToolTip.getPilotTipLine(entity).toString();
        }
    }

    private static String getQuirks(Entity entity, Game game, boolean details) {
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            String sQuirks = "<BR>";
            String quirksList = getOptionList(entity.getQuirks().getGroups(), entity::countQuirks, details);
            if (!quirksList.isEmpty()) {
                sQuirks += quirksList;
            }
            for (Mounted weapon : entity.getWeaponList()) {
                String wpQuirksList = getOptionList(weapon.getQuirks().getGroups(),
                        grp -> weapon.countQuirks(), (e) -> weapon.getDesc(), details);
                if (!wpQuirksList.isEmpty()) {
                    // Line break after weapon name not useful here
                    sQuirks += wpQuirksList.replace(":</I><BR>", ":</I>");
                }
            }

            return  guiScaledFontHTML(GUIP.getUnitToolTipQuirkColor(), TT_SMALLFONT_DELTA) + sQuirks + "</FONT>";
        }

        return "";
    }

    private static String getPartialRepairs(Entity entity, boolean details) {
        String result = "";
        String partialList = getOptionList(entity.getPartialRepairs().getGroups(),
                grp -> entity.countPartialRepairs(), details);

        if (!partialList.isEmpty()) {
            result += guiScaledFontHTML(GUIP.getPrecautionColor(), TT_SMALLFONT_DELTA) + partialList + "</FONT>";
        }

        return result;
    }

    private static boolean hideArmorLocation(Entity entity, int location) {
        return ((entity.getOArmor(location) <= 0) && (entity.getOInternal(location) <= 0) && !entity.hasRearArmor(location))
                || (entity.isConventionalInfantry() && (location != Infantry.LOC_INFANTRY));
    }

    private static String locationHeader(Entity entity, int location) {
        String msg_activetroopers =Messages.getString("BoardView1.Tooltip.ActiveTroopers");
        return entity.isConventionalInfantry() ? ((Infantry) entity).getShootingStrength() + " " + msg_activetroopers : entity.getLocationAbbr(location);
    }

    private static StringBuilder sysCrits(Entity entity, int type, int index, int loc, String locAbbr) {
        String result = "";
        int total = entity.getNumberOfCriticals(type, index, loc);
        int hits = entity.getHitCriticals(type, index, loc);
        int good = total - hits;
        boolean bad = (entity.getBadCriticals(type,index, loc) > 0);

        if ((good + hits) > 0) {
            locAbbr = "&nbsp;&nbsp;" + locAbbr + ":&nbsp;";
            result = guiScaledFontHTML(TT_SMALLFONT_DELTA) + locAbbr + "</FONT>";
            result += systemBar(good, hits, bad);
        }

        return new StringBuilder().append(result);
    }

    private static StringBuilder sysStabilizers(Tank tank, int loc, String locAbbr) {
        String result = "";
        int total = 1;
        int hits = tank.isStabiliserHit(loc) ? 1 : 0;
        int good = total - hits;
        boolean bad = hits > 0;

        locAbbr = "&nbsp;&nbsp;" + locAbbr + ":&nbsp;";
        result = guiScaledFontHTML(TT_SMALLFONT_DELTA) + locAbbr + "</FONT>";
        result += systemBar(good, hits, bad);

        return new StringBuilder().append(result);
    }

    private static StringBuilder sysTurretLocked(Tank tank, int loc, String locAbbr) {
        String result = "";
        int total = 1;
        int hits = tank.isTurretLocked(loc) ? 1 : 0;
        int good = total - hits;
        boolean bad = hits > 0;

        locAbbr = "&nbsp;&nbsp;" + locAbbr + ":&nbsp;";
        result = guiScaledFontHTML(TT_SMALLFONT_DELTA) + locAbbr + "</FONT>";
        result += systemBar(good, hits, bad);

        return new StringBuilder().append(result);
    }

    private static StringBuilder sysEngineHit(Tank tank, String locAbbr) {
        String result = "";
        int total = 1;
        int hits = tank.isEngineHit() ? 1 : 0;
        int good = total - hits;
        boolean bad = hits > 0;

        locAbbr = "&nbsp;&nbsp;" + locAbbr + ":&nbsp;";
        result = guiScaledFontHTML(TT_SMALLFONT_DELTA) + locAbbr + "</FONT>";
        result += systemBar(good, hits, bad);

        return new StringBuilder().append(result);
    }

    private static StringBuilder sysSensorHit(Tank tank, String locAbbr) {
        String result = "";
        int total = Tank.CRIT_SENSOR_MAX;
        int hits = tank.getSensorHits();
        int good = total - hits;
        boolean bad = hits > 0;

        locAbbr = "&nbsp;&nbsp;" + locAbbr + ":&nbsp;";
        result = guiScaledFontHTML(TT_SMALLFONT_DELTA) + locAbbr + "</FONT>";
        result += systemBar(good, hits, bad);

        return new StringBuilder().append(result);
    }

    private static StringBuilder sysMinorMovementDamage(Tank tank, String locAbbr) {
        String result = "";
        int total = 1;
        int hits = tank.hasMinorMovementDamage() ? 1 : 0;
        int good = total - hits;
        boolean bad = hits > 0;

        locAbbr = "&nbsp;&nbsp;" + locAbbr + ":&nbsp;";
        result = guiScaledFontHTML(TT_SMALLFONT_DELTA) + locAbbr + "</FONT>";
        result += systemBar(good, hits, bad);

        return new StringBuilder().append(result);
    }

    private static StringBuilder sysModerateMovementDamage(Tank tank, String locAbbr) {
        String result = "";
        int total = 1;
        int hits = tank.hasModerateMovementDamage() ? 1 : 0;
        int good = total - hits;
        boolean bad = hits > 0;

        locAbbr = "&nbsp;&nbsp;" + locAbbr + ":&nbsp;";
        result = guiScaledFontHTML(TT_SMALLFONT_DELTA) + locAbbr + "</FONT>";
        result += systemBar(good, hits, bad);

        return new StringBuilder().append(result);
    }

    private static StringBuilder sysHeavyMovementDamage(Tank tank, String locAbbr) {
        String result = "";
        int total = 1;
        int hits = tank.hasHeavyMovementDamage() ? 1 : 0;
        int good = total - hits;
        boolean bad = hits > 0;

        locAbbr = "&nbsp;&nbsp;" + locAbbr + ":&nbsp;";
        result = guiScaledFontHTML(TT_SMALLFONT_DELTA) + locAbbr + "</FONT>";
        result += systemBar(good, hits, bad);

        return new StringBuilder().append(result);
    }

    /** Returns the graphical Armor representation. */
    private static StringBuilder addArmorMiniVisToTT(Entity entity) {
        if (!GUIP.getshowArmorMiniVisTT()) {
            return new StringBuilder();
        }

        String armorChar = GUIP.getUnitToolTipArmorMiniArmorChar();
        if (entity.isCapitalScale()) {
            armorChar = GUIP.getUnitToolTipArmorMiniCapArmorChar();
        }
        String internalChar = GUIP.getUnitToolTipArmorMiniISChar();
        String col1 = "";
        String col2 = "";
        String col3 = "";
        String row = "";
        String rows = "";

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
        String msg_abbr_stabilizers = Messages.getString("BoardView1.Tooltip.AbbreviationStabilizers");
        String msg_abbr_turretlocked = Messages.getString("BoardView1.Tooltip.AbbreviationTurretLocked");
        String msg_abbr_minormovementdamage = Messages.getString("BoardView1.Tooltip.AbbreviationMinorMovementDamage");
        String msg_abbr_moderatemovementdamage = Messages.getString("BoardView1.Tooltip.AbbreviationModerateMovementDamage");
        String msg_abbr_heavymovementdamage = Messages.getString("BoardView1.Tooltip.AbbreviationHeavyMovementDamage");

        for (int loc = 0 ; loc < entity.locations(); loc++) {
            // do not show locations that do not support/have armor/internals like HULL on Aero
            if (hideArmorLocation(entity, loc)) {
                continue;
            }

            boolean locDestroyed = (entity.getInternal(loc) == IArmorState.ARMOR_DOOMED || entity.getInternal(loc) == IArmorState.ARMOR_DESTROYED);

            if (locDestroyed) {
                // Destroyed location
                col1 = "";
                String sLocHeader = "&nbsp;&nbsp;" + locationHeader(entity, loc) + ":&nbsp;";
                col2 = guiScaledFontHTML(TT_SMALLFONT_DELTA) + sLocHeader + "</FONT>";
                col2 += destroyedLocBar(entity.getOArmor(loc, true)).toString();
            } else {
                // Rear armor
                if (entity.hasRearArmor(loc)) {
                    String msg_abbr_rear = Messages.getString("BoardView1.Tooltip.AbbreviationRear");
                    String sLocHeader = "&nbsp;&nbsp;" + locationHeader(entity, loc) + msg_abbr_rear + "&nbsp;";
                    col1 = guiScaledFontHTML(TT_SMALLFONT_DELTA) + sLocHeader + "</FONT>";
                    col1 += intactLocBar(entity.getOArmor(loc, true), entity.getArmor(loc, true), armorChar).toString();
                } else {
                    // No rear armor: empty table cells instead
                    // At small font sizes, writing one character at the correct font size is
                    // necessary to prevent the table rows from being spaced non-beautifully
                    col1 = guiScaledFontHTML(TT_SMALLFONT_DELTA) + "&nbsp;" + "</FONT>";
                }
                // Front armor
                String sLocHeader = "&nbsp;&nbsp;" + locationHeader(entity, loc) + ":&nbsp;";
                col2 = guiScaledFontHTML(TT_SMALLFONT_DELTA) + sLocHeader + "</FONT>";
                col2 += intactLocBar(entity.getOInternal(loc), entity.getInternal(loc), internalChar).toString();
                col2 += intactLocBar(entity.getOArmor(loc), entity.getArmor(loc), armorChar).toString();
            }

            if (entity instanceof Mech) {
                switch (loc) {
                    case Mech.LOC_HEAD:
                        col3 = sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, loc, msg_abbr_sensors).toString();
                        col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, loc, msg_abbr_lifesupport).toString();
                        break;
                    case Mech.LOC_CT:
                        col3 = sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, loc, msg_abbr_engine).toString();
                        col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, loc, msg_abbr_gyro).toString();
                        col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, loc, msg_abbr_sensors).toString();
                        col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, loc, msg_abbr_lifesupport).toString();
                        break;
                    case Mech.LOC_RT:
                    case Mech.LOC_LT:
                        col3 = sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, loc, msg_abbr_engine).toString();
                        col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, loc, msg_abbr_lifesupport).toString();
                        break;
                    case Mech.LOC_RARM:
                    case Mech.LOC_LARM:
                        col3 = sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER, loc, msg_abbr_shoulder).toString();
                        col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_ARM, loc, msg_abbr_upperarm).toString();
                        col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_ARM, loc, msg_abbr_lowerarm).toString();
                        col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HAND, loc, msg_abbr_hand).toString();
                        col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc, msg_abbr_hip).toString();
                        col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, loc, msg_abbr_upperleg).toString();
                        col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, loc, msg_abbr_lowerleg).toString();
                        col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, loc, msg_abbr_foot).toString();
                        break;
                    case Mech.LOC_RLEG:
                    case Mech.LOC_LLEG:
                    case Mech.LOC_CLEG:
                        col3 = sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc, msg_abbr_hip).toString();
                        col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, loc, msg_abbr_upperleg).toString();
                        col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, loc, msg_abbr_lowerleg).toString();
                        col3 += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, loc, msg_abbr_foot).toString();
                        break;
                    default:
                        col3 = "";
                }
            } else if (entity instanceof SuperHeavyTank || entity instanceof LargeSupportTank) {
                Tank tank = (Tank) entity;

                switch (loc) {
                    case SuperHeavyTank.LOC_BODY:
                    case SuperHeavyTank.LOC_FRONT:
                    case SuperHeavyTank.LOC_RIGHT:
                    case SuperHeavyTank.LOC_LEFT:
                    case SuperHeavyTank.LOC_REARRIGHT:
                    case SuperHeavyTank.LOC_REARLEFT:
                    case SuperHeavyTank.LOC_REAR:
                        col3 = sysStabilizers(tank, loc, msg_abbr_stabilizers).toString();
                        break;
                    case SuperHeavyTank.LOC_TURRET:
                    case SuperHeavyTank.LOC_TURRET_2:
                        col3 = sysStabilizers(tank, loc, msg_abbr_stabilizers).toString();
                        col3 += tank.getTurretCount() > 0 ? sysTurretLocked(tank, loc, msg_abbr_turretlocked).toString() : "";
                        break;
                    default:
                        col3 = "";
                }
            } else if (entity instanceof Tank) {
                Tank tank = (Tank) entity;

                switch (loc) {
                    case Tank.LOC_BODY:
                    case Tank.LOC_FRONT:
                    case Tank.LOC_RIGHT:
                    case Tank.LOC_LEFT:
                    case Tank.LOC_REAR:
                        col3 = sysStabilizers(tank, loc, msg_abbr_stabilizers).toString();
                        break;
                    case Tank.LOC_TURRET:
                    case Tank.LOC_TURRET_2:
                        col3 = sysStabilizers(tank, loc, msg_abbr_stabilizers).toString();
                        col3 += tank.getTurretCount() > 0 ? sysTurretLocked(tank, loc, msg_abbr_turretlocked).toString() : "";
                        break;
                    default:
                        col3 = "";
                }
            }

            col1 = "<TD>" + col1 + "</TD>";
            col2 = "<TD>" + col2 + "</TD>";
            col3 = "<TD>" + col3 + "</TD>";
            row = "<TR>" + col1 + col2 + col3 + "</TR>";
            rows += row;
        }

        if (entity instanceof GunEmplacement) {
            Tank tank = (Tank) entity;
            col1 = "";
            col2 = sysSensorHit(tank, msg_abbr_sensors).toString();
            col3 = "";

            col1 = "<TD>" + col1 + "</TD>";
            col2 = "<TD>" + col2 + "</TD>";
            col3 = "<TD>" + col3 + "</TD>";
            row = "<TR>" + col1 + col2 + col3 + "</TR>";
            rows += row;
        } else if (entity instanceof VTOL) {
            Tank tank = (Tank) entity;
            col1 = "";
            col2 = sysEngineHit(tank, msg_abbr_engine).toString();
            col2 += sysSensorHit(tank, msg_abbr_sensors).toString();
            col3 = "";

            col1 = "<TD>" + col1 + "</TD>";
            col2 = "<TD>" + col2 + "</TD>";
            col3 = "<TD>" + col3 + "</TD>";
            row = "<TR>" + col1 + col2 + col3 + "</TR>";
            rows += row;
        } else if (entity instanceof Tank) {
            Tank tank = (Tank) entity;
            col1 = "";
            col2 = sysEngineHit(tank, msg_abbr_engine).toString();
            col2 += sysSensorHit(tank, msg_abbr_sensors).toString();
            col3 = sysMinorMovementDamage(tank, msg_abbr_minormovementdamage).toString();
            col3 += sysModerateMovementDamage(tank, msg_abbr_moderatemovementdamage).toString();
            col3 += sysHeavyMovementDamage(tank, msg_abbr_heavymovementdamage).toString();

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
        String destroyedChar = GUIP.getUnitToolTipArmorMiniDestoryedChar();
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
        Color colorIntact = GUIP.getColor(GUIPreferences.UNIT_TOOLTIP_ARMORMINI_COLOR_INTACT);
        Color colorPartialDmg = GUIP.getColor(GUIPreferences.UNIT_TOOLTIP_ARMORMINI_COLOR_PARTIAL_DMG);
        Color colorDamaged = GUIP.getColor(GUIPreferences.UNIT_TOOLTIP_ARMORMINI_COLOR_DAMAGED);
        int visUnit = GUIP.getInt(GUIPreferences.UNIT_TOOLTIP_ARMORMINI_UNITS_PER_BLOCK);

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
                String sIntact = dChar + msg_x + tensIntact * 10;
                sIntact += repeat(dChar, numIntact - 10 * tensIntact);
                result += guiScaledFontHTML(colorIntact, TT_SMALLFONT_DELTA) + sIntact + "</FONT>";
            } else {
                String sIntact = repeat(dChar, numIntact);
                result += guiScaledFontHTML(colorIntact, TT_SMALLFONT_DELTA) + sIntact  + "</FONT>";
            }
        }
        if (numPartial > 0) {
            String sPartial = repeat(dChar, numPartial);
            result += guiScaledFontHTML(colorPartialDmg, TT_SMALLFONT_DELTA) + sPartial + "</FONT>";
        }
        if (numDmgd > 0) {
            if (numDmgd > 15 && numIntact + numDmgd > 30) {
                int tensDmgd = (numDmgd - 1) / 10;
                String msg_x = Messages.getString("BoardView1.Tooltip.X");
                String sDamage = dChar + msg_x + tensDmgd * 10;
                sDamage += repeat(dChar, numDmgd - 10 * tensDmgd);
                result +=  guiScaledFontHTML(colorIntact, TT_SMALLFONT_DELTA) + sDamage + "</FONT>";
            } else {
                String sDamage  = repeat(dChar, numDmgd);
                result += guiScaledFontHTML(colorDamaged, TT_SMALLFONT_DELTA) + sDamage + "</FONT>";
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
        Color colorIntact = GUIP.getUnitTooltipArmorMiniColorIntact();
        Color colorDamaged = GUIP.getUnitTooltipArmorMiniColorDamaged();
        String dChar =  GUIP.getUnitToolTipArmorMiniDestoryedChar();
        String iChar = GUIP.getUnitToolTipArmorMiniCriticalChar();

        if (good > 0)  {
            if (!destroyed) {
                String sGood = repeat(iChar, good);
                result = guiScaledFontHTML(colorIntact, TT_SMALLFONT_DELTA) + sGood + "</FONT>";
            } else {
                String sGood = repeat(iChar, good);
                result = guiScaledFontHTML(colorDamaged, TT_SMALLFONT_DELTA) + sGood + "</FONT>";
            }
        }
        if (bad > 0) {
            String sBad =repeat(dChar, bad);
            result += guiScaledFontHTML(colorDamaged, TT_SMALLFONT_DELTA) + sBad + "</FONT>";
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
        if (!GUIP.getShowWpsinTT()) {
            return new StringBuilder();
        }

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

            if (GUIP.getShowWpsLocinTT() && (entity.locations() > 1)) {
                weapDesc += " ["+entity.getLocationAbbr(curWp.getLocation()) + ']';
            }

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

                col1 = guiScaledFontHTML(GUIP.getUnitToolTipWeaponColor()) + col1 + "</FONT>";
                col1 = "<TD>" + col1 + "</TD>";
                col2 = guiScaledFontHTML(GUIP.getUnitToolTipWeaponColor()) + col2 + "</FONT>";
                col2 = "<TD>" + col2 + "</TD>";
                row = "<TR>" + col1 + col2 + "</TR>";
            }

            rows += row;
        }

        String tbody = "<TBODY>" + rows + "</TBODY>";
        String table = "<TABLE CELLSPACING=0 CELLPADDING=0>" + tbody + "</TABLE>";
        table = guiScaledFontHTML() + table + "</FONT>";

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

                    col1 = guiScaledFontHTML(GUIP.getUnitToolTipWeaponColor()) + col1 + "</FONT>";
                    col1 = "<TD>" + col1 + "</TD>";
                    col2 = guiScaledFontHTML(GUIP.getUnitToolTipWeaponColor()) + col2 + "</FONT>";
                    col2 = "<TD>" + col2 + "</TD>";
                    col3 = guiScaledFontHTML(GUIP.getUnitToolTipWeaponColor()) + col3 + "</FONT>";
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
            col2 = guiScaledFontHTML(GUIP.getCautionColor(), -0.2f) + col2 + "</FONT>";
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
                col2 = guiScaledFontHTML(GUIP.getCautionColor(), -0.2f) + col2 + "</FONT>";
                col2 = "<TD>" + col2 + "</TD>";
                row = "<TR>" + col1 + col2 + "</TR>";
                rows += row;
            }
        }

        return new StringBuilder().append(rows);
    }

    /** Returns a line showing ECM / ECCM. */
    private static StringBuilder ecmInfo(Entity entity) {
        String sECMInfo = "";
        String result = "";
        if (entity.hasActiveECM()) {
            String msg_ecmsource = Messages.getString("BoardView1.ecmSource");
            sECMInfo += ECM_SIGN + " " + msg_ecmsource;
        }
        if (entity.hasActiveECCM()) {
            String msg_eccmsource = Messages.getString("BoardView1.eccmSource");
            sECMInfo += ECM_SIGN+ " " +msg_eccmsource;
        }

        result = guiScaledFontHTML() + sECMInfo + "</FONT>";
        result = guiScaledFontHTML() + result + "</FONT>";

        return new StringBuilder().append(result);
    }

    public static class HeatDisplayHelper {
        public String heatCapacityStr;
        public int heatCapWater;
        public HeatDisplayHelper(String heatCapacityStr, int heatCapWater) {
            this.heatCapacityStr = heatCapacityStr;
            this.heatCapWater = heatCapWater;
        }
    }

    /**
     * returns total heat capacity factoring in normal capacity, water and radical HS
     *
     */
    public static HeatDisplayHelper getHeatCapacityForDisplay(Entity e) {
        int heatCap;

        if (e instanceof Mech) {
            Mech m = (Mech) e;
            heatCap = m.getHeatCapacity(true, false);
        } else if (e instanceof Aero) {
            Aero a = (Aero) e;
            heatCap = a.getHeatCapacity(false);
        } else {
            heatCap = e.getHeatCapacity();
        }

        int heatCapOrg = heatCap;

        int heatCapWater = e.getHeatCapacityWithWater();

        if (e instanceof Mech) {
            Mech m = (Mech) e;
            if (m.getCoolantFailureAmount() > 0) {
                heatCap -= m.getCoolantFailureAmount();
                heatCapWater -= m.getCoolantFailureAmount();
            }
        }

        if (e.hasActivatedRadicalHS()) {
            if (e instanceof Mech) {
                Mech m = (Mech) e;
                heatCap += m.getActiveSinks();
                heatCapWater += m.getActiveSinks();
            } else if (e instanceof Aero) {
                Aero a = (Aero) e;
                heatCap += a.getHeatSinks();
                heatCapWater += a.getHeatSinks();
            }
        }

        String heatCapacityStr = Integer.toString(heatCap) ;

        if (heatCap < heatCapOrg) {
            heatCapacityStr += "*";
        }

        if (heatCap < heatCapWater) {
            heatCapacityStr += " [" + heatCapWater + ']';
        }

        return new HeatDisplayHelper(heatCapacityStr, heatCapWater);
    }

    public static String getOneLineSummary(Entity entity) {
        String result = "";
        boolean isGunEmplacement = entity instanceof GunEmplacement;
        String armorStr = entity.getTotalArmor() + " / " + entity.getTotalOArmor();
        String internalStr = entity.getTotalInternal() + " / " + entity.getTotalOInternal();
        result += Messages.getString("BoardView1.Tooltip.ArmorInternals",armorStr, internalStr);
        result += ' ' + getDamageLevelDesc(entity, true);

        if (!isGunEmplacement && entity.isImmobile()) {
            result += ' '+guiScaledFontHTML(GUIP.getWarningColor()) + Messages.getString("BoardView1.Tooltip.Immobile") + "</FONT>";
        }

        // Unit Prone
        if (!isGunEmplacement && entity.isProne()) {
            result += ' '+guiScaledFontHTML(GUIP.getWarningColor()) + Messages.getString("BoardView1.Tooltip.Prone") + "</FONT>";
        }

        return result;
    }

    public static String getSensorDesc(Entity e) {
        Compute.SensorRangeHelper srh = Compute.getSensorRanges(e.getGame(), e);

        if (srh == null) {
            return Messages.getString("NONE");
        }

        if (e.isAirborne() && e.getGame().getBoard().onGround()) {
            return e.getActiveSensor().getDisplayName() + " (" + srh.minSensorRange + "-"
                    + srh.maxSensorRange + ")" + " {" + Messages.getString("BoardView1.Tooltip.sensor_range_vs_ground_target")
                    + " (" + srh.minGroundSensorRange + "-" + srh.maxGroundSensorRange + ")}";
        }
        return e.getActiveSensor().getDisplayName() + " (" + srh.minSensorRange + "-"
                + srh.maxSensorRange + ")";
    }

    public static String getDamageLevelDesc(Entity entity, boolean useHtml) {
        String result;

        if (entity.isDoomed() || entity.isDestroyed()) {
            String msg_destroyed = Messages.getString("BoardView1.Tooltip.Destroyed");
            return useHtml ? guiScaledFontHTML(GUIP.getWarningColor()) + msg_destroyed + "</FONT>" : msg_destroyed;
        }

        switch (entity.getDamageLevel()) {
            case Entity.DMG_CRIPPLED:
                String msg_crippled = Messages.getString("BoardView1.Tooltip.Crippled");
                result = useHtml ? guiScaledFontHTML(GUIP.getWarningColor()) + msg_crippled + "</FONT>" : msg_crippled;
                break;
            case Entity.DMG_HEAVY:
                String msg_heavydmg = Messages.getString("BoardView1.Tooltip.HeavyDmg");
                result = useHtml ? guiScaledFontHTML(GUIP.getWarningColor()) + msg_heavydmg + "</FONT>" : msg_heavydmg;
                break;
            case Entity.DMG_MODERATE:
                String msg_moderatedmg = Messages.getString("BoardView1.Tooltip.ModerateDmg");
                result = msg_moderatedmg;
                break;
            case Entity.DMG_LIGHT:
                String msg_lightdmg = Messages.getString("BoardView1.Tooltip.LightDmg");
                result = msg_lightdmg ;
                break;
            default:
                String msg_undamaged = Messages.getString("BoardView1.Tooltip.Undamaged");
                result = msg_undamaged;
                break;
        }
        return result;
    }

    /** Returns values that only are relevant when in-game such as heat. */
    private static StringBuilder inGameValues(Entity entity, Player localPlayer, boolean inGameValue,
                                              boolean showBV, boolean showSensors, boolean showSeenBy) {
        Game game = entity.getGame();
        GameOptions gameOptions = game.getOptions();
        boolean isGunEmplacement = entity instanceof GunEmplacement;
        String result = "";

        if (!inGameValue) {
            return new StringBuilder();
        }

        if (showBV) {
            // BV Info
            // Hidden for invisible units when in double blind and hide enemy bv is selected
            // Also not shown in the lobby as BV is shown there outside the tooltip
            boolean showEnemyBV = !(gameOptions.booleanOption(OptionsConstants.ADVANCED_SUPPRESS_DB_BV) && gameOptions.booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND));
            boolean isVisible = EntityVisibilityUtils.trackThisEntitiesVisibilityInfo(localPlayer, entity);

            if (isVisible || showEnemyBV) {
                int currentBV = entity.calculateBattleValue(false, false);
                int initialBV = entity.getInitialBV();
                double percentage = (double) currentBV / initialBV;
                result += addToTT("BV", BR, currentBV, initialBV, percentage).toString();
            }
        }

        result += " " + getDamageLevelDesc(entity, true);

        // Actual Movement
        if (!isGunEmplacement) {
            // "Has not yet moved" only during movement phase
            if (!entity.isDone() && game.getPhase().isMovement()) {
                String sNotYetMoved = addToTT("NotYetMoved", BR).toString();
                sNotYetMoved = "<I>" + sNotYetMoved + "</I>";
                result += guiScaledFontHTML(GUIP.getColorForMovement(entity.moved)) + sNotYetMoved + "</FONT>";
            } else if ((entity.isDone() && game.getPhase().isMovement())
                    || (game.getPhase().isMovementReport())
                    || (game.getPhase().isFiring())
                    || (game.getPhase().isFiringReport())
                    || (game.getPhase().isPhysical())
                    || (game.getPhase().isPhysicalReport())) {
                int tmm = Compute.getTargetMovementModifier(game, entity.getId()).getValue();
                String sMove = "";

                if (entity.moved == EntityMovementType.MOVE_NONE) {
                    sMove = addToTT("NoMove", BR, tmm).toString();
                    sMove = "<I>" + sMove + "</I>";
                } else {
                    sMove = addToTT("MovementF", BR, entity.getMovementString(entity.moved),
                            entity.delta_distance, tmm).toString();
                    sMove = "<I>" + sMove + "</I>";
                }

                // Special Moves
                if (entity.isEvading()) {
                    String sSpecialMove = addToTT("Evade", BR).toString();
                    sSpecialMove = "<I>" + sSpecialMove + "</I>";
                    sSpecialMove = guiScaledFontHTML(GUIP.getPrecautionColor()) + sSpecialMove + "</FONT>";
                    sMove += sSpecialMove;
                }

                if ((entity instanceof Infantry) && ((Infantry) entity).isTakingCover()) {
                    String sTakingCover = addToTT("TakingCover", BR).toString();
                    sTakingCover = "<I>" + sTakingCover + "</I>";
                    sTakingCover = guiScaledFontHTML(GUIP.getPrecautionColor()) + sTakingCover + "</FONT>";
                    sMove += sTakingCover;
                }

                if (entity.isCharging()) {
                    sMove += addToTT("Charging", BR).toString();
                }

                if (entity.isMakingDfa()) {
                    String sDFA = addToTT("DFA", BR).toString();
                    sDFA = "<I>" + sDFA + "</I>";
                    sDFA = guiScaledFontHTML(GUIP.getWarningColor()) + sDFA + "</FONT>";
                    sMove += sDFA;
                }

                if (entity.isUnjammingRAC()) {
                    String sUnJamming = "<BR>";
                    String msg_unjammingrac = Messages.getString("BoardView1.Tooltip.UnjammingRAC");
                    sUnJamming += msg_unjammingrac;
                    if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_UNJAM_UAC)) {
                        String msg_andac = Messages.getString("BoardView1.Tooltip.AndAC");
                        sUnJamming += msg_andac;
                    }
                    sMove += sUnJamming;
                }

                result += guiScaledFontHTML(GUIP.getColorForMovement(entity.moved)) + sMove + "</FONT>";
            }
        }

        if (entity instanceof Infantry) {
            InfantryMount mount = ((Infantry) entity).getMount();
            if ((mount != null) && entity.getMovementMode().isSubmarine() && (entity.underwaterRounds > 0)) {
                String uw = "<br/>" + addToTT("InfUWDuration", NOBR, mount.getUWEndurance() - entity.underwaterRounds).toString();
                if (entity.underwaterRounds >=mount.getUWEndurance()) {
                    uw = guiScaledFontHTML(GUIP.getWarningColor()) + uw + "</font>";
                }
                result += uw;
            }
        }

        String sAeroInfo = "";

        if (entity.isAero()) {
            // Velocity, Altitude, Elevation, Fuel
            IAero aero = (IAero) entity;
            sAeroInfo = addToTT("AeroVelAltFuel", BR, aero.getCurrentVelocity(), aero.getAltitude(), aero.getCurrentFuel()).toString();
        } else if (entity.getElevation() != 0) {
            // Elevation only
            sAeroInfo = addToTT("Elev", BR, entity.getElevation()).toString();
        }
        sAeroInfo = "<I>" + sAeroInfo + "</I>";
        result += guiScaledFontHTML(GUIP.getUnitToolTipHighlightColor()) + sAeroInfo + "</FONT>";

        result += "<BR>";
        String msg_facing = Messages.getString("BoardView1.Tooltip.Facing");
        String sFacingTwist = "&nbsp;&nbsp;" + msg_facing + ":&nbsp;" + entity.getFacingName(entity.getFacing());
        if (entity.getFacing() != entity.getSecondaryFacing()) {
            String msg_twist = Messages.getString("BoardView1.Tooltip.Twist");
            sFacingTwist += "&nbsp;&nbsp;" + msg_twist + ":&nbsp;" + entity.getFacingName(entity.getSecondaryFacing());
        }
        result += guiScaledFontHTML() + sFacingTwist + "</FONT>";

        // Heat, not shown for units with 999 heat sinks (vehicles)
        if (entity.getHeatCapacity() != 999) {
            int heat = entity.heat;
            String sHeat = "";
            if (heat == 0) {
                sHeat += addToTT("Heat0", BR).toString();
            } else {
                sHeat += addToTT("Heat", BR, heat).toString();
            }
            HeatDisplayHelper hdh = getHeatCapacityForDisplay(entity);
            sHeat += " / "+ hdh.heatCapacityStr;
            result += guiScaledFontHTML(GUIP.getColorForHeat(heat, GUIP.getUnitToolTipFGColor())) + sHeat + "</FONT>";

            if (entity instanceof Mech && ((Mech) entity).hasActiveTSM()) {
                result += DOT_SPACER;
                String sTSM = "TSM";
                result += guiScaledFontHTML(GUIP.getPrecautionColor()) + sTSM + "</FONT>";
            }
        }

        String illuminated = entity.isIlluminated() ?  DOT_SPACER +"\uD83D\uDCA1" : "";
        result += guiScaledFontHTML(GUIP.getCautionColor()) + illuminated + "</FONT>";

        if (entity.hasSearchlight()) {
            String searchLight = entity.isUsingSearchlight() ? DOT_SPACER + "\uD83D\uDD26" : "";
            searchLight += entity.usedSearchlight() ? " \u2580\u2580" : "";
            result += guiScaledFontHTML(GUIP.getCautionColor()) + searchLight + "</FONT>";
        } else {
            String searchLight = "\uD83D\uDD26";
            result += guiScaledFontHTML(GUIP.getWarningColor()) + DOT_SPACER + searchLight + "</FONT>";
        }

        // Gun Emplacement Status
        if (isGunEmplacement) {
            GunEmplacement emp = (GunEmplacement) entity;
            if (emp.isTurret() && emp.isTurretLocked(emp.getLocTurret())) {
                String sTurretLocked = addToTT("TurretLocked", BR).toString();
                sTurretLocked = "<I>" + sTurretLocked + "</I>";
                result += guiScaledFontHTML(GUIP.getWarningColor()) + sTurretLocked + "</FONT>";
            }
        }

        // Unit Immobile
        if (!isGunEmplacement && entity.isImmobile()) {
            String sImmobile = addToTT("Immobile", BR).toString();
            result += guiScaledFontHTML(GUIP.getWarningColor()) + sImmobile + "</FONT>";
        }

        // Unit Prone
        if (!isGunEmplacement && entity.isProne()) {
            String sUnitProne = addToTT("Prone", BR).toString();
            result += guiScaledFontHTML(GUIP.getCautionColor()) + sUnitProne + "</FONT>";
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
            String sSwarmed = addToTT("Swarmed", BR, sa).toString();
            result += guiScaledFontHTML(GUIP.getWarningColor()) + sSwarmed + "</FONT>";
        }

        // Spotting
        if (entity.isSpotting() && game.hasEntity(entity.getSpotTargetId())) {
            String sSpotting = addToTT("Spotting", BR, game.getEntity(entity.getSpotTargetId()).getDisplayName()).toString();
            result += guiScaledFontHTML() + sSpotting + "</FONT>";
        }

        if (showSeenBy) {
            // If Double Blind, add information about who sees this Entity
            if (gameOptions.booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
                StringBuffer tempList = new StringBuffer();
                boolean teamVision = gameOptions.booleanOption(OptionsConstants.ADVANCED_TEAM_VISION);
                int seenByResolution = GUIP.getUnitToolTipSeenByResolution();
                String tmpStr = "";

                dance:
                for (Player player : entity.getWhoCanSee()) {
                    if (player.isEnemyOf(entity.getOwner()) || !teamVision) {
                        switch (seenByResolution) {
                            case 0:
                                String msg_someone = Messages.getString("BoardView1.Tooltip.Someone");
                                tempList.append(msg_someone);
                                tempList.append(", ");
                                break dance;
                            case 1:
                                Team team = game.getTeamForPlayer(player);
                                tmpStr = team != null ? team.toString() : "";
                                break;
                            case 2:
                                tmpStr = player.getName();
                                break;
                            case 3:
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
                    String sSeenBy = addToTT("SeenBy", BR, tempList.toString()).toString();
                    result += guiScaledFontHTML() + sSeenBy + "</FONT>";
                }
            }
        }

        if (showSensors) {
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            // If sensors, display what sensors this unit is using
            if (gameOptions.booleanOption(OptionsConstants.ADVANCED_TACOPS_SENSORS)
                    || (gameOptions.booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS)) && entity.isSpaceborne()) {
                String visualRange = Compute.getMaxVisualRange(entity, false) + "";
                if (conditions.isIlluminationEffective()) {
                    visualRange += " (" + Compute.getMaxVisualRange(entity, true) + ")";
                }
                result += addToTT("Sensors", BR, getSensorDesc(entity), visualRange);
            } else {
                String visualRange = Compute.getMaxVisualRange(entity, false) + "";
                if (conditions.isIlluminationEffective()) {
                    visualRange += " (" + Compute.getMaxVisualRange(entity, true) + ")";
                }
                result += addToTT("Visual", BR, visualRange);
            }
            if (gameOptions.booleanOption(OptionsConstants.ADVANCED_TACOPS_BAP) && entity.hasBAP()) {
                result += addToTT("BAPRange", NOBR, entity.getBAPRange());
            }
        }

        if (entity.hasAnyTypeNarcPodsAttached()) {
            String sNarced = addToTT(entity.hasNarcPodsAttached() ? "Narced" : "INarced", BR).toString();
            result += guiScaledFontHTML(GUIP.getPrecautionColor()) + sNarced + "</FONT>";
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

        // Coloring to make these transient entries stand out
        result = guiScaledFontHTML(GUIP.getUnitToolTipHighlightColor()) + result + "</FONT>";

        return new StringBuilder().append(result);
    }

    /** Returns unit values that are relevant in-game and in the lobby such as movement ability. */
    private static StringBuilder getMovement(Entity entity) {
        boolean isGunEmplacement = entity instanceof GunEmplacement;
        String result = "";
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

                        partialWingWeaterMod = ((Mech) entity).getPartialWingJumpAtmoBonus()
                                - ((Mech) entity).getPartialWingJumpWeightClassBonus();
                    }
                }

                paritalWing += paritalWingDistroyed;
            }

            int walkMP = entity.getOriginalWalkMP();
            int runMP = entity.getOriginalRunMP();
            int jumpMP = entity.getOriginalJumpMP();
            String sMove = addToTT("Movement", NOBR , walkMP,  runMP).toString();

            if (jumpMP > 0) {
                sMove += "/" + jumpMP;
            }

            int walkMPModified = entity.getWalkMP();
            int runMPModified = entity.getRunMP();
            int jumpMPModified = entity.getJumpMP();

            if ((walkMP != walkMPModified) || (runMP != runMPModified) || (jumpMP != jumpMPModified)) {
                sMove += DOT_SPACER + walkMPModified + "/" + runMPModified;
                if (jumpMPModified > 0) {
                    sMove += "/" + jumpMPModified;
                }
            } else if ((entity instanceof Jumpship) && ((Jumpship) entity).hasStationKeepingDrive()) {
                sMove += String.format("%s%1.1f", DOT_SPACER, ((Jumpship) entity).getAccumulatedThrust());
            }

            sMove += DOT_SPACER;
            String sMoveMode = entity.getMovementModeAsString();
            sMove += sMoveMode;

            if ((walkMP != walkMPModified) || (runMP != runMPModified) || (jumpMP != jumpMPModified)) {
                if (entity.getGame().getPlanetaryConditions().getGravity()  != 1.0) {
                    sMove += DOT_SPACER;
                    String sGravity =  entity.getGame().getPlanetaryConditions().getGravity() + "g";
                    sMove += guiScaledFontHTML(GUIP.getWarningColor()) + sGravity + "</FONT>";
                }
                int walkMPNoHeat = entity.getWalkMP(MPCalculationSetting.NO_HEAT);
                int runMPNoHeat = entity.getRunMP(MPCalculationSetting.NO_HEAT);
                if ((walkMPNoHeat != walkMPModified) || (runMPNoHeat != runMPModified)) {
                    sMove += DOT_SPACER;
                    String sHeat = "\uD83D\uDD25";
                    sMove += guiScaledFontHTML(GUIP.getWarningColor()) + sHeat + "</FONT>";
                }
            }

            if (entity instanceof IBomber) {
                int bombMod = 0;
                bombMod = ((IBomber) entity).reduceMPByBombLoad(walkMP);
                if (bombMod != walkMP) {
                    sMove += DOT_SPACER;
                    String sBomb = "\uD83D\uDCA3";
                    sMove += guiScaledFontHTML(GUIP.getWarningColor()) + sBomb + "</FONT>";
                }
            }

            int weatherMod = entity.getGame().getPlanetaryConditions().getMovementMods(entity);

            if ((weatherMod != 0) || (partialWingWeaterMod != 0)) {
                sMove += DOT_SPACER;
                String sWeather = "\u2602";
                sMove += guiScaledFontHTML(GUIP.getWarningColor()) + sWeather + "</FONT>";
            }

            if ((legsDestroyed > 0) || (hipHits > 0) || (actuatorHits > 0) || (jumpJetDistroyed > 0) || (paritalWingDistroyed > 0)
                    || (jumpBoosterDistroyed > 0) || (entity.isImmobile()) || (entity.isGyroDestroyed())) {
                sMove += DOT_SPACER;
                String sDamage = "\uD83D\uDD27";
                sMove += guiScaledFontHTML(GUIP.getWarningColor()) + sDamage + "</FONT>";
            }

            if ((entity instanceof BipedMech) || (entity instanceof TripodMech)) {
                int shieldMod = 0;
                if (entity.hasShield()) {
                    shieldMod -= entity.getNumberOfShields(MiscType.S_SHIELD_LARGE);
                    shieldMod -= entity.getNumberOfShields(MiscType.S_SHIELD_MEDIUM);
                }

                if (shieldMod != 0) {
                    sMove += DOT_SPACER;
                    String sShield = "\u26E8";
                    sMove += guiScaledFontHTML(GUIP.getWarningColor()) + sShield + "</FONT>";
                }
            }

            if (entity.hasModularArmor()) {
                sMove += DOT_SPACER;
                String sArmor = "\u27EC\u25AE";
                sMove += DOT_SPACER + guiScaledFontHTML(GUIP.getWarningColor()) + sArmor + "</FONT>";
            }

            l1 = "<Li style=\"list-style-type: none; list-style-image: none; margin: 0; padding: 0; width: 300px;\">" + sMove + "</Li>";

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
                String sInfantrySpec = addToTT("InfSpec", NOBR, Infantry.getSpecializationName(spec)).toString();
                l3 = "<Li style=\"list-style-type: none; list-style-image: none; margin: 0; padding: 0;\">" + sInfantrySpec + "</Li>";
            }
        }

        String ul = "<UL style=\"list-style-type: none; list-style-image: none; margin: 0; padding: 0;\">" + l1 + l2 + l3 + "</UL>";
        result = guiScaledFontHTML() + ul + "</FONT>";

        return new StringBuilder().append(result);
    }

    private static StringBuilder getArmor(Entity entity) {
        boolean isGunEmplacement = entity instanceof GunEmplacement;
        String result = "";
        String l1= "";

        // Armor and Internals
        if (entity instanceof FighterSquadron) {
            String msg_armorcapital = Messages.getString("BoardView1.Tooltip.ArmorCapital");
            String armorStr = entity.getTotalArmor() + " / " + entity.getTotalOArmor() + " " + msg_armorcapital;
            String sArmorInternals = Messages.getString("BoardView1.Tooltip.FSQTotalArmor", armorStr);
            l1 = "<Li style=\"list-style-type: none; list-style-image: none; margin: 0; padding: 0;\">" + sArmorInternals + "</Li>";
        } else if (!isGunEmplacement) {
            String msg_unknown = Messages.getString("BoardView1.Tooltip.Unknown");
            String armorType = TROView.formatArmorType(entity, true).replace(msg_unknown, "");
            if (!armorType.isBlank()) {
                String msg_armorcapital = Messages.getString("BoardView1.Tooltip.ArmorCapital");
                armorType = (entity.isCapitalScale() ? msg_armorcapital + " " : "") + armorType;
                armorType = " (" + armorType + ") ";
            }
            String armorStr = entity.getTotalArmor() + " / " + entity.getTotalOArmor() + armorType;
            String internalStr = entity.getTotalInternal() + " / " + entity.getTotalOInternal();
            String sArmorInternals = addToTT("ArmorInternals" , NOBR, armorStr, internalStr).toString();
            l1 = "<Li style=\"list-style-type: none; list-style-image: none; margin: 0; padding: 0;\">" + sArmorInternals + "</Li>";
        }

        String ul = "<UL style=\"list-style-type: none; list-style-image: none; margin: 0; padding: 0;\">" + l1 + "</UL>";
        result = guiScaledFontHTML() + ul + "</FONT>";

        return new StringBuilder().append(result);
    }

    /** Returns warnings about problems that should be solved before deploying. */
    private static StringBuilder deploymentWarnings(Entity entity, MapSettings mapSettings, boolean details) {
        String result = "";
        String sWarnings = "";

        if (!details || (mapSettings == null)) {
            return new StringBuilder();
        }

        // Critical (red) warnings
        if (entity.getGame().getPlanetaryConditions().whyDoomed(entity, entity.getGame()) != null) {
            String msg_cannotsurvive = Messages.getString("BoardView1.Tooltip.CannotSurvive");
            sWarnings += "<BR>" + msg_cannotsurvive + " " + entity.getGame().getPlanetaryConditions().whyDoomed(entity, entity.getGame());
        }
        if (entity.doomedInAtmosphere() && mapSettings.getMedium() == MapSettings.MEDIUM_ATMOSPHERE) {
            String msg_cannotsurviveatmo = Messages.getString("BoardView1.Tooltip.CannotSurviveAtmo");
            sWarnings += "<BR>" + msg_cannotsurviveatmo;
        }
        if (entity.doomedOnGround() && mapSettings.getMedium() == MapSettings.MEDIUM_GROUND) {
            String msg_cannotsurviveground = Messages.getString("BoardView1.Tooltip.CannotSurviveGround");
            sWarnings += "<BR>" + msg_cannotsurviveground;
        }
        if (entity.doomedInSpace() && mapSettings.getMedium() == MapSettings.MEDIUM_SPACE) {
            String msg_cannotsurvivespace = Messages.getString("BoardView1.Tooltip.CannotSurviveSpace");
            sWarnings += "<BR>" + msg_cannotsurvivespace;
        }
        result += guiScaledFontHTML(GUIP.getWarningColor()) + sWarnings + "</FONT>";

        String sNoncritial = "";
        // Non-critical (yellow) warnings
        if (((entity.hasC3i() || entity.hasNavalC3()) && (entity.calculateFreeC3Nodes() == 5))
                || ((entity.getC3Master() == null) && entity.hasC3S())
                || (entity.hasNovaCEWS() && (entity.calculateFreeC3Nodes() == 2))) {
            String msg_unconnectedc3computer = Messages.getString("BoardView1.Tooltip.UnconnectedC3Computer");
            sNoncritial += "<BR>" + msg_unconnectedc3computer;
        }

        // Non-critical (yellow) warnings
        if (entity instanceof FighterSquadron && entity.getLoadedUnits().isEmpty()) {
            String msg_fightersquadronempty = Messages.getString("BoardView1.Tooltip.FighterSquadronEmpty");
            sNoncritial += "<BR>" + msg_fightersquadronempty;
        }

        result += guiScaledFontHTML(GUIP.getCautionColor()) + sNoncritial + "</FONT>" + "<BR>";

        return new StringBuilder().append(result);
    }

    /** Returns a list of units loaded onto this unit. */
    private static StringBuilder carriedUnits(Entity entity) {
        String result = "";
        String sCarriedUnits = "";

        if (!entity.getLoadedUnits().isEmpty()) {
            if (entity instanceof FighterSquadron) {
                String msg_fighter = Messages.getString("BoardView1.Tooltip.Fighters");
                sCarriedUnits += msg_fighter + ":";
            } else {
                String msg_carriedunits = Messages.getString("BoardView1.Tooltip.CarriedUnits");
                sCarriedUnits += msg_carriedunits + ":";
            }

            for (Entity carried: entity.getLoadedUnits()) {
                sCarriedUnits += "<BR>&nbsp;&nbsp;" + carried.getShortNameRaw();
                if (PreferenceManager.getClientPreferences().getShowUnitId()) {
                    sCarriedUnits += " [" + carried.getId() + "]";
                }
            }

            result = guiScaledFontHTML() + sCarriedUnits + "</FONT>";
        }

        return new StringBuilder().append(result);
    }

    /** Returns the full force chain the entity is in as one text line. */
    private static StringBuilder forceEntry(Entity entity, Player localPlayer) {
        String result = "";
        String sForceEntry = "";

        if (entity.partOfForce()) {
            // Get the my / ally / enemy color and desaturate it
            Color color = GUIP.getEnemyUnitColor();
            if (localPlayer != null && entity.getOwnerId() == localPlayer.getId()) {
                color = GUIP.getMyUnitColor();
            } else if (localPlayer != null && !localPlayer.isEnemyOf(entity.getOwner())) {
                color = GUIP.getAllyUnitColor();
            }
            color = addGray(color, 128).brighter();
            sForceEntry = "<BR>";
            var forceChain = entity.getGame().getForces().forceChain(entity);
            for (int i = forceChain.size() - 1; i >= 0; i--) {
                sForceEntry += forceChain.get(i).getName();
                sForceEntry += i != 0 ? ", " : "";
            }
            result = guiScaledFontHTML(color) + sForceEntry + "</FONT>";
        }

        return new StringBuilder().append(result);
    }

    /** Returns an overview of the C3 system the unit is in. */
    private static StringBuilder c3Info(Entity entity, boolean details) {
        String result = "";
        String sC3Info = "";

        if (details && entity.hasAnyC3System()) {
            List<String> members = entity.getGame().getEntitiesVector().stream()
                    .filter(e -> e.onSameC3NetworkAs(entity))
                    .sorted(Comparator.comparingInt(Entity::getId))
                    .map(e -> c3UnitName(e, entity)).collect(Collectors.toList());
            if (members.size() > 1) {
                if (entity.hasNhC3()) {
                    String msg_c3i = Messages.getString("BoardView1.Tooltip.C3i");
                    String msg_nc3 = Messages.getString("BoardView1.Tooltip.NC3");
                    sC3Info = entity.hasC3i() ? msg_c3i : msg_nc3;
                } else {
                    String msg_c3 = Messages.getString("BoardView1.Tooltip.C3");
                    sC3Info = msg_c3;
                }
                String msg_network = Messages.getString("BoardView1.Tooltip.Network");
                sC3Info += " " + msg_network + ": <BR>&nbsp;&nbsp;";
                sC3Info += String.join("<BR>&nbsp;&nbsp;", members);
                sC3Info += "<BR>";
            }

            result = guiScaledFontHTML(GUIP.getUnitToolTipHighlightColor(), -0.2f) + sC3Info + "</FONT>";

        }

        return new StringBuilder().append(result);
    }

    private static String c3UnitName(Entity c3member, Entity entity) {
        String result = "";
        String msg_c3 = "";
        String sC3UnitName = "";
        String tmp = "";

        sC3UnitName = " [" + c3member.getId() + "] ";

        if (c3member.isC3CompanyCommander()) {
            msg_c3 = Messages.getString("BoardView1.Tooltip.C3CC")  + " ";
        } else if (c3member.hasC3M()) {
            msg_c3 = Messages.getString("BoardView1.Tooltip.C3M") + " ";
        }

        sC3UnitName += "<I>" + msg_c3 + "</I>";
        result += guiScaledFontHTML(GUIP.getUnitToolTipFGColor(), -0.2f) + sC3UnitName + "</FONT>";
        result += c3member.getShortNameRaw();

        String msg_thisunit = " (" + Messages.getString("BoardView1.Tooltip.ThisUnit") + ")";
        tmp = "<I>" + msg_thisunit + "</I>";
        String sC3Member = c3member.equals(entity) ? tmp : "";
        result += guiScaledFontHTML(GUIP.getUnitToolTipFGColor(), -0.2f) + sC3Member + "</FONT>";

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

    private UnitToolTip() { }
}

/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.tooltip;

import static megamek.client.ui.clientGUI.tooltip.TipUtil.BR;
import static megamek.client.ui.clientGUI.tooltip.TipUtil.NOBR;
import static megamek.client.ui.clientGUI.tooltip.TipUtil.getOptionList;
import static megamek.client.ui.util.UIUtil.DOT_SPACER;
import static megamek.client.ui.util.UIUtil.ECM_SIGN;
import static megamek.client.ui.util.UIUtil.VRT_SIGN;
import static megamek.client.ui.util.UIUtil.repeat;
import static megamek.common.units.LandAirMek.CONV_MODE_FIGHTER;

import java.awt.Color;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.tooltip.info.WeaponInfo;
import megamek.client.ui.util.UIUtil;
import megamek.common.CriticalSlot;
import megamek.common.Hex;
import megamek.common.MPCalculationSetting;
import megamek.common.Player;
import megamek.common.RangeType;
import megamek.common.ReportMessages;
import megamek.common.Team;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.compute.Compute;
import megamek.common.enums.VariableRangeTargetingMode;
import megamek.common.equipment.*;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.game.Game;
import megamek.common.game.InGameObject;
import megamek.common.loaders.MapSettings;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.preference.PreferenceManager;
import megamek.common.templates.TROView;
import megamek.common.units.*;
import megamek.logging.MMLogger;

public final class UnitToolTip {
    private static final MMLogger logger = MMLogger.create(UnitToolTip.class);

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    public static StringBuilder lobbyTip(InGameObject unit, Player localPlayer, MapSettings mapSettings) {
        if (unit instanceof Entity) {
            return getEntityTipTable((Entity) unit,
                  localPlayer,
                  true,
                  false,
                  false,
                  mapSettings,
                  true,
                  false,
                  false,
                  false,
                  false,
                  false);
        } else if (unit instanceof AlphaStrikeElement) {
            // TODO : Provide a suitable tip
            return new StringBuilder("AlphaStrikeElement " + ((AlphaStrikeElement) unit).getName());
        } else {
            return new StringBuilder("This type of object has currently no table entry.");
        }
    }

    /** Returns the unit tooltip with values that are relevant in the lobby. */
    public static StringBuilder getEntityTipLobby(Entity entity, Player localPlayer, MapSettings mapSettings) {
        return getEntityTipTable(entity,
              localPlayer,
              true,
              true,
              false,
              mapSettings,
              true,
              false,
              false,
              false,
              false,
              false);
    }

    /** Returns the unit tooltip with values that are relevant in-game. */
    public static StringBuilder getEntityTipGame(Entity entity, Player localPlayer) {
        return getEntityTipTable(entity, localPlayer, false, true, true, null, true, true, true, true, true, false);
    }

    /**
     * Returns the unit tooltip with values that are relevant in-game without the Pilot info.
     */
    public static StringBuilder getEntityTipUnitDisplay(Entity entity, Player localPlayer) {
        return getEntityTipTable(entity, localPlayer, true, false, false, null, true, true, true, true, true, false);
    }

    /** Returns the unit tooltip with minimal but useful information */
    public static StringBuilder getEntityTipAsTarget(Entity entity, Player localPlayer) {
        return getEntityTipTable(entity, localPlayer, false, true, false, null, true, true, false, false, false, false);
    }

    /** Returns the unit tooltip with minimal but useful information */
    public static StringBuilder getEntityTipReport(Entity entity) {
        return getEntityTipTable(entity, null, true, false, true, null, false, true, false, false, false, true);
    }

    public static String wrapWithHTML(String text) {
        String fgColor = GUIPreferences.hexColor(GUIP.getUnitToolTipFGColor());
        String bgColor = GUIPreferences.hexColor(GUIP.getUnitToolTipBGColor());
        String attr = String.format("style=\"color:%s; background-color:%s;\"", fgColor, bgColor);
        String body = UIUtil.tag("BODY", attr, text);
        return UIUtil.tag("HTML", "", body);
    }

    // PRIVATE

    /** Assembles the whole unit tooltip. */
    private static StringBuilder getEntityTipTable(Entity entity, Player localPlayer, boolean details,
          boolean pilotInfoShow, boolean pilotInfoStandard, @Nullable MapSettings mapSettings, boolean showName,
          boolean inGameValue, boolean showBV, boolean showSensors, boolean showSeenBy, boolean report) {
        // Tooltip info for a sensor blip
        if ((!report) && (EntityVisibilityUtils.onlyDetectedBySensors(localPlayer, entity))) {
            String msgSenorReturn = Messages.getString("BoardView1.sensorReturn");
            return new StringBuilder(msgSenorReturn);
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
            String col = UIUtil.tag("TD", "", result);
            String row = UIUtil.tag("TR", "", col);
            String table = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0 width=100%", row);
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

        // Partial repairs
        result += getPartialRepairs(entity, details);

        // Carried Units
        result += carriedUnits(entity);

        // carried cargo
        result += carriedCargo(entity);

        // C3 Info
        result += c3Info(entity, details);

        // StratOps quirks, chassis and weapon
        result += getQuirks(entity, game, details);

        String col = UIUtil.tag("TD", "", result);
        String row = UIUtil.tag("TR", "", col);
        String table = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0 width=100%", row);
        table = UnitToolTip.addPlayerColorBoarder(entity, table);
        return new StringBuilder().append(table);
    }

    public static String getTargetTipDetail(Targetable target, @Nullable Client client) {
        if (target instanceof Entity) {
            return UnitToolTip.getEntityTipAsTarget((Entity) target, (client != null) ? client.getLocalPlayer() : null)
                  .toString();
        } else if (target instanceof BuildingTarget buildingTarget) {
            Board board = (client != null) ? client.getBoard(target.getBoardId()) : null;
            return HexTooltip.getBuildingTargetTip(buildingTarget, board);
        } else if (target instanceof Hex hex) {
            // LEGACY replace with real board ID
            return HexTooltip.getHexTip(hex, client, 0);
        } else {
            return getTargetTipSummary(target, client);
        }
    }

    public static String getTargetTipSummary(Targetable target, @Nullable Client client) {
        if (target == null) {
            return Messages.getString("BoardView1.Tooltip.NoTarget");
        } else if (target instanceof Entity targetEntity) {
            String result = getTargetTipSummaryEntity(targetEntity, client);
            result = UnitToolTip.addPlayerColorBoarder(targetEntity, result);
            return result;
        } else if (target instanceof BuildingTarget) {
            if (client != null) {
                return HexTooltip.getOneLineSummary((BuildingTarget) target, client.getGame().getBoard(target));
            }
        }

        return target.getDisplayName();
    }

    public static String getTargetTipSummaryEntity(Entity entity, @Nullable Client client) {
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

    private static String addPlayerColorBoarder(Entity entity, String entityTip) {
        Color color = UnitToolTip.GUIP.getUnitToolTipFGColor();
        // the player's color
        // Table to add a bar to the left of an entity in
        if (!EntityVisibilityUtils.onlyDetectedBySensors(entity.getOwner(), entity)) {
            color = entity.getOwner().getColour().getColour();
        }

        String attr = String.format("FACE=Dialog  COLOR=%s", UIUtil.toColorHexString(color));
        entityTip = UIUtil.tag("FONT", attr, entityTip);
        attr = String.format("BGCOLOR=%s WIDTH=6", UIUtil.toColorHexString(color));
        String col1 = UIUtil.tag("TD", attr, "");
        String col2 = UIUtil.tag("TD", "", entityTip);
        String row = UIUtil.tag("TR", "", col1 + col2);
        attr = String.format("CELLSPACING=0 CELLPADDING=4 BORDER=0 BGCOLOR=%s WIDTH=100%%",
              GUIPreferences.hexColor(UnitToolTip.GUIP.getUnitToolTipBGColor()));
        return UIUtil.tag("TABLE", attr, row);
    }

    private static String getChassisInfo(Entity entity) {
        String msgClanBrackets = Messages.getString("BoardView1.Tooltip.ClanBrackets");
        String clanStr = entity.isClan() && !entity.isMixedTech() ? " " + msgClanBrackets + " " : "";
        String chassis = entity.getFullChassis() + clanStr + " (" + (int) entity.getWeight() + "t)";
        chassis += "&nbsp;&nbsp;" + Entity.getEntityTypeName(entity.getEntityType());
        return chassis;
    }

    private static String getOwnerInfo(Entity entity, Player owner) {
        String ownerName = (owner != null) ?
              owner.getName() :
              ReportMessages.getString("BoardView1.Tooltip.unknownOwner");
        String msg_id = MessageFormat.format(" [ID: {0}]", entity.getId());
        String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getUnitToolTipFGColor()));
        ownerName += UIUtil.tag("FONT", attr, msg_id);

        return ownerName;
    }

    private static String getDisplayNames(Entity entity, @Nullable Game game, boolean showName) {
        String result = "";

        if (showName) {
            String col;
            String row;
            String rows;
            String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
            Player owner = (game != null) ? game.getPlayer(entity.getOwnerId()) : null;

            col = getChassisInfo(entity);
            Color ownerColor = (owner != null) ? owner.getColour().getColour() : GUIP.getUnitToolTipFGColor();
            String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(ownerColor));
            col = UIUtil.tag("FONT", attr, col);
            col = UIUtil.tag("span", fontSizeAttr, col);
            col = UIUtil.tag("TD", "", col);
            row = UIUtil.tag("TR", "", col);
            rows = row;

            col = getOwnerInfo(entity, owner);
            attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(ownerColor));
            col = UIUtil.tag("FONT", attr, col);
            col = UIUtil.tag("span", fontSizeAttr, col);
            col = UIUtil.tag("TD", "", col);
            row = UIUtil.tag("TR", "", col);
            rows += row;

            result = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0", rows);
        }

        return result;
    }

    private static String getPilotInfo(Entity entity, boolean pilotInfoShow, boolean pilotInfoStandard,
          boolean report) {
        if (!pilotInfoShow) {
            return "";
        }

        if (pilotInfoStandard) {
            return PilotToolTip.getPilotTipShort(entity, GUIP.getShowPilotPortraitTT(), report).toString();
        } else {
            return PilotToolTip.getPilotTipLine(entity).toString();
        }
    }

    private static String getQuirks(Entity entity, Game game, boolean details) {
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            StringBuilder sQuirks = new StringBuilder();
            String quirksList = getOptionList(entity.getQuirks().getGroups(), entity::countQuirks, details);
            if (!quirksList.isEmpty()) {
                sQuirks.append(quirksList);
            }
            for (Mounted<?> weapon : entity.getWeaponList()) {
                String wpQuirksList = getOptionList(weapon.getQuirks().getGroups(),
                      grp -> weapon.countQuirks(),
                      (e) -> weapon.getDesc(),
                      details);
                if (!wpQuirksList.isEmpty()) {
                    // Line break after weapon name not useful here
                    sQuirks.append(wpQuirksList.replace(":</I><BR>", ":</I>"));
                }
            }

            if (!sQuirks.isEmpty()) {
                String attr = String.format("FACE=Dialog COLOR=%s",
                      UIUtil.toColorHexString(GUIP.getUnitToolTipQuirkColor()));
                sQuirks = new StringBuilder(UIUtil.tag("FONT", attr, sQuirks.toString()));
                String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
                sQuirks = new StringBuilder(UIUtil.tag("span", fontSizeAttr, sQuirks.toString()));

                String col = UIUtil.tag("TD", "", sQuirks.toString());
                String row = UIUtil.tag("TR", "", col);
                String tbody = UIUtil.tag("TBODY", "", row);
                sQuirks = new StringBuilder(UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0", tbody));
            }

            return sQuirks.toString();
        }

        return "";
    }

    private static String getPartialRepairs(Entity entity, boolean details) {
        String result = "";
        String partialList = getOptionList(entity.getPartialRepairs().getGroups(),
              grp -> entity.countPartialRepairs(),
              details);

        if (!partialList.isEmpty()) {
            String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getPrecautionColor()));
            partialList = UIUtil.tag("FONT", attr, partialList);
            String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
            partialList = UIUtil.tag("span", fontSizeAttr, partialList);

            String col = UIUtil.tag("TD", "", partialList);
            String row = UIUtil.tag("TR", "", col);
            String tbody = UIUtil.tag("TBODY", "", row);
            result = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0", tbody);
        }

        return result;
    }

    private static boolean hideArmorLocation(Entity entity, int location) {
        return ((entity.getOArmor(location) <= 0) &&
              (entity.getOInternal(location) <= 0) &&
              !entity.hasRearArmor(location)) ||
              (entity.isConventionalInfantry() && (location != Infantry.LOC_INFANTRY));
    }

    private static String locationHeader(Entity entity, int location) {
        String msgActiveTroopers = Messages.getString("BoardView1.Tooltip.ActiveTroopers");
        return entity.isConventionalInfantry() ?
              ((Infantry) entity).getShootingStrength() + " " + msgActiveTroopers :
              entity.getLocationAbbr(location);
    }

    private static StringBuilder sysCrits(Entity entity, int type, int index, int loc, String locAbbr) {
        String result;
        int total = entity.getNumberOfCriticalSlots(type, index, loc);
        int hits = entity.getHitCriticalSlots(type, index, loc);
        int good = total - hits;
        boolean bad = (entity.getBadCriticalSlots(type, index, loc) > 0);

        if ((good + hits) > 0) {
            result = "&nbsp;&nbsp;" + locAbbr + ":&nbsp;";
            result += systemBar(good, hits, bad);
        } else {
            result = "&nbsp;";
        }

        return new StringBuilder().append(result);
    }

    private static StringBuilder sysStabilizers(Tank tank, int loc, String locAbbr) {
        String result;
        int total = 1;
        int hits = tank.isStabiliserHit(loc) ? 1 : 0;
        int good = total - hits;
        boolean bad = hits > 0;

        result = "&nbsp;&nbsp;" + locAbbr + ":&nbsp;";
        result += systemBar(good, hits, bad);

        return new StringBuilder().append(result);
    }

    private static StringBuilder sysTurretLocked(Tank tank, int loc, String locAbbr) {
        String result;
        int total = 1;
        int hits = tank.isTurretLocked(loc) ? 1 : 0;
        int good = total - hits;
        boolean bad = hits > 0;

        result = "&nbsp;&nbsp;" + locAbbr + ":&nbsp;";
        result += systemBar(good, hits, bad);

        return new StringBuilder().append(result);
    }

    private static StringBuilder sysEngineHit(Tank tank, String locAbbr) {
        String result;
        int total = 1;
        int hits = tank.isEngineHit() ? 1 : 0;
        int good = total - hits;
        boolean bad = hits > 0;

        result = "&nbsp;&nbsp;" + locAbbr + ":&nbsp;";
        result += systemBar(good, hits, bad);

        return new StringBuilder().append(result);
    }

    private static StringBuilder sysSensorHit(Tank tank, String locAbbr) {
        String result;
        int total = Tank.CRIT_SENSOR_MAX;
        int hits = tank.getSensorHits();
        int good = total - hits;
        boolean bad = hits > 0;

        result = "&nbsp;&nbsp;" + locAbbr + ":&nbsp;";
        result += systemBar(good, hits, bad);

        return new StringBuilder().append(result);
    }

    private static StringBuilder sysMinorMovementDamage(Tank tank, String locAbbr) {
        String result;
        int total = 1;
        int hits = tank.hasMinorMovementDamage() ? 1 : 0;
        int good = total - hits;
        boolean bad = hits > 0;

        result = "&nbsp;&nbsp;" + locAbbr + ":&nbsp;";
        result += systemBar(good, hits, bad);

        return new StringBuilder().append(result);
    }

    private static StringBuilder sysModerateMovementDamage(Tank tank, String locAbbr) {
        String result;
        int total = 1;
        int hits = tank.hasModerateMovementDamage() ? 1 : 0;
        int good = total - hits;
        boolean bad = hits > 0;

        result = "&nbsp;&nbsp;" + locAbbr + ":&nbsp;";
        result += systemBar(good, hits, bad);

        return new StringBuilder().append(result);
    }

    private static StringBuilder sysHeavyMovementDamage(Tank tank, String locAbbr) {
        String result;
        int total = 1;
        int hits = tank.hasHeavyMovementDamage() ? 1 : 0;
        int good = total - hits;
        boolean bad = hits > 0;

        result = "&nbsp;&nbsp;" + locAbbr + ":&nbsp;";
        result += systemBar(good, hits, bad);

        return new StringBuilder().append(result);
    }

    private static StringBuilder buildSysCrits(Entity entity, int loc) {
        String result = "";
        String msgAbbrSensors = Messages.getString("BoardView1.Tooltip.AbbreviationSensors");
        String msgAbbrLifeSupport = Messages.getString("BoardView1.Tooltip.AbbreviationLifeSupport");
        String msgAbbrEngine = Messages.getString("BoardView1.Tooltip.AbbreviationEngine");
        String msgAbbrGyro = Messages.getString("BoardView1.Tooltip.AbbreviationGyro");
        String msgAbbrShoulder = Messages.getString("BoardView1.Tooltip.AbbreviationShoulder");
        String msgAbbrUpperArm = Messages.getString("BoardView1.Tooltip.AbbreviationUpperArm");
        String msgAbbrLowerArm = Messages.getString("BoardView1.Tooltip.AbbreviationLowerArm");
        String msgAbbrHand = Messages.getString("BoardView1.Tooltip.AbbreviationHand");
        String msgAbbrHip = Messages.getString("BoardView1.Tooltip.AbbreviationHip");
        String msgAbbrUpperLeg = Messages.getString("BoardView1.Tooltip.AbbreviationUpperLeg");
        String msgAbbrLowerLeg = Messages.getString("BoardView1.Tooltip.AbbreviationLowerLeg");
        String msgAbbrFoot = Messages.getString("BoardView1.Tooltip.AbbreviationLowerFoot");
        String msgAbbrStabilizers = Messages.getString("BoardView1.Tooltip.AbbreviationStabilizers");
        String msgAbbrTurretLocked = Messages.getString("BoardView1.Tooltip.AbbreviationTurretLocked");

        if (entity instanceof Mek) {
            switch (loc) {
                case Mek.LOC_HEAD:
                    result = sysCrits(entity,
                          CriticalSlot.TYPE_SYSTEM,
                          Mek.SYSTEM_SENSORS,
                          loc,
                          msgAbbrSensors).toString();
                    result += sysCrits(entity,
                          CriticalSlot.TYPE_SYSTEM,
                          Mek.SYSTEM_LIFE_SUPPORT,
                          loc,
                          msgAbbrLifeSupport).toString();
                    break;
                case Mek.LOC_CENTER_TORSO:
                    result = sysCrits(entity,
                          CriticalSlot.TYPE_SYSTEM,
                          Mek.SYSTEM_ENGINE,
                          loc,
                          msgAbbrEngine).toString();
                    result += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, loc, msgAbbrGyro).toString();
                    result += sysCrits(entity,
                          CriticalSlot.TYPE_SYSTEM,
                          Mek.SYSTEM_SENSORS,
                          loc,
                          msgAbbrSensors).toString();
                    result += sysCrits(entity,
                          CriticalSlot.TYPE_SYSTEM,
                          Mek.SYSTEM_LIFE_SUPPORT,
                          loc,
                          msgAbbrLifeSupport).toString();
                    break;
                case Mek.LOC_RIGHT_TORSO:
                case Mek.LOC_LEFT_TORSO:
                    result = sysCrits(entity,
                          CriticalSlot.TYPE_SYSTEM,
                          Mek.SYSTEM_ENGINE,
                          loc,
                          msgAbbrEngine).toString();
                    result += sysCrits(entity,
                          CriticalSlot.TYPE_SYSTEM,
                          Mek.SYSTEM_LIFE_SUPPORT,
                          loc,
                          msgAbbrLifeSupport).toString();
                    break;
                case Mek.LOC_RIGHT_ARM:
                case Mek.LOC_LEFT_ARM:
                    result = sysCrits(entity,
                          CriticalSlot.TYPE_SYSTEM,
                          Mek.ACTUATOR_SHOULDER,
                          loc,
                          msgAbbrShoulder).toString();
                    result += sysCrits(entity,
                          CriticalSlot.TYPE_SYSTEM,
                          Mek.ACTUATOR_UPPER_ARM,
                          loc,
                          msgAbbrUpperArm).toString();
                    result += sysCrits(entity,
                          CriticalSlot.TYPE_SYSTEM,
                          Mek.ACTUATOR_LOWER_ARM,
                          loc,
                          msgAbbrLowerArm).toString();
                    result += sysCrits(entity,
                          CriticalSlot.TYPE_SYSTEM,
                          Mek.ACTUATOR_HAND,
                          loc,
                          msgAbbrHand).toString();
                    result += sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP, loc, msgAbbrHip).toString();
                    result += sysCrits(entity,
                          CriticalSlot.TYPE_SYSTEM,
                          Mek.ACTUATOR_UPPER_LEG,
                          loc,
                          msgAbbrUpperLeg).toString();
                    result += sysCrits(entity,
                          CriticalSlot.TYPE_SYSTEM,
                          Mek.ACTUATOR_LOWER_LEG,
                          loc,
                          msgAbbrLowerLeg).toString();
                    result += sysCrits(entity,
                          CriticalSlot.TYPE_SYSTEM,
                          Mek.ACTUATOR_FOOT,
                          loc,
                          msgAbbrFoot).toString();
                    break;
                case Mek.LOC_RIGHT_LEG:
                case Mek.LOC_LEFT_LEG:
                case Mek.LOC_CENTER_LEG:
                    result = sysCrits(entity, CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP, loc, msgAbbrHip).toString();
                    result += sysCrits(entity,
                          CriticalSlot.TYPE_SYSTEM,
                          Mek.ACTUATOR_UPPER_LEG,
                          loc,
                          msgAbbrUpperLeg).toString();
                    result += sysCrits(entity,
                          CriticalSlot.TYPE_SYSTEM,
                          Mek.ACTUATOR_LOWER_LEG,
                          loc,
                          msgAbbrLowerLeg).toString();
                    result += sysCrits(entity,
                          CriticalSlot.TYPE_SYSTEM,
                          Mek.ACTUATOR_FOOT,
                          loc,
                          msgAbbrFoot).toString();
                    break;
                default:
                    result = "&nbsp;";
            }
        } else if (entity instanceof SuperHeavyTank || entity instanceof LargeSupportTank) {
            Tank tank = (Tank) entity;

            switch (loc) {
                case SuperHeavyTank.LOC_BODY:
                case SuperHeavyTank.LOC_FRONT:
                case SuperHeavyTank.LOC_RIGHT:
                case SuperHeavyTank.LOC_LEFT:
                case SuperHeavyTank.LOC_REAR_RIGHT:
                case SuperHeavyTank.LOC_REAR_LEFT:
                case SuperHeavyTank.LOC_REAR:
                    result = sysStabilizers(tank, loc, msgAbbrStabilizers).toString();
                    break;
                case SuperHeavyTank.LOC_TURRET:
                case SuperHeavyTank.LOC_TURRET_2:
                    result = sysStabilizers(tank, loc, msgAbbrStabilizers).toString();
                    result += tank.getTurretCount() > 0 ?
                          sysTurretLocked(tank, loc, msgAbbrTurretLocked).toString() :
                          "&nbsp;";
                    break;
                default:
                    result = "&nbsp;";
            }
        } else if (entity instanceof Tank tank) {

            switch (loc) {
                case Tank.LOC_BODY:
                case Tank.LOC_FRONT:
                case Tank.LOC_RIGHT:
                case Tank.LOC_LEFT:
                case Tank.LOC_REAR:
                    result = sysStabilizers(tank, loc, msgAbbrStabilizers).toString();
                    break;
                case Tank.LOC_TURRET:
                case Tank.LOC_TURRET_2:
                    result = sysStabilizers(tank, loc, msgAbbrStabilizers).toString();
                    result += tank.getTurretCount() > 0 ?
                          sysTurretLocked(tank, loc, msgAbbrTurretLocked).toString() :
                          "&nbsp;";
                    break;
                default:
                    result = "&nbsp;";
            }
        }

        return new StringBuilder().append(result);
    }

    /** Returns the graphical Armor representation. */
    private static StringBuilder addArmorMiniVisToTT(Entity entity) {
        if (!GUIP.getShowArmorMiniVisTT()) {
            return new StringBuilder();
        }

        String armorChar = GUIP.getUnitToolTipArmorMiniArmorChar();
        if (entity.isCapitalScale()) {
            armorChar = GUIP.getUnitToolTipArmorMiniCapArmorChar();
        }
        String internalChar = GUIP.getUnitToolTipArmorMiniISChar();
        String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
        String col1;
        String col2;
        String col3;
        String row;
        StringBuilder rows = new StringBuilder();

        String msg_abbr_sensors = Messages.getString("BoardView1.Tooltip.AbbreviationSensors");
        String msg_abbr_engine = Messages.getString("BoardView1.Tooltip.AbbreviationEngine");
        String msgAbbrMinorMovementDamage = Messages.getString("BoardView1.Tooltip.AbbreviationMinorMovementDamage");
        String msgAbbrModerateMovementDamage = Messages.getString(
              "BoardView1.Tooltip.AbbreviationModerateMovementDamage");
        String msgAbbrHeavyMovementDamage = Messages.getString("BoardView1.Tooltip.AbbreviationHeavyMovementDamage");

        for (int loc = 0; loc < entity.locations(); loc++) {
            // do not show locations that do not support/have armor/internals like HULL on
            // Aero
            if (hideArmorLocation(entity, loc)) {
                continue;
            }

            boolean locDestroyed = (entity.getInternal(loc) == IArmorState.ARMOR_DOOMED ||
                  entity.getInternal(loc) == IArmorState.ARMOR_DESTROYED);

            if (locDestroyed) {
                // Destroyed location
                col1 = "&nbsp;";
                col2 = "&nbsp;&nbsp;" + locationHeader(entity, loc) + ":&nbsp;";
                col2 += destroyedLocBar(entity.getOArmor(loc, true)).toString();
            } else {
                // Rear armor
                if (entity.hasRearArmor(loc)) {
                    String msg_abbr_rear = Messages.getString("BoardView1.Tooltip.AbbreviationRear");
                    col1 = "&nbsp;&nbsp;" + locationHeader(entity, loc) + msg_abbr_rear + "&nbsp;";
                    col1 += intactLocBar(entity.getOArmor(loc, true), entity.getArmor(loc, true), armorChar).toString();
                } else {
                    // No rear armor: empty table cells instead
                    // At small font sizes, writing one character at the correct font size is
                    // necessary to prevent the table rows from being spaced non-beautifully
                    col1 = "&nbsp;";
                }
                // Front armor
                col2 = "&nbsp;&nbsp;" + locationHeader(entity, loc) + ":&nbsp;";
                col2 += intactLocBar(entity.getOInternal(loc), entity.getInternal(loc), internalChar).toString();
                col2 += intactLocBar(entity.getOArmor(loc), entity.getArmor(loc), armorChar).toString();
            }

            col3 = buildSysCrits(entity, loc).toString();

            col1 = UIUtil.tag("span", fontSizeAttr, col1);
            col2 = UIUtil.tag("span", fontSizeAttr, col2);
            col3 = UIUtil.tag("span", fontSizeAttr, col3);

            col1 = UIUtil.tag("TD", "", col1);
            col2 = UIUtil.tag("TD", "", col2);
            col3 = UIUtil.tag("TD", "", col3);
            row = UIUtil.tag("TR", "", col1 + col2 + col3);
            rows.append(row);
        }

        if (entity instanceof GunEmplacement tank) {
            col1 = "&nbsp;";
            col2 = sysSensorHit(tank, msg_abbr_sensors).toString();
            col3 = "&nbsp;";

            col1 = UIUtil.tag("span", fontSizeAttr, col1);
            col2 = UIUtil.tag("span", fontSizeAttr, col2);
            col3 = UIUtil.tag("span", fontSizeAttr, col3);

            col1 = UIUtil.tag("TD", "", col1);
            col2 = UIUtil.tag("TD", "", col2);
            col3 = UIUtil.tag("TD", "", col3);
            row = UIUtil.tag("TR", "", col1 + col2 + col3);
            rows.append(row);
        } else if (entity instanceof VTOL tank) {
            col1 = "&nbsp;";
            col2 = sysEngineHit(tank, msg_abbr_engine).toString();
            col2 += sysSensorHit(tank, msg_abbr_sensors).toString();
            col3 = "&nbsp;";

            col1 = UIUtil.tag("span", fontSizeAttr, col1);
            col2 = UIUtil.tag("span", fontSizeAttr, col2);
            col3 = UIUtil.tag("span", fontSizeAttr, col3);

            col1 = UIUtil.tag("TD", "", col1);
            col2 = UIUtil.tag("TD", "", col2);
            col3 = UIUtil.tag("TD", "", col3);
            row = UIUtil.tag("TR", "", col1 + col2 + col3);
            rows.append(row);
        } else if (entity instanceof Tank tank) {
            col1 = "";
            col2 = sysEngineHit(tank, msg_abbr_engine).toString();
            col2 += sysSensorHit(tank, msg_abbr_sensors).toString();
            col3 = sysMinorMovementDamage(tank, msgAbbrMinorMovementDamage).toString();
            col3 += sysModerateMovementDamage(tank, msgAbbrModerateMovementDamage).toString();
            col3 += sysHeavyMovementDamage(tank, msgAbbrHeavyMovementDamage).toString();

            col1 = UIUtil.tag("span", fontSizeAttr, col1);
            col2 = UIUtil.tag("span", fontSizeAttr, col2);
            col3 = UIUtil.tag("span", fontSizeAttr, col3);

            col1 = UIUtil.tag("TD", "", col1);
            col2 = UIUtil.tag("TD", "", col2);
            col3 = UIUtil.tag("TD", "", col3);
            row = UIUtil.tag("TR", "", col1 + col2 + col3);
            rows.append(row);
        }

        String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getUnitToolTipFGColor()));
        rows = new StringBuilder(UIUtil.tag("FONT", attr, rows.toString()));

        String tbody = UIUtil.tag("TBODY", "", rows.toString());
        String table = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0", tbody);

        return new StringBuilder().append(table);
    }

    /**
     * Used for destroyed locations. Returns a string representing armor or internal structure of the location. The
     * location has the given orig original Armor/IS.
     */
    private static StringBuilder destroyedLocBar(int orig) {
        String destroyedChar = GUIP.getUnitToolTipArmorMiniDestroyedChar();
        return locBar(orig, orig, destroyedChar, true);
    }

    /**
     * Used for intact locations. Returns a string representing armor or internal structure of the location. The
     * location has the given orig original Armor/IS.
     */
    private static StringBuilder intactLocBar(int orig, int curr, String dChar) {
        return locBar(orig, curr, dChar, false);
    }

    /**
     * Returns a string representing armor or internal structure of one location. The location has the given orig
     * original Armor/IS and the given curr current Armor/IS. The character dChar will be repeated at appropriate colors
     * depending on the value of curr, orig and the static visUnit which gives the amount of Armor/IS per single
     * character.
     */
    private static StringBuilder locBar(int orig, int curr, String dChar, boolean destroyed) {
        // Internal Structure can be zero, e.g. in Aero
        if (orig == 0) {
            return new StringBuilder();
        }

        String result = "";
        Color colorIntact = GUIP.getColor(GUIPreferences.UNIT_TOOLTIP_ARMOR_MINI_COLOR_INTACT);
        Color colorPartialDmg = GUIP.getColor(GUIPreferences.UNIT_TOOLTIP_ARMOR_MINI_COLOR_PARTIAL_DMG);
        Color colorDamaged = GUIP.getColor(GUIPreferences.UNIT_TOOLTIP_ARMOR_MINI_COLOR_DAMAGED);
        int visUnit = GUIP.getInt(GUIPreferences.UNIT_TOOLTIP_ARMOR_MINI_UNITS_PER_BLOCK);

        if (destroyed) {
            colorIntact = colorDamaged;
            colorPartialDmg = colorDamaged;
        }

        int numPartial = ((curr != orig) && (curr % visUnit) > 0) ? 1 : 0;
        int numIntact = (curr - 1) / visUnit + 1 - numPartial;
        int numDamaged = (orig - 1) / visUnit + 1 - numPartial - numIntact;

        if (curr <= 0) {
            numPartial = 0;
            numIntact = 0;
            numDamaged = (orig - 1) / visUnit + 1;
        }

        if (numIntact > 0) {
            if (numIntact > 15 && numIntact + numDamaged > 30) {
                int tensIntact = (numIntact - 1) / 10;
                String msg_x = Messages.getString("BoardView1.Tooltip.X");
                String sIntact = dChar + msg_x + tensIntact * 10;
                sIntact += repeat(dChar, numIntact - 10 * tensIntact);
                String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(colorIntact));
                result += UIUtil.tag("FONT", attr, sIntact);
            } else {
                String sIntact = repeat(dChar, numIntact);
                String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(colorIntact));
                result += UIUtil.tag("FONT", attr, sIntact);
            }
        }
        if (numPartial > 0) {
            String sPartial = repeat(dChar, numPartial);
            String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(colorPartialDmg));
            result += UIUtil.tag("FONT", attr, sPartial);
        }
        if (numDamaged > 0) {
            if (numDamaged > 15 && numIntact + numDamaged > 30) {
                int tensDamaged = (numDamaged - 1) / 10;
                String msg_x = Messages.getString("BoardView1.Tooltip.X");
                String sDamage = dChar + msg_x + tensDamaged * 10;
                sDamage += repeat(dChar, numDamaged - 10 * tensDamaged);
                String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(colorDamaged));
                result += UIUtil.tag("FONT", attr, sDamage);
            } else {
                String sDamage = repeat(dChar, numDamaged);
                String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(colorDamaged));
                result += UIUtil.tag("FONT", attr, sDamage);
            }
        }
        return new StringBuilder().append(result);
    }

    private static StringBuilder systemBar(int good, int bad, boolean destroyed) {
        // Internal Structure can be zero, e.g. in Aero
        if ((good + bad) == 0) {
            return new StringBuilder();
        }

        String result = "";
        Color colorIntact = GUIP.getUnitTooltipArmorMiniColorIntact();
        Color colorDamaged = GUIP.getUnitTooltipArmorMiniColorDamaged();
        String dChar = GUIP.getUnitToolTipArmorMiniDestroyedChar();
        String iChar = GUIP.getUnitToolTipArmorMiniCriticalChar();

        if (good > 0) {
            String sGood = repeat(iChar, good);
            String attr;
            if (!destroyed) {
                attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(colorIntact));
            } else {
                attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(colorDamaged));
            }
            result += UIUtil.tag("FONT", attr, sGood);
        }
        if (bad > 0) {
            String sBad = repeat(dChar, bad);
            String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(colorDamaged));
            result += UIUtil.tag("FONT", attr, sBad);
        }
        return new StringBuilder().append(result);
    }

    /**
     * Returns true if the {@link WeaponType} should be excluded from the Tooltip. This is true for C3 computers (only
     * Masters are weapons) and special Infantry attacks (Swarm Attacks and the like).
     */
    private static boolean isNotTTRelevant(WeaponType weaponType) {
        return weaponType.hasFlag(WeaponType.F_C3M) ||
              weaponType.hasFlag(WeaponType.F_C3MBS) ||
              weaponType.hasFlag(WeaponTypeFlag.INTERNAL_REPRESENTATION);
    }

    /**
     * @return True if the given {@link MiscType} should be excluded from the tooltip. True for everything except clubs.
     */
    private static boolean isNotTTRelevant(MiscType miscType) {
        return !miscType.hasFlag(MiscType.F_CLUB);
    }

    private static final String RAPID_FIRE = "|RF|";
    private static final String CLAN_WEAPON = "|CL|";

    /** Returns the assembled weapons with ranges etc. */
    private static StringBuilder weaponList(Entity entity) {
        if (!GUIP.getShowWpsInTT()) {
            return new StringBuilder();
        }

        HashMap<String, WeaponInfo> wpInfos = createWeaponList(entity);

        // Print to Tooltip
        StringBuilder rows = new StringBuilder();

        // Display sorted by weapon name
        var wps = new ArrayList<>(wpInfos.values());
        wps.sort(Comparator.comparing(w -> w.sortString));

        for (WeaponInfo currentEquip : wps) {
            // This WeaponInfo is ammo
            if (!currentEquip.ammunition.isEmpty()) {
                rows.append(createAmmoEntry(currentEquip));
            } else {
                rows.append(createWeaponEntry(entity, currentEquip));
            }
        }

        String tbody = UIUtil.tag("TBODY", "", rows.toString());
        String table = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0", tbody);

        return new StringBuilder().append(table);
    }

    public static StringBuilder getWeaponList(Entity entity) {
        return weaponList(entity);
    }

    /**
     * Gather names, counts, Clan/IS
     */
    public static HashMap<String, WeaponInfo> createWeaponList(Entity entity) {
        HashMap<String, WeaponInfo> wpInfos = new HashMap<>();
        WeaponInfo currentWp;
        List<WeaponMounted> weapons = entity.getWeaponList();

        for (WeaponMounted curWp : weapons) {
            WeaponType weaponType = curWp.getType();

            if (isNotTTRelevant(weaponType)) {
                continue;
            }

            String weaponDesc = curWp.getDesc();

            if (GUIP.getShowWpsLocinTT() && (entity.locations() > 1)) {
                weaponDesc += " [" + entity.getLocationAbbr(curWp.getLocation()) + ']';
            }

            // Distinguish equal weapons with and without rapid fire
            if (isRapidFireActive(entity.getGame()) && curWp.isRapidFire() && !curWp.isDestroyed()) {
                weaponDesc += RAPID_FIRE;
            }

            if (weaponDesc.startsWith("+")) {
                weaponDesc = weaponDesc.substring(1);
            }

            if (curWp.getType().isClan()) {
                weaponDesc += CLAN_WEAPON;
            }

            String msgClanBrackets = Messages.getString("BoardView1.Tooltip.ClanBrackets");
            String msgClanParens = Messages.getString("BoardView1.Tooltip.ClanParens");
            weaponDesc = weaponDesc.replace(msgClanBrackets, "").replace(msgClanParens, "").trim();

            if (wpInfos.containsKey(weaponDesc)) {
                currentWp = wpInfos.get(weaponDesc);
                currentWp.count++;
                wpInfos.put(weaponDesc, currentWp);
                String msg_ammo = Messages.getString("BoardView1.Tooltip.Ammo");

                if (!curWp.isDestroyed() && wpInfos.containsKey(curWp.getName() + msg_ammo)) {
                    WeaponInfo currAmmo = wpInfos.get(curWp.getName() + msg_ammo);
                    currAmmo.ammoActiveWeaponCount++;
                }
            } else {
                currentWp = new WeaponInfo();
                currentWp.name = weaponDesc;
                currentWp.sortString = curWp.getName();

                // Sort active weapons below destroyed to keep them close to their ammo
                if (!curWp.isDestroyed()) {
                    currentWp.sortString += "1";
                }

                currentWp.isRapidFire = weaponDesc.contains(RAPID_FIRE);
                // Create the ranges String
                int[] ranges;

                if (entity.isAero() && !entity.isAeroLandedOnGroundMap()) {
                    ranges = weaponType.getATRanges();
                } else {
                    ranges = weaponType.getRanges(curWp);
                }

                StringBuilder rangeString = new StringBuilder(" \u22EF ");

                if (ranges[RangeType.RANGE_MINIMUM] > 0) {
                    rangeString.append("(").append(ranges[RangeType.RANGE_MINIMUM]).append(") ");
                }

                int maxRange = RangeType.RANGE_LONG;

                if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE)) {
                    maxRange = RangeType.RANGE_EXTREME;
                }

                for (int i = RangeType.RANGE_SHORT; i <= maxRange; i++) {
                    rangeString.append(ranges[i]);

                    if (i != maxRange) {
                        rangeString.append("\u2B1D");
                    }
                }

                WeaponType wpT = curWp.getType();

                if (!wpT.hasFlag(WeaponType.F_AMS) ||
                      entity.getGame()
                            .getOptions()
                            .booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_MANUAL_AMS)) {
                    currentWp.range = rangeString.toString();
                }

                currentWp.isClan = wpT.isClan();
                wpInfos.put(weaponDesc, currentWp);

                // Add ammo info if the weapon has ammo
                // Check wpInfos for dual entries to avoid displaying ammo twice for
                // non/rapid-fire
                if ((weaponType.getAmmoType() != AmmoType.AmmoTypeEnum.NA) &&
                      (!weaponType.hasFlag(WeaponType.F_ONE_SHOT) ||
                            weaponType.hasFlag(WeaponType.F_BA_INDIVIDUAL)) &&
                      (weaponType.getAmmoType() != AmmoType.AmmoTypeEnum.INFANTRY)) {
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

                        for (Mounted<?> amounted : entity.getAmmo()) {
                            boolean canSwitchToAmmo = AmmoType.canSwitchToAmmo(curWp, (AmmoType) amounted.getType());

                            if (canSwitchToAmmo && !amounted.isDumping()) {
                                String msgAntiPersonnel = Messages.getString("BoardView1.Tooltip.AntiPersonnel");
                                String msgAP = Messages.getString("BoardView1.Tooltip.AP");
                                String msgISBracket = Messages.getString("BoardView1.Tooltip.ISBracket");
                                String msgHalfBrackets = Messages.getString("BoardView1.Tooltip.HalfBrackets");
                                String msgHalf = Messages.getString("BoardView1.Tooltip.Half");
                                String msgStandard = Messages.getString("BoardView1.Tooltip.Standard");
                                String msgHotLoadedParens = Messages.getString("BoardView1.Tooltip.HotLoadedParens");

                                String name = amounted.getName()
                                      .replace(msgAntiPersonnel, msgAP)
                                      .replace(msg_ammo, "")
                                      .replace(msgISBracket, "")
                                      .replace(msgClanBrackets, "")
                                      .replace(msgClanParens, "")
                                      .replace(msgHalfBrackets, "")
                                      .replace(msgHalf, "")
                                      .replace(curWp.getDesc(), "")
                                      .trim();

                                if (name.isBlank()) {
                                    name = msgStandard;
                                }

                                if (amounted.isHotLoaded()) {
                                    name += " " + msgHotLoadedParens;
                                }

                                int count = amounted.getUsableShotsLeft();
                                count += currAmmo.ammunition.getOrDefault(name, 0);
                                currAmmo.ammunition.put(name, count);
                            }
                        }

                        wpInfos.put(curWp.getName() + msg_ammo, currAmmo);
                    }
                }
            }
        }

        // Also show hatchets and such equipment in the tooltip as weaponry
        for (MiscMounted misc : entity.getMisc()) {
            MiscType type = misc.getType();

            if (isNotTTRelevant(type)) {
                continue;
            }

            String weaponDesc = misc.getDesc();

            if (GUIP.getShowWpsLocinTT() && (entity.locations() > 1)) {
                weaponDesc += " [" + entity.getLocationAbbr(misc.getLocation()) + ']';
            }

            if (weaponDesc.startsWith("+")) {
                weaponDesc = weaponDesc.substring(1);
            }

            if (misc.getType().isClan()) {
                weaponDesc += CLAN_WEAPON;
            }

            String msgClanBrackets = Messages.getString("BoardView1.Tooltip.ClanBrackets");
            String msgClanParens = Messages.getString("BoardView1.Tooltip.ClanParens");
            weaponDesc = weaponDesc.replace(msgClanBrackets, "").replace(msgClanParens, "").trim();

            if (wpInfos.containsKey(weaponDesc)) {
                currentWp = wpInfos.get(weaponDesc);
                currentWp.count++;
                wpInfos.put(weaponDesc, currentWp);
            } else {
                currentWp = new WeaponInfo();
                currentWp.name = weaponDesc;
                currentWp.sortString = misc.getName();

                // Sort active weapons below destroyed to keep them close to their ammo
                if (!misc.isDestroyed()) {
                    currentWp.sortString += "1";
                }

                currentWp.isClan = type.isClan();
                wpInfos.put(weaponDesc, currentWp);
            }
        }

        return wpInfos;
    }

    private static String weaponModifier(boolean isDestroyed, WeaponInfo currentEquip) {
        if (isDestroyed) {
            // Ends the strikethrough that is added for destroyed weapons
            return "";
        } else if (currentEquip.isHotLoaded) {
            String msgHotLoaded = Messages.getString("BoardView1.Tooltip.HotLoaded");
            String s = UIUtil.tag("I", "", msgHotLoaded);
            return " \u22EF" + s;
        } else if (currentEquip.isRapidFire) {
            String msgRapidFire = Messages.getString("BoardView1.Tooltip.Rapidfire");
            String s = UIUtil.tag("I", "", msgRapidFire);
            return " \u22EF" + s;
        }
        return "";
    }

    /**
     * Returns the weapon line(s) for the weapons type. Check if weapon is destroyed, text gray and strikethrough if so,
     * remove the "x "/"*"
     **/
    private static StringBuilder createWeaponEntry(Entity entity, WeaponInfo currentEquip) {
        String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
        String col1;
        String col2;
        String row;
        boolean isDestroyed = false;
        String nameStr = currentEquip.name;

        if (nameStr == null) {
            nameStr = Messages.getString("BoardView1.Tooltip.NullWeaponName"); // Happens with Vehicle Flamers!
        }

        if (nameStr.startsWith("x ")) {
            nameStr = nameStr.substring(2);
            isDestroyed = true;
        }

        if (nameStr.startsWith("*")) {
            nameStr = nameStr.substring(1);
            isDestroyed = true;
        }

        // Remove the rapid fire marker (used only to distinguish weapons set to
        // different modes)
        nameStr = nameStr.replace(RAPID_FIRE, "");
        nameStr = nameStr.replace(CLAN_WEAPON, "");
        nameStr += currentEquip.range;
        String techBase = "";

        if (entity.isMixedTech()) {
            String msg_clan = Messages.getString("BoardView1.Tooltip.Clan");
            String msg_is = Messages.getString("BoardView1.Tooltip.IS");
            techBase = currentEquip.isClan ? msg_clan : msg_is;
            techBase += " ";
        }

        String destStr = "";

        // group and list with a multiplier "4 x Small Laser"
        if (currentEquip.count > 1) {
            String msg_x = Messages.getString("BoardView1.Tooltip.X");
            col1 = currentEquip.count + " " + msg_x + " ";
        } else {
            col1 = "&nbsp;";
        }

        col2 = addToTT("Weapon", false, currentEquip.count, techBase, nameStr, destStr).toString();
        col2 += weaponModifier(isDestroyed, currentEquip);

        if (isDestroyed) {
            col2 = UIUtil.tag("S", "", col2);
        }

        String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getUnitToolTipWeaponColor()));
        col1 = UIUtil.tag("FONT", attr, col1);
        col1 = UIUtil.tag("span", fontSizeAttr, col1);
        col1 = UIUtil.tag("TD", "", col1);

        attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getUnitToolTipWeaponColor()));
        col2 = UIUtil.tag("FONT", attr, col2);
        col2 = UIUtil.tag("span", fontSizeAttr, col2);
        col2 = UIUtil.tag("TD", "", col2);
        row = UIUtil.tag("TR", "", col1 + col2);

        return new StringBuilder().append(row);
    }

    /** Returns the ammo line(s) for the ammo of one weapon type. */
    private static StringBuilder createAmmoEntry(WeaponInfo ammoInfo) {
        String col1;
        String col2;
        String row;
        StringBuilder rows = new StringBuilder();
        String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
        int totalAmmo = ammoInfo.ammunition.values().stream().mapToInt(n -> n).sum();

        if (totalAmmo == 0 && ammoInfo.ammoActiveWeaponCount > 0) {
            String msgOutOfAmmo = Messages.getString("BoardView1.Tooltip.OutOfAmmo");
            col1 = "&nbsp;";
            col2 = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + msgOutOfAmmo;

            col1 = UIUtil.tag("span", fontSizeAttr, col1);
            col1 = UIUtil.tag("TD", "", col1);

            String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getCautionColor()));
            col2 = UIUtil.tag("FONT", attr, col2);
            col2 = UIUtil.tag("span", fontSizeAttr, col2);
            col2 = UIUtil.tag("TD", "", col2);

            row = UIUtil.tag("TR", "", col1 + col2);
            rows.append(row);
        } else {
            for (Entry<String, Integer> ammo : ammoInfo.ammunition.entrySet()) {
                String msg_standard = Messages.getString("BoardView1.Tooltip.Standard");
                String ammoName = ammo.getKey().equals(msg_standard) && ammoInfo.ammunition.size() == 1 ?
                      "" :
                      ammo.getKey() + ": ";

                // No entry when no ammo of this type left but some other type left
                if (ammo.getValue() == 0) {
                    continue;
                }

                col1 = "&nbsp;";
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

                col1 = UIUtil.tag("span", fontSizeAttr, col1);
                col1 = UIUtil.tag("TD", "", col1);

                String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getCautionColor()));
                col2 = UIUtil.tag("FONT", attr, col2);
                col2 = UIUtil.tag("span", fontSizeAttr, col2);
                col2 = UIUtil.tag("TD", "", col2);

                row = UIUtil.tag("TR", "", col1 + col2);
                rows.append(row);
            }
        }

        return new StringBuilder().append(rows);
    }

    /** Returns the assembled bombs loaded on unit */
    private static StringBuilder bombList(Entity entity) {
        String col1;
        String col2;
        String col3;
        String row;
        StringBuilder rows = new StringBuilder();
        String table;
        String result = "";

        if (entity.isBomber()) {
            BombLoadout loadout;
            String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());

            if (entity.getGame().getPhase().isLounge()) {
                loadout = ((IBomber) entity).getBombChoices();
            } else {
                loadout = entity.getBombLoadout();
            }
            for (Map.Entry<BombTypeEnum, Integer> entry : loadout.entrySet()) {
                BombTypeEnum bombType = entry.getKey();
                int count = entry.getValue();
                if (count > 0) {
                    col1 = String.valueOf(count);
                    col2 = "&nbsp;x&nbsp;";
                    col3 = bombType.getDisplayName();

                    String attr = String.format("FACE=Dialog COLOR=%s",
                          UIUtil.toColorHexString(GUIP.getUnitToolTipWeaponColor()));
                    col1 = UIUtil.tag("FONT", attr, col1);
                    col1 = UIUtil.tag("span", fontSizeAttr, col1);
                    col1 = UIUtil.tag("TD", "", col1);

                    attr = String.format("FACE=Dialog COLOR=%s",
                          UIUtil.toColorHexString(GUIP.getUnitToolTipWeaponColor()));
                    col2 = UIUtil.tag("FONT", attr, col2);
                    col2 = UIUtil.tag("span", fontSizeAttr, col2);
                    col2 = UIUtil.tag("TD", "", col2);

                    attr = String.format("FACE=Dialog COLOR=%s",
                          UIUtil.toColorHexString(GUIP.getUnitToolTipWeaponColor()));
                    col3 = UIUtil.tag("FONT", attr, col3);
                    col3 = UIUtil.tag("span", fontSizeAttr, col3);
                    col3 = UIUtil.tag("TD", "", col3);

                    row = UIUtil.tag("TR", "", col1 + col2 + col3);
                } else {
                    row = "";
                }

                rows.append(row);
            }

            String tbody = UIUtil.tag("TBODY", "", rows.toString());
            table = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0", tbody);
            String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getUnitToolTipFGColor()));
            result = UIUtil.tag("FONT", attr, table);
        }

        return new StringBuilder().append(result);
    }

    /** Returns a line showing ECM / ECCM. */
    private static StringBuilder ecmInfo(Entity entity) {
        String sECMInfo = "";
        String result = "";
        if (entity.hasActiveECM()) {
            String msgECMSource = Messages.getString("BoardView1.ecmSource");
            sECMInfo += ECM_SIGN + " " + msgECMSource;
        }
        if (entity.hasActiveECCM()) {
            String msgECCMSource = Messages.getString("BoardView1.eccmSource");
            sECMInfo += ECM_SIGN + " " + msgECCMSource;
        }

        if (!sECMInfo.isEmpty()) {
            String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
            sECMInfo = UIUtil.tag("span", fontSizeAttr, sECMInfo);

            String col = UIUtil.tag("TD", "", sECMInfo);
            String row = UIUtil.tag("TR", "", col);
            String tbody = UIUtil.tag("TBODY", "", row);
            result = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0", tbody);
        }

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
     */
    public static HeatDisplayHelper getHeatCapacityForDisplay(Entity entity) {
        int heatCap;

        if (entity instanceof Mek mek) {
            heatCap = mek.getHeatCapacity(true, false);
        } else if (entity instanceof Aero aero) {
            heatCap = aero.getHeatCapacity(false);
        } else {
            heatCap = entity.getHeatCapacity();
        }

        int heatCapOrg = heatCap;
        int heatCapWater = entity.getHeatCapacityWithWater();

        if (entity.hasActivatedRadicalHS()) {
            if (entity instanceof Mek mek) {
                heatCap += mek.getActiveSinks();
                heatCapWater += mek.getActiveSinks();
            } else if (entity instanceof Aero aero) {
                heatCap += aero.getHeatSinks();
                heatCapWater += aero.getHeatSinks();
            }
        }

        String heatCapacityStr = Integer.toString(heatCap);

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
        result += Messages.getString("BoardView1.Tooltip.ArmorInternals", armorStr, internalStr);
        result += ' ' + getDamageLevelDesc(entity, true);

        if (!isGunEmplacement && entity.isImmobile()) {
            String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getWarningColor()));
            result += UIUtil.tag("FONT", attr, Messages.getString("BoardView1.Tooltip.Immobile"));
        }

        // Unit Prone
        if (!isGunEmplacement && entity.isProne()) {
            String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getWarningColor()));
            result += UIUtil.tag("FONT", attr, Messages.getString("BoardView1.Tooltip.Prone"));
        }

        return result;
    }

    public static String getSensorDesc(Entity e) {
        Compute.SensorRangeHelper srh = Compute.getSensorRanges(e.getGame(), e);

        if (srh == null) {
            return Messages.getString("NONE");
        }

        if (e.isAirborneAeroOnGroundMap()) {
            return e.getActiveSensor().getDisplayName() +
                  " (" +
                  srh.minSensorRange +
                  "-" +
                  srh.maxSensorRange +
                  ")" +
                  " {" +
                  Messages.getString("BoardView1.Tooltip.sensor_range_vs_ground_target") +
                  " (" +
                  srh.minGroundSensorRange +
                  "-" +
                  srh.maxGroundSensorRange +
                  ")}";
        }
        return e.getActiveSensor().getDisplayName() + " (" + srh.minSensorRange + "-" + srh.maxSensorRange + ")";
    }

    public static String getDamageLevelDesc(Entity entity, boolean useHtml) {
        String result;
        String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getWarningColor()));

        if (entity.isDoomed() || entity.isDestroyed()) {
            String msg_destroyed = Messages.getString("BoardView1.Tooltip.Destroyed");

            return useHtml ? UIUtil.tag("FONT", attr, msg_destroyed) : msg_destroyed;
        }

        result = switch (entity.getDamageLevel()) {
            case Entity.DMG_CRIPPLED -> {
                String msgCrippled = Messages.getString("BoardView1.Tooltip.Crippled");
                yield useHtml ? UIUtil.tag("FONT", attr, msgCrippled) : msgCrippled;
            }
            case Entity.DMG_HEAVY -> {
                String msgHeavyDmg = Messages.getString("BoardView1.Tooltip.HeavyDmg");
                yield useHtml ? UIUtil.tag("FONT", attr, msgHeavyDmg) : msgHeavyDmg;
            }
            case Entity.DMG_MODERATE -> Messages.getString("BoardView1.Tooltip.ModerateDmg");
            case Entity.DMG_LIGHT -> Messages.getString("BoardView1.Tooltip.LightDmg");
            default -> Messages.getString("BoardView1.Tooltip.Undamaged");
        };
        return result;
    }

    private static String getBvInfo(GameOptions gameOptions, Entity entity, Player localPlayer, boolean showBV) {
        String result = "";
        if (showBV) {
            // BV Info
            // Hidden for invisible units when in double-blind and hide enemy bv is selected
            // Also not shown in the lobby as BV is shown there outside the tooltip
            boolean showEnemyBV = !(gameOptions.booleanOption(OptionsConstants.ADVANCED_SUPPRESS_DB_BV) &&
                  gameOptions.booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND));
            boolean isVisible = EntityVisibilityUtils.trackThisEntitiesVisibilityInfo(localPlayer, entity);

            if (isVisible || showEnemyBV) {
                int currentBV = entity.calculateBattleValue(false, false);
                int initialBV = entity.getInitialBV();
                double percentage = (double) currentBV / initialBV;
                result += addToTT("BV", false, currentBV, initialBV, percentage).toString();
            }
        }

        return result;
    }

    private static String getMovementInfo(Game game, Entity entity) {
        String result = "";
        // "Has not yet moved" only during movement phase
        if (!entity.isDone() && game.getPhase().isMovement()) {
            String sNotYetMoved = addToTT("NotYetMoved", NOBR).toString();
            String attr = String.format("FACE=Dialog COLOR=%s",
                  UIUtil.toColorHexString(GUIP.getColorForMovement(entity.moved)));
            sNotYetMoved = UIUtil.tag("FONT", attr, sNotYetMoved);
            result += UIUtil.tag("I", "", sNotYetMoved);
        } else if ((entity.isDone() && game.getPhase().isMovement()) ||
              (game.getPhase().isMovementReport()) ||
              (game.getPhase().isFiring()) ||
              (game.getPhase().isFiringReport()) ||
              (game.getPhase().isPhysical()) ||
              (game.getPhase().isPhysicalReport())) {
            int tmm = Compute.getTargetMovementModifier(game, entity.getId()).getValue();
            String sMove;

            if (entity.moved == EntityMovementType.MOVE_NONE) {
                sMove = addToTT("NoMove", NOBR, tmm).toString();
            } else {
                sMove = addToTT("MovementF",
                      NOBR,
                      entity.getMovementString(entity.moved),
                      entity.delta_distance,
                      tmm).toString();
            }
            sMove = UIUtil.tag("I", "", sMove);

            // Special Moves
            if (entity.isEvading()) {
                String sSpecialMove = addToTT("Evade", NOBR).toString();
                String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getPrecautionColor()));
                sSpecialMove = UIUtil.tag("FONT", attr, sSpecialMove);
                sSpecialMove = UIUtil.tag("I", "", sSpecialMove);
                sMove += sSpecialMove;
            }

            if ((entity instanceof Infantry) && ((Infantry) entity).isTakingCover()) {
                String sTakingCover = addToTT("TakingCover", NOBR).toString();
                String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getPrecautionColor()));
                sTakingCover = UIUtil.tag("FONT", attr, sTakingCover);
                sTakingCover = UIUtil.tag("I", "", sTakingCover);
                sMove += sTakingCover;
            }

            if (entity.isCharging()) {
                sMove += addToTT("Charging", NOBR).toString();
            }

            if (entity.isMakingDfa()) {
                String sDFA = addToTT("DFA", NOBR).toString();
                sDFA = UIUtil.tag("I", "", sDFA);
                String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getWarningColor()));
                sDFA = UIUtil.tag("FONT", attr, sDFA);
                sMove += sDFA;
            }

            if (entity.isUnjammingRAC()) {
                String sUnJamming = " ";
                String msgUnjammingRAC = Messages.getString("BoardView1.Tooltip.UnjammingRAC");
                sUnJamming += msgUnjammingRAC;
                if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_UNJAM_UAC)) {
                    String msgAndAC = Messages.getString("BoardView1.Tooltip.AndAC");
                    sUnJamming += msgAndAC;
                }
                sMove += sUnJamming;
            }

            String attr = String.format("FACE=Dialog COLOR=%s",
                  UIUtil.toColorHexString(GUIP.getColorForMovement(entity.moved)));
            result += UIUtil.tag("FONT", attr, sMove);
        }

        if (entity instanceof Infantry) {
            InfantryMount mount = ((Infantry) entity).getMount();
            if ((mount != null) && entity.getMovementMode().isSubmarine() && (entity.underwaterRounds > 0)) {
                String uw = " " + addToTT("InfUWDuration", NOBR, mount.getUWEndurance() - entity.underwaterRounds);
                if (entity.underwaterRounds >= mount.getUWEndurance()) {
                    String attr = String.format("FACE=Dialog COLOR=%s",
                          UIUtil.toColorHexString(GUIP.getWarningColor()));
                    uw = UIUtil.tag("FONT", attr, uw);
                }
                result += uw;
            }
        }

        String sAeroInfo = "";

        if (entity.isAero()) {
            // Velocity, Altitude, Elevation, Fuel
            IAero aero = (IAero) entity;
            sAeroInfo = addToTT("AeroVelAltFuel",
                  NOBR,
                  aero.getCurrentVelocity(),
                  aero.getAltitude(),
                  aero.getCurrentFuel()).toString();
        } else if (entity.getElevation() != 0) {
            // Elevation only
            sAeroInfo = addToTT("Elev", NOBR, entity.getElevation()).toString();
        }

        String attr = String.format("FACE=Dialog COLOR=%s",
              UIUtil.toColorHexString(GUIP.getUnitToolTipHighlightColor()));
        sAeroInfo = UIUtil.tag("FONT", attr, sAeroInfo);
        result += UIUtil.tag("I", attr, sAeroInfo);

        return result;
    }

    private static String getHeatInfo(Entity entity) {
        String attr;
        String result = "";

        // Heat, not shown for units with 999 heat sinks (vehicles)
        if (entity.getHeatCapacity() != 999) {
            int heat = entity.heat;
            String sHeat = "";
            if (heat == 0) {
                sHeat += addToTT("Heat0", NOBR).toString();
            } else {
                sHeat += addToTT("Heat", NOBR, heat).toString();
            }
            HeatDisplayHelper hdh = getHeatCapacityForDisplay(entity);
            sHeat += " / " + hdh.heatCapacityStr;
            attr = String.format("FACE=Dialog COLOR=%s",
                  UIUtil.toColorHexString(GUIP.getColorForHeat(heat, GUIP.getUnitToolTipFGColor())));
            result += UIUtil.tag("FONT", attr, sHeat);

            if (entity instanceof Mek && ((Mek) entity).hasActiveTSM()) {
                result += DOT_SPACER;
                String sTSM = "TSM";
                attr = String.format("FACE=Dialog COLOR=%s",
                      UIUtil.toColorHexString(GUIP.getColorForHeat(heat, GUIP.getPrecautionColor())));
                result += UIUtil.tag("FONT", attr, sTSM);
            }
        }

        String illuminated = entity.isIlluminated() ? DOT_SPACER + "\uD83D\uDCA1" : "";
        if (!illuminated.isEmpty()) {
            attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString((GUIP.getCautionColor())));
            result += UIUtil.tag("FONT", attr, illuminated);
        }

        if (entity.hasSearchlight()) {
            String searchLight = entity.isUsingSearchlight() ? DOT_SPACER + "\uD83D\uDD26" : "&nbsp;";
            searchLight += entity.usedSearchlight() ? " \u2580\u2580" : "";
            attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString((GUIP.getCautionColor())));
            result += UIUtil.tag("FONT", attr, searchLight);
        } else {
            String searchLight = "\uD83D\uDD26";
            attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString((GUIP.getWarningColor())));
            result += UIUtil.tag("FONT", attr, DOT_SPACER + searchLight);
        }

        return result;
    }

    private static String getUnitStatus(Game game, Entity entity, boolean isGunEmplacement) {
        String attr;
        String result = "";

        // Gun Emplacement Status
        if (isGunEmplacement) {
            GunEmplacement emp = (GunEmplacement) entity;
            if (emp.isTurret() && emp.isTurretLocked(emp.getLocTurret())) {
                String sTurretLocked = addToTT("TurretLocked", NOBR) + " ";
                attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString((GUIP.getWarningColor())));
                sTurretLocked = UIUtil.tag("FONT", attr, sTurretLocked);
                result += UIUtil.tag("I", "", sTurretLocked);
            }
        }

        // Unit Immobile
        if (!isGunEmplacement && entity.isImmobile()) {
            String sImmobile = addToTT("Immobile", NOBR) + " ";
            attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString((GUIP.getWarningColor())));
            result += UIUtil.tag("FONT", attr, sImmobile);
        }

        // Unit Prone
        if (!isGunEmplacement && entity.isProne()) {
            String sUnitProne = addToTT("Prone", NOBR) + " ";
            attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString((GUIP.getCautionColor())));
            result += UIUtil.tag("FONT", attr, sUnitProne);
        }

        if (!entity.getHiddenActivationPhase().isUnknown()) {
            result += addToTT("HiddenActivating", NOBR, entity.getHiddenActivationPhase().toString()) + " ";
        } else if (entity.isHidden()) {
            result += addToTT("Hidden", BR) + " ";
        }

        // Swarmed
        if (entity.getSwarmAttackerId() != Entity.NONE) {
            final Entity swarmAttacker = game.getEntity(entity.getSwarmAttackerId());
            if (swarmAttacker == null) {
                logger.error("Entity {} is currently swarmed by an unknown attacker with id {}",
                      entity.getId(),
                      entity.getSwarmAttackerId());
            }
            String msg_error = Messages.getString("ERROR");
            String sa = (swarmAttacker == null) ? msg_error : swarmAttacker.getDisplayName();
            String sSwarmed = addToTT("Swarmed", NOBR, sa) + " ";
            attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString((GUIP.getWarningColor())));
            result += UIUtil.tag("FONT", attr, sSwarmed);
        }

        // Spotting
        Entity spotTarget = game.getEntity(entity.getSpotTargetId());
        if (entity.isSpotting() && spotTarget != null) {
            String sSpotting = addToTT("Spotting", NOBR, spotTarget.getDisplayName()) + " ";
            result += sSpotting;
        }

        if (entity.hasAnyTypeNarcPodsAttached()) {
            String sNarced = addToTT(entity.hasNarcPodsAttached() ? "Narced" : "INarced", NOBR) + " ";
            attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString((GUIP.getPrecautionColor())));
            result += UIUtil.tag("FONT", attr, sNarced);
        }

        // Pheromone impaired (IO pg 79)
        if ((entity instanceof Infantry infantry) && infantry.isPheromoneImpaired()) {
            String sPheromone = addToTT("PheromoneImpaired", NOBR) + " ";
            attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString((GUIP.getWarningColor())));
            result += UIUtil.tag("FONT", attr, sPheromone);
        }

        // Towing
        if (!entity.getAllTowedUnits().isEmpty()) {
            String unitList = entity.getAllTowedUnits()
                  .stream()
                  .map(id -> Objects.requireNonNull(entity.getGame().getEntity(id)).getShortName())
                  .collect(Collectors.joining(", "));
            if (unitList.length() > 1) {
                result += addToTT("Towing", NOBR, unitList) + " ";
            }
        }

        return result;
    }

    private static String getSeenByInfo(Game game, GameOptions gameOptions, Entity entity) {
        String result = "";

        // If Double Blind, add information about who sees this Entity
        if (gameOptions.booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
            StringBuilder tempList = new StringBuilder();
            boolean teamVision = gameOptions.booleanOption(OptionsConstants.ADVANCED_TEAM_VISION);
            int seenByResolution = GUIP.getUnitToolTipSeenByResolution();
            String tmpStr;

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
                result = addToTT("SeenBy", NOBR, tempList.toString()).toString();
            }
        }

        return result;
    }

    private static String getSensorInfo(GameOptions gameOptions, Entity entity, PlanetaryConditions conditions) {
        String sensors = "";
        // If sensors, display what sensors this unit is using
        if (gameOptions.booleanOption(OptionsConstants.ADVANCED_TAC_OPS_SENSORS) ||
              (gameOptions.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADVANCED_SENSORS)) &&
                    entity.isSpaceborne()) {
            String visualRange = Compute.getMaxVisualRange(entity, false) + "";
            if (conditions.getLight().isDuskOrFullMoonOrMoonlessOrPitchBack()) {
                visualRange += " (" + Compute.getMaxVisualRange(entity, true) + ")";
            }
            sensors += addToTT("Sensors", NOBR, getSensorDesc(entity), visualRange);
        } else {
            String visualRange = Compute.getMaxVisualRange(entity, false) + "";
            if (conditions.getLight().isDuskOrFullMoonOrMoonlessOrPitchBack()) {
                visualRange += " (" + Compute.getMaxVisualRange(entity, true) + ")";
            }
            sensors += addToTT("Visual", NOBR, visualRange);
        }
        if (gameOptions.booleanOption(OptionsConstants.ADVANCED_TAC_OPS_BAP) && entity.hasBAP()) {
            sensors += addToTT("BAPRange", NOBR, entity.getBAPRange());
        }

        return sensors;
    }

    /**
     * Returns Variable Range Targeting mode info for tooltip display. Shows icon + mode name for units with VRT quirk
     * (BMM pg. 86).
     */
    private static String getVariableRangeTargetingInfo(Entity entity) {
        if (!entity.hasVariableRangeTargeting()) {
            return "";
        }

        VariableRangeTargetingMode mode = entity.getVariableRangeTargetingMode();
        String modeKey = mode.isLong()
              ? "BoardView1.Tooltip.VRTModeLong"
              : "BoardView1.Tooltip.VRTModeShort";
        String modeText = Messages.getString(modeKey);

        // Format: VRT_SIGN VRT: Long (or Short)
        return VRT_SIGN + Messages.getString("BoardView1.Tooltip.VRT") + ": " + modeText;
    }

    /** Returns values that only are relevant when in-game such as heat. */
    private static StringBuilder inGameValues(Entity entity, Player localPlayer, boolean inGameValue, boolean showBV,
          boolean showSensors, boolean showSeenBy) {
        String col;
        String row;
        String rows = "";
        String attr;
        Game game = entity.getGame();
        GameOptions gameOptions = game.getOptions();
        PlanetaryConditions conditions = game.getPlanetaryConditions();
        boolean isGunEmplacement = entity instanceof GunEmplacement;
        String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());

        if (!inGameValue) {
            return new StringBuilder();
        }

        // BV and Damage
        String bvDamageLevel = getBvInfo(gameOptions, entity, localPlayer, showBV);

        bvDamageLevel += " " + getDamageLevelDesc(entity, true);

        if (!bvDamageLevel.isEmpty()) {
            attr = String.format("FACE=Dialog COLOR=%s",
                  UIUtil.toColorHexString((GUIP.getUnitToolTipHighlightColor())));
            bvDamageLevel = UIUtil.tag("FONT", attr, bvDamageLevel);
            bvDamageLevel = UIUtil.tag("span", fontSizeAttr, bvDamageLevel);
            col = UIUtil.tag("TD", "", bvDamageLevel);
            row = UIUtil.tag("TR", "", col);
            rows += row;
        }

        // Actual Movement
        if (!isGunEmplacement) {
            String movementInfo = getMovementInfo(game, entity);
            if (!movementInfo.isEmpty()) {
                movementInfo = UIUtil.tag("span", fontSizeAttr, movementInfo);
                col = UIUtil.tag("TD", "", movementInfo);
                row = UIUtil.tag("TR", "", col);
                rows += row;
            }
        }

        // Facing
        String msg_facing = Messages.getString("BoardView1.Tooltip.Facing");
        String sFacingTwist = "&nbsp;&nbsp;" + msg_facing + ":&nbsp;" + entity.getFacingName(entity.getFacing());

        if (entity.getFacing() != entity.getSecondaryFacing()) {
            String msg_twist = Messages.getString("BoardView1.Tooltip.Twist");
            sFacingTwist += "&nbsp;&nbsp;" + msg_twist + ":&nbsp;" + entity.getFacingName(entity.getSecondaryFacing());
        }

        if (!sFacingTwist.isEmpty()) {
            attr = String.format("FACE=Dialog COLOR=%s",
                  UIUtil.toColorHexString((GUIP.getUnitToolTipHighlightColor())));
            sFacingTwist = UIUtil.tag("FONT", attr, sFacingTwist);
            sFacingTwist = UIUtil.tag("span", fontSizeAttr, sFacingTwist);
            col = UIUtil.tag("TD", "", sFacingTwist);
            row = UIUtil.tag("TR", "", col);
            rows += row;
        }

        // Variable Range Targeting mode (BMM pg. 86)
        String vrtInfo = getVariableRangeTargetingInfo(entity);
        if (!vrtInfo.isEmpty()) {
            attr = String.format("FACE=Dialog COLOR=%s",
                  UIUtil.toColorHexString((GUIP.getUnitToolTipHighlightColor())));
            vrtInfo = UIUtil.tag("FONT", attr, vrtInfo);
            vrtInfo = UIUtil.tag("span", fontSizeAttr, vrtInfo);
            col = UIUtil.tag("TD", "", vrtInfo);
            row = UIUtil.tag("TR", "", col);
            rows += row;
        }

        // Heat
        String heatInfo = getHeatInfo(entity);
        if (!heatInfo.isEmpty()) {
            heatInfo = UIUtil.tag("span", fontSizeAttr, heatInfo);
            col = UIUtil.tag("TD", "", heatInfo);
            row = UIUtil.tag("TR", "", col);
            rows += row;
        }

        // Unit status
        String unitStatus = getUnitStatus(game, entity, isGunEmplacement);
        if (!unitStatus.isEmpty()) {
            unitStatus = UIUtil.tag("span", fontSizeAttr, unitStatus);
            col = UIUtil.tag("TD", "", unitStatus);
            row = UIUtil.tag("TR", "", col);
            rows += row;
        }

        // Seen by
        if (showSeenBy) {
            String seenByInfo = getSeenByInfo(game, gameOptions, entity);
            if (!seenByInfo.isEmpty()) {
                attr = String.format("FACE=Dialog COLOR=%s",
                      UIUtil.toColorHexString((GUIP.getUnitToolTipHighlightColor())));
                seenByInfo = UIUtil.tag("FONT", attr, seenByInfo);
                seenByInfo = UIUtil.tag("span", fontSizeAttr, seenByInfo);
                col = UIUtil.tag("TD", "", seenByInfo);
                row = UIUtil.tag("TR", "", col);
                rows += row;
            }
        }

        // Sensors
        if (showSensors) {
            String sensorInfo = getSensorInfo(gameOptions, entity, conditions);
            attr = String.format("FACE=Dialog COLOR=%s",
                  UIUtil.toColorHexString((GUIP.getUnitToolTipHighlightColor())));
            sensorInfo = UIUtil.tag("FONT", attr, sensorInfo);
            sensorInfo = UIUtil.tag("span", fontSizeAttr, sensorInfo);
            col = UIUtil.tag("TD", "", sensorInfo);
            row = UIUtil.tag("TR", "", col);
            rows += row;
        }

        String table = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0", rows);

        return new StringBuilder().append(table);
    }

    /**
     * Returns unit values that are relevant in-game and in the lobby such as movement ability.
     */
    private static StringBuilder getMovement(Entity entity) {
        boolean isGunEmplacement = entity instanceof GunEmplacement;
        String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
        String result;
        String col;
        String row;
        String rows = "";

        // Unit movement ability
        if (!isGunEmplacement) {
            int hipHits = 0;
            int actuatorHits = 0;
            int legsDestroyed = 0;

            if (entity instanceof Mek) {
                if (entity.getMovementMode() == EntityMovementMode.TRACKED) {
                    for (Mounted<?> m : entity.getMisc()) {
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
                                if (((Mek) entity).legHasHipCrit(i)) {
                                    hipHits++;
                                    if ((entity.getGame() == null) ||
                                          (!entity.getGame()
                                                .getOptions()
                                                .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEG_DAMAGE))) {
                                        continue;
                                    }
                                }
                                actuatorHits += ((Mek) entity).countLegActuatorCrits(i);
                            } else {
                                legsDestroyed++;
                            }
                        }
                    }
                }
            }

            int jumpJet = 0;
            int jumpJetDestroyed = 0;
            int jumpBooster = 0;
            int jumpBoosterDestroyed = 0;
            int partialWing = 0;
            int partialWingDestroyed = 0;
            int partialWingWeatherMod = 0;

            if ((entity instanceof Mek) || (entity instanceof Tank)) {
                for (Mounted<?> mounted : entity.getMisc()) {
                    if (mounted.getType().hasFlag(MiscType.F_JUMP_JET)) {
                        jumpJet++;
                        if (mounted.isDestroyed() || mounted.isBreached()) {
                            jumpJetDestroyed++;
                        }
                    }
                    if (mounted.getType().hasFlag(MiscType.F_JUMP_BOOSTER)) {
                        jumpBooster++;
                        if (mounted.isDestroyed() || mounted.isBreached()) {
                            jumpBoosterDestroyed++;
                        }
                    }
                    if (mounted.getType().hasFlag(MiscType.F_PARTIAL_WING)) {
                        int eNum = entity.getEquipmentNum(mounted);
                        partialWing += entity.getGoodCriticalSlots(CriticalSlot.TYPE_EQUIPMENT,
                              eNum,
                              Mek.LOC_RIGHT_TORSO);
                        partialWing += entity.getGoodCriticalSlots(CriticalSlot.TYPE_EQUIPMENT,
                              eNum,
                              Mek.LOC_LEFT_TORSO);
                        partialWingDestroyed += entity.getBadCriticalSlots(CriticalSlot.TYPE_EQUIPMENT,
                              eNum,
                              Mek.LOC_RIGHT_TORSO);
                        partialWingDestroyed += entity.getBadCriticalSlots(CriticalSlot.TYPE_EQUIPMENT,
                              eNum,
                              Mek.LOC_LEFT_TORSO);

                        if (entity instanceof Mek mek) {
                            partialWingWeatherMod = mek.getPartialWingJumpAtmosphereBonus() -
                                  mek.getPartialWingJumpWeightClassBonus();
                        }
                    }
                }

                partialWing += partialWingDestroyed;
            }

            int walkMP = entity.getOriginalWalkMP();
            int runMP = entity.getOriginalRunMP();
            int jumpMP = entity.getOriginalJumpMP();
            String sMove = addToTT("Movement", NOBR, walkMP, runMP).toString();

            if (jumpMP > 0) {
                sMove += "/" + jumpMP;
            }

            if (entity instanceof Mek mek) {
                int mekMechanicalJumpMP = mek.getMechanicalJumpBoosterMP();
                if (mekMechanicalJumpMP > 0) {
                    if (jumpMP == 0) {
                        sMove += "/%d".formatted(mekMechanicalJumpMP);
                    } else {
                        sMove += " (%d)".formatted(mekMechanicalJumpMP);
                    }
                }
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
                if (entity.getGame().getPlanetaryConditions().getGravity() != 1.0) {
                    sMove += DOT_SPACER;
                    String sGravity = entity.getGame().getPlanetaryConditions().getGravity() + "g";
                    String attr = String.format("FACE=Dialog COLOR=%s",
                          UIUtil.toColorHexString((GUIP.getWarningColor())));
                    sMove += UIUtil.tag("FONT", attr, sGravity);
                }
                int walkMPNoHeat = entity.getWalkMP(MPCalculationSetting.NO_HEAT);
                int runMPNoHeat = entity.getRunMP(MPCalculationSetting.NO_HEAT);
                if ((walkMPNoHeat != walkMPModified) || (runMPNoHeat != runMPModified)) {
                    sMove += DOT_SPACER;
                    String sHeat = "\uD83D\uDD25";
                    String attr = String.format("FACE=Dialog COLOR=%s",
                          UIUtil.toColorHexString((GUIP.getWarningColor())));
                    sMove += UIUtil.tag("FONT", attr, sHeat);
                }
            }

            if (entity instanceof IBomber) {
                int bombMod = ((IBomber) entity).reduceMPByBombLoad(walkMP);
                if (bombMod != walkMP) {
                    sMove += DOT_SPACER;
                    String sBomb = "\uD83D\uDCA3";
                    String attr = String.format("FACE=Dialog COLOR=%s",
                          UIUtil.toColorHexString((GUIP.getWarningColor())));
                    sMove += UIUtil.tag("FONT", attr, sBomb);
                }
            }

            int weatherMod = entity.getGame().getPlanetaryConditions().getMovementMods(entity);

            if ((weatherMod != 0) || (partialWingWeatherMod != 0)) {
                sMove += DOT_SPACER;
                String sWeather = "\u2602";
                String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString((GUIP.getWarningColor())));
                sMove += UIUtil.tag("FONT", attr, sWeather);
            }

            if ((legsDestroyed > 0) ||
                  (hipHits > 0) ||
                  (actuatorHits > 0) ||
                  (jumpJetDestroyed > 0) ||
                  (partialWingDestroyed > 0) ||
                  (jumpBoosterDestroyed > 0) ||
                  (entity.isImmobile()) ||
                  (entity.isGyroDestroyed())) {
                sMove += DOT_SPACER;
                String sDamage = "\uD83D\uDD27";
                String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString((GUIP.getWarningColor())));
                sMove += UIUtil.tag("FONT", attr, sDamage);
            }

            if ((entity instanceof BipedMek) || (entity instanceof TripodMek)) {
                int shieldMod = 0;
                if (entity.hasShield()) {
                    shieldMod -= entity.getNumberOfShields(MiscType.S_SHIELD_LARGE);
                    shieldMod -= entity.getNumberOfShields(MiscType.S_SHIELD_MEDIUM);
                }

                if (shieldMod != 0) {
                    sMove += DOT_SPACER;
                    String sShield = "\u26E8";
                    String attr = String.format("FACE=Dialog COLOR=%s",
                          UIUtil.toColorHexString((GUIP.getWarningColor())));
                    sMove += UIUtil.tag("FONT", attr, sShield);
                }
            }

            if (entity.hasModularArmor()) {
                sMove += DOT_SPACER;
                String sArmor = "\u27EC\u25AE";
                String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString((GUIP.getWarningColor())));
                sMove += UIUtil.tag("FONT", attr, sArmor);
            }
            // Display SI for Aerodynes, and LAMs only if in Fighter mode
            if (entity instanceof IAero unit &&
                  !(entity instanceof LandAirMek && !(entity.getConversionMode() == CONV_MODE_FIGHTER))) {
                sMove += DOT_SPACER;
                sMove += String.format(" SI: %d", unit.getSI());
            }


            sMove = UIUtil.tag("span", fontSizeAttr, sMove);
            col = UIUtil.tag("TD", "", sMove);
            row = UIUtil.tag("TR", "", col);
            rows += row;

            if ((jumpJetDestroyed > 0) || (jumpBoosterDestroyed > 0) || (partialWingDestroyed > 0)) {
                String jj = "";
                if (jumpJetDestroyed > 0) {
                    String msgJumpJets = Messages.getString("BoardView1.Tooltip.JumpJets");
                    jj = msgJumpJets + ": " + (jumpJet - jumpJetDestroyed) + "/" + jumpJet;
                }
                if (jumpBoosterDestroyed > 0) {
                    String msg_jumpBoosters = Messages.getString("BoardView1.Tooltip.JumpBoosters");
                    jj += "; " + msg_jumpBoosters + ": " + (jumpBooster - jumpBoosterDestroyed) + "/" + jumpBooster;
                }
                if (partialWingDestroyed > 0) {
                    String msgPartialWing = Messages.getString("BoardView1.Tooltip.PartialWing");
                    jj += "; " + msgPartialWing + ": " + (partialWing - partialWingDestroyed) + "/" + partialWing;
                }
                if (jj.startsWith(";")) {
                    jj = jj.substring(2);
                }

                jj = UIUtil.tag("span", fontSizeAttr, jj);
                col = UIUtil.tag("TD", "", jj);
                row = UIUtil.tag("TR", "", col);
                rows += row;
            }
        }
        // Infantry specialization like SCUBA
        if (entity instanceof Infantry infantry) {
            int spec = infantry.getSpecializations();
            if (spec > 0) {
                String sInfantrySpec = addToTT("InfSpec", NOBR, Infantry.getSpecializationName(spec)).toString();
                sInfantrySpec = UIUtil.tag("span", fontSizeAttr, sInfantrySpec);
                col = UIUtil.tag("TD", "", sInfantrySpec);
                row = UIUtil.tag("TR", "", col);
                rows += row;
            }
        }

        String tbody = UIUtil.tag("TBODY", "", rows);
        String table = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0", tbody);

        String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getUnitToolTipFGColor()));
        result = UIUtil.tag("FONT", attr, table);

        return new StringBuilder().append(result);
    }

    private static StringBuilder getArmor(Entity entity) {
        boolean isGunEmplacement = entity instanceof GunEmplacement;
        String result;
        String col;
        String row;
        String rows = "";
        String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());

        // Armor and Internals
        if (entity instanceof FighterSquadron) {
            String msgArmorCapital = Messages.getString("BoardView1.Tooltip.ArmorCapital");
            String armorStr = entity.getTotalArmor() + " / " + entity.getTotalOArmor() + " " + msgArmorCapital;
            String sArmorInternals = Messages.getString("BoardView1.Tooltip.FSQTotalArmor", armorStr);
            sArmorInternals = UIUtil.tag("span", fontSizeAttr, sArmorInternals);

            col = UIUtil.tag("TD", "", sArmorInternals);
            row = UIUtil.tag("TR", "", col);
            rows += row;
        } else if (!isGunEmplacement) {
            String msg_unknown = Messages.getString("BoardView1.Tooltip.Unknown");
            String armorType = TROView.formatArmorType(entity, true).replace(msg_unknown, "");
            if (!armorType.isBlank()) {
                String msgArmorCapital = Messages.getString("BoardView1.Tooltip.ArmorCapital");
                armorType = (entity.isCapitalScale() ? msgArmorCapital + " " : "") + armorType;
                armorType = " (" + armorType + ") ";
            }
            String armorStr = entity.getTotalArmor() + " / " + entity.getTotalOArmor() + armorType;
            String internalStr = entity.getTotalInternal() + " / " + entity.getTotalOInternal();
            String sArmorInternals = addToTT("ArmorInternals", NOBR, armorStr, internalStr).toString();
            sArmorInternals = UIUtil.tag("span", fontSizeAttr, sArmorInternals);

            col = UIUtil.tag("TD", "", sArmorInternals);
            row = UIUtil.tag("TR", "", col);
            rows += row;
        }

        String tbody = UIUtil.tag("TBODY", "", rows);
        String table = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0", tbody);

        String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getUnitToolTipFGColor()));
        result = UIUtil.tag("FONT", attr, table);

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
            String msgCannotSurvive = Messages.getString("BoardView1.Tooltip.CannotSurvive");
            sWarnings += "<BR>" +
                  msgCannotSurvive +
                  " " +
                  entity.getGame().getPlanetaryConditions().whyDoomed(entity, entity.getGame());
        }
        if (entity.doomedInAtmosphere() && mapSettings.getMedium() == MapSettings.MEDIUM_ATMOSPHERE) {
            String msgCannotSurviveAtmosphere = Messages.getString("BoardView1.Tooltip.CannotSurviveAtmo");
            sWarnings += "<BR>" + msgCannotSurviveAtmosphere;
        }
        if (entity.doomedOnGround() && mapSettings.getMedium() == MapSettings.MEDIUM_GROUND) {
            String msgCannotSurviveGround = Messages.getString("BoardView1.Tooltip.CannotSurviveGround");
            sWarnings += "<BR>" + msgCannotSurviveGround;
        }
        if (entity.doomedInSpace() && mapSettings.getMedium() == MapSettings.MEDIUM_SPACE) {
            String msgCannotSurviveSpace = Messages.getString("BoardView1.Tooltip.CannotSurviveSpace");
            sWarnings += "<BR>" + msgCannotSurviveSpace;
        }

        result += sWarnings;

        String sNonCritical = "";
        // Non-critical (yellow) warnings
        if (((entity.hasC3i() || entity.hasNavalC3()) && (entity.calculateFreeC3Nodes() == 5)) ||
              ((entity.getC3Master() == null) && entity.hasC3S()) ||
              (entity.hasNovaCEWS() && (entity.calculateFreeC3Nodes() == 2))) {
            String msgUnconnectedC3Computer = Messages.getString("BoardView1.Tooltip.UnconnectedC3Computer");
            sNonCritical += "<BR>" + msgUnconnectedC3Computer;
        }

        // Non-critical (yellow) warnings
        if (entity instanceof FighterSquadron && entity.getLoadedUnits().isEmpty()) {
            String msgFighterSquadronEmpty = Messages.getString("BoardView1.Tooltip.FighterSquadronEmpty");
            sNonCritical += "<BR>" + msgFighterSquadronEmpty;
        }

        result += sNonCritical;
        if (!result.isEmpty()) {
            String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getCautionColor()));
            result += UIUtil.tag("FONT", attr, result);

            String col = UIUtil.tag("TD", "", result);
            String row = UIUtil.tag("TR", "", col);
            String tbody = UIUtil.tag("TBODY", "", row);
            result = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0", tbody);
        }

        return new StringBuilder().append(result);
    }

    /** Returns a list of units loaded onto this unit. */
    private static StringBuilder carriedUnits(Entity entity) {
        String result = "";
        StringBuilder sCarriedUnits = new StringBuilder();

        if (!entity.getLoadedUnits().isEmpty()) {
            if (entity instanceof FighterSquadron) {
                String msg_fighter = Messages.getString("BoardView1.Tooltip.Fighters");
                sCarriedUnits.append(msg_fighter).append(":");
            } else {
                String msgCarriedUnits = Messages.getString("BoardView1.Tooltip.CarriedUnits");
                sCarriedUnits.append(msgCarriedUnits).append(":");
            }

            for (Entity carried : entity.getLoadedUnits()) {
                sCarriedUnits.append("<BR>&nbsp;&nbsp;").append(carried.getShortNameRaw());
                if (PreferenceManager.getClientPreferences().getShowUnitId()) {
                    sCarriedUnits.append(" [").append(carried.getId()).append("]");
                }
            }

            String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
            sCarriedUnits = new StringBuilder(UIUtil.tag("span", fontSizeAttr, sCarriedUnits.toString()));

            String col = UIUtil.tag("TD", "", sCarriedUnits.toString());
            String row = UIUtil.tag("TR", "", col);
            String tbody = UIUtil.tag("TBODY", "", row);
            result = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0", tbody);
        }

        return new StringBuilder().append(result);
    }

    private static StringBuilder carriedCargo(Entity entity) {
        StringBuilder sb = new StringBuilder();
        String result = "";
        List<ICarryable> cargoList = entity.getDistinctCarriedObjects();

        if (!cargoList.isEmpty()) {
            StringBuilder carriedCargo = new StringBuilder(Messages.getString("MissionRole.cargo"));
            carriedCargo.append(":<br/>&nbsp;&nbsp;");

            for (ICarryable cargo : entity.getDistinctCarriedObjects()) {
                carriedCargo.append(cargo.toString());
                carriedCargo.append("<br/>&nbsp;&nbsp;");
            }

            String col = UIUtil.tag("TD", "", carriedCargo.toString());
            String row = UIUtil.tag("TR", "", col);
            String tbody = UIUtil.tag("TBODY", "", row);
            result = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0", tbody);
        }

        return sb.append(result);
    }


    private static String getForceInfo(Entity entity) {
        StringBuilder sForceEntry = new StringBuilder();
        var forceChain = entity.getGame().getForces().forceChain(entity);

        for (int i = forceChain.size() - 1; i >= 0; i--) {
            sForceEntry.append(forceChain.get(i).getName());
            sForceEntry.append(i != 0 ? ", " : "");
        }

        return sForceEntry.toString();
    }

    /** Returns the full force chain the entity is in as one text line. */
    private static StringBuilder forceEntry(Entity entity, Player localPlayer) {
        String result = "";
        String sForceEntry;

        if (entity.partOfForce()) {
            // Get the / ally / enemy color and desaturate it
            Color color = GUIP.getEnemyUnitColor();
            if (localPlayer != null && entity.getOwnerId() == localPlayer.getId()) {
                color = GUIP.getMyUnitColor();
            } else if (localPlayer != null && !localPlayer.isEnemyOf(entity.getOwner())) {
                color = GUIP.getAllyUnitColor();
            }

            String force = getForceInfo(entity);
            if (!force.isEmpty()) {
                String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(color));
                sForceEntry = UIUtil.tag("FONT", attr, force);
                String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
                sForceEntry = UIUtil.tag("span", fontSizeAttr, sForceEntry);

                String col = UIUtil.tag("TD", "", sForceEntry);
                String row = UIUtil.tag("TR", "", col);
                String tbody = UIUtil.tag("TBODY", "", row);
                result = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0", tbody);
            }
        }

        return new StringBuilder().append(result);
    }

    /** Returns an overview of the C3 system the unit is in. */
    private static StringBuilder c3Info(Entity entity, boolean details) {
        String result = "";
        String sC3Info = "";

        if (details && entity.hasAnyC3System()) {
            List<String> members = entity.getGame()
                  .getEntitiesVector()
                  .stream()
                  .filter(e -> e.onSameC3NetworkAs(entity))
                  .sorted(Comparator.comparingInt(Entity::getId))
                  .map(e -> c3UnitName(e, entity))
                  .collect(Collectors.toList());
            if (members.size() > 1) {
                if (entity.hasNhC3()) {
                    String msg_c3i = Messages.getString("BoardView1.Tooltip.C3i");
                    String msg_nc3 = Messages.getString("BoardView1.Tooltip.NC3");
                    String msg_nova = Messages.getString("BoardView1.Tooltip.NovaCEWS");

                    if (entity.hasC3i()) {
                        sC3Info = msg_c3i;
                    } else if (entity.hasNovaCEWS()) {
                        sC3Info = msg_nova;
                    } else {  // hasNavalC3()
                        sC3Info = msg_nc3;
                    }
                } else {
                    sC3Info = Messages.getString("BoardView1.Tooltip.C3");
                }
                String msg_network = Messages.getString("BoardView1.Tooltip.Network");
                sC3Info += " " + msg_network + ": <BR>&nbsp;&nbsp;";
                sC3Info += String.join("<BR>&nbsp;&nbsp;", members);
                sC3Info += "<BR>";
            }

            String attr = String.format("FACE=Dialog COLOR=%s",
                  UIUtil.toColorHexString(GUIP.getUnitToolTipHighlightColor()));
            sC3Info = UIUtil.tag("FONT", attr, sC3Info);
            String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
            sC3Info = UIUtil.tag("span", fontSizeAttr, sC3Info);

            String col = UIUtil.tag("TD", "", sC3Info);
            String row = UIUtil.tag("TR", "", col);
            String tbody = UIUtil.tag("TBODY", "", row);
            result = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0", tbody);
        }

        return new StringBuilder().append(result);
    }

    private static String c3UnitName(Entity c3member, Entity entity) {
        String result = "";
        String msg_c3 = "";
        String sC3UnitName;
        String tmp = "";

        sC3UnitName = " [" + c3member.getId() + "] ";

        if (c3member.isC3CompanyCommander()) {
            msg_c3 = Messages.getString("BoardView1.Tooltip.C3CC") + " ";
        } else if (c3member.hasC3M()) {
            msg_c3 = Messages.getString("BoardView1.Tooltip.C3M") + " ";
        }

        sC3UnitName += UIUtil.tag("I", "", msg_c3);
        String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getUnitToolTipFGColor()));
        sC3UnitName = UIUtil.tag("FONT", attr, sC3UnitName);
        String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
        result += UIUtil.tag("span", fontSizeAttr, sC3UnitName);
        result += c3member.getShortNameRaw();

        String msgThisUnit = " (" + Messages.getString("BoardView1.Tooltip.ThisUnit") + ")";
        tmp += UIUtil.tag("I", "", msgThisUnit);
        String sC3Member = c3member.equals(entity) ? tmp : "";
        attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getUnitToolTipFGColor()));
        sC3Member = UIUtil.tag("FONT", attr, sC3Member);
        result += UIUtil.tag("span", fontSizeAttr, sC3Member);

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
        return game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HOT_LOAD);
    }

    /** Returns true when Hot-Loading LRMs is on. */
    static boolean isRapidFireActive(Game game) {
        return game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BURST);
    }

    private UnitToolTip() {
    }
}

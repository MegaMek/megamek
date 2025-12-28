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
package megamek.client.ui.panels.phaseDisplay.lobby;

import static megamek.client.ui.Messages.getString;
import static megamek.client.ui.util.UIUtil.*;
import static megamek.common.options.PilotOptions.LVL3_ADVANTAGES;
import static megamek.common.options.PilotOptions.MD_ADVANTAGES;

import java.awt.Color;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.List;

import megamek.MegaMek;
import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.util.PlayerColour;
import megamek.client.ui.util.UIUtil;
import megamek.common.Player;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.board.Board;
import megamek.common.force.Force;
import megamek.common.game.Game;
import megamek.common.game.InGameObject;
import megamek.common.interfaces.ForceAssignable;
import megamek.common.interfaces.IStartingPositions;
import megamek.common.loaders.MapSettings;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.*;
import megamek.common.util.CollectionUtil;
import megamek.common.util.CrewSkillSummaryUtil;

class LobbyMekCellFormatter {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private LobbyMekCellFormatter() {
    }

    static String unitTableEntry(InGameObject unit, ChatLounge lobby, boolean forceView, boolean compactView) {
        if (unit instanceof Entity) {
            return compactView ? formatUnitCompact((Entity) unit, lobby, forceView)
                  : formatUnitFull((Entity) unit, lobby, forceView);
        } else if (unit instanceof AlphaStrikeElement) {
            return MekTableASUnitEntry.fullEntry((AlphaStrikeElement) unit, lobby, forceView, compactView);
            // TODO : Provide a suitable lobby table entry
        } else {
            return "This type of object has currently no table entry.";
        }
    }

    static String pilotTableEntry(InGameObject unit, boolean compactView, boolean hide, boolean rpgSkills) {
        if (unit instanceof Entity) {
            return compactView ? formatPilotCompact((Entity) unit, hide, rpgSkills)
                  : formatPilotFull((Entity) unit, hide);
        } else if (unit instanceof AlphaStrikeElement) {
            // TODO : Provide a suitable lobby table entry
            return "AlphaStrikeElement " + ((AlphaStrikeElement) unit).getName();
        } else {
            return "This type of object has currently no table entry.";
        }
    }

    /**
     * Creates and returns the display content of the Unit column for the given entity and for the non-compact display
     * mode. When blindDrop is true, the unit details are not given.
     */
    static String formatUnitFull(Entity entity, ChatLounge lobby, boolean forceView) {
        StringBuilder result = new StringBuilder("<HTML><NOBR>" + fontHTML());

        Client client = lobby.getClientGUI().getClient();
        Game game = client.getGame();

        GameOptions options = game.getOptions();
        Player localPlayer = client.getLocalPlayer();
        Player owner = entity.getOwner();
        boolean localGM = localPlayer.isGameMaster();
        boolean hideEntity = !localGM && owner.isEnemyOf(localPlayer)
              && options.booleanOption(OptionsConstants.BASE_BLIND_DROP);
        if (hideEntity) {
            result.append(MekTableModel.DOT_SPACER);
            if (entity instanceof Infantry) {
                result.append(Messages.getString("ChatLounge.0"));
            } else if (entity instanceof ProtoMek) {
                result.append(Messages.getString("ChatLounge.1"));
            } else if (entity.isBuildingEntityOrGunEmplacement()) {
                result.append(Messages.getString("ChatLounge.2"));
            } else if (entity.isSupportVehicle()) {
                result.append(entity.getWeightClassName());
            } else if (entity.isFighter()) {
                result.append(entity.getWeightClassName()).append(Messages.getString("ChatLounge.4"));
            } else if (entity instanceof Mek) {
                result.append(entity.getWeightClassName()).append(Messages.getString("ChatLounge.3"));
            } else if (entity instanceof Tank) {
                result.append(entity.getWeightClassName()).append(Messages.getString("ChatLounge.6"));
            } else {
                result.append(entity.getWeightClassName());
            }
            result.append(MekTableModel.DOT_SPACER);
            return result.toString();
        }

        boolean isCarried = entity.getTransportId() != Entity.NONE;
        boolean isTowed = entity.getTowedBy() != Entity.NONE;
        boolean hasWarning = false;
        boolean hasCritical = false;
        int mapType = lobby.mapSettings.getMedium();

        // First line
        if (LobbyUtility.hasYellowWarning(entity)) {
            result.append(UIUtil.fontHTML(uiYellow()));
            result.append(WARNING_SIGN + "</FONT>");
            hasWarning = true;
        }

        // Critical (Red) Warnings
        if ((game.getPlanetaryConditions().whyDoomed(entity, entity.getGame()) != null)
              || (entity.doomedInAtmosphere() && mapType == MapSettings.MEDIUM_ATMOSPHERE)
              || (entity.doomedOnGround() && mapType == MapSettings.MEDIUM_GROUND)
              || (entity.doomedInSpace() && mapType == MapSettings.MEDIUM_SPACE)
              || (!entity.isDesignValid())) {
            result.append(UIUtil.fontHTML(GUIP.getWarningColor()));
            result.append(WARNING_SIGN + "</FONT>");
            hasCritical = true;
        }

        // Unit Name
        if (hasCritical) {
            result.append(UIUtil.fontHTML(GUIP.getWarningColor()));
        } else if (hasWarning) {
            result.append(UIUtil.fontHTML(uiYellow()));
        } else {
            result.append(fontHTML());
        }
        result.append("<B>").append(entity.getShortNameRaw()).append("</B></FONT>");

        // ID
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            result.append(UIUtil.fontHTML(uiGray()));
            result.append(" [ID: ").append(entity.getId()).append("]</FONT>");
        }
        if (!forceView) {
            result.append("<BR>");
        }

        // Tonnage
        result.append(fontHTML());
        if (forceView) {
            result.append(MekTableModel.DOT_SPACER);
        }
        NumberFormat formatter = NumberFormat.getNumberInstance(MegaMek.getMMOptions().getLocale());
        result.append(formatter.format(entity.getWeight()));
        result.append(Messages.getString("ChatLounge.Tons"));
        result.append("</FONT>");

        // Alpha Strike Unit Role
        if (!entity.isUnitGroup()) {
            result.append(MekTableModel.DOT_SPACER);
            result.append(entity.getRole().toString());
        }

        // Invalid Design
        if (!forceView) {
            if (!entity.isDesignValid()) {
                result.append(MekTableModel.DOT_SPACER);
                result.append(UIUtil.fontHTML(GUIP.getWarningColor()));
                result.append("\u26D4 ").append(Messages.getString("ChatLounge.invalidDesign"));
                result.append("</FONT>");
            }
        }

        // Shutdown
        if (!forceView) {
            if (entity.isShutDown()) {
                result.append(MekTableModel.DOT_SPACER);
                result.append(UIUtil.fontHTML(GUIP.getWarningColor()));
                result.append(WARNING_SIGN).append(Messages.getString("ChatLounge.shutdown"));
                result.append("</FONT>");
            }
        }

        // ECM
        if (entity.hasActiveECM()) {
            result.append(MekTableModel.DOT_SPACER).append(UIUtil.fontHTML(uiC3Color()));
            result.append(ECM_SIGN + " ");
            result.append(Messages.getString("BoardView1.ecmSource"));
            result.append("</FONT>");
        }

        // Quirk Count
        int quirkCount = entity.countQuirks() + entity.countWeaponQuirks();
        if (quirkCount > 0) {
            result.append(MekTableModel.DOT_SPACER);
            result.append(UIUtil.fontHTML(uiQuirksColor()));
            result.append(QUIRKS_SIGN);
            result.append(Messages.getString("ChatLounge.Quirks"));
            result.append("</FONT>");
        }

        // Pilot
        if (forceView) {
            Crew pilot = entity.getCrew();
            result.append(fontHTML());
            result.append(MekTableModel.DOT_SPACER);

            if (pilot.getSlotCount() > 1 || entity instanceof FighterSquadron) {
                result.append("<I>").append(Messages.getString("ChatLounge.multipleCrew")).append("</I>");
            } else if ((pilot.getNickname(0) != null) && !pilot.getNickname(0).isEmpty()) {
                result.append(UIUtil.fontHTML(uiNickColor())).append("<B>'");
                result.append(pilot.getNickname(0).toUpperCase()).append("'</B></FONT>");
                if (!pilot.getStatusDesc(0).isEmpty()) {
                    result.append(" (").append(pilot.getStatusDesc(0)).append(")");
                }
            } else {
                result.append(pilot.getDesc(0));
            }

            final boolean rpgSkills = options.booleanOption(OptionsConstants.RPG_RPG_GUNNERY);
            result.append(" (").append(pilot.getSkillsAsString(rpgSkills)).append(")");
            if (pilot.countOptions() > 0) {
                result.append(MekTableModel.DOT_SPACER).append(UIUtil.fontHTML(uiQuirksColor()));
                result.append(Messages.getString("ChatLounge.abilities"));
                result.append("</FONT>");
            }

            // Owner
            if (!localPlayer.equals(owner)) {
                result.append(MekTableModel.DOT_SPACER);
                result.append(UIUtil.fontHTML(owner.getColour().getColour()));
                result.append("\u2691 ");
                result.append(entity.getOwner().getName()).append("</FONT>");
            }

            // Info sign (i)
            result.append(UIUtil.fontHTML(uiGreen()));
            result.append("&nbsp;  \u24D8</FONT>");
        }

        // SECOND OR THIRD LINE in Force View / Table
        result.append("<BR>");

        // Controls the separator dot character
        boolean firstEntry = true;

        // Start Position
        int sp = entity.getStartingPos(true);
        int spe = entity.getStartingPos(false);
        if ((!entity.isOffBoard())
              && (sp >= 0)) {
            firstEntry = dotSpacer(result, firstEntry);
            if (spe != Board.START_NONE) {
                result.append(UIUtil.fontHTML(uiLightGreen()));
            }
            String msg_start = Messages.getString("ChatLounge.Start");
            result.append(" ").append(msg_start).append(": ");

            if (sp <= Board.NUM_ZONES) {
                result.append(IStartingPositions.START_LOCATION_NAMES[sp]);
            } else {
                result.append(" Zone ").append(Board.decodeCustomDeploymentZoneID(sp));
            }

            if (sp == 0) {
                int NWx = entity.getStartingAnyNWx() + 1;
                int NWy = entity.getStartingAnyNWy() + 1;
                int SEx = entity.getStartingAnySEx() + 1;
                int SEy = entity.getStartingAnySEy() + 1;
                int hexes = (1 + SEx - NWx) * (1 + SEy - NWy);
                if ((NWx + NWy + SEx + SEy) > 0) {
                    result.append(" (")
                          .append(NWx)
                          .append(", ")
                          .append(NWy)
                          .append(")-(")
                          .append(SEx)
                          .append(", ")
                          .append(SEy)
                          .append(") (")
                          .append(hexes)
                          .append(")");
                }
            }
            int so = entity.getStartingOffset(true);
            int sw = entity.getStartingWidth(true);
            if ((so != 0) || (sw != 3)) {
                result.append(", ").append(so);
                result.append(", ").append(sw);
            }
            if (spe != Board.START_NONE) {
                result.append("</FONT>");
            }
        }

        // Invalid Design
        if (forceView) {
            if (!entity.isDesignValid()) {
                firstEntry = dotSpacer(result, firstEntry);
                result.append(UIUtil.fontHTML(GUIP.getWarningColor()));
                result.append("\u26D4 </FONT>").append(Messages.getString("ChatLounge.invalidDesign"));
            }
        }

        // C3 ...
        if (entity.hasC3i()) {
            firstEntry = dotSpacer(result, firstEntry);
            result.append(UIUtil.fontHTML(uiC3Color()));
            if (entity.calculateFreeC3Nodes() >= 5) {
                result.append("C3i" + UNCONNECTED_SIGN);
            } else {
                result.append("C3i" + CONNECTED_SIGN).append(entity.getC3NetId());
                if (entity.calculateFreeC3Nodes() > 0) {
                    result.append(Messages.getString("ChatLounge.C3iNodes", entity.calculateFreeC3Nodes()));
                }
            }
            result.append("</FONT>");
        }

        if (entity.hasNavalC3()) {
            firstEntry = dotSpacer(result, firstEntry);
            result.append(UIUtil.fontHTML(uiC3Color()));
            if (entity.calculateFreeC3Nodes() >= 5) {
                result.append("NC3" + UNCONNECTED_SIGN);
            } else {
                result.append("NC3" + CONNECTED_SIGN).append(entity.getC3NetId());
                if (entity.calculateFreeC3Nodes() > 0) {
                    result.append(Messages.getString("ChatLounge.C3iNodes", entity.calculateFreeC3Nodes()));
                }
            }
            result.append("</FONT>");
        }

        if (entity.hasNovaCEWS()) {
            firstEntry = dotSpacer(result, firstEntry);
            result.append(UIUtil.fontHTML(uiC3Color()));
            if (entity.calculateFreeC3Nodes() >= 2) {
                result.append("Nova CEWS").append(UNCONNECTED_SIGN);
            } else {
                result.append("Nova CEWS").append(CONNECTED_SIGN).append(entity.getC3NetId());
            }
            result.append("</FONT>");
        }

        if (entity.hasC3()) {
            if (entity.getC3Master() == null) {
                if (entity.hasC3S()) {
                    firstEntry = dotSpacer(result, firstEntry);
                    result.append(UIUtil.fontHTML(uiC3Color()))
                          .append(Messages.getString("ChatLounge.C3S"))
                          .append(UNCONNECTED_SIGN);
                    result.append("</FONT>");
                }

                if (entity.hasC3M()) {
                    firstEntry = dotSpacer(result, firstEntry);
                    result.append(UIUtil.fontHTML(uiC3Color())).append(Messages.getString("ChatLounge.C3Master"));
                    int freeS = entity.calculateFreeC3Nodes();
                    if (freeS == 0) {
                        result.append(" (full)");
                    } else {
                        result.append(Messages.getString("ChatLounge.C3SNodes", entity.calculateFreeC3Nodes()));
                    }
                    result.append("</FONT>");
                }
            } else if (entity.C3MasterIs(entity)) {
                firstEntry = dotSpacer(result, firstEntry);
                result.append(UIUtil.fontHTML(uiC3Color())).append(Messages.getString("ChatLounge.C3CC"));
                if (entity.hasC3MM()) {
                    String msgFreeC3MNodes = Messages.getString("ChatLounge.FreeC3MNodes");
                    result.append(MessageFormat.format(" " + msgFreeC3MNodes,
                          entity.calculateFreeC3MNodes(), entity.calculateFreeC3Nodes()));
                } else {
                    result.append(getString("ChatLounge.C3MNodes", entity.calculateFreeC3MNodes()));
                }
                result.append("</FONT>");
            } else {
                firstEntry = dotSpacer(result, firstEntry);
                result.append(UIUtil.fontHTML(uiC3Color()));
                if (entity.hasC3S()) {
                    result.append(getString("ChatLounge.C3S")).append(CONNECTED_SIGN);
                } else {
                    result.append(getString("ChatLounge.C3Master"));
                    int freeS = entity.calculateFreeC3Nodes();
                    if (freeS == 0) {
                        result.append(getString("ChatLounge.C3full"));
                    } else {
                        result.append(getString("ChatLounge.C3SNodes", entity.calculateFreeC3Nodes()));
                    }
                    result.append(CONNECTED_SIGN + "(CC) ");
                }
                result.append(entity.getC3Master().getChassis());
                result.append("</FONT>");
            }
        }

        // Loaded onto transport
        result.append(UIUtil.fontHTML(uiGreen()));
        if (isCarried) {
            firstEntry = dotSpacer(result, firstEntry);
            Entity loader = entity.getGame().getEntity(entity.getTransportId());

            if (loader != null) {
                result.append(UIUtil.fontHTML(uiGreen())).append(LOADED_SIGN);
                result.append("<I> ")
                      .append(Messages.getString("ChatLounge.aboard"))
                      .append(" ")
                      .append(loader.getChassis());
            }

            if (PreferenceManager.getClientPreferences().getShowUnitId()) {
                result.append(" [").append(entity.getTransportId()).append("]");
            }
            result.append("</I></FONT>");

        } else if (isTowed) { // Towed
            firstEntry = dotSpacer(result, firstEntry);
            Entity tractor = entity.getGame().getEntity(entity.getTowedBy());

            if (tractor != null) {
                result.append(UIUtil.fontHTML(uiGreen())).append(LOADED_SIGN);
                result.append("<I> ")
                      .append(Messages.getString("ChatLounge.towedBy"))
                      .append(" ")
                      .append(tractor.getChassis());
            }

            if (PreferenceManager.getClientPreferences().getShowUnitId()) {
                result.append(" [").append(entity.getTransportId()).append("]");
            }
            result.append("</I></FONT>");

        } else { // Hide deployment info when a unit is carried or towed
            if (entity.isHidden() && mapType == MapSettings.MEDIUM_GROUND) {
                firstEntry = dotSpacer(result, firstEntry);
                result.append(getString("ChatLounge.compact.hidden"));
            }

            if (entity.isHullDown()) {
                firstEntry = dotSpacer(result, firstEntry);
                result.append(getString("ChatLounge.hulldown"));
            }

            if (entity.isProne()) {
                firstEntry = dotSpacer(result, firstEntry);
                result.append(getString("ChatLounge.prone"));
            }
        }

        if (entity.isOffBoard()) {
            firstEntry = dotSpacer(result, firstEntry);
            result.append(getString("ChatLounge.deploysOffBoard"));
            result.append(",  ").append(entity.getOffBoardDirection());
            result.append(", ").append(entity.getOffBoardDistance());
        }

        if (entity.getDeployRound() > 0) {
            firstEntry = dotSpacer(result, firstEntry);
            result.append(getString("ChatLounge.deploysAfterRound", entity.getDeployRound()));
        }
        result.append("</FONT>");

        // Starting heat
        if (entity.getHeat() != 0 && entity.tracksHeat()) {
            firstEntry = dotSpacer(result, firstEntry);
            result.append(UIUtil.fontHTML(uiLightRed()));
            result.append(entity.getHeat()).append(" Heat").append("</FONT>");
        }

        // Partial Repairs
        int partRepCount = entity.countPartialRepairs();
        if ((partRepCount > 0)) {
            firstEntry = dotSpacer(result, firstEntry);
            result.append(UIUtil.fontHTML(uiLightRed()));
            result.append(Messages.getString("ChatLounge.PartialRepairs"));
            result.append("</FONT>");
        }

        // Starting values for Altitude / Velocity / Elevation
        if (!isCarried) {
            if (entity.isAero()) {
                IAero aero = (IAero) entity;
                firstEntry = dotSpacer(result, firstEntry);
                result.append(UIUtil.fontHTML(uiGreen())).append("<I>");
                result.append(Messages.getString("ChatLounge.compact.velocity")).append(": ");
                result.append(aero.getCurrentVelocity());
                if (mapType != MapSettings.MEDIUM_SPACE) {
                    result.append(", ").append(Messages.getString("ChatLounge.compact.altitude")).append(": ");
                    result.append(aero.getAltitude());
                }
                if (options.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_FUEL_CONSUMPTION)) {
                    result.append(", ").append(Messages.getString("ChatLounge.compact.fuel")).append(": ");
                    result.append(aero.getCurrentFuel());
                }
                result.append("</I></FONT>");
            } else if ((entity.getElevation() != 0) || (entity instanceof VTOL)) {
                firstEntry = dotSpacer(result, firstEntry);
                result.append(UIUtil.fontHTML(uiGreen())).append("<I>");
                result.append(Messages.getString("ChatLounge.compact.elevation")).append(": ");
                result.append(entity.getElevation()).append("</I></FONT>");
            }
        }

        // Auto Eject
        String msgAutoEjectDisabled = Messages.getString("ChatLounge.AutoEjectDisabled");
        if (entity instanceof Mek mek) {
            if ((mek.hasEjectSeat()) && (!mek.isAutoEject())) {
                firstEntry = dotSpacer(result, firstEntry);
                result.append(UIUtil.fontHTML(uiYellow()));
                result.append(WARNING_SIGN + "\u23CF<I>");
                result.append(msgAutoEjectDisabled);
                result.append("</I></FONT>");
            }
        }
        if ((entity instanceof Aero aero)
              && (!(entity instanceof Jumpship))
              && (!(entity instanceof SmallCraft))) {
            if ((aero.hasEjectSeat())
                  && (!aero.isAutoEject())) {
                firstEntry = dotSpacer(result, firstEntry);
                result.append(UIUtil.fontHTML(uiYellow()));
                result.append(WARNING_SIGN + "\u23CF<I>");
                result.append(msgAutoEjectDisabled);
                result.append("</I></FONT>");
            }
        }

        return result.toString();
    }

    /**
     * Creates and returns the display content of the C3-MekTree cell for the given entity and for the compact display
     * mode. Assumes that no enemy or blind-drop-hidden units are provided.
     */
    static String formatUnitCompact(Entity entity, ChatLounge lobby, boolean forceView) {
        Client client = lobby.getClientGUI().getClient();
        Game game = client.getGame();
        GameOptions options = game.getOptions();
        Player localPlayer = client.getLocalPlayer();
        Player owner = entity.getOwner();
        boolean localGM = localPlayer.isGameMaster();
        boolean hideEntity = !localGM && owner.isEnemyOf(localPlayer)
              && options.booleanOption(OptionsConstants.BASE_BLIND_DROP);
        if (hideEntity) {
            String value = "<HTML><NOBR>&nbsp;&nbsp;";
            if (PreferenceManager.getClientPreferences().getShowUnitId()) {
                value += UIUtil.fontHTML(uiGray());
                value += MessageFormat.format("[{0}] </FONT>", entity.getId());
            }
            String uType;
            if (entity instanceof Infantry) {
                uType = Messages.getString("ChatLounge.0");
            } else if (entity instanceof ProtoMek) {
                uType = Messages.getString("ChatLounge.1");
            } else if (entity.isBuildingEntityOrGunEmplacement()) {
                uType = Messages.getString("ChatLounge.2");
            } else if (entity.isSupportVehicle()) {
                uType = entity.getWeightClassName();
            } else if (entity.isFighter()) {
                uType = entity.getWeightClassName() + Messages.getString("ChatLounge.4");
            } else if (entity instanceof Mek) {
                uType = entity.getWeightClassName() + Messages.getString("ChatLounge.3");
            } else if (entity instanceof Tank) {
                uType = entity.getWeightClassName() + Messages.getString("ChatLounge.6");
            } else {
                uType = entity.getWeightClassName();
            }
            return value + fontHTML() + MekTableModel.DOT_SPACER + uType + MekTableModel.DOT_SPACER;
        }

        StringBuilder result = new StringBuilder("<HTML><NOBR>&nbsp;&nbsp;" + fontHTML());
        boolean isCarried = entity.getTransportId() != Entity.NONE;
        int mapType = lobby.mapSettings.getMedium();

        Color color = GUIP.getEnemyUnitColor();
        if (owner.getId() == localPlayer.getId()) {
            color = GUIP.getMyUnitColor();
        } else if (!localPlayer.isEnemyOf(owner)) {
            color = GUIP.getAllyUnitColor();
        }
        color = addGray(color, 128).brighter();

        if (entity.getForceId() == Force.NO_FORCE && forceView) {
            result.append(UIUtil.fontHTML(color));
            result.append("\u25AD </FONT>");
        }

        // Signs before the unit name
        // ID
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            result.append(UIUtil.fontHTML(uiGray()));
            result.append(MessageFormat.format("[{0}] </FONT>", entity.getId()));
        }

        // Critical (Red) Warnings
        if ((game.getPlanetaryConditions().whyDoomed(entity, entity.getGame()) != null)
              || (entity.doomedInAtmosphere() && mapType == MapSettings.MEDIUM_ATMOSPHERE)
              || (entity.doomedOnGround() && mapType == MapSettings.MEDIUM_GROUND)
              || (entity.doomedInSpace() && mapType == MapSettings.MEDIUM_SPACE)
              || (!entity.isDesignValid())) {
            result.append(UIUtil.fontHTML(GUIP.getWarningColor()));
            result.append(WARNING_SIGN + "</FONT>");
        }

        // General (Yellow) Warnings
        if (LobbyUtility.hasYellowWarning(entity)) {
            result.append(UIUtil.fontHTML(uiYellow()));
            result.append(WARNING_SIGN + "</FONT>");
        }

        // Loaded unit
        if (isCarried) {
            result.append(UIUtil.fontHTML(uiGreen())).append(LOADED_SIGN).append("</FONT>");
        }

        // Unit name
        // Gray out if the unit is a fighter in a squadron
        if (entity.isPartOfFighterSquadron()) {
            result.append(UIUtil.fontHTML(uiGray()));
            result.append(entity.getShortNameRaw()).append("</FONT>");
        } else {
            result.append(entity.getShortNameRaw());
        }

        // Pilot
        if (forceView) {
            Crew pilot = entity.getCrew();
            result.append(fontHTML());
            result.append(MekTableModel.DOT_SPACER);

            if (pilot.getSlotCount() > 1 || entity instanceof FighterSquadron) {
                result.append("<I>").append(Messages.getString("ChatLounge.multipleCrew")).append("</I>");
            } else if ((pilot.getNickname(0) != null) && !pilot.getNickname(0).isEmpty()) {
                result.append(UIUtil.fontHTML(uiNickColor())).append("<B>'");
                result.append(pilot.getNickname(0).toUpperCase()).append("'</B></FONT>");
                if (!pilot.getStatusDesc(0).isEmpty()) {
                    result.append(" (").append(pilot.getStatusDesc(0)).append(")");
                }
            } else {
                result.append(pilot.getDesc(0));
            }

            final boolean rpgSkills = options.booleanOption(OptionsConstants.RPG_RPG_GUNNERY);
            result.append(" (").append(pilot.getSkillsAsString(rpgSkills)).append(")");
            if (pilot.countOptions() > 0) {
                result.append(MekTableModel.DOT_SPACER).append(UIUtil.fontHTML(uiQuirksColor()));
                result.append(Messages.getString("ChatLounge.abilities"));
            }

            // Owner
            if (!localPlayer.equals(owner)) {
                result.append(MekTableModel.DOT_SPACER);
                result.append(UIUtil.fontHTML(owner.getColour().getColour()));
                result.append("\u2691 ");
                result.append(entity.getOwner().getName()).append("</FONT>");
            }
        }

        // Invalid unit design
        if (!entity.isDesignValid()) {
            result.append(MekTableModel.DOT_SPACER);
            result.append(UIUtil.fontHTML(GUIP.getWarningColor()));
            result.append("\u26D4 </FONT>").append(Messages.getString("ChatLounge.invalidDesign"));
        }

        // ECM
        if (entity.hasActiveECM()) {
            result.append(fontHTML(uiC3Color()));
            result.append(ECM_SIGN);
            result.append("</FONT>");
        }

        // Quirk Count
        int quirkCount = entity.countQuirks() + entity.countWeaponQuirks();
        if (quirkCount > 0) {
            result.append(fontHTML(uiQuirksColor()));
            result.append(QUIRKS_SIGN);
            result.append("</FONT>");
        }

        // C3 ...
        if (entity.hasC3i() || entity.hasNavalC3() || entity.hasNovaCEWS()) {
            result.append(MekTableModel.DOT_SPACER).append(UIUtil.fontHTML(uiC3Color()));
            String msg_c3i = Messages.getString("ChatLounge.C3i");
            String msg_nc3 = Messages.getString("ChatLounge.NC3");

            String c3Name;
            int maxNodes;
            if (entity.hasC3i()) {
                c3Name = msg_c3i;
                maxNodes = 5;
            } else if (entity.hasNavalC3()) {
                c3Name = msg_nc3;
                maxNodes = 5;
            } else { // Nova CEWS
                c3Name = "Nova CEWS";
                maxNodes = 2;
            }

            if (entity.calculateFreeC3Nodes() >= maxNodes) {
                result.append(c3Name).append(UNCONNECTED_SIGN);
            } else {
                result.append(c3Name).append(CONNECTED_SIGN).append(entity.getC3NetId());
            }
            result.append("</FONT>");
        }

        if (entity.hasC3()) {
            String msgC3SAbbreviation = Messages.getString("ChatLounge.C3SAbrv");
            String msg_c3m = Messages.getString("ChatLounge.C3M");
            String msg_c3mcc = Messages.getString("ChatLounge.C3MCC");

            result.append(MekTableModel.DOT_SPACER).append(UIUtil.fontHTML(uiC3Color()));
            if (entity.getC3Master() == null) {
                if (entity.hasC3S()) {
                    result.append(msgC3SAbbreviation).append(UNCONNECTED_SIGN);
                }
                if (entity.hasC3M()) {
                    result.append(msg_c3m);
                }
            } else if (entity.C3MasterIs(entity)) {
                result.append(msg_c3mcc);
            } else {
                if (entity.hasC3S()) {
                    result.append(msgC3SAbbreviation).append(CONNECTED_SIGN);
                } else {
                    result.append(msg_c3m).append(CONNECTED_SIGN);
                }
                result.append(entity.getC3Master().getChassis());
            }
            result.append("</FONT>");
        }

        // Loaded onto another unit
        if (isCarried) {
            Entity loader = entity.getGame().getEntity(entity.getTransportId());

            if (loader != null) {
                result.append(MekTableModel.DOT_SPACER).append(UIUtil.fontHTML(uiGreen())).append("<I>(");
                result.append(loader.getChassis());
                if (PreferenceManager.getClientPreferences().getShowUnitId()) {
                    result.append(" [").append(entity.getTransportId()).append("]");
                }
                result.append(")</I></FONT>");
            }
        }

        // Deployment info, doesn't matter when the unit is carried
        if (!isCarried) {
            if (entity.isHidden()) {
                result.append(MekTableModel.DOT_SPACER).append(UIUtil.fontHTML(uiGreen())).append("<I>");
                result.append(Messages.getString("ChatLounge.compact.hidden")).append("</I></FONT>");
            }

            if (entity.isHullDown()) {
                result.append(MekTableModel.DOT_SPACER).append(UIUtil.fontHTML(uiGreen())).append("<I>");
                result.append(Messages.getString("ChatLounge.compact.hulldown")).append("</I></FONT>");
            }

            if (entity.isProne()) {
                result.append(MekTableModel.DOT_SPACER).append(UIUtil.fontHTML(uiGreen())).append("<I>");
                result.append(Messages.getString("ChatLounge.compact.prone")).append("</I></FONT>");
            }
        }

        if (entity.countPartialRepairs() > 0) {
            result.append(MekTableModel.DOT_SPACER).append(UIUtil.fontHTML(uiLightRed()));
            result.append("Partial Repairs</FONT>");
        }

        // Offboard deployment
        if (entity.isOffBoard()) {
            result.append(MekTableModel.DOT_SPACER).append(UIUtil.fontHTML(uiGreen())).append("<I>");
            result.append(Messages.getString("ChatLounge.compact.deploysOffBoard")).append("</I></FONT>");
        } else if (entity.getDeployRound() > 0) {
            result.append(MekTableModel.DOT_SPACER).append(UIUtil.fontHTML(uiGreen())).append("<I>");
            result.append(Messages.getString("ChatLounge.compact.deployRound", entity.getDeployRound()));
            if (entity.getStartingPos(false) != Board.START_NONE) {
                result.append(Messages.getString("ChatLounge.compact.deployZone",
                      IStartingPositions.START_LOCATION_NAMES[entity.getStartingPos(false)]));
            }
            result.append("</I></FONT>");
        }

        // Starting values for Altitude / Velocity / Elevation
        if (!isCarried) {
            if (entity.isAero()) {
                IAero aero = (IAero) entity;
                result.append(MekTableModel.DOT_SPACER).append(UIUtil.fontHTML(uiGreen())).append("<I>");
                result.append(Messages.getString("ChatLounge.compact.velocity")).append(": ");
                result.append(aero.getCurrentVelocity());
                if (mapType != MapSettings.MEDIUM_SPACE) {
                    result.append(", ").append(Messages.getString("ChatLounge.compact.altitude")).append(": ");
                    result.append(aero.getAltitude());
                }
                if (options.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_FUEL_CONSUMPTION)) {
                    result.append(", ").append(Messages.getString("ChatLounge.compact.fuel")).append(": ");
                    result.append(aero.getCurrentFuel());
                }
                result.append("</I></FONT>");
            } else if ((entity.getElevation() != 0) || (entity instanceof VTOL)) {
                result.append(MekTableModel.DOT_SPACER).append(UIUtil.fontHTML(uiGreen())).append("<I>");
                result.append(Messages.getString("ChatLounge.compact.elevation")).append(": ");
                result.append(entity.getElevation()).append("</I></FONT>");
            }
        }

        // Starting heat
        if (entity.getHeat() != 0 && entity.tracksHeat()) {
            result.append(MekTableModel.DOT_SPACER).append(UIUtil.fontHTML(uiGreen()));
            result.append("<I>Heat: ").append(entity.getHeat()).append(" </I></FONT>");
        }

        result.append("</FONT>");

        // Info tooltip sign
        if (forceView) {
            result.append(fontHTML(uiGreen()));
            result.append("&nbsp;  \u24D8");
        }

        // Auto Eject
        if (entity instanceof Mek mek) {
            if ((mek.hasEjectSeat()) && (!mek.isAutoEject())) {
                result.append(MekTableModel.DOT_SPACER).append(UIUtil.fontHTML(uiGreen()));
                result.append(UIUtil.fontHTML(uiYellow()));
                result.append(WARNING_SIGN + "\u23CF</FONT>");
            }
        }
        if ((entity instanceof Aero aero)
              && (!(entity instanceof Jumpship))
              && (!(entity instanceof SmallCraft))) {
            if ((aero.hasEjectSeat())
                  && (!aero.isAutoEject())) {
                result.append(MekTableModel.DOT_SPACER).append(UIUtil.fontHTML(uiGreen()));
                result.append(UIUtil.fontHTML(uiYellow()));
                result.append(WARNING_SIGN + "\u23CF</FONT>");
            }
        }

        return LobbyUtility.abbreviateUnitName(result.toString());
    }

    /**
     * Creates and returns the display content of the C3-MekTree cell for the given entity and for the compact display
     * mode. Assumes that no enemy or blind-drop-hidden units are provided.
     */
    static String formatForceCompact(Force force, ChatLounge lobby) {
        return formatForce(force, lobby);
    }

    /**
     * Creates and returns the display content of the C3-MekTree cell for the given entity and for the compact display
     * mode. Assumes that no enemy or blind-drop-hidden units are provided.
     */
    static String formatForceFull(Force force, ChatLounge lobby) {
        return formatForce(force, lobby);
    }

    private static String formatForce(Force force, ChatLounge lobby) {
        Client client = lobby.getClientGUI().getClient();
        Game game = client.getGame();
        Player localPlayer = client.getLocalPlayer();
        int ownerId = game.getForces().getOwnerId(force);
        Player owner = game.getPlayer(ownerId);

        // Get the my / ally / enemy color and desaturate it
        Color color = GUIP.getEnemyUnitColor();
        if (ownerId == localPlayer.getId()) {
            color = GUIP.getMyUnitColor();
        } else if (!localPlayer.isEnemyOf(owner)) {
            color = GUIP.getAllyUnitColor();
        }
        color = addGray(color, 128).brighter();

        StringBuilder result = new StringBuilder("<HTML><NOBR>");
        result.append(fontHTML(color));

        // A top-level / sub force special char
        if (force.isTopLevel()) {
            result.append("\u2327&nbsp;&nbsp; ");
        } else {
            result.append("\u25E5&nbsp;&nbsp; ");
        }

        // Name
        result.append("<B>").append(force.getName()).append("</B></FONT>");

        // ID
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            result.append(fontHTML(uiGray()));
            result.append(" [").append(force.getId()).append("]</FONT>");
        }

        // Display force owner
        if ((ownerId != client.getLocalPlayerNumber()) && (owner != null)) {
            result.append(fontHTML());
            result.append(MekTableModel.DOT_SPACER).append("</FONT>");

            PlayerColour ownerColour = (owner.getColour() == null) ? PlayerColour.FIRE_BRICK : owner.getColour();
            result.append(fontHTML(ownerColour.getColour()));
            result.append("\u2691 ");
            result.append(owner.getName()).append("</FONT>");
        }

        // BV
        List<Entity> fullEntities = ForceAssignable.filterToEntityList(lobby.game().getForces().getFullEntities(force));
        result.append(fontHTML(color));
        result.append(MekTableModel.DOT_SPACER);
        int totalBv = fullEntities.stream().filter(e -> !e.isPartOfFighterSquadron())
              .mapToInt(Entity::calculateBattleValue).sum();
        if (totalBv > 0) {
            String msgBVPlain = Messages.getString("ChatLounge.BVplain");
            result.append(msgBVPlain).append(" ").append(String.format("%,d", totalBv));
            // Unit Type
            long unitTypes = fullEntities.stream().map(e -> Entity.getEntityMajorTypeName(e.getEntityType())).distinct()
                  .count();
            result.append(fontHTML(color));
            result.append(MekTableModel.DOT_SPACER);
            if (unitTypes > 1) {
                String msg_mixed = Messages.getString("ChatLounge.Mixed");
                result.append(" ").append(msg_mixed);
            } else if (unitTypes == 1) {
                Entity entity = CollectionUtil.anyOneElement(fullEntities);
                result.append(UnitType.getTypeName(entity.getUnitType()));
            }
        } else {
            result.append("Empty");
        }
        result.append("</FONT>");

        return result.toString();
    }

    /**
     * Creates and returns the display content of the Pilot column for the given entity and for the compact display
     * mode. When blindDrop is true, the pilot details are not given.
     */
    static String formatPilotCompact(Entity entity, boolean blindDrop, boolean rpgSkills) {
        Crew pilot = entity.getCrew();
        StringBuilder result = new StringBuilder("<HTML><NOBR>");
        result.append(fontHTML());

        if (blindDrop) {
            result.append(Messages.getString("ChatLounge.Unknown"));
            return result.toString();
        }

        if (pilot.getSlotCount() > 1 || entity instanceof FighterSquadron) {
            result.append("<I>").append(Messages.getString("ChatLounge.multipleCrew")).append("</I>");
        } else if ((pilot.getNickname(0) != null) && !pilot.getNickname(0).isEmpty()) {
            result.append(UIUtil.fontHTML(uiNickColor())).append("<B>'");
            result.append(pilot.getNickname(0).toUpperCase()).append("'</B></FONT>");
            if (!pilot.getStatusDesc(0).isEmpty()) {
                result.append(" (").append(pilot.getStatusDesc(0)).append(")");
            }
        } else {
            result.append(pilot.getDesc(0));
        }

        result.append(" (").append(pilot.getSkillsAsString(rpgSkills)).append(")");
        if (pilot.countOptions() > 0) {
            result.append(MekTableModel.DOT_SPACER).append(UIUtil.fontHTML(uiQuirksColor()));
            result.append(Messages.getString("ChatLounge.abilities"));
        }

        result.append("</FONT>");
        return result.toString();
    }

    /**
     * Creates and returns the display content of the Pilot column for the given entity and for the non-compact display
     * mode. When blindDrop is true, the pilot details are not given.
     */
    static String formatPilotFull(Entity entity, boolean blindDrop) {
        StringBuilder result = new StringBuilder("<HTML><NOBR>");

        final Crew crew = entity.getCrew();
        final GameOptions options = entity.getGame().getOptions();
        final boolean rpgSkills = options.booleanOption(OptionsConstants.RPG_RPG_GUNNERY);

        result.append(fontHTML());

        if (blindDrop) {
            result.append("<B>").append(Messages.getString("ChatLounge.Unknown")).append("</B>");
            return result.toString();
        }

        // Uncrewed
        if (entity.isUncrewed()) {
            result.append("<I>").append(Messages.getString("ChatLounge.noCrew")).append("</I>");
            result.append("<BR>");
        } else {
            if (crew.getSlotCount() == 1 && !(entity instanceof FighterSquadron)) { // Single-person crew
                if (crew.isMissing(0)) {
                    result.append("<B>No ").append(crew.getCrewType().getRoleName(0)).append("</B>");
                } else {
                    if ((crew.getNickname(0) != null) && !crew.getNickname(0).isEmpty()) {
                        result.append(fontHTML(uiNickColor()));
                        result.append("<B>'").append(crew.getNickname(0).toUpperCase()).append("'</B></FONT>");
                    } else {
                        result.append("<B>").append(crew.getDesc(0)).append("</B>");
                    }
                }
                result.append("<BR>");
            } else { // Multi-person crew
                result.append("<I>").append(Messages.getString("ChatLounge.multipleCrew")).append("</I>");
                result.append("<BR>");
            }
            result.append(CrewSkillSummaryUtil.getSkillNames(entity)).append(": ");
            result.append("<B>").append(crew.getSkillsAsString(rpgSkills)).append("</B><BR>");

            // Advantages, MD, Edge
            if ((crew.countOptions(LVL3_ADVANTAGES) > 0) || (crew.countOptions(MD_ADVANTAGES) > 0)) {
                result.append(fontHTML(uiQuirksColor()));
                result.append(Messages.getString("ChatLounge.abilities"));
                result.append("</FONT>");
            }
        }
        result.append("</FONT>");
        return result.toString();
    }

    static void formatSpan(StringBuilder current, Color color) {
        current.append("<SPAN style=color:");
        current.append(Integer.toHexString(color.getRGB() & 0xFFFFFF));
        current.append(";>");
    }

    static void formatSpan(StringBuilder current, String hexColor) {
        current.append("<SPAN style=color:");
        current.append(hexColor);
        current.append(";>");
    }

    static void fullIDString(StringBuilder current, int id) {
        formatSpan(current, uiGray());
        current.append(" [ID: ").append(id).append("]</SPAN>");
    }

    static boolean dotSpacer(StringBuilder current, boolean firstElement) {
        if (!firstElement) {
            current.append(MekTableModel.DOT_SPACER);
        }
        return false;
    }
}

/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.forceDisplay;

import static megamek.client.ui.panels.phaseDisplay.lobby.MekTableModel.DOT_SPACER;
import static megamek.client.ui.util.UIUtil.CONNECTED_SIGN;
import static megamek.client.ui.util.UIUtil.ECM_SIGN;
import static megamek.client.ui.util.UIUtil.QUIRKS_SIGN;
import static megamek.client.ui.util.UIUtil.UNCONNECTED_SIGN;
import static megamek.client.ui.util.UIUtil.fontHTML;
import static megamek.client.ui.util.UIUtil.uiGray;

import java.awt.Color;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.List;

import megamek.MegaMek;
import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.tooltip.UnitToolTip;
import megamek.client.ui.util.UIUtil;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.equipment.GunEmplacement;
import megamek.common.force.Force;
import megamek.common.game.Game;
import megamek.common.interfaces.ForceAssignable;
import megamek.common.interfaces.IStartingPositions;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.*;
import megamek.common.util.CollectionUtil;

class ForceDisplayMekCellFormatter {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private ForceDisplayMekCellFormatter() {
    }

    /**
     * Creates and returns the display content of the C3-MekTree cell for the given entity and for the compact display
     * mode. Assumes that no enemy or blind-drop-hidden units are provided.
     */
    static String formatUnitCompact(Entity entity, Client client, int row) {
        Game game = client.getGame();
        GameOptions options = game.getOptions();
        Player localPlayer = client.getLocalPlayer();
        Player owner = entity.getOwner();
        boolean showAsUnknown = owner.isEnemyOf(localPlayer)
              && !EntityVisibilityUtils.detectedOrHasVisual(localPlayer, client.getGame(), entity);

        if (entity.isSensorReturn(localPlayer)) {
            String value = "";
            String uType;

            if (entity instanceof Infantry) {
                uType = Messages.getString("ChatLounge.0");
            } else if (entity instanceof ProtoMek) {
                uType = Messages.getString("ChatLounge.1");
            } else if (entity instanceof GunEmplacement) {
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

            uType = DOT_SPACER + uType + DOT_SPACER;
            value += uType;
            return UnitToolTip.wrapWithHTML(value);
        } else if (showAsUnknown) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        boolean isCarried = entity.getTransportId() != Entity.NONE;

        Color color = GUIP.getEnemyUnitColor();
        if (owner.getId() == localPlayer.getId()) {
            color = GUIP.getMyUnitColor();
        } else if (!localPlayer.isEnemyOf(owner)) {
            color = GUIP.getAllyUnitColor();
        }

        if (entity.getForceId() == Force.NO_FORCE) {
            result.append(formatCell(UIUtil.fontHTML(color) + "\u25AD" + "</FONT>", 10));
        }

        // ID
        if (GUIP.getForceDisplayBtnID()) {
            String id = MessageFormat.format("[{0}] ", entity.getId());
            result.append(formatCell(
                  UIUtil.fontHTML(GUIP.getUnitToolTipHighlightColor()) +
                        id +
                        "</FONT>", 20));
        }

        // Done
        if (!game.getPhase().isReport()) {
            String done;
            if (!entity.isDone()) {
                done = "\u2610 ";
            } else {
                done = "\u2611 ";
            }
            result.append(formatCell(UIUtil.fontHTML(color) + done + "</FONT>", 25));
        }

        // Unit name
        // Gray out if the unit is a fighter in a squadron
        if (entity.isPartOfFighterSquadron()) {
            result.append(formatCell(UIUtil.fontHTML(GUIP.getUnitToolTipHighlightColor()) +
                  entity.getShortNameRaw() +
                  "</FONT>", 180, entity.getOwner().getColour().getColour()));
        } else {
            result.append(formatCell(entity.getShortNameRaw(), 180, entity.getOwner().getColour().getColour()));
        }

        // Pilot
        Crew pilot = entity.getCrew();
        if (GUIP.getForceDisplayBtnPilot()) {
            if (pilot.getSlotCount() > 1 || entity instanceof FighterSquadron) {
                result.append(formatCell(
                      "<I>" +
                            Messages.getString("ChatLounge.multipleCrew") +
                            "</I>", 150));
            } else {
                String txt = "";
                if ((pilot.getNickname(0) != null) && !pilot.getNickname(0).isEmpty()) {
                    txt += UIUtil.fontHTML(GUIP.getUnitToolTipHighlightColor()) +
                          "<B>'" +
                          pilot.getNickname(0).toUpperCase() +
                          "'</B></FONT>";
                } else {
                    txt += pilot.getName(0).toUpperCase();
                }
                // Pilot Status
                if (!pilot.getStatusDesc(0).isEmpty()) {
                    txt += "<br><font color='" +
                          UIUtil.hexColor(GUIP.getCautionColor()) +
                          "'>" +
                          pilot.getStatusDesc(0) +
                          "</font>";
                }
                result.append(formatCell(txt, 150));
            }

            final boolean rpgSkills = options.booleanOption(OptionsConstants.RPG_RPG_GUNNERY);
            result.append(formatCell("(" + pilot.getSkillsAsString(rpgSkills) + ")", 50));
        }

        // Movement Points MP
        if (GUIP.getForceDisplayBtnMP()) {
            if (entity.getJumpMP() != 0) {
                result.append(formatCell("MP: " +
                            entity.getWalkMP() +
                            "/" +
                            entity.getRunMP() + "/" + entity.getJumpMP(),
                      90));
            } else {
                result.append(formatCell("MP: " +
                            entity.getWalkMP() +
                            "/" +
                            entity.getRunMP(),
                      90));
            }
        }

        // Heat
        if (GUIP.getForceDisplayBtnHeat()) {
            if (entity.getHeatCapacity() != 999) { // if unit is not a vehicle (999 heat sinks)
                result.append(formatCell("H: <font color='" +
                      UIUtil.hexColor(GUIP.getColorForHeat(entity.getHeat())) +
                      "'>" +
                      entity.getHeat() +
                      "/" +
                      entity.getHeatCapacity() +
                      "</font>", 70));
            } else {
                result.append(formatCell("-", 70));
            }

        }

        // Weapons
        if (GUIP.getForceDisplayBtnWeapons()) {
            result.append(formatCell(UnitToolTip.getWeaponList(entity).toString(), 320));
        }

        // Damage Description
        if (GUIP.getForceDisplayBtnDamageDesc()) {
            result.append(formatCell(UnitToolTip.getDamageLevelDesc(entity, true), 110));
        }

        // Damage Values - Armor / Internal
        if (GUIP.getForceDisplayBtnArmor()) {
            Color clr = GUIP.getUnitToolTipFGColor();
            if ((double) entity.getTotalArmor() / entity.getTotalOArmor() <= 0.5) {
                clr = GUIP.getCautionColor();
            } else if ((double) entity.getTotalArmor() / entity.getTotalOArmor() <= 0.1) {
                clr = GUIP.getWarningColor();
            }
            result.append(formatCell("A: <font color='" + UIUtil.hexColor(clr) + "'>" +
                  entity.getTotalArmor() +
                  "/" + entity.getTotalOArmor() + "</font>", 90));

            clr = GUIP.getUnitToolTipFGColor();

            if ((double) entity.getTotalInternal() / entity.getTotalOInternal() <= 0.5) {
                clr = GUIP.getCautionColor();
            } else if ((double) entity.getTotalInternal() / entity.getTotalOInternal() <= 0.1) {
                clr = GUIP.getWarningColor();
            }
            result.append(formatCell("I: <font color='" + UIUtil.hexColor(clr) + "'>" +
                  entity.getTotalInternal() +
                  "/" + entity.getTotalOInternal() + "</font>", 90));
        }

        // Tonnage
        if (GUIP.getForceDisplayBtnTonnage()) {
            NumberFormat formatter = NumberFormat.getNumberInstance(MegaMek.getMMOptions().getLocale());
            String tonnage = formatter.format(entity.getWeight());
            tonnage += "t";
            result.append(formatCell(tonnage, 40));
        }

        // Alpha Strike Unit Role
        if (GUIP.getForceDisplayBtnRole()) {
            if (!entity.isUnitGroup()) {
                result.append(formatCell(entity.getRole().toString(), 100));
            }
        }

        if (GUIP.getForceDisplayBtnPilot()) {
            if (pilot.countOptions() > 0) {
                String quirks = Messages.getString("ChatLounge.abilities");
                result.append(formatCell(UIUtil.fontHTML(GUIP.getUnitToolTipQuirkColor()) + quirks + "</FONT>", 50));
            } else {
                result.append(formatCell("-", 50));
            }
        }

        if (GUIP.getForceDisplayBtnECM()) {
            // ECM
            if (entity.hasActiveECM()) {
                result.append(formatCell(fontHTML(GUIP.getUnitToolTipHighlightColor()) + ECM_SIGN + "</FONT>", 20));
            } else {
                result.append(formatCell("-", 20));
            }
        }

        // Quirk Count
        if (GUIP.getForceDisplayBtnQuirks()) {
            int quirkCount = entity.countQuirks() + entity.countWeaponQuirks();
            if (quirkCount > 0) {
                result.append(formatCell(fontHTML(GUIP.getUnitToolTipHighlightColor()) + QUIRKS_SIGN + "</FONT>", 100));
            } else {
                result.append(formatCell("-", 100));
            }
        }

        // C3 ...
        if (GUIP.getForceDisplayBtnC3()) {
            if (entity.hasC3i() || entity.hasNavalC3() || entity.hasNovaCEWS()) {
                String msg_c3i = Messages.getString("ChatLounge.C3i");
                String msg_nc3 = Messages.getString("ChatLounge.NC3");
                String msg_nova = Messages.getString("BoardView1.Tooltip.NovaCEWS");

                String c3Name;
                if (entity.hasC3i()) {
                    c3Name = msg_c3i;
                } else if (entity.hasNovaCEWS()) {
                    c3Name = msg_nova;
                } else {  // hasNavalC3()
                    c3Name = msg_nc3;
                }

                if (entity.calculateFreeC3Nodes() >= 5) {
                    c3Name += UNCONNECTED_SIGN;
                } else {
                    c3Name += CONNECTED_SIGN + entity.getC3NetId();
                }
                result.append(formatCell(UIUtil.fontHTML(GUIP.getUnitToolTipHighlightColor()) + c3Name + "</FONT>",
                      70));
            } else if (entity.hasC3()) {
                String msgC3SAbbreviation = Messages.getString("ChatLounge.C3SAbrv");
                String msg_c3m = Messages.getString("ChatLounge.C3M");
                String msg_c3mcc = Messages.getString("ChatLounge.C3MCC");
                String c3 = "";

                if (entity.getC3Master() == null) {
                    if (entity.hasC3S()) {
                        c3 = msgC3SAbbreviation + UNCONNECTED_SIGN;
                    }
                    if (entity.hasC3M()) {
                        c3 = msg_c3m;
                    }
                } else if (entity.C3MasterIs(entity)) {
                    result.append(msg_c3mcc);
                } else {
                    if (entity.hasC3S()) {
                        c3 = msgC3SAbbreviation + CONNECTED_SIGN;
                    } else {
                        c3 = msg_c3m + CONNECTED_SIGN;
                    }

                    c3 += entity.getC3Master().getChassis();
                }

                result.append(formatCell(UIUtil.fontHTML(GUIP.getUnitToolTipHighlightColor()) + c3 + "</FONT>", 70));
            } else {
                result.append(formatCell("-", 70));
            }
        }

        if (GUIP.getForceDisplayBtnMisc()) {
            // Loaded onto another unit
            if (isCarried) {
                Entity loader = entity.getGame().getEntity(entity.getTransportId());

                if (loader != null) {
                    result.append("&nbsp;&nbsp;&nbsp;&nbsp;");
                    String carried = "(" + loader.getChassis() + " [" + entity.getTransportId() + "])";
                    carried = "<I>" + carried + "</I>";
                    result.append(UIUtil.fontHTML(GUIP.getUnitToolTipHighlightColor()))
                          .append(carried)
                          .append("</FONT>");
                }
            }

            if (entity.countPartialRepairs() > 0) {
                result.append("&nbsp;&nbsp;&nbsp;&nbsp;");
                result.append(UIUtil.fontHTML(GUIP.getWarningColor())).append("Partial Repairs").append("</FONT>");
            }

            // Offboard deployment
            if (entity.isOffBoard()) {
                result.append("&nbsp;&nbsp;&nbsp;&nbsp;");
                String msg_offboard = Messages.getString("ChatLounge.compact.deploysOffBoard");
                msg_offboard = "<I>" + msg_offboard + "</I>";
                result.append(UIUtil.fontHTML(GUIP.getUnitToolTipHighlightColor()))
                      .append(msg_offboard)
                      .append("</FONT>");
            } else if (!entity.isDeployed()) {
                result.append("&nbsp;&nbsp;&nbsp;&nbsp;");
                String msg_deploy = Messages.getString("ChatLounge.compact.deployRound", entity.getDeployRound());
                String msg_zone = "";
                if (entity.getStartingPos(false) != Board.START_NONE) {
                    msg_zone = Messages.getString("ChatLounge.compact.deployZone",
                          IStartingPositions.START_LOCATION_NAMES[entity.getStartingPos(false)]);
                }
                msg_deploy = "<I>" + msg_deploy + msg_zone + "</I>";
                result.append(UIUtil.fontHTML(GUIP.getUnitToolTipHighlightColor()))
                      .append(msg_deploy)
                      .append("</FONT>");
            }

            // Starting values for Altitude / Velocity / Elevation
            if (!isCarried) {
                if (entity.isAero()) {
                    IAero aero = (IAero) entity;
                    result.append("&nbsp;&nbsp;&nbsp;&nbsp;");
                    String msg_vel = Messages.getString("ChatLounge.compact.velocity") + ": ";
                    msg_vel += aero.getCurrentVelocity();
                    String msg_alt = "";
                    String msg_fuel = "";
                    if (!game.getBoard().isSpace()) {
                        msg_alt = ", " + Messages.getString("ChatLounge.compact.altitude") + ": ";
                        msg_alt += aero.getAltitude();
                    }
                    if (options.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_FUEL_CONSUMPTION)) {
                        msg_fuel = ", " + Messages.getString("ChatLounge.compact.fuel") + ": ";
                        msg_fuel += aero.getCurrentFuel();
                    }
                    msg_vel = "<I>" + msg_vel + msg_alt + msg_fuel + "</I>";
                    result.append(UIUtil.fontHTML(GUIP.getUnitToolTipHighlightColor()))
                          .append(msg_vel)
                          .append("</FONT>");
                } else if (entity.getPosition() != null && ((entity.getElevation() != 0) || (entity instanceof VTOL))) {
                    result.append("&nbsp;&nbsp;&nbsp;&nbsp;");
                    String msg_ele = Messages.getString("ChatLounge.compact.elevation") + ": ";
                    msg_ele += entity.getElevation();
                    msg_ele = "<I>" + msg_ele + "</I>;";
                    result.append(UIUtil.fontHTML(GUIP.getUnitToolTipHighlightColor()))
                          .append(msg_ele)
                          .append("</FONT>");
                }
            }

            // Owner
            if (!localPlayer.equals(owner)) {
                result.append("&nbsp;&nbsp;&nbsp;&nbsp;");
                String player = entity.getOwner().getName() + " \u2691 ";
                result.append(UIUtil.fontHTML(color)).append(player).append("</FONT>");
            }
        }

        return UnitToolTip.wrapWithHTML(formatRow(result.toString(), row));
    }

    /**
     * Creates and returns the display content of the C3-MekTree cell for the given entity and for the compact display
     * mode. Assumes that no enemy or blind-drop-hidden units are provided.
     */
    static String formatForceCompact(Force force, Client client) {
        return formatForce(force, client);
    }

    private static String formatForce(Force force, Client client) {
        Game game = client.getGame();
        Player localPlayer = client.getLocalPlayer();
        int ownerId = game.getForces().getOwnerId(force);
        Player owner = game.getPlayer(ownerId);

        // Get the / ally / enemy color
        Color color = GUIP.getEnemyUnitColor();
        if (ownerId == localPlayer.getId()) {
            color = GUIP.getMyUnitColor();
        } else if (!localPlayer.isEnemyOf(owner)) {
            color = GUIP.getAllyUnitColor();
        }

        StringBuilder result = new StringBuilder("<NOBR>");

        // A top-level / sub force special char
        String fLevel;
        if (force.isTopLevel()) {
            fLevel = "\u2327&nbsp;&nbsp; ";
        } else {
            fLevel = "\u25E5&nbsp;&nbsp; ";
        }
        result.append(fontHTML(color)).append(fLevel).append("</FONT>");

        // Name
        String fName = force.getName();
        fName = "<B>" + fName + "</B>";
        result.append(fontHTML(color)).append(fName).append("</FONT>");

        // ID
        String id = " [" + force.getId() + "]";
        result.append(fontHTML(GUIP.getUnitToolTipHighlightColor())).append(id).append("</FONT>");

        // Display force owner
        if ((ownerId != client.getLocalPlayerNumber()) && (owner != null)) {
            result.append(DOT_SPACER);
            String oName = "\u2691 " + owner.getName();
            result.append(fontHTML(color)).append(oName).append("</FONT>");
        }

        // BV
        List<Entity> fullEntities = ForceAssignable.filterToEntityList(game.getForces().getFullEntities(force));
        result.append(DOT_SPACER);
        int totalBv = fullEntities.stream()
              .filter(e -> !e.isPartOfFighterSquadron())
              .mapToInt(Entity::calculateBattleValue)
              .sum();

        if (totalBv > 0) {
            String msgBVPlain = Messages.getString("ChatLounge.BVplain");
            msgBVPlain = msgBVPlain + " " + String.format("%,d", totalBv);
            result.append(fontHTML(color)).append(msgBVPlain).append("</FONT>");

            // Unit Type
            long unitTypes = fullEntities.stream()
                  .map(e -> Entity.getEntityMajorTypeName(e.getEntityType()))
                  .distinct()
                  .count();
            result.append(DOT_SPACER);

            if (unitTypes > 1) {
                String msg_mixed = Messages.getString("ChatLounge.Mixed");
                result.append(fontHTML(color)).append(msg_mixed).append("</FONT>");
            } else if (unitTypes == 1) {
                Entity entity = CollectionUtil.anyOneElement(fullEntities);
                String eType = UnitType.getTypeName(entity.getUnitType());
                result.append(fontHTML(color)).append(eType).append("</FONT>");
            }

        } else {
            result.append(fontHTML(color)).append("Empty").append("</FONT>");
        }

        return UnitToolTip.wrapWithHTML(result.toString());
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

    static String formatRow(String text, int row) {
        String rowBGColor = "0,0,0,0";
        if ((row & 1) == 0) {  //check if even line
            rowBGColor = "0,0,0,0.1";
        }
        return "<table><tr valign='top' style='background-color: rgba(" + rowBGColor + ")'>" + text +
              "</tr" +
              "></table>";
    }

    private static String formatCell(String text, int width) {
        return "<td width='" + UIUtil.scaleForGUI(width) + "'>" + text + "</td>";
    }

    private static String formatCell(String text, int width, Color color) {
        String cellBGColor = color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",0.1";
        return "<td width='" +
              UIUtil.scaleForGUI(width) +
              "' style='background-color: rgba(" +
              cellBGColor +
              ")'>" +
              text +
              "</td>";
    }

    static void fullIDString(StringBuilder current, int id) {
        formatSpan(current, uiGray());
        current.append(" [ID: ").append(id).append("]</SPAN>");
    }
}

/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.forceDisplay;

import megamek.MegaMek;
import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.*;
import megamek.common.force.Force;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.util.CollectionUtil;

import java.awt.*;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.List;

import static megamek.client.ui.swing.lobby.MekTableModel.DOT_SPACER;
import static megamek.client.ui.swing.util.UIUtil.*;

class ForceDisplayMekCellFormatter {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private ForceDisplayMekCellFormatter() {
    }
    
    /** 
     * Creates and returns the display content of the C3-MekTree cell for the given entity and 
     * for the compact display mode. Assumes that no enemy or blind-drop-hidden units are provided. 
     */
    static String formatUnitCompact(Entity entity, ClientGUI clientGUI) {
        Client client = clientGUI.getClient();
        Game game = client.getGame();
        GameOptions options = game.getOptions();
        Player localPlayer = client.getLocalPlayer();
        Player owner = entity.getOwner();

        if (entity.isSensorReturn(localPlayer)) {
            String value = "<NOBR>&nbsp;&nbsp;";
            String uType = "";

            if (entity instanceof Infantry) {
                uType = Messages.getString("ChatLounge.0");
            } else if (entity instanceof Protomech) {
                uType = Messages.getString("ChatLounge.1");
            } else if (entity instanceof GunEmplacement) {
                uType = Messages.getString("ChatLounge.2");
            } else if (entity.isSupportVehicle()) {
                uType = entity.getWeightClassName();
            } else if (entity.isFighter()) {
                uType = entity.getWeightClassName() + Messages.getString("ChatLounge.4");
            } else if (entity instanceof Mech) {
                uType = entity.getWeightClassName() + Messages.getString("ChatLounge.3");
            } else if (entity instanceof Tank) {
                uType = entity.getWeightClassName() + Messages.getString("ChatLounge.6");
            } else {
                uType = entity.getWeightClassName();
            }

            uType = DOT_SPACER + uType + DOT_SPACER;
            value += guiScaledFontHTML() + uType + "</FONT>";;
            return UnitToolTip.wrapWithHTML(value);
        } else if (!entity.isVisibleToEnemy()) {
           return "";
        }

        StringBuilder result = new StringBuilder("<NOBR>&nbsp;&nbsp;" + guiScaledFontHTML());
        boolean isCarried = entity.getTransportId() != Entity.NONE;
        
        Color color = GUIP.getEnemyUnitColor();
        if (owner.getId() == localPlayer.getId()) {
            color = GUIP.getMyUnitColor();
        } else if (!localPlayer.isEnemyOf(owner)) {
            color = GUIP.getAllyUnitColor();
        }
        color = addGray(color, 128).brighter();

        if (entity.getForceId() == Force.NO_FORCE) {
            result.append(guiScaledFontHTML(color) + "\u25AD </FONT>");
        }

        String id = MessageFormat.format("[{0}] ", entity.getId());
        result.append(guiScaledFontHTML(GUIP.getUnitToolTipHighlightColor()) + id + "</FONT>");

        // Unit name
        // Gray out if the unit is a fighter in a squadron
        if (entity.isPartOfFighterSquadron()) {
            result.append(guiScaledFontHTML(GUIP.getUnitToolTipHighlightColor()) + entity.getShortNameRaw() + "</FONT>");
        } else {
            result.append(entity.getShortNameRaw());
        }

        // Pilot
        Crew pilot = entity.getCrew();
        result.append(guiScaledFontHTML());
        result.append(DOT_SPACER);

        if (pilot.getSlotCount() > 1 || entity instanceof FighterSquadron) {
            result.append("<I>" + Messages.getString("ChatLounge.multipleCrew") + "</I>");
        } else if ((pilot.getNickname(0) != null) && !pilot.getNickname(0).isEmpty()) {
            result.append(guiScaledFontHTML(uiNickColor()) + "<B>'");
            result.append(pilot.getNickname(0).toUpperCase() + "'</B></FONT>");
            if (!pilot.getStatusDesc(0).isEmpty()) {
                result.append(" (" + pilot.getStatusDesc(0) + ")");
            }
        } else {
            result.append(pilot.getDesc(0));
        }

        final boolean rpgSkills = options.booleanOption(OptionsConstants.RPG_RPG_GUNNERY);
        result.append(" (" + pilot.getSkillsAsString(rpgSkills) + ")");

        result.append(DOT_SPACER);
        result.append(UnitToolTip.getDamageLevelDesc(entity).trim());

        // Tonnage
        result.append(DOT_SPACER);
        NumberFormat formatter = NumberFormat.getNumberInstance(MegaMek.getMMOptions().getLocale());
        String tonnage = formatter.format(entity.getWeight());
        tonnage += Messages.getString("ChatLounge.Tons");
        result.append(guiScaledFontHTML() + tonnage + "</FONT>");

        // Alpha Strike Unit Role
        if (!entity.isUnitGroup()) {
            result.append(DOT_SPACER);
            result.append(entity.getRole().toString());
        }

        // Controls the separator dot character
        boolean firstEntry = true;

        if (pilot.countOptions() > 0) {
            firstEntry = dotSpacerOnlyFirst(result, firstEntry);
            String quirks = Messages.getString("ChatLounge.abilities");
            result.append(guiScaledFontHTML(GUIP.getUnitToolTipQuirkColor()) + quirks + "</FONT>");
        }

        // ECM
        if (entity.hasActiveECM()) {
            firstEntry = dotSpacerOnlyFirst(result, firstEntry);
            result.append(guiScaledFontHTML(GUIP.getUnitToolTipHighlightColor(), 0.2f) + ECM_SIGN + "</FONT>");
        }

        // Quirk Count
        int quirkCount = entity.countQuirks() + entity.countWeaponQuirks();
        if (quirkCount > 0) {
            firstEntry = dotSpacerOnlyFirst(result, firstEntry);
            result.append(guiScaledFontHTML(GUIP.getUnitToolTipQuirkColor(), 0.2f) + QUIRKS_SIGN + "</FONT>");
        }
        
        // C3 ...
        if (entity.hasC3i() || entity.hasNavalC3()) {
            firstEntry = dotSpacerOnlyFirst(result, firstEntry);
            String msg_c3i = Messages.getString("ChatLounge.C3i");
            String msg_nc3 = Messages.getString("ChatLounge.NC3");

            String c3Name = entity.hasC3i() ? msg_c3i : msg_nc3;
            if (entity.calculateFreeC3Nodes() >= 5) {
                c3Name += UNCONNECTED_SIGN;
            } else {
                c3Name += CONNECTED_SIGN + entity.getC3NetId();
            }
            result.append(guiScaledFontHTML(uiC3Color()) + c3Name + "</FONT>");
        } 

        if (entity.hasC3()) {
            String msg_c3sabrv = Messages.getString("ChatLounge.C3SAbrv");
            String msg_c3m = Messages.getString("ChatLounge.C3M");
            String msg_c3mcc = Messages.getString("ChatLounge.C3MCC");
            String c3 = "";
            firstEntry = dotSpacerOnlyFirst(result, firstEntry);

            if (entity.getC3Master() == null) {
                if (entity.hasC3S()) {
                    c3 = msg_c3sabrv + UNCONNECTED_SIGN;
                }  
                if (entity.hasC3M()) {
                     c3 = msg_c3m;
                }
            } else if (entity.C3MasterIs(entity)) {
                result.append(msg_c3mcc);
            } else {
                if (entity.hasC3S()) {
                    c3 = msg_c3sabrv + CONNECTED_SIGN;
                } else {
                    c3 = msg_c3m + CONNECTED_SIGN;
                }

                c3 += entity.getC3Master().getChassis();
            }

            result.append(guiScaledFontHTML(GUIP.getUnitToolTipHighlightColor()) + c3 + "</FONT>");
        }

        // Loaded onto another unit
        if (isCarried) {
            Entity loader = entity.getGame().getEntity(entity.getTransportId());
            result.append(DOT_SPACER);
            String carried = "(" + loader.getChassis() + " [" + entity.getTransportId() + "])";
            carried = "<I>" + carried + "</I>";
            result.append(guiScaledFontHTML(uiGreen()) + carried + "</FONT>");
        }

        if (entity.countPartialRepairs() > 0) {
            result.append(DOT_SPACER);
            result.append(guiScaledFontHTML(GUIP.getWarningColor()) + "Partial Repairs" + "</FONT>");
        }
        
        // Offboard deployment
        if (entity.isOffBoard()) {
            result.append(DOT_SPACER);
            String msg_offboard = Messages.getString("ChatLounge.compact.deploysOffBoard");
            msg_offboard = "<I>" + msg_offboard + "</I>";
            result.append(guiScaledFontHTML(uiGreen()) + msg_offboard + "</FONT>");
        } else if (!entity.isDeployed()) {
            result.append(DOT_SPACER);
            String msg_deploy = Messages.getString("ChatLounge.compact.deployRound", entity.getDeployRound());
            String msg_zone = "";
            if (entity.getStartingPos(false) != Board.START_NONE) {
                msg_zone = Messages.getString("ChatLounge.compact.deployZone",
                        IStartingPositions.START_LOCATION_NAMES[entity.getStartingPos(false)]);
            }
            msg_deploy = "<I>" + msg_deploy + msg_zone + "</I>";
            result.append(guiScaledFontHTML(uiGreen()) + msg_deploy + "</FONT>");
        }

        // Starting values for Altitude / Velocity / Elevation
        if (!isCarried) {
            if (entity.isAero()) {
                IAero aero = (IAero) entity;
                result.append(DOT_SPACER);
                String msg_vel = Messages.getString("ChatLounge.compact.velocity") + ": ";
                msg_vel += aero.getCurrentVelocity();
                String msg_alt = "";
                String msg_fuel = "";
                if (!game.getBoard().inSpace()) {
                    msg_alt = ", " + Messages.getString("ChatLounge.compact.altitude") + ": ";
                    msg_alt += aero.getAltitude();
                } 
                if (options.booleanOption(OptionsConstants.ADVAERORULES_FUEL_CONSUMPTION)) {
                    msg_fuel = ", " + Messages.getString("ChatLounge.compact.fuel") + ": ";
                    msg_fuel += aero.getCurrentFuel();
                }
                msg_vel = "<I>" + msg_vel + msg_alt + msg_fuel + "</I>";
                result.append(guiScaledFontHTML(uiGreen()) + msg_vel + "</FONT>");
            } else if ((entity.getElevation() != 0) || (entity instanceof VTOL)) {
                result.append(DOT_SPACER);
                String msg_ele = Messages.getString("ChatLounge.compact.elevation") + ": ";
                msg_ele += entity.getElevation();
                msg_ele = "<I>" + msg_ele + "</I>;";
                result.append(guiScaledFontHTML(uiGreen()) + msg_ele + "</FONT>");
            }
        }

        // Owner
        if (!localPlayer.equals(owner)) {
            result.append(DOT_SPACER);
            String player = entity.getOwner().getName() + " \u2691 ";
            result.append(guiScaledFontHTML(color) + player + "</FONT>");
        }
        
        return UnitToolTip.wrapWithHTML(result.toString());
    }
    
    /** 
     * Creates and returns the display content of the C3-MekTree cell for the given entity and 
     * for the compact display mode. Assumes that no enemy or blind-drop-hidden units are provided. 
     */
    static String formatForceCompact(Force force, ClientGUI clientGUI) {
        return formatForce(force, clientGUI, 0);
    }

    private static String formatForce(Force force, ClientGUI clientGUI, float size) {
        Client client = clientGUI.getClient();
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

        StringBuilder result = new StringBuilder("<NOBR>");

        // A top-level / subforce special char
        String fLevel = "";
        if (force.isTopLevel()) {
            fLevel = "\u2327&nbsp;&nbsp; ";
        } else {
            fLevel = "\u25E5&nbsp;&nbsp; ";
        }
        result.append(guiScaledFontHTML(color, size) + fLevel +  "</FONT>");
        
        // Name
        String fName = force.getName();
        fName = "<B>" + fName + "</B>";
        result.append(guiScaledFontHTML(color, size) + fName +  "</FONT>");
        
        // ID
        String id = " [" + force.getId() + "]";
        result.append(guiScaledFontHTML(GUIP.getUnitToolTipHighlightColor(), size) + id + "</FONT>");
        
        // Display force owner
        if ((ownerId != client.getLocalPlayerNumber()) && (owner != null)) {
            result.append(DOT_SPACER);
            
            PlayerColour ownerColour = (owner.getColour() == null) ?
                    PlayerColour.FIRE_BRICK : owner.getColour();
            String oName = "\u2691 " + owner.getName();
            result.append(guiScaledFontHTML(ownerColour.getColour(), size) + oName + "</FONT>");
        }
        
        // BV
        List<Entity> fullEntities = ForceAssignable.filterToEntityList(game.getForces().getFullEntities(force));
        result.append(DOT_SPACER);
        int totalBv = fullEntities.stream().filter(e -> !e.isPartOfFighterSquadron()).mapToInt(Entity::calculateBattleValue).sum();

        if (totalBv > 0) {
            String msg_bvplain = Messages.getString("ChatLounge.BVplain");
            msg_bvplain +=  msg_bvplain + " " + String.format("%,d", totalBv);
            result.append(guiScaledFontHTML(color, size) + msg_bvplain  + "</FONT>");

            // Unit Type
            long unittypes = fullEntities.stream().map(e -> Entity.getEntityMajorTypeName(e.getEntityType())).distinct().count();
            result.append(DOT_SPACER);

            if (unittypes > 1) {
                String msg_mixed = Messages.getString("ChatLounge.Mixed");
                result.append(guiScaledFontHTML(color, size) + msg_mixed + "</FONT>");
            } else if (unittypes == 1) {
                Entity entity = CollectionUtil.anyOneElement(fullEntities);
                String eType = UnitType.getTypeName(entity.getUnitType());
                result.append(guiScaledFontHTML(color, size) + eType + "</FONT>");
            }

        } else {
            result.append(guiScaledFontHTML(color, size) + "Empty" + "</FONT>");
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
    
    static void fullidString(StringBuilder current, int id) {
        formatSpan(current, uiGray());
        current.append(" [ID: ").append(id).append("]</SPAN>");
    }
    
    static boolean dotSpacerOnlyFirst(StringBuilder current, boolean firstElement) {
        if (firstElement) {
            current.append(DOT_SPACER);
        }
        return false;
    }
}

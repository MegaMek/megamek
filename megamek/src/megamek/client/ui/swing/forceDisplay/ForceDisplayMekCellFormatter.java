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
import megamek.common.preference.PreferenceManager;
import megamek.common.util.CollectionUtil;

import java.awt.*;
import java.text.MessageFormat;
import java.util.List;

import static megamek.client.ui.swing.lobby.MekTableModel.DOT_SPACER;
import static megamek.client.ui.swing.util.UIUtil.*;

class ForceDisplayMekCellFormatter {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private ForceDisplayMekCellFormatter() {
    }

    static String unitTableEntry(InGameObject unit, boolean forceView, ClientGUI clientGUI) {
        if (unit instanceof Entity) {
            return formatUnitCompact((Entity) unit, clientGUI, forceView);
        } else {
            return "This type of object has currently no table entry.";
        }
    }
    
    /** 
     * Creates and returns the display content of the C3-MekTree cell for the given entity and 
     * for the compact display mode. Assumes that no enemy or blind-drop-hidden units are provided. 
     */
    static String formatUnitCompact(Entity entity, ClientGUI clientGUI, boolean forceView) {
        Client client = clientGUI.getClient();
        Game game = client.getGame();
        GameOptions options = game.getOptions();
        Player localPlayer = client.getLocalPlayer();
        Player owner = entity.getOwner();
        boolean localGM = localPlayer.isGameMaster();
        boolean hideEntity = !localGM && !entity.isVisibleToEnemy();
        if (hideEntity) {
            String value = "<HTML><NOBR>&nbsp;&nbsp;";
            if (PreferenceManager.getClientPreferences().getShowUnitId()) {
                value += guiScaledFontHTML(uiGray());
                value += MessageFormat.format("[{0}] </FONT>", entity.getId());
            }
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
            return value + guiScaledFontHTML() + DOT_SPACER + uType + DOT_SPACER;
        }

        StringBuilder result = new StringBuilder("<HTML><NOBR>&nbsp;&nbsp;" + guiScaledFontHTML());
        boolean isCarried = entity.getTransportId() != Entity.NONE;
        
        Color color = GUIP.getEnemyUnitColor();
        if (owner.getId() == localPlayer.getId()) {
            color = GUIP.getMyUnitColor();
        } else if (!localPlayer.isEnemyOf(owner)) {
            color = GUIP.getAllyUnitColor();
        }
        color = addGray(color, 128).brighter();

        if (entity.getForceId() == Force.NO_FORCE && forceView) {
            result.append(guiScaledFontHTML(color));
            result.append("\u25AD </FONT>");
        }

        result.append(guiScaledFontHTML(uiGray()));
        result.append(MessageFormat.format("[{0}] </FONT>", entity.getId()));

        // Unit name
        // Gray out if the unit is a fighter in a squadron
        if (entity.isPartOfFighterSquadron()) {
            result.append(guiScaledFontHTML(uiGray()));
            result.append(entity.getShortNameRaw()).append("</FONT>");
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

        // Alpha Strike Unit Role
        if (!entity.isUnitGroup()) {
            result.append(DOT_SPACER);
            result.append(entity.getRole().toString());
        }

        result.append(DOT_SPACER);
        result.append(UnitToolTip.getDamageLevelDesc(entity).trim());

        // Controls the separator dot character
        boolean firstEntry = true;

        if (pilot.countOptions() > 0) {
            firstEntry = dotSpacerOnlyFirst(result, firstEntry);
            result.append(guiScaledFontHTML(uiQuirksColor()));
            result.append(Messages.getString("ChatLounge.abilities"));
            result.append("</FONT>");
        }

        // ECM
        if (entity.hasActiveECM()) {
            firstEntry = dotSpacerOnlyFirst(result, firstEntry);
            result.append(guiScaledFontHTML(uiC3Color(), 0.2f));
            result.append(ECM_SIGN);
            result.append("</FONT>");
        }

        // Quirk Count
        int quirkCount = entity.countQuirks() + entity.countWeaponQuirks();
        if (quirkCount > 0) {
            firstEntry = dotSpacerOnlyFirst(result, firstEntry);
            result.append(guiScaledFontHTML(uiQuirksColor(), 0.2f));
            result.append(QUIRKS_SIGN);
            result.append("</FONT>");
        }
        
        // C3 ...
        if (entity.hasC3i() || entity.hasNavalC3()) {
            firstEntry = dotSpacerOnlyFirst(result, firstEntry);
            result.append(guiScaledFontHTML(uiC3Color()));
            String msg_c3i = Messages.getString("ChatLounge.C3i");
            String msg_nc3 = Messages.getString("ChatLounge.NC3");

            String c3Name = entity.hasC3i() ? msg_c3i : msg_nc3;
            if (entity.calculateFreeC3Nodes() >= 5) {
                result.append(c3Name + UNCONNECTED_SIGN);
            } else {
                result.append(c3Name + CONNECTED_SIGN + entity.getC3NetId());
            }
            result.append("</FONT>");
        } 

        if (entity.hasC3()) {
            String msg_c3sabrv = Messages.getString("ChatLounge.C3SAbrv");
            String msg_c3m = Messages.getString("ChatLounge.C3M");
            String msg_c3mcc = Messages.getString("ChatLounge.C3MCC");

            firstEntry = dotSpacerOnlyFirst(result, firstEntry);
            result.append(guiScaledFontHTML(uiC3Color()));
            if (entity.getC3Master() == null) {
                if (entity.hasC3S()) {
                    result.append(msg_c3sabrv + UNCONNECTED_SIGN);
                }  
                if (entity.hasC3M()) {
                    result.append(msg_c3m);
                }
            } else if (entity.C3MasterIs(entity)) {
                result.append(msg_c3mcc);
            } else {
                if (entity.hasC3S()) {
                    result.append(msg_c3sabrv + CONNECTED_SIGN);
                } else {
                    result.append(msg_c3m + CONNECTED_SIGN);
                }
                result.append(entity.getC3Master().getChassis());
            }
            result.append("</FONT>");
        }

        // Loaded onto another unit
        if (isCarried) {
            Entity loader = entity.getGame().getEntity(entity.getTransportId());
            result.append(DOT_SPACER + guiScaledFontHTML(uiGreen()) +  "<I>(");
            result.append(loader.getChassis());
            if (PreferenceManager.getClientPreferences().getShowUnitId()) {
                result.append(" [" + entity.getTransportId() + "]");
            }
            result.append(")</I></FONT>");
        }

        // Deployment info, doesn't matter when the unit is carried
        if (!isCarried  && !entity.isDeployed()) {
            if (entity.isHidden()) {
                result.append(DOT_SPACER + guiScaledFontHTML(uiGreen()) + "<I>");
                result.append(Messages.getString("ChatLounge.compact.hidden") + "</I></FONT>");
            }

            if (entity.isHullDown()) {
                result.append(DOT_SPACER + guiScaledFontHTML(uiGreen()) + "<I>");
                result.append(Messages.getString("ChatLounge.compact.hulldown") + "</I></FONT>");
            }

            if (entity.isProne()) {
                result.append(DOT_SPACER + guiScaledFontHTML(uiGreen()) + "<I>");
                result.append(Messages.getString("ChatLounge.compact.prone") + "</I></FONT>");
            }
        }

        if (entity.countPartialRepairs() > 0) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiLightRed()));
            result.append("Partial Repairs</FONT>");
        }
        
        // Offboard deployment
        if (entity.isOffBoard()) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiGreen()) + "<I>"); 
            result.append(Messages.getString("ChatLounge.compact.deploysOffBoard") + "</I></FONT>");
        } else if (entity.getDeployRound() > 0) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiGreen()) + "<I>");
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
                result.append(DOT_SPACER + guiScaledFontHTML(uiGreen()) + "<I>"); 
                result.append(Messages.getString("ChatLounge.compact.velocity") + ": ");
                result.append(aero.getCurrentVelocity());
                if (game.getBoard().inSpace()) {
                    result.append(", " + Messages.getString("ChatLounge.compact.altitude") + ": ");
                    result.append(aero.getAltitude());
                } 
                if (options.booleanOption(OptionsConstants.ADVAERORULES_FUEL_CONSUMPTION)) {
                    result.append(", " + Messages.getString("ChatLounge.compact.fuel") + ": ");
                    result.append(aero.getCurrentFuel());
                }
                result.append("</I></FONT>");
            } else if ((entity.getElevation() != 0) || (entity instanceof VTOL)) {
                result.append(DOT_SPACER + guiScaledFontHTML(uiGreen()) + "<I>");
                result.append(Messages.getString("ChatLounge.compact.elevation") + ": ");
                result.append(entity.getElevation() + "</I></FONT>");
            }
        }

        // Owner
        if (!localPlayer.equals(owner)) {
            result.append(DOT_SPACER);
            result.append(guiScaledFontHTML(owner.getColour().getColour()));
            result.append("\u2691 ");
            result.append(entity.getOwner().getName()).append("</FONT>");
        }

        result.append("</FONT>");
        
        return result.toString();
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

        StringBuilder result = new StringBuilder("<HTML><NOBR>");
        result.append(guiScaledFontHTML(color, size));
        
        // A top-level / subforce special char
        if (force.isTopLevel()) {
            result.append("\u2327&nbsp;&nbsp; ");
        } else {
            result.append("\u25E5&nbsp;&nbsp; ");
        }
        
        // Name
        result.append("<B>").append(force.getName()).append("</B></FONT>");
        
        // ID
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            result.append(guiScaledFontHTML(uiGray(), size));
            result.append(" [").append(force.getId()).append("]</FONT>");
        }
        
        // Display force owner
        if ((ownerId != client.getLocalPlayerNumber()) && (owner != null)) {
            result.append(guiScaledFontHTML(size));
            result.append(DOT_SPACER).append("</FONT>");
            
            PlayerColour ownerColour = (owner.getColour() == null) ?
                    PlayerColour.FIRE_BRICK : owner.getColour();
            result.append(guiScaledFontHTML(ownerColour.getColour(), size));
            result.append("\u2691 ");
            result.append(owner.getName()).append("</FONT>");
        }
        
        // BV
        List<Entity> fullEntities = ForceAssignable.filterToEntityList(game.getForces().getFullEntities(force));
        result.append(guiScaledFontHTML(color, size));
        result.append(DOT_SPACER);
        int totalBv = fullEntities.stream().filter(e -> !e.isPartOfFighterSquadron()).mapToInt(Entity::calculateBattleValue).sum();
        if (totalBv > 0) {
            String msg_bvplain = Messages.getString("ChatLounge.BVplain");
            result.append(msg_bvplain + " ").append(String.format("%,d", totalBv));
            // Unit Type
            long unittypes = fullEntities.stream().map(e -> Entity.getEntityMajorTypeName(e.getEntityType())).distinct().count();
            result.append(guiScaledFontHTML(color, size));
            result.append(DOT_SPACER);
            if (unittypes > 1) {
                String msg_mixed = Messages.getString("ChatLounge.Mixed");
                result.append(" " + msg_mixed);
            } else if (unittypes == 1) {
                Entity entity = CollectionUtil.anyOneElement(fullEntities);
                result.append(UnitType.getTypeName(entity.getUnitType()));
            }
        } else {
            result.append("Empty");
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

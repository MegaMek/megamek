/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.lobby;

import megamek.MegaMek;
import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.calculationReport.FlexibleCalculationReport;
import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.*;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.force.Force;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.CollectionUtil;
import megamek.common.util.CrewSkillSummaryUtil;

import java.awt.*;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.List;

import static megamek.client.ui.Messages.getString;
import static megamek.client.ui.swing.lobby.MekTableModel.DOT_SPACER;
import static megamek.client.ui.swing.util.UIUtil.*;

class LobbyMekCellFormatter {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    
    /** 
     * Creates and returns the display content of the Unit column for the given entity and
     * for the non-compact display mode.
     * When blindDrop is true, the unit details are not given.
     */
    static String formatUnitFull(Entity entity, ChatLounge lobby, boolean forceView) {
        StringBuilder result = new StringBuilder("<HTML><NOBR>" + guiScaledFontHTML());

        Client client = lobby.getClientgui().getClient();
        Game game = client.getGame();
        GameOptions options = game.getOptions();
        Player localPlayer = client.getLocalPlayer();
        Player owner = entity.getOwner();
        boolean hideEntity = owner.isEnemyOf(localPlayer)
                && options.booleanOption(OptionsConstants.BASE_BLIND_DROP);
        if (hideEntity) {
            result.append(DOT_SPACER);
            if (entity instanceof Infantry) {
                result.append(Messages.getString("ChatLounge.0"));
            } else if (entity instanceof Protomech) {
                result.append(Messages.getString("ChatLounge.1"));
            } else if (entity instanceof GunEmplacement) {
                result.append(Messages.getString("ChatLounge.2"));
            } else if (entity.isSupportVehicle()) {
                result.append(entity.getWeightClassName());
            } else if (entity.isFighter()) {
                result.append(entity.getWeightClassName() + Messages.getString("ChatLounge.4"));
            } else if (entity instanceof Mech) {
                result.append(entity.getWeightClassName() + Messages.getString("ChatLounge.3"));
            } else if (entity instanceof Tank) {
                result.append(entity.getWeightClassName() + Messages.getString("ChatLounge.6"));
            } else {
                result.append(entity.getWeightClassName());
            }
            result.append(DOT_SPACER);
            return result.toString();
        } 

        boolean isCarried = entity.getTransportId() != Entity.NONE; 
        boolean hasWarning = false;
        boolean hasCritical = false;
        int mapType = lobby.mapSettings.getMedium();
        
        // First line
        if (LobbyUtility.hasYellowWarning(entity)) {
            result.append(guiScaledFontHTML(uiYellow())); 
            result.append(WARNING_SIGN + "</FONT>");
            hasWarning = true;
        }

        // Critical (Red) Warnings
        if ((entity.getGame().getPlanetaryConditions().whyDoomed(entity, entity.getGame()) != null)
                || (entity.doomedInAtmosphere() && mapType == MapSettings.MEDIUM_ATMOSPHERE)
                || (entity.doomedOnGround() && mapType == MapSettings.MEDIUM_GROUND)
                || (entity.doomedInSpace() && mapType == MapSettings.MEDIUM_SPACE)
                || (!entity.isDesignValid())) {
            result.append(guiScaledFontHTML(GUIP.getWarningColor()));
            result.append(WARNING_SIGN + "</FONT>");
            hasCritical = true;
        }

        // Unit Name
        if (hasCritical) {
            result.append(guiScaledFontHTML(GUIP.getWarningColor()));
        } else if (hasWarning) {
            result.append(guiScaledFontHTML(uiYellow()));
        } else {
            result.append(guiScaledFontHTML());
        }
        result.append("<B>" + entity.getShortNameRaw() + "</B></FONT>");

        // ID
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            result.append(guiScaledFontHTML(uiGray()));
            result.append(" [ID: " + entity.getId() + "]</FONT>");
        }
        if (!forceView) {
            result.append( "<BR>");
        }
        
        // Tonnage
        result.append(guiScaledFontHTML());
        if (forceView) {
            result.append(DOT_SPACER);
        }
        NumberFormat formatter = NumberFormat.getNumberInstance(MegaMek.getMMOptions().getLocale());
        result.append(formatter.format(entity.getWeight()));
        result.append(Messages.getString("ChatLounge.Tons"));
        result.append("</FONT>");

        // Alpha Strike Unit Role
        if (ASConverter.canConvert(entity)) {
            FlexibleCalculationReport report = new FlexibleCalculationReport();
            AlphaStrikeElement element = ASConverter.convert(entity, false, report);
            result.append(DOT_SPACER);
            result.append(element.getRole());
        }
        
        // Invalid Design
        if (!forceView) {
            if (!entity.isDesignValid()) {
                result.append(DOT_SPACER);
                result.append(guiScaledFontHTML(GUIP.getWarningColor()));
                result.append("\u26D4 </FONT>").append(Messages.getString("ChatLounge.invalidDesign"));
            }
        }
        
        // ECM
        if (entity.hasActiveECM()) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiC3Color()));
            result.append(ECM_SIGN + " ");
            result.append(Messages.getString("BoardView1.ecmSource"));
            result.append("</FONT>");
        }
        
        // Quirk Count
        int quirkCount = entity.countQuirks() + entity.countWeaponQuirks();
        if (quirkCount > 0) {
            result.append(DOT_SPACER);
            result.append(guiScaledFontHTML(uiQuirksColor()));
            result.append(QUIRKS_SIGN);
            result.append(Messages.getString("ChatLounge.Quirks"));
            result.append("</FONT>");
        }
        
        // Pilot
        if (forceView) {
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
            if (pilot.countOptions() > 0) {
                result.append(DOT_SPACER + guiScaledFontHTML(uiQuirksColor()));
                result.append(Messages.getString("ChatLounge.abilities"));
            }

            // Owner
            if (!localPlayer.equals(owner)) {
                result.append(DOT_SPACER);
                result.append(guiScaledFontHTML(owner.getColour().getColour()));
                result.append("\u2691 ");
                result.append(entity.getOwner().getName()).append("</FONT>");
            }

            // Info sign (i)
            result.append(guiScaledFontHTML(uiGreen()));
            result.append("&nbsp;  \u24D8</FONT>");
        }

        // SECOND OR THIRD LINE in Force View / Table
        result.append( "<BR>");

        // Controls the separator dot character
        boolean firstEntry = true;

        // Start Position
        int sp = entity.getStartingPos(true);
        int spe = entity.getStartingPos(false);
        if ((!entity.isOffBoard())
                && (sp >= 0)
                && (sp < IStartingPositions.START_LOCATION_NAMES.length)) {
            firstEntry = dotSpacer(result, firstEntry);
            if (spe != Board.START_NONE) {
                result.append(guiScaledFontHTML(uiLightGreen()));
            }
            String msg_start = Messages.getString("ChatLounge.Start");
            result.append(" " + msg_start + ":" + IStartingPositions.START_LOCATION_NAMES[sp]);
            int so = entity.getStartingOffset(true);
            int sw = entity.getStartingWidth(true);
            if ((so != 0) || (sw != 3)) {
                result.append(", " + so);
                result.append(", " + sw);
            }
            if (spe != Board.START_NONE) {
                result.append("</FONT>");
            }
        }

        // Invalid Design
        if (forceView) {
            if (!entity.isDesignValid()) {
                firstEntry = dotSpacer(result, firstEntry);
                result.append(guiScaledFontHTML(GUIP.getWarningColor()));
                result.append("\u26D4 </FONT>").append(Messages.getString("ChatLounge.invalidDesign"));
            }
        }

        // C3 ...
        if (entity.hasC3i()) {
            firstEntry = dotSpacer(result, firstEntry);
            result.append(guiScaledFontHTML(uiC3Color()));
            if (entity.calculateFreeC3Nodes() >= 5) {
                result.append("C3i" + UNCONNECTED_SIGN);
            } else {
                result.append("C3i" + CONNECTED_SIGN + entity.getC3NetId());
                if (entity.calculateFreeC3Nodes() > 0) {
                    result.append(Messages.getString("ChatLounge.C3iNodes", entity.calculateFreeC3Nodes()));
                }
            }
            result.append("</FONT>");
        } 

        if (entity.hasNavalC3()) {
            firstEntry = dotSpacer(result, firstEntry);
            result.append(guiScaledFontHTML(uiC3Color()));
            if (entity.calculateFreeC3Nodes() >= 5) {
                result.append("NC3" + UNCONNECTED_SIGN);
            } else {
                result.append("NC3" + CONNECTED_SIGN + entity.getC3NetId());
                if (entity.calculateFreeC3Nodes() > 0) {
                    result.append(Messages.getString("ChatLounge.C3iNodes", entity.calculateFreeC3Nodes()));
                }
            }
            result.append("</FONT>");
        } 

        if (entity.hasC3()) {
            if (entity.getC3Master() == null) {
                if (entity.hasC3S()) {
                    firstEntry = dotSpacer(result, firstEntry);
                    result.append(guiScaledFontHTML(uiC3Color()) + Messages.getString("ChatLounge.C3S") + UNCONNECTED_SIGN);
                    result.append("</FONT>");
                } 

                if (entity.hasC3M()) {
                    firstEntry = dotSpacer(result, firstEntry);
                    result.append(guiScaledFontHTML(uiC3Color()) + Messages.getString("ChatLounge.C3Master"));
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
                result.append(guiScaledFontHTML(uiC3Color()) + Messages.getString("ChatLounge.C3CC"));
                if (entity.hasC3MM()) {
                    String msg_freec3mnodes = Messages.getString("ChatLounge.FreeC3MNodes");
                    result.append(MessageFormat.format(" " + msg_freec3mnodes,
                            entity.calculateFreeC3MNodes(), entity.calculateFreeC3Nodes()));
                } else {
                    result.append(getString("ChatLounge.C3MNodes", entity.calculateFreeC3MNodes()));
                }
                result.append("</FONT>");
            } else {
                firstEntry = dotSpacer(result, firstEntry);
                result.append(guiScaledFontHTML(uiC3Color()));
                if (entity.hasC3S()) {
                    result.append(getString("ChatLounge.C3S") + CONNECTED_SIGN); 
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
        result.append(guiScaledFontHTML(uiGreen())); 
        if (isCarried) {
            firstEntry = dotSpacer(result, firstEntry);
            Entity loader = entity.getGame().getEntity(entity.getTransportId());
            result.append(guiScaledFontHTML(uiGreen()) + LOADED_SIGN);
            result.append("<I> " + Messages.getString("ChatLounge.aboard") + " " + loader.getChassis());
            if (PreferenceManager.getClientPreferences().getShowUnitId()) {
                result.append(" [" + entity.getTransportId() + "]");
            }
            result.append("</I></FONT>");

        } else { // Hide deployment info when a unit is carried
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
            result.append(",  " + entity.getOffBoardDirection());
            result.append(", " + entity.getOffBoardDistance());
        } else if (entity.getDeployRound() > 0) {
            firstEntry = dotSpacer(result, firstEntry);
            result.append(getString("ChatLounge.deploysAfterRound", entity.getDeployRound()));
        }
        result.append("</FONT>");
        
        // Starting heat
        if (entity.getHeat() != 0 && entity.tracksHeat()) {
            firstEntry = dotSpacer(result, firstEntry);
            result.append(guiScaledFontHTML(uiLightRed())); 
            result.append(entity.getHeat()).append(" Heat").append("</FONT>");
        }
        
        // Partial Repairs
        int partRepCount = entity.countPartialRepairs();
        if ((partRepCount > 0)) {
            firstEntry = dotSpacer(result, firstEntry);
            result.append(guiScaledFontHTML(uiLightRed()));
            result.append(Messages.getString("ChatLounge.PartialRepairs"));
            result.append("</FONT>");
        }
        
        // Starting values for Altitude / Velocity / Elevation
        if (!isCarried) {
            if (entity.isAero()) {
                IAero aero = (IAero) entity;
                firstEntry = dotSpacer(result, firstEntry);
                result.append(guiScaledFontHTML(uiGreen()) + "<I>"); 
                result.append(Messages.getString("ChatLounge.compact.velocity") + ": ");
                result.append(aero.getCurrentVelocity());
                if (mapType != MapSettings.MEDIUM_SPACE) {
                    result.append(", " + Messages.getString("ChatLounge.compact.altitude") + ": ");
                    result.append(aero.getAltitude());
                } 
                if (options.booleanOption(OptionsConstants.ADVAERORULES_FUEL_CONSUMPTION)) {
                    result.append(", " + Messages.getString("ChatLounge.compact.fuel") + ": ");
                    result.append(aero.getCurrentFuel());
                }
                result.append("</I></FONT>");
            } else if ((entity.getElevation() != 0) || (entity instanceof VTOL)) {
                firstEntry = dotSpacer(result, firstEntry);
                result.append(guiScaledFontHTML(uiGreen()) + "<I>");
                result.append(Messages.getString("ChatLounge.compact.elevation") + ": ");
                result.append(entity.getElevation() + "</I></FONT>");
            }
        }

        // Auto Eject
        String msg_autoejectdisabled = Messages.getString("ChatLounge.AutoEjectDisabled");
        if (entity instanceof Mech) {
            Mech mech = ((Mech) entity);
            if ((mech.hasEjectSeat()) && (!mech.isAutoEject())) {
                firstEntry = dotSpacer(result, firstEntry);
                result.append(guiScaledFontHTML(uiYellow()));
                result.append(WARNING_SIGN + "\u23CF<I>");
                result.append(msg_autoejectdisabled);
                result.append("</I></FONT>");
            }
        }
        if ((entity instanceof Aero)
                && (!(entity instanceof Jumpship))
                && (!(entity instanceof SmallCraft))) {
            Aero aero = ((Aero) entity);
            if ((aero.hasEjectSeat())
                    && (!aero.isAutoEject())) {
                firstEntry = dotSpacer(result, firstEntry);
                result.append(guiScaledFontHTML(uiYellow()));
                result.append(WARNING_SIGN + "\u23CF<I>");
                result.append(msg_autoejectdisabled);
                result.append("</I></FONT>");
            }
        }
        
        return result.toString();
    }
    
    /** 
     * Creates and returns the display content of the C3-MekTree cell for the given entity and 
     * for the compact display mode. Assumes that no enemy or blind-drop-hidden units are provided. 
     */
    static String formatUnitCompact(Entity entity, ChatLounge lobby, boolean forceView) {
        Client client = lobby.getClientgui().getClient();
        Game game = client.getGame();
        GameOptions options = game.getOptions();
        Player localPlayer = client.getLocalPlayer();
        Player owner = entity.getOwner();
        boolean hideEntity = owner.isEnemyOf(localPlayer)
                && options.booleanOption(OptionsConstants.BASE_BLIND_DROP);
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
        int mapType = lobby.mapSettings.getMedium();
        
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
        
        // Signs before the unit name
        // ID
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            result.append(guiScaledFontHTML(uiGray()));
            result.append(MessageFormat.format("[{0}] </FONT>", entity.getId()));
        }

        // Critical (Red) Warnings
        if ((entity.getGame().getPlanetaryConditions().whyDoomed(entity, entity.getGame()) != null)
                || (entity.doomedInAtmosphere() && mapType == MapSettings.MEDIUM_ATMOSPHERE)
                || (entity.doomedOnGround() && mapType == MapSettings.MEDIUM_GROUND)
                || (entity.doomedInSpace() && mapType == MapSettings.MEDIUM_SPACE)
                || (!entity.isDesignValid())) {
            result.append(guiScaledFontHTML(GUIP.getWarningColor()));
            result.append(WARNING_SIGN + "</FONT>");
        }

        // General (Yellow) Warnings
        if (LobbyUtility.hasYellowWarning(entity)) {
            result.append(guiScaledFontHTML(uiYellow())); 
            result.append(WARNING_SIGN + "</FONT>");
        }

        // Loaded unit
        if (isCarried) {
            result.append(guiScaledFontHTML(uiGreen()) + LOADED_SIGN + "</FONT>");
        }

        // Unit name
        // Gray out if the unit is a fighter in a squadron
        if (entity.isPartOfFighterSquadron()) {
            result.append(guiScaledFontHTML(uiGray()));
            result.append(entity.getShortNameRaw()).append("</FONT>");
        } else {
            result.append(entity.getShortNameRaw());
        }

        // Pilot
        if (forceView) {
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
            if (pilot.countOptions() > 0) {
                result.append(DOT_SPACER + guiScaledFontHTML(uiQuirksColor()));
                result.append(Messages.getString("ChatLounge.abilities"));
            }

            // Owner
            if (!localPlayer.equals(owner)) {
                result.append(DOT_SPACER);
                result.append(guiScaledFontHTML(owner.getColour().getColour()));
                result.append("\u2691 ");
                result.append(entity.getOwner().getName()).append("</FONT>");
            }
        }

        // Invalid unit design
        if (!entity.isDesignValid()) {
            result.append(DOT_SPACER);
            result.append(guiScaledFontHTML(GUIP.getWarningColor()));
            result.append("\u26D4 </FONT>").append(Messages.getString("ChatLounge.invalidDesign"));
        }

        // ECM
        if (entity.hasActiveECM()) {
            result.append(guiScaledFontHTML(uiC3Color(), 0.2f));
            result.append(ECM_SIGN);
            result.append("</FONT>");
        }

        // Quirk Count
        int quirkCount = entity.countQuirks() + entity.countWeaponQuirks();
        if (quirkCount > 0) {
            result.append(guiScaledFontHTML(uiQuirksColor(), 0.2f));
            result.append(QUIRKS_SIGN);
            result.append("</FONT>");
        }
        
        // C3 ...
        if (entity.hasC3i() || entity.hasNavalC3()) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiC3Color()));
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

            result.append(DOT_SPACER + guiScaledFontHTML(uiC3Color()));
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
        if (!isCarried) {
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
                if (mapType != MapSettings.MEDIUM_SPACE) {
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
        
        // Starting heat
        if (entity.getHeat() != 0 && entity.tracksHeat()) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiGreen())); 
            result.append("<I>Heat: ").append(entity.getHeat()).append(" </I></FONT>");
        }
        
        result.append("</FONT>");
        
        // Info tooltip sign
        if (forceView) {
            result.append(guiScaledFontHTML(uiGreen(), 0.3f));
            result.append("&nbsp;  \u24D8");
        }

        // Auto Eject
        if (entity instanceof Mech) {
            Mech mech = ((Mech) entity);
            if ((mech.hasEjectSeat()) && (!mech.isAutoEject())) {
                result.append(DOT_SPACER + guiScaledFontHTML(uiGreen()));
                result.append(guiScaledFontHTML(uiYellow()));
                result.append(WARNING_SIGN + "\u23CF</FONT>");
            }
        }
        if ((entity instanceof Aero)
                && (!(entity instanceof Jumpship))
                && (!(entity instanceof SmallCraft))) {
            Aero aero = ((Aero) entity);
            if ((aero.hasEjectSeat())
                    && (!aero.isAutoEject())) {
                result.append(DOT_SPACER + guiScaledFontHTML(uiGreen()));
                result.append(guiScaledFontHTML(uiYellow()));
                result.append(WARNING_SIGN + "\u23CF</FONT>");
            }
        }
        
        return LobbyUtility.abbreviateUnitName(result.toString()); 
    }
    
    /** 
     * Creates and returns the display content of the C3-MekTree cell for the given entity and 
     * for the compact display mode. Assumes that no enemy or blind-drop-hidden units are provided. 
     */
    static String formatForceCompact(Force force, ChatLounge lobby) {
        return formatForce(force, lobby, 0);
    }
    
    /** 
     * Creates and returns the display content of the C3-MekTree cell for the given entity and 
     * for the compact display mode. Assumes that no enemy or blind-drop-hidden units are provided. 
     */
    static String formatForceFull(Force force, ChatLounge lobby) {
        return formatForce(force, lobby, 0.2f);
    }
    
    private static String formatForce(Force force, ChatLounge lobby, float size) {
        Client client = lobby.getClientgui().getClient();
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
        List<Entity> fullEntities = ForceAssignable.filterToEntityList(lobby.game().getForces().getFullEntities(force));
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
    
    /** 
     * Creates and returns the display content of the Pilot column for the given entity and
     * for the compact display mode.
     * When blindDrop is true, the pilot details are not given.
     */
    static String formatPilotCompact(Entity entity, boolean blindDrop, boolean rpgSkills) {
        Crew pilot = entity.getCrew();
        StringBuilder result = new StringBuilder("<HTML><NOBR>");
        result.append(guiScaledFontHTML());
        
        if (blindDrop) {
            result.append(Messages.getString("ChatLounge.Unknown"));
            return result.toString();
        } 

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

        result.append(" (" + pilot.getSkillsAsString(rpgSkills) + ")");
        if (pilot.countOptions() > 0) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiQuirksColor()));
            result.append(Messages.getString("ChatLounge.abilities"));
        }

        result.append("</FONT>");
        return result.toString();
    }
    
    /** 
     * Creates and returns the display content of the Pilot column for the given entity and
     * for the non-compact display mode.
     * When blindDrop is true, the pilot details are not given.
     */
    static String formatPilotFull(Entity entity, boolean blindDrop) {
        StringBuilder result = new StringBuilder("<HTML><NOBR>");
        
        final Crew crew = entity.getCrew();
        final GameOptions options = entity.getGame().getOptions();
        final boolean rpgSkills = options.booleanOption(OptionsConstants.RPG_RPG_GUNNERY);
        final float overallScale = 0f;
        
        result.append(guiScaledFontHTML(overallScale));
        
        if (blindDrop) {
            result.append("<B>" + Messages.getString("ChatLounge.Unknown") + "</B>");
            return result.toString();
        } 
        
        if (crew.getSlotCount() == 1 && !(entity instanceof FighterSquadron)) { // Single-person crew
            if (crew.isMissing(0)) {
                result.append("<B>No " + crew.getCrewType().getRoleName(0) + "</B>");
            } else {
                if ((crew.getNickname(0) != null) && !crew.getNickname(0).isEmpty()) {
                    result.append(guiScaledFontHTML(uiNickColor(), overallScale)); 
                    result.append("<B>'" + crew.getNickname(0).toUpperCase() + "'</B></FONT>");
                } else {
                    result.append("<B>" + crew.getDesc(0) + "</B>");
                }
            }
            result.append("<BR>");
        } else { // Multi-person crew
            result.append("<I>" + Messages.getString("ChatLounge.multipleCrew") + "</I>");
            result.append("<BR>");
        }
        result.append(CrewSkillSummaryUtil.getSkillNames(entity) + ": ");
        result.append("<B>" + crew.getSkillsAsString(rpgSkills) + "</B><BR>");
        
        // Advantages, MD, Edge
        if (crew.countOptions() > 0) {
            result.append(guiScaledFontHTML(uiQuirksColor(), overallScale));
            result.append(Messages.getString("ChatLounge.abilities"));
            result.append("</FONT>");
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
    
    static boolean dotSpacer(StringBuilder current, boolean firstElement) {
        if (!firstElement) {
            current.append(DOT_SPACER);
        }
        return false;
    }
}

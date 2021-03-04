/*  
 * MegaMek - Copyright (C) 2021 - The MegaMek Team  
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
package megamek.client.ui.swing.lobby;


import static megamek.client.ui.swing.util.UIUtil.*;

import java.text.MessageFormat;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Aero;
import megamek.common.Board;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.FighterSquadron;
import megamek.common.GunEmplacement;
import megamek.common.IStartingPositions;
import megamek.common.Infantry;
import megamek.common.MapSettings;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.VTOL;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.CrewSkillSummaryUtil;
import static megamek.client.ui.swing.lobby.MekTableModel.DOT_SPACER;

public class MekTreeC3CellFormatter {
    
    /** 
     * Creates and returns the display content of the C3-MekTree cell for the given entity and 
     * for the compact display mode. Assumes that no enemy or blind-drop-hidden units are provided. 
     */
    static String formatNetworkCompact(String netId) {

        StringBuilder result = new StringBuilder("<HTML><NOBR>&nbsp;&nbsp;");
        String type = "C3";
        if (netId.contains("C3i")) {
            type = "C3i";
        } else if (netId.contains("NC3")) {
            type = "Naval C3";
        }
        result.append(guiScaledFontHTML(uiC3Color()));
        result.append(type);
        result.append(" Network ");
        result.append(guiScaledFontHTML(uiGray()));
        result.append("[").append(netId).append("]");
        return result.toString();
    }

    /** 
     * Creates and returns the display content of the C3-MekTree cell for the given entity and 
     * for the compact display mode. Assumes that no enemy or blind-drop-hidden units are provided. 
     */
    static String formatUnitCompact(Entity entity) {

        StringBuilder result = new StringBuilder("<HTML><NOBR>&nbsp;&nbsp;" + guiScaledFontHTML());
        boolean isCarried = entity.getTransportId() != Entity.NONE;
        
        // C3 ...
        if (entity.hasC3i() || entity.hasNavalC3()) {
            result.append(guiScaledFontHTML(uiC3Color()));
            String c3Name = entity.hasC3i() ? "C3i" : "NC3";
            if (entity.calculateFreeC3Nodes() >= 5) {
                result.append(c3Name);
            } else {
                result.append(c3Name);
            }
            result.append("</FONT> ");
        } 

        if (entity.hasC3()) {
            result.append(guiScaledFontHTML(uiC3Color()));
            if (entity.getC3Master() == null) {
                if (entity.hasC3S()) {
                    result.append("C3S"); 
                }  
                if (entity.hasC3M()) {
                    result.append("C3M"); 
                }
            } else if (entity.C3MasterIs(entity)) {
                result.append("Co Comdr ");
            } else {
                if (entity.hasC3S()) {
                    result.append("C3S"); 
                } else {
                    result.append("C3M"); 
                }
            }
            result.append("</FONT>");
        }

        // Signs before the unit name
        // ID
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            result.append(guiScaledFontHTML(uiGray()));
            result.append(MessageFormat.format("[{0}] </FONT>", entity.getId()));
        }

        // Critical (Red) Warnings
//        if ((entity.getGame().getPlanetaryConditions().whyDoomed(entity, entity.getGame()) != null)
//                || (entity.doomedInAtmosphere() && mapType == MapSettings.MEDIUM_ATMOSPHERE)
//                || (entity.doomedOnGround() && mapType == MapSettings.MEDIUM_GROUND)
//                || (entity.doomedInSpace() && mapType == MapSettings.MEDIUM_SPACE)
//                || (!entity.isDesignValid())
//                ) {
//            result.append(guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor())); 
//            result.append(WARNING_SIGN + "</FONT>");
//        }
//
//        // General (Yellow) Warnings
//        if (((entity.hasC3i() || entity.hasNavalC3()) && (entity.calculateFreeC3Nodes() == 5))
//                || ((entity.getC3Master() == null) && entity.hasC3S())
//                ) {
//            result.append(guiScaledFontHTML(uiYellow())); 
//            result.append(WARNING_SIGN + "</FONT>");
//        }
//
//        // Loaded unit
//        if (isCarried) {
//            result.append(guiScaledFontHTML(uiGreen()) + LOADED_SIGN + "</FONT>");
//        }

        // Unit name
        result.append(entity.getShortNameRaw());

        // Invalid unit design
        if (!entity.isDesignValid()) {
            result.append(DOT_SPACER + guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor()));
            result.append(Messages.getString("ChatLounge.invalidDesign"));
            result.append("</FONT>");
        }

        // ECM
        if (entity.hasActiveECM()) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiC3Color()));
            result.append("\u25CE");
            result.append("</FONT>");
        }

        // Quirk Count
//        int quirkCount = entity.countQuirks() + entity.countWeaponQuirks();
//        if (quirkCount > 0) {
//            result.append(DOT_SPACER + guiScaledFontHTML(uiQuirksColor()));
//            result.append(Messages.getString("BT.Quirks"));
//            result.append("</FONT>");
//        }

      

        // Loaded onto another unit
//        if (isCarried) {
//            Entity loader = entity.getGame().getEntity(entity.getTransportId());
//            result.append(DOT_SPACER + guiScaledFontHTML(uiGreen()) +  "<I>(");
//            result.append(loader.getChassis());
//            if (PreferenceManager.getClientPreferences().getShowUnitId()) {
//                result.append(" [" + entity.getTransportId() + "]");
//            }
//            result.append(")</I></FONT>");
//        }

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
//        if (!isCarried) {
//            if (entity.isAero()) {
//                Aero aero = (Aero) entity;
//                result.append(DOT_SPACER + guiScaledFontHTML(uiGreen()) + "<I>"); 
//                result.append(Messages.getString("ChatLounge.compact.velocity") + ": ");
//                result.append(aero.getCurrentVelocity());
//                if (mapType != MapSettings.MEDIUM_SPACE) {
//                    result.append(", " + Messages.getString("ChatLounge.compact.altitude") + ": ");
//                    result.append(aero.getAltitude());
//                } 
//                if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_FUEL_CONSUMPTION)) {
//                    result.append(", " + Messages.getString("ChatLounge.compact.fuel") + ": ");
//                    result.append(aero.getCurrentFuel());
//                }
//                result.append("</I></FONT>");
//            } else if ((entity.getElevation() != 0) || (entity instanceof VTOL)) {
//                result.append(DOT_SPACER + guiScaledFontHTML(uiGreen()) + "<I>");
//                result.append(Messages.getString("ChatLounge.compact.elevation") + ": ");
//                result.append(entity.getElevation() + "</I></FONT>");
//            }
//        }

        return result.toString(); 
    }

    /** 
     * Creates and returns the display content of the Unit column for the given entity and
     * for the non-compact display mode.
     * When blindDrop is true, the unit details are not given.
     */
    static String formatUnitFull(Entity entity, boolean blindDrop, int mapType) {

        StringBuilder result = new StringBuilder("<HTML><NOBR>" + guiScaledFontHTML());

        if (blindDrop) {
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
        // General (Yellow) Warnings
        if (((entity.hasC3i() || entity.hasNavalC3()) && (entity.calculateFreeC3Nodes() == 5))
                || ((entity.getC3Master() == null) && entity.hasC3S())
                ) {
            result.append(guiScaledFontHTML(uiYellow())); 
            result.append(WARNING_SIGN + "</FONT>");
            hasWarning = true;
        }

        // Critical (Red) Warnings
        if ((entity.getGame().getPlanetaryConditions().whyDoomed(entity, entity.getGame()) != null)
                || (entity.doomedInAtmosphere() && mapType == MapSettings.MEDIUM_ATMOSPHERE)
                || (entity.doomedOnGround() && mapType == MapSettings.MEDIUM_GROUND)
                || (entity.doomedInSpace() && mapType == MapSettings.MEDIUM_SPACE)
                || (!entity.isDesignValid())
                ) {
            result.append(guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor())); 
            result.append(WARNING_SIGN + "</FONT>");
            hasCritical = true;
        }

        // Unit Name
        if (hasCritical) {
            result.append(guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor()));
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
        result.append( "<BR>");

        // SECOND LINE----
        // Tonnage
        result.append(guiScaledFontHTML());
        result.append(Math.round(entity.getWeight()) + Messages.getString("ChatLounge.Tons"));
        result.append("</FONT>");

        // Invalid Design
        if (!entity.isDesignValid()) {
            result.append(DOT_SPACER);
            result.append(Messages.getString("ChatLounge.invalidDesign"));
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
            result.append(Messages.getString("ChatLounge.Quirks"));

            //                int posQuirks = entity.countQuirks(Quirks.POS_QUIRKS);
            //                String pos = posQuirks > 0 ? "+" + posQuirks : ""; 
            //                int negQuirks = entity.countQuirks(Quirks.NEG_QUIRKS);
            //                String neg = negQuirks > 0 ? "-" + negQuirks : "";
            //                int wpQuirks = entity.countWeaponQuirks();
            //                String wpq = wpQuirks > 0 ? "W" + wpQuirks : "";
            //                result.append(arrangeInLine(" / ", pos, neg, wpq));

            result.append("</FONT>");
        }

        // Partial Repairs
        int partRepCount = entity.countPartialRepairs();
        if ((partRepCount > 0)) {
            result.append(DOT_SPACER);
            result.append(guiScaledFontHTML(uiLightRed()));
            result.append(Messages.getString("ChatLounge.PartialRepairs"));
            result.append("</FONT>");
        }
        result.append("<BR>");

        // THIRD LINE----

        // Controls the separator dot character
        boolean subsequentElement = false;

        // C3 ...
        if (entity.hasC3i()) {
            if (subsequentElement) {
                result.append(DOT_SPACER);
            }
            subsequentElement = true;
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
            if (subsequentElement) {
                result.append(DOT_SPACER);
            }
            subsequentElement = true;
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
                    if (subsequentElement) {
                        result.append(DOT_SPACER);
                    }
                    subsequentElement = true;
                    result.append(guiScaledFontHTML(uiC3Color()) + Messages.getString("ChatLounge.C3S") + UNCONNECTED_SIGN);
                    result.append("</FONT>");
                } 

                if (entity.hasC3M()) {
                    if (subsequentElement) {
                        result.append(DOT_SPACER);
                    }
                    subsequentElement = true;

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
                if (subsequentElement) {
                    result.append(DOT_SPACER);
                }
                subsequentElement = true;
                result.append(guiScaledFontHTML(uiC3Color()) + Messages.getString("ChatLounge.C3CC"));
                if (entity.hasC3MM()) {
                    result.append(MessageFormat.format(" ({0}M, {1}S free)", 
                            entity.calculateFreeC3MNodes(), entity.calculateFreeC3Nodes()));
                } else {
                    result.append(Messages.getString("ChatLounge.C3MNodes", entity.calculateFreeC3MNodes()));
                }
                result.append("</FONT>");
            } else {
                if (subsequentElement) {
                    result.append(DOT_SPACER);
                }
                subsequentElement = true;
                result.append(guiScaledFontHTML(uiC3Color()));
                if (entity.hasC3S()) {
                    result.append(Messages.getString("ChatLounge.C3S") + CONNECTED_SIGN); 
                } else {
                    result.append(Messages.getString("ChatLounge.C3Master"));
                    int freeS = entity.calculateFreeC3Nodes();
                    if (freeS == 0) {
                        result.append(Messages.getString("ChatLounge.C3full"));
                    } else {
                        result.append(Messages.getString("ChatLounge.C3SNodes", entity.calculateFreeC3Nodes()));
                    }
                    result.append(CONNECTED_SIGN + "(CC) "); 
                }
                result.append(entity.getC3Master().getChassis());
                result.append("</FONT>");
            }
        }

        // Loaded onto transport
        if (isCarried) {
            if (subsequentElement) {
                result.append(DOT_SPACER);
            }
            subsequentElement = true;
            Entity loader = entity.getGame().getEntity(entity.getTransportId());
            result.append(guiScaledFontHTML(uiGreen()) + LOADED_SIGN);
            result.append("<I> " + Messages.getString("ChatLounge.aboard") + " " + loader.getChassis());
            if (PreferenceManager.getClientPreferences().getShowUnitId()) {
                result.append(" [" + entity.getTransportId() + "]");
            }
            result.append("</I></FONT>");

        } else { // Hide deployment info when a unit is carried

            if (entity.isHidden() && mapType == MapSettings.MEDIUM_GROUND) {
                if (subsequentElement) {
                    result.append(DOT_SPACER);
                }
                subsequentElement = true;
                result.append(guiScaledFontHTML(uiGreen()) + "<I>");
                result.append(Messages.getString("ChatLounge.compact.hidden") + "</I></FONT>");
            }

            if (entity.isHullDown()) {
                if (subsequentElement) {
                    result.append(DOT_SPACER);
                }
                subsequentElement = true;
                result.append(guiScaledFontHTML(uiGreen()) + "<I>");
                result.append(Messages.getString("ChatLounge.hulldown") + "</I></FONT>");
            }

            if (entity.isProne()) {
                if (subsequentElement) {
                    result.append(DOT_SPACER);
                }
                subsequentElement = true;
                result.append(guiScaledFontHTML(uiGreen()) + "<I>");
                result.append(Messages.getString("ChatLounge.prone") + "</I></FONT>");
            }
        }

        if (entity.isOffBoard()) {
            if (subsequentElement) {
                result.append(DOT_SPACER);
            }
            subsequentElement = true;
            result.append(guiScaledFontHTML(uiGreen()) + "<I>");
            result.append(Messages.getString("ChatLounge.deploysOffBoard") + "</I></FONT>"); 
        } else if (entity.getDeployRound() > 0) {
            if (subsequentElement) {
                result.append(DOT_SPACER);
            }
            subsequentElement = true;
            result.append(guiScaledFontHTML(uiGreen()) + "<I>");
            result.append(Messages.getString("ChatLounge.deploysAfterRound", entity.getDeployRound())); 
            if (entity.getStartingPos(false) != Board.START_NONE) {
                result.append(Messages.getString("ChatLounge.deploysAfterZone", 
                        IStartingPositions.START_LOCATION_NAMES[entity.getStartingPos(false)]));
            }
            result.append("</I></FONT>");
        }

        // Starting values for Altitude / Velocity / Elevation
        if (!isCarried) {
            if (entity.isAero()) {
                Aero aero = (Aero) entity;
                if (subsequentElement) {
                    result.append(DOT_SPACER);
                }
                subsequentElement = true;
                result.append(guiScaledFontHTML(uiGreen()) + "<I>"); 
                result.append(Messages.getString("ChatLounge.compact.velocity") + ": ");
                result.append(aero.getCurrentVelocity());
                if (mapType != MapSettings.MEDIUM_SPACE) {
                    result.append(", " + Messages.getString("ChatLounge.compact.altitude") + ": ");
                    result.append(aero.getAltitude());
                } 
                if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_FUEL_CONSUMPTION)) {
                    result.append(", " + Messages.getString("ChatLounge.compact.fuel") + ": ");
                    result.append(aero.getCurrentFuel());
                }
                result.append("</I></FONT>");
            } else if ((entity.getElevation() != 0) || (entity instanceof VTOL)) {
                if (subsequentElement) {
                    result.append(DOT_SPACER);
                }
                subsequentElement = true;
                result.append(guiScaledFontHTML(uiGreen()) + "<I>");
                result.append(Messages.getString("ChatLounge.compact.elevation") + ": ");
                result.append(entity.getElevation() + "</I></FONT>");
            }
        }

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

}


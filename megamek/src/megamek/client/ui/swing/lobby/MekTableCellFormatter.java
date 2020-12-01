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
package megamek.client.ui.swing.lobby;

import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;
import static megamek.client.ui.swing.util.UIUtil.uiGreen;
import static megamek.client.ui.swing.util.UIUtil.uiLightRed;
import static megamek.client.ui.swing.util.UIUtil.uiNickColor;
import static megamek.client.ui.swing.util.UIUtil.uiQuirksColor;
import static megamek.client.ui.swing.util.UIUtil.uiYellow;

import java.text.MessageFormat;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Aero;
import megamek.common.Board;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.IStartingPositions;
import megamek.common.Infantry;
import megamek.common.MapSettings;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.VTOL;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.options.Quirks;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.CrewSkillSummaryUtil;
import static megamek.client.ui.swing.lobby.MekTableModel.DOT_SPACER;

public class MekTableCellFormatter {
    
    private static final String LOADED_SIGN = " \u26DF ";
    private static final String UNCONNECTED_SIGN = " \u26AC";
    private static final String CONNECTED_SIGN = " \u26AF ";
    private static final String WARNING_SIGN = " \u26A0 ";

    /** 
     * Creates and returns the display content of the Unit column for the given entity and 
     * for the compact display mode. 
     * When blindDrop is true, the unit details are not given.
     */
    static String formatUnitCompact(Entity entity, boolean blindDrop, int mapType) {
        
        if (blindDrop) {
            String value = "<HTML><NOBR>";
            if (PreferenceManager.getClientPreferences().getShowUnitId()) {
                value += UIUtil.guiScaledFontHTML(UIUtil.uiGray());
                value += MessageFormat.format("[{0}] </FONT>", entity.getId());
            }
            String uType;
            if (entity instanceof Infantry) {
                uType = Messages.getString("ChatLounge.0");
            } else if (entity instanceof Protomech) {
                uType = Messages.getString("ChatLounge.1");
            } else if (entity instanceof GunEmplacement) {
                uType = Messages.getString("ChatLounge.2");
            } else {
                uType = entity.getWeightClassName();
                if (entity instanceof Tank) {
                    uType += Messages.getString("ChatLounge.6");
                }
            }
            return value + UIUtil.guiScaledFontHTML() + DOT_SPACER + uType + DOT_SPACER;
        }
        
        StringBuilder result = new StringBuilder("<HTML><NOBR>" + UIUtil.guiScaledFontHTML());
        
        // Signs before the unit name
        // ID
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            result.append(UIUtil.guiScaledFontHTML(UIUtil.uiGray()));
            result.append(MessageFormat.format("[{0}] </FONT>", entity.getId()));
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
        }
        
        // General (Yellow) Warnings
        if (((entity.hasC3i() || entity.hasNavalC3()) && (entity.calculateFreeC3Nodes() == 5))
                || ((entity.getC3Master() == null) && entity.hasC3S())
                ) {
            result.append(guiScaledFontHTML(uiYellow())); 
            result.append(WARNING_SIGN + "</FONT>");
        }
        
        // Loaded unit
        if (entity.getTransportId() != Entity.NONE) {
            result.append(guiScaledFontHTML(uiGreen()) + LOADED_SIGN + "</FONT>");
        }
        
        // Unit name
        result.append(entity.getShortNameRaw());

        // Invalid unit design
        if (!entity.isDesignValid()) {
            result.append(DOT_SPACER + guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor()));
            result.append(Messages.getString("ChatLounge.invalidDesign"));
            result.append("</FONT>");
        }
        
        // Quirk Count
        int quirkCount = entity.countQuirks() + entity.countWeaponQuirks();
        if (quirkCount > 0) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiQuirksColor()));
            result.append(Messages.getString("BT.Quirks") + ": ");
            result.append(quirkCount + "</FONT>");
        }
        
        // C3 ...
        if (entity.hasC3i() || entity.hasNavalC3()) {
            result.append(DOT_SPACER + UIUtil.guiScaledFontHTML(UIUtil.uiC3Color()));
            String c3Name = entity.hasC3i() ? "C3i" : "NC3";
            if (entity.calculateFreeC3Nodes() >= 5) {
                result.append(c3Name + UNCONNECTED_SIGN);
            } else {
                result.append(c3Name + CONNECTED_SIGN + entity.getC3NetId());
            }
            result.append("</FONT>");
        } 

        if (entity.hasC3()) {
            result.append(DOT_SPACER + UIUtil.guiScaledFontHTML(UIUtil.uiC3Color()));
            if (entity.getC3Master() == null) {
                if (entity.hasC3S()) {
                    result.append("C3S" + UNCONNECTED_SIGN); 
                }  
                if (entity.hasC3M()) {
                    result.append("C3M"); 
                }
            } else if (entity.C3MasterIs(entity)) {
                result.append("C3M (CC)");
            } else {
                if (entity.hasC3S()) {
                    result.append("C3S" + CONNECTED_SIGN); 
                } else {
                    result.append("C3M" + CONNECTED_SIGN); 
                }
                result.append(entity.getC3Master().getChassis());
            }
            result.append("</FONT>");
        }
        
        // Loaded onto another unit
        if (entity.getTransportId() != Entity.NONE) {
            Entity loader = entity.getGame().getEntity(entity.getTransportId());
            result.append(DOT_SPACER + UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) +  "<I>(");
            result.append(loader.getChassis() + ")</I></FONT>");
        }

        // Hidden deployment
        if (entity.isHidden() && mapType == MapSettings.MEDIUM_GROUND) {
            result.append(DOT_SPACER + UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>");
            result.append(Messages.getString("ChatLounge.compact.hidden") + "</I></FONT>");
        }
        
        if (entity.isHullDown()) {
            result.append(DOT_SPACER + UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>");
            result.append(Messages.getString("ChatLounge.compact.hulldown") + "</I></FONT>");
        }
        
        if (entity.isProne()) {
            result.append(DOT_SPACER + UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>");
            result.append(Messages.getString("ChatLounge.compact.prone") + "</I></FONT>");
        }
        
        if (entity.countPartialRepairs() > 0) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiLightRed()));
            result.append("Partial Repairs</FONT>");
        }

        // Offboard deployment
        if (entity.isOffBoard()) {
            result.append(DOT_SPACER + UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>"); 
            result.append(Messages.getString("ChatLounge.compact.deploysOffBoard") + "</I></FONT>");
        } else if (entity.getDeployRound() > 0) {
            result.append(DOT_SPACER + UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>");
            result.append(Messages.getString("ChatLounge.compact.deployRound", entity.getDeployRound()));
            if (entity.getStartingPos(false) != Board.START_NONE) {
                result.append(Messages.getString("ChatLounge.compact.deployZone", 
                        IStartingPositions.START_LOCATION_NAMES[entity.getStartingPos(false)]));
            }
            result.append("</I></FONT>");
        }

        // Starting values for Altitude / Velocity / Elevation
        if (entity.isAero()) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiGreen()) + "<I>"); 
            Aero aero = (Aero) entity;
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
            result.append(DOT_SPACER + guiScaledFontHTML(uiGreen()) + "<I>");
            result.append(Messages.getString("ChatLounge.compact.elevation") + ": ");
            result.append(entity.getElevation() + "</I></FONT>");
        }
        return result.toString(); 
    }

    /** 
     * Creates and returns the display content of the Unit column for the given entity and
     * for the non-compact display mode.
     * When blindDrop is true, the unit details are not given.
     */
    static String formatUnitFull(Entity entity, boolean blindDrop, int mapType) {

        String value = "<HTML><NOBR>" + UIUtil.guiScaledFontHTML();
        // ID
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            value += UIUtil.guiScaledFontHTML(UIUtil.uiGray());
            value += "[ID: " + entity.getId() + "] </FONT>";
        }

        if (blindDrop) {
            value += DOT_SPACER;
            if (entity instanceof Infantry) {
                value += Messages.getString("ChatLounge.0"); 
            } else if (entity instanceof Protomech) {
                value += Messages.getString("ChatLounge.1"); 
            } else if (entity instanceof GunEmplacement) {
                value += Messages.getString("ChatLounge.2"); 
            } else {
                value += entity.getWeightClassName();
                if (entity instanceof Tank) {
                    value += Messages.getString("ChatLounge.6"); 
                }
            }
            value += DOT_SPACER;
            return value;
        } 
        
        //DEBUG
            value += "Carried:" + entity.getTransportId();
            value += " Carrying:";
            for (Entity carriedUnit: entity.getLoadedUnits()) {
                value += carriedUnit.getId()+";";
            } 

        boolean hasWarning = false;
        boolean hasCritical = false;
        // General (Yellow) Warnings
        if (((entity.hasC3i() || entity.hasNavalC3()) && (entity.calculateFreeC3Nodes() == 5))
                || ((entity.getC3Master() == null) && entity.hasC3S())
                ) {
            value += UIUtil.guiScaledFontHTML(UIUtil.uiYellow()); 
            value += WARNING_SIGN + "</FONT>";
            hasWarning = true;
        }

        // Critical (Red) Warnings
        if ((entity.getGame().getPlanetaryConditions().whyDoomed(entity, entity.getGame()) != null)
                || (entity.doomedInAtmosphere() && mapType == MapSettings.MEDIUM_ATMOSPHERE)
                || (entity.doomedOnGround() && mapType == MapSettings.MEDIUM_GROUND)
                || (entity.doomedInSpace() && mapType == MapSettings.MEDIUM_SPACE)
                || (!entity.isDesignValid())
                ) {
            value += UIUtil.guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor()); 
            value += WARNING_SIGN + "</FONT>";
            hasCritical = true;
        }
        
        // Unit Name
        if (hasCritical) {
            value += UIUtil.guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor());
        } else if (hasWarning) {
            value += UIUtil.guiScaledFontHTML(UIUtil.uiYellow());
        } else {
            value += UIUtil.guiScaledFontHTML();
        }
        value += "<B>" + entity.getShortNameRaw() + "</B></FONT><BR>";

        // SECOND LINE----
        // Tonnage
        value += UIUtil.guiScaledFontHTML();
        value += Math.round(entity.getWeight()) + Messages.getString("ChatLounge.Tons");
        value += "</FONT>";

        // Invalid Design
        if (!entity.isDesignValid()) {
            value += DOT_SPACER;
            value += Messages.getString("ChatLounge.invalidDesign");
        }
        
        // Quirk Count
        int quirkCount = entity.countQuirks() + entity.countWeaponQuirks();
        if (quirkCount > 0) {
            value += DOT_SPACER;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiQuirksColor());
            value += Messages.getString("ChatLounge.Quirks") + ": ";
            
            int posQuirks = entity.countQuirks(Quirks.POS_QUIRKS);
            String pos = posQuirks > 0 ? "+" + posQuirks : ""; 
            int negQuirks = entity.countQuirks(Quirks.NEG_QUIRKS);
            String neg = negQuirks > 0 ? "-" + negQuirks : "";
            int wpQuirks = entity.countWeaponQuirks();
            String wpq = wpQuirks > 0 ? "W" + wpQuirks : "";
            value += UIUtil.arrangeInLine(" / ", pos, neg, wpq);
            
            value += "</FONT>";
        }

        // Partial Repairs
        int partRepCount = entity.countPartialRepairs();
        if ((partRepCount > 0)) {
            value += DOT_SPACER;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiLightRed());
            value += Messages.getString("ChatLounge.PartialRepairs");
            value += "</FONT>";
        }
        value += "<BR>";
        
        // THIRD LINE----
        
        // Controls the separator dot character
        boolean subsequentElement = false;

        // C3 ...
        if (entity.hasC3i()) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiC3Color());
            if (entity.calculateFreeC3Nodes() >= 5) {
                value += "C3i" + UNCONNECTED_SIGN;
            } else {
                value += "C3i" + CONNECTED_SIGN + entity.getC3NetId();
                if (entity.calculateFreeC3Nodes() > 0) {
                    value += Messages.getString("ChatLounge.C3iNodes", entity.calculateFreeC3Nodes());
                }
            }
            value += "</FONT>";
        } 
        
        if (entity.hasNavalC3()) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiC3Color());
            if (entity.calculateFreeC3Nodes() >= 5) {
                value += "NC3" + UNCONNECTED_SIGN;
            } else {
                value += "NC3" + CONNECTED_SIGN + entity.getC3NetId();
                if (entity.calculateFreeC3Nodes() > 0) {
                    value += Messages.getString("ChatLounge.C3iNodes", entity.calculateFreeC3Nodes());
                }
            }
            value += "</FONT>";
        } 
        
        if (entity.hasC3()) {
            
            if (entity.getC3Master() == null) {
                if (entity.hasC3S()) {
                    if (subsequentElement) {
                        value += DOT_SPACER;
                    }
                    subsequentElement = true;
                    value += UIUtil.guiScaledFontHTML(UIUtil.uiC3Color()) + "C3 Slave" + UNCONNECTED_SIGN;
                    value += "</FONT>";
                } 
                
                if (entity.hasC3M()) {
                    if (subsequentElement) {
                        value += DOT_SPACER;
                    }
                    subsequentElement = true;

                    value += UIUtil.guiScaledFontHTML(UIUtil.uiC3Color()) + "C3 Master";
                    int freeS = entity.calculateFreeC3Nodes();
                    if (freeS == 0) {
                        value += " (full)";
                    } else {
                        value += Messages.getString("ChatLounge.C3SNodes", entity.calculateFreeC3Nodes());
                    }
                    value += "</FONT>";
                }
            } else if (entity.C3MasterIs(entity)) {
                if (subsequentElement) {
                    value += DOT_SPACER;
                }
                subsequentElement = true;
                value += UIUtil.guiScaledFontHTML(UIUtil.uiC3Color()) + "C3 Company Commander";
                if (entity.hasC3MM()) {
                    value += MessageFormat.format(" ({0}M, {1}S free)", entity.calculateFreeC3MNodes(), entity.calculateFreeC3Nodes());
                } else {
                    value += Messages.getString("ChatLounge.C3MNodes", entity.calculateFreeC3MNodes());
                }
                value += "</FONT>";
            } else {
                if (subsequentElement) {
                    value += DOT_SPACER;
                }
                subsequentElement = true;
                value += UIUtil.guiScaledFontHTML(UIUtil.uiC3Color());
                if (entity.hasC3S()) {
                    value += "C3 Slave" + CONNECTED_SIGN; 
                } else {
                    value += "C3 Master";
                    int freeS = entity.calculateFreeC3Nodes();
                    if (freeS == 0) {
                        value += " (full)";
                    } else {
                        value += Messages.getString("ChatLounge.C3SNodes", entity.calculateFreeC3Nodes());
                    }
                    value += CONNECTED_SIGN + "(CC) "; 
                }
                value += entity.getC3Master().getChassis();
                value += "</FONT>";
            }
        }

        // Loaded onto transport
        if (entity.getTransportId() != Entity.NONE) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            Entity loader = entity.getGame().getEntity(entity.getTransportId());
            value += UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + LOADED_SIGN;
            value += "<I> aboard " + loader.getChassis() + "</I></FONT>";
        }
        
        if (entity.isHidden() && mapType == MapSettings.MEDIUM_GROUND) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>";
            value += Messages.getString("ChatLounge.compact.hidden") + "</I></FONT>";
        }
        
        if (entity.isHullDown()) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>";
            value += Messages.getString("ChatLounge.hulldown") + "</I></FONT>";
        }
        
        if (entity.isProne()) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>";
            value += Messages.getString("ChatLounge.prone") + "</I></FONT>";
        }

        if (entity.isOffBoard()) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>";
            value += Messages.getString("ChatLounge.deploysOffBoard") + "</I></FONT>"; 
        } else if (entity.getDeployRound() > 0) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>";
            value += Messages.getString("ChatLounge.deploysAfterRound", entity.getDeployRound()); 
            if (entity.getStartingPos(false) != Board.START_NONE) {
                value += Messages.getString("ChatLounge.deploysAfterZone", 
                        IStartingPositions.START_LOCATION_NAMES[entity.getStartingPos(false)]);
            }
            value += "</I></FONT>";
        }
        
        // Starting values for Altitude / Velocity / Elevation
        if (entity.isAero()) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>"; 
            Aero aero = (Aero) entity;
            value += Messages.getString("ChatLounge.compact.velocity") + ": ";
            value += aero.getCurrentVelocity();
            if (mapType != MapSettings.MEDIUM_SPACE) {
                value += ", " + Messages.getString("ChatLounge.compact.altitude") + ": ";
                value += aero.getAltitude();
            } 
            if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_FUEL_CONSUMPTION)) {
                value += ", " + Messages.getString("ChatLounge.compact.fuel") + ": ";
                value += aero.getCurrentFuel();
            }
            value += "</I></FONT>";
        } else if ((entity.getElevation() != 0) || (entity instanceof VTOL)) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            value += guiScaledFontHTML(uiGreen()) + "<I>";
            value += Messages.getString("ChatLounge.compact.elevation") + ": ";
            value += entity.getElevation() + "</I></FONT>";
        }
        
        return value;
    }

    /** 
     * Creates and returns the display content of the Pilot column for the given entity and
     * for the compact display mode.
     * When blindDrop is true, the pilot details are not given.
     */
    static String formatPilotCompact(Crew pilot, boolean blindDrop, boolean rpgSkills) {
        StringBuilder result = new StringBuilder("<HTML><NOBR>");
        result.append(guiScaledFontHTML());
        
        if (blindDrop) {
            result.append(Messages.getString("ChatLounge.Unknown"));
            return result.toString();
        } 

        if (pilot.getSlotCount() > 1) {
            result.append("<I>Multiple Crewmembers</I>");
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
        int advs = pilot.countOptions();
        if (advs > 0) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiQuirksColor()));
            String msg = "ChatLounge.compact." + (advs == 1 ? "advantage" : "advantages");
            result.append(pilot.countOptions() + Messages.getString(msg));
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
        
        result.append(UIUtil.guiScaledFontHTML(overallScale));
        
        if (blindDrop) {
            result.append("<B>" + Messages.getString("ChatLounge.Unknown") + "</B>");
            return result.toString();
        } 
        
        if (crew.getSlotCount() == 1) { // Single-person crew
            if (crew.isMissing(0)) {
                result.append("<B>No " + crew.getCrewType().getRoleName(0) + "</B>");
            } else {
                if ((crew.getNickname(0) != null) && !crew.getNickname(0).isEmpty()) {
                    result.append(UIUtil.guiScaledFontHTML(UIUtil.uiNickColor(), overallScale)); 
                    result.append("<B>'" + crew.getNickname(0).toUpperCase() + "'</B></FONT>");
                } else {
                    result.append("<B>" + crew.getDesc(0) + "</B>");
                }
            }
            result.append("<BR>");
        } else { // Multi-person crew
            result.append("<I><B>Multiple Crewmembers</B></I>");
            result.append("<BR>");
        }
        result.append(CrewSkillSummaryUtil.getSkillNames(entity) + ": ");
        result.append("<B>" + crew.getSkillsAsString(rpgSkills) + "</B><BR>");
        
        // Advantages, MD, Edge
        if (crew.countOptions() > 0) {
            result.append(UIUtil.guiScaledFontHTML(UIUtil.uiQuirksColor(), overallScale));
            result.append(Messages.getString("ChatLounge.abilities"));
            result.append(" " + crew.countOptions());
            result.append("</FONT>");
        }
        result.append("</FONT>");
        return result.toString();
    }
    
}

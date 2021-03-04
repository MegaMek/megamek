package megamek.client.ui.swing.lobby;

import static megamek.client.ui.swing.lobby.MekTableModel.DOT_SPACER;
import static megamek.client.ui.swing.util.UIUtil.*;

import java.awt.Color;
import java.text.MessageFormat;
import java.util.ArrayList;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Aero;
import megamek.common.Board;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.FighterSquadron;
import megamek.common.GunEmplacement;
import megamek.common.IGame;
import megamek.common.IPlayer;
import megamek.common.IStartingPositions;
import megamek.common.Infantry;
import megamek.common.MapSettings;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.UnitType;
import megamek.common.VTOL;
import megamek.common.force.Force;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.CollectionUtil;
import megamek.common.util.CrewSkillSummaryUtil;

public class MekForceTreeCellFormatter {
    
    /** 
     * Creates and returns the display content of the C3-MekTree cell for the given entity and 
     * for the compact display mode. Assumes that no enemy or blind-drop-hidden units are provided. 
     */
    static String formatForceCompact(Force force, ChatLounge lobby) {
        Client client = lobby.getClientgui().getClient();
        IGame game = client.getGame();
        IPlayer localPlayer = client.getLocalPlayer();
        int ownerId = game.getForces().getOwnerId(force);
        IPlayer owner = game.getPlayer(ownerId);
        
        // Get the my / ally / enemy color and desaturate it
        Color color = GUIPreferences.getInstance().getEnemyUnitColor();
        if (ownerId == localPlayer.getId()) {
            color = GUIPreferences.getInstance().getMyUnitColor();
        } else if (!localPlayer.isEnemyOf(owner)) {
            color = GUIPreferences.getInstance().getAllyUnitColor();
        }
        color = addGray(color, 128).brighter();

        StringBuilder result = new StringBuilder("<HTML><NOBR>");
        result.append(guiScaledFontHTML(color, 0.2f)).append("");
        
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
            result.append(guiScaledFontHTML(uiGray(), 0.2f));
            result.append(" [").append(force.getId()).append("]</FONT>");
        }
        
        // Owner
        if (game.getForces().getOwnerId(force) != client.getLocalPlayerNumber()) {
            result.append(guiScaledFontHTML(0.2f));
            result.append(DOT_SPACER).append("</FONT>");
            result.append(guiScaledFontHTML(owner.getColour().getColour(), 0.2f));
            result.append("\u2691 ");
            result.append(owner.getName()).append("</FONT>");
        }
        
        // BV
        ArrayList<Entity> fullEntities = lobby.game().getForces().getFullEntities(force);
        result.append(guiScaledFontHTML(color));
        result.append(DOT_SPACER);
        int totalBv = fullEntities.stream().mapToInt(e -> e.getInitialBV()).sum();
        if (totalBv > 0) {
            result.append("BV ");
            result.append(String.format("%,d", totalBv));
            // Unit Type
            long unittypes = fullEntities.stream().mapToLong(e -> e.getEntityType()).distinct().count();
            result.append(guiScaledFontHTML(color));
            result.append(DOT_SPACER);
            if (unittypes > 1) {
                result.append(" Mixed");
            } else if (unittypes == 1) {
                Entity entity = CollectionUtil.randomElement(fullEntities);
                result.append(UnitType.getTypeName(entity.getUnitType()));
            }
        } else {
            result.append("Empty");
        }
        result.append("</FONT>");

        
        return result.toString();
    }
    
    /** 
     * Creates and returns the display content of the C3-MekTree cell for the given entity and 
     * for the compact display mode. Assumes that no enemy or blind-drop-hidden units are provided. 
     */
    static String formatForceFull(Force force, ChatLounge lobby) {
        Client client = lobby.getClientgui().getClient();
        IGame game = client.getGame();
        float size = 0.5f;

        StringBuilder result = new StringBuilder("<HTML><NOBR>&nbsp;&nbsp;");
        result.append(guiScaledFontHTML(size));
        
        result.append(guiScaledFontHTML(uiQuirksColor(), size));
        result.append(force.getName()).append("</FONT>");
        
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            result.append(guiScaledFontHTML(uiGray(), size));
            result.append(" [").append(force.getId()).append("]</FONT>");
        }
        
//        IPlayer owner = lobby.game().getPlayer(game.getForces().getOwner(force));
//        if (!game.getForces().getOwner(force).equals(client.getLocalPlayer())) {
        if (game.getForces().getOwnerId(force) != client.getLocalPlayerNumber()) {
            IPlayer owner = game.getPlayer(game.getForces().getOwnerId(force));
            result.append(DOT_SPACER);
            result.append(guiScaledFontHTML(owner.getColour().getColour(), size));
            result.append(owner.getName()).append("</FONT>");
        }
        
//            result.append(guiScaledFontHTML(uiGray(), size));
//            result.append("<BR>&nbsp;&nbsp; Total number of units: ");
//            List<Force> allsubforces = game.getForces().getFullSubForces(force);
//            int numUnits = force.entityCount() + allsubforces.stream().mapToInt(f -> f.entityCount()).sum();
//            result.append(numUnits);
            
//            result.append("<BR>&nbsp;&nbsp;");
            
        return result.toString();
    }

    /** 
     * Creates and returns the display content of the C3-MekTree cell for the given entity and 
     * for the compact display mode. Assumes that no enemy or blind-drop-hidden units are provided. 
     */
    static String formatUnitCompact(Entity entity, ChatLounge lobby) {
        Client client = lobby.getClientgui().getClient();
        IGame game = client.getGame();
        IPlayer localPlayer = client.getLocalPlayer();
        IPlayer owner = entity.getOwner();
        boolean hideEntity = owner.isEnemyOf(localPlayer)
                && game.getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP);
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
        
        Color color = GUIPreferences.getInstance().getEnemyUnitColor();
        if (owner.getId() == localPlayer.getId()) {
            color = GUIPreferences.getInstance().getMyUnitColor();
        } else if (!localPlayer.isEnemyOf(owner)) {
            color = GUIPreferences.getInstance().getAllyUnitColor();
        }
        color = addGray(color, 128).brighter();

        if (entity.getForceId() == Force.NO_FORCE) {
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
        if (isCarried) {
            result.append(guiScaledFontHTML(uiGreen()) + LOADED_SIGN + "</FONT>");
        }

        // Unit name
        result.append(entity.getShortNameRaw());

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
        
        final boolean rpgSkills = game.getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY);
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


        // Invalid unit design
        if (!entity.isDesignValid()) {
            result.append(DOT_SPACER + guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor()));
            result.append(Messages.getString("ChatLounge.invalidDesign"));
            result.append("</FONT>");
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
            String c3Name = entity.hasC3i() ? "C3i" : "NC3";
            if (entity.calculateFreeC3Nodes() >= 5) {
                result.append(c3Name + UNCONNECTED_SIGN);
            } else {
                result.append(c3Name + CONNECTED_SIGN + entity.getC3NetId());
            }
            result.append("</FONT>");
        } 

        if (entity.hasC3()) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiC3Color()));
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
                Aero aero = (Aero) entity;
                result.append(DOT_SPACER + guiScaledFontHTML(uiGreen()) + "<I>"); 
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
        }
        
        // Starting heat
        if (entity.getHeat() != 0 && entity.tracksHeat()) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiGreen())); 
            result.append("<I>Heat: ").append(entity.getHeat()).append(" </I></FONT>");
        }
        
        result.append("</FONT>");
        
        result.append(guiScaledFontHTML(uiGreen(), 0.3f));
        result.append("&nbsp;  \u24D8");
        
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

package megamek.client.ui.swing.lobby;

import megamek.MegaMek;
import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.calculationReport.FlexibleCalculationReport;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;

import java.text.MessageFormat;
import java.text.NumberFormat;

import static megamek.client.ui.Messages.getString;
import static megamek.client.ui.swing.lobby.MekTableModel.DOT_SPACER;
import static megamek.client.ui.swing.util.UIUtil.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;

public class MekTableASUnitEntry {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    /**
     * Creates and returns the display content of the Unit column for the given AlphaStrikeElement and
     * for the non-compact display mode.
     * When blindDrop is true, the unit details are not given.
     */
    static String fullEntry(AlphaStrikeElement element, ChatLounge lobby, boolean forceView, boolean compactView) {
        StringBuilder result = new StringBuilder("<HTML><NOBR>" + guiScaledFontHTML());

        Client client = lobby.getClientgui().getClient();
        Game game = client.getGame();
        GameOptions options = game.getOptions();
        Player localPlayer = client.getLocalPlayer();
        Player owner = game.getPlayer(element.getOwnerId());
        boolean hideEntity = owner.isEnemyOf(localPlayer)
                && options.booleanOption(OptionsConstants.BASE_BLIND_DROP);

        if (hideEntity) {
            result.append(DOT_SPACER);
            if (element.isInfantry()) {
                result.append(Messages.getString("ChatLounge.0"));
            } else if (element.isProtoMek()) {
                result.append(Messages.getString("ChatLounge.1"));
            } else if (element.isFighter()) {
                result.append(Messages.getString("ChatLounge.4"));
            } else if (element.isMek()) {
                result.append(Messages.getString("ChatLounge.3"));
            } else if (element.isVehicle()) {
                result.append(Messages.getString("ChatLounge.6"));
            } else {
                result.append("SZ ").append(element.getSize()).append(" ").append(element.getASUnitType());
            }
            result.append(DOT_SPACER);
            return result.toString();
        }

//        boolean isCarried = element.getTransportId() != Entity.NONE;
        boolean hasWarning = false;
        boolean hasCritical = false;
        int mapType = lobby.mapSettings.getMedium();

        // First line
//        if (LobbyUtility.hasYellowWarning(element)) {
//            result.append(guiScaledFontHTML(uiYellow()));
//            result.append(WARNING_SIGN + "</FONT>");
//            hasWarning = true;
//        }

        // Critical (Red) Warnings
//        if ((element.getGame().getPlanetaryConditions().whyDoomed(element, element.getGame()) != null)
//                || (element.doomedInAtmosphere() && mapType == MapSettings.MEDIUM_ATMOSPHERE)
//                || (element.doomedOnGround() && mapType == MapSettings.MEDIUM_GROUND)
//                || (element.doomedInSpace() && mapType == MapSettings.MEDIUM_SPACE)
//                || (!element.isDesignValid())) {
//            result.append(guiScaledFontHTML(GUIP.getWarningColor()));
//            result.append(WARNING_SIGN + "</FONT>");
//            hasCritical = true;
//        }

        // Unit Name
        if (hasCritical) {
            result.append(guiScaledFontHTML(GUIP.getWarningColor()));
        } else if (hasWarning) {
            result.append(guiScaledFontHTML(uiYellow()));
        } else {
            result.append(guiScaledFontHTML(uiLightGreen()));
        }
        result.append("<B>").append(element.getName()).append("</B></FONT>");

        // ID
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            result.append(guiScaledFontHTML(uiGray()));
            result.append(" [ID: ").append(element.getId()).append("]</FONT>");
        }
        if (!forceView && !compactView) {
            result.append( "<BR>");
        } else {
            result.append(DOT_SPACER);
        }

        // Tonnage
        result.append(guiScaledFontHTML());
        if (forceView) {
            result.append(DOT_SPACER);
        }
        result.append(element.getASUnitType());
        result.append(DOT_SPACER);
        result.append("SZ ").append(element.getSize());
        result.append("</FONT>");
        result.append(DOT_SPACER);
        result.append("MV ").append(element.getMovementAsString());
        if (element.usesTMM()) {
            result.append(DOT_SPACER);
            result.append("TMM ").append(element.getTMM());
        }
        if (!element.usesArcs()) {
            result.append(DOT_SPACER);
            result.append("DMG ").append(element.getStandardDamage());
        }
        if (element.hasOV()) {
            result.append(DOT_SPACER);
            result.append("OV ").append(element.getOV());
        }
        if (element.getRole().hasRole()) {
            result.append(DOT_SPACER);
            result.append(element.getRole());
        }

        // Invalid Design
//        if (!forceView) {
//            if (!element.isDesignValid()) {
//                result.append(DOT_SPACER);
//                result.append(guiScaledFontHTML(GUIP.getWarningColor()));
//                result.append("\u26D4 </FONT>").append(Messages.getString("ChatLounge.invalidDesign"));
//            }
//        }

        // ECM
        if (element.hasAnySUAOf(ECM, LECM, AECM)) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiC3Color()));
            result.append(ECM_SIGN + " ");
            result.append(Messages.getString("BoardView1.ecmSource"));
            result.append("</FONT>");
        }

        // Quirk Count
//        int quirkCount = element.countQuirks() + element.countWeaponQuirks();
//        if (quirkCount > 0) {
//            result.append(DOT_SPACER);
//            result.append(guiScaledFontHTML(uiQuirksColor()));
//            result.append(QUIRKS_SIGN);
//            result.append(Messages.getString("ChatLounge.Quirks"));
//            result.append("</FONT>");
//        }

        // Pilot
//        if (forceView) {
//            Crew pilot = element.getCrew();
//            result.append(guiScaledFontHTML());
//            result.append(DOT_SPACER);
//
//            if (pilot.getSlotCount() > 1 || element instanceof FighterSquadron) {
//                result.append("<I>" + Messages.getString("ChatLounge.multipleCrew") + "</I>");
//            } else if ((pilot.getNickname(0) != null) && !pilot.getNickname(0).isEmpty()) {
//                result.append(guiScaledFontHTML(uiNickColor()) + "<B>'");
//                result.append(pilot.getNickname(0).toUpperCase() + "'</B></FONT>");
//                if (!pilot.getStatusDesc(0).isEmpty()) {
//                    result.append(" (" + pilot.getStatusDesc(0) + ")");
//                }
//            } else {
//                result.append(pilot.getDesc(0));
//            }
//
//            final boolean rpgSkills = options.booleanOption(OptionsConstants.RPG_RPG_GUNNERY);
//            result.append(" (" + pilot.getSkillsAsString(rpgSkills) + ")");
//            if (pilot.countOptions() > 0) {
//                result.append(DOT_SPACER + guiScaledFontHTML(uiQuirksColor()));
//                result.append(Messages.getString("ChatLounge.abilities"));
//            }
//
//            // Owner
//            if (!localPlayer.equals(owner)) {
//                result.append(DOT_SPACER);
//                result.append(guiScaledFontHTML(owner.getColour().getColour()));
//                result.append("\u2691 ");
//                result.append(element.getOwner().getName()).append("</FONT>");
//            }
//
//            // Info sign (i)
//            result.append(guiScaledFontHTML(uiGreen()));
//            result.append("&nbsp;  \u24D8</FONT>");
//        }

        // SECOND OR THIRD LINE in Force View / Table
        if (!compactView) {
            result.append("<BR>");
        } else {
            result.append(DOT_SPACER);
        }

        if (element.usesArcs() || compactView) {
            result.append("\u25cf ").append(element.getCurrentStructure());
            result.append(" \u25cb ").append(element.getCurrentArmor());
        } else {
            result.append(UIUtil.repeat("\u25cf", element.getCurrentStructure()));
            result.append(UIUtil.repeat("\u25cb", element.getCurrentArmor()));
        }

        if (element.usesThreshold()) {
            result.append(DOT_SPACER);
            result.append(" TH ");
            result.append(element.getThreshold());
        }

        String specials = element.getSpecialsDisplayString(element);
        if (!specials.isBlank()) {
            result.append(DOT_SPACER);
            result.append(" ").append(specials);
        }

        // Controls the separator dot character
        boolean firstEntry = true;

        // Start Position
//        int sp = element.getStartingPos(true);
//        int spe = element.getStartingPos(false);
//        if ((!element.isOffBoard())
//                && (sp >= 0)
//                && (sp < IStartingPositions.START_LOCATION_NAMES.length)) {
//            firstEntry = dotSpacer(result, firstEntry);
//            if (spe != Board.START_NONE) {
//                result.append(guiScaledFontHTML(uiLightGreen()));
//            }
//            String msg_start = Messages.getString("ChatLounge.Start");
//            result.append(" " + msg_start + ":" + IStartingPositions.START_LOCATION_NAMES[sp]);
//            int so = element.getStartingOffset(true);
//            int sw = element.getStartingWidth(true);
//            if ((so != 0) || (sw != 3)) {
//                result.append(", " + so);
//                result.append(", " + sw);
//            }
//            if (spe != Board.START_NONE) {
//                result.append("</FONT>");
//            }
//        }

        // Invalid Design
//        if (forceView) {
//            if (!element.isDesignValid()) {
//                firstEntry = dotSpacer(result, firstEntry);
//                result.append(guiScaledFontHTML(GUIP.getWarningColor()));
//                result.append("\u26D4 </FONT>").append(Messages.getString("ChatLounge.invalidDesign"));
//            }
//        }

        // C3 ...
        if (element.hasSUA(C3I)) {
//            firstEntry = dotSpacer(result, firstEntry);
//            result.append(guiScaledFontHTML(uiC3Color()));
//            if (element.calculateFreeC3Nodes() >= 5) {
//                result.append("C3i" + UNCONNECTED_SIGN);
//            } else {
//                result.append("C3i" + CONNECTED_SIGN + element.getC3NetId());
//                if (element.calculateFreeC3Nodes() > 0) {
//                    result.append(Messages.getString("ChatLounge.C3iNodes", element.calculateFreeC3Nodes()));
//                }
//            }
//            result.append("</FONT>");
        }

//        if (element.hasNavalC3()) {
//            firstEntry = dotSpacer(result, firstEntry);
//            result.append(guiScaledFontHTML(uiC3Color()));
//            if (element.calculateFreeC3Nodes() >= 5) {
//                result.append("NC3" + UNCONNECTED_SIGN);
//            } else {
//                result.append("NC3" + CONNECTED_SIGN + element.getC3NetId());
//                if (element.calculateFreeC3Nodes() > 0) {
//                    result.append(Messages.getString("ChatLounge.C3iNodes", element.calculateFreeC3Nodes()));
//                }
//            }
//            result.append("</FONT>");
//        }

//        if (element.hasC3()) {
//            if (element.getC3Master() == null) {
//                if (element.hasC3S()) {
//                    firstEntry = dotSpacer(result, firstEntry);
//                    result.append(guiScaledFontHTML(uiC3Color()) + Messages.getString("ChatLounge.C3S") + UNCONNECTED_SIGN);
//                    result.append("</FONT>");
//                }
//
//                if (element.hasC3M()) {
//                    firstEntry = dotSpacer(result, firstEntry);
//                    result.append(guiScaledFontHTML(uiC3Color()) + Messages.getString("ChatLounge.C3Master"));
//                    int freeS = element.calculateFreeC3Nodes();
//                    if (freeS == 0) {
//                        result.append(" (full)");
//                    } else {
//                        result.append(Messages.getString("ChatLounge.C3SNodes", element.calculateFreeC3Nodes()));
//                    }
//                    result.append("</FONT>");
//                }
//            } else if (element.C3MasterIs(element)) {
//                firstEntry = dotSpacer(result, firstEntry);
//                result.append(guiScaledFontHTML(uiC3Color()) + Messages.getString("ChatLounge.C3CC"));
//                if (element.hasC3MM()) {
//                    String msg_freec3mnodes = Messages.getString("ChatLounge.FreeC3MNodes");
//                    result.append(MessageFormat.format(" " + msg_freec3mnodes,
//                            element.calculateFreeC3MNodes(), element.calculateFreeC3Nodes()));
//                } else {
//                    result.append(getString("ChatLounge.C3MNodes", element.calculateFreeC3MNodes()));
//                }
//                result.append("</FONT>");
//            } else {
//                firstEntry = dotSpacer(result, firstEntry);
//                result.append(guiScaledFontHTML(uiC3Color()));
//                if (element.hasC3S()) {
//                    result.append(getString("ChatLounge.C3S") + CONNECTED_SIGN);
//                } else {
//                    result.append(getString("ChatLounge.C3Master"));
//                    int freeS = element.calculateFreeC3Nodes();
//                    if (freeS == 0) {
//                        result.append(getString("ChatLounge.C3full"));
//                    } else {
//                        result.append(getString("ChatLounge.C3SNodes", element.calculateFreeC3Nodes()));
//                    }
//                    result.append(CONNECTED_SIGN + "(CC) ");
//                }
//                result.append(element.getC3Master().getChassis());
//                result.append("</FONT>");
//            }
//        }

        // Loaded onto transport
//        result.append(guiScaledFontHTML(uiGreen()));
//        if (isCarried) {
//            firstEntry = dotSpacer(result, firstEntry);
//            Entity loader = element.getGame().getEntity(element.getTransportId());
//            result.append(guiScaledFontHTML(uiGreen()) + LOADED_SIGN);
//            result.append("<I> " + Messages.getString("ChatLounge.aboard") + " " + loader.getChassis());
//            if (PreferenceManager.getClientPreferences().getShowUnitId()) {
//                result.append(" [" + element.getTransportId() + "]");
//            }
//            result.append("</I></FONT>");
//
//        } else { // Hide deployment info when a unit is carried
//            if (element.isHidden() && mapType == MapSettings.MEDIUM_GROUND) {
//                firstEntry = dotSpacer(result, firstEntry);
//                result.append(getString("ChatLounge.compact.hidden"));
//            }
//
//            if (element.isHullDown()) {
//                firstEntry = dotSpacer(result, firstEntry);
//                result.append(getString("ChatLounge.hulldown"));
//            }
//
//            if (element.isProne()) {
//                firstEntry = dotSpacer(result, firstEntry);
//                result.append(getString("ChatLounge.prone"));
//            }
//        }

//        if (element.isOffBoard()) {
//            firstEntry = dotSpacer(result, firstEntry);
//            result.append(getString("ChatLounge.deploysOffBoard"));
//            result.append(",  " + element.getOffBoardDirection());
//            result.append(", " + element.getOffBoardDistance());
//        } else if (element.getDeployRound() > 0) {
//            firstEntry = dotSpacer(result, firstEntry);
//            result.append(getString("ChatLounge.deploysAfterRound", element.getDeployRound()));
//        }
//        result.append("</FONT>");

        // Starting heat
//        if (element.getHeat() != 0 && element.tracksHeat()) {
//            firstEntry = dotSpacer(result, firstEntry);
//            result.append(guiScaledFontHTML(uiLightRed()));
//            result.append(element.getHeat()).append(" Heat").append("</FONT>");
//        }

        // Starting values for Altitude / Velocity / Elevation
//        if (!isCarried) {
//            if (element.isAero()) {
//                IAero aero = (IAero) element;
//                firstEntry = dotSpacer(result, firstEntry);
//                result.append(guiScaledFontHTML(uiGreen()) + "<I>");
//                result.append(Messages.getString("ChatLounge.compact.velocity") + ": ");
//                result.append(aero.getCurrentVelocity());
//                if (mapType != MapSettings.MEDIUM_SPACE) {
//                    result.append(", " + Messages.getString("ChatLounge.compact.altitude") + ": ");
//                    result.append(aero.getAltitude());
//                }
//                if (options.booleanOption(OptionsConstants.ADVAERORULES_FUEL_CONSUMPTION)) {
//                    result.append(", " + Messages.getString("ChatLounge.compact.fuel") + ": ");
//                    result.append(aero.getCurrentFuel());
//                }
//                result.append("</I></FONT>");
//            } else if ((element.getElevation() != 0) || (element instanceof VTOL)) {
//                firstEntry = dotSpacer(result, firstEntry);
//                result.append(guiScaledFontHTML(uiGreen()) + "<I>");
//                result.append(Messages.getString("ChatLounge.compact.elevation") + ": ");
//                result.append(element.getElevation() + "</I></FONT>");
//            }
//        }

        // Auto Eject        String msg_autoejectdisabled = Messages.getString("ChatLounge.AutoEjectDisabled");
        ////        if (element instanceof Mech) {
        ////            Mech mech = ((Mech) element);
        ////            if ((mech.hasEjectSeat()) && (!mech.isAutoEject())) {
        ////                firstEntry = dotSpacer(result, firstEntry);
        ////                result.append(guiScaledFontHTML(uiYellow()));
        ////                result.append(WARNING_SIGN + "\u23CF<I>");
        ////                result.append(msg_autoejectdisabled);
        ////                result.append("</I></FONT>");
        ////            }
        ////        }
        ////        if ((element instanceof Aero)
        ////                && (!(element instanceof Jumpship))
        ////                && (!(element instanceof SmallCraft))) {
        ////            Aero aero = ((Aero) element);
        ////            if ((aero.hasEjectSeat())
        ////                    && (!aero.isAutoEject())) {
        ////                firstEntry = dotSpacer(result, firstEntry);
        ////                result.append(guiScaledFontHTML(uiYellow()));
        ////                result.append(WARNING_SIGN + "\u23CF<I>");
        ////                result.append(msg_autoejectdisabled);
        ////                result.append("</I></FONT>");
        ////            }
        ////        }
//

        return result.toString();
    }

    private MekTableASUnitEntry() { }
}

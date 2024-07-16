/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.*;
import megamek.common.force.Forces;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.common.strategicBattleSystems.SBFMovePath;
import megamek.common.strategicBattleSystems.SBFTurn;
import megamek.common.util.ImageUtil;
import org.apache.logging.log4j.LogManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is the game client for Strategic BattleForce games, as one would think.
 */
public class SBFClient extends AbstractClient {

    /**
     * The game object that holds all game information. This object is persistent, i.e. it is never replaced
     * by another game object sent from the server. Instead, the info in this game object is added to
     * or replaced as necessary. Therefore, other objects may keep a reference to this game object. Other
     * objects however, like the players or units, *will* be replaced by objects sent from the server.
     */
    private final SBFGame game = new SBFGame();

    /**
     * Construct a client which will try to connect. If the connection fails, it
     * will alert the player, free resources and hide the frame.
     *
     * @param name the player name for this client
     * @param host the hostname
     * @param port the host port
     */
    public SBFClient(String name, String host, int port) {
        super(name, host, port);
    }

    @Override
    public SBFGame getGame() {
        return game;
    }

    @Override
    public boolean isMyTurn() {
        return (game.getTurn() != null) && game.getTurn().isValid(localPlayerNumber, game);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected boolean handleGameSpecificPacket(Packet packet) {
        LogManager.getLogger().info("Player {} received packet: {}", localPlayerNumber, packet);
        switch (packet.getCommand()) {
            case SENDING_ENTITIES:
                receiveEntities(packet);
                break;
            case SENDING_REPORTS_ALL:
                var receivedReports = (Map<Integer, List<Report>>) packet.getObject(0);
                game.replaceAllReports(receivedReports);
                if (keepGameLog()) {
                    // Re-write the gamelog from scratch
                    initGameLog();
                    for (int round : receivedReports.keySet().stream().sorted().collect(Collectors.toList())) {
                        possiblyWriteToLog(assembleAndAddImages(receivedReports.get(round)));
                    }
                }
                roundReport = assembleAndAddImages(receivedReports.get(game.getCurrentRound()));
                // We don't really have a copy of the phase report at this point, so I guess we'll just use the
                // round report until the next phase actually completes.
                phaseReport = roundReport;
                break;
            case SENDING_REPORTS:
                phaseReport = assembleAndAddImages((List<Report>) packet.getObject(0));
                if (keepGameLog()) {
                    if ((log == null) && (game.getCurrentRound() == 1)) {
                        initGameLog();
                    }
                    if (log != null) {
                        //TODO
//                        log.append(phaseReport);
                    }
                }
                game.addReports((List<Report>) packet.getObject(0));
                roundReport = assembleAndAddImages(game.getGameReport().get(game.getCurrentRound()));
                break;
            case SENDING_TURNS:
                game.setTurns((List<SBFTurn>) packet.getObject(0));
                break;
            case TURN:
                game.setTurnIndex(packet.getIntValue(0), packet.getIntValue(1));
                break;
            case ENTITY_UPDATE:
                getGame().receiveUnit((InGameObject) packet.getObject(0));
                break;
            case UNIT_INVISIBLE:
                getGame().forget((int) packet.getObject(0));
            default:
                return false;
        }
        return true;
    }

    private String assembleAndAddImages(List<Report> reports) {
        if (reports == null) {
            LogManager.getLogger().error("Received a null list of reports!");
            return "";
        }

        StringBuilder assembledReport = new StringBuilder();
        for (Report report : reports) {
            if (report != null) {
                assembledReport.append(report.getText());
            }
        }

        return assembledReport.toString();
    }

    /**
     * Loads the entities from the data in the net command.
     */
    @SuppressWarnings("unchecked")
    protected void receiveEntities(Packet c) {
        List<InGameObject> newActiveUnits = (List<InGameObject>) c.getObject(0);
        List<InGameObject> newGraveyard = (List<InGameObject>) c.getObject(1);
        Forces newForces = (Forces) c.getObject(2);
        // Replace the entities in the game.
        if (newForces != null) {
            game.setForces(newForces);
        }
        game.setUnitList(newActiveUnits);
        if (newGraveyard != null) {
            game.setGraveyard(newGraveyard);
            for (InGameObject e: newGraveyard) {
                cacheImgTag(e);
            }
        }
        // cache the image data for the entities and set force for entities
        for (InGameObject unit: newActiveUnits) {
            cacheImgTag(unit);
            if (unit instanceof ForceAssignable) {
                ((ForceAssignable) unit).setForceId(game.getForces().getForceId((ForceAssignable) unit));
            }
        }

        if (GUIPreferences.getInstance().getMiniReportShowSprites() &&
                game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND) &&
                iconCache != null && !iconCache.containsKey(Report.HIDDEN_ENTITY_NUM)) {
            ImageUtil.createDoubleBlindHiddenImage(iconCache);
        }
    }

    /**
     * Hashtable for storing img tags containing base64Text src.
     */
    protected void cacheImgTag(InGameObject unit) {
        if (unit == null) {
            return;
        }

        iconCache.remove(unit.getId());
        if (getTargetImage(unit) != null) {
            // convert image to base64, add to the <img> tag and store in cache
            BufferedImage image = ImageUtil.getScaledImage(getTargetImage(unit), 56, 48);
            String base64Text = ImageUtil.base64TextEncodeImage(image);
            String img = "<img src='data:image/png;base64," + base64Text + "'>";
            iconCache.put(unit.getId(), img);
        }
    }

    /**
     * Gets the current mech image
     */
    private Image getTargetImage(InGameObject e) {
        return e.getIcon();
//        if (bv == null) {
//            return null;
//        } else if (e.isDestroyed()) {
//            return bv.getTilesetManager().wreckMarkerFor(e, -1);
//        } else {
//            return bv.getTilesetManager().imageFor(e);
//        }
    }

    /**
     * Send movement data for the given unit to the server.
     */
    public void moveUnit(SBFMovePath movePath) {
        send(new Packet(PacketCommand.ENTITY_MOVE, Objects.requireNonNull(movePath)));
    }


}

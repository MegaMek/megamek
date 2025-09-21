/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.interfaces.ForceAssignable;
import megamek.common.game.InGameObject;
import megamek.common.Report;
import megamek.common.actions.EntityAction;
import megamek.common.force.Forces;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.InvalidPacketDataException;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.common.strategicBattleSystems.SBFMovePath;
import megamek.common.strategicBattleSystems.SBFReportEntry;
import megamek.common.util.ImageUtil;
import megamek.logging.MMLogger;

/**
 * This is the game client for Strategic BattleForce games, as one would think.
 */
public class SBFClient extends AbstractClient {
    private final static MMLogger logger = MMLogger.create(SBFClient.class);
    /**
     * The game object that holds all game information. This object is persistent, i.e. it is never replaced by another
     * game object sent from the server. Instead, the info in this game object is added to or replaced as necessary.
     * Therefore, other objects may keep a reference to this game object. Other objects however, like the players or
     * units, *will* be replaced by objects sent from the server.
     */
    private final SBFGame game = new SBFGame();

    /**
     * Construct a client which will try to connect. If the connection fails, it will alert the player, free resources
     * and hide the frame.
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
    protected boolean handleGameSpecificPacket(Packet packet) {
        try {
            switch (packet.command()) {
                case SENDING_ENTITIES:
                    receiveEntities(packet);
                    break;
                case SENDING_REPORTS_ALL:
                    var receivedReports = packet.getIntegerWithSBFReportEntryList(0);
                    game.replaceAllReports(receivedReports);
                    if (keepGameLog()) {
                        // Re-write the gamelog from scratch
                        initGameLog();
                        for (int round : receivedReports.keySet().stream().sorted().toList()) {
                            possiblyWriteToLog(assembleAndAddImages(receivedReports.get(round)));
                        }
                    }
                    roundReport = assembleAndAddImages(receivedReports.get(game.getCurrentRound()));
                    // We don't really have a copy of the phase report at this point, so I guess
                    // we'll just use the
                    // round report until the next phase actually completes.
                    phaseReport = roundReport;
                    break;
                case SENDING_REPORTS:
                    phaseReport = assembleAndAddImages(packet.getSBFReportEntryList(0));
                    if (keepGameLog()) {
                        if ((log == null) && (game.getCurrentRound() == 1)) {
                            initGameLog();
                        }
                    }
                    game.addReports(packet.getSBFReportEntryList(0));
                    roundReport = assembleAndAddImages(game.getGameReport().get(game.getCurrentRound()));
                    break;
                case SENDING_TURNS:
                    game.setTurns(packet.getSBFTurnList(0));
                    break;
                case TURN:
                    game.setTurnIndex(packet.getIntValue(0), packet.getIntValue(1));
                    break;
                case ENTITY_UPDATE:
                    getGame().receiveUnit(packet.getInGameObject(0));
                    break;
                case UNIT_INVISIBLE:
                    getGame().forget(packet.getIntValue(0));
                case ACTIONS:
                    getGame().clearActions();
                    for (EntityAction action : packet.getEntityActionList(0)) {
                        getGame().addAction(action);
                    }
                default:
                    return false;
            }
            return true;

        } catch (InvalidPacketDataException e) {
            logger.error("Invalid packet data:", e);
            return false;
        }
    }

    private String assembleAndAddImages(List<SBFReportEntry> reports) {
        if (reports == null) {
            logger.error("Received a null list of reports!");
            return "";
        }

        StringBuilder assembledReport = new StringBuilder();
        for (SBFReportEntry report : reports) {
            if (report != null) {
                assembledReport.append(report.text());
            }
        }

        return assembledReport.toString();
    }

    /**
     * Loads the entities from the data in the net command.
     */
    protected void receiveEntities(Packet packet) throws InvalidPacketDataException {
        List<InGameObject> newActiveUnits = packet.getInGameObjectList(0);
        List<InGameObject> newGraveyard = packet.getInGameObjectList(1);
        Forces newForces = packet.getForces(2);
        // Replace the entities in the game.
        if (newForces != null) {
            game.setForces(newForces);
        }
        game.setUnitList(newActiveUnits);
        game.setGraveyard(newGraveyard);

        for (InGameObject inGameObject : newGraveyard) {
            cacheImgTag(inGameObject);
        }
        // cache the image data for the entities and set force for entities
        for (InGameObject unit : newActiveUnits) {
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
     * Gets the current mek image
     */
    private Image getTargetImage(InGameObject e) {
        return e.getIcon();
    }

    public void moveUnit(SBFMovePath movePath) {
        send(new Packet(PacketCommand.ENTITY_MOVE, Objects.requireNonNull(movePath)));
    }

    public void sendAttackData(List<EntityAction> attacks, int formationId) {
        send(new Packet(PacketCommand.ENTITY_ATTACK, formationId, Objects.requireNonNull(attacks)));
    }
}

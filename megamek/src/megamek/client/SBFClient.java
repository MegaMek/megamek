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

import megamek.client.ui.swing.tooltip.PilotToolTip;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.net.packets.Packet;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.common.util.ImageUtil;
import org.apache.logging.log4j.LogManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    public IGame getIGame() {
        return game;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected boolean handleGameSpecificPacket(Packet packet) {
        LogManager.getLogger().info("Received packet: {}", packet);
        switch (packet.getCommand()) {
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
}

/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */
package megamek.client;

import megamek.common.Entity;
import megamek.common.EntityListFile;
import megamek.common.Game;
import megamek.common.actions.AttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameEndEvent;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GameNewActionEvent;
import megamek.common.net.packets.Packet;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import static megamek.client.ui.swing.ClientGUI.CG_FILEEXTENTIONMUL;
import static megamek.client.ui.swing.ClientGUI.CG_FILENAMESALVAGE;

/**
 * This class is instantiated for the player. It allows to communicate with the server with no GUI attached.
 * @author Luana Coppio
 */
public class HeadlessClient extends Client {
    private final static MMLogger logger = MMLogger.create(HeadlessClient.class);
    protected static final ClientPreferences PREFERENCES = PreferenceManager.getClientPreferences();

    private boolean sendDoneOnVictoryAutomatically = true;

    public HeadlessClient(String name, String host, int port) {
        super(name, host, port);

        // Make a list of the player's living units.
        // Be sure to include all units that have retreated.
        // save all destroyed units in a separate "salvage MUL"
        // Save the destroyed entities to the file.
        var gameListener = new GameListenerAdapter() {
            @Override
            public void gameEnd(GameEndEvent e) {
                // Make a list of the player's living units.
                ArrayList<Entity> living = getGame().getPlayerEntities(getLocalPlayer(), false);

                // Be sure to include all units that have retreated.
                for (Enumeration<Entity> iter = getGame().getRetreatedEntities(); iter.hasMoreElements(); ) {
                    Entity ent = iter.nextElement();
                    if (ent.getOwnerId() == getLocalPlayer().getId()) {
                        living.add(ent);
                    }
                }

                // save all destroyed units in a separate "salvage MUL"
                ArrayList<Entity> destroyed = new ArrayList<>();
                Enumeration<Entity> graveyard = getGame().getGraveyardEntities();
                while (graveyard.hasMoreElements()) {
                    Entity entity = graveyard.nextElement();
                    if (entity.isSalvage()) {
                        destroyed.add(entity);
                    }
                }

                if (!destroyed.isEmpty()) {
                    String sLogDir = PREFERENCES.getLogDirectory();
                    File logDir = new File(sLogDir);
                    if (!logDir.exists()) {
                        try {
                            // noinspection ResultOfMethodCallIgnored
                            logDir.mkdir();
                        } catch (SecurityException ex) {
                            logger.error(ex, "Failed to create log directory");
                            return;
                        }
                    }
                    String fileName = CG_FILENAMESALVAGE + CG_FILEEXTENTIONMUL;
                    if (PREFERENCES.stampFilenames()) {
                        fileName = StringUtil.addDateTimeStamp(fileName);
                    }
                    File unitFile = new File(sLogDir + File.separator + fileName);
                    try {
                        // Save the destroyed entities to the file.
                        EntityListFile.saveTo(unitFile, destroyed);
                    } catch (IOException ex) {
                        logger.error(ex, "Failed to save entity list file");
                    }
                }
            }
        };

        getGame().addGameListener(gameListener);
    }

    public void setSendDoneOnVictoryAutomatically(boolean value) {
        sendDoneOnVictoryAutomatically = value;
    }

    @Override
    public void changePhase(GamePhase phase) {
        if (phase == GamePhase.VICTORY) {
            if (sendDoneOnVictoryAutomatically) {
                sendDone(true);
            }
        }
        super.changePhase(phase);
    }

}

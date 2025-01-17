/*
 * MegaMek -
 * Copyright (c) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
 */
package megamek.client;

import megamek.common.Entity;
import megamek.common.EntityListFile;
import megamek.common.Game;
import megamek.common.actions.AttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.enums.GamePhase;
import megamek.common.event.*;
import megamek.common.net.packets.Packet;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import static megamek.client.ui.swing.AbstractClientGUI.CP;
import static megamek.client.ui.swing.ClientGUI.CG_FILEEXTENTIONMUL;
import static megamek.client.ui.swing.ClientGUI.CG_FILENAMESALVAGE;

/**
 * This class is instantiated for each client and for each bot running on that
 * client. non-local clients are not also instantiated on the local server.
 */
public class HeadlessClient extends Client {
    private final static MMLogger logger = MMLogger.create(HeadlessClient.class);

    /**
     * The game state object: this object is not ever replaced during a game, only
     * updated. A
     * reference can therefore be cached by other objects.
     */
    protected final Game game = new Game();
    private int idleCounter;
    private boolean sendDoneOnVictoryAutomatically = true;

    public HeadlessClient(String name, String host, int port) {
        super(name, host, port);
        idleCounter = 0;
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
        } else if (phase == GamePhase.FIRING) {
            idleCounter++;
        }
        if (idleCounter > 5) {
            sendChat("/victory");
        }
        super.changePhase(phase);
    }

    @Override
    protected void receiveEntityRemove(Packet packet) {
        idleCounter=0;
        super.receiveEntityRemove(packet);
    }

    private final GameListener gameListener = new GameListenerAdapter() {

        @Override
        public void gameNewAction(GameNewActionEvent e) {
            EntityAction entityAction = e.getAction();
            if (entityAction instanceof AttackAction attackAction) {
                idleCounter=0;
            }
        }

        @Override
        public void gameEnd(GameEndEvent e) {
            // Make a list of the player's living units.
            ArrayList<Entity> living = getGame().getPlayerEntities(getLocalPlayer(), false);

            // Be sure to include all units that have retreated.
            for (Enumeration<Entity> iter = getGame().getRetreatedEntities(); iter.hasMoreElements();) {
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
                String sLogDir = CP.getLogDirectory();
                File logDir = new File(sLogDir);
                if (!logDir.exists()) {
                    logDir.mkdir();
                }
                String fileName = CG_FILENAMESALVAGE + CG_FILEEXTENTIONMUL;
                if (CP.stampFilenames()) {
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

}

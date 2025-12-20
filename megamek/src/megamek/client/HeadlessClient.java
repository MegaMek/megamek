/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.client.ui.clientGUI.ClientGUI.CG_FILENAME_SALVAGE;
import static megamek.client.ui.clientGUI.ClientGUI.CG_FILE_EXTENSION_MUL;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import megamek.common.Player;
import megamek.common.units.Entity;
import megamek.common.units.EntityListFile;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameEndEvent;
import megamek.common.event.GameListenerAdapter;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;

/**
 * This class is instantiated for the player. It allows to communicate with the server with no GUI attached.
 *
 * @author Luana Coppio
 */
public class HeadlessClient extends Client {
    private final static MMLogger LOGGER = MMLogger.create(HeadlessClient.class);
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
                            LOGGER.error(ex, "Failed to create log directory");
                            return;
                        }
                    }
                    String fileName = CG_FILENAME_SALVAGE + CG_FILE_EXTENSION_MUL;
                    if (PREFERENCES.stampFilenames()) {
                        fileName = StringUtil.addDateTimeStamp(fileName);
                    }
                    File unitFile = new File(sLogDir + File.separator + fileName);
                    try {
                        // Save the destroyed entities to the file.
                        EntityListFile.saveTo(unitFile, destroyed);
                    } catch (IOException ex) {
                        LOGGER.error(ex, "Failed to save entity list file");
                    }
                }
                saveVictoryList();
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


    private void saveVictoryList() {
        String filename = getLocalPlayer().getName();

        // Did we select a file?
        File unitFile = new File(filename + CG_FILE_EXTENSION_MUL);
        if (!(unitFile.getName().toLowerCase().endsWith(CG_FILE_EXTENSION_MUL))) {
            try {
                unitFile = new File(unitFile.getCanonicalPath() + CG_FILE_EXTENSION_MUL);
            } catch (Exception ignored) {
                LOGGER.error("Could not set proper extension for file path.");
                return;
            }
        }


            // What bot was this player? We need it to get the propper salvage MUL, just in case
            Player botPlayer =
                  getGame().getPlayersList().stream().filter(p -> p.isBot() && p.getName().equals(getLocalPlayer().getName() +
                        "@AI")).findFirst().orElse(null);

            if (botPlayer != null) {
                try {
                    // Save the player's entities to the file.
                    EntityListFile.saveTo(unitFile, this, botPlayer);
                } catch (Exception ex) {
                    LOGGER.error(ex, "saveVictoryList");
                }
            }
        }
}

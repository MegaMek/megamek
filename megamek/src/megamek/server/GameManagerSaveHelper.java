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

package megamek.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import megamek.MMConstants;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.util.SerializationHelper;
import megamek.logging.MMLogger;

public class GameManagerSaveHelper {
    private static final MMLogger logger = MMLogger.create(GameManagerSaveHelper.class);

    private final AbstractGameManager gameManager;

    GameManagerSaveHelper(AbstractGameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Saves the game server-side. Will announce the save (or error) in chat if the given sendChat is true.
     *
     * @param fileName The filename to use
     * @param sendChat When true, the saving (or error) is announced in chat
     */
    protected void saveGame(String fileName, boolean sendChat) {
        // We need to strip the .gz if it exists, otherwise we'll double up on it.
        if (fileName.endsWith(".gz")) {
            fileName = fileName.replace(".gz", "");
        }

        String finalFileName = fileName;
        if (!finalFileName.endsWith(MMConstants.SAVE_FILE_EXT)) {
            finalFileName = fileName + MMConstants.SAVE_FILE_EXT;
        }

        File saveGameDir = new File(MMConstants.SAVEGAME_DIR);
        if (!saveGameDir.exists()) {
            saveGameDir.mkdir();
        }

        finalFileName = saveGameDir + File.separator + finalFileName;

        try (OutputStream os = new FileOutputStream(finalFileName + ".gz");
              OutputStream gzo = new GZIPOutputStream(os);
              Writer writer = new OutputStreamWriter(gzo, StandardCharsets.UTF_8)) {
            SerializationHelper.getSaveGameXStream().toXML(gameManager.getGame(), writer);
        } catch (Exception e) {
            String message = String.format("Unable to save file: %s", finalFileName);
            logger.error(e, message);

            if (sendChat) {
                gameManager.sendChat("MegaMek", "Could not save the game to " + finalFileName);
            }
        }

        if (sendChat) {
            gameManager.sendChat("MegaMek", "Game saved to " + finalFileName);
        }
    }

    /**
     * Saves the game and sends it to the specified connection
     *
     * @param connId    The <code>int</code> connection id to send to
     * @param fileName  The <code>String</code> filename to use
     * @param localPath The <code>String</code> path to the file to be used on the client
     */
    public void sendSaveGame(int connId, String fileName, String localPath) {
        saveGame(fileName, false);

        String finalFileName = fileName;
        if (!finalFileName.endsWith(MMConstants.SAVE_FILE_GZ_EXT)) {
            if (finalFileName.endsWith(MMConstants.SAVE_FILE_EXT)) {
                finalFileName = fileName + ".gz";
            } else {
                finalFileName = fileName + MMConstants.SAVE_FILE_GZ_EXT;
            }
        }

        localPath = localPath.replace("|", " ");
        String localFile = MMConstants.SAVEGAME_DIR + File.separator + finalFileName;

        try (InputStream in = new FileInputStream(localFile); InputStream bin = new BufferedInputStream(in)) {
            List<Integer> data = new ArrayList<>();
            int input;
            while ((input = bin.read()) != -1) {
                data.add(input);
            }
            gameManager.send(connId, new Packet(PacketCommand.SEND_SAVEGAME, finalFileName, data, localPath));
            gameManager.sendChat(connId, "***Server", "Save game has been sent to you.");
        } catch (Exception ex) {
            String message = String.format("Unable to load file: %s", localFile);
            logger.error(ex, message);
        }
    }
}

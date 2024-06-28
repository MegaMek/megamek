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
package megamek.server;

import megamek.MMConstants;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.util.SerializationHelper;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

class GameManagerSaveHelper {

    private final AbstractGameManager gameManager;

    GameManagerSaveHelper(AbstractGameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Saves the game server-side. Will announce the save (or error) in chat if the given sendChat
     * is true.
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
            LogManager.getLogger().error("Unable to save file: {}", finalFileName, e);
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
     * @param connId The <code>int</code> connection id to send to
     * @param fileName The <code>String</code> filename to use
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
        localPath = localPath.replaceAll("\\|", " ");
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
            LogManager.getLogger().error("Unable to load file: {}", localFile, ex);
        }
    }
}

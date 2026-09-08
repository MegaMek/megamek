/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

import static megamek.client.ui.clientGUI.ClientGUI.CG_FILE_EXTENSION_MUL;

import java.io.File;

import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameVictoryEvent;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.Entity;
import megamek.common.units.EntityListFile;
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

    /**
     * The game result captured the instant the VICTORY phase begins, before any server-side reset can wipe the board.
     * This is how the result reaches MekHQ even when the VICTORY phase never formally ends (e.g. a bot disconnect
     * stalls the readiness check). See issue #8889.
     */
    private GameVictoryEvent victorySnapshot;

    /**
     * A client with no GUI has nothing to show an entity's picture in, so it does not cache one.
     *
     * <p>Building that cache is what pulls in the hex tile set, which parses thousands of template hexes and their
     * terrain. {@code BotClient} already declines it for the same reason; a headless client wants it no more than a
     * bot does, and anywhere clients are created repeatedly - a benchmark playing many games in one process - the
     * cost is paid and retained once per game.</p>
     *
     * @param entity ignored
     */
    @Override
    protected void cacheImgTag(Entity entity) {
        // Deliberately empty: see the javadoc.
    }

    public HeadlessClient(String name, String host, int port) {
        super(name, host, port);
        // Note: the end-of-game MUL is written from changePhase(VICTORY) via saveVictoryList(), not from a gameEnd
        // listener. GameEndEvent is only fired when the client receives an END_OF_GAME packet, which the server no
        // longer sends, so a gameEnd listener here would never run. See issue #8890.
    }

    public void setSendDoneOnVictoryAutomatically(boolean value) {
        sendDoneOnVictoryAutomatically = value;
    }

    @Override
    public void changePhase(GamePhase phase) {
        if (phase == GamePhase.VICTORY) {
            // Capture the final game state now, while the board is still populated, so a consumer (e.g. MekHQ via the
            // Commander interface) can be handed the result on demand without depending on the VICTORY phase ending
            // or on the GAME_VICTORY_EVENT packet arriving. See issue #8889.
            victorySnapshot = new GameVictoryEvent(this, getGame());
            // Write the player's end-of-game MUL now, while the board is still populated. Waiting for gameEnd never
            // worked (its END_OF_GAME packet is no longer sent) and the game is reset right after VICTORY. See #8890.
            saveVictoryList();
            if (sendDoneOnVictoryAutomatically) {
                sendDone(true);
            }
        }
        super.changePhase(phase);
    }

    /**
     * @return the game result captured when the VICTORY phase began, or {@code null} if the game has not reached
     *       victory yet
     */
    public @Nullable GameVictoryEvent getVictorySnapshot() {
        return victorySnapshot;
    }

    /**
     * Writes a MUL of the player's surviving force to the log directory, named after the player. PACAR hands the
     * player's units to an "@AI" bot on the player's own team, so the force is gathered by team rather than by the old
     * (and now removed) lookup of a bot named "{@literal <player>@AI}", which {@code resetGame()} deleted before the
     * write could ever run. The file is a fallback the player can use to resolve the scenario manually if the
     * post-scenario dialogs are lost. See issue #8890.
     */
    private void saveVictoryList() {
        Player localPlayer = getLocalPlayer();
        if (localPlayer == null) {
            return;
        }

        String logDirectoryPath = PREFERENCES.getLogDirectory();
        File logDir = new File(logDirectoryPath);
        if (!logDir.exists() && !logDir.mkdirs()) {
            LOGGER.error("Failed to create log directory {}", logDirectoryPath);
            return;
        }

        String fileName = localPlayer.getName() + CG_FILE_EXTENSION_MUL;
        if (PREFERENCES.stampFilenames()) {
            fileName = StringUtil.addDateTimeStamp(fileName);
        }
        File unitFile = new File(logDir, fileName);

        try {
            // teamAsLiving = true: gather the whole player team (the player plus the "@AI" bot the units were handed
            // to) into the survivors/retreated sections; the enemy team goes to salvage. See issue #8890.
            EntityListFile.saveTo(unitFile, this, localPlayer, true);
        } catch (Exception ex) {
            LOGGER.error(ex, "saveVictoryList");
        }
    }
}

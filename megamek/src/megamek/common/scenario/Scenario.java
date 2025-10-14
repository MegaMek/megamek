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

package megamek.common.scenario;

import java.io.IOException;

import megamek.common.game.GameType;
import megamek.common.game.IGame;
import megamek.common.jacksonAdapters.BotParser;
import megamek.server.IGameManager;

public interface Scenario {

    String COMMENT_MARK = "#";
    String FILE_SUFFIX_BOARD = ".board";
    String MMS_VERSION = "MMSVersion";

    String NAME = "name";
    String DESCRIPTION = "description";
    String PLANET = "planet";
    String GAMETYPE = "gametype";

    String PARAM_GAME_OPTIONS_FILE = "GameOptionsFile";
    String PARAM_GAME_OPTIONS_FIXED = "FixedGameOptions";
    String PARAM_GAME_EXTERNAL_ID = "ExternalId";
    String PARAM_FACTIONS = "factions";
    String PARAM_SINGLEPLAYER = "singleplayer";


    String MMS_PLANET_CONDITIONS = "planetaryconditions";
    String PARAM_PLANET_CONDITIONS_FIXED = "fixedplanetaryconditions";

    String PARAM_MAP_WIDTH = "MapWidth";
    String PARAM_MAP_HEIGHT = "MapHeight";
    String MAP_RANDOM = "RANDOM";
    String PARAM_BOARD_WIDTH = "BoardWidth";
    String PARAM_BOARD_HEIGHT = "BoardHeight";
    String PARAM_BRIDGE_CF = "BridgeCF";
    String PARAM_MAPS = "Maps";
    String PARAM_MAP_DIRECTORIES = "RandomDirs";

    String PARAM_TEAM = "team";
    String PARAM_LOCATION = "Location";
    String PARAM_MINEFIELDS = "Minefields";
    String PARAM_DAMAGE = "Damage";
    String PARAM_SPECIFIC_DAMAGE = "DamageSpecific";
    String PARAM_CRITICAL_HIT = "CritHit";
    String PARAM_AMMO_AMOUNT = "SetAmmoTo";
    String PARAM_AMMO_TYPE = "SetAmmoType";
    String PARAM_PILOT_HITS = "PilotHits";
    String PARAM_EXTERNAL_ID = "ExternalID";
    String PARAM_ADVANTAGES = "Advantages";
    String PARAM_AUTO_EJECT = "AutoEject";
    String PARAM_COMMANDER = "Commander";
    String PARAM_DEPLOYMENT_ROUND = "DeploymentRound";
    String PARAM_CAMO = "camo";
    String PARAM_ALTITUDE = "altitude";

    /**
     * @return The name (title) of the scenario.
     */
    String getName();

    /**
     * @return The description for the scenario.
     */
    String getDescription();

    String getFileName();

    default GameType getGameType() {
        return GameType.TW;
    }

    default String getPlanet() {
        return "";
    }

    IGame createGame() throws ScenarioLoaderException, IOException;

    boolean isSinglePlayer();

    boolean hasFixedGameOptions();

    boolean hasFixedPlanetaryConditions();

    void applyDamage(IGameManager gameManager);

    default int findIndex(String[] sa, String s) {
        for (int x = 0; x < sa.length; x++) {
            if (sa[x].equalsIgnoreCase(s)) {
                return x;
            }
        }
        return -1;
    }

    default boolean hasBotInfo(String playerName) {
        return getBotInfo(playerName) != null;
    }

    default BotParser.BotInfo getBotInfo(String playerName) {
        return null;
    }
}

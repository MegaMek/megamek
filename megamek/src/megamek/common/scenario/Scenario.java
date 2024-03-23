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
package megamek.common.scenario;

import megamek.common.GameType;
import megamek.common.IGame;
import megamek.server.IGameManager;

import java.io.IOException;

public interface Scenario {

    String COMMENT_MARK = "#";
    String FILE_SUFFIX_BOARD = ".board";
    String MMSVERSION = "MMSVersion";

    String NAME = "name";
    String DESCRIPTION = "description";
    String PLANET = "planet";
    String GAMETYPE = "gametype";

    String PARAM_GAME_OPTIONS_FILE = "GameOptionsFile";
    String PARAM_GAME_OPTIONS_FIXED = "FixedGameOptions";
    String PARAM_GAME_EXTERNAL_ID = "ExternalId";
    String PARAM_FACTIONS = "factions";
    String PARAM_SINGLEPLAYER = "singleplayer";


    String MMS_PLANETCOND = "planetaryconditions";
    String PARAM_PLANETCOND_FIXED = "fixedplanetaryconditions";

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

    String GAMETYPE_TW = "TW";
    String GAMETYPE_SBF = "SBF";
    String GAMETYPE_AS = "AS";
    String GAMETYPE_BF = "BF";


    String getName();

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
}
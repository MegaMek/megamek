/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.server.totalWarfare;

import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Writes the Advanced Buildings a scenario file places on a fixed hex into the map.
 *
 * <p>A building deployed during play is written into every hex it covers by
 * {@link AbstractBuildingEntity#updateBuildingEntityHexes}, which the deployment step calls. A scenario unit given a
 * position with {@code at:} arrives already deployed and never passes through that step, so the building exists as a
 * unit while its hexes stay open ground: the map shows no building, and anything that looks for one there, such as
 * the firing arc for a weapon aimed at it, finds nothing. This runs once, as the scenario starts, and gives those
 * buildings the same treatment deployment would have.</p>
 */
class ScenarioBuildingPlacementHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(ScenarioBuildingPlacementHandler.class);

    ScenarioBuildingPlacementHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Writes every building that is already standing on the board into the hexes it covers.
     */
    void placePreDeployedBuildings() {
        int placedCount = 0;
        for (Entity entity : getGame().getEntitiesVector()) {
            if (!(entity instanceof AbstractBuildingEntity building)) {
                continue;
            }
            boolean isPlaced = building.isDeployed() && (building.getPosition() != null);
            if (!isPlaced) {
                LOGGER.debug("[ScenarioBuilding] {} is not placed on the board yet; it will be written in when it"
                      + " deploys", building.getShortName());
                continue;
            }
            if (building.isOffBoard()) {
                LOGGER.debug("[ScenarioBuilding] {} is off the board; it has no hexes to write into the map",
                      building.getShortName());
                continue;
            }
            building.updateBuildingEntityHexes(building.getBoardId(), gameManager);
            placedCount++;
            LOGGER.debug("[ScenarioBuilding] {} written into the map at {} on board {}", building.getShortName(),
                  building.getCoordsList(), building.getBoardId());
        }
        if (placedCount > 0) {
            LOGGER.info("[ScenarioBuilding] {} building(s) placed by the scenario file written into the map",
                  placedCount);
        }
    }
}

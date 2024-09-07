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
package megamek.client.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.client.ui.swing.ClientGUI;
import megamek.common.Entity;

/**
 * A command that displays unit or terrain information
 * given an entity ID, parameters and maximum distance
 * @author NickAragua
 *
 */
public class SituationReportCommand extends ClientCommand {
    private static final int DEFAULT_HEX_RANGE = -1;

    public SituationReportCommand(ClientGUI clientGUI) {
        super(clientGUI, "sitrep", "Display visible board state relative to this entity. Use #sitrep HELP for more information.");
    }

    @Override
    public String run(String[] args) {
        boolean showFriendly = true;
        boolean showHostile = true;
        int hexRange = DEFAULT_HEX_RANGE;
        int entityID = Entity.NONE;

        if (args.length > 1) {
            if (args[1].equalsIgnoreCase("HELP")) {
                return "#sitrep [entityID] [friendly|hostile|all] [x] = Print out friendly, " +
                        "hostile or all units relative to the entity with the specified ID, optionally within [x] hexes only. " +
                        "By default, [x] is the entity's maximum weapons range. " +
                        "Degrees is the direction of the unit, with 0 being directly north.";
            } else {
                entityID = Integer.parseInt(args[1]);

                if (args.length > 2) {
                    showFriendly = !args[2].equalsIgnoreCase("hostile");
                    showHostile = !args[2].equalsIgnoreCase("friendly");
                }

                if (args.length > 3) {
                    hexRange = Integer.parseInt(args[3]);
                }

                return buildUnitSituationReport(entityID, showFriendly, showHostile, hexRange);
            }
        }

        return "No arguments given, or there was an error parsing the arguments.";
    }

    /**
     * Worker function that generates a list of entities that are
     * @param entityID ID of the entity needing a SitRep
     * @param showFriendly Whether to show friendly units
     * @param showHostile Whether to show hostile units
     * @param maxHexRange Maximum distance
     * @return SitRep string
     */
    private String buildUnitSituationReport(int entityID, boolean showFriendly, boolean showHostile, int maxHexRange) {
        Entity currentEntity = getClient().getEntity(entityID);
        if (maxHexRange == DEFAULT_HEX_RANGE) {
            maxHexRange = currentEntity.getMaxWeaponRange();
        }

        if (currentEntity.getOwnerId() != getClient().getLocalPlayerNumber()) {
            return String.format("Entity %d is not owned by player %s", entityID, getClient().getLocalPlayer());
        }

        StringBuilder retVal = new StringBuilder();
        Map <Integer, List<Entity>> distanceBuckets = new HashMap<>();
        int maxDistance = -1;

        // first we assemble the qualifying entities into buckets, sorted by distance
        for (Entity entity : getClient().getEntitiesVector()) {
            boolean hostileEntity = entity.getOwner().isEnemyOf(getClient().getLocalPlayer());
            boolean displayEntity = (showFriendly && !hostileEntity) ||
                                    (showHostile && hostileEntity);

            if (displayEntity && (entity.getId() != currentEntity.getId())) {
                if (entity.isOffBoard() || !entity.isDeployed()) {
                    continue;
                }
                int distance = currentEntity.getPosition().distance(entity.getPosition());
                if (distance > maxHexRange) {
                    continue;
                }

                if (distance > maxDistance) {
                    maxDistance = distance;
                }

                if (!distanceBuckets.containsKey(distance)) {
                    distanceBuckets.put(distance, new ArrayList<>());
                }

                distanceBuckets.get(distance).add(entity);
            }
        }

        // now we go through our buckets from lowest to highest, and output
        // the contained entities.
        for (int distance = 0; distance <= maxDistance; distance++) {
            if (!distanceBuckets.containsKey(distance)) {
                continue;
            }

            for (Entity entity : distanceBuckets.get(distance)) {
                int direction = currentEntity.getPosition().degree(entity.getPosition());

                // entity name, id, status, distance, direction
                retVal.append(String.format("%s | %d hexes | %d degrees\n",
                        entity.getDisplayName(),
                        distance,
                        direction));
            }
        }

        return retVal.toString();
    }
}

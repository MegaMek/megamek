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

package megamek.common.jacksonAdapters;

import static megamek.common.jacksonAdapters.ASElementSerializer.FULL_NAME;
import static megamek.common.jacksonAdapters.MMUReader.requireFields;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.common.CriticalSlot;
import megamek.common.OffBoardDirection;
import megamek.common.board.Coords;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.enums.BombType;
import megamek.common.icons.Camouflage;
import megamek.common.loaders.MekSummary;
import megamek.common.scenario.Scenario;
import megamek.common.scenario.ScenarioLoaderException;
import megamek.common.units.Entity;
import megamek.common.units.IAero;
import megamek.common.units.IBomber;
import megamek.common.units.Mek;
import megamek.common.units.Tank;

public class EntityDeserializer extends StdDeserializer<Entity> {

    private static final String ID = "id";
    private static final String AT = "at";
    private static final String BOARD = "board";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String OFFBOARD = "offboard";
    private static final String DISTANCE = "distance";
    private static final String STATUS = "status";
    private static final String PRONE = "prone";
    private static final String SHUTDOWN = "shutdown";
    private static final String HIDDEN = "hidden";
    private static final String HULL_DOWN = "hulldown";
    private static final String FACING = "facing";
    private static final String DEPLOYMENT_ROUND = "deploymentround";
    private static final String ELEVATION = "elevation";
    private static final String ALTITUDE = "altitude";
    private static final String VELOCITY = "velocity";
    private static final String REMAINING = "remaining";
    private static final String ARMOR = "armor";
    private static final String INTERNAL = "internal";
    private static final String EXTERNAL = "external";
    private static final String FORCE = "force";
    private static final String CRITS = "crits";
    private static final String AMMO = "ammo";
    private static final String SLOT = "slot";
    private static final String SHOTS = "shots";
    public static final String FLEE_AREA = "fleefrom";
    private static final String AREA = "area";
    private static final String BOMBS = "bombs";
    private static final String ENGINE = "engine";

    public EntityDeserializer() {
        this(null);
    }

    public EntityDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Entity deserialize(JsonParser jp, DeserializationContext context) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        requireFields("TW Unit", node, FULL_NAME);

        // A TW unit (=Entity) must be loaded from the cache
        Entity entity = loadEntity(node);
        assignPosition(entity, node);
        assignFacing(entity, node);
        assignDeploymentRound(entity, node);
        assignStatus(entity, node);
        assignIndividualCamo(entity, node);
        assignElevation(entity, node);
        assignAltitude(entity, node);
        assignVelocity(entity, node);
        assignID(entity, node);
        assignForce(entity, node);
        CrewDeserializer.parseCrew(node, entity);
        assignRemaining(entity, node);
        assignCrits(entity, node);
        assignAmmunition(entity, node);
        assignBombs(entity, node);
        assignFleeArea(entity, node);
        return entity;
    }

    private void assignID(Entity entity, JsonNode node) {
        if (node.has(ID)) {
            entity.setId(node.get(ID).intValue());
        }
    }

    private Entity loadEntity(JsonNode node) {
        String fullName = node.get(FULL_NAME).textValue();
        Entity entity = MekSummary.loadEntity(fullName);
        if (entity == null) {
            throw new IllegalArgumentException("Could not retrieve unit " + fullName + " from cache!");
        }
        return entity;
    }

    private void assignPosition(Entity entity, JsonNode node) {
        try {
            if (node.has(AT)) {
                setDeployedPosition(entity, CoordsDeserializer.parseNode(node.get(AT)));
            } else if (node.has(X) || node.has(Y)) {
                setDeployedPosition(entity, CoordsDeserializer.parseNode(node));
            }
            if (node.has(BOARD)) {
                entity.setBoardId(node.get(BOARD).intValue());
            }
            if (node.has(OFFBOARD)) {
                int distance = node.has(DISTANCE) ? node.get(DISTANCE).intValue() : 17;
                entity.setOffBoard(distance, OffBoardDirection.valueOf(node.get(OFFBOARD).textValue()));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Illegal position information for entity " + entity, e);
        }
    }

    private void assignFacing(Entity entity, JsonNode node) {
        if (node.has(FACING)) {
            JsonNode facingNode = node.get(FACING);
            if (facingNode.isInt()) {
                int facing = facingNode.asInt();
                if (facing < 0 || facing > 5) {
                    throw new IllegalArgumentException("Illegal facing: " + facing + " for entity " + entity);
                }
                entity.setFacing(facing);
                entity.setSecondaryFacing(facing, false);
            }
        }
    }

    private void assignDeploymentRound(Entity entity, JsonNode node) {
        if (node.has(DEPLOYMENT_ROUND)) {
            entity.setDeployRound(node.get(DEPLOYMENT_ROUND).asInt());
        }
    }

    private void setDeployedPosition(Entity entity, Coords coords) {
        entity.setDeployed(true);
        // translate the position so "at: 2, 3" will place a unit on 0203 (instead of 0102)
        entity.setPosition(coords);
    }

    private void assignStatus(Entity entity, JsonNode node) {
        if (node.has(STATUS)) {
            JsonNode statusNode = node.get(STATUS);
            if (statusNode.isContainerNode() && statusNode.isArray()) {
                statusNode.iterator().forEachRemaining(n -> parseStatus(entity, n.textValue()));
            } else if (statusNode.isTextual()) {
                parseStatus(entity, statusNode.asText());
            }
        }
    }

    private void assignRemaining(Entity entity, JsonNode node) {
        if (node.has(REMAINING)) {
            JsonNode remainingNode = node.get(REMAINING);
            if (remainingNode.has(ARMOR)) {
                JsonNode armorNode = remainingNode.get(ARMOR);
                for (int location = 0; location < entity.locations(); location++) {
                    String locationAbbr = entity.getLocationAbbr(location);
                    if (armorNode.has(locationAbbr)) {
                        // don't allow more than the maximum armor
                        int newArmor = Math.min(armorNode.get(locationAbbr).intValue(), entity.getArmor(location));
                        entity.setArmor(newArmor, location);
                    }
                    if (entity.hasRearArmor(location)) {
                        String rearLocationAbbr = entity.getLocationAbbr(location) + "R";
                        if (armorNode.has(rearLocationAbbr)) {
                            int newArmor = Math.min(armorNode.get(rearLocationAbbr).intValue(),
                                  entity.getArmor(location, true));
                            entity.setArmor(newArmor, location, true);
                        }
                    }
                }
            }
            if (remainingNode.has(INTERNAL)) {
                JsonNode internalNode = remainingNode.get(INTERNAL);
                for (int location = 0; location < entity.locations(); location++) {
                    String locationAbbr = entity.getLocationAbbr(location);
                    if (internalNode.has(locationAbbr)) {
                        int newIS = Math.min(internalNode.get(locationAbbr).intValue(),
                              entity.getInternal(location));
                        entity.setInternal(newIS, location);
                    }
                }
            }
        }
    }

    private void parseStatus(Entity entity, String statusString) {
        switch (statusString) {
            case PRONE:
                entity.setProne(true);
                break;
            case SHUTDOWN:
                entity.setShutDown(true);
                break;
            case HIDDEN:
                entity.setHidden(true);
                break;
            case HULL_DOWN:
                entity.setHullDown(true);
                break;
            default:
                throw new IllegalArgumentException("Unknown status " + statusString);
        }
    }

    private void assignIndividualCamo(Entity entity, JsonNode node) {
        if (node.has(Scenario.PARAM_CAMO)) {
            String camoPath = node.get(Scenario.PARAM_CAMO).textValue();
            entity.setCamouflage(new Camouflage(new File(camoPath)));
        }
    }

    private void assignElevation(Entity entity, JsonNode node) {
        if (node.has(ELEVATION)) {
            entity.setElevation(node.get(ELEVATION).asInt());
        }
    }

    private void assignForce(Entity entity, JsonNode node) {
        if (node.has(FORCE)) {
            entity.setForceString(node.get(FORCE).asText());
        }
    }

    private void assignAltitude(Entity entity, JsonNode node) {
        if (node.has(ALTITUDE)) {
            if (!(entity instanceof IAero)) {
                throw new IllegalArgumentException("Illegal keyword altitude for non-aerospace unit");
            }
            int altitude = node.get(ALTITUDE).asInt();
            if (altitude < 0 || altitude > 10) {
                throw new IllegalArgumentException("Illegal altitude " + altitude + " for entity " + entity);
            }
            entity.setAltitude(altitude);
            if (altitude == 0) {
                ((IAero) entity).land();
            }
        }
    }

    private void assignVelocity(Entity entity, JsonNode node) {
        if (node.has(VELOCITY)) {
            if (!(entity instanceof IAero)) {
                throw new IllegalArgumentException("Illegal keyword velocity for non-aerospace unit");
            }
            int velocity = node.get(VELOCITY).asInt();
            if (velocity < 0) {
                throw new IllegalArgumentException("Illegal velocity " + velocity + " for entity " + entity);
            }
            ((IAero) entity).setCurrentVelocity(velocity);
            ((IAero) entity).setNextVelocity(velocity);
        }
    }

    private void assignCrits(Entity entity, JsonNode node) {
        if (!node.has(CRITS)) {
            return;
        }
        JsonNode critsNode = node.get(CRITS);
        if (entity instanceof Tank tank) {
            if (critsNode.has(ENGINE)) {
                tank.engineHit();
            }

        } else if (entity instanceof Mek) {

            for (int location = 0; location < entity.locations(); location++) {
                String locationAbbr = entity.getLocationAbbr(location);
                if (critsNode.has(locationAbbr)) {
                    for (int slot : parseArrayOrSingleNode(critsNode.get(locationAbbr))) {
                        int zeroBasedSlot = slot - 1;
                        CriticalSlot cs = entity.getCritical(location, zeroBasedSlot);
                        if ((cs == null) || !cs.isHittable()) {
                            throw new IllegalArgumentException("Invalid slot " + location + ":" + slot + " on " + entity);
                        } else {
                            cs.setHit(true);
                            if ((cs.getType() == CriticalSlot.TYPE_SYSTEM) && (cs.getIndex() == Mek.SYSTEM_ENGINE)) {
                                entity.engineHitsThisPhase++;
                            } else {
                                Mounted<?> mounted = cs.getMount();
                                mounted.setDestroyed(true);
                            }
                        }
                    }
                }
            }
        }
    }

    private void assignAmmunition(Entity entity, JsonNode node) {
        if (node.has(AMMO)) {
            JsonNode critsNode = node.get(AMMO);
            for (int location = 0; location < entity.locations(); location++) {
                String locationAbbr = entity.getLocationAbbr(location);
                final int finalLoc = location;
                if (critsNode.has(locationAbbr)) {
                    critsNode.get(locationAbbr).iterator().forEachRemaining(n -> assignAmmunition(entity, n, finalLoc));
                }
            }
        }
    }

    private void assignAmmunition(Entity entity, JsonNode node, int location) {
        int slot = node.get(SLOT).asInt() - 1;
        int shots = node.get(SHOTS).asInt();
        CriticalSlot cs = entity.getCritical(location, slot);
        if (cs != null) {
            Mounted<?> ammo = cs.getMount();
            if (ammo.getType() instanceof AmmoType) {
                // Also make sure we don't exceed the max allowed
                ammo.setShotsLeft(Math.min(shots, ammo.getBaseShotsLeft()));
            } else {
                throw new IllegalArgumentException("Invalid ammo slot "
                      + location
                      + ":"
                      + (slot + 1)
                      + " on "
                      + entity);
            }
        }
    }

    private void assignBombs(Entity entity, JsonNode node) {
        if (node.has(BOMBS) && entity instanceof IBomber bomber) {
            JsonNode bombsNode = node.get(BOMBS);
            // bombs must use the external and/or internal keywords or give the bombs directly, in which case they are
            // external
            if (bombsNode.has(EXTERNAL) || bombsNode.has(INTERNAL)) {
                if (bombsNode.has(EXTERNAL)) {
                    bomber.setExtBombChoices(readBombLoadout(bombsNode.get(EXTERNAL)));
                }
                if (bombsNode.has(INTERNAL)) {
                    bomber.setIntBombChoices(readBombLoadout(bombsNode.get(INTERNAL)));
                }
            } else {
                bomber.setExtBombChoices(readBombLoadout(bombsNode));
            }
        }
    }

    private BombLoadout readBombLoadout(JsonNode node) {
        BombLoadout loadout = new BombLoadout();
        node.fieldNames().forEachRemaining(name -> {
            var bombNode = node.get(name);
            try {
                loadout.put(BombType.BombTypeEnum.valueOf(name), bombNode.asInt());
            } catch (IllegalArgumentException e) {
                throw new ScenarioLoaderException("Cannot parse bomb type " + name);
            }
        });
        return loadout;
    }

    private void assignFleeArea(Entity entity, JsonNode node) {
        if (node.has(FLEE_AREA)) {
            // allow using or omitting "area:"
            if (node.get(FLEE_AREA).has(AREA)) {
                entity.setFleeZone(HexAreaDeserializer.parseShape(node.get(FLEE_AREA).get(AREA)));
            } else {
                entity.setFleeZone(HexAreaDeserializer.parseShape(node.get(FLEE_AREA)));
            }
        }
    }

    /**
     * Returns all Integers of a node as a List. The node may be either of the form "node: singleNumber", in which case
     * the List will only contain singleNumber, or it may be an array node of the form "node: [ firstNumber,
     * secondNumber ]" (or the multi-line form using dashes) in which case the list contains all the given numbers.
     *
     * @param node The node to parse
     *
     * @return A list of the given numbers of the node
     */
    public static List<Integer> parseArrayOrSingleNode(JsonNode node) {
        List<Integer> result = new ArrayList<>();
        if (node.isArray()) {
            node.iterator().forEachRemaining(n -> result.add(n.asInt()));
        } else {
            result.add(node.asInt());
        }
        return result;
    }
}

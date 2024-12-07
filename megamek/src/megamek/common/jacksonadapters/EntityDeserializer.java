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
package megamek.common.jacksonadapters;

import static megamek.common.jacksonadapters.ASElementSerializer.FULL_NAME;
import static megamek.common.jacksonadapters.MMUReader.requireFields;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import megamek.common.*;
import megamek.common.icons.Camouflage;
import megamek.common.modifiers.EquipmentModifier;
import megamek.common.scenario.Scenario;

public class EntityDeserializer extends StdDeserializer<Entity> {

    private static final String ID = "id";
    private static final String AT = "at";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String STATUS = "status";
    private static final String PRONE = "prone";
    private static final String SHUTDOWN = "shutdown";
    private static final String HIDDEN = "hidden";
    private static final String HULLDOWN = "hulldown";
    private static final String FACING = "facing";
    private static final String DEPLOYMENTROUND = "deploymentround";
    private static final String ELEVATION = "elevation";
    private static final String ALTITUDE = "altitude";
    private static final String VELOCITY = "velocity";
    private static final String REMAINING = "remaining";
    private static final String ARMOR = "armor";
    private static final String INTERNAL = "internal";
    private static final String FORCE = "force";
    private static final String CRITS = "crits";
    private static final String AMMO = "ammo";
    private static final String SLOT = "slot";
    private static final String SHOTS = "shots";
    private static final String MODIFIERS = "modifiers";
    private static final String UNIT = "unit";
    private static final String ENGINE = "engine";
    private static final String AVIONICS = "avionics";
    public static final String FLEE_AREA = "fleefrom";
    private static final String AREA = "area";

    public EntityDeserializer() {
        this(null);
    }

    public EntityDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Entity deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
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
        assignAmmos(entity, node);
        assignModifiers(entity, node);
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
        if (node.has(DEPLOYMENTROUND)) {
            entity.setDeployRound(node.get(DEPLOYMENTROUND).asInt());
        }
    }

    private void setDeployedPosition(Entity entity, Coords coords) {
        entity.setDeployed(true);
        // translate the position so "at: 2, 3" will place a unit on 0203 (instead of 0102)
        entity.setPosition(new Coords(coords.getX() - 1, coords.getY() - 1));
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
            case HULLDOWN:
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
        if (!(entity instanceof Mek) || !node.has(CRITS)) {
            // Implementation very different for different entities; for now: Meks
            return;
        }
        JsonNode critsNode = node.get(CRITS);
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

    private void assignAmmos(Entity entity, JsonNode node) {
        if (node.has(AMMO)) {
            JsonNode critsNode = node.get(AMMO);
            for (int location = 0; location < entity.locations(); location++) {
                String locationAbbr = entity.getLocationAbbr(location);
                final int finalLoc = location;
                if (critsNode.has(locationAbbr)) {
                    critsNode.get(locationAbbr).iterator().forEachRemaining(n -> assignAmmo(entity, n, finalLoc));
                }
            }
        }
    }

    private void assignAmmo(Entity entity, JsonNode node, int location) {
        int slot = node.get(SLOT).asInt() - 1;
        int shots = node.get(SHOTS).asInt();
        CriticalSlot cs = entity.getCritical(location, slot);
        if (cs != null) {
            Mounted<?> ammo = cs.getMount();
            if (ammo.getType() instanceof AmmoType) {
                // Also make sure we dont exceed the max allowed
                ammo.setShotsLeft(Math.min(shots, ammo.getBaseShotsLeft()));
            } else {
                throw new IllegalArgumentException("Invalid ammo slot " + location + ":" + (slot + 1) + " on " + entity);
            }
        }
    }

    private void assignModifiers(Entity entity, JsonNode node) {
        if (node.has(MODIFIERS)) {
            JsonNode modifiersNode = node.get(MODIFIERS);
            for (int location = 0; location < entity.locations(); location++) {
                String locationAbbr = entity.getLocationAbbr(location);
                final int finalLoc = location;
                if (modifiersNode.has(locationAbbr)) {
                    modifiersNode.get(locationAbbr).iterator().forEachRemaining(n -> assignModifier(entity, n, finalLoc));
                }
            }
            if (modifiersNode.has(UNIT)) {
                parseModifiers(modifiersNode.get(UNIT)).forEach(entity::addEquipmentModifier);
            }
            if (modifiersNode.has(ENGINE) && entity.hasEngine()) {
                Engine engine = entity.getEngine();
                parseModifiers(modifiersNode.get(ENGINE)).forEach(engine::addEquipmentModifier);
            }

        }
    }

    private void assignModifier(Entity entity, JsonNode node, int location) {
        int slot = node.get(SLOT).asInt() - 1;
        CriticalSlot cs = entity.getCritical(location, slot);
        if (cs != null) {
            Mounted<?> equipment = cs.getMount();
            if (equipment != null) {
                parseModifiers(node.get(MODIFIERS)).forEach(equipment::addEquipmentModifier);
            } else {
                throw new IllegalArgumentException("Invalid equipment slot " + location + ":" + (slot + 1) + " on " + entity);
            }
        } else {
            throw new IllegalArgumentException("Empty slot " + location + ":" + (slot + 1) + " on " + entity);
        }
    }

    private List<EquipmentModifier> parseModifiers(JsonNode node) {
        if (node.isArray()) {
            List<EquipmentModifier> result = new ArrayList<>();
            node.elements().forEachRemaining(n -> result.add(EquipmentModifierDeserializer.parseNode(n)));
            return result;
        } else {
            return List.of(EquipmentModifierDeserializer.parseNode(node));
        }
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
     * Returns all Integers of a node as a List. The node may be either of the form "node: singleNumber", in
     * which case the List will only contain singleNumber, or it may be an array node of the form
     * "node: [ firstNumber, secondNumber ]" (or the multi-line form using dashes) in which case the list
     * contains all the given numbers.
     *
     * @param node The node to parse
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

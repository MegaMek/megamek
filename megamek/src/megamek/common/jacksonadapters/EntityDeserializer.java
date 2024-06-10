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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.common.*;
import megamek.common.icons.Camouflage;
import megamek.common.scenario.Scenario;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static megamek.common.jacksonadapters.ASElementSerializer.FULL_NAME;
import static megamek.common.jacksonadapters.MMUReader.requireFields;

public class EntityDeserializer extends StdDeserializer<Entity> {

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
        CrewDeserializer.parseCrew(node, entity);
        return entity;
    }

    private Entity loadEntity(JsonNode node) {
        String fullName = node.get(FULL_NAME).textValue();
        Entity entity = MechSummary.loadEntity(fullName);
        if (entity == null) {
            throw new IllegalArgumentException("Could not retrieve unit " + fullName + " from cache!");
        }
        return entity;
    }

    private void assignPosition(Entity entity, JsonNode node) {
        try {
            if (node.has(AT)) {
                List<Integer> xyList = new ArrayList<>();
                node.get(AT).elements().forEachRemaining(n -> xyList.add(n.asInt()));
                setDeployedPosition(entity, new Coords(xyList.get(0), xyList.get(1)));

            } else if (node.has(X) || node.has(Y)) {
                requireFields("Entity", node, X, Y);
                setDeployedPosition(entity, new Coords(node.get(X).asInt(), node.get(Y).asInt()));
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
}
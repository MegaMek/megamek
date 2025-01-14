/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.common.autoresolve.converter;

import io.sentry.Sentry;
import megamek.common.*;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.copy.CrewRefBreak;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;

import java.util.*;

public class MMSetupForces extends SetupForces {
    private static final MMLogger logger = MMLogger.create(MMSetupForces.class);

    private final Game game;

    public MMSetupForces(Game game) {
        this.game = game;
    }

    /**
     * Create the forces for the game object, using the campaign, units and scenario
     * @param simulation The game object to setup the forces in
     */
    @Override
    public void createForcesOnSimulation(SimulationContext simulation) {
        for (var player : game.getPlayersList()) {
            setupPlayer(player, game.getInGameObjects(), simulation);
        }
        // the forces are present in "game" and should be applied to simulation
        simulation.setForces(game.getForces());
        convertForcesIntoFormations(simulation);
    }

    @Override
    public void addOrdersToForces(SimulationContext context) {
        // do nothing
    }

    private static class FailedToConvertForceToFormationException extends RuntimeException {
        public FailedToConvertForceToFormationException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Convert the forces in the game to formations, this is the most important step in the setup of the game,
     * it converts every top level force into a single formation, and those formations are then added to the game
     * and used in the auto resolve in place of the original entities
     * @param simulationContext The simulationContext which contains the forces to be converted
     */
    private void convertForcesIntoFormations(SimulationContext simulationContext) {
        if (!simulationContext.getForces().getAllForces().isEmpty()) {
            new KeepCurrentForces().consolidateForces(simulationContext);
            // Check the depth of the force, according to the depth of the force, it will either be
            for (var force : simulationContext.getForces().getTopLevelForces()) {
                try {
                    var formation = new LowestForceAsUnit(force, simulationContext).convert();
                    formation.setTargetFormationId(Entity.NONE);
                    simulationContext.addUnit(formation);
                } catch (Exception e) {
                    Sentry.captureException(e);
                    throw new FailedToConvertForceToFormationException(e);
                }
            }
            return;
        }

        for (var inGameObject : simulationContext.getInGameObjects()) {
            try {
                if (inGameObject instanceof Entity entity) {
                    var formation = new EntityAsFormation(entity, simulationContext).convert();
                    formation.setTargetFormationId(Entity.NONE);
                    simulationContext.addUnit(formation);
                }
            } catch (Exception e) {
                Sentry.captureException(e);
                throw new FailedToConvertForceToFormationException(e);
            }
        }

    }

    /**
     * Setup the player, its forces and entities in the game, it also sets the player skill level.
     * @param simulation The game object to setup the player in
     */
    private void setupPlayer(Player player, List<InGameObject> inGameObjects, SimulationContext simulation) {
        var cleanPlayer = getCleanPlayer(player);
        simulation.addPlayer(player.getId(), cleanPlayer);
        var playerObjects = inGameObjects.stream()
            .filter(Entity.class::isInstance)
            .filter(entity -> entity.getOwnerId() == player.getId())
            .map(Entity.class::cast)
            .toList();
        var entities = setupPlayerForces(playerObjects, cleanPlayer);
        sendEntities(entities, simulation);
    }

    /**
     * Create a player object from the campaign and scenario wichi doesnt have a reference to the original player
     * @return The clean player object
     */
    private Player getCleanPlayer(Player originalPlayer) {
        var player = new Player(originalPlayer.getId(), originalPlayer.getName());
        player.setCamouflage(originalPlayer.getCamouflage().clone());
        player.setColour(originalPlayer.getColour());
        player.setStartingPos(originalPlayer.getStartingPos());
        player.setStartOffset(originalPlayer.getStartOffset());
        player.setStartWidth(originalPlayer.getStartWidth());
        player.setStartingAnyNWx(originalPlayer.getStartingAnyNWx());
        player.setStartingAnyNWy(originalPlayer.getStartingAnyNWy());
        player.setStartingAnySEx(originalPlayer.getStartingAnySEx());
        player.setStartingAnySEy(originalPlayer.getStartingAnySEy());
        player.setTeam(originalPlayer.getTeam());
        player.setNbrMFActive(originalPlayer.getNbrMFActive());
        player.setNbrMFConventional(originalPlayer.getNbrMFConventional());
        player.setNbrMFInferno(originalPlayer.getNbrMFInferno());
        player.setNbrMFVibra(originalPlayer.getNbrMFVibra());
        player.getTurnInitBonus();
        return player;
    }

    /**
     * Setup the player forces and entities for the game
     * @param player The player object to setup the forces for
     * @return A list of entities for the player
     */
    private List<Entity> setupPlayerForces(List<Entity> inGameObjects, Player player) {
        var entities = new ArrayList<Entity>();
        for (var unit : inGameObjects) {
            var entity = ASConverter.getUndamagedEntity(unit);
            // Set the TempID for auto reporting
            if (Objects.isNull(entity)) {
                continue;
            }
            entity.setId(unit.getId());
            entity.setExternalIdAsString(unit.getExternalIdAsString());
            entity.setOwner(player);
            entity.setNCrew(unit.getNCrew());
            entity.setNMarines(unit.getNMarines());
            // Calculate deployment round
            int deploymentRound = entity.getDeployRound();
            entity.setDeployRound(deploymentRound);

            entity.setForceString(unit.getForceString());
            var newCrewRef = new CrewRefBreak(unit.getCrew()).copy();
            entity.setCrew(newCrewRef);
            entities.add(entity);
        }
        return entities;
    }

    /**
     * Send the entities to the game object
     * @param entities The entities to send
     * @param simulation the game object to send the entities to
     */
    private void sendEntities(List<Entity> entities, SimulationContext simulation) {
        Map<Integer, Integer> forceMapping = new HashMap<>();
        for (final Entity entity : new ArrayList<>(entities)) {
            if (entity instanceof ProtoMek) {
                int numPlayerProtos = simulation.getSelectedEntityCount(new EntitySelector() {
                    private final int ownerId = entity.getOwnerId();
                    @Override
                    public boolean accept(Entity entity) {
                        return (entity instanceof ProtoMek) && (ownerId == entity.getOwnerId());
                    }
                });

                entity.setUnitNumber((short) (numPlayerProtos / 5));
            }

            if (Entity.NONE == entity.getId()) {
                entity.setId(simulation.getNextEntityId());
            }

            // Give the unit a spotlight, if it has the spotlight quirk
            entity.setExternalSearchlight(entity.hasExternalSearchlight()
                || entity.hasQuirk(OptionsConstants.QUIRK_POS_SEARCHLIGHT));

            simulation.getPlayer(entity.getOwnerId()).changeInitialEntityCount(1);

            if (!entity.getForceString().isBlank()) {
                List<Force> forceList = Forces.parseForceString(entity);
                int realId = Force.NO_FORCE;
                boolean topLevel = true;

                for (Force force : forceList) {
                    if (!forceMapping.containsKey(force.getId())) {
                        if (topLevel) {
                            realId = simulation.getForces().addTopLevelForce(force, entity.getOwner());
                        } else {
                            Force parent = simulation.getForces().getForce(realId);
                            realId = simulation.getForces().addSubForce(force, parent);
                        }
                        forceMapping.put(force.getId(), realId);
                    } else {
                        realId = forceMapping.get(force.getId());
                    }
                    topLevel = false;
                }
                entity.setForceString("");
                entity.setIGame(simulation);
                simulation.addEntity(entity);
                simulation.getForces().addEntity(entity, realId);
            } else {
                entity.setIGame(simulation);
                simulation.addEntity(entity);
            }
        }
    }
}

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
package megamek.server;

import java.util.Enumeration;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.IndustrialElevator;
import megamek.common.Messages;
import megamek.common.Report;
import megamek.common.Team;
import megamek.common.actions.CallElevatorAction;
import megamek.common.actions.EntityAction;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Processes industrial elevator movement during the End Phase.
 * <p>
 * This processor handles player-controlled industrial elevators, which are distinct from the random Solaris 7 moving
 * walls handled by {@link ElevatorProcessor}.
 * <p>
 * Key responsibilities:
 * <ul>
 *   <li>Initialize elevators from terrain data on first round</li>
 *   <li>Process CallElevatorAction to queue elevator calls</li>
 *   <li>Move elevator platforms 1 level per turn toward nearest caller</li>
 *   <li>Check elevator functionality (disabled if shaft damaged)</li>
 *   <li>Generate movement reports</li>
 * </ul>
 *
 * @author MegaMek Team
 * @since 0.51.01
 */
public class IndustrialElevatorProcessor extends DynamicTerrainProcessor {

    private static final MMLogger LOGGER = MMLogger.create(IndustrialElevatorProcessor.class);

    private boolean initialized = false;

    public IndustrialElevatorProcessor(TWGameManager gameManager) {
        super(gameManager);
    }

    @Override
    public void doEndPhaseChanges(Vector<Report> vPhaseReport) {
        Game game = gameManager.getGame();

        // Initialize elevators only once (not every round)
        if (!initialized) {
            initializeElevators();
            initialized = true;
        }

        // Process any pending CallElevatorActions
        processCallElevatorActions(game, vPhaseReport);

        // Move each elevator toward its nearest caller
        processElevatorMovement(game, vPhaseReport);
    }

    /**
     * Scans the board for INDUSTRIAL_ELEVATOR terrain and creates elevator objects.
     * <p>
     * This method should be called during game initialization (after board is set) to ensure elevators are available
     * before movement phase.
     * <p>
     * Skips initialization if elevators already exist to preserve runtime state (platform positions from player
     * movement).
     */
    public void initializeElevators() {
        Game game = gameManager.getGame();

        // Skip if elevators already exist to preserve platform positions
        if (!game.getIndustrialElevators().isEmpty()) {
            initialized = true;
            return;
        }

        game.clearIndustrialElevators();

        for (Board board : game.getBoards().values()) {
            if (board.isLowAltitude() || board.isSpace()) {
                continue;
            }

            int height = board.getHeight();
            int width = board.getWidth();

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    Hex hex = board.getHex(x, y);
                    if (hex.containsTerrain(Terrains.INDUSTRIAL_ELEVATOR)) {
                        Terrain terrain = hex.getTerrain(Terrains.INDUSTRIAL_ELEVATOR);
                        BoardLocation location = BoardLocation.of(new Coords(x, y), board.getBoardId());
                        IndustrialElevator elevator = IndustrialElevator.fromTerrain(
                              location, terrain.getLevel(), terrain.getExits());
                        game.addIndustrialElevator(elevator);
                        // Show the platform's starting level on the board; the board carries this to clients
                        if (elevator.syncDisplayLevel(game)) {
                            markHexUpdate(elevator.getCoords(), elevator.getBoardId());
                        }
                    }
                }
            }
        }
    }

    /**
     * Processes pending CallElevatorAction actions from the game actions list.
     */
    private void processCallElevatorActions(Game game, Vector<Report> vPhaseReport) {
        for (Enumeration<EntityAction> actions = game.getActions(); actions.hasMoreElements(); ) {
            EntityAction action = actions.nextElement();
            if (action instanceof CallElevatorAction callAction) {
                processCallAction(game, callAction, vPhaseReport);
            }
        }
    }

    /**
     * Processes a single CallElevatorAction by adding it to the elevator's queue.
     */
    private void processCallAction(Game game, CallElevatorAction callAction, Vector<Report> vPhaseReport) {
        IndustrialElevator elevator = game.getIndustrialElevator(callAction.getElevatorLocation());
        if (elevator == null) {
            LOGGER.warn("[IndustrialElevator] Ignoring call action for entity {}: no elevator at {}",
                  callAction.getEntityId(), callAction.getElevatorLocation());
            return;
        }

        Entity caller = game.getEntity(callAction.getEntityId());
        if (caller == null) {
            LOGGER.warn("[IndustrialElevator] Ignoring call action for elevator at {}: calling entity {} not found",
                  callAction.getElevatorLocation(), callAction.getEntityId());
            return;
        }

        // Validate caller is adjacent to elevator
        if (!isAdjacentToElevator(caller, elevator)) {
            LOGGER.debug("[IndustrialElevator] Ignoring call by {}: not adjacent to elevator at {}",
                  caller.getShortName(), elevator.getLocation());
            return;
        }

        // Create the elevator call
        int turnCalled = game.getRoundCount();
        int initiativeBonus = getPlayerInitiative(game, caller.getOwnerId());
        IndustrialElevator.ElevatorCall call = new IndustrialElevator.ElevatorCall(
              caller.getOwnerId(),
              caller.getPosition(),
              callAction.getTargetLevel(),
              turnCalled,
              initiativeBonus,
              elevator.getPlatformLevel()
        );

        elevator.addCall(call);

        // Generate report: Unit called elevator
        Report report = new Report(5295, Report.PUBLIC);
        report.subject = callAction.getEntityId();
        report.add(caller.getDisplayName());
        report.add(callAction.getTargetLevel());
        vPhaseReport.add(report);
    }

    /**
     * Checks if an entity is adjacent to the elevator hex.
     */
    private boolean isAdjacentToElevator(Entity entity, IndustrialElevator elevator) {
        Coords entityPos = entity.getPosition();
        Coords elevatorPos = elevator.getCoords();
        return entityPos != null && elevatorPos != null
              && entityPos.distance(elevatorPos) == 1
              && entity.getBoardId() == elevator.getBoardId();
    }

    /**
     * Gets the initiative value for a player (for tie-breaking in queue). Higher initiative means earlier in the turn
     * order, which wins call-queue ties.
     */
    private int getPlayerInitiative(Game game, int playerId) {
        Team team = game.getTeamForPlayer(game.getPlayer(playerId));
        if (team == null) {
            LOGGER.warn("[IndustrialElevator] No team found for player {}; using initiative 0 for call priority",
                  playerId);
            return 0;
        }
        return team.getInitiative().getRoll(0);
    }

    /**
     * Moves each elevator toward its nearest caller.
     */
    private void processElevatorMovement(Game game, Vector<Report> vPhaseReport) {
        for (IndustrialElevator elevator : game.getIndustrialElevators()) {
            if (!elevator.isFunctional()) {
                continue;
            }

            // Check if overloaded
            if (!elevator.canMove(game)) {
                Report report = new Report(5296, Report.PUBLIC);
                report.add(elevator.getLocation().toFriendlyString());
                report.add((int) elevator.getCurrentLoad(game));
                report.add(elevator.getCapacityTons());
                vPhaseReport.add(report);
                continue;
            }

            int previousLevel = elevator.getPlatformLevel();
            boolean moved = elevator.processEndPhaseMovement(game);

            if (moved) {
                int newLevel = elevator.getPlatformLevel();
                String direction = (newLevel > previousLevel)
                      ? Messages.getString("IndustrialElevator.directionUp")
                      : Messages.getString("IndustrialElevator.directionDown");

                // Report elevator movement
                Report report = new Report(5297, Report.PUBLIC);
                report.add(elevator.getLocation().toFriendlyString());
                report.add(direction);
                report.add(previousLevel);
                report.add(newLevel);
                vPhaseReport.add(report);

                // Update entity elevations that were on the platform
                updateEntitiesOnPlatform(game, elevator, previousLevel, newLevel);

                // Update the hex so the tileset shows the platform's new level, then mark it for re-render
                elevator.syncDisplayLevel(game);
                markHexUpdate(elevator.getCoords(), elevator.getBoardId());
            }
        }
    }

    /**
     * Updates the elevation of entities that were riding the elevator platform.
     */
    private void updateEntitiesOnPlatform(Game game, IndustrialElevator elevator,
          int previousLevel, int newLevel) {
        for (Entity entity : game.getEntitiesVector()) {
            Coords entityPos = entity.getPosition();
            if (entityPos != null
                  && entityPos.equals(elevator.getCoords())
                  && entity.getBoardId() == elevator.getBoardId()
                  && entity.getElevation() == previousLevel) {
                entity.setElevation(newLevel);
            }
        }
    }
}

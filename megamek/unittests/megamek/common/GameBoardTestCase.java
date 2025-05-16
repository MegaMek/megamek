/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.function.Function;

import megamek.common.annotations.Nullable;
import megamek.common.moves.MovePath;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for all tests that need a game board.
 * @author Luana Coppio
 */
public abstract class GameBoardTestCase {

    static {
        /*
         * Initialize the EquipmentType class.
         * This is necessary to initialize entities.
         */
        EquipmentType.initializeTypes();
    }

    /**
     * Map of all boards used in the tests.
     * The BOARDS should be initialized in the static block of the test class.
     * Example:
     * <pre>
     *     static {
     *         initializeBoard("BOARD_NAME","""
     * size 1 2
     * hex 0101 0 "" ""
     * hex 0102 0 "bldg_elev:6;building:2:8;bldg_cf:100" ""
     * end"""
     *         );
     *     }
     * </pre>
     */
    protected static final Map<String, Board> BOARDS = new HashMap<>();
    private Game game;
    private Entity entity;

    /**
     * Load a board from a string
     * @param name name for the board
     * @param data the board as a string
     */
    protected static void initializeBoard(String name, String data) {
        int[] size = parseBoardSize(data);
        Board board = new Board(size[0], size[1]);
        List<String> errors = new ArrayList<>();
        board.load(data, errors);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Errors loading board " + name + ": " + errors);
        }
        BOARDS.put(name, board);
    }

    /**
     * Parse the board size from the data string
     * @param data the data string
     * @return an array with the width and height of the board
     */
    private static int[] parseBoardSize(String data) {
        try {
            String[] lines = data.split("\n");
            String[] size = lines[0].split(" ");
            int width = Integer.parseInt(size[1]);
            int height = Integer.parseInt(size[2]);
            return new int[]{width, height};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid board size in data: " + data, e);
        }
    }

    /**
     * Get a board by name
     * @param name the name of the board
     * @return the board
     * @throws IllegalArgumentException if the board is not found
     */
    protected Board getBoard(String name) {
        if (!BOARDS.containsKey(name)) {
            throw new IllegalArgumentException("Board " + name + " not found");
        }
        return BOARDS.get(name);
    }

    /**
     * Get the game instance
     * @return the game instance
     */
    protected Game getGame() {
        return game;
    }

    /**
     * Get the entity instance
     * @return the entity instance
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * Set the board for the game instance
     * @param name the name of the board loaded in the initialization of the class
     */
    protected void setBoard(String name) {
        getGame().setBoard(getBoard(name));
    }

    /**
     * Initialize a unit for the test
     * @param unit the Entity instance to be initializes
     * @param game the game
     * @param movementMode the movement mode
     * @param startingElevation the starting elevation (Integer.MAX_VALUE means to place on the board
     *                          {@link Hex#ceiling()})
     * @return the initialized unit
     * @param <T> the type of the unit extended from Entity
     */
    private static <T extends Entity> T initializeUnit(
          T unit,
          Game game,
          @Nullable EntityMovementMode movementMode,
          int startingElevation
    ) {
        if (unit.getId() == Entity.NONE) {
            if (movementMode != null) {
                unit.setMovementMode(movementMode);
            }
            if (unit instanceof Infantry) {
                unit.setWeight(5.0);
            } else {
                unit.setWeight(50.0);
            }
            unit.setOriginalWalkMP(8);
            unit.setOriginalJumpMP(8);
            unit.setId(5);
        }

        game.addEntity(unit);
        unit.setPosition(new Coords(0, 0));
        unit.setFacing(3);

        if (startingElevation != Integer.MAX_VALUE) {
            unit.setElevation(startingElevation);
        } else {
            int elevationModifier = EntityMovementMode.WIGE.equals(movementMode) ? 1 : 0;
            int ceiling = game.getBoard().getHex(unit.getPosition()).ceiling();
            int floor = game.getBoard().getHex(unit.getPosition()).floor();
            unit.setElevation(ceiling - floor + elevationModifier);
        }

        return unit;
    }

    protected List<MovePath> getMovePathFuzzerFor(Entity entity, int maxSteps, Function<MovePath, Boolean> function) {
        // Initialize the starting path for this entity
        MovePath initialPath = new MovePath(getGame(), initializeUnit(entity, getGame(), null, Integer.MAX_VALUE));
        List<MovePath> paths = new LinkedList<>();
        // Queue for BFS
        Queue<MovePath> queue = new LinkedList<>();
        queue.offer(initialPath);
        try {
            while (!queue.isEmpty()) {
                MovePath currentPath = queue.poll();

                // Check if this path satisfies our condition
                if (function.apply(currentPath)) {
                    paths.add(currentPath.clone());
                }

                // Don't exceed maximum path length
                if (currentPath.getStepVector().size() >= maxSteps) {
                    continue;
                }

                // Try all possible move types
                for (MovePath.MoveStepType moveStepType : MovePath.MoveStepType.values()) {
                    // Skip irrelevant or invalid movement types for this entity
                    if (!isValidStepType(currentPath, moveStepType)) {
                        continue;
                    }

                    // Create a new path by adding this step
                    MovePath newPath = currentPath.clone();
                    newPath.addStep(moveStepType);

                    // Only add legal moves to the queue
                    if (newPath.isMoveLegal()) {
                        var steps = newPath.getStepVector();
                        for (int i = 0; i < steps.size(); i++) {
                            System.out.println("Step " + steps.get(i) + " - " + i + ": " + new StepLog(steps.get(i)));
                        }
                        queue.offer(newPath);
                    }
                }
            }
        } catch (Exception e) {
            // If there's an exception during callable execution, return best path so far
        }

        return paths;
    }

    /**
     * Checks if a move step type is valid for the current path and entity
     * @param path The current path
     * @param stepType The step type to check
     * @return true if valid, false otherwise
     */
    private boolean isValidStepType(MovePath path, MovePath.MoveStepType stepType) {
        Entity entity = path.getEntity();

        // Filter out irrelevant move types based on entity type and capabilities
        return switch (stepType) {
            case START_JUMP -> entity.getOriginalJumpMP() > 0;
            case SWIM -> entity.hasUMU();
            case HOVER -> Set.of(EntityMovementMode.HOVER, EntityMovementMode.WIGE, EntityMovementMode.VTOL).contains(entity.getMovementMode());
            case DFA -> entity instanceof Mek;
            case CHARGE -> entity instanceof Mek || entity instanceof Tank;
            case LAY_MINE, CHAFF, CLEAR_MINEFIELD, BRACE, SELF_DESTRUCT -> false;
            default -> true;
        };
    }

    /**
     * Generates the MovePath for the test
     * @param entity the entity
     * @param startingElevation the starting elevation
     * @param movementMode the movement mode
     * @param steps the steps to be added to the MovePath
     * @return the MovePath
     */
    protected MovePath getMovePathFor(Entity entity, int startingElevation, @Nullable EntityMovementMode movementMode,
          MovePath.MoveStepType ... steps) {
        return getMovePath(new MovePath(getGame(), initializeUnit(entity, getGame(), movementMode, startingElevation)),
              steps);
    }

    /**
     * Generates the MovePath for the test
     * @param entity the entity
     * @param steps the steps to be added to the MovePath
     * @return the MovePath
     */
    protected MovePath getMovePathFor(Entity entity, MovePath.MoveStepType ... steps) {
        return getMovePathFor(entity, Integer.MAX_VALUE, null, steps);
    }

    /**
     * Generates the MovePath for the test
     * @param entity the entity
     * @param movementMode the movement mode
     * @param steps the steps to be added to the MovePath
     * @return the MovePath
     */
    protected MovePath getMovePathFor(Entity entity, @Nullable EntityMovementMode movementMode,
          MovePath.MoveStepType ... steps) {
        return getMovePathFor(entity, Integer.MAX_VALUE, movementMode, steps);
    }

    /**
     * Generates the movepath for the test
     * @param path the game
     * @return the MovePath
     */
    private static MovePath getMovePath(final MovePath path,final MovePath.MoveStepType... steps) {
        MovePath movePath = path.clone();
        for (var step : steps) {
            movePath = movePath.addStep(step);
        }
        return movePath;
    }

    public record ExpectedElevation(int expectedElevation, String justification) {
        public static ExpectedElevation of(int expectedElevation, String justification) {
            return new ExpectedElevation(expectedElevation, justification);
        }
    }

    /**
     *
     * @param movePath the MovePath
     * @param expectedElevations list of integers representing the expected elevation for each of the steps on the
     *                           MovePath
     */
    public void assertMovePathElevations(MovePath movePath, int ... expectedElevations) {
        assertMovePathElevations(movePath,
              Arrays.stream(expectedElevations)
                    .mapToObj(i -> ExpectedElevation.of(i, ""))
                    .toArray(ExpectedElevation[]::new));
    }

    /**
     *
     * @param movePath the MovePath
     * @param expectedElevations list of ExpectedElevation representing the expected elevation for each of the steps on
     *                           the MovePath and the justification for the expected elevation
     */
    public void assertMovePathElevations(MovePath movePath, ExpectedElevation ... expectedElevations) {
        Vector<MoveStep> steps = movePath.getStepVector();
        assertEquals(steps.size(), expectedElevations.length, "Number of expected elevations must match the " +
                                                                    "number of steps on movePath.");
        for (int i = 0; i < steps.size(); i++) {
            Hex hex = getGame().getBoard().getHex(steps.get(i).getPosition());
            assertEquals(expectedElevations[i].expectedElevation(), steps.get(i).getElevation(),
                  "Step " + steps.get(i) + " - " + i + ": " + new StepLog(movePath.getStepVector().get(i)) +
                        " on hex: " + hex.toString() + " doesn't match the expected elevation" +
                        expectedElevations[i].justification());
        }
    }


    /**
     * Before each test, reset the game and the entity
     * to not interfere with other tests
     */
    @BeforeEach
    void setUpEach() {
        entity = null;
        game = new Game();
    }
}

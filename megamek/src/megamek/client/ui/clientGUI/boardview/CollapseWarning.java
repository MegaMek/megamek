/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.boardview;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.panels.phaseDisplay.DeploymentDisplay;
import megamek.client.ui.panels.phaseDisplay.MovementDisplay;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.logging.MMLogger;

/**
 * Construction Factor Warning Logic. Handles events, help methods and logic related to CF Warning in a way that can be
 * unit tested and encapsulated from BoardView and ClientGUI and other actors.
 */
public final class CollapseWarning {
    private final static MMLogger logger = MMLogger.create(CollapseWarning.class);

    /*
     * Handler for ClientGUI actionPerformed event. Encapsulates
     * as much Construction Factory Warning logic possible.
     */
    public static void handleActionPerformed() {
        toggleCFWarning();
    }

    /*
     * Return true if the passed in phase is a phase that should allow
     * Construction Factor Warnings such as Deploy and Movement.
     */
    public static boolean isCFWarningPhase(GamePhase gp) {
        return (gp == GamePhase.DEPLOYMENT || gp == GamePhase.MOVEMENT);
    }

    /*
     * Returns true if the show construction factor warning preference
     * is enabled and in a phase that should show warnings.
     */
    public static boolean shouldShow(GamePhase gp, boolean isEnabled) {
        return (isEnabled && isCFWarningPhase(gp));
    }

    private static void toggleCFWarning() {
        // Toggle the GUI Preference setting for CF Warning setting.
        GUIPreferences GUIP = GUIPreferences.getInstance();
        GUIP.setShowCFWarnings(!GUIP.getShowCFWarnings());
        GUIP.getShowCFWarnings();
    }

    public static List<BoardLocation> findCFWarningsMovement(Game game, Entity entity) {
        // Since this is limited to ground units, only show warnings on their board
        int boardId = entity.getBoardId();
        var warnList = new LinkedList<BoardLocation>();
        if (game.hasBoard(boardId)) {
            List<Coords> boardWarnings = findCFWarningsMovement(game, entity, game.getBoard(boardId));
            warnList.addAll(boardWarnings.stream().map(c -> BoardLocation.of(c, boardId)).toList());
            warnList.removeIf(BoardLocation::isNoLocation);
        }
        return warnList;
    }

    /**
     * For the provided entity, find all hexes within movement range with buildings that would collapse if entered or
     * landed upon. This is used by the {@link MovementDisplay} class.
     *
     * @param game   {@link Game} provided by the phase display class
     * @param entity {@link Entity} currently selected in the movement phase.
     * @param board  {@link Board} board object with building data.
     *
     * @return returns a list of {@link Coords} that where warning flags should be placed.
     */
    public static List<Coords> findCFWarningsMovement(Game game, Entity entity, Board board) {
        List<Coords> warnList = new ArrayList<>();

        // If we don't have an entity, can't do anything.
        if (entity == null) {
            return warnList;
        }

        try {
            // Only calculate CF Warnings for entity types in the whitelist.
            if (!entityTypeIsInWhiteList(entity)) {
                return warnList;
            }

            Coords pos = entity.getPosition();
            int range = Math.max(entity.getAnyTypeMaxJumpMP(), entity.getRunMP());

            List<Coords> hexesToCheck = new ArrayList<>();
            if (pos != null) {
                hexesToCheck = pos.allAtDistanceOrLess(range);
            } else {
                return hexesToCheck;
            }

            // For each hex in jumping range, look for buildings, if found check for collapse.
            for (Coords c : hexesToCheck) {
                // is there a building at this location? If so add it to hexes with buildings.
                IBuilding bld = board.getBuildingAt(c);

                // If a building, compare total weight and add to warning list.
                if (null != bld) {
                    if (calculateTotalTonnage(game, entity, c) > bld.getCurrentCF(c)) {
                        warnList.add(c);
                    }
                }
            }
        } catch (Exception exc) {
            // Something bad is going to happen. This is a passive feature return an empty list.
            logger.error(exc, "Unable to calculate construction factor collapse candidates. (Movement)");
            return new ArrayList<>();
        }

        return warnList;
    }

    /*
     * Returns true if the selected entity should have CF warnings calculated when
     * selected.
     */
    public static boolean entityTypeIsInWhiteList(Entity entity) {
        // Include entities that are ground units and onboard only. Flying units need not apply.
        return (entity.isGround() && !entity.isOffBoard());
    }

    public static List<BoardLocation> findCFWarningsDeployment(Game game, Entity entity) {
        List<BoardLocation> warnList = new LinkedList<>();
        for (Board board : game.getBoards().values()) {
            List<Coords> boardWarnings = findCFWarningsDeployment(game, entity, board);
            warnList.addAll(boardWarnings.stream().map(c -> BoardLocation.of(c, board.getBoardId())).toList());
        }
        warnList.removeIf(BoardLocation::isNoLocation);
        return warnList;
    }

    /**
     * Looks for all building locations in a legal deploy zone that would collapse if the currently selected entity
     * would deploy there. This is used by {@link DeploymentDisplay} to render a warning sprite on danger hexes.
     *
     * @param game   {@link Game} provided by the phase display class
     * @param entity {@link Entity} currently selected in the movement phase.
     * @param board  {@link Board} board object with building data.
     *
     * @return returns a list of {@link Coords} that where warning flags should be placed.
     */
    public static List<Coords> findCFWarningsDeployment(Game game, Entity entity, Board board) {
        List<Coords> warnList = new ArrayList<>();

        try {
            // Only calculate CF Warnings for entity types in the whitelist.
            if (!entityTypeIsInWhiteList(entity)) {
                return warnList;
            }

            Enumeration<IBuilding> buildings = board.getBuildings();

            // Enumerate through all the buildings
            while (buildings.hasMoreElements()) {
                IBuilding bld = buildings.nextElement();
                List<Coords> buildingList = bld.getCoordsList();

                // For each hex occupied by the building, check if it's a legal deploy hex.
                for (Coords c : buildingList) {
                    if (board.isLegalDeployment(c, entity)) {
                        // Check for weight limits for collapse and add a warning sprite.
                        if (calculateTotalTonnage(game, entity, c) > bld.getCurrentCF(c)) {
                            warnList.add(c);
                        }
                    }
                }
            }
        } catch (Exception exc) {
            // Something bad is going to happen. This is a passive feature return an empty
            // list.
            logger.error(exc, "Unable to calculate construction factor collapse candidates. (Deployment)");
            return new ArrayList<>();
        }

        return warnList;
    }

    /*
     * Determine the total weight burden for a building hex at a location.
     * This includes the entity current weight summed with any unit weights
     * at the hex location that could cause a building to collapse.
     */
    public static double calculateTotalTonnage(Game g, Entity selected, Coords c) {
        // Calculate total weight of entity and all entities at the location.
        double totalWeight = selected.getWeight();
        List<Entity> units = g.getEntitiesVector(c, true);
        for (Entity ent : units) {
            if (CollapseWarning.isEntityPartOfWeight(selected, ent)) {
                totalWeight += ent.getWeight();
            }
        }
        return totalWeight;
    }

    private static boolean isEntityPartOfWeight(Entity selected, Entity inHex) {
        return ((selected != inHex) && inHex.isGround() && !inHex.isAirborneVTOLorWIGE());
    }

    private CollapseWarning() {
    }
}

/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.planetaryConditions.Wind;
import megamek.common.planetaryConditions.WindDirection;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

public class FireProcessor extends DynamicTerrainProcessor {
    private static final MMLogger logger = MMLogger.create(FireProcessor.class);

    private Game game;
    Vector<Report> vPhaseReport;

    public FireProcessor(TWGameManager gameManager) {
        super(gameManager);
    }

    @Override
    public void doEndPhaseChanges(Vector<Report> vPhaseReport) {
        game = gameManager.getGame();
        this.vPhaseReport = vPhaseReport;
        removeSmokeTerrainFromHexes();
        game.getBoardIds().forEach(this::resolveFire);
        reapplySmokeTerrain();
        this.vPhaseReport = null;
    }

    /**
     * Make fires spread, smoke spread, and make sure that all fires started this turn are marked as "burning" for next
     * turn. What turn the fire started on is no longer determined by level but is rather a characteristic of the hex.
     * Level now denotes standard and inferno fires.
     */
    private void resolveFire(int boardId) {
        Board board = game.getBoard(boardId);
        if (board.isLowAltitude() || board.isSpace()) {
            return;
        }
        int width = board.getWidth();
        int height = board.getHeight();
        WindDirection windDirection = game.getPlanetaryConditions().getWindDirection();
        Wind windStrength = game.getPlanetaryConditions().getWind();

        // Get the position map of all entities in the game.
        Map<BoardLocation, List<Entity>> positionMap = game.getPositionMapMulti();

        // process smoke FIRST, before any fires spread or
        // smoke is produced.
        resolveSmoke(boardId);

        // Cycle through all buildings, checking for fire.
        // ASSUMPTION: buildings don't lose 2 CF on the turn a fire starts.
        // ASSUMPTION: multi-hex buildings lose 2 CF in each burning hex
        Enumeration<IBuilding> buildings = board.getBuildings();
        while (buildings.hasMoreElements()) {
            IBuilding bldg = buildings.nextElement();
            Enumeration<Coords> bldgCoords = bldg.getCoords();
            while (bldgCoords.hasMoreElements()) {
                Coords coords = bldgCoords.nextElement();
                if (bldg.isBurning(coords)) {
                    int cf = Math.max(bldg.getCurrentCF(coords) - 2, 0);
                    bldg.setCurrentCF(cf, coords);

                    // Does the building burn down?
                    if (cf == 0) {
                        vPhaseReport.addElement(Report.publicReport(5120).add(bldg.getName()));
                    } else if (!gameManager.checkForCollapse(bldg, positionMap, coords, false, vPhaseReport)) {
                        // If it doesn't collapse under its load, mark it for update.
                        bldg.setPhaseCF(cf, coords);
                    }
                }
            }
        }

        // Cycle through all hexes, checking for fire and the spread of fire
        for (int currentXCoord = 0; currentXCoord < width; currentXCoord++) {
            for (int currentYCoord = 0; currentYCoord < height; currentYCoord++) {
                Coords currentCoords = new Coords(currentXCoord, currentYCoord);
                Hex currentHex = board.getHex(currentXCoord, currentYCoord);

                if (currentHex.containsTerrain(Terrains.FIRE)) {
                    // If the woods have been cleared, or the building
                    // has collapsed put non-inferno fires out.
                    if ((currentHex.terrainLevel(Terrains.FIRE) == Terrains.FIRE_LVL_NORMAL)
                          && !currentHex.isIgnitable()) {
                        gameManager.removeFire(currentCoords, "lack of fuel");
                        continue;
                    }

                    // only check spread for fires that didn't start this turn
                    if (currentHex.getFireTurn() > 0) {
                        // optional rule, woods burn down
                        Vector<Report> burnReports = null;
                        if ((currentHex.containsTerrain(Terrains.WOODS)
                              || currentHex.containsTerrain(Terrains.JUNGLE))
                              && game.getOptions().booleanOption(OptionsConstants.ADVANCED_WOODS_BURN_DOWN)) {
                            burnReports = burnDownWoods(currentCoords, board);
                        }
                        // report and check for fire spread
                        boolean isInferno = (currentHex.terrainLevel(Terrains.FIRE) == Terrains.FIRE_LVL_INFERNO)
                              || (currentHex.terrainLevel(Terrains.FIRE) == Terrains.FIRE_LVL_INFERNO_BOMB)
                              || (currentHex.terrainLevel(Terrains.FIRE) == Terrains.FIRE_LVL_INFERNO_IV);
                        vPhaseReport.addElement(
                              Report.publicReport(isInferno ? 5130 : 5125).add(currentCoords.getBoardNum()));
                        if (burnReports != null) {
                            vPhaseReport.addAll(burnReports);
                        }
                        spreadFire(board, currentXCoord, currentYCoord, windDirection, windStrength);
                    }
                }
            }
        }

        // Cycle through all hexes again, reporting new fires, spreading smoke, and
        // incrementing the fire turn.
        // Can't do this in first loop because new fires may be spread
        for (int currentXCoord = 0; currentXCoord < width; currentXCoord++) {
            for (int currentYCoord = 0; currentYCoord < height; currentYCoord++) {
                Coords currentCoords = new Coords(currentXCoord, currentYCoord);
                Hex currentHex = board.getHex(currentXCoord, currentYCoord);

                if (currentHex.containsTerrain(Terrains.FIRE)) {
                    IBuilding bldg = board.getBuildingAt(currentCoords);
                    // Was the fire started this turn?
                    if (currentHex.getFireTurn() == 0) {
                        // Report fire started this round
                        vPhaseReport.addElement(Report.publicReport(5135).add(currentCoords.getBoardNum()));

                        // If the hex contains a building, set it on fire.
                        if (bldg != null) {
                            bldg.setBurning(true, currentCoords);
                        }
                    }

                    // Check for any explosions
                    gameManager.checkExplodeIndustrialZone(currentCoords, boardId, vPhaseReport);

                    // Add smoke, unless tornado or optional rules
                    boolean containsForest = (currentHex.containsTerrain(Terrains.WOODS)
                          || currentHex.containsTerrain(Terrains.JUNGLE));
                    boolean bInferno = currentHex.terrainLevel(Terrains.FIRE) == 2;
                    PlanetaryConditions conditions = game.getPlanetaryConditions();
                    if (conditions.getWind().isWeakerThan(Wind.TORNADO_F1_TO_F3)
                          && !(game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_FOREST_FIRES_NO_SMOKE)
                          && containsForest
                          && (bldg == null))) {
                        ArrayList<Coords> smokeList = new ArrayList<>();

                        smokeList.add(currentCoords.translated(windDirection.ordinal()));
                        smokeList.add(currentCoords.translated(windDirection.rotateClockwise().ordinal()));
                        smokeList.add(currentCoords.translated(windDirection.rotateCounterClockwise().ordinal()));

                        gameManager.addSmoke(smokeList, board, bInferno);
                        board.initializeAround(currentXCoord, currentYCoord);
                    }

                    // increment the fire turn counter
                    currentHex.incrementFireTurn();
                    markHexUpdate(currentCoords, boardId);
                }
            }
        }
    }

    public Vector<Report> burnDownWoods(Coords coords, Board board) {
        Vector<Report> burnReports = new Vector<>();
        int burnDamage = 5;
        try {
            burnDamage = game.getOptions().intOption(OptionsConstants.ADVANCED_WOODS_BURN_DOWN_AMOUNT);
        } catch (Exception ex) {
            logger.error("", ex);
        }

        // Report that damage applied to terrain
        burnReports.addElement(Report.publicReport(3383).indent().add(burnDamage));

        Vector<Report> newReports = gameManager.tryClearHex(coords, board.getBoardId(), burnDamage, Entity.NONE);
        for (Report nr : newReports) {
            nr.indent(2);
        }
        burnReports.addAll(newReports);
        return burnReports;
    }

    /**
     * Spreads the fire around the specified coordinates.
     */
    public void spreadFire(Board board, int x, int y, WindDirection windDir, Wind windStr) {
        Coords src = new Coords(x, y);
        Coords nextCoords = src.translated(windDir.ordinal());

        // check for height differences between hexes
        // TODO: until further clarification only the heights matter (not the base
        // elevation)
        // This means that a fire cannot spread from a level 6 building at base level 0
        // to
        // a level 1 building at base level 0, for example.

        final int curHeight = board.getHex(src).ceiling();

        TargetRoll directRoll = new TargetRoll(9, "spread downwind");
        TargetRoll obliqueRoll = new TargetRoll(11, "spread 60 degrees to downwind");

        if (windStr.isLightGale()
              || windStr.isModerateGale()) {
            directRoll.addModifier(-2, "light/moderate gale");
            obliqueRoll.addModifier(-1, "light/moderate gale");
        } else if (windStr.isStrongerThan(Wind.MOD_GALE)) {
            directRoll.addModifier(-3, "strong gale+");
            directRoll.addModifier(-2, "strong gale+");
        }

        spreadFire(board, src, nextCoords, directRoll, curHeight);

        // Spread to the next hex downwind on a 12 if the first hex wasn't
        // burning...
        // unless a higher hex intervenes
        Hex nextHex = board.getHex(nextCoords);
        Hex jumpHex = board.getHex(nextCoords.translated(windDir.ordinal()));
        if ((nextHex != null) && (jumpHex != null) && !(nextHex.containsTerrain(Terrains.FIRE))
              && ((curHeight >= nextHex.ceiling()) || (jumpHex.ceiling() >= nextHex.ceiling()))) {
            // we've already gone one step in the wind direction, now go another
            directRoll.addModifier(3, "crossing non-burning hex");
            spreadFire(board, src, nextCoords.translated(windDir.ordinal()), directRoll, curHeight);
        }

        // spread fire 60 degrees clockwise....
        spreadFire(board, src, src.translated(windDir.rotateClockwise().ordinal()), obliqueRoll, curHeight);

        // spread fire 60 degrees counterclockwise
        spreadFire(board, src, src.translated(windDir.rotateCounterClockwise().ordinal()), obliqueRoll, curHeight);
    }

    /**
     * Spreads the fire, and reports the spread, to the specified hex, if possible, if the hex isn't already on fire,
     * and the fire roll is made.
     *
     * @param origin the origin coordinates
     * @param coords the coordinates to check to see if the fire spreads to them
     * @param roll   the target number for roll for fire to spread
     * @param height the height of the origin hex
     */
    public void spreadFire(Board board, Coords origin, Coords coords, final TargetRoll roll,
          int height) {
        Hex hex = board.getHex(coords);
        if ((hex == null) || (Math.abs(hex.ceiling() - height) > 4)) {
            // Don't attempt to spread fire off the board or for large differences in height
            return;
        }

        if (!(hex.containsTerrain(Terrains.FIRE)) && gameManager.checkIgnition(coords, board.getBoardId(), roll)) {
            vPhaseReport.addElement(Report.publicReport(5150).add(coords.getBoardNum()).add(origin.getBoardNum()));
        }
    }

    /** Processes smoke drift and dissipation. */
    private void resolveSmoke(int boardId) {
        final Board board = game.getBoard(boardId);
        final WindDirection windDir = game.getPlanetaryConditions().getWindDirection();
        PlanetaryConditions conditions = game.getPlanetaryConditions();
        Wind windStr = game.getPlanetaryConditions().getWind();

        // If the Smoke Drift option is turned on, treat wind strength as light gale if
        // there is none
        if (game.getOptions().booleanOption(OptionsConstants.BASE_BREEZE)
              && conditions.getWind().isCalm()) {
            windStr = Wind.LIGHT_GALE;
        }

        HashMap<SmokeCloud, ArrayList<Coords>> allReplacementHexes = new HashMap<>();

        // Process smoke drifting
        for (SmokeCloud cloud : preExistingSmokeClouds()) {
            markHexUpdate(cloud.getCoordsList(), boardId);

            final ArrayList<Coords> replacementHexes = new ArrayList<>();
            for (Coords currentCoords : cloud.getCoordsList()) {
                Coords smokeCoords = driftAddSmoke(currentCoords, board, windDir, windStr);
                if (smokeCoords == null) {
                    // Smoke has Dissipated by moving into a hex with a greater than 4 elevation
                    // drop.
                    vPhaseReport.addElement(new Report(5220).makePublic().add(currentCoords.getBoardNum()));
                    vPhaseReport.addElement(new Report(5222, Report.PUBLIC));
                } else if (!board.contains(smokeCoords)) {
                    // Smoke has blown off the map
                    vPhaseReport.addElement(new Report(5230).makePublic().add(currentCoords.getBoardNum()));
                } else if (!currentCoords.equals(smokeCoords)) {
                    // Smoke has drifted
                    replacementHexes.add(smokeCoords);
                    cloud.setDrift(true);
                } else {
                    // Smoke hasn't moved
                    replacementHexes.add(smokeCoords);
                }
            }
            allReplacementHexes.put(cloud, replacementHexes);
        }
        allReplacementHexes.keySet().forEach(cloud -> cloud.replaceCoords(allReplacementHexes.get(cloud)));

        game.removeEmptySmokeClouds();

        // Process smoke dissipation
        for (SmokeCloud cloud : preExistingSmokeClouds()) {
            boolean dissipated = checkSmokeDissipation(cloud, windStr);
            if (dissipated || cloud.didDrift()) {
                driftSmokeReport(cloud, dissipated);
                if (dissipated) {
                    cloud.reduceSmokeLevel();
                }
            }
            cloud.setDrift(false);
        }

        game.removeCompletelyDissipatedSmokeClouds();
    }

    /**
     * (Re-)Applies smoke (Terrains.SMOKE) to all hexes of all smoke clouds on all boards and marks the hexes for client
     * update.
     */
    private void reapplySmokeTerrain() {
        for (SmokeCloud cloud : game.getSmokeCloudList()) {
            for (Coords coords : cloud.getCoordsList()) {
                Hex smokeHex = gameManager.getGame().getBoard(cloud.getBoardId()).getHex(coords);
                if (smokeHex != null) {
                    if (smokeHex.containsTerrain(Terrains.SMOKE)) {
                        if (smokeHex.terrainLevel(Terrains.SMOKE) == SmokeCloud.SMOKE_LIGHT) {
                            smokeHex.addTerrain(new Terrain(Terrains.SMOKE, SmokeCloud.SMOKE_HEAVY));
                            markHexUpdate(coords, cloud.getBoardId());
                        }
                    } else {
                        smokeHex.addTerrain(new Terrain(Terrains.SMOKE, cloud.getSmokeLevel()));
                        markHexUpdate(coords, cloud.getBoardId());
                    }
                }
            }
        }
    }

    /**
     * @return A list of the game's smoke clouds that have existed before the start of this round.
     */
    private List<SmokeCloud> preExistingSmokeClouds() {
        final int currentRound = gameManager.getGame().getRoundCount();
        return gameManager.getSmokeCloudList().stream()
              .filter(cloud -> currentRound != cloud.getRoundOfGeneration())
              .toList();
    }

    /** Removes smoke (Terrains.SMOKE) from all hexes of all smoke clouds on all boards. */
    private void removeSmokeTerrainFromHexes() {
        for (SmokeCloud cloud : game.getSmokeCloudList()) {
            game.getBoard(cloud.getBoardId()).getHexes(cloud.getCoordsList())
                  .forEach(h -> h.removeTerrain(Terrains.SMOKE));
        }
    }

    /**
     * Override for the main driftAddSmoke to allow for 0 direction changes
     *
     * @param source        the source coordinates
     * @param windDirection the wind's direction
     * @param windStrength  the wind's strength
     *
     * @return the coordinates where the smoke has drifted to, or null if it dissipates while on the board.
     */
    public @Nullable Coords driftAddSmoke(final Coords source, Board board, final WindDirection windDirection,
          final Wind windStrength) {
        return driftAddSmoke(source, board, windDirection, windStrength, 0);
    }

    /**
     * Smoke cannot climb more than 4 hexes if the next hex is more than 4 in elevation then the smoke will try to go
     * right. If it cannot go right it'll try to go left. If it cannot go left it'll stay put.
     *
     * @param src              the source coordinates
     * @param windDir          the wind's direction
     * @param windStr          the wind's strength
     * @param directionChanges How many times the smoke has tried to change directions to get around an obstacle.
     *
     * @return the coordinates where the smoke has drifted to, or null if it dissipates while on the board.
     */
    public @Nullable Coords driftAddSmoke(final Coords src, Board board, final WindDirection windDir,
          final Wind windStr,
          final int directionChanges) {
        Coords nextCoords = src.translated(windDir.ordinal());

        // if the wind conditions are calm, then don't drift it
        if (windStr.isCalm()) {
            return src;
        }

        // The smoke has blown off the map, so we return the next coords to note that
        // and prevent
        // null issues later
        if (!board.contains(nextCoords)) {
            return nextCoords;
        }

        // if the smoke didn't start on the board because of shifting wind then return
        // it
        if (!board.contains(src)) {
            return src;
        }

        int hexElevation = board.getHex(src).getLevel();
        int nextElevation = board.getHex(nextCoords).getLevel();

        if (board.getHex(nextCoords).containsTerrain(Terrains.BUILDING)) {
            nextElevation += board.getHex(nextCoords).terrainLevel(Terrains.BLDG_ELEV);
        }

        if (board.getHex(src).containsTerrain(Terrains.BUILDING)) {
            hexElevation += board.getHex(src).terrainLevel(Terrains.BLDG_ELEV);
        }

        // If the smoke moves into a hex that has a greater than 4 elevation drop it
        // dissipates.
        if ((hexElevation - nextElevation) > 4) {
            return null;
        }

        if ((hexElevation - nextElevation) < -4) {
            // Try Right
            if (directionChanges == 0) {
                return driftAddSmoke(src, board, windDir.rotateClockwise(), windStr, directionChanges + 1);
            } else if (directionChanges == 1) {
                // Try Left
                return driftAddSmoke(src, board, windDir.rotateCounterClockwise(), windStr, directionChanges + 1);
            } else {
                // Stay put
                return src;
            }
        }

        // stronger wind causes smoke to drift farther
        if (windStr.isStrongerThan(Wind.MOD_GALE)) {
            return driftAddSmoke(nextCoords, board, windDir, windStr.lowerWind());
        }

        return nextCoords;
    }

    /**
     * Checks for the given SmokeCloud if it dissipates, either by a level or entirely and returns true if it does. This
     * checks both dissipation by wind and by duration.
     *
     * @param cloud   The Smoke cloud to test
     * @param windStr The current wind strength
     *
     * @return True when the smoke cloud dissipates by a level or entirely
     */
    public boolean checkSmokeDissipation(SmokeCloud cloud, Wind windStr) {
        if ((cloud.getDuration() == 1)
              || windStr.isStrongerThan(Wind.STORM)) {
            cloud.setSmokeLevel(0);
            return true;
        } else if (cloud.getDuration() > 1) {
            // Clouds that can last indefinitely (no duration) use duration = 0
            cloud.setDuration(cloud.getDuration() - 1);
        }

        // Dissipate in various winds
        int roll = Compute.d6(2);
        return (roll > 10)
              || ((roll > 9) && windStr.isModerateGale())
              || ((roll > 7) && windStr.isStrongGale())
              || ((roll > 5) && windStr.isStorm());
    }

    public void driftSmokeReport(SmokeCloud cloud, boolean dissipated) {
        int newIntensity = cloud.getSmokeLevel();
        List<Coords> coords = cloud.getCoordsList();
        // The initializer report types are for smoke drifting and dissipating
        int firstPosReport = 5223;
        int closingReport = 5224;

        if (newIntensity == 0 && cloud.didDrift()) {
            // A smoke cloud is removed from the board after drifting
            firstPosReport = 5225;
            closingReport = 5222;

        } else if (newIntensity == 0) {
            // A smoke cloud is removed from the board without drifting

        } else if ((newIntensity % 2 == 0) && dissipated) {
            // heavy smoke drifts and dissipates to light
            firstPosReport = 5210;
            closingReport = 5212;

        } else if (newIntensity % 2 == 0) {
            // heavy smoke drifts
            firstPosReport = 5210;
            closingReport = 5213;

        } else if ((newIntensity % 2 == 1) && dissipated) {
            // light smoke drifts and dissipates
            firstPosReport = 5220;
            closingReport = 5222;

        } else if (newIntensity % 2 == 1) {
            // light smoke drifts
            firstPosReport = 5220;
            closingReport = 5213;
        }

        for (int pos = 0; pos < cloud.getCoordsList().size(); pos++) {
            int reportType = (pos == 0) ? firstPosReport : 5211;
            vPhaseReport.addElement(Report.publicReport(reportType).add(coords.get(pos).getBoardNum()).noNL());
        }
        vPhaseReport.addElement(new Report(closingReport, Report.PUBLIC));
    }
}

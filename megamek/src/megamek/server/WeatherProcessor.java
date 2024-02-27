/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
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
 */
package megamek.server;

import java.util.Vector;

import megamek.common.*;
import megamek.common.enums.Wind;

/**
 * Cycle through hexes on a map and make any necessary adjustments based on weather
 * What will happen here:
 *  - add light and heavy snow for snowfall
 *  - add ice for snowfall, sleet, and ice storm
 *  - add/take away(?) rapids and torrent for winds
 *  - add mud, rapids, and torrent for rain (no that has to be done before play starts so it is in
 *    server.applyBoardSettings()
 *  - put out fires (DONE)
 */
public class WeatherProcessor extends DynamicTerrainProcessor {

    private Game game;
    Vector<Report> vPhaseReport;

    //track turns of snow, sleet, and ice
    // do it this way because we may eventually implement more customizable conditions
    int modSnowTurn = 0;
    int heavySnowTurn = 0;
    int sleetTurn = 0;
    int iceTurn = 0;

    public WeatherProcessor(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    void doEndPhaseChanges(Vector<Report> vPhaseReport) {
        game = gameManager.getGame();
        this.vPhaseReport = vPhaseReport;
        resolveWeather();
        this.vPhaseReport = null;

    }

    private void resolveWeather() {
        PlanetaryConditions conditions = game.getPlanetaryConditions();
        Board board = game.getBoard();
        int width = board.getWidth();
        int height = board.getHeight();
        boolean lightSnow = false;
        boolean deepSnow = false;
        boolean ice = false;

        if (!conditions.isTerrainAffected()) {
            return;
        }

        // first we need to increment the conditions
        if (conditions.getWeather().isModerateSnow()
                || conditions.getWeather().isSnowFlurries()
                && game.getBoard().onGround()) {
            modSnowTurn = modSnowTurn + 1;
            if (modSnowTurn == 9) {
                lightSnow = true;
            }
            if (modSnowTurn == 19) {
                deepSnow = true;
                ice = true;
            }
        }
        if (conditions.getWeather().isHeavySnow()
                && game.getBoard().onGround()) {
            heavySnowTurn = heavySnowTurn + 1;
            if (heavySnowTurn == 4) {
                lightSnow = true;
            }
            if (heavySnowTurn == 14) {
                deepSnow = true;
            }
            if (heavySnowTurn == 19) {
                ice = true;
            }
        }
        if (conditions.getWeather().isSleet()
                && game.getBoard().onGround()) {
            sleetTurn = sleetTurn + 1;
            if (sleetTurn == 14) {
                ice = true;
            }
        }
        if (conditions.getWeather().isIceStorm()
                && game.getBoard().onGround()) {
            iceTurn = iceTurn + 1;
            if (iceTurn == 9) {
                ice = true;
            }
        }

        if (lightSnow) {
            Report r = new Report(5505, Report.PUBLIC);
            vPhaseReport.addElement(r);
        }
        if (deepSnow) {
            Report r = new Report(5510, Report.PUBLIC);
            vPhaseReport.addElement(r);
        }
        if (ice) {
            Report r = new Report(5515, Report.PUBLIC);
            vPhaseReport.addElement(r);
        }

        // Cycle through all hexes, checking for the appropriate weather changes
        for (int currentXCoord = 0; currentXCoord < width; currentXCoord++) {
            for (int currentYCoord = 0; currentYCoord < height; currentYCoord++) {
                Coords currentCoords = new Coords(currentXCoord, currentYCoord);
                Hex currentHex = board.getHex(currentXCoord, currentYCoord);

                // check for fires and potentially put them out
                if (currentHex.containsTerrain(Terrains.FIRE)) {
                    // only standard fires get put out
                    if (currentHex.terrainLevel(Terrains.FIRE)
                            == Terrains.FIRE_LVL_NORMAL) {
                        if (conditions.putOutFire()) {
                            gameManager.removeFire(currentCoords, "weather conditions");
                        }
                    // Downgrade Inferno fires so they can burn out
                    } else if (currentHex.terrainLevel(Terrains.FIRE)
                            == Terrains.FIRE_LVL_INFERNO) {
                        //inferno fires should become regular fires
                        currentHex.removeTerrain(Terrains.FIRE);
                        currentHex.addTerrain(new Terrain(Terrains.FIRE, 1));
                        gameManager.getHexUpdateSet().add(currentCoords);
                    // Check Inferno Bombs
                    } else if (currentHex.terrainLevel(Terrains.FIRE)
                            == Terrains.FIRE_LVL_INFERNO_BOMB) {
                        if (currentHex.getFireTurn() > 30) {
                            gameManager.removeFire(currentCoords,
                                    "inferno bomb burning out");
                        }
                    }
                    // Inferno IV doesn't burn out, TO pg 356
                }

                if (ice && !currentHex.containsTerrain(Terrains.ICE)
                        && currentHex.containsTerrain(Terrains.WATER)) {
                    currentHex.addTerrain(new Terrain(Terrains.ICE, 1));
                    gameManager.getHexUpdateSet().add(currentCoords);
                }

                if (lightSnow
                        && !currentHex.containsTerrain(Terrains.SNOW)
                        && !(currentHex.containsTerrain(Terrains.WATER)
                                && !currentHex.containsTerrain(Terrains.ICE))
                        && !currentHex.containsTerrain(Terrains.MAGMA)) {
                    currentHex.addTerrain(new Terrain(Terrains.SNOW, 1));
                    gameManager.getHexUpdateSet().add(currentCoords);
                }

                if (deepSnow && !(currentHex.terrainLevel(Terrains.SNOW) > 1)
                        && !(currentHex.containsTerrain(Terrains.WATER)
                                && !currentHex.containsTerrain(Terrains.ICE))
                        && !currentHex.containsTerrain(Terrains.MAGMA)) {
                    currentHex.addTerrain(new Terrain(Terrains.SNOW, 2));
                    gameManager.getHexUpdateSet().add(currentCoords);
                }

                // check for the melting of any snow or ice
                if (currentHex.terrainLevel(Terrains.SNOW) > 1
                        && currentHex.containsTerrain(Terrains.FIRE)
                        && currentHex.getFireTurn() == 3) {
                    currentHex.removeTerrain(Terrains.SNOW);
                    if (!currentHex.containsTerrain(Terrains.MUD)
                            && !currentHex.containsTerrain(Terrains.WATER)) {
                        currentHex.addTerrain(new Terrain(Terrains.MUD, 1));
                    }
                }

                if (currentHex.terrainLevel(Terrains.SNOW) == 1
                        && currentHex.containsTerrain(Terrains.FIRE)
                        && currentHex.getFireTurn() == 1) {
                    currentHex.removeTerrain(Terrains.SNOW);
                    if (!currentHex.containsTerrain(Terrains.MUD)
                            && !currentHex.containsTerrain(Terrains.WATER)) {
                        currentHex.addTerrain(new Terrain(Terrains.MUD, 1));
                    }
                }

                if (currentHex.containsTerrain(Terrains.ICE)
                        && currentHex.containsTerrain(Terrains.FIRE)
                        && currentHex.getFireTurn() == 2) {
                    currentHex.removeTerrain(Terrains.ICE);
                    if (!currentHex.containsTerrain(Terrains.MUD)
                            && !currentHex.containsTerrain(Terrains.WATER)) {
                        currentHex.addTerrain(new Terrain(Terrains.MUD, 1));
                    }
                }

                if (currentHex.containsTerrain(Terrains.BLACK_ICE)
                        && currentHex.containsTerrain(Terrains.FIRE)
                        && currentHex.getFireTurn() == 2) {
                    currentHex.removeTerrain(Terrains.BLACK_ICE);
                }

                // check for rapids/torrents created by wind
                // FIXME: This doesn't seem to be doing anything
                if (currentHex.containsTerrain(Terrains.WATER)
                        && currentHex.depth(true) > 0) {
                    if (conditions.getWind().isStrongerThan(Wind.STORM)) {
                        if (!(currentHex.terrainLevel(Terrains.RAPIDS) > 1)) {
                            currentHex.addTerrain(new Terrain(Terrains.RAPIDS, 2));
                        }
                    } else if (conditions.getWind().isStrongerThan(Wind.MOD_GALE)) {
                        if (!currentHex.containsTerrain(Terrains.RAPIDS)) {
                            currentHex.addTerrain(new Terrain(Terrains.RAPIDS, 1));
                        }
                    }
                }
            }
        }
    }
}

/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.server;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.PlanetaryConditions;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.Terrains;
import megamek.common.options.OptionsConstants;

public class FireProcessor extends DynamicTerrainProcessor {

    private IGame game;
    Vector<Report> vPhaseReport;

    public FireProcessor(Server server) {
        super(server);
    }

    @Override
    void doEndPhaseChanges(Vector<Report> vPhaseReport) {
        game = server.getGame();
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_START_FIRE)) {
            this.vPhaseReport = vPhaseReport;
            resolveFire();
            this.vPhaseReport = null;
        }
    }

    /**
     * This debug/profiling function will print the current time (in
     * milliseconds) to the log. If the boolean is true, the garbage collector
     * will be called in an attempt to minimize timing errors. You should try
     * and minimize applications being run in the background when using this
     * function. Note that MS Windows only has 10 milisecond resolution. The
     * function should be optimized completely out of the code when the first
     * if-statement below reads "if (false)...", so performance shouldn't be
     * impacted if you leave calls to this function in the code (I think).
     */
    @SuppressWarnings("unused")
    private void debugTime(String s, boolean collectGarbage) {
        // Change the "false" below to "true" to enable this function
        if (false) {
            if (collectGarbage) {
                System.gc();
            }
            System.out.println(s + ": " + System.currentTimeMillis());
        }
    }

    /**
     * Make fires spread, smoke spread, and make sure that all fires started
     * this turn are marked as "burning" for next turn. What turn the fire startd on is no
     * longer determined by level but is rather a characteristic of the hex.
     * Level now denotes standard and inferno fires.
     */
    private void resolveFire() {
        IBoard board = game.getBoard();
        int width = board.getWidth();
        int height = board.getHeight();
        int windDirection = game.getPlanetaryConditions().getWindDirection();
        int windStrength = game.getPlanetaryConditions().getWindStrength();
        Report r;

        // Get the position map of all entities in the game.
        Hashtable<Coords, Vector<Entity>> positionMap = game.getPositionMap();

        // process smoke FIRST, before any fires spread or
        // smoke is produced.
        resolveSmoke();

        // Cycle through all buildings, checking for fire.
        // ASSUMPTION: buildings don't lose 2 CF on the turn a fire starts.
        // ASSUMPTION: multi-hex buildings lose 2 CF in each burning hex
        Enumeration<Building> buildings = game.getBoard().getBuildings();
        while (buildings.hasMoreElements()) {
            Building bldg = buildings.nextElement();
            Enumeration<Coords> bldgCoords = bldg.getCoords();
            while (bldgCoords.hasMoreElements()) {
                Coords coords = bldgCoords.nextElement();
                if (bldg.isBurning(coords)) {
                    int cf = Math.max(bldg.getCurrentCF(coords) - 2, 0);
                    bldg.setCurrentCF(cf, coords);

                    // Does the building burn down?
                    if (cf == 0) {
                        r = new Report(5120, Report.PUBLIC);
                        r.add(bldg.getName());
                        vPhaseReport.addElement(r);
                    }

                    // If it doesn't collapse under its load, mark it for update.
                    else if (!server.checkForCollapse(bldg, positionMap, coords, false, vPhaseReport)) {
                        bldg.setPhaseCF(cf, coords);
                    }
                }

            }
         }

        debugTime("resolve fire 1", true);

        // Cycle through all hexes, checking for fire and the spread of fire
        for (int currentXCoord = 0; currentXCoord < width; currentXCoord++) {
            for (int currentYCoord = 0; currentYCoord < height; currentYCoord++) {
                Coords currentCoords = new Coords(currentXCoord, currentYCoord);
                IHex currentHex = board.getHex(currentXCoord, currentYCoord);

                if(currentHex.containsTerrain(Terrains.FIRE)) {
                    //If the woods has been cleared, or the building
                    // has collapsed put non-inferno fires out.
                    if ((currentHex.terrainLevel(Terrains.FIRE) 
                            == Terrains.FIRE_LVL_NORMAL) 
                            && !currentHex.isIgnitable()) {
                        server.removeFire(currentCoords, "lack of fuel");
                        continue;
                    }

                    //only check spread for fires that didn't start this turn
                    if(currentHex.getFireTurn() > 0) {
                        //optional rule, woods burn down
                        Vector<Report> burnReports = null;
                        if ((currentHex.containsTerrain(Terrains.WOODS) || currentHex
                                .containsTerrain(Terrains.JUNGLE))
                                && game.getOptions().booleanOption(
                                        OptionsConstants.ADVANCED_WOODS_BURN_DOWN)) {
                            burnReports = burnDownWoods(currentCoords);
                        }
                        //report and check for fire spread
                        r = new Report(5125, Report.PUBLIC);
                        if ((currentHex.terrainLevel(Terrains.FIRE) 
                                == Terrains.FIRE_LVL_INFERNO)
                                || (currentHex.terrainLevel(Terrains.FIRE) 
                                        == Terrains.FIRE_LVL_INFERNO_BOMB)
                                || (currentHex.terrainLevel(Terrains.FIRE) 
                                        == Terrains.FIRE_LVL_INFERNO_IV)) {
                            r.messageId = 5130;
                        }
                        r.add(currentCoords.getBoardNum());
                        vPhaseReport.addElement(r);
                        if (burnReports != null) {
                            vPhaseReport.addAll(burnReports);
                        }
                        spreadFire(currentXCoord, currentYCoord, windDirection,
                                windStrength);
                    }
                }
            }
        }        

        //Cycle through all hexes again, reporting new fires, spreading smoke, and incrementing the fire turn.
        //Can't do this in first loop because new fires may be spread
        for (int currentXCoord = 0; currentXCoord < width; currentXCoord++) {
            for (int currentYCoord = 0; currentYCoord < height; currentYCoord++) {
                Coords currentCoords = new Coords(currentXCoord, currentYCoord);
                IHex currentHex = board.getHex(currentXCoord, currentYCoord);

                if(currentHex.containsTerrain(Terrains.FIRE)) {
                    Building bldg = game.getBoard().getBuildingAt(
                            currentCoords);
                    //was the fire started this turn?
                    if(currentHex.getFireTurn() == 0) {
                        //report fire started this round
                        r = new Report(5135, Report.PUBLIC);
                        r.add(currentCoords.getBoardNum());
                        vPhaseReport.addElement(r);

                        // If the hex contains a building, set it on fire.
                        if (bldg != null) {
                            bldg.setBurning(true, currentCoords);
                        }
                    }

                    //check for any explosions
                    server.checkExplodeIndustrialZone(currentCoords, vPhaseReport);

                    // Add smoke, unless tornado or optional rules
                    boolean containsForest = (currentHex.containsTerrain(Terrains.WOODS)
                            || currentHex.containsTerrain(Terrains.JUNGLE));
                    boolean bInferno = currentHex.terrainLevel(Terrains.FIRE) == 2;
                    if ((game.getPlanetaryConditions().getWindStrength() < PlanetaryConditions.WI_TORNADO_F13)
                            && !(game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_FOREST_FIRES_NO_SMOKE)
                                    && containsForest && (bldg == null))) {
                        ArrayList<Coords> smokeList = new ArrayList<Coords>();

                        smokeList.add(new Coords(Coords.xInDir(currentXCoord, currentYCoord, windDirection),
                                Coords.yInDir(currentXCoord, currentYCoord, windDirection)));
                        smokeList.add(new Coords(Coords.xInDir(currentXCoord, currentYCoord, (windDirection + 1) % 6),
                                Coords.yInDir(currentXCoord, currentYCoord, (windDirection + 1) % 6)));
                        smokeList.add(new Coords(Coords.xInDir(currentXCoord, currentYCoord, (windDirection + 5) % 6),
                                Coords.yInDir(currentXCoord, currentYCoord, (windDirection + 5) % 6)));

                        server.addSmoke(smokeList, windDirection, bInferno);
                        board.initializeAround(currentXCoord, currentYCoord);
                    }
                    //increment the fire turn counter
                    currentHex.incrementFireTurn();
                    server.getHexUpdateSet().add(currentCoords);
                }
            }
        }        

    } // End the ResolveFire() method

    public Vector<Report> burnDownWoods(Coords coords) {
        Vector<Report> burnReports = new Vector<>();
        int burnDamage = 5;
        try {
            burnDamage = game.getOptions().intOption(OptionsConstants.ADVANCED_WOODS_BURN_DOWN_AMOUNT);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // Report that damage applied to terrain
        Report r = new Report(3383, Report.PUBLIC);
        r.indent(1);
        r.add(burnDamage);
        burnReports.addElement(r);
        
        Vector<Report> newReports = 
                server.tryClearHex(coords, burnDamage, Entity.NONE);
        for (Report nr : newReports) {
            nr.indent(2);
        }
        burnReports.addAll(newReports);
        return burnReports;
    }

    /**
     * Spreads the fire around the specified coordinates.
     */
    public void spreadFire(int x, int y, int windDir, int windStr) {
        Coords src = new Coords(x, y);
        Coords nextCoords = src.translated(windDir);

        //check for height differences between hexes
        //TODO: until further clarification only the heights matter (not the base elevation)
        //This means that a fire cannot spread from a level 6 building at base level 0 to
        //a level 1 building at base level 0, for example.

        int curHeight = game.getBoard().getHex(src).ceiling();

        TargetRoll directroll = new TargetRoll(9, "spread downwind");
        TargetRoll obliqueroll = new TargetRoll(11, "spread 60 degrees to downwind");

        if((windStr > PlanetaryConditions.WI_NONE) && (windStr < PlanetaryConditions.WI_STRONG_GALE)) {
            directroll.addModifier(-2, "light/moderate gale");
            obliqueroll.addModifier(-1, "light/moderate gale");
        }
        else if(windStr > PlanetaryConditions.WI_MOD_GALE) {
            directroll.addModifier(-3, "strong gale+");
            directroll.addModifier(-2, "strong gale+");
        }

        spreadFire(nextCoords, directroll, curHeight);

        // Spread to the next hex downwind on a 12 if the first hex wasn't
        // burning...
        // unless a higher hex intervenes
        IHex nextHex = game.getBoard().getHex(nextCoords);
        IHex jumpHex = game.getBoard().getHex(nextCoords.translated(windDir));
        if ((nextHex != null) && (jumpHex != null) && !(nextHex.containsTerrain(Terrains.FIRE))
                && ((curHeight >= nextHex.ceiling()) || (jumpHex.ceiling() >= nextHex.ceiling()))) {
            // we've already gone one step in the wind direction, now go another
            directroll.addModifier(3, "crossing non-burning hex");
            spreadFire(nextCoords.translated(windDir), directroll, curHeight);
        }

        // spread fire 60 degrees clockwise....
        spreadFire(src.translated((windDir + 1) % 6), obliqueroll, curHeight);

        // spread fire 60 degrees counterclockwise
        spreadFire(src.translated((windDir + 5) % 6), obliqueroll, curHeight);
    }

    /**
     * Spreads the fire, and reports the spread, to the specified hex, if
     * possible, if the hex isn't already on fire, and the fire roll is made.
     */
    public void spreadFire(Coords coords, TargetRoll roll, int height) {
        IHex hex = game.getBoard().getHex(coords);
        if (hex == null) {
            // Don't attempt to spread fire off the board.
            return;
        }

        if(Math.abs(hex.ceiling() - height) > 4) {
            return;
        }

        if (!(hex.containsTerrain(Terrains.FIRE)) && server.checkIgnition(coords, roll)) {
            Report r = new Report(5150, Report.PUBLIC);
            r.add(coords.getBoardNum());
            vPhaseReport.addElement(r);
        }
    }

    /**
     * Under L3 rules, smoke drifts in the direction of the wind and has a
     * chance to dissipate. This function will keep track of hexes to have smoke
     * removed and added, since there's no other way to tell if a certain smoke
     * cloud has drifted that turn. This method creates the class SmokeDrift to
     * store hex and size data for the smoke clouds. This method calls functions
     * driftAddSmoke, driftSmokeDissipate, driftSmokeReport
     */
    private void resolveSmoke() {
        IBoard board = game.getBoard();
        int windDir = game.getPlanetaryConditions().getWindDirection();
        int windStr = game.getPlanetaryConditions().getWindStrength();
        //if the breeze option is turned on, then treat wind strength like light gale if none
        if(game.getOptions().booleanOption(OptionsConstants.BASE_BREEZE) && (windStr == PlanetaryConditions.WI_NONE)) {
            windStr = PlanetaryConditions.WI_LIGHT_GALE;
        }

        ArrayList<Coords> smokeToAdd;
        HashMap<SmokeCloud, ArrayList<Coords>> smokeCloudData = new HashMap<SmokeCloud, ArrayList<Coords>>();

        // Cycle through all smokeclouds

        for ( SmokeCloud cloud : server.getSmokeCloudList() ){
            smokeToAdd = new ArrayList<Coords>();
            for ( Coords currentCoords : cloud.getCoordsList() ){

                Coords smokeCoords = driftAddSmoke(currentCoords, windDir, windStr);
                //Smoke has Dissipated by moving into a hex with a greater then 4 elevation drop.
                if ( smokeCoords == null ){
                    Report r = new Report(5220, Report.PUBLIC);
                    r.add(currentCoords.getBoardNum());
                    vPhaseReport.addElement(r);
                    r = new Report(5222,Report.PUBLIC);
                    vPhaseReport.addElement(r);
                }
                else if (board.contains(smokeCoords) && !currentCoords.equals(smokeCoords) ) {
                    // don't add it to the vector if it's not on board!
                    smokeToAdd.add(smokeCoords);
                    cloud.setDrift(true);
                } else if ( !board.contains(smokeCoords) ) {
                    // report that the smoke has blown off the map
                    Report r = new Report(5230, Report.PUBLIC);
                    r.add(currentCoords.getBoardNum());
                    vPhaseReport.addElement(r);
                }

            } // end the loop through Cloud coordinates
            if ( smokeToAdd.size() > 0 ) {
                smokeCloudData.put(cloud,smokeToAdd);
            }
        } // end the loop through clouds

        //update all the new coords for the smoke cloud.
        for ( SmokeCloud cloud : smokeCloudData.keySet() ){
            smokeToAdd = smokeCloudData.get(cloud);
            server.updateSmoke(cloud, smokeToAdd);
        }

        // Cycle through the vector again and dissipate the smoke, then
        // reporting it
        for ( SmokeCloud cloud : server.getSmokeCloudList() ){

                int roll = Compute.d6(2);

                boolean dissipated = driftSmokeDissipate(cloud, roll, windStr);

                if ( dissipated || cloud.didDrift() ){
                    driftSmokeReport(cloud,dissipated);
                    if (dissipated) {
                        cloud.reduceSmokeLevel();
                    }
                }
                cloud.setDrift(false);
        }

    } // end smoke resolution

    /**
     * Override for the main driftAddSmoke to allow for 0 direction changes
     * @param x
     * @param y
     * @param windDir
     * @param windStr
     * @return
     */
    public Coords driftAddSmoke(Coords coords, int windDir, int windStr) {
        return driftAddSmoke(coords, windDir, windStr, 0);
    }

    /**
     * Smoke cannot climb more then 4 hexes if the next hex is more then 4 in elevation then
     * The smoke will try to go right. If it cannot go right it'll try to go left
     * if it cannot go left it'll stay put.
     *
     * @param x
     * @param y
     * @param windDir
     * @param windStr
     * @param directionChanges How many times the smoke has tried to change directions to get around an obsticle.
     * @return
     */
    public Coords driftAddSmoke(Coords src, int windDir, int windStr, int directionChanges) {
        //Coords src = new Coords(x, y);
        Coords nextCoords = src.translated(windDir);
        IBoard board = game.getBoard();

        //if the wind conditions are calm, then don't drift it
        if(windStr == PlanetaryConditions.WI_NONE) {
            return src;
        }

        //if it is no longer on the board then return it now to avoid getting null arguments later
        if(!board.contains(nextCoords)) {
            return nextCoords;
        }
        //if the smoke didn't start on the board because of shifting wind then return it
        if(!board.contains(src)) {
            return src;
        }

        int hexElevation = board.getHex(src).getLevel();
        int nextElevation = board.getHex(nextCoords).getLevel();

        if ( board.getHex(nextCoords).containsTerrain(Terrains.BUILDING) ) {
            nextElevation += board.getHex(nextCoords).terrainLevel(Terrains.BLDG_ELEV);
        }

        if ( board.getHex(src).containsTerrain(Terrains.BUILDING) ) {
            hexElevation += board.getHex(src).terrainLevel(Terrains.BLDG_ELEV);
        }
        //If the smoke moves into a hex that has a greater then 4 elevation drop it dissipates.
        if ( (hexElevation - nextElevation) > 4 ) {
            return null;
        }

        if ( (hexElevation - nextElevation) < -4 ){
            //Try Right
            if ( directionChanges == 0 ){
                return driftAddSmoke(src, (windDir + 1) % 6, windStr, ++directionChanges);
            }
            //Try Left
            else if ( directionChanges == 1) {
                return driftAddSmoke(src, (windDir - 2 ) % 6, windStr, ++directionChanges);
            //Stay put
            } else {
                return src;
            }
        }

        // stronger wind causes smoke to drift farther
        if (windStr > PlanetaryConditions.WI_MOD_GALE) {
            return driftAddSmoke(nextCoords, windDir, --windStr);
        }

        return nextCoords;
    }

    /**
     * Diisipates Smoke clouds instead of indivdiual smoke hexes
     * @param cloud
     * @param roll
     * @param windStr
     * @return
     */
    public boolean driftSmokeDissipate(SmokeCloud cloud, int roll, int windStr) {

        //HVAC Heavy smoke dissipation
        if ( (cloud.getDuration() > 0) && ((cloud.getDuration()-1) == 0) ) {
            cloud.setDuration(0);
            cloud.setSmokeLevel(0);
            return true;
        }

        if ( (cloud.getDuration() > 0) && ((cloud.getDuration()-1) > 0) ) {
            cloud.setDuration(cloud.getDuration()-1);
        }

        // Dissipate in various winds
        if ((roll > 10) || ((roll > 9) && (windStr == PlanetaryConditions.WI_MOD_GALE))
                || ((roll > 7) && (windStr == PlanetaryConditions.WI_STRONG_GALE))
                || ((roll > 5) && (windStr == PlanetaryConditions.WI_STORM))) {
            return true;
        }
        //All smoke goes bye bye in Tornados
        if ( windStr > PlanetaryConditions.WI_STORM ) {
            cloud.setSmokeLevel(0);
            return true;
        }
        return false;
    }

    public void driftSmokeReport(SmokeCloud cloud, boolean dis) {
        Report r;
        int size = cloud.getSmokeLevel();
        if ((size % 2 == 0) && (dis == true)) {
            // heavy smoke drifts and dissipates to light
            for ( int pos = 0; pos < cloud.getCoordsList().size(); pos++ ) {
                if ( pos == 0 ) {
                    r = new Report(5210, Report.PUBLIC);
                } else {
                    r = new Report(5211, Report.PUBLIC);
                }
                r.add(cloud.getCoordsList().get(pos).getBoardNum());
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }

            r = new Report(5212, Report.PUBLIC);
            vPhaseReport.addElement(r);
        } else if ((size % 2 == 0) && (dis == false)) {
            // heavy smoke drifts
            for ( int pos = 0; pos < cloud.getCoordsList().size(); pos++ ) {
                if ( pos == 0 ) {
                    r = new Report(5210, Report.PUBLIC);
                } else {
                    r = new Report(5211, Report.PUBLIC);
                }
                r.add(cloud.getCoordsList().get(pos).getBoardNum());
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }

            r = new Report(5213, Report.PUBLIC);
            vPhaseReport.addElement(r);
        } else if ((size % 2 == 1) && (dis == true)) {
            // light smoke drifts and dissipates
            for ( int pos = 0; pos < cloud.getCoordsList().size(); pos++ ) {
                if ( pos == 0 ) {
                    r = new Report(5220, Report.PUBLIC);
                } else {
                    r = new Report(5211, Report.PUBLIC);
                }
                r.add(cloud.getCoordsList().get(pos).getBoardNum());
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }

            r = new Report(5222, Report.PUBLIC);
            vPhaseReport.addElement(r);

        } else if ((size % 2 == 1) && (dis == false)) {
            // light smoke drifts

            for ( int pos = 0; pos < cloud.getCoordsList().size(); pos++ ) {
                if ( pos == 0) {
                    r = new Report(5220, Report.PUBLIC);
                } else {
                    r = new Report(5211, Report.PUBLIC);
                }

                r.add(cloud.getCoordsList().get(pos).getBoardNum());
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }

            r = new Report(5213, Report.PUBLIC);
            vPhaseReport.addElement(r);

        }else if (size < 1 ) {
            // light smoke drifts and dissipates
            for ( int pos = 0; pos < cloud.getCoordsList().size(); pos++ ) {
                if ( pos == 0 ) {
                    r = new Report(5223, Report.PUBLIC);
                } else {
                    r = new Report(5211, Report.PUBLIC);
                }
                r.add(cloud.getCoordsList().get(pos).getBoardNum());
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }

            r = new Report(5224, Report.PUBLIC);
            vPhaseReport.addElement(r);

        }
    }
}

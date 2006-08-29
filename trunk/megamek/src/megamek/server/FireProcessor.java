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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Report;
import megamek.common.Terrains;

public class FireProcessor extends DynamicTerrainProcessor {

    private IGame game;
    Vector<Report> vPhaseReport;
    
    public FireProcessor(Server server) {
        super(server);
        // TODO Auto-generated constructor stub
    }

    @Override
    void DoEndPhaseChanges(Vector<Report> vPhaseReport) {
        this.vPhaseReport = vPhaseReport;
        game = server.getGame();
        resolveFire();
        this.vPhaseReport = null;
    }

    /**
     * This debug/profiling function will print the current time
     * (in milliseconds) to the log.  If the boolean is true, the
     * garbage collector will be called in an attempt to minimize
     * timing errors.  You should try and minimize applications
     * being run in the background when using this function.
     * Note that MS Windows only has 10 milisecond resolution.
     *
     * The function should be optimized completely out of the code
     * when the first if-statement below reads "if (false)...", so
     * performance shouldn't be impacted if you leave calls to this
     * function in the code (I think).
     */
    private void debugTime(String s, boolean collectGarbage) {
        //Change the "false" below to "true" to enable this function
        if (false) {
            if (collectGarbage)
                System.gc();
            System.out.println(s + ": " + System.currentTimeMillis());
        }
    }

    /**
     * Make fires spread, smoke spread, and make sure that all fires
     * started this turn are marked as "burning" for next turn.
     *
     * A "FIRE" terrain has one of two levels:
     *  1 (Created this turn, and so can't spread of generate heat)
     *  2 (Created as a result of spreading fire or on a previous turn)
     *
     * Since fires created at end of turn act normally in the following turn,
     * spread fires have level 2.
     *
     * At NO TIME should any fire created outside this function have a level of
     * 2, nor should anything except this function SET fires to level 2.
     *
     * Newly created "spread" fires have a level of 1, so that they do not
     * spread in the turn they are created.  After all spreading has been
     * completed, all burning hexes are set to level 2.
     */
    private void resolveFire() {
        IBoard board = game.getBoard();
        int width = board.getWidth();
        int height = board.getHeight();
        int windDirection = game.getWindDirection();
        Report r;

        // Get the position map of all entities in the game.
        Hashtable positionMap = game.getPositionMap();

        // Build vector to send for updated buildings at once.
        Vector burningBldgs = new Vector();

        // If we're in L3 rules, process smoke FIRST, before any fires spread or smoke is produced.
        if (game.getOptions().booleanOption("maxtech_fire")) {
            resolveSmoke();
        }

        // Cycle through all buildings, checking for fire.
        // ASSUMPTION: buildings don't lose 2 CF on the turn a fire starts.
        // ASSUMPTION: multi-hex buildings lose 2 CF max, regardless of # fires
        Enumeration buildings = game.getBoard().getBuildings();
        while ( buildings.hasMoreElements() ) {
            Building bldg = (Building) buildings.nextElement();
            if ( bldg.isBurning() ) {
                int cf = Math.max(bldg.getCurrentCF() - 2, 0);
                bldg.setCurrentCF( cf );

                // Does the building burned down?
                if ( cf == 0 ) {
                    r = new Report(5120, Report.PUBLIC);
                    r.add(bldg.getName());
                    vPhaseReport.addElement(r);
                    server.collapseBuilding( bldg, positionMap );
                }

                // If it doesn't collapse under its load, mark it for update.
                else if ( !server.checkForCollapse(bldg, positionMap) ) {
                    bldg.setPhaseCF( cf );
                    burningBldgs.addElement( bldg );
                }
            }
        }

        debugTime("resolve fire 1", true);

        // Cycle through all hexes, checking for fire.
        for (int currentXCoord = 0; currentXCoord < width; currentXCoord++ ) {

            for (int currentYCoord = 0; currentYCoord < height;
                 currentYCoord++) {
                Coords currentCoords = new Coords(currentXCoord,
                                                  currentYCoord);
                IHex currentHex = board.getHex(currentXCoord, currentYCoord);
                boolean infernoBurning = board.burnInferno( currentCoords );

                // optional rule, woods burn down
                if ((currentHex.containsTerrain(Terrains.WOODS) || currentHex.containsTerrain(Terrains.JUNGLE))
                        && currentHex.terrainLevel(Terrains.FIRE) == 2 && game.getOptions().booleanOption("woods_burn_down")) {
                    burnDownWoods(currentCoords);
                }

                // If the woods has been cleared, or the building
                // has collapsed put non-inferno fires out.
                if ( currentHex.containsTerrain(Terrains.FIRE)
                        && !infernoBurning
                        && !(currentHex.containsTerrain(Terrains.WOODS))
                        && !(currentHex.containsTerrain(Terrains.JUNGLE))
                        && !(currentHex.containsTerrain(Terrains.FUEL_TANK))
                        && !(currentHex.containsTerrain(Terrains.BUILDING)) ) {
                    server.removeFire(currentXCoord, currentYCoord, currentHex);
                }

                // Was the fire started on a previous turn?
                else if (currentHex.terrainLevel(Terrains.FIRE) == 2)
                {
                    r = new Report(5125, Report.PUBLIC);
                    if ( infernoBurning )
                        r.messageId = 5130;
                    r.add(currentCoords.getBoardNum());
                    vPhaseReport.addElement(r);
                    spreadFire(currentXCoord, currentYCoord, windDirection);
                }
            }
        }

        debugTime("resolve fire 1 end, begin resolve fire 2", true);

        //  Loop a second time, to set all fires to level 2 before next turn, and add smoke.
        for (int currentXCoord = 0; currentXCoord < width; currentXCoord++ ) {

            for (int currentYCoord = 0; currentYCoord < height; currentYCoord++) {
                Coords currentCoords = new Coords(currentXCoord, currentYCoord);
                IHex currentHex = board.getHex(currentXCoord,currentYCoord);
                // if the fire in the hex was started this turn
                if (currentHex.terrainLevel(Terrains.FIRE) == 1) {
                    currentHex.removeTerrain(Terrains.FIRE);
                    currentHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 2));
                    server.sendChangedHex(currentCoords);
                    //fire started this round
                    r = new Report(5135, Report.PUBLIC);
                    r.add(currentCoords.getBoardNum());
                    vPhaseReport.addElement(r);

                    // If the hex contains a building, set it on fire.
                    Building bldg = game.getBoard().getBuildingAt( currentCoords );
                    if ( bldg != null ) {
                        bldg.setBurning( true );
                        burningBldgs.addElement( bldg );
                    }
                }
                // If the L3 smoke rule is off, add smoke normally, otherwise call the L3 method
                if (currentHex.containsTerrain(Terrains.FIRE) && !game.getOptions().booleanOption("maxtech_fire")) {
                    server.addSmoke(currentXCoord, currentYCoord, windDirection);
                    server.addSmoke(currentXCoord, currentYCoord, (windDirection+1)%6);
                    server.addSmoke(currentXCoord, currentYCoord, (windDirection+5)%6);
                    board.initializeAround(currentXCoord,currentYCoord);
                }
                else if (currentHex.containsTerrain(Terrains.FIRE) && game.getOptions().booleanOption("maxtech_fire")) {
                    server.addL3Smoke(currentXCoord, currentYCoord);
                    board.initializeAround(currentXCoord, currentYCoord);
                }
            }
        }

        debugTime("resolve fire 2 end", false);

        // If any buildings are burning, update the clients.
        if ( !burningBldgs.isEmpty() ) {
            server.sendChangedCFBuildings(burningBldgs);
        }

        // If we're in L3 rules, shift the wind.
        if (game.getOptions().booleanOption("maxtech_fire")) {
            game.determineWind();
        }

    }  // End the ResolveFire() method

    public void burnDownWoods(Coords coords) {
        IHex hex = game.getBoard().getHex(coords);
        int roll = Compute.d6(2);
        Report r;
        if(roll >= 11) {
            if(hex.terrainLevel(Terrains.WOODS) > 2) {
                hex.removeTerrain(Terrains.WOODS);
                hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.WOODS, 2));
                //ultra heavy woods burned down to heavy woods
                r = new Report(5141, Report.PUBLIC);
                r.add(coords.getBoardNum());
                vPhaseReport.addElement(r);
            }
            else if(hex.terrainLevel(Terrains.WOODS) == 2) {
                hex.removeTerrain(Terrains.WOODS);
                hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.WOODS, 1));
                //heavy woods burned down to light woods
                r = new Report(5140, Report.PUBLIC);
                r.add(coords.getBoardNum());
                vPhaseReport.addElement(r);
            }
            else if(hex.terrainLevel(Terrains.WOODS) == 1) {
                hex.removeTerrain(Terrains.WOODS);
                hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.ROUGH, 1));
                //light woods burns down, fire goes out
                r = new Report(5145, Report.PUBLIC);
                r.add(coords.getBoardNum());
                vPhaseReport.addElement(r);
            }
            if(hex.terrainLevel(Terrains.JUNGLE) > 2) {
                hex.removeTerrain(Terrains.JUNGLE);
                hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.JUNGLE, 2));
                //ultra heavy jungle burned down to heavy jungle
                r = new Report(5143, Report.PUBLIC);
                r.add(coords.getBoardNum());
                vPhaseReport.addElement(r);
            }
            else if(hex.terrainLevel(Terrains.JUNGLE) == 2) {
                hex.removeTerrain(Terrains.JUNGLE);
                hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.JUNGLE, 1));
                //heavy jungle burned down to light jungle
                r = new Report(5142, Report.PUBLIC);
                r.add(coords.getBoardNum());
                vPhaseReport.addElement(r);
            }
            else if(hex.terrainLevel(Terrains.JUNGLE) == 1) {
                hex.removeTerrain(Terrains.JUNGLE);
                hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.ROUGH, 1));
                //light jungle burns down, fire goes out
                r = new Report(5146, Report.PUBLIC);
                r.add(coords.getBoardNum());
                vPhaseReport.addElement(r);
            }
            server.sendChangedHex(coords);
        }
    }

    /**
     * Spreads the fire around the specified coordinates.
     */
    public void spreadFire(int x, int y, int windDir) {
        Coords src = new Coords(x, y);
        Coords nextCoords = src.translated(windDir);

        spreadFire(nextCoords, 9);

        // Spread to the next hex downwind on a 12 if the first hex wasn't burning...
        IHex nextHex = game.getBoard().getHex(nextCoords);
        if (nextHex != null && !(nextHex.containsTerrain(Terrains.FIRE))) {
            // we've already gone one step in the wind direction, now go another
            spreadFire(nextCoords.translated(windDir), 12);
        }

        // spread fire 60 degrees clockwise....
        spreadFire(src.translated((windDir + 1) % 6), 11);

        // spread fire 60 degrees counterclockwise
        spreadFire(src.translated((windDir + 5) % 6), 11);
    }

    /**
     * Spreads the fire, and reports the spread, to the specified hex, if
     * possible, if the hex isn't already on fire, and the fire roll is made.
     */
    public void spreadFire(Coords coords, int roll) {
        IHex hex = game.getBoard().getHex(coords);
        if (hex == null) {
            // Don't attempt to spread fire off the board.
            return;
        }
        if (!(hex.containsTerrain(Terrains.FIRE)) && server.ignite(hex, roll)) {
            server.sendChangedHex(coords);
            Report r = new Report(5150, Report.PUBLIC);
            r.add(coords.getBoardNum());
            vPhaseReport.addElement(r);
        }
    }

    /**
     * Under L3 rules, smoke drifts in the direction of the wind and
     * has a chance to dissipate.  This function will keep track of
     * hexes to have smoke removed and added, since there's no other
     * way to tell if a certain smoke cloud has drifted that turn.
     * This method creates the class SmokeDrift to store hex and size
     * data for the smoke clouds.  This method calls functions
     * driftAddSmoke, driftSmokeDissipate, driftSmokeReport
     */
    private void resolveSmoke() {
        IBoard board = game.getBoard();
        int width = board.getWidth();
        int height = board.getHeight();
        int windDir = game.getWindDirection();
        int windStr = game.getWindStrength();
        Vector SmokeToAdd = new Vector();

        class SmokeDrift { // hold the hex and level of the smoke cloud
            public Coords coords;
            public int size;

            public SmokeDrift(Coords c, int s) {
                coords = c;
                size = s;
            }

            public SmokeDrift(SmokeDrift sd) {
                sd.coords = coords;
                sd.size = size;
            }
        }

        // Cycle through all hexes, checking for smoke, IF the wind is higher than calm! Calm means no drift!
        if(windStr > 0) {

            debugTime("resolve smoke 1", true);

            for (int currentXCoord = 0; currentXCoord < width; currentXCoord++ ) {

                for (int currentYCoord = 0; currentYCoord < height; currentYCoord++) {
                    Coords currentCoords = new Coords(currentXCoord, currentYCoord);
                    IHex currentHex = board.getHex(currentXCoord, currentYCoord);

                    // check for existence of smoke, then add it to the vector...if the wind is not Calm!
                    if (currentHex.containsTerrain(Terrains.SMOKE)){
                        int smokeLevel = currentHex.terrainLevel(Terrains.SMOKE);
                        Coords smokeCoords = driftAddSmoke(currentXCoord, currentYCoord, windDir, windStr);
                        //                        System.out.println(currentCoords.toString() + " to " + smokeCoords.toString());
                        if( board.contains(smokeCoords)) { // don't add it to the vector if it's not on board!
                            SmokeToAdd.addElement(new SmokeDrift(new Coords(smokeCoords), smokeLevel));
                        }
                        else {
                            // report that the smoke has blown off the map
                            Report r = new Report(5230, Report.PUBLIC);
                            r.add(currentCoords.getBoardNum());
                            vPhaseReport.addElement(r);
                        }
                        currentHex.removeTerrain(Terrains.SMOKE);
                        server.sendChangedHex(currentCoords);

                    }

                }  // end the loop through Y coordinates
            }  // end the loop through X coordinates

            debugTime("resolve smoke 1 end, resolve smoke 2 begin", true);

            // Cycle through the vector and add the drifted smoke
            for (int sta = 0; sta < SmokeToAdd.size(); sta++ ) {
                SmokeDrift drift = (SmokeDrift)SmokeToAdd.elementAt(sta);
                Coords smokeCoords = drift.coords;
                int smokeSize = drift.size;
                IHex smokeHex = game.getBoard().getHex(smokeCoords);
                smokeHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.SMOKE, smokeSize));
                server.sendChangedHex(smokeCoords);
            }

            debugTime("resolve smoke 2 end, resolve smoke 3 begin", true);

            // Cycle through the vector again and dissipate the smoke, then reporting it
            for (int dis = 0; dis < SmokeToAdd.size(); dis++ ) {
                SmokeDrift drift = (SmokeDrift)SmokeToAdd.elementAt(dis);
                Coords smokeCoords = drift.coords;
                int smokeSize = drift.size;
                IHex smokeHex = game.getBoard().getHex(smokeCoords);
                int roll = Compute.d6(2);

                boolean smokeDis = driftSmokeDissipate(smokeHex, roll, smokeSize, windStr);
                driftSmokeReport(smokeCoords, smokeSize, smokeDis);
                server.sendChangedHex(smokeCoords);
            }

            debugTime("resolve smoke 3 end", false);

        } // end smoke resolution
    }

    public Coords driftAddSmoke(int x, int y, int windDir, int windStr){
        Coords src = new Coords(x, y);
        Coords nextCoords = src.translated(windDir);

        // if the wind is High, it blows 2 hexes! If it's Calm, there's no drift!
        if (windStr == 3) {
            nextCoords = nextCoords.translated(windDir);
        }

        return nextCoords;
    }

    /**
     * This method does not currently support "smoke clouds" as specified
     * in MaxTech (revised ed.) under "Dissipation" on page 51.  The
     * added complexity was not worth it given that smoke-delivering
     * weapons were not even implemented yet (and might never be).
     */
    public boolean driftSmokeDissipate(IHex smokeHex, int roll, int smokeSize, int windStr) {
        // Dissipate in various winds
        if (roll > 10 || (roll > 9 && windStr == 2) || (roll > 7 && windStr == 3)) {
            smokeHex.removeTerrain(Terrains.SMOKE);

            if (smokeSize == 2) {
                smokeHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.SMOKE, 1));
                return true;
            }
			return true;
        }
		return false;
    }

    public void driftSmokeReport(Coords smokeCoords, int size, boolean dis) {
        Report r;
        if (size == 2 && dis == true) {
            //heavy smoke drifts and dissipates to light
            r = new Report(5210, Report.PUBLIC);
            r.add(smokeCoords.getBoardNum());
            vPhaseReport.addElement(r);
        }
        else if (size == 2 && dis == false) {
            //heavy smoke drifts
            r = new Report(5215, Report.PUBLIC);
            r.add(smokeCoords.getBoardNum());
            vPhaseReport.addElement(r);
        }
        else if (size == 1 && dis == true) {
            //light smoke drifts and dissipates
            r = new Report(5220, Report.PUBLIC);
            r.add(smokeCoords.getBoardNum());
            vPhaseReport.addElement(r);
        }
        else if (size == 1 && dis == false) {
            //light smoke drifts
            r = new Report(5225, Report.PUBLIC);
            r.add(smokeCoords.getBoardNum());
            vPhaseReport.addElement(r);
        }
    }
}

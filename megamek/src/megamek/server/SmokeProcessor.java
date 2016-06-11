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
import java.util.List;
import java.util.Vector;

import megamek.common.Coords;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Report;
import megamek.common.Terrains;

public class SmokeProcessor extends DynamicTerrainProcessor {

    private IGame game;
    Vector<Report> vPhaseReport;

    public SmokeProcessor(Server server) {
        super(server);
    }

    @Override
    void doEndPhaseChanges(Vector<Report> vPhaseReport) {
        game = server.getGame();

        this.vPhaseReport = vPhaseReport;
        resolveSmoke();
        this.vPhaseReport = null;

    }

    private void resolveSmoke() {
        updateSmoke();
        removeEmptyClouds();
    }

    /**
     * Remove any empty clouds from the array
     */
    public void removeEmptyClouds() {
        List<SmokeCloud> cloudsToRemove = new ArrayList<>();
        for (SmokeCloud cloud: server.getSmokeCloudList()) {
            if ( cloud.getCoordsList().size() < 1 ) {
                cloudsToRemove.add(cloud);
            }else if ( cloud.getSmokeLevel() < 1 ) {
                server.removeSmokeTerrain(cloud);
                cloudsToRemove.add(cloud);
            }
        }
        server.getGame().removeSmokeClouds(cloudsToRemove);
    }

    /**
     * Creates the Smoke Terrain Markers.
     * @param cloud
     */
    public void createSmokeTerrain(SmokeCloud cloud){

        for( Coords coords : cloud.getCoordsList() ){
            IHex smokeHex = game.getBoard().getHex(coords);
            if (smokeHex != null ){
                if (smokeHex.containsTerrain(Terrains.SMOKE)) {
                    if (smokeHex.terrainLevel(Terrains.SMOKE) 
                            == SmokeCloud.SMOKE_LIGHT) {
                        smokeHex.removeTerrain(Terrains.SMOKE);
                        smokeHex.addTerrain(Terrains.getTerrainFactory()
                                .createTerrain(Terrains.SMOKE,
                                        SmokeCloud.SMOKE_HEAVY));
                        server.getHexUpdateSet().add(coords);
                    }
                } else if (cloud.getSmokeLevel() > SmokeCloud.SMOKE_NONE) {
                    smokeHex.addTerrain(Terrains.getTerrainFactory()
                            .createTerrain(Terrains.SMOKE,
                                    cloud.getSmokeLevel()));
                    server.getHexUpdateSet().add(coords);
                }
            }
        }
    }

    /**
     * Update the Map
     */
    public void updateSmoke(){
        //Have to remove all smoke at once before creating new ones.
        for (SmokeCloud cloud : server.getSmokeCloudList()){
            server.removeSmokeTerrain(cloud);
            // Dissipate the cloud, this gets handled in FireProcessor if 
            //  TO start fires is on
            if (!game.getOptions().booleanOption("tacops_start_fire")) {
                if ((cloud.getDuration() > 0)
                        && ((cloud.getDuration() - 1) > 0)) {
                    cloud.setDuration(cloud.getDuration() - 1);
                }
                if (cloud.getDuration() < 1) {
                    cloud.setSmokeLevel(0);
                }
            }
        }
        
        //Remove any smoke clouds that no longer exist
        removeEmptyClouds();
        //Create new Smoke Clouds.
        for ( SmokeCloud cloud : server.getSmokeCloudList() ){
            if ((cloud.getCoordsList().size() > 0)
                    && (cloud.getSmokeLevel() > 0)) {
                createSmokeTerrain(cloud);
            }
        }
    }
}
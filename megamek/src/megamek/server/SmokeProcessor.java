/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
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

import megamek.common.*;
import megamek.common.options.OptionsConstants;

public class SmokeProcessor extends DynamicTerrainProcessor {

    private Game game;
    Vector<Report> vPhaseReport;

    public SmokeProcessor(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    void doEndPhaseChanges(Vector<Report> vPhaseReport) {
        game = gameManager.getGame();

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
        for (SmokeCloud cloud: gameManager.getSmokeCloudList()) {
            if (cloud.getCoordsList().size() < 1) {
                cloudsToRemove.add(cloud);
            } else if (cloud.getSmokeLevel() < 1) {
                gameManager.removeSmokeTerrain(cloud);
                cloudsToRemove.add(cloud);
            }
        }
        gameManager.getGame().removeSmokeClouds(cloudsToRemove);
    }

    /**
     * Creates the Smoke Terrain Markers.
     * @param cloud
     */
    public void createSmokeTerrain(SmokeCloud cloud) {
        for (Coords coords : cloud.getCoordsList()) {
            Hex smokeHex = game.getBoard().getHex(coords);
            if (smokeHex != null) {
                if (smokeHex.containsTerrain(Terrains.SMOKE)) {
                    if (smokeHex.terrainLevel(Terrains.SMOKE) == SmokeCloud.SMOKE_LIGHT) {
                        smokeHex.removeTerrain(Terrains.SMOKE);
                        smokeHex.addTerrain(new Terrain(Terrains.SMOKE, SmokeCloud.SMOKE_HEAVY));
                        gameManager.getHexUpdateSet().add(coords);
                    }
                } else if (cloud.getSmokeLevel() > SmokeCloud.SMOKE_NONE) {
                    smokeHex.addTerrain(new Terrain(Terrains.SMOKE, cloud.getSmokeLevel()));
                    gameManager.getHexUpdateSet().add(coords);
                }
            }
        }
    }

    /**
     * Update the Map
     */
    public void updateSmoke() {
        //Have to remove all smoke at once before creating new ones.
        for (SmokeCloud cloud : gameManager.getSmokeCloudList()) {
            gameManager.removeSmokeTerrain(cloud);
            // Dissipate the cloud, this gets handled in FireProcessor if 
            //  TO start fires is on
            if (!game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_START_FIRE)) {
                if ((cloud.getDuration() > 0) && ((cloud.getDuration() - 1) > 0)) {
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
        for (SmokeCloud cloud : gameManager.getSmokeCloudList()) {
            if (!cloud.getCoordsList().isEmpty() && (cloud.getSmokeLevel() > 0)) {
                createSmokeTerrain(cloud);
            }
        }
    }
}
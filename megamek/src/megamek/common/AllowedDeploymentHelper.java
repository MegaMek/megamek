/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static megamek.common.DeploymentElevationType.*;

public record AllowedDeploymentHelper(Entity entity, Coords coords, Board board, Hex hex, Game game) {

    /**
     * Returns a list of elevations/altitudes that the given entity can deploy to at the given coords. This
     * can be anything from the seafloor, swimming, ice, water surface, ground, up to elevations and
     * altitudes. For VTOLs, elevations up to 10 are always included individually if available. Above that
     * and above all terrain features of the hex, only a single elevation is reported using the
     * ELEVATIONS_ABOVE marker, meaning that any elevation above the reported value is also available.
     * Altitudes are always reported individually (1 to 10).
     * 
     * @return All legal deployment elevations/altitudes
     */
    public List<ElevationOption> findAllowedElevations() {
        if (board.inSpace()) {
            throw new IllegalStateException("Cannot find allowed deployment elevations in space!");
        }
//        else if (entity.isHexProhibited(board, coords)) {
//            return Collections.emptyList();
//        }

        List<ElevationOption> result = new ArrayList<>();
        if (entity.isAero()) {
            result.addAll(allowedAeroAltitudes());
        } else {
            result.addAll(allowedGroundElevations());
        }
        return result;
    }

    private List<ElevationOption> allowedAeroAltitudes() {
        List<ElevationOption> result = new ArrayList<>();
        for (int altitude = Math.max(0, board.getHex(coords).ceiling(board.inAtmosphere()) + 1); altitude <= 10; altitude++) {
            result.add(new ElevationOption(altitude, ALTITUDE));
        }
        // TODO: Make GameManager respect selection
        // TODO: allow landing here?
        // TODO: remember altitude? -> DeploymentDisplay
        return result;
    }

    private List<ElevationOption> allowedGroundElevations() {
        List<ElevationOption> result = new ArrayList<>();
        if (hex.containsTerrain(Terrains.WATER)) {
            result.addAll(allowedElevationsWithWater());
        }
        if (hex.containsTerrain(Terrains.BRIDGE)) {
            result.addAll(allowedBridgeElevations());
        } else if (hex.containsTerrain(Terrains.BUILDING)) {
            result.addAll(findAllowedElevationsWithBuildings());
        }
        if (!hasZeroElevationOption(result)) {
            result.addAll(allowedZeroElevation());
        }

        // Bridges block deployment for units of more than 1 level height if they intersect; height() == 0 is 1 level
        if (hex.containsTerrain(Terrains.BRIDGE) && (entity.height() > 0)) {
            int bridgeHeight = hex.terrainLevel(Terrains.BRIDGE_ELEV);
            result.removeIf(o -> (o.elevation() < bridgeHeight) && (o.elevation() + entity.getHeight() >= bridgeHeight));
        }

        if (entity.getMovementMode().isVTOL()) {
            result.addAll(findAllowedVTOLElevations());
        }

        //TODO: test stacking violations
        result.removeIf(o -> entity.isLocationProhibited(coords, o.elevation()));

        return result;
    }

    private List<ElevationOption> allowedZeroElevation() {
        List<ElevationOption> result = new ArrayList<>();
        if (!entity.getMovementMode().isSubmarine() && hex.terrainLevel(Terrains.WATER) < 1) {
            result.add(new ElevationOption(0, ON_GROUND));
        }
        return result;
    }

    private List<ElevationOption> allowedElevationsWithWater() {
        List<ElevationOption> result = new ArrayList<>();
        int depth = hex.terrainLevel(Terrains.WATER);
        // Ice matters only when there is water
        boolean hasIce = hex.containsTerrain(Terrains.ICE);
        EntityMovementMode moveMode = entity.getMovementMode();

        if ((moveMode.isNaval() || moveMode.isHydrofoil() || moveMode.isHover()) && !hasIce) {
            result.add(new ElevationOption(0, WATER_SURFACE));
        } else if ((entity instanceof Infantry infantry) && infantry.isNonMechSCUBA()) {
            for (int elevation = -1; elevation >= Math.max(-depth, -2); elevation--) {
                result.add(new ElevationOption(elevation, SUBMERGED));
            }
        } else if (entity.getMovementMode().isSubmarine() || entity.hasUMU()) {
            // TODO: what depths can an upright UMU mek go to? -1? head above water
            for (int elevation = -1; elevation >= -depth; elevation--) {
                result.add(new ElevationOption(elevation, SUBMERGED));
            }
        } else if (!moveMode.isTrackedWheeledOrHover() && (!hasIce || (depth > entity.height()))) {
            // when there is ice over depth 1 water, don't allow standing Meks to deploy under the ice
            result.add(new ElevationOption(hex.floor() - hex.getLevel(), ON_SEAFLOOR));

        }
        if (hasIce && !entity.getMovementMode().isSubmarine()) {
            result.add(new ElevationOption(0, ON_ICE));
        }
        return result;
    }

    private List<ElevationOption> allowedBridgeElevations() {
        List<ElevationOption> result = new ArrayList<>();
        int bridgeHeight = hex.terrainLevel(Terrains.BRIDGE_ELEV);
        if ((bridgeHeight > 0) && !entity.isNaval()) {
            result.add(new ElevationOption(bridgeHeight, BRIDGE));
        }
        return result;
    }

    private List<ElevationOption> findAllowedVTOLElevations() {
        List<ElevationOption> result = new ArrayList<>();
        if (entity instanceof VTOL) {
            for (int elevation = 1; elevation < Math.max(10, hex.ceiling() + 1); elevation++) {
                result.add(new ElevationOption(elevation, ELEVATION));
            }
            result.add(new ElevationOption(Math.max(10, hex.ceiling() + 1), ELEVATIONS_ABOVE));
            // TODO: Make GameManager respect selection (when landed)
            // TODO: Choose elevation for 11+
        }
        return result;
    }

    private boolean hasZeroElevationOption(List<ElevationOption> options) {
        return options.stream().anyMatch(o -> o.elevation() == 0);
    }

    private List<ElevationOption> findAllowedElevationsWithBuildings() {
        List<ElevationOption> result = new ArrayList<>();
        if ((entity instanceof VTOL) || entity.getMovementMode().isWiGE()) {
            return result;
        }

        int height = hex.terrainLevel(Terrains.BLDG_ELEV);
        if (Compute.stackingViolation(game, entity, 0, coords, null, entity.climbMode()) == null) {
            result.add((new ElevationOption(0, ON_GROUND)));
        }

        for (int elevation = 1; elevation < height; elevation++) {
            if (Compute.stackingViolation(game, entity, elevation, coords, null, entity.climbMode()) == null) {
                result.add((new ElevationOption(elevation, BUILDING_FLOOR)));
            }
        }

        if (Compute.stackingViolation(game, entity, height, coords, null, entity.climbMode()) == null) {
            result.add((new ElevationOption(height, BUILDING_TOP)));
        }
        return result;
    }
}

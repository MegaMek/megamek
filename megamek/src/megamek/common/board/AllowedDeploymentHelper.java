/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.board;

import static megamek.common.board.DeploymentElevationType.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.common.Hex;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Infantry;
import megamek.common.units.Tank;
import megamek.common.units.Terrains;
import megamek.common.units.VTOL;

public record AllowedDeploymentHelper(Entity entity, Coords coords, Board board, Hex hex, Game game) {

    /**
     * Returns a list of elevations/altitudes that the given entity can deploy to at the given coords. This can be
     * anything from the seafloor, swimming, ice, water surface, ground, up to elevations and altitudes. For VTOLs,
     * elevations up to 10 are always included individually if available. Above that and above all terrain features of
     * the hex, only a single elevation is reported using the ELEVATIONS_ABOVE marker, meaning that any elevation above
     * the reported value is also available. Altitudes are always reported individually (0 to 10).
     *
     * @return All legal deployment elevations/altitudes
     */
    public List<ElevationOption> findAllowedElevations() {
        return findAllowedElevations(null);
    }

    /**
     * Returns a list of elevations/altitudes that the given entity can deploy to at the given coords that are of the
     * given type. This can be anything from the seafloor, swimming, ice, water surface, ground, up to elevations and
     * altitudes. For VTOLs, elevations up to 10 are always included individually if available. Above that and above all
     * terrain features of the hex, only a single elevation is reported using the ELEVATIONS_ABOVE marker, meaning that
     * any elevation above the reported value is also available. Altitudes are always reported individually (0 to 10).
     *
     * @return All legal deployment elevations/altitudes of the given type
     */
    public List<ElevationOption> findAllowedElevations(@Nullable DeploymentElevationType limitToType) {
        if (board.isSpace()) {
            throw new IllegalStateException("Cannot find allowed deployment elevations in space!");
        }

        List<ElevationOption> result = new ArrayList<>();
        if (entity.isAero()) {
            result.addAll(allowedAeroAltitudes());
        } else {
            result.addAll(allowedGroundElevations());
        }
        result.removeIf(o -> entity.isLocationProhibited(coords, board().getBoardId(), o.elevation()));
        result.removeIf(o -> Compute.stackingViolation(game, entity, o.elevation(), coords,
              board.getBoardId(), null, entity.climbMode(), true) != null);

        if (entity.getMovementMode().isWiGE()) {
            addAirborneWiGEOptions(result);
        }

        if (limitToType != null) {
            result.removeIf(o -> o.type() != limitToType);
        }

        Collections.sort(result);
        return result;
    }

    /**
     * Adds airborne elevations where the WiGE could deploy landed.
     *
     * @param result The current options where the WiGE is landed
     */
    private void addAirborneWiGEOptions(List<ElevationOption> result) {
        // WiGE may also deploy flying wherever they can be grounded
        List<ElevationOption> wIGEOptions = new ArrayList<>();
        for (ElevationOption currentOption : result) {
            if (!hasElevationOption(result, currentOption.elevation() + 1)) {
                // If an elevation already exists, it is probably a bridge; at that elevation, the WiGE is landed
                wIGEOptions.add(new ElevationOption(currentOption.elevation() + 1, ELEVATION));
            }
        }
        result.addAll(wIGEOptions);
    }

    private List<ElevationOption> allowedAeroAltitudes() {
        List<ElevationOption> result = new ArrayList<>();
        if (board.isGround()) {
            result.add(new ElevationOption(0, ON_GROUND));
        }
        int startingAltitude = 1;
        if (board.isLowAltitude()) {
            // Stay above terrain on an atmospheric board, but never below altitude 1
            startingAltitude += Math.max(0, board.getHex(coords).ceiling(true));
        }
        for (int altitude = startingAltitude; altitude <= 10; altitude++) {
            result.add(new ElevationOption(altitude, ALTITUDE));
        }
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
            result.removeIf(o -> (o.elevation() < bridgeHeight) &&
                  (o.elevation() + entity.getHeight() >= bridgeHeight));
        }

        if (entity.getMovementMode().isVTOL()) {
            List<ElevationOption> vtolElevations = findAllowedVTOLElevations();
            // remove VTOL elevations that are already present (= where the VTOl can land)
            for (ElevationOption elevationOption : result) {
                vtolElevations.removeIf(o -> (o.elevation() == elevationOption.elevation()));
            }
            result.addAll(vtolElevations);
        }

        return result;
    }

    /**
     * Adds a 0 elevation deployment option if there isn't already one by other considerations and there should be one.
     *
     * @return Up to one additional surface deployment option
     */
    private List<ElevationOption> allowedZeroElevation() {
        List<ElevationOption> result = new ArrayList<>();
        if (!entity.getMovementMode().isSubmarine() && hex.terrainLevel(Terrains.WATER) < 1) {
            result.add(new ElevationOption(0, ON_GROUND));
        } else if (entity.getMovementMode().isSubmarine() && hex.terrainLevel(Terrains.WATER) > 0) {
            result.add(new ElevationOption(0, WATER_SURFACE));
        }
        return result;
    }

    private List<ElevationOption> allowedElevationsWithWater() {
        List<ElevationOption> result = new ArrayList<>();
        int depth = hex.terrainLevel(Terrains.WATER);
        // Ice matters only when there is water
        boolean hasIce = hex.containsTerrain(Terrains.ICE);
        EntityMovementMode moveMode = entity.getMovementMode();

        if ((moveMode.isNaval() || moveMode.isHydrofoil() || moveMode.isHoverOrWiGE()) && !hasIce) {
            result.add(new ElevationOption(0, WATER_SURFACE));
        } else if ((entity instanceof Infantry infantry) && infantry.isNonMechSCUBA()) {
            for (int elevation = -1; elevation >= Math.max(-depth, -2); elevation--) {
                result.add(new ElevationOption(elevation, SUBMERGED));
            }
        } else if (entity.getMovementMode().isSubmarine() || entity.hasUMU()) {
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
        if (entity instanceof VTOL || entity.getMovementMode().isHoverVTOLOrWiGE()) {
            for (int elevation = 1; elevation < Math.max(10, hex.ceiling() + 1); elevation++) {
                result.add(new ElevationOption(elevation, ELEVATION));
            }
            result.add(new ElevationOption(Math.max(10, hex.ceiling() + 1), ELEVATIONS_ABOVE));
        }
        return result;
    }

    /**
     * @return True if the given options contain at least one with elevation 0.
     */
    private boolean hasZeroElevationOption(List<ElevationOption> options) {
        return hasElevationOption(options, 0);
    }

    /**
     * @return True if the given options contain at least one of the given elevation.
     */
    private boolean hasElevationOption(List<ElevationOption> options, int elevation) {
        return options.stream().anyMatch(o -> o.elevation() == elevation);
    }

    private List<ElevationOption> findAllowedElevationsWithBuildings() {
        List<ElevationOption> result = new ArrayList<>();
        int height = hex.terrainLevel(Terrains.BLDG_ELEV);
        result.add((new ElevationOption(0, ON_GROUND)));
        if (!(entity instanceof Tank)) {
            for (int elevation = 1; elevation < height; elevation++) {
                result.add((new ElevationOption(elevation, BUILDING_FLOOR)));
            }
        }
        result.add((new ElevationOption(height, BUILDING_TOP)));
        return result;
    }
}

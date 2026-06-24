/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common.equipment;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.BridgeConstruction;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;

/**
 * Stateless rules helper for Bridge-Layer (AVLB) equipment, TM p.242 / TW. Holds the carried-bridge query, deploy
 * eligibility, hit-redirection and weapon-fire-block logic that operates on an {@link Entity} and its bridgelayer
 * {@link MiscMounted}s. Extracted from {@link Entity} so the bridgelayer rules do not add to that already very large
 * class; this mirrors the codebase's other Entity-operating utilities ({@link BridgeConstruction},
 * {@code megamek.common.compute.Compute}).
 *
 * @author Claude Code (Opus 4.8)
 */
public final class BridgeLayerLogic {

    /** Dedicated logger for Bridge-Layer (AVLB) diagnostics; see {@link BridgeLayerState#DIAGNOSTIC_LOGGER_NAME}. */
    private static final MMLogger AVLB_LOGGER = MMLogger.create(BridgeLayerState.DIAGNOSTIC_LOGGER_NAME);

    private BridgeLayerLogic() {
    }

    /**
     * @param entity the unit to inspect
     *
     * @return all carried, still-deployable Bridge-Layer (AVLB) mounts on the unit, in equipment order - each a
     *       bridgelayer whose folding bridge is not yet deployed, whose deploy mechanism is intact, that still has
     *       Construction Factor, and that is not itself destroyed. A unit may carry more than one (e.g. the Prometheus
     *       has a Right and a Left bridge), and the player chooses which to deploy. TM p.242 / TW.
     */
    public static List<MiscMounted> getDeployableBridgeLayers(Entity entity) {
        List<MiscMounted> deployable = new ArrayList<>();
        for (MiscMounted misc : entity.getMisc()) {
            BridgeLayerState bridgeState = misc.getBridgeLayerState();
            if ((bridgeState != null) && !bridgeState.isDeployed() && !bridgeState.isDeployMechanismDisabled()
                  && (bridgeState.getCurrentCF() > 0) && !misc.isInoperable()) {
                deployable.add(misc);
            }
        }
        return deployable;
    }

    /**
     * @param entity the unit to inspect
     *
     * @return the first carried, still-deployable Bridge-Layer (AVLB) mount on the unit, or {@code null} if none. Used
     *       for eligibility checks; see {@link #getDeployableBridgeLayers(Entity)} for the full list the player chooses
     *       from.
     */
    public static @Nullable MiscMounted getDeployableBridgeLayer(Entity entity) {
        List<MiscMounted> deployable = getDeployableBridgeLayers(entity);
        return deployable.isEmpty() ? null : deployable.get(0);
    }

    /**
     * @param entity the unit to inspect
     *
     * @return the Bridge-Layer mount on the unit that has a deploy declared and awaiting placement, or {@code null} if
     *       none.
     */
    public static @Nullable MiscMounted getPendingDeployBridgeLayer(Entity entity) {
        for (MiscMounted misc : entity.getMisc()) {
            BridgeLayerState bridgeState = misc.getBridgeLayerState();
            if ((bridgeState != null) && bridgeState.isDeployPending() && !bridgeState.isDeployed()) {
                return misc;
            }
        }
        return null;
    }

    /**
     * @param entity the unit taking the hit
     * @param hit    the incoming hit
     *
     * @return the carried (not-yet-deployed) bridgelayer mount whose folding bridge should absorb a hit to this
     *       location, or {@code null} if none applies. A hit to the location where a bridgelayer is mounted is absorbed
     *       by the carried bridge (e.g. the Right/Left side bridges on the Prometheus absorb RS/LS hits). On a Support
     *       Vehicle a hit to the turret is also absorbed (TM p.242 / TW; this covers turret-mounted SV bridgelayers,
     *       while the mounted-location rule still protects side/body-mounted ones). Returns {@code null} once the
     *       carried bridge has no Construction Factor left - it is destroyed and the location then takes damage
     *       normally.
     */
    public static @Nullable MiscMounted getBridgeLayerForHit(Entity entity, HitData hit) {
        boolean isSupportVehicleTurretHit = entity.isSupportVehicle() && (entity instanceof Tank)
              && (hit.getLocation() == Tank.LOC_TURRET);
        for (MiscMounted misc : entity.getMisc()) {
            BridgeLayerState bridgeState = misc.getBridgeLayerState();
            if ((bridgeState == null) || bridgeState.isDeployed() || (bridgeState.getCurrentCF() <= 0)
                  || misc.isMissing()) {
                continue;
            }
            // The bridge absorbs hits to the location where it is mounted; on a Support Vehicle a turret hit is
            // absorbed too (regardless of where the bridgelayer sits).
            if ((misc.getLocation() == hit.getLocation()) || isSupportVehicleTurretHit) {
                return misc;
            }
        }
        return null;
    }

    /**
     * @param entity   the firing unit
     * @param location a weapon's location index
     *
     * @return whether a carried, not-yet-deployed bridgelayer occupies this location and so blocks weapons mounted
     *       there from firing (TM p.242 / TW: "If the bridge has not yet been deployed, the unit cannot make attacks
     *       from any weapons mounted in its location."). A destroyed or already-deployed bridge no longer blocks fire.
     */
    public static boolean isWeaponLocationBlockedByCarriedBridge(Entity entity, int location) {
        for (MiscMounted misc : entity.getMisc()) {
            BridgeLayerState bridgeState = misc.getBridgeLayerState();
            if ((bridgeState == null) || bridgeState.isDeployed() || (bridgeState.getCurrentCF() <= 0)
                  || misc.isMissing()) {
                continue;
            }
            if (misc.getLocation() == location) {
                AVLB_LOGGER.debug("[AVLB] {}: weapons in location {} cannot fire - carried bridge not yet deployed",
                      entity.getShortName(), location);
                return true;
            }
        }
        return false;
    }

    /**
     * @param entity the unit to inspect
     *
     * @return whether the unit's motive system currently permits deploying a bridgelayer (TM p.242 / TW): ground units
     *       always qualify; Hover and WiGE units only when landed (they spent no MP the previous round and sit at
     *       ground level, not airborne); Naval, Hydrofoil and Submersible units only when surfaced (at Depth 0). The
     *       stationary-this-turn requirement is enforced separately, since the deploy declaration must be the unit's
     *       only action.
     */
    public static boolean isBridgeLayerMotiveReady(Entity entity) {
        EntityMovementMode movementMode = entity.getMovementMode();
        if (movementMode.isHoverOrWiGE()) {
            return (entity.getMpUsedLastRound() == 0) && (entity.getElevation() == 0)
                  && !entity.isAirborneVTOLorWIGE();
        }
        if (movementMode.isNaval() || movementMode.isHydrofoil() || movementMode.isSubmarine()) {
            return entity.getElevation() == 0;
        }
        return true;
    }

    /**
     * @param entity the unit to inspect
     *
     * @return the hex directly in front of the unit (in its current facing), where its bridgelayer would deploy its
     *       folding bridge, or {@code null} if the unit has no board position.
     */
    public static @Nullable Coords getBridgeLayerTargetCoords(Entity entity) {
        Coords currentPosition = entity.getPosition();
        return (currentPosition == null) ? null : currentPosition.translated(entity.getFacing());
    }

    /**
     * @param entity the unit to inspect
     *
     * @return the exits bitmask for a bridge deployed straight ahead of the unit, connecting the front hexside and its
     *       opposite so the single-hex span runs along the unit's facing (TM p.242 / TW: the bridge cannot extend at an
     *       angle).
     */
    public static int getBridgeLayerExits(Entity entity) {
        int facing = entity.getFacing();
        return BridgeConstruction.exitsFor(facing, (facing + 3) % 6);
    }

    /**
     * Determines whether the unit may declare a bridgelayer deployment now (during the movement phase). Each failing
     * condition is logged at DEBUG with its reason and relevant values so a playtest can diagnose a disabled "Deploy
     * Bridge" button from megamek.log. TM p.242 / TW.
     *
     * @param entity the unit declaring the deployment
     * @param game   the current game (for the board and the target hex validation)
     *
     * @return {@code true} if a deployment may be declared
     */
    public static boolean canDeclareBridgeDeploy(Entity entity, Game game) {
        if (getDeployableBridgeLayer(entity) == null) {
            AVLB_LOGGER.debug("[AVLB] {}: deploy unavailable - no functional carried bridgelayer",
                  entity.getShortName());
            return false;
        }
        if (getPendingDeployBridgeLayer(entity) != null) {
            AVLB_LOGGER.debug("[AVLB] {}: deploy unavailable - a bridge deployment is already pending",
                  entity.getShortName());
            return false;
        }
        if (!isBridgeLayerMotiveReady(entity)) {
            AVLB_LOGGER.debug("[AVLB] {}: deploy unavailable - motive system not ready (mode {}, elevation {}, "
                        + "mpUsedLastRound {})", entity.getShortName(), entity.getMovementMode(), entity.getElevation(),
                  entity.getMpUsedLastRound());
            return false;
        }
        Coords target = getBridgeLayerTargetCoords(entity);
        if (target == null) {
            AVLB_LOGGER.debug("[AVLB] {}: deploy unavailable - unit has no position", entity.getShortName());
            return false;
        }
        Board board = game.getBoard(entity.getBoardId());
        int exits = getBridgeLayerExits(entity);
        if (!BridgeConstruction.isValidBridgeSite(board, target, exits)) {
            AVLB_LOGGER.debug("[AVLB] {}: deploy unavailable - {} is not a valid bridge site (exits bitmask {})",
                  entity.getShortName(), target, exits);
            return false;
        }
        // A bridge may be placed in a water hex (adjacent to >=1 land/bridge - already covered by isValidBridgeSite,
        // which also lets a naval layer in water bridge to a far land bank). A DRY hex is only a legal site as a gap
        // "between two elevated hexes", so both banks along the facing must be rims higher than the target floor (the
        // shared site check accepts a single rim, for engineer span-chaining, which is too loose for AVLB).
        // TM p.242 / TW.
        Hex targetHex = board.getHex(target);
        if ((targetHex != null) && !BridgeConstruction.isOverWater(targetHex)) {
            Hex nearBank = board.getHex(entity.getPosition());
            Hex farBank = board.getHex(target.translated(entity.getFacing()));
            boolean spansTwoElevatedHexes = (nearBank != null) && (farBank != null)
                  && BridgeConstruction.isAnchoringBank(nearBank, targetHex)
                  && BridgeConstruction.isAnchoringBank(farBank, targetHex);
            if (!spansTwoElevatedHexes) {
                AVLB_LOGGER.debug("[AVLB] {}: deploy unavailable - {} is dry ground, not a gap between two elevated "
                      + "hexes (both banks along the facing must be rims above it)", entity.getShortName(), target);
                return false;
            }
        }
        return true;
    }
}

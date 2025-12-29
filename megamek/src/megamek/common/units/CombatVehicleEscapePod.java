/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.units;

import java.io.Serial;

import megamek.common.MPCalculationSetting;
import megamek.common.board.Coords;
import megamek.logging.MMLogger;

/**
 * Represents a Combat Vehicle Escape Pod (CVEP) that has been launched from a combat vehicle. Per TO:AUE p.121, this is
 * a one-use system that allows vehicle crew to escape their vehicle.
 *
 * <p>The escape pod lands on the battlefield and the crew can choose to:
 * <ul>
 *   <li>Exit the pod as conventional foot infantry</li>
 *   <li>Remain inside the pod (useful in water or toxic environments)</li>
 * </ul>
 *
 * <p>The pod itself is immobile and uses the Life Boat sprite.
 *
 * @author MegaMek Team
 */
public class CombatVehicleEscapePod extends EjectedCrew {
    private static final MMLogger logger = MMLogger.create(CombatVehicleEscapePod.class);

    @Serial
    private static final long serialVersionUID = 1L;

    /** Chassis name used for image lookup in mekset.txt */
    public static final String CVEP_CHASSIS = "Life Boat";

    /** Whether the crew is still inside the pod (vs having exited as infantry) */
    private boolean crewInside = true;

    /**
     * Creates a new Combat Vehicle Escape Pod from a launching vehicle.
     *
     * @param originalRide  The Tank that launched this escape pod
     * @param landingCoords The coordinates where the pod landed
     */
    public CombatVehicleEscapePod(Tank originalRide, Coords landingCoords) {
        super(originalRide);

        // Override chassis to use Life Boat sprite
        setChassis(CVEP_CHASSIS);
        setModel("from " + originalRide.getDisplayName());

        // Generate the display name
        setDisplayName(CVEP_CHASSIS + " from " + originalRide.getDisplayName());

        // Set position
        setPosition(landingCoords);

        // Escape pods are immobile - set movement to 0
        setOriginalWalkMP(0);
        setMovementMode(EntityMovementMode.NONE);

        logger.debug("Created CVEP at {} from {}", landingCoords.toFriendlyString(),
              originalRide.getDisplayName());
    }

    /**
     * Default constructor for serialization/MUL parser.
     */
    public CombatVehicleEscapePod() {
        super();
        setChassis(CVEP_CHASSIS);
        setMovementMode(EntityMovementMode.NONE);
    }

    @Override
    public boolean isImmobile() {
        // Escape pods cannot move while crew is inside
        return crewInside;
    }

    @Override
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        // Pods are immobile while crew is inside
        return crewInside ? 0 : super.getWalkMP(mpCalculationSetting);
    }

    @Override
    public int getRunMP(MPCalculationSetting mpCalculationSetting) {
        return crewInside ? 0 : super.getRunMP(mpCalculationSetting);
    }

    @Override
    public int getJumpMP(MPCalculationSetting mpCalculationSetting) {
        return 0; // Pods never jump
    }

    /**
     * @return true if the crew is still inside the pod
     */
    public boolean isCrewInside() {
        return crewInside;
    }

    /**
     * Sets whether the crew is inside the pod.
     *
     * @param crewInside true if crew is inside, false if they have exited
     */
    public void setCrewInside(boolean crewInside) {
        this.crewInside = crewInside;
        // When crew exits, they become normal infantry and can move
        if (!crewInside) {
            setChassis(VEE_EJECT_NAME); // Revert to infantry display
            setMovementMode(EntityMovementMode.INF_LEG);
        }
    }

    /**
     * Checks if the crew can safely exit the pod at its current location. Crew cannot exit into deep water or other
     * hazardous terrain without appropriate equipment.
     *
     * @return true if crew can safely exit
     */
    public boolean canCrewExit() {
        if (!crewInside || getCrew() == null || getCrew().isDead()) {
            return false;
        }

        if (getGame() == null || getPosition() == null) {
            return false;
        }

        // Check for hazardous terrain at current position
        // Crew should stay in pod if in deep water (depth > 0)
        var hex = getGame().getBoard().getHex(getPosition());
        if (hex != null && hex.containsTerrain(Terrains.WATER)) {
            int depth = hex.terrainLevel(Terrains.WATER);
            if (depth > 0) {
                return false; // Can't exit into water
            }
        }

        return true;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_COMBAT_VEHICLE_ESCAPE_POD | Entity.ETYPE_INFANTRY;
    }

    @Override
    public boolean isEligibleForMovement() {
        // Pods don't move while crew is inside
        return !crewInside && super.isEligibleForMovement();
    }

    @Override
    public boolean isEligibleForFiring() {
        // Crew cannot fire weapons while inside the pod
        return !crewInside && super.isEligibleForFiring();
    }

    @Override
    public boolean isEligibleForTargetingPhase() {
        // Crew cannot use indirect fire while inside the pod
        return !crewInside && super.isEligibleForTargetingPhase();
    }

    @Override
    public boolean isEligibleForOffboard() {
        // Crew cannot use offboard attacks while inside the pod
        return !crewInside && super.isEligibleForOffboard();
    }

    @Override
    public boolean isEligibleForPhysical() {
        // Crew cannot make physical attacks while inside the pod
        return !crewInside && super.isEligibleForPhysical();
    }

    @Override
    public boolean isSelectableThisTurn() {
        // Can be selected if crew can exit
        if (crewInside) {
            return canCrewExit();
        }
        return super.isSelectableThisTurn();
    }
}

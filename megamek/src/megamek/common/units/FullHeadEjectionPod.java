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
 * Represents a Full-Head Ejection Pod that has been launched from a BattleMek or IndustrialMek. Per TO:AUE p.121, this
 * system ejects the entire head assembly as an escape capsule.
 *
 * <p>Key differences from standard ejection:</p>
 * <ul>
 *   <li>Can be deployed at any phase of a turn</li>
 *   <li>MekWarrior takes 1 automatic damage on launch</li>
 *   <li>12 hex landing range (forward arc only if prone)</li>
 *   <li>PSR +3 to land on target; scatter 1d6/2 hexes on failure</li>
 *   <li>Additional ejection roll +2 for second damage point</li>
 *   <li>Uses head's armor/structure values (not simple threshold)</li>
 *   <li>If submerged: rockets to surface and floats as displacement hull</li>
 * </ul>
 *
 * <p>After landing, the MekWarrior may exit as conventional foot infantry or remain inside.</p>
 *
 * <p><b>Damage Model:</b> Unlike CVEP's simple 2-point threshold, damage to a Full-Head Ejection Pod
 * applies to the head's remaining armor first, then internal structure. The pod is breached when
 * internal structure is reduced to 0.</p>
 *
 * @author MegaMek Team
 */
public class FullHeadEjectionPod extends EjectedCrew {
    private static final MMLogger logger = MMLogger.create(FullHeadEjectionPod.class);

    @Serial
    private static final long serialVersionUID = 1L;

    /** Chassis name used for image lookup in wreckset.txt */
    public static final String FHEP_SPRITE_NAME = "Full Head Escape Pod";

    /** Display name for the escape pod in game logs and UI */
    public static final String FHEP_DISPLAY_NAME = "FHEP";

    /** Original head armor value from the source Mek */
    private int originalHeadArmor;

    /** Original head internal structure value from the source Mek */
    private int originalHeadInternalStructure;

    /** Current head armor remaining */
    private int currentHeadArmor;

    /** Current head internal structure remaining */
    private int currentHeadInternalStructure;

    /** Whether the crew is still inside the pod (vs having exited as infantry) */
    private boolean crewInside = true;

    /** Whether the pod has been breached (internal structure reduced to 0) */
    private boolean breached = false;

    /** Whether the pod is floating on water (launched while submerged) */
    private boolean floating = false;

    /**
     * Creates a new Full-Head Ejection Pod from a launching Mek.
     *
     * @param originalRide  The Mek that launched this escape pod
     * @param landingCoords The coordinates where the pod landed
     */
    public FullHeadEjectionPod(Mek originalRide, Coords landingCoords) {
        super(originalRide);

        // Copy head armor/structure values BEFORE the head is destroyed
        this.originalHeadArmor = originalRide.getOArmor(Mek.LOC_HEAD);
        this.originalHeadInternalStructure = originalRide.getOInternal(Mek.LOC_HEAD);
        this.currentHeadArmor = originalRide.getArmor(Mek.LOC_HEAD);
        this.currentHeadInternalStructure = originalRide.getInternal(Mek.LOC_HEAD);

        // Override chassis to use FHEP sprite for image lookup
        setChassis(FHEP_SPRITE_NAME);
        setModel("from " + originalRide.getDisplayName());

        // Generate the display name using FHEP terminology
        setDisplayName(FHEP_DISPLAY_NAME + " from " + originalRide.getDisplayName());

        // Set position
        setPosition(landingCoords);

        // Escape pods are immobile - set movement to 0
        setOriginalWalkMP(0);
        setMovementMode(EntityMovementMode.NONE);

        logger.debug("Created FHEP at {} from {} (armor: {}/{}, structure: {}/{})",
              landingCoords.toFriendlyString(), originalRide.getDisplayName(),
              currentHeadArmor, originalHeadArmor,
              currentHeadInternalStructure, originalHeadInternalStructure);
    }

    /**
     * Default constructor for serialization/MUL parser.
     */
    public FullHeadEjectionPod() {
        super();
        setChassis(FHEP_SPRITE_NAME);
        setMovementMode(EntityMovementMode.NONE);
    }

    @Override
    public boolean isImmobile() {
        // Per TO:AUE p.121, FHEP is always targeted as an immobile unit
        return true;
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
            setChassis(MW_EJECT_NAME); // MekWarrior designation
            setMovementMode(EntityMovementMode.INF_LEG);
            setFloating(false); // No longer floating if crew exits
        }
    }

    /**
     * @return the original head armor value from the source Mek
     */
    public int getOriginalHeadArmor() {
        return originalHeadArmor;
    }

    /**
     * @return the original head internal structure value from the source Mek
     */
    public int getOriginalHeadInternalStructure() {
        return originalHeadInternalStructure;
    }

    /**
     * @return the current head armor remaining
     */
    public int getCurrentHeadArmor() {
        return currentHeadArmor;
    }

    /**
     * Sets the current head armor value.
     *
     * @param armor the new armor value
     */
    public void setCurrentHeadArmor(int armor) {
        this.currentHeadArmor = Math.max(0, armor);
    }

    /**
     * @return the current head internal structure remaining
     */
    public int getCurrentHeadInternalStructure() {
        return currentHeadInternalStructure;
    }

    /**
     * Sets the current head internal structure value.
     *
     * @param structure the new structure value
     */
    public void setCurrentHeadInternalStructure(int structure) {
        this.currentHeadInternalStructure = Math.max(0, structure);
        if (this.currentHeadInternalStructure <= 0) {
            this.breached = true;
        }
    }

    /**
     * Applies damage to the escape pod. Damage applies to armor first, then internal structure. The pod is breached
     * when internal structure is reduced to 0.
     *
     * @param damage the amount of damage to apply
     *
     * @return true if the pod was breached by this damage
     */
    public boolean applyDamage(int damage) {
        if (breached || damage <= 0) {
            return false;
        }

        int remainingDamage = damage;

        // Apply to armor first
        if (currentHeadArmor > 0) {
            int armorDamage = Math.min(remainingDamage, currentHeadArmor);
            currentHeadArmor -= armorDamage;
            remainingDamage -= armorDamage;
            logger.debug("FHEP {} armor takes {} damage, {} remaining",
                  getDisplayName(), armorDamage, currentHeadArmor);
        }

        // Apply remaining to internal structure
        if (remainingDamage > 0 && currentHeadInternalStructure > 0) {
            int structureDamage = Math.min(remainingDamage, currentHeadInternalStructure);
            currentHeadInternalStructure -= structureDamage;
            logger.debug("FHEP {} structure takes {} damage, {} remaining",
                  getDisplayName(), structureDamage, currentHeadInternalStructure);

            if (currentHeadInternalStructure <= 0) {
                breached = true;
                logger.debug("FHEP {} breached! Crew killed.", getDisplayName());
                return true;
            }
        }

        return false;
    }

    /**
     * Returns whether this pod has been breached (internal structure reduced to 0). A breached pod has its occupants
     * killed.
     *
     * @return true if the pod has been breached
     */
    public boolean isBreached() {
        return breached;
    }

    /**
     * Sets the breached status of the pod.
     *
     * @param breached true if the pod is breached
     */
    public void setBreached(boolean breached) {
        this.breached = breached;
    }

    /**
     * @return true if the pod is floating on water (launched while submerged)
     */
    public boolean isFloating() {
        return floating;
    }

    /**
     * Sets whether the pod is floating on water. When floating, the pod uses NAVAL movement mode and sits at elevation
     * 0 (water surface).
     *
     * @param floating true if the pod is floating
     */
    public void setFloating(boolean floating) {
        this.floating = floating;
        if (floating) {
            setMovementMode(EntityMovementMode.NAVAL);
            setElevation(0); // Water surface
        } else if (crewInside) {
            setMovementMode(EntityMovementMode.NONE);
        }
    }

    /**
     * Checks if the crew can safely exit the pod at its current location. Crew cannot exit into deep water or other
     * hazardous terrain.
     *
     * @return true if crew can safely exit
     */
    public boolean canCrewExit() {
        // Can't exit if pod is breached (crew is dead)
        if (breached) {
            return false;
        }

        if (!crewInside || getCrew() == null || getCrew().isDead()) {
            return false;
        }

        if (getGame() == null || getPosition() == null) {
            return false;
        }

        // Check for hazardous terrain at current position
        // Crew should stay in pod if in water (even if floating on surface)
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
        return Entity.ETYPE_FULL_HEAD_EJECTION_POD | Entity.ETYPE_INFANTRY;
    }

    /**
     * Returns false because FHEP uses its own damage model (head armor/structure), not the infantry platoon damage
     * model (burst-fire multipliers, "in the open" doubling, etc.). Per TO:AUE p.121, attacks against a jettisoned pod
     * are treated as targeting an immobile unit.
     *
     * @return false - FHEP is not conventional infantry for damage purposes
     */
    @Override
    public boolean isConventionalInfantry() {
        return false;
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

    /**
     * Override to use FHEP display name instead of chassis name for game messages. The chassis is set to "Full Head
     * Escape Pod" for sprite lookup, but we want "FHEP" in messages.
     *
     * @return Short name using FHEP terminology
     */
    @Override
    public String getShortNameRaw() {
        String unitName = FHEP_DISPLAY_NAME;
        unitName += (model == null || model.isBlank()) ? "" : " " + model;
        return unitName;
    }
}

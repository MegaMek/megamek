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
package megamek.common.equipment.enums;

/**
 * The standard nuclear weapons, with the values of the Standard Nuclear Weaponry Table (IO:AE 3rd printing, p.164).
 * Radii and damage reductions are in ground-map hexes; heights are Low-Altitude Map altitudes.
 */
public enum NuclearWeaponType {
    /** Type Ia "Davy Crockett-I", 0.5 kt, infantry-fired (IO:AE p.168). Cannot air-burst. */
    DAVY_CROCKETT_I(100, 1, 11, new BurstFigures(20, 5, 40), null, 6, 8, 0),
    /** Type Ib "Davy Crockett-M", 0.5 kt, Long Tom / Arrow IV round (IO:AE p.168). Cannot air-burst. */
    DAVY_CROCKETT_M(100, 1, 11, new BurstFigures(20, 5, 40), null, 6, 8, 0),
    /** Type II "Alamo", 5 kt (IO:AE p.169). */
    ALAMO(1_000, 10, 10, new BurstFigures(43, 23, 85), new BurstFigures(58, 18, 114), 8, 9, 1),
    /** Type III "Santa Ana", 50 kt (IO:AE p.169). */
    SANTA_ANA(10_000, 100, 9, new BurstFigures(92, 109, 184), new BurstFigures(123, 81, 246), 9, 10, 3),
    /** Type IV "Peacemaker", 500 kt (IO:AE p.170). */
    PEACEMAKER(100_000, 1_000, 8, new BurstFigures(198, 504, 397), new BurstFigures(264, 378, 529), 10, 10, 5);

    /**
     * The blast figures for one kind of detonation (ground burst or air burst).
     *
     * @param blastRadius     hexes from Ground Zero in which the weapon deals blast damage
     * @param damageReduction blast damage lost per hex from Ground Zero
     * @param secondaryRadius hexes from Ground Zero in which secondary effects apply
     */
    public record BurstFigures(int blastRadius, int damageReduction, int secondaryRadius) {}

    private final int impactDamage;
    private final int capitalDamage;
    private final int criticalHitTarget;
    private final BurstFigures groundBurst;
    private final BurstFigures airBurst;
    private final int blastHeight;
    private final int secondaryHeight;
    private final int craterDepth;

    NuclearWeaponType(int impactDamage, int capitalDamage, int criticalHitTarget, BurstFigures groundBurst,
          BurstFigures airBurst, int blastHeight, int secondaryHeight, int craterDepth) {
        this.impactDamage = impactDamage;
        this.capitalDamage = capitalDamage;
        this.criticalHitTarget = criticalHitTarget;
        this.groundBurst = groundBurst;
        this.airBurst = airBurst;
        this.blastHeight = blastHeight;
        this.secondaryHeight = secondaryHeight;
        this.craterDepth = craterDepth;
    }

    /**
     * @return the standard-scale damage at Ground Zero
     */
    public int getImpactDamage() {
        return impactDamage;
    }

    /**
     * @return the capital-scale damage against a spacecraft hull in space (1 percent of impact damage)
     */
    public int getCapitalDamage() {
        return capitalDamage;
    }

    /**
     * @return the unmodified 2d6 roll needed for the special critical hit in space
     */
    public int getCriticalHitTarget() {
        return criticalHitTarget;
    }

    /**
     * @return the blast figures for a ground burst
     */
    public BurstFigures getGroundBurst() {
        return groundBurst;
    }

    /**
     * @return {@code true} if this weapon can be set for an air burst
     */
    public boolean canAirBurst() {
        return airBurst != null;
    }

    /**
     * @return the blast figures for an air burst, or {@code null} if this weapon cannot air-burst
     */
    public BurstFigures getAirBurst() {
        return airBurst;
    }

    /**
     * @return the highest Low-Altitude Map altitude at which an airborne unit takes blast damage
     */
    public int getBlastHeight() {
        return blastHeight;
    }

    /**
     * @return the highest Low-Altitude Map altitude at which an airborne unit suffers secondary effects
     */
    public int getSecondaryHeight() {
        return secondaryHeight;
    }

    /**
     * @return the levels a ground burst lowers the terrain at Ground Zero
     */
    public int getCraterDepth() {
        return craterDepth;
    }
}

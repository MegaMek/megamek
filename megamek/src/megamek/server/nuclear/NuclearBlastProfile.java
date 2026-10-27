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
package megamek.server.nuclear;

import megamek.common.equipment.enums.NuclearWeaponType;
import megamek.common.equipment.enums.NuclearWeaponType.BurstFigures;

/**
 * The resolved numbers for one nuclear detonation on a ground map, and the blast geometry derived from them (IO:AE
 * pp. 164-165). Not serialized: it only lives while a detonation is scheduled or resolved.
 *
 * @param impactDamage    standard-scale damage at Ground Zero
 * @param damageReduction blast damage lost per hex from Ground Zero
 * @param blastRadius     hexes from Ground Zero in which blast damage applies
 * @param secondaryRadius hexes from Ground Zero in which secondary effects apply
 * @param craterDepth     levels the terrain is lowered at Ground Zero (0 for no crater)
 */
public record NuclearBlastProfile(int impactDamage, int damageReduction, int blastRadius, int secondaryRadius,
      int craterDepth) {

    /**
     * Terrain damaged by less than this has no nuclear terrain effect (woods lose a level per 20 points, IO:AE
     * p.165).
     */
    public static final int MINIMUM_TERRAIN_DAMAGE = 20;

    /**
     * @param weaponType the standard nuclear weapon
     *
     * @return the ground-burst profile of that weapon, from the Standard Nuclear Weaponry Table (IO:AE p.164)
     */
    public static NuclearBlastProfile groundBurst(NuclearWeaponType weaponType) {
        BurstFigures figures = weaponType.getGroundBurst();
        return new NuclearBlastProfile(weaponType.getImpactDamage(),
              figures.damageReduction(),
              figures.blastRadius(),
              figures.secondaryRadius(),
              weaponType.getCraterDepth());
    }

    /**
     * A profile from raw numbers, as entered with the GM nuke commands. With no blast radius given, the blast reaches
     * as far as the damage lasts.
     *
     * @param impactDamage    standard-scale damage at Ground Zero
     * @param damageReduction blast damage lost per hex; values below 1 are treated as 1
     * @param secondaryRadius hexes in which secondary effects apply
     * @param craterDepth     crater depth at Ground Zero
     *
     * @return the profile
     */
    public static NuclearBlastProfile custom(int impactDamage, int damageReduction, int secondaryRadius,
          int craterDepth) {
        int reduction = Math.max(1, damageReduction);
        return new NuclearBlastProfile(impactDamage, reduction, impactDamage / reduction, secondaryRadius,
              craterDepth);
    }

    /**
     * Blast damage at a distance: Base Impact Damage minus distance times Damage Reduction (IO:AE p.165). A Type Ia 8
     * hexes from Ground Zero deals 100 - 8 x 5 = 60.
     *
     * @param distance hexes from Ground Zero
     *
     * @return the blast damage, or 0 outside the blast radius
     */
    public int damageAt(int distance) {
        if ((distance < 0) || (distance > blastRadius)) {
            return 0;
        }
        return Math.max(0, impactDamage - (distance * damageReduction));
    }

    /**
     * Crater depth at a distance: the crater becomes 1 level shallower for every 2 hexes from Ground Zero (IO:AE
     * p.164). A Type III (depth 3) is 3 levels deep at Ground Zero, 2 at 2 hexes, 1 at 4 hexes and 0 at 6 hexes.
     *
     * @param distance hexes from Ground Zero
     *
     * @return the levels the terrain is lowered, 0 outside the crater
     */
    public int craterDepthAt(int distance) {
        if (distance < 0) {
            return 0;
        }
        return Math.max(0, craterDepth - (distance / 2));
    }

    /**
     * The Blast Height: the highest Low-Altitude Map altitude at which an airborne unit takes blast damage (IO:AE
     * p.164). A custom device has no table row, so it takes the height of the standard weapon closest to it in impact
     * damage: a custom 5,000-point device uses the Alamo's 8.
     *
     * @return the blast height in Low-Altitude Map altitudes
     */
    public int blastHeight() {
        return closestStandardWeapon().getBlastHeight();
    }

    /**
     * The Secondary Effect Height: the highest Low-Altitude Map altitude at which an airborne unit suffers the
     * secondary effects (IO:AE p.165). A custom device takes the height of the standard weapon closest to it in
     * impact damage, as for {@link #blastHeight()}: an Alamo's is 9.
     *
     * @return the secondary effect height in Low-Altitude Map altitudes
     */
    public int secondaryHeight() {
        return closestStandardWeapon().getSecondaryHeight();
    }

    private NuclearWeaponType closestStandardWeapon() {
        NuclearWeaponType closest = NuclearWeaponType.values()[0];
        for (NuclearWeaponType weaponType : NuclearWeaponType.values()) {
            if (Math.abs(weaponType.getImpactDamage() - impactDamage)
                  < Math.abs(closest.getImpactDamage() - impactDamage)) {
                closest = weaponType;
            }
        }
        return closest;
    }

    /**
     * @return the number of hex rings, counted from Ground Zero, that lie in the crater; units and terrain in them
     *       are annihilated
     */
    public int craterRadius() {
        return 2 * craterDepth;
    }

    /**
     * @return blast damage by distance from Ground Zero, index 0 being Ground Zero, ending at the last ring that still
     *       takes damage
     */
    public int[] blastDamageByDistance() {
        int rings = 0;
        while ((rings <= blastRadius) && (damageAt(rings) > 0)) {
            rings++;
        }
        int[] damages = new int[rings];
        for (int distance = 0; distance < rings; distance++) {
            damages[distance] = damageAt(distance);
        }
        return damages;
    }
}

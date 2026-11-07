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

import megamek.common.equipment.enums.NuclearBurstMode;
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
 * @param burstMode       ground burst or air burst; an air burst never makes a crater
 */
public record NuclearBlastProfile(int impactDamage, int damageReduction, int blastRadius, int secondaryRadius,
      int craterDepth, NuclearBurstMode burstMode) {

    /**
     * Terrain damaged by less than this has no nuclear terrain effect (woods lose a level per 20 points, IO:AE
     * p.165).
     */
    public static final int MINIMUM_TERRAIN_DAMAGE = 20;

    /** One Low-Altitude Map hex holds 17 ground hexes (IO:AE p.164, Standard Nuclear Weaponry Table footnote). */
    public static final int GROUND_HEXES_PER_LOW_ALTITUDE_HEX = 17;

    /** Low-Altitude Map distance is multiplied by 8.5 times the Damage Reduction (IO:AE p.166). */
    public static final double LOW_ALTITUDE_DISTANCE_FACTOR = 8.5;

    /** A unit in the detonation's own Low-Altitude Map hex counts as 0.5 hexes away (IO:AE p.166). */
    public static final double LOW_ALTITUDE_MINIMUM_DISTANCE = 0.5;

    /**
     * An air burst makes no crater (IO:AE p.164), whatever crater depth it was given. A profile read from a save made
     * before burst modes existed has no mode and is a ground burst.
     */
    public NuclearBlastProfile {
        if (burstMode == null) {
            burstMode = NuclearBurstMode.GROUND;
        }
        if (burstMode.isAirBurst()) {
            craterDepth = 0;
        }
    }

    /**
     * A ground-burst profile.
     *
     * @param impactDamage    standard-scale damage at Ground Zero
     * @param damageReduction blast damage lost per hex from Ground Zero
     * @param blastRadius     hexes from Ground Zero in which blast damage applies
     * @param secondaryRadius hexes from Ground Zero in which secondary effects apply
     * @param craterDepth     levels the terrain is lowered at Ground Zero (0 for no crater)
     */
    public NuclearBlastProfile(int impactDamage, int damageReduction, int blastRadius, int secondaryRadius,
          int craterDepth) {
        this(impactDamage, damageReduction, blastRadius, secondaryRadius, craterDepth, NuclearBurstMode.GROUND);
    }

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
     * @param weaponType the standard nuclear weapon; it must be able to air-burst
     *
     * @return the air-burst profile of that weapon, from the Standard Nuclear Weaponry Table (IO:AE p.164): no crater.
     *       An Alamo air burst reaches 58 hexes and loses 18 a hex, against 43 and 23 for its ground burst
     *
     * @throws IllegalArgumentException if the weapon has no air-burst figures, as the Davy Crockett
     */
    public static NuclearBlastProfile airBurst(NuclearWeaponType weaponType) {
        BurstFigures figures = weaponType.getAirBurst();
        if (figures == null) {
            throw new IllegalArgumentException(weaponType + " cannot air-burst");
        }
        return new NuclearBlastProfile(weaponType.getImpactDamage(),
              figures.damageReduction(),
              figures.blastRadius(),
              figures.secondaryRadius(),
              0,
              NuclearBurstMode.AIR);
    }

    /**
     * @param weaponType the standard nuclear weapon
     * @param burstMode  ground burst or air burst; an air burst needs a weapon that can air-burst
     *
     * @return the weapon's profile for that kind of detonation
     */
    public static NuclearBlastProfile of(NuclearWeaponType weaponType, NuclearBurstMode burstMode) {
        return burstMode.isAirBurst() ? airBurst(weaponType) : groundBurst(weaponType);
    }

    /**
     * A ground-burst profile from raw numbers, as entered with the GM nuke commands.
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
        return custom(impactDamage, damageReduction, secondaryRadius, craterDepth, NuclearBurstMode.GROUND);
    }

    /**
     * A profile from raw numbers, as entered with the GM nuke commands. With no blast radius given, the blast reaches
     * as far as the damage lasts. The GM enters the air-burst figures for an air burst; its crater depth is ignored.
     *
     * @param impactDamage    standard-scale damage at Ground Zero
     * @param damageReduction blast damage lost per hex; values below 1 are treated as 1
     * @param secondaryRadius hexes in which secondary effects apply
     * @param craterDepth     crater depth at Ground Zero
     * @param burstMode       ground burst or air burst
     *
     * @return the profile
     */
    public static NuclearBlastProfile custom(int impactDamage, int damageReduction, int secondaryRadius,
          int craterDepth, NuclearBurstMode burstMode) {
        int reduction = Math.max(1, damageReduction);
        return new NuclearBlastProfile(impactDamage, reduction, impactDamage / reduction, secondaryRadius,
              craterDepth, burstMode);
    }

    /**
     * @return {@code true} if this detonation is an air burst
     */
    public boolean isAirBurst() {
        return burstMode.isAirBurst();
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
     * The blast area on the Low-Altitude Map: the blast radius divided by 17, rounded up (IO:AE p.166). An Alamo air
     * burst reaches 58 / 17 = 3.41, so 4 Low-Altitude Map hexes.
     *
     * @return the blast radius in Low-Altitude Map hexes
     */
    public int lowAltitudeRadius() {
        return (int) Math.ceil(blastRadius / (double) GROUND_HEXES_PER_LOW_ALTITUDE_HEX);
    }

    /**
     * The secondary effect radius on the Low-Altitude Map: the secondary radius divided by 17, rounded up, as for the
     * blast radius (HammerGS, 2026-11-06). An Alamo air burst's 114 hexes become 7 Low-Altitude Map hexes.
     *
     * @return the secondary effect radius in Low-Altitude Map hexes
     */
    public int lowAltitudeSecondaryRadius() {
        return (int) Math.ceil(secondaryRadius / (double) GROUND_HEXES_PER_LOW_ALTITUDE_HEX);
    }

    /**
     * The Damage Reduction used on the Low-Altitude Map: the weapon's air-burst value, whatever the detonation type
     * (IO:AE p.166). A weapon with no air-burst figures, such as the Davy Crockett, and a custom device use their own
     * value. An Alamo ground burst uses the Alamo's air-burst 18, not its ground 23.
     *
     * @return the Damage Reduction per Low-Altitude Map distance unit
     */
    public int lowAltitudeDamageReduction() {
        if (isAirBurst()) {
            return damageReduction;
        }
        for (NuclearWeaponType weaponType : NuclearWeaponType.values()) {
            if (groundBurst(weaponType).equals(this)) {
                BurstFigures airFigures = weaponType.getAirBurst();
                return (airFigures == null) ? damageReduction : airFigures.damageReduction();
            }
        }
        return damageReduction;
    }

    /**
     * Blast damage to an airborne unit on the Low-Altitude Map (IO:AE p.166): Impact Damage minus the distance times
     * 8.5 times the Damage Reduction, rounded up, with a unit in the detonation's own hex counted as 0.5 hexes away.
     * An Alamo air burst 3 hexes from a Stuka deals 1,000 - (3 x 8.5 x 18 = 459) = 541; in its own hex it deals
     * 1,000 - 77 = 923.
     *
     * @param distance Low-Altitude Map hexes from the detonation
     *
     * @return the blast damage, or 0 outside the Low-Altitude Map blast radius
     */
    public int lowAltitudeDamageAt(int distance) {
        if ((distance < 0) || (distance > lowAltitudeRadius())) {
            return 0;
        }
        double countedDistance = (distance == 0) ? LOW_ALTITUDE_MINIMUM_DISTANCE : distance;
        int reduction = (int) Math.ceil(countedDistance * LOW_ALTITUDE_DISTANCE_FACTOR
              * lowAltitudeDamageReduction());
        return Math.max(0, impactDamage - reduction);
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

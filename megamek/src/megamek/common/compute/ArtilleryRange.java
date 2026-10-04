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
package megamek.common.compute;

import megamek.common.board.Board;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.weapons.capitalWeapons.CapitalMissileWeapon;

/**
 * How far an artillery weapon reaches. Artillery is rated in map sheets on the artillery range table (CamOps p.78:
 * Arrow IV 8 or 9, Thumper 21, Sniper 18, Long Tom 30, BA Tube 2, and the cruise missiles), and those are exactly the
 * weapons that carry {@link WeaponType#F_ARTILLERY}. Two things change that rating in play: the planet's gravity
 * (TO:AR p.155) and a crew with the Oblique Artilleryman ability (CamOps p.78, 5th printing).
 *
 * <p>These are stateless calculations over rules data, kept out of the to-hit and dialog classes that ask for them.
 * Nothing here is stored in game state, so no serialization handling is required.</p>
 */
public final class ArtilleryRange {

    /**
     * Factor by which the Oblique Artilleryman ability multiplies an artillery weapon's range. The rule gives the
     * increase as ten percent of the weapon's range in metres, rounded up (CamOps p.78, 5th printing).
     */
    public static final double OBLIQUE_ARTILLERYMAN_MULTIPLIER = 1.1;

    private ArtilleryRange() {}

    /**
     * Calculates how far an indirect-fire artillery attack may reach. The result is in hexes rather than map sheets
     * because the Oblique Artilleryman extension is a fraction of a map sheet.
     *
     * <p>Gravity is applied first, in whole map sheets as the artillery range table is written, and the ability's ten
     * percent is taken afterwards, on the range the weapon actually has under those conditions.</p>
     *
     * @param game       the current game, for the planetary gravity
     * @param attacker   the attacking unit, whose crew may have the Oblique Artilleryman ability
     * @param weaponType the type of the weapon being fired
     * @param weapon     the weapon mount being fired, for the capital missile range bracket
     *
     * @return the maximum range of the attack, in hexes
     */
    public static int maximumIndirectRangeInHexes(Game game, Entity attacker, WeaponType weaponType,
          WeaponMounted weapon) {
        int ratedRangeInMapSheets = ratedRangeInMapSheets(weaponType, weapon);
        int gravityAdjustedMapSheets = (int) (Math.floor(
              (double) (ratedRangeInMapSheets * Board.DEFAULT_BOARD_HEIGHT) /
                    game.getPlanetaryConditions().getGravity()) / (float) Board.DEFAULT_BOARD_HEIGHT);
        return extendedRangeInHexes(gravityAdjustedMapSheets, isExtendedByObliqueArtilleryman(attacker, weaponType));
    }

    /**
     * @param rangeInMapSheets       a range in map sheets
     * @param hasObliqueArtilleryman whether the Oblique Artilleryman ability applies to this attack
     *
     * @return that range in hexes, ten percent longer and rounded up when the ability applies
     */
    public static int extendedRangeInHexes(int rangeInMapSheets, boolean hasObliqueArtilleryman) {
        int rangeInHexes = rangeInMapSheets * Board.DEFAULT_BOARD_HEIGHT;
        return hasObliqueArtilleryman ? (int) Math.ceil(rangeInHexes * OBLIQUE_ARTILLERYMAN_MULTIPLIER) : rangeInHexes;
    }

    /**
     * The extended range as a number of map sheets, for display alongside a weapon's rated range. Ten percent of a map
     * sheet is a fraction of one, so this is deliberately not rounded: an Arrow IV rated at 8 reaches 8.8.
     *
     * @param ratedRangeInMapSheets the weapon's rated range, in map sheets
     *
     * @return the range the Oblique Artilleryman ability extends it to, in map sheets
     */
    public static double extendedRangeInMapSheets(int ratedRangeInMapSheets) {
        return ratedRangeInMapSheets * OBLIQUE_ARTILLERYMAN_MULTIPLIER;
    }

    /**
     * The ability extends artillery pieces only. A capital missile fired at a ground target resolves as an artillery
     * attack but is not an artillery piece, and an artillery cannon is a separate weapon class fired in the weapons
     * phase; neither appears on the artillery range table.
     *
     * @param attacker   the attacking unit
     * @param weaponType the type of the weapon being fired
     *
     * @return {@code true} if this attack's range is extended by the crew's Oblique Artilleryman ability
     */
    public static boolean isExtendedByObliqueArtilleryman(Entity attacker, WeaponType weaponType) {
        boolean isArtilleryPiece = weaponType.hasFlag(WeaponType.F_ARTILLERY);
        boolean crewHasAbility = attacker.hasAbility(OptionsConstants.GUNNERY_OBLIQUE_ARTILLERY);
        return isArtilleryPiece && crewHasAbility;
    }

    /**
     * @param weaponType the type of the weapon being fired
     * @param weapon     the weapon mount being fired, for the capital missile range bracket
     *
     * @return the weapon's rated range in map sheets, before gravity or any crew ability
     */
    private static int ratedRangeInMapSheets(WeaponType weaponType, WeaponMounted weapon) {
        if (!(weaponType instanceof CapitalMissileWeapon)) {
            return weaponType.getLongRange();
        }
        // Capital/subcapital missiles have a board range equal to their max space hex range
        return switch (weaponType.getMaxRange(weapon)) {
            case WeaponType.RANGE_EXT -> 50;
            case WeaponType.RANGE_LONG -> 40;
            case WeaponType.RANGE_MED -> 24;
            case WeaponType.RANGE_SHORT -> 12;
            default -> weaponType.getLongRange();
        };
    }
}

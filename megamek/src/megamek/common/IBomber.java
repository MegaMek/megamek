/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common;

import java.util.Arrays;
import java.util.List;

import megamek.common.BombType.BombTypeEnum;
import megamek.common.equipment.BombMounted;
import megamek.common.options.OptionsConstants;

/**
 * Common interface for all entities capable of carrying bombs and making bomb attacks, includig Aero, LandAirMek, and
 * VTOL.
 *
 * @author Neoancient
 */
public interface IBomber {

    String SPACE_BOMB_ATTACK = "SpaceBombAttack";
    String DIVE_BOMB_ATTACK = "DiveBombAttack";
    String ALT_BOMB_ATTACK = "AltBombAttack";

    /**
     * Set count of internal bombs used; this is used to reset, revert, or increase count of internal bombs a unit has
     * dropped during a turn.
     *
     * @param b
     */
    void setUsedInternalBombs(int b);

    /**
     * Increase count of internal bombs used this turn.
     *
     * @param b
     */
    void increaseUsedInternalBombs(int b);

    /**
     * @return the number of internal bombs used by this bomber during a turn, for IBB internal hit calculations.
     */
    int getUsedInternalBombs();

    /**
     * @return The total number of bomb points that the bomber can carry.
     */
    int getMaxBombPoints();

    /**
     * Fighters and VTOLs can carry any size bomb up to the maximum number of points per location (internal/external),
     * but LAMs are limited to the number of bays in a single location.
     *
     * @return The largest single bomb that can be carried internally.
     */
    default int getMaxIntBombSize() {
        return getMaxIntBombPoints();
    }

    /**
     * @return The largest single bomb that can be carried externally.
     */
    default int getMaxExtBombSize() {
        return getMaxExtBombPoints();
    }

    /**
     * @return The number of each internally-mounted bomb type that was selected prior to deployment
     */
    BombLoadout getIntBombChoices();

    /**
     * @return The number of each externally-mounted bomb type that was selected prior to deployment
     */
    BombLoadout getExtBombChoices();

    /**
     * Sets the bomb type selections prior to deployment.
     *
     * @param bc An array with the count of each bomb type as the value of the bomb type's index
     */
    void setIntBombChoices(BombLoadout bc);

    /**
     * Sets the bomb type selections for external mounts.
     *
     * @param bc An array with the count of each bomb type as the value of the bomb type's index
     */
    void setExtBombChoices(BombLoadout bc);

    /**
     * @return summed combination of internal and external choices
     */
    default BombLoadout getBombChoices() {
        BombLoadout combined = new BombLoadout(getIntBombChoices());
        getExtBombChoices().forEach((type, count) ->
              combined.merge(type, count, Integer::sum));
        return combined;
    }

    /**
     * Backwards compatibility bomb choice setter that only affects external stores.
     *
     * @param ebc
     */
    default void setBombChoices(BombLoadout ebc) {
        setExtBombChoices(ebc);
    }

    /**
     * Sets the count of each bomb to zero
     */
    void clearBombChoices();

    /**
     * @return The calculates movement factoring in the load of bombs currently on unit, t is current movement
     */
    int reduceMPByBombLoad(int t);

    /**
     * @param cost The cost of the bomb to be mounted
     *
     * @return A location with sufficient space to mount the bomb, or Entity.LOC_NONE if the unit does not have the
     *       space.
     */
    int availableBombLocation(int cost);

    /**
     * Used by VTOLs and LAMs in airmek mode to declare the target hex for a bomb attack during the movement phase.
     */
    default void setVTOLBombTarget(Targetable target) {
    }

    default Targetable getVTOLBombTarget() {
        return null;
    }

    default boolean isVTOLBombing() {
        return getVTOLBombTarget() != null;
    }

    // For convenience
    List<BombMounted> getBombs();

    /**
     * @return the number of total bomb points for this unit
     */
    default int getBombPoints() {
        return getBombPoints(false);
    }

    /**
     * @return the number of externally-mounted ordnance points (useful for MP calculations)
     */
    default int getExternalBombPoints() {
        return getBombPoints(true);
    }

    /**
     * @return total damage from remaining bombs
     */
    default int getInternalBombsDamageTotal() {
        int total = 0;
        for (Mounted<?> bomb : getBombs()) {
            if (bomb.isInternalBomb()) {
                total += bomb.getExplosionDamage();
            }
        }

        return total;
    }

    /**
     * @return The number of points taken up by all mounted bombs, or just external
     */
    default int getBombPoints(boolean externalOnly) {
        int points = 0;
        for (Mounted<?> bomb : getBombs()) {
            if (bomb.getUsableShotsLeft() > 0) {
                // Add points if A) not external only, and any kind of bomb, or B) external
                // only, and not internal bomb
                points += !(externalOnly && bomb.isInternalBomb())
                      ? ((BombType) bomb.getType()).getBombType().getCost()
                      : 0;
            }
        }
        return points;
    }

    /**
     * Iterate through the bomb choices that were configured prior to deployment and add the corresponding equipment.
     */
    default void applyBombs() {
        Game game = ((Entity) this).getGame();
        int gameTL = TechConstants.getSimpleLevel(game.getOptions().stringOption(OptionsConstants.ALLOWED_TECHLEVEL));

        // Apply the largest bombs first because we need to fit larger bombs into a
        // single location in LAMs.
        List<BombTypeEnum> sortedBySize = Arrays.stream(BombTypeEnum.values())
              .filter(type -> type != BombTypeEnum.NONE)
              .sorted((a, b) -> Integer.compare(b.getCost(), a.getCost()))
              .toList();

        // First, internal bombs
        BombLoadout intBombs = getIntBombChoices();
        for (BombTypeEnum bombType : sortedBySize) {
            int count = intBombs.getOrDefault(bombType, 0);
            for (int i = 0; i < count; i++) {
                if (!bombType.isAllowedByGameOptions(game.getOptions())) {
                    continue;
                }
                int loc = availableBombLocation(bombType.getCost());
                // some bombs need an associated weapon and if so
                // they need a weapon for each bomb
                if (null != bombType.getWeaponName()) {
                    applyBombWeapons(bombType, loc, true);
                } else {
                    applyBombEquipment(bombType, loc, true);
                }
            }
        }

        // Now external bombs
        BombLoadout extBombs = getExtBombChoices();
        for (BombTypeEnum bombType : sortedBySize) {
            int count = extBombs.getOrDefault(bombType, 0);
            for (int i = 0; i < count; i++) {
                if (!bombType.isAllowedByGameOptions(game.getOptions())) {
                    continue;
                }
                int loc = availableBombLocation(bombType.getCost());
                // some bombs need an associated weapon and if so
                // they need a weapon for each bomb
                if (null != bombType.getWeaponName()) {
                    applyBombWeapons(bombType, loc, false);
                } else {
                    applyBombEquipment(bombType, loc, false);
                }
            }
        }
        clearBombChoices();
    }

    /**
     * Helper to apply equipment-type bombs, either externally or internally.
     *
     * @param type     of bomb equipment.
     * @param loc      location where mounted.
     * @param internal mounted internally or not.
     */
    private void applyBombEquipment(BombTypeEnum bombType, int loc, boolean internal) {
        try {
            EquipmentType et = EquipmentType.get(bombType.getInternalName());
            Mounted<?> m = ((Entity) this).addEquipment(et, loc, false);
            m.setInternalBomb(internal);
        } catch (LocationFullException ignored) {

        }

    }

    /**
     * Helper to apply weapon-type bombs, either externally or internally.
     *
     * @param type     of bomb equipment.
     * @param loc      location where mounted.
     * @param internal mounted internally or not.
     */
    private void applyBombWeapons(BombTypeEnum bombType, int loc, boolean internal) {
        Mounted<?> m;
        try {
            EquipmentType et = EquipmentType.get(bombType.getWeaponName());
            m = ((Entity) this).addBomb(et, loc);
            m.setInternalBomb(internal);
            // Add bomb itself as single-shot ammo.
            if (bombType != BombTypeEnum.TAG) {
                Mounted<?> ammo = Mounted.createMounted((Entity) this,
                      EquipmentType.get(bombType.getInternalName()));
                ammo.setShotsLeft(1);
                ammo.setInternalBomb(internal);
                m.setLinked(ammo);
                ((Entity) this).addEquipment(ammo, loc, false);

            }
        } catch (LocationFullException ignored) {

        }
    }

    void clearBombs();

    /**
     * @return maximum number of bomb points this bomber can mount externally
     */
    int getMaxExtBombPoints();

    /**
     * @return maximum number of bomb points this bomber can mount internally
     */
    int getMaxIntBombPoints();
}

/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import megamek.common.options.OptionsConstants;

/**
 * Common interface for all entities capable of carrying bombs and making bomb attacks, includig Aero,
 * LandAirMech, and VTOL.
 *
 * @author Neoancient
 */
public interface IBomber {

    String SPACE_BOMB_ATTACK = "SpaceBombAttack";
    String DIVE_BOMB_ATTACK = "DiveBombAttack";
    String ALT_BOMB_ATTACK = "AltBombAttack";

    void setUsedInternalBombs(int b);
    void increaseUsedInternalBombs(int b);
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
     *
     * @return The largest single bomb that can be carried externally.
     */
    default int getMaxExtBombSize() {
        return getMaxExtBombPoints();
    }

    /**
     * @return The number of each bomb type that was selected prior to deployment
     */
    int[] getIntBombChoices();
    int[] getExtBombChoices();

    /**
     * Sets the bomb type selections prior to deployment.
     *
     * @param bc An array with the count of each bomb type as the value of the bomb type's index
     */
    void setIntBombChoices(int[] bc);
    void setExtBombChoices(int[] bc);

    /**
     * @return summed combination of internal and external choices
     */
    default int[] getBombChoices(){
        int[] intArr = getIntBombChoices();
        int[] extArr = getExtBombChoices();
        IntStream range = IntStream.range(0, Math.min(intArr.length, extArr.length));
        IntStream stream3 = range.map(i -> intArr[i] + extArr[i]);
        return stream3.toArray();
    }

    default void setBombChoices(int[] ebc) {
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
     * @return A location with sufficient space to mount the bomb, or Entity.LOC_NONE if the unit does not have the space.
     */
    int availableBombLocation(int cost);

    /**
     * Used by VTOLs and LAMs in airmech mode to declare the target hex for a bomb attack during the movement
     * phase.
     */
    default void setVTOLBombTarget(Targetable target) {}

    default Targetable getVTOLBombTarget() {
        return null;
    }

    default boolean isVTOLBombing() {
        return getVTOLBombTarget() != null;
    }

    // For convenience
    List<Mounted> getBombs();

    /**
     *
     * @return the number of total bomb points for this unit
     */
    default int getBombPoints() {
        return getBombPoints(false);
    }

    /**
     *
     * @return the number of externally-mounted ordnance points (useful for MP calculations)
     */
    default int getExternalBombPoints() {
        return getBombPoints(true);
    }

    /**
     *
     * @return total damage from remaining bombs
     */
    default int getInternalBombsDamageTotal() {
        int total = 0;
        for (Mounted bomb: getBombs()) {
            if (bomb.isInternalBomb()) {
                total += bomb.getExplosionDamage();
            }
        }

        // int total = getBombs().stream().filter(
        //         b -> b.isInternalBomb()
        // ).mapToInt(b -> b.getExplosionDamage()).sum();
        return total;
    }

    /**
     * @return The number of points taken up by all mounted bombs, or just external
     */
    default int getBombPoints(boolean externalOnly) {
        int points = 0;
        for (Mounted bomb : getBombs()) {
            if (bomb.getUsableShotsLeft() > 0) {
                // Add points if A) not external only, and any kind of bomb, or B) external only, and not internal bomb
                points += !(externalOnly && bomb.isInternalBomb()) ?
                        BombType.getBombCost(((BombType) bomb.getType()).getBombType()) : 0;
            }
        }
        return points;
    }


    /**
     * Iterate through the bomb choices that were configured prior to deployment and add the corresponding
     * equipment.
     */
    default void applyBombs() {
        Game game = ((Entity) this).getGame();
        int gameTL = TechConstants.getSimpleLevel(game.getOptions().stringOption("techlevel"));
        Integer[] iSorted = new Integer[BombType.B_NUM];
        // Apply the largest bombs first because we need to fit larger bombs into a single location
        // in LAMs.
        for (int i = 0; i < iSorted.length; i++) {
            iSorted[i] = i;
        }
        Integer[] eSorted = iSorted.clone();

        Arrays.sort(iSorted, (a, b) -> BombType.bombCosts[b] - BombType.bombCosts[a]);
        Arrays.sort(eSorted, (a, b) -> BombType.bombCosts[b] - BombType.bombCosts[a]);

        // First, internal bombs
        for (int type : iSorted) {
            for (int i = 0; i < getIntBombChoices()[type]; i++) {
                int loc = availableBombLocation(BombType.bombCosts[type]);
                if ((type == BombType.B_ALAMO)
                        && !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AT2_NUKES)) {
                    continue;
                }
                if ((type > BombType.B_TAG)
                        && (gameTL < TechConstants.T_SIMPLE_ADVANCED)) {
                    continue;
                }

                // some bombs need an associated weapon and if so
                // they need a weapon for each bomb
                if (null != BombType.getBombWeaponName(type)) {
                    applyBombWeapons(type, loc, true);
                } else {
                    applyBombEquipment(type, loc, true);
                }
            }
        }

        // Now external bombs
        for (int type : eSorted) {
            for (int i = 0; i < getExtBombChoices()[type]; i++) {
                int loc = availableBombLocation(BombType.bombCosts[type]);
                if ((type == BombType.B_ALAMO)
                        && !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AT2_NUKES)) {
                    continue;
                }
                if ((type > BombType.B_TAG)
                        && (gameTL < TechConstants.T_SIMPLE_ADVANCED)) {
                    continue;
                }

                // some bombs need an associated weapon and if so
                // they need a weapon for each bomb
                if (null != BombType.getBombWeaponName(type)) {
                    applyBombWeapons(type, loc, false);
                } else {
                    applyBombEquipment(type, loc, false);
                }
            }
        }
        clearBombChoices();
    }

    private void applyBombEquipment(int type, int loc, boolean internal){
        try {
            EquipmentType et = EquipmentType.get(BombType.getBombInternalName(type));
            Mounted m = ((Entity) this).addEquipment(et, loc, false);
            m.setInternalBomb(internal);
        } catch (LocationFullException ignored) {

        }

    }

    private void applyBombWeapons(int type, int loc, boolean internal){
        Mounted m;
        try {
            EquipmentType et = EquipmentType.get(BombType.getBombWeaponName(type));
            m = ((Entity) this).addBomb(et, loc);
            m.setInternalBomb(internal);
            // Add bomb itself as single-shot ammo.
            if (type != BombType.B_TAG) {
                Mounted ammo = new Mounted((Entity) this,
                        EquipmentType.get(BombType.getBombInternalName(type)));
                ammo.setShotsLeft(1);
                ammo.setInternalBomb(internal);
                m.setLinked(ammo);
                ((Entity) this).addEquipment(ammo, loc, false);

            }
        } catch (LocationFullException ignored) {

        }
    }

    void clearBombs();

    int getMaxExtBombPoints();

    int getMaxIntBombPoints();
}

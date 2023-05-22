/*
 * Copyright (c) 2010 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

public class BattleArmorHandlesTank extends BattleArmorHandles {
    private static final long serialVersionUID = 1031947858009941399L;

    /**
     * The set of front locations that load troopers externally.
     */
    private static final int[] EXTERIOR_LOCATIONS = { Tank.LOC_RIGHT, Tank.LOC_LEFT, Tank.LOC_REAR };

    /**
     * Get the exterior locations that a loaded squad covers.
     * <p>
     * Sub-classes are encouraged to override this method.
     *
     * @param isRear a <code>boolean</code> value stating if the given location is rear facing; if
     *              <code>false</code>, the location is front facing.
     * @return an array of <code>int</code> listing the exterior locations.
     */
    @Override
    protected int[] getExteriorLocs(boolean isRear) {
        return BattleArmorHandlesTank.EXTERIOR_LOCATIONS;
    }

    /**
     * Determine if transported units prevent a weapon in the given location
     * from firing.
     * <p>
     *
     * @param loc - the <code>int</code> location attempting to fire.
     * @param isRear - a <code>boolean</code> value stating if the given
     *            location is rear facing; if <code>false</code>, the
     *            location is front facing.
     * @return <code>true</code> if a transported unit is in the way,
     *         <code>false</code> if the weapon can fire.
     */
    @Override
    public final boolean isWeaponBlockedAt(int loc, boolean isRear) {
        // Assume that the weapon is not blocked.
        boolean result = false;

        // The weapon can only be blocked if we are carrying troopers.
        Entity trooper = game.getEntity(troopers);
        if (null != trooper) {

            // Is the relevant trooper alive?
            int tloc = BattleArmor.LOC_SQUAD;
            int tloc2 = BattleArmor.LOC_SQUAD;
            switch (loc) {
                case Tank.LOC_REAR:
                    tloc = BattleArmor.LOC_TROOPER_5;
                    tloc2 = BattleArmor.LOC_TROOPER_6;
                    break;
                case Tank.LOC_LEFT:
                    tloc = BattleArmor.LOC_TROOPER_3;
                    tloc2 = BattleArmor.LOC_TROOPER_4;
                    break;
                case Tank.LOC_RIGHT:
                    tloc = BattleArmor.LOC_TROOPER_1;
                    tloc2 = BattleArmor.LOC_TROOPER_2;
                    break;
            }
            if (((trooper.locations() > tloc) && (trooper.getInternal(tloc) > 0))
                    || ((trooper.locations() > tloc2) && (trooper.getInternal(tloc2) > 0))) {
                result = true;
            }
        }

        // Return our result.
        return result;
    }
}

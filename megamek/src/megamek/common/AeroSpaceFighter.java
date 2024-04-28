/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

/**
 * AeroSpaceFighter subclass of Aero that encapsulates Fighter functionality
 */
public class AeroSpaceFighter extends Aero {
    public AeroSpaceFighter() {
        super();
    }

    @Override
    public int getUnitType() {
        return UnitType.AEROSPACEFIGHTER;
    }

    @Override
    public void autoSetMaxBombPoints() {
        // Aerospace fighters can carry both external and internal ordnances, if configured and quirked
        // appropriately
        maxExtBombPoints = (int) Math.round(getWeight() / 5);
        // Can't check quirk here, as they don't exist in unit files yet.
        maxIntBombPoints = getTransportBays().stream().mapToInt(
                tb -> (tb instanceof CargoBay) ? (int) Math.floor(tb.getUnused()) : 0
        ).sum();
    }

    @Override
    public int reduceMPByBombLoad(int t) {
        return Math.max(0, t - (int) Math.ceil(getExternalBombPoints() / 5.0));
    }

    @Override
    public boolean isSpheroid() {
        return false;
    }

    @Override
    public boolean isBomber() {
        return true;
    }

    @Override
    public boolean isFighter() {
        return true;
    }

    @Override
    public boolean isAerospaceFighter() {
        return true;
    }

    /**
     * Method to enable mass location damaging, mainly for Fighter Squadrons
     * @param loc that every fighter in the squadron needs to damage, for MekHQ tracking
     */
    public void damageLocation(int loc) {
        weaponList.stream().filter(x -> x.getLocation() == loc).forEach(
            (weapon)-> {
                //Damage the weapon
                weapon.setHit(true);
                //Damage the critical slot
                for (int i = 0; i < getNumberOfCriticals(loc); i++) {
                    CriticalSlot slot1 = getCritical(loc, i);
                    if ((slot1 == null) ||
                            (slot1.getType() == CriticalSlot.TYPE_SYSTEM)) {
                        continue;
                    }
                    Mounted mounted = slot1.getMount();
                    if (mounted.equals(weapon)) {
                        hitAllCriticals(loc, i);
                        break;
                    }
                }
            }
        );
    }

    @Override
    public long getEntityType() {
        return super.getEntityType() | Entity.ETYPE_AEROSPACEFIGHTER;
    }

    @Override
    public int getGenericBattleValue() {
        return (int) Math.round(Math.exp(3.729 + 0.898*Math.log(getWeight())));
    }
}
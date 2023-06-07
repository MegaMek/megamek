/*
 * Copyright (c) 2022-2023 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.battlevalue;

import megamek.common.*;
import megamek.common.weapons.bayweapons.BayWeapon;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;

public abstract class LargeAeroBVCalculator extends AeroBVCalculator {

    protected static final int BVLOC_NOSE = 0;
    protected static final int BVLOC_LEFT = 1;
    protected static final int BVLOC_LEFT_AFT = 2;
    protected static final int BVLOC_AFT = 3;
    protected static final int BVLOC_RIGHT_AFT = 4;
    protected static final int BVLOC_RIGHT = 5;

    protected int nominalNoseLocation;
    protected int nominalLeftLocation;
    protected int nominalRightLocation;
    protected final Map<Mounted, Integer> collectedWeapons = new HashMap<>();

    LargeAeroBVCalculator(Entity entity) {
        super(entity);
    }

    @Override
    protected void reset() {
        super.reset();
        collectedWeapons.clear();
    }

    @Override
    protected boolean usesWeaponHeat() {
        return false;
    }

    @Override
    protected void assembleAmmo() {
        for (Mounted ammo : entity.getAmmo()) {
            AmmoType ammoType = (AmmoType) ammo.getType();

            // don't count depleted ammo, AMS and oneshot ammo
            if (ammoCounts(ammo)) {
                // Ammo may be loaded in multi-ton increments
                int ratio = Math.max(1, ammo.getUsableShotsLeft() / ammoType.getShots());

                // Ammo must be handled per arc, therefore include the arc in the key
                String key = bvLocation(ammo) + ":" + ammoType.getAmmoType() + ":" + ammoType.getRackSize();
                if (!keys.contains(key)) {
                    keys.add(key);
                    names.put(key, equipmentDescriptor(ammo));
                }
                if (!ammoMap.containsKey(key)) {
                    ammoMap.put(key, getAmmoBV(ammo) * ratio);
                } else {
                    ammoMap.put(key, getAmmoBV(ammo) * ratio + ammoMap.get(key));
                }
            }
        }

        for (Mounted weapon : entity.getTotalWeaponList()) {
            WeaponType wtype = (WeaponType) weapon.getType();

            if (weapon.isDestroyed() || wtype.hasFlag(WeaponType.F_AMS)
                    || wtype.hasFlag(WeaponType.F_B_POD) || wtype.hasFlag(WeaponType.F_M_POD)
                    || wtype instanceof BayWeapon || weapon.isWeaponGroup()) {
                continue;
            }

            // add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !((wtype.getAmmoType() == AmmoType.T_PLASMA)
                    || (wtype.getAmmoType() == AmmoType.T_VEHICLE_FLAMER)
                    || (wtype.getAmmoType() == AmmoType.T_HEAVY_FLAMER)
                    || (wtype.getAmmoType() == AmmoType.T_CHEMICAL_LASER)))
                    || wtype.hasFlag(WeaponType.F_ONESHOT)
                    || wtype.hasFlag(WeaponType.F_INFANTRY)
                    || (wtype.getAmmoType() == AmmoType.T_NA))) {
                String key = bvLocation(weapon) + ":" + wtype.getAmmoType() + ":" + wtype.getRackSize();
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(entity));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(entity) + weaponsForExcessiveAmmo.get(key));
                }
            }
        }
    }

    @Override
    protected void processExplosiveEquipment() { }

    @Override
    protected int heatEfficiency() {
        bvReport.addLine("Heat Efficiency:", " = " + entity.getHeatCapacity(), "");
        return entity.getHeatCapacity();
    }

    protected abstract Predicate<Mounted> leftWeaponFilter();

    protected abstract Predicate<Mounted> leftAftWeaponFilter();

    protected abstract Predicate<Mounted> rightWeaponFilter();

    protected abstract Predicate<Mounted> rightAftWeaponFilter();

    @Override
    protected double arcFactor(Mounted equipment) {
        return arcFactor(bvLocation(equipment));
    }

    protected double arcFactor(int bvLocation) {
        if (!frontAndRearDecided) {
            return 1;
        } else if (bvLocation == nominalNoseLocation) {
            return 1;
        } else if (bvLocation == nominalLeftLocation) {
            return heatEfficiencyExceeded ? 0.5 : 1;
        } else if (bvLocation == nominalRightLocation) {
            return heatEfficiencyExceeded ? 0.25 : 0.5;
        } else {
            return 0.25;
        }
    }

    @Override
    protected boolean isNominalRear(Mounted weapon) {
        return false;
    }

    @Override
    protected boolean isNominalRearArc(Mounted weapon) {
        return super.isNominalRearArc(weapon);
    }

    protected boolean isNominalArc(int nominalBvLocation, Mounted equipment) {
        return bvLocation(equipment) == nominalBvLocation;
    }

    protected abstract int bvLocation(Mounted equipment);

    @Override
    protected void determineFront() {
        Predicate<Mounted> frontFilter = frontWeaponFilter();
        Predicate<Mounted> rearFilter = rearWeaponFilter();
        Predicate<Mounted> leftFilter = leftWeaponFilter();
        Predicate<Mounted> rightFilter = rightWeaponFilter();
        Predicate<Mounted> leftAftFilter = leftAftWeaponFilter();
        Predicate<Mounted> rightAftFilter = rightAftWeaponFilter();
        Map<Integer, Double> bvPerArc = new HashMap<>();
        double weaponsBVFront = processWeaponSection(false, frontFilter, false);
        double weaponsBVRear = processWeaponSection(false, rearFilter, false);
        double weaponsBVLeft = processWeaponSection(false, leftFilter, false);
        double weaponsBVRight = processWeaponSection(false, rightFilter, false);
        double weaponsBVAftLeft = processWeaponSection(false, leftAftFilter, false);
        double weaponsBVAftRight = processWeaponSection(false, rightAftFilter, false);
        bvPerArc.put(BVLOC_NOSE, weaponsBVFront);
        bvPerArc.put(BVLOC_LEFT, weaponsBVLeft);
        bvPerArc.put(BVLOC_RIGHT, weaponsBVRight);
        bvPerArc.put(BVLOC_AFT, weaponsBVRear);
        bvPerArc.put(BVLOC_LEFT_AFT, weaponsBVAftLeft);
        bvPerArc.put(BVLOC_RIGHT_AFT, weaponsBVAftRight);
        final double maxBV = bvPerArc.values().stream().mapToDouble(bv -> bv).max().orElse(0);
        for (Map.Entry<Integer, Double> entry : bvPerArc.entrySet()) {
            if (entry.getValue() == maxBV) {
                nominalNoseLocation = entry.getKey();
                break;
            }
        }
        int firstAdjacentArc = getAdjacentLocationCCW(nominalNoseLocation);
        int secondAdjacentArc = getAdjacentLocationCW(nominalNoseLocation);
        if (bvPerArc.get(firstAdjacentArc) > bvPerArc.get(secondAdjacentArc)) {
            nominalLeftLocation = firstAdjacentArc;
            nominalRightLocation = secondAdjacentArc;
        } else {
            nominalLeftLocation = secondAdjacentArc;
            nominalRightLocation = firstAdjacentArc;
        }

        bvReport.addLine("Nominal Nose Location",
                arcName(nominalNoseLocation) + ", Weapon BV: " + formatForReport(bvPerArc.get(nominalNoseLocation)),
                "");
        bvReport.addLine("Nominal Left Location",
                arcName(nominalLeftLocation) + ", Weapon BV: " + formatForReport(bvPerArc.get(nominalLeftLocation)),
                "");
        bvReport.addLine("Nominal Right Location",
                arcName(nominalRightLocation) + ", Weapon BV: " + formatForReport(bvPerArc.get(nominalRightLocation)),
                "");
        frontAndRearDecided = true;
    }

    /** @return True when the two weapons are equal for conversion purposes (same type, location and links). */
    protected boolean canBeSummed(Mounted weapon1, Mounted weapon2) {
        return weapon1.getType().equals(weapon2.getType())
                && weapon1.getLocation() == weapon2.getLocation()
                && weapon1.isRearMounted() == weapon2.isRearMounted()
                && ((weapon1.getLinkedBy() == null && weapon2.getLinkedBy() == null)
                || (weapon1.getLinkedBy() != null
                && weapon1.getLinkedBy().getType().equals(weapon2.getLinkedBy().getType())));
    }

    @Override
    protected void processWeapons() {
        for (Mounted weapon : entity.getTotalWeaponList()) {
            if (countAsOffensiveWeapon(weapon)) {
                Mounted key = collectedWeapons.keySet().stream()
                        .filter(wp -> canBeSummed(weapon, wp)).findFirst().orElse(weapon);
                collectedWeapons.merge(key, 1, Integer::sum);
            }
        }
        int heatEfficiency = heatEfficiency();

        double totalHeatSum = processArc(nominalNoseLocation);
        bvReport.addLine("Total Heat:", formatForReport(totalHeatSum), "");
        heatEfficiencyExceeded = totalHeatSum > heatEfficiency;
        if (heatEfficiencyExceeded) {
            bvReport.addLine("Heat Efficiency Exceeded", "", "");
        }

        totalHeatSum += processArc(nominalLeftLocation);
        bvReport.addLine("Total Heat:", formatForReport(totalHeatSum), "");
        heatEfficiencyExceeded = totalHeatSum > heatEfficiency;
        if (heatEfficiencyExceeded) {
            bvReport.addLine("Heat Efficiency Exceeded", "", "");
        }

        processArc(nominalRightLocation);
        processArc(getOppositeLocation(nominalNoseLocation));
        processArc(getOppositeLocation(nominalLeftLocation));
        processArc(getOppositeLocation(nominalRightLocation));
        bvReport.addEmptyLine();
    }

    protected double processArc(final int bvNominalLocation) {
        double arcHeat = 0;
        bvReport.addEmptyLine();
        bvReport.addLine(arcName(bvNominalLocation) + ":", "", "");
        boolean arcEmpty = true;
        for (Map.Entry<Mounted, Integer> weaponEntry : collectedWeapons.entrySet()) {
            Mounted weapon = weaponEntry.getKey();
            if (isNominalArc(bvNominalLocation, weapon) && countAsOffensiveWeapon(weapon)) {
                arcHeat += weaponHeat(weapon) * weaponEntry.getValue();
                processWeapon(weapon, true, true, weaponEntry.getValue());
                arcEmpty = false;
            }
        }
        if (arcEmpty) {
            bvReport.addLine("- None.", "", "");
        }

        processAmmo(bvNominalLocation);
        return arcHeat;
    }

    protected void processAmmo(int bvLocation) {
        bvReport.startTentativeSection();
        boolean hasAmmo = false;
        for (String key : keys) {
            if (!key.startsWith(bvLocation + ":")) {
                continue;
            }
            if (!weaponsForExcessiveAmmo.containsKey(key)) {
                // Coolant Pods have no matching weapon
                if (key.equals(Integer.valueOf(AmmoType.T_COOLANT_POD).toString() + "1")) {
                    offensiveValue += ammoMap.get(key);
                }
                continue;
            }
            if (!ammoMap.containsKey(key)) {
                continue;
            }
            String calculation = "+ ";
            double arcFactor = arcFactor(bvLocation);

            if (ammoMap.get(key) > weaponsForExcessiveAmmo.get(key)) {
                offensiveValue += weaponsForExcessiveAmmo.get(key) * arcFactor;
                calculation += formatForReport(weaponsForExcessiveAmmo.get(key)) + " (Excessive)";
            } else {
                offensiveValue += ammoMap.get(key) * arcFactor;
                calculation += formatForReport(ammoMap.get(key));
            }
            if (arcFactor != 1) {
                calculation += " x " + formatForReport(arcFactor) + " (Arc)";
            }
            bvReport.addLine("- " + names.get(key), calculation, "= " + formatForReport(offensiveValue));
            hasAmmo = true;
        }
        if (hasAmmo) {
            bvReport.endTentativeSection();
        } else {
            bvReport.discardTentativeSection();
        }
    }

    @Override
    protected void processAmmo() { }

    protected abstract String arcName(int bvLocation);

    /** @return The adjacent firing arc location, counter-clockwise */
    protected int getAdjacentLocationCCW(int bvLocation) {
        return (bvLocation + 1) % 6;
    }

    /** @return The adjacent firing arc location, clockwise */
    protected int getAdjacentLocationCW(int bvLocation) {
        return (bvLocation + 5) % 6;
    }

    /** @return The opposite firing arc location, counter-clockwise */
    protected int getOppositeLocation(int bvLocation) {
        return (bvLocation + 3) % 6;
    }
}
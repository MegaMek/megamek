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
package megamek.common.alphaStrike.conversion;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.*;
import megamek.common.alphaStrike.*;
import megamek.common.annotations.Nullable;
import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.bayweapons.ArtilleryBayWeapon;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.missiles.MissileWeapon;
import megamek.common.weapons.other.CLFussilade;

import java.util.*;

import static megamek.client.ui.swing.calculationReport.CalculationReport.fmtDouble;
import static megamek.common.alphaStrike.AlphaStrikeElement.*;
import static megamek.common.alphaStrike.BattleForceSPA.*;

public final class ASDamageConverter {

    // AP weapon mounts have a set damage value.
    static final double AP_MOUNT_DAMAGE = 0.05;

    private final static int[] TROOP_FACTOR = {
            0, 0, 1, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12,
            13, 14, 15, 16, 16, 17, 17, 17, 18, 18
    };

    static void convertDamage(ASConverter.ConversionData conversionData) {
        Entity entity = conversionData.entity;
        AlphaStrikeElement element = conversionData.element;
        CalculationReport report = conversionData.conversionReport;
        report.addEmptyLine();
        report.addSubHeader("Damage:");

        double[] baseDamage = new double[element.getRangeBands()];
        boolean hasTC = entity.hasTargComp();
        int[] ranges;
        double pointDefense = 0;
        int bombRacks = 0;
        double baseIFDamage = 0;
        //TODO: multiple turrets
        var turretsArcs = new HashMap<String, ASArcSummary>();
        if (!element.usesArcs()) {
            for (int loc = 0; loc < element.weaponLocations.length; loc++) {
                if (element.locationNames[loc].startsWith("TUR")) {
                    turretsArcs.put(element.locationNames[loc], ASArcSummary.createTurretSummary(element));
                }
            }
        }
        //Track weapons we've already calculated ammunition for
        HashMap<String,Boolean> ammoForWeapon = new HashMap<>();

        ArrayList<Mounted> weaponsList = entity.getWeaponList();

        for (int pos = 0; pos < weaponsList.size(); pos++) {
            Arrays.fill(baseDamage, 0);
            double damageModifier = 1;
            Mounted mount = weaponsList.get(pos);
            if (mount == null) {
                continue;
            }

            WeaponType weapon = (WeaponType) mount.getType();

            //TODO: Remove this: The only thing that counts is the range bracket
            ranges = weapon.isCapital() ? CAPITAL_RANGES : STANDARD_RANGES;

            if (weapon.getAmmoType() == AmmoType.T_INARC) {
                element.getSpecialAbilities().addSPA(INARC, 1);
                findArcTurret(element, entity, mount, turretsArcs).forEach(a -> a.getSpecials().addSPA(INARC, 1));
                continue;
            } else if (weapon.getAmmoType() == AmmoType.T_NARC) {
                if (weapon.hasFlag(WeaponType.F_BA_WEAPON)) {
                    element.getSpecialAbilities().addSPA(CNARC, 1);
                } else {
                    element.getSpecialAbilities().addSPA(SNARC, 1);
                    findArcTurret(element, entity, mount, turretsArcs).forEach(a -> a.getSpecials().addSPA(SNARC, 1));
                }
                continue;
            }

            if (weapon.getAtClass() == WeaponType.CLASS_SCREEN) {
                element.getSpecialAbilities().addSPA(SCR, 1);
                continue;
            }

            //TODO: really not Aero? Where is AMS pointdefense handled else?
            if (weapon.hasFlag(WeaponType.F_AMS)) {
                if (locationMultiplier(entity, 0, mount) > 0) {
                    if (entity instanceof Aero) {
                        pointDefense += 0.3;
                    } else {
                        element.addSPA(AMS);
                    }
                }
                for (ASArcSummary arcTurret : findArcTurret(element, entity, mount, turretsArcs)) {
                    if (entity instanceof Aero) {
                        arcTurret.getSpecials().addSPA(PNT, 0.3);
                    } else {
                        arcTurret.getSpecials().addSPA(AMS);
                    }
                }
                continue;
            }

            if (weapon.hasFlag(WeaponType.F_TAG)) {
                if (weapon.hasFlag(WeaponType.F_C3MBS)) {
                    element.getSpecialAbilities().addSPA(C3BSM, 1);
                    element.getSpecialAbilities().addSPA(MHQ, 6);
                } else if (weapon.hasFlag(WeaponType.F_C3M)) {
                    element.getSpecialAbilities().addSPA(C3M, 1);
                    element.getSpecialAbilities().addSPA(MHQ, 5);
                }
                if (weapon.getShortRange() < 5) {
                    element.addSPA(LTAG);
                    findArcTurret(element, entity, mount, turretsArcs).forEach(a -> a.getSpecials().addSPA(LTAG));
                } else {
                    element.addSPA(TAG);
                    findArcTurret(element, entity, mount, turretsArcs).forEach(a -> a.getSpecials().addSPA(TAG));
                }
                continue;
            }

            if (weapon.hasFlag(WeaponType.F_FLAMER) || weapon.hasFlag(WeaponType.F_PLASMA)) {
                // For CI, add a placeholder heat value to be replaced later by the S damage value, ASC p.124
                element.getSpecialAbilities().addSPA(HT, ASDamageVector.createNormRndDmg(0));
            }

            if (weapon.getDamage() == WeaponType.DAMAGE_ARTILLERY) {
                if (!((entity instanceof Aero) && isArtilleryCannon(weapon))) {
                    element.getSpecialAbilities().addSPA(getArtilleryType(weapon), 1);
                    findArcTurret(element, entity, mount, turretsArcs).forEach(a -> a.getSpecials().addSPA(getArtilleryType(weapon), 1));
                    continue;
                }
            }

            if (weapon instanceof ArtilleryBayWeapon) {
                for (int index : mount.getBayWeapons()) {
                    Mounted m = entity.getEquipment(index);
                    if (m.getType() instanceof WeaponType) {
                        element.getSpecialAbilities().addSPA(getArtilleryType((WeaponType)m.getType()), 1);
                        findArcTurret(element, entity, mount, turretsArcs)
                                .forEach(a -> a.getSpecials().addSPA(getArtilleryType((WeaponType)m.getType()), 1));
                    }
                }
            }

            if (weapon.getAmmoType() == AmmoType.T_BA_MICRO_BOMB) {
                bombRacks++;
                continue;
            }

            if (weapon.getAmmoType() == AmmoType.T_TASER) {
                if (entity instanceof BattleArmor) {
                    element.getSpecialAbilities().addSPA(BTAS, 1);
                } else {
                    element.getSpecialAbilities().addSPA(MTAS, 1);
                }
            }

            if (weapon.hasFlag(WeaponType.F_TSEMP)) {
                BattleForceSPA spa = weapon.hasFlag(WeaponType.F_ONESHOT) ? TSEMPO : TSEMP;
                element.getSpecialAbilities().addSPA(spa, 1);
                findArcTurret(element, entity, mount, turretsArcs).forEach(a -> a.getSpecials().addSPA(spa, 1));
                continue;
            }

            if (weapon instanceof InfantryAttack) {
                element.addSPA(AM);
                continue;
            }

            // Check ammo weapons first since they had a hidden modifier
            if ((weapon.getAmmoType() != AmmoType.T_NA)
                    && !weapon.hasFlag(WeaponType.F_ONESHOT)
                    && (!(entity instanceof BattleArmor) || weapon instanceof MissileWeapon)) {
                if (!ammoForWeapon.containsKey(weapon.getName())) {
                    int weaponsForAmmo = 1;
                    for (int nextPos = 0; nextPos < weaponsList.size(); nextPos++) {
                        if (nextPos == pos) {
                            continue;
                        }

                        Mounted nextWeapon = weaponsList.get(nextPos);

                        if (nextWeapon == null) {
                            continue;
                        }

                        if (nextWeapon.getType().equals(weapon)) {
                            weaponsForAmmo++;
                        }

                    }
                    int ammoCount = 0;
                    // Check if they have enough ammo for all the guns to last at least 10 rounds
                    // RACs and UACs require 60 / 20 shots per weapon
                    int divisor = 1;
                    for (Mounted ammo : entity.getAmmo()) {
                        AmmoType at = (AmmoType) ammo.getType();
                        if ((at.getAmmoType() == weapon.getAmmoType()) && (at.getRackSize() == weapon.getRackSize())) {
                            ammoCount += ammo.getUsableShotsLeft();
                            if (at.getAmmoType() == AmmoType.T_AC_ROTARY) {
                                divisor = 6;
                            } else if (at.getAmmoType() == AmmoType.T_AC_ULTRA
                                    || at.getAmmoType() == AmmoType.T_AC_ULTRA_THB) {
                                divisor = 2;
                            }
                        }
                    }
                    ammoForWeapon.put(weapon.getName(), (ammoCount / weaponsForAmmo / divisor) >= 10);
                }
                if (!ammoForWeapon.get(weapon.getName())) {
                    damageModifier *= 0.75;
                }
            }

            // Fussilade launchers have a low ammo multiplier already pre-applied to their value
            if (weapon.hasFlag(WeaponType.F_ONESHOT) && !(weapon instanceof CLFussilade)) {
                damageModifier *= .1;
            }

            if (weapon instanceof BayWeapon) {
                for (int index : mount.getBayWeapons()) {
                    Mounted m = entity.getEquipment(index);
                    if (m.getType() instanceof WeaponType) {
                        if (m.getType().hasFlag(WeaponType.F_AMS)) {
                            if (ASLocationMapper.damageLocationMultiplier(entity, 0, m) != 0) {
                                element.getFrontArc().getSpecials().addSPA(PNT, 0.3d);
                            }
                            if (ASLocationMapper.damageLocationMultiplier(entity, 1, m) != 0) {
                                element.getLeftArc().getSpecials().addSPA(PNT, 0.3d);
                            }
                            if (ASLocationMapper.damageLocationMultiplier(entity, 2, m) != 0) {
                                element.getRightArc().getSpecials().addSPA(PNT, 0.3d);
                            }
                            if (ASLocationMapper.damageLocationMultiplier(entity, 3, m) != 0) {
                                element.getRearArc().getSpecials().addSPA(PNT, 0.3d);
                            }
                            continue;
                        }

                        if (((WeaponType) m.getType()).isAlphaStrikePointDefense()) {
                            double sDmg = ((WeaponType) m.getType()).getBattleForceDamage(ranges[0], m.getLinkedBy());
                            if (ASLocationMapper.damageLocationMultiplier(entity, 0, m) != 0) {
                                element.getFrontArc().getSpecials().addSPA(PNT, sDmg);
                            }
                            if (ASLocationMapper.damageLocationMultiplier(entity, 1, m) != 0) {
                                element.getLeftArc().getSpecials().addSPA(PNT, sDmg);
                            }
                            if (ASLocationMapper.damageLocationMultiplier(entity, 2, m) != 0) {
                                element.getRightArc().getSpecials().addSPA(PNT, sDmg);
                            }
                            if (ASLocationMapper.damageLocationMultiplier(entity, 3, m) != 0) {
                                element.getRearArc().getSpecials().addSPA(PNT, sDmg);
                            }
                        }

                        // TELE missile special ability for Large Aero
                        if ((((WeaponType) m.getType()).getAtClass() == WeaponType.CLASS_TELE_MISSILE)) {
                            addSPAToArcs(TELE, m, entity, element);
                        }

                        // MSL missile special ability for Large Aero
                        if (!element.usesCapitalWeapons() && element.isAerospace()
                                && ((WeaponType) m.getType()).getBattleForceClass() == WeaponType.BFCLASS_CAPITAL_MISSILE) {
                            addSPAToArcs(MSL, m, entity, element);
                        }

                        // SCAP missile special ability for Large Aero
                        if (!element.usesCapitalWeapons() && element.isAerospace()
                                && ((WeaponType) m.getType()).getBattleForceClass() == WeaponType.BFCLASS_SUBCAPITAL) {
                            addSPAToArcs(SCAP, m, entity, element);
                        }

                        for (int r = 0; r < element.getRangeBands(); r++) {
                            baseDamage[r] += ((WeaponType)m.getType()).getBattleForceDamage(ranges[r], m.getLinkedBy());
                            element.heat[r] += ((WeaponType)m.getType()).getBattleForceHeatDamage(ranges[r]);
                        }
                    }
                }
            } else {
                for (int r = 0; r < element.getRangeBands(); r++) {
                    if (entity instanceof BattleArmor) {
                        baseDamage[r] = baseDamage[r] = getBattleArmorDamage(weapon, ranges[r], ((BattleArmor)entity),
                                mount.isAPMMounted());
                        baseIFDamage = baseDamage[RANGE_BAND_LONG];
                    } else {
                        baseDamage[r] = weapon.getBattleForceDamage(ranges[r], mount.getLinkedBy());
                        // Disregard any Artemis bonus for IF:
                        baseIFDamage = baseDamage[RANGE_BAND_LONG];
                        if (MountedHelper.isAnyArtemis(mount.getLinkedBy()) && isSRMorLRMSpecial(weapon)) {
                            baseIFDamage = weapon.getBattleForceDamage(ranges[r], null);
                        }
                    }
                    for (int loc = 0; loc < element.weaponLocations.length; loc++) {
                        Integer ht = element.weaponLocations[loc].heatDamage.get(r);
                        if (ht == null) {
                            ht = 0;
                        }
                        ht += (int)(locationMultiplier(entity, loc, mount) * weapon.getBattleForceHeatDamage(r));
                        element.weaponLocations[loc].heatDamage.set(r, ht);
                    }
                }
            }

            // Targetting Computer
            if (hasTC && weapon.hasFlag(WeaponType.F_DIRECT_FIRE)
                    && (weapon.getAmmoType() != AmmoType.T_AC_LBX)
                    && (weapon.getAmmoType() != AmmoType.T_AC_LBX_THB)) {
                damageModifier *= 1.10;
            }

            // Actuator Enhancement System
            if (entity.hasWorkingMisc(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM, -1, mount.getLocation())
                    && ((mount.getLocation() == Mech.LOC_LARM) || (mount.getLocation() == Mech.LOC_RARM))) {
                damageModifier *= 1.05;
            }

            for (int loc = 0; loc < element.weaponLocations.length; loc++) {
                double locMultiplier = locationMultiplier(entity, loc, mount);
                if (locMultiplier == 0) {
                    continue;
                }
                for (int r = 0; r < element.getRangeBands(); r++) {
                    double dam = baseDamage[r] * damageModifier * locMultiplier;
                    report.addLine(element.locationNames[loc] + ": " + mount.getName() + " " + "SMLE".charAt(r) + ": ",
                            " Mul: " + damageModifier, "", dam);
                    if (!weapon.isCapital() && weapon.getBattleForceClass() != WeaponType.BFCLASS_TORP) {
                        // Standard Damage
                        element.weaponLocations[loc].addDamage(r, dam);
                    }
                    // Special Damage (SRM/LRM blocked by Artemis)
                    if (!(MountedHelper.isAnyArtemis(mount.getLinkedBy()) && isSRMorLRMSpecial(weapon))) {
                        if (weapon.getBattleForceClass() == WeaponType.BFCLASS_MML) {
                            if (r == RANGE_BAND_SHORT) {
                                element.weaponLocations[loc].addDamage(WeaponType.BFCLASS_SRM, r, dam);
                            } else if (r == RANGE_BAND_MEDIUM) {
                                element.weaponLocations[loc].addDamage(WeaponType.BFCLASS_SRM, r, dam / 2.0);
                                element.weaponLocations[loc].addDamage(WeaponType.BFCLASS_LRM, r, dam / 2.0);
                            } else {
                                element.weaponLocations[loc].addDamage(WeaponType.BFCLASS_LRM, r, dam);
                            }
                        } else {
                            element.weaponLocations[loc].addDamage(weapon.getBattleForceClass(), r, dam);
                        }
                    }
                    if ((r == RANGE_BAND_LONG) && !(entity instanceof Aero) && weapon.hasAlphaStrikeIndirectFire()) {
                        element.weaponLocations[loc].addIF(baseIFDamage * damageModifier * locMultiplier);
                    }
                }
            }
            if (entity instanceof Aero && weapon.isAlphaStrikePointDefense()) {
                pointDefense += baseDamage[RANGE_BAND_SHORT] * damageModifier * locationMultiplier(entity, 0, mount);
            }
        }

        if (entity.getEntityType() == Entity.ETYPE_INFANTRY) {
            Infantry infantry = (Infantry) entity;
            int baseRange = 0;
            if ((infantry.getSecondaryWeapon() != null) && (infantry.getSecondaryN() >= 2)) {
                baseRange = infantry.getSecondaryWeapon().getInfantryRange();
            } else if (infantry.getPrimaryWeapon() != null) {
                baseRange = infantry.getPrimaryWeapon().getInfantryRange();
            }
            int range = baseRange * 3;
            for (int r = 0; r < STANDARD_RANGES.length; r++) {
                if (range >= STANDARD_RANGES[r]) {
                    element.weaponLocations[0].addDamage(r, getConvInfantryStandardDamage(infantry));
                } else {
                    break;
                }
            }
        }

        if (entity instanceof BattleArmor) {
            int vibroClaws = entity.countWorkingMisc(MiscType.F_VIBROCLAW);
            if (vibroClaws > 0) {
                element.weaponLocations[0].addDamage(0, vibroClaws);
                element.weaponLocations[0].addDamage(WeaponType.BFCLASS_STANDARD, 0, vibroClaws);
            }
            if (bombRacks > 0) {
                element.getSpecialAbilities().addSPA(BOMB, (bombRacks * ((BattleArmor)entity).getShootingStrength()) / 5);
            }
        }

        if (entity instanceof Aero && ASConverter.roundUp(pointDefense) > 0) {
            element.getSpecialAbilities().addSPA(PNT, ASConverter.roundUp(pointDefense));
        }

        adjustForHeat(conversionData);

        // Standard HT
        if (!element.isType(ASUnitType.CI)) {
            //TODO this is wrong ASC p.124
            int htS = resultingHTValue(element.weaponLocations[0].heatDamage.get(0));
            int htM = resultingHTValue(element.weaponLocations[0].heatDamage.get(1));
            int htL = resultingHTValue(element.weaponLocations[0].heatDamage.get(2));
            if (htS + htM + htL > 0) {
                element.getSpecialAbilities().addSPA(HT, ASDamageVector.createNormRndDmg(htS, htM, htL));
            }
        }

        // IF (uses an ASDamage as value)
        if (element.weaponLocations[0].getIF() > 0) {
            element.getSpecialAbilities().addSPA(IF, ASDamageVector.createNormRndDmg(element.weaponLocations[0].getIF()).S);
        }

        // LRM ... IATM specials
        for (int i = WeaponType.BFCLASS_LRM; i <= WeaponType.BFCLASS_REL; i++) {
            // Aero do not get LRM/SRM/AC/IATM, FLK only in arcs
            if ((entity instanceof Aero) && (i != WeaponType.BFCLASS_FLAK)) {
                continue;
            }
            if (element.usesArcs() && (i == WeaponType.BFCLASS_FLAK)) {
                List<Double> dmg = element.weaponLocations[0].specialDamage.get(i);
                if (dmg != null) {
                    element.getFrontArc().getSpecials().addSPA(FLK, ASDamageVector.createNormRndDmg(dmg, 4));
                }
                dmg = element.weaponLocations[1].specialDamage.get(i);
                if (dmg != null) {
                    element.getLeftArc().getSpecials().addSPA(FLK, ASDamageVector.createNormRndDmg(dmg, 4));
                }
                dmg = element.weaponLocations[2].specialDamage.get(i);
                if (dmg != null) {
                    element.getRightArc().getSpecials().addSPA(FLK, ASDamageVector.createNormRndDmg(dmg, 4));
                }
                dmg = element.weaponLocations[3].specialDamage.get(i);
                if (dmg != null) {
                    element.getRearArc().getSpecials().addSPA(FLK, ASDamageVector.createNormRndDmg(dmg, 4));
                }
                continue;
            }
            BattleForceSPA spa = BattleForceSPA.getSPAForDmgClass(i);
            List<Double> dmg = element.weaponLocations[0].specialDamage.get(i);
            if ((dmg != null) && qualifiesForSpecial(dmg, spa)) {
                if (spa == SRM) {
                    element.getSpecialAbilities().addSPA(SRM, ASDamageVector.createNormRndDmgNoMin(dmg, 2));
                } else if ((spa == LRM) || (spa == AC) || (spa == IATM)) {
                    element.getSpecialAbilities().addSPA(spa, ASDamageVector.createNormRndDmgNoMin(dmg, element.getRangeBands()));
                } else if ((spa == FLK) || (spa == TOR)) {
                    element.getSpecialAbilities().addSPA(spa, ASDamageVector.createNormRndDmg(dmg, element.getRangeBands()));
                } else if (spa == REL) {
                    element.addSPA(spa);
                }
            }
        }

        // REL
        if (element.weaponLocations[0].hasDamageClass(WeaponType.BFCLASS_REL)) {
            element.addSPA(REL);
        }

        if (element.usesArcs()) {
            // Large Aero (using Arcs)
            setArcDamage(element, 0, element.getFrontArc());
            setArcDamage(element, 1, element.getLeftArc());
            setArcDamage(element, 2, element.getRightArc());
            setArcDamage(element, 3, element.getRearArc());
        } else {
            // Standard damage
            element.setStandardDamage(ASDamageVector.createUpRndDmg(
                    element.weaponLocations[0].standardDamage, element.getRangeBands()));
        }

        // REAR damage
        int rearLoc = getRearLocation(element);
        if (rearLoc != -1 && element.weaponLocations[rearLoc].hasDamage()) {
            // Double check; Moray Heavy Attack Sub has only LTR in the rear leading to REAR-/-/- otherwise
            ASDamageVector rearDmg = ASDamageVector.createNormRndDmg(
                    element.weaponLocations[rearLoc].standardDamage, element.getRangeBands());
            if (rearDmg.hasDamage()) {
                element.getSpecialAbilities().addSPA(REAR, rearDmg);
            }

        }

        // Turrets have SRM, LRM, AC, FLK, HT, TSEMP, TOR, AMS, TAG, ARTx, xNARC, IATM, REL
        //TODO: 2 Turrets?
        for (int loc = 0; loc < element.weaponLocations.length; loc++) {
            if (turretsArcs.containsKey(element.locationNames[loc])) {
                ASArcSummary arcTurret = turretsArcs.get(element.locationNames[loc]);
                arcTurret.setStdDamage(ASDamageVector.createUpRndDmgMinus(element.weaponLocations[loc].standardDamage,
                        element.getRangeBands()));
                for (int i = WeaponType.BFCLASS_LRM; i <= WeaponType.BFCLASS_REL; i++) {
                    BattleForceSPA spa = BattleForceSPA.getSPAForDmgClass(i);
                    List<Double> dmg = element.weaponLocations[loc].specialDamage.get(i);
                    if ((dmg != null) && qualifiesForSpecial(dmg, spa)) {
                        if (spa == SRM) {
                            arcTurret.getSpecials().addSPA(spa, ASDamageVector.createNormRndDmgNoMin(dmg, 2));
                        } else if ((spa == LRM) || (spa == TOR) || (spa == AC) || (spa == IATM)) {
                            arcTurret.getSpecials().addSPA(spa, ASDamageVector.createNormRndDmgNoMin(dmg, element.getRangeBands()));
                        } else if (spa == FLK) {
                            arcTurret.getSpecials().addSPA(spa, ASDamageVector.createNormRndDmg(dmg, element.getRangeBands()));
                        } else if (spa == REL) {
                            arcTurret.getSpecials().addSPA(spa);
                        }
                    }
                }
                if (element.weaponLocations[loc].getIF() > 0) {
                    arcTurret.getSpecials().addSPA(IF, ASDamageVector.createNormRndDmg(element.weaponLocations[loc].getIF()));
                }
                int htS = resultingHTValue(element.weaponLocations[loc].heatDamage.get(0));
                int htM = resultingHTValue(element.weaponLocations[loc].heatDamage.get(1));
                int htL = resultingHTValue(element.weaponLocations[loc].heatDamage.get(2));
                if (htS + htM + htL > 0) {
                    arcTurret.getSpecials().addSPA(HT, ASDamageVector.createNormRndDmg(htS, htM, htL));
                }
                if (!arcTurret.isEmpty()) {
                    //TODO: If this is an arc??
                    element.getSpecialAbilities().addSPA(TUR, arcTurret);
                }
            }
        }

    }

    private static List<ASArcSummary> findArcTurret(AlphaStrikeElement element, Entity entity,
                                                    Mounted mount, Map<String, ASArcSummary> arcsTurrets) {
        var result = new ArrayList<ASArcSummary>();
        for (int loc = 0; loc < element.weaponLocations.length; loc++) {
            if (element.locationNames[loc].equals("TUR") && (locationMultiplier(entity, loc, mount) != 0)) {
                result.add(arcsTurrets.get(element.locationNames[loc]));
            }
        }
        return result;
    }

    private static double locationMultiplier(Entity en, int loc, Mounted mount) {
        if (ASLocationMapper.locationName(en, loc).startsWith("TUR") && (en instanceof Mech) && mount.isMechTurretMounted()) {
            return 1;
        } else if (ASLocationMapper.locationName(en, loc).startsWith("TUR") && (en instanceof Tank)
                && (mount.isPintleTurretMounted() || mount.isSponsonTurretMounted())) {
            return 1;
        } else {
            return ASLocationMapper.damageLocationMultiplier(en, loc, mount);
        }
    }

    private static boolean isArtilleryCannon(WeaponType weapon) {
        return (weapon.getAmmoType() == AmmoType.T_LONG_TOM_CANNON)
                || (weapon.getAmmoType() == AmmoType.T_SNIPER_CANNON)
                || (weapon.getAmmoType() == AmmoType.T_THUMPER_CANNON);
    }

    private static @Nullable BattleForceSPA getArtilleryType(WeaponType weapon) {
        switch (weapon.getAmmoType()) {
            case AmmoType.T_ARROW_IV:
                if (weapon.getInternalName().charAt(0) == 'C') {
                    return BattleForceSPA.ARTAC;
                } else {
                    return BattleForceSPA.ARTAIS;
                }
            case AmmoType.T_LONG_TOM:
                return BattleForceSPA.ARTLT;
            case AmmoType.T_SNIPER:
                return BattleForceSPA.ARTS;
            case AmmoType.T_THUMPER:
                return BattleForceSPA.ARTT;
            case AmmoType.T_LONG_TOM_CANNON:
                return BattleForceSPA.ARTLTC;
            case AmmoType.T_SNIPER_CANNON:
                return BattleForceSPA.ARTSC;
            case AmmoType.T_THUMPER_CANNON:
                return BattleForceSPA.ARTTC;
            case AmmoType.T_CRUISE_MISSILE:
                switch(weapon.getRackSize()) {
                    case 50:
                        return BattleForceSPA.ARTCM5;
                    case 70:
                        return BattleForceSPA.ARTCM7;
                    case 90:
                        return BattleForceSPA.ARTCM9;
                    case 120:
                        return BattleForceSPA.ARTCM12;
                }
            case AmmoType.T_BA_TUBE:
                return BattleForceSPA.ARTBA;
        }
        return null;
    }

    private static double getBattleArmorDamage(WeaponType weapon, int range, BattleArmor ba, boolean apmMount) {
        double dam = 0;
        if (apmMount) {
            if (range == 0) {
                dam = AP_MOUNT_DAMAGE;
            }
        } else {
            dam = weapon.getBattleForceDamage(range);
        }
        return dam * (TROOP_FACTOR[Math.min(ba.getShootingStrength(), 30)] + 0.5);
    }

    /** Returns true when the given weapon contributes to the LRM/SRM specials. */
    private static boolean isSRMorLRMSpecial(WeaponType weapon) {
        return weapon.getBattleForceClass() == WeaponType.BFCLASS_SRM
                || weapon.getBattleForceClass() == WeaponType.BFCLASS_LRM
                || weapon.getBattleForceClass() == WeaponType.BFCLASS_MML;
    }

    private static double getConvInfantryStandardDamage(Infantry inf) {
        return inf.getDamagePerTrooper() * TROOP_FACTOR[Math.min(inf.getShootingStrength(), 30)] / 10.0;
    }

    private static int resultingHTValue(int heatSum) {
        if (heatSum > 10) {
            return 2;
        } else if (heatSum > 4) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Adjusts all damage values for overheat, if applicable (i.e., if the unit tracks heat,
     * if its heat output is sufficiently over its heat dissipation and for L/E values, if
     * the prerequisites for OVL are fulfilled.
     * Also assigns OVL where applicable.
     */
    private static void adjustForHeat(ASConverter.ConversionData conversionData) {
        AlphaStrikeElement element = conversionData.element;
        Entity entity = conversionData.entity;
        CalculationReport report = conversionData.conversionReport;
        if (!entity.tracksHeat() && !element.usesArcs()) {
            return;
        }
        int totalFrontHeat = getHeatGeneration(entity, element,false, false);
        int heatCapacity = getHeatCapacity(entity, element);
        report.addEmptyLine();
        report.addSubHeader("Heat Adjustment:");
        report.addLine("Heat Capacity: ", "", heatCapacity);
        report.addLine("Total Heat M: ", "", totalFrontHeat);
        if (totalFrontHeat - 4 <= heatCapacity) {
            report.addLine("", "No adjustment needed", "");
            return;
        }

        // Determine OV from the medium range damage
        //TODO: this should not be necessary:
        while (element.weaponLocations[0].standardDamage.size() < 4) {
            element.weaponLocations[0].standardDamage.add(0.0);
        }
        double nonRounded = element.weaponLocations[0].standardDamage.get(RANGE_BAND_MEDIUM);
        element.setOverheat(Math.min(heatDelta(nonRounded, heatCapacity, totalFrontHeat), 4));

        report.addLine("Total Raw Dmg M: ", "", fmtDouble(nonRounded));
        report.addLine("Total Adjusted Dmg M: ",
                fmtDouble(nonRounded) + " x " + heatCapacity + " / (" + totalFrontHeat + " - 4)",
                fmtDouble(nonRounded * heatCapacity / (totalFrontHeat - 4)));
        report.addLine("Resulting Overheat", "OV " + element.getOverheat());

        // Determine OVL from long range damage and heat
        if (element.getOverheat() > 0 && element.usesOV()) {
            double heatLong = getHeatGeneration(entity, element, false, true);
            report.addLine("Total Heat L:", "", heatLong);
            if (heatLong - 4 > heatCapacity) {
                double nonRoundedL = element.weaponLocations[0].standardDamage.get(RANGE_BAND_LONG);
                report.addLine("Total Damage L before Adjustment: ", "", nonRoundedL);
                if (heatDelta(nonRoundedL, heatCapacity, heatLong) >= 1) {
                    report.addLine("Resulting SPA", "OVL", "");
                    element.addSPA(OVL);
                }
            }
        }

        // Adjust all weapon damages (E for units with OVL and arced units, M otherwise)
        int maxAdjustmentRange = 1 + ((element.hasSUA(OVL) || element.usesArcs()) ? RANGE_BAND_EXTREME : RANGE_BAND_MEDIUM);
        double frontadjustment = (double) heatCapacity / (totalFrontHeat - 4);
        double rearHeat = getHeatGeneration(entity, element,true, false);
        double rearAdjustment = rearHeat - 4 > heatCapacity ? heatCapacity / (rearHeat - 4) : 1;
        // No special treatment for the rear arc of Large Aero units
        if (element.usesArcs()) {
            rearAdjustment = frontadjustment;
        }
        for (int loc = 0; loc < element.weaponLocations.length; loc++) {
            ASConverter.WeaponLocation wloc = element.weaponLocations[loc];
            double adjustment = element.locationNames[loc].equals("REAR") ? rearAdjustment : frontadjustment;
            for (int i = 0; i < Math.min(maxAdjustmentRange, wloc.standardDamage.size()); i++) {
                wloc.standardDamage.set(i, heatAdjust(wloc.standardDamage.get(i), adjustment));
            }
            for (List<Double> damage : wloc.specialDamage.values()) {
                for (int i = 0; i < Math.min(maxAdjustmentRange, damage.size()); i++) {
                    damage.set(i, heatAdjust(damage.get(i), adjustment));
                }
            }
            // IF is long-range fire; only adjust when the unit can overheat even in long range
            if (element.hasSUA(OVL)) {
                //TODO: really adjust this with round up to tenth?
                wloc.indirect = heatAdjust(wloc.indirect, adjustment);
            }
        }
    }

    /**
     * Returns the delta between the unadjusted rounded and heat-adjusted rounded damage value,
     * according to ASC - Converting Heat Errata v1.2.
     * Use only for determining if a unit has OV or OVL.
     */
    private static int heatDelta(double damage, int heatCapacity, double heat) {
        int roundedUp = ASConverter.roundUp(roundUpToTenth(damage));
        int roundedUpAdjustedL = ASConverter.roundUp(roundUpToTenth(damage * heatCapacity / (heat - 4)));
        return roundedUp - roundedUpAdjustedL;
    }

    private static double heatAdjust(double value, double adjustment) {
        return roundUpToTenth(value * adjustment);
    }

    /**
     * Returns true when the heat-adjusted and tenth-rounded damage values in the List allow the given spa.
     * Only use for the damage specials LRM, SRM, TOR, IATM, AC, FLK
     */
    private static boolean qualifiesForSpecial(List<Double> dmg, BattleForceSPA spa) {
        if (((spa == FLK) || (spa == TOR)) && dmg.stream().mapToDouble(Double::doubleValue).sum() > 0) {
            return true;
        } else {
            return (dmg.size() > 1) && (dmg.get(1) >= 1);
        }
    }

    /**
     * Returns the given number, rounded up to the nearest tenth. ASC Converting Heat Errata v1.2:
     * A value of 0.401 -> 0.5; i.e. any fraction will make it round up, not just the 100th.
     */
    public static double roundUpToTenth(double number) {
        double intermediate = 10 * number; // 0.401 -> 4.01 or 4.00999999 or 4.010000001
        double result = (int) intermediate; // 4
        if (intermediate - (int) intermediate > 0.000001) { // -> 0.01 or 0.009999 or 0.01000001
            result += 1; // 5
        }
        return result / 10; // 0.5 or 0.4999999999

    }

    private static int getRearLocation(AlphaStrikeElement element) {
        for (int loc = 0; loc < element.weaponLocations.length; loc++) {
            if (element.locationNames[loc].equals("REAR")) {
                return loc;
            }
        }
        return -1;
    }

    /**
     * Returns the total generated heat (weapons and movement) for a Mech or Aero for
     * the purpose of finding OV / OVL values.
     * If onlyRear is true, only rear-facing weapons are included, otherwise only front-
     * facing weapons are included!
     * If onlyLongRange is true, only weapons with an L damage value are included.
     */
    private static int getHeatGeneration(Entity entity, AlphaStrikeElement element, boolean onlyRear, boolean onlyLongRange) {
        if (entity instanceof Mech) {
            return getMechHeatGeneration((Mech)entity, element, onlyRear, onlyLongRange);
        } else {
            return getAeroHeatGeneration((Aero)entity, onlyRear, onlyLongRange);
        }
    }

    /**
     * Returns the total generated heat (weapons and movement) for a Mech for
     * the purpose of finding OV / OVL values.
     * If onlyRear is true, rear-facing weapons are included, otherwise only front-
     * facing weapons are included!
     * If onlyLongRange is true, only weapons with an L damage value are included.
     */
    private static int getMechHeatGeneration(Mech entity, AlphaStrikeElement element, boolean onlyRear, boolean onlyLongRange) {
        int totalHeat = 0;

        if (entity.getJumpMP() > 0) {
            totalHeat += getJumpHeat(entity, element);
        } else if (!entity.isIndustrial() && entity.hasEngine()) {
            totalHeat += entity.getEngine().getRunHeat(entity);
        }

//        System.out.println("Total Heat Movement: " + totalHeat);

        for (Mounted mount : entity.getWeaponList()) {
            WeaponType weapon = (WeaponType) mount.getType();
            if (weapon.hasFlag(WeaponType.F_ONESHOT)
                    || (onlyRear && !mount.isRearMounted())
                    || (!onlyRear && mount.isRearMounted())
                    || (onlyLongRange && weapon.getBattleForceDamage(LONG_RANGE) == 0)) {
                continue;
            }
            if (weapon.getAmmoType() == AmmoType.T_AC_ROTARY) {
                totalHeat += weapon.getHeat() * 6;
//                //System.out.println(weapon.getName() + " Heat: " + weapon.getHeat() * 6);
            } else if (weapon.getAmmoType() == AmmoType.T_AC_ULTRA
                    || weapon.getAmmoType() == AmmoType.T_AC_ULTRA_THB) {
                totalHeat += weapon.getHeat() * 2;
//                //System.out.println(weapon.getName() + " Heat: " + weapon.getHeat() * 2);
            } else {
                totalHeat += weapon.getHeat();
//                System.out.println(weapon.getName() + " Heat: " + weapon.getHeat());
            }
        }

//        System.out.println("Total Heat After wps: " + totalHeat);

        if (entity.hasWorkingMisc(MiscType.F_STEALTH, -1)
                || entity.hasWorkingMisc(MiscType.F_VOIDSIG, -1)
                || entity.hasWorkingMisc(MiscType.F_NULLSIG, -1)) {
            totalHeat += 10;
        }

        if (entity.hasWorkingMisc(MiscType.F_CHAMELEON_SHIELD, -1)) {
            totalHeat += 6;
        }

        return totalHeat;
    }

    private static int getJumpHeat(Entity entity, AlphaStrikeElement element) {
        if ((entity.getJumpType() == Mech.JUMP_IMPROVED)
                && (entity.getEngine().getEngineType() == Engine.XXL_ENGINE)) {
            return Math.max(3, element.getJumpMove() / 2);
        } else if (entity.getJumpType() == Mech.JUMP_IMPROVED) {
            return Math.max(3, ASConverter.roundUp(0.25 * element.getJumpMove()));
        } else if (entity.getEngine().getEngineType() == Engine.XXL_ENGINE) {
            return Math.max(6, element.getJumpMove());
        } else {
            return Math.max(3, element.getJumpMove() / 2);
        }
    }

    /**
     * Returns the total generated heat (weapons and movement) for an Aero for
     * the purpose of finding OV / OVL values.
     * If onlyRear is true, rear-facing weapons are included, otherwise only front-
     * facing weapons are included!
     * If onlyLongRange is true, only weapons with an L damage value are included.
     */
    private static int getAeroHeatGeneration(Aero entity, boolean onlyRear, boolean onlyLongRange) {
        int totalHeat = 0;

        for (Mounted mount : entity.getWeaponList()) {
            WeaponType weapon = (WeaponType) mount.getType();
            if (weapon instanceof BayWeapon) {
                for (int index : mount.getBayWeapons()) {
                    totalHeat += entity.getEquipment(index).getType().getHeat();
                }
            } else {
                if (weapon.hasFlag(WeaponType.F_ONESHOT)
                        || (onlyRear && !mount.isRearMounted() && mount.getLocation() != Aero.LOC_AFT)
                        || (!onlyRear && (mount.isRearMounted() || mount.getLocation() == Aero.LOC_AFT))
                        || (onlyLongRange && weapon.getLongRange() < LONG_RANGE)) {
                    continue;
                }
                if (weapon.getAmmoType() == AmmoType.T_AC_ROTARY) {
                    totalHeat += weapon.getHeat() * 6;
//                    //System.out.println(weapon.getName() + " Heat: " + weapon.getHeat() * 6);
                } else if (weapon.getAmmoType() == AmmoType.T_AC_ULTRA
                        || weapon.getAmmoType() == AmmoType.T_AC_ULTRA_THB) {
                    totalHeat += weapon.getHeat() * 2;
                    //System.out.println(weapon.getName() + " Heat: " + weapon.getHeat() * 2);
                } else {
                    totalHeat += weapon.getHeat();
                    //System.out.println(weapon.getName() + " Heat: " + weapon.getHeat());
                }
            }
        }

        if (entity.hasWorkingMisc(MiscType.F_STEALTH, -1)) {
            totalHeat += 10;
        }

        return totalHeat;
    }

    /**
     * Returns the heat dissipation for Mechs and ASFs, according to ASC - Converting Heat
     * Errata v1.2.
     */
    private static int getHeatCapacity(Entity entity, AlphaStrikeElement element) {
        int result = 0;
        if (entity instanceof Mech) {
            result = ((Mech)entity).getHeatCapacity(false, false);
        } else if (entity.isFighter()) {
            result = entity.getHeatCapacity(false);
        } else if (element.usesArcs()) {
            result = entity.getHeatCapacity(false);
        }
        result += entity.getEquipment().stream().filter(Mounted::isCoolantPod).count();
        result += entity.hasWorkingMisc(MiscType.F_PARTIAL_WING) ? 3 : 0;
        result += element.hasSUA(RHS) ? 1 : 0;
        result += element.hasSUA(ECS) ? 1 : 0;
        return result;
    }

    private static void addSPAToArcs(BattleForceSPA spa, Mounted mount, Entity entity, AlphaStrikeElement element) {
        if (ASLocationMapper.damageLocationMultiplier(entity, 0, mount) != 0) {
            element.getFrontArc().getSpecials().addSPA(spa);
        }
        if (ASLocationMapper.damageLocationMultiplier(entity, 1, mount) != 0) {
            element.getLeftArc().getSpecials().addSPA(spa);
        }
        if (ASLocationMapper.damageLocationMultiplier(entity, 2, mount) != 0) {
            element.getRightArc().getSpecials().addSPA(spa);
        }
        if (ASLocationMapper.damageLocationMultiplier(entity, 3, mount) != 0) {
            element.getRearArc().getSpecials().addSPA(spa);
        }
    }

    private static void setArcDamage(AlphaStrikeElement element, int loc, ASArcSummary arc) {
        ASConverter.WeaponLocation wpLoc = element.weaponLocations[loc];
        Map<Integer, List<Double>> specialDmg = wpLoc.specialDamage;
        arc.setStdDamage(ASDamageVector.createUpRndDmg(wpLoc.standardDamage, 4));
        if (wpLoc.hasDamageClass(WeaponType.BFCLASS_SUBCAPITAL)) {
            arc.setSCAPDamage(ASDamageVector.createUpRndDmgMinus(specialDmg.get(WeaponType.BFCLASS_SUBCAPITAL), 4));
        }
        if (wpLoc.hasDamageClass(WeaponType.BFCLASS_CAPITAL)) {
            arc.setCAPDamage(ASDamageVector.createUpRndDmgMinus(specialDmg.get(WeaponType.BFCLASS_CAPITAL), 4));
        }
        if (wpLoc.hasDamageClass(WeaponType.BFCLASS_CAPITAL_MISSILE)) {
            arc.setMSLDamage(ASDamageVector.createUpRndDmgMinus(specialDmg.get(WeaponType.BFCLASS_CAPITAL_MISSILE), 4));
        }
    }

    // Make non-instantiable
    private ASDamageConverter() { }
}

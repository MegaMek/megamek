/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of Megaentity.
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
 * along with Megaentity. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.battlevalue;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;
import static megamek.common.EquipmentType.T_STRUCTURE_COMPOSITE;
import static megamek.common.EquipmentType.T_STRUCTURE_INDUSTRIAL;
import static megamek.common.EquipmentType.T_STRUCTURE_REINFORCED;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.weapons.autocannons.HVACWeapon;
import megamek.common.weapons.gaussrifles.GaussWeapon;
import megamek.common.weapons.lasers.CLImprovedHeavyLaserLarge;
import megamek.common.weapons.lasers.CLImprovedHeavyLaserMedium;
import megamek.common.weapons.lasers.CLImprovedHeavyLaserSmall;
import megamek.common.weapons.lasers.ISRISCHyperLaser;
import megamek.common.weapons.other.ISMekTaser;
import megamek.common.weapons.other.TSEMPWeapon;
import megamek.common.weapons.ppc.PPCWeapon;

public class MekBVCalculator extends HeatTrackingBVCalculator {

    private final Mek mek;

    MekBVCalculator(Entity entity) {
        super(entity);
        mek = (Mek) entity;
    }

    @Override
    protected double addTorsoMountedCockpit() {
        if (mek.getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED) {
            return entity.getArmor(Mek.LOC_CT) + entity.getArmor(Mek.LOC_CT, true);
        } else {
            return 0;
        }
    }

    @Override
    protected void processStructure() {
        String calculation = "+ " + entity.getTotalInternal() + " x 1.5";
        List<String> modifiers = new ArrayList<>();
        double typeMultiplier = 1.0;
        int structureType = entity.getStructureType();
        if ((structureType == T_STRUCTURE_INDUSTRIAL) || (structureType == T_STRUCTURE_COMPOSITE)) {
            typeMultiplier = 0.5;
            modifiers.add((structureType == T_STRUCTURE_COMPOSITE) ? "Comp." : "Ind.");
        } else if (structureType == T_STRUCTURE_REINFORCED) {
            typeMultiplier = 2.0;
            modifiers.add("Reinf.");
        }
        if (hasBlueShield) {
            typeMultiplier += 0.2;
            modifiers.add("Blue Shield");
        }
        if (typeMultiplier != 1) {
            calculation += " x " + formatForReport(typeMultiplier);
        }

        double engineMultiplier = entity.hasEngine() ? entity.getEngine().getBVMultiplier() : 1.0;
        if (engineMultiplier != 1) {
            calculation += " x " + formatForReport(engineMultiplier);
            modifiers.add(entity.getEngine().getShortEngineName());
        }
        if (!modifiers.isEmpty()) {
            calculation += " (" + String.join(", ", modifiers) + ")";
        }
        defensiveValue += entity.getTotalInternal() * 1.5 * typeMultiplier * engineMultiplier;
        bvReport.addLine("Internal Structure:", calculation, "= " + formatForReport(defensiveValue));

        defensiveValue += entity.getWeight() * mek.getGyroMultiplier();
        bvReport.addLine("Gyro:",
                "+ " + formatForReport(entity.getWeight()) + " x " + formatForReport(mek.getGyroMultiplier()),
                "= " + formatForReport(defensiveValue));
    }

    @Override
    protected void processDefensiveEquipment() {
        super.processDefensiveEquipment();
        double armoredCompBV = mek.getArmoredComponentBV();
        if (armoredCompBV > 0) {
            defensiveValue += armoredCompBV;
            bvReport.addLine("Armored Components:",
                    "+ " + formatForReport(armoredCompBV), "= " + formatForReport(defensiveValue));
        }
    }

    @Override
    protected void processExplosiveEquipment() {
        boolean hasExplosiveEquipment = false;
        bvReport.startTentativeSection();
        bvReport.addLine("Explosive Equipment:", "", "");
        if (hasBlueShield) {
            int unProtectedCrits = 0;
            for (int loc = Mek.LOC_CT; loc <= Mek.LOC_LLEG; loc++) {
                if (entity.hasCASEII(loc)) {
                    continue;
                }
                if (entity.isClan()) {
                    // Clan meks only count ammo in ct, legs or head (per BMRr).
                    // Also count ammo in side torsos if mek has xxl engine
                    // (extrapolated from rule intent - not covered in rules)
                    if (((loc != Mek.LOC_CT) && (loc != Mek.LOC_RLEG) && (loc != Mek.LOC_LLEG))
                            && !(((loc == Mek.LOC_RT) || (loc == Mek.LOC_LT)) && entity.hasEngine() &&
                                    (entity.getEngine().getSideTorsoCriticalSlots().length > 2))) {
                        continue;
                    }
                } else {
                    // inner sphere with XL or XXL counts everywhere
                    if (entity.hasEngine() && (entity.getEngine().getSideTorsoCriticalSlots().length <= 2)) {
                        // without XL or XXL, only count torsos if not CASEed,
                        // and arms if arm & torso not CASEed
                        if (((loc == Mek.LOC_RT) || (loc == Mek.LOC_LT)) && entity.locationHasCase(loc)) {
                            continue;
                        } else if ((loc == Mek.LOC_LARM)
                                && (entity.locationHasCase(loc) || entity.locationHasCase(Mek.LOC_LT))) {
                            continue;
                        } else if ((loc == Mek.LOC_RARM)
                                && (entity.locationHasCase(loc) || entity.locationHasCase(Mek.LOC_RT))) {
                            continue;
                        }
                    }
                }
                unProtectedCrits++;
            }
            defensiveValue -= unProtectedCrits;
            bvReport.addLine("Blue Shield", "- " + formatForReport(unProtectedCrits),
                    "= " + formatForReport(defensiveValue));
            hasExplosiveEquipment = true;
        }
        List<CriticalSlot> slotAlreadyCounted = new ArrayList<>();
        for (Mounted<?> mounted : entity.getEquipment()) {
            if (!countsAsExplosive(mounted)) {
                continue;
            }
            EquipmentType etype = mounted.getType();
            if ((etype instanceof MiscType) && etype.hasFlag(MiscType.F_BLUE_SHIELD)) {
                continue;
            }

            // For superheavies, make sure to subtract at most once for each slot
            CriticalSlot critSlot = getCriticalSlot(mounted);
            if (slotAlreadyCounted.contains(critSlot)) {
                continue;
            }

            int loc = mounted.getLocation();
            if (!hasExplosiveEquipmentPenalty(loc) && !hasExplosiveEquipmentPenalty(mounted.getSecondLocation())) {
                continue;
            }

            int toSubtract = 15;
            // Gauss rifles only subtract 1 point per slot, same for HVACs and iHeavy Lasers
            // and mektasers
            if ((etype instanceof GaussWeapon) || (etype instanceof HVACWeapon)
                    || (etype instanceof CLImprovedHeavyLaserLarge)
                    || (etype instanceof CLImprovedHeavyLaserMedium)
                    || (etype instanceof CLImprovedHeavyLaserSmall)
                    || (etype instanceof ISRISCHyperLaser)
                    || (etype instanceof TSEMPWeapon)
                    || (etype instanceof ISMekTaser)
                    || (etype instanceof WeaponType && (etype.hasFlag(WeaponType.F_B_POD) || etype.hasFlag(WeaponType.F_M_POD)))) {
                toSubtract = 1;
            }

            // PPCs with capacitors subtract 1
            if (etype instanceof PPCWeapon) {
                toSubtract = 1;
            }

            if ((etype instanceof MiscType)
                    && (etype.hasFlag(MiscType.F_PPC_CAPACITOR)
                            || etype.hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)
                            || etype.hasFlag(MiscType.F_EMERGENCY_COOLANT_SYSTEM)
                            || etype.hasFlag(MiscType.F_JUMP_JET))) {
                toSubtract = 1;
            }

            if (etype instanceof AmmoType
                    && ((AmmoType) mounted.getType()).getAmmoType() == AmmoType.T_COOLANT_POD) {
                toSubtract = 1;
            }

            // For weapons split between locations, subtract per critical slot; on superheavy, consider the reduced slot count
            int criticals;
            if (mounted.isSplit() || mek.isSuperHeavy()) {
                criticals = 0;
                for (int l = 0; l < entity.locations(); l++) {
                    if (((l == mounted.getLocation()) || (l == mounted.getSecondLocation()))
                            && hasExplosiveEquipmentPenalty(l)) {
                        for (int i = 0; i < entity.getNumberOfCriticals(l); i++) {
                            CriticalSlot slot = entity.getCritical(l, i);
                            if ((slot != null) && mounted.equals(slot.getMount())) {
                                criticals++;
                            }
                        }
                    }
                }
            } else if (mounted.getType() instanceof HVACWeapon) {
                // HVAC are only -1 total, regardless of number of crits. None are large enough
                // to be splittable.
                criticals = 1;
            } else {
                criticals = mounted.getCriticals();
            }
            toSubtract *= criticals;
            defensiveValue -= toSubtract;
            bvReport.addLine("- " + equipmentDescriptor(mounted), "- " + formatForReport(toSubtract),
                    "= " + formatForReport(defensiveValue));
            slotAlreadyCounted.add(critSlot);
            hasExplosiveEquipment = true;
        }
        bvReport.finalizeTentativeSection(hasExplosiveEquipment);
        super.processExplosiveEquipment();
    }

    @Override
    protected double tmmFactor(int tmmRunning, int tmmJumping, int tmmUmu) {
        int targetMovementModifier = Math.max(tmmRunning, Math.max(tmmJumping, tmmUmu));
        // Try to find a Mek Stealth or similar system.
        if (entity.hasStealth() || mek.hasNullSig()) {
            targetMovementModifier += 2;
            bvReport.addLine("Stealth +2", "+2");
        }
        if (mek.hasChameleonShield()) {
            targetMovementModifier += 2;
            bvReport.addLine("Chameleon +2", "+2");
        }
        if (mek.hasVoidSig()) {
            String modifier = "-";
            if (targetMovementModifier < 3) {
                targetMovementModifier = 3;
                modifier = "3";
            } else if (targetMovementModifier == 3) {
                targetMovementModifier++;
                modifier = "+1";
            }
            bvReport.addLine("Void Sig", modifier);
        }
        return 1 + targetMovementModifier / 10.0;
    }

    @Override
    protected void setUmuMP() {
        if (entity.hasShield() && (entity.getNumberOfShields(MiscType.S_SHIELD_LARGE) > 0)) {
            return;
        }
        umuMP = 0;
        for (Mounted<?> m : entity.getMisc()) {
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && type.hasFlag(MiscType.F_UMU) && !m.isInoperable()) {
                umuMP++;
            }
        }
    }

    @Override
    protected int heatEfficiency() {
        int heatCapacity = entity.getHeatCapacity();
        int mekHeatEfficiency = 6 + heatCapacity;
        String calculation = "6 + " + heatCapacity;
        if ((mek instanceof LandAirMek) && (((LandAirMek) mek).getLAMType() == LandAirMek.LAM_STANDARD)) {
            mekHeatEfficiency += 3;
            calculation += " + 3 (LAM)";
        }

        long coolantPods = entity.getAmmo().stream().map(a -> ((AmmoType) a.getType()).getAmmoType())
                .filter(t -> t == AmmoType.T_COOLANT_POD).count();
        if (coolantPods > 0) {
            int coolantPodBonus = (int) Math.ceil((mek.getNumberOfSinks() * coolantPods) / 5d);
            mekHeatEfficiency += coolantPodBonus;
            calculation += " + " + coolantPodBonus + " (Cool. Pods)";
        }

        if (entity.hasWorkingMisc(MiscType.F_EMERGENCY_COOLANT_SYSTEM)) {
            mekHeatEfficiency += 4;
            calculation += " + 4 (ECS)";
        }

        int moveHeat;
        String moveHeatType = " (Run)";
        if ((mek instanceof LandAirMek) && (((LandAirMek) mek).getLAMType() == LandAirMek.LAM_STANDARD)) {
            moveHeat = (int) Math.round(((LandAirMek) mek).getAirMekFlankMP(MPCalculationSetting.BV_CALCULATION) / 3d);
        } else if ((mek.getJumpMP(MPCalculationSetting.BV_CALCULATION) > 0)
                && (entity.getJumpHeat(mek.getJumpMP(MPCalculationSetting.BV_CALCULATION)) > entity.getRunHeat())) {
            moveHeat = entity.getJumpHeat(mek.getJumpMP(MPCalculationSetting.BV_CALCULATION));
            moveHeatType = " (Jump)";
        } else {
            if (mek.hasSCM()) {
                moveHeat = 0;
                moveHeatType = " (SCM)";
            } else {
                moveHeat = entity.getRunHeat();
            }
        }
        mekHeatEfficiency -= moveHeat;
        calculation += " - " + moveHeat + moveHeatType;

        if (entity.hasStealth()) {
            mekHeatEfficiency -= 10;
            calculation += " - 10 (Stealth)";
        }
        if (mek.hasChameleonShield()) {
            mekHeatEfficiency -= 6;
            calculation += " - 6 (Chameleon)";
        }
        if (mek.hasNullSig()) {
            mekHeatEfficiency -= 10;
            calculation += " - 10 (Null Sig.)";
        }
        if (mek.hasVoidSig()) {
            mekHeatEfficiency -= 10;
            calculation += " - 10 (Void Sig.)";
        }
        if (mek.hasEngine() && (mek.getEngineHits() > 0)
                && (mek.getEngine().isFusion() || mek.getEngine().isFission())) {
            mekHeatEfficiency -= mek.getEngineHits() * 5;
            calculation += " - " + mek.getEngineHits() * 5 + " (Engine Hits)";
        }
        bvReport.addLine("Heat Efficiency:", calculation + " = " + mekHeatEfficiency, "");
        return mekHeatEfficiency;
    }

    @Override
    protected Predicate<Mounted<?>> frontWeaponFilter() {
        return weapon -> countAsOffensiveWeapon(weapon)
                && !mek.isArm(weapon.getLocation()) && !weapon.isMekTurretMounted()
                && (!weapon.isRearMounted() || isFrontFacingVGL(weapon));
    }

    @Override
    protected Predicate<Mounted<?>> rearWeaponFilter() {
        return weapon -> countAsOffensiveWeapon(weapon)
                && !mek.isArm(weapon.getLocation()) && !weapon.isMekTurretMounted()
                && (weapon.isRearMounted() || isRearFacingVGL(weapon));
    }

    @Override
    protected boolean isNominalRear(Mounted<?> weapon) {
        return (switchRearAndFront ^ rearWeaponFilter().test(weapon)) && !mek.isArm(weapon.getLocation())
                && !weapon.isMekTurretMounted();
    }

    @Override
    protected void processOffensiveTypeModifier() {
        if (!mek.hasAdvancedFireControl()) {
            // Industrial Meks without AFC multiplay their offensive rating with 0.9
            bvReport.addLine("Fire Control Modifier:",
                    formatForReport(offensiveValue) + " x 0.9",
                    "= " + formatForReport(offensiveValue * 0.9));
            offensiveValue *= 0.9;
        }
    }

    @Override
    protected void processWeight() {
        double aesMultiplier = 1;
        if (entity.hasFunctionalArmAES(Mek.LOC_LARM)) {
            aesMultiplier += 0.1;
        }
        if (entity.hasFunctionalArmAES(Mek.LOC_RARM)) {
            aesMultiplier += 0.1;
        }
        if (entity.hasFunctionalLegAES()) {
            if (mek instanceof BipedMek) {
                aesMultiplier += 0.2;
            } else if (mek instanceof QuadMek) {
                aesMultiplier += 0.4;
            }
        }

        double weight = entity.getWeight() * aesMultiplier;
        String calculation = "+ " + formatForReport(entity.getWeight());
        if (aesMultiplier != 1) {
            calculation += " x " + formatForReport(aesMultiplier) + " (AES)";
        }

        if (mek.hasTSM(true)) {
            offensiveValue += weight * 1.5;
            calculation += " x 1.5 (TSM)";
        } else if (mek.hasIndustrialTSM()) {
            offensiveValue += weight * 1.15;
            calculation += " x 1.15 (ITSM)";
        } else {
            offensiveValue += weight;
        }
        bvReport.addLine("Weight:", calculation, "= " + formatForReport(offensiveValue));
    }

    @Override
    protected void processSummarize() {
        double cockpitMod = 1;
        double riscKitMod = 1;
        String modifier = "";
        if ((mek.getCockpitType() == Mek.COCKPIT_SMALL)
                || (mek.getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED)
                || (mek.getCockpitType() == Mek.COCKPIT_SMALL_COMMAND_CONSOLE)) {
            cockpitMod = 0.95;
            modifier = " (" + mek.getCockpitTypeString() + ")";
        } else if (entity.hasWorkingMisc(MiscType.F_DRONE_OPERATING_SYSTEM)) {
            cockpitMod = 0.95;
            modifier = " (Drone Op. Sys.)";
        } else if (mek.getCockpitType() == Mek.COCKPIT_INTERFACE) {
            cockpitMod = 1.3;
            modifier = " (" + mek.getCockpitTypeString() + ")";
        }

        if (mek.hasRiscHeatSinkOverrideKit()) {
            riscKitMod = 1.01;
        }

        if ((cockpitMod != 1) || (riscKitMod != 1)) {
            baseBV = defensiveValue + offensiveValue;
            bvReport.addEmptyLine();
            bvReport.addSubHeader("Battle Value:");
            bvReport.addLine("Defensive BR + Offensive BR:",
                    formatForReport(defensiveValue) + " + " + formatForReport(offensiveValue),
                    "= " + formatForReport(baseBV));
            if (cockpitMod != 1) {
                bvReport.addLine("Cockpit Modifier:", formatForReport(baseBV) + " x " + formatForReport(cockpitMod) + modifier, "= " + formatForReport(baseBV * cockpitMod));
                baseBV *= cockpitMod;
            }
            if (riscKitMod != 1) {
                bvReport.addLine("RISC Heat Sink Override Kit: ", formatForReport(baseBV) + " x " + formatForReport(riscKitMod) + modifier, "= " + formatForReport(baseBV * riscKitMod));
                baseBV *= riscKitMod;
            }
            bvReport.addLine("--- Base Unit BV:", "" + (int) Math.round(baseBV));
        } else {
            super.processSummarize();
        }
    }

    /**
     * Used in BV calculations. Any equipment that will destroy the unit or leg it
     * if it explodes
     * decreases the defensive battle rating. This is anything in the head, CT, or
     * leg,
     * or side torso if it has >= 3 engine crits, or any location that can transfer
     * damage to that
     * location.
     *
     * @param loc The location index
     * @return Whether explosive equipment in the location should decrease BV
     */
    protected boolean hasExplosiveEquipmentPenalty(int loc) {
        if ((loc == Entity.LOC_NONE) || entity.hasCASEII(loc)) {
            return false;
        }
        if (!entity.entityIsQuad() && ((loc == Mek.LOC_RARM) || (loc == Mek.LOC_LARM))) {
            return !entity.locationHasCase(loc) && hasExplosiveEquipmentPenalty(entity.getTransferLocation(loc));
        } else if ((loc == Mek.LOC_RT) || (loc == Mek.LOC_LT)) {
            return !entity.locationHasCase(loc) || (entity.getEngine().getSideTorsoCriticalSlots().length >= 3);
        } else {
            return true;
        }
    }

    /**
     * Returns the (first) CriticalSlot object for a given mounted equipment in its
     * main location. This is
     * used to find the CriticalSlot for ammo which only uses a single CriticalSlot.
     *
     * @param mounted the equipment to look for
     * @return a CriticalSlot that holds the mounted or null if none can be found
     */
    public @Nullable CriticalSlot getCriticalSlot(Mounted<?> mounted) {
        int location = mounted.getLocation();
        if (location == Entity.LOC_NONE) {
            return null;
        }
        for (int slot = 0; slot < entity.getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = entity.getCritical(location, slot);
            if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_EQUIPMENT)) {
                if (cs.getMount().equals(mounted) ||
                        ((cs.getMount2() != null) && (cs.getMount2().equals(mounted)))) {
                    return cs;
                }
            }
        }
        return null;
    }

    @Override
    protected int getRunningTMM() {
        int mp = runMP;
        if ((mek instanceof LandAirMek) && (((LandAirMek) mek).getLAMType() == LandAirMek.LAM_STANDARD)) {
            mp = ((LandAirMek) mek).getAirMekFlankMP(MPCalculationSetting.BV_CALCULATION);
            if (mp == 0) {
                return 0;
            } else { // IO p. 192 - When determining TMM for a LAM, include the +1 "airborne modifier".
                return Compute.getTargetMovementModifier(mp, false, true, entity.getGame()).getValue();
            }
        } else {
            if (mp == 0) {
                return 0;
            } else {
                return Compute.getTargetMovementModifier(mp, false, false, entity.getGame()).getValue();
            }
        }
    }

    @Override
    protected int offensiveSpeedFactorMP() {
        if ((mek instanceof LandAirMek) && (((LandAirMek) mek).getLAMType() == LandAirMek.LAM_STANDARD)) {
            return runMP + (int) (Math
                    .round(((LandAirMek) mek).getAirMekFlankMP(MPCalculationSetting.BV_CALCULATION) / 2.0));
        } else {
            return runMP + (int) (Math.round(Math.max(jumpMP, umuMP) / 2.0));
        }
    }
}

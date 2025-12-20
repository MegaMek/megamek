/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2012-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.verifier;

import static megamek.client.ui.clientGUI.calculationReport.CalculationReport.formatForReport;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.client.ui.clientGUI.calculationReport.DummyCalculationReport;
import megamek.client.ui.clientGUI.calculationReport.TextCalculationReport;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.exceptions.LocationFullException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Infantry;
import megamek.common.units.InfantryMount;
import megamek.common.weapons.artillery.ArtilleryCannonWeapon;
import megamek.common.weapons.artillery.ArtilleryWeapon;

/**
 * @author Jay Lawson (Taharqa)
 */
public class TestInfantry extends TestEntity {
    private final Infantry infantry;

    public TestInfantry(Infantry infantry, TestEntityOption option, String fileString) {
        super(option, null, null);
        this.infantry = infantry;
        this.fileString = fileString;
    }

    @Override
    public Entity getEntity() {
        return infantry;
    }

    @Override
    public boolean isTank() {
        return false;
    }

    @Override
    public boolean isMek() {
        return false;
    }

    @Override
    public boolean isAero() {
        return false;
    }

    @Override
    public boolean isSmallCraft() {
        return false;
    }

    @Override
    public boolean isAdvancedAerospace() {
        return false;
    }

    @Override
    public boolean isProtoMek() {
        return false;
    }

    @Override
    public double getWeightControls() {
        return 0;
    }

    @Override
    public double getWeightMisc() {
        return 0;
    }

    @Override
    public double getWeightHeatSinks() {
        return 0;
    }

    @Override
    public double getWeightEngine() {
        return 0;
    }

    @Override
    public double getWeightStructure() {
        return 0;
    }

    @Override
    public double getWeightArmor() {
        return 0;
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        return false;
    }

    @Override
    public int getCountHeatSinks() {
        return 0;
    }

    @Override
    public String printWeightMisc() {
        return "";
    }

    @Override
    public String printWeightControls() {
        return "";
    }

    @Override
    public String printWeightStructure() {
        return "";
    }

    @Override
    public String printWeightArmor() {
        return "";
    }

    @Override
    public boolean correctEntity(StringBuffer buff, int ammoTechLvl) {
        Infantry inf = (Infantry) getEntity();
        boolean correct = true;
        if (skip()) {
            return true;
        }
        // Infantry has too many problems with intro date for its equipments therefore we are not testing the
        // year of introduction of the equipments.

        int max = maxSecondaryWeapons(inf);
        if (inf.getSecondaryWeaponsPerSquad() > max) {
            buff.append("Number of secondary weapons exceeds maximum of ").append(max).append("\n\n");
            correct = false;
        }

        if (inf.getSecondaryWeapon() != null) {
            int secondaryCrew = inf.getSecondaryWeapon().getCrew();
            // Beast mounted infantry divide crew requirement in half, rounding up.
            if (inf.getMount() != null) {
                secondaryCrew = secondaryCrew / 2 + secondaryCrew % 2;
            }
            if (inf.getCrew() != null) {
                if (inf.hasAbility(OptionsConstants.MD_TSM_IMPLANT)) {
                    secondaryCrew--;
                }
                if (inf.hasAbility(OptionsConstants.MD_DERMAL_ARMOR)) {
                    secondaryCrew--;
                }
            }
            secondaryCrew = Math.max(secondaryCrew, 1);
            if (secondaryCrew * inf.getSecondaryWeaponsPerSquad() > inf.getSquadSize()) {
                buff.append("Secondary weapon crew requirement exceeds squad size.\n\n");
                correct = false;
            }
        }

        max = maxSquadSize(inf.getMovementMode(), inf.hasMicrolite() || (inf.getAllUMUCount() > 1), inf.getMount());
        if (inf.getSquadSize() > max) {
            buff.append("Maximum squad size is ").append(max).append("\n\n");
            correct = false;
        }

        max = maxSquadCount(inf.getMovementMode(), inf.hasMicrolite() || (inf.getAllUMUCount() > 1),
              inf.getSpecializations(), inf.getMount());
        if (inf.getSquadCount() > max) {
            buff.append("Maximum squad count is ").append(max).append("\n\n");
        }

        max = maxUnitSize(inf.getMovementMode(), inf.hasMicrolite() || (inf.getAllUMUCount() > 1),
              inf.hasSpecialization(Infantry.COMBAT_ENGINEERS | Infantry.MOUNTAIN_TROOPS), inf.getMount());
        if (inf.getShootingStrength() > max) {
            buff.append("Maximum platoon size is ").append(max).append("\n\n");
            correct = false;
        }

        if (inf.isMechanized() && inf.countEquipment(EquipmentTypeLookup.ANTI_MEK_GEAR) > 0) {
            buff.append("Mechanized infantry may not have anti-mek gear!\n");
            correct = false;
        }

        if (inf.countWorkingMisc(MiscType.F_ARMOR_KIT) > 1) {
            buff.append("Infantry may not have more than one armor kit!\n");
            correct = false;
        }

        // Dermal Armor and Dermal Camo Armor are mutually exclusive
        if (inf.hasAbility(OptionsConstants.MD_DERMAL_ARMOR)
              && inf.hasAbility(OptionsConstants.MD_DERMAL_CAMO_ARMOR)) {
            buff.append("Dermal Armor and Dermal Camo Armor are mutually exclusive!\n");
            correct = false;
        }

        if (infantry.hasFieldWeapon()) {
            // These tests include field artillery
            Mounted<?> firstFieldGun = infantry.originalFieldWeapons().get(0);
            EquipmentType fieldGunType = firstFieldGun.getType();
            int fieldGunCount = infantry.originalFieldWeapons().size();

            if (fieldGunCount > 1) {
                if (isFieldArtilleryWeapon(firstFieldGun)) {
                    buff.append("Infantry may only use a single field artillery weapon!\n");
                    correct = false;
                }
                for (Mounted<?> fieldGun : infantry.originalFieldWeapons()) {
                    if (fieldGun.getType() != fieldGunType) {
                        buff.append("All field guns must be of the same type and size!\n");
                        correct = false;
                    }
                }
            }

            int troopersRequired = fieldGunCount * fieldGunCrewRequirement(fieldGunType, infantry);
            if (troopersRequired > infantry.getOriginalTrooperCount()) {
                buff.append("Insufficient troopers to operate the field guns!\n");
                correct = false;
            }
        }

        if (getEntity().hasQuirk(OptionsConstants.QUIRK_NEG_ILLEGAL_DESIGN)
              || getEntity().canonUnitWithInvalidBuild()) {
            correct = true;
        }
        return correct;
    }

    /**
     * @return True if the given equipment type is suitable as a field artillery weapon for a conventional infantry
     *       unit; false for a null equipment type.
     */
    public static boolean isFieldArtilleryType(@Nullable EquipmentType equipmentType) {
        return (equipmentType instanceof ArtilleryWeapon) || (equipmentType instanceof ArtilleryCannonWeapon);
    }

    /**
     * @return True if the given equipment is suitable as a field artillery weapon for a conventional infantry unit;
     *       false for a null equipment.
     */
    public static boolean isFieldArtilleryWeapon(@Nullable Mounted<?> mounted) {
        return (mounted != null) && isFieldArtilleryType(mounted.getType());
    }

    /**
     * Returns the number of troopers of the given infantry required to operate each of the given field gun equipment.
     * Neither parameter is checked for correctness. The returned result is never 0.
     *
     * @param equip    The weapon type to be used as a field gun
     * @param infantry The infantry unit
     *
     * @return The troopers required to operate the field gun
     */
    public static int fieldGunCrewRequirement(EquipmentType equip, Infantry infantry) {
        return Math.max(2, (int) Math.ceil(equip.getTonnage(infantry)));
    }

    public static int maxSecondaryWeapons(Infantry inf) {
        int max;
        if (inf.getMount() != null) {
            max = inf.getMount().size().supportWeaponsPerCreature;
        } else if (inf.getMovementMode() == EntityMovementMode.VTOL) {
            max = inf.hasMicrolite() ? 0 : 1;
        } else if (inf.getMovementMode() == EntityMovementMode.INF_UMU) {
            max = inf.getAllUMUCount();
        } else {
            max = 2;
        }

        if (inf.hasSpecialization(Infantry.COMBAT_ENGINEERS)) {
            max = 0;
        }
        if (inf.hasSpecialization(Infantry.MOUNTAIN_TROOPS | Infantry.PARAMEDICS)) {
            max = 1;
        }
        if (inf.getCrew() != null) {
            if (inf.hasAbility(OptionsConstants.MD_DERMAL_ARMOR)) {
                max++;
            }
            if (inf.hasAbility(OptionsConstants.MD_TSM_IMPLANT)) {
                max++;
            }
        }
        return max;
    }

    /**
     * Maximum squad size based on motive type
     *
     * @param movementMode The platoon's movement mode
     * @param alt          True indicates that VTOL is microlite and INF_UMU is motorized.
     * @param mount        The mount if the unit is beast-mounted, otherwise null.
     *
     * @return The maximum size of a squad.
     */
    public static int maxSquadSize(EntityMovementMode movementMode, boolean alt, @Nullable InfantryMount mount) {
        if (mount == null) {
            return switch (movementMode) {
                case HOVER, SUBMARINE -> 5;
                case WHEELED -> 6;
                case TRACKED -> 7;
                case INF_UMU -> alt ? 6 : 10;
                case VTOL -> alt ? 2 : 4;
                default -> 10;
            };
        } else if (mount.size().troopsPerCreature == 1) {
            return 10; // use foot infantry limit
        } else {
            return mount.size().troopsPerCreature;
        }
    }

    /**
     * The maximum number of squads in a platoon based on its movement mode.
     *
     * @param movementMode   The platoon's movement mode
     * @param alt            True indicates that VTOL is microlite and INF_UMU is motorized.
     * @param specialization The infantry's specialization, if any.
     * @param mount          The mount if the unit is beast-mounted, otherwise null.
     *
     * @return The maximum number of squads/creatures per platoon.
     */
    public static int maxSquadCount(EntityMovementMode movementMode, boolean alt,
          int specialization, @Nullable InfantryMount mount) {

        if (mount == null) {
            int squads = switch (movementMode) {
                case VTOL, HOVER, WHEELED, TRACKED, SUBMARINE -> 4;
                case INF_UMU -> alt ? 2 : 4;
                default -> 5;
            };

            if ((specialization & (Infantry.COMBAT_ENGINEERS | Infantry.MOUNTAIN_TROOPS)) > 0) {
                squads = Math.min(squads, 2);
            }

            if ((specialization & Infantry.PARATROOPS) > 0) {
                squads = Math.min(squads, 3);
            }

            if ((specialization & Infantry.MARINES) > 0) {
                squads = Math.min(squads, 4);
            }

            return squads;

        } else {
            // For Very Large and Monstrous (but not Large) creatures, each creature is one squad.
            if (mount.size() == InfantryMount.BeastSize.LARGE) {
                return 5;
            }
            return mount.size().creaturesPerPlatoon;
        }
    }

    public static int maxUnitSize(EntityMovementMode movementMode, boolean alt, boolean engOrMountain,
          InfantryMount mount) {
        int max;
        if (mount == null) {
            switch (movementMode) {
                case INF_UMU:
                    if (alt) {
                        max = 12;
                    } else {
                        max = 30;
                    }
                    break;
                case HOVER:
                case SUBMARINE:
                    max = 20;
                    break;
                case WHEELED:
                    max = 24;
                    break;
                case TRACKED:
                    max = 28;
                    break;
                case VTOL:
                    max = maxSquadSize(movementMode, alt, null) * 4;
                    break;
                default:
                    max = 30;
                    break;
            }
        } else {
            max = mount.size().creaturesPerPlatoon * mount.size().troopsPerCreature;
        }
        if (engOrMountain) {
            max = Math.min(max, 20);
        }
        return max;
    }

    @Override
    public StringBuffer printEntity() {
        StringBuffer buff = new StringBuffer();
        buff.append("Mek: ").append(infantry.getDisplayName()).append("\n");
        buff.append("Found in: ").append(fileString).append("\n");
        buff.append(printTechLevel());
        buff.append("Intro year: ").append(infantry.getYear()).append("\n");
        buff.append(printSource());
        buff.append(printShortMovement());
        if (correctWeight(buff, true, true)) {
            buff.append("Weight: ").append(getWeight()).append("\n");
        }
        buff.append(printWeightCalculation()).append("\n");
        printFailedEquipment(buff);
        return buff;
    }

    @Override
    public String printWeightCalculation() {
        TextCalculationReport weightReport = new TextCalculationReport();
        getWeightExact(infantry, weightReport);
        return weightReport.toString();
    }

    @Override
    public String getName() {
        return "Infantry: " + infantry.getDisplayName();
    }

    @Override
    public double getWeightPowerAmp() {
        return 0;
    }

    @Override
    public double calculateWeightExact() {
        return getWeightExact(infantry, new DummyCalculationReport());
    }

    /**
     * Calculates the weight of the given Conventional Infantry unit. Infantry weight is not fixed as in Meks and
     * Vehicles but calculated from the infantry configuration.
     *
     * @param infantry The conventional infantry
     *
     * @return The rounded weight in tons
     */
    public static double getWeight(Infantry infantry) {
        double weight = getWeightExact(infantry, new DummyCalculationReport());
        return ceil(weight, Ceil.HALF_TON);
    }

    /**
     * Calculates the weight of the given Conventional Infantry unit. Infantry weight is not fixed as in Meks and
     * Vehicles but calculated from the infantry configuration. The given CalculationReport will be filled in with the
     * weight calculation (the report includes the final rounding step but the returned result does not).
     *
     * @param infantry The conventional infantry
     * @param report   A CalculationReport to fill in
     *
     * @return The exact weight in tons
     */
    public static double getWeightExact(Infantry infantry, CalculationReport report) {
        String header = "Weight Calculation for ";
        String fullName = infantry.getChassis() + " " + infantry.getModel();
        if (fullName.length() < 20) {
            report.addHeader(header + fullName);
        } else if (fullName.length() < 50) {
            report.addHeader(header);
            report.addHeader(fullName);
        } else {
            report.addHeader(header);
            report.addHeader(infantry.getChassis());
            report.addHeader(infantry.getModel());
        }
        report.addEmptyLine();

        InfantryMount mount = infantry.getMount();
        int activeTroopers = Math.max(0, infantry.getInternal(Infantry.LOC_INFANTRY));
        double weight;

        if (mount != null) {
            String calculation;
            report.addLine("Mounted: " + mount.name() + ", "
                  + mount.size().troopsPerCreature + " trooper(s) per mount", "");
            if (mount.size().troopsPerCreature > 1) {
                weight = (mount.weight() + 0.2 * infantry.getSquadSize()) * infantry.getSquadCount();
                calculation = "(" + formatForReport(mount.weight()) + " + 0.2 x "
                      + infantry.getSquadSize() + ") x " + infantry.getSquadCount();
            } else {
                weight = (mount.weight() + 0.2) * activeTroopers;
                calculation = "(" + formatForReport(mount.weight()) + " + 0.2) x " + activeTroopers;
            }
            report.addLine("", calculation, formatForReport(weight) + " t");

        } else { // not beast-mounted
            double multiplier;
            switch (infantry.getMovementMode()) {
                case INF_MOTORIZED:
                    multiplier = 0.195;
                    break;
                case HOVER:
                case TRACKED:
                case WHEELED:
                    multiplier = 1.0;
                    break;
                case VTOL:
                    multiplier = infantry.hasMicrolite() ? 1.4 : 1.9;
                    break;
                case INF_JUMP:
                    multiplier = 0.165;
                    break;
                case INF_UMU:
                    if (infantry.getActiveUMUCount() > 1) {
                        multiplier = 0.295; // motorized + 0.1 for motorized scuba
                    } else {
                        multiplier = 0.135; // foot + 0.05 for scuba
                    }
                    break;
                case SUBMARINE:
                    multiplier = 0.9;
                    break;
                case INF_LEG:
                default:
                    multiplier = 0.085;
            }
            report.addLine("Base Weight: ", infantry.getMovementModeAsString(), formatForReport(multiplier) + " t");

            if (infantry.hasSpecialization(Infantry.COMBAT_ENGINEERS)) {
                multiplier += 0.1;
                report.addLine("", "Combat Engineers", "+ 0.1 t");

            }

            if (infantry.hasSpecialization(Infantry.PARATROOPS)) {
                multiplier += 0.05;
                report.addLine("", "Paratroopers", "+ 0.05 t");
            }

            if (infantry.hasSpecialization(Infantry.PARAMEDICS)) {
                multiplier += 0.05;
                report.addLine("", "Paramedics", "+ 0.05 t");
            }

            if (infantry.hasAntiMekGear()) {
                multiplier += .015;
                report.addLine("", "Anti-Mek Gear", "+ 0.015 t");
            }

            weight = activeTroopers * multiplier;
            report.addLine("Trooper Weight:", activeTroopers + " x " + formatForReport(multiplier) + " t",
                  formatForReport(weight) + " t");

            weight += infantry.activeFieldWeapons().stream().mapToDouble(Mounted::getTonnage).sum();
            weight += infantry.getAmmo().stream().mapToDouble(Mounted::getTonnage).sum();

            infantry.activeFieldWeapons().forEach(mounted ->
                  report.addLine(mounted.getName(), "",
                        "+ " + formatForReport(mounted.getTonnage()) + " t"));
            infantry.getAmmo().forEach(mounted ->
                  report.addLine(mounted.getName(), "",
                        "+ " + formatForReport(mounted.getTonnage()) + " t"));
        }

        report.addEmptyLine();
        // Intentional: Add the final rounding to the report, but return the exact weight
        double roundedWeight = ceil(weight, Ceil.HALF_TON);
        report.addLine("Final Weight:", "round up to nearest half ton",
              formatForReport(roundedWeight) + " t");
        return weight;
    }

    public static void adaptAntiMekAttacks(Infantry infantry) {
        try {
            removeAntiMekAttacks(infantry);
            if (infantry.canMakeAntiMekAttacks()) {
                InfantryMount mount = infantry.getMount();
                if ((mount == null) || mount.size().canMakeSwarmAttacks) {
                    infantry.addEquipment(EquipmentType.get(Infantry.SWARM_MEK), Infantry.LOC_INFANTRY);
                    infantry.addEquipment(EquipmentType.get(Infantry.STOP_SWARM), Infantry.LOC_INFANTRY);
                }
                if ((mount == null) || mount.size().canMakeLegAttacks) {
                    infantry.addEquipment(EquipmentType.get(Infantry.LEG_ATTACK), Infantry.LOC_INFANTRY);
                }
            }
        } catch (LocationFullException ignored) {
            // not on Infantry
        }
    }

    // The following methods are a condensed version of MML's UnitUtil.removeMounted
    // and can be replaced if the latter is ever moved into MM
    public static void removeAntiMekAttacks(Infantry unit) {
        removeAntiMekAttack(unit, EquipmentType.get(Infantry.SWARM_MEK));
        removeAntiMekAttack(unit, EquipmentType.get(Infantry.STOP_SWARM));
        removeAntiMekAttack(unit, EquipmentType.get(Infantry.LEG_ATTACK));
        unit.recalculateTechAdvancement();
    }

    public static void removeAntiMekAttack(Infantry unit, EquipmentType antiMekType) {
        unit.getEquipment().removeIf(m -> m.getType() == antiMekType);
        unit.getWeaponList().removeIf(m -> m.getType() == antiMekType);
        unit.getTotalWeaponList().removeIf(m -> m.getType() == antiMekType);
    }
}

/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.verifier;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.client.ui.swing.calculationReport.DummyCalculationReport;
import megamek.client.ui.swing.calculationReport.TextCalculationReport;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;

/**
 * @author Jay Lawson (Taharqa)
 */
public class TestInfantry extends TestEntity {
    private Infantry infantry;
    
    public TestInfantry(Infantry infantry, TestEntityOption option, String fileString) {
        super(option, null, null, null);
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
    public boolean isMech() {
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
    public boolean isProtomech() {
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
    public boolean correctEntity(StringBuffer buff) {
        return correctEntity(buff, getEntity().getTechLevel());
    }

    @Override
    public boolean correctEntity(StringBuffer buff, int ammoTechLvl) {
        Infantry inf = (Infantry) getEntity();
        boolean correct = true;
        if (skip()) {
            return true;
        }

        // We currently have many unit introduction dates that are too early for their gear or anti-mek attacks
        // enable this when dates have been straightened
//        if (showIncorrectIntroYear() && hasIncorrectIntroYear(buff)) {
//            correct = false;
//        }

        int max = maxSecondaryWeapons(inf);
        if (inf.getSecondaryWeaponsPerSquad() > max) {
            buff.append("Number of secondary weapons exceeds maximum of " + max).append("\n\n");
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
            buff.append("Maximum squad size is " + max + "\n\n");
            correct = false;
        }

        max = maxUnitSize(inf.getMovementMode(), inf.hasMicrolite() || (inf.getAllUMUCount() > 1),
                inf.hasSpecialization(Infantry.COMBAT_ENGINEERS | Infantry.MOUNTAIN_TROOPS), inf.getMount());
        if (inf.getShootingStrength() > max) {
            buff.append("Maximum platoon size is " + max + "\n\n");
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

        return correct;
    }
    
    public static int maxSecondaryWeapons(Infantry inf) {
        int max;
        if (inf.getMount() != null) {
            max = inf.getMount().getSize().supportWeaponsPerCreature;
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
     * @param movementMode  The platoon's movement mode
     * @param alt           True indicates that VTOL is microlite and INF_UMU is motorized.
     * @param mount         The mount if the unit is beast-mounted, otherwise null.
     * @return              The maximum size of a squad.
     */
    public static int maxSquadSize(EntityMovementMode movementMode, boolean alt, @Nullable InfantryMount mount) {
        if (mount == null) {
            switch (movementMode) {
                case HOVER:
                case SUBMARINE:
                    return 5;
                case WHEELED:
                    return 6;
                case TRACKED:
                    return 7;
                case INF_UMU:
                    return alt ? 6 : 10;
                case VTOL:
                    return alt ? 2 : 4;
                default:
                    return 10;
            }
        } else if (mount.getSize().troopsPerCreature == 1) {
            return 10; // use foot infantry limit
        } else {
            return mount.getSize().troopsPerCreature;
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
                    max = maxSquadSize(movementMode, alt, mount) * 4;
                    break;
                default:
                    max = 30;
                    break;
            }
        } else {
            max = mount.getSize().creaturesPerPlatoon * mount.getSize().troopsPerCreature;
        }
        if (engOrMountain) {
            max = Math.min(max, 20);
        }
        return max;
    }

    @Override
    public StringBuffer printEntity() {
        StringBuffer buff = new StringBuffer();
        buff.append("Mech: ").append(infantry.getDisplayName()).append("\n");
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
     * Calculates the weight of the given Conventional Infantry unit. Infantry weight
     * is not fixed as in Meks and Vehicles but calculated from the infantry configuration.
     *
     * @param infantry The conventional infantry
     * @return The rounded weight in tons
     */
    public static double getWeight(Infantry infantry) {
        double weight = getWeightExact(infantry, new DummyCalculationReport());
        return ceil(weight, Ceil.HALFTON);
    }

    /**
     * Calculates the weight of the given Conventional Infantry unit. Infantry weight
     * is not fixed as in Meks and Vehicles but calculated from the infantry configuration.
     * The given CalculationReport will be filled in with the weight calculation (the
     * report includes the final rounding step but the returned result does not).
     *
     * @param infantry The conventional infantry
     * @param report A CalculationReport to fill in
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
        int activeTroopers = infantry.getInternal(Infantry.LOC_INFANTRY);
        double weight;

        if (mount != null) {
            String calculation;
            report.addLine("Mounted: " + mount.getName() + ", "
                    + mount.getSize().troopsPerCreature + " trooper(s) per mount", "");
            if (mount.getSize().troopsPerCreature > 1) {
                weight = (mount.getWeight() + 0.2 * infantry.getSquadSize()) * infantry.getSquadCount();
                calculation = "(" + formatForReport(mount.getWeight()) + " + 0.2 x "
                        + infantry.getSquadSize() + ") x " + infantry.getSquadCount();
            } else {
                weight = (mount.getWeight() + 0.2) * activeTroopers;
                calculation = "(" + formatForReport(mount.getWeight()) + " + 0.2) x " + activeTroopers;
            }
            report.addLine("", calculation, formatForReport(weight) + " t");

        } else { // not beast-mounted
            double mult;
            switch (infantry.getMovementMode()) {
                case INF_MOTORIZED:
                    mult = 0.195;
                    break;
                case HOVER:
                case TRACKED:
                case WHEELED:
                    mult = 1.0;
                    break;
                case VTOL:
                    mult = infantry.hasMicrolite() ? 1.4 : 1.9;
                    break;
                case INF_JUMP:
                    mult = 0.165;
                    break;
                case INF_UMU:
                    if (infantry.getActiveUMUCount() > 1) {
                        mult = 0.295; // motorized + 0.1 for motorized scuba
                    } else {
                        mult = 0.135; // foot + 0.05 for scuba
                    }
                    break;
                case SUBMARINE:
                    mult = 0.9;
                    break;
                case INF_LEG:
                default:
                    mult = 0.085;
            }
            report.addLine("Base Weight: ", infantry.getMovementModeAsString(), formatForReport(mult) + " t");

            if (infantry.hasSpecialization(Infantry.COMBAT_ENGINEERS)) {
                mult += 0.1;
                report.addLine("", "Combat Engineers", "+ 0.1 t");

            }

            if (infantry.hasSpecialization(Infantry.PARATROOPS)) {
                mult += 0.05;
                report.addLine("", "Paratroopers", "+ 0.05 t");
            }

            if (infantry.hasSpecialization(Infantry.PARAMEDICS)) {
                mult += 0.05;
                report.addLine("", "Paramedics", "+ 0.05 t");
            }

            if (infantry.hasAntiMekGear()) {
                mult += .015;
                report.addLine("", "Anti-Mek Gear", "+ 0.015 t");
            }

            weight = activeTroopers * mult;
            report.addLine("Trooper Weight:", activeTroopers + " x " + formatForReport(mult) + " t",
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
        double roundedWeight = ceil(weight, Ceil.HALFTON);
        report.addLine("Final Weight:", "round up to nearest half ton",
                formatForReport(roundedWeight) + " t");
        return weight;
    }

    public static void adaptAntiMekAttacks(Infantry infantry) {
        try {
            removeAntiMekAttacks(infantry);
            if (infantry.canMakeAntiMekAttacks()) {
                InfantryMount mount = infantry.getMount();
                if ((mount == null) || mount.getSize().canMakeSwarmAttacks) {
                    infantry.addEquipment(EquipmentType.get(Infantry.SWARM_MEK), Infantry.LOC_INFANTRY);
                    infantry.addEquipment(EquipmentType.get(Infantry.STOP_SWARM), Infantry.LOC_INFANTRY);
                }
                if ((mount == null) || mount.getSize().canMakeLegAttacks) {
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

    public static void removeAntiMekAttack(Infantry unit, EquipmentType et) {
        for (int pos = unit.getEquipment().size() - 1; pos >= 0; pos--) {
            Mounted mount = unit.getEquipment().get(pos);
            if (mount.getType().equals(et)) {
                unit.getEquipment().remove(mount);
                unit.getWeaponList().remove(mount);
                unit.getTotalWeaponList().remove(mount);
            }
        }
    }
}
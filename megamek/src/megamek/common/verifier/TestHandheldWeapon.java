/*
 * MegaMek - Copyright (C) 2025 The MegaMek Team
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

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.HandheldWeapon;
import megamek.common.MiscType;
import megamek.common.equipment.AmmoMounted;

public class TestHandheldWeapon extends TestEntity {
    private final HandheldWeapon hhw;

    public TestHandheldWeapon(HandheldWeapon hhw, TestEntityOption option, String fileString) {
        super(option, null, null);
        this.hhw = hhw;
        this.fileString = fileString;
    }

    @Override
    public Entity getEntity() {
        return hhw;
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
        return Math.max(0, getCountHeatSinks());
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        return false;
    }

    @Override
    public int getCountHeatSinks() {
        return heatNeutralHSRequirement();
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
    public boolean correctEntity(StringBuffer buff, int ammoTechLvl) {
        boolean correct = true;
        if (skip()) {
            return true;
        }
        if (!correctWeight(buff)) {
            buff.append(printWeightCalculation()).append("\n");
            correct = false;
        }
        if (hhw.getMiscEquipment(MiscType.F_CLUB).isEmpty()) {
            var items = hhw.getEquipment().stream()
                .filter(m -> !(m.getType() instanceof AmmoType) && !m.getType().hasFlag(MiscType.F_WEAPON_ENHANCEMENT))
                .count();
            if (items > 6) {
                buff.append("Handheld Weapon can only mount up to 6 items!\n");
                correct = false;
            }
        } else {
            if (hhw.getEquipment().size() > 1) {
                buff.append("A Handheld Weapon with a Melee Weapon can have no other items!\n");
                correct = false;
            }
        }

        return correct;
    }

    @Override
    public StringBuffer printEntity() {
        var buff = new StringBuffer();
        buff.append("Handheld Weapon: ").append(hhw.getDisplayName()).append("\n");
        buff.append("Found in: ").append(fileString).append("\n");
        buff.append(printTechLevel());
        buff.append("Intro year: ").append(hhw.getYear()).append("\n");
        buff.append(printSource());
        if (correctWeight(buff, true, true)) {
            buff.append("Weight: ").append(getWeight()).append(" (").append(
                calculateWeight()).append(")\n");
        }

        buff.append(printWeightCalculation()).append("\n");
        printFailedEquipment(buff);
        return buff;
    }

    @Override
    public String getName() {
        return "Handheld Weapon: " + hhw.getDisplayName();
    }

    @Override
    public double getWeightPowerAmp() {
        return 0;
    }

    @Override
    public double getWeightStructure() {
        return 0;
    }

    @Override
    public String printWeightStructure() {
        return "";
    }
}

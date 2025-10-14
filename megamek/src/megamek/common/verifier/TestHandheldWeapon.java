/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import java.util.HashSet;
import java.util.Set;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import org.apache.commons.lang3.tuple.Pair;

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
                  // Ammo and weapon enhancements (artemis, ppc capacitors, etc.) don't count towards the
                  // item limit
                  .filter(m -> !(m.getType() instanceof AmmoType) && !(m instanceof MiscMounted && m.getType()
                        .hasFlag(MiscType.F_WEAPON_ENHANCEMENT)))
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

        if (showFailedEquip() && hasFailedEquipment(buff)) {
            correct = false;
        }
        if (hasIllegalTechLevels(buff, ammoTechLvl)) {
            correct = false;
        }
        if (showIncorrectIntroYear() && hasIncorrectIntroYear(buff)) {
            correct = false;
        }
        if (hasIllegalEquipmentCombinations(buff)) {
            correct = false;
        }
        if (getEntity().hasQuirk(OptionsConstants.QUIRK_NEG_ILLEGAL_DESIGN)
              || getEntity().canonUnitWithInvalidBuild()) {
            correct = true;
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
    public boolean hasIllegalEquipmentCombinations(StringBuffer buff) {
        boolean illegal = super.hasIllegalEquipmentCombinations(buff);

        Set<Pair<AmmoTypeEnum, Integer>> ammoKinds = new HashSet<>();
        for (var at : hhw.getAmmo()) {
            var kind = Pair.of(at.getType().getAmmoType(), at.getType().getRackSize());
            if (ammoKinds.contains(kind)) {
                illegal = true;
                buff.append("Handheld weapon can only mount a single ammo bin for a given kind of ammo.\n");
                buff.append(
                      "        Hint: If you're designing this weapon, instead of adding more ammo bins,\n        you can edit the value in the Shots column of the equipment list.\n");
                break;
            }
            ammoKinds.add(kind);
        }

        return illegal;
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

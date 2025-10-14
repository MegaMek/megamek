/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.List;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.units.Mek;

public enum MekJumpJets {
    JJ_STANDARD(EquipmentTypeLookup.JUMP_JET, true, Mek.JUMP_STANDARD),
    JJ_IMPROVED(EquipmentTypeLookup.IMPROVED_JUMP_JET, false, Mek.JUMP_IMPROVED),
    JJ_PROTOTYPE(EquipmentTypeLookup.PROTOTYPE_JUMP_JET, true, Mek.JUMP_PROTOTYPE),
    JJ_PROTOTYPE_IMPROVED(EquipmentTypeLookup.PROTOTYPE_IMPROVED_JJ, false, Mek.JUMP_PROTOTYPE_IMPROVED),
    JJ_UMU(EquipmentTypeLookup.MEK_UMU, false, Mek.JUMP_NONE);

    private final String internalName;
    private final boolean industrial;
    private final int jumpType;

    MekJumpJets(String internalName, boolean industrial, int jumpType) {
        this.internalName = internalName;
        this.industrial = industrial;
        this.jumpType = jumpType;
    }

    public String getName() {
        return internalName;
    }

    public boolean canIndustrialUse() {
        return industrial;
    }

    public int getJumpType() {
        return jumpType;
    }

    public static List<EquipmentType> allJJs(boolean industrialOnly) {
        List<EquipmentType> retVal = new ArrayList<>();
        for (MekJumpJets jj : values()) {
            if (jj.industrial || !industrialOnly) {
                retVal.add(EquipmentType.get(jj.internalName));
            }
        }
        return retVal;
    }

}

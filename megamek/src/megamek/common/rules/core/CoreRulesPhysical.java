package megamek.common.rules.core;
/*
 * Copyright (C) 2026 James Magnan (bmazur@sev.org)
 * Copyright (C) 2004-2026 The MegaMek Team. All Rights Reserved.
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

import megamek.common.CriticalSlot;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.rules.RulesPhysical;
import megamek.common.units.Entity;

public class CoreRulesPhysical extends RulesPhysical {

    // Shield rules for damage while punching. Core p.195
    public int getShieldDamageBoost(Entity entity, int armLoc) {
        if (entity.hasShield()) {
            for (int slot = 0; slot < entity.getNumberOfCriticalSlots(armLoc); slot++) {
                CriticalSlot cs = entity.getCritical(armLoc, slot);

                if (cs == null) {
                    continue;
                }

                if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
                    continue;
                }

                Mounted<?> m = cs.getMount();
                EquipmentType type = m.getType();
                if ((type instanceof MiscType) && ((MiscType) type).isShield()) {
                    if ((((MiscMounted) m).getDamageAbsorption(entity, armLoc) > 0)
                          && (((MiscMounted) m).getCurrentDamageCapacity(entity, armLoc) > 0)) {
                        if (type.hasFlag(MiscTypeFlag.S_SHIELD_LARGE)) {
                            return 3;
                        } else if (type.hasFlag(MiscTypeFlag.S_SHIELD_MEDIUM)) {
                            return 2;
                        } else if (type.hasFlag(MiscTypeFlag.S_SHIELD_SMALL)) {
                            return 1;
                        }
                    } else {
                        // Shield DA or DC is 0, so no bonus
                        return 0;
                    }
                }
            }
        }

        // if there is no shield, or fallback
        return 0;
    }

    // Claws now have a TN modifier of 0. Core p.194
    public int getClawToHitModifier() { return 0; }

    // Shields reset their state at the end of the phase
    public boolean phaseChangeShield() {
        return true;
    }
}

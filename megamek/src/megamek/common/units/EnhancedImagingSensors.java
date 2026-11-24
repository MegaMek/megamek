/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common.units;

import megamek.common.CriticalSlot;

/**
 * Decides whether a unit's sensors are damaged for the purpose of its Enhanced Imaging (EI) system. IO p.69: a
 * critical hit to the sensors disables EI, including the 1-hex active probe it grants, while a shut down interface or a
 * pilot without implants keeps the probe.
 */
final class EnhancedImagingSensors {

    private EnhancedImagingSensors() {
    }

    /**
     * Returns whether the unit has sensor damage that disables its Enhanced Imaging. A Mek checks its head sensors, and
     * the centre torso sensor slot as well when its cockpit is torso-mounted. A ProtoMek checks its head, as
     * {@link ProtoMek#hasActiveEiCockpit()} does. Other units have no sensor slots, so this is {@code false} for them.
     *
     * @param entity the unit carrying the EI system
     *
     * @return {@code true} if a sensor hit disables the unit's Enhanced Imaging
     */
    static boolean areDamaged(Entity entity) {
        if (entity instanceof ProtoMek protoMek) {
            return protoMek.getCritsHit(ProtoMek.LOC_HEAD) > 0;
        }
        if (entity instanceof Mek mek) {
            if (mek.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, Mek.LOC_HEAD) > 0) {
                return true;
            }
            return (mek.getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED)
                  && (mek.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, Mek.LOC_CENTER_TORSO) > 0);
        }
        return false;
    }
}

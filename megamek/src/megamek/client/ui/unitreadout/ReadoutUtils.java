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
package megamek.client.ui.unitreadout;

import megamek.common.Entity;
import megamek.common.MiscType;
import megamek.common.equipment.MiscMounted;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class contains some readout modules that are used by Entity types that are not in the same hierarchy (e.g.
 * support vehicles that can be Aero and Tank) but still have only limited use.
 */
final class ReadoutUtils {

    static List<ViewElement> createChassisModList(Entity entity) {

        List<MiscMounted> chassisMods = entity.getMisc().stream()
              .filter(m -> m.getType().hasFlag(MiscType.F_CHASSIS_MODIFICATION))
              .toList();

        if (chassisMods.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<ViewElement> result = new ArrayList<>();
            ItemList list = new ItemList("Chassis Modifications");
            chassisMods.forEach(mod -> list.addItem(mod.getShortName()));
            result.add(list);
            return result;
        }
    }

    private ReadoutUtils() { }
}

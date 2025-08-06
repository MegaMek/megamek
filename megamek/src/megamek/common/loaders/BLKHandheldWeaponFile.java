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

package megamek.common.loaders;

import megamek.common.Entity;
import megamek.common.HandheldWeapon;
import megamek.common.util.BuildingBlock;

public class BLKHandheldWeaponFile extends BLKFile implements IMekLoader {
    public BLKHandheldWeaponFile(BuildingBlock block) {
        dataFile = block;
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {
        HandheldWeapon e = new HandheldWeapon();
        setBasicEntityData(e);

        if (!dataFile.exists("year")) {
            throw new EntityLoadingException("Could not find year block.");
        }
        e.setYear(dataFile.getDataAsInt("year")[0]);

        loadEquipment(e, "Gun", HandheldWeapon.LOC_GUN);

        if (!dataFile.exists("tonnage")) {
            throw new EntityLoadingException("Could not find weight block.");
        }
        e.setWeight(dataFile.getDataAsDouble("tonnage")[0]);

        if (!dataFile.exists("armor")) {
            throw new EntityLoadingException("Could not find armor block.");
        }
        int armor = dataFile.getDataAsInt("armor")[0];
        e.initializeArmor(armor, HandheldWeapon.LOC_GUN);
        e.setArmorTonnage(e.getArmorWeight());

        e.recalculateTechAdvancement();
        return e;
    }
}

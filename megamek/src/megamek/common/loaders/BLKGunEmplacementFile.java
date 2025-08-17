/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org) Copyright (C)
 * Copyright (C) 2005 Mike Gratton <mike@vee.net>
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

import megamek.common.units.Entity;
import megamek.common.equipment.GunEmplacement;
import megamek.common.util.BuildingBlock;

/**
 * @author njrkrynn
 * @since April 6, 2002, 2:06 AM
 */
public class BLKGunEmplacementFile extends BLKFile implements IMekLoader {

    public BLKGunEmplacementFile(BuildingBlock bb) {
        dataFile = bb;
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {
        GunEmplacement gunEmplacement = new GunEmplacement();
        setBasicEntityData(gunEmplacement);

        if (dataFile.exists("Turret")) {
            if (dataFile.getDataAsInt("Turret")[0] != 1) {
                gunEmplacement.setHasNoTurret(true);
            }
        }

        // our gun emplacements do not support dual turrets at this time
        gunEmplacement.setHasNoDualTurret(true);

        loadEquipment(gunEmplacement, "Guns", GunEmplacement.LOC_GUNS);
        gunEmplacement.setArmorTonnage(gunEmplacement.getArmorWeight());
        loadQuirks(gunEmplacement);
        return gunEmplacement;
    }
}

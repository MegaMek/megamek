/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.artillery;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentTypeLookup;

/**
 * @author Sebastian Brocks
 * @since Oct 20, 2004
 */
public class Sniper extends ArtilleryWeapon {
    @Serial
    private static final long serialVersionUID = -5022670163785084036L;

    public Sniper() {
        super();

        name = "Sniper";
        setInternalName(EquipmentTypeLookup.SNIPER_ARTY);
        addLookupName("ISSniperArtillery");
        addLookupName("IS Sniper");
        addLookupName("CLSniper");
        addLookupName("CLSniperArtillery");
        addLookupName("Clan Sniper");
        heat = 10;
        rackSize = 20;
        ammoType = AmmoType.AmmoTypeEnum.SNIPER;
        shortRange = 1;
        mediumRange = 2;
        longRange = 18;
        extremeRange = 18; // No extreme range.
        tonnage = 20;
        criticalSlots = 20;
        svSlots = 10;
        bv = 85;
        cost = 300000;
        rulesRefs = "96, TO:AUE";
        techAdvancement.setTechBase(TechBase.ALL)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }
}

/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.unofficial.clan;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.autoCannons.RACWeapon;

/**
 * @author Sebastian Brocks
 * @since Oct 19, 2004
 */
public class CLRAC10 extends RACWeapon {
    @Serial
    private static final long serialVersionUID = 7945585759921446908L;

    public CLRAC10() {
        super();

        this.name = "Rotary AC/10";
        this.setInternalName("CLRotaryAC10");
        this.addLookupName("Clan Rotary AC/10");
        this.addLookupName("Clan Rotary Assault Cannon/10");
        this.heat = 3;
        this.damage = 10;
        this.rackSize = 10;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 18;
        this.extremeRange = 24;
        this.tonnage = 14.0;
        this.criticalSlots = 7;
        this.bv = 617;
        this.cost = 640000;
        rulesRefs = "Unofficial";
        flags = flags.andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(3073, 3104, 3145, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CSF);
    }
}

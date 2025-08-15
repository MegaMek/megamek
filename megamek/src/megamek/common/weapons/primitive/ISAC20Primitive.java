/*

 * Copyright (C) 2000-2007 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.primitive;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.PrimitiveACWeaponHandler;
import megamek.common.weapons.autocannons.ACWeapon;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class ISAC20Primitive extends ACWeapon {
    private static final long serialVersionUID = 4703572491922950865L;

    public ISAC20Primitive() {
        super();

        name = "Primitive Prototype Autocannon/20";
        setInternalName("Autocannon/20 Primitive");
        addLookupName("IS Auto Cannon/20 Primitive");
        addLookupName("Auto Cannon/20 Primitive");
        addLookupName("AutoCannon/20 Primitive");
        addLookupName("ISAC20p");
        addLookupName("IS Autocannon/20 Primitive");
        this.shortName = "AC/20p";
        ammoType = AmmoType.AmmoTypeEnum.AC_PRIMITIVE;
        heat = 7;
        damage = 20;
        rackSize = 20;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 14.0;
        criticals = 10;
        bv = 178;
        cost = 300000;
        shortAV = 20;
        maxRange = RANGE_SHORT;
        flags = flags.or(F_PROTOTYPE);
        explosionDamage = damage;
        // IO Doesn't strictly define when these weapons stop production. Checked with Herb, and
        // they would always be around. This to cover some of the back worlds in the Periphery.
        rulesRefs = "118, IO";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2488, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        return new PrimitiveACWeaponHandler(toHit, waa, game, manager);
    }
}

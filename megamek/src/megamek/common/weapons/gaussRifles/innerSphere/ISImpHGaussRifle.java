/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.gaussRifles.innerSphere;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.weapons.gaussRifles.GaussWeapon;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.HGRHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Jason Tighe
 * @since Oct 19, 2004
 */
public class ISImpHGaussRifle extends GaussWeapon {
    @Serial
    private static final long serialVersionUID = -2379383217525139478L;

    public ISImpHGaussRifle() {
        super();

        name = "Improved Heavy Gauss Rifle";
        shortName = "Imp. Heavy Gauss Rifle";
        setInternalName("ISImprovedHeavyGaussRifle");
        addLookupName("IS Improved Heavy Gauss Rifle");
        sortingName = "Gauss IMP D";
        heat = 2;
        damage = 22;
        ammoType = AmmoType.AmmoTypeEnum.IGAUSS_HEAVY;
        minimumRange = 3;
        shortRange = 6;
        mediumRange = 12;
        longRange = 19;
        extremeRange = 28;
        tonnage = 20.0;
        criticalSlots = 11;
        bv = 385;
        cost = 700000;
        shortAV = 22;
        medAV = 22;
        longAV = 22;
        maxRange = RANGE_LONG;
        explosionDamage = 30;
        rulesRefs = "126, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3065, DATE_NONE, 3081, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.LC).setProductionFactions(Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        try {
            return new HGRHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }
}

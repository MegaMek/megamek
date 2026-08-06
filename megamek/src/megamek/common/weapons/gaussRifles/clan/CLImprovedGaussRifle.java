/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.gaussRifles.clan;

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
import megamek.common.weapons.handlers.GRHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Andrew Hunter
 * @since Oct 19, 2004
 */
public class CLImprovedGaussRifle extends GaussWeapon {
    @Serial
    private static final long serialVersionUID = -8454131645293473685L;

    public CLImprovedGaussRifle() {
        super();
        name = "Improved Gauss Rifle";
        setInternalName("Improved Gauss Rifle");
        addLookupName("CLIMPGaussRifle");
        sortingName = "Gauss Imp";
        heat = 1;
        damage = 15;
        ammoType = AmmoType.AmmoTypeEnum.GAUSS_IMP;
        minimumRange = 2;
        shortRange = 7;
        mediumRange = 15;
        longRange = 22;
        extremeRange = 33;
        tonnage = 13.0;
        criticalSlots = 6;
        bv = 320;
        cost = 300000;
        shortAV = 15;
        medAV = 15;
        longAV = 15;
        maxRange = RANGE_LONG;
        explosionDamage = 20;
        rulesRefs = "90, IO:AE";
        techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.E)
              .setClanAdvancement(2818, 2821, 2822, 2837, 3080)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CGS)
              .setProductionFactions(Faction.CGS)
              .setReintroductionFactions(Faction.EI)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        try {
            return new GRHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }
}

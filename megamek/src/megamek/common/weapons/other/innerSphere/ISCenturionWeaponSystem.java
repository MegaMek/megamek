/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.other.innerSphere;

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
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.CenturionWeaponSystemHandler;
import megamek.common.weapons.lasers.EnergyWeapon;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 13, 2004
 */
public class ISCenturionWeaponSystem extends EnergyWeapon {
    @Serial
    private static final long serialVersionUID = 5355363156621487309L;

    public ISCenturionWeaponSystem() {
        super();
        name = "Centurion Weapon System";
        setInternalName(name);
        heat = 4;
        damage = 0;
        minimumRange = 0;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        tonnage = 5.0;
        criticalSlots = 2;
        bv = 190;
        cost = 1000000;
        shortAV = 0;
        medAV = 0;
        waterShortRange = 4;
        waterMediumRange = 7;
        waterLongRange = 10;
        waterExtremeRange = 14;
        maxRange = RANGE_MED;
        flags = flags.or(F_MEK_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON).or(F_CWS);
        rulesRefs = "79, IO:AE";
        techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.X)
              .setISAdvancement(2762, DATE_NONE, DATE_NONE, 2770, DATE_NONE)
              .setISApproximate(true, false, false, true, false)
              .setPrototypeFactions(Faction.TH).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.game.Game,
     * megamek.server.Server)
     */
    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        try {
            return new CenturionWeaponSystemHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;

    }
}

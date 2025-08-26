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

package megamek.common.weapons.battleArmor.clan;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

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
import megamek.common.weapons.Weapon;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.MicroBombHandler;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public class CLBAMicroBomb extends Weapon {
    @Serial
    private static final long serialVersionUID = 1467436625346131281L;

    public CLBAMicroBomb() {
        super();
        name = "Bomb Rack (Micro)";
        setInternalName("CLBAMicroBomb");
        addLookupName("CLBAMicro Bomb");
        heat = 0;
        damage = DAMAGE_VARIABLE;
        rackSize = 2;
        ammoType = AmmoType.AmmoTypeEnum.BA_MICRO_BOMB;
        shortRange = 0;
        mediumRange = 0;
        longRange = 0;
        extremeRange = 0;
        bv = 11;
        flags = flags.or(F_NO_FIRES)
              .or(F_BA_WEAPON)
              .or(F_ONE_SHOT)
              .andNot(F_MEK_WEAPON)
              .andNot(F_TANK_WEAPON)
              .andNot(F_AERO_WEAPON)
              .andNot(F_PROTO_WEAPON);
        tonnage = .1;
        criticalSlots = 2;
        cost = 30000;
        rulesRefs = "253, TM";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(3055, 3060, 3065, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCC)
              .setProductionFactions(Faction.CCC);
    }

    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        try {
            return new MicroBombHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;

    }
}

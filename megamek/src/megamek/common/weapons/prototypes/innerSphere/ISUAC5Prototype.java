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

package megamek.common.weapons.prototypes.innerSphere;

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
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.weapons.autoCannons.UACWeapon;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.prototype.PrototypeISUltraWeaponHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Andrew Hunter
 * @since Oct 1, 2004
 */
public class ISUAC5Prototype extends UACWeapon {
    @Serial
    private static final long serialVersionUID = -2740269177146528640L;

    public ISUAC5Prototype() {
        super();
        name = "Prototype Ultra Autocannon/5";
        setInternalName("ISUltraAC5Prototype");
        addLookupName("IS Ultra AC/5 Prototype");
        shortName = "Ultra AC/5 (P)";
        flags = flags.or(F_PROTOTYPE);
        heat = 1;
        damage = 5;
        rackSize = 5;
        minimumRange = 2;
        shortRange = 6;
        mediumRange = 13;
        longRange = 20;
        extremeRange = 26;
        tonnage = 9.0;
        criticalSlots = 6;
        bv = 112;
        cost = 1000000;
        explosionDamage = damage;
        shortAV = 7;
        medAV = 7;
        longAV = 7;
        maxRange = RANGE_LONG;
        flags = flags.or(F_PROTOTYPE);
        rulesRefs = "98, IO:AE";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(3029, DATE_NONE, DATE_NONE, 3035, DATE_NONE)
              .setISApproximate(false, false, false, true, false)
              .setPrototypeFactions(Faction.FS)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.game.Game)
     */
    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        try {
            Mounted<?> weapon = game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId());
            if (weapon.curMode().equals("Ultra")) {
                return new PrototypeISUltraWeaponHandler(toHit, waa, game, manager);
            }
            return super.getCorrectHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }
}

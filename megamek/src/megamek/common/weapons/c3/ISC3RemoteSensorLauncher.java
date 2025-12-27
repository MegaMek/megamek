/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.c3;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.missiles.MissileWeapon;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public class ISC3RemoteSensorLauncher extends MissileWeapon {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -6850419038862085767L;

    /**
     *
     */
    public ISC3RemoteSensorLauncher() {
        super();
        name = "C3 Remote Sensor Launcher";
        setInternalName("ISC3RemoteSensorLauncher");
        addLookupName("C3RemoteSensorLauncher");
        flags = flags.or(F_NO_FIRES);
        ammoType = AmmoType.AmmoTypeEnum.C3_REMOTE_SENSOR;
        cost = 400000;
        criticalSlots = 3;
        tankSlots = 1;
        tonnage = 4;
        rackSize = 1;
        damage = 0;
        bv = 30;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        flags = flags.or(F_MEK_WEAPON).or(F_TANK_WEAPON);
        // suppveeslots = 3;
        rulesRefs = "110, TO:AUE";
        techAdvancement.setTechBase(TechBase.IS).setISAdvancement(3072, 3093).setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
    public AttackHandler getCorrectHandler(ToHitData toHit,
          WeaponAttackAction waa, Game game, TWGameManager manager) {
        return super.getCorrectHandler(toHit, waa, game, manager);
        // FIXME: Implement handler
    }
}

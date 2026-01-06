/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.weapons.handlers.ADAMissileWeaponHandler;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Martin Metke
 * @since Sep 12, 2023
 */

public abstract class ArrowIV extends ArtilleryWeapon {
    @Serial
    private static final long serialVersionUID = -4495524659692575107L;

    // Air-Defense Arrow IV (ADA) missile ranges differ from normal Arrow IV ammo
    public final int ADA_MIN_RANGE = 0;
    public final int ADA_SHORT_RANGE = 17;
    public final int ADA_MED_RANGE = 34;
    public final int ADA_LONG_RANGE = 51;
    public final int ADA_EXT_RANGE = 51;

    public ArrowIV() {
        super();

        name = "Arrow IV";
        setInternalName("ArrowIV");
        addLookupName("ArrowIVSystem");
        addLookupName("Arrow IV System");
        addLookupName("Arrow IV Missile System");
        heat = 10;
        rackSize = 20;
        ammoType = AmmoType.AmmoTypeEnum.ARROW_IV;
        bv = 240;
        cost = 450000;
        this.flags = flags.or(F_MISSILE);
        this.missileArmor = 20;
        rulesRefs = "96, TO:AUE";
    }

    @Override
    public int[] getRanges(Mounted<?> weapon) {
        // modify the ranges for Arrow missile systems based on the ammo selected
        int minRange = getMinimumRange();
        int sRange = getShortRange();
        int mRange = getMediumRange();
        int lRange = getLongRange();
        int eRange = getExtremeRange();
        boolean hasLoadedAmmo = (weapon.getLinked() != null);
        if (hasLoadedAmmo) {
            AmmoType ammoType = (AmmoType) weapon.getLinked().getType();
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ADA)) {
                minRange = ADA_MIN_RANGE;
                sRange = ADA_SHORT_RANGE;
                mRange = ADA_MED_RANGE;
                lRange = ADA_LONG_RANGE;
                eRange = ADA_EXT_RANGE;
            }
        }
        return new int[] { minRange, sRange, mRange, lRange, eRange };
    }

    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit,
          WeaponAttackAction waa, Game game, TWGameManager manager) {
        try {
            if (waa.getAmmoMunitionType().contains(AmmoType.Munitions.M_ADA)) {
                return new ADAMissileWeaponHandler(toHit, waa, game, manager);
            }
            return super.getCorrectHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }
}

/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.CLIATMHandler;
import megamek.common.weapons.missiles.MissileWeapon;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks, Modified by Greg
 */
public abstract class CLIATMWeapon extends MissileWeapon {

    @Serial
    private static final long serialVersionUID = 1L;

    public CLIATMWeapon() {
        super();
        ammoType = AmmoType.AmmoTypeEnum.IATM; // the Artemis Bonus is Tied to the ATM ammo, but I think I can ignore it in the
        // handler. However, I think I still need a new ammo type since I don't know if
        // the special ammo could get used with regular ATMs if I don#t change it. And I
        // assume bad things will happen.
        atClass = CLASS_ATM; // Do I need to change this? Streak LRMs still use the CLASS_LRM flag... I think
        // I can leave it.
        techAdvancement.setTechBase(TechBase.CLAN);
        techAdvancement.setClanAdvancement(3049, 3070);
        techAdvancement.setTechRating(TechRating.F);
        techAdvancement.setAvailability(AvailabilityValue.X,
              AvailabilityValue.X,
              AvailabilityValue.F,
              AvailabilityValue.E);

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
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game, TWGameManager manager) {
        try {
            // MML does different handlers here. I think I'll go with implementing different
            // ammo in the Handler.
            return new CLIATMHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return 0.3 * rackSize;
        } else if (range == AlphaStrikeElement.MEDIUM_RANGE) {
            return 0.2 * rackSize;
        } else {
            return 0.1 * rackSize;
        }
    }

    @Override
    public int getBattleForceClass() {
        return BF_CLASS_IATM;
    }

    @Override
    public boolean isAlphaStrikeIndirectFire() {
        return false;
    }

    @Override
    public boolean hasIndirectFire() {
        return true;
    }

    @Override
    public void adaptToGameOptions(IGameOptions gameOptions) {
        super.adaptToGameOptions(gameOptions);

        // Indirect Fire
        if (gameOptions.booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)) {
            addMode("");
            addMode("Indirect");
        } else {
            removeMode("");
            removeMode("Indirect");
        }
    }

    @Override
    public String getSortingName() {
        return "ATM IMP " + ((rackSize < 10) ? "0" + rackSize : rackSize);
    }

    /**
     * This is a streak weapon, so we use the rack size for the Aero damage.
     */
    protected double getBaseAeroDamage() {
        return 2 * this.getRackSize();
    }
}

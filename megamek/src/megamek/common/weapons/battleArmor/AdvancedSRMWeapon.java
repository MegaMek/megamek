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

package megamek.common.weapons.battleArmor;

import java.io.Serial;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.srm.AdvancedSRMHandler;
import megamek.common.weapons.srms.SRMWeapon;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public abstract class AdvancedSRMWeapon extends SRMWeapon {

    @Serial
    private static final long serialVersionUID = 8098857067349950771L;

    public AdvancedSRMWeapon() {
        super();
        this.ammoType = AmmoType.AmmoTypeEnum.SRM_ADVANCED;
        flags = flags.andNot(F_ARTEMIS_COMPATIBLE);
    }

    @Override
    public AttackHandler getCorrectHandler(ToHitData toHit,
          WeaponAttackAction waa, Game game, TWGameManager manager) {
        return new AdvancedSRMHandler(toHit, waa, game, manager);
    }

    /**
     * non-squad size version for AlphaStrike base damage
     */
    @Override
    public double getBattleForceDamage(int range) {
        if (range > getLongRange()) {
            return 0;
        }
        double damage = Compute.calculateClusterHitTableAmount(8, getRackSize()) * 2;
        if ((range == AlphaStrikeElement.SHORT_RANGE) && (getMinimumRange() > 0)) {
            damage = adjustBattleForceDamageForMinRange(damage);
        }
        return damage / 10.0;
    }

    @Override
    public double getBattleForceDamage(int range, int baSquadSize) {
        if (range > getLongRange()) {
            return 0;
        }
        double damage = Compute.calculateClusterHitTableAmount(8, getRackSize() * baSquadSize);
        if ((range == AlphaStrikeElement.SHORT_RANGE) && (getMinimumRange() > 0)) {
            damage = adjustBattleForceDamageForMinRange(damage);
        }
        return damage / 10.0;
    }

    @Override
    public int getBattleForceClass() {
        return BF_CLASS_STANDARD;
    }

    @Override
    public String getSortingName() {
        if (sortingName != null) {
            return sortingName;
        } else {
            String oneShotTag = hasFlag(F_ONE_SHOT) ? "OS " : "";
            if (name.contains("I-OS")) {
                oneShotTag = "OSI ";
            }
            return "SRM Z Advanced " + oneShotTag + rackSize;
        }
    }
}

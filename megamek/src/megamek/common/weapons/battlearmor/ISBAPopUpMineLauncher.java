/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.battlearmor;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.PopUpMineLauncherHandler;
import megamek.common.weapons.Weapon;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Andrew Hunter
 * @since Sep 24, 2004
 */
public class ISBAPopUpMineLauncher extends Weapon {
    private static final long serialVersionUID = -3445048091894801251L;

    public ISBAPopUpMineLauncher() {
        super();
        name = "Pop-up Mine";
        setInternalName(BattleArmor.MINE_LAUNCHER);
        addLookupName("ISMine Launcher");
        heat = 0;
        damage = DAMAGE_SPECIAL;
        rackSize = 1;
        ammoType = AmmoType.AmmoTypeEnum.MINE;
        shortRange = 0;
        mediumRange = 0;
        longRange = 0;
        extremeRange = 0;
        tonnage = 0.2;
        criticals = 1;
        cost = 2500;
        bv = 6;
        String[] modeStrings = { "Single", "2-shot", "3-shot", "4-shot", "5-shot", "6-shot" };
        setModes(modeStrings);
        flags = flags.or(F_DIRECT_FIRE).or(F_SOLO_ATTACK).or(F_BA_WEAPON).or(F_ONESHOT).or(F_BA_INDIVIDUAL);
        rulesRefs = "267, TM";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.F)
              .setISAdvancement(DATE_NONE, 3050, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.FS, Faction.LC);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     * megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        return new PopUpMineLauncherHandler(toHit, waa, game, manager);
    }
}

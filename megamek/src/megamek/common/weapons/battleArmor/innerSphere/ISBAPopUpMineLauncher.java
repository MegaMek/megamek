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

package megamek.common.weapons.battleArmor.innerSphere;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.PopUpMineLauncherHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Andrew Hunter
 * @since Sep 24, 2004
 */
public class ISBAPopUpMineLauncher extends Weapon {
    @Serial
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
        criticalSlots = 1;
        cost = 2500;
        bv = 6;
        String[] modeStrings = { "Single", "2-shot", "3-shot", "4-shot", "5-shot", "6-shot" };
        setModes(modeStrings);
        flags = flags.or(F_DIRECT_FIRE).or(F_SOLO_ATTACK).or(F_BA_WEAPON).or(F_ONE_SHOT).or(F_BA_INDIVIDUAL);
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
     * megamek.common.actions.WeaponAttackAction, megamek.common.game.Game,
     * megamek.server.Server)
     */
    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game, TWGameManager manager) {
        try {
            return new PopUpMineLauncherHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;

    }
}

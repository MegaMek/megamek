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

package megamek.common.weapons.lrms;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.IGameOptions;
import megamek.common.units.Entity;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.lrm.StreakLRMHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public abstract class StreakLRMWeapon extends LRMWeapon {

    @Serial
    private static final long serialVersionUID = -2552069184709782928L;

    public StreakLRMWeapon() {
        super();
        this.ammoType = AmmoType.AmmoTypeEnum.LRM_STREAK;
        flags = flags.or(F_PROTO_WEAPON).andNot(F_ARTEMIS_COMPATIBLE);
        clearModes();
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY).setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public double getTonnage(Entity entity, int location, double size) {
        if ((entity != null) && entity.hasETypeFlag(Entity.ETYPE_PROTOMEK)) {
            return getRackSize() * 0.4;
        } else {
            return super.getTonnage(entity, location, size);
        }
    }

    @Override
    // Streak LRMs could end up with an Indirect mode as a result of this game option selection. Overriding to 
    // prevent this. Bug report #7618
    public void adaptToGameOptions(IGameOptions gameOptions) {
        super.adaptToGameOptions(gameOptions);

        // Make sure Indirect Fire is not present for Streak LRMs
        clearModes();
    }

    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit,
          WeaponAttackAction waa, Game game, TWGameManager manager) {
        try {
            return new StreakLRMHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        return (range <= AlphaStrikeElement.LONG_RANGE) ? 0.1 * getRackSize() : 0;
    }

    @Override
    public int getBattleForceClass() {
        return BF_CLASS_STANDARD;
    }

    @Override
    public String getSortingName() {
        String oneShotTag = hasFlag(F_ONE_SHOT) ? "OS " : "";
        if (name.contains("I-OS")) {
            oneShotTag = "OSI ";
        }
        return "LRM STREAK " + oneShotTag + ((rackSize < 10) ? "0" + rackSize : rackSize);
    }

    @Override
    public boolean hasIndirectFire() {
        return false;
    }
}

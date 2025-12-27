/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2009-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.MekTaserHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Jason Tighe
 */
public class ISMekTaser extends AmmoWeapon {
    @Serial
    private static final long serialVersionUID = 4393086562754363816L;

    public ISMekTaser() {
        super();
        // CHECKSTYLE IGNORE ForbiddenWords FOR 4 LINES
        name = "BattleMech Taser";
        setInternalName("Mek Taser");
        addLookupName("ISMekTaser");
        addLookupName("ISBattleMechTaser");
        heat = 6;
        rackSize = 1;
        damage = 1;
        ammoType = AmmoType.AmmoTypeEnum.TASER;
        shortRange = 1;
        mediumRange = 2;
        longRange = 4;
        extremeRange = 6;
        bv = 40;
        toHitModifier = 1;
        cost = 200000;
        tonnage = 4;
        criticalSlots = 3;
        explosionDamage = 6;
        explosive = true;
        flags = flags.or(F_MEK_WEAPON).or(F_BALLISTIC).or(F_DIRECT_FIRE)
              .or(F_TASER).or(F_TANK_WEAPON);
        rulesRefs = "157, TO:AUE";
        techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3065, 3084)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        try {
            return new MekTaserHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;

    }

    @Override
    public double getBattleForceDamage(int range) {
        return (range == AlphaStrikeElement.SHORT_RANGE) ? 0.1 : 0;
    }
}

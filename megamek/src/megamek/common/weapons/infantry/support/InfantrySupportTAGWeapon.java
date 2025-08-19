/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.infantry.support;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.game.Game;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.TAGHandler;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.server.totalwarfare.TWGameManager;

/**
 * TAG for conventional infantry. Rules not found in TacOps 2nd printing are in this forum post:
 * http://bg.battletech.com/forums/index.php?topic=5902.0
 *
 * @author Neoancient
 */
public class InfantrySupportTAGWeapon extends InfantryWeapon {
    private static final long serialVersionUID = 4986981464279987117L;

    public InfantrySupportTAGWeapon() {
        super();
        flags = flags.andNot(F_MEK_WEAPON).or(F_INF_SUPPORT).or(F_TAG).or(F_NO_FIRES).or(F_INF_ENCUMBER);

        name = "TAG (Light, Man-Portable)";
        setInternalName(EquipmentTypeLookup.INFANTRY_TAG);
        addLookupName("Infantry TAG");
        damage = 0;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        cost = 40000;
        tonnage = .020;
        rulesRefs = "273, TM";
        techAdvancement.setTechBase(TechBase.ALL).setISAdvancement(2598, 2610, DATE_NONE, 2770, 3051)
              .setISApproximate(true, true, false, false, false)
              .setClanAdvancement(2598, 2610, DATE_NONE, DATE_NONE, 3051)
              .setClanApproximate(true, true, false, false, false).setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH).setReintroductionFactions(Faction.DC).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        return new TAGHandler(toHit, waa, game, manager);
    }
}

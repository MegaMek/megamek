/*
 * Copyright (c) 2017-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons.infantry;

import megamek.common.EquipmentTypeLookup;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.TAGHandler;
import megamek.server.Server;

/**
 * TAG for conventional infantry. Rules not found in TacOps 2nd printing are in
 * this forum post: http://bg.battletech.com/forums/index.php?topic=5902.0
 * 
 * @author Neoancient
 */
public class InfantrySupportTAGWeapon extends InfantryWeapon {
    private static final long serialVersionUID = 4986981464279987117L;

    public InfantrySupportTAGWeapon() {
        super();
        flags = flags.andNot(F_MECH_WEAPON).or(F_INF_SUPPORT).or(F_TAG).or(F_NO_FIRES).or(F_INF_ENCUMBER);

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
        techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2598, 2610, DATE_NONE, 2770, 3051)
                .setISApproximate(true, true, false, false, false)
                .setClanAdvancement(2598, 2610, DATE_NONE, DATE_NONE, 3051)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_X, RATING_E, RATING_E);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              Server server) {
        return new TAGHandler(toHit, waa, game, server);
    }
}

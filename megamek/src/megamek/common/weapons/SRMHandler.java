/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons;

import megamek.common.Compute;
import megamek.common.Game;
import megamek.common.Infantry;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class SRMHandler extends MissileWeaponHandler {
    private static final long serialVersionUID = -1618484541772117621L;

    public SRMHandler(ToHitData t, WeaponAttackAction w, Game g, Server s) {
        this(t, w, g, s, 0);
    }

    public SRMHandler(ToHitData t, WeaponAttackAction w, Game g, Server s, int salvoMod) {
        super(t, w, g, s);
        nSalvoBonus = salvoMod;
    }

    @Override
    protected int calcDamagePerHit() {
        if (target.isConventionalInfantry()) {
            double toReturn = Compute.directBlowInfantryDamage(
                    wtype.getRackSize() * 2, bDirect ? toHit.getMoS() / 3 : 0,
                    wtype.getInfantryDamageClass(),
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null, ae.getId(), calcDmgPerHitReport);
            
            toReturn = applyGlancingBlowModifier(toReturn, true);
            
            return (int) toReturn;
        }
        return 2;
    }

    @Override
    protected int calcnCluster() {
        return 1;
    }
}

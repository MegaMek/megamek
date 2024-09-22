/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.totalwarfare.TWGameManager;

import java.io.Serial;
import java.util.Vector;

/**
 * @author Martin Metke
 *
 * This class extends MissleWeaponHandler to ensure that Air-Defense Arrow IV
 * missiles, which act differently from most other missiles, are handled correctly.
 *
 * Specifically, ADA Missiles do a single hit of 20 damage to 1 location without
 * AE damage and without rolling on the cluster table.
 */
public class ADAMissileWeaponHandler extends MissileWeaponHandler {
    @Serial
    private static final long serialVersionUID = 6329291710822071023L;

    public ADAMissileWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) {
        super(t, w, g, m);
    }

    @Override
    protected int calcDamagePerHit() {
        return 20;
    }

    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        return 1;
    }

    @Override
    protected int calcnCluster() {
        return 1;
    }

    @Override
    protected boolean usesClusterTable(){
        return false;
    }
}

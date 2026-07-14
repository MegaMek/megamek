/*
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.handlers;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Infantry;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public class HAGWeaponHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = -8193801876308832102L;

    /**
     *
     */
    public HAGWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
        sSalvoType = " projectiles ";
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcnCluster()
     */
    @Override
    protected int calculateNumCluster() {
        return 5;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        if (target.isConventionalInfantry()) {
            double toReturn = weaponType.getRackSize();
            toReturn = Compute.directBlowInfantryDamage(
                  toReturn, bDirect ? toHit.getMoS() / 3 : 0,
                  weaponType.getInfantryDamageClass(),
                  ((Infantry) target).isMechanized(),
                  toHit.getThruBldg() != null, attackingEntity.getId(), calcDmgPerHitReport);
            toReturn = applyGlancingBlowModifier(toReturn, true);
            return (int) toReturn;
        }
        return 1;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs can't mount HAGs
        if (target.isConventionalInfantry()) {
            return 1;
        }
        int nHits;
        int nHitsModifier = getClusterModifiers(true);
        if (nRange <= weaponType.getShortRange()) {
            nHitsModifier += 2;
        } else if (nRange > weaponType.getMediumRange()) {
            nHitsModifier -= 2;
        }

        if (allShotsHit()) {
            nHits = weaponType.getRackSize();
        } else {
            nHits = Compute.missilesHit(weaponType.getRackSize(), nHitsModifier);
        }
        Report r = new Report(3325);
        r.subject = subjectId;
        r.add(nHits);
        r.add(sSalvoType);
        r.newlines = 0;
        r.add(toHit.getTableDesc());
        vPhaseReport.addElement(r);
        if (nHitsModifier != 0) {
            r = new Report(3340);
            if (nHitsModifier < 0) {
                r = new Report(3341);
            }
            r.subject = subjectId;
            r.add(nHitsModifier);
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        r = new Report(3345);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return nHits;
    }

    @Override
    protected boolean usesClusterTable() {
        return true;
    }
}

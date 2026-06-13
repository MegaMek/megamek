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

package megamek.common.weapons.handlers.srm;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.common.weapons.DamageType;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public class SRMAntiTSMHandler extends SRMSmokeWarheadHandler {
    @Serial
    private static final long serialVersionUID = 6380017303917455020L;

    public SRMAntiTSMHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
        sSalvoType = " anti-TSM missile(s) ";
        damageType = DamageType.ANTI_TSM;
    }

    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if (target.isConventionalInfantry()) {
            if (attackingEntity instanceof BattleArmor) {
                bSalvo = true;
                return ((BattleArmor) attackingEntity).getShootingStrength();
            }
            return 1;
        }
        int missilesHit;
        int nMissilesModifier = getClusterModifiers(true);

        // Add ams mod
        nMissilesModifier += getAMSHitsMod(vPhaseReport);

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)) {
            Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                  : null;
            if (entityTarget != null && entityTarget.isLargeCraft()) {
                nMissilesModifier -= (int) Math.floor(getAeroSanityAMSHitsMod());
            }
        }

        if (allShotsHit()) {
            // We want buildings and large craft to be able to affect this number with AMS
            // treat as a Streak launcher (cluster roll 11) to make this happen
            missilesHit = Compute.missilesHit(weaponType.getRackSize(),
                  nMissilesModifier, weapon.isHotLoaded(), true,
                  isAdvancedAMS());
        } else {
            // anti tsm hit with half the normal number, round up
            missilesHit = Compute.missilesHit(weaponType.getRackSize(),
                  nMissilesModifier, weapon.isHotLoaded(), false, isAdvancedAMS());
            missilesHit = (int) Math.ceil((double) missilesHit / 2);
        }
        Report r = new Report(3325);
        r.subject = subjectId;
        r.add(missilesHit);
        r.add(sSalvoType);
        r.add(toHit.getTableDesc());
        r.newlines = 0;
        vPhaseReport.addElement(r);
        if (nMissilesModifier != 0) {
            if (nMissilesModifier > 0) {
                r = new Report(3340);
            } else {
                r = new Report(3341);
            }
            r.subject = subjectId;
            r.add(nMissilesModifier);
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        r = new Report(3345);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return missilesHit;
    }
}

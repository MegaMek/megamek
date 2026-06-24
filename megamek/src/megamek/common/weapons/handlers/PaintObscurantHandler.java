/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.rolls.Roll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Resolves an attack by a Fluid Gun or Sprayer firing Paint/Obscurant Ammo (TO:AUE p.174). On a
 * successful hit against a non-infantry unit, a 2D6 roll of 9+ fouls the target's sensors, adding a +1
 * to-hit modifier to all of that unit's weapon attacks for the rest of the scenario (stacking to a
 * maximum of +3, unless washed off by Water Ammo). It has no effect on infantry and does no damage.
 *
 * @author The MegaMek Team
 */
public class PaintObscurantHandler extends AmmoWeaponHandler {
    private static final MMLogger LOGGER = MMLogger.create(PaintObscurantHandler.class);

    @Serial
    private static final long serialVersionUID = -3477345021151736742L;

    private static final int OBSCURANT_TARGET_NUMBER = 9;

    public PaintObscurantHandler(ToHitData toHit, WeaponAttackAction waa, Game game, TWGameManager manager)
          throws EntityLoadingException {
        super(toHit, waa, game, manager);
        generalDamageType = HitData.DAMAGE_BALLISTIC;
    }

    @Override
    protected int calcDamagePerHit() {
        // Paint/Obscurant Ammo inflicts no damage; its only effect is sensor fouling.
        return 0;
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport, IBuilding building,
          int hits, int nCluster, int bldgAbsorbs) {
        if (entityTarget.isConventionalInfantry()) {
            // Paint/Obscurant Ammo has no effect against infantry (TO:AUE p.174).
            return;
        }

        Roll diceRoll = Compute.rollD6(2);
        Report report = new Report(3393);
        report.subject = subjectId;
        report.addDesc(entityTarget);
        report.add(diceRoll);
        report.indent(2);
        vPhaseReport.add(report);

        if (diceRoll.getIntValue() < OBSCURANT_TARGET_NUMBER) {
            Report failure = new Report(3398);
            failure.subject = subjectId;
            failure.indent(3);
            vPhaseReport.add(failure);
            LOGGER.debug("[Fluid:Paint] {}: sensor-foul rolled {} vs {} -> no penalty applied",
                  entityTarget.getShortName(), diceRoll.getIntValue(), OBSCURANT_TARGET_NUMBER);
            return;
        }

        if (entityTarget.addObscurantToHitPenalty()) {
            Report applied = new Report(3394);
            applied.subject = subjectId;
            applied.addDesc(entityTarget);
            applied.add(entityTarget.getObscurantToHitPenalty());
            applied.indent(3);
            vPhaseReport.add(applied);
            LOGGER.debug("[Fluid:Paint] {}: sensor-foul rolled {} vs {} -> penalty applied, now +{} to-hit",
                  entityTarget.getShortName(), diceRoll.getIntValue(), OBSCURANT_TARGET_NUMBER,
                  entityTarget.getObscurantToHitPenalty());
        } else {
            Report maxed = new Report(3396);
            maxed.subject = subjectId;
            maxed.addDesc(entityTarget);
            maxed.indent(3);
            vPhaseReport.add(maxed);
            LOGGER.debug("[Fluid:Paint] {}: sensor-foul rolled {} vs {} -> already at maximum penalty +{}",
                  entityTarget.getShortName(), diceRoll.getIntValue(), OBSCURANT_TARGET_NUMBER,
                  entityTarget.getObscurantToHitPenalty());
        }
    }
}

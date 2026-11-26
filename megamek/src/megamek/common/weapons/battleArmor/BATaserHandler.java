/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.battleArmor;

import java.io.Serial;
import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.rolls.Roll;
import megamek.common.units.Entity;
import megamek.common.weapons.handlers.MekTaserHandler;
import megamek.server.totalWarfare.TWGameManager;

public class BATaserHandler extends MekTaserHandler {
    @Serial
    private static final long serialVersionUID = 1308895663099714573L;

    public BATaserHandler(ToHitData toHitData, WeaponAttackAction weaponAttackAction, Game game,
          TWGameManager twGameManager) throws EntityLoadingException {
        super(toHitData, weaponAttackAction, game, twGameManager);
    }

    @Override
    protected int calcHits(Vector<Report> reports) {
        // Only one trooper fires a taser per attack, including squad-mounted launchers (TO:AUE p.158).
        return 1;
    }

    @Override
    protected void applyEntityDamage(Entity entityTarget, int damage, Vector<Report> vPhaseReport) {
        BattleArmor squad = (BattleArmor) attackingEntity;
        int firer = weapon.getLocation() == BattleArmor.LOC_SQUAD ? squad.getRandomTrooper() : weapon.getLocation();
        super.applyEntityDamage(entityTarget, damage, vPhaseReport);
        // Feedback is a separate roll on a successful hit regardless of the target effect (TO:AUE p.158).
        Roll diceRoll2 = Compute.rollD6(2);
        Report report = new Report(3715);
        report.addDesc(attackingEntity);
        report.add(diceRoll2);
        report.newlines = 0;
        report.indent(2);
        vPhaseReport.add(report);
        if (diceRoll2.getIntValue() >= 7) {
            report = new Report(3720);
            vPhaseReport.add(report);
            // +1 to-hit for 3 turns
            attackingEntity.setTaserFeedback(3);
        } else {
            report = new Report(3725);
            vPhaseReport.add(report);
            if (firer > BattleArmor.LOC_SQUAD && squad.isTrooperActive(firer)) {
                squad.destroyLocation(firer);
                if (squad.getTransferLocation(new HitData(firer)).getLocation() == Entity.LOC_DESTROYED) {
                    vPhaseReport.addAll(gameManager.destroyEntity(squad, "all troopers eliminated", true));
                }
            }
        }
    }
}

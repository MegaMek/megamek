/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.handlers;

import java.io.Serial;
import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.FighterSquadron;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Jay Lawson
 */
public class ScreenLauncherBayHandler extends AmmoBayWeaponHandler {

    @Serial
    private static final long serialVersionUID = -1618484541772117621L;

    /**
     *
     */
    public ScreenLauncherBayHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
    }

    /**
     * Screen Launchers always deal 15 damage per launcher. Override parent's calcAttackValue which sums all weapons in
     * bay. Each launcher in the bay fires independently in the handle() loop.
     */
    @Override
    protected int calcAttackValue() {
        return 15;
    }

    /**
     * handle this weapons firing
     *
     * @return a <code>boolean</code> value indicating whether this should be kept or not
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!this.cares(phase)) {
            return true;
        }

        // Calculate attack value (damage) - must be done before applying damage
        attackValue = calcAttackValue();

        // same as ScreenLauncher handler, except run multiple times depending
        // on
        // how many screen launchers in bay

        // Report weapon attack and its to-hit value.
        Report r = new Report(3115);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        r.add(weaponType.getName());
        r.messageId = 3120;
        r.add(target.getDisplayName(), true);
        vPhaseReport.addElement(r);
        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(3135);
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
            return false;
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
            r = new Report(3140);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            r = new Report(3145);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        }

        addHeat();

        // iterate through by number of weapons in bay
        for (int i = 0; i < weapon.getBayWeapons().size(); i++) {
            // deliver screen
            Coords coords = target.getPosition();
            gameManager.deliverScreen(coords, vPhaseReport);
            // damage any entities in the hex
            for (Entity entity : game.getEntitiesVector(coords)) {
                // if fighter squadron all fighters are damaged
                if (entity instanceof FighterSquadron) {
                    // Squadron: each fighter takes a single hit (standard-scale damage)
                    entity.getSubEntities().forEach(
                          ent -> {
                              // Per TM p.237: damage rolled on Nose column of Hit Location Table
                              HitData hit = ent.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
                              // Standard-scale damage - will be divided by 10 for capital-scale targets
                              hit.setCapital(false);
                              vPhaseReport.addAll(gameManager.damageEntity(ent, hit, attackValue));
                              gameManager.creditKill(ent, attackingEntity);
                          });
                } else if (entity.isCapitalScale() || entity.isLargeCraft()) {
                    // Capital-scale targets (capital fighters) or large craft (DropShips, etc.): single hit
                    // Capital fighters take 15 -> 2 damage after capital-scale conversion
                    // DropShips have standard-scale armor so take full 15 damage
                    // Per TM p.237: damage rolled on Nose column of Hit Location Table
                    HitData hit = entity.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
                    // Standard-scale damage - will be divided by 10 for capital-scale targets
                    hit.setCapital(false);
                    vPhaseReport.addAll(gameManager.damageEntity(entity, hit, attackValue));
                    gameManager.creditKill(entity, attackingEntity);
                } else {
                    // Standard-scale small craft (individual non-capital fighters): 5-point clusters
                    // See: https://battletech.com/forums/index.php?topic=77239
                    int remainingDamage = attackValue;
                    while (remainingDamage > 0) {
                        int clusterDamage = Math.min(5, remainingDamage);
                        // Per TM p.237: damage rolled on Nose column of Hit Location Table
                        HitData hit = entity.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
                        hit.setCapital(false);
                        vPhaseReport.addAll(gameManager.damageEntity(entity, hit, clusterDamage));
                        remainingDamage -= clusterDamage;
                    }
                    gameManager.creditKill(entity, attackingEntity);
                }
            }
        }
        return false;
    }

}

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
public class ScreenLauncherHandler extends AmmoWeaponHandler {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -2536312899803153911L;

    /**
     *
     */
    public ScreenLauncherHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
    }

    /**
     * Screen Launchers always deal 15 damage.
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

        // deliver screen
        Coords coords = target.getPosition();
        gameManager.deliverScreen(coords, vPhaseReport);

        // damage any entities in the hex
        for (Entity entity : game.getEntitiesVector(coords)) {
            // if fighter squadron all fighters are damaged
            if (entity instanceof FighterSquadron) {
                entity.getSubEntities().forEach(
                      ent -> {
                          // Add summary report for this fighter
                          Report summary = new Report(6175);
                          summary.subject = ent.getId();
                          summary.indent(2);
                          summary.addDesc(ent);
                          summary.add(attackValue);
                          vPhaseReport.addElement(summary);

                          ToHitData squadronToHit = new ToHitData();
                          squadronToHit.setHitTable(ToHitData.HIT_NORMAL);
                          HitData hit = ent.rollHitLocation(squadronToHit.getHitTable(), ToHitData.SIDE_FRONT);
                          hit.setCapital(false);
                          vPhaseReport.addAll(gameManager.damageEntity(ent, hit, attackValue));
                          gameManager.creditKill(ent, attackingEntity);
                      });
            } else {
                // Add summary report for this entity
                Report summary = new Report(6175);
                summary.subject = entity.getId();
                summary.indent(2);
                summary.addDesc(entity);
                summary.add(attackValue);
                vPhaseReport.addElement(summary);

                ToHitData hexToHit = new ToHitData();
                hexToHit.setHitTable(ToHitData.HIT_NORMAL);

                if (entity.isLargeCraft()) {
                    // Large craft (500+ tons): single hit
                    HitData hit = entity.rollHitLocation(hexToHit.getHitTable(), ToHitData.SIDE_FRONT);
                    hit.setCapital(false);
                    vPhaseReport.addAll(gameManager.damageEntity(entity, hit, attackValue));
                } else {
                    // Individual craft: 5-point clusters per official ruling
                    // See: https://battletech.com/forums/index.php?topic=77239
                    int clusterSize = 5;
                    int remainingDamage = attackValue;
                    while (remainingDamage > 0) {
                        int clusterDamage = Math.min(clusterSize, remainingDamage);
                        HitData hit = entity.rollHitLocation(hexToHit.getHitTable(), ToHitData.SIDE_FRONT);
                        hit.setCapital(false);
                        vPhaseReport.addAll(gameManager.damageEntity(entity, hit, clusterDamage));
                        remainingDamage -= clusterDamage;
                    }
                }
                gameManager.creditKill(entity, attackingEntity);
            }
        }
        return false;
    }

}

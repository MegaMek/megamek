/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2026 The MegaMek Team. All Rights Reserved.
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

import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 23, 2004
 */
public class FireExtinguisherHandler extends WeaponHandler {
    private static final MMLogger LOGGER = MMLogger.create(FireExtinguisherHandler.class);

    @Serial
    private static final long serialVersionUID = -7047033962986081773L;

    public FireExtinguisherHandler(ToHitData toHit, WeaponAttackAction waa, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(toHit, waa, g, m);
    }

    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport, Entity entityTarget) {
        if (!bMissed) {
            Report r = new Report(2270);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.add(r);
            if (Targetable.TYPE_HEX_EXTINGUISH == target.getTargetType()) {
                r = new Report(3540);
                r.subject = subjectId;
                r.add(target.getPosition().getBoardNum());
                r.indent(3);
                vPhaseReport.add(r);
                int boardId = target.getBoardId();
                Board extinguishedBoard = game.getBoard(boardId);
                Hex extinguishedHex = (extinguishedBoard == null) ? null
                      : extinguishedBoard.getHex(target.getPosition());
                // A corrupted or out-of-sync action could carry a stale boardId/coords; guard against it so
                // resolution never crashes the phase with an NPE.
                if (extinguishedHex == null) {
                    LOGGER.warn("[Firefight] cannot extinguish hex {} on board {}: {} - skipping terrain update",
                          target.getPosition(), boardId,
                          (extinguishedBoard == null) ? "board not found" : "hex not found");
                } else {
                    extinguishedHex.removeTerrain(Terrains.FIRE);
                    // Reset the fire-turn counter so the hex is treated as unburned. Without this it keeps a
                    // stale value, and a fire later restarted here (e.g. by a flamer) is mistaken for an
                    // already-burning fire - skipping the "fire started" report and spreading immediately.
                    extinguishedHex.resetFireTurn();
                    gameManager.sendChangedHex(target.getPosition(), boardId);
                    extinguishedBoard.removeInfernoFrom(target.getPosition());
                    extinguishedBoard.removeFlamerStartedFire(target.getPosition());
                }
            } else if (target instanceof Entity) {
                if (entityTarget.infernos.isStillBurning()
                      || (target instanceof Tank && ((Tank) target).isOnFire())) {
                    r = new Report(3550);
                    r.subject = subjectId;
                    r.addDesc(entityTarget);
                    r.indent(3);
                    vPhaseReport.add(r);
                }
                entityTarget.infernos.clear();
                if (target instanceof Tank) {
                    for (int i = 0; i < entityTarget.locations(); i++) {
                        ((Tank) target).extinguishAll();
                    }
                }
            }
        }
        // Firefighting engineers that keep battling the same hex get a cumulative bonus next turn,
        // whether or not this attempt put the fire out (TO:AuE p.153). Record the attempt either way.
        // isFirefighter() implies ConvInfantry, which records the consecutive-turn streak.
        if (attackingEntity.isFirefighter() && (Targetable.TYPE_HEX_EXTINGUISH == target.getTargetType())
              && (attackingEntity instanceof ConvInfantry firefighter)) {
            firefighter.recordFirefight(target.getPosition(), game.getRoundCount());
        }
        return true;
    }
}

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

import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.units.FluidCoating;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Resolves an attack by a Fluid Gun or Sprayer firing Flame-Retardant Foam Ammo (TO:AUE p.173). Foam
 * immediately douses all fires on the struck hex, structure or unit - including those created by Inferno
 * munitions - and coats the struck hex so that later rolls to set it on fire take a +4 modifier. It does
 * no damage.
 *
 * @author The MegaMek Team
 */
public class FoamHandler extends AmmoWeaponHandler {
    private static final MMLogger LOGGER = MMLogger.create(FoamHandler.class);

    @Serial
    private static final long serialVersionUID = -1632498387764828124L;

    public FoamHandler(ToHitData toHit, WeaponAttackAction waa, Game game, TWGameManager manager)
          throws EntityLoadingException {
        super(toHit, waa, game, manager);
    }

    @Override
    protected int calcDamagePerHit() {
        // Flame-Retardant Foam inflicts no damage; it only douses fires and coats the target.
        return 0;
    }

    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport, Entity entityTarget) {
        if (!bMissed) {
            Report hit = new Report(2270);
            hit.subject = subjectId;
            hit.newlines = 0;
            vPhaseReport.add(hit);

            if ((Targetable.TYPE_HEX_EXTINGUISH == target.getTargetType())
                  || (Targetable.TYPE_HEX_FLUID == target.getTargetType())) {
                // Foam douses any fire in the hex (whether the player aimed to extinguish or just to coat it),
                // then coats the hex so later ignition rolls there take +4 (TO:AUE p.173).
                extinguishHex(vPhaseReport);
                applyFoam(vPhaseReport);
            } else if (entityTarget != null) {
                // Foam aimed at a unit douses its fires and coats the unit, so ignition rolls against it
                // take +4 wherever it goes (TO:AUE p.173) rather than pooling on the ground.
                extinguishUnit(entityTarget, vPhaseReport);
                applyFoamToUnit(entityTarget, vPhaseReport);
            }
        }
        return true;
    }

    private void applyFoamToUnit(Entity entityTarget, Vector<Report> vPhaseReport) {
        entityTarget.setFluidCoating(FluidCoating.FLAME_RETARDANT_FOAM);
        LOGGER.debug("[Fluid:Foam] foam-coated {}", entityTarget.getShortName());

        Report report = new Report(3404);
        report.subject = subjectId;
        report.addDesc(entityTarget);
        report.indent(3);
        vPhaseReport.add(report);
    }

    private void extinguishHex(Vector<Report> vPhaseReport) {
        int boardId = target.getBoardId();
        Board board = game.getBoard(boardId);
        Hex hex = (board == null) ? null : board.getHex(target.getPosition());
        if (hex == null) {
            LOGGER.warn("[Foam] cannot extinguish hex {} on board {} - skipping terrain update",
                  target.getPosition(), boardId);
            return;
        }
        if (!hex.containsTerrain(Terrains.FIRE)) {
            // Nothing to put out (e.g. foam aimed at a bare hex to coat it); just coat it afterwards.
            return;
        }
        Report report = new Report(3540);
        report.subject = subjectId;
        report.add(target.getPosition().getBoardNum());
        report.indent(3);
        vPhaseReport.add(report);

        // Foam puts out every fire immediately, including Inferno-fuelled ones (TO:AUE p.173).
        hex.removeTerrain(Terrains.FIRE);
        hex.resetFireTurn();
        gameManager.sendChangedHex(target.getPosition(), boardId);
        board.removeInfernoFrom(target.getPosition());
        board.removeFlamerStartedFire(target.getPosition());
        FluidFireSuppression.clearBuildingFire(game, gameManager, target.getPosition(), boardId, subjectId);
        LOGGER.debug("[Fluid:Foam] extinguished fire in hex {}", target.getPosition().getBoardNum());
    }

    private void extinguishUnit(Entity entityTarget, Vector<Report> vPhaseReport) {
        if (entityTarget.infernos.isStillBurning()
              || ((target instanceof Tank tank) && tank.isOnFire())) {
            Report report = new Report(3550);
            report.subject = subjectId;
            report.addDesc(entityTarget);
            report.indent(3);
            vPhaseReport.add(report);
        }
        // Foam douses all fires on the unit immediately, including Inferno gel (TO:AUE p.173).
        entityTarget.infernos.clear();
        if (target instanceof Tank tank) {
            tank.extinguishAll();
        }
        LOGGER.debug("[Fluid:Foam] doused all fires on {}", entityTarget.getShortName());
    }

    private void applyFoam(Vector<Report> vPhaseReport) {
        Coords coords = target.getPosition();
        Board board = game.getBoard(target.getBoardId());
        if ((coords == null) || (board == null) || !board.contains(coords)) {
            return;
        }
        board.markFlameRetardantFoam(coords);
        gameManager.sendChangedHex(coords, target.getBoardId());
        LOGGER.debug("[Fluid:Foam] foam-coated hex {}", coords.getBoardNum());

        Report report = new Report(3388);
        report.subject = subjectId;
        report.add(coords.getBoardNum());
        report.indent(3);
        vPhaseReport.add(report);
    }
}

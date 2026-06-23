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
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.units.FluidCoating;
import megamek.common.units.IBuilding;
import megamek.common.units.Targetable;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Resolves an attack by a Fluid Gun or Sprayer firing Oil Slick Ammo (TO:AUE p.174). Oil Slick inflicts
 * no damage; instead it douses the struck hex, which then forces ground units (other than infantry,
 * hovercraft and WiGEs) into a skid check when passing through and makes the hex easier to set alight.
 *
 * @author The MegaMek Team
 */
public class OilSlickHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = 4751095925582942501L;

    public OilSlickHandler(ToHitData toHit, WeaponAttackAction waa, Game game, TWGameManager manager)
          throws EntityLoadingException {
        super(toHit, waa, game, manager);
        generalDamageType = HitData.DAMAGE_BALLISTIC;
    }

    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport, Entity entityTarget) {
        if (!bMissed && (target.getTargetType() == Targetable.TYPE_HEX_FLUID)) {
            // Oil Slick fired at a bare hex coats that hex (TO:AUE p.174).
            applyOilSlick(target.getPosition(), target.getBoardId(), vPhaseReport);
            return true;
        }
        return false;
    }

    @Override
    protected int calcDamagePerHit() {
        // Oil Slick Ammo does no damage; it only douses the hex (TO:AUE p.174).
        return 0;
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport, IBuilding building,
          int hits, int nCluster, int bldgAbsorbs) {
        // Oil Slick fired at a unit coats that unit, so it is easier to set alight wherever it goes
        // (TO:AUE p.174); the slick travels with the unit rather than pooling on the ground.
        entityTarget.setFluidCoating(FluidCoating.OIL_SLICK);

        Report report = new Report(3382);
        report.subject = subjectId;
        report.addDesc(entityTarget);
        report.indent(2);
        vPhaseReport.addElement(report);
    }

    @Override
    protected void handleIgnitionDamage(Vector<Report> vPhaseReport, IBuilding bldg, int hits) {
        applyOilSlick(target.getPosition(), target.getBoardId(), vPhaseReport);
    }

    @Override
    protected void handleClearDamage(Vector<Report> vPhaseReport, IBuilding bldg, int nDamage) {
        applyOilSlick(target.getPosition(), target.getBoardId(), vPhaseReport);
    }

    private void applyOilSlick(Coords coords, int boardId, Vector<Report> vPhaseReport) {
        Board board = game.getBoard(boardId);
        if ((coords == null) || (board == null) || !board.contains(coords)) {
            return;
        }
        board.markOilSlick(coords);
        gameManager.sendChangedHex(coords, boardId);

        Report report = new Report(3387);
        report.subject = subjectId;
        report.add(coords.getBoardNum());
        report.indent(2);
        vPhaseReport.addElement(report);
    }
}

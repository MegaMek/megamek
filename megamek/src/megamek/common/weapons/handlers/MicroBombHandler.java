/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

import megamek.common.HexTarget;
import megamek.common.Messages;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.SpecialHexDisplay;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.enums.BombType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 23, 2004
 */
public class MicroBombHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = -2995118961278208244L;

    /**
     *
     */
    public MicroBombHandler(ToHitData toHit, WeaponAttackAction waa, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(toHit, waa, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.handlers.WeaponHandler#specialResolution(java.util.Vector,
     * megamek.common.units.Entity, boolean)
     */
    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport, Entity entityTarget) {
        Coords coords = target.getPosition();
        Player player = attackingEntity.getOwner();
        String bombMsg;

        if (!bMissed) {
            Report r = new Report(3190);
            r.subject = subjectId;
            r.add(coords.getBoardNum());
            vPhaseReport.add(r);

            bombMsg = "Bomb hit here on round " + game.getRoundCount()
                  + ", dropped by " + ((player != null) ? player.getName() : "somebody");

            game.getBoard(target).addSpecialHexDisplay(coords,
                  new SpecialHexDisplay(SpecialHexDisplay.Type.BOMB_HIT, game.getRoundCount(),
                        player, bombMsg));

        } else {
            // magic number - BA-launched micro bombs only scatter 1 hex per TW-2018 p 228
            coords = Compute.scatter(coords, 1);
            if (game.getBoard().contains(coords)) {
                Report report = new Report(3195);
                report.subject = subjectId;
                report.add(coords.getBoardNum());
                vPhaseReport.add(report);

                bombMsg = "Bomb missed!  Round " + game.getRoundCount()
                      + ", by " + ((player != null) ? player.getName() : "somebody") + ", drifted to "
                      + coords.getBoardNum();
                game.getBoard(target).addSpecialHexDisplay(coords,
                      new SpecialHexDisplay(SpecialHexDisplay.Type.BOMB_MISS, game.getRoundCount(),
                            player, bombMsg));

            } else {
                Report report = new Report(3200);
                report.subject = subjectId;
                vPhaseReport.add(report);

                bombMsg = "Bomb missed!  Round " + game.getRoundCount()
                      + ", by " + ((player != null) ? player.getName() : "somebody")
                      + ", drifted off the board";
                game.getBoard(target).addSpecialHexDisplay(coords,
                      new SpecialHexDisplay(SpecialHexDisplay.Type.BOMB_MISS, game.getRoundCount(),
                            player, bombMsg));

                return !bMissed;
            }
        }

        // Deal bomb damage to the hit coordinates, with all that entails
        HexTarget dropHex = new HexTarget(coords, target.getBoardId(), target.getTargetType());
        Vector<Integer> hitIds = gameManager.deliverBombDamage(dropHex,
              BombType.BombTypeEnum.HE, subjectId, weaponEntity,
              vPhaseReport);

        // Display drifts that hit nothing separately from drifts that dealt damage
        if (bMissed) {
            if (hitIds == null || hitIds.isEmpty()) {
                game.getBoard(target).addSpecialHexDisplay(coords,
                      new SpecialHexDisplay(SpecialHexDisplay.Type.BOMB_DRIFT, game.getRoundCount(),
                            player, Messages.getString("BombMessage.drifted")
                            + " " + coords.getBoardNum()));
            } else {
                game.getBoard(target).addSpecialHexDisplay(coords,
                      new SpecialHexDisplay(SpecialHexDisplay.Type.BOMB_HIT, game.getRoundCount(),
                            player, Messages.getString("BombMessage.drifted")
                            + " " + coords.getBoardNum()));
            }
        }
        return true;
    }
}

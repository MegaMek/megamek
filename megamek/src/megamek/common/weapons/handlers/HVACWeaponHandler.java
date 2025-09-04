/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
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

import megamek.common.CriticalSlot;
import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.Terrains;
import megamek.common.weapons.handlers.ac.ACWeaponHandler;
import megamek.server.SmokeCloud;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Jason Tighe
 */
public class HVACWeaponHandler extends ACWeaponHandler {
    @Serial
    private static final long serialVersionUID = 7326881584091651519L;

    public HVACWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
    }

    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        PlanetaryConditions conditions = game.getPlanetaryConditions();
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_START_FIRE)
              && !conditions.getAtmosphere().isVacuum()) {
            int rear = (attackingEntity.getFacing() + 3 + (weapon.isMekTurretMounted() ? weapon.getFacing() : 0)) % 6;
            Coords src = attackingEntity.getPosition();
            Coords rearCoords = src.translated(rear);
            Board board = game.getBoard(attackingEntity);
            Hex currentHex = board.getHex(src);

            if (!board.contains(rearCoords)) {
                rearCoords = src;
            } else if (board.getHex(rearCoords).getLevel() > currentHex.getLevel()) {
                rearCoords = src;
            } else if ((board.getBuildingAt(rearCoords) != null)
                  && ((board.getHex(rearCoords).terrainLevel(
                  Terrains.BLDG_ELEV)
                  + board.getHex(rearCoords)
                  .getLevel()) > currentHex.getLevel())) {
                rearCoords = src;
            }

            gameManager.createSmoke(rearCoords, board, SmokeCloud.SMOKE_HEAVY, 2);
        }
        return super.handle(phase, vPhaseReport);
    }

    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (doAmmoFeedProblemCheck(vPhaseReport)) {
            return true;
        }

        if ((roll.getIntValue() == 2) && !attackingEntity.isConventionalInfantry()) {
            Report report = new Report(3162);
            report.subject = subjectId;
            weapon.setJammed(true);
            weapon.setHit(true);
            int weaponLocation = weapon.getLocation();
            for (int i = 0; i < attackingEntity.getNumberOfCriticalSlots(weaponLocation); i++) {
                CriticalSlot slot1 = attackingEntity.getCritical(weaponLocation, i);
                if ((slot1 == null) ||
                      (slot1.getType() == CriticalSlot.TYPE_SYSTEM)) {
                    continue;
                }
                Mounted<?> mounted = slot1.getMount();
                if (mounted.equals(weapon)) {
                    attackingEntity.hitAllCriticalSlots(weaponLocation, i);
                    break;
                }
            }
            vPhaseReport.addAll(gameManager.explodeEquipment(attackingEntity, weaponLocation, weapon));
            report.choose(false);
            vPhaseReport.addElement(report);
            return true;
        } else {
            return super.doChecks(vPhaseReport);
        }
    }
}

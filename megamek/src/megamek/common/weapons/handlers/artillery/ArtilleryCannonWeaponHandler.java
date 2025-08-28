/*

 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2010-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.handlers.artillery;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.NukeDetonatedAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Minefield;
import megamek.common.equipment.Mounted;
import megamek.common.event.player.GamePlayerStrategicActionEvent;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.common.weapons.handlers.AmmoWeaponHandler;
import megamek.common.weapons.handlers.AreaEffectHelper;
import megamek.logging.MMLogger;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Numien, based work by Sebastian Brocks
 */
public class ArtilleryCannonWeaponHandler extends AmmoWeaponHandler {
    private static final MMLogger LOGGER = MMLogger.create(ArtilleryCannonWeaponHandler.class);

    @Serial
    private static final long serialVersionUID = 1L;
    boolean handledAmmoAndReport = false;

    public ArtilleryCannonWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
    }

    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }
        if (attackingEntity == null) {
            LOGGER.error("Artillery Entity is null!");
            return true;
        }

        Coords targetPos = target.getPosition();
        Hex targetHex = game.getHexOf(target);
        boolean targetIsEntity = target.getTargetType() == Targetable.TYPE_ENTITY;
        boolean isFlak = targetIsEntity && Compute.isFlakAttack(attackingEntity, (Entity) target);
        boolean asfFlak = isFlak && target.isAirborne();
        Mounted<?> ammoUsed = attackingEntity.getEquipment(weaponAttackAction.getAmmoId());
        final AmmoType ammoType = (ammoUsed == null) ? null : (AmmoType) ammoUsed.getType();

        // Report weapon attack and its to-hit value.
        Report report = new Report(3120);
        report.indent();
        report.newlines = 0;
        report.subject = subjectId;
        if (weaponType != null) {
            report.add(weaponType.getName());
        } else {
            report.add("Error: From Nowhere");
        }

        report.add(target.getDisplayName(), true);
        vPhaseReport.addElement(report);
        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            report = new Report(3135);
            report.subject = subjectId;
            report.add(toHit.getDesc());
            vPhaseReport.addElement(report);
            return false;
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
            report = new Report(3140);
            report.newlines = 0;
            report.subject = subjectId;
            report.add(toHit.getDesc());
            vPhaseReport.addElement(report);
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            report = new Report(3145);
            report.newlines = 0;
            report.subject = subjectId;
            report.add(toHit.getDesc());
            vPhaseReport.addElement(report);
        } else {
            // roll to hit
            report = new Report(3150);
            report.newlines = 0;
            report.subject = subjectId;
            report.add(toHit);
            vPhaseReport.addElement(report);
        }

        // dice have been rolled, thanks
        report = new Report(3155);
        report.newlines = 0;
        report.subject = subjectId;
        report.add(roll);
        vPhaseReport.addElement(report);

        // do we hit?
        bMissed = roll.getIntValue() < toHit.getValue();
        // Set Margin of Success/Failure.
        toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));

        // Do this stuff first, because some weapon's miss report reference the
        // amount of shots fired and stuff.
        if (!handledAmmoAndReport) {
            addHeat();
        }
        if (!bMissed) {
            if (!isFlak) {
                report = new Report(3190);
            } else {
                report = new Report(3191);
            }
            report.subject = subjectId;
            report.add(targetPos.getBoardNum());
            vPhaseReport.addElement(report);
        } else {
            Board board = game.getBoard(target);
            if (!board.isSpace()) {
                targetPos = Compute.scatter(targetPos, (Math.abs(toHit.getMoS()) + 1) / 2);
                if (board.contains(targetPos)) {
                    // misses and scatters to another hex
                    if (!isFlak) {
                        report = new Report(3195);
                    } else {
                        report = new Report(3192);
                    }
                    report.subject = subjectId;
                    report.add(targetPos.getBoardNum());
                    vPhaseReport.addElement(report);
                } else {
                    // misses and scatters off-board
                    if (isFlak) {
                        report = new Report(3193);
                    } else {
                        report = new Report(3200);
                    }
                    report.subject = subjectId;
                    vPhaseReport.addElement(report);
                    return !bMissed;
                }
            } else {
                // No scattering in space
                report = new Report(3196);
                report.subject = subjectId;
                report.add(targetPos.getBoardNum());
                vPhaseReport.addElement(report);
                return !bMissed;
            }
        }

        int height = ((targetHex != null) ? targetHex.getLevel() : 0);
        if (asfFlak) {
            height = target.getAltitude();
        } else if (isFlak) {
            height += target.getElevation();
        }

        // According to TacOps errata, artillery cannons can only fire standard
        // rounds and fuel-air cannon shells (Interstellar Ops p165).
        // But, they're still in as unofficial tech, because they're fun. :)
        if (null != ammoType) {
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_FLARE)) {
                int radius;
                if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.LONG_TOM) {
                    radius = 3;
                } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.SNIPER) {
                    radius = 2;
                } else {
                    radius = 1;
                }
                gameManager.deliverArtilleryFlare(targetPos, radius);
                return false;
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_DAVY_CROCKETT_M)) {
                gameManager.drawNukeHitOnBoard(targetPos);
                gameManager.getGame().processGameEvent(
                      new GamePlayerStrategicActionEvent(gameManager,
                            new NukeDetonatedAction(attackingEntity.getId(),
                                  attackingEntity.getOwnerId(),
                                  AmmoType.Munitions.M_DAVY_CROCKETT_M)));
                gameManager.doNuclearExplosion(targetPos, 1, vPhaseReport);
                return false;
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_FASCAM)) {
                gameManager.deliverFASCAMMinefield(targetPos, attackingEntity.getOwner().getId(),
                      ammoType.getRackSize(), attackingEntity.getId());
                return false;
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_SMOKE)) {
                gameManager.deliverArtillerySmoke(targetPos, vPhaseReport);
                return false;
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_FAE)) {
                // Currently Artillery Cannons _can_ make Flak attacks using FAE munitions
                // If this is an ASF Flak attack we know we hit an entity by itself in the air,
                // so just hit it for full damage.
                if (asfFlak) {
                    AreaEffectHelper.artilleryDamageEntity((Entity) target,
                          ammoType.getRackSize(),
                          null,
                          0,
                          false,
                          true,
                          isFlak,
                          height,
                          targetPos,
                          this.ammoType,
                          targetPos,
                          false,
                          attackingEntity,
                          null,
                          getAttackerId(),
                          vPhaseReport,
                          gameManager);
                } else {
                    AreaEffectHelper.processFuelAirDamage(targetPos,
                          target.getBoardId(),
                          height,
                          ammoType,
                          attackingEntity,
                          vPhaseReport,
                          gameManager);
                }

                return false;
            }
        }

        // check to see if this is a mine clearing attack
        // According to the RAW you have to hit the right hex to hit even if the
        // scatter hex has minefields
        // TODO: Does this apply to arty cannons?
        boolean mineClear = target.getTargetType() == Targetable.TYPE_MINEFIELD_CLEAR;
        if (mineClear && !isFlak && !bMissed) {
            report = new Report(3255);
            report.indent(1);
            report.subject = subjectId;
            vPhaseReport.addElement(report);

            AreaEffectHelper.clearMineFields(targetPos, Minefield.CLEAR_NUMBER_WEAPON,
                  attackingEntity, vPhaseReport, game,
                  gameManager);
        }

        gameManager.artilleryDamageArea(targetPos, ammoType,
              subjectId, attackingEntity, isFlak, height, mineClear, vPhaseReport,
              asfFlak);

        // artillery may unintentionally clear minefields, but only if it wasn't trying
        // to
        // TODO : Does this apply to arty cannons?
        if (!mineClear) {
            AreaEffectHelper.clearMineFields(targetPos, Minefield.CLEAR_NUMBER_WEAPON_ACCIDENT,
                  attackingEntity, vPhaseReport, game,
                  gameManager);
        }

        return false;
    }

    @Override
    protected int calcDamagePerHit() {
        double toReturn = weaponType.getDamage();
        // area effect damage is double
        if (target.isConventionalInfantry()) {
            toReturn /= 0.5;
        }

        toReturn = applyGlancingBlowModifier(toReturn, target.isConventionalInfantry());

        return (int) Math.ceil(toReturn);
    }
}

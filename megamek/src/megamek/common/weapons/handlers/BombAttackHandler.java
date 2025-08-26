/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2009-2025 The MegaMek Team. All Rights Reserved.
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
import java.util.List;
import java.util.Map;
import java.util.Vector;

import megamek.common.HexTarget;
import megamek.common.HitData;
import megamek.common.Messages;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.SpecialHexDisplay;
import megamek.common.SpecialHexDisplay.Type;
import megamek.common.TagInfo;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.BombType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.IBomber;
import megamek.common.units.Targetable;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Jay Lawson
 * @since Sep 23, 2004
 */
public class BombAttackHandler extends WeaponHandler {
    @Serial
    private static final long serialVersionUID = -2997052348538688888L;

    /**
     *
     */
    public BombAttackHandler(ToHitData toHit, WeaponAttackAction waa, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(toHit, waa, g, m);
        generalDamageType = HitData.DAMAGE_NONE;
    }

    /**
     * Does this attack use the cluster hit table? necessary to determine how Aero damage should be applied
     */
    @Override
    protected boolean usesClusterTable() {
        return true;
    }

    @Override
    protected void useAmmo() {
        BombLoadout payload = weaponAttackAction.getBombPayload();
        if (!attackingEntity.isBomber() || (null == payload)) {
            return;
        }
        for (Map.Entry<BombTypeEnum, Integer> entry : payload.entrySet()) {
            BombTypeEnum bombType = entry.getKey();
            int bombCount = entry.getValue();
            for (int i = 0; i < bombCount; i++) {
                // find the first mounted bomb of this type and drop it
                for (Mounted<?> bomb : attackingEntity.getBombs()) {
                    if (!bomb.isDestroyed()
                          && (bomb.getUsableShotsLeft() > 0)
                          && (((BombType) bomb.getType()).getBombType() == bombType)) {
                        bomb.setShotsLeft(0);
                        if (bomb.isInternalBomb()) {
                            ((IBomber) attackingEntity).increaseUsedInternalBombs(1);
                        }
                        break;
                    }
                }
            }
        }
        super.useAmmo();
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.AttackHandler#handle(int, java.util.Vector)
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        BombLoadout payload = weaponAttackAction.getBombPayload();
        Coords coords = target.getPosition();
        Coords drop;

        Entity entity = game.getEntity(weaponAttackAction.getEntityId());

        if (entity == null) {
            return false;
        }

        Player player = entity.getOwner();
        String bombMsg;
        Vector<Integer> hitIds = null;

        // now go through the payload and drop the bombs one at a time
        for (Map.Entry<BombTypeEnum, Integer> entry : payload.entrySet()) {
            BombTypeEnum type = entry.getKey();
            int bombCount = entry.getValue();

            if (bombCount <= 0) {continue;}
            // to hit, adjusted for bomb-type specific rules
            ToHitData typeModifiedToHit = new ToHitData();
            typeModifiedToHit.append(toHit);
            typeModifiedToHit.setHitTable(toHit.getHitTable());
            typeModifiedToHit.setSideTable(toHit.getSideTable());

            // currently, only type of bomb with type-specific to-hit mods
            // Laser-Guided Bombs are getting errata to get bonus from either A) a tagged
            // hex or B) a tagged target
            boolean laserGuided = false;
            if (type == BombTypeEnum.LG) {
                for (TagInfo ti : game.getTagInfo()) {
                    if (ti.missed || entity.isEnemyOf(game.getEntity(ti.attackerId))) {
                        // Not a usable friendly TAG
                        continue;
                    }
                    if (target.getId() == ti.target.getId()
                          || ((ti.targetType != Targetable.TYPE_HEX_TAG)
                          && target.getPosition().equals(ti.target.getPosition()))) {
                        typeModifiedToHit.addModifier(-2,
                              "laser-guided bomb against tagged target");
                        laserGuided = true;
                        break;
                    }
                }
            }

            for (int i = 0; i < bombCount; i++) {
                // Report weapon attack and its to-hit value.
                Report report;

                if (laserGuided) {
                    report = new Report(3433);
                    report.indent();
                    report.newlines = 1;
                    report.subject = subjectId;
                    vPhaseReport.addElement(report);
                }

                report = new Report(3120);
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
                if (typeModifiedToHit.getValue() == TargetRoll.IMPOSSIBLE) {
                    report = new Report(3135);
                    report.subject = subjectId;
                    report.add(typeModifiedToHit.getDesc());
                    vPhaseReport.addElement(report);
                    return false;
                } else if (typeModifiedToHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
                    report = new Report(3140);
                    report.newlines = 0;
                    report.subject = subjectId;
                    report.add(typeModifiedToHit.getDesc());
                    vPhaseReport.addElement(report);
                } else if (typeModifiedToHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
                    report = new Report(3145);
                    report.newlines = 0;
                    report.subject = subjectId;
                    report.add(typeModifiedToHit.getDesc());
                    vPhaseReport.addElement(report);
                } else {
                    // roll to hit
                    report = new Report(3150);
                    report.newlines = 0;
                    report.subject = subjectId;
                    report.add(typeModifiedToHit);
                    vPhaseReport.addElement(report);
                }

                // dice have been rolled, thanks
                report = new Report(3155);
                report.newlines = 0;
                report.subject = subjectId;
                report.add(roll);
                vPhaseReport.addElement(report);

                // do we hit?
                bMissed = roll.getIntValue() < typeModifiedToHit.getValue();
                // Set Margin of Success/Failure.
                typeModifiedToHit.setMoS(roll.getIntValue() - Math.max(2, typeModifiedToHit.getValue()));

                if (!bMissed) {
                    report = new Report(3190);
                } else {
                    report = new Report(3196);
                }
                report.subject = subjectId;
                report.add(coords.getBoardNum());
                vPhaseReport.addElement(report);

                drop = coords;
                // each bomb can scatter a different direction
                if (!bMissed) {
                    report = new Report(6697);
                    report.indent(1);
                    report.add(type.getDisplayName());
                    report.subject = subjectId;
                    report.newlines = 1;
                    vPhaseReport.add(report);

                    bombMsg = "Bomb hit here on round " + game.getRoundCount()
                          + ", dropped by " + ((player != null) ? player.getName() : "somebody");
                    game.getBoard(target).addSpecialHexDisplay(coords,
                          new SpecialHexDisplay(Type.BOMB_HIT, game.getRoundCount(),
                                player, bombMsg));
                } else {
                    int moF = -typeModifiedToHit.getMoS();
                    if (attackingEntity.hasAbility(OptionsConstants.GUNNERY_GOLDEN_GOOSE)) {
                        if ((-typeModifiedToHit.getMoS() - 2) < 1) {
                            moF = 0;
                        } else {
                            moF = -typeModifiedToHit.getMoS() - 2;
                        }
                    }
                    if (weaponType.hasFlag(WeaponType.F_ALT_BOMB)) {
                        // Need to determine location in flight path
                        int idx = 0;
                        for (; idx < attackingEntity.getPassedThrough().size(); idx++) {
                            if (attackingEntity.getPassedThrough().get(idx).equals(coords)) {
                                break;
                            }
                        }
                        // Retrieve facing at current step in flight path
                        int facing = attackingEntity.getPassedThroughFacing().get(idx);
                        // Scatter, based on location and facing
                        drop = Compute.scatterAltitudeBombs(coords, facing, moF);
                    } else {
                        drop = Compute.scatterDiveBombs(coords, moF);
                    }

                    if (game.getBoard(target).contains(drop)) {
                        // misses and scatters to another hex
                        report = new Report(6698);
                        report.indent(1);
                        report.subject = subjectId;
                        report.newlines = 1;
                        report.add(type.getDisplayName());
                        report.add(drop.getBoardNum());
                        vPhaseReport.addElement(report);
                        bombMsg = "Bomb missed!  Round " + game.getRoundCount()
                              + ", by " + ((player != null) ? player.getName() : "somebody") + ", drifted to "
                              + drop.getBoardNum();
                        game.getBoard(target).addSpecialHexDisplay(coords,
                              new SpecialHexDisplay(Type.BOMB_MISS, game.getRoundCount(),
                                    player, bombMsg));
                    } else {
                        // misses and scatters off-board
                        report = new Report(6699);
                        report.indent(1);
                        report.subject = subjectId;
                        report.newlines = 1;
                        report.add(type.getDisplayName());
                        vPhaseReport.addElement(report);
                        bombMsg = "Bomb missed!  Round " + game.getRoundCount()
                              + ", by " + ((player != null) ? player.getName() : "somebody")
                              + ", drifted off the board";
                        game.getBoard(target).addSpecialHexDisplay(coords,
                              new SpecialHexDisplay(Type.BOMB_MISS, game.getRoundCount(),
                                    player, bombMsg));
                        continue;
                    }
                }
                // Capture drop hex info
                HexTarget dropHex = new HexTarget(drop, target.getBoardId(), target.getTargetType());
                dropHex.setTargetLevel(((HexTarget) target).getTargetLevel());

                if (type == BombTypeEnum.INFERNO) {
                    hitIds = gameManager.deliverBombInferno(drop, attackingEntity, subjectId, vPhaseReport);
                } else if (type == BombTypeEnum.THUNDER) {
                    gameManager.deliverThunderMinefield(drop,
                          attackingEntity.getOwner().getId(),
                          20,
                          attackingEntity.getId());
                    List<Coords> hexes = drop.allAdjacent();
                    for (Coords c : hexes) {
                        gameManager.deliverThunderMinefield(c,
                              attackingEntity.getOwner().getId(),
                              20,
                              attackingEntity.getId());
                    }
                } else {
                    // We want to make this a HexTarget so we can ensure drifts happen at the same elevation
                    // (Currently this is not working correctly because we don't pass targetLevel over the wire)
                    hitIds = gameManager.deliverBombDamage(dropHex, type, subjectId, attackingEntity, vPhaseReport);
                }

                // Display drifts that hit nothing separately from drifts that dealt damage
                if (bMissed) {
                    if (hitIds == null || hitIds.isEmpty()) {
                        game.getBoard(target).addSpecialHexDisplay(drop,
                              new SpecialHexDisplay(Type.BOMB_DRIFT, game.getRoundCount(),
                                    player, Messages.getString("BombMessage.drifted")
                                    + " " + coords.getBoardNum()));
                    } else {
                        game.getBoard(target).addSpecialHexDisplay(drop,
                              new SpecialHexDisplay(Type.BOMB_HIT, game.getRoundCount(),
                                    player, Messages.getString("BombMessage.drifted")
                                    + " " + coords.getBoardNum()));
                    }
                }

                // Finally, we need a new attack roll for the next bomb, if any.
                roll = Compute.rollD6(2);
            }
        }

        return false;
    }
}

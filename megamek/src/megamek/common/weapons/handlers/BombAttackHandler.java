/*
  Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.IBomber;
import megamek.common.units.Targetable;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Jay Lawson
 * @since Sep 23, 2004
 */
public class BombAttackHandler extends WeaponHandler {
    private static final long serialVersionUID = -2997052348538688888L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public BombAttackHandler(ToHitData toHit, WeaponAttackAction waa, Game g,
          TWGameManager m) {
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
        BombLoadout payload = waa.getBombPayload();
        if (!ae.isBomber() || (null == payload)) {
            return;
        }
        for (Map.Entry<BombTypeEnum, Integer> entry : payload.entrySet()) {
            BombTypeEnum bombType = entry.getKey();
            int bombCount = entry.getValue();
            for (int i = 0; i < bombCount; i++) {
                // find the first mounted bomb of this type and drop it
                for (Mounted<?> bomb : ae.getBombs()) {
                    if (!bomb.isDestroyed()
                          && (bomb.getUsableShotsLeft() > 0)
                          && (((BombType) bomb.getType()).getBombType() == bombType)) {
                        bomb.setShotsLeft(0);
                        if (bomb.isInternalBomb()) {
                            ((IBomber) ae).increaseUsedInternalBombs(1);
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
        BombLoadout payload = waa.getBombPayload();
        Coords coords = target.getPosition();
        Coords drop;
        Player player = game.getEntity(waa.getEntityId()).getOwner();
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
            // Laser-Guided Bombs are getting errata'ed to get bonus from either A) a tagged
            // hex or B) a tagged target
            boolean laserGuided = false;
            if (type == BombTypeEnum.LG) {
                for (TagInfo ti : game.getTagInfo()) {
                    if (ti.missed || game.getEntity(waa.getEntityId()).isEnemyOf(game.getEntity(ti.attackerId))) {
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
                Report r;

                if (laserGuided) {
                    r = new Report(3433);
                    r.indent();
                    r.newlines = 1;
                    r.subject = subjectId;
                    vPhaseReport.addElement(r);
                }

                r = new Report(3120);
                r.indent();
                r.newlines = 0;
                r.subject = subjectId;
                if (wtype != null) {
                    r.add(wtype.getName());
                } else {
                    r.add("Error: From Nowhere");
                }

                r.add(target.getDisplayName(), true);
                vPhaseReport.addElement(r);
                if (typeModifiedToHit.getValue() == TargetRoll.IMPOSSIBLE) {
                    r = new Report(3135);
                    r.subject = subjectId;
                    r.add(typeModifiedToHit.getDesc());
                    vPhaseReport.addElement(r);
                    return false;
                } else if (typeModifiedToHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
                    r = new Report(3140);
                    r.newlines = 0;
                    r.subject = subjectId;
                    r.add(typeModifiedToHit.getDesc());
                    vPhaseReport.addElement(r);
                } else if (typeModifiedToHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
                    r = new Report(3145);
                    r.newlines = 0;
                    r.subject = subjectId;
                    r.add(typeModifiedToHit.getDesc());
                    vPhaseReport.addElement(r);
                } else {
                    // roll to hit
                    r = new Report(3150);
                    r.newlines = 0;
                    r.subject = subjectId;
                    r.add(typeModifiedToHit);
                    vPhaseReport.addElement(r);
                }

                // dice have been rolled, thanks
                r = new Report(3155);
                r.newlines = 0;
                r.subject = subjectId;
                r.add(roll);
                vPhaseReport.addElement(r);

                // do we hit?
                bMissed = roll.getIntValue() < typeModifiedToHit.getValue();
                // Set Margin of Success/Failure.
                typeModifiedToHit.setMoS(roll.getIntValue() - Math.max(2, typeModifiedToHit.getValue()));

                if (!bMissed) {
                    r = new Report(3190);
                    r.subject = subjectId;
                    r.add(coords.getBoardNum());
                    vPhaseReport.addElement(r);
                } else {
                    r = new Report(3196);
                    r.subject = subjectId;
                    r.add(coords.getBoardNum());
                    vPhaseReport.addElement(r);
                }

                drop = coords;
                // each bomb can scatter a different direction
                if (!bMissed) {
                    r = new Report(6697);
                    r.indent(1);
                    r.add(type.getDisplayName());
                    r.subject = subjectId;
                    r.newlines = 1;
                    vPhaseReport.add(r);

                    bombMsg = "Bomb hit here on round " + game.getRoundCount()
                          + ", dropped by " + ((player != null) ? player.getName() : "somebody");
                    game.getBoard(target).addSpecialHexDisplay(coords,
                          new SpecialHexDisplay(Type.BOMB_HIT, game.getRoundCount(),
                                player, bombMsg));
                } else {
                    int moF = -typeModifiedToHit.getMoS();
                    if (ae.hasAbility(OptionsConstants.GUNNERY_GOLDEN_GOOSE)) {
                        if ((-typeModifiedToHit.getMoS() - 2) < 1) {
                            moF = 0;
                        } else {
                            moF = -typeModifiedToHit.getMoS() - 2;
                        }
                    }
                    if (wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
                        // Need to determine location in flight path
                        int idx = 0;
                        for (; idx < ae.getPassedThrough().size(); idx++) {
                            if (ae.getPassedThrough().get(idx).equals(coords)) {
                                break;
                            }
                        }
                        // Retrieve facing at current step in flight path
                        int facing = ae.getPassedThroughFacing().get(idx);
                        // Scatter, based on location and facing
                        drop = Compute.scatterAltitudeBombs(coords, facing, moF);
                    } else {
                        drop = Compute.scatterDiveBombs(coords, moF);
                    }

                    if (game.getBoard(target).contains(drop)) {
                        // misses and scatters to another hex
                        r = new Report(6698);
                        r.indent(1);
                        r.subject = subjectId;
                        r.newlines = 1;
                        r.add(type.getDisplayName());
                        r.add(drop.getBoardNum());
                        vPhaseReport.addElement(r);
                        bombMsg = "Bomb missed!  Round " + game.getRoundCount()
                              + ", by " + ((player != null) ? player.getName() : "somebody") + ", drifted to "
                              + drop.getBoardNum();
                        game.getBoard(target).addSpecialHexDisplay(coords,
                              new SpecialHexDisplay(Type.BOMB_MISS, game.getRoundCount(),
                                    player, bombMsg));
                    } else {
                        // misses and scatters off-board
                        r = new Report(6699);
                        r.indent(1);
                        r.subject = subjectId;
                        r.newlines = 1;
                        r.add(type.getDisplayName());
                        vPhaseReport.addElement(r);
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
                    hitIds = gameManager.deliverBombInferno(drop, ae, subjectId, vPhaseReport);
                } else if (type == BombTypeEnum.THUNDER) {
                    gameManager.deliverThunderMinefield(drop, ae.getOwner().getId(), 20, ae.getId());
                    List<Coords> hexes = drop.allAdjacent();
                    for (Coords c : hexes) {
                        gameManager.deliverThunderMinefield(c, ae.getOwner().getId(), 20, ae.getId());
                    }
                } else {
                    // We want to make this a HexTarget so we can ensure drifts happen at the same elevation
                    // (Currently this is not working correctly because we don't pass targetLevel over the wire)
                    hitIds = gameManager.deliverBombDamage(dropHex, type, subjectId, ae, vPhaseReport);
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

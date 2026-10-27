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
package megamek.server.nuclear;

import java.util.Vector;

import megamek.common.CriticalSlot;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.rolls.Roll;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.common.units.Terrains;
import megamek.common.weapons.DamageType;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Applies the secondary effects of a nuclear detonation to the units around Ground Zero: the roll on the Secondary
 * Nuclear Effects Table and the electromagnetic interference that follows (IO:AE pp. 165-166).
 */
public class NuclearSecondaryEffects {
    private final TWGameManager gameManager;
    private final Game game;

    /**
     * @param gameManager the game manager running the game the detonation happens in
     */
    public NuclearSecondaryEffects(TWGameManager gameManager) {
        this.gameManager = gameManager;
        this.game = gameManager.getGame();
    }

    /**
     * Handles secondary effects from nuclear blasts against all units in range.
     *
     * @param entity   The entity to affect.
     * @param position The coordinates of the nuclear blast, for to-hit directions.
     * @param vDesc    a description vector to use for reports.
     */
    public void apply(Entity entity, Coords position, Vector<Report> vDesc) {
        // If it's already destroyed, give up. We really don't care.
        if (entity.isDestroyed()) {
            return;
        }

        // Check to see if the infantry is in a protective structure.
        boolean inHardenedBuilding = (Compute.isInBuilding(game, entity) &&
              (game.getBoard()
                    .getHex(entity.getPosition())
                    .terrainLevel(Terrains.BUILDING) == 4));

        // Roll 2d6.
        Roll diceRoll = Compute.rollD6(2);
        int rollValue = diceRoll.getIntValue();

        Report r = new Report(6555);
        r.subject = entity.getId();
        r.add(entity.getDisplayName());
        r.add(diceRoll);

        // If they are in protective structure, add 2 to the roll.
        if (inHardenedBuilding) {
            rollValue += 2;
            r.add(" + 2 (unit is in hardened building)");
        } else {
            r.add("");
        }

        // Also, if the entity is "hardened" against EMI, it gets a +2.
        // For these purposes, I'm going to hand this off to the Entity itself
        // to tell us.
        // Right now, it IS based purely on class, but I won't rule out the idea
        // of
        // "nuclear hardening" as equipment for a support vehicle, for example.
        if (entity.isNuclearHardened()) {
            rollValue += 2;
            r.add(" + 2 (unit is hardened against EMI)");
        } else {
            r.add("");
        }

        r.indent(2);
        vDesc.add(r);

        // Now, compare it to the table, and apply the effects.
        if (rollValue <= 4) {
            // The unit is destroyed.
            // Sucks, doesn't it?
            // This applies to all units.
            // Yup, just sucks.
            vDesc.addAll(gameManager.destroyEntity(entity, "nuclear explosion secondary effects", false, false));
            // Kill the crew
            entity.getCrew().setDoomed(true);
        } else if (rollValue <= 6) {
            if (entity instanceof BattleArmor myBA) {
                // It takes 50% casualties, rounded up.
                int numDeaths = (int) (Math.ceil((myBA.getNumberActiveTroopers()) / 2.0));
                for (int x = 0; x < numDeaths; x++) {
                    vDesc.addAll(gameManager.applyCriticalHit(entity, 0, null, false, 0, false));
                }
            } else if (entity instanceof Infantry) {
                // Standard infantry are auto-killed in this band, unless
                // they're in a building.
                if (game.getBoard().getHex(entity.getPosition()).containsTerrain(Terrains.BUILDING)) {
                    // 50% casualties, rounded up.
                    int damage = (int) (Math.ceil((entity.getInternal(ConvInfantry.LOC_INFANTRY)) / 2.0));
                    vDesc.addAll(gameManager.damageEntity(entity, new HitData(ConvInfantry.LOC_INFANTRY), damage, true,
                          DamageType.NONE, false));
                } else {
                    vDesc.addAll(gameManager.destroyEntity(entity, "nuclear explosion secondary effects", false, false));
                    entity.getCrew().setDoomed(true);
                }
            } else if (entity instanceof Tank) {
                // All vehicles suffer two critical hits...
                HitData hd = entity.rollHitLocation(ToHitData.HIT_NORMAL, entity.sideTable(position));
                vDesc.addAll(gameManager.oneCriticalEntity(entity, hd.getLocation(), hd.isRear(), 0));
                hd = entity.rollHitLocation(ToHitData.HIT_NORMAL, entity.sideTable(position));
                vDesc.addAll(gameManager.oneCriticalEntity(entity, hd.getLocation(), hd.isRear(), 0));

                // ...and a Crew Killed hit.
                vDesc.addAll(gameManager.applyCriticalHit(entity, 0, new CriticalSlot(0, Tank.CRIT_CREW_KILLED), false, 0, false));
            } else if ((entity instanceof Mek) || (entity instanceof ProtoMek)) {
                // 'Meks suffer two critical hits...
                HitData hd = entity.rollHitLocation(ToHitData.HIT_NORMAL, entity.sideTable(position));
                vDesc.addAll(gameManager.oneCriticalEntity(entity, hd.getLocation(), hd.isRear(), 0));
                hd = entity.rollHitLocation(ToHitData.HIT_NORMAL, entity.sideTable(position));
                vDesc.addAll(gameManager.oneCriticalEntity(entity, hd.getLocation(), hd.isRear(), 0));

                // and four pilot hits.
                vDesc.addAll(gameManager.damageCrew(entity, 4));
            }
            // Buildings and gun emplacements and such are only affected by the EMI.
            // No auto-crits or anything.
        } else if (rollValue <= 10) {
            if (entity instanceof BattleArmor myBA) {
                // It takes 25% casualties, rounded up.
                int numDeaths = (int) (Math.ceil(((myBA.getNumberActiveTroopers())) / 4.0));
                for (int x = 0; x < numDeaths; x++) {
                    vDesc.addAll(gameManager.applyCriticalHit(entity, 0, null, false, 0, false));
                }
            } else if (entity instanceof Infantry) {
                if (game.getBoard().getHex(entity.getPosition()).containsTerrain(Terrains.BUILDING)) {
                    // 25% casualties, rounded up.
                    int damage = (int) (Math.ceil((entity.getInternal(ConvInfantry.LOC_INFANTRY)) / 4.0));
                    vDesc.addAll(gameManager.damageEntity(entity, new HitData(ConvInfantry.LOC_INFANTRY), damage, true,
                          DamageType.NONE, false));
                } else {
                    // 50% casualties, rounded up.
                    int damage = (int) (Math.ceil((entity.getInternal(ConvInfantry.LOC_INFANTRY)) / 2.0));
                    vDesc.addAll(gameManager.damageEntity(entity, new HitData(ConvInfantry.LOC_INFANTRY), damage, true,
                          DamageType.NONE, false));
                }
            } else if (entity instanceof Tank) {
                // It takes one crit...
                HitData hd = entity.rollHitLocation(ToHitData.HIT_NORMAL, entity.sideTable(position));
                vDesc.addAll(gameManager.oneCriticalEntity(entity, hd.getLocation(), hd.isRear(), 0));

                // Plus a Crew Stunned critical.
                vDesc.addAll(gameManager.applyCriticalHit(entity, 0, new CriticalSlot(0, Tank.CRIT_CREW_STUNNED), false, 0, false));
            } else if ((entity instanceof Mek) || (entity instanceof ProtoMek)) {
                // 'Meks suffer a critical hit...
                HitData hd = entity.rollHitLocation(ToHitData.HIT_NORMAL, entity.sideTable(position));
                vDesc.addAll(gameManager.oneCriticalEntity(entity, hd.getLocation(), hd.isRear(), 0));

                // and two pilot hits.
                vDesc.addAll(gameManager.damageCrew(entity, 2));
            }
            // Buildings and gun emplacements and such are only affected by
            // the EMI.
            // No auto-crits or anything.
        }
        // If it's 11+, there are no secondary effects beyond EMI.
        // Lucky bastards.

        // And lastly, the unit is now affected by electromagnetic interference.
        entity.setEMI(true);
    }
}

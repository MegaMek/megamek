/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */
package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;
import megamek.client.bot.common.StructOfUnitArrays;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.UnitRole;

import java.util.List;

import static megamek.codeUtilities.MathUtility.clamp01;
import static megamek.common.UnitRole.AMBUSHER;
import static megamek.common.UnitRole.BRAWLER;
import static megamek.common.UnitRole.JUGGERNAUT;
import static megamek.common.UnitRole.MISSILE_BOAT;
import static megamek.common.UnitRole.SCOUT;
import static megamek.common.UnitRole.SKIRMISHER;
import static megamek.common.UnitRole.SNIPER;
import static megamek.common.UnitRole.STRIKER;


/**
 * Calculates the unit role
 * @author Luana Coppio
 */
public class UnitRoleBehaviorCalculator extends BaseAxisCalculator {

    private static final UnitTmmCalculator unitTmmCalculator = new UnitTmmCalculator();
    private static final DamageRatioCalculator damageRatioCalculator = new DamageRatioCalculator();

    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the unit role
        float[] unitRole = axis();
        Entity unit = pathing.getEntity();

        float roleScore = switch (unit.getRole()) {
            case AMBUSHER -> ambusherScore(pathing, gameState);
            case SKIRMISHER -> skirmisherScore(pathing, gameState);
            case BRAWLER -> brawlerScore(pathing, gameState);
            case SCOUT -> scoutScore(pathing, gameState);
            case MISSILE_BOAT -> missileBoatScore(pathing, gameState);
            case JUGGERNAUT -> juggernautScore(pathing, gameState);
            case STRIKER -> strikerScore(pathing, gameState);
            case SNIPER -> sniperScore(pathing, gameState);
            default -> 0.5f;
        };

        unitRole[0] = roleScore;

        return unitRole;
    }

    private float ambusherScore(Pathing pathing, GameState gameState) {
        float enemies = 1 / (this.enemiesCloseBy(pathing, gameState.getEnemyUnitsSOU()) + 1f);
        float closestEnemy = distanceToClosestEnemy(pathing, gameState.getEnemyUnitsSOU());
        float toDivideBy = Math.min(7, pathing.getEntity().getMaxWeaponRange());
        return 0.7f * enemies * ((1 - (closestEnemy / toDivideBy)) * 0.3f);
    }

    private float skirmisherScore(Pathing pathing, GameState gameState) {
        float tmm = unitTmmCalculator.calculateAxis(pathing, gameState)[0];
        float damageRatio = damageRatioCalculator.calculateAxis(pathing, gameState)[0];
        return tmm * 0.6f + damageRatio * 0.4f;
    }

    private float brawlerScore(Pathing pathing, GameState gameState) {
        return 3 / (enemiesCloseBy(pathing, gameState.getEnemyUnitsSOU()) + 1f);
    }

    private float scoutScore(Pathing pathing, GameState gameState) {
        float roleScore = 1 / (enemiesCloseBy(pathing, gameState.getEnemyUnitsSOU()) + 1f);
        roleScore *= unitTmmCalculator.calculateAxis(pathing, gameState)[0];
        return roleScore;
    }

    private float missileBoatScore(Pathing pathing, GameState gameState) {
        float distance = distanceToClosestEnemy(pathing, gameState.getEnemyUnitsSOU());
        float maxRange = pathing.getEntity().getMaxWeaponRange();
        return 1 - (Math.abs(distance - maxRange) / maxRange);
    }

    private float juggernautScore(Pathing pathing, GameState gameState) {
        float distance = distanceToClosestEnemy(pathing, gameState.getEnemyUnitsSOU());
        return clamp01(1 - (distance / 7f));
    }

    private float strikerScore(Pathing pathing, GameState gameState) {
        float distance = distanceToClosestEnemy(pathing, gameState.getEnemyUnitsSOU());
        return clamp01(1 - (distance / 7f));
    }

    private float sniperScore(Pathing pathing, GameState gameState) {
        float maxRange = pathing.getEntity().getMaxWeaponRange();
        float distance = distanceToClosestEnemy(pathing, gameState.getEnemyUnitsSOU());
        return clamp01(1 - (Math.abs(distance - (maxRange * 0.7f)) / (maxRange * 0.3f)));
    }

    private int distanceToClosestEnemy(Pathing pathing, StructOfUnitArrays structOfUnitArrays) {
        int xd;
        int yd;
        int x = pathing.getFinalCoords().getX();
        int y = pathing.getFinalCoords().getY();
        int length = structOfUnitArrays.size();
        int dist;
        int closestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < length; i++) {
            xd = structOfUnitArrays.getX(i);
            yd = structOfUnitArrays.getY(i);
            dist = Coords.distance(x, y, xd, yd);
            if (dist < closestDistance) {
                closestDistance = dist;
            }
        }
        return closestDistance;
    }

    private int enemiesCloseBy(Pathing pathing, StructOfUnitArrays structOfUnitArrays) {
        int xd;
        int yd;
        int x = pathing.getFinalCoords().getX();
        int y = pathing.getFinalCoords().getY();
        int length = structOfUnitArrays.size();
        int enemyRange;
        double dist;
        int units = 0;

        for (int i = 0; i < length; i++) {
            xd = structOfUnitArrays.getX(i);
            yd = structOfUnitArrays.getY(i);
            enemyRange = structOfUnitArrays.getMaxWeaponRange(i);
            dist = Coords.distance(x, y, xd, yd);
            if (dist <= enemyRange) {
                units++;
            }
        }
        return units;
    }
}

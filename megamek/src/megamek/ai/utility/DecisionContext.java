/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.ai.utility;

import java.util.*;


public abstract class DecisionContext<IN_GAME_OBJECT, TARGETABLE> {

    private final World<IN_GAME_OBJECT, TARGETABLE> world;
    private final IN_GAME_OBJECT currentUnit;
    private final List<TARGETABLE> targetUnits;
    protected final Map<String, Double> damageCache;
    protected final static int DAMAGE_CACHE_SIZE = 10_000;

    public DecisionContext(World<IN_GAME_OBJECT, TARGETABLE> world) {
        this(world, null, Collections.emptyList());
    }

    public DecisionContext(World<IN_GAME_OBJECT, TARGETABLE> world, IN_GAME_OBJECT currentUnit) {
        this(world, currentUnit, Collections.emptyList());
    }

    public DecisionContext(World<IN_GAME_OBJECT, TARGETABLE> world, IN_GAME_OBJECT currentUnit, List<TARGETABLE> targetUnits, Map<String, Double> damageCache) {
        this.world = world;
        this.currentUnit = currentUnit;
        this.targetUnits = targetUnits;
        this.damageCache = damageCache;
    }

    public DecisionContext(World<IN_GAME_OBJECT, TARGETABLE> world, IN_GAME_OBJECT currentUnit, List<TARGETABLE> targetUnits) {
        this.world = world;
        this.currentUnit = currentUnit;
        this.targetUnits = targetUnits;
        this.damageCache = new LinkedHashMap<>(32, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Double> eldest) {
                return size() > DAMAGE_CACHE_SIZE;
            }
        };
    }

    public World<IN_GAME_OBJECT, TARGETABLE> getWorld() {
        return world;
    }

    public List<TARGETABLE> getTargets() {
        return targetUnits;
    }

    public IN_GAME_OBJECT getCurrentUnit() {
        return currentUnit;
    }

    public List<TARGETABLE> getEnemyUnits() {
        return world.getEnemyUnits();
    }

    public double getUnitMaxDamageAtRange(TARGETABLE unit, int enemyRange) {
        String cacheKey = unit.hashCode() + "-" + enemyRange;
        if (damageCache.containsKey(cacheKey)) {
            return damageCache.get(cacheKey);
        }

        double maxDamage = calculateUnitMaxDamageAtRange(unit, enemyRange);
        damageCache.put(cacheKey, maxDamage);
        return maxDamage;
    }

    public abstract double calculateUnitMaxDamageAtRange(TARGETABLE unit, int enemyRange);

    public void clearCaches() {
        damageCache.clear();
    }

    public abstract double getBonusFactor();
}

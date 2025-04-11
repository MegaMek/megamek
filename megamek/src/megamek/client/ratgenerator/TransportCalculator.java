/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.client.ratgenerator;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.InfantryTransporter;
import megamek.common.MekFileParser;
import megamek.common.MekSummary;
import megamek.common.UnitType;
import megamek.common.bays.*;
import megamek.common.loaders.EntityLoadingException;

/**
 * Generates drop-ships and jump ships to fulfill transport requirements for a unit.
 *
 * @author Neoancient
 */
public class TransportCalculator {

    // In order to determine the transport capacity of generated units we need to load the Entity and look at the
    // bays and docking hard points. Since this is a relatively expensive operation we will cache the results.
    private static final Map<MekSummary, Map<Integer, Integer>> bayTypeCache = new HashMap<>();

    public static void dispose() {
        bayTypeCache.clear();
    }

    private final ForceDescriptor fd;
    private final Map<Integer, Integer> unitCounts;

    public TransportCalculator(ForceDescriptor fd) {
        this.fd = fd;
        this.unitCounts = getUnitTypeCounts();
    }

    /**
     * Determines number of each type of unit based on transport requirements.
     *
     * @return The number of units of each type mapped to its UnitType. UnitType.VTOL is used for light vehicle bays and
     *       UnitType.NAVAL for super heavy vehicles.
     */
    private Map<Integer, Integer> getUnitTypeCounts() {
        Map<Integer, Integer> unitCounts = new HashMap<>();
        List<Entity> allUnits = new ArrayList<>();
        fd.addAllEntities(allUnits);
        for (Entity en : allUnits) {
            if (en.hasETypeFlag(Entity.ETYPE_MEK)) {
                unitCounts.merge(UnitType.MEK, 1, Integer::sum);
            } else if (en.hasETypeFlag(Entity.ETYPE_PROTOMEK)) {
                unitCounts.merge(UnitType.PROTOMEK, 1, Integer::sum);
            } else if (en.hasETypeFlag(Entity.ETYPE_TANK)) {
                if (en.getWeight() > 100) {
                    unitCounts.merge(UnitType.NAVAL, 1, Integer::sum);
                } else if (en.getWeight() > 50) {
                    unitCounts.merge(UnitType.TANK, 1, Integer::sum);
                } else {
                    unitCounts.merge(UnitType.VTOL, 1, Integer::sum);
                }
            } else if (en.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)) {
                unitCounts.merge(UnitType.BATTLE_ARMOR, 1, Integer::sum);
            } else if (en.hasETypeFlag(Entity.ETYPE_INFANTRY)) {
                // Here we need to count the transport weight of the platoon rather than just the number
                unitCounts.merge(UnitType.INFANTRY,
                      InfantryTransporter.PlatoonType.getPlatoonType(en).getWeight(),
                      Integer::sum);
            } else if (en.hasETypeFlag(Entity.ETYPE_DROPSHIP)) {
                unitCounts.merge(UnitType.DROPSHIP, 1, Integer::sum);
            } else if (en.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
                unitCounts.merge(UnitType.SMALL_CRAFT, 1, Integer::sum);
            } else if (en.isFighter()) {
                unitCounts.merge(UnitType.AEROSPACEFIGHTER, 1, Integer::sum);
            }
        }
        return unitCounts;
    }

    /**
     * Generates dropships to provide enough capacity to transport the given ratio of the formation.
     *
     * @param ratio The ratio of dropships to generate to the total needs of the unit
     *
     * @return A list of generated dropships
     */
    public List<MekSummary> calcDropships(double ratio) {
        UnitTable table = UnitTable.findTable(fd.getFactionRec(),
              UnitType.DROPSHIP,
              fd.getYear(),
              fd.getRating(),
              null,
              ModelRecord.NETWORK_NONE,
              EnumSet.noneOf(EntityMovementMode.class),
              EnumSet.noneOf(MissionRole.class),
              0);
        List<MekSummary> retVal = new ArrayList<>();
        Map<Integer, Integer> currentCapacity = new HashMap<>();
        for (Integer unitType : unitCounts.keySet()) {
            // We counted dropships so we include them in the jump ship calculation, but we're not looking for
            // transport bays for them.
            if (UnitType.DROPSHIP == unitType) {
                continue;
            }

            while (unitCounts.get(unitType) * ratio > (double) (currentCapacity.getOrDefault(unitType, 0))) {
                MekSummary dropship = table.generateUnit(ms -> hasBayFor(ms, unitType));

                if (null == dropship) {
                    break; // Could not find any transport for the unit type; skip
                }

                bayTypeCache.get(dropship).forEach((k, v) -> currentCapacity.merge(k, v, Integer::sum));

                retVal.add(dropship);
            }
        }
        return retVal;
    }

    /**
     * Generates jump ships to provide enough docking collars to transport the given ratio of dropships.
     *
     * @param ratio            The ratio of jump ships to generate to the total needs of the unit
     * @param transportCollars The number of dropships generated for transport
     *
     * @return A list of generated jump ships
     */
    public List<MekSummary> calcJumpShips(double ratio, int transportCollars) {
        UnitTable table = UnitTable.findTable(fd.getFactionRec(),
              UnitType.JUMPSHIP,
              fd.getYear(),
              fd.getRating(),
              null,
              ModelRecord.NETWORK_NONE,
              EnumSet.noneOf(EntityMovementMode.class),
              EnumSet.noneOf(MissionRole.class),
              0);
        List<MekSummary> retVal = new ArrayList<>();
        int currentCapacity = 0;

        if (unitCounts.containsKey(UnitType.DROPSHIP)) {
            transportCollars += unitCounts.get(UnitType.DROPSHIP);
        }

        while (transportCollars * ratio > (double) currentCapacity) {
            // It's possible to have a jump ship with no docking collars, e.g. for scout use
            MekSummary jumpship = table.generateUnit(ms -> countHardPoints(ms) > 0);

            if (null == jumpship) {
                break; // Could not find any transport for the unit type; skip
            }

            currentCapacity += countHardPoints(jumpship);
            retVal.add(jumpship);
        }
        return retVal;
    }

    /**
     * Determines whether potential transport has capacity for the type of unit.
     *
     * @param ms       A potential transporting unit
     * @param unitType The unit to be carried
     *
     * @return True if the unit can be carried by the transporting unit.
     */
    private boolean hasBayFor(MekSummary ms, int unitType) {
        if (getBayCount(ms, unitType) > 0) {
            return true;
        }
        if (unitType == UnitType.VTOL) {
            return (getBayCount(ms, UnitType.TANK) > 0) || (getBayCount(ms, UnitType.NAVAL) > 0);
        } else if (unitType == UnitType.TANK) {
            return getBayCount(ms, UnitType.NAVAL) > 0;
        }
        return false;
    }

    private int getBayCount(MekSummary ms, int unitType) {
        if (bayTypeCache.containsKey(ms) || countBays(ms)) {
            return bayTypeCache.get(ms).getOrDefault(unitType, 0);
        }
        return 0;
    }

    /**
     * Loads the entity, counts the unit type transport capacity, and adds to the cache.
     *
     * @param ms The unit to load
     *
     * @return true if the Entity can be loaded and counted, false if there was an EntityLoadingException
     */
    private boolean countBays(MekSummary ms) {
        try {
            Entity entity = new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            bayTypeCache.put(ms, countBays(entity));
            return true;
        } catch (EntityLoadingException ex) {
            return false;
        }
    }

    /**
     * Counts the unit type transport capacity, and adds to the cache.
     *
     * @param entity The transporting unit
     *
     * @return a Mapping of unit types with counts.
     */
    private Map<Integer, Integer> countBays(Entity entity) {
        Map<Integer, Integer> bayCount = new HashMap<>();
        for (Bay bay : entity.getTransportBays()) {
            if (bay instanceof MekBay) {
                bayCount.merge(UnitType.MEK, (int) bay.getCapacity(), Integer::sum);
            } else if (bay instanceof ProtoMekBay) {
                bayCount.merge(UnitType.PROTOMEK, (int) bay.getCapacity(), Integer::sum);
            } else if (bay instanceof HeavyVehicleBay) {
                bayCount.merge(UnitType.TANK, (int) bay.getCapacity(), Integer::sum);
            } else if (bay instanceof LightVehicleBay) {
                bayCount.merge(UnitType.VTOL, (int) bay.getCapacity(), Integer::sum);
            } else if (bay instanceof SuperHeavyVehicleBay) {
                bayCount.merge(UnitType.NAVAL, (int) bay.getCapacity(), Integer::sum);
            } else if (bay instanceof BattleArmorBay) {
                bayCount.merge(UnitType.BATTLE_ARMOR, (int) bay.getCapacity(), Integer::sum);
            } else if (bay instanceof InfantryBay) {
                bayCount.merge(UnitType.BATTLE_ARMOR, (int) bay.getCapacity(), Integer::sum);
            } else if (bay instanceof ASFBay) {
                bayCount.merge(UnitType.AEROSPACEFIGHTER, (int) bay.getCapacity(), Integer::sum);
            } else if (bay instanceof SmallCraftBay) {
                bayCount.merge(UnitType.SMALL_CRAFT, (int) bay.getCapacity(), Integer::sum);
            }
        }
        return bayCount;
    }

    /**
     * Loads the Entity and counts the number of docking hard points.
     *
     * @param ms The unit to load
     *
     * @return The number of docking hard points on the unit.
     */
    private int countHardPoints(MekSummary ms) {
        try {
            Entity entity = new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            // TODO: count drop shuttle bays
            return entity.getDockingCollars().size();
        } catch (EntityLoadingException ex) {
            return 0;
        }
    }

}

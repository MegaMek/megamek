/**
 *
 */
package megamek.client.ratgenerator;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.ASFBay;
import megamek.common.BattleArmorBay;
import megamek.common.Bay;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.HeavyVehicleBay;
import megamek.common.InfantryBay;
import megamek.common.LightVehicleBay;
import megamek.common.MechBay;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.ProtomechBay;
import megamek.common.SmallCraftBay;
import megamek.common.SuperHeavyVehicleBay;
import megamek.common.UnitType;
import megamek.common.loaders.EntityLoadingException;

/**
 * Generates dropships and jumpships to fulfill transport requirements for a unit.
 *
 * @author Neoancient
 *
 */
public class TransportCalculator {

    // In order to determine the transport capacity of generated units we need to load the
    // Entity and look at the bays and docking hardpoints. Since this is a relatively expensive
    // operation we will cache the results.
    private static final Map<MechSummary, Map<Integer, Integer>> bayTypeCache = new HashMap<>();
    private static final Map<MechSummary, Integer> hardpointCache = new HashMap<>();

    public static void dispose() {
        bayTypeCache.clear();
        hardpointCache.clear();
    }

    private ForceDescriptor fd;
    private Map<Integer, Integer> unitCounts;

    public TransportCalculator(ForceDescriptor fd) {
        this.fd = fd;
        this.unitCounts = getUnitTypeCounts();
    }

    /**
     * Determines number of each type of unit based on transport requirements.
     *
     * @return The number of units of each type mapped to its UnitType.
     *         UnitType.VTOL is used for light vehicle bays and UnitType.NAVAL for superheavy vehicles.
     */
    private Map<Integer, Integer> getUnitTypeCounts() {
        Map<Integer, Integer> unitCounts = new HashMap<>();
        List<Entity> allUnits = new ArrayList<>();
        fd.addAllEntities(allUnits);
        for (Entity en : allUnits) {
            if (en.hasETypeFlag(Entity.ETYPE_MECH)) {
                unitCounts.merge(UnitType.MEK, 1, Integer::sum);
            } else if (en.hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
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
                unitCounts.merge(UnitType.INFANTRY, InfantryBay.PlatoonType.getPlatoonType(en).getWeight(),
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
     * @param ratio            The ratio of dropships to generate to the total needs of the unit
     * @return                 A list of generated dropships
     */
    public List<MechSummary> calcDropships(double ratio) {
        UnitTable table = UnitTable.findTable(fd.getFactionRec(), UnitType.DROPSHIP,
                fd.getYear(), fd.getRating(), null, ModelRecord.NETWORK_NONE,
                EnumSet.noneOf(EntityMovementMode.class), EnumSet.noneOf(MissionRole.class),
                0);
        List<MechSummary> retVal = new ArrayList<>();
        Map<Integer, Integer> currentCapacity = new HashMap<>();
        for (Integer unitType : unitCounts.keySet()) {
            // We counted dropships so we include them in the jumpship calculation, but we're
            // not looking for transport bays for them.
            if (UnitType.DROPSHIP == unitType) {
                continue;
            }
            while (unitCounts.get(unitType) * ratio > currentCapacity.getOrDefault(unitType, 0)) {
                MechSummary dropship = table.generateUnit(ms -> hasBayFor(ms, unitType));
                if (null == dropship) {
                    break; // Could not find any transport for the unit type; skip
                }
                bayTypeCache.get(dropship).forEach((k, v) -> {
                    currentCapacity.merge(k, v, Integer::sum);
                });
                retVal.add(dropship);
            }
        }
        return retVal;
    }

    /**
     * Generates jumpships to provide enough docking collars to transport the given ratio of dropships.
     *
     * @param ratio            The ratio of jumpships to generate to the total needs of the unit
     * @param transportCollars The number of dropships generated for transport
     * @return                 A list of generated jumpships
     */
    public List<MechSummary> calcJumpships(double ratio, int transportCollars) {
        UnitTable table = UnitTable.findTable(fd.getFactionRec(), UnitType.JUMPSHIP,
                fd.getYear(), fd.getRating(), null, ModelRecord.NETWORK_NONE,
                EnumSet.noneOf(EntityMovementMode.class), EnumSet.noneOf(MissionRole.class),
                0);
        List<MechSummary> retVal = new ArrayList<>();
        int currentCapacity = 0;
        if (unitCounts.containsKey(UnitType.DROPSHIP)) {
            transportCollars += unitCounts.get(UnitType.DROPSHIP);
        }
        while (transportCollars * ratio > currentCapacity) {
            // It's possible to have a jumpship with no docking collars, e.g. for scout use
            MechSummary jumpship = table.generateUnit(ms -> countHardpoints(ms) > 0);
            if (null == jumpship) {
                break; // Could not find any transport for the unit type; skip
            }
            currentCapacity += countHardpoints(jumpship);
            retVal.add(jumpship);
        }
        return retVal;
    }

    /**
     * Determines whether a potential transport has capacity for the type of unit.
     *
     * @param ms        A potential tranporting unit
     * @param unitType  The unit to be carried
     * @return          True if the unit can be carried by the transporting unit.
     */
    private boolean hasBayFor(MechSummary ms, int unitType) {
        if (getBayCount(ms, unitType) > 0) {
            return true;
        }
        if (unitType == UnitType.VTOL) {
            return (getBayCount(ms, UnitType.TANK) > 0)
                    || (getBayCount(ms, UnitType.NAVAL) > 0);
        } else if (unitType == UnitType.TANK) {
            return getBayCount(ms, UnitType.NAVAL) > 0;
        }
        return false;
    }

    private int getBayCount(MechSummary ms, int unitType) {
        if (bayTypeCache.containsKey(ms) || countBays(ms)) {
            return bayTypeCache.get(ms).getOrDefault(unitType, 0);
        }
        return 0;
    }

    /**
     * Loads the entity, counts the unit type transport capacity, and adds to the cache.
     *
     * @param ms  The unit to load
     * @return    true if the Entity can be loaded and counted, false if there was an EntityLoadingException
     */
    private boolean countBays(MechSummary ms) {
        try {
            Entity entity = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            bayTypeCache.put(ms, countBays(entity));
            return true;
        } catch (EntityLoadingException ex) {
            return false;
        }
    }

    /**
     * Counts the unit type transport capacity, and adds to the cache.
     *
     * @param entity  The transporting unit
     * @return        true if the Entity can be loaded and counted, false if there was an EntityLoadingException
     */
    private Map<Integer, Integer> countBays(Entity entity) {
        Map<Integer, Integer> bayCount = new HashMap<>();
        for (Bay bay : entity.getTransportBays()) {
            if (bay instanceof MechBay) {
                bayCount.merge(UnitType.MEK, (int) bay.getCapacity(), Integer::sum);
            } else if (bay instanceof ProtomechBay) {
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
     * Loads the Entity and counts the number of docking hardpoints.
     *
     * @param ms The unit to load
     * @return   The number of docking hardpoints on the unit.
     */
    private int countHardpoints(MechSummary ms) {
        try {
            Entity entity = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            int hardpoints = entity.getDockingCollars().size();
            // TODO: count dropshuttle bays
            hardpointCache.put(ms, hardpoints);
            return hardpoints;
        } catch (EntityLoadingException ex) {
            return 0;
        }
    }

}

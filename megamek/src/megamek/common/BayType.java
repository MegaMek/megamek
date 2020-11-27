/*
 * MegaMek - Copyright (C) 2017 - The MegaMek Team
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
package megamek.common;

import java.util.function.Predicate;

import megamek.common.annotations.Nullable;

/**
 * Data for various transport bay types. This is used by MekHQ for cubicle parts, but can also be
 * used in the future for a generic bay that hold multiple types of units.
 * 
 * @author Neoancient
 *
 */
public enum BayType implements ITechnologyDelegator {
    STANDARD_CARGO (BayType.CATEGORY_CARGO, 1.0, 1.0, 0, 0, e -> false, CargoBay.techAdvancement()),
    LIQUID_CARGO (BayType.CATEGORY_CARGO, 1.0, 0.91, 0, 100, e -> false, LiquidCargoBay.techAdvancement()),
    REFRIGERATED_CARGO (BayType.CATEGORY_CARGO, 1.0, 0.87, 0, 250, e -> false, RefrigeratedCargoBay.techAdvancement()),
    INSULATED_CARGO (BayType.CATEGORY_CARGO, 1.0, 0.87, 0, 200, e -> false, RefrigeratedCargoBay.techAdvancement()),
    LIVESTOCK_CARGO (BayType.CATEGORY_CARGO, 1.0, 0.83, 0, 2500, e -> false, LivestockCargoBay.techAdvancement()),
    INFANTRY_FOOT (BayType.CATEGORY_INFANTRY, 5.0, 1.0, 28, 15000, e -> 
        e.hasETypeFlag(Entity.ETYPE_INFANTRY)
            && !e.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)
            && (e.getMovementMode() == EntityMovementMode.INF_LEG), InfantryBay.techAdvancement()),
    INFANTRY_JUMP (BayType.CATEGORY_INFANTRY, 6.0, 1.0, 21, 15000, e -> 
        e.hasETypeFlag(Entity.ETYPE_INFANTRY)
            && !e.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)
            && (e.getMovementMode() == EntityMovementMode.INF_JUMP), InfantryBay.techAdvancement()),
    INFANTRY_MOTORIZED (BayType.CATEGORY_INFANTRY, 7.0, 1.0, 28, 15000, e -> 
        e.hasETypeFlag(Entity.ETYPE_INFANTRY)
            && (e.getMovementMode() == EntityMovementMode.INF_MOTORIZED), InfantryBay.techAdvancement()),
    INFANTRY_MECHANIZED (BayType.CATEGORY_INFANTRY, 8.0, 1.0, 5, 15000, e -> 
        e.hasETypeFlag(Entity.ETYPE_INFANTRY)
            && ((Infantry) e).isMechanized(), InfantryBay.techAdvancement()),
    BATTLEARMOR_IS (BayType.CATEGORY_INFANTRY, 8.0, 1.0, 6, 15000, e -> 
        e.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)
            && (((BattleArmor) e).getSquadSize() <= 4), BattleArmorBay.techAdvancement()),
    BATTLEARMOR_CLAN (BayType.CATEGORY_INFANTRY, 10.0, 1.0, 6, 15000, e -> 
        e.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)
            && (((BattleArmor) e).getSquadSize() <= 5), BattleArmorBay.techAdvancement()),
    BATTLEARMOR_CS (BayType.CATEGORY_INFANTRY, 12.0, 1.0, 6, 15000, e -> 
        e.hasETypeFlag(Entity.ETYPE_BATTLEARMOR), BattleArmorBay.techAdvancement()),
    MECH (BayType.CATEGORY_NON_INFANTRY, 150.0, 1.0, 2, 20000,
            e -> e.hasETypeFlag(Entity.ETYPE_MECH), MechBay.techAdvancement()),
    FIGHTER (BayType.CATEGORY_NON_INFANTRY, 150.0, 1.0, 2, 20000, e -> e.isFighter(), ASFBay.techAdvancement()),
    PROTOMECH (BayType.CATEGORY_NON_INFANTRY, 50.0, 5.0, 6, 10000, e -> e.hasETypeFlag(Entity.ETYPE_PROTOMECH), ProtomechBay.techAdvancement()),
    SMALL_CRAFT (BayType.CATEGORY_NON_INFANTRY, 200.0, 1.0, 5, 20000, e -> e.hasETypeFlag(Entity.ETYPE_AERO)
            && (e.getWeight() <= 200.0), SmallCraftBay.techAdvancement()),
    VEHICLE_LIGHT (BayType.CATEGORY_NON_INFANTRY, 50.0, 1.0, 5, 10000, e -> e.hasETypeFlag(Entity.ETYPE_TANK)
            && (e.getWeight() <= 50.0), LightVehicleBay.techAdvancement()),
    VEHICLE_HEAVY (BayType.CATEGORY_NON_INFANTRY, 100.0, 1.0, 8, 10000, e -> e.hasETypeFlag(Entity.ETYPE_TANK)
            && (e.getWeight() <= 100.0), HeavyVehicleBay.techAdvancement()),
    VEHICLE_SH (BayType.CATEGORY_NON_INFANTRY, 200.0, 1.0, 15, 20000, e -> e.hasETypeFlag(Entity.ETYPE_TANK)
            && (e.getWeight() <= 200.0), SuperHeavyVehicleBay.techAdvancement());
    
    public static final int CATEGORY_CARGO        = 0;
    public static final int CATEGORY_INFANTRY     = 1;
    public static final int CATEGORY_NON_INFANTRY = 2;
    
    private int category;
    private double weight;
    private double capacity;
    private int personnel;
    private long cost;
    private Predicate<Entity> canLoad;
    private TechAdvancement techAdvancement;
    
    BayType(int category, double weight, double capacity, int personnel, long cost,
            Predicate<Entity> canLoad, TechAdvancement techAdvancement) {
        this.category = category;
        this.weight = weight;
        this.capacity = capacity;
        this.personnel = personnel;
        this.cost = cost;
        this.canLoad = canLoad;
        this.techAdvancement = techAdvancement;
    }
    
    /**
     * Bays fall into three basic categories:
     * CATEGORY_CARGO is for bulk transport and can be sized in fractional increments (usually half ton).
     *                Capacity is the fraction of bay tonnage that is available for storage.
     * CATEGORY_INFANTRY calculates cost based on tonnage rather than unit capacity and has no cost for aerospace units.
     * CATEGORY_NON_INFANTRY calculates cost per cubicle. Capacity is the number of entities that can fit
     *                       in a single cubicle.
     * @return The category index.
     */
    public int getCategory() {
        return category;
    }

    /**
     * @return The tonnage weight of a single unit of the bay type. For cargo the base unit is a single ton;
     *         for units the base unit is a number of Entity units equal to the capacity.
     */
    public double getWeight() {
        return weight;
    }
    
    /**
     * @return The capacity of a single unit of the bay type. For cargo the is the number of tons of cargo
     *         that can be stored in a single ton of bay capacity. For unit transport bays this is the Entity
     *         count for the cubicle (usually one).
     */
    public double getCapacity() {
        return capacity;
    }
    
    /**
     * @return The number of personnel that can be housed in by each unit of the bay.
     */
    public int getPersonnel() {
        return personnel;
    }
    
    /**
     * @return The base cost of a single unit of the bay type. Note that the cost for infantry units
     *         (conventional and battlearmor) is per ton and not per platoon/squad and aerospace units
     *         do not pay for the cost of infantry transport bays.
     */
    public long getCost() {
        return cost;
    }
    
    /**
     * @param en An entity to load into a bay.
     * @return   true if the Entity can be housed in the type of bay.
     */
    public boolean canLoad(Entity en) {
        return canLoad.test(en);
    }

    @Override
    public ITechnology getTechSource() {
        return techAdvancement;
    }
    
    /**
     * @return The name of the type of bay.
     */
    public String getDisplayName() {
        return EquipmentMessages.getString("BayType." + name()); //$NON-NLS-1$
    }
    
    public static @Nullable BayType parse(String name) {
        if (null != name) {
            for (BayType bt : values()) {
                if (bt.toString().toLowerCase().equals(name.toLowerCase())) {
                    return bt;
                }
            }
        }
        return null;
    }
    
    /**
     * Finds the BayType that matches an existing bay.
     * 
     * @param bay A transport bay object
     * @return    The BayType for the bay.
     */
    public static BayType getTypeForBay(Bay bay) {
        if (bay instanceof CargoBay) {
            return STANDARD_CARGO;
        } else if (bay instanceof LiquidCargoBay) {
            return LIQUID_CARGO;
        } else if (bay instanceof RefrigeratedCargoBay) {
            return REFRIGERATED_CARGO;
        } else if (bay instanceof InsulatedCargoBay) {
            return INSULATED_CARGO;
        } else if (bay instanceof LivestockCargoBay) {
            return LIVESTOCK_CARGO;
        } else if (bay instanceof InfantryBay) {
            InfantryBay.PlatoonType ptype = ((InfantryBay) bay).getPlatoonType();
            if (ptype == InfantryBay.PlatoonType.JUMP) {
                return INFANTRY_JUMP;
            } else if (ptype == InfantryBay.PlatoonType.MOTORIZED) {
                return INFANTRY_MOTORIZED;
            } else if (ptype == InfantryBay.PlatoonType.MECHANIZED) {
                return INFANTRY_MECHANIZED;
            } else 
                return INFANTRY_FOOT;
        } else if (bay instanceof BattleArmorBay) {
            if (bay.isClan()) {
                return BATTLEARMOR_CLAN;
            } else if (bay.toString().contains("C*")) {
                return BATTLEARMOR_CS;
            } else {
                return BATTLEARMOR_IS;
            }
        } else if (bay instanceof MechBay) {
            return MECH;
        } else if (bay instanceof ASFBay) {
            return FIGHTER;
        } else if (bay instanceof ProtomechBay) {
            return PROTOMECH;
        } else if (bay instanceof SmallCraftBay) {
            return SMALL_CRAFT;
        } else if (bay instanceof LightVehicleBay) {
            return VEHICLE_LIGHT;
        } else if (bay instanceof HeavyVehicleBay) {
            return VEHICLE_HEAVY;
        } else if (bay instanceof SuperHeavyVehicleBay) {
            return VEHICLE_SH;
        } else {
            return STANDARD_CARGO;
        }
    }
}

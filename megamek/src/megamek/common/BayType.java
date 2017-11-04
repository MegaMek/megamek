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

/**
 * Data for various transport bay types. This is used by MekHQ for cubicle parts, but can also be
 * used in the future for a generic bay that hold multiple types of units.
 * 
 * @author Neoancient
 *
 */
public enum BayType implements ITechnologyDelegator {
    STANDARD_CARGO (true, 1.0, 1.0, 0, 0, e -> false, CargoBay.techAdvancement()),
    LIQUID_CARGO (true, 1.0, 0.91, 0, 0, e -> false, LiquidCargoBay.techAdvancement()),
    REFRIGERATED_CARGO (true, 1.0, 0.87, 0, 0, e -> false, RefrigeratedCargoBay.techAdvancement()),
    LIVESTOCK_CARGO (true, 1.0, 0.83, 0, 0, e -> false, LivestockCargoBay.techAdvancement()),
    INFANTRY_FOOT (false, 5.0, 1.0, 28, 0, e -> 
        e.hasETypeFlag(Entity.ETYPE_INFANTRY)
            && !e.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)
            && (e.getMovementMode() == EntityMovementMode.INF_LEG), InfantryBay.techAdvancement()),
    INFANTRY_JUMP (false, 6.0, 1.0, 21, 0, e -> 
        e.hasETypeFlag(Entity.ETYPE_INFANTRY)
            && !e.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)
            && (e.getMovementMode() == EntityMovementMode.INF_JUMP), InfantryBay.techAdvancement()),
    INFANTRY_MOTORIZED (false, 7.0, 1.0, 28, 0, e -> 
        e.hasETypeFlag(Entity.ETYPE_INFANTRY)
            && (e.getMovementMode() == EntityMovementMode.INF_MOTORIZED), InfantryBay.techAdvancement()),
    INFANTRY_MECHANIZED (false, 8.0, 1.0, 5, 0, e -> 
        e.hasETypeFlag(Entity.ETYPE_INFANTRY)
            && ((Infantry) e).isMechanized(), InfantryBay.techAdvancement()),
    BATTLEARMOR_IS (false, 8.0, 1.0, 6, 0, e -> 
        e.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)
            && (((BattleArmor) e).getSquadSize() <= 4), BattleArmorBay.techAdvancement()),
    BATTLEARMOR_CLAN (false, 10.0, 1.0, 6, 0, e -> 
        e.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)
            && (((BattleArmor) e).getSquadSize() <= 5), BattleArmorBay.techAdvancement()),
    BATTLEARMOR_CS (false, 12.0, 1.0, 6, 0, e -> 
        e.hasETypeFlag(Entity.ETYPE_BATTLEARMOR), BattleArmorBay.techAdvancement()),
    MECH (false, 150.0, 1.0, 2, 0, e -> e.hasETypeFlag(Entity.ETYPE_MECH), MechBay.techAdvancement()),
    FIGHTER (false, 150.0, 1.0, 2, 0, e -> e.isFighter(), ASFBay.techAdvancement()),
    PROTOMECH (false, 50.0, 5.0, 6, 0, e -> e.hasETypeFlag(Entity.ETYPE_PROTOMECH), ProtomechBay.techAdvancement()),
    SMALL_CRAFT (false, 200.0, 1.0, 5, 0, e -> e.hasETypeFlag(Entity.ETYPE_AERO)
            && (e.getWeight() <= 200.0), SmallCraftBay.techAdvancement()),
    VEHICLE_LIGHT (false, 50.0, 1.0, 5, 0, e -> e.hasETypeFlag(Entity.ETYPE_TANK)
            && (e.getWeight() <= 50.0), LightVehicleBay.techAdvancement()),
    VEHICLE_HEAVY (false, 100.0, 1.0, 8, 0, e -> e.hasETypeFlag(Entity.ETYPE_TANK)
            && (e.getWeight() <= 100.0), HeavyVehicleBay.techAdvancement()),
    VEHICLE_SH (false, 200.0, 1.0, 15, 0, e -> e.hasETypeFlag(Entity.ETYPE_TANK)
            && (e.getWeight() <= 200.0), SuperHeavyVehicleBay.techAdvancement());
    
    private boolean cargo;
    private double weight;
    private double capacity;
    private int personnel;
    private long cost;
    private Predicate<Entity> canLoad;
    private TechAdvancement techAdvancement;
    
    BayType(boolean cargo, double weight, double capacity, int personnel, long cost,
            Predicate<Entity> canLoad, TechAdvancement techAdvancement) {
        this.cargo = cargo;
        this.weight = weight;
        this.capacity = capacity;
        this.personnel = personnel;
        this.cost = cost;
        this.canLoad = canLoad;
        this.techAdvancement = techAdvancement;
    }
    
    
    public boolean isCargo() {
        return cargo;
    }

    public double getWeight() {
        return weight;
    }
    
    public double getCapacity() {
        return capacity;
    }
    
    public int getPersonnel() {
        return personnel;
    }
    
    public long getCost() {
        return cost;
    }
    
    public boolean canLoad(Entity en) {
        return canLoad.test(en);
    }

    @Override
    public ITechnology getTechSource() {
        return techAdvancement;
    }
    
}

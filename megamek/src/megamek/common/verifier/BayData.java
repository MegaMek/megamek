/*
 * MegaMek - Copyright (C) 2018 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.verifier;

import java.util.function.BiFunction;

import megamek.common.*;
import megamek.common.annotations.Nullable;

/**
 * Construction data used by transport bays for mechs, vees, and aerospace units.
 * 
 * @author Neoancient
 *
 */
public enum BayData {
    MECH ("Mech", 150.0, 2, MechBay.techAdvancement(),
            (size, num) -> new MechBay(size, 1, num)),
    PROTOMECH ("Protomech", 10.0, 6, ProtomechBay.techAdvancement(),
            (size, num) -> new ProtomechBay(size, 1, num)),
    VEHICLE_HEAVY ("Heavy Vehicle", 100.0, 8, HeavyVehicleBay.techAdvancement(),
            (size, num) -> new HeavyVehicleBay(size, 1, num)),
    VEHICLE_LIGHT ("Light Vehicle", 50.0, 5, LightVehicleBay.techAdvancement(),
            (size, num) -> new LightVehicleBay(size, 1, num)),
    VEHICLE_SH ("Superheavy Vehicle", 200.0, 15, SuperHeavyVehicleBay.techAdvancement(),
            (size, num) -> new SuperHeavyVehicleBay(size, 1, num)),
    INFANTRY_FOOT ("Infantry (Foot)", 5.0, 0, InfantryBay.techAdvancement(),
            (size, num) -> new InfantryBay(size, 1, num, InfantryBay.PlatoonType.FOOT)),
    INFANTRY_JUMP ("Infantry (Jump)", 6.0, 0, InfantryBay.techAdvancement(),
            (size, num) -> new InfantryBay(size, 1, num, InfantryBay.PlatoonType.JUMP)),
    INFANTRY_MOTORIZED ("Infantry (Motorized)", 7.0, 0, InfantryBay.techAdvancement(),
            (size, num) -> new InfantryBay(size, 1, num, InfantryBay.PlatoonType.MOTORIZED)),
    INFANTRY_MECHANIZED ("Infantry (Mech. Squad)", 8.0, 0, InfantryBay.techAdvancement(),
            (size, num) -> new InfantryBay(size, 1, num, InfantryBay.PlatoonType.MECHANIZED)),
    IS_BATTLE_ARMOR ("BattleArmor (IS)", 8.0, 6, BattleArmorBay.techAdvancement(),
            (size, num) -> new BattleArmorBay(size, 1, num, false, false)),
    CLAN_BATTLE_ARMOR ("BattleArmor (Clan)", 10.0, 6, BattleArmorBay.techAdvancement(),
            (size, num) -> new BattleArmorBay(size, 1, num, true, false)),
    CS_BATTLE_ARMOR ("BattleArmor (CS)", 12.0, 6, BattleArmorBay.techAdvancement(),
            (size, num) -> new BattleArmorBay(size, 1, num, false, true)),
    FIGHTER ("Fighter", 150.0, 2, ASFBay.techAdvancement(),
            (size, num) -> new ASFBay(size, 1, num)),
    SMALL_CRAFT ("Small Craft", 200.0, 5, SmallCraftBay.techAdvancement(),
            (size, num) -> new SmallCraftBay(size, 1, num)),
    DROPSHUTTLE ("Dropshuttle", 11000.0, 0, DropshuttleBay.techAdvancement(),
            (size, num) -> new DropshuttleBay(1, num, 0)),
    REPAIR_UNPRESSURIZED ("Standard Repair Facility (Unpressurized)", 0.025, 0, NavalRepairFacility.techAdvancement(),
            (size, num) -> new NavalRepairFacility(size, 1, num, 0, false)),
    REPAIR_PRESSURIZED ("Standard Repair Facility (Pressurized)", 0.075, 0, NavalRepairFacility.techAdvancement(),
            (size, num) -> new NavalRepairFacility(size, 1, num, 0, true)),
    REPAIR_REINFORCED ("Reinforced Repair Facility", 0.1, 0, ReinforcedRepairFacility.techAdvancement(),
            (size, num) -> new ReinforcedRepairFacility(size, 1, num, 0)),
    ARTS_FIGHTER ("ARTS Fighter", 187.5, 0, Bay.artsTechAdvancement(),
            (size, num) -> new ASFBay(size, 1, num, true)),
    ARTS_SMALL_CRAFT ("ARTS Small Craft", 250.0, 0, Bay.artsTechAdvancement(),
            (size, num) -> new SmallCraftBay(size, 1, num, true)),
    ARTS_REPAIR_UNPRESSURIZED ("ARTS Standard Repair Facility (Unpressurized)", 0.03125, 0, Bay.artsTechAdvancement(),
            (size, num) -> new NavalRepairFacility(size, 1, num, 0, false, true)),
    ARTS_REPAIR_PRESSURIZED ("ARTS Standard Repair Facility (Pressurized)", 0.09375, 0, Bay.artsTechAdvancement(),
            (size, num) -> new NavalRepairFacility(size, 1, num, 0, true, true)),
    CARGO ("Cargo", 1.0, 0, CargoBay.techAdvancement(),
            (size, num) -> new CargoBay(size, 1, num)),
    LIQUID_CARGO ("Cargo (Liquid)", 1/0.91, 0, CargoBay.techAdvancement(),
            (size, num) -> new LiquidCargoBay(size, 1, num)),
    REFRIGERATED_CARGO ("Cargo (Refrigerated)", 1/0.87, 0, CargoBay.techAdvancement(),
            (size, num) -> new RefrigeratedCargoBay(size, 1, num)),
    INSULATED_CARGO ("Cargo (Insulated)", 1/0.87, 0, CargoBay.techAdvancement(),
            (size, num) -> new InsulatedCargoBay(size, 1, num)),
    LIVESTOCK_CARGO ("Cargo Livestock)", 1/0.83, 0, CargoBay.techAdvancement(),
            (size, num) -> new LivestockCargoBay(size, 1, num));
    
    private final String name;
    private final double weight;
    private final int personnel;
    private final TechAdvancement techAdvancement;
    private final BiFunction<Double,Integer,Bay> init;
    
    BayData(String name, double weight, int personnel,
            TechAdvancement techAdvancement, BiFunction<Double,Integer,Bay> init) {
        this.name = name;
        this.weight = weight;
        this.personnel = personnel;
        this.techAdvancement = techAdvancement;
        this.init = init;
    }
    
    /**
     * @return A String identifying the type of bay suitable for display.
     */
    public String getDisplayName() {
        return name;
    }

    /**
     * The weight of a single unit of capacity. For unit transport bays this is the weight of a single
     * cubicle. For cargo this is the weight of one ton of cargo capacity. 
     * 
     * @return The weight of a single unit.
     */
    public double getWeight() {
        return weight;
    }
    
    /**
     * @return The number of bay personnel normally quartered per unit capacity.
     */
    public int getPersonnel() {
        return personnel;
    }

    /**
     * Creates a new bay of the type.
     * 
     * @param size    The size of bay in cubicles (units) or tons (cargo; this is bay tonnage, not capacity).
     * @param bayNum  The bay number; this should be unique for the unit.
     * @return        The new bay.
     */
    public Bay newBay(double size, int bayNum) {
        if (isCargoBay()) {
            // Remove floating point inaccuracy
            return init.apply(RoundWeight.nearestKg(size / weight), bayNum);
        } else {
            return init.apply(size, bayNum);
        }
    }
    
    /**
     * @return The tech progression for the bay type.
     */
    public TechAdvancement getTechAdvancement() {
        return techAdvancement;
    }

    /**
     * Identifies the type of bay.
     * 
     * @param bay A <code>Bay</code> that is (or can be) mounted on a unit. 
     * @return    The enum value for the bay. Returns null if the bay is not a transport by
     *            (e.g. crew quarters)
     */
    public static @Nullable BayData getBayType(Bay bay) {
        if (bay instanceof MechBay) {
            return MECH;
        } else if (bay instanceof ProtomechBay) {
            return PROTOMECH;
        } else if (bay instanceof HeavyVehicleBay) {
            return VEHICLE_HEAVY;
        } else if (bay instanceof LightVehicleBay) {
            return VEHICLE_LIGHT;
        } else if (bay instanceof SuperHeavyVehicleBay) {
            return VEHICLE_SH;
        } else if (bay instanceof InfantryBay) {
            switch (((InfantryBay) bay).getPlatoonType()) {
                case JUMP:
                    return INFANTRY_JUMP;
                case MECHANIZED:
                    return INFANTRY_MECHANIZED;
                case MOTORIZED:
                    return INFANTRY_MOTORIZED;
                case FOOT:
                default:
                    return INFANTRY_FOOT;
                
            }
        } else if (bay instanceof BattleArmorBay) {
            if (bay.getWeight() / bay.getCapacity() == 12) {
                return CS_BATTLE_ARMOR;
            } else if (bay.getWeight() / bay.getCapacity() == 10) {
                return CLAN_BATTLE_ARMOR;
            }
            return IS_BATTLE_ARMOR;
        } else if (bay instanceof ASFBay) {
            return ((ASFBay) bay).hasARTS() ? ARTS_FIGHTER : FIGHTER;
        } else if (bay instanceof DropshuttleBay) {
            return DROPSHUTTLE;
        } else if (bay instanceof ReinforcedRepairFacility) {
            return REPAIR_REINFORCED;
        } else if (bay instanceof NavalRepairFacility) {
            if (((NavalRepairFacility) bay).isPressurized()) {
                return ((NavalRepairFacility) bay).hasARTS() ? ARTS_REPAIR_PRESSURIZED : REPAIR_PRESSURIZED;
            } else {
                return ((NavalRepairFacility) bay).hasARTS() ? ARTS_REPAIR_UNPRESSURIZED : REPAIR_UNPRESSURIZED;
            }
        } else if (bay instanceof SmallCraftBay) {
            return ((SmallCraftBay) bay).hasARTS() ? ARTS_SMALL_CRAFT : SMALL_CRAFT;
        } else if (bay instanceof LiquidCargoBay) {
            return LIQUID_CARGO;
        } else if (bay instanceof LivestockCargoBay) {
            return LIVESTOCK_CARGO;
        } else if (bay instanceof RefrigeratedCargoBay) {
            return REFRIGERATED_CARGO;
        } else if (bay instanceof InsulatedCargoBay) {
            return INSULATED_CARGO;
        } else if (bay instanceof CargoBay) {
            return CARGO;
        } else {
            // Crew quarters are implemented as bays and should not be mixed with transport bays
            return null;
        }
    }
    
    /**
     * @return true if the bay is a type of cargo bay rather than a unit transport bay.
     */
    public boolean isCargoBay() {
        return ordinal() >= CARGO.ordinal();
    }

    /**
     * @return true if the bay is an infantry transport bay (including battlearmor)
     */
    public boolean isInfantryBay() {
        return (ordinal() >= INFANTRY_FOOT.ordinal())
                && (ordinal() <= CS_BATTLE_ARMOR.ordinal());
    }
    
    /**
     * Determines whether the bay is legal to mount on a given <code>Entity</code>. Whether it is
     * technically possible or practical is another matter.
     * 
     * @param en The entity
     * @return   Whether the bay is legal
     */
    public boolean isLegalFor(Entity en) {
        //TODO: Container cargo bays aren't implemented, but when added they can be carried by
        // industrial but not battlemechs.
        if (en.hasETypeFlag(Entity.ETYPE_MECH)) {
            return isCargoBay() && (this != LIVESTOCK_CARGO);
        } else if ((this == DROPSHUTTLE)
                || (this == REPAIR_UNPRESSURIZED)
                || (this == REPAIR_PRESSURIZED)
                || (this == REPAIR_REINFORCED)) {
            return en.hasETypeFlag(Entity.ETYPE_JUMPSHIP);
        } else {
            return en.hasETypeFlag(Entity.ETYPE_TANK)
                    || en.hasETypeFlag(Entity.ETYPE_AERO);
        }
    }

    /**
     * @return Whether the bay type requires a designated armor facing.
     */
    public boolean requiresFacing() {
        return (this == DROPSHUTTLE)
                || (this == REPAIR_UNPRESSURIZED)
                || (this == REPAIR_PRESSURIZED)
                || (this == REPAIR_REINFORCED)
                || (this == ARTS_REPAIR_PRESSURIZED)
                || (this == ARTS_REPAIR_UNPRESSURIZED);
    }

    /**
     * @return Whether the bay capacity can be changed.
     */
    public boolean hasVariableSize() {
        return this != DROPSHUTTLE;
    }
}


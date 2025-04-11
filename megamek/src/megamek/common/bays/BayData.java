/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.bays;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import megamek.common.Entity;
import megamek.common.InfantryTransporter.PlatoonType;
import megamek.common.RoundWeight;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;

/**
 * Construction data used by transport bays for meks, vees, and aerospace units.
 *
 * @author Neoancient
 */
public enum BayData {
    MEK("Mek", 150.0, 2, MekBay.techAdvancement(), (size, num) -> new MekBay(size, 1, num)),
    PROTOMEK("Protomek", 10.0, 6, ProtoMekBay.techAdvancement(), (size, num) -> new ProtoMekBay(size, 1, num)),
    VEHICLE_HEAVY("Heavy Vehicle",
          100.0,
          8,
          HeavyVehicleBay.techAdvancement(),
          (size, num) -> new HeavyVehicleBay(size, 1, num)),
    VEHICLE_LIGHT("Light Vehicle",
          50.0,
          5,
          LightVehicleBay.techAdvancement(),
          (size, num) -> new LightVehicleBay(size, 1, num)),
    VEHICLE_SH("Superheavy Vehicle",
          200.0,
          15,
          SuperHeavyVehicleBay.techAdvancement(),
          (size, num) -> new SuperHeavyVehicleBay(size, 1, num)),
    INFANTRY_FOOT("Infantry (Foot)",
          5.0,
          0,
          InfantryBay.techAdvancement(),
          (size, num) -> new InfantryBay(size, 0, num, PlatoonType.FOOT)),
    INFANTRY_JUMP("Infantry (Jump)",
          6.0,
          0,
          InfantryBay.techAdvancement(),
          (size, num) -> new InfantryBay(size, 0, num, PlatoonType.JUMP)),
    INFANTRY_MOTORIZED("Infantry (Motorized)",
          7.0,
          0,
          InfantryBay.techAdvancement(),
          (size, num) -> new InfantryBay(size, 0, num, PlatoonType.MOTORIZED)),
    INFANTRY_MECHANIZED("Infantry (Mek. Squad)",
          8.0,
          0,
          InfantryBay.techAdvancement(),
          (size, num) -> new InfantryBay(size, 0, num, PlatoonType.MECHANIZED)),
    IS_BATTLE_ARMOR("BattleArmor (IS)",
          8.0,
          6,
          BattleArmorBay.techAdvancement(),
          (size, num) -> new BattleArmorBay(size, 0, num, false, false)),
    CLAN_BATTLE_ARMOR("BattleArmor (Clan)",
          10.0,
          6,
          BattleArmorBay.techAdvancement(),
          (size, num) -> new BattleArmorBay(size, 0, num, true, false)),
    CS_BATTLE_ARMOR("BattleArmor (CS)",
          12.0,
          6,
          BattleArmorBay.techAdvancement(),
          (size, num) -> new BattleArmorBay(size, 0, num, false, true)),
    FIGHTER("Fighter", 150.0, 2, ASFBay.techAdvancement(), (size, num) -> new ASFBay(size, 1, num)),
    SMALL_CRAFT("Small Craft",
          200.0,
          5,
          SmallCraftBay.techAdvancement(),
          (size, num) -> new SmallCraftBay(size, 1, num)),
    DROPSHUTTLE("Dropshuttle",
          11000.0,
          0,
          DropShuttleBay.techAdvancement(),
          (size, num) -> new DropShuttleBay(1, num, 0)),
    REPAIR_UNPRESSURIZED("Standard Repair Facility (Unpressurized)",
          0.025,
          0,
          NavalRepairFacility.techAdvancement(),
          (size, num) -> new NavalRepairFacility(size, 1, num, 0, false)),
    REPAIR_PRESSURIZED("Standard Repair Facility (Pressurized)",
          0.075,
          0,
          NavalRepairFacility.techAdvancement(),
          (size, num) -> new NavalRepairFacility(size, 1, num, 0, true)),
    REPAIR_REINFORCED("Reinforced Repair Facility",
          0.1,
          0,
          ReinforcedRepairFacility.techAdvancement(),
          (size, num) -> new ReinforcedRepairFacility(size, 1, num, 0)),
    ARTS_FIGHTER("ARTS Fighter", 187.5, 0, Bay.artsTechAdvancement(), (size, num) -> new ASFBay(size, 1, num, true)),
    ARTS_SMALL_CRAFT("ARTS Small Craft",
          250.0,
          0,
          Bay.artsTechAdvancement(),
          (size, num) -> new SmallCraftBay(size, 1, num, true)),
    ARTS_REPAIR_UNPRESSURIZED("ARTS Standard Repair Facility (Unpressurized)",
          0.03125,
          0,
          Bay.artsTechAdvancement(),
          (size, num) -> new NavalRepairFacility(size, 1, num, 0, false, true)),
    ARTS_REPAIR_PRESSURIZED("ARTS Standard Repair Facility (Pressurized)",
          0.09375,
          0,
          Bay.artsTechAdvancement(),
          (size, num) -> new NavalRepairFacility(size, 1, num, 0, true, true)),
    CARGO("Cargo", 1.0, 0, CargoBay.techAdvancement(), (size, num) -> new CargoBay(size, 0, num)),
    LIQUID_CARGO("Cargo (Liquid)",
          1 / 0.91,
          0,
          CargoBay.techAdvancement(),
          (size, num) -> new LiquidCargoBay(size, 0, num)),
    REFRIGERATED_CARGO("Cargo (Refrigerated)",
          1 / 0.87,
          0,
          CargoBay.techAdvancement(),
          (size, num) -> new RefrigeratedCargoBay(size, 0, num)),
    INSULATED_CARGO("Cargo (Insulated)",
          1 / 0.87,
          0,
          CargoBay.techAdvancement(),
          (size, num) -> new InsulatedCargoBay(size, 0, num)),
    LIVESTOCK_CARGO("Cargo Livestock)",
          1 / 0.83,
          0,
          CargoBay.techAdvancement(),
          (size, num) -> new LivestockCargoBay(size, 0, num));

    private final String name;
    private final double weight;
    private final int personnel;
    private final TechAdvancement techAdvancement;
    private final BiFunction<Double, Integer, Bay> init;

    static final List<BayData> noMinDoorBayTypes = new ArrayList<>();

    static {
        noMinDoorBayTypes.addAll(List.of(INFANTRY_FOOT,
              INFANTRY_JUMP,
              INFANTRY_MECHANIZED,
              INFANTRY_MOTORIZED,
              IS_BATTLE_ARMOR,
              CLAN_BATTLE_ARMOR,
              CS_BATTLE_ARMOR,
              CARGO,
              INSULATED_CARGO,
              LIQUID_CARGO,
              LIVESTOCK_CARGO,
              REFRIGERATED_CARGO));
    }

    BayData(String name, double weight, int personnel, TechAdvancement techAdvancement,
          BiFunction<Double, Integer, Bay> init) {
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
     * The weight of a single unit of capacity. For unit transport bays this is the weight of a single cubicle. For
     * cargo this is the weight of one ton of cargo capacity.
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
     * Return the minimum number of doors required for the Bay type this BayType encapsulates.
     *
     * @return 0 if no minimum door count applies, or 1 otherwise.
     */
    public int getMinDoors() {
        return noMinDoorBayTypes.contains(this) ? 0 : 1;
    }

    /**
     * Creates a new bay of the type.
     *
     * @param size   The size of bay in cubicles (units) or tons (cargo; this is bay tonnage, not capacity).
     * @param bayNum The bay number; this should be unique for the unit.
     *
     * @return The new bay.
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
     *
     * @return The enum value for the bay. Returns null if the bay is not transport by (e.g. crew quarters)
     *
     * @deprecated Use {@link Bay#getBayType()} instead
     */
    @Deprecated(since = "0.50.05")
    public static @Nullable BayData getBayType(Bay bay) {
        return bay.getBayData();
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
        return (ordinal() >= INFANTRY_FOOT.ordinal()) && (ordinal() <= CS_BATTLE_ARMOR.ordinal());
    }

    /**
     * Determines whether the bay is legal to mount on a given <code>Entity</code>. Whether it is technically possible
     * or practical is another matter.
     *
     * @param en The entity
     *
     * @return Whether the bay is legal
     */
    public boolean isLegalFor(Entity en) {
        //TODO: Container cargo bays aren't implemented, but when added they can be carried by
        // industrial but not BattleMeks.
        if (en.hasETypeFlag(Entity.ETYPE_MEK)) {
            return isCargoBay() && (this != LIVESTOCK_CARGO);
        } else if ((this == DROPSHUTTLE) ||
                         (this == REPAIR_UNPRESSURIZED) ||
                         (this == REPAIR_PRESSURIZED) ||
                         (this == REPAIR_REINFORCED)) {
            return en.hasETypeFlag(Entity.ETYPE_JUMPSHIP);
        } else {
            return en.hasETypeFlag(Entity.ETYPE_TANK) || en.hasETypeFlag(Entity.ETYPE_AERO);
        }
    }

    /**
     * @return Whether the bay type requires a designated armor facing.
     */
    public boolean requiresFacing() {
        return (this == DROPSHUTTLE) ||
                     (this == REPAIR_UNPRESSURIZED) ||
                     (this == REPAIR_PRESSURIZED) ||
                     (this == REPAIR_REINFORCED) ||
                     (this == ARTS_REPAIR_PRESSURIZED) ||
                     (this == ARTS_REPAIR_UNPRESSURIZED);
    }

    /**
     * @return Whether the bay capacity can be changed.
     */
    public boolean hasVariableSize() {
        return this != DROPSHUTTLE;
    }
}

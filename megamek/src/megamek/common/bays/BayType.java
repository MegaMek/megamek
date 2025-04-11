/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.function.Predicate;

import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EquipmentMessages;
import megamek.common.ITechnology;
import megamek.common.ITechnologyDelegator;
import megamek.common.Infantry;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;

/**
 * Data for various transport bay types. This is used by MekHQ for cubicle parts, but can also be used in the future for
 * a generic bay that hold multiple types of units.
 *
 * @author Neoancient
 */
public enum BayType implements ITechnologyDelegator {
    STANDARD_CARGO(BayType.CATEGORY_CARGO, 1.0, 1.0, 0, 0, e -> false, CargoBay.techAdvancement()),
    LIQUID_CARGO(BayType.CATEGORY_CARGO, 1.0, 0.91, 0, 100, e -> false, LiquidCargoBay.techAdvancement()),
    REFRIGERATED_CARGO(BayType.CATEGORY_CARGO, 1.0, 0.87, 0, 250, e -> false, RefrigeratedCargoBay.techAdvancement()),
    INSULATED_CARGO(BayType.CATEGORY_CARGO, 1.0, 0.87, 0, 200, e -> false, RefrigeratedCargoBay.techAdvancement()),
    LIVESTOCK_CARGO(BayType.CATEGORY_CARGO, 1.0, 0.83, 0, 2500, e -> false, LivestockCargoBay.techAdvancement()),
    INFANTRY_FOOT(BayType.CATEGORY_INFANTRY,
          5.0,
          1.0,
          28,
          15000,
          e -> e.hasETypeFlag(Entity.ETYPE_INFANTRY) &&
                     !e.hasETypeFlag(Entity.ETYPE_BATTLEARMOR) &&
                     (e.getMovementMode() == EntityMovementMode.INF_LEG),
          InfantryBay.techAdvancement()),
    INFANTRY_JUMP(BayType.CATEGORY_INFANTRY,
          6.0,
          1.0,
          21,
          15000,
          e -> e.hasETypeFlag(Entity.ETYPE_INFANTRY) &&
                     !e.hasETypeFlag(Entity.ETYPE_BATTLEARMOR) &&
                     (e.getMovementMode() == EntityMovementMode.INF_JUMP),
          InfantryBay.techAdvancement()),
    INFANTRY_MOTORIZED(BayType.CATEGORY_INFANTRY,
          7.0,
          1.0,
          28,
          15000,
          e -> e.hasETypeFlag(Entity.ETYPE_INFANTRY) && (e.getMovementMode() == EntityMovementMode.INF_MOTORIZED),
          InfantryBay.techAdvancement()),
    INFANTRY_MECHANIZED(BayType.CATEGORY_INFANTRY,
          8.0,
          1.0,
          5,
          15000,
          e -> e.hasETypeFlag(Entity.ETYPE_INFANTRY) && ((Infantry) e).isMechanized(),
          InfantryBay.techAdvancement()),
    BATTLEARMOR_IS(BayType.CATEGORY_INFANTRY,
          8.0,
          1.0,
          6,
          15000,
          e -> e.hasETypeFlag(Entity.ETYPE_BATTLEARMOR) && (((BattleArmor) e).getSquadSize() <= 4),
          BattleArmorBay.techAdvancement()),
    BATTLEARMOR_CLAN(BayType.CATEGORY_INFANTRY,
          10.0,
          1.0,
          6,
          15000,
          e -> e.hasETypeFlag(Entity.ETYPE_BATTLEARMOR) && (((BattleArmor) e).getSquadSize() <= 5),
          BattleArmorBay.techAdvancement()),
    BATTLEARMOR_CS(BayType.CATEGORY_INFANTRY,
          12.0,
          1.0,
          6,
          15000,
          e -> e.hasETypeFlag(Entity.ETYPE_BATTLEARMOR),
          BattleArmorBay.techAdvancement()),
    MEK(BayType.CATEGORY_NON_INFANTRY,
          150.0,
          1.0,
          2,
          20000,
          e -> e.hasETypeFlag(Entity.ETYPE_MEK),
          MekBay.techAdvancement()),
    FIGHTER(BayType.CATEGORY_NON_INFANTRY, 150.0, 1.0, 2, 20000, Entity::isFighter, ASFBay.techAdvancement()),
    PROTOMEK(BayType.CATEGORY_NON_INFANTRY,
          50.0,
          5.0,
          6,
          10000,
          e -> e.hasETypeFlag(Entity.ETYPE_PROTOMEK),
          ProtoMekBay.techAdvancement()),
    SMALL_CRAFT(BayType.CATEGORY_NON_INFANTRY,
          200.0,
          1.0,
          5,
          20000,
          e -> e.hasETypeFlag(Entity.ETYPE_AERO) && (e.getWeight() <= 200.0),
          SmallCraftBay.techAdvancement()),
    VEHICLE_LIGHT(BayType.CATEGORY_NON_INFANTRY,
          50.0,
          1.0,
          5,
          10000,
          e -> e.hasETypeFlag(Entity.ETYPE_TANK) && (e.getWeight() <= 50.0),
          LightVehicleBay.techAdvancement()),
    VEHICLE_HEAVY(BayType.CATEGORY_NON_INFANTRY,
          100.0,
          1.0,
          8,
          10000,
          e -> e.hasETypeFlag(Entity.ETYPE_TANK) && (e.getWeight() <= 100.0),
          HeavyVehicleBay.techAdvancement()),
    VEHICLE_SH(BayType.CATEGORY_NON_INFANTRY,
          200.0,
          1.0,
          15,
          20000,
          e -> e.hasETypeFlag(Entity.ETYPE_TANK) && (e.getWeight() <= 200.0),
          SuperHeavyVehicleBay.techAdvancement());

    public static final int CATEGORY_CARGO = 0;
    public static final int CATEGORY_INFANTRY = 1;
    public static final int CATEGORY_NON_INFANTRY = 2;

    private final int category;
    private final double weight;
    private final double capacity;
    private final int personnel;
    private final long cost;
    private final Predicate<Entity> canLoad;
    private final TechAdvancement techAdvancement;

    BayType(int category, double weight, double capacity, int personnel, long cost, Predicate<Entity> canLoad,
          TechAdvancement techAdvancement) {
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
     * <ul>
     *     <li>CATEGORY_CARGO is for bulk transport and can be sized in fractional increments (usually half ton).
     *     Capacity is the fraction of bay tonnage that is available for storage.</li>
     *     <li>CATEGORY_INFANTRY calculates cost based on tonnage rather than unit capacity and has 0 cost for
     *     aerospace units.</li>
     *     <li>CATEGORY_NON_INFANTRY calculates cost per cubicle. Capacity is the number of entities that can fit in a
     *     single cubicle.</li>
     * </ul>
     *
     * @return The category index.
     */
    public int getCategory() {
        return category;
    }

    /**
     * @return The tonnage weight of a single unit of the bay type. For cargo the base unit is a single ton; for units
     *       the base unit is a number of Entity units equal to the capacity.
     */
    public double getWeight() {
        return weight;
    }

    /**
     * @return The capacity of a single unit of the bay type. For cargo this is the number of tons of cargo that can be
     *       stored in a single ton of bay capacity. For unit transport bays this is the Entity count for the cubicle
     *       (usually one).
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
     * @return The base cost of a single unit of the bay type. Note that the cost for infantry units (conventional and
     *       BattleArmor) is per ton and not per platoon/squad and aerospace units do not pay for the cost of infantry
     *       transport bays.
     */
    public long getCost() {
        return cost;
    }

    /**
     * @param en An entity to load into a bay.
     *
     * @return true if the Entity can be housed in the type of bay.
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
        return EquipmentMessages.getString("BayType." + name());
    }

    public static @Nullable BayType parse(String name) {
        if (null != name) {
            for (BayType bt : values()) {
                if (bt.toString().equalsIgnoreCase(name)) {
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
     *
     * @return The BayType for the bay.
     *
     * @deprecated Use the {@link Bay#getBayType()} directly.
     */
    @Deprecated(since = "0.50.05")
    public static BayType getTypeForBay(Bay bay) {
        return bay.getBayType();
    }
}

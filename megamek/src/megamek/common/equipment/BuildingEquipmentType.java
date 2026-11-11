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

package megamek.common.equipment;

import megamek.common.SimpleTechLevel;
import megamek.common.SourceBookCode;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.units.Entity;
import megamek.common.util.RoundWeight;

/** Building facilities use ordinary equipment/size blocks, rather than parallel storage fields. */
public class BuildingEquipmentType extends MiscType {
    public enum Facility {
        UNSPECIFIED("Unspecified Building Equipment", "Unspecified equipment", 0, 0),
        FLIGHT_DECK("Building Flight Deck", "Flight deck", 1500, 1000000),
        HELIPAD("Building Helipad", "Helipad", 500, 200000),
        LANDING_DECK("Building Landing Deck", "Landing deck", 500, 500000),
        MODULAR_LINKAGE("Modular Structure Linkage", "Modular structure linkage", 1, 0);

        private final String id;
        private final String label;
        private final double weight;
        private final double cost;

        Facility(String id, String label, double weight, double cost) {
            this.id = id;
            this.label = label;
            this.weight = weight;
            this.cost = cost;
        }

        public String internalName() {
            return id;
        }

        public boolean isRoof() {
            return this == FLIGHT_DECK || this == HELIPAD || this == LANDING_DECK;
        }

        public boolean isSpreadable() {
            return this == FLIGHT_DECK || this == LANDING_DECK;
        }
    }

    private final Facility facility;

    private BuildingEquipmentType(Facility facility) {
        this.facility = facility;
        name = facility.label;
        setInternalName(facility.id);
        tonnage = TONNAGE_VARIABLE;
        cost = COST_VARIABLE;
        criticalSlots = 1;
        bv = 0;
        spreadable = facility.isSpreadable();
        if (facility.weight == 0 || facility == Facility.LANDING_DECK) {
            flags = flags.or(F_VARIABLE_SIZE);
        }
        rulesRefs = facility.isRoof() ? rulesRefs(SourceBookCode.TO_AUE, 124, 131)
              : facility == Facility.MODULAR_LINKAGE ? rulesRefs(SourceBookCode.TO_AUE, 81)
                    : rulesRefs(SourceBookCode.TO_AR, 129, 133, 208);
        techAdvancement.setTechBase(TechBase.ALL).setAdvancement(DATE_PS, DATE_PS, DATE_PS)
              .setTechRating(TechRating.B).setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        if (facility == Facility.LANDING_DECK) {
            techAdvancement.setAdvancement(DATE_ES, DATE_ES, DATE_ES)
                  .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C);
        }
    }

    public static void initializeTypes() {
        for (var facility : Facility.values()) {
            EquipmentType.addType(new BuildingEquipmentType(facility));
        }
    }

    public Facility getFacility() {
        return facility;
    }

    @Override
    public double getTonnage(Entity entity, int location, double size, RoundWeight rounding) {
        if (facility == Facility.MODULAR_LINKAGE) {
            return Math.ceil(entity.getOInternal(location) / 2.0);
        }
        return facility == Facility.LANDING_DECK ? facility.weight * size : facility.weight == 0 ? size : facility.weight;
    }

    @Override
    public double getCost(Entity entity, boolean armored, int location, double size) {
        // Unspecified-equipment cost is charged once per occupied hex, independently of the number of floors/items.
        return facility.weight == 0 || facility == Facility.LANDING_DECK ? facility.cost * size : facility.cost;
    }

    @Override
    public Double variableStepSize() {
        return facility == Facility.LANDING_DECK ? 1.0 : .5;
    }

    @Override
    public Double variableMaxSize() {
        return facility == Facility.LANDING_DECK ? 37.0 : null;
    }

    @Override
    public double getBV(Entity entity) {
        return 0;
    }
}

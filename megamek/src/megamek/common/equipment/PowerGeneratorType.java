/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.Messages;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.equipment.enums.StructureEngine;

/**
 * TO:AR 131-132 Advanced Buildings power generators.
 */
public class PowerGeneratorType extends MiscType {

    public static final MiscTypeFlag F_POWER_GENERATOR = MiscTypeFlag.F_POWER_GENERATOR;

    private final StructureEngine structureEngine;

    public PowerGeneratorType(StructureEngine structureEngine) {this.structureEngine = structureEngine;}


    public static void initializeTypes() {
        for (StructureEngine engineType : StructureEngine.values()) {
            EquipmentType.addType(createPowerGenerator(engineType));
        }
    }

    public static PowerGeneratorType createPowerGenerator(StructureEngine engineType) {
        PowerGeneratorType powerGeneratorType = new PowerGeneratorType(engineType);
        powerGeneratorType.name =
              Messages.getString("EquipmentType.StructureEngine." + engineType.name()) + " " + Messages.getString(
                    "EquipmentType.PowerGenerator");
        powerGeneratorType.setInternalName(engineType.name() + " PowerGenerator");
        powerGeneratorType.tonnage = TONNAGE_VARIABLE;
        powerGeneratorType.criticalSlots = CRITICAL_SLOTS_VARIABLE;
        powerGeneratorType.cost = COST_VARIABLE;
        powerGeneratorType.bv = BV_VARIABLE;
        powerGeneratorType.spreadable = true;
        powerGeneratorType.flags = powerGeneratorType.flags.or(F_POWER_GENERATOR).or(F_VARIABLE_SIZE);
        powerGeneratorType.rulesRefs = "131-133, TO:AR";

        // Let's create a dummy engine to get the tech advancement
        Engine engine = new Engine(0, engineType.getEngineType(), 0);

        // TODO: Fix tech advancement, I have no clue how this is supposed to work
        powerGeneratorType.techAdvancement = engine.getTechAdvancement();

        return powerGeneratorType;
    }

    public StructureEngine getStructureEngine() {
        return structureEngine;
    }
}

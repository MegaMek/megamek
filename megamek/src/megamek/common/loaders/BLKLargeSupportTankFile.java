/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.loaders;

import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.FuelType;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.LargeSupportTank;
import megamek.common.units.Tank;
import megamek.common.util.BuildingBlock;
import megamek.logging.MMLogger;

/**
 * @author njrkrynn
 * @since April 6, 2002, 2:06 AM
 */
public class BLKLargeSupportTankFile extends BLKFile implements IMekLoader {
    private static final MMLogger logger = MMLogger.create(BLKLargeSupportTankFile.class);

    public BLKLargeSupportTankFile(BuildingBlock bb) {
        dataFile = bb;
    }

    @Override
    protected int defaultVGLFacing(int location, boolean rearFacing) {
        return switch (location) {
            case LargeSupportTank.LOC_FRONT_RIGHT -> 1;
            case LargeSupportTank.LOC_REAR_RIGHT, LargeSupportTank.LOC_REAR -> 2;
            case LargeSupportTank.LOC_REAR_LEFT, LargeSupportTank.LOC_FRONT_LEFT -> 4;
            default -> 0;
        };
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {

        LargeSupportTank t = new LargeSupportTank();
        setBasicEntityData(t);

        if (!dataFile.exists("tonnage")) {
            throw new EntityLoadingException("Could not find weight block.");
        }
        t.setWeight(dataFile.getDataAsDouble("tonnage")[0]);

        if (!dataFile.exists("motion_type")) {
            throw new EntityLoadingException("Could not find movement block.");
        }
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        EntityMovementMode nMotion = EntityMovementMode.parseFromString(sMotion);
        if (nMotion == EntityMovementMode.NONE) {
            throw new EntityLoadingException("Invalid movement type: " + sMotion);
        }
        t.setMovementMode(nMotion);

        addTransports(t);

        int engineCode = BLKFile.FUSION;
        if (dataFile.exists("engine_type")) {
            engineCode = dataFile.getDataAsInt("engine_type")[0];
        }
        // TODO: At some point fix this to throw an error if missing
        if (dataFile.exists("fuel")) {
            t.setFuelTonnage(dataFile.getDataAsDouble("fuel")[0]);
        }
        int engineFlags = Engine.TANK_ENGINE | Engine.SUPPORT_VEE_ENGINE;
        if (t.isClan()) {
            engineFlags |= Engine.CLAN_ENGINE;
        }
        if (!dataFile.exists("cruiseMP")) {
            throw new EntityLoadingException("Could not find cruiseMP block.");
        }
        int engineRating = (dataFile.getDataAsInt("cruiseMP")[0] * (int) t.getWeight()) - t.getSuspensionFactor();
        if ((engineRating % 5) > 0) {
            engineRating += (5 - (engineRating % 5));
        }
        t.setEngine(new Engine(engineRating, BLKFile.translateEngineCode(engineCode), engineFlags));
        t.setOriginalWalkMP(dataFile.getDataAsInt("cruiseMP")[0]);

        loadSVArmor(t);

        if (dataFile.exists("internal_type")) {
            t.setStructureType(dataFile.getDataAsInt("internal_type")[0]);
        } else {
            t.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);
        }

        if (!dataFile.exists("armor")) {
            throw new EntityLoadingException("Could not find armor block.");
        }

        int[] armor = dataFile.getDataAsInt("armor");

        if ((armor.length < 6) || (armor.length > 8)) {
            throw new EntityLoadingException("Incorrect armor array length");
        }

        t.setHasNoTurret(armor.length == 6);
        t.setHasNoDualTurret(armor.length == 6 || armor.length == 7);

        // add the body to the armor array
        int[] fullArmor = new int[armor.length + 1];
        fullArmor[0] = 0;
        System.arraycopy(armor, 0, fullArmor, 1, armor.length);
        for (int x = 0; x < fullArmor.length; x++) {
            t.initializeArmor(fullArmor[x], x);
        }

        // Set the structural tech rating
        if (!dataFile.exists("structural_tech_rating")) {
            throw new EntityLoadingException("Could not find " +
                  "structural_tech_rating block!");
        }
        t.setStructuralTechRating(dataFile
              .getDataAsInt("structural_tech_rating")[0]);
        // Set armor tech rating, if it exists (defaults to structural tr)
        if (dataFile.exists("armor_tech_rating")) {
            t.setArmorTechRating(dataFile
                  .getDataAsInt("armor_tech_rating")[0]);
        }
        // Set engine tech rating, if it exists (defaults to structural tr)
        if (dataFile.exists("engine_tech_rating")) {
            t.setEngineTechRating(dataFile
                  .getDataAsInt("engine_tech_rating")[0]);
        }

        t.autoSetInternal();
        t.recalculateTechAdvancement();

        loadEquipment(t, "Front", Tank.LOC_FRONT);
        loadEquipment(t, "Front Right", LargeSupportTank.LOC_FRONT_RIGHT);
        loadEquipment(t, "Front Left", LargeSupportTank.LOC_FRONT_LEFT);
        loadEquipment(t, "Rear Right", LargeSupportTank.LOC_REAR_RIGHT);
        loadEquipment(t, "Rear Left", LargeSupportTank.LOC_REAR_LEFT);
        loadEquipment(t, "Rear", LargeSupportTank.LOC_REAR);
        if (!t.hasNoTurret()) {
            loadEquipment(t, "Turret", LargeSupportTank.LOC_TURRET);
        }
        loadEquipment(t, "Body", Tank.LOC_BODY);

        if (dataFile.exists("omni")) {
            t.setOmni(true);
        }
        t.setArmorTonnage(t.getArmorWeight());

        if (dataFile.exists("baseChassisTurretWeight")) {
            t.setBaseChassisTurretWeight(dataFile.getDataAsDouble("baseChassisTurretWeight")[0]);
        }

        if (dataFile.exists("baseChassisTurret2Weight")) {
            t.setBaseChassisTurret2Weight(dataFile.getDataAsDouble("baseChassisTurret2Weight")[0]);
        }

        if (dataFile.exists("baseChassisSponsonPintleWeight")) {
            t.setBaseChassisSponsonPintleWeight(dataFile.getDataAsDouble("baseChassisSponsonPintleWeight")[0]);
        }

        if (dataFile.exists("hasNoControlSystems")) {
            t.setHasNoControlSystems(true);
        }

        if (dataFile.exists("baseChassisFireConWeight")) {
            t.setBaseChassisFireConWeight((dataFile.getDataAsDouble("baseChassisFireConWeight")[0]));
        }

        if (dataFile.exists("fuelType")) {
            try {
                t.setICEFuelType(FuelType.valueOf(dataFile.getDataAsString("fuelType")[0]));
            } catch (IllegalArgumentException ex) {
                logger.error("While loading {}: Could not parse ICE fuel type {}",
                      t.getShortNameRaw(),
                      dataFile.getDataAsString("fuelType")[0]);
                t.setICEFuelType(FuelType.PETROCHEMICALS);
            }
        }
        loadQuirks(t);

        resetCrew(t);

        return t;
    }
}

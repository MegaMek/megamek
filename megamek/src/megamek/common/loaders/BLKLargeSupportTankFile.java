/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2019 The MegaMek Team
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

package megamek.common.loaders;

import megamek.MegaMek;
import megamek.common.*;
import megamek.common.util.BuildingBlock;

/**
 * BLkFile.java
 *
 * Created on April 6, 2002, 2:06 AM
 *
 * @author njrkrynn
 */
public class BLKLargeSupportTankFile extends BLKFile implements IMechLoader {
    public BLKLargeSupportTankFile(BuildingBlock bb) {
        dataFile = bb;
    }

    @Override
    protected int defaultVGLFacing(int location, boolean rearFacing) {
        switch (location) {
            case LargeSupportTank.LOC_FRONTRIGHT:
                return 1;
            case LargeSupportTank.LOC_REARRIGHT:
                return 2;
            case LargeSupportTank.LOC_REAR:
                return 2;
            case LargeSupportTank.LOC_REARLEFT:
                return 4;
            case LargeSupportTank.LOC_FRONTLEFT:
                return 4;
            case LargeSupportTank.LOC_FRONT:
            case LargeSupportTank.LOC_TURRET:
            case LargeSupportTank.LOC_TURRET_2:
            default:
                return 0;
        }
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {

        LargeSupportTank t = new LargeSupportTank();

        if (dataFile.exists("source")) {
            t.setSource(dataFile.getDataAsString("source")[0]);
        }

        if (!dataFile.exists("Name")) {
            throw new EntityLoadingException("Could not find name block.");
        }
        t.setChassis(dataFile.getDataAsString("Name")[0]);
        if (dataFile.exists("Model") && (dataFile.getDataAsString("Model")[0] != null)) {
            t.setModel(dataFile.getDataAsString("Model")[0]);
        } else {
            t.setModel("");
        }

        setTechLevel(t);
        setFluff(t);
        checkManualBV(t);

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

        boolean patchworkArmor = false;
        if (dataFile.exists("armor_type")) {
            if (dataFile.getDataAsInt("armor_type")[0] == EquipmentType.T_ARMOR_PATCHWORK) {
                patchworkArmor = true;
            } else {
                t.setArmorType(dataFile.getDataAsInt("armor_type")[0]);
            }
        } else {
            t.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        }
        if (!patchworkArmor && dataFile.exists("armor_tech")) {
            t.setArmorTechLevel(dataFile.getDataAsInt("armor_tech")[0]);
        }
        if (!patchworkArmor) {
            if (!dataFile.exists("barrating")) {
                throw new EntityLoadingException("Could not find barrating block.");
            }
            t.setBARRating(dataFile.getDataAsInt("barrating")[0]);
        } else {
            for (int i = 1; i < t.locations(); i++) {
                t.setArmorType(dataFile.getDataAsInt(t.getLocationName(i) + "_armor_type")[0], i);
                t.setArmorTechLevel(dataFile.getDataAsInt(t.getLocationName(i) + "_armor_type")[0], i);
                t.setBARRating(dataFile.getDataAsInt(t.getLocationName(i) + "_barrating")[0], i);
            }
        }
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
        loadEquipment(t, "Front Right", LargeSupportTank.LOC_FRONTRIGHT);
        loadEquipment(t, "Front Left", LargeSupportTank.LOC_FRONTLEFT);
        loadEquipment(t, "Rear Right", LargeSupportTank.LOC_REARRIGHT);
        loadEquipment(t, "Rear Left", LargeSupportTank.LOC_REARLEFT);
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
                MegaMek.getLogger().error("While loading " + t.getShortNameRaw()
                                + ": Could not parse ICE fuel type "
                                + dataFile.getDataAsString("fuelType")[0]);
                t.setICEFuelType(FuelType.PETROCHEMICALS);
            }
        }

        return t;
    }
}

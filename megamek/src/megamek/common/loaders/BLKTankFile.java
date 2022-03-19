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

import megamek.common.*;
import megamek.common.util.BuildingBlock;
import org.apache.logging.log4j.LogManager;

/**
 * @author njrkrynn
 * @since April 6, 2002, 2:06 AM
 */
public class BLKTankFile extends BLKFile implements IMechLoader {
    
    private boolean superheavy = false;
    
    public BLKTankFile(BuildingBlock bb) {
        dataFile = bb;
    }

    @Override
    protected int defaultVGLFacing(int location, boolean rearFacing) {
        if (superheavy) {
            switch (location) {
                case SuperHeavyTank.LOC_FRONTRIGHT:
                    return 1;
                case SuperHeavyTank.LOC_REARRIGHT:
                case SuperHeavyTank.LOC_REAR:
                    return 2;
                case SuperHeavyTank.LOC_REARLEFT:
                case SuperHeavyTank.LOC_FRONTLEFT:
                    return 4;
                case SuperHeavyTank.LOC_FRONT:
                case SuperHeavyTank.LOC_TURRET:
                case SuperHeavyTank.LOC_TURRET_2:
                default:
                    return 0;
            }
        } else {
            switch (location) {
                case Tank.LOC_RIGHT:
                    return 2;
                case Tank.LOC_REAR:
                    return 3;
                case Tank.LOC_LEFT:
                    return 5;
                case Tank.LOC_FRONT:
                case Tank.LOC_TURRET:
                case Tank.LOC_TURRET_2:
                default:
                    return 0;
            }
        }
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {
        if (!dataFile.exists("tonnage")) {
            throw new EntityLoadingException("Could not find weight block.");
        }
        double weight = dataFile.getDataAsDouble("tonnage")[0];
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        EntityMovementMode nMotion = EntityMovementMode.parseFromString(sMotion);
        if (nMotion == EntityMovementMode.NONE) {
            throw new EntityLoadingException("Invalid movement type: " + sMotion);
        }

        Tank t = new Tank();

        switch (nMotion) {
            case HOVER:
                if (weight > 50) {
                    t = new SuperHeavyTank();
                    superheavy = true;
                }
                break;
            case NAVAL:
            case SUBMARINE:
                if (weight > 300) {
                    t = new SuperHeavyTank();
                    superheavy = true;
                }
                break;
            case TRACKED:
                if (weight > 100) {
                    t = new SuperHeavyTank();
                    superheavy = true;
                }
                break;
            case WHEELED:
            case WIGE:
                if (weight > 80) {
                    t = new SuperHeavyTank();
                    superheavy = true;
                }
                break;
            default:
                break;
        }

        t.setWeight(weight);

        if (!dataFile.exists("Name")) {
            throw new EntityLoadingException("Could not find name block.");
        }
        t.setChassis(dataFile.getDataAsString("Name")[0]);
        if (dataFile.exists("Model") && (dataFile.getDataAsString("Model")[0] != null)) {
            t.setModel(dataFile.getDataAsString("Model")[0]);
        } else {
            t.setModel("");
        }

        if (dataFile.exists(MtfFile.MUL_ID)) {
            t.setMulId(dataFile.getDataAsInt(MtfFile.MUL_ID)[0]);
        }

        setTechLevel(t);
        setFluff(t);
        checkManualBV(t);

        if (!dataFile.exists("motion_type")) {
            throw new EntityLoadingException("Could not find movement block.");
        }

        t.setMovementMode(nMotion);

        addTransports(t);
        if (dataFile.exists(BLK_EXTRA_SEATS)) {
            t.setExtraCrewSeats(dataFile.getDataAsInt(BLK_EXTRA_SEATS)[0]);
        }

        int engineCode = BLKFile.FUSION;
        if (dataFile.exists("engine_type")) {
            engineCode = dataFile.getDataAsInt("engine_type")[0];
        }
        int engineFlags = Engine.TANK_ENGINE;
        // Support for mixed tech units with an engine with a different tech base
        if (dataFile.exists("clan_engine")) {
            if (Boolean.parseBoolean(dataFile.getDataAsString("clan_engine")[0])) {
                engineFlags |= Engine.CLAN_ENGINE;
            }
        } else if (t.isClan()) {
            engineFlags |= Engine.CLAN_ENGINE;
        }
        if (!dataFile.exists("cruiseMP")) {
            throw new EntityLoadingException("Could not find cruiseMP block.");
        }
        int engineRating = Math.max(10, (dataFile.getDataAsInt("cruiseMP")[0] * (int) t.getWeight()) - t.getSuspensionFactor());
        if (dataFile.getDataAsInt("cruiseMP")[0] == 0) {
            engineRating = engineCode == BLKFile.NONE ? 0 : 10;
        }
        if ((engineRating % 5) > 0) {
            engineRating += (5 - (engineRating % 5));
        }
        t.setEngine(new Engine(engineRating, BLKFile.translateEngineCode(engineCode), engineFlags));
        t.setOriginalWalkMP(dataFile.getDataAsInt("cruiseMP")[0]);

        if (dataFile.exists("internal_type")) {
            t.setStructureType(dataFile.getDataAsInt("internal_type")[0]);
        } else {
            t.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);
        }

        if (!dataFile.exists("armor")) {
            throw new EntityLoadingException("Could not find armor block.");
        }

        int[] armor = dataFile.getDataAsInt("armor");

        if (!t.isSuperHeavy()) {
            if ((armor.length < 4) || (armor.length > 6)) {
                throw new EntityLoadingException("Incorrect armor array length");
            }
            t.setHasNoTurret(armor.length == 4);
            t.setHasNoDualTurret((armor.length == 4) || (armor.length == 5));
        } else {
            if ((armor.length < 6) || (armor.length > 8)) {
                throw new EntityLoadingException("Incorrect armor array length");
            }
            t.setHasNoTurret(armor.length == 6);
            t.setHasNoDualTurret((armor.length == 6) || (armor.length == 7));
        }

        // add the body to the armor array
        int[] fullArmor = new int[armor.length + 1];
        fullArmor[0] = 0;
        System.arraycopy(armor, 0, fullArmor, 1, armor.length);
        for (int x = 0; x < fullArmor.length; x++) {
            t.initializeArmor(fullArmor[x], x);
        }

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
        if (patchworkArmor) {
            for (int i = 1; i < t.locations(); i++) {
                t.setArmorType(dataFile.getDataAsInt(t.getLocationName(i) + "_armor_type")[0], i);
                t.setArmorTechLevel(dataFile.getDataAsInt(t.getLocationName(i) + "_armor_type")[0], i);
            }
        }
        
        t.autoSetInternal();
        t.recalculateTechAdvancement();

        if (superheavy) {
            loadEquipment(t, "Front", Tank.LOC_FRONT);
            loadEquipment(t, "Front Right", SuperHeavyTank.LOC_FRONTRIGHT);
            loadEquipment(t, "Front Left", SuperHeavyTank.LOC_FRONTLEFT);
            loadEquipment(t, "Rear Left", SuperHeavyTank.LOC_REARLEFT);
            loadEquipment(t, "Rear Left", SuperHeavyTank.LOC_REARRIGHT);
            loadEquipment(t, "Rear", SuperHeavyTank.LOC_REAR);
            if (t.hasNoDualTurret()) {
                if (!t.hasNoTurret()) {
                    loadEquipment(t, "Turret", SuperHeavyTank.LOC_TURRET);
                }
            } else {
                loadEquipment(t, "Rear Turret", SuperHeavyTank.LOC_TURRET);
                loadEquipment(t, "Front Turret", SuperHeavyTank.LOC_TURRET_2);
            }

        } else {
            loadEquipment(t, "Front", Tank.LOC_FRONT);
            loadEquipment(t, "Right", Tank.LOC_RIGHT);
            loadEquipment(t, "Left", Tank.LOC_LEFT);
            loadEquipment(t, "Rear", Tank.LOC_REAR);
            if (t.hasNoDualTurret()) {
                if (!t.hasNoTurret()) {
                    loadEquipment(t, "Turret", Tank.LOC_TURRET);
                }
            } else {
                loadEquipment(t, "Rear Turret", Tank.LOC_TURRET);
                loadEquipment(t, "Front Turret", Tank.LOC_TURRET_2);
            }
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

        if (dataFile.exists("fuelType")) {
            try {
                t.setICEFuelType(FuelType.valueOf(dataFile.getDataAsString("fuelType")[0]));
            } catch (IllegalArgumentException ex) {
                LogManager.getLogger().error("While loading " + t.getShortNameRaw()
                                + ": Could not parse ICE fuel type "
                                + dataFile.getDataAsString("fuelType")[0]);
                t.setICEFuelType(FuelType.PETROCHEMICALS);
            }
        }

        if (dataFile.exists("hasNoControlSystems")) {
            t.setHasNoControlSystems(true);
        }

        if (dataFile.exists("trailer")) {
            t.setTrailer(true);
        }

        return t;
    }
}

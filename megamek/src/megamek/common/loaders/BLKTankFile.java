/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.equipment.ArmorType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.FuelType;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.SuperHeavyTank;
import megamek.common.units.Tank;
import megamek.common.util.BuildingBlock;
import megamek.logging.MMLogger;

/**
 * @author njrkrynn
 * @since April 6, 2002, 2:06 AM
 */
public class BLKTankFile extends BLKFile implements IMekLoader {
    private static final MMLogger logger = MMLogger.create(BLKTankFile.class);

    private boolean superheavy = false;

    public BLKTankFile(BuildingBlock bb) {
        dataFile = bb;
    }

    @Override
    protected int defaultVGLFacing(int location, boolean rearFacing) {
        if (superheavy) {
            return switch (location) {
                case SuperHeavyTank.LOC_FRONT_RIGHT -> 1;
                case SuperHeavyTank.LOC_REAR_RIGHT, SuperHeavyTank.LOC_REAR -> 2;
                case SuperHeavyTank.LOC_REAR_LEFT, SuperHeavyTank.LOC_FRONT_LEFT -> 4;
                default -> 0;
            };
        } else {
            return switch (location) {
                case Tank.LOC_RIGHT -> 2;
                case Tank.LOC_REAR -> 3;
                case Tank.LOC_LEFT -> 5;
                default -> 0;
            };
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
        setBasicEntityData(t);

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
        int engineRating = Math.max(10,
              (dataFile.getDataAsInt("cruiseMP")[0] * (int) t.getWeight()) - t.getSuspensionFactor());
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

        setupArmorTypeAndTechLevel(t);

        t.autoSetInternal();

        if (superheavy) {
            loadEquipment(t, "Front", Tank.LOC_FRONT);
            loadEquipment(t, "Front Right", SuperHeavyTank.LOC_FRONT_RIGHT);
            loadEquipment(t, "Front Left", SuperHeavyTank.LOC_FRONT_LEFT);
            loadEquipment(t, "Rear Left", SuperHeavyTank.LOC_REAR_LEFT);
            loadEquipment(t, "Rear Left", SuperHeavyTank.LOC_REAR_RIGHT);
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
                logger.error("While loading {}: Could not parse ICE fuel type {}",
                      t.getShortNameRaw(),
                      dataFile.getDataAsString("fuelType")[0]);
                t.setICEFuelType(FuelType.PETROCHEMICALS);
            }
        }

        if (dataFile.exists("hasNoControlSystems")) {
            t.setHasNoControlSystems(true);
        }

        if (dataFile.exists("trailer")) {
            t.setTrailer(true);
        }
        t.recalculateTechAdvancement();
        loadQuirks(t);

        resetCrew(t);

        return t;
    }

    private void setupArmorTypeAndTechLevel(Tank t) {
        boolean patchworkArmor = false;
        if (dataFile.exists("armor_type")) {
            if (dataFile.getDataAsInt("armor_type")[0] == EquipmentType.T_ARMOR_PATCHWORK) {
                patchworkArmor = true;
            } else {
                var armorTypeId = dataFile.getDataAsInt("armor_type")[0];
                t.setArmorType(armorTypeId);
                var armorType = ArmorType.of(armorTypeId, t.isClan());
                var techLevel = -1;
                if (dataFile.exists("armor_tech_level")) {
                    techLevel = dataFile.getDataAsInt("armor_tech_level")[0];
                } else {
                    techLevel = armorType.getStaticTechLevel().getCompoundTechLevel(t.isClan());
                }
                t.setArmorTechLevel(techLevel);
            }
        } else {
            t.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        }
        setArmorTechLevelFromDataFile(t);
        if (patchworkArmor) {
            for (int i = 1; i < t.locations(); i++) {
                var armorTypeId = dataFile.getDataAsInt(t.getLocationName(i) + "_armor_type")[0];
                var armorType = ArmorType.of(armorTypeId, t.isClan());
                t.setArmorType(armorTypeId, i);
                var techLevel = armorType.getStaticTechLevel().getCompoundTechLevel(t.isClan());
                t.setArmorTechLevel(techLevel, i);
            }
        }
    }
}

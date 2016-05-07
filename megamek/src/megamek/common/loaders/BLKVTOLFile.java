/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Jun 2, 2005
 */
package megamek.common.loaders;

import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EquipmentType;
import megamek.common.Tank;
import megamek.common.VTOL;
import megamek.common.util.BuildingBlock;

/**
 * @author Andrew Hunter
 */
public class BLKVTOLFile extends BLKFile implements IMechLoader {
    public BLKVTOLFile(BuildingBlock bb) {
        dataFile = bb;
    }

    public Entity getEntity() throws EntityLoadingException {
        VTOL t = new VTOL();

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

        if (dataFile.exists("source")) {
            t.setSource(dataFile.getDataAsString("source")[0]);
        }

        if (!dataFile.exists("tonnage")) {
            throw new EntityLoadingException("Could not find weight block.");
        }
        t.setWeight(dataFile.getDataAsDouble("tonnage")[0]);

        if (!dataFile.exists("motion_type")) {
            throw new EntityLoadingException("Could not find movement block.");
        }
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        EntityMovementMode nMotion = EntityMovementMode.getMode(sMotion);
        if (nMotion == EntityMovementMode.NONE) {
            throw new EntityLoadingException("Invalid movement type: " + sMotion);
        }
        t.setMovementMode(nMotion);
        addTransports(t);

        int engineCode = BLKFile.FUSION;
        if (dataFile.exists("engine_type")) {
            engineCode = dataFile.getDataAsInt("engine_type")[0];
        }
        int engineFlags = Engine.TANK_ENGINE;
        if (t.isClan()) {
            engineFlags |= Engine.CLAN_ENGINE;
        }
        if (!dataFile.exists("cruiseMP")) {
            throw new EntityLoadingException("Could not find cruiseMP block.");
        }
        int engineRating = Math.max(10, (dataFile.getDataAsInt("cruiseMP")[0] * (int) t.getWeight()) - t.getSuspensionFactor());
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

        if ((armor.length != 5) && (armor.length != 6)) {
            throw new EntityLoadingException("Incorrect armor array length");
        }
        t.setHasNoTurret(armor.length == 5);
        t.setHasNoDualTurret(true);
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

        loadEquipment(t, "Front", Tank.LOC_FRONT);
        loadEquipment(t, "Right", Tank.LOC_RIGHT);
        loadEquipment(t, "Left", Tank.LOC_LEFT);
        loadEquipment(t, "Rear", Tank.LOC_REAR);
        loadEquipment(t, "Body", Tank.LOC_BODY);
        loadEquipment(t, "Rotor", VTOL.LOC_ROTOR);
        if (armor.length == 6) {
            loadEquipment(t, "Turret", VTOL.LOC_TURRET);
        }

        if (dataFile.exists("omni")) {
            t.setOmni(true);
        }
        t.setArmorTonnage(t.getArmorWeight());
        return t;
    }
}

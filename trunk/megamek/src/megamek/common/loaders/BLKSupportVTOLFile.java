/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */
/*
 * Created on Jun 2, 2005
 *
 */
package megamek.common.loaders;

import java.util.Vector;

import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.SupportVTOL;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.TroopSpace;
import megamek.common.util.BuildingBlock;

/**
 * @author Andrew Hunter
 */
public class BLKSupportVTOLFile extends BLKFile implements IMechLoader {

    private static final String[] MOVES = { "", "", "", "Tracked", "Wheeled",
            "Hover", "VTOL" };

    public BLKSupportVTOLFile(BuildingBlock bb) {
        dataFile = bb;
    }

    public Entity getEntity() throws EntityLoadingException {
        SupportVTOL t = new SupportVTOL();
        
        if (!dataFile.exists("barrating"))
            throw new EntityLoadingException("Could not find barrating block.");
        t.setBARRating(dataFile.getDataAsInt("barrating")[0]);

        if (!dataFile.exists("Name"))
            throw new EntityLoadingException("Could not find name block.");
        t.setChassis(dataFile.getDataAsString("Name")[0]);
        if (dataFile.exists("Model")
                && dataFile.getDataAsString("Model")[0] != null) {
            t.setModel(dataFile.getDataAsString("Model")[0]);
        } else {
            t.setModel("");
        }

        if (!dataFile.exists("year"))
            throw new EntityLoadingException("Could not find year block.");
        t.setYear(dataFile.getDataAsInt("year")[0]);

        if (!dataFile.exists("type"))
            throw new EntityLoadingException("Could not find type block.");

        if (dataFile.getDataAsString("type")[0].equals("IS")) {
            if (t.getYear() == 3025) {
                t.setTechLevel(TechConstants.T_INTRO_BOXSET);
            } else {
                t.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
            }
        } else if (dataFile.getDataAsString("type")[0].equals("IS Level 1")) {
            t.setTechLevel(TechConstants.T_INTRO_BOXSET);
        } else if (dataFile.getDataAsString("type")[0].equals("IS Level 2")) {
            t.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
        } else if (dataFile.getDataAsString("type")[0].equals("IS Level 3")) {
            t.setTechLevel(TechConstants.T_IS_ADVANCED);
        } else if (dataFile.getDataAsString("type")[0].equals("Clan")
                || dataFile.getDataAsString("type")[0].equals("Clan Level 2")) {
            t.setTechLevel(TechConstants.T_CLAN_TW);
        } else if (dataFile.getDataAsString("type")[0].equals("Clan Level 3")) {
            t.setTechLevel(TechConstants.T_CLAN_ADVANCED);
        } else if (dataFile.getDataAsString("type")[0]
                .equals("Mixed (IS Chassis)")) {
            t.setTechLevel(TechConstants.T_IS_ADVANCED);
            t.setMixedTech(true);
        } else if (dataFile.getDataAsString("type")[0]
                .equals("Mixed (Clan Chassis)")) {
            t.setTechLevel(TechConstants.T_CLAN_ADVANCED);
            t.setMixedTech(true);
        } else if (dataFile.getDataAsString("type")[0].equals("Mixed")) {
            throw new EntityLoadingException(
                    "Unsupported tech base: \"Mixed\" is no longer allowed by itself.  You must specify \"Mixed (IS Chassis)\" or \"Mixed (Clan Chassis)\".");
        } else {
            throw new EntityLoadingException("Unsupported tech level: "
                    + dataFile.getDataAsString("type")[0]);
        }

        if (!dataFile.exists("tonnage"))
            throw new EntityLoadingException("Could not find weight block.");
        t.setWeight(dataFile.getDataAsFloat("tonnage")[0]);

        if (!dataFile.exists("motion_type"))
            throw new EntityLoadingException("Could not find movement block.");
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        int nMotion = -1;
        for (int x = 0; x < MOVES.length; x++) {
            if (sMotion.equals(MOVES[x])) {
                nMotion = x;
                break;
            }
        }
        if (nMotion == -1)
            throw new EntityLoadingException("Invalid movment type: " + sMotion);
        t.setMovementMode(nMotion);

        if (dataFile.exists("transporters")) {
            String[] transporters = dataFile.getDataAsString("transporters");
            // Walk the array of transporters.
            for (int index = 0; index < transporters.length; index++) {
                // TroopSpace:
                if (transporters[index].startsWith("TroopSpace:", 0)) {
                    // Everything after the ':' should be the space's size.
                    Double fsize = new Double(transporters[index].substring(11));
                    t.addTransporter(new TroopSpace(fsize.doubleValue()));
                }

            } // Handle the next transportation component.

        } // End has-transporters

        int engineCode = BLKFile.FUSION;
        if (dataFile.exists("engine_type")) {
            engineCode = dataFile.getDataAsInt("engine_type")[0];
        }
        int engineFlags = Engine.TANK_ENGINE;
        if (t.isClan())
            engineFlags |= Engine.CLAN_ENGINE;
        if (!dataFile.exists("cruiseMP"))
            throw new EntityLoadingException("Could not find cruiseMP block.");
        int engineRating = dataFile.getDataAsInt("cruiseMP")[0]
                * (int) t.getWeight() - t.getSuspensionFactor();
        t.setEngine(new Engine(engineRating, BLKFile
                .translateEngineCode(engineCode), engineFlags));

        if (dataFile.exists("armor_type"))
            t.setArmorType(dataFile.getDataAsInt("armor_type")[0]);
        if (dataFile.exists("armor_tech"))
            t.setArmorTechLevel(dataFile.getDataAsInt("armor_tech")[0]);
        if (dataFile.exists("internal_type"))
            t.setStructureType(dataFile.getDataAsInt("internal_type")[0]);

        if (!dataFile.exists("armor"))
            throw new EntityLoadingException("Could not find armor block.");

        int[] armor = dataFile.getDataAsInt("armor");

        if (armor.length != 5) {
            throw new EntityLoadingException("Incorrect armor array length");
        }
        // add the body to the armor array
        int[] fullArmor = new int[armor.length + 1];
        fullArmor[0] = 0;
        System.arraycopy(armor, 0, fullArmor, 1, armor.length);
        for (int x = 0; x < fullArmor.length; x++) {
            t.initializeArmor(fullArmor[x], x);
        }

        t.autoSetInternal();

        loadEquipment(t, "Front", Tank.LOC_FRONT);
        loadEquipment(t, "Right", Tank.LOC_RIGHT);
        loadEquipment(t, "Left", Tank.LOC_LEFT);
        loadEquipment(t, "Rear", Tank.LOC_REAR);
        loadEquipment(t, "Body", Tank.LOC_BODY);
        
        if (dataFile.exists("omni")) {
            t.setOmni(true);
        }
        
        return t;
    }
    
    public static void encode(String fileName, SupportVTOL t) {
        BuildingBlock blk = new BuildingBlock();
        blk.createNewBlock();
        blk.writeBlockData("UnitType", "SupportVTOL");
        blk.writeBlockData("blockversion", 1);
        blk.writeBlockData("Name", t.getChassis());
        blk.writeBlockData("Model", t.getModel());
        blk.writeBlockData("year", t.getYear());
        String type;
        if (t.isMixedTech()) {
            if (!t.isClan())
                type = "Mixed (IS Chassis)";
            else
                type = "Mixed (Clan Chassis)";
        } else {
            switch (t.getTechLevel()) {
                case TechConstants.T_INTRO_BOXSET:
                    type = "IS Level 1";
                    break;
                case TechConstants.T_IS_TW_NON_BOX:
                    type = "IS Level 2";
                    break;
                case TechConstants.T_IS_ADVANCED:
                default:
                    type = "IS Level 3";
                    break;
                case TechConstants.T_CLAN_TW:
                    type = "Clan Level 2";
                    break;
                case TechConstants.T_CLAN_ADVANCED:
                    type = "Clan Level 3";
                    break;
            }
        }
        blk.writeBlockData("type", type);
        blk.writeBlockData("tonnage", t.getWeight());
        blk.writeBlockData("motion_type", t.getMovementModeAsString());
        if (t.getTroopCarryingSpace() > 0)
            blk.writeBlockData("transporters", "TroopSpace: "
                    + t.getTroopCarryingSpace());
        int engineCode = BLKFile.FUSION;
        switch (t.getEngine().getEngineType()) {
            case Engine.COMBUSTION_ENGINE:
                engineCode = BLKFile.ICE;
                break;
            case Engine.LIGHT_ENGINE:
                engineCode = BLKFile.LIGHT;
                break;
            case Engine.XL_ENGINE:
                engineCode = BLKFile.XL;
                break;
            case Engine.XXL_ENGINE:
                engineCode = BLKFile.XXL;
                break;
        }
        blk.writeBlockData("engine_type", engineCode);
        blk.writeBlockData("cruiseMP", t.getOriginalWalkMP());
        if (t.getArmorType() != 0) {
            blk.writeBlockData("armor_type", t.getArmorType());
            blk.writeBlockData("armor_tech", t.getArmorTechLevel());
        }
        if (t.getStructureType() != 0)
            blk.writeBlockData("internal_type", t.getStructureType());
        if (t.isOmni())
            blk.writeBlockData("omni", 1);
        int armor_array[];
        armor_array = new int[t.locations() - 1];
        for (int i = 1; i < t.locations(); i++) {
            armor_array[i - 1] = t.getOArmor(i);
        }
        blk.writeBlockData("armor", armor_array);

        Vector<Vector<String>> eq = new Vector<Vector<String>>(t.locations());
        for (int i = 0; i < t.locations(); i++) {
            eq.add(new Vector<String>());
        }
        for (Mounted m : t.getEquipment()) {
            String name = m.getType().getInternalName();
            int loc = m.getLocation();
            if (loc != Entity.LOC_NONE)
                eq.get(loc).add(name);
        }
        for (int i = 0; i < t.locations(); i++) {
            blk.writeBlockData(t.getLocationName(i) + " Equipment", eq.get(i));
        }
        blk.writeBlockFile(fileName);
    }

}

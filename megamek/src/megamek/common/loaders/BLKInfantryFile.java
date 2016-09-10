/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
 * BLkFile.java
 * 
 * Created on April 6, 2002, 2:06 AM
 */

/**
 * This class loads Infantry BLK files.
 * 
 * @author Suvarov454@sourceforge.net (James A. Damour )
 * @version $revision:$
 */
package megamek.common.loaders;

import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EquipmentType;
import megamek.common.Infantry;
import megamek.common.LocationFullException;
import megamek.common.util.BuildingBlock;
import megamek.common.weapons.infantry.InfantryWeapon;

public class BLKInfantryFile extends BLKFile implements IMechLoader {

    public BLKInfantryFile(BuildingBlock bb) {
        dataFile = bb;
    }

    public Entity getEntity() throws EntityLoadingException {

        Infantry t = new Infantry();

        if (!dataFile.exists("name")) {
            throw new EntityLoadingException("Could not find name block.");
        }
        t.setChassis(dataFile.getDataAsString("Name")[0]);

        if (!dataFile.exists("model")) {
            throw new EntityLoadingException("Could not find model block.");
        }
        t.setModel(dataFile.getDataAsString("Model")[0]);

        setTechLevel(t);
        setFluff(t);
        checkManualBV(t);

        if (dataFile.exists("source")) {
            t.setSource(dataFile.getDataAsString("source")[0]);
        }

        if (!dataFile.exists("squad_size")) {
            throw new EntityLoadingException("Could not find squad size.");
        }
        t.setSquadSize(dataFile.getDataAsInt("squad_size")[0]);
        if (!dataFile.exists("squadn")) {
            throw new EntityLoadingException("Could not find number of squads.");
        }
        t.setSquadN(dataFile.getDataAsInt("squadn")[0]);

        t.autoSetInternal();

        if (dataFile.exists("InfantryArmor")) {
            t.setDamageDivisor(dataFile.getDataAsInt("InfantryArmor")[0]);
        }

        if (!dataFile.exists("motion_type")) {
            throw new EntityLoadingException("Could not find movement block.");
        }
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        EntityMovementMode nMotion = EntityMovementMode.getMode(sMotion);
        if (nMotion == EntityMovementMode.NONE) {
            throw new EntityLoadingException("Invalid movement type: " + sMotion);
        }
        t.setMovementMode(nMotion);

        // get primary and secondary weapons
        if (dataFile.exists("secondn")) {
            t.setSecondaryN(dataFile.getDataAsInt("secondn")[0]);
        }

        if (!dataFile.exists("Primary")) {
            throw new EntityLoadingException("Could not find primary weapon.");
        }
        String primaryName = dataFile.getDataAsString("Primary")[0];
        EquipmentType ptype = EquipmentType.get(primaryName);
        if ((null == ptype) || !(ptype instanceof InfantryWeapon)) {
            throw new EntityLoadingException("primary weapon is not an infantry weapon");
        }
        t.setPrimaryWeapon((InfantryWeapon) ptype);

        EquipmentType stype = null;
        if (dataFile.exists("Secondary")) {
            String secondName = dataFile.getDataAsString("Secondary")[0];
            stype = EquipmentType.get(secondName);
            if ((null == stype) || !(stype instanceof InfantryWeapon)) {
                throw new EntityLoadingException("secondary weapon " + secondName + " is not an infantry weapon");
            }
            t.setSecondaryWeapon((InfantryWeapon) stype);
        }

        // if there is more than one secondary weapon per squad, then add that
        // to the unit
        // otherwise add the primary weapon
        if ((t.getSecondaryN() > 1) && (null != stype)) {
            try {
                t.addEquipment(stype, Infantry.LOC_INFANTRY);
            } catch (LocationFullException ex) {
                throw new EntityLoadingException(ex.getMessage());
            }
        } else {
            try {
                t.addEquipment(ptype, Infantry.LOC_INFANTRY);
            } catch (LocationFullException ex) {
                throw new EntityLoadingException(ex.getMessage());
            }
        }

        if (dataFile.exists("dest")) {
            t.setDEST(true);
        }
        if (dataFile.exists("specialization")) {
            t.setSpecializations(Integer.valueOf(dataFile
                    .getDataAsString("specialization")[0]));
        }
        if (dataFile.exists("encumberingarmor")) {
            t.setArmorEncumbering(true);
        }
        if (dataFile.exists("spacesuit")) {
            t.setSpaceSuit(true);
        }
        if (dataFile.exists("sneakcamo")) {
            t.setSneakCamo(true);
        }
        if (dataFile.exists("sneakir")) {
            t.setSneakIR(true);
        }
        if (dataFile.exists("sneakecm")) {
            t.setSneakECM(true);
        }
        if (dataFile.exists("armordivisor")) {
            t.setDamageDivisor(Double.valueOf(dataFile.getDataAsString("armordivisor")[0]));
        }
        // get field guns
        loadEquipment(t, "Field Guns", Infantry.LOC_FIELD_GUNS);

        if (dataFile.exists("antimek")) {
            int startIndex = dataFile.findStartIndex("antimek");
            int endIndex = dataFile.findEndIndex("antimek");
            int[] amSkill;
            // If startIndex is the same as end, then tag is blank, use defaults
            if (startIndex == endIndex) {
                amSkill = new int[0];
            } else {
                String[] amSkillString = dataFile.getDataAsString("antimek");
                if (amSkillString[0].equalsIgnoreCase("false")) {
                    amSkill = new int[1];
                    amSkill[0] = Infantry.ANTI_MECH_SKILL_UNTRAINED;
                } else if (amSkillString[0].equalsIgnoreCase("true")) {
                    amSkill = null;
                } else {
                    amSkill = dataFile.getDataAsInt("antimek");
                }
            }
            // If we just have the tag without values, take defaults
            if ((amSkill == null) || (amSkill.length < 1)) {
                // TM lists AM skill defaults on pg 40
                if ((t.getMovementMode() == EntityMovementMode.INF_MOTORIZED) 
                        || (t.getMovementMode() == EntityMovementMode.INF_JUMP)) {
                    t.setAntiMekSkill(Infantry.ANTI_MECH_SKILL_JUMP);
                } else {
                    t.setAntiMekSkill(Infantry.ANTI_MECH_SKILL_FOOT);
                }
            } else {
                t.setAntiMekSkill(amSkill[0]);
            }
        } else {
            t.setAntiMekSkill(Infantry.ANTI_MECH_SKILL_UNTRAINED);
        }

        return t;
    }
}

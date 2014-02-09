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
 * This class loads BattleArmor BLK files.
 *
 * @author Suvarov454@sourceforge.net (James A. Damour )
 * @version $revision:$
 */
package megamek.common.loaders;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EquipmentType;
import megamek.common.LocationFullException;
import megamek.common.Mounted;
import megamek.common.TechConstants;
import megamek.common.util.BuildingBlock;

public class BLKBattleArmorFile extends BLKFile implements IMechLoader {

    public BLKBattleArmorFile(BuildingBlock bb) {
        dataFile = bb;
    }

    public Entity getEntity() throws EntityLoadingException {

        BattleArmor t = new BattleArmor();

        if (!dataFile.exists("name")) {
            throw new EntityLoadingException("Could not find name block.");
        }
        t.setChassis(dataFile.getDataAsString("Name")[0]);

        // Model is not strictly necessary.
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

        if (dataFile.exists("exoskeleton") && dataFile.getDataAsString("exoskeleton")[0].equalsIgnoreCase("true")) {
            t.setIsExoskeleton(true);
        }

        if (!dataFile.exists("trooper count")) {
            throw new EntityLoadingException("Could not find trooper count block.");
        }
        t.setTroopers(dataFile.getDataAsInt("trooper count")[0]);

        if (!dataFile.exists("weightclass")) {
            throw new EntityLoadingException("Could not find weightclass block.");
        }
        t.setWeightClass(dataFile.getDataAsInt("weightclass")[0]);
        t.setWeight(t.getTroopers());

        if (!dataFile.exists("chassis")) {
            throw new EntityLoadingException("Could not find chassis block.");
        }
        String chassis = dataFile.getDataAsString("chassis")[0];
        if (chassis.toLowerCase().equals("biped")) {
            t.setChassisType(BattleArmor.CHASSIS_TYPE_BIPED);
        } else if (chassis.toLowerCase().equals("quad")) {
            t.setChassisType(BattleArmor.CHASSIS_TYPE_QUAD);
        } else {
            throw new EntityLoadingException("Unsupported chassis type: " + chassis);
        }

        if (!dataFile.exists("motion_type")) {
            throw new EntityLoadingException("Could not find movement block.");
        }
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        EntityMovementMode nMotion = EntityMovementMode.NONE;
        if (sMotion.equalsIgnoreCase("leg")) {
            nMotion = EntityMovementMode.INF_LEG;
        } else if (sMotion.equalsIgnoreCase("jump")) {
            nMotion = EntityMovementMode.INF_JUMP;
        } else if (sMotion.equalsIgnoreCase("vtol")) {
            nMotion = EntityMovementMode.VTOL;
        } else if (sMotion.equalsIgnoreCase("submarine")) {
            nMotion = EntityMovementMode.INF_UMU;
        }
        if (nMotion == EntityMovementMode.NONE) {
            throw new EntityLoadingException("Invalid movement type: " + sMotion);
        }
        t.setMovementMode(nMotion);

        if (!dataFile.exists("cruiseMP")) {
            throw new EntityLoadingException("Could not find cruiseMP block.");
        }
        t.setOriginalWalkMP(dataFile.getDataAsInt("cruiseMP")[0]);

        if (dataFile.exists("jumpingMP")) {
            t.setOriginalJumpMP(dataFile.getDataAsInt("jumpingMP")[0]);
        }

        if (!dataFile.exists("armor")) {
            throw new EntityLoadingException("Could not find armor block.");
        }

        int[] armor = dataFile.getDataAsInt("armor");

        // Each trooper has the same amount of armor
        if (armor.length != 1) {
            throw new EntityLoadingException("Incorrect armor array length");
        }

        // add the body to the armor array
        t.refreshLocations();
        for (int x = 1; x < t.locations(); x++) {
            t.initializeArmor(armor[0], x);
        }

        t.autoSetInternal();
        
        if (dataFile.exists("armor_type")){
            t.setArmorType(dataFile.getDataAsInt("armor_type")[0]);
        }
        
        if (dataFile.exists("armor_tech")) {
            t.setArmorTechLevel(dataFile.getDataAsInt("armor_tech")[0]);
        }

        String[] abbrs = t.getLocationAbbrs();
        for (int loop = 0; loop < t.locations(); loop++) {
            loadEquipment(t, abbrs[loop], loop);
        }

        if (dataFile.exists("cost")) {
            t.setCost(dataFile.getDataAsInt("cost")[0]);
        }
        t.setArmorTonnage(t.getArmorWeight());

        return t;
    }

    @Override
    protected void loadEquipment(Entity t, String sName, int nLoc) throws EntityLoadingException {
        String[] saEquip = dataFile.getDataAsString(sName + " Equipment");
        if (saEquip == null) {
            return;
        }

        // prefix is "Clan " or "IS "
        String prefix;
        if (t.getTechLevel() == TechConstants.T_CLAN_TW) {
            prefix = "Clan ";
        } else {
            prefix = "IS ";
        }

        if (saEquip[0] != null) {
            for (int x = 0; x < saEquip.length; x++) {
                int mountLoc = BattleArmor.MOUNT_LOC_NONE;
                if  (saEquip[x].contains(":Body")){
                    mountLoc = BattleArmor.MOUNT_LOC_BODY;
                    saEquip[x] = saEquip[x].replace(":Body", "");
                } else if  (saEquip[x].contains(":LA")){
                    mountLoc = BattleArmor.MOUNT_LOC_LARM;
                    saEquip[x] = saEquip[x].replace(":LA", "");
                } else if  (saEquip[x].contains(":RA")){
                    mountLoc = BattleArmor.MOUNT_LOC_RARM;
                    saEquip[x] = saEquip[x].replace(":RA", "");
                }
                
                boolean dwpMounted = saEquip[x].contains(":DWP");
                saEquip[x] = saEquip[x].replace(":DWP", "");
                
                boolean apmMounted = saEquip[x].contains(":APM");
                saEquip[x] = saEquip[x].replace(":APM", "");
                
                int numShots = 0;
                if (saEquip[x].contains(":Shots")){
                    String shotString = saEquip[x].substring(
                            saEquip[x].indexOf(":Shots"),
                            saEquip[x].indexOf("#")+1);
                    numShots = Integer.parseInt(
                            shotString.replace(":Shots", "").replace("#", ""));
                    saEquip[x] = saEquip[x].replace(shotString, "");
                }
                
                String equipName = saEquip[x].trim();
                EquipmentType etype = EquipmentType.get(equipName);

                
                if (etype == null) {
                    // try w/ prefix
                    etype = EquipmentType.get(prefix + equipName);
                }

                if (etype != null) {
                    try {
                        Mounted m = t.addEquipment(etype, nLoc, false, 
                                mountLoc, dwpMounted);
                        if (numShots != 0 && m != null 
                                && (m.getType() instanceof AmmoType)){
                            m.setShotsLeft(numShots);
                        }
                        m.setAPMMounted(apmMounted);
                    } catch (LocationFullException ex) {
                        throw new EntityLoadingException(ex.getMessage());
                    }
                } else if (!equipName.equals("")) {
                    t.addFailedEquipment(equipName);
                }
            }
        }
    }
}

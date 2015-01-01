/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
 * BLkFile.java
 *
 * Created on April 6, 2002, 2:06 AM
 */

/**
 *
 * @author  taharqa
 * @version 
 */
package megamek.common.loaders;

import megamek.common.ASFBay;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmorBay;
import megamek.common.CargoBay;
import megamek.common.Dropship;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.HeavyVehicleBay;
import megamek.common.InfantryBay;
import megamek.common.LightVehicleBay;
import megamek.common.LocationFullException;
import megamek.common.MechBay;
import megamek.common.Mounted;
import megamek.common.SmallCraftBay;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.util.BuildingBlock;

public class BLKDropshipFile extends BLKFile implements IMechLoader {

    // armor locatioms
    public static final int NOSE = 0;
    public static final int RW = 1;
    public static final int LW = 2;
    public static final int AFT = 3;

    public BLKDropshipFile(BuildingBlock bb) {
        dataFile = bb;
    }

    public Entity getEntity() throws EntityLoadingException {

        Dropship a = new Dropship();

        if (!dataFile.exists("Name")) {
            throw new EntityLoadingException("Could not find name block.");
        }
        a.setChassis(dataFile.getDataAsString("Name")[0]);
        if (dataFile.exists("Model") && dataFile.getDataAsString("Model")[0] != null) {
            a.setModel(dataFile.getDataAsString("Model")[0]);
        } else {
            a.setModel("");
        }
        setTechLevel(a);

        if (dataFile.exists("source")) {
            a.setSource(dataFile.getDataAsString("source")[0]);
        }

        if (!dataFile.exists("crew")) {
            throw new EntityLoadingException("Could not find crew block.");
        }
        a.setNCrew(dataFile.getDataAsInt("crew")[0]);

        if (!dataFile.exists("passengers")) {
            throw new EntityLoadingException("Could not find passenger block.");
        }
        a.setNPassenger(dataFile.getDataAsInt("passengers")[0]);

        if (!dataFile.exists("life_boat")) {
            throw new EntityLoadingException("Could not find life boat block.");
        }
        a.setLifeBoats(dataFile.getDataAsInt("life_boat")[0]);

        if (!dataFile.exists("escape_pod")) {
            throw new EntityLoadingException("Could not find escape pod block.");
        }
        a.setEscapePods(dataFile.getDataAsInt("escape_pod")[0]);

        if (!dataFile.exists("tonnage")) {
            throw new EntityLoadingException("Could not find weight block.");
        }
        a.setWeight(dataFile.getDataAsFloat("tonnage")[0]);

        // get a movement mode - lets try Aerodyne
        if (!dataFile.exists("motion_type")) {
            throw new EntityLoadingException("Could not find movement block.");
        }
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        int nMotion = 16;
        if (sMotion.equals("spheroid")) {
            nMotion = 17;
            a.setSpheroid(true);
        }
        a.setMovementMode(nMotion);

        // figure out structural integrity
        if (!dataFile.exists("structural_integrity")) {
            throw new EntityLoadingException("Could not find structual integrity block.");
        }
        a.set0SI(dataFile.getDataAsInt("structural_integrity")[0]);

        // figure out heat
        if (!dataFile.exists("heatsinks")) {
            throw new EntityLoadingException("Could not find weight block.");
        }
        a.setHeatSinks(dataFile.getDataAsInt("heatsinks")[0]);
        if (!dataFile.exists("sink_type")) {
            throw new EntityLoadingException("Could not find weight block.");
        }
        a.setHeatType(dataFile.getDataAsInt("sink_type")[0]);

        // figure out fuel
        if (!dataFile.exists("fuel")) {
            throw new EntityLoadingException("Could not find fuel block.");
        }
        a.setFuel(dataFile.getDataAsInt("fuel")[0]);

        // figure out engine stuff
        // not done for small craft and up
        if (!dataFile.exists("SafeThrust")) {
            throw new EntityLoadingException("Could not find Safe Thrust block.");
        }
        a.setOriginalWalkMP(dataFile.getDataAsInt("SafeThrust")[0]);

        a.setEngine(new Engine(400, 0, 0));

        if (dataFile.exists("armor_type")) {
            a.setArmorType(dataFile.getDataAsInt("armor_type")[0]);
        }
        if (dataFile.exists("armor_tech")) {
            a.setArmorTechLevel(dataFile.getDataAsInt("armor_tech")[0]);
        }
        if (dataFile.exists("internal_type")) {
            a.setStructureType(dataFile.getDataAsInt("internal_type")[0]);
        }

        if (!dataFile.exists("armor")) {
            throw new EntityLoadingException("Could not find armor block.");
        }

        int[] armor = dataFile.getDataAsInt("armor");

        if (armor.length != 4) {
            throw new EntityLoadingException("Incorrect armor array length");
        }

        a.initializeArmor(armor[BLKAeroFile.NOSE], Aero.LOC_NOSE);
        a.initializeArmor(armor[BLKAeroFile.RW], Aero.LOC_RWING);
        a.initializeArmor(armor[BLKAeroFile.LW], Aero.LOC_LWING);
        a.initializeArmor(armor[BLKAeroFile.AFT], Aero.LOC_AFT);

        a.autoSetInternal();
        // This is not working right for arrays for some reason
        a.autoSetThresh();

        loadEquipment(a, "Nose", Aero.LOC_NOSE);
        loadEquipment(a, "Right Side", Aero.LOC_RWING);
        loadEquipment(a, "Left Side", Aero.LOC_LWING);
        loadEquipment(a, "Aft", Aero.LOC_AFT);

        if (dataFile.exists("omni")) {
            a.setOmni(true);
        }

        // get the bays on this dropship
        // should be of format name:units:doors
        if (dataFile.exists("transporters")) {
            String[] transporters = dataFile.getDataAsString("transporters");
            // Walk the array of transporters.
            for (String transporter : transporters) {
                if (transporter.startsWith("ASFBay:", 0)) {
                    String numbers = transporter.substring(7);
                    String temp[] = numbers.split(":");
                    int size = Integer.parseInt(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    a.addTransporter(new ASFBay(size, doors));
                } else if (transporter.startsWith("SmallCraftBay:", 0)) {
                    String numbers = transporter.substring(14);
                    String temp[] = numbers.split(":");
                    int size = Integer.parseInt(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    a.addTransporter(new SmallCraftBay(size, doors));
                } else if (transporter.startsWith("MechBay:", 0)) {
                    String numbers = transporter.substring(8);
                    String temp[] = numbers.split(":");
                    int size = Integer.parseInt(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    a.addTransporter(new MechBay(size, doors));
                } else if (transporter.startsWith("LightVehicleBay:", 0)) {
                    String numbers = transporter.substring(16);
                    String temp[] = numbers.split(":");
                    int size = Integer.parseInt(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    a.addTransporter(new LightVehicleBay(size, doors));
                } else if (transporter.startsWith("HeavyVehicleBay:", 0)) {
                    String numbers = transporter.substring(16);
                    String temp[] = numbers.split(":");
                    int size = Integer.parseInt(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    a.addTransporter(new HeavyVehicleBay(size, doors));
                } else if (transporter.startsWith("InfantryBay:", 0)) {
                    String numbers = transporter.substring(12);
                    String temp[] = numbers.split(":");
                    int size = Integer.parseInt(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    a.addTransporter(new InfantryBay(size, doors));
                } else if (transporter.startsWith("BattleArmorBay:", 0)) {
                    String numbers = transporter.substring(15);
                    String temp[] = numbers.split(":");
                    int size = Integer.parseInt(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    a.addTransporter(new BattleArmorBay(size, doors));
                } else if (transporter.startsWith("CargoBay:", 0)) {
                    String numbers = transporter.substring(9);
                    String temp[] = numbers.split(":");
                    int size = Integer.parseInt(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    a.addTransporter(new CargoBay(size, doors));
                }
            }
        }
        return a;
    }

    protected void loadEquipment(Dropship a, String sName, int nLoc) throws EntityLoadingException {
        String[] saEquip = dataFile.getDataAsString(sName + " Equipment");
        if (saEquip == null) {
            return;
        }

        // prefix is "Clan " or "IS "
        String prefix;
        if (a.getTechLevel() == TechConstants.T_CLAN_TW) {
            prefix = "Clan ";
        } else {
            prefix = "IS ";
        }

        boolean rearMount = false;
        int nAmmo = 1;
        // set up a new weapons bay mount
        Mounted bayMount = null;
        // set up a new bay type
        boolean newBay = false;
        boolean subCap = false;
        int bayDamage = 0;
        int nBay = 1;
        if (saEquip[0] != null) {
            for (String element : saEquip) {
                rearMount = false;
                nAmmo = 1;
                newBay = false;
                subCap = false;
                String equipName = element.trim();

                // I will need to deal with rear-mounted bays on Dropships
                if (equipName.startsWith("(R) ")) {
                    rearMount = true;
                    equipName = equipName.substring(4);
                }

                if (equipName.startsWith("(B) ")) {
                    newBay = true;
                    equipName = equipName.substring(4);
                }

                if (equipName.startsWith("(SC) ")) {
                    newBay = true;
                    subCap = true;
                    equipName = equipName.substring(4);
                }

                // check for ammo loadouts
                if (equipName.contains(":") && equipName.contains("Ammo")) {
                    // then split by the :
                    String[] temp;
                    temp = equipName.split(":");
                    equipName = temp[0];
                    if (temp[1] != null) {
                        nAmmo = Integer.parseInt(temp[1]);
                    }
                }

                EquipmentType etype = EquipmentType.get(equipName);

                if (etype == null) {
                    // try w/ prefix
                    etype = EquipmentType.get(prefix + equipName);
                }

                if (etype != null) {
                    // first load the equipment
                    Mounted newmount;
                    try {
                        if (nAmmo == 1) {
                            newmount = a.addEquipment(etype, nLoc, rearMount);
                        } else {
                            newmount = a.addEquipment(etype, nLoc, rearMount, nAmmo);
                        }
                    } catch (LocationFullException ex) {
                        throw new EntityLoadingException(ex.getMessage());
                    }

                    // this is where weapon bays go
                    // first, lets see if it is a weapon
                    if (newmount.getType() instanceof WeaponType) {
                        // if so then I need to find out if it is the same class
                        // as the current weapon bay
                        // If the current bay is null, then it needs to be
                        // initialized
                        WeaponType weap = (WeaponType) newmount.getType();
                        if (bayMount == null) {
                            if (subCap) {
                                try {
                                    bayMount = a.addEquipment(WeaponType.getSubCapBayType(weap.getAtClass()), nLoc, rearMount);
                                    newBay = false;
                                } catch (LocationFullException ex) {
                                    throw new EntityLoadingException(ex.getMessage());
                                }
                            } else {
                                try {
                                    bayMount = a.addEquipment(WeaponType.getBayType(weap.getAtClass()), nLoc, rearMount);
                                    newBay = false;
                                } catch (LocationFullException ex) {
                                    throw new EntityLoadingException(ex.getMessage());
                                }
                            }
                        }

                        int damage = weap.getRoundShortAV();
                        if (weap.isCapital()) {
                            damage *= 10;
                        }
                        if (!newBay && (bayDamage + damage) <= 700 && bayMount.isRearMounted() == rearMount && weap.getAtClass() == ((WeaponType) bayMount.getType()).getAtClass() && !(((WeaponType) bayMount.getType()).isSubCapital() && !weap.isSubCapital())) {
                            // then we should add this weapon to the current bay
                            bayMount.addWeaponToBay(a.getEquipmentNum(newmount));
                            bayDamage += damage;
                        } else {
                            if (subCap) {
                                try {
                                    bayMount = a.addEquipment(WeaponType.getSubCapBayType(weap.getAtClass()), nLoc, rearMount);
                                } catch (LocationFullException ex) {
                                    throw new EntityLoadingException(ex.getMessage());
                                }
                            } else {
                                try {
                                    bayMount = a.addEquipment(WeaponType.getBayType(weap.getAtClass()), nLoc, rearMount);
                                } catch (LocationFullException ex) {
                                    throw new EntityLoadingException(ex.getMessage());
                                }
                            }
                            bayMount.addWeaponToBay(a.getEquipmentNum(newmount));
                            // reset bay damage
                            bayDamage = damage;
                            nBay++;
                        }
                    }
                    // ammo should also get loaded into the bay
                    if (newmount.getType() instanceof AmmoType) {
                        bayMount.addAmmoToBay(a.getEquipmentNum(newmount));
                    }
                } else if (equipName != "0") {
                    a.addFailedEquipment(equipName);
                }
            }
        }
    }

}

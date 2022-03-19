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
package megamek.common.loaders;

import megamek.common.*;
import megamek.common.util.BuildingBlock;

/**
 * @author taharqa
 * @since April 6, 2002, 2:06 AM
 */
public class BLKDropshipFile extends BLKFile implements IMechLoader {

    public BLKDropshipFile(BuildingBlock bb) {
        dataFile = bb;
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {

        Dropship a = new Dropship();

        if (!dataFile.exists("Name")) {
            throw new EntityLoadingException("Could not find name block.");
        }
        a.setChassis(dataFile.getDataAsString("Name")[0]);
        if (dataFile.exists("Model")
                && (dataFile.getDataAsString("Model")[0] != null)) {
            a.setModel(dataFile.getDataAsString("Model")[0]);
        } else {
            a.setModel("");
        }
        if (dataFile.exists(MtfFile.MUL_ID)) {
            a.setMulId(dataFile.getDataAsInt(MtfFile.MUL_ID)[0]);
        }
        setTechLevel(a);
        setFluff(a);
        checkManualBV(a);

        if (dataFile.exists("source")) {
            a.setSource(dataFile.getDataAsString("source")[0]);
        }

        if (dataFile.exists("originalBuildYear")) {
            a.setOriginalBuildYear(dataFile.getDataAsInt("originalBuildYear")[0]);
        }

        if (!dataFile.exists("crew")) {
            throw new EntityLoadingException("Could not find crew block.");
        }
        a.setNCrew(dataFile.getDataAsInt("crew")[0]);

        if (dataFile.exists("officers")) {
            a.setNOfficers(dataFile.getDataAsInt("officers")[0]);
        }

        if (dataFile.exists("gunners")) {
            a.setNGunners(dataFile.getDataAsInt("gunners")[0]);
        }

        if (dataFile.exists("passengers")) {
            a.setNPassenger(dataFile.getDataAsInt("passengers")[0]);
        }

        if (dataFile.exists("battlearmor")) {
            a.setNBattleArmor(dataFile.getDataAsInt("battlearmor")[0]);
        }

        if (dataFile.exists("marines")) {
            a.setNMarines(dataFile.getDataAsInt("marines")[0]);
        }

        if (dataFile.exists("otherpassenger")) {
            a.setNOtherPassenger(dataFile.getDataAsInt("otherpassenger")[0]);
        }

        if (!dataFile.exists("life_boat")) {
            throw new EntityLoadingException("Could not find life boat block.");
        }
        a.setLifeBoats(dataFile.getDataAsInt("life_boat")[0]);

        if (!dataFile.exists("escape_pod")) {
            throw new EntityLoadingException("Could not find escape pod block.");
        }
        a.setEscapePods(dataFile.getDataAsInt("escape_pod")[0]);

        if (!dataFile.exists("tonnage")) {
            throw new EntityLoadingException("Could not find tonnage block.");
        }
        a.setWeight(dataFile.getDataAsDouble("tonnage")[0]);

        // get a movement mode - lets try Aerodyne
        if (!dataFile.exists("motion_type")) {
            throw new EntityLoadingException("Could not find movement block.");
        }
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        EntityMovementMode nMotion = EntityMovementMode.AERODYNE;
        if (sMotion.equalsIgnoreCase("spheroid")) {
            nMotion = EntityMovementMode.SPHEROID;
            a.setSpheroid(true);
        }
        a.setMovementMode(nMotion);

        // All dropships are VSTOL and can hover
        a.setVSTOL(true);


        // figure out structural integrity
        if (!dataFile.exists("structural_integrity")) {
            throw new EntityLoadingException(
                    "Could not find structural integrity block.");
        }
        a.set0SI(dataFile.getDataAsInt("structural_integrity")[0]);

        if (dataFile.exists("collartype")) {
            a.setCollarType(dataFile.getDataAsInt("collartype")[0]);
        }
        // figure out heat
        if (!dataFile.exists("heatsinks")) {
            throw new EntityLoadingException("Could not find heatsink block.");
        }
        a.setHeatSinks(dataFile.getDataAsInt("heatsinks")[0]);
        a.setOHeatSinks(dataFile.getDataAsInt("heatsinks")[0]);
        if (!dataFile.exists("sink_type")) {
            throw new EntityLoadingException("Could not find sink_type block.");
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
            throw new EntityLoadingException(
                    "Could not find Safe Thrust block.");
        }
        a.setOriginalWalkMP(dataFile.getDataAsInt("SafeThrust")[0]);

        a.setEngine(new Engine(400, 0, 0));

        // Switch older files with standard armor to aerospace
        int at = EquipmentType.T_ARMOR_AEROSPACE;
        if (dataFile.exists("armor_type")) {
            at = dataFile.getDataAsInt("armor_type")[0];
            if (at == EquipmentType.T_ARMOR_STANDARD) {
                at = EquipmentType.T_ARMOR_AEROSPACE;
            }
        }
        a.setArmorType(at);
        if (dataFile.exists("armor_tech")) {
            a.setArmorTechLevel(dataFile.getDataAsInt("armor_tech")[0]);
        }
        if (dataFile.exists("internal_type")) {
            a.setStructureType(dataFile.getDataAsInt("internal_type")[0]);
        } else {
            a.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);
        }
        if (dataFile.exists("designtype")) {
            a.setDesignType(dataFile.getDataAsInt("designtype")[0]);
        } else {
            a.setDesignType(Aero.MILITARY);
        }

        if (!dataFile.exists("armor")) {
            throw new EntityLoadingException("Could not find armor block.");
        }

        int[] armor = dataFile.getDataAsInt("armor");

        if (armor.length != 4) {
            throw new EntityLoadingException("Incorrect armor array length");
        }

        a.initializeArmor(armor[BLKAeroFile.NOSE], Dropship.LOC_NOSE);
        a.initializeArmor(armor[BLKAeroFile.RW], Dropship.LOC_RWING);
        a.initializeArmor(armor[BLKAeroFile.LW], Dropship.LOC_LWING);
        a.initializeArmor(armor[BLKAeroFile.AFT], Dropship.LOC_AFT);
        a.initializeArmor(IArmorState.ARMOR_NA, Dropship.LOC_HULL);

        a.autoSetInternal();
        a.recalculateTechAdvancement();
        // This is not working right for arrays for some reason
        a.autoSetThresh();

        for (int loc = 0; loc < a.locations(); loc++) {
            loadEquipment(a, a.getLocationName(loc), loc);
        }

        if (dataFile.exists("omni")) {
            a.setOmni(true);
        }

        addTransports(a);
        a.setArmorTonnage(a.getArmorWeight());
        return a;
    }

    protected void loadEquipment(Dropship a, String sName, int nLoc)
            throws EntityLoadingException {
        String[] saEquip1 = dataFile.getDataAsString(sName + " Equipment");
        String[] saEquip2 = new String[2];
        String[] saEquip;

        // Special case handling for the LongTomIIICannon in the Fortress BLK
        // A bit of a hack, but it works...
        if ((nLoc == Aero.LOC_NOSE) && dataFile.exists("transporters")) {
            String[] transporters = dataFile.getDataAsString("transporters");
            for (String transporter : transporters) {
                if (transporter.equals("LongTomIIICannon")) {
                    saEquip2[0] = "ISLongTom";
                    saEquip2[1] = "ISLongTomAmmo:125";
                }
            }
        }
        if (saEquip2[0] != null) {
            saEquip = new String[saEquip1.length + saEquip2.length];
            System.arraycopy(saEquip2, 0, saEquip, 0, saEquip2.length);
            System.arraycopy(saEquip1, 0, saEquip, 2, saEquip1.length);
        } else {
            saEquip = new String[saEquip1.length];
            System.arraycopy(saEquip1, 0, saEquip, 0, saEquip1.length);
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
        double bayDamage = 0;
        if (saEquip[0] != null) {
            for (String element : saEquip) {
                rearMount = false;
                nAmmo = 1;
                newBay = false;
                String equipName = element.trim();

                double size = 0.0;
                int sizeIndex = equipName.toUpperCase().indexOf(":SIZE:");
                if (sizeIndex > 0) {
                    size = Double.parseDouble(equipName.substring(sizeIndex + 6));
                    equipName = equipName.substring(0, sizeIndex);
                }
                // I will need to deal with rear-mounted bays on Dropships
                if (equipName.startsWith("(R) ")) {
                    rearMount = true;
                    equipName = equipName.substring(4);
                }

                if (equipName.startsWith("(B) ")) {
                    newBay = true;
                    equipName = equipName.substring(4);
                }

                // check for ammo loadouts
                if (equipName.contains(":") && (equipName.contains("Ammo")
                        || equipName.contains("Pod"))) {
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
                if ((etype == null) && checkLegacyExtraEquipment(equipName)) {
                    continue;
                }

                if (etype != null) {
                    // first load the equipment
                    Mounted newmount;
                    try {
                        if (nAmmo == 1) {
                            newmount = a.addEquipment(etype, nLoc, rearMount);
                        } else {
                            newmount = a.addEquipment(etype, nLoc, rearMount,
                                    nAmmo);
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
                            try {
                                bayMount = a.addEquipment(weap.getBayType(),
                                        nLoc, rearMount);
                                newBay = false;
                            } catch (LocationFullException ex) {
                                throw new EntityLoadingException(
                                        ex.getMessage());
                            }
                        }

                        double damage = weap.getShortAV();
                        if (weap.isCapital()) {
                            damage *= 10;
                        }
                        if (!newBay
                                && ((bayDamage + damage) <= 700)
                                && (bayMount.isRearMounted() == rearMount)
                                && (weap.getAtClass() == ((WeaponType) bayMount
                                        .getType()).getAtClass())
                                && !(((WeaponType) bayMount.getType())
                                        .isSubCapital() && !weap.isSubCapital())) {
                            // then we should add this weapon to the current bay
                            bayMount.addWeaponToBay(a.getEquipmentNum(newmount));
                            bayDamage += damage;
                        } else {
                            try {
                                bayMount = a.addEquipment(weap.getBayType(),
                                        nLoc, rearMount);
                            } catch (LocationFullException ex) {
                                throw new EntityLoadingException(
                                        ex.getMessage());
                            }
                            bayMount.addWeaponToBay(a.getEquipmentNum(newmount));
                            // reset bay damage
                            bayDamage = damage;
                        }
                    } else if (newmount.getType() instanceof AmmoType) {
                        // ammo should also get loaded into the bay
                        bayMount.addAmmoToBay(a.getEquipmentNum(newmount));
                    }
                    if (etype.isVariableSize()) {
                        if (size == 0.0) {
                            size = getLegacyVariableSize(equipName);
                        }
                        newmount.setSize(size);
                    }
                } else if (!equipName.isBlank()) {
                    a.addFailedEquipment(equipName);
                }
            }
        }
        if (mashOperatingTheaters > 0) {
            for (Mounted m : a.getMisc()) {
                if (m.getType().hasFlag(MiscType.F_MASH)) {
                    // includes one as part of the core component
                    m.setSize(m.getSize() + mashOperatingTheaters);
                    break;
                }
            }
        }
        if (legacyDCCSCapacity > 0) {
            for (Mounted m : a.getMisc()) {
                if (m.getType().hasFlag(MiscType.F_DRONE_CARRIER_CONTROL)) {
                    // core system does not include drone capacity
                    m.setSize(legacyDCCSCapacity);
                    break;
                }
            }
        }
    }
}

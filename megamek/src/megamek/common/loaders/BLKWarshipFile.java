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
public class BLKWarshipFile extends BLKFile implements IMechLoader {

    public BLKWarshipFile(BuildingBlock bb) {
        dataFile = bb;
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {

        Warship a = new Warship();

        if (!dataFile.exists("Name")) {
            throw new EntityLoadingException("Could not find name block.");
        }
        a.setChassis(dataFile.getDataAsString("Name")[0]);
        if (dataFile.exists("Model") && (dataFile.getDataAsString("Model")[0] != null)) {
            a.setModel(dataFile.getDataAsString("Model")[0]);
        } else {
            a.setModel("");
        }
        if (dataFile.exists(MtfFile.MUL_ID)) {
            a.setMulId(dataFile.getDataAsInt(MtfFile.MUL_ID)[0]);
        }
        if (dataFile.exists("source")) {
            a.setSource(dataFile.getDataAsString("source")[0]);
        }

        if (!dataFile.exists("year")) {
            throw new EntityLoadingException("Could not find year block.");
        }
        a.setYear(dataFile.getDataAsInt("year")[0]);

        setTechLevel(a);
        setFluff(a);
        checkManualBV(a);

        // Tonnage
        if (!dataFile.exists("tonnage")) {
            throw new EntityLoadingException("Could not find weight block.");
        }
        a.setWeight(dataFile.getDataAsDouble("tonnage")[0]);

        // Crew
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

        // Marines
        if (!dataFile.exists("marines")) {
            //throw new EntityLoadingException("Could not find marines block.");
        }
        a.setNMarines(dataFile.getDataAsInt("marines")[0]);

        // Battle Armor
        if (!dataFile.exists("battlearmor")) {
            //throw new EntityLoadingException("Could not find battlearmor block.");
        }
        a.setNBattleArmor(dataFile.getDataAsInt("battlearmor")[0]);

        // Passengers
        if (!dataFile.exists("passengers")) {
            throw new EntityLoadingException("Could not find passenger block.");
        }
        a.setNPassenger(dataFile.getDataAsInt("passengers")[0]);

        // Other Passengers
        if (!dataFile.exists("other_crew")) {
            //throw new EntityLoadingException("Could not find other_crew block.");
        }
        a.setNOtherCrew(dataFile.getDataAsInt("other_crew")[0]);

        if (!dataFile.exists("life_boat")) {
            throw new EntityLoadingException("Could not find life boat block.");
        }
        a.setLifeBoats(dataFile.getDataAsInt("life_boat")[0]);

        if (!dataFile.exists("escape_pod")) {
            throw new EntityLoadingException("Could not find escape pod block.");
        }
        a.setEscapePods(dataFile.getDataAsInt("escape_pod")[0]);

        // get a movement mode - lets try Aerodyne
        EntityMovementMode nMotion = EntityMovementMode.AERODYNE;
        a.setMovementMode(nMotion);

        // figure out structural integrity
        if (!dataFile.exists("structural_integrity")) {
            throw new EntityLoadingException("Could not find structual integrity block.");
        }
        a.set0SI(dataFile.getDataAsInt("structural_integrity")[0]);

        // figure out heat
        if (!dataFile.exists("heatsinks")) {
            throw new EntityLoadingException("Could not find heatsinks block.");
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

        if (!dataFile.exists("SafeThrust")) {
            throw new EntityLoadingException("Could not find Safe Thrust block.");
        }
        a.setOriginalWalkMP(dataFile.getDataAsInt("SafeThrust")[0]);

        a.setEngine(new Engine(400, 0, 0));

        if (dataFile.exists("kf_core")) {
            a.setDriveCoreType(dataFile.getDataAsInt("kf_core")[0]);
        }

        if (dataFile.exists("jump_range")) {
            a.setJumpRange(dataFile.getDataAsInt("jump_range")[0]);
        }

        if (dataFile.exists("lithium-fusion")) {
            a.setLF(true);
        }

        if (dataFile.exists("hpg")) {
            a.setHPG(true);
        }

        if (dataFile.exists("sail")) {
            a.setSail(dataFile.getDataAsInt("sail")[0] != 0);
        }

        if (dataFile.exists("overview")) {
            a.getFluff().setOverview(dataFile.getDataAsString("overview")[0]);
        }
        // Grav Decks - two approaches
        // First, the old method, where a number of grav decks for each category is specified
        //  This doesn't allow us to specify precise size
        if (dataFile.exists("grav_deck")) {
            a.setGravDeck(dataFile.getDataAsInt("grav_deck")[0]);
        }
        if (dataFile.exists("grav_deck_large")) {
            a.setGravDeckLarge(dataFile.getDataAsInt("grav_deck_large")[0]);
        }
        if (dataFile.exists("grav_deck_huge")) {
            a.setGravDeckHuge(dataFile.getDataAsInt("grav_deck_huge")[0]);
        }
        // Second, the new method, where a white space separated list of numbers is given
        //  Each number represents a distinct grav deck, with the specified size
        if (dataFile.exists("grav_decks")) {
            String[] toks = dataFile.getDataAsString("grav_decks");
            for (String t : toks) {
                a.addGravDeck(Integer.parseInt(t));
            }
        }
        // Add a damage tracker value for each grav deck
        for (int i = 0; i < a.getTotalGravDeck(); i++) {
            a.initializeGravDeckDamage(i);
        }

        // Switch older files with standard armor to capital
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
        
        //Warships should always be military craft
        if (dataFile.exists("designtype")) {
            a.setDesignType(dataFile.getDataAsInt("designtype")[0]);
        } else {
            a.setDesignType(Aero.MILITARY);
        }

        if (dataFile.exists("overview")) {
            a.getFluff().setOverview(dataFile.getDataAsString("overview")[0]);
        }
        
        if (dataFile.exists("capabilities")) {
            a.getFluff().setCapabilities(dataFile.getDataAsString("capabilities")[0]);
        }
        
        if (dataFile.exists("deployment")) {
            a.getFluff().setDeployment(dataFile.getDataAsString("deployment")[0]);
        }

        if (dataFile.exists("imagepath")) {
            a.getFluff().setMMLImagePath(dataFile.getDataAsString("imagepath")[0]);
        }

        if (!dataFile.exists("armor")) {
            throw new EntityLoadingException("Could not find armor block.");
        }

        int[] armor = dataFile.getDataAsInt("armor");

        if (armor.length != 6) {
            throw new EntityLoadingException("Incorrect armor array length");
        }

        for (int i = 0; i < armor.length; i++) {
            a.initializeArmor(armor[i], i);
        }
        a.initializeArmor(IArmorState.ARMOR_NA, Warship.LOC_HULL);
        a.initializeArmor(IArmorState.ARMOR_NA, Warship.LOC_LBS);
        a.initializeArmor(IArmorState.ARMOR_NA, Warship.LOC_RBS);

        a.autoSetInternal();
        a.recalculateTechAdvancement();
        a.autoSetThresh();
        a.initializeKFIntegrity();
        a.initializeSailIntegrity();

        for (int loc = 0; loc < a.locations(); loc++) {
            loadEquipment(a, a.getLocationName(loc), loc);
        }

        // legacy support for older files that have different location names than
        // what is returned by getLocationName(int)
        loadEquipment(a, "Front Right Side", Warship.LOC_FRS);
        loadEquipment(a, "Front Left Side", Warship.LOC_FLS);
        loadEquipment(a, "Right Broadside", Warship.LOC_RBS);
        loadEquipment(a, "Left Broadside", Warship.LOC_LBS);

        addTransports(a);

        // get docking collars (legacy BLK files)
        int docks = dataFile.getDataAsInt("docking_collar")[0];
        while (docks > 0) {
            a.addTransporter(new DockingCollar(a.getTransports().size() + 1));
            docks--;
        }
        a.setArmorTonnage(a.getArmorWeight());

        return a;
    }

    @Override
    protected void loadEquipment(Entity en, String sName, int nLoc) throws EntityLoadingException {
        Warship a = (Warship) en;
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
                if (equipName.startsWith("(B) ")) {
                    newBay = true;
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
                            try {
                                bayMount = a.addEquipment(weap.getBayType(), nLoc, rearMount);
                                newBay = false;
                            } catch (LocationFullException ex) {
                                throw new EntityLoadingException(ex.getMessage());
                            }
                        }
                        double damage = weap.getShortAV();
                        if (weap.isCapital()) {
                            damage *= 10;
                        }
                        if (!newBay && (((bayDamage + damage) <= 700) || weap.hasFlag(WeaponType.F_MASS_DRIVER)) && (bayMount.isRearMounted() == rearMount) && (weap.getAtClass() == ((WeaponType) bayMount.getType()).getAtClass()) && !(((WeaponType) bayMount.getType()).isSubCapital() && !weap.isSubCapital())) {
                            // then we should add this weapon to the current bay
                            bayMount.addWeaponToBay(a.getEquipmentNum(newmount));
                            bayDamage += damage;
                        } else {
                            try {
                                bayMount = a.addEquipment(weap.getBayType(), nLoc, rearMount);
                            } catch (LocationFullException ex) {
                                throw new EntityLoadingException(ex.getMessage());
                            }
                            bayMount.addWeaponToBay(a.getEquipmentNum(newmount));
                            // reset bay damage
                            bayDamage = damage;
                        }
                    }
                    // ammo should also get loaded into the bay
                    if (newmount.getType() instanceof AmmoType) {
                        if (null != bayMount) {
                            bayMount.addAmmoToBay(a.getEquipmentNum(newmount));
                        } else {
                            // If we get to ammo when we're not working on a bay we treat it as failed
                            // equipment rather than trying to guess.
                            a.getEquipment().remove(newmount);
                            a.getAmmo().remove(newmount);
                            a.addFailedEquipment(equipName);
                        }
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

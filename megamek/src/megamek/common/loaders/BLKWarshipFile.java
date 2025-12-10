/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.TechConstants;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.DockingCollar;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Warship;
import megamek.common.util.BuildingBlock;
import megamek.logging.MMLogger;

/**
 * @author taharqa
 * @since April 6, 2002, 2:06 AM
 */
public class BLKWarshipFile extends BLKFile implements IMekLoader {
    final static private MMLogger LOGGER = MMLogger.create(BLKWarshipFile.class);

    public BLKWarshipFile(BuildingBlock bb) {
        dataFile = bb;
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {

        Warship a = new Warship();
        setBasicEntityData(a);

        if (!dataFile.exists("year")) {
            throw new EntityLoadingException("Could not find year block.");
        }
        a.setYear(dataFile.getDataAsInt("year")[0]);

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
            LOGGER.error("Could not find marine block.");
            // throw new EntityLoadingException("Could not find marines block.");
        }
        a.setNMarines(dataFile.getDataAsInt("marines")[0]);

        // Battle Armor
        if (!dataFile.exists("battlearmor")) {
            LOGGER.error("Could not find battlearmor block.");
            // throw new EntityLoadingException("Could not find battlearmor block.");
        }
        a.setNBattleArmor(dataFile.getDataAsInt("battlearmor")[0]);

        // Passengers
        if (!dataFile.exists("passengers")) {
            throw new EntityLoadingException("Could not find passenger block.");
        }
        a.setNPassenger(dataFile.getDataAsInt("passengers")[0]);

        // Other Passengers
        if (!dataFile.exists("other_crew")) {
            // throw new EntityLoadingException("Could not find other_crew block.");
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
            throw new EntityLoadingException("Could not find structural integrity block.");
        }
        a.setOSI(dataFile.getDataAsInt("structural_integrity")[0]);

        // figure out heat
        if (!dataFile.exists("heatsinks")) {
            throw new EntityLoadingException("Could not find heat sinks block.");
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
        // First, the old method, where a number of grav decks for each category is
        // specified
        // This doesn't allow us to specify precise size
        if (dataFile.exists("grav_deck")) {
            a.setGravDeck(dataFile.getDataAsInt("grav_deck")[0]);
        }
        if (dataFile.exists("grav_deck_large")) {
            a.setGravDeckLarge(dataFile.getDataAsInt("grav_deck_large")[0]);
        }
        if (dataFile.exists("grav_deck_huge")) {
            a.setGravDeckHuge(dataFile.getDataAsInt("grav_deck_huge")[0]);
        }
        // Second, the new method, where a white space separated list of numbers is
        // given
        // Each number represents a distinct grav deck, with the specified size
        if (dataFile.exists("grav_decks")) {
            String[] tokens = dataFile.getDataAsString("grav_decks");
            for (String token : tokens) {
                a.addGravDeck(Integer.parseInt(token));
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
        setArmorTechLevelFromDataFile(a);
        if (dataFile.exists("internal_type")) {
            a.setStructureType(dataFile.getDataAsInt("internal_type")[0]);
        } else {
            a.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);
        }

        // Warships should always be military craft
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
        loadQuirks(a);
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

        int nAmmo;
        // set up a new weapons bay mount
        WeaponMounted bayMount = null;
        // set up a new bay type
        boolean newBay;
        double bayDamage = 0;

        if (saEquip[0] != null) {
            for (String element : saEquip) {
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

                if (etype != null) {
                    // first load the equipment
                    Mounted<?> newMount;
                    try {
                        if (nAmmo == 1) {
                            newMount = a.addEquipment(etype, nLoc, false);
                        } else {
                            newMount = a.addEquipment(etype, nLoc, false, nAmmo);
                        }
                    } catch (LocationFullException ex) {
                        throw new EntityLoadingException(ex.getMessage());
                    }

                    // this is where weapon bays go
                    // first, lets see if it is a weapon
                    if (newMount.getType() instanceof WeaponType weaponType) {
                        // if so then I need to find out if it is the same class
                        // as the current weapon bay
                        // If the current bay is null, then it needs to be
                        // initialized
                        if (bayMount == null) {
                            try {
                                bayMount = (WeaponMounted) a.addEquipment(weaponType.getBayType(), nLoc, false);
                                newBay = false;
                            } catch (LocationFullException ex) {
                                throw new EntityLoadingException(ex.getMessage());
                            }
                        }
                        double damage = weaponType.getShortAV();
                        if (weaponType.isCapital()) {
                            damage *= 10;
                        }
                        if (!newBay
                              && (bayDamage + damage <= 700 || weaponType.hasFlag(WeaponType.F_MASS_DRIVER))
                              && !bayMount.isRearMounted()
                              && weaponType.getAtClass() == bayMount.getType().getAtClass()
                              && !(bayMount.getType().isSubCapital() && !weaponType.isSubCapital())) {
                            // then we should add this weapon to the current bay
                            bayMount.addWeaponToBay(a.getEquipmentNum(newMount));
                            bayDamage += damage;
                        } else {
                            try {
                                bayMount = (WeaponMounted) a.addEquipment(weaponType.getBayType(), nLoc, false);
                            } catch (LocationFullException ex) {
                                throw new EntityLoadingException(ex.getMessage());
                            }
                            bayMount.addWeaponToBay(a.getEquipmentNum(newMount));
                            // reset bay damage
                            bayDamage = damage;
                        }
                    }
                    // ammo should also get loaded into the bay
                    if (newMount.getType() instanceof AmmoType) {
                        if (null != bayMount) {
                            bayMount.addAmmoToBay(a.getEquipmentNum(newMount));
                        } else {
                            // If we get to ammo when we're not working on a bay we treat it as failed
                            // equipment rather than trying to guess.
                            a.getEquipment().remove(newMount);

                            if (newMount instanceof AmmoMounted mountedAmmo) {
                                a.getAmmo().remove(mountedAmmo);
                            }

                            a.addFailedEquipment(equipName);
                        }
                    }
                    if (etype.isVariableSize()) {
                        if (size == 0) {
                            size = MtfFile.extractLegacySize(equipName);
                        }
                        newMount.setSize(size);
                    }
                } else if (!equipName.isBlank()) {
                    a.addFailedEquipment(equipName);
                }
            }
        }
    }
}

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
import megamek.common.units.Jumpship;
import megamek.common.util.BuildingBlock;
import megamek.logging.MMLogger;

/**
 * @author taharqa
 * @since April 6, 2002, 2:06 AM
 */
public class BLKJumpshipFile extends BLKFile implements IMekLoader {
    private final static MMLogger LOGGER = MMLogger.create(BLKJumpshipFile.class);

    public BLKJumpshipFile(BuildingBlock bb) {
        dataFile = bb;
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {

        Jumpship jumpship = new Jumpship();
        setBasicEntityData(jumpship);

        if (!dataFile.exists("tonnage")) {
            throw new EntityLoadingException("Could not find weight block.");
        }
        jumpship.setWeight(dataFile.getDataAsDouble("tonnage")[0]);

        if (dataFile.exists("originalBuildYear")) {
            jumpship.setOriginalBuildYear(dataFile.getDataAsInt("originalBuildYear")[0]);
        }

        // Crew
        if (!dataFile.exists("crew")) {
            throw new EntityLoadingException("Could not find crew block.");
        }
        jumpship.setNCrew(dataFile.getDataAsInt("crew")[0]);

        // Marines
        if (!dataFile.exists("marines")) {
            LOGGER.error("Could not find marines block.");
            // throw new EntityLoadingException("Could not find marines block.");
        }
        jumpship.setNMarines(dataFile.getDataAsInt("marines")[0]);

        // Battle Armor
        if (!dataFile.exists("battlearmor")) {
            LOGGER.error("Could not find battlearmor block.");
            // throw new EntityLoadingException("Could not find battlearmor block.");
        }
        jumpship.setNBattleArmor(dataFile.getDataAsInt("battlearmor")[0]);

        // Passengers
        if (!dataFile.exists("passengers")) {
            throw new EntityLoadingException("Could not find passenger block.");
        }
        jumpship.setNPassenger(dataFile.getDataAsInt("passengers")[0]);

        if (dataFile.exists("officers")) {
            jumpship.setNOfficers(dataFile.getDataAsInt("officers")[0]);
        }

        if (dataFile.exists("gunners")) {
            jumpship.setNGunners(dataFile.getDataAsInt("gunners")[0]);
        }

        // Other Passengers
        if (!dataFile.exists("other_crew")) {
            // throw new EntityLoadingException("Could not find other_crew block.");
        }

        jumpship.setNOtherCrew(dataFile.getDataAsInt("other_crew")[0]);

        if (!dataFile.exists("life_boat")) {
            throw new EntityLoadingException("Could not find life boat block.");
        }
        jumpship.setLifeBoats(dataFile.getDataAsInt("life_boat")[0]);

        if (!dataFile.exists("escape_pod")) {
            throw new EntityLoadingException("Could not find escape pod block.");
        }
        jumpship.setEscapePods(dataFile.getDataAsInt("escape_pod")[0]);

        // get jumpship movement mode - lets try Aerodyne
        EntityMovementMode nMotion = EntityMovementMode.AERODYNE;
        jumpship.setMovementMode(nMotion);

        // figure out structural integrity
        if (!dataFile.exists("structural_integrity")) {
            throw new EntityLoadingException("Could not find structural integrity block.");
        }
        jumpship.setOSI(dataFile.getDataAsInt("structural_integrity")[0]);

        // figure out heat
        if (!dataFile.exists("heatsinks")) {
            throw new EntityLoadingException("Could not find heats inks block.");
        }
        jumpship.setHeatSinks(dataFile.getDataAsInt("heatsinks")[0]);
        jumpship.setOHeatSinks(dataFile.getDataAsInt("heatsinks")[0]);
        if (!dataFile.exists("sink_type")) {
            throw new EntityLoadingException("Could not find sink_type block.");
        }
        jumpship.setHeatType(dataFile.getDataAsInt("sink_type")[0]);

        // figure out fuel
        if (!dataFile.exists("fuel")) {
            throw new EntityLoadingException("Could not find fuel block.");
        }
        jumpship.setFuel(dataFile.getDataAsInt("fuel")[0]);

        jumpship.setOriginalWalkMP(0);

        jumpship.setEngine(new Engine(400, 0, 0));

        if (dataFile.exists("lithium-fusion")) {
            jumpship.setLF(true);
        }

        if (dataFile.exists("hpg")) {
            jumpship.setHPG(true);
        }

        if (dataFile.exists("sail")) {
            jumpship.setSail(dataFile.getDataAsInt("sail")[0] != 0);
        }

        // Grav Decks - two approaches
        // First, the old method, where jumpship number of grav decks for each category is
        // specified
        // This doesn't allow us to specify precise size
        if (dataFile.exists("grav_deck")) {
            jumpship.setGravDeck(dataFile.getDataAsInt("grav_deck")[0]);
        }
        if (dataFile.exists("grav_deck_large")) {
            jumpship.setGravDeckLarge(dataFile.getDataAsInt("grav_deck_large")[0]);
        }
        if (dataFile.exists("grav_deck_huge")) {
            jumpship.setGravDeckHuge(dataFile.getDataAsInt("grav_deck_huge")[0]);
        }
        // Second, the new method, where jumpship white space separated list of numbers is
        // given
        // Each number represents jumpship distinct grav deck, with the specified size
        if (dataFile.exists("grav_decks")) {
            String[] tokens = dataFile.getDataAsString("grav_decks");
            for (String token : tokens) {
                jumpship.addGravDeck(Integer.parseInt(token));
            }
        }
        // Add jumpship damage tracker value for each grav deck
        for (int i = 0; i < jumpship.getTotalGravDeck(); i++) {
            jumpship.initializeGravDeckDamage(i);
        }

        // Switch older files with standard armor to aerospace
        int at = EquipmentType.T_ARMOR_AEROSPACE;
        if (dataFile.exists("armor_type")) {
            at = dataFile.getDataAsInt("armor_type")[0];
            if (at == EquipmentType.T_ARMOR_STANDARD) {
                at = EquipmentType.T_ARMOR_AEROSPACE;
            }
        }
        jumpship.setArmorType(at);
        setArmorTechLevelFromDataFile(jumpship);
        if (dataFile.exists("internal_type")) {
            jumpship.setStructureType(dataFile.getDataAsInt("internal_type")[0]);
        } else {
            jumpship.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);
        }

        if (dataFile.exists("designtype")) {
            jumpship.setDesignType(dataFile.getDataAsInt("designtype")[0]);
        } else {
            jumpship.setDesignType(Aero.CIVILIAN);
        }

        if (!dataFile.exists("armor")) {
            throw new EntityLoadingException("Could not find armor block.");
        }

        int[] armor = dataFile.getDataAsInt("armor");

        if (armor.length != 6) {
            throw new EntityLoadingException("Incorrect armor array length");
        }

        for (int i = 0; i < armor.length; i++) {
            jumpship.initializeArmor(armor[i], i);
        }
        jumpship.initializeArmor(IArmorState.ARMOR_NA, Jumpship.LOC_HULL);

        jumpship.autoSetInternal();
        jumpship.autoSetThresh();
        jumpship.initializeKFIntegrity();
        jumpship.initializeSailIntegrity();
        jumpship.recalculateTechAdvancement();

        for (int loc = 0; loc < jumpship.locations(); loc++) {
            loadEquipment(jumpship, jumpship.getLocationName(loc), loc);
        }

        // legacy
        loadEquipment(jumpship, "Front Right Side", Jumpship.LOC_FRS);
        loadEquipment(jumpship, "Front Left Side", Jumpship.LOC_FLS);

        addTransports(jumpship);

        // get docking collars (legacy BLK files)
        int docks = dataFile.getDataAsInt("docking_collar")[0];
        while (docks > 0) {
            jumpship.addTransporter(new DockingCollar(jumpship.getTransports().size() + 1));
            docks--;
        }
        jumpship.setArmorTonnage(jumpship.getArmorWeight());
        loadQuirks(jumpship);
        return jumpship;
    }

    protected void loadEquipment(Jumpship a, String sName, int nLoc) throws EntityLoadingException {
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
                              && bayDamage + damage <= 700
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
                    if (etype.isVariableSize()) {
                        newMount.setSize(size);
                    } else if (newMount.getType() instanceof AmmoType) {
                        // ammo should also get loaded into the bay
                        if (bayMount != null) {
                            bayMount.addAmmoToBay(a.getEquipmentNum(newMount));
                        }
                    }
                } else if (!equipName.isBlank()) {
                    a.addFailedEquipment(equipName);
                }
            }
        }
    }
}

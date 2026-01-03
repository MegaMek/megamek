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

import megamek.common.enums.ProstheticEnhancementType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.InfantryWeaponMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Infantry;
import megamek.common.units.InfantryMount;
import megamek.common.util.BuildingBlock;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * This class loads Infantry BLK files.
 *
 * @author Suvarov454@sourceforge.net (James A. Damour)
 * @since April 6, 2002, 2:06 AM
 */
public class BLKInfantryFile extends BLKFile implements IMekLoader {

    public BLKInfantryFile(BuildingBlock bb) {
        dataFile = bb;
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {

        Infantry infantry = new Infantry();
        setBasicEntityData(infantry);

        if (!dataFile.exists("squad_size")) {
            throw new EntityLoadingException("Could not find squad size.");
        }
        infantry.setSquadSize(dataFile.getDataAsInt("squad_size")[0]);
        if (!dataFile.exists("squadn")) {
            throw new EntityLoadingException("Could not find number of squads.");
        }
        infantry.setSquadCount(dataFile.getDataAsInt("squadn")[0]);

        infantry.autoSetInternal();

        if (dataFile.exists("InfantryArmor")) {
            infantry.setCustomArmorDamageDivisor(dataFile.getDataAsInt("InfantryArmor")[0]);
        }

        if (!dataFile.exists("motion_type")) {
            throw new EntityLoadingException("Could not find movement block.");
        }
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        infantry.setMicrolite(sMotion.equalsIgnoreCase("microlite"));
        if (sMotion.startsWith("Beast:")) {
            infantry.setMount(InfantryMount.parse(sMotion));
        } else {
            EntityMovementMode nMotion = EntityMovementMode.parseFromString(sMotion);
            if (nMotion.isNone()) {
                throw new EntityLoadingException("Invalid movement type: " + sMotion);
            }
            if (nMotion == EntityMovementMode.INF_UMU
                  && sMotion.toLowerCase().contains("motorized")) {
                infantry.setMotorizedScuba();
            } else {
                infantry.setMovementMode(nMotion);
            }
        }

        // get primary and secondary weapons
        // the check for "secondary" is for legacy infantry files that have a <secondn> block but no actual secondary weapon
        if (dataFile.exists("secondn") && dataFile.exists("Secondary")) {
            infantry.setSecondaryWeaponsPerSquad(dataFile.getDataAsInt("secondn")[0]);
        }

        if (!dataFile.exists("Primary")) {
            throw new EntityLoadingException("Could not find primary weapon.");
        }
        String primaryName = dataFile.getDataAsString("Primary")[0];
        EquipmentType primaryWeaponType = EquipmentType.get(primaryName);
        if (!(primaryWeaponType instanceof InfantryWeapon)) {
            throw new EntityLoadingException("primary weapon is not an infantry weapon");
        }
        infantry.setPrimaryWeapon((InfantryWeapon) primaryWeaponType);

        EquipmentType secondaryWeaponType = null;
        if (dataFile.exists("Secondary")) {
            String secondName = dataFile.getDataAsString("Secondary")[0];
            secondaryWeaponType = EquipmentType.get(secondName);
            if (!(secondaryWeaponType instanceof InfantryWeapon)) {
                throw new EntityLoadingException("secondary weapon " + secondName + " is not an infantry weapon");
            }
            infantry.setSecondaryWeapon((InfantryWeapon) secondaryWeaponType);
        }

        // if there is more than one secondary weapon per squad, then add that to the
        // unit
        // otherwise add the primary weapon
        Mounted<?> m;
        try {
            if ((infantry.getSecondaryWeaponsPerSquad() > 1)) {
                m = new InfantryWeaponMounted(infantry,
                      (InfantryWeapon) secondaryWeaponType,
                      (InfantryWeapon) primaryWeaponType);
            } else if (secondaryWeaponType != null) {
                m = new InfantryWeaponMounted(infantry,
                      (InfantryWeapon) primaryWeaponType,
                      (InfantryWeapon) secondaryWeaponType);
            } else {
                m = Mounted.createMounted(infantry, primaryWeaponType);
            }
        } catch (ClassCastException ex) {
            throw new EntityLoadingException(ex.getMessage());
        }
        try {
            infantry.addEquipment(m, Infantry.LOC_INFANTRY, false);
        } catch (LocationFullException ex) {
            throw new EntityLoadingException(ex.getMessage());
        }

        // TAG infantry have separate attacks for primary and secondary weapons.
        if (null != secondaryWeaponType && secondaryWeaponType.hasFlag(WeaponType.F_TAG)) {
            infantry.setSpecializations(infantry.getSpecializations() | Infantry.TAG_TROOPS);
            try {
                infantry.addEquipment(primaryWeaponType, Infantry.LOC_INFANTRY);
            } catch (LocationFullException ex) {
                throw new EntityLoadingException(ex.getMessage());
            }
        }

        // backward compatibility: armor kits should now be saved and loaded as part of
        // LOC_INFANTRY equipment
        if (dataFile.exists("armorKit")) {
            String kitName = dataFile.getDataAsString("armorKit")[0];
            EquipmentType kit = EquipmentType.get(kitName);
            if ((null == kit) || !(kit.hasFlag(MiscType.F_ARMOR_KIT))) {
                throw new EntityLoadingException(kitName + " is not an infantry armor kit");
            }
            infantry.setArmorKit(kit);
        }

        if (dataFile.exists("dest")) {
            infantry.setDEST(true);
        }

        if (dataFile.exists("specialization")) {
            try {
                infantry.setSpecializations(Integer.parseInt(dataFile.getDataAsString("specialization")[0]));
            } catch (NumberFormatException ex) {
                throw new EntityLoadingException("Could not read specialization");
            }
        }

        if (dataFile.exists("encumberingarmor")) {
            infantry.setArmorEncumbering(true);
        }

        if (dataFile.exists("spacesuit")) {
            infantry.setSpaceSuit(true);
        }

        if (dataFile.exists("sneakcamo")) {
            infantry.setSneakCamo(true);
        }

        if (dataFile.exists("sneakir")) {
            infantry.setSneakIR(true);
        }

        if (dataFile.exists("sneakecm")) {
            infantry.setSneakECM(true);
        }

        if (dataFile.exists("armordivisor")) {
            try {
                infantry.setCustomArmorDamageDivisor(Double.parseDouble(dataFile.getDataAsString("armordivisor")[0]));
            } catch (NumberFormatException ex) {
                throw new EntityLoadingException("Could not read armor divisor");
            }
        }

        loadEquipment(infantry, "Field Guns", Infantry.LOC_FIELD_GUNS);
        loadEquipment(infantry, "Troopers", Infantry.LOC_INFANTRY);

        // Update some internals if there's an armor kit
        infantry.getMisc().stream()
              .filter(misc -> misc.getType().hasFlag(MiscType.F_ARMOR_KIT))
              .findFirst()
              .ifPresent(mounted -> infantry.setArmorKit(mounted.getType()));

        // backward compatibility: if an anti mek better than 8 entry exists, assume
        // anti-mek gear
        if (dataFile.exists("antimek")) {
            int[] amSkill = dataFile.getDataAsInt("antimek");
            if (amSkill[0] != 8) {
                try {
                    infantry.addEquipment(EquipmentType.get(EquipmentTypeLookup.ANTI_MEK_GEAR), Infantry.LOC_INFANTRY);
                } catch (LocationFullException ex) {
                    throw new EntityLoadingException(ex.getMessage());
                }
            }
        }

        // Some units (mostly Manei Domini) have cybernetics/prosthetics as part of the
        // official unit description.
        if (dataFile.exists("augmentation")) {
            String[] augmentations = dataFile.getDataAsString("augmentation");
            for (String aug : augmentations) {
                try {
                    infantry.getCrew().getOptions().getOption(aug).setValue(true);
                } catch (NullPointerException ex) {
                    throw new EntityLoadingException("Could not locate pilot option " + aug);
                }
            }
        }

        // Prosthetic Enhancement (Enhanced Limbs) - IO p.84
        // Slot 1 (Standard Enhanced and Improved Enhanced)
        if (dataFile.exists("prostheticEnhancement1")) {
            String enhancementName = dataFile.getDataAsString("prostheticEnhancement1")[0];
            ProstheticEnhancementType enhancement = ProstheticEnhancementType.parseFromString(enhancementName);
            if (enhancement == null) {
                throw new EntityLoadingException("Invalid prosthetic enhancement 1: " + enhancementName);
            }
            infantry.setProstheticEnhancement1(enhancement);

            if (dataFile.exists("prostheticEnhancement1Count")) {
                infantry.setProstheticEnhancement1Count(dataFile.getDataAsInt("prostheticEnhancement1Count")[0]);
            } else {
                infantry.setProstheticEnhancement1Count(1);
            }
        } else if (dataFile.exists("prostheticEnhancement")) {
            // Legacy format support - old single-slot format maps to slot 1
            String enhancementName = dataFile.getDataAsString("prostheticEnhancement")[0];
            ProstheticEnhancementType enhancement = ProstheticEnhancementType.parseFromString(enhancementName);
            if (enhancement == null) {
                throw new EntityLoadingException("Invalid prosthetic enhancement: " + enhancementName);
            }
            infantry.setProstheticEnhancement1(enhancement);

            if (dataFile.exists("prostheticEnhancementCount")) {
                infantry.setProstheticEnhancement1Count(dataFile.getDataAsInt("prostheticEnhancementCount")[0]);
            } else {
                infantry.setProstheticEnhancement1Count(1);
            }
        }

        // Slot 2 (Improved Enhanced only)
        if (dataFile.exists("prostheticEnhancement2")) {
            String enhancementName = dataFile.getDataAsString("prostheticEnhancement2")[0];
            ProstheticEnhancementType enhancement = ProstheticEnhancementType.parseFromString(enhancementName);
            if (enhancement == null) {
                throw new EntityLoadingException("Invalid prosthetic enhancement 2: " + enhancementName);
            }
            infantry.setProstheticEnhancement2(enhancement);

            if (dataFile.exists("prostheticEnhancement2Count")) {
                infantry.setProstheticEnhancement2Count(dataFile.getDataAsInt("prostheticEnhancement2Count")[0]);
            } else {
                infantry.setProstheticEnhancement2Count(1);
            }
        }

        // Extraneous (Enhanced) Limbs - each pair always provides 2 items
        if (dataFile.exists("extraneousPair1")) {
            String enhancementName = dataFile.getDataAsString("extraneousPair1")[0];
            ProstheticEnhancementType enhancement = ProstheticEnhancementType.parseFromString(enhancementName);
            if (enhancement == null) {
                throw new EntityLoadingException("Invalid extraneous pair 1: " + enhancementName);
            }
            infantry.setExtraneousPair1(enhancement);
        }

        if (dataFile.exists("extraneousPair2")) {
            String enhancementName = dataFile.getDataAsString("extraneousPair2")[0];
            ProstheticEnhancementType enhancement = ProstheticEnhancementType.parseFromString(enhancementName);
            if (enhancement == null) {
                throw new EntityLoadingException("Invalid extraneous pair 2: " + enhancementName);
            }
            infantry.setExtraneousPair2(enhancement);
        }

        infantry.recalculateTechAdvancement();
        loadQuirks(infantry);
        return infantry;
    }
}

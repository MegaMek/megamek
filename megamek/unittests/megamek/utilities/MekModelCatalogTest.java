/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import megamek.client.ui.tileset.UnitModelKey;
import megamek.common.equipment.EquipmentBitSet;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponType;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MekModelCatalogTest {
    @Test
    void exportedMountsMatchTheLoadedVariantIncludingRearWeapons() throws Exception {
        EquipmentType.initializeTypes();
        Mek atlas = (Mek) new MekFileParser(new File("testresources/megamek/common/units/Atlas AS7-D.mtf")).getEntity();
        var mounts = MekModelCatalog.mounts(atlas);
        assertEquals(atlas.getEquipment().size(), mounts.size());
        assertEquals(2, mounts.stream().filter(m -> m.family().equals("laser") && m.rear()).count());
        assertTrue(mounts.stream().anyMatch(m -> m.family().equals("missile") && m.rackSize() == 20
              && m.location().equals("LT")));
        for (var mount : mounts) {
            assertEquals(atlas.getEquipment(mount.index()).getType().getInternalName(), mount.internalName());
        }
        String stockKey = UnitModelKey.forEntity(atlas);
        String stockName = atlas.getShortNameRaw();
        atlas.addEquipment(EquipmentType.get("ISSmallLaser"), Mek.LOC_LEFT_ARM);
        assertEquals(stockName, atlas.getShortNameRaw());
        assertNotEquals(stockKey, UnitModelKey.forEntity(atlas), "A same-name refit must not select the stock mesh");
    }

    @ParameterizedTest
    @CsvSource({
          "Medium Laser, laser",
          "CLERPPC, ppc",
          "Machine Gun, machine-gun",
          "CLGaussRifle, ballistic",
          "ISAntiMissileSystem, ballistic",
          "ISLaserAntiMissileSystem, laser",
          "LRM 20, missile",
          "ISMekMortar4, missile",
          "Flamer, flamer",
          "ISPlasmaRifle, energy",
          "TSEMP Cannon, energy",
          "ISC3MasterUnit, sensor",
          "ISC3MasterBoostedSystemUnit, sensor",
          "ISLongTom, ballistic",
          "ISPrimitiveLongTom, ballistic",
          "ISSniper, ballistic",
          "ISThumper, ballistic",
          "ISCruiseMissile50, missile",
          "ISCruiseMissile120, missile",
          "BombArrowIV, missile",
          "BombTAG, sensor",
          "Fire Extinguisher, extinguisher",
          "Screen Launcher, screen-launcher",
          "CLBAMicroBomb, bomb",
          "BAMineLauncher, mine",
          "Blade (Sword), infantry-melee",
          "Staff (Shock Staff), infantry-melee",
          "Prosthetic Climbing Claws, infantry-melee",
          "Bow (Compound), ballistic",
          "ISLMGA, internal",
          "AC Bay, internal",
          "AltBombAttack, internal",
          "DiveBombAttack, internal",
          "SpaceBombAttack, internal",
          "LegAttack, internal",
          "SwarmMek, internal",
          "SwarmWeaponMek, internal",
          "StopSwarm, internal"
    })
    void weaponFamiliesPreserveExistingArtAndCoverSpecialEquipment(String internalName, String family) {
        EquipmentType.initializeTypes();
        EquipmentType type = EquipmentType.get(internalName);
        assertNotNull(type, internalName);
        assertTrue(type instanceof WeaponType, internalName);
        assertEquals(family, MekModelCatalog.family(type), internalName);
    }

    @ParameterizedTest
    @CsvSource({
          "Hatchet, hatchet",
          "Sword, blade",
          "Retractable Blade, blade",
          "ISSmallVibroblade, blade",
          "ISMediumVibroblade, blade",
          "ISLargeVibroblade, blade",
          "Mace, mace",
          "IS Lance, lance",
          "IS Flail, flail",
          "Chain Whip, chain-whip",
          "IS Wrecking Ball, wrecking-ball",
          "ISClaw, claw",
          "ISSmallShield, shield",
          "ISMediumShield, shield",
          "ISLargeShield, shield",
          "Spikes, spikes",
          "Talons, talons",
          "Tree Club, club",
          "Girder Club, club",
          "Limb Club, club",
          "Chainsaw, saw",
          "Dual Saw, saw",
          "Buzzsaw, saw",
          "Backhoe, backhoe",
          "Combine, combine",
          "Heavy-Duty Pile Driver, pile-driver",
          "MiningDrill, mining-drill",
          "Rock Cutter, rock-cutter",
          "Spot Welder, spot-welder"
    })
    void physicalFamiliesCoverWeaponsWithoutClubsAndGroupRelatedShapes(String internalName, String family) {
        EquipmentType.initializeTypes();
        EquipmentType type = EquipmentType.get(internalName);
        assertNotNull(type, internalName);
        assertTrue(type instanceof MiscType && type.hasFlag(MiscType.F_PHYSICAL_WEAPON), internalName);
        assertEquals(family, MekModelCatalog.family(type), internalName);
    }

    @Test
    void allRegisteredWeaponsAndPhysicalEquipmentHaveKnownFamilies() {
        for (EquipmentType type : EquipmentType.allTypes()) {
            if ((type instanceof WeaponType)
                  || ((type instanceof MiscType) && type.hasFlag(MiscType.F_PHYSICAL_WEAPON))) {
                String family = MekModelCatalog.family(type);
                assertFalse(family.startsWith("unmapped-"), type.getInternalName());
                if (type instanceof MiscType) {
                    assertNotEquals("internal", family, type.getInternalName());
                }
            }
        }
    }

    @Test
    void unrecognizedEquipmentRetainsAnExplicitFallbackAndDoesNotBecomeVisibleByName() {
        assertEquals("unmapped-weapon", MekModelCatalog.family(new WeaponType()));
        MiscType misc = MiscType.createSword();
        misc.setFlags(new EquipmentBitSet());
        assertEquals("internal", MekModelCatalog.family(misc));
        misc.getFlags().set(MiscType.F_PHYSICAL_WEAPON);
        assertEquals("unmapped-melee", MekModelCatalog.family(misc));
    }
}

/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.BombType;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Jumpship;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.SmallCraft;
import megamek.common.units.Tank;
import megamek.common.verifier.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class BulkUnitFileTest {

    @BeforeAll
    public static void initializeStuff() {
        MekFileParser.initCanonUnitNames();
        EquipmentType.initializeTypes();
        AmmoType.initializeTypes();
        ArmorType.initializeTypes();
        WeaponType.initializeTypes();
        MiscType.initializeTypes();
        BombType.initializeTypes();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allBlkFiles")
    void loadVerifySaveVerifyBLKFiles(File file) throws EntitySavingException, IOException {
        checkEntityFile(file);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allMtfFiles")
    void loadVerifySaveVerifyMTFFiles(File file) throws EntitySavingException, IOException {
        checkEntityFile(file);
    }

    void checkEntityFile(File file) throws EntitySavingException, IOException {
        Entity entity = loadUnit(file);
        var validation = verify(entity);
        // This print is to make sure you are looking at the file you expected to be looking at
        System.out.println(file.getAbsoluteFile());
        assertEquals(UnitValidation.VALID, validation.state(),
              "The unit is invalid:\n\t" + entity.getDisplayName() + "\n" + validation.report());

        var suffix = entity instanceof Mek ? ".mtf" : ".blk";
        var tmpFile = File.createTempFile(file.getName(), suffix);
        tmpFile.deleteOnExit();

        if (persistUnit(tmpFile, entity)) {
            Entity repersistedEntity = loadUnit(tmpFile);
            var reValidation = verify(repersistedEntity);
            assertEquals(UnitValidation.VALID, reValidation.state(),
                  "The unit is invalid after re-persisting:\n\t"
                        + tmpFile
                        + "\n\t"
                        + entity.getDisplayName()
                        + "\n"
                        + reValidation.report());
            assertEquals(reValidation.state(), validation.state());
        }
    }

    @Test
    void loadVerifySaveVerifySpecificFile() throws EntitySavingException, IOException {
        var file = new File("testresources/data/mekfiles/Aegis Heavy Cruiser (2372).blk");
        checkEntityFile(file);
    }

    private static final List<String> excludeFiles = List.of("Air Car.blk");

    public static List<File> allMtfFiles() {
        try (Stream<Path> paths = Files.walk(Paths.get("testresources/data/mekfiles"))) {
            return paths
                  .filter(Files::isRegularFile)
                  .filter(path -> path.toString().endsWith(".mtf"))
                  .map(Path::toFile)
                  .filter(file -> !excludeFiles.contains(file.getName()))
                  .toList();
        } catch (IOException e) {
            // do nothing
        }
        return List.of();
    }

    public static List<File> allBlkFiles() {
        try (Stream<Path> paths = Files.walk(Paths.get("testresources/data/mekfiles"))) {
            return paths
                  .filter(Files::isRegularFile)
                  .filter(path -> path.toString().endsWith(".blk"))
                  .map(Path::toFile)
                  .filter(file -> !excludeFiles.contains(file.getName()))
                  .toList();
        } catch (IOException e) {
            // do nothing
        }
        return List.of();
    }

    private Entity loadUnit(File file) {
        try {
            MekFileParser mfp = new MekFileParser(file);
            return mfp.getEntity();
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        throw new IllegalStateException("Should not reach here");
    }

    public static TestEntity getEntityVerifier(Entity unit) {
        EntityVerifier entityVerifier = EntityVerifier.getInstance(new File(
              "testresources/data/mekfiles/UnitVerifierOptions.xml"));
        TestEntity testEntity = null;

        if (unit.hasETypeFlag(Entity.ETYPE_MEK)) {
            testEntity = new TestMek((Mek) unit, entityVerifier.mekOption, null);
        } else if (unit.hasETypeFlag(Entity.ETYPE_PROTOMEK)) {
            testEntity = new TestProtoMek((ProtoMek) unit, entityVerifier.protomekOption, null);
        } else if (unit.isSupportVehicle()) {
            testEntity = new TestSupportVehicle(unit, entityVerifier.tankOption, null);
        } else if (unit.hasETypeFlag(Entity.ETYPE_TANK)) {
            testEntity = new TestTank((Tank) unit, entityVerifier.tankOption, null);
        } else if (unit.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
            testEntity = new TestSmallCraft((SmallCraft) unit, entityVerifier.aeroOption, null);
        } else if (unit.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
            testEntity = new TestAdvancedAerospace((Jumpship) unit, entityVerifier.aeroOption, null);
        } else if (unit.hasETypeFlag(Entity.ETYPE_AERO)) {
            testEntity = new TestAero((Aero) unit, entityVerifier.aeroOption, null);
        } else if (unit.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)) {
            testEntity = new TestBattleArmor((BattleArmor) unit, entityVerifier.baOption, null);
        } else if (unit.hasETypeFlag(Entity.ETYPE_INFANTRY)) {
            testEntity = new TestInfantry((Infantry) unit, entityVerifier.infOption, null);
        } else if (unit.hasETypeFlag(Entity.ETYPE_BUILDING_ENTITY)) {
            testEntity = new TestBuilding((AbstractBuildingEntity) unit, entityVerifier.tankOption, null);
        }
        return testEntity;
    }

    private enum UnitValidation {
        VALID,
        INVALID;

        public static UnitValidation of(boolean valid) {
            return valid ? VALID : INVALID;
        }
    }

    private record Validation(UnitValidation state, String report) {}

    private static Validation verify(Entity unit) {
        var sb = new StringBuffer();
        var testEntity = getEntityVerifier(unit);

        var succeeded = testEntity.correctEntity(sb, unit.getTechLevel());

        return new Validation(UnitValidation.of(succeeded), sb.toString());
    }

    public static boolean persistUnit(File outFile, Entity entity) throws EntitySavingException {
        if (entity instanceof Mek) {
            try (BufferedWriter out = new BufferedWriter(new FileWriter(outFile))) {
                out.write(((Mek) entity).getMtf());
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return false;
            }
            return true;
        }

        BLKFile.encode(outFile, entity);
        return true;
    }

}

/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

import megamek.common.SimpleTechLevel;
import megamek.common.battlefieldSupport.BFSArtilleryType;
import megamek.common.battlefieldSupport.BFSAssetType;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.battlefieldSupport.BattlefieldSupportAssetData;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Era;
import megamek.common.enums.TechRating;
import megamek.common.equipment.EquipmentType;
import megamek.common.interfaces.ITechnology;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BFSFileTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void loadsAssetFromBfsFixtureFile() throws Exception {
        Entity entity = new MekFileParser(
              new File("testresources/data/mekfiles/Maxim Heavy Hover Transport.bfs")).getEntity();

        BattlefieldSupportAsset asset = assertInstanceOf(BattlefieldSupportAsset.class, entity);
        assertTrue(asset.isBattlefieldSupportAsset());
        assertEquals(UnitType.BATTLEFIELD_SUPPORT_ASSET, asset.getUnitType());
        // The asset carries its own unit-file UUID and links to its base unit by the base unit's UUID.
        assertEquals("019f5efd-5a93-78c3-b4f2-dd83804af175", asset.getUnitFileUUID());
        assertTrue(asset.isLinkedToBaseUnit());
        assertEquals("019f583e-e2c6-7b99-a188-ba0759db128e", asset.getLinkedUnitId());
        assertEquals("Maxim Heavy Hover Transport", asset.getChassis());
        assertEquals(BFSAssetType.VEHICLE, asset.getAssetType());
        assertEquals(3151, asset.getYear());
        assertEquals("BattleTech: Mercenaries", asset.getSource());
        // Tech base "IS" round-trips as a non-Clan asset (assets are always tech level Standard).
        assertFalse(asset.isClan());
        assertEquals(8, asset.getMp());
        assertEquals(EntityMovementMode.HOVER, asset.getMovementMode());
        assertEquals("8H", asset.getMovementDisplay());
        assertEquals("3/6/9", asset.getRangeDisplay());
        assertEquals("6(5)", asset.getSkillDisplay());
        assertEquals("5x4", asset.getDamageDisplay());
        assertEquals("23(27)", asset.getCostDisplay());
        assertEquals("APC1, IF2", asset.getSpecialsDisplay());
    }

    @Test
    void loadsLinkedArtilleryAssetWithKeywordRange() throws Exception {
        BattlefieldSupportAsset asset = assertInstanceOf(BattlefieldSupportAsset.class, new MekFileParser(
              new File("testresources/data/mekfiles/Mobile Long Tom LT-MOB-25.bfs")).getEntity());

        assertEquals("Mobile Long Tom Artillery", asset.getChassis());
        assertEquals("LT-MOB-25", asset.getModel());
        assertEquals(BFSAssetType.VEHICLE, asset.getAssetType());
        assertEquals("2T", asset.getMovementDisplay());
        // Keyword range renders the artillery type derived from the Artillery (LT) special.
        assertEquals("Long Tom", asset.getRangeDisplay());
        assertEquals(BFSArtilleryType.LONG_TOM, asset.getArtilleryType());
        assertEquals("8", asset.getSkillDisplay());
        // Regular-only unit: no Veteran skill or cost column on the card.
        assertNull(asset.getVeteranSkill());
        assertNull(asset.getVeteranCost());
        assertEquals("66", asset.getCostDisplay());
        // Zero damage renders as an em dash.
        assertEquals("\u2014", asset.getDamageDisplay());
        assertEquals("Artillery (LT), No Turret", asset.getSpecialsDisplay());
    }

    @Test
    void loadsStandaloneEmplacementAsset() throws Exception {
        BattlefieldSupportAsset asset = assertInstanceOf(BattlefieldSupportAsset.class, new MekFileParser(
              new File("testresources/data/mekfiles/Heavy Emplacement.bfs")).getEntity());

        assertEquals("Heavy Emplacement", asset.getChassis());
        assertEquals(BFSAssetType.EMPLACEMENT, asset.getAssetType());
        // A standalone asset is not linked to any base unit.
        assertFalse(asset.isLinkedToBaseUnit());
        assertNull(asset.getLinkedUnitId());
        assertEquals(EntityMovementMode.NONE, asset.getMovementMode());
        // Immobile emplacement: MP 0 with no movement-mode letter.
        assertEquals("0", asset.getMovementDisplay());
        // Immobile assets append a * to the TMM to flag the effective -4 Immobile modifier.
        assertEquals("+0*", asset.getTmmDisplay());
        assertTrue(asset.isImmobileAsset());
        assertEquals("3/6/9", asset.getRangeDisplay());
        assertEquals("5(4)", asset.getSkillDisplay());
        // Single grouping renders without the count.
        assertEquals("15", asset.getDamageDisplay());
        assertEquals("13(15)", asset.getCostDisplay());
        assertEquals("Immobile, No Turret, Spotter", asset.getSpecialsDisplay());
    }

    @Test
    void loadsLinkedInfantryAsset() throws Exception {
        BattlefieldSupportAsset asset = assertInstanceOf(BattlefieldSupportAsset.class, new MekFileParser(
              new File("testresources/data/mekfiles/Foot Platoon (Rifle).bfs")).getEntity());

        assertEquals("Foot Platoon", asset.getChassis());
        assertEquals("(Rifle)", asset.getModel());
        assertEquals(BFSAssetType.CONV_INFANTRY, asset.getAssetType());
        assertEquals(EntityMovementMode.INF_LEG, asset.getMovementMode());
        assertEquals("1F", asset.getMovementDisplay());
        assertEquals("+0", asset.getTmmDisplay());
        assertEquals("1/2/3", asset.getRangeDisplay());
        assertEquals("5x2", asset.getDamageDisplay());
        assertEquals("7(8)", asset.getCostDisplay());
        assertEquals("Nimble, Spotter, Swarm", asset.getSpecialsDisplay());
    }

    @Test
    void loadsLinkedBattleArmorAsset() throws Exception {
        BattlefieldSupportAsset asset = assertInstanceOf(BattlefieldSupportAsset.class, new MekFileParser(
              new File("testresources/data/mekfiles/Elemental Battle Armor [MG] (Sqd5).bfs")).getEntity());

        assertEquals("Elemental Battle Armor", asset.getChassis());
        assertEquals("[MG](Sqd5)", asset.getModel());
        assertEquals(BFSAssetType.BATTLE_ARMOR, asset.getAssetType());
        assertEquals(EntityMovementMode.INF_JUMP, asset.getMovementMode());
        assertEquals("3J", asset.getMovementDisplay());
        assertEquals("+2", asset.getTmmDisplay());
        assertEquals("6x2", asset.getDamageDisplay());
        assertEquals("17(20)", asset.getCostDisplay());
        assertEquals("Mechanized, Nimble, Spotter, Swarm", asset.getSpecialsDisplay());
    }

    @Test
    void loadsAssetWithNoRangeOrDamage() throws Exception {
        BattlefieldSupportAsset asset = assertInstanceOf(BattlefieldSupportAsset.class, new MekFileParser(
              new File("testresources/data/mekfiles/Browning Mobile HQ.bfs")).getEntity());

        assertEquals("Browning Mobile HQ", asset.getChassis());
        assertEquals("7W", asset.getMovementDisplay());
        assertEquals("+3", asset.getTmmDisplay());
        // No ranged attack and no relevant keyword special -> em dash.
        assertEquals("\u2014", asset.getRangeDisplay());
        // No damage -> em dash.
        assertEquals("\u2014", asset.getDamageDisplay());
        assertEquals("18(21)", asset.getCostDisplay());
        assertEquals("AMS, ECM6", asset.getSpecialsDisplay());
    }

    @Test
    void mekFileParserDispatchesByBfsExtension() throws Exception {
        String yaml = """
              chassis: "Test Asset"
              model: "X"
              assetType: "Conventional Infantry"
              movement:
                mp: 1
                mode: "INF_LEG"
              tmm: 0
              range: [1, 2, 3]
              skill:
                standard: 5
              damage:
                perHit: 2
                hits: 1
              destroyCheck: 8
              threshold: 3
              cost:
                standard: 4
              specials:
              - "TAG"
              """;
        MekFileParser parser = new MekFileParser(
              new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "Test Asset.bfs");
        BattlefieldSupportAsset asset = assertInstanceOf(BattlefieldSupportAsset.class, parser.getEntity());
        assertEquals("Test Asset", asset.getChassis());
        assertEquals(BFSAssetType.CONV_INFANTRY, asset.getAssetType());
        assertEquals(EntityMovementMode.INF_LEG, asset.getMovementMode());
        assertEquals("1F", asset.getMovementDisplay());
    }

    @Test
    void mekFileParserDispatchesBfsFromContentString() throws Exception {
        // The undo/redo mementos parse an in-memory unit string with no filename, so parse(String) must recognize the
        // .bfs YAML format by content (not extension). Previously it defaulted to .mtf and silently failed, breaking
        // undo for assets.
        String yaml = """
              chassis: "Content Asset"
              model: "Y"
              assetType: "Vehicle"
              movement:
                mp: 5
                mode: "TRACKED"
              tmm: 2
              range: [1, 2, 3]
              skill:
                standard: 6
              damage:
                perHit: 3
                hits: 2
              destroyCheck: 8
              threshold: 4
              cost:
                standard: 10
              specials:
              - "TAG"
              """;
        BattlefieldSupportAsset asset = assertInstanceOf(BattlefieldSupportAsset.class,
              new MekFileParser(yaml).getEntity());
        assertEquals("Content Asset", asset.getChassis());
        assertEquals(BFSAssetType.VEHICLE, asset.getAssetType());
        assertEquals(EntityMovementMode.TRACKED, asset.getMovementMode());
    }

    @Test
    void bfsContentDetection() {
        // A valid YAML mapping carrying assetType is a .bfs document.
        assertTrue(BFSFile.isBfsContent("chassis: \"A\"\nassetType: \"Vehicle\"\n"));
        // Valid YAML but no assetType key -> not a .bfs document.
        assertFalse(BFSFile.isBfsContent("chassis: \"A\"\nmodel: \"B\"\n"));
        // A .mtf-style block does not parse as a YAML mapping, even though it superficially uses key:value lines.
        assertFalse(BFSFile.isBfsContent("Version:1.0\nchassis:Atlas\nmodel:AS7-D\nConfig:Biped\nMass:100\n"));
        // Containing the literal assetType substring is not enough if the document is not valid YAML.
        assertFalse(BFSFile.isBfsContent("Version:1.0\nassetType:Vehicle\nMass:100\n"));
        assertFalse(BFSFile.isBfsContent(null));
        assertFalse(BFSFile.isBfsContent(""));
    }

    @Test
    void techBaseRoundTripsAllFourVariants() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();

        asset.setAssetTechBase(BattlefieldSupportAssetData.TECH_BASE_IS);
        assertFalse(asset.isClan());
        assertFalse(asset.isMixedTech());
        assertEquals(BattlefieldSupportAssetData.TECH_BASE_IS, asset.getAssetTechBase());

        asset.setAssetTechBase(BattlefieldSupportAssetData.TECH_BASE_CLAN);
        assertTrue(asset.isClan());
        assertFalse(asset.isMixedTech());
        assertEquals(BattlefieldSupportAssetData.TECH_BASE_CLAN, asset.getAssetTechBase());

        asset.setAssetTechBase(BattlefieldSupportAssetData.TECH_BASE_MIXED_IS);
        assertFalse(asset.isClan());
        assertTrue(asset.isMixedTech());
        assertEquals(BattlefieldSupportAssetData.TECH_BASE_MIXED_IS, asset.getAssetTechBase());

        asset.setAssetTechBase(BattlefieldSupportAssetData.TECH_BASE_MIXED_CLAN);
        assertTrue(asset.isClan());
        assertTrue(asset.isMixedTech());
        assertEquals(BattlefieldSupportAssetData.TECH_BASE_MIXED_CLAN, asset.getAssetTechBase());
    }

    @Test
    void constructionTechAdvancementIsCommonPsStandardRatingA() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
        var advancement = asset.getConstructionTechAdvancement();
        assertEquals(ITechnology.DATE_PS, advancement.getIntroductionDate(false));
        assertEquals(ITechnology.DATE_PS, advancement.getIntroductionDate(true));
        assertEquals(SimpleTechLevel.STANDARD, advancement.getStaticTechLevel());
        assertEquals(TechRating.A, advancement.getTechRating());
        for (Era era : Era.values()) {
            assertEquals(AvailabilityValue.A, advancement.getBaseAvailability(era),
                  "availability should be A in era " + era);
        }
    }
}

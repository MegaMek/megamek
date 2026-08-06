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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Vector;

import java.awt.Color;

import megamek.common.Player;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.battlefieldSupport.OverlayStyle;
import megamek.common.battlefieldSupport.StripeDirection;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.loaders.MULParser;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that a Battlefield Support Asset's persistent state - the current (damage-lowered) Destroy Check and the
 * Regular/Veteran crew grade - round-trips through the MUL/entity-XML used by save games, force files and MekHQ.
 */
class EntityListFileBattlefieldSupportAssetTest {

    private static final String ASSET_UUID = "0191b3e2-1a2b-7c3d-8e4f-1a2b3c4d5e6f";

    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
    }

    /** A Veteran-capable asset with an original Destroy Check of 7 and a Veteran crew, damaged to a current check. */
    private BattlefieldSupportAsset damagedVeteranAsset(int currentDestroyCheck) {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
        asset.setChassis("Test Asset");
        asset.setModel("TA-1");
        asset.setUnitFileUUID(ASSET_UUID);
        asset.setCost(10);
        asset.setVeteranCost(12); // gives it a Veteran profile
        asset.setODestroyCheck(7); // sets original and current to 7
        asset.setCrew(new Crew(asset.defaultCrewType()));
        asset.setVeteranCrew(true); // Gunnery 3
        asset.setDestroyCheck(currentDestroyCheck);
        asset.setGame(game);
        asset.setId(game.getNextEntityId());
        asset.setOwner(game.getPlayer(0));
        return asset;
    }

    private String toMul(BattlefieldSupportAsset asset, boolean embed) throws Exception {
        StringWriter writer = new StringWriter();
        ArrayList<Entity> list = new ArrayList<>();
        list.add(asset);
        EntityListFile.writeEntityList(writer, list, embed);
        return writer.toString();
    }

    @Test
    @DisplayName("a damaged asset writes its UUID, current Destroy Check and Veteran gunnery")
    void damagedAssetIsSerialized() throws Exception {
        String xml = toMul(damagedVeteranAsset(5), false);

        assertTrue(xml.contains(MULParser.ATTR_UNIT_FILE_UUID + "=\"" + ASSET_UUID + "\""),
              "MUL should include the asset's unit-file UUID: " + xml);
        assertTrue(xml.contains(MULParser.ATTR_ENTITY_FORM + "=\""
                    + MULParser.VALUE_BATTLEFIELD_SUPPORT_ASSET + "\""),
              "MUL should explicitly preserve the Battlefield Support Asset form: " + xml);
        assertTrue(xml.contains(MULParser.ATTR_DESTROY_CHECK + "=\"5\""),
              "MUL should include the current Destroy Check: " + xml);
        assertTrue(xml.contains(MULParser.ATTR_GUNNERY + "=\"3\""),
              "MUL should record the Veteran crew gunnery: " + xml);
    }

    @Test
    @DisplayName("an undamaged asset writes no Destroy Check attribute (but still its UUID)")
    void undamagedAssetWritesNoDestroyCheck() throws Exception {
        String xml = toMul(damagedVeteranAsset(7), false); // current == original == 7 -> undamaged

        assertFalse(xml.contains(MULParser.ATTR_DESTROY_CHECK + "=\""),
              "MUL should omit the Destroy Check attribute when the asset is undamaged: " + xml);
        assertTrue(xml.contains(MULParser.ATTR_UNIT_FILE_UUID + "=\"" + ASSET_UUID + "\""),
              "MUL should still include the asset's unit-file UUID: " + xml);
    }

    @Test
    @DisplayName("a damaged Veteran asset round-trips through the MUL (embedded, no cache needed)")
    void damagedAssetRoundTrips() throws Exception {
        String xml = toMul(damagedVeteranAsset(5), true);

        MULParser parser = new MULParser(
              new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), null);
        Vector<Entity> loaded = parser.getEntities();

        assertEquals(1, loaded.size(), "One entity should be parsed back");
        BattlefieldSupportAsset asset = assertInstanceOf(BattlefieldSupportAsset.class, loaded.get(0),
              "The loaded unit should be a Battlefield Support Asset");
        assertEquals(7, asset.getODestroyCheck(), "The as-constructed Destroy Check comes from the embedded .bfs");
        assertEquals(5, asset.getDestroyCheck(), "The current Destroy Check (damage) is restored from the MUL");
        assertTrue(asset.isVeteranCrew(), "The Veteran crew grade is restored from the MUL pilot gunnery");
    }

      @Test
      void staleBaseUuidFallsBackToSameNameAssetThroughMulParser() throws Exception {
            File assetFile = new File("testresources/data/mekfiles/Maxim Heavy Hover Transport.bfs");
            BattlefieldSupportAsset source = assertInstanceOf(BattlefieldSupportAsset.class,
                    new MekFileParser(assetFile).getEntity());
            source.setGame(game);
            source.setId(game.getNextEntityId());
            source.setOwner(game.getPlayer(0));

            MekSummary assetSummary = MekSummaryCache.getSummaryFromFile(assetFile);
            MekSummary sameNameBase = MekSummaryCache.getSummaryFromFile(
                    new File("testresources/data/mekfiles/Bulldog Medium Tank.blk"));
            assertTrue((assetSummary != null) && (sameNameBase != null));
            sameNameBase.setName(source.getShortNameRaw());
            sameNameBase.setChassis(source.getChassis());
            sameNameBase.setModel(source.getModel());
            sameNameBase.setUnitFileUUID("stale-base-uuid");

            MekSummaryCache testCache = newCache(assetSummary, sameNameBase);
            Field instanceField = MekSummaryCache.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            Object originalCache = instanceField.get(null);
            instanceField.set(null, testCache);
            try {
                  String xml = toMul(source, false).replace(source.getUnitFileUUID(), "stale-base-uuid");
                  MULParser parser = new MULParser(
                          new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), null);

                  BattlefieldSupportAsset loaded = assertInstanceOf(BattlefieldSupportAsset.class,
                          parser.getEntities().getFirst());
                  assertEquals(source.getChassis(), loaded.getChassis());
                  assertEquals(source.getUnitFileUUID(), loaded.getUnitFileUUID());
                  assertTrue(parser.getWarningMessage().contains("does not identify a Battlefield Support Asset"));
            } finally {
                  instanceField.set(null, originalCache);
            }
      }

      private static MekSummaryCache newCache(MekSummary... summaries) throws Exception {
            Constructor<MekSummaryCache> constructor = MekSummaryCache.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            MekSummaryCache cache = constructor.newInstance();

            Method updateData = MekSummaryCache.class.getDeclaredMethod("updateData", Vector.class);
            updateData.setAccessible(true);
            assertTrue((boolean) updateData.invoke(cache, new Vector<>(java.util.List.of(summaries))));

            Field initialized = MekSummaryCache.class.getDeclaredField("initialized");
            initialized.setAccessible(true);
            initialized.set(cache, true);
            return cache;
      }

      @Test
      void destroyCheckBoundsAreAccepted() throws Exception {
            assertEquals(0, parseWithDestroyCheck(0).getDestroyCheck());
            assertEquals(7, parseWithDestroyCheck(7).getDestroyCheck());
      }

      @Test
      void destroyCheckBelowZeroIsRejected() throws Exception {
            ParseResult result = parseWithInvalidDestroyCheck(-1);

            assertEquals(7, result.asset().getDestroyCheck());
            assertTrue(result.warning().contains("expected 0..7"));
      }

      @Test
      void destroyCheckAboveOriginalIsRejected() throws Exception {
            ParseResult result = parseWithInvalidDestroyCheck(8);

            assertEquals(7, result.asset().getDestroyCheck());
            assertTrue(result.warning().contains("expected 0..7"));
      }

      private BattlefieldSupportAsset parseWithDestroyCheck(int destroyCheck) throws Exception {
            return parseMul(toMul(damagedVeteranAsset(destroyCheck), true)).asset();
      }

      private ParseResult parseWithInvalidDestroyCheck(int destroyCheck) throws Exception {
            String xml = toMul(damagedVeteranAsset(7), true).replace(
                    MULParser.ATTR_ENTITY_FORM + "=\"" + MULParser.VALUE_BATTLEFIELD_SUPPORT_ASSET + "\"",
                    MULParser.ATTR_ENTITY_FORM + "=\"" + MULParser.VALUE_BATTLEFIELD_SUPPORT_ASSET + "\" "
                              + MULParser.ATTR_DESTROY_CHECK + "=\"" + destroyCheck + "\"");
            return parseMul(xml);
      }

      private ParseResult parseMul(String xml) throws Exception {
            MULParser parser = new MULParser(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), null);
            BattlefieldSupportAsset asset = assertInstanceOf(BattlefieldSupportAsset.class, parser.getEntities().get(0));
            return new ParseResult(asset, parser.hasWarningMessage() ? parser.getWarningMessage() : "");
      }

      private record ParseResult(BattlefieldSupportAsset asset, String warning) { }

    @Test
    @DisplayName("a non-default asset marker overlay is written to the MUL")
    void nonDefaultOverlayIsSerialized() throws Exception {
        BattlefieldSupportAsset asset = damagedVeteranAsset(7);
        asset.getCamouflage().setOverlayStyle(OverlayStyle.HAZARD);
        asset.getCamouflage().setOverlayDirection(StripeDirection.VERTICAL);
        asset.getCamouflage().setOverlayColor(new Color(0x3366CC));

        String xml = toMul(asset, false);

        assertTrue(xml.contains(MULParser.ATTR_CAMO_OVERLAY_STYLE + "=\"HAZARD\""),
              "MUL should include the overlay style: " + xml);
        assertTrue(xml.contains(MULParser.ATTR_CAMO_OVERLAY_DIRECTION + "=\"VERTICAL\""),
              "MUL should include the overlay direction: " + xml);
        assertTrue(xml.contains(MULParser.ATTR_CAMO_OVERLAY_COLOR + "=\"3366CC\""),
              "MUL should include the overlay color as a hex RGB string: " + xml);
    }

    @Test
    @DisplayName("a default asset marker overlay writes no overlay attributes")
    void defaultOverlayWritesNothing() throws Exception {
        String xml = toMul(damagedVeteranAsset(7), false); // camo left at defaults

        assertFalse(xml.contains(MULParser.ATTR_CAMO_OVERLAY_STYLE + "=\""),
              "MUL should omit overlay attributes when the overlay is at its defaults: " + xml);
    }

    @Test
    @DisplayName("a non-default asset marker overlay round-trips through the MUL")
    void overlayRoundTrips() throws Exception {
        BattlefieldSupportAsset source = damagedVeteranAsset(5);
        source.getCamouflage().setOverlayStyle(OverlayStyle.NONE);
        source.getCamouflage().setOverlayDirection(StripeDirection.ANTI_DIAGONAL);
        source.getCamouflage().setOverlayColor(new Color(0x00FF80));

        String xml = toMul(source, true);

        MULParser parser = new MULParser(
              new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), null);
        Vector<Entity> loaded = parser.getEntities();

        assertEquals(1, loaded.size(), "One entity should be parsed back");
        Entity asset = loaded.get(0);
        assertEquals(OverlayStyle.NONE, asset.getCamouflage().getOverlayStyle(),
              "The overlay style should be restored from the MUL");
        assertEquals(StripeDirection.ANTI_DIAGONAL, asset.getCamouflage().getOverlayDirection(),
              "The overlay direction should be restored from the MUL");
        assertEquals(new Color(0x00FF80), asset.getCamouflage().getOverlayColor(),
              "The overlay color should be restored from the MUL");
    }
}

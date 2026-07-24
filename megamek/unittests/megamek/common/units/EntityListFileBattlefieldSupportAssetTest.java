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
import java.io.StringWriter;
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

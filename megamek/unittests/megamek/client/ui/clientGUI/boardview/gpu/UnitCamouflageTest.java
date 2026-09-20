/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import megamek.client.ui.tileset.EntityImage;
import megamek.client.ui.util.PlayerColour;
import megamek.common.Player;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.battlefieldSupport.OverlayStyle;
import megamek.common.battlefieldSupport.StripeDirection;
import megamek.common.force.Force;
import megamek.common.game.Game;
import megamek.common.icons.Camouflage;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

class UnitCamouflageTest {
    @Test
    void existingUnitForceAndOwnerPrecedenceIsPreserved() {
        var game = new Game();
        var owner = new Player(1, "Camo owner");
        owner.setCamouflage(Camouflage.of(PlayerColour.BLUE));
        game.addPlayer(owner.getId(), owner);
        var mek = new BipedMek();
        mek.setId(1);
        mek.setOwner(owner);
        game.addEntity(mek, false);
        assertEquals(PlayerColour.BLUE.getHex(), UnitModelState.capture(mek).appearance().camo().rgb());
        var force = Force.createToplevelForce("Camo force", owner);
        force.setCamouflage(Camouflage.of(PlayerColour.GREEN));
        int forceId = game.getForces().addTopLevelForce(force, owner);
        game.getForces().addEntity(mek, forceId);
        assertEquals(PlayerColour.GREEN.getHex(), UnitModelState.capture(mek).appearance().camo().rgb());
        mek.setCamouflage(Camouflage.of(PlayerColour.RED));
        assertEquals(PlayerColour.RED.getHex(), UnitModelState.capture(mek).appearance().camo().rgb());
    }

    @Test
    void imagePixelsAreSharedAcrossTransformsButReleasedWhenNoLongerVisible() {
        var source = new UnitCamouflage();
        var mek = new BipedMek();
        var icon = new Camouflage("Word of Blake/", "TerraSec (Camo).png");
        mek.setCamouflage(icon);
        source.begin();
        var before = source.resolve(selection(mek));
        var pixels = before.state().appearance().camo().image();
        assertNotNull(pixels);
        assertEquals(BoardScene.Pixels.copy(icon.getImage()), pixels);
        icon.setRotationAngle(90);
        icon.setScale(20);
        var after = source.resolve(selection(mek));
        assertSame(pixels, after.state().appearance().camo().image());
        assertEquals(before.state().structure(), after.state().structure());
        assertEquals(0, before.state().appearance().camo().rotation());
        assertEquals(90, after.state().appearance().camo().rotation());
        assertEquals(20, after.state().appearance().camo().scale());
        source.retain();
        source.begin();
        assertNull(source.resolve(null), "Sensor contacts have no model selection to resolve");
        source.retain();
        assertNotSame(pixels, source.resolve(selection(mek)).state().appearance().camo().image());
        source.clear();
    }

    @Test
    void missingImageUsesTheExistingFallbackAndOnlySupportAssetsReceiveMarkers() {
        var source = new UnitCamouflage();
        var icon = new Camouflage("missing-review-category/", "missing-review-image.png");
        icon.setOverlayDirection(StripeDirection.HORIZONTAL);
        icon.setOverlayStyle(OverlayStyle.HAZARD);
        var mek = new BipedMek();
        mek.setCamouflage(icon);
        var paint = source.resolve(selection(mek)).state().appearance().camo();
        assertEquals(BoardScene.Pixels.copy(icon.getImage()), paint.image());
        assertNull(paint.marker());
        var asset = new BattlefieldSupportAsset();
        asset.setCamouflage(icon);
        var marker = source.resolve(selection(asset)).state().appearance().camo().marker();
        assertNotNull(marker.image());
        for (int y = 0; y < 72; y++) {
            for (int x = 0; x < 84; x++) {
                int argb = EntityImage.markerPixel(x, y, icon.getOverlayColor().getRGB(),
                      icon.getOverlayDirection(), icon.getOverlayStyle());
                assertEquals((argb << 8) | (argb >>> 24), marker.image().rgba(y * 84 + x));
            }
        }
        icon.setOverlayStyle(OverlayStyle.NONE);
        assertNull(source.resolve(selection(asset)).state().appearance().camo().marker());
        source.clear();
    }

    static BoardScene.UnitModel selection(Entity entity) {
        return new BoardScene.UnitModel("units/modular/meks/warhammer.json", null, "camo-review", 1,
              0, BoardScene.LocationDamage.NONE, UnitModelState.capture(entity));
    }
}

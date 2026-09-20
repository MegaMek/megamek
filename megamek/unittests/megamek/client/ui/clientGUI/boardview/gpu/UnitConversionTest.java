/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Vector;
import javax.swing.SwingUtilities;

import megamek.client.ui.tileset.MekTileset;
import megamek.common.Configuration;
import megamek.common.board.Coords;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.common.units.QuadVee;
import megamek.common.units.UnitLocation;
import org.junit.jupiter.api.Test;

class UnitConversionTest {
    @Test
    void sparseMovementPacketsKeepGroundAndAeroFormsOnTheirOwnLegs() throws Exception {
        try (var fixture = GpuBoardFixture.create()) {
            var entity = new megamek.common.loaders.MekFileParser(
                  new java.io.File("testresources/megamek/common/units/Shadow Hawk LAM SHD-X2.mtf")).getEntity();
            entity.setId(61);
            entity.setWeight(50);
            entity.setOwner(fixture.player);
            entity.setDeployed(true);
            entity.setPosition(new Coords(5, 5));
            for (int loc = 0; loc < entity.locations(); loc++) { entity.initializeInternal(10, loc); }
            var ground = UnitLocation.Form.capture(entity);
            SwingUtilities.invokeAndWait(() -> {
                fixture.game.addEntity(entity, false);
                fixture.source.refresh();
            });
            fixture.source.takeFrame();
            SwingUtilities.invokeAndWait(() -> {
                entity.setConversionMode(LandAirMek.CONV_MODE_FIGHTER);
                var fighter = UnitLocation.Form.capture(entity);
                var path = new Vector<>(List.of(new UnitLocation(61, new Coords(5, 5), 0, 0, 0, null, ground),
                      new UnitLocation(61, new Coords(6, 5), 0, 0, 0),
                      new UnitLocation(61, new Coords(6, 5), 0, 0, 0, null, fighter),
                      new UnitLocation(61, new Coords(6, 4), 0, 0, 0)));
                entity.setPosition(new Coords(6, 4));
                fixture.game.fireGameEvent(new GameEntityChangeEvent(fixture.game, entity, path));
            });
            var events = fixture.source.takeFrame().animations();
            assertEquals(3, events.size());
            var walk = (BoardScene.Movement) events.get(0);
            var conversion = (BoardScene.Conversion) events.get(1);
            var taxi = (BoardScene.Movement) events.get(2);
            walk.path().forEach(point -> {
                assertEquals(ground, point.form());
                assertNull(point.aeroState(), "The final aircraft mode must not leak into the earlier walking leg");
            });
            taxi.path().forEach(point -> assertEquals(BoardScene.AeroState.LANDED, point.aeroState()));
            assertEquals(walk.path().getLast(), conversion.before().location());
            assertEquals(taxi.path().getFirst(), conversion.after().location());
            assertNotEquals(walk.unit().model().fallback(), taxi.unit().model().fallback());
        }
    }

    @Test
    void observedFormsUseTheTilesetWithoutChangingTheCurrentGameEntity() throws Exception {
        var tileset = new MekTileset(Configuration.unitImagesDir());
        tileset.loadFromFile("mekset.txt");
        for (Mek entity : List.of(new LandAirMek(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD, LandAirMek.LAM_STANDARD), new QuadVee())) {
            entity.setId(61);
            for (int location = 0; location < entity.locations(); location++) { entity.initializeInternal(10, location); }
            var before = UnitModelSelection.capture(entity, -1, false, tileset);
            var form = UnitLocation.Form.capture(entity);
            entity.setConversionMode(entity instanceof LandAirMek ? LandAirMek.CONV_MODE_FIGHTER : QuadVee.CONV_MODE_VEHICLE);
            var after = UnitModelSelection.capture(entity, -1, false, tileset);
            var observed = UnitModelSelection.inForm(after, entity, -1, tileset, form);
            assertEquals(before.asset(), observed.asset());
            assertEquals(before.fallback(), observed.fallback());
            assertSame(after.state().appearance(), observed.state().appearance());
            assertEquals(form.movement(), observed.state().structure().movement());
            assertEquals(after.state().pose().form(), UnitLocation.Form.capture(entity));
            if (entity instanceof QuadVee) { assertEquals(before.fallback(), after.fallback()); }
            else { assertNotEquals(before.fallback(), after.fallback()); }
            var location = new UnitLocation(61, new Coords(2, 2), 0, 0, 0, null, form);
            var bytes = new ByteArrayOutputStream();
            try (var out = new ObjectOutputStream(bytes)) { out.writeObject(location); }
            try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                assertEquals(location, in.readObject());
            }
            assertNull(new UnitLocation(61, new Coords(2, 2), 0, 0, 0).form());
        }
    }

    @Test
    void conversionKeepsItsCapturedFormThenDeploysOnTheSharedClockAtEverySpeed() {
        var before = UnitPlaybackTest.unit(61, 0);
        var after = UnitPlaybackTest.unit(61, 1);
        var change = new BoardScene.Conversion(0, before, after);
        var latest = UnitPlaybackTest.scene(after);
        for (var speed : List.of(UnitMotion.Speed.HALF, UnitMotion.Speed.NORMAL, UnitMotion.Speed.DOUBLE, UnitMotion.Speed.QUADRUPLE)) {
            var playback = new UnitPlayback();
            playback.accept(List.of(new BoardScene.SceneUpdate(UnitPlaybackTest.scene(before)), change,
                  new BoardScene.SceneUpdate(latest)), latest, ignored -> false);
            playback.advance(.39 / speed.rate, speed);
            assertSame(before, playback.present(latest).units().getFirst());
            playback.togglePaused();
            float time = playback.conversion().seconds;
            playback.advance(5, speed);
            assertEquals(time, playback.conversion().seconds);
            playback.togglePaused();
            playback.advance(.02 / speed.rate, speed);
            assertSame(after, playback.present(latest).units().getFirst());
            playback.advance((UnitConversion.DURATION_SECONDS - playback.conversion().seconds) / speed.rate, speed);
            assertEquals(0, playback.conversion().fold(), .00001);
            assertEquals(1, playback.holdSeconds(), .00001);
            assertSame(after, playback.present(latest).units().getFirst());
            playback.advance(1, speed);
            assertFalse(playback.busy());
            playback.accept(List.of(change), latest, ignored -> false);
            playback.advance(.1, speed);
            playback.advance(0, UnitMotion.Speed.INSTANT);
            assertSame(after, playback.present(latest).units().getFirst());
            assertNull(playback.conversion());
            playback.accept(List.of(change), latest, ignored -> false);
            playback.clear();
            playback.advance(5, speed);
            assertFalse(playback.busy());
            assertNull(playback.conversion());
        }
    }
}

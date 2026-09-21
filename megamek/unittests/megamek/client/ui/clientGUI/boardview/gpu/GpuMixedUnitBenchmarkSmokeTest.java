/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.common.Hex;
import megamek.common.ResolvedAttack;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.icons.Camouflage;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.EntityMovementType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Full production scene probe. Timings include submission + GPU completion, not window/vsync waiting. */
@Tag("on-demand")
class GpuMixedUnitBenchmarkSmokeTest {
    private static final List<String> DESIGNS = List.of("Atlas AS7-D.mtf", "Barghest BGS-1T.mtf", "Triskelion TRK-4V.mtf",
          "Elemental BA [Laser] (Sqd5).blk", "Foot Platoon (AFFS) (Laser 3067+).blk", "Bulldog Medium Tank.blk",
          "Kanga Medium Hovertank.blk", "Cobra Transport VTOL.blk", "Chippewa CHP-W7.blk", "Union (3055).blk");

    @Test
    void measuresDenseMixedBoardsAndReleasesEveryLibraryOnClose() throws Exception {
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        Files.createDirectories(output.toPath());
        boolean battalion = "battalion".equals(System.getProperty("megamek.gpu.benchmarkMix"));
        boolean compareLod = Boolean.getBoolean("megamek.gpu.compareLod");
        int views = battalion ? 4 : 2;
        int[] counts = battalion ? new int[] { 72, 144 } : new int[] { 64, 256, 512 };
        StringBuilder stages = new StringBuilder("CPU submission and asynchronous GPU timestamp intervals, milliseconds.\n")
              .append("GPU intervals include command-stream idle time; CPU and GPU values overlap and must not be added.\n")
              .append("The existing whole-frame glFinish remains outside every measured stage; query reads never wait.\n");
        StringBuilder report = new StringBuilder("Mixed production board, terrain/shadows/camouflage/poses/effects enabled.\n")
              .append("1280x900; 45 warmup + 64 samples per view; submission + glFinish; GLProfiler outside timing.\n")
              .append("Allocation is render-thread Java allocation; heap is process-wide, not retained GPU memory.\n")
              .append("The Swing capture timer is stopped after the fixture snapshot; source changes are explicit.\n")
              .append("Warmup includes travel; measured frames repeatedly fire the first unit's actual loadout, bypassing completion holds.\n")
              .append("Camera framing is disabled for repeatable top/isometric views; preview time is fixed at 13:00.\n")
              .append("units,view,distance,equipment,drawCalls,shaderSwitches,medianMs,p95Ms,p99Ms,KiBperFrame,heapMiB,firstFrameMs\n");
        for (int count : counts) {
            AtomicReference<Throwable> failure = new AtomicReference<>();
            try (var fixture = GpuBoardFixture.create(board())) {
                var capturedWeapons = new java.util.HashMap<Integer, List<ResolvedAttack>>();
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        for (int i = 1; i < count; i++) {
                            int design = i % DESIGNS.size();
                            if (battalion) {
                                int slot = i % 72;
                                design = slot < 36 ? slot % 3 : slot < 54 ? 5 + slot % 2
                                      : slot < 66 ? 3 + slot % 2 : slot < 70 ? 7 + slot % 2 : slot == 70 ? 9 : 4;
                            }
                            var entity = new MekFileParser(new File("testresources/megamek/common/units", DESIGNS.get(design))).getEntity();
                            entity.setId(i + 1);
                            entity.setOwner(fixture.player);
                            entity.setPosition(battalion ? new Coords(2 + i % 12 * 2, 3 + i / 12 * 2) : new Coords(1 + i % 30, 1 + i / 30));
                            entity.setFacing(i % 6);
                            entity.setDeployed(true);
                            var camo = new Camouflage("Word of Blake/", "TerraSec (Camo).png");
                            camo.setRotationAngle(i % 6 * 60);
                            entity.setCamouflage(camo);
                            fixture.game.addEntity(entity, false);
                        }
                        for (var entity : fixture.game.getEntitiesVector()) {
                            var location = new megamek.common.units.UnitLocation(entity.getId(), entity.getPosition(), entity.getFacing(), 0, 0);
                            capturedWeapons.put(entity.getId(), entity.getWeaponList().stream().map(gun -> new ResolvedAttack(
                                  java.util.UUID.randomUUID(), ResolvedAttack.Kind.SHOT, location, location,
                                  megamek.common.units.Targetable.TYPE_ENTITY, gun.getEquipmentNum(), gun.getType().getInternalName(),
                                  gun.getLocation(), true, ResolvedAttack.captureMounts(entity, gun.getEquipmentNum()),
                                  ResolvedAttack.Shot.capture(gun))).toList());
                        }
                        fixture.source.refresh();
                        ((Timer) field(fixture.source, "timer")).stop();
                    } catch (Exception error) { throw new IllegalStateException(error); }
                });
                var config = GpuBoardWindow.configuration(false);
                config.setWindowedMode(1280, 900);
                config.useVsync(false);
                config.setForegroundFPS(0);
                new Lwjgl3Application(new GpuBattleView(fixture.source) {
                    final double[] samples = new double[64];
                    final com.sun.management.ThreadMXBean allocation = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
                    GLProfiler profiler;
                    GpuStageTimings timings;
                    long allocated;
                    double first;
                    int frame;
                    List<BoardScene.Animation> firing = List.of();

                    @Override
                    boolean preparePlaybackCamera(UnitPlayback state, BoardScene scene) { return true; }

                    @Override
                    void updateCameraFocus(BoardScene scene, BoardView.CenterRequest request, BoardScene.Animation action) {
                        if (frame == 0) { super.updateCameraFocus(scene, request, action); }
                    }

                    @Override
                    void updateEquipmentDetail() {
                        if (compareLod && frame / 111 < views) {
                            try {
                                for (Object value : ((Map<?, ?>) field(this, "unitInstances")).values()) {
                                    if (value instanceof GpuUnitInstance instance) { instance.equipmentDetail(boardCamera.camera, true); }
                                }
                            } catch (Exception error) { throw new IllegalStateException(error); }
                        } else {
                            super.updateEquipmentDetail();
                        }
                    }

                    @Override
                    public void create() {
                        super.create();
                        profiler = new GLProfiler(Gdx.graphics);
                        timings = new GpuStageTimings();
                        allocation.setThreadAllocatedMemoryEnabled(true);
                        if (count == counts[0]) {
                            report.insert(0, "Renderer: " + Gdx.gl.glGetString(GL20.GL_RENDERER) + "\nGL: "
                                  + Gdx.gl.glGetString(GL20.GL_VERSION) + "\nCPU: " + System.getenv("PROCESSOR_IDENTIFIER") + "\n");
                        }
                    }

                    @Override
                    void renderStage(String stage) {
                        if (frame % 111 >= 45 && frame % 111 < 109) { timings.stage(stage); }
                    }

                    @Override
                    public void render() {
                        try {
                            int phase = frame % 111;
                            int view = frame / 111;
                            if (phase >= 44 && !firing.isEmpty()) {
                                var playback = (UnitPlayback) field(this, "playback");
                                if (phase == 44 || playback.holdSeconds() > 0 || !playback.busy()) {
                                    playback.clear();
                                    playback.accept(firing, (BoardScene) field(this, "scene"), unit -> false);
                                }
                            }
                            if (phase == 110) { profiler.reset(); profiler.enable(); }
                            if (phase >= 45 && phase < 109) { timings.beginFrame(); }
                            long bytes = allocation.getThreadAllocatedBytes(Thread.currentThread().threadId());
                            long start = System.nanoTime();
                            super.render();
                            Gdx.gl.glFinish();
                            double elapsed = (System.nanoTime() - start) / 1e6;
                            if (frame == 0) { first = elapsed; }
                            if (phase >= 45 && phase < 109) {
                                samples[phase - 45] = elapsed;
                                allocated += allocation.getThreadAllocatedBytes(Thread.currentThread().threadId()) - bytes;
                            }
                            if (phase == 1) {
                                ((Slider) GpuBoardTestUi.stage().getRoot().findActor("Time of day")).setValue(13);
                                var scene = (BoardScene) field(this, "scene");
                                assertEquals(count, scene.units().size());
                                boardCamera.setIsometric(battalion ? view % views >= 2 : view % views == 1);
                                boardCamera.fit(scene);
                                if (battalion && view % 2 == 1) { boardCamera.zoom(.45f); }
                                var playback = (UnitPlayback) field(this, "playback");
                                playback.clear();
                                List<BoardScene.Animation> events = new ArrayList<>();
                                for (int i = 0; i < 8; i++) {
                                    var unit = scene.units().get(i);
                                    events.add(new BoardScene.Movement(unit.id(), 0, List.of(
                                          new BoardScene.Waypoint(unit.location().coords().translated(3, 2), unit.location().elevation(), 0),
                                          unit.location()), EntityMovementType.MOVE_WALK, 0, 4, unit));
                                    var target = scene.units().get(i + 8);
                                    for (var result : capturedWeapons.getOrDefault(unit.id(), List.of())) {
                                        var destination = new megamek.common.units.UnitLocation(target.id(), target.location().coords(), Math.round(target.location().facing() / 60), 0, 0);
                                        var shot = new ResolvedAttack(result.id(), result.kind(), result.attacker(), destination, result.targetType(),
                                              result.equipmentIndex(), result.equipmentName(), result.limb(), result.hit(), result.mounts(), result.shot());
                                        events.add(new BoardScene.Combat(shot, unit, target, target.location()));
                                    }
                                }
                                playback.accept(events, scene, unit -> false);
                                firing = events.stream().filter(event -> event instanceof BoardScene.Combat).toList();
                            }
                            if (phase == 110) {
                                assertEquals(battalion ? view % views >= 2 : view % views == 1, boardCamera.isIsometric(),
                                      "Combat playback must not change the benchmark camera");
                                int draws = profiler.getDrawCalls(), switches = profiler.getShaderSwitches();
                                profiler.disable();
                                timings.appendReport(stages, count + " units, " + (boardCamera.isIsometric() ? "isometric" : "top")
                                      + ", view " + view);
                                Arrays.sort(samples);
                                report.append(String.format(Locale.ROOT, "%d,%s,%s,%s,%d,%d,%.3f,%.3f,%.3f,%.1f,%.1f,%.1f%n",
                                      count, boardCamera.isIsometric() ? "isometric" : "top", battalion && view % 2 == 1 ? "close" : "far",
                                      compareLod && view < views ? "full" : "lod", draws, switches,
                                      samples[31], samples[60], samples[63], allocated / (64.0 * 1024),
                                      (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576.0, first));
                                GpuBoardTestUi.capture(new File(output, "mixed-" + count + "-" + view + ".png"));
                                allocated = 0;
                                if (view == views * (compareLod ? 2 : 1) - 1) { Gdx.app.exit(); }
                            }
                            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                            frame++;
                        } catch (Throwable error) {
                            failure.set(error);
                            profiler.disable();
                            Gdx.app.exit();
                        }
                    }

                    @Override
                    public void dispose() {
                        try {
                            var library = field(this, "unitModels");
                            timings.close();
                            super.dispose();
                            for (String cache : List.of("assemblies", "modular", "models", "descriptors", "failed")) {
                                Object value = field(library, cache);
                                assertTrue(value instanceof Map<?, ?> map ? map.isEmpty() : ((java.util.Set<?>) value).isEmpty(), cache);
                            }
                        } catch (Throwable error) { failure.compareAndSet(null, error); }
                    }
                }, config);
            }
            assertNull(failure.get(), () -> String.valueOf(failure.get()));
            Files.writeString(output.toPath().resolve((compareLod ? "equipment-lod" : battalion ? "battalion" : "mixed-unit")
                  + "-board-benchmark.txt"), report);
            Files.writeString(output.toPath().resolve("mixed-unit-stage-timing.txt"), stages);
        }
        System.out.print(report);
    }

    static Object field(Object object, String name) throws Exception {
        Class<?> type = object.getClass();
        while (type != null) {
            try {
                var field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException ignored) { type = type.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }

    private static Board board() {
        var sample = new Board();
        sample.load(new File("data/boards/AGoAC Maps/16x17 Grassland 2.board"));
        Hex[] hexes = new Hex[32 * 34];
        for (int y = 0; y < 34; y++) {
            for (int x = 0; x < 32; x++) { hexes[y * 32 + x] = sample.getHex(x % 16, y % 17).duplicate(); }
        }
        return new Board(32, 34, hexes);
    }
}

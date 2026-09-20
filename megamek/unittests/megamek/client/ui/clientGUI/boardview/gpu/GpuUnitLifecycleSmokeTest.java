/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static megamek.client.ui.clientGUI.boardview.gpu.GpuMixedUnitBenchmarkSmokeTest.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Real scene ownership through refits, reinforcements, conceal/reveal, terrain replacement and window disposal. */
@Tag("on-demand")
class GpuUnitLifecycleSmokeTest {
    @Test
    void resourcesFollowVisibleUnitsAndIdleFramesDoNotUploadTextures() throws Exception {
        for (int cycle = 0; cycle < 2; cycle++) {
            var failure = new AtomicReference<Throwable>();
            try (var fixture = GpuBoardFixture.create()) {
                SwingUtilities.invokeAndWait(() -> {
                    try { ((Timer) field(fixture.source, "timer")).stop(); }
                    catch (Exception error) { throw new IllegalStateException(error); }
                });
                new Lwjgl3Application(new GpuBattleView(fixture.source) {
                    final AtomicInteger uploads = new AtomicInteger();
                    GL20 real;
                    final java.util.Set<Integer> textures = new java.util.HashSet<>();
                    final java.util.Set<Integer> buffers = new java.util.HashSet<>();
                    int peakTextures, peakBuffers;
                    int frame;
                    int assetCount;

                    @Override
                    public void create() {
                        real = Gdx.gl;
                        GL20 watched = (GL20) Proxy.newProxyInstance(GL20.class.getClassLoader(), new Class<?>[] { GL20.class },
                              (proxy, method, args) -> {
                                  if (method.getName().contains("TexImage") || method.getName().contains("TexSubImage")) { uploads.incrementAndGet(); }
                                  try {
                                      Object value = method.invoke(real, args);
                                      switch (method.getName()) {
                                          case "glGenTexture" -> textures.add((Integer) value);
                                          case "glGenBuffer" -> buffers.add((Integer) value);
                                          case "glDeleteTexture" -> textures.remove((Integer) args[0]);
                                          case "glDeleteBuffer" -> buffers.remove((Integer) args[0]);
                                          default -> { }
                                      }
                                      peakTextures = Math.max(peakTextures, textures.size());
                                      peakBuffers = Math.max(peakBuffers, buffers.size());
                                      return value;
                                  }
                                  catch (InvocationTargetException error) { throw error.getCause(); }
                              });
                        Gdx.graphics.setGL20(watched);
                        Gdx.gl = Gdx.gl20 = watched;
                        super.create();
                    }

                    @Override
                    public void render() {
                        try {
                            if (frame == 4 || frame == 16) {
                                SwingUtilities.invokeAndWait(() -> {
                                    try {
                                        int id = 2;
                                        for (String file : List.of("Elemental BA [Laser] (Sqd5).blk", "Foot Platoon (AFFS) (Laser 3067+).blk",
                                              "Bulldog Medium Tank.blk", "Chippewa CHP-W7.blk")) {
                                            var entity = new MekFileParser(new File("testresources/megamek/common/units", file)).getEntity();
                                            entity.setId(id++);
                                            entity.setPosition(new Coords(id, 6));
                                            entity.setOwner(fixture.player);
                                            entity.setDeployed(true);
                                            fixture.game.addEntity(entity, false);
                                        }
                                        fixture.source.refresh();
                                    } catch (Exception error) { throw new IllegalStateException(error); }
                                });
                            }
                            if (frame == 8) {
                                SwingUtilities.invokeAndWait(() -> {
                                    try { fixture.entity.addEquipment(EquipmentType.get("ISSmallLaser"), Mek.LOC_LEFT_ARM); }
                                    catch (Exception error) { throw new IllegalStateException(error); }
                                    fixture.source.refresh();
                                });
                            }
                            if (frame == 12 || frame == 32) {
                                SwingUtilities.invokeAndWait(() -> {
                                    for (var entity : List.copyOf(fixture.game.getEntitiesVector())) {
                                        if (frame == 32 || entity.getId() != 1) {
                                            fixture.game.removeEntity(entity.getId(), IEntityRemovalConditions.REMOVE_UNKNOWN);
                                        }
                                    }
                                    fixture.source.refresh();
                                });
                            }
                            if (frame == 22) {
                                SwingUtilities.invokeAndWait(() -> {
                                    fixture.game.setBoard(new Board(8, 8, java.util.stream.Stream.generate(Hex::new).limit(64).toArray(Hex[]::new)));
                                    fixture.source.refresh();
                                });
                            }
                            if (frame == 26) { boardCamera.setIsometric(true); }
                            uploads.set(0);
                            super.render();
                            var library = field(this, "unitModels");
                            var assemblies = (Map<?, ?>) field(library, "assemblies");
                            if (frame == 10) {
                                assertEquals(5, assemblies.size());
                                assetCount = ((Map<?, ?>) field(library, "modular")).size();
                            }
                            if (frame == 14) { assertEquals(1, assemblies.size()); }
                            if (frame == 20) {
                                assertEquals(5, assemblies.size());
                                assertEquals(assetCount, ((Map<?, ?>) field(library, "modular")).size(), "Reveals reuse the asset buffers");
                            }
                            if (List.of(3, 7, 11, 15, 21, 25, 29, 35).contains(frame)) {
                                assertEquals(0, uploads.get(), "Unchanged textures must not upload every frame");
                            }
                            if (frame == 36) {
                                assertTrue(assemblies.isEmpty());
                                assertTrue(((Map<?, ?>) field(this, "unitInstances")).isEmpty());
                                assertEquals(0, ((GpuUnitCamouflage) field(this, "camouflage")).textureCount());
                                Gdx.app.exit();
                            }
                            assertEquals(GL20.GL_NO_ERROR, real.glGetError());
                            frame++;
                        } catch (Throwable error) { failure.set(error); Gdx.app.exit(); }
                    }

                    @Override
                    public void dispose() {
                        super.dispose();
                        Gdx.graphics.setGL20(real);
                        Gdx.gl = Gdx.gl20 = real;
                        try {
                            assertTrue(peakTextures > 0 && peakBuffers > 0, "The GL resource audit must observe native allocations");
                            assertTrue(textures.isEmpty(), "Undisposed textures: " + textures);
                            assertTrue(buffers.isEmpty(), "Undisposed buffers: " + buffers);
                            System.out.println("View resource peak: " + peakTextures + " textures, " + peakBuffers + " buffers; all released.");
                            for (String cache : List.of("unitInstances", "animators", "unitDamage", "unitTints")) {
                                assertTrue(((Map<?, ?>) field(this, cache)).isEmpty(), cache);
                            }
                        } catch (Throwable error) { failure.compareAndSet(null, error); }
                    }
                }, GpuBoardWindow.configuration(false));
            }
            assertNull(failure.get(), () -> String.valueOf(failure.get()));
        }
    }
}

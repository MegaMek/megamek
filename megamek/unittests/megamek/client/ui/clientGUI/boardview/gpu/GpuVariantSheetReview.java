/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipFile;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import megamek.client.ui.tileset.MekTileset;
import megamek.common.Configuration;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Mek;

/**
 * Every variant of one chassis on a single sheet, assembled from its real loadout.
 *
 * <p>This is the comparison view: it shows what the shared body looks like under every loadout the
 * game will actually hang on it, so one variant can be picked out for a closer render.</p>
 */
final class GpuVariantSheetReview {
    private static final int COLUMNS = 8;
    private static final int CELL = 250;
    private static final int CAPTION = 48;
    private static final int HEADER = 112;
    /** One framing for every cell, so the variants stay comparable rather than each filling its box. */
    private static final float FRAME = 82;

    private GpuVariantSheetReview() { }

    private record Variant(String model, ModelInstance instance, int triangles) { }

    static void render(GpuUnitModels library, ModelBatch batch, MekTileset tileset, String chassis, String asset)
          throws Exception {
        List<Variant> variants = collect(library, tileset, chassis, asset);
        if (variants.isEmpty()) {
            return;
        }
        int rows = (variants.size() + COLUMNS - 1) / COLUMNS;
        int width = COLUMNS * CELL;
        int height = HEADER + rows * (CELL + CAPTION);
        var environment = new Environment();
        environment.set(ColorAttribute.createAmbientLight(.7f, .7f, .7f, 1));
        environment.add(new DirectionalLight().set(.8f, .8f, .8f, -.4f, -.7f, -1));
        environment.add(new DirectionalLight().set(.38f, .38f, .38f, .5f, .8f, -.35f));
        // Framed to the chassis: an assault Mek fills the full frame, and a light one is drawn closer rather than
        // shrinking to a speck in the corner of its cell. The weight lineup is where sizes are compared.
        var bounds = variants.getFirst().instance().calculateBoundingBox(new com.badlogic.gdx.math.collision.BoundingBox());
        float frame = Math.min(FRAME, Math.max(bounds.getHeight(), bounds.getWidth()) * 2.3f);
        var camera = new OrthographicCamera(frame, frame);
        camera.near = 1;
        camera.far = 1000;
        camera.position.set(170, 210, 115);
        camera.up.set(Vector3.Z);
        camera.lookAt(0, 0, Math.min(25, bounds.getHeight() * .6f));
        camera.update();

        var target = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, true);
        target.begin();
        try {
            Gdx.gl.glViewport(0, 0, width, height);
            Gdx.gl.glClearColor(.47f, .37f, .33f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
            for (int index = 0; index < variants.size(); index++) {
                // Cells run left to right from the top, under the heading band.
                int left = (index % COLUMNS) * CELL;
                int top = HEADER + (index / COLUMNS) * (CELL + CAPTION);
                Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
                Gdx.gl.glViewport(left, height - top - CELL, CELL, CELL);
                batch.begin(camera);
                batch.render(variants.get(index).instance(), environment);
                batch.end();
            }
            Gdx.gl.glViewport(0, 0, width, height);
            caption(variants, chassis, width, height, rows);
            Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, width, height);
            try {
                File output = new File(System.getProperty("megamek.gpu.screenshots"));
                assertTrue(output.isDirectory() || output.mkdirs());
                PixmapIO.writePNG(new FileHandle(new File(output, "variants-" + chassis + ".png")), pixels, -1, true);
            } finally {
                pixels.dispose();
            }
        } finally {
            target.end();
            target.dispose();
        }
    }

    private static void caption(List<Variant> variants, String chassis, int width, int height, int rows) {
        int least = Integer.MAX_VALUE;
        int most = 0;
        for (Variant variant : variants) {
            least = Math.min(least, variant.triangles());
            most = Math.max(most, variant.triangles());
        }
        var generator = new FreeTypeFontGenerator(new FileHandle(
              new File(Configuration.fontsDir(), "Noto Sans/NotoSans-Regular.ttf")));
        BitmapFont title = font(generator, 38);
        BitmapFont small = font(generator, 15);
        var batch = new SpriteBatch();
        try {
            batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, width, height));
            batch.begin();
            title.setColor(1, 1, 1, .95f);
            small.setColor(1, 1, 1, .72f);
            centred(batch, title, chassis.toUpperCase(), width / 2f, height - 34f);
            centred(batch, small, variants.size() + " variants  |  " + least + "-" + most
                  + " triangles  |  actual exported game meshes", width / 2f, height - 74f);
            for (int index = 0; index < variants.size(); index++) {
                Variant variant = variants.get(index);
                float middle = (index % COLUMNS) * CELL + CELL / 2f;
                float base = height - (HEADER + (index / COLUMNS) * (CELL + CAPTION) + CELL);
                small.setColor(1, 1, 1, .9f);
                centred(batch, small, variant.model(), middle, base - 6);
                small.setColor(1, 1, 1, .6f);
                centred(batch, small, variant.triangles() + " triangles", middle, base - 27);
            }
            batch.end();
        } finally {
            batch.dispose();
            title.dispose();
            small.dispose();
            generator.dispose();
        }
    }

    private static BitmapFont font(FreeTypeFontGenerator generator, int size) {
        var settings = new FreeTypeFontGenerator.FreeTypeFontParameter();
        settings.size = size;
        return generator.generateFont(settings);
    }

    private static void centred(SpriteBatch batch, BitmapFont font, String text, float middle, float baseline) {
        var layout = new GlyphLayout(font, text);
        font.draw(batch, layout, middle - layout.width / 2, baseline);
    }

    private static List<Variant> collect(GpuUnitModels library, MekTileset tileset, String chassis, String asset)
          throws Exception {
        File archive = new File(Configuration.dataDir(), "mekfiles/unit_files.zip");
        List<String> entries = new ArrayList<>();
        try (var zip = new ZipFile(archive)) {
            for (var entry : Collections.list(zip.entries())) {
                // A prefix match keeps the parse cheap; the full chassis is confirmed once the file is
                // read. Clan units carry two names, so getChassis alone drops Mad Cat (Timber Wolf).
                if (entry.getName().endsWith(".mtf")
                      && entry.getName().substring(entry.getName().lastIndexOf('/') + 1).startsWith(chassis)) {
                    entries.add(entry.getName());
                }
            }
        }
        Collections.sort(entries);
        List<Variant> variants = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        int id = 900;
        for (String entry : entries) {
            if (!(new MekFileParser(archive, entry).getEntity() instanceof Mek mek)
                  || !chassis.equals(mek.getFullChassis()) || seen.contains(mek.getModel())) {
                continue;
            }
            mek.setId(id++);
            var selected = UnitModelSelection.capture(mek, -1, false, tileset);
            // A variant with its own dedicated artwork belongs on that body's sheet, not this one.
            if (!asset.equals(selected.asset())) {
                continue;
            }
            var visual = library.get(selected, mek.getId());
            var drawn = new ModelInstance(visual.instance.model);
            visual.showEquipment(drawn, selected.state().appearance());
            seen.add(mek.getModel());
            variants.add(new Variant(mek.getModel(), drawn, triangles(drawn.nodes)));
        }
        // Unit files are grouped by era on disk; a sheet reads better in model order.
        variants.sort(Comparator.comparing(Variant::model));
        return variants;
    }

    /** Counts what is actually drawn, so a hidden actuator or a disabled mount is not charged for. */
    private static int triangles(Iterable<Node> nodes) {
        int total = 0;
        for (Node node : nodes) {
            for (var part : node.parts) {
                if (part.enabled) {
                    total += part.meshPart.size / 3;
                }
            }
            total += triangles(node.getChildren());
        }
        return total;
    }
}

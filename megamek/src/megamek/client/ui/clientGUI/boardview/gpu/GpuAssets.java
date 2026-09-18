/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import megamek.common.Configuration;

/** GL-thread ownership of shared models, repeating terrain materials, and animated water frames. */
final class GpuAssets implements Disposable {
    private final File root = new File(Configuration.dataDir(), "models/board");
    private final Map<String, Model> models = new HashMap<>();
    private final Map<Interior, Model> interiors = new HashMap<>();
    private final Map<String, Texture> materials = new HashMap<>();
    private final Map<Integer, Water> water = new HashMap<>();

    private record Interior(String asset, int levels) { }

    private record Water(List<Texture> frames, float[] ends, float duration) {
        Texture at(float time) {
            float position = time % duration;
            for (int index = 0; index < ends.length; index++) {
                if (position < ends[index]) {
                    return frames.get(index);
                }
            }
            return frames.getLast();
        }
    }

    Model model(String name) {
        return models.computeIfAbsent(name, key -> {
            var data = new G3dModelLoader(new JsonReader()).loadModelData(new FileHandle(new File(root, key + ".g3dj")));
            Model model = new Model(data, filename -> texture(new FileHandle(filename)));
            // Textures are shared across models and terrain; only this cache disposes them.
            Iterator<Disposable> owned = model.getManagedDisposables().iterator();
            while (owned.hasNext()) {
                if (owned.next() instanceof Texture) {
                    owned.remove();
                }
            }
            return model;
        });
    }

    Texture material(String name) {
        return texture(new FileHandle(new File(root, "textures/" + name + ".png")));
    }

    /** Reuse only the south-edge artwork, so rotating a cliff never selects a baked sunlit variant. */
    Texture incline(BoardScene.Surface surface) {
        String path = switch (surface) {
            case GRASS -> "Default/High_Incline_Top_Grass_08.png";
            case DIRT -> "Mars/High_Incline_Top_Mars_08.png";
            case SAND -> "Desert/High_Incline_Top_08.png";
            case ROCK, CONCRETE -> "Lunar/High_Incline_Top_Lunar_08.png";
            case SNOW -> "Snow/High_Incline_Top_Snow_08.png";
        };
        return texture(new FileHandle(new File(root, "tileset/High_Incline/" + path)));
    }

    private Texture texture(FileHandle file) {
        return materials.computeIfAbsent(file.file().toPath().normalize().toString(), key -> {
            Texture texture = new Texture(file, true);
            texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            boolean repeating = file.file().toPath().toAbsolutePath().normalize()
                  .startsWith(new File(root, "textures").toPath().toAbsolutePath().normalize());
            Texture.TextureWrap wrap = repeating ? Texture.TextureWrap.Repeat : Texture.TextureWrap.ClampToEdge;
            texture.setWrap(wrap, wrap);
            if (repeating) {
                int level = Math.max(0, (int) Math.ceil(Math.log(Math.max(texture.getWidth(), texture.getHeight()) / 128.0) / Math.log(2)));
                texture.bind();
                // Match the board artwork's texel density while retaining editable source images.
                Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL30.GL_TEXTURE_BASE_LEVEL, level);
            }
            return texture;
        });
    }

    Texture water(int depth, float time) {
        // Saxarba provides depths 0..4. Deeper water retains its real geometry and uses the deepest artwork.
        return water.computeIfAbsent(Math.min(4, Math.max(0, depth)), this::loadWater).at(time);
    }

    record WaterFrames(List<BoardScene.Pixels> frames, float[] ends, float duration) { }

    private Water loadWater(int depth) {
        File file = new File(root, "tileset/saxarba/anim_water_" + depth + ".gif");
        WaterFrames decoded = readWater(file);
        List<Texture> frames = new ArrayList<>();
        try {
            for (BoardScene.Pixels image : decoded.frames()) {
                Pixmap pixmap = new Pixmap(image.width(), image.height(), Pixmap.Format.RGBA8888);
                try {
                    pixmap.setBlending(Pixmap.Blending.None);
                    for (int y = 0; y < image.height(); y++) {
                        for (int x = 0; x < image.width(); x++) {
                            pixmap.drawPixel(x, y, image.rgba(y * image.width() + x));
                        }
                    }
                    Texture texture = new Texture(pixmap);
                    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
                    frames.add(texture);
                } finally {
                    pixmap.dispose();
                }
            }
            return new Water(List.copyOf(frames), decoded.ends(), decoded.duration());
        } catch (RuntimeException error) {
            frames.forEach(Texture::dispose);
            throw error;
        }
    }

    /** GIF frames can be transparent, offset patches; compose them before uploading full-size water textures. */
    static WaterFrames readWater(File file) {
        ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
        try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
            reader.setInput(input);
            var stream = reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0");
            var screen = child(stream, "LogicalScreenDescriptor");
            int width = attribute(screen, "logicalScreenWidth"), height = attribute(screen, "logicalScreenHeight");
            BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int count = reader.getNumImages(true);
            List<BoardScene.Pixels> frames = new ArrayList<>();
            float[] ends = new float[count];
            float duration = 0;
            for (int index = 0; index < count; index++) {
                var metadata = reader.getImageMetadata(index).getAsTree("javax_imageio_gif_image_1.0");
                var control = child(metadata, "GraphicControlExtension");
                var descriptor = child(metadata, "ImageDescriptor");
                String disposal = control.getAttributes().getNamedItem("disposalMethod").getNodeValue();
                int left = attribute(descriptor, "imageLeftPosition"), top = attribute(descriptor, "imageTopPosition");
                BufferedImage patch = reader.read(index);
                BufferedImage previous = null;
                if (disposal.equals("restoreToPrevious")) {
                    previous = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    previous.setData(canvas.getData());
                }
                Graphics2D graphics = canvas.createGraphics();
                try {
                    graphics.drawImage(patch, left, top, null);
                    frames.add(waterFrame(canvas));
                    if (disposal.equals("restoreToBackgroundColor")) {
                        graphics.setComposite(AlphaComposite.Clear);
                        graphics.fillRect(left, top, patch.getWidth(), patch.getHeight());
                    }
                } finally {
                    graphics.dispose();
                }
                if (previous != null) {
                    canvas = previous;
                }
                duration += Math.max(0.02f, attribute(control, "delayTime") / 100f);
                ends[index] = duration;
            }
            return new WaterFrames(List.copyOf(frames), ends, duration);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot decode board water " + file, exception);
        } finally {
            reader.dispose();
        }
    }

    /** The mesh defines the shoreline; repeating hex-shaped alpha would pinch a scrolling waterfall. */
    private static BoardScene.Pixels waterFrame(BufferedImage canvas) {
        BufferedImage frame = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < canvas.getHeight(); y++) {
            int left = 0, right = canvas.getWidth() - 1;
            while (left < right && (canvas.getRGB(left, y) >>> 24) == 0) {
                left++;
            }
            while (right > left && (canvas.getRGB(right, y) >>> 24) == 0) {
                right--;
            }
            for (int x = 0; x < canvas.getWidth(); x++) {
                frame.setRGB(x, y, canvas.getRGB(Math.max(left, Math.min(right, x)), y));
            }
        }
        return new BoardScene.Pixels(frame);
    }

    /** Interior meshes share the shell's local coordinates; geometry tuning only changes their instance transform. */
    Model interior(String asset, int levels) {
        return interiors.computeIfAbsent(new Interior(asset, levels),
              key -> GpuBuildingInterior.build(model(key.asset()), key.levels()));
    }

    private static org.w3c.dom.Node child(org.w3c.dom.Node parent, String name) {
        for (var node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeName().equals(name)) {
                return node;
            }
        }
        throw new IllegalArgumentException("Missing GIF metadata: " + name);
    }

    private static int attribute(org.w3c.dom.Node node, String name) {
        return Integer.parseInt(node.getAttributes().getNamedItem(name).getNodeValue());
    }

    @Override
    public void dispose() {
        interiors.values().forEach(Model::dispose);
        interiors.clear();
        models.values().forEach(Model::dispose);
        materials.values().forEach(Texture::dispose);
        water.values().forEach(animation -> animation.frames().forEach(Texture::dispose));
        models.clear();
        materials.clear();
        water.clear();
    }
}

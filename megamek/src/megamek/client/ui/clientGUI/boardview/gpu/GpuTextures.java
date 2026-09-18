/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/** Shared atlas ownership. Pixel changes update existing slots without invalidating mesh UVs. */
final class GpuTextures<K> implements Disposable {
    private static final int ATLAS_BLEED = 2;
    private record Entry(BoardScene.Pixels pixels, String name, TextureRegion region) { }

    private final Map<K, Entry> entries = new HashMap<>();
    private final boolean mipmaps;
    private PixmapPacker packer;
    private TextureAtlas atlas;

    GpuTextures() {
        this(false);
    }

    GpuTextures(boolean mipmaps) {
        this.mipmaps = mipmaps;
    }

    /** Returns true only when the atlas layout changes. Must run on the GL thread. */
    boolean update(Map<K, BoardScene.Pixels> images) {
        boolean sameLayout = atlas != null && entries.keySet().equals(images.keySet())
              && images.entrySet().stream().allMatch(item -> {
                  BoardScene.Pixels old = entries.get(item.getKey()).pixels();
                  return old.width() == item.getValue().width() && old.height() == item.getValue().height();
              });
        if (sameLayout) {
            Set<Texture> updated = new HashSet<>();
            images.forEach((key, pixels) -> {
                Entry old = entries.get(key);
                if (old.pixels() != pixels) {
                    Pixmap pixmap = pixmap(pixels);
                    try {
                        TextureRegion region = old.region();
                        // Keep the managed backing image in sync for context restoration as well.
                        Pixmap page = packer.getPage(old.name()).getPixmap();
                        page.setBlending(Pixmap.Blending.None);
                        page.drawPixmap(pixmap, region.getRegionX(), region.getRegionY());
                        region.getTexture().bind();
                        Gdx.gl.glTexSubImage2D(GL20.GL_TEXTURE_2D, 0, region.getRegionX(), region.getRegionY(),
                              pixmap.getWidth(), pixmap.getHeight(), pixmap.getGLFormat(), pixmap.getGLType(),
                              pixmap.getPixels());
                        entries.put(key, new Entry(pixels, old.name(), region));
                        updated.add(region.getTexture());
                    } finally {
                        pixmap.dispose();
                    }
                }
            });
            if (mipmaps) {
                updated.forEach(texture -> {
                    texture.bind();
                    Gdx.gl.glGenerateMipmap(GL20.GL_TEXTURE_2D);
                });
            }
            return false;
        }
        dispose();
        Map<K, String> names = new HashMap<>();
        // Guillotine packing reserves two outer margins AND one margin on each packed rectangle.
        int width = Math.max(2048, images.values().stream().mapToInt(BoardScene.Pixels::width).max().orElse(0) + 3 * ATLAS_BLEED);
        int height = Math.max(2048, images.values().stream().mapToInt(BoardScene.Pixels::height).max().orElse(0) + 3 * ATLAS_BLEED);
        packer = new PixmapPacker(width, height, Pixmap.Format.RGBA8888, ATLAS_BLEED, true);
        images.forEach((key, pixels) -> {
            Pixmap pixmap = pixmap(pixels);
            try {
                String name = "image" + names.size();
                packer.pack(name, pixmap);
                names.put(key, name);
            } finally {
                pixmap.dispose();
            }
        });
        atlas = packer.generateTextureAtlas(mipmaps ? Texture.TextureFilter.MipMapLinearLinear : Texture.TextureFilter.Linear,
              Texture.TextureFilter.Linear, mipmaps);
        if (mipmaps) {
            for (Texture texture : atlas.getTextures()) {
                texture.bind();
                // Keep the sampling footprint inside the artwork margin and duplicated atlas border.
                Gdx.gl.glTexParameterf(GL20.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAX_LEVEL, 2);
            }
        }
        names.forEach((key, name) -> entries.put(key, new Entry(images.get(key), name, atlas.findRegion(name))));
        return true;
    }

    private static Pixmap pixmap(BoardScene.Pixels image) {
        Pixmap result = new Pixmap(image.width(), image.height(), Pixmap.Format.RGBA8888);
        result.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                result.drawPixel(x, y, image.rgba(y * image.width() + x));
            }
        }
        return result;
    }

    TextureRegion region(K key) {
        return entries.get(key).region();
    }

    @Override
    public void dispose() {
        if (atlas != null) {
            atlas.dispose();
            atlas = null;
        }
        if (packer != null) {
            packer.dispose();
            packer = null;
        }
        entries.clear();
    }
}

/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
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
        if (images.isEmpty()) {
            boolean changed = !entries.isEmpty();
            dispose();
            return changed;
        }
        Map<Entry, BoardScene.Pixels> replacements = new IdentityHashMap<>();
        boolean sameLayout = atlas != null && entries.keySet().equals(images.keySet())
              && images.entrySet().stream().allMatch(item -> {
                  Entry old = entries.get(item.getKey());
                  BoardScene.Pixels pixels = item.getValue();
                  BoardScene.Pixels shared = replacements.putIfAbsent(old, pixels);
                  // A shared slot can change in place only if all of its users still agree.
                  return old.pixels().width() == pixels.width() && old.pixels().height() == pixels.height()
                        && (shared == null || shared.equals(pixels));
              });
        // Previously different images may become identical after an edit; merge those slots too.
        sameLayout &= new HashSet<>(replacements.values()).size() == replacements.size();
        if (sameLayout) {
            Set<Texture> updated = new HashSet<>();
            Map<Entry, Entry> next = new IdentityHashMap<>();
            replacements.forEach((old, pixels) -> {
                if (!old.pixels().equals(pixels)) {
                    // PixmapPacker duplicates one edge pixel. Refresh it as well as the interior when a slot changes.
                    Pixmap pixmap = pixmap(pixels, 1);
                    try {
                        TextureRegion region = old.region();
                        // Keep the managed backing image in sync for context restoration as well.
                        Pixmap page = packer.getPage(old.name()).getPixmap();
                        page.setBlending(Pixmap.Blending.None);
                        page.drawPixmap(pixmap, region.getRegionX() - 1, region.getRegionY() - 1);
                        region.getTexture().bind();
                        Gdx.gl.glTexSubImage2D(GL20.GL_TEXTURE_2D, 0, region.getRegionX() - 1, region.getRegionY() - 1,
                              pixmap.getWidth(), pixmap.getHeight(), pixmap.getGLFormat(), pixmap.getGLType(),
                              pixmap.getPixels());
                        next.put(old, new Entry(pixels, old.name(), region));
                        updated.add(region.getTexture());
                    } finally {
                        pixmap.dispose();
                    }
                }
            });
            entries.replaceAll((key, old) -> next.getOrDefault(old, old));
            if (mipmaps) {
                updated.forEach(texture -> {
                    texture.bind();
                    Gdx.gl.glGenerateMipmap(GL20.GL_TEXTURE_2D);
                });
            }
            return false;
        }
        dispose();
        Map<BoardScene.Pixels, String> names = new HashMap<>();
        Set<BoardScene.Pixels> unique = new HashSet<>(images.values());
        long area = unique.stream().mapToLong(pixels ->
              (long) (pixels.width() + ATLAS_BLEED) * (pixels.height() + ATLAS_BLEED)).sum();
        int side = 128;
        while (side < 2048 && side * (long) side < area * 1.3) {
            side *= 2;
        }
        // Guillotine packing reserves two outer margins AND one margin on each packed rectangle.
        int width = Math.max(side, unique.stream().mapToInt(BoardScene.Pixels::width).max().orElse(0) + 3 * ATLAS_BLEED);
        int height = Math.max(side, unique.stream().mapToInt(BoardScene.Pixels::height).max().orElse(0) + 3 * ATLAS_BLEED);
        packer = new PixmapPacker(width, height, Pixmap.Format.RGBA8888, ATLAS_BLEED, true);
        images.forEach((key, pixels) -> {
            if (names.containsKey(pixels)) {
                return;
            }
            Pixmap pixmap = pixmap(pixels, 0);
            try {
                String name = "image" + names.size();
                packer.pack(name, pixmap);
                names.put(pixels, name);
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
        Map<BoardScene.Pixels, Entry> shared = new HashMap<>();
        names.forEach((pixels, name) -> shared.put(pixels, new Entry(pixels, name, atlas.findRegion(name))));
        images.forEach((key, pixels) -> entries.put(key, shared.get(pixels)));
        return true;
    }

    private static Pixmap pixmap(BoardScene.Pixels image, int border) {
        Pixmap result = new Pixmap(image.width() + 2 * border, image.height() + 2 * border, Pixmap.Format.RGBA8888);
        result.setBlending(Pixmap.Blending.None);
        // One direct-buffer write loop instead of one JNI call per pixel. RGBA bytes have fixed order.
        ByteBuffer buffer = result.getPixels().duplicate().order(ByteOrder.BIG_ENDIAN);
        for (int y = -border; y < image.height() + border; y++) {
            int row = Math.clamp(y, 0, image.height() - 1) * image.width();
            for (int x = -border; x < image.width() + border; x++) {
                buffer.putInt(image.rgba(row + Math.clamp(x, 0, image.width() - 1)));
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

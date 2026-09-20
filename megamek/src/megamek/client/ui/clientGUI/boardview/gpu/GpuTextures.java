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
    private record Images(BoardScene.Pixels color, BoardScene.Pixels normal) { }
    private record Entry(Images images, String name, TextureRegion region) { }

    private final Map<K, Entry> entries = new HashMap<>();
    private final Map<Texture, Texture> normals = new IdentityHashMap<>();
    private final boolean mipmaps;
    private PixmapPacker packer;
    private TextureAtlas atlas;
    private PixmapPacker normalPacker;
    private TextureAtlas normalAtlas;

    GpuTextures() {
        this(false);
    }

    GpuTextures(boolean mipmaps) {
        this.mipmaps = mipmaps;
    }

    /** Returns true only when the atlas layout changes. Must run on the GL thread. */
    boolean update(Map<K, BoardScene.Pixels> images) {
        return update(images, Map.of());
    }

    /** Optional pre-generated normals share each color slot's placement and lifetime. */
    boolean update(Map<K, BoardScene.Pixels> colors, Map<K, BoardScene.Pixels> normalImages) {
        Map<K, Images> images = new HashMap<>();
        colors.forEach((key, pixels) -> {
            BoardScene.Pixels normal = normalImages.get(key);
            if (normal != null && (normal.width() != pixels.width() || normal.height() != pixels.height())) {
                throw new IllegalArgumentException("Normal map dimensions must match the ground artwork");
            }
            images.put(key, new Images(pixels, normal));
        });
        boolean normalMaps = !normalImages.isEmpty();
        if (images.isEmpty()) {
            boolean changed = !entries.isEmpty();
            dispose();
            return changed;
        }
        Map<Entry, Images> replacements = new IdentityHashMap<>();
        boolean sameLayout = atlas != null && (normalAtlas != null) == normalMaps && entries.keySet().equals(images.keySet())
              && images.entrySet().stream().allMatch(item -> {
                  Entry old = entries.get(item.getKey());
                  Images pixels = item.getValue();
                  Images shared = replacements.putIfAbsent(old, pixels);
                  // A shared slot can change in place only if all of its users still agree.
                  return old.images().color().width() == pixels.color().width()
                        && old.images().color().height() == pixels.color().height()
                        && (shared == null || shared.equals(pixels));
              });
        // Previously different images may become identical after an edit; merge those slots too.
        sameLayout &= new HashSet<>(replacements.values()).size() == replacements.size();
        if (sameLayout) {
            Set<Texture> updated = new HashSet<>();
            Map<Entry, Entry> next = new IdentityHashMap<>();
            replacements.forEach((old, pixels) -> {
                if (!old.images().equals(pixels)) {
                    // PixmapPacker duplicates one edge pixel. Refresh it as well as the interior when a slot changes.
                    replace(packer, old.name(), old.region(), pixels.color(), false);
                    next.put(old, new Entry(pixels, old.name(), old.region()));
                    updated.add(old.region().getTexture());
                    if (normalMaps) {
                        TextureRegion normal = normalAtlas.findRegion(old.name());
                        replace(normalPacker, old.name(), normal,
                              pixels.normal() == null ? pixels.color() : pixels.normal(), pixels.normal() == null);
                        updated.add(normal.getTexture());
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
        Map<Images, String> names = new HashMap<>();
        Set<Images> unique = new HashSet<>(images.values());
        long area = unique.stream().mapToLong(pixels ->
              (long) (pixels.color().width() + ATLAS_BLEED) * (pixels.color().height() + ATLAS_BLEED)).sum();
        int side = 128;
        while (side < 2048 && side * (long) side < area * 1.3) {
            side *= 2;
        }
        // Guillotine packing reserves two outer margins AND one margin on each packed rectangle.
        int width = Math.max(side, unique.stream().mapToInt(pixels -> pixels.color().width()).max().orElse(0) + 3 * ATLAS_BLEED);
        int height = Math.max(side, unique.stream().mapToInt(pixels -> pixels.color().height()).max().orElse(0) + 3 * ATLAS_BLEED);
        packer = new PixmapPacker(width, height, Pixmap.Format.RGBA8888, ATLAS_BLEED, true);
        if (normalMaps) {
            normalPacker = new PixmapPacker(width, height, Pixmap.Format.RGBA8888, ATLAS_BLEED, true);
        }
        images.forEach((key, pixels) -> {
            if (names.containsKey(pixels)) {
                return;
            }
            String name = "image" + names.size();
            pack(packer, name, pixels.color(), false);
            if (normalMaps) {
                // Identical sizes and insertion order give both atlases the same pages and UVs.
                // Deduplicate the pair: identical colors can carry different authored normal maps.
                pack(normalPacker, name, pixels.normal() == null ? pixels.color() : pixels.normal(), pixels.normal() == null);
            }
            names.put(pixels, name);
        });
        atlas = atlas(packer);
        if (normalMaps) {
            normalAtlas = atlas(normalPacker);
            names.values().forEach(name -> normals.put(atlas.findRegion(name).getTexture(),
                  normalAtlas.findRegion(name).getTexture()));
        }
        Map<Images, Entry> shared = new HashMap<>();
        names.forEach((pixels, name) -> shared.put(pixels, new Entry(pixels, name, atlas.findRegion(name))));
        images.forEach((key, pixels) -> entries.put(key, shared.get(pixels)));
        return true;
    }

    private TextureAtlas atlas(PixmapPacker source) {
        TextureAtlas result = source.generateTextureAtlas(mipmaps ? Texture.TextureFilter.MipMapLinearLinear : Texture.TextureFilter.Linear,
              Texture.TextureFilter.Linear, mipmaps);
        if (mipmaps) {
            for (Texture texture : result.getTextures()) {
                texture.bind();
                // Keep the sampling footprint inside the artwork margin and duplicated atlas border.
                Gdx.gl.glTexParameterf(GL20.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAX_LEVEL, 2);
            }
        }
        return result;
    }

    private static void pack(PixmapPacker target, String name, BoardScene.Pixels pixels, boolean flatNormal) {
        Pixmap pixmap = pixmap(pixels, 0, flatNormal);
        try {
            target.pack(name, pixmap);
        } finally {
            pixmap.dispose();
        }
    }

    private static void replace(PixmapPacker target, String name, TextureRegion region, BoardScene.Pixels pixels, boolean flatNormal) {
        Pixmap pixmap = pixmap(pixels, 1, flatNormal);
        try {
            // Keep the managed backing image in sync for context restoration as well.
            Pixmap page = target.getPage(name).getPixmap();
            page.setBlending(Pixmap.Blending.None);
            page.drawPixmap(pixmap, region.getRegionX() - 1, region.getRegionY() - 1);
            region.getTexture().bind();
            Gdx.gl.glTexSubImage2D(GL20.GL_TEXTURE_2D, 0, region.getRegionX() - 1, region.getRegionY() - 1,
                  pixmap.getWidth(), pixmap.getHeight(), pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels());
        } finally {
            pixmap.dispose();
        }
    }

    static Pixmap pixmap(BoardScene.Pixels image, int border, boolean flatNormal) {
        Pixmap result = new Pixmap(image.width() + 2 * border, image.height() + 2 * border, Pixmap.Format.RGBA8888);
        result.setBlending(Pixmap.Blending.None);
        // One direct-buffer write loop instead of one JNI call per pixel. RGBA bytes have fixed order.
        ByteBuffer buffer = result.getPixels().duplicate().order(ByteOrder.BIG_ENDIAN);
        for (int y = -border; y < image.height() + border; y++) {
            int row = Math.clamp(y, 0, image.height() - 1) * image.width();
            for (int x = -border; x < image.width() + border; x++) {
                buffer.putInt(flatNormal ? 0x8080ffff : image.rgba(row + Math.clamp(x, 0, image.width() - 1)));
            }
        }
        return result;
    }

    TextureRegion region(K key) {
        return entries.get(key).region();
    }

    /** The normal page uses exactly the diffuse page's UVs, including its duplicated border and mip levels. */
    Texture normal(Texture diffuse) {
        return normals.get(diffuse);
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
        if (normalAtlas != null) {
            normalAtlas.dispose();
            normalAtlas = null;
        }
        if (normalPacker != null) {
            normalPacker.dispose();
            normalPacker = null;
        }
        normals.clear();
        entries.clear();
    }
}

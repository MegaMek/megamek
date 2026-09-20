/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import megamek.client.ui.clientGUI.boardview.BoardFieldOfView;

/** One texel per hex, containing shared visibility results rather than painted board artwork. */
final class GpuFieldOfView implements Disposable {
    enum Style {
        DIMMED("Dimmed"), GRAYSCALE("Grayscale"), FOG_OF_WAR("Fog of war");

        final String label;

        Style(String label) {
            this.label = label;
        }
    }

    /** Default presentation: all styles use the same LOS/sensor results and unit visibility. */
    static final Style STYLE = Style.FOG_OF_WAR;

    /** Default FoV opacity multiplier: 0 leaves brightness unchanged, 1 uses full opacity. */
    static final float DARKNESS = 0.25f;

    private Style style;
    private float darkness = DARKNESS;
    private BoardFieldOfView previous = BoardFieldOfView.EMPTY;
    private Texture mask;
    private boolean active;
    private long uploads;

    GpuFieldOfView(Style style) {
        this.style = style;
    }

    /** Render-thread presentation settings; changes reuse the current visibility mask. */
    void configure(Style style, float darkness) {
        this.style = style;
        this.darkness = darkness;
    }

    void update(BoardFieldOfView next) {
        if (previous.equals(next)) {
            return;
        }
        previous = next;
        active = next.active();
        if (!active) {
            return;
        }
        Pixmap pixels = new Pixmap(next.width(), next.height(), Pixmap.Format.RGBA8888);
        try {
            pixels.setBlending(Pixmap.Blending.None);
            for (int x = 0; x < next.width(); x++) {
                for (int y = 0; y < next.height(); y++) {
                    BoardFieldOfView.Hex hex = next.hexes().get(x * next.height() + y);
                    // Low bits classify visibility; bit 3 says this visible hex has a distance-ring tint.
                    int state = hex.visibility().ordinal();
                    if (hex.visibility() == BoardFieldOfView.Visibility.VISIBLE && (hex.tint() >>> 24) != 0) {
                        state |= 8;
                    }
                    pixels.drawPixel(x, y, (hex.tint() << 8) | state);
                }
            }
            if (mask == null || mask.getWidth() != next.width() || mask.getHeight() != next.height()) {
                if (mask != null) {
                    mask.dispose();
                }
                mask = new Texture(pixels);
                mask.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                mask.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
            } else {
                mask.draw(pixels, 0, 0);
            }
            uploads++;
        } finally {
            pixels.dispose();
        }
    }

    boolean active() {
        return active;
    }

    long uploads() {
        return uploads;
    }

    void bind(ShaderProgram shader, Camera camera) {
        mask.bind(3);
        shader.setUniformi("u_fov", 3);
        shader.setUniformf("u_fovSize", previous.width(), previous.height());
        shader.setUniformf("u_fovHexSize", BoardGeometry.WIDTH, BoardGeometry.HEIGHT);
        shader.setUniformMatrix("u_fovInverseView", camera.invProjectionView);
        float opacity = previous.darken() ? previous.darkenAlpha() / 255f * darkness : 0;
        boolean grayscale = previous.grayscale() || style == Style.GRAYSCALE && previous.darken();
        shader.setUniformf("u_fovOptions", opacity,
              previous.highlightAlpha() / 255f, grayscale ? 1 : 0, previous.spotting() ? 1 : 0);
        shader.setUniformf("u_fovStyle", style == Style.FOG_OF_WAR ? 1 : 0);
        float pixel = camera instanceof OrthographicCamera ortho ? ortho.zoom / BoardGeometry.HEIGHT : 0.01f;
        shader.setUniformf("u_fovEdge", Math.max(0.004f, Math.min(0.04f, pixel * 1.5f)));
    }

    @Override
    public void dispose() {
        if (mask != null) {
            mask.dispose();
            mask = null;
        }
        previous = BoardFieldOfView.EMPTY;
        active = false;
    }
}

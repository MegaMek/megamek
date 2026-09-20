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

    /** Outside visual LOS, unless a known sensor range also excludes the hex. */
    static final Style FOV_STYLE = Style.DIMMED;
    static final float FOV_DARKNESS = 0.65f;
    /** Artwork desaturation before shading: 0 keeps the original colors, 1 uses their luminance. */
    static final float DIMMED_DESATURATION = 0.25f;

    /** Outside both visual LOS and sensor coverage. */
    static final Style SENSOR_STYLE = Style.FOG_OF_WAR;
    static final float SENSOR_DARKNESS = 0.25f;

    private Style fovStyle = FOV_STYLE;
    private float fovDarkness = FOV_DARKNESS;
    private Style sensorStyle = SENSOR_STYLE;
    private float sensorDarkness = SENSOR_DARKNESS;
    private BoardFieldOfView previous = BoardFieldOfView.EMPTY;
    private Texture mask;
    private boolean active;
    private long uploads;

    /** Render-thread opacity multipliers and styles; changes reuse the current visibility mask. */
    void configure(Style fovStyle, float fovDarkness, Style sensorStyle, float sensorDarkness) {
        this.fovStyle = fovStyle;
        this.fovDarkness = fovDarkness;
        this.sensorStyle = sensorStyle;
        this.sensorDarkness = sensorDarkness;
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
                    // Low bits classify LOS; bit 3 marks a distance-ring tint, bit 4 is outside sensor range.
                    int state = hex.visibility().ordinal();
                    if (hex.visibility() == BoardFieldOfView.Visibility.VISIBLE && (hex.tint() >>> 24) != 0) {
                        state |= 8;
                    }
                    if (hex.outsideSensorRange()) {
                        state |= 16;
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
        shader.setUniformf("u_fovOptions", previous.highlightAlpha() / 255f, previous.spotting() ? 1 : 0);
        shader.setUniformf("u_dimmedDesaturation", DIMMED_DESATURATION);
        bindEffect(shader, "u_fovEffect", fovStyle, fovDarkness);
        bindEffect(shader, "u_sensorEffect", sensorStyle, sensorDarkness);
        float pixel = camera instanceof OrthographicCamera ortho ? ortho.zoom / BoardGeometry.HEIGHT : 0.01f;
        shader.setUniformf("u_fovEdge", Math.max(0.004f, Math.min(0.04f, pixel * 1.5f)));
    }

    private void bindEffect(ShaderProgram shader, String uniform, Style style, float darkness) {
        float opacity = previous.darken() ? previous.darkenAlpha() / 255f * darkness : 0;
        // GPU styles are explicit: the classic view's grayscale preference must not override Dimmed.
        boolean grayscale = style == Style.GRAYSCALE && previous.darken();
        shader.setUniformf(uniform, opacity, grayscale ? 1 : 0, style == Style.FOG_OF_WAR ? 1 : 0);
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

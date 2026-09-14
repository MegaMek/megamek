/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.nio.FloatBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.utils.BufferUtils;
import org.lwjgl.glfw.GLFW;

/** One logical-pixel scale for the native UI, board camera, and shared overlay input. */
final class GpuDisplayScale {
    private final FloatBuffer scaleX = BufferUtils.newFloatBuffer(1);
    private final FloatBuffer scaleY = BufferUtils.newFloatBuffer(1);

    float read(float preference) {
        var graphics = (Lwjgl3Graphics) Gdx.graphics;
        GLFW.glfwGetWindowContentScale(graphics.getWindow().getWindowHandle(), scaleX, scaleY);
        float pixelRatio = graphics.getBackBufferWidth() / (float) Math.max(1, graphics.getWidth());
        return calculate(graphics.getWidth(), graphics.getHeight(), Math.max(scaleX.get(0), scaleY.get(0)),
              pixelRatio, preference);
    }

    static float calculate(int width, int height, float dpiScale, float pixelRatio, float preference) {
        // Retina already scales logical pixels; Windows commonly reports framebuffer-sized window coordinates.
        float dpi = Math.max(1, dpiScale) / Math.max(1, pixelRatio);
        float resolution = Math.min(width / 1600f, height / 1000f);
        float desired = Math.max(dpi, resolution) * preference;
        float available = Math.min(width / 960f, height / 640f);
        return Math.max(0.01f, Math.min(desired, available));
    }
}

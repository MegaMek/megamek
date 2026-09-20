/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

/** Shared cast-shadow shading for terrain, props and units in every camera view. */
final class GpuShadowShader {
    /** Extra ambient-light occlusion: 0 keeps the original shadows, 1 removes all ambient light inside them. */
    static final float SHADOW_OPACITY = 0.3f;

    private static final String MAIN = "void main() {";

    private GpuShadowShader() { }

    static String fragmentSource(String source) {
        int main = source.indexOf(MAIN);
        if (main < 0 || source.indexOf(MAIN, main + MAIN.length()) >= 0) {
            throw new IllegalStateException("Incompatible shadow fragment shader: expected exactly one " + MAIN);
        }
        // Only rewrite uses inside main, retaining upstream declarations and shadow-map sampling.
        String body = source.substring(main + MAIN.length())
              .replace("getShadow()", "boardShadowVisibility")
              .replace("v_ambientLight", "(v_ambientLight * boardShadowAmbient)");
        return "#define BOARD_SHADOW_OPACITY " + SHADOW_OPACITY + "\n" + source.substring(0, main) + MAIN + "\n" + """
              #if defined(lightingFlag) && defined(shadowMapFlag)
                  float boardShadowVisibility = getShadow();
                  float boardShadowAmbient = mix(1.0 - BOARD_SHADOW_OPACITY, 1.0, boardShadowVisibility);
              #else
                  float boardShadowAmbient = 1.0;
              #endif
              """ + body;
    }
}

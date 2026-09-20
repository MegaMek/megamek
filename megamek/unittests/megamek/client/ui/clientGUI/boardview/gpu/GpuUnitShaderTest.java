/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.UnaryOperator;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Checks the upstream GLSL and classpath resources without creating an OpenGL context. */
class GpuUnitShaderTest {
    private Files previousFiles;

    @BeforeEach
    void setUpFiles() {
        previousFiles = Gdx.files;
        Gdx.files = new Lwjgl3Files();
    }

    @AfterEach
    void restoreFiles() {
        Gdx.files = previousFiles;
    }

    @Test
    void composesTheBundledLibGdxShadersWithTheShippedResources() {
        assertDoesNotThrow(() -> GpuUnitShader.vertexSource(DefaultShader.getDefaultVertexShader()));
        assertDoesNotThrow(() -> GpuUnitShader.fragmentSource(DefaultShader.getDefaultFragmentShader()));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
          vertex, void main() {
          vertex, v_diffuseUV = u_diffuseUVTransform.xy + a_texCoord0 * u_diffuseUVTransform.zw;
          fragment, void main() {
          fragment, #if defined(emissiveTextureFlag) && defined(emissiveColorFlag)
          """)
    void rejectsMissingOrAmbiguousUpstreamInsertionPoints(String stage, String anchor) {
        boolean vertex = "vertex".equals(stage);
        String source = vertex ? DefaultShader.getDefaultVertexShader() : DefaultShader.getDefaultFragmentShader();
        UnaryOperator<String> compose = vertex ? GpuUnitShader::vertexSource : GpuUnitShader::fragmentSource;
        // Whitespace alone must not silently drop an effect; a second match must not inject it twice.
        for (String incompatible : List.of(source.replace(anchor, anchor.replace(" ", "  ")), source + "\n" + anchor)) {
            var error = assertThrows(IllegalStateException.class, () -> compose.apply(incompatible));
            assertTrue(error.getMessage().contains(stage));
            assertTrue(error.getMessage().contains(anchor));
        }
    }
}

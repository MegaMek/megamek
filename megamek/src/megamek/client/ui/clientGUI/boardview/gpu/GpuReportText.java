/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/** A normal wrapped label with hit areas taken from its actual glyphs, so links survive resizing and wrapping. */
final class GpuReportText extends Label {
    private static final Color LINK_COLOR = Color.valueOf("D8BC82");
    private record Hit(GpuReportLog.Link link, Rectangle bounds, int firstGlyph, int endGlyph) { }
    private final List<GpuReportLog.Link> links;
    private final List<Hit> hits = new ArrayList<>();
    private final Drawable line;
    private final Color tint = new Color();

    GpuReportText(GpuReportLog.Entry entry, Skin skin, Consumer<GpuReportLog.Link> activate) {
        super(entry.text().replace('\n', ' '), skin, "menu");
        links = entry.links();
        line = skin.getDrawable("white");
        setWrap(true);
        TextTooltip tooltip = new TextTooltip("", skin);
        addListener(new InputListener() {
            private GpuReportLog.Link hovered;
            private GpuReportLog.Link pressed;

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                GpuReportLog.Link next = linkAt(x, y);
                if (next != hovered) {
                    tooltip.hide();
                    hovered = next;
                    if (next != null) {
                        tooltip.getActor().setText(next.unitId() >= 0 ? "Open unit readout"
                              : next.detail() + "\nClick to keep details open");
                        tooltip.enter(event, x, y, -1, null);
                    }
                    Gdx.graphics.setSystemCursor(next == null ? SystemCursor.Arrow : SystemCursor.Hand);
                }
                tooltip.mouseMoved(event, x, y);
                return false;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer == -1) {
                    hovered = null;
                    tooltip.hide();
                    Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                }
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                pressed = button == Input.Buttons.LEFT ? linkAt(x, y) : null;
                return pressed != null;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (pressed != null && pressed.equals(linkAt(x, y))) {
                    tooltip.hide();
                    activate.accept(pressed);
                }
                pressed = null;
            }
        });
    }

    GpuReportLog.Link linkAt(float x, float y) {
        validate();
        return hits.stream().filter(hit -> hit.bounds().contains(x, y)).map(Hit::link).findFirst().orElse(null);
    }

    @Override
    public void layout() {
        super.layout();
        hits.clear();
        var cache = getBitmapFontCache();
        int[] pageOffsets = new int[cache.getPageCount()];
        int character = 0;
        int glyphIndex = 0;
        for (var run : getGlyphLayout().runs) {
            Hit previous = null;
            for (var glyph : run.glyphs) {
                // Word wrapping removes spaces at line boundaries from the glyph cache.
                while (character < getText().length() && Character.isWhitespace(getText().charAt(character))
                      && getText().charAt(character) != glyph.id) {
                    character++;
                }
                int offset = pageOffsets[glyph.page];
                pageOffsets[glyph.page] += 20; // Four XY/color/UV vertices per glyph.
                GpuReportLog.Link link = null;
                for (var candidate : links) {
                    if (character >= candidate.start() && character < candidate.end()) {
                        link = candidate;
                        break;
                    }
                }
                if (link != null) {
                    float[] vertices = cache.getVertices(glyph.page);
                    Rectangle bounds = new Rectangle(vertices[offset] - cache.getX(), vertices[offset + 1] - cache.getY(),
                          vertices[offset + 10] - vertices[offset], vertices[offset + 6] - vertices[offset + 1]);
                    if (previous != null && previous.link().equals(link)) {
                        previous.bounds().merge(bounds);
                        hits.removeLast();
                        previous = new Hit(link, previous.bounds(), previous.firstGlyph(), glyphIndex + 1);
                    } else {
                        previous = new Hit(link, bounds, glyphIndex, glyphIndex + 1);
                    }
                    hits.add(previous);
                } else {
                    previous = null;
                }
                character++;
                glyphIndex++;
            }
        }
        hits.forEach(hit -> {
            hit.bounds().y -= 2;
            hit.bounds().height += 4;
        });
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        validate();
        var cache = getBitmapFontCache();
        tint.set(getStyle().fontColor).mul(getColor());
        tint.a *= parentAlpha;
        cache.setColors(tint);
        tint.set(LINK_COLOR).mul(getColor());
        tint.a *= parentAlpha;
        hits.forEach(hit -> cache.setColors(tint, hit.firstGlyph(), hit.endGlyph()));
        cache.setPosition(getX(), getY());
        cache.draw(batch);
        float oldColor = batch.getPackedColor();
        batch.setColor(tint);
        for (Hit hit : hits) {
            Rectangle bounds = hit.bounds();
            line.draw(batch, getX() + bounds.x, getY() + bounds.y, bounds.width, 0.6f);
        }
        batch.setPackedColor(oldColor);
    }
}

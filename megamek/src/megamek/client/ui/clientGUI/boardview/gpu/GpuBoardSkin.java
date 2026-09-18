/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Disposable;
import megamek.client.ui.widget.MegaMekBorder;
import megamek.client.ui.widget.SkinSpecification;
import megamek.client.ui.widget.SkinXMLHandler;
import megamek.common.Configuration;
import megamek.common.util.fileUtils.MegaMekFile;

/** GPU presentation of the existing MegaMek skin, with window-owned textures and fonts. */
final class GpuBoardSkin implements Disposable {
    static final Color TEXT = Color.valueOf("E6E9E9");
    static final Color MUTED = Color.valueOf("ADB5B6");
    static final Color ACCENT = Color.valueOf("CDD8DA");
    static final Color DISABLED = Color.valueOf("7F8585");
    final Skin skin = new Skin();
    private final List<FreeTypeFontGenerator> generators = new ArrayList<>();

    GpuBoardSkin() {
        Pixmap white = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        white.setColor(Color.WHITE);
        white.fill();
        skin.add("white", new Texture(white));
        white.dispose();
        BitmapFont font = font("default-font", "Noto Sans/NotoSans-Regular.ttf", 15);
        BitmapFont bold = font("bold-font", "Noto Sans/NotoSans-Bold.ttf", 15);
        BitmapFont heading = font("heading-font", "Noto Sans/NotoSans-Bold.ttf", 19);
        BitmapFont small = font("small-font", "Noto Sans/NotoSans-Regular.ttf", 12);
        skin.add("default", new Label.LabelStyle(font, TEXT));
        skin.add("heading", new Label.LabelStyle(heading, TEXT));
        skin.add("small", new Label.LabelStyle(small, MUTED));
        skin.add("kicker", new Label.LabelStyle(small, ACCENT));
        skin.add("muted", new Label.LabelStyle(small, MUTED));

        SkinSpecification buttons = SkinXMLHandler.getSkin(SkinSpecification.UIComponents.PhaseDisplayButton.getComp(), false, true);
        SkinSpecification phase = SkinXMLHandler.getSkin(SkinSpecification.UIComponents.PhaseDisplay.getComp());
        // The fallback is the shipped skin shown by the classic client, including its authored pressed state.
        Drawable normal = buttonArtwork("button-normal", buttons, 0, "Bloodwolf/Default/button/buttonNormal.png");
        Drawable pressed = buttonArtwork("button-pressed", buttons, 1, "Bloodwolf/Default/button/buttonPushed.png");
        NinePatchDrawable inset = recessedWell("inset-well", false);
        Drawable focused = focus(normal);
        Drawable panel = panelArtwork(phase);
        skin.add("panel", panel, Drawable.class);
        skin.add("bar", panel, Drawable.class);
        skin.add("inset", inset, Drawable.class);
        skin.add("rule", skin.newDrawable("white", Color.valueOf("5C6668")), Drawable.class);

        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle();
        button.font = font;
        button.fontColor = fontColor(buttons, 0, TEXT);
        button.disabledFontColor = fontColor(buttons, 1, DISABLED);
        button.overFontColor = fontColor(buttons, 2, TEXT);
        button.up = normal;
        button.over = focused;
        button.down = pressed;
        button.checked = focus(pressed);
        button.checkedOver = focused;
        button.disabled = normal;
        skin.add("default", button);
        TextButton.TextButtonStyle iconButton = new TextButton.TextButtonStyle(button);
        iconButton.up = inset;
        iconButton.over = focus(inset);
        iconButton.down = inset;
        skin.add("icon", iconButton);
        TextButton.TextButtonStyle toolbar = new TextButton.TextButtonStyle(button);
        toolbar.font = small;
        skin.add("toolbar", toolbar);
        TextButton.TextButtonStyle primary = new TextButton.TextButtonStyle(button);
        primary.font = bold;
        SkinSpecification completion = SkinXMLHandler.getSkin(SkinSpecification.UIComponents.PhaseDisplayDoneButton.getComp(), false, true);
        primary.up = completion == buttons ? normal
              : buttonArtwork("done-normal", completion, 0, "Bloodwolf/Default/button/buttonNormal.png");
        primary.down = completion == buttons ? pressed
              : buttonArtwork("done-pressed", completion, 1, "Bloodwolf/Default/button/buttonPushed.png");
        primary.fontColor = fontColor(completion, 0, TEXT);
        primary.disabledFontColor = fontColor(completion, 1, DISABLED);
        primary.overFontColor = fontColor(completion, 2, TEXT);
        primary.over = focus(primary.up);
        primary.checkedOver = primary.over;
        primary.checked = primary.down;
        primary.disabled = primary.up;
        skin.add("primary", primary);
        TextButton.TextButtonStyle action = new TextButton.TextButtonStyle(button);
        action.font = bold;
        skin.add("action", action);

        ScrollPane.ScrollPaneStyle scrolling = new ScrollPane.ScrollPaneStyle();
        scrolling.vScroll = skin.newDrawable("white", Color.valueOf("151A1B"));
        scrolling.vScrollKnob = new TextureRegionDrawable(skin.get("button-normal", Texture.class));
        scrolling.vScroll.setMinWidth(7);
        scrolling.vScrollKnob.setMinWidth(7);
        scrolling.vScrollKnob.setMinHeight(28);
        skin.add("default", scrolling);
        NinePatchDrawable input = new NinePatchDrawable(inset);
        input.setLeftWidth(38);
        NinePatchDrawable focusedInput = recessedWell("focused-input-well", true);
        focusedInput.setLeftWidth(38);
        TextField.TextFieldStyle field = new TextField.TextFieldStyle(font, TEXT,
              skin.newDrawable("white", ACCENT), skin.newDrawable("white", Color.valueOf("526168")), input);
        field.focusedBackground = focusedInput;
        field.messageFont = small;
        field.messageFontColor = MUTED;
        skin.add("default", field);
        Slider.SliderStyle slider = new Slider.SliderStyle();
        slider.background = skin.newDrawable("white", Color.valueOf("242B2D"));
        slider.background.setMinHeight(3);
        slider.knobBefore = skin.newDrawable("white", Color.valueOf("839294"));
        slider.knobBefore.setMinHeight(3);
        slider.knob = new TextureRegionDrawable(skin.get("button-normal", Texture.class));
        slider.knob.setMinWidth(14);
        slider.knob.setMinHeight(18);
        skin.add("default", slider);
        TextTooltip.TextTooltipStyle tooltip = new TextTooltip.TextTooltipStyle(
              skin.get("small", Label.LabelStyle.class), skin.getDrawable("panel"));
        tooltip.wrapWidth = 340;
        skin.add("default", tooltip);
        for (String icon : List.of("target", "move", "group", "orders", "info", "unit", "hex", "close",
              "search", "arrow", "lock")) {
            icon(icon);
        }
    }

    private static Color fontColor(SkinSpecification spec, int index, Color fallback) {
        return !spec.hasBackgrounds() || spec.fontColors.size() <= index ? fallback
              : new Color((spec.fontColors.get(index).getRGB() << 8) | 0xff);
    }

    /** A matte recessed surface, with a thin steel lip instead of a raised button's chrome. */
    private NinePatchDrawable recessedWell(String name, boolean focused) {
        Pixmap pixels = new Pixmap(12, 12, Pixmap.Format.RGBA8888);
        pixels.setColor(Color.valueOf(focused ? "849799" : "4A5456"));
        pixels.fillRectangle(1, 1, 10, 10);
        pixels.setColor(Color.valueOf("080B0C"));
        pixels.fillRectangle(2, 2, 8, 8);
        pixels.setColor(Color.valueOf("161C1E"));
        pixels.fillRectangle(3, 4, 6, 5);
        pixels.setColor(Color.valueOf("263033"));
        pixels.drawLine(3, 9, 8, 9);
        Texture texture = new Texture(pixels);
        pixels.dispose();
        skin.add(name, texture);
        NinePatchDrawable drawable = new NinePatchDrawable(new NinePatch(texture, 4, 4, 4, 4));
        drawable.setMinWidth(0);
        drawable.setMinHeight(0);
        drawable.setLeftWidth(12);
        drawable.setRightWidth(12);
        drawable.setTopHeight(8);
        drawable.setBottomHeight(8);
        return drawable;
    }

    private BitmapFont font(String name, String path, int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(new FileHandle(new File(Configuration.fontsDir(), path)));
        generators.add(generator);
        FreeTypeFontGenerator.FreeTypeFontParameter settings = new FreeTypeFontGenerator.FreeTypeFontParameter();
        settings.size = size * 4;
        settings.incremental = true;
        settings.minFilter = Texture.TextureFilter.Linear;
        settings.magFilter = Texture.TextureFilter.Linear;
        BitmapFont font = generator.generateFont(settings);
        font.getData().setScale(0.25f);
        font.setUseIntegerPositions(false);
        skin.add(name, font);
        return font;
    }

    private Drawable buttonArtwork(String name, SkinSpecification spec, int index, String fallback) {
        Texture texture = texture(name, spec.backgrounds.size() > index ? spec.backgrounds.get(index) : fallback);
        // Keep the authored chrome at a fixed thickness. Stretching the entire bitmap would move its
        // visual inner edge underneath the text as a command row grows taller or wider.
        BaseDrawable drawable;
        if (spec.hasBackgrounds() && spec.tileBackground) {
            drawable = new TiledDrawable(new TextureRegion(texture));
        } else {
            int x = Math.min(18, (texture.getWidth() - 1) / 2);
            int y = Math.min(18, (texture.getHeight() - 1) / 2);
            NinePatch patch = new NinePatch(texture, x, x, y, y);
            patch.scale(0.5f, 0.5f);
            drawable = new NinePatchDrawable(patch);
        }
        drawable.setMinWidth(0);
        drawable.setMinHeight(0);
        drawable.setLeftWidth(14);
        drawable.setRightWidth(14);
        drawable.setTopHeight(7);
        drawable.setBottomHeight(7);
        return drawable;
    }

    private Drawable panelArtwork(SkinSpecification spec) {
        Texture background = texture("panel-background", spec.backgrounds.isEmpty()
              ? "Bloodwolf/Parts/phase/background.png" : spec.backgrounds.getFirst());
        TiledDrawable tiled = new TiledDrawable(new TextureRegion(background));
        // Reuse the classic border painter so the skin XML remains the source of truth for corners and edges.
        MegaMekBorder border = new MegaMekBorder(spec);
        BufferedImage borderImage = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = borderImage.createGraphics();
        try {
            border.paintBorder(null, graphics, 0, 0, 96, 96);
        } finally {
            graphics.dispose();
        }
        Insets insets = border.getBorderInsets(null);
        NinePatch frame = new NinePatch(texture("panel-frame", borderImage),
              Math.clamp(insets.left, 1, 32), Math.clamp(insets.right, 1, 32),
              Math.clamp(insets.top, 1, 32), Math.clamp(insets.bottom, 1, 32));
        BaseDrawable drawable = new BaseDrawable() {
            @Override
            public void draw(Batch batch, float x, float y, float width, float height) {
                float previous = batch.getPackedColor();
                Color tint = batch.getColor();
                batch.setColor(tint.r * 0.62f, tint.g * 0.62f, tint.b * 0.62f, tint.a);
                tiled.draw(batch, x, y, width, height);
                batch.setPackedColor(previous);
                frame.draw(batch, x, y, width, height);
            }
        };
        drawable.setLeftWidth(12);
        drawable.setRightWidth(12);
        drawable.setTopHeight(10);
        drawable.setBottomHeight(10);
        return drawable;
    }

    private Texture texture(String name, String path) {
        File file = new MegaMekFile(Configuration.widgetsDir(), path).getFile();
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new IOException("Unsupported skin image: " + file);
            }
            return texture(name, image);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load MegaMek skin artwork: " + file, exception);
        }
    }

    private Texture texture(String name, BufferedImage image) {
        Pixmap pixels = new Pixmap(image.getWidth(), image.getHeight(), Pixmap.Format.RGBA8888);
        pixels.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                pixels.drawPixel(x, y, (argb << 8) | ((argb >>> 24) & 0xff));
            }
        }
        Texture texture = new Texture(pixels);
        pixels.dispose();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        skin.add(name, texture);
        return texture;
    }

    /** A restrained silver inset marks hover/keyboard focus without replacing the original artwork. */
    private Drawable focus(Drawable background) {
        return new BaseDrawable(background) {
            @Override
            public void draw(Batch batch, float x, float y, float width, float height) {
                background.draw(batch, x, y, width, height);
                float previous = batch.getPackedColor();
                Color tint = batch.getColor();
                batch.setColor(tint.r * 0.82f, tint.g * 0.9f, tint.b * 0.91f, tint.a * 0.75f);
                Texture white = skin.get("white", Texture.class);
                batch.draw(white, x + 7, y + 5, Math.max(0, width - 14), 1);
                batch.draw(white, x + 7, y + height - 6, Math.max(0, width - 14), 1);
                batch.setPackedColor(previous);
            }
        };
    }

    /** Additional command symbols share the metal skin's bright upper edge and recessed lower edge. */
    private void icon(String name) {
        Pixmap pixels = new Pixmap(48, 48, Pixmap.Format.RGBA8888);
        pixels.setColor(Color.WHITE);
        switch (name) {
            case "target" -> {
                pixels.drawCircle(24, 24, 13);
                pixels.drawCircle(24, 24, 12);
                stroke(pixels, 24, 4, 24, 16);
                stroke(pixels, 24, 32, 24, 44);
                stroke(pixels, 4, 24, 16, 24);
                stroke(pixels, 32, 24, 44, 24);
                pixels.fillCircle(24, 24, 2);
            }
            case "move", "unit" -> {
                stroke(pixels, 9, 25, 24, 10);
                stroke(pixels, 24, 10, 39, 25);
                stroke(pixels, 9, 38, 24, 23);
                stroke(pixels, 24, 23, 39, 38);
            }
            case "group", "orders" -> {
                for (int y = 12; y <= 36; y += 12) {
                    pixels.fillRectangle(7, y - 2, 5, 5);
                    stroke(pixels, 19, y, 40, y);
                }
            }
            case "info" -> {
                pixels.drawCircle(24, 24, 18);
                pixels.drawCircle(24, 24, 17);
                pixels.fillCircle(24, 14, 2);
                pixels.fillRectangle(22, 22, 4, 13);
            }
            case "hex" -> {
                int[] x = { 5, 14, 34, 43, 34, 14 };
                int[] y = { 24, 7, 7, 24, 41, 41 };
                for (int i = 0; i < 6; i++) {
                    stroke(pixels, x[i], y[i], x[(i + 1) % 6], y[(i + 1) % 6]);
                }
                pixels.fillCircle(24, 24, 3);
            }
            case "close" -> {
                stroke(pixels, 13, 13, 35, 35);
                stroke(pixels, 13, 35, 35, 13);
            }
            case "search" -> {
                pixels.drawCircle(20, 20, 12);
                pixels.drawCircle(20, 20, 11);
                stroke(pixels, 29, 29, 41, 41);
            }
            case "lock" -> {
                pixels.drawCircle(24, 18, 9);
                pixels.drawCircle(24, 18, 8);
                pixels.fillRectangle(12, 20, 25, 19);
                pixels.setColor(Color.CLEAR);
                pixels.setBlending(Pixmap.Blending.None);
                pixels.fillRectangle(23, 25, 3, 8);
            }
            default -> {
                stroke(pixels, 18, 10, 32, 24);
                stroke(pixels, 32, 24, 18, 38);
            }
        }
        Pixmap metal = new Pixmap(48, 48, Pixmap.Format.RGBA8888);
        for (int y = 1; y < 47; y++) {
            for (int x = 1; x < 47; x++) {
                if ((pixels.getPixel(x, y) & 0xff) != 0) {
                    boolean highlight = (pixels.getPixel(x, y - 1) & 0xff) == 0;
                    boolean shadow = (pixels.getPixel(x, y + 1) & 0xff) == 0;
                    metal.drawPixel(x, y, highlight ? 0xF2F4F4FF : shadow ? 0x586568FF : 0xBCC8CAFF);
                }
            }
        }
        Texture texture = new Texture(metal);
        metal.dispose();
        pixels.dispose();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        skin.add("icon-" + name + "-texture", texture);
        skin.add("icon-" + name, new TextureRegionDrawable(texture), Drawable.class);
    }

    private static void stroke(Pixmap pixels, int x1, int y1, int x2, int y2) {
        pixels.drawLine(x1, y1, x2, y2);
        pixels.drawLine(x1 + 1, y1, x2 + 1, y2);
        pixels.drawLine(x1, y1 + 1, x2, y2 + 1);
    }

    @Override
    public void dispose() {
        skin.dispose();
        generators.forEach(FreeTypeFontGenerator::dispose);
    }
}

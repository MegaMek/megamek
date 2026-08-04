/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.panels.battlefieldSupport;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Vector;
import java.util.function.Supplier;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.WrapLayout;
import megamek.client.ui.util.FontHandler;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.battlefieldSupport.cardDrawer.BattlefieldSupportCard.ColorMode;

/**
 * A JPanel that displays a Battlefield Support (BFS) Asset card together with controls to configure the display (font,
 * scale, color mode) and to copy the card image to the clipboard. The asset to display can be changed after
 * construction and may be {@code null}. Modeled on {@code ConfigurableASCardPanel}.
 */
public class ConfigurableBFSCardPanel extends JPanel {

    /**
     * Optional application-supplied defaults for a newly created panel's font and color mode. MegaMekLab registers
     * these at startup so its editor and unit selector default the card to the current record-sheet settings; plain
     * MegaMek leaves them unset and the panel uses its built-in defaults (sans-serif font, black-and-white).
     */
    private static Supplier<String> defaultFontProvider;
    private static Supplier<ColorMode> defaultColorProvider;
    private static Supplier<Color> defaultDamageColorProvider;

    /**
     * Registers the application defaults applied to every {@code ConfigurableBFSCardPanel} created afterward. Pass
     * {@code null} for any provider to leave that aspect at its built-in default.
     *
     * @param fontProvider        supplies the default font family name, or {@code null}
     * @param colorProvider       supplies the default color mode, or {@code null}
     * @param damageColorProvider supplies the color for a damaged asset's current Destroy Check, or {@code null}
     */
    public static void setDefaultProviders(@Nullable Supplier<String> fontProvider,
          @Nullable Supplier<ColorMode> colorProvider, @Nullable Supplier<Color> damageColorProvider) {
        defaultFontProvider = fontProvider;
        defaultColorProvider = colorProvider;
        defaultDamageColorProvider = damageColorProvider;
    }

    private final JComboBox<String> fontChooser;
    private final JComboBox<Float> sizeChooser = new JComboBox<>();
    private final JComboBox<ColorMode> colorChooser = new JComboBox<>(ColorMode.values());
    private final JButton copyButton = new JButton(Messages.getString("CASCardPanel.copyCard"));
    private final BFSCardPanel cardPanel = new BFSCardPanel();
    private final JPanel menuPanel;
    private final boolean fitToPanel;

    /** Constructs a panel without an asset to display. */
    public ConfigurableBFSCardPanel() {
        this(false, null);
    }

    /**
     * Constructs a panel displaying the given asset.
     *
     * @param asset the Battlefield Support Asset to display, or {@code null}
     */
    public ConfigurableBFSCardPanel(@Nullable BattlefieldSupportAsset asset) {
        this(false, asset);
    }

    /**
     * Constructs a panel displaying the given asset.
     *
     * @param fitToPanel if {@code true}, the card is scaled to fill the panel (and the size picker is hidden); if
     *                   {@code false}, the card uses a fixed size selectable from the size picker
     * @param asset      the Battlefield Support Asset to display, or {@code null}
     */
    public ConfigurableBFSCardPanel(boolean fitToPanel, @Nullable BattlefieldSupportAsset asset) {
        this.fitToPanel = fitToPanel;

        fontChooser = new JComboBox<>(new Vector<>(FontHandler.getAvailableNonSymbolFonts()));
        fontChooser.addItem("");
        fontChooser.addActionListener(ev -> updateFont());

        sizeChooser.addItem(2f);
        sizeChooser.addItem(1.5f);
        sizeChooser.addItem(1f);
        sizeChooser.addItem(0.75f);
        sizeChooser.addItem(0.5f);
        sizeChooser.addItem(0.33f);
        sizeChooser.setSelectedItem(0.75f);
        sizeChooser.addActionListener(ev -> updateSize());
        sizeChooser.setRenderer((list, value, index, isSelected, cellHasFocus) -> new JLabel(Float.toString(value)));

        colorChooser.setSelectedItem(ColorMode.NONE);
        colorChooser.addActionListener(ev -> updateColor());
        colorChooser.setRenderer((list, value, index, isSelected, cellHasFocus) ->
              new JLabel(colorModeLabel(value)));

        copyButton.addActionListener(ev -> copyCardToClipboard());

        menuPanel = new UIUtil.FixedYPanel(new WrapLayout(FlowLayout.LEFT, 15, 10));
        JPanel fontChooserPanel = new JPanel();
        fontChooserPanel.add(new JLabel(Messages.getString("CASCardPanel.font")));
        fontChooserPanel.add(fontChooser);
        JPanel colorChooserPanel = new JPanel();
        colorChooserPanel.add(new JLabel(Messages.getString("ConfigurableBFSCardPanel.color")));
        colorChooserPanel.add(colorChooser);
        menuPanel.add(fontChooserPanel);
        if (!fitToPanel) {
            JPanel sizeChooserPanel = new JPanel();
            sizeChooserPanel.add(new JLabel(Messages.getString("CASCardPanel.cardSize")));
            sizeChooserPanel.add(sizeChooser);
            menuPanel.add(sizeChooserPanel);
        }
        menuPanel.add(colorChooserPanel);
        menuPanel.add(copyButton);

        if (fitToPanel) {
            // The card scales itself to fill the available area; no scroll pane, and the menu sits above it.
            cardPanel.setFitToPanel(true);
            setLayout(new BorderLayout());
            add(menuPanel, BorderLayout.NORTH);
            add(cardPanel, BorderLayout.CENTER);
        } else {
            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            var cardLine = new JScrollPane(cardPanel);
            cardLine.getVerticalScrollBar().setUnitIncrement(16);
            add(menuPanel);
            add(cardLine);
            updateSize();
        }
        applyDefaultProviders();
        setAsset(asset);
    }

    /** Applies any application-registered default font/color to this panel (see {@link #setDefaultProviders}). */
    private void applyDefaultProviders() {
        if (defaultColorProvider != null) {
            setColorMode(defaultColorProvider.get());
        }
        if (defaultFontProvider != null) {
            setCardFont(defaultFontProvider.get());
        }
        if (defaultDamageColorProvider != null) {
            cardPanel.setDamageColor(defaultDamageColorProvider.get());
        }
    }

    /**
     * Sets the panel to display the given asset.
     *
     * @param asset the Battlefield Support Asset to display, or {@code null}
     */
    public void setAsset(@Nullable BattlefieldSupportAsset asset) {
        cardPanel.setAsset(asset);
        copyButton.setEnabled(asset != null);
    }

    private void updateFont() {
        String selectedItem = (String) fontChooser.getSelectedItem();
        if ((selectedItem == null) || selectedItem.isBlank()) {
            cardPanel.setCardFont(new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 14));
        } else {
            cardPanel.setCardFont(Font.decode(selectedItem));
        }
    }

    private void updateSize() {
        if (sizeChooser.getSelectedItem() != null) {
            cardPanel.setScale((Float) sizeChooser.getSelectedItem());
        }
    }

    private void updateColor() {
        ColorMode mode = (ColorMode) colorChooser.getSelectedItem();
        cardPanel.setColorMode(mode != null ? mode : ColorMode.NONE);
    }

    /**
     * Sets the color mode shown by the card and selected in the color picker. Callers (for example MegaMekLab) use this
     * to seed the default from their own settings; the user may still change it afterward.
     *
     * @param mode the color mode to default to
     */
    public void setColorMode(ColorMode mode) {
        colorChooser.setSelectedItem(mode != null ? mode : ColorMode.NONE);
    }

    /**
     * Sets the font shown by the card and selected in the font picker. Callers (for example MegaMekLab) use this to
     * seed the default from their own settings; the user may still change it afterward. A blank or null name selects
     * the default sans-serif font.
     *
     * @param fontName the font family name to default to
     */
    public void setCardFont(@Nullable String fontName) {
        fontChooser.setSelectedItem((fontName == null) ? "" : fontName);
    }

    private static String colorModeLabel(ColorMode mode) {
        return switch (mode) {
            case NONE -> Messages.getString("ConfigurableBFSCardPanel.color.none");
            case LOGO_ONLY -> Messages.getString("ConfigurableBFSCardPanel.color.logoOnly");
            case ALL -> Messages.getString("ConfigurableBFSCardPanel.color.full");
        };
    }

    private void copyCardToClipboard() {
        Image image = cardPanel.getCardImage();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new CardImageTransferable(image), null);
    }

    public void toggleMenu(boolean menuVisible) {
        menuPanel.setVisible(menuVisible);
    }

    /** A minimal {@link Transferable} that puts a single image onto the clipboard. */
    private record CardImageTransferable(Image image) implements Transferable {

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { DataFlavor.imageFlavor };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!DataFlavor.imageFlavor.equals(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }
}

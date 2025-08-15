/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panels.alphaStrike;

import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.List;
import java.util.Vector;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.WrapLayout;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.dialogs.abstractDialogs.ASConversionInfoDialog;
import megamek.client.ui.util.FontHandler;
import megamek.client.ui.util.UIUtil;
import megamek.common.alphaStrike.ASCardDisplayable;
import megamek.common.alphaStrike.ASStatsExporter;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.cardDrawer.ASCardPrinter;
import megamek.common.annotations.Nullable;

/**
 * This is a JPanel that displays an AlphaStrike unit card and elements to configure the display of the card and allow
 * copying to clipboard. The AlphaStrike element to display can be changed after construction and it may be null.
 */
public class ConfigurableASCardPanel extends JPanel {

    private final JComboBox<String> fontChooser;
    private final JComboBox<Float> sizeChooser = new JComboBox<>();
    private final JButton copyButton = new JButton(Messages.getString("CASCardPanel.copyCard"));
    private final JButton copyStatsButton = new JButton(Messages.getString("CASCardPanel.copyStats"));
    private final JButton printButton = new JButton(Messages.getString("CASCardPanel.printCard"));
    private final JButton mulButton = new JButton(Messages.getString("CASCardPanel.MUL"));
    private final JButton conversionButton = new JButton(Messages.getString("CASCardPanel.conversionReport"));
    private final ASCardPanel cardPanel = new ASCardPanel();
    private ASCardDisplayable element;
    private int mulId;
    private final JFrame parent;
    private final JPanel menuPanel;

    /**
     * Constructs a panel with the given AlphaStrike element to display.
     *
     * @param element The AlphaStrike element to display
     */
    public ConfigurableASCardPanel(@Nullable ASCardDisplayable element, JFrame parent) {
        this.parent = parent;
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        fontChooser = new JComboBox<>(new Vector<>(FontHandler.getAvailableNonSymbolFonts()));
        fontChooser.addItem("");
        fontChooser.addActionListener(ev -> updateFont());
        fontChooser.setSelectedItem(GUIPreferences.getInstance().getAsCardFont());

        sizeChooser.addItem(2f);
        sizeChooser.addItem(1.5f);
        sizeChooser.addItem(1f);
        sizeChooser.addItem(0.75f);
        sizeChooser.addItem(0.5f);
        sizeChooser.addItem(0.33f);
        sizeChooser.setSelectedItem(GUIPreferences.getInstance().getAsCardSize());
        sizeChooser.addActionListener(ev -> updateSize());
        sizeChooser.setRenderer((list, value, index, isSelected, cellHasFocus) -> new JLabel(Float.toString(value)));

        copyButton.addActionListener(ev -> copyCardToClipboard());
        copyStatsButton.addActionListener(ev -> copyStats());
        printButton.addActionListener(ev -> printCard());

        mulButton.addActionListener(ev -> UIUtil.showMUL(mulId, this));
        mulButton.setToolTipText("Show the Master Unit List entry for this unit. Opens a browser window.");

        conversionButton.addActionListener(e -> showConversionReport());

        menuPanel = new UIUtil.FixedYPanel(new WrapLayout(FlowLayout.LEFT, 15, 10));
        JPanel fontChooserPanel = new JPanel();
        fontChooserPanel.add(new JLabel(Messages.getString("CASCardPanel.font")));
        fontChooserPanel.add(fontChooser);
        JPanel sizeChooserPanel = new JPanel();
        sizeChooserPanel.add(new JLabel(Messages.getString("CASCardPanel.cardSize")));
        sizeChooserPanel.add(sizeChooser);
        menuPanel.add(fontChooserPanel);
        menuPanel.add(sizeChooserPanel);
        menuPanel.add(copyButton);
        menuPanel.add(copyStatsButton);
        menuPanel.add(printButton);
        menuPanel.add(mulButton);
        menuPanel.add(conversionButton);

        var cardLine = new JScrollPane(cardPanel);
        cardLine.getVerticalScrollBar().setUnitIncrement(16);

        add(menuPanel);
        add(cardLine);
        updateSize();
        setASElement(element);
    }

    /** Construct a new panel without an AlphaStrike element to display. */
    public ConfigurableASCardPanel(JFrame parent) {
        this(null, parent);
    }

    /**
     * Set the panel to display the given element.
     *
     * @param element The AlphaStrike element to display
     */
    public void setASElement(@Nullable ASCardDisplayable element) {
        this.element = element;
        cardPanel.setASElement(element);
        mulId = (element != null) ? element.getMulId() : -1;
        mulButton.setEnabled(mulId > 0);
        copyStatsButton.setEnabled(element != null);
        copyButton.setEnabled(element != null);
        printButton.setEnabled(element != null);
        conversionButton.setEnabled(element instanceof AlphaStrikeElement);
    }

    /** Set the card to use a newly selected font. */
    private void updateFont() {
        String selectedItem = (String) fontChooser.getSelectedItem();
        if ((selectedItem == null) || selectedItem.isBlank()) {
            cardPanel.setCardFont(new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 14));
        } else {
            cardPanel.setCardFont(Font.decode(selectedItem));
            GUIPreferences.getInstance().setAsCardFont(selectedItem);
        }
    }

    /** Set the card to use a newly selected scale. */
    private void updateSize() {
        if (sizeChooser.getSelectedItem() != null) {
            cardPanel.setScale((Float) sizeChooser.getSelectedItem());
            GUIPreferences.getInstance().setAsCardSize((Float) sizeChooser.getSelectedItem());
        }
    }

    private void showConversionReport() {
        if (element instanceof AlphaStrikeElement) {
            var dialog = new ASConversionInfoDialog(parent,
                  ((AlphaStrikeElement) element).getConversionReport(),
                  element);
            dialog.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
            dialog.setVisible(true);
        }
    }

    private void printCard() {
        new ASCardPrinter(List.of(element), parent).printCards();
    }

    private void copyStats() {
        var statsExporter = new ASStatsExporter(element);
        StringSelection stringSelection = new StringSelection(statsExporter.getStats());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    // Taken from https://alvinalexander.com/java/java-copy-image-to-clipboard-example/
    private void copyCardToClipboard() {
        ImageSelection imgSel = new ImageSelection(cardPanel.getCardImage());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imgSel, null);
    }

    static class ImageSelection implements Transferable {

        private final Image image;

        public ImageSelection(Image image) {
            this.image = image;
        }

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

    public void toggleMenu(boolean menuVisible) {
        menuPanel.setVisible(menuVisible);
    }
}

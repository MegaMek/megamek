/*
 * Copyright (c) 2022-2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.alphaStrike;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.WrapLayout;
import megamek.client.ui.dialogs.ASConversionInfoDialog;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.FontHandler;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.alphaStrike.ASCardDisplayable;
import megamek.common.alphaStrike.ASStatsExporter;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.cardDrawer.ASCardPrinter;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.util.List;
import java.util.Vector;

/**
 * This is a JPanel that displays an AlphaStrike unit card and elements to configure the display of
 * the card and allow copying to clipboard. The AlphaStrike element to display can be changed after
 * construction and it may be null.
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

        var chooserLine = new UIUtil.FixedYPanel(new WrapLayout(FlowLayout.LEFT, 15, 10));
        JPanel fontChooserPanel = new JPanel();
        fontChooserPanel.add(new JLabel(Messages.getString("CASCardPanel.font")));
        fontChooserPanel.add(fontChooser);
        JPanel sizeChooserPanel = new JPanel();
        sizeChooserPanel.add(new JLabel(Messages.getString("CASCardPanel.cardSize")));
        sizeChooserPanel.add(sizeChooser);
        chooserLine.add(fontChooserPanel);
        chooserLine.add(sizeChooserPanel);
        chooserLine.add(copyButton);
        chooserLine.add(copyStatsButton);
        chooserLine.add(printButton);
        chooserLine.add(mulButton);
        chooserLine.add(conversionButton);

        var cardLine = new JScrollPane(cardPanel);
        cardLine.getVerticalScrollBar().setUnitIncrement(16);

        add(chooserLine);
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
            var dialog = new ASConversionInfoDialog(parent, ((AlphaStrikeElement) element).getConversionReport(), element);
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
}
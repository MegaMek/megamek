/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
import megamek.client.ui.dialogs.ASConversionInfoDialog;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.alphaStrike.ASCardDisplayable;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.net.URL;

/**
 * This is a JPanel that displays an AlphaStrike unit card and elements to configure the display of
 * the card and allow copying to clipboard. The AlphaStrike element to display can be changed after
 * construction and it may be null.
 */
public class ConfigurableASCardPanel extends JPanel {

    private final JComboBox<String> fontChooser = new JComboBox<>();
    private final JComboBox<Float> sizeChooser = new JComboBox<>();
    private final JButton copyButton = new JButton("Copy to Clipboard");
    private final JButton mulButton = new JButton("MUL");
    private final JButton conversionButton = new JButton("Conversion Report");
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

        fontChooser.addItem("");
        for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            fontChooser.addItem(family);
        }
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

        mulButton.addActionListener(ev -> showMUL());
        mulButton.setToolTipText("Show the Master Unit List entry for this unit. Opens a browser window.");

        conversionButton.addActionListener(e -> showConversionReport());

        var chooserLine = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        chooserLine.setBorder(new EmptyBorder(10, 0, 10, 0));
        chooserLine.add(Box.createHorizontalStrut(15));
        chooserLine.add(new JLabel("Font: "));
        chooserLine.add(fontChooser);
        chooserLine.add(Box.createHorizontalStrut(15));
        chooserLine.add(new JLabel("Card Size: "));
        chooserLine.add(sizeChooser);
        chooserLine.add(Box.createHorizontalStrut(15));
        chooserLine.add(copyButton);
        chooserLine.add(Box.createHorizontalStrut(15));
        chooserLine.add(mulButton);
        chooserLine.add(Box.createHorizontalStrut(15));
        chooserLine.add(conversionButton);

        var cardLine = new JScrollPane(cardPanel);
        cardPanel.setBorder(new EmptyBorder(5, 65, 0, 0));
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
        conversionButton.setEnabled(element instanceof AlphaStrikeElement);
    }

    /** Set the card to use a newly selected font. */
    private void updateFont() {
        String selectedItem = (String) fontChooser.getSelectedItem();
        if ((selectedItem == null) || selectedItem.isBlank()) {
            cardPanel.setCardFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
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

    private void showMUL() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URL(MMConstants.MUL_URL_PREFIX + mulId).toURI());
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showConversionReport() {
        if (element instanceof AlphaStrikeElement) {
            var dialog = new ASConversionInfoDialog(parent, ((AlphaStrikeElement) element).getConversionReport(), element);
            dialog.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
            dialog.setVisible(true);
        }
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
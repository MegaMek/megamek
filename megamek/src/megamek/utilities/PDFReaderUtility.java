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
package megamek.utilities;

import static megamek.common.internationalization.I18n.getTextAt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;

import megamek.client.ui.util.UIUtil;
import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * A Swing-based dialog that enables users to view and navigate PDF documents.
 *
 * <p>This utility leverages the {@code PDFBox} library to render each page of a PDF as an image, providing a seamless,
 * scrollable reading experience even for multipage documents. The dialog offers intuitive zoom controls, including zoom
 * in, zoom out, and reset to the default zoom. It also manages progress feedback for loading and rendering operations,
 * ensuring a responsive and user-friendly interface.</p>
 *
 * <p><b>Features</b></p>
 * <ul>
 *     <li>Open and display multipage PDF files in a dedicated dialog</li>
 *     <li>Render each page as a Swing image component for fast display and scrolling</li>
 *     <li>Zoom in and out with predefined DPI steps and boundaries</li>
 *     <li>Reset zoom to the default DPI value</li>
 *     <li>Progress dialog for time-consuming operations</li>
 *     <li>Thread-safe UI updates using SwingWorker</li>
 *     <li>Automatic closing & resource cleanup on dialog disposal</li>
 * </ul>
 *
 * <p><b>Typical usage:</b></p>
 * {@code PDFReaderUtility pdfViewer = new PDFReaderUtility(parentFrame, "/path/to/File.pdf", "My PDF Viewer");}
 *
 * @author Illiani
 * @since 0.50.07
 */
public class PDFReaderUtility extends JDialog {
    private static final String RESOURCE_BUNDLE = "megamek/common/PDFReaderUtility";
    static MMLogger LOGGER = MMLogger.create(PDFReaderUtility.class);

    private static final int PADDING = UIUtil.scaleForGUI(10);
    private static final Dimension DEFAULT_DIALOG_SIZE = UIUtil.scaleForGUI(1350, 800);
    private static final Dimension BUTTON_SIZE = UIUtil.scaleForGUI(100, 30);

    private static final int DEFAULT_DPI = 150;
    private static final int MIN_DPI = 50;
    private static final int MAX_DPI = 600;
    private static final int ZOOM_STEP = 25;
    private int currentDpi = DEFAULT_DPI;

    private PDDocument document;
    private PDFRenderer renderer;
    private JPanel pnlPages;
    private final List<JLabel> lblPages = new ArrayList<>();
    private final JButton zoomInButton;
    private final JButton zoomOutButton;
    private final JButton resetZoomButton;
    private JDialog progressDialog;


    /**
     * Constructs and displays a modal dialog for viewing a PDF file.
     *
     * <p>The dialog is initialized with zoom controls and begins rendering the pages of the provided PDF document
     * path. Operations that may block the UI, such as rendering or file IO, are performed off the Event Dispatch
     * Thread. The dialog appears in the center of the specified parent window.</p>
     *
     * @param parent  the parent window (can be null for a top-level dialog)
     * @param pdfPath file system path of the PDF document to display
     * @param title   dialog window title
     *
     * @author Illiani
     * @since 0.50.07
     */
    public PDFReaderUtility(Window parent, String pdfPath, String title) {
        super(parent, title);

        zoomInButton = getButton(getTextAt(RESOURCE_BUNDLE, "PDFReaderUtility.button.zoomIn"));
        zoomInButton.setSize(BUTTON_SIZE);

        zoomOutButton = getButton(getTextAt(RESOURCE_BUNDLE, "PDFReaderUtility.button.zoomOut"));
        zoomOutButton.setSize(BUTTON_SIZE);

        resetZoomButton = getButton(getTextAt(RESOURCE_BUNDLE, "PDFReaderUtility.button.zoomReset"));
        resetZoomButton.setSize(BUTTON_SIZE);

        setSize(DEFAULT_DIALOG_SIZE);
        setLocationRelativeTo(parent);
        processDisplay(pdfPath);
        setVisible(true);
    }

    /**
     * Initiates loading and initial rendering of the PDF document given its file path.
     *
     * <p>Displays a modal progress dialog during processing and starts an asynchronous worker to load all pages and
     * display them as images in a panel. Handles error reporting to the user if the file cannot be opened or
     * rendered.</p>
     *
     * @param pdfPath absolute or relative path to the PDF file to process
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void processDisplay(String pdfPath) {
        showProgressDialog(getTextAt(RESOURCE_BUNDLE, "PDFReaderUtility.progressDialog.description"), true);

        SwingWorker<Void, PageUpdate> loaderWorker = getPageUpdateSwingWorker(pdfPath);
        loaderWorker.execute();
    }

    /**
     * Creates a {@link SwingWorker} that handles the loading of a PDF document and the rendering of each page as a
     * {@link BufferedImage} in the background.
     *
     * <p>Published images are added to the main display panel as soon as they become available, so the UI can
     * progressively show content for large documents. Upon completion, UI components (like zoom buttons and scroll
     * pane) are initialized and enabled.</p>
     *
     * @param pdfPath file path of the PDF document to load
     *
     * @return a {@link SwingWorker} that performs PDF loading and image rendering
     *
     * @author Illiani
     * @since 0.50.07
     */
    private SwingWorker<Void, PageUpdate> getPageUpdateSwingWorker(String pdfPath) {
        return new SwingWorker<>() {
            PDDocument tempDocument = null;

            @Override
            protected Void doInBackground() throws Exception {
                tempDocument = Loader.loadPDF(new File(pdfPath));
                if (tempDocument == null) {
                    throw new Exception("PDF document could not be loaded.");
                }
                renderer = new PDFRenderer(tempDocument);

                pnlPages = new JPanel();
                pnlPages.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
                pnlPages.setLayout(new BoxLayout(pnlPages, BoxLayout.Y_AXIS));

                // Pre-create labels for pages
                int numPages = tempDocument.getNumberOfPages();
                for (int i = 0; i < numPages; i++) {
                    BufferedImage pageImage = renderer.renderImageWithDPI(i, currentDpi);
                    publish(new PageUpdate(i, pageImage));
                }
                return null;
            }

            @Override
            protected void process(List<PageUpdate> chunks) {
                for (PageUpdate update : chunks) {
                    // On first run, create page label; otherwise update icon
                    if (lblPages.size() <= update.pageIndex) {
                        JLabel label = new JLabel(new ImageIcon(update.pageImage));
                        label.setBorder(BorderFactory.createEmptyBorder(PADDING, 0, 0, 0));
                        lblPages.add(label);
                        pnlPages.add(label);
                    } else {
                        lblPages.get(update.pageIndex).setIcon(new ImageIcon(update.pageImage));
                    }
                }
                pnlPages.revalidate();
                pnlPages.repaint();
            }

            @Override
            protected void done() {
                try {
                    get(); // Re-throw exception if occurred
                    JScrollPane scrollPane = new JScrollPane(pnlPages);
                    scrollPane.setBorder(BorderFactory.createCompoundBorder(
                          getBorder(),
                          BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING)
                    ));

                    JPanel zoomPanel = new JPanel();
                    zoomPanel.add(zoomInButton);
                    zoomPanel.add(zoomOutButton);
                    zoomPanel.add(resetZoomButton);

                    zoomInButton.addActionListener(e -> zoom(currentDpi + ZOOM_STEP));
                    zoomOutButton.addActionListener(e -> zoom(currentDpi - ZOOM_STEP));
                    resetZoomButton.addActionListener(e -> zoom(DEFAULT_DPI));

                    getContentPane().setLayout(new BorderLayout());
                    getContentPane().add(scrollPane, BorderLayout.CENTER);
                    getContentPane().add(zoomPanel, BorderLayout.NORTH);

                    PDFReaderUtility.this.document = tempDocument;

                    addWindowListener(new java.awt.event.WindowAdapter() {
                        public void windowClosing(java.awt.event.WindowEvent e) {
                            try {
                                if (tempDocument != null) {
                                    tempDocument.close();
                                }
                            } catch (Exception ignored) {}
                        }
                    });

                    getContentPane().revalidate();
                    getContentPane().repaint();

                } catch (Exception ex) {
                    LOGGER.error("Failed to load or render PDF: {}\n{}", pdfPath, ex);
                    JOptionPane.showMessageDialog(PDFReaderUtility.this,
                          "Could not open PDF:\n" + ex.getMessage(),
                          "Error",
                          JOptionPane.ERROR_MESSAGE);
                    dispose();
                } finally {
                    showProgressDialog(null, false);
                }
            }
        };
    }

    /**
     * Supplies the default border style for the page display area.
     *
     * <p>This provides a simple visual separation for the PDF viewing content, relying on a theme-tolerant gray
     * line.</p>
     *
     * <p><b>Usage:</b> This is a protected method that can be called with {@code @Override} to allow for adjustments
     * so that the border matches the 'look and feel' of the client it is called in. For example, MekHQ should replace
     * this with {@code RoundedLineBorder.createRoundedLineBorder()}.</p>
     *
     * @return a {@link Border} instance for content panels
     *
     * @author Illiani
     * @since 0.50.07
     */
    protected static Border getBorder() {
        return BorderFactory.createLineBorder(UIUtil.uiGray());
    }


    /**
     * Utility method to create a {@link JButton} with the given localized text label.
     *
     * <p><b>Usage:</b> This is a protected method that can be called with {@code @Override} to allow for adjustments
     * so that the button matches the 'look and feel' of the client it is called in. For example, MekHQ should replace
     * this with {@code RoundedJButton}.</p>
     *
     * @param buttonLabel the localized text to display on the button
     *
     * @return a new {@link JButton} instance with the specified text
     *
     * @author Illiani
     * @since 0.50.07
     */
    protected static JButton getButton(String buttonLabel) {
        return new JButton(buttonLabel);
    }

    /**
     * Updates the zoom level for rendering all pages of the currently loaded PDF document.
     *
     * <p>The method clamps the requested DPI (dots per inch) value within the allowable range and prevents
     * redundant re-rendering if the desired DPI matches the current setting.</p>
     *
     * <p>If the zoom action is required, all zoom control buttons are temporarily disabled, and a
     * {@link SwingWorker} is started to re-render each page at the new zoom level in the background. As each page
     * finishes rendering, its image is published and updated in the user interface. Once all pages are updated, the
     * zoom controls are re-enabled.</p>
     *
     * @param newDpi the requested DPI value for rendering; will be clamped to valid bounds.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void zoom(int newDpi) {
        newDpi = MathUtility.clamp(newDpi, MIN_DPI, MAX_DPI);

        if (newDpi == currentDpi) { // Prevent unnecessary processing
            return;
        }

        currentDpi = newDpi;

        setZoomButtonsEnabled(false);

        SwingWorker<Void, PageUpdate> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    BufferedImage pageImage = renderer.renderImageWithDPI(i, currentDpi);
                    publish(new PageUpdate(i, pageImage));
                }
                return null;
            }

            @Override
            protected void process(List<PageUpdate> chunks) {
                for (PageUpdate update : chunks) {
                    lblPages.get(update.pageIndex).setIcon(new ImageIcon(update.pageImage));
                }
                pnlPages.revalidate();
                pnlPages.repaint();
            }

            @Override
            protected void done() {
                setZoomButtonsEnabled(true);
            }
        };
        worker.execute();
    }

    /**
     * A data record used to pass image update information from a {@link SwingWorker} to the UI thread.
     *
     * <p>Each {@code PageUpdate} bundles the page index and the corresponding rendered image, so that the
     * {@link JLabel} objects showing each page can be updated efficiently.</p>
     *
     * @param pageIndex zero-based index of the page in the PDF
     * @param pageImage {@link BufferedImage} containing the rendered page content
     *
     * @author Illiani
     * @since 0.50.07
     */
    private record PageUpdate(int pageIndex, BufferedImage pageImage) {}

    /**
     * Toggles the enabled state of all zoom-related controls (zoom in, zoom out, and reset zoom).
     *
     * <p>This prevents user interaction during background operations such as image rendering, thereby avoiding
     * potential concurrency issues or inconsistent state in the UI.</p>
     *
     * @param enabled {@code true} if all zoom controls should be interactive; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void setZoomButtonsEnabled(boolean enabled) {
        zoomInButton.setEnabled(enabled);
        zoomOutButton.setEnabled(enabled);
        resetZoomButton.setEnabled(enabled);
    }

    /**
     * Shows or hides a modal progress dialog to indicate that a long-running operation, such as loading or re-rendering
     * all PDF pages, is in progress.
     *
     * <p>The dialog prevents user input to this window for the duration of the task.</p>
     *
     * @param message the message to display to the user while busy; ignored if hiding the dialog
     * @param show    {@code true} to show the progress dialog; {@code false} to hide it
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void showProgressDialog(String message, boolean show) {
        if (show) {
            if (progressDialog == null) {
                progressDialog = new JDialog(this,
                      getTextAt(RESOURCE_BUNDLE, "PDFReaderUtility.progressDialog.title"),
                      true);
                progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                JPanel panel = new JPanel(new BorderLayout(PADDING, PADDING));
                panel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
                JProgressBar bar = new JProgressBar();
                bar.setIndeterminate(true);
                panel.add(new JLabel(message), BorderLayout.NORTH);
                panel.add(bar, BorderLayout.CENTER);
                progressDialog.getContentPane().add(panel);
                progressDialog.pack();
                progressDialog.setLocationRelativeTo(this);
                progressDialog.setResizable(false);
            }
            SwingUtilities.invokeLater(() -> progressDialog.setVisible(true));
        } else if (progressDialog != null) {
            SwingUtilities.invokeLater(() -> progressDialog.setVisible(false));
        }
    }

    /**
     * Closes the dialog and ensures that any open PDF resources are properly released.
     *
     * <p>This overrides {@link JDialog#dispose()}, and performs cleanup such as closing open files, ending
     * background tasks, and preventing potential resource leaks.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    @Override
    public void dispose() {
        // Close the PDF document resource if it's open
        try {
            if (document != null) {
                document.close();
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to close PDF document: {}", ex.getMessage());
        }
        // Always call the superclass's 'dispose' to properly dispose the dialog
        super.dispose();
    }
}

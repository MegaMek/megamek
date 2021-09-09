/*
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 */
/*
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 */
package megamek.client.ui.swing.widget;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import megamek.client.ui.swing.unitDisplay.UnitDisplay;
import megamek.common.Configuration;

public class MechPanelTabStrip extends PicMap {

    /**
     *
     */
    private static final long serialVersionUID = -1282343469769007184L;

    private PMPicPolygonalArea[] tabs = new PMPicPolygonalArea[6];
    private static final Image[] idleImage = new Image[6];
    private static final Image[] activeImage = new Image[6];
    private Image idleCorner, selectedCorner;
    private int activeTab = 0;
    UnitDisplay md;

    public MechPanelTabStrip(UnitDisplay md) {
        super();
        this.md = md;
    }

    public void setTab(int i) {
        if (i > 5) {
            i = 5;
        }
        activeTab = i;
        redrawImages();
        update();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        setImages();
        setAreas();
        setListeners();
        update();
    }

    private void setImages() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler
                .getUnitDisplaySkin();
        MediaTracker mt = new MediaTracker(this);
        Toolkit tk = getToolkit();
        idleImage[0] = tk.getImage(new File(Configuration.widgetsDir(), udSpec.getGeneralTabIdle()).toString());
        idleImage[1] = tk.getImage(new File(Configuration.widgetsDir(), udSpec.getPilotTabIdle()).toString());
        idleImage[2] = tk.getImage(new File(Configuration.widgetsDir(), udSpec.getArmorTabIdle()).toString());
        idleImage[3] = tk.getImage(new File(Configuration.widgetsDir(), udSpec.getSystemsTabIdle()).toString());
        idleImage[4] = tk.getImage(new File(Configuration.widgetsDir(), udSpec.getWeaponsTabIdle()).toString());
        idleImage[5] = tk.getImage(new File(Configuration.widgetsDir(), udSpec.getExtrasTabIdle()).toString());
        activeImage[0] = tk.getImage(new File(Configuration.widgetsDir(), udSpec.getGeneralTabActive()).toString());
        activeImage[1] = tk.getImage(new File(Configuration.widgetsDir(), udSpec.getPilotTabActive()).toString());
        activeImage[2] = tk.getImage(new File(Configuration.widgetsDir(), udSpec.getArmorTabActive()).toString());
        activeImage[3] = tk.getImage(new File(Configuration.widgetsDir(), udSpec.getSystemsTabActive()).toString());
        activeImage[4] = tk.getImage(new File(Configuration.widgetsDir(), udSpec.getWeaponsTabActive()).toString());
        activeImage[5] = tk.getImage(new File(Configuration.widgetsDir(), udSpec.getExtraTabActive()).toString());
        idleCorner = tk.getImage(new File(Configuration.widgetsDir(), udSpec.getCornerIdle()).toString());
        selectedCorner = tk.getImage(new File(Configuration.widgetsDir(), udSpec.getCornerActive()).toString());

        // If we don't flush, we might have stale data
        idleCorner.flush();
        selectedCorner.flush();

        for (int i = 0; i < 6; i++) {
            // If we don't flush, we might have stale data
            idleImage[i].flush();
            activeImage[i].flush();
            mt.addImage(idleImage[i], 0);
            mt.addImage(activeImage[i], 0);
        }
        mt.addImage(idleCorner, 0);
        mt.addImage(selectedCorner, 0);
        try {
            mt.waitForAll();
        } catch (InterruptedException e) {
            System.out.println("TabStrip: Error while image loading."); //$NON-NLS-1$
        }
        if (mt.isErrorID(0)) {
            System.out.println("TabStrip: Could Not load Image."); //$NON-NLS-1$
        }

        for (int i = 0; i < 6; i++) {
            if (idleImage[i].getWidth(null) != activeImage[i].getWidth(null)) {
                System.out.println("TabStrip Warning: idleImage and "
                        + "activeImage do not match widths for image " + i);
            }
            if (idleImage[i].getHeight(null) != activeImage[i].getHeight(null)) {
                System.out.println("TabStrip Warning: idleImage and "
                        + "activeImage do not match heights for image " + i);
            }
        }
        if (idleCorner.getWidth(null) != selectedCorner.getWidth(null)) {
            System.out.println("TabStrip Warning: idleCorner and "
                    + "selectedCorner do not match widths!");
        }
        if (idleCorner.getHeight(null) != selectedCorner.getHeight(null)) {
            System.out.println("TabStrip Warning: idleCorner and "
                    + "selectedCorner do not match heights!");
        }
    }

    private void setAreas() {
        int cornerWidth = idleCorner.getWidth(null);

        for (int i = 0; i < idleImage.length; i++) {
            int width = idleImage[i].getWidth(null);
            int height = idleImage[i].getHeight(null);
            int[] pointsX = new int[] { 0, width, width + cornerWidth, 0 };
            int[] pointsY = new int[] { 0, 0, height, height };
            tabs[i] = new PMPicPolygonalArea(new Polygon(pointsX, pointsY, 4),
                    createImage(width, height));
        }

        int cumWidth = 0;
        for (int i = 0; i < idleImage.length; i++) {
            drawIdleImage(i);
            tabs[i].translate(cumWidth, 0);
            addElement(tabs[i]);
            cumWidth += idleImage[i].getWidth(null);
        }
    }

    private void setListeners() {
        tabs[0].addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand() == PMHotArea.MOUSE_DOWN) {
                    md.showPanel("movement"); //$NON-NLS-1$
                }
            }
        });
        tabs[1].addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand() == PMHotArea.MOUSE_DOWN) {
                    md.showPanel("pilot"); //$NON-NLS-1$
                }
            }
        });
        tabs[2].addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand() == PMHotArea.MOUSE_DOWN) {
                    md.showPanel("armor"); //$NON-NLS-1$
                }
            }
        });
        tabs[3].addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand() == PMHotArea.MOUSE_DOWN) {
                    md.showPanel("systems"); //$NON-NLS-1$
                }
            }
        });
        tabs[4].addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand() == PMHotArea.MOUSE_DOWN) {
                    md.showPanel("weapons"); //$NON-NLS-1$
                }
            }
        });
        tabs[5].addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand() == PMHotArea.MOUSE_DOWN) {
                    md.showPanel("extras"); //$NON-NLS-1$
                }
            }
        });

    }

    private void redrawImages() {
        for (int i = 0; i < 6; i++) {
            drawIdleImage(i);
        }
    }

    private void drawIdleImage(int tab) {
        if (tabs[tab] == null) {
            // hmm, display not initialized yet...
            return;
        }
        Graphics g = tabs[tab].getIdleImage().getGraphics();

        if (activeTab == tab) {
            g.drawImage(activeImage[tab], 0, 0, null);
        } else {
            g.drawImage(idleImage[tab], 0, 0, null);
            if ((tab - activeTab) == 1) {
                g.drawImage(selectedCorner, 0, 4, null);
            } else if (tab > 0) {
                g.drawImage(idleCorner, 0, 4, null);
            }
        }
        g.dispose();
    }

    @Override
    public void onResize() {
        //ignore
    }

}

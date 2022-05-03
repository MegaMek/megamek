/*
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 */
/*
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 */
package megamek.client.ui.swing.widget;

import megamek.client.ui.swing.unitDisplay.UnitDisplay;
import megamek.common.Configuration;
import megamek.common.util.fileUtils.MegaMekFile;
import org.apache.logging.log4j.LogManager;

import java.awt.*;

public class MechPanelTabStrip extends PicMap {
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
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler.getUnitDisplaySkin();
        MediaTracker mt = new MediaTracker(this);
        Toolkit tk = getToolkit();
        idleImage[0] = tk.getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getGeneralTabIdle()).toString());
        idleImage[1] = tk.getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getPilotTabIdle()).toString());
        idleImage[2] = tk.getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getArmorTabIdle()).toString());
        idleImage[3] = tk.getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getSystemsTabIdle()).toString());
        idleImage[4] = tk.getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getWeaponsTabIdle()).toString());
        idleImage[5] = tk.getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getExtrasTabIdle()).toString());
        activeImage[0] = tk.getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getGeneralTabActive()).toString());
        activeImage[1] = tk.getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getPilotTabActive()).toString());
        activeImage[2] = tk.getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getArmorTabActive()).toString());
        activeImage[3] = tk.getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getSystemsTabActive()).toString());
        activeImage[4] = tk.getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getWeaponsTabActive()).toString());
        activeImage[5] = tk.getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getExtraTabActive()).toString());
        idleCorner = tk.getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getCornerIdle()).toString());
        selectedCorner = tk.getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getCornerActive()).toString());

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
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }

        if (mt.isErrorID(0)) {
            LogManager.getLogger().warn("Could not load image");
        }

        for (int i = 0; i < 6; i++) {
            if (idleImage[i].getWidth(null) != activeImage[i].getWidth(null)) {
                LogManager.getLogger().warn("idleImage and activeImage do not match widths for image " + i);
            }
            if (idleImage[i].getHeight(null) != activeImage[i].getHeight(null)) {
                LogManager.getLogger().warn("idleImage and activeImage do not match heights for image " + i);
            }
        }
        if (idleCorner.getWidth(null) != selectedCorner.getWidth(null)) {
            LogManager.getLogger().warn("idleCorner and selectedCorner do not match widths!");
        }
        if (idleCorner.getHeight(null) != selectedCorner.getHeight(null)) {
            LogManager.getLogger().warn("idleCorner and selectedCorner do not match heights!");
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
        tabs[0].addActionListener(e -> {
            if (e.getActionCommand() == PMHotArea.MOUSE_DOWN) {
                md.showPanel("movement");
            }
        });
        tabs[1].addActionListener(e -> {
            if (e.getActionCommand() == PMHotArea.MOUSE_DOWN) {
                md.showPanel("pilot");
            }
        });
        tabs[2].addActionListener(e -> {
            if (e.getActionCommand() == PMHotArea.MOUSE_DOWN) {
                md.showPanel("armor");
            }
        });
        tabs[3].addActionListener(e -> {
            if (e.getActionCommand() == PMHotArea.MOUSE_DOWN) {
                md.showPanel("systems");
            }
        });
        tabs[4].addActionListener(e -> {
            if (e.getActionCommand() == PMHotArea.MOUSE_DOWN) {
                md.showPanel("weapons");
            }
        });
        tabs[5].addActionListener(e -> {
            if (e.getActionCommand() == PMHotArea.MOUSE_DOWN) {
                md.showPanel("extras");
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
        // ignore
    }
}

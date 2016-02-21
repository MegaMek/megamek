package megamek.client.ui.swing.unitDisplay;

import java.awt.Rectangle;
import java.util.Enumeration;

import megamek.client.ui.swing.widget.BackGroundDrawer;
import megamek.client.ui.swing.widget.PicMap;
import megamek.client.ui.swing.widget.PilotMapSet;
import megamek.common.Entity;

/**
 * The pilot panel contains all the information about the pilot/crew of this
 * unit.
 */
class PilotPanel extends PicMap {

    /**
     *
     */
    private static final long serialVersionUID = 8284603003897415518L;

    private PilotMapSet pi;

    private int minTopMargin = 8;
    private int minLeftMargin = 8;

    PilotPanel() {
        pi = new PilotMapSet(this);
        addElement(pi.getContentGroup());
        Enumeration<BackGroundDrawer> iter = pi.getBackgroundDrawers()
                                               .elements();
        while (iter.hasMoreElements()) {
            addBgDrawer(iter.nextElement());
        }
        onResize();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        update();
    }

    @Override
    public void onResize() {
        int w = getSize().width;
        Rectangle r = getContentBounds();
        int dx = Math.round(((w - r.width) / 2));
        if (dx < minLeftMargin) {
            dx = minLeftMargin;
        }
        int dy = minTopMargin;
        setContentMargins(dx, dy, dx, dy);
    }

    /**
     * updates fields for the specified mech
     */
    public void displayMech(Entity en) {
        pi.setEntity(en);
        onResize();
        update();
    }

}
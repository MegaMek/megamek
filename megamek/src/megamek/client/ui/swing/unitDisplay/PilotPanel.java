package megamek.client.ui.swing.unitDisplay;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.Enumeration;

import javax.swing.JComboBox;

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
    private JComboBox<String> cbCrewSlot = new JComboBox<>();
    
    //We need to hold onto the entity in case the crew slot changes.
    private Entity entity;

    PilotPanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(minTopMargin, minLeftMargin, minTopMargin, minLeftMargin);
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        add(cbCrewSlot, gbc);
        cbCrewSlot.addActionListener(e -> selectCrewSlot());
        
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
        entity = en;
        pi.setEntity(en);
        if (en.getCrew().getSlotCount() > 1) {
            cbCrewSlot.removeAllItems();
            for (int i = 0; i < en.getCrew().getSlotCount(); i++) {
                cbCrewSlot.addItem(en.getCrew().getCrewType().getRoleName(i));
            }
            cbCrewSlot.setVisible(true);
        } else {
            cbCrewSlot.setVisible(false);
        }
        
        onResize();
        update();
    }
    
    private void selectCrewSlot() {
        if (null != entity && cbCrewSlot.getSelectedIndex() >= 0) {
            pi.setEntity(entity, cbCrewSlot.getSelectedIndex());
            onResize();
            update();
        }
    }
    
}
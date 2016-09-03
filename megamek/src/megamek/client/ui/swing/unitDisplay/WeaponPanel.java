package megamek.client.ui.swing.unitDisplay;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;

import megamek.client.event.MechDisplayEvent;
import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.FiringDisplay;
import megamek.client.ui.swing.MovementDisplay;
import megamek.client.ui.swing.TargetingPhaseDisplay;
import megamek.client.ui.swing.widget.BackGroundDrawer;
import megamek.client.ui.swing.widget.PMUtil;
import megamek.client.ui.swing.widget.PicMap;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.client.ui.swing.widget.UnitDisplaySkinSpecification;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Configuration;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.FighterSquadron;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.ILocationExposureStatus;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.SmallCraft;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.WeaponComparatorArc;
import megamek.common.WeaponComparatorCustom;
import megamek.common.WeaponComparatorDamage;
import megamek.common.WeaponComparatorNum;
import megamek.common.WeaponComparatorRange;
import megamek.common.WeaponType;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.BayWeapon;
import megamek.common.weapons.HAGWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * This class contains the all the gizmos for firing the mech's weapons.
 */
public class WeaponPanel extends PicMap implements ListSelectionListener,
        ActionListener {
    
    /**
     * Mouse adaptor for the weapon list.  Supports rearranging the weapons
     * to define a custom ordering.
     *
     * @author arlith
     *
     */
    private class WeaponListMouseAdapter extends MouseInputAdapter {

        private boolean mouseDragging = false;
        private int dragSourceIndex;

        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                Object src = e.getSource();
                if (src instanceof JList) {
                    dragSourceIndex = ((JList<?>) src).getSelectedIndex();
                    mouseDragging = true;
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            mouseDragging = false;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            removeListeners();
            
            Object src = e.getSource();
            // Check to see if we are in a state we care about
            if (!mouseDragging || !(src instanceof JList)) {
                return;
            }
            JList<?> srcList = (JList<?>) src;
            WeaponListModel srcModel = (WeaponListModel) srcList.getModel();
            int currentIndex = srcList.locationToIndex(e.getPoint());
            if (currentIndex != dragSourceIndex) {
                int dragTargetIndex = srcList.getSelectedIndex();
                Mounted weap1 = srcModel.getWeaponAt(dragSourceIndex);
                srcModel.swapIdx(dragSourceIndex, dragTargetIndex);
                dragSourceIndex = currentIndex;
                Entity ent = weap1.getEntity();
                // Is the sort order custom?
                int customId = Entity.WeaponSortOrder.CUSTOM.ordinal();
                // Update weap sort order drop down
                if (weapSortOrder.getSelectedIndex() != customId) {
                    // Set the order to custom
                    ent.setWeaponSortOrder(Entity.WeaponSortOrder.CUSTOM);
                    weapSortOrder.setSelectedIndex(customId);
                }
                // Update custom order
                for (int i = 0; i < srcModel.getSize(); i++) {
                    Mounted m = srcModel.getWeaponAt(i);
                    ent.setCustomWeaponOrder(m, i);
                }
                if (unitDisplay.getClientGUI() != null) {
                    unitDisplay.getClientGUI().getMenuBar()
                            .updateSaveWeaponOrderMenuItem();
                }
            }
            addListeners();
        }
    }
    
    /**
     * ListModel implementation that supports keeping track of a list of Mounted
     * instantiations, how to display them in the JList, and an ability to sort
     * the Mounteds given WeaponComparators.
     *
     * @author arlith
     *
     */
    class WeaponListModel extends AbstractListModel<String> {

        /**
         *
         */
        private static final long serialVersionUID = 6312003196674512339L;

        /**
         * A collection of Mounted instantiations.
         */
        private ArrayList<Mounted> weapons;

        /**
         * The Entity that owns the collection of Mounteds.
         */
        private Entity en;

        WeaponListModel(Entity e) {
            en = e;
            weapons = new ArrayList<Mounted>();
        }

        /**
         * Add a new weapon to the list.
         *
         * @param w
         */
        public void addWeapon(Mounted w) {
            weapons.add(w);
            fireIntervalAdded(this,weapons.size()-1, weapons.size()-1);
        }

        /**
         * Given the equipment (weapon) id, return the index in the (possibly
         * sorted) list of Mounteds.
         *
         * @param weaponId
         * @return
         */
        public int getIndex(int weaponId) {
            Mounted mount = en.getEquipment(weaponId);
            for (int i = 0; i < weapons.size(); i++) {
                if (weapons.get(i).equals(mount)) {
                    return i;
                }
            }
            return -1;
        }

        public void removeAllElements() {
            int numWeapons = weapons.size() - 1;
            weapons.clear();
            fireIntervalRemoved(this, 0, numWeapons);
        }

        /**
         * Swap the Mounteds at the two specified index values.
         *
         * @param idx1
         * @param idx2
         */
        public void swapIdx(int idx1, int idx2) {
            // Bounds checking
            if ((idx1 >= weapons.size()) || (idx2 >= weapons.size())
                    || (idx1 < 0) || (idx2 < 0)) {
                return;
            }
            Mounted m1 = weapons.get(idx1);
            weapons.set(idx1, weapons.get(idx2));
            weapons.set(idx2, m1);
            fireContentsChanged(this, idx1, idx1);
            fireContentsChanged(this, idx2, idx2);
        }

        public Mounted getWeaponAt(int index) {
            return weapons.get(index);
        }

        /**
         * Given an index into the (possibly sorted) list of Mounted, return a
         * text description.  This consists of the Mounted's description, as
         * well as additional information like location, whether the Mounted is
         * shot/jammed/destroyed, etc.  This is what the JList will display.
         */
        @Override
        public String getElementAt(int index) {
            final Mounted mounted = weapons.get(index);
            final WeaponType wtype = (WeaponType) mounted.getType();
            IGame game = null;
            if (unitDisplay.getClientGUI() != null) {
                game = unitDisplay.getClientGUI().getClient().getGame();
            }

            StringBuffer wn = new StringBuffer(mounted.getDesc());
            wn.append(" ["); //$NON-NLS-1$
            wn.append(en.getLocationAbbr(mounted.getLocation()));
            if (mounted.isSplit()) {
                wn.append('/');
                wn.append(en.getLocationAbbr(mounted.getSecondLocation()));
            }
            wn.append(']');
            // determine shots left & total shots left
            if ((wtype.getAmmoType() != AmmoType.T_NA)
                && !wtype.hasFlag(WeaponType.F_ONESHOT)) {
                int shotsLeft = 0;
                if ((mounted.getLinked() != null)
                    && !mounted.getLinked().isDumping()) {
                    shotsLeft = mounted.getLinked().getUsableShotsLeft();
                }

                EquipmentType typeUsed = null;
                if (mounted.getLinked() != null) {
                    typeUsed = mounted.getLinked().getType();
                }

                int totalShotsLeft = en.getTotalMunitionsOfType(typeUsed);

                wn.append(" ("); //$NON-NLS-1$
                wn.append(shotsLeft);
                wn.append('/'); //$NON-NLS-1$
                wn.append(totalShotsLeft);
                wn.append(')'); //$NON-NLS-1$
            }

            // MG rapidfire
            if (mounted.isRapidfire()) {
                wn.append(Messages.getString("MechDisplay.rapidFire")); //$NON-NLS-1$
            }

            // Hotloaded Missile Launchers
            if (mounted.isHotLoaded()) {
                wn.append(Messages.getString("MechDisplay.isHotLoaded")); //$NON-NLS-1$
            }

            // Fire Mode - lots of things have variable modes
            if (wtype.hasModes()) {
                wn.append(' ');
                
                wn.append(mounted.curMode().getDisplayableName());
                if (!mounted.pendingMode().equals("None")) {
                    wn.append(" (next turn, ");
                    wn.append(mounted.pendingMode().getDisplayableName());
                    wn.append(')');
                }
            }
            if ((game != null)
                    && game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CALLED_SHOTS)) { //$NON-NLS-1$
                wn.append(' ');
                wn.append(mounted.getCalledShot().getDisplayableName());
            }
            return wn.toString();
        }

        /**
         * Returns the number of Mounteds in the list.
         */
        @Override
        public int getSize() {
            return weapons.size();
        }

        /**
         * Sort the Mounteds, generally using a WeaponComparator.
         *
         * @param comparator
         */
        public void sort(Comparator<Mounted> comparator) {
            Collections.sort(weapons, comparator);
            fireContentsChanged(this, 0, weapons.size() - 1);
        }

    }
    
    
    /**
     * 
     */
    private final UnitDisplay unitDisplay;
    
    /**
     *
     */
    private static final long serialVersionUID = -5728839963281503332L;
    private JComboBox<String> weapSortOrder;
    public JList<String> weaponList;
    /**
     * Keep track of the previous target, used for certain weapons (like VGLs)
     * that will force a target.  With this, we can restore the previous target
     * after the forced target.
     */
    private Targetable prevTarget = null;
    private JComboBox<String> m_chAmmo;
    public JComboBox<String> m_chBayWeapon;

    private JLabel wSortOrder;
    private JLabel wAmmo;
    private JLabel wBayWeapon;
    private JLabel wNameL;
    private JLabel wHeatL;
    private JLabel wArcHeatL;
    private JLabel wDamL;
    private JLabel wMinL;
    private JLabel wShortL;
    private JLabel wMedL;
    private JLabel wLongL;
    private JLabel wExtL;
    private JLabel wAVL;
    private JLabel wNameR;
    private JLabel wHeatR;
    private JLabel wArcHeatR;
    private JLabel wDamR;
    private JLabel wMinR;
    private JLabel wShortR;
    private JLabel wMedR;
    private JLabel wLongR;
    private JLabel wExtR;
    private JLabel wShortAVR;
    private JLabel wMedAVR;
    private JLabel wLongAVR;
    private JLabel wExtAVR;
    private JLabel currentHeatBuildupL;
    private JLabel currentHeatBuildupR;

    private JLabel wTargetL;
    private JLabel wRangeL;
    private JLabel wToHitL;
    public JLabel wTargetR;
    public JLabel wRangeR;
    public JLabel wToHitR;

    private JLabel wDamageTrooperL;
    private JLabel wDamageTrooperR;
    private JLabel wInfantryRange0L;
    private JLabel wInfantryRange0R;
    private JLabel wInfantryRange1L;
    private JLabel wInfantryRange1R;
    private JLabel wInfantryRange2L;
    private JLabel wInfantryRange2R;
    private JLabel wInfantryRange3L;
    private JLabel wInfantryRange3R;
    private JLabel wInfantryRange4L;
    private JLabel wInfantryRange4R;
    private JLabel wInfantryRange5L;
    private JLabel wInfantryRange5R;

    public JTextArea toHitText;

    // I need to keep a pointer to the weapon list of the
    // currently selected mech.
    private ArrayList<Mounted> vAmmo;
    private Entity entity;

    private int minTopMargin = 8;
    private int minLeftMargin = 8;

    WeaponPanel(UnitDisplay unitDisplay) {

        this.unitDisplay = unitDisplay;
        setLayout(new GridBagLayout());

        int gridy = 0;
        wSortOrder = new JLabel(
                Messages.getString("MechDisplay.WeaponSortOrder.label"),
                SwingConstants.LEFT);
        wSortOrder.setOpaque(false);
        wSortOrder.setForeground(Color.WHITE);
        add(wSortOrder, GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(15, 9, 1, 1).gridy(gridy).gridx(0));
        weapSortOrder = new JComboBox<String>();
        for (Entity.WeaponSortOrder s : Entity.WeaponSortOrder.values()) {
            String entry = "MechDisplay.WeaponSortOrder." + s.i18nEntry;
            weapSortOrder.addItem(Messages.getString(entry));
        }
        add(weapSortOrder,
                GBC.eol().insets(15, 9, 15, 1)
                   .fill(GridBagConstraints.HORIZONTAL)
                   .anchor(GridBagConstraints.CENTER).gridy(gridy)
                   .gridx(1));
        gridy++;

        // weapon list
        weaponList = new JList<String>(new DefaultListModel<String>());
        WeaponListMouseAdapter mouseAdapter = new WeaponListMouseAdapter();
        weaponList.addMouseListener(mouseAdapter);
        weaponList.addMouseMotionListener(mouseAdapter);
        JScrollPane tWeaponScroll = new JScrollPane(weaponList);
        tWeaponScroll.setMinimumSize(new Dimension(200, 100));
        tWeaponScroll.setPreferredSize(new Dimension(200, 100));
        add(tWeaponScroll,
            GBC.eol().insets(15, 9, 15, 9)
               .fill(GridBagConstraints.HORIZONTAL)
               .anchor(GridBagConstraints.CENTER).gridy(gridy)
               .gridx(0));
        gridy++;
        weaponList.resetKeyboardActions();
        for (KeyListener key : weaponList.getKeyListeners()) {
            weaponList.removeKeyListener(key);
        }

        // adding Ammo choice + label

        wAmmo = new JLabel(
                Messages.getString("MechDisplay.Ammo"), SwingConstants.LEFT); //$NON-NLS-1$
        wAmmo.setOpaque(false);
        wAmmo.setForeground(Color.WHITE);
        m_chAmmo = new JComboBox<String>();

        wBayWeapon = new JLabel(
                Messages.getString("MechDisplay.Weapon"), SwingConstants.LEFT); //$NON-NLS-1$
        wBayWeapon.setOpaque(false);
        wBayWeapon.setForeground(Color.WHITE);
        m_chBayWeapon = new JComboBox<String>();

        add(wBayWeapon, GBC.std().insets(15, 1, 1, 1).gridy(gridy).gridx(0));

        add(m_chBayWeapon, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                              .insets(1, 1, 15, 1).gridy(gridy).gridx(1));
        gridy++;
        add(wAmmo, GBC.std().insets(15, 9, 1, 1).gridy(gridy).gridx(0));

        add(m_chAmmo,
            GBC.eol().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 15, 1).gridy(gridy).gridx(1));
        gridy++;
        // Adding Heat Buildup

        currentHeatBuildupL = new JLabel(
                Messages.getString("MechDisplay.HeatBuildup"), SwingConstants.RIGHT); //$NON-NLS-1$
        currentHeatBuildupL.setOpaque(false);
        currentHeatBuildupL.setForeground(Color.WHITE);
        currentHeatBuildupR = new JLabel("--", SwingConstants.LEFT); //$NON-NLS-1$
        currentHeatBuildupR.setOpaque(false);
        currentHeatBuildupR.setForeground(Color.WHITE);

        add(currentHeatBuildupL,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .anchor(GridBagConstraints.EAST).insets(9, 1, 1, 9)
               .gridy(gridy).gridx(0));

        add(currentHeatBuildupR,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 1, 9, 9).gridy(gridy).gridx(1));
        gridy++;

        // Adding weapon display labels
        wNameL = new JLabel(
                Messages.getString("MechDisplay.Name"), SwingConstants.CENTER); //$NON-NLS-1$
        wNameL.setOpaque(false);
        wNameL.setForeground(Color.WHITE);
        wHeatL = new JLabel(
                Messages.getString("MechDisplay.Heat"), SwingConstants.CENTER); //$NON-NLS-1$
        wHeatL.setOpaque(false);
        wHeatL.setForeground(Color.WHITE);
        wDamL = new JLabel(
                Messages.getString("MechDisplay.Damage"), SwingConstants.CENTER); //$NON-NLS-1$
        wDamL.setOpaque(false);
        wDamL.setForeground(Color.WHITE);
        wArcHeatL = new JLabel(
                Messages.getString("MechDisplay.ArcHeat"), SwingConstants.CENTER); //$NON-NLS-1$
        wArcHeatL.setOpaque(false);
        wArcHeatL.setForeground(Color.WHITE);
        wNameR = new JLabel("", SwingConstants.CENTER); //$NON-NLS-1$
        wNameR.setOpaque(false);
        wNameR.setForeground(Color.WHITE);
        wHeatR = new JLabel("--", SwingConstants.CENTER); //$NON-NLS-1$
        wHeatR.setOpaque(false);
        wHeatR.setForeground(Color.WHITE);
        wDamR = new JLabel("--", SwingConstants.CENTER); //$NON-NLS-1$
        wDamR.setOpaque(false);
        wDamR.setForeground(Color.WHITE);
        wArcHeatR = new JLabel("--", SwingConstants.CENTER); //$NON-NLS-1$
        wArcHeatR.setOpaque(false);
        wArcHeatR.setForeground(Color.WHITE);

        wDamageTrooperL = new JLabel(
                Messages.getString("MechDisplay.DamageTrooper"), SwingConstants.CENTER); //$NON-NLS-1$
        wDamageTrooperL.setOpaque(false);
        wDamageTrooperL.setForeground(Color.WHITE);
        wDamageTrooperR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wDamageTrooperR.setOpaque(false);
        wDamageTrooperR.setForeground(Color.WHITE);

        add(wNameL,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 1, 1).gridy(gridy).gridx(0));

        add(wHeatL,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 1, 1).gridy(gridy).gridx(1));

        add(wDamL,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 1, 1).gridy(gridy).gridx(2));

        add(wArcHeatL, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                          .insets(1, 9, 1, 1).gridy(gridy).gridx(3));

        add(wDamageTrooperL, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                                .insets(1, 9, 1, 1).gridy(gridy).gridx(3));
        gridy++;
        add(wNameR,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 1, 1).gridy(gridy).gridx(0));

        add(wHeatR,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 1, 1).gridy(gridy).gridx(1));

        add(wDamR,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 1, 1).gridy(gridy).gridx(2));

        add(wArcHeatR, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                          .insets(1, 9, 1, 1).gridy(gridy).gridx(3));

        add(wDamageTrooperR, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                                .insets(1, 9, 1, 1).gridy(gridy).gridx(3));
        gridy++;
        // Adding range labels
        wMinL = new JLabel(
                Messages.getString("MechDisplay.Min"), SwingConstants.CENTER); //$NON-NLS-1$
        wMinL.setOpaque(false);
        wMinL.setForeground(Color.WHITE);
        wShortL = new JLabel(
                Messages.getString("MechDisplay.Short"), SwingConstants.CENTER); //$NON-NLS-1$
        wShortL.setOpaque(false);
        wShortL.setForeground(Color.WHITE);
        wMedL = new JLabel(
                Messages.getString("MechDisplay.Med"), SwingConstants.CENTER); //$NON-NLS-1$
        wMedL.setOpaque(false);
        wMedL.setForeground(Color.WHITE);
        wLongL = new JLabel(
                Messages.getString("MechDisplay.Long"), SwingConstants.CENTER); //$NON-NLS-1$
        wLongL.setOpaque(false);
        wLongL.setForeground(Color.WHITE);
        wExtL = new JLabel(
                Messages.getString("MechDisplay.Ext"), SwingConstants.CENTER); //$NON-NLS-1$
        wExtL.setOpaque(false);
        wExtL.setForeground(Color.WHITE);
        wMinR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wMinR.setOpaque(false);
        wMinR.setForeground(Color.WHITE);
        wShortR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wShortR.setOpaque(false);
        wShortR.setForeground(Color.WHITE);
        wMedR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wMedR.setOpaque(false);
        wMedR.setForeground(Color.WHITE);
        wLongR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wLongR.setOpaque(false);
        wLongR.setForeground(Color.WHITE);
        wExtR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wExtR.setOpaque(false);
        wExtR.setForeground(Color.WHITE);
        wAVL = new JLabel(
                Messages.getString("MechDisplay.AV"), SwingConstants.CENTER); //$NON-NLS-1$
        wAVL.setOpaque(false);
        wAVL.setForeground(Color.WHITE);
        wShortAVR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wShortAVR.setOpaque(false);
        wShortAVR.setForeground(Color.WHITE);
        wMedAVR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wMedAVR.setOpaque(false);
        wMedAVR.setForeground(Color.WHITE);
        wLongAVR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wLongAVR.setOpaque(false);
        wLongAVR.setForeground(Color.WHITE);
        wExtAVR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wExtAVR.setOpaque(false);
        wExtAVR.setForeground(Color.WHITE);

        wInfantryRange0L = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wInfantryRange0L.setOpaque(false);
        wInfantryRange0L.setForeground(Color.WHITE);
        wInfantryRange0R = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wInfantryRange0R.setOpaque(false);
        wInfantryRange0R.setForeground(Color.WHITE);
        wInfantryRange1L = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wInfantryRange1L.setOpaque(false);
        wInfantryRange1L.setForeground(Color.WHITE);
        wInfantryRange1R = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wInfantryRange1R.setOpaque(false);
        wInfantryRange1R.setForeground(Color.WHITE);
        wInfantryRange2L = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wInfantryRange2L.setOpaque(false);
        wInfantryRange2L.setForeground(Color.WHITE);
        wInfantryRange2R = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wInfantryRange2R.setOpaque(false);
        wInfantryRange2R.setForeground(Color.WHITE);
        wInfantryRange3L = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wInfantryRange3L.setOpaque(false);
        wInfantryRange3L.setForeground(Color.WHITE);
        wInfantryRange3R = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wInfantryRange3R.setOpaque(false);
        wInfantryRange3R.setForeground(Color.WHITE);
        wInfantryRange4L = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wInfantryRange4L.setOpaque(false);
        wInfantryRange4L.setForeground(Color.WHITE);
        wInfantryRange4R = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wInfantryRange4R.setOpaque(false);
        wInfantryRange4R.setForeground(Color.WHITE);
        wInfantryRange5L = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wInfantryRange5L.setOpaque(false);
        wInfantryRange5L.setForeground(Color.WHITE);
        wInfantryRange5R = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wInfantryRange5R.setOpaque(false);
        wInfantryRange5R.setForeground(Color.WHITE);

        add(wMinL,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 9, 1).gridy(gridy).gridx(0));

        add(wShortL,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 9, 1).gridy(gridy).gridx(1));

        add(wMedL,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 9, 1).gridy(gridy).gridx(2));

        add(wLongL,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 9, 1).gridy(gridy).gridx(3));

        add(wExtL,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 9, 1).gridy(gridy).gridx(4));

        add(wInfantryRange0L, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                .insets(1, 9, 9, 1).gridy(gridy).gridx(0));

        add(wInfantryRange1L, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                            .insets(1, 9, 9, 1).gridy(gridy).gridx(1));

        add(wInfantryRange2L, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                            .insets(1, 9, 9, 1).gridy(gridy).gridx(2));

        add(wInfantryRange3L, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                            .insets(1, 9, 9, 1).gridy(gridy).gridx(3));

        add(wInfantryRange4L, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                            .insets(1, 9, 9, 1).gridy(gridy).gridx(4));

        add(wInfantryRange5L, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                            .insets(1, 9, 9, 1).gridy(gridy).gridx(4));

        gridy++;
        // ----------------

        add(wMinR,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 9, 1).gridy(gridy).gridx(0));

        add(wShortR,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 9, 1).gridy(gridy).gridx(1));

        add(wMedR,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 9, 1).gridy(gridy).gridx(2));

        add(wLongR,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 9, 1).gridy(gridy).gridx(3));

        add(wExtR,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 9, 1).gridy(gridy).gridx(4));

        add(wInfantryRange0R, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                .insets(1, 9, 9, 1).gridy(gridy).gridx(0));

        add(wInfantryRange1R, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                            .insets(1, 9, 9, 1).gridy(gridy).gridx(1));

        add(wInfantryRange2R, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                            .insets(1, 9, 9, 1).gridy(gridy).gridx(2));

        add(wInfantryRange3R, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                            .insets(1, 9, 9, 1).gridy(gridy).gridx(3));

        add(wInfantryRange4R, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                            .insets(1, 9, 9, 1).gridy(gridy).gridx(4));

        add(wInfantryRange5R, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                            .insets(1, 9, 9, 1).gridy(gridy).gridx(5));

        gridy++;
        // ----------------
        add(wAVL,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 9, 1).gridy(gridy).gridx(0));

        add(wShortAVR, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                          .insets(1, 9, 9, 1).gridy(gridy).gridx(1));

        add(wMedAVR,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 9, 1).gridy(gridy).gridx(2));

        add(wLongAVR,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 9, 1).gridy(gridy).gridx(3));

        add(wExtAVR,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 9, 1).gridy(gridy).gridx(4));

        gridy++;



        // target panel
        wTargetL = new JLabel(
                Messages.getString("MechDisplay.Target"), SwingConstants.CENTER); //$NON-NLS-1$
        wTargetL.setOpaque(false);
        wTargetL.setForeground(Color.WHITE);
        wRangeL = new JLabel(
                Messages.getString("MechDisplay.Range"), SwingConstants.CENTER); //$NON-NLS-1$
        wRangeL.setOpaque(false);
        wRangeL.setForeground(Color.WHITE);
        wToHitL = new JLabel(
                Messages.getString("MechDisplay.ToHit"), SwingConstants.CENTER); //$NON-NLS-1$
        wToHitL.setOpaque(false);
        wToHitL.setForeground(Color.WHITE);

        wTargetR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wTargetR.setOpaque(false);
        wTargetR.setForeground(Color.WHITE);
        wRangeR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wRangeR.setOpaque(false);
        wRangeR.setForeground(Color.WHITE);
        wToHitR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
        wToHitR.setOpaque(false);
        wToHitR.setForeground(Color.WHITE);

        add(wTargetL,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 1, 1).gridy(gridy).gridx(0));

        add(wTargetR,
            GBC.eol().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 1, 1).gridy(gridy).gridx(1));
        gridy++;

        add(wRangeL,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 1, 1).gridy(gridy).gridx(0));

        add(wRangeR,
            GBC.eol().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 1, 1).gridy(gridy).gridx(1));
        gridy++;

        add(wToHitL,
            GBC.std().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 1, 1).gridy(gridy).gridx(0));

        add(wToHitR,
            GBC.eol().fill(GridBagConstraints.HORIZONTAL)
               .insets(1, 9, 1, 1).gridy(gridy).gridx(1));
        gridy++;

        // to-hit text
        toHitText = new JTextArea("", 2, 20); //$NON-NLS-1$
        toHitText.setEditable(false);
        toHitText.setLineWrap(true);
        toHitText.setFont(new Font("SansSerif", Font.PLAIN, 10)); //$NON-NLS-1$
        add(toHitText,
            GBC.eol().fill(GridBagConstraints.BOTH)
               .insets(15, 9, 15, 9).gridy(gridy).gridx(0)
               .gridheight(2));
        gridy++;

        addListeners();
        
        setBackGround();
        onResize();
    }

    @Override
    public void onResize() {
        int w = getSize().width;
        Rectangle r = getContentBounds();
        if (r == null) {
            return;
        }
        int dx = Math.round(((w - r.width) / 2));
        if (dx < minLeftMargin) {
            dx = minLeftMargin;
        }
        int dy = minTopMargin;
        setContentMargins(dx, dy, dx, dy);
    }

    private void setBackGround() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler
                .getUnitDisplaySkin();

        Image tile = getToolkit()
                .getImage(
                        new File(Configuration.widgetsDir(), udSpec
                                .getBackgroundTile()).toString());
        PMUtil.setImage(tile, this);
        int b = BackGroundDrawer.TILING_BOTH;
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_TOP;
        tile = getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec.getTopLine())
                        .toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_BOTTOM;
        tile = getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec.getBottomLine())
                        .toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec.getLeftLine())
                        .toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec.getRightLine())
                        .toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit()
                .getImage(
                        new File(Configuration.widgetsDir(), udSpec
                                .getTopLeftCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec
                        .getBottomLeftCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit()
                .getImage(
                        new File(Configuration.widgetsDir(), udSpec
                                .getTopRightCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec
                        .getBottomRightCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

    }

    /**
     * updates fields for the specified mech
     * <p/>
     * fix the ammo when it's added
     */
    public void displayMech(Entity en) {
        removeListeners();
        
        // Grab a copy of the game.
        IGame game = null;

        if (unitDisplay.getClientGUI() != null) {
            game = unitDisplay.getClientGUI().getClient().getGame();
        }

        // update pointer to weapons
        entity = en;

        // Check Game Options for max external heat
        int max_ext_heat = game != null ? game.getOptions().intOption(OptionsConstants.ADVCOMBAT_MAX_EXTERNAL_HEAT) : 15;
        if (max_ext_heat < 0) {
            max_ext_heat = 15; // Standard value specified in TW p.159
        }

        int currentHeatBuildup = (en.heat // heat from last round                
                + en.getEngineCritHeat() // heat engine crits will add
                + Math.min(max_ext_heat, en.heatFromExternal) // heat from external sources
                + en.heatBuildup) // heat we're building up this round
                - Math.min(9, en.coolFromExternal); // cooling from external
        // sources
        if (en instanceof Mech) {
            if (en.infernos.isStillBurning()) { // hit with inferno ammo
                currentHeatBuildup += en.infernos.getHeat();
            }
            if (!((Mech) en).hasLaserHeatSinks()) {
                // extreme temperatures.
                if ((game != null) && (game.getPlanetaryConditions().getTemperature() > 0)) {
                    int buildup = game.getPlanetaryConditions()
                                      .getTemperatureDifference(50, -30);
                    if (((Mech) en).hasIntactHeatDissipatingArmor()) {
                        buildup /= 2;
                    }
                    currentHeatBuildup += buildup;
                } else if (game != null) {
                    currentHeatBuildup -= game.getPlanetaryConditions()
                                              .getTemperatureDifference(50, -30);
                }
            }
        }
        Coords position = entity.getPosition();
        if (!en.isOffBoard() && (position != null)) {
            IHex hex = game.getBoard().getHex(position);
            if (hex.containsTerrain(Terrains.FIRE)
                && (hex.getFireTurn() > 0)) {
                // standing in fire
                if ((en instanceof Mech)
                    && ((Mech) en).hasIntactHeatDissipatingArmor()) {
                    currentHeatBuildup += 2;
                } else {
                    currentHeatBuildup += 5;
                }
            }
            if (hex.terrainLevel(Terrains.MAGMA) == 1) {
                if ((en instanceof Mech)
                    && ((Mech) en).hasIntactHeatDissipatingArmor()) {
                    currentHeatBuildup += 2;
                } else {
                    currentHeatBuildup += 5;
                }
            } else if (hex.terrainLevel(Terrains.MAGMA) == 2) {
                if ((en instanceof Mech)
                    && ((Mech) en).hasIntactHeatDissipatingArmor()) {
                    currentHeatBuildup += 5;
                } else {
                    currentHeatBuildup += 10;
                }
            }
        }
        if ((((en instanceof Mech) || (en instanceof Aero)) && en.isStealthActive())
            || en.isNullSigActive() || en.isVoidSigActive()) {
            currentHeatBuildup += 10; // active stealth/null sig/void sig
            // heat
        }

        if ((en instanceof Mech) && en.isChameleonShieldOn()) {
            currentHeatBuildup += 6;
        }

        if (((en instanceof Mech) || (en instanceof Aero)) && en.hasActiveNovaCEWS()) {
            currentHeatBuildup += 2;
        }

        // update weapon list
        weaponList.setModel(new WeaponListModel(en));
        ((DefaultComboBoxModel<String>) m_chAmmo.getModel()).removeAllElements();

        m_chAmmo.setEnabled(false);
        m_chBayWeapon.removeAllItems();
        m_chBayWeapon.setEnabled(false);

        // on large craft we may need to take account of firing arcs
        boolean[] usedFrontArc = new boolean[entity.locations()];
        boolean[] usedRearArc = new boolean[entity.locations()];
        for (int i = 0; i < entity.locations(); i++) {
            usedFrontArc[i] = false;
            usedRearArc[i] = false;
        }

        boolean hasFiredWeapons = false;
        for (int i = 0; i < entity.getWeaponList().size(); i++) {
            Mounted mounted = entity.getWeaponList().get(i);

            ((WeaponListModel) weaponList.getModel()).addWeapon(mounted);
            if (mounted.isUsedThisRound()
                && (game.getPhase() == mounted.usedInPhase())
                && (game.getPhase() == IGame.Phase.PHASE_FIRING)) {
                hasFiredWeapons = true;
                // add heat from weapons fire to heat tracker
                if (entity.usesWeaponBays()) {
                    // if using bay heat option then don't add total arc
                    if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_HEAT_BY_BAY)) {
                        for (int wId : mounted.getBayWeapons()) {
                            currentHeatBuildup += entity.getEquipment(wId)
                                                        .getCurrentHeat();
                        }
                    } else {
                        // check whether arc has fired
                        int loc = mounted.getLocation();
                        boolean rearMount = mounted.isRearMounted();
                        if (!rearMount) {
                            if (!usedFrontArc[loc]) {
                                currentHeatBuildup += entity.getHeatInArc(
                                        loc, rearMount);
                                usedFrontArc[loc] = true;
                            }
                        } else {
                            if (!usedRearArc[loc]) {
                                currentHeatBuildup += entity.getHeatInArc(
                                        loc, rearMount);
                                usedRearArc[loc] = true;
                            }
                        }
                    }
                } else {
                    if (!mounted.isBombMounted()) {
                        currentHeatBuildup += mounted.getCurrentHeat();
                    }
                }
            }
        }
        weapSortOrder.setSelectedIndex(entity.getWeaponSortOrder()
                .ordinal());
        setWeaponComparator(weapSortOrder.getSelectedIndex());

        if (en.hasDamagedRHS() && hasFiredWeapons) {
            currentHeatBuildup++;
        }

        // This code block copied from the MovementPanel class,
        // bad coding practice (duplicate code).
        int heatCap;
        if (en instanceof Mech) {
            heatCap = ((Mech) en).getHeatCapacity(true, false);
        } else if (en instanceof Aero) {
            heatCap = ((Aero) en).getHeatCapacity(false);
        } else {
            heatCap = en.getHeatCapacity();
        }
        int heatCapWater = en.getHeatCapacityWithWater();
        if (en.getCoolantFailureAmount() > 0) {
            heatCap -= en.getCoolantFailureAmount();
            heatCapWater -= en.getCoolantFailureAmount();
        }
        if (en.hasActivatedRadicalHS()) {
            if (en instanceof Mech) {
                heatCap += ((Mech) en).getActiveSinks();
                heatCapWater += ((Mech) en).getActiveSinks();
            } else if (en instanceof Aero) {
                heatCap += ((Aero) en).getHeatSinks();
                heatCapWater += ((Aero) en).getHeatSinks();
            }
        }
        String heatCapacityStr = Integer.toString(heatCap);

        if (heatCap < heatCapWater) {
            heatCapacityStr = heatCap + " [" + heatCapWater + ']'; //$NON-NLS-1$
        }
        // end duplicate block

        String heatText = Integer.toString(currentHeatBuildup);
        if (currentHeatBuildup > en.getHeatCapacityWithWater()) {
            heatText += "*"; // overheat indication //$NON-NLS-1$
        }
        // check for negative values due to extreme temp
        if (currentHeatBuildup < 0) {
            currentHeatBuildup = 0;
        }
        currentHeatBuildupR
                .setText(heatText + " (" + heatCapacityStr + ')'); //$NON-NLS-1$

        // change what is visible based on type
        if (entity.usesWeaponBays()) {
            wArcHeatL.setVisible(true);
            wArcHeatR.setVisible(true);
            m_chBayWeapon.setVisible(true);
            wBayWeapon.setVisible(true);
        } else {
            wArcHeatL.setVisible(false);
            wArcHeatR.setVisible(false);
            m_chBayWeapon.setVisible(false);
            wBayWeapon.setVisible(false);
        }

        wDamageTrooperL.setVisible(false);
        wDamageTrooperR.setVisible(false);
        wInfantryRange0L.setVisible(false);
        wInfantryRange0R.setVisible(false);
        wInfantryRange1L.setVisible(false);
        wInfantryRange1R.setVisible(false);
        wInfantryRange2L.setVisible(false);
        wInfantryRange2R.setVisible(false);
        wInfantryRange3L.setVisible(false);
        wInfantryRange3R.setVisible(false);
        wInfantryRange4L.setVisible(false);
        wInfantryRange4R.setVisible(false);
        wInfantryRange5L.setVisible(false);
        wInfantryRange5R.setVisible(false);

        if (entity.isAirborne() || entity.usesWeaponBays()) {
            wAVL.setVisible(true);
            wShortAVR.setVisible(true);
            wMedAVR.setVisible(true);
            wLongAVR.setVisible(true);
            wExtAVR.setVisible(true);
            wMinL.setVisible(false);
            wMinR.setVisible(false);
        } else {
            wAVL.setVisible(false);
            wShortAVR.setVisible(false);
            wMedAVR.setVisible(false);
            wLongAVR.setVisible(false);
            wExtAVR.setVisible(false);
            wMinL.setVisible(true);
            wMinR.setVisible(true);
        }

        // If MaxTech range rules are in play, display the extreme range.
        if (((game != null) && game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE))
                || (entity.isAirborne() || entity.usesWeaponBays())) { // $NON-NLS-1$
            wExtL.setVisible(true);
            wExtR.setVisible(true);
        } else {
            wExtL.setVisible(false);
            wExtR.setVisible(false);
        }
        onResize();
        addListeners();
    }

    public int getSelectedEntityId() {
        return entity.getId();
    }

    /**
     * Selects the weapon with the specified weapon ID.
     */
    public void selectWeapon(int wn) {
        if (wn == -1) {
            weaponList.setSelectedIndex(-1);
            return;
        }
        int index = ((WeaponListModel)weaponList.getModel()).getIndex(wn);
        if (index == -1) {
            weaponList.setSelectedIndex(-1);
            return;
        }
        weaponList.setSelectedIndex(index);
        weaponList.ensureIndexIsVisible(index);
        displaySelected();
        weaponList.repaint();
    }

    /**
     * Returns the Mounted for the selected weapon in the weapon list.
     * @return
     */
    public Mounted getSelectedWeapon() {
        int selected = weaponList.getSelectedIndex();
        if (selected == -1) {
            return null;
        }
        return ((WeaponListModel) weaponList.getModel())
                .getWeaponAt(selected);
    }

    /**
     * Returns the equipment ID number for the weapon currently selected
     */
    public int getSelectedWeaponNum() {
        int selected = weaponList.getSelectedIndex();
        if (selected == -1) {
            return -1;
        }
        return entity.getEquipmentNum(((WeaponListModel) weaponList
                .getModel()).getWeaponAt(selected));
    }

    /**
     * Selects the first valid weapon in the weapon list.
     * @return The weapon id of the weapon selected
     */
    public int selectFirstWeapon() {
        // Entity has no weapons, return -1;
        if (entity.getWeaponList().size() == 0
                || (entity.usesWeaponBays() 
                        && entity.getWeaponBayList().size() == 0)) {
            return -1;
        }
        WeaponListModel weapList = (WeaponListModel)weaponList.getModel();
        for (int i = 0; i < weaponList.getModel().getSize(); i++) {
            Mounted selectedWeap = weapList.getWeaponAt(i);
            if (entity.isWeaponValidForPhase(selectedWeap)) {
                weaponList.setSelectedIndex(i);
                weaponList.ensureIndexIsVisible(i);
                return entity.getEquipmentNum(selectedWeap);
            }
        }
        // Found no valid weapon
        weaponList.setSelectedIndex(-1);
        return -1;
    }

    public int getNextWeaponListIdx() {
        int selected = weaponList.getSelectedIndex();
        // In case nothing was selected
        if (selected == -1) {
            selected = weaponList.getModel().getSize() - 1;
        }
        Mounted selectedWeap;
        int initialSelection = selected;
        
        boolean hasLooped = false;
        do {
            selected++;
            if (selected >= weaponList.getModel().getSize()) {
                selected = 0;
            }
            if (selected == initialSelection) {
                hasLooped = true;
            }
            selectedWeap = ((WeaponListModel) weaponList.getModel())
                    .getWeaponAt(selected);
        } while (!hasLooped && !entity.isWeaponValidForPhase(selectedWeap));

        if ((selected >= 0) && (selected < entity.getWeaponList().size())
                && !hasLooped) {
            return selected;
        } else {
            return -1;
        } 
    }
    
    public int getPrevWeaponListIdx() {
        int selected = weaponList.getSelectedIndex();
        // In case nothing was selected
        if (selected == -1) {
            selected = 0;
        }
        Mounted selectedWeap;
        int initialSelection = selected;
        boolean hasLooped = false;
        do {
            selected--;
            if (selected < 0) {
                selected = weaponList.getModel().getSize() - 1;
            }
            if (selected == initialSelection) {
                hasLooped = true;
            }
            selectedWeap = ((WeaponListModel) weaponList.getModel())
                    .getWeaponAt(selected);
        } while (!hasLooped && !entity.isWeaponValidForPhase(selectedWeap));

        if ((selected >= 0) && (selected < entity.getWeaponList().size())
                && !hasLooped) {
            return selected;
        } else {
            return -1;
        }
    }
    
    public int getNextWeaponNum() {
        int selected = getNextWeaponListIdx();
        if ((selected >= 0) && (selected < entity.getWeaponList().size())) {
            return entity.getEquipmentNum(((WeaponListModel) weaponList
                    .getModel()).getWeaponAt(selected));
        } else {
            return -1;
        }
    }
    
    /**
     * Selects the next valid weapon in the weapon list.
     *
     * @return The weaponId for the selected weapon
     */
    public int selectNextWeapon() {
        int selected = getNextWeaponListIdx();
        weaponList.setSelectedIndex(selected);
        weaponList.ensureIndexIsVisible(selected);
        if ((selected >= 0) && (selected < entity.getWeaponList().size())) {
            return entity.getEquipmentNum(((WeaponListModel) weaponList
                    .getModel()).getWeaponAt(selected));
        } else {
            return -1;
        }
    }

    /**
     * Selects the prevous valid weapon in the weapon list.
     *
     * @return The weaponId for the selected weapon
     */
    public int selectPrevWeapon() {
        int selected = getPrevWeaponListIdx();
        weaponList.setSelectedIndex(selected);
        weaponList.ensureIndexIsVisible(selected);
        if ((selected >= 0) && (selected < entity.getWeaponList().size())) {
            return entity.getEquipmentNum(((WeaponListModel) weaponList
                    .getModel()).getWeaponAt(selected));
        } else {
            return -1;
        }
    }


    /**
     * displays the selected item from the list in the weapon display panel.
     */
    private void displaySelected() {
        removeListeners();
        // short circuit if not selected
        if (weaponList.getSelectedIndex() == -1) {
            ((DefaultComboBoxModel<String>) m_chAmmo.getModel())
                    .removeAllElements();
            m_chAmmo.setEnabled(false);
            m_chBayWeapon.removeAllItems();
            m_chBayWeapon.setEnabled(false);
            wNameR.setText(""); //$NON-NLS-1$
            wHeatR.setText("--"); //$NON-NLS-1$
            wArcHeatR.setText("---"); //$NON-NLS-1$
            wDamR.setText("--"); //$NON-NLS-1$
            wMinR.setText("---"); //$NON-NLS-1$
            wShortR.setText("---"); //$NON-NLS-1$
            wMedR.setText("---"); //$NON-NLS-1$
            wLongR.setText("---"); //$NON-NLS-1$
            wExtR.setText("---"); //$NON-NLS-1$

            wDamageTrooperL.setVisible(false);
            wDamageTrooperR.setVisible(false);
            wInfantryRange0L.setVisible(false);
            wInfantryRange0R.setVisible(false);
            wInfantryRange1L.setVisible(false);
            wInfantryRange1R.setVisible(false);
            wInfantryRange2L.setVisible(false);
            wInfantryRange2R.setVisible(false);
            wInfantryRange3L.setVisible(false);
            wInfantryRange3R.setVisible(false);
            wInfantryRange4L.setVisible(false);
            wInfantryRange4R.setVisible(false);
            wInfantryRange5L.setVisible(false);
            wInfantryRange5R.setVisible(false);

            return;
        }

        Mounted mounted = ((WeaponListModel) weaponList.getModel())
                .getWeaponAt(weaponList.getSelectedIndex());
        WeaponType wtype = (WeaponType) mounted.getType();
        // update weapon display
        wNameR.setText(mounted.getDesc());
        wHeatR.setText(Integer.toString(mounted.getCurrentHeat()));

        wArcHeatR.setText(Integer.toString(entity.getHeatInArc(
                mounted.getLocation(), mounted.isRearMounted())));

        if (wtype instanceof InfantryWeapon) {
            wDamageTrooperL.setVisible(true);
            wDamageTrooperR.setVisible(true);
            InfantryWeapon inftype = (InfantryWeapon) wtype;
            if ((entity instanceof Infantry)
                && !(entity instanceof BattleArmor)) {
                wDamageTrooperR
                        .setText(Double.toString((double) Math
                                .round(((Infantry) entity)
                                               .getDamagePerTrooper() * 1000) / 1000));
            } else {
                wDamageTrooperR.setText(Double.toString(inftype
                                                                .getInfantryDamage()));
            }
            // what a nightmare to set up all the range info for infantry
            // weapons
            wMinL.setVisible(false);
            wShortL.setVisible(false);
            wMedL.setVisible(false);
            wLongL.setVisible(false);
            wExtL.setVisible(false);
            wMinR.setVisible(false);
            wShortR.setVisible(false);
            wMedR.setVisible(false);
            wLongR.setVisible(false);
            wExtR.setVisible(false);
            wInfantryRange0L.setVisible(false);
            wInfantryRange0R.setVisible(false);
            wInfantryRange1L.setVisible(false);
            wInfantryRange1R.setVisible(false);
            wInfantryRange2L.setVisible(false);
            wInfantryRange2R.setVisible(false);
            wInfantryRange3L.setVisible(false);
            wInfantryRange3R.setVisible(false);
            wInfantryRange4L.setVisible(false);
            wInfantryRange4R.setVisible(false);
            wInfantryRange5L.setVisible(false);
            wInfantryRange5R.setVisible(false);
            int zeromods = 0;
            if (inftype.hasFlag(WeaponType.F_INF_POINT_BLANK)) {
                zeromods++;
            }

            if (inftype.hasFlag(WeaponType.F_INF_ENCUMBER)
                || (inftype.getCrew() > 1)) {
                zeromods++;
            }

            if (inftype.hasFlag(WeaponType.F_INF_BURST)) {
                zeromods--;
            }
            switch (inftype.getInfantryRange()) {
                case 0:
                    wInfantryRange0L.setText("0");
                    wInfantryRange0R.setText("+" + zeromods);
                    wInfantryRange0L.setVisible(true);
                    wInfantryRange0R.setVisible(true);
                    break;
                case 1:
                    wInfantryRange0L.setText("0");
                    wInfantryRange0R
                            .setText(Integer.toString(zeromods - 2));
                    wInfantryRange0L.setVisible(true);
                    wInfantryRange0R.setVisible(true);
                    wInfantryRange1L.setText("1");
                    wInfantryRange1R.setText("+0");
                    wInfantryRange1L.setVisible(true);
                    wInfantryRange1R.setVisible(true);
                    wInfantryRange2L.setText("2");
                    wInfantryRange2R.setText("+2");
                    wInfantryRange2L.setVisible(true);
                    wInfantryRange2R.setVisible(true);
                    wInfantryRange3L.setText("3");
                    wInfantryRange3R.setText("+4");
                    wInfantryRange3L.setVisible(true);
                    wInfantryRange3R.setVisible(true);
                    break;
                case 2:
                    wInfantryRange0L.setText("0");
                    wInfantryRange0R
                            .setText(Integer.toString(zeromods - 2));
                    wInfantryRange0L.setVisible(true);
                    wInfantryRange0R.setVisible(true);
                    wInfantryRange1L.setText("1-2");
                    wInfantryRange1R.setText("+0");
                    wInfantryRange1L.setVisible(true);
                    wInfantryRange1R.setVisible(true);
                    wInfantryRange2L.setText("3-4");
                    wInfantryRange2R.setText("+2");
                    wInfantryRange2L.setVisible(true);
                    wInfantryRange2R.setVisible(true);
                    wInfantryRange3L.setText("5-6");
                    wInfantryRange3R.setText("+4");
                    wInfantryRange3L.setVisible(true);
                    wInfantryRange3R.setVisible(true);
                    break;
                case 3:
                    wInfantryRange0L.setText("0");
                    wInfantryRange0R
                            .setText(Integer.toString(zeromods - 2));
                    wInfantryRange0L.setVisible(true);
                    wInfantryRange0R.setVisible(true);
                    wInfantryRange1L.setText("1-3");
                    wInfantryRange1R.setText("+0");
                    wInfantryRange1L.setVisible(true);
                    wInfantryRange1R.setVisible(true);
                    wInfantryRange2L.setText("4-6");
                    wInfantryRange2R.setText("+2");
                    wInfantryRange2L.setVisible(true);
                    wInfantryRange2R.setVisible(true);
                    wInfantryRange3L.setText("7-9");
                    wInfantryRange3R.setText("+4");
                    wInfantryRange3L.setVisible(true);
                    wInfantryRange3R.setVisible(true);
                    break;
                case 4:
                    wInfantryRange0L.setText("0");
                    wInfantryRange0R
                            .setText(Integer.toString(zeromods - 2));
                    wInfantryRange0L.setVisible(true);
                    wInfantryRange0R.setVisible(true);
                    wInfantryRange1L.setText("1-4");
                    wInfantryRange1R.setText("+0");
                    wInfantryRange1L.setVisible(true);
                    wInfantryRange1R.setVisible(true);
                    wInfantryRange2L.setText("5-6");
                    wInfantryRange2R.setText("+1");
                    wInfantryRange2L.setVisible(true);
                    wInfantryRange2R.setVisible(true);
                    wInfantryRange3L.setText("7-8");
                    wInfantryRange3R.setText("+2");
                    wInfantryRange3L.setVisible(true);
                    wInfantryRange3R.setVisible(true);
                    wInfantryRange4L.setText("9-10");
                    wInfantryRange4R.setText("+3");
                    wInfantryRange4L.setVisible(true);
                    wInfantryRange4R.setVisible(true);
                    wInfantryRange5L.setText("11-12");
                    wInfantryRange5R.setText("+4");
                    wInfantryRange5L.setVisible(true);
                    wInfantryRange5R.setVisible(true);
                    break;
                case 5:
                    wInfantryRange0L.setText("0");
                    wInfantryRange0R
                            .setText(Integer.toString(zeromods - 1));
                    wInfantryRange0L.setVisible(true);
                    wInfantryRange0R.setVisible(true);
                    wInfantryRange1L.setText("1-5");
                    wInfantryRange1R.setText("+0");
                    wInfantryRange1L.setVisible(true);
                    wInfantryRange1R.setVisible(true);
                    wInfantryRange2L.setText("6-7");
                    wInfantryRange2R.setText("+1");
                    wInfantryRange2L.setVisible(true);
                    wInfantryRange2R.setVisible(true);
                    wInfantryRange3L.setText("8-10");
                    wInfantryRange3R.setText("+2");
                    wInfantryRange3L.setVisible(true);
                    wInfantryRange3R.setVisible(true);
                    wInfantryRange4L.setText("11-12");
                    wInfantryRange4R.setText("+3");
                    wInfantryRange4L.setVisible(true);
                    wInfantryRange4R.setVisible(true);
                    wInfantryRange5L.setText("13-15");
                    wInfantryRange5R.setText("+4");
                    wInfantryRange5L.setVisible(true);
                    wInfantryRange5R.setVisible(true);
                    break;
                case 6:
                    wInfantryRange0L.setText("0");
                    wInfantryRange0R
                            .setText(Integer.toString(zeromods - 1));
                    wInfantryRange0L.setVisible(true);
                    wInfantryRange0R.setVisible(true);
                    wInfantryRange1L.setText("1-6");
                    wInfantryRange1R.setText("+0");
                    wInfantryRange1L.setVisible(true);
                    wInfantryRange1R.setVisible(true);
                    wInfantryRange2L.setText("7-9");
                    wInfantryRange2R.setText("+1");
                    wInfantryRange2L.setVisible(true);
                    wInfantryRange2R.setVisible(true);
                    wInfantryRange3L.setText("10-12");
                    wInfantryRange3R.setText("+2");
                    wInfantryRange3L.setVisible(true);
                    wInfantryRange3R.setVisible(true);
                    wInfantryRange4L.setText("13-15");
                    wInfantryRange4R.setText("+4");
                    wInfantryRange4L.setVisible(true);
                    wInfantryRange4R.setVisible(true);
                    wInfantryRange5L.setText("16-18");
                    wInfantryRange5R.setText("+5");
                    wInfantryRange5L.setVisible(true);
                    wInfantryRange5R.setVisible(true);
                    break;
                case 7:
                    wInfantryRange0L.setText("0");
                    wInfantryRange0R
                            .setText(Integer.toString(zeromods - 1));
                    wInfantryRange0L.setVisible(true);
                    wInfantryRange0R.setVisible(true);
                    wInfantryRange1L.setText("1-7");
                    wInfantryRange1R.setText("+0");
                    wInfantryRange1L.setVisible(true);
                    wInfantryRange1R.setVisible(true);
                    wInfantryRange2L.setText("8-10");
                    wInfantryRange2R.setText("+1");
                    wInfantryRange2L.setVisible(true);
                    wInfantryRange2R.setVisible(true);
                    wInfantryRange3L.setText("11-14");
                    wInfantryRange3R.setText("+2");
                    wInfantryRange3L.setVisible(true);
                    wInfantryRange3R.setVisible(true);
                    wInfantryRange4L.setText("15-17");
                    wInfantryRange4R.setText("+4");
                    wInfantryRange4L.setVisible(true);
                    wInfantryRange4R.setVisible(true);
                    wInfantryRange5L.setText("18-21");
                    wInfantryRange5R.setText("+6");
                    wInfantryRange5L.setVisible(true);
                    wInfantryRange5R.setVisible(true);
                    break;
            }
        } else {
            wDamageTrooperL.setVisible(false);
            wDamageTrooperR.setVisible(false);
            wInfantryRange0L.setVisible(false);
            wInfantryRange0R.setVisible(false);
            wInfantryRange1L.setVisible(false);
            wInfantryRange1R.setVisible(false);
            wInfantryRange2L.setVisible(false);
            wInfantryRange2R.setVisible(false);
            wInfantryRange3L.setVisible(false);
            wInfantryRange3R.setVisible(false);
            wInfantryRange4L.setVisible(false);
            wInfantryRange4R.setVisible(false);
            wInfantryRange5L.setVisible(false);
            wInfantryRange5R.setVisible(false);
            wShortL.setVisible(true);
            wMedL.setVisible(true);
            wLongL.setVisible(true);

            wMinR.setVisible(true);
            wShortR.setVisible(true);
            wMedR.setVisible(true);
            wLongR.setVisible(true);

            if (!(entity.isAirborne() || entity.usesWeaponBays())) {
                wMinL.setVisible(true);
                wMinR.setVisible(true);
            }
            if (((entity.getGame() != null)
                    && entity.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE))
                    || (entity.isAirborne() || entity.usesWeaponBays())) {
                wExtL.setVisible(true);
                wExtR.setVisible(true);
            }
        }

        if (wtype.getDamage() == WeaponType.DAMAGE_BY_CLUSTERTABLE) {
            if (wtype instanceof HAGWeapon) {
                wDamR.setText(Messages.getString("MechDisplay.Variable")); //$NON-NLS-1$
            } else {
                wDamR.setText(Messages.getString("MechDisplay.Missile")); //$NON-NLS-1$
            }
        } else if (wtype.getDamage() == WeaponType.DAMAGE_VARIABLE) {
            wDamR.setText(Messages.getString("MechDisplay.Variable")); //$NON-NLS-1$
        } else if (wtype.getDamage() == WeaponType.DAMAGE_SPECIAL) {
            wDamR.setText(Messages.getString("MechDisplay.Special")); //$NON-NLS-1$
        } else if (wtype.getDamage() == WeaponType.DAMAGE_ARTILLERY) {
            StringBuffer damage = new StringBuffer();
            int artyDamage = wtype.getRackSize();
            damage.append(Integer.toString(artyDamage));
            artyDamage -= 10;
            while (artyDamage > 0) {
                damage.append('/').append(Integer.toString(artyDamage));
                artyDamage -= 10;
            }
            wDamR.setText(damage.toString());
        } else if (wtype.hasFlag(WeaponType.F_ENERGY)
                   && wtype.hasModes()
                   && (unitDisplay.getClientGUI() != null) && unitDisplay.getClientGUI().getClient().getGame().getOptions().booleanOption(
                OptionsConstants.ADVCOMBAT_TACOPS_ENERGY_WEAPONS)) {
            if (mounted.hasChargedCapacitor() != 0) {
                if (mounted.hasChargedCapacitor() == 1) {
                    wDamR.setText(Integer.toString(Compute.dialDownDamage(
                            mounted, wtype) + 5));
                }
                if (mounted.hasChargedCapacitor() == 2) {
                    wDamR.setText(Integer.toString(Compute.dialDownDamage(
                            mounted, wtype) + 10));
                }
            } else {
                wDamR.setText(Integer.toString(Compute.dialDownDamage(
                        mounted, wtype)));
            }
        } else {
            wDamR.setText(Integer.toString(wtype.getDamage()));
        }

        // update range
        int shortR = wtype.getShortRange();
        int mediumR = wtype.getMediumRange();
        int longR = wtype.getLongRange();
        int extremeR = wtype.getExtremeRange();
        if ((entity.getLocationStatus(mounted.getLocation()) == ILocationExposureStatus.WET)
            || (longR == 0)) {
            shortR = wtype.getWShortRange();
            mediumR = wtype.getWMediumRange();
            longR = wtype.getWLongRange();
            extremeR = wtype.getWExtremeRange();
        }
        // We need to adjust the ranges for Centurion Weapon Systems: it's
        //  default range is 6/12/18 but that's only for units that are
        //  susceptible to CWS, for those that aren't the ranges are 1/2/3
        if (wtype.hasFlag(WeaponType.F_CWS)) {
            Entity target = null;
            if ((unitDisplay.getClientGUI() != null)
                && (unitDisplay.getClientGUI().getCurrentPanel() instanceof FiringDisplay)) {
                Targetable t =
                        ((FiringDisplay) unitDisplay.getClientGUI().getCurrentPanel()).getTarget();
                if (t instanceof Entity) {
                    target = (Entity) t;
                }
            }
            if ((target == null) || !target.hasQuirk("susceptible_cws")) {
                shortR = 1;
                mediumR = 2;
                longR = 3;
                extremeR = 4;
            }
        }
        if (wtype.getMinimumRange() > 0) {
            wMinR.setText(Integer.toString(wtype.getMinimumRange()));
        } else {
            wMinR.setText("---"); //$NON-NLS-1$
        }
        if (shortR > 1) {
            wShortR.setText("1 - " + shortR); //$NON-NLS-1$
        } else {
            wShortR.setText("" + shortR); //$NON-NLS-1$
        }
        if ((mediumR - shortR) > 1) {
            wMedR.setText(shortR + 1 + " - " + mediumR); //$NON-NLS-1$
        } else {
            wMedR.setText("" + mediumR); //$NON-NLS-1$
        }
        if ((longR - mediumR) > 1) {
            wLongR.setText(mediumR + 1 + " - " + longR); //$NON-NLS-1$
        } else {
            wLongR.setText("" + longR); //$NON-NLS-1$
        }
        if ((extremeR - longR) > 1) {
            wExtR.setText(longR + 1 + " - " + extremeR); //$NON-NLS-1$
        } else {
            wExtR.setText("" + extremeR); //$NON-NLS-1$
        }

        // Update the range display to account for the weapon's loaded ammo.
        if (mounted.getLinked() != null) {
            updateRangeDisplayForAmmo(mounted.getLinked());
        }

        if (entity.isAirborne() || entity.usesWeaponBays()) {
            // change damage report to a statement of standard or capital
            if (wtype.isCapital()) {
                wDamR.setText(Messages.getString("MechDisplay.CapitalD")); //$NON-NLS-1$
            } else {
                wDamR.setText(Messages.getString("MechDisplay.StandardD")); //$NON-NLS-1$
            }

            // if this is a weapons bay, then I need to compile it to get
            // accurate results
            if (wtype instanceof BayWeapon) {
                compileWeaponBay(mounted, wtype.isCapital());
            } else {
                // otherwise I need to replace range display with standard
                // ranges and attack values
                updateAttackValues(mounted, mounted.getLinked());
            }

        }

        // update weapon bay selector
        int chosen = m_chBayWeapon.getSelectedIndex();
        m_chBayWeapon.removeAllItems();
        if (!(wtype instanceof BayWeapon) || !entity.usesWeaponBays()) {
            m_chBayWeapon.setEnabled(false);
        } else {
            m_chBayWeapon.setEnabled(true);
            for (int wId : mounted.getBayWeapons()) {
                Mounted curWeapon = entity.getEquipment(wId);
                if (null == curWeapon) {
                    continue;
                }

                m_chBayWeapon.addItem(formatBayWeapon(curWeapon));
            }

            if (chosen == -1) {
                m_chBayWeapon.setSelectedIndex(0);
            } else {
                m_chBayWeapon.setSelectedIndex(chosen);
            }
        }

        // update ammo selector
        ((DefaultComboBoxModel<String>) m_chAmmo.getModel()).removeAllElements();
        Mounted oldmount = mounted;
        if (wtype instanceof BayWeapon) {
            int n = m_chBayWeapon.getSelectedIndex();
            if (n == -1) {
                n = 0;
            }
            mounted = entity.getEquipment(mounted.getBayWeapons()
                                                 .elementAt(n));
            wtype = (WeaponType) mounted.getType();
        }
        if (wtype.getAmmoType() == AmmoType.T_NA) {
            m_chAmmo.setEnabled(false);
        } else if (wtype.hasFlag(WeaponType.F_ONESHOT)) {
            m_chAmmo.setEnabled(false);
            Mounted mountedAmmo = mounted.getLinked();
            if (mountedAmmo != null) {
                m_chAmmo.addItem(formatAmmo(mountedAmmo));
            }
        } else {
            m_chAmmo.setEnabled(true);
            vAmmo = new ArrayList<Mounted>();
            int nCur = -1;
            int i = 0;
            for (Mounted mountedAmmo : entity.getAmmo()) {
                AmmoType atype = (AmmoType) mountedAmmo.getType();
                // for all aero units other than fighters,
                // ammo must be located in the same place to be usable
                boolean same = true;
                if ((entity instanceof SmallCraft)
                    || (entity instanceof Jumpship)) {
                    same = (mounted.getLocation() == mountedAmmo
                            .getLocation());
                }

                boolean rightBay = true;
                if (entity.usesWeaponBays()
                    && !(entity instanceof FighterSquadron)) {
                    rightBay = oldmount.ammoInBay(entity
                                                          .getEquipmentNum(mountedAmmo));
                }

                if (mountedAmmo.isAmmoUsable() && same && rightBay
                    && (atype.getAmmoType() == wtype.getAmmoType())
                    && (atype.getRackSize() == wtype.getRackSize())) {

                    vAmmo.add(mountedAmmo);
                    m_chAmmo.addItem(formatAmmo(mountedAmmo));
                    if ((mounted.getLinked() != null) &&
                        mounted.getLinked().equals(mountedAmmo)) {
                        nCur = i;
                    }
                    i++;
                }
            }
            if (nCur != -1) {
                m_chAmmo.setSelectedIndex(nCur);
            }
        }

        // send event to other parts of the UI which care
        setFieldofFire(mounted);
        unitDisplay.processMechDisplayEvent(new MechDisplayEvent(this, entity, mounted));
        onResize();
        addListeners();
    }
    
    // this gathers all the range data 
    // it is a boiled down version of displaySelected,
    // updateAttackValues, updateRangeDisplayforAmmo
    private void setFieldofFire(Mounted mounted) {
        // No field of fire without ClientGUI
        if (unitDisplay.getClientGUI() == null) 
            return;
        
        ClientGUI gui = unitDisplay.getClientGUI();

        WeaponType wtype = (WeaponType) mounted.getType();
        int[][] ranges = new int[2][5];
        ranges[0] = wtype.getRanges(mounted);

        AmmoType atype = null;
        if (mounted.getLinked() != null) 
            atype = (AmmoType) mounted.getLinked().getType();

        // gather underwater ranges
        ranges[1] = wtype.getWRanges();
        if (atype != null) {
            if ((wtype.getAmmoType() == AmmoType.T_SRM)
                    || (wtype.getAmmoType() == AmmoType.T_MRM)
                    || (wtype.getAmmoType() == AmmoType.T_LRM)
                    || (wtype.getAmmoType() == AmmoType.T_MML)) {
                if (atype.getMunitionType() == AmmoType.M_TORPEDO) {
                    ranges[1] = wtype.getRanges(mounted);
                } else if (atype.getMunitionType() == AmmoType.M_MULTI_PURPOSE) {
                    ranges[1] = wtype.getRanges(mounted);
                }
            }
        }

        // Infantry range types 4+ are simplified to 
        // the usual range types as displaying 5 range circles
        // would be visual overkill (and besides this makes
        // things easier)
        if (wtype instanceof InfantryWeapon) {
            InfantryWeapon inftype = (InfantryWeapon) wtype;
            int iR = inftype.getInfantryRange();
            ranges[0] = 
                    new int[] { 0, iR, iR * 2, iR * 3, 0 };
        }

        // Artillery gets fixed ranges, 100 as an arbitrary
        // large range for the targeting phase and
        // 6 to 17 in the other phases as it will be
        // direct fire then
        if (wtype.hasFlag(WeaponType.F_ARTILLERY)) {
            if (gui.getCurrentPanel() instanceof TargetingPhaseDisplay) {
                ranges[0] = new int[] { 0, 0, 0, 100, 0 };  
                ranges[1] = new int[] { 0, 0, 0, 0, 0 };
            } else {
                ranges[0] = new int[] { 6, 0, 0, 17, 0 };  
                ranges[1] = new int[] { 0, 0, 0, 0, 0 };
            }
        }

        // Override for the various ATM and MML ammos
        if (atype != null) {
            if (atype.getAmmoType() == AmmoType.T_ATM) {
                if (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) 
                    ranges[0] = new int[] { 4, 9, 18, 27, 36 }; 
                else if (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) 
                    ranges[0] = new int[] { 0, 3, 6, 9, 12 };
                else 
                    ranges[0] = new int[] { 4, 5, 10, 15, 20 };
            } 
            else if (atype.getAmmoType() == AmmoType.T_MML) {
                if (atype.hasFlag(AmmoType.F_MML_LRM)) 
                    ranges[0] = new int[] { 6, 7, 14, 21, 28 };
                else 
                    ranges[0] = new int[] { 0, 3, 6, 9, 12 };
            } else if (atype.getAmmoType() == AmmoType.T_IATM) {
                if (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) 
                    ranges[0] = new int[] { 4, 9, 18, 27, 36 };
                else if (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) 
                    ranges[0] = new int[] { 0, 3, 6, 9, 12 };
                else if (atype.getMunitionType() == AmmoType.M_IATM_IMP) 
                    ranges[0] = new int[] { 0, 3, 6, 9, 12 };
                else  
                    ranges[0] = new int[] { 4, 5, 10, 15, 20 };
            }
        }

        // No minimum range for hotload
        if ((mounted.getLinked() != null) && mounted.getLinked().isHotLoaded()) {
            ranges[0][0] = 0;
        }

        // Aero
        if (entity.isAirborne() 
                || entity.usesWeaponBays()) {

            // prepare fresh ranges, no underwater
            ranges[0] = new int[] { 0, 0, 0, 0, 0 };  
            ranges[1] = new int[] { 0, 0, 0, 0, 0 };
            int maxr = WeaponType.RANGE_SHORT;

            maxr = WeaponType.RANGE_SHORT;
            // In the WeaponPanel, when the weapon is out of ammo
            // or otherwise nonfunctional, SHORT range will be listed;
            // the field of fire is instead disabled
            // Here as well as in WeaponPanel, choosing a specific ammo
            // only works for the current player's units
            if (!mounted.isBreached() && !mounted.isMissing() 
                    && !mounted.isDestroyed() && !mounted.isJammed() 
                    && ((mounted.getLinked() == null) 
                            || (mounted.getLinked().getUsableShotsLeft() > 0))) {
                maxr = wtype.getMaxRange(mounted);

                if (atype != null) {
                    if (atype.getAmmoType() == AmmoType.T_ATM) {
                        if (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
                            maxr = WeaponType.RANGE_EXT;
                        } else if (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
                            maxr = WeaponType.RANGE_SHORT;
                        }
                    } else if (atype.getAmmoType() == AmmoType.T_MML) {
                        if (!atype.hasFlag(AmmoType.F_MML_LRM)) {
                            maxr = WeaponType.RANGE_SHORT; 
                        }
                    } 
                }

                // set the standard ranges, depending on capital or no
                boolean isCap = wtype.isCapital();
                ranges[0][0] = 0;
                ranges[0][1] = isCap ? 12 : 6;
                if (maxr > WeaponType.RANGE_SHORT) 
                    ranges[0][2] = isCap ? 24 : 12;
                if (maxr > WeaponType.RANGE_MED)
                    ranges[0][3] = isCap ? 40 : 20;
                if (maxr > WeaponType.RANGE_LONG) 
                    ranges[0][4] = isCap ? 50 : 25;
                
                final IGame game = unitDisplay.getClientGUI().getClient().getGame();
                if (game.getBoard().onGround()) {
                    ranges[0][1] *= 8;
                    ranges[0][2] *= 8;
                    ranges[0][3] *= 8;
                    ranges[0][4] *= 8;
                }
            }
        }
        
        // pass all this to the Displays
        int arc = entity.getWeaponArc(entity.getEquipmentNum(mounted));
        int loc = mounted.getLocation();
        
        if (gui.getCurrentPanel() instanceof FiringDisplay) {
            // twisting
            int facing = entity.getFacing();
            if (entity.isSecondaryArcWeapon(entity.getEquipmentNum(mounted))) 
                facing = entity.getSecondaryFacing();
            ((FiringDisplay) gui.getCurrentPanel()).FieldofFire(entity, ranges, arc, loc, facing);
        } else if (gui.getCurrentPanel() instanceof TargetingPhaseDisplay) {
            // twisting
            int facing = entity.getFacing();
            if (entity.isSecondaryArcWeapon(entity.getEquipmentNum(mounted))) 
                facing = entity.getSecondaryFacing();
            ((TargetingPhaseDisplay) gui.getCurrentPanel()).FieldofFire(entity, ranges, arc, loc, facing);
        } else if (gui.getCurrentPanel() instanceof MovementDisplay) {
            // no twisting here
            ((MovementDisplay)gui.getCurrentPanel()).FieldofFire(entity, ranges, arc, loc);
        }
    }

    private String formatAmmo(Mounted m) {
        StringBuffer sb = new StringBuffer(64);
        int ammoIndex = m.getDesc().indexOf(
                Messages.getString("MechDisplay.0")); //$NON-NLS-1$
        int loc = m.getLocation();
        if (loc != Entity.LOC_NONE) {
            sb.append('[').append(entity.getLocationAbbr(loc)).append("] "); //$NON-NLS-1$
        }
        if (ammoIndex == -1) {
            sb.append(m.getDesc());
        } else {
            sb.append(m.getDesc().substring(0, ammoIndex));
            sb.append(m.getDesc().substring(ammoIndex + 4));
        }
        return sb.toString();
    }

    private String formatBayWeapon(Mounted m) {
        StringBuffer sb = new StringBuffer(64);
        sb.append(m.getDesc());
        return sb.toString();
    }

    /**
     * Update the range display for the selected ammo.
     *
     * @param mAmmo - the <code>AmmoType</code> of the weapon's loaded ammo.
     */
    private void updateRangeDisplayForAmmo(Mounted mAmmo) {
        if (!(mAmmo.getType() instanceof AmmoType)) {
            return;
        }
        AmmoType atype = (AmmoType) mAmmo.getType();
        // Only override the display for the various ATM and MML ammos
        if (atype.getAmmoType() == AmmoType.T_ATM) {
            if (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
                wMinR.setText("4"); //$NON-NLS-1$
                wShortR.setText("1 - 9"); //$NON-NLS-1$
                wMedR.setText("10 - 18"); //$NON-NLS-1$
                wLongR.setText("19 - 27"); //$NON-NLS-1$
                wExtR.setText("28 - 36"); //$NON-NLS-1$
            } else if (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
                wMinR.setText("---"); //$NON-NLS-1$
                wShortR.setText("1 - 3"); //$NON-NLS-1$
                wMedR.setText("4 - 6"); //$NON-NLS-1$
                wLongR.setText("7 - 9"); //$NON-NLS-1$
                wExtR.setText("10 - 12"); //$NON-NLS-1$
            } else {
                wMinR.setText("4"); //$NON-NLS-1$
                wShortR.setText("1 - 5"); //$NON-NLS-1$
                wMedR.setText("6 - 10"); //$NON-NLS-1$
                wLongR.setText("11 - 15"); //$NON-NLS-1$
                wExtR.setText("16 - 20"); //$NON-NLS-1$
            }
        } // End weapon-is-ATM
        else if (atype.getAmmoType() == AmmoType.T_MML) {
            if (atype.hasFlag(AmmoType.F_MML_LRM)) {
                wMinR.setText("6"); //$NON-NLS-1$
                wShortR.setText("1 - 7"); //$NON-NLS-1$
                wMedR.setText("8 - 14"); //$NON-NLS-1$
                wLongR.setText("15 - 21"); //$NON-NLS-1$
                wExtR.setText("21 - 28"); //$NON-NLS-1$
            } else {
                wMinR.setText("---"); //$NON-NLS-1$
                wShortR.setText("1 - 3"); //$NON-NLS-1$
                wMedR.setText("4 - 6"); //$NON-NLS-1$
                wLongR.setText("7 - 9"); //$NON-NLS-1$
                wExtR.setText("10 - 12"); //$NON-NLS-1$
            }
        } else if (atype.getAmmoType() == AmmoType.T_IATM) {
            if (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
                wMinR.setText("4"); //$NON-NLS-1$
                wShortR.setText("1 - 9"); //$NON-NLS-1$
                wMedR.setText("10 - 18"); //$NON-NLS-1$
                wLongR.setText("19 - 27"); //$NON-NLS-1$
                wExtR.setText("28 - 36"); //$NON-NLS-1$
            } else if (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
                wMinR.setText("---"); //$NON-NLS-1$
                wShortR.setText("1 - 3"); //$NON-NLS-1$
                wMedR.setText("4 - 6"); //$NON-NLS-1$
                wLongR.setText("7 - 9"); //$NON-NLS-1$
                wExtR.setText("10 - 12"); //$NON-NLS-1$
            } else if (atype.getMunitionType() == AmmoType.M_IATM_IIW) {
                wMinR.setText("4"); //$NON-NLS-1$
                wShortR.setText("1 - 5"); //$NON-NLS-1$
                wMedR.setText("6 - 10"); //$NON-NLS-1$
                wLongR.setText("11 - 15"); //$NON-NLS-1$
                wExtR.setText("16 - 20"); //$NON-NLS-1$
            } else if (atype.getMunitionType() == AmmoType.M_IATM_IMP) {
                wMinR.setText("---"); //$NON-NLS-1$
                wShortR.setText("1 - 3"); //$NON-NLS-1$
                wMedR.setText("4 - 6"); //$NON-NLS-1$
                wLongR.setText("7 - 9"); //$NON-NLS-1$
                wExtR.setText("10 - 12"); //$NON-NLS-1$
            } else /* standard */ {
                wMinR.setText("4"); //$NON-NLS-1$
                wShortR.setText("1 - 5"); //$NON-NLS-1$
                wMedR.setText("6 - 10"); //$NON-NLS-1$
                wLongR.setText("11 - 15"); //$NON-NLS-1$
                wExtR.setText("16 - 20"); //$NON-NLS-1$
            }
        }

        // Min range 0 for hotload
        if (mAmmo.isHotLoaded()) {
            wMinR.setText("---");
        }

        onResize();
    } // End private void updateRangeDisplayForAmmo( AmmoType )

    private void updateAttackValues(Mounted weapon, Mounted ammo) {
        WeaponType wtype = (WeaponType) weapon.getType();
        // update Attack Values and change range
        int avShort = wtype.getRoundShortAV();
        int avMed = wtype.getRoundMedAV();
        int avLong = wtype.getRoundLongAV();
        int avExt = wtype.getRoundExtAV();
        int maxr = wtype.getMaxRange(weapon);

        // change range and attack values based upon ammo
        if (null != ammo) {
            AmmoType atype = (AmmoType) ammo.getType();
            double[] changes = changeAttackValues(atype, avShort, avMed,
                                                  avLong, avExt, maxr);
            avShort = (int) changes[0];
            avMed = (int) changes[1];
            avLong = (int) changes[2];
            avExt = (int) changes[3];
            maxr = (int) changes[4];
        }

        if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY) && wtype.isCapital()) {
            avShort *= 10;
            avMed *= 10;
            avLong *= 10;
            avExt *= 10;
        }

        // set default values in case if statement stops
        wShortAVR.setText("---"); //$NON-NLS-1$
        wMedAVR.setText("---"); //$NON-NLS-1$
        wLongAVR.setText("---"); //$NON-NLS-1$
        wExtAVR.setText("---"); //$NON-NLS-1$
        wShortR.setText("---"); //$NON-NLS-1$
        wMedR.setText("---"); //$NON-NLS-1$
        wLongR.setText("---"); //$NON-NLS-1$
        wExtR.setText("---"); //$NON-NLS-1$
        // every weapon gets at least short range
        wShortAVR.setText(Integer.toString(avShort));
        if (wtype.isCapital()) {
            wShortR.setText("1-12"); //$NON-NLS-1$
        } else {
            wShortR.setText("1-6"); //$NON-NLS-1$
        }
        if (maxr > WeaponType.RANGE_SHORT) {
            wMedAVR.setText(Integer.toString(avMed));
            if (wtype.isCapital()) {
                wMedR.setText("13-24"); //$NON-NLS-1$
            } else {
                wMedR.setText("7-12"); //$NON-NLS-1$
            }
        }
        if (maxr > WeaponType.RANGE_MED) {
            wLongAVR.setText(Integer.toString(avLong));
            if (wtype.isCapital()) {
                wLongR.setText("25-40"); //$NON-NLS-1$
            } else {
                wLongR.setText("13-20"); //$NON-NLS-1$
            }
        }
        if (maxr > WeaponType.RANGE_LONG) {
            wExtAVR.setText(Integer.toString(avExt));
            if (wtype.isCapital()) {
                wExtR.setText("41-50"); //$NON-NLS-1$
            } else {
                wExtR.setText("21-25"); //$NON-NLS-1$
            }
        }

        onResize();
    }

    private double[] changeAttackValues(AmmoType atype, double avShort,
                                        double avMed, double avLong, double avExt, int maxr) {

        if (AmmoType.T_ATM == atype.getAmmoType()) {
            if (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
                maxr = WeaponType.RANGE_EXT;
                avShort = avShort / 2;
                avMed = avMed / 2;
                avLong = avMed;
                avExt = avMed;
            } else if (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
                maxr = WeaponType.RANGE_SHORT;
                avShort = avShort + (avShort / 2);
                avMed = 0;
                avLong = 0;
                avExt = 0;
            }
        } // End weapon-is-ATM
        else if (atype.getAmmoType() == AmmoType.T_MML) {
            // first check for artemis
            int bonus = 0;
            if (atype.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE) {
                int rack = atype.getRackSize();
                if (rack == 5) {
                    bonus += 1;
                } else if (rack >= 7) {
                    bonus += 2;
                }
                avShort = avShort + bonus;
                avMed = avMed + bonus;
                avLong = avLong + bonus;
            }
            if (!atype.hasFlag(AmmoType.F_MML_LRM)) {
                maxr = WeaponType.RANGE_SHORT;
                avShort = avShort * 2;
                avMed = 0;
                avLong = 0;
                avExt = 0;
            }
        } // end weapon is MML
        else if ((atype.getAmmoType() == AmmoType.T_LRM)
                 || (atype.getAmmoType() == AmmoType.T_SRM)) {

            if (atype.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE) {
                if (atype.getAmmoType() == AmmoType.T_LRM) {
                    int bonus = (int) Math.ceil(atype.getRackSize() / 5.0);
                    avShort = avShort + bonus;
                    avMed = avMed + bonus;
                    avLong = avLong + bonus;
                }
                if (atype.getAmmoType() == AmmoType.T_SRM) {
                    avShort = avShort + 2;
                }
            }
        } else if (atype.getAmmoType() == AmmoType.T_AC_LBX) {
            if (atype.getMunitionType() == AmmoType.M_CLUSTER) {
                int newAV = (int) Math.floor(0.6 * atype.getRackSize());
                avShort = newAV;
                if (avMed > 0) {
                    avMed = newAV;
                }
                if (avLong > 0) {
                    avLong = newAV;
                }
                if (avExt > 0) {
                    avExt = newAV;
                }
            }
        } else if (atype.getAmmoType() == AmmoType.T_AR10) {
            if (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                avShort = 4;
                avMed = 4;
                avLong = 4;
                avExt = 4;
            } else if (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                avShort = 3;
                avMed = 3;
                avLong = 3;
                avExt = 3;
            } else {
                avShort = 2;
                avMed = 2;
                avLong = 2;
                avExt = 2;
            }
        }

        double[] result = {avShort, avMed, avLong, avExt, maxr};
        return result;

    }

    private void compileWeaponBay(Mounted weapon, boolean isCapital) {

        Vector<Integer> bayWeapons = weapon.getBayWeapons();
        WeaponType wtype = (WeaponType) weapon.getType();

        // set default values in case if statement stops
        wShortAVR.setText("---"); //$NON-NLS-1$
        wMedAVR.setText("---"); //$NON-NLS-1$
        wLongAVR.setText("---"); //$NON-NLS-1$
        wExtAVR.setText("---"); //$NON-NLS-1$
        wShortR.setText("---"); //$NON-NLS-1$
        wMedR.setText("---"); //$NON-NLS-1$
        wLongR.setText("---"); //$NON-NLS-1$
        wExtR.setText("---"); //$NON-NLS-1$

        int heat = 0;
        double avShort = 0;
        double avMed = 0;
        double avLong = 0;
        double avExt = 0;
        int maxr = WeaponType.RANGE_SHORT;

        for (int wId : bayWeapons) {
            Mounted m = entity.getEquipment(wId);
            if (!m.isBreached()
                && !m.isMissing()
                && !m.isDestroyed()
                && !m.isJammed()
                && ((m.getLinked() == null) || (m.getLinked()
                                                 .getUsableShotsLeft() > 0))) {
                WeaponType bayWType = ((WeaponType) m.getType());
                heat = heat + m.getCurrentHeat();
                double mAVShort = bayWType.getShortAV();
                double mAVMed = bayWType.getMedAV();
                double mAVLong = bayWType.getLongAV();
                double mAVExt = bayWType.getExtAV();
                int mMaxR = bayWType.getMaxRange(m);

                // deal with any ammo adjustments
                if (null != m.getLinked()) {
                    double[] changes = changeAttackValues((AmmoType) m
                                                                  .getLinked().getType(), mAVShort, mAVMed,
                                                          mAVLong, mAVExt, mMaxR);
                    mAVShort = changes[0];
                    mAVMed = changes[1];
                    mAVLong = changes[2];
                    mAVExt = changes[3];
                    mMaxR = (int) changes[4];
                }

                avShort = avShort + mAVShort;
                avMed = avMed + mAVMed;
                avLong = avLong + mAVLong;
                avExt = avExt + mAVExt;
                if (mMaxR > maxr) {
                    maxr = mMaxR;
                }
            }
        }
        // check for bracketing
        double mult = 1.0;
        if (wtype.hasModes() && weapon.curMode().equals("Bracket 80%")) {
            mult = 0.8;
        }
        if (wtype.hasModes() && weapon.curMode().equals("Bracket 60%")) {
            mult = 0.6;
        }
        if (wtype.hasModes() && weapon.curMode().equals("Bracket 40%")) {
            mult = 0.4;
        }
        avShort = mult * avShort;
        avMed = mult * avMed;
        avLong = mult * avLong;
        avExt = mult * avExt;

        if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY) && wtype.isCapital()) {
            avShort *= 10;
            avMed *= 10;
            avLong *= 10;
            avExt *= 10;
        }

        wHeatR.setText(Integer.toString(heat));
        wShortAVR.setText(Integer.toString((int) Math.ceil(avShort)));
        if (isCapital) {
            wShortR.setText("1-12"); //$NON-NLS-1$
        } else {
            wShortR.setText("1-6"); //$NON-NLS-1$
        }
        if (maxr > WeaponType.RANGE_SHORT) {
            wMedAVR.setText(Integer.toString((int) Math.ceil(avMed)));
            if (isCapital) {
                wMedR.setText("13-24"); //$NON-NLS-1$
            } else {
                wMedR.setText("7-12"); //$NON-NLS-1$
            }
        }
        if (maxr > WeaponType.RANGE_MED) {
            wLongAVR.setText(Integer.toString((int) Math.ceil(avLong)));
            if (isCapital) {
                wLongR.setText("25-40"); //$NON-NLS-1$
            } else {
                wLongR.setText("13-20"); //$NON-NLS-1$
            }
        }
        if (maxr > WeaponType.RANGE_LONG) {
            wExtAVR.setText(Integer.toString((int) Math.ceil(avExt)));
            if (isCapital) {
                wExtR.setText("41-50"); //$NON-NLS-1$
            } else {
                wExtR.setText("21-25"); //$NON-NLS-1$
            }
        }
        onResize();
    }

    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        if (event.getSource().equals(weaponList)) {
            displaySelected();
            
            // Can't do anything if ClientGUI is null
            if (unitDisplay.getClientGUI() == null) {
                return;
            }
            
            JComponent currPanel = unitDisplay.getClientGUI().getCurrentPanel();
            // When in the Firing Phase, update the targeting information.
            if (currPanel instanceof FiringDisplay) {
                FiringDisplay firingDisplay = (FiringDisplay)currPanel;

                Mounted mounted = null;
                WeaponListModel weaponModel = (WeaponListModel) weaponList
                        .getModel();
                if (weaponList.getSelectedIndex() != -1) {
                    mounted = weaponModel.getWeaponAt(weaponList
                            .getSelectedIndex());
                }
                Mounted prevMounted = null;
                if ((event.getLastIndex() != -1) 
                        && (event.getLastIndex() < weaponModel.getSize())) {
                    prevMounted = weaponModel.getWeaponAt(event.getLastIndex());
                }
                // Some weapons have a specific target, which gets handled
                // in the target method
                if (mounted != null
                        && mounted.getType().hasFlag(WeaponType.F_VGL)) {
                    // Store previous target, if it's a weapon that doesn't 
                    // have a forced target
                    if ((prevMounted != null)
                            && !prevMounted.getType().hasFlag(WeaponType.F_VGL)) {
                        prevTarget = firingDisplay.getTarget();
                    }                    
                    firingDisplay.target(null);
                } else {
                    if (prevTarget != null) {
                        firingDisplay.target(prevTarget);
                        unitDisplay.getClientGUI().getBoardView()
                                .select(prevTarget.getPosition());
                        prevTarget = null;
                    } else {
                        firingDisplay.updateTarget();
                    }
                }
            } else if (currPanel instanceof TargetingPhaseDisplay) {
                ((TargetingPhaseDisplay) currPanel).updateTarget();
            }
            
            // Tell the <Phase>Display to update the 
            // firing arc info when a weapon has been de-selected
            if (weaponList.getSelectedIndex() == -1) {
                unitDisplay.getClientGUI().bv.clearFieldofF();
            }
        }
        onResize();
    }

    public void actionPerformed(ActionEvent ev) {
        ClientGUI clientgui = unitDisplay.getClientGUI();
        if (ev.getSource().equals(m_chAmmo)
                && (m_chAmmo.getSelectedIndex() != -1)
                && (clientgui != null)) {
            // only change our own units
            if (!clientgui.getClient().getLocalPlayer()
                    .equals(entity.getOwner())) {
                return;
            }
            int n = weaponList.getSelectedIndex();
            if (weaponList.getSelectedIndex() == -1) {
                return;
            }
            Mounted mWeap = ((WeaponListModel) weaponList.getModel())
                    .getWeaponAt(n);
            Mounted oldWeap = mWeap;
            Mounted oldAmmo = mWeap.getLinked();
            Mounted mAmmo = vAmmo.get(m_chAmmo.getSelectedIndex());
            // if this is a weapon bay, then this is not what we want
            boolean isBay = false;
            if (mWeap.getType() instanceof BayWeapon) {
                isBay = true;
                n = m_chBayWeapon.getSelectedIndex();
                if (n == -1) {
                    return;
                }
                //
                Mounted sWeap = entity.getEquipment(mWeap.getBayWeapons()
                        .elementAt(n));
                // cycle through all weapons of the same type and load with
                // this ammo
                for (int wid : mWeap.getBayWeapons()) {
                    Mounted bWeap = entity.getEquipment(wid);
                    if (bWeap.getType().equals(sWeap.getType())) {
                        entity.loadWeapon(bWeap, mAmmo);
                        // Alert the server of the update.
                        clientgui.getClient().sendAmmoChange(
                                entity.getId(),
                                entity.getEquipmentNum(bWeap),
                                entity.getEquipmentNum(mAmmo));
                    }
                }
            } else {
                entity.loadWeapon(mWeap, mAmmo);
                // Alert the server of the update.
                clientgui.getClient().sendAmmoChange(entity.getId(),
                        entity.getEquipmentNum(mWeap),
                        entity.getEquipmentNum(mAmmo));
            }

            // Refresh for hot load change
            if ((((oldAmmo == null) || !oldAmmo.isHotLoaded()) && mAmmo
                    .isHotLoaded())
                    || ((oldAmmo != null) && oldAmmo.isHotLoaded() && !mAmmo
                            .isHotLoaded())) {
                displayMech(entity);
                weaponList.setSelectedIndex(n);
                weaponList.ensureIndexIsVisible(n);
                displaySelected();
            }

            // Update the range display to account for the weapon's loaded
            // ammo.
            updateRangeDisplayForAmmo(mAmmo);
            if (entity.isAirborne() || entity.usesWeaponBays()) {
                WeaponType wtype = (WeaponType) mWeap.getType();
                if (isBay) {
                    compileWeaponBay(oldWeap, wtype.isCapital());
                } else {
                    // otherwise I need to replace range display with
                    // standard ranges and attack values
                    updateAttackValues(mWeap, mAmmo);
                }
            }

            // When in the Firing Phase, update the targeting information.
            if (clientgui.getCurrentPanel() instanceof FiringDisplay) {
                ((FiringDisplay) clientgui.getCurrentPanel()).updateTarget();
            } else if (clientgui.getCurrentPanel() instanceof TargetingPhaseDisplay) {
                ((TargetingPhaseDisplay) clientgui.getCurrentPanel()).updateTarget();
            }
            displaySelected();
        } else if (ev.getSource().equals(m_chBayWeapon)
                   && (m_chBayWeapon.getItemCount() > 0)) {
            int n = weaponList.getSelectedIndex();
            if (n == -1) {
                return;
            }
            displaySelected();
        } else if (ev.getSource().equals(weapSortOrder)) {
            setWeaponComparator(weapSortOrder.getSelectedIndex());
        }
        onResize();
    }

    void setWeaponComparator(int sortIdx) {
        Comparator<Mounted> weapComparator;
        if (sortIdx == Entity.WeaponSortOrder.RANGE_LH.ordinal()) {
            entity.setWeaponSortOrder(Entity.WeaponSortOrder.RANGE_LH);
            weapComparator = new WeaponComparatorRange(true);
        } else if (sortIdx == Entity.WeaponSortOrder.RANGE_HL.ordinal()) {
            entity.setWeaponSortOrder(Entity.WeaponSortOrder.RANGE_HL);
            weapComparator = new WeaponComparatorRange(false);
        } else if (sortIdx == Entity.WeaponSortOrder.DAMAGE_LH.ordinal()) {
            entity.setWeaponSortOrder(Entity.WeaponSortOrder.DAMAGE_LH);
            weapComparator = new WeaponComparatorDamage(true);
        } else if (sortIdx == Entity.WeaponSortOrder.DAMAGE_HL.ordinal()) {
            entity.setWeaponSortOrder(Entity.WeaponSortOrder.DAMAGE_HL);
            weapComparator = new WeaponComparatorDamage(false);
        } else if (sortIdx == Entity.WeaponSortOrder.CUSTOM.ordinal()) {
            entity.setWeaponSortOrder(Entity.WeaponSortOrder.CUSTOM);
            weapComparator = new WeaponComparatorCustom(entity);
        } else if (sortIdx == Entity.WeaponSortOrder.ARC.ordinal()) {
            entity.setWeaponSortOrder(Entity.WeaponSortOrder.ARC);
            weapComparator = new WeaponComparatorArc(entity);
        } else { // Default
            entity.setWeaponSortOrder(Entity.WeaponSortOrder.DEFAULT);
            weapComparator = new WeaponComparatorNum(entity);
        }
        if (unitDisplay.getClientGUI() != null) {
            unitDisplay.getClientGUI().getMenuBar()
                    .updateSaveWeaponOrderMenuItem();
        }
        ((WeaponListModel)weaponList.getModel()).sort(weapComparator);
    }
    
    private void addListeners() {
        weapSortOrder.addActionListener(this);
        m_chAmmo.addActionListener(this);
        m_chBayWeapon.addActionListener(this);
        
        weaponList.addListSelectionListener(this);
    }
    
    private void removeListeners() {
        weapSortOrder.removeActionListener(this);
        m_chAmmo.removeActionListener(this);
        m_chBayWeapon.removeActionListener(this);
        
        weaponList.removeListSelectionListener(this);
    }

    public Targetable getPrevTarget() {
        return prevTarget;
    }

    public void setPrevTarget(Targetable prevTarget) {
        this.prevTarget = prevTarget;
    }
}
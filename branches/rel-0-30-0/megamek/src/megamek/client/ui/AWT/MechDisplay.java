/**
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.client.ui.AWT;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Enumeration;

import megamek.client.Client;
import megamek.client.ui.AWT.widget.*;
import megamek.common.*;

/**
 * Displays the info for a mech.  This is also a sort
 * of interface for special movement and firing actions.
 */
public class MechDisplay extends BufferedPanel {
    // buttons & gizmos for top level

    MechPanelTabStrip tabStrip;


    public BufferedPanel        displayP;
    public MovementPanel        mPan;
    public ArmorPanel           aPan;
    public WeaponPanel          wPan;
    public SystemPanel          sPan;
    public ExtraPanel           ePan;
    private ClientGUI           clientgui;
    private Client              client;

    private Entity              currentlyDisplaying = null;

    /**
     * Creates and lays out a new mech display.
     */
    public MechDisplay(ClientGUI clientgui) {
        super(new GridBagLayout());

        this.clientgui = clientgui;
        this.client = clientgui.getClient();

        tabStrip = new MechPanelTabStrip(this);

        displayP = new BufferedPanel(new CardLayout());
        mPan = new MovementPanel();
        displayP.add("movement", mPan); //$NON-NLS-1$
        aPan = new ArmorPanel();
        displayP.add("armor", aPan); //$NON-NLS-1$
        wPan = new WeaponPanel(clientgui);
        displayP.add("weapons", wPan); //$NON-NLS-1$
        sPan = new SystemPanel(clientgui);
        displayP.add("systems", sPan); //$NON-NLS-1$
        ePan = new ExtraPanel(clientgui);
        displayP.add("extras", ePan); //$NON-NLS-1$

        // layout main panel
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 1, 0, 1);

        c.weightx = 1.0;    c.weighty = 0.0;

        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(tabStrip, c);
        c.insets = new Insets(0, 1, 1, 1);
        c.weighty = 1.0;
        addBag(displayP, c);

        ((CardLayout)displayP.getLayout()).show(displayP, "movement"); //$NON-NLS-1$
        //tabStrip.setTab(0);
        
        clientgui.mechW.addKeyListener(clientgui.menuBar);
    }

    public void addBag(Component comp, GridBagConstraints c) {
        ((GridBagLayout)getLayout()).setConstraints(comp, c);
        add(comp);
    }

    /**
     * Displays the specified entity in the panel.
     */
    public void displayEntity(Entity en) {

        clientgui.mechW.setTitle(en.getShortName());

        this.currentlyDisplaying = en;

        mPan.displayMech(en);
        aPan.displayMech(en);
        wPan.displayMech(en);
        sPan.displayMech(en);
        ePan.displayMech(en);
    }

    /**
     * Returns the entity we'return currently displaying
     */

    public Entity getCurrentEntity() {
        return currentlyDisplaying;
    }


    /**
     * Changes to the specified panel.
     */
    public void showPanel(String s) {
        ((CardLayout) displayP.getLayout()).show(displayP, s);
        if (s == "movement") { //$NON-NLS-1$
            tabStrip.setTab(0);
        } else if (s == "armor") { //$NON-NLS-1$
            tabStrip.setTab(1);
        } else if (s == "weapons") { //$NON-NLS-1$
            tabStrip.setTab(3);
        } else if (s == "systems") { //$NON-NLS-1$
            tabStrip.setTab(2);
        } else if (s == "extras") { //$NON-NLS-1$
            tabStrip.setTab(4);
        }
    }

}
/**
 * The movement panel contains all the buttons, readouts
 * and gizmos relating to moving around on the
 * battlefield.
 */
class MovementPanel extends PicMap{

    private GeneralInfoMapSet gi;

    private int minTopMargin = 8;
    private int minLeftMargin = 8;

    public MovementPanel() {
        gi = new GeneralInfoMapSet(this);
        addElement(gi.getContentGroup());
        Vector v = gi.getBackgroundDrawers();
        Enumeration iter = v.elements();
        while(iter.hasMoreElements()){
          addBgDrawer( (BackGroundDrawer) iter.nextElement());
        }
        onResize();
    }

    public void addNotify(){
        super.addNotify();
        update();
    }

    public void onResize(){
      int w = getSize().width;
      Rectangle r = getContentBounds();
      int dx = Math.round((w - r.width)/2);
      if (dx < minLeftMargin) dx = minLeftMargin;
      int dy = minTopMargin;
        if( r != null) setContentMargins(dx, dy, dx, dy);
    }

    /**
     * updates fields for the specified mech
     */
    public void displayMech(Entity en) {
       gi.setEntity(en);
       onResize();
       update();
    }

}

/**
 * This panel contains the armor readout display.
 */
class ArmorPanel  extends PicMap
{
    private TankMapSet tank;
    private MechMapSet mech;
    private InfantryMapSet infantry;
    private BattleArmorMapSet battleArmor;
    private ProtomechMapSet proto;
    private VTOLMapSet vtol;
    private QuadMapSet quad;
    private int minTopMargin = 0;
    private int minLeftMargin = 0;
    private int minBottomMargin = 0;
    private int minRightMargin = 0;

    private static final int minTankTopMargin = 8;
    private static final int minTankLeftMargin = 8;
    private static final int minVTOLTopMargin = 8;
    private static final int minVTOLLeftMargin = 8;
    private static final int minMechTopMargin = 18;
    private static final int minMechLeftMargin = 7;
    private static final int minMechBottomMargin = 0;
    private static final int minMechRightMargin = 0;
    private static final int minInfTopMargin = 8;
    private static final int minInfLeftMargin = 8;

    public void addNotify() {
        super.addNotify();
        tank = new TankMapSet(this);
        mech = new MechMapSet(this);
        infantry = new InfantryMapSet(this);
        battleArmor = new BattleArmorMapSet(this);
        proto = new ProtomechMapSet(this);
        vtol = new VTOLMapSet(this);
        quad = new QuadMapSet(this);
    }

    public void onResize(){
        Rectangle r = getContentBounds();
        if( r == null) return;
        int w = Math.round((getSize().width - r.width)/2);
        int h = Math.round((getSize().height - r.height)/2);
        int dx = (w < minLeftMargin) ? minLeftMargin : w;
        int dy = (h < minTopMargin) ? minTopMargin : h;
        setContentMargins(dx, dy, minRightMargin, minBottomMargin);
    }

    /**
     * updates fields for the specified mech
     */
    public void displayMech(Entity en) {
        // Look out for a race condition.
        if (null == en) {
            return;
        }
        DisplayMapSet ams = mech;
        removeAll();
        if (en instanceof QuadMech) {
            ams = quad;
            minLeftMargin = minMechLeftMargin;
            minTopMargin = minMechTopMargin;
            minBottomMargin = minMechBottomMargin;
            minRightMargin = minMechRightMargin;
        } else if (en instanceof Mech) {
            ams = mech;
            minLeftMargin = minMechLeftMargin;
            minTopMargin = minMechTopMargin;
            minBottomMargin = minMechBottomMargin;
            minRightMargin = minMechRightMargin;
        } else if (en instanceof VTOL) {
            ams = vtol;
            minLeftMargin = minVTOLLeftMargin;
            minTopMargin = minVTOLTopMargin;
            minBottomMargin = minVTOLTopMargin;
            minRightMargin = minVTOLLeftMargin;
        } else if (en instanceof Tank) {
            ams = tank;
            minLeftMargin = minTankLeftMargin;
            minTopMargin = minTankTopMargin;
            minBottomMargin = minTankTopMargin;
            minRightMargin = minTankLeftMargin;
        } else if (en instanceof BattleArmor) {
            ams = battleArmor;
            minLeftMargin = minInfLeftMargin;
            minTopMargin = minInfTopMargin;
            minBottomMargin = minInfTopMargin;
            minRightMargin = minInfLeftMargin;

        } else if (en instanceof Infantry) {
            ams = infantry;
            minLeftMargin = minInfLeftMargin;
            minTopMargin = minInfTopMargin;
            minBottomMargin = minInfTopMargin;
            minRightMargin = minInfLeftMargin;

        } else if (en instanceof Protomech) {
            ams = proto;
            minLeftMargin = minTankLeftMargin;
            minTopMargin = minTankTopMargin;
            minBottomMargin = minTankTopMargin;
            minRightMargin = minTankLeftMargin;
        }
        if (null == ams) {
            System.err.println("The armor panel is null."); //$NON-NLS-1$
            return;
        }
        ams.setEntity(en);
        this.addElement(ams.getContentGroup());
        Vector v = ams.getBackgroundDrawers();
        Enumeration iter = v.elements();
        while (iter.hasMoreElements()) {
            addBgDrawer((BackGroundDrawer) iter.nextElement());
        }
        onResize();
        update();
    }
}


/**
 * This class contains the all the gizmos for firing the
 * mech's weapons.
 */
class WeaponPanel extends BufferedPanel
    implements ItemListener
{
    private static final String IMAGE_DIR = "data/images/widgets";
    
    public java.awt.List weaponList;
    public Choice m_chAmmo;
        
    public TransparentLabel wAmmo, wNameL, wHeatL, wDamL, wMinL, wShortL, wMedL, wLongL, wExtL;
    public TransparentLabel wNameR, wHeatR, wDamR, wMinR, wShortR,wMedR, wLongR, wExtR;
    public TransparentLabel currentHeatBuildupL, currentHeatBuildupR;

    public TransparentLabel wTargetL, wRangeL, wToHitL;
    public TransparentLabel wTargetR, wRangeR, wToHitR;

    public TextArea toHitText;

    // I need to keep a pointer to the weapon list of the
    // currently selected mech.
    private Vector weapons;
    private Vector vAmmo;
    private Entity entity;
    private ClientGUI client;

    private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayMediumFontSize")); //$NON-NLS-1$

    public WeaponPanel(ClientGUI client) {
        super(new GridBagLayout());

        this.client = client;

        FontMetrics fm = getFontMetrics(FONT_VALUE);

        Color clr = Color.white;

        // weapon list
        weaponList = new java.awt.List(4, false);
        weaponList.addItemListener(this);
        weaponList.addKeyListener(client.menuBar);

        // layout main panel
        GridBagConstraints c = new GridBagConstraints();

        //adding Weapon List
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(15, 9, 1, 9);
        c.weightx = 0.0;    c.weighty = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout)getLayout()).setConstraints(weaponList, c);
        add(weaponList);

        //adding Ammo choice + label

        wAmmo = new TransparentLabel(Messages.getString("MechDisplay.Ammo"), fm, clr, TransparentLabel.LEFT); //$NON-NLS-1$
        m_chAmmo = new Choice();
        m_chAmmo.addItemListener(this);
        m_chAmmo.addKeyListener(client.menuBar);

        c.insets = new Insets(1, 9, 1, 1);

        c.gridwidth = 1;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.NONE;
        ((GridBagLayout)getLayout()).setConstraints(wAmmo, c);
        add(wAmmo);

        c.insets = new Insets(1, 1, 1, 9);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        ((GridBagLayout)getLayout()).setConstraints(m_chAmmo, c);
        add(m_chAmmo);

        //Adding Heat Buildup

        currentHeatBuildupL = new TransparentLabel(Messages.getString("MechDisplay.HeatBuildup"), fm, clr, TransparentLabel.RIGHT); //$NON-NLS-1$
        currentHeatBuildupR = new TransparentLabel("--", fm, clr, TransparentLabel.LEFT); //$NON-NLS-1$

        c.insets = new Insets(2, 9, 2, 1);
        c.gridwidth = 2; c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        ((GridBagLayout)getLayout()).setConstraints(currentHeatBuildupL, c);
        add(currentHeatBuildupL);

        c.insets = new Insets(2, 1, 2, 9);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 2;
        c.anchor = GridBagConstraints.WEST;
        //c.fill = GridBagConstraints.HORIZONTAL;
        ((GridBagLayout)getLayout()).setConstraints(currentHeatBuildupR, c);
        add(currentHeatBuildupR);


        //Adding weapon display labels
        wNameL = new TransparentLabel(Messages.getString("MechDisplay.Name"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        wHeatL = new TransparentLabel(Messages.getString("MechDisplay.Heat"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        wDamL =  new TransparentLabel(Messages.getString("MechDisplay.Damage"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        wNameR = new TransparentLabel("", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        wHeatR = new TransparentLabel("--", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        wDamR =  new TransparentLabel("--", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$


         c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 9, 1, 1);
        c.gridwidth = 2; c.gridx = 0;
        ((GridBagLayout)getLayout()).setConstraints(wNameL, c);
        add(wNameL);


        c.insets = new Insets(2, 1, 1, 1);
        c.gridwidth = 1; c.gridx = 2;
        ((GridBagLayout)getLayout()).setConstraints(wHeatL, c);
        add(wHeatL);

        c.insets = new Insets(2, 1, 1, 9);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 3;
        ((GridBagLayout)getLayout()).setConstraints(wDamL, c);
        add(wDamL);

        c.insets = new Insets(1, 9, 2, 1);
        c.gridwidth = 2;
        c.gridx = 0; c.gridy = 4;
        ((GridBagLayout)getLayout()).setConstraints(wNameR, c);
        add(wNameR);

        c.gridwidth = 1;
        c.gridx = 2;
        ((GridBagLayout) getLayout()).setConstraints(wHeatR, c);
        add(wHeatR);

        c.insets = new Insets(1, 1, 2, 9);
        c.gridx = 3;
        c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout) getLayout()).setConstraints(wDamR, c);
        add(wDamR);


        // Adding range labels
        wMinL = new TransparentLabel(Messages.getString("MechDisplay.Min"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        wShortL = new TransparentLabel(Messages.getString("MechDisplay.Short"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        wMedL = new TransparentLabel(Messages.getString("MechDisplay.Med"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        wLongL = new TransparentLabel(Messages.getString("MechDisplay.Long"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        wExtL = new TransparentLabel(Messages.getString("MechDisplay.Ext"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        wMinR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        wShortR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        wMedR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        wLongR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        wExtR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        
        c.weightx = 1.0;
        c.insets = new Insets(2, 9, 1, 1);
        c.gridx = 0; c.gridy = 5; c.gridwidth = 1;
        ((GridBagLayout) getLayout()).setConstraints(wMinL, c);
        add(wMinL);

        c.insets = new Insets(2, 1, 1, 1);
        c.gridx = 1; c.gridy = 5;
        ((GridBagLayout) getLayout()).setConstraints(wShortL, c);
        add(wShortL);

        c.gridx = 2; c.gridy = 5;
        ((GridBagLayout) getLayout()).setConstraints(wMedL, c);
        add(wMedL);
        
//         c.insets = new Insets(2, 1, 1, 9);
        c.gridx = 3; c.gridy = 5;
//  c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout) getLayout()).setConstraints(wLongL, c);
        add(wLongL);
        
        c.insets = new Insets(2, 1, 1, 9);
        c.gridx = 4; c.gridy = 5;
        c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout) getLayout()).setConstraints(wExtL, c);
        add(wExtL);
        //----------------

         c.insets = new Insets(1, 9, 2, 1);
        c.gridx = 0; c.gridy = 6; c.gridwidth = 1;
        ((GridBagLayout) getLayout()).setConstraints(wMinR, c);
        add(wMinR);

        c.insets = new Insets(1, 1, 2, 1);
        c.gridx = 1; c.gridy = 6;
        ((GridBagLayout) getLayout()).setConstraints(wShortR, c);
        add(wShortR);

        c.gridx = 2; c.gridy = 6;
        ((GridBagLayout) getLayout()).setConstraints(wMedR, c);
        add(wMedR);

//         c.insets = new Insets(1, 1, 2, 9);
        c.gridx = 3; c.gridy = 6;
//  c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout) getLayout()).setConstraints(wLongR, c);
        add(wLongR);        
        
        c.insets = new Insets(1, 1, 2, 9);
        c.gridx = 4; c.gridy = 6;
        ((GridBagLayout) getLayout()).setConstraints(wExtR, c);
        add(wExtR);
        
            
        // target panel
        wTargetL = new TransparentLabel(Messages.getString("MechDisplay.Target"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        wRangeL = new TransparentLabel(Messages.getString("MechDisplay.Range"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        wToHitL = new TransparentLabel(Messages.getString("MechDisplay.ToHit"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$

        wTargetR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        wRangeR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
        wToHitR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$

        c.weightx = 0.0;
        c.insets = new Insets(2, 9, 1, 1);
        c.gridx = 0; c.gridy = 7; c.gridwidth = 1;
        ((GridBagLayout) getLayout()).setConstraints(wTargetL, c);
        add(wTargetL);

        c.insets = new Insets(2, 1, 1, 9);
        c.gridx = 1; c.gridy = 7; c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout) getLayout()).setConstraints(wTargetR, c);
        add(wTargetR);

        c.insets = new Insets(1, 9, 1, 1);
        c.gridx = 0; c.gridy = 8; c.gridwidth = 1;
        ((GridBagLayout) getLayout()).setConstraints(wRangeL, c);
        add(wRangeL);

        c.insets = new Insets(1, 1, 1, 9);
        c.gridx = 1; c.gridy = 8; c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout) getLayout()).setConstraints(wRangeR, c);
        add(wRangeR);

        c.insets = new Insets(1, 9, 1, 1);
        c.gridx = 0; c.gridy = 9; c.gridwidth = 1;
        ((GridBagLayout) getLayout()).setConstraints(wToHitL, c);
        add(wToHitL);

        c.insets = new Insets(1, 1, 1, 9);
        c.gridx = 1; c.gridy = 9; c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout) getLayout()).setConstraints(wToHitR, c);
        add(wToHitR);

        // to-hit text
        toHitText = new TextArea("", 2, 20, TextArea.SCROLLBARS_VERTICAL_ONLY); //$NON-NLS-1$
        toHitText.setEditable(false);
        toHitText.addKeyListener(client.menuBar);

        c.insets = new Insets(1, 9, 15, 9);
        c.gridx = 0; c.gridy = 10; c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout) getLayout()).setConstraints(toHitText, c);
        add(toHitText);


        setBackGround();


    }

    private void setBackGround(){
        Image tile = getToolkit().getImage(IMAGE_DIR+"/tile.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        int b = BackGroundDrawer.TILING_BOTH;
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_HORIZONTAL |
                BackGroundDrawer.VALIGN_TOP;
        tile = getToolkit().getImage(IMAGE_DIR+"/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_HORIZONTAL |
                BackGroundDrawer.VALIGN_BOTTOM;
        tile = getToolkit().getImage(IMAGE_DIR+"/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_VERTICAL |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(IMAGE_DIR+"/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_VERTICAL |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage(IMAGE_DIR+"/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));


            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(IMAGE_DIR+"/tl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(IMAGE_DIR+"/bl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage(IMAGE_DIR+"/tr_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage(IMAGE_DIR+"/br_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

    }


    /**
     * updates fields for the specified mech
     *
     * fix the ammo when it's added
     */
    public void displayMech(Entity en) {

        // Grab a copy of the game.
        IGame game = client.getClient().game;

        // update pointer to weapons
        this.weapons = en.getWeaponList();
        this.entity = en;

        int currentHeatBuildup = en.heat // heat from last round
            + en.getEngineCritHeat() // heat engine crits will add
            + en.heatBuildup; // heat we're building up this round
        if ( en instanceof Mech) {
            if (en.infernos.isStillBurning() ) { // hit with inferno ammo
                currentHeatBuildup += en.infernos.getHeat();
            }
            if(!((Mech)en).hasLaserHeatSinks()) {
                // extreme temperatures.
                if (game.getOptions().intOption("temperature") > 0) {
                    currentHeatBuildup += game.getTemperatureDifference();
                } else {
                    currentHeatBuildup -= game.getTemperatureDifference();
                }
            }
        }
        Coords position = entity.getPosition();
        if (!en.isOffBoard()) {
            if ( position != null
                    && game.getBoard().getHex(position).terrainLevel(Terrains.FIRE) == 2 ) {
                   currentHeatBuildup += 5; // standing in fire
               }
        }
        if ( en instanceof Mech && en.isStealthActive() ) {
            currentHeatBuildup += 10; // active stealth heat
        }

        // update weapon list
        weaponList.removeAll();
        m_chAmmo.removeAll();
        m_chAmmo.setEnabled(false);

        for(int i = 0; i < weapons.size(); i++) {
            Mounted mounted = (Mounted) weapons.elementAt(i);
            WeaponType wtype = (WeaponType) mounted.getType();
            StringBuffer wn = new StringBuffer(mounted.getDesc());
            wn.append(" ["); //$NON-NLS-1$
            wn.append(en.getLocationAbbr(mounted.getLocation()));
            wn.append("]"); //$NON-NLS-1$
            // determine shots left & total shots left
            if (wtype.getAmmoType() != AmmoType.T_NA
                && !wtype.hasFlag(WeaponType.F_ONESHOT)) {
                int shotsLeft = 0;
                if (mounted.getLinked() != null
                    && !mounted.getLinked().isDumping()) {
                    shotsLeft = mounted.getLinked().getShotsLeft();
                }

                EquipmentType typeUsed = null;
                if (null != mounted.getLinked()) {
                    typeUsed = mounted.getLinked().getType();
                }

                int totalShotsLeft = entity.getTotalMunitionsOfType(typeUsed);

                wn.append(" ("); //$NON-NLS-1$
                wn.append(shotsLeft);
                wn.append("/"); //$NON-NLS-1$
                wn.append(totalShotsLeft);
                wn.append(")"); //$NON-NLS-1$
            }
            
            // MG rapidfire
            if (mounted.isRapidfire()) {
                wn.append(Messages.getString("MechDisplay.rapidFire")); //$NON-NLS-1$
            }

            // Fire Mode - lots of things have variable modes
            if (wtype.hasModes()) {
                wn.append(" ");wn.append(mounted.curMode().getDisplayableName()); //$NON-NLS-1$
            }
            weaponList.add(wn.toString());
            if (mounted.isUsedThisRound()
                && game.getPhase() == IGame.PHASE_FIRING) {
                // add heat from weapons fire to heat tracker
                currentHeatBuildup += wtype.getHeat() * mounted.howManyShots();
            }
        }

        // This code block copied from the MovementPanel class,
        //  bad coding practice (duplicate code).
        int heatCap = en.getHeatCapacity();
        int heatCapWater = en.getHeatCapacityWithWater();
        String heatCapacityStr = Integer.toString(heatCap);

        if ( heatCap < heatCapWater ) {
          heatCapacityStr = heatCap + " [" + heatCapWater + "]"; //$NON-NLS-1$ //$NON-NLS-2$
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
        this.currentHeatBuildupR.setText(heatText + " (" + heatCapacityStr + ")"); //$NON-NLS-1$ //$NON-NLS-2$

        // If MaxTech range rules are in play, display the extreme range.
        if (game.getOptions().booleanOption("maxtech_range")) { //$NON-NLS-1$
            wExtL.setVisible (true);
            wExtR.setVisible (true);
        }
        else {
            wExtL.setVisible (false);
            wExtR.setVisible (false);
        }
    }

    /**
     * Selects the weapon at the specified index in the list
     */
    public void selectWeapon(int wn) {
        if (wn == -1) {
            weaponList.select(-1);
            return;
        }
        int index = weapons.indexOf(entity.getEquipment(wn));
        weaponList.select(index);
        displaySelected();
    }

    /**
     * Selects the weapon at the specified index in the list
     */
    public int getSelectedWeaponNum() {
        int selected = weaponList.getSelectedIndex();
        if (selected == -1) {
            return -1;
        }
        return entity.getEquipmentNum((Mounted)weapons.elementAt(selected));
    }

    /**
     * displays the selected item from the list in the weapon
     * display panel.
     */
    public void displaySelected() {
        // short circuit if not selected
        if(weaponList.getSelectedIndex() == -1) {
            m_chAmmo.removeAll();
            m_chAmmo.setEnabled(false);
            wNameR.setText(""); //$NON-NLS-1$
            wHeatR.setText("--"); //$NON-NLS-1$
            wDamR.setText("--"); //$NON-NLS-1$
            wMinR.setText("---"); //$NON-NLS-1$
            wShortR.setText("---"); //$NON-NLS-1$
            wMedR.setText("---"); //$NON-NLS-1$
            wLongR.setText("---"); //$NON-NLS-1$
            wExtR.setText("---"); //$NON-NLS-1$
            return;
        }
        Mounted mounted = (Mounted)weapons.elementAt(weaponList.getSelectedIndex());
        WeaponType wtype = (WeaponType)mounted.getType();
        // update weapon display
        wNameR.setText(mounted.getDesc());
        wHeatR.setText(wtype.getHeat() + ""); //$NON-NLS-1$
        if(wtype.getDamage() == WeaponType.DAMAGE_MISSILE) {
            wDamR.setText(Messages.getString("MechDisplay.Missile")); //$NON-NLS-1$
        } else if(wtype.getDamage() == WeaponType.DAMAGE_VARIABLE) {
            wDamR.setText(Messages.getString("MechDisplay.Variable")); //$NON-NLS-1$
        } else if(wtype.getDamage() == WeaponType.DAMAGE_SPECIAL) {
            wDamR.setText(Messages.getString("MechDisplay.Special")); //$NON-NLS-1$
        } else if(wtype.getDamage() == WeaponType.DAMAGE_ARTILLERY) {
            StringBuffer damage = new StringBuffer();
            damage.append( Integer.toString(wtype.getRackSize()) )
                .append( '/' )
                .append( Integer.toString(wtype.getRackSize()/2) );
            wDamR.setText( damage.toString() );
        } else {
            wDamR.setText( Integer.toString(wtype.getDamage()) );
        }

        // update range
        int shortR   = wtype.getShortRange();
        int mediumR  = wtype.getMediumRange();
        int longR    = wtype.getLongRange();
        int extremeR = wtype.getExtremeRange();
        if ( ILocationExposureStatus.WET == entity.getLocationStatus(mounted.getLocation()) 
        		|| longR == 0) {
            shortR = wtype.getWShortRange();
            mediumR = wtype.getWMediumRange();
            longR = wtype.getWLongRange();
            extremeR = wtype.getWExtremeRange();
        }
        if(wtype.getMinimumRange() > 0) {
            wMinR.setText(Integer.toString(wtype.getMinimumRange()));
        } else {
            wMinR.setText("---"); //$NON-NLS-1$
        }
        if(shortR > 1) {
            wShortR.setText("1 - " + shortR); //$NON-NLS-1$
        } else {
            wShortR.setText("" + shortR); //$NON-NLS-1$
        }
        if(mediumR - shortR > 1) {
            wMedR.setText((shortR + 1) + " - " + mediumR); //$NON-NLS-1$
        } else {
            wMedR.setText("" + mediumR); //$NON-NLS-1$
        }
        if(longR - mediumR > 1) {
            wLongR.setText((mediumR + 1) + " - " + longR); //$NON-NLS-1$
        } else {
            wLongR.setText("" + longR); //$NON-NLS-1$
        }
        if(extremeR - longR > 1) {
            wExtR.setText((longR + 1) + " - " + extremeR); //$NON-NLS-1$
        } else {
            wExtR.setText("" + extremeR); //$NON-NLS-1$
        }

        // Update the range display to account for the weapon's loaded ammo.
        if ( null != mounted.getLinked() ) {
            AmmoType atype = (AmmoType) mounted.getLinked().getType();
            updateRangeDisplayForAmmo( atype );
        }

        // update ammo selector
        boolean bOwner = (client.getClient().getLocalPlayer() == entity.getOwner());
        m_chAmmo.removeAll();
        if (wtype.getAmmoType() == AmmoType.T_NA || !bOwner) {
            m_chAmmo.setEnabled(false);
        } else if (wtype.hasFlag(WeaponType.F_ONESHOT)) {
            if (mounted.getLinked().getShotsLeft() == 1) {
                m_chAmmo.add( formatAmmo(mounted.getLinked()) );
                m_chAmmo.setEnabled(true);
            } else {
                m_chAmmo.setEnabled(false);
            }
        } else {
            m_chAmmo.setEnabled(true);
            vAmmo = new Vector();
            int nCur = -1;
            int i = 0;
            for (Enumeration j = entity.getAmmo(); j.hasMoreElements();) {
                Mounted mountedAmmo = (Mounted)j.nextElement();
                AmmoType atype = (AmmoType)mountedAmmo.getType();
                if (mountedAmmo.isAmmoUsable() &&
                    atype.getAmmoType() == wtype.getAmmoType() &&
                    atype.getRackSize() == wtype.getRackSize()) {

                    vAmmo.addElement(mountedAmmo);
                    m_chAmmo.add( formatAmmo(mountedAmmo) );
                    if (mounted.getLinked() == mountedAmmo) {
                        nCur = i;
                    }
                    i++;
                }
            }
            if (nCur == -1) {
                // outcommenting this fixes bug 1041003 (if a linked ammobin 
                // gets breached, it will continue to be used, and other ammo
                // can't be selected for the weapon that the breached ammo was
                // linked to), and did not cause anything to misbehave in my
                // testing, apart from the Choice being enabled even if there
                // is no ammo to choose from, but that is only a minor issue
                // m_chAmmo.setEnabled(false);
            }
            else {
                m_chAmmo.select(nCur);
            }
        }
    }

    private String formatAmmo(Mounted m)
    {
        StringBuffer sb = new StringBuffer(64);
        int ammoIndex = m.getDesc().indexOf(Messages.getString("MechDisplay.0")); //$NON-NLS-1$
        int loc = m.getLocation();
        if (loc != Entity.LOC_NONE) {
            sb.append("[").append(entity.getLocationAbbr(loc)).append("] "); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (ammoIndex == -1) {
            sb.append(m.getDesc());
        } else {
            sb.append(m.getDesc().substring(0, ammoIndex));
            sb.append(m.getDesc().substring(ammoIndex + 4));
        }
        return sb.toString();
    }

    /**
     * Update the range display for the selected ammo.
     *
     * @param   atype - the <code>AmmoType</code> of the weapon's loaded ammo.
     */
    private void updateRangeDisplayForAmmo( AmmoType atype ) {

        // Only override the display for the various ATM ammos
        if(AmmoType.T_ATM == atype.getAmmoType())
        {
            if((atype.getAmmoType() == AmmoType.T_ATM)
                        && atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE)
            {
                wMinR.setText("4"); //$NON-NLS-1$
                wShortR.setText("1 - 9"); //$NON-NLS-1$
                wMedR.setText("10 - 18"); //$NON-NLS-1$
                wLongR.setText("19 - 27"); //$NON-NLS-1$
                wExtR.setText("28 - 36"); //$NON-NLS-1$
            }
            else if ((atype.getAmmoType() == AmmoType.T_ATM)
                        && atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE)
            {
                wMinR.setText("---"); //$NON-NLS-1$
                wShortR.setText("1 - 3"); //$NON-NLS-1$
                wMedR.setText("4 - 6"); //$NON-NLS-1$
                wLongR.setText("7 - 9"); //$NON-NLS-1$
                wExtR.setText("10 - 12"); //$NON-NLS-1$
            }
            else
            {
                wMinR.setText("4"); //$NON-NLS-1$
                wShortR.setText("1 - 5"); //$NON-NLS-1$
                wMedR.setText("6 - 10"); //$NON-NLS-1$
                wLongR.setText("11 - 15"); //$NON-NLS-1$
                wExtR.setText("16 - 20"); //$NON-NLS-1$
            }
        } // End weapon-is-ATM

    } // End private void updateRangeDisplayForAmmo( AmmoType )

    //
    // ItemListener
    //
    public void itemStateChanged(ItemEvent ev) {
        if(ev.getItemSelectable() == weaponList) {
            displaySelected();
        }
        else if (ev.getItemSelectable() == m_chAmmo) {
            int n = weaponList.getSelectedIndex();
            if (n == -1) {
                return;
            }
            Mounted mWeap = (Mounted)weapons.elementAt(n);
            Mounted mAmmo = (Mounted)vAmmo.elementAt(m_chAmmo.getSelectedIndex());
            entity.loadWeapon(mWeap, mAmmo);

            // Update the range display to account for the weapon's loaded ammo.
            AmmoType atype = (AmmoType) mAmmo.getType();
            updateRangeDisplayForAmmo( atype );

            // When in the Firing Phase, update the targeting information.
            // TODO: make this an accessor function instead of a member access.
            if ( this.client.curPanel instanceof FiringDisplay ) {
                ( (FiringDisplay) this.client.curPanel ).updateTarget();
            } else if (this.client.curPanel instanceof TargetingPhaseDisplay ) {
                ( (TargetingPhaseDisplay) this.client.curPanel).updateTarget();
            }

            // Alert the server of the update.
            this.client.getClient().sendAmmoChange( entity.getId(),
                                        entity.getEquipmentNum(mWeap),
                                        entity.getEquipmentNum(mAmmo) );
        }
    }

}

/**
 * This class shows the critical hits and systems for a mech
 */
class SystemPanel
    extends BufferedPanel
    implements ItemListener, ActionListener
{
    private static final String IMAGE_DIR = "data/images/widgets";
    
    private static Object SYSTEM = new Object();

    private TransparentLabel locLabel, slotLabel, modeLabel;
    public java.awt.List slotList;
    public java.awt.List locList;

    public Choice m_chMode;
    public Button m_bDumpAmmo;
    //public Label modeLabel;
    private ClientGUI clientgui;

    private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayLargeFontSize")); //$NON-NLS-1$

    Entity en;

    public SystemPanel(ClientGUI clientgui) {
        super();

        FontMetrics fm = getFontMetrics(FONT_VALUE);

        this.clientgui = clientgui;
        locLabel = new TransparentLabel(Messages.getString("MechDisplay.Location"), fm, Color.white,TransparentLabel.CENTER); //$NON-NLS-1$
        slotLabel = new TransparentLabel(Messages.getString("MechDisplay.Slot"), fm, Color.white,TransparentLabel.CENTER); //$NON-NLS-1$

        locList = new List(8, false);
        locList.addItemListener(this);
        locList.addKeyListener(clientgui.menuBar);

        slotList = new List(12, false);
        slotList.addItemListener(this);
        slotList.addKeyListener(clientgui.menuBar);
        //slotList.setEnabled(false);

        m_chMode = new Choice();
        m_chMode.add("   "); //$NON-NLS-1$
        m_chMode.setEnabled(false);
        m_chMode.addItemListener(this);
        m_chMode.addKeyListener(clientgui.menuBar);

        m_bDumpAmmo = new Button(Messages.getString("MechDisplay.m_bDumpAmmo")); //$NON-NLS-1$
        m_bDumpAmmo.setEnabled(false);
        m_bDumpAmmo.setActionCommand("dump"); //$NON-NLS-1$
        m_bDumpAmmo.addActionListener(this);
        m_bDumpAmmo.addKeyListener(clientgui.menuBar);

        modeLabel = new TransparentLabel(Messages.getString("MechDisplay.modeLabel"), fm, Color.white,TransparentLabel.RIGHT); //$NON-NLS-1$
        //modeLabel.setEnabled(false);


        // layout main panel
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(15, 9, 1, 1);
        c.gridy = 0; c.gridx = 0;
        c.weightx = 0.5;    c.weighty = 0.0;
        c.gridwidth = 1; c.gridheight = 1;
        gridbag.setConstraints(locLabel, c);
        add(locLabel);

        c.weightx = 0.0;
        c.gridy = 0; c.gridx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(15, 1, 1, 9);
        gridbag.setConstraints(slotLabel, c);
        add(slotLabel);

        c.weightx = 0.5;
        //c.weighty = 1.0;
        c.gridy = 1; c.gridx = 0;
        c.gridwidth = 1;
        c.insets = new Insets(1, 9, 15, 1);
        c.gridheight = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(locList, c);
        add(locList);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.gridy = 1;
        c.gridx = 1;
        c.weightx = 0.0;
        c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 9);
        gridbag.setConstraints(slotList, c);
        add(slotList);

        c.gridwidth = 1;
        c.gridy = 2;
        c.gridx = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(modeLabel, c);
        c.insets = new Insets(1, 1, 1, 1);
        add(modeLabel);

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy = 2;
        c.gridx = 2;
        c.insets = new Insets(1, 1, 1, 9);
        gridbag.setConstraints(m_chMode, c);
        add(m_chMode);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.gridy = 3;
        c.gridx = 1;
        c.insets = new Insets(4, 4, 15, 9);
        gridbag.setConstraints(m_bDumpAmmo, c);
        add(m_bDumpAmmo);


        setBackGround();
    }

    public int getSelectedLocation() {
        return locList.getSelectedIndex();
    }

    public Mounted getSelectedEquipment() {
        int loc = locList.getSelectedIndex();
        int slot = slotList.getSelectedIndex();
        if (loc == -1 || slot == -1) {
            return null;
        }
        if (en instanceof Tank) {
            if (en.hasTargComp()) {
                Enumeration equip = en.getEquipment();
                while (equip.hasMoreElements()) {
                    Mounted m = (Mounted)equip.nextElement();
                    if (m.getType() instanceof MiscType && 
                        m.getType().hasFlag(MiscType.F_TARGCOMP) ) {
                        return m;
                    }
                }
            }
        }
        final CriticalSlot cs = en.getCritical(loc, slot);
        if (null == cs) {
            return null;
        }
        if (cs.getType() == CriticalSlot.TYPE_SYSTEM) {
            return null;
        }
        return en.getEquipment (cs.getIndex());
    }

    /**
     * updates fields for the specified mech
     */
    public void displayMech(Entity en) {
        this.en = en;

        locList.removeAll();
        for(int i = 0; i < en.locations(); i++) {
            if(en.getNumberOfCriticals(i) > 0) {
                locList.add(en.getLocationName(i), i);
            }
        }
        if (en instanceof Tank) {
            if (en.hasTargComp()) {
                locList.add(en.getLocationName(0));
            }
        }
        locList.select(0);
        displaySlots();
    }

    public void displaySlots() {
        int loc = locList.getSelectedIndex();
        slotList.removeAll();
        for (int i = 0; i < en.getNumberOfCriticals(loc); i++) {
            final CriticalSlot cs = en.getCritical(loc, i);
            StringBuffer sb = new StringBuffer(32);
            if(cs == null) {
                sb.append("---"); //$NON-NLS-1$
            } else {
                switch(cs.getType()) {
                case CriticalSlot.TYPE_SYSTEM :
                    sb.append(cs.isDestroyed() ? "*" : "") //$NON-NLS-1$ //$NON-NLS-2$
                        .append(cs.isBreached() ? "x" : ""); //$NON-NLS-1$ //$NON-NLS-2$
                    // Protomechs have different systme names.
                    if ( en instanceof Protomech ) {
                        sb.append(Protomech.systemNames[cs.getIndex()]);
                    } else {
                        sb.append(Mech.systemNames[cs.getIndex()]);
                    }
                    break;
                case CriticalSlot.TYPE_EQUIPMENT :
                    Mounted m = en.getEquipment(cs.getIndex());
                    sb.append(cs.isDestroyed() ? "*" : "").append(cs.isBreached() ? "x" : "").append(m.getDesc()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                    if (m.getType().hasModes()) {
                        sb.append(" (").append(m.curMode().getDisplayableName()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    break;
                }
            }
            slotList.add(sb.toString());
        }
        if (en instanceof Tank) {
            if (en.hasTargComp()) {
                Enumeration equip = en.getEquipment();
                while (equip.hasMoreElements()) {
                    Mounted m = (Mounted)equip.nextElement();
                    if (m.getType() instanceof MiscType && 
                        m.getType().hasFlag(MiscType.F_TARGCOMP) ) {
                        StringBuffer sb = new StringBuffer(32);
                        sb.append(m.isDestroyed() ? "*" : "").append(m.isBreached() ? "x" : "").append(m.getDesc()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                        if (m.getType().hasModes()) {
                            sb.append(" (").append(m.curMode().getDisplayableName()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                        slotList.add(sb.toString());                                                
                    }                    
                }
            }
        }
    }

    //
    // ItemListener
    //
    public void itemStateChanged(ItemEvent ev) {
        if(ev.getItemSelectable() == locList) {
            m_chMode.removeAll();
            m_chMode.setEnabled(false);
            displaySlots();
        }
        else if (ev.getItemSelectable() == slotList) {
            m_bDumpAmmo.setEnabled(false);
            m_chMode.setEnabled(false);
            modeLabel.setEnabled(false);
            Mounted m = getSelectedEquipment();

            boolean bOwner = (clientgui.getClient().getLocalPlayer() == en.getOwner());
            if ( m != null && bOwner && m.getType() instanceof AmmoType
                 && IGame.PHASE_DEPLOYMENT != clientgui.getClient().game.getPhase()
                 && m.getShotsLeft() > 0 && !m.isDumping() && en.isActive() ) {
                m_bDumpAmmo.setEnabled(true);
            }
            else if (m != null && bOwner && m.getType().hasModes()) {
                if (!m.isDestroyed() && en.isActive()) {
                    m_chMode.setEnabled(true);
                }
                if (!m.isDestroyed() && m.getType().hasFlag(MiscType.F_STEALTH)) {
                    m_chMode.setEnabled(true);
                }
                modeLabel.setEnabled(true);
                m_chMode.removeAll();
                for (Enumeration e = m.getType().getModes(); e.hasMoreElements();) {
                    EquipmentMode em = (EquipmentMode) e.nextElement();
                    m_chMode.add(em.getDisplayableName());
                }
                m_chMode.select(m.curMode().getDisplayableName());
            }
        }
        else if (ev.getItemSelectable() == m_chMode) {
            Mounted m = getSelectedEquipment();
            if (m != null && m.getType().hasModes()) {
                int nMode = m_chMode.getSelectedIndex();
                if (nMode >= 0) {
                    m.setMode(nMode);                    
                    // send the event to the server
                    clientgui.getClient().sendModeChange(en.getId(), en.getEquipmentNum(m), nMode);
                    
                    // notify the player
                    if (m.getType().hasInstantModeSwitch()) {                    
                        clientgui.systemMessage(Messages.getString("MechDisplay.switched", new Object[]{m.getName(),m.curMode().getDisplayableName()}));//$NON-NLS-1$
                    }
                    else {
                        if (IGame.PHASE_DEPLOYMENT == clientgui.getClient().game.getPhase() ) {                        
                            clientgui.systemMessage(Messages.getString("MechDisplay.willSwitchAtStart", new Object[]{m.getName(),m.pendingMode().getDisplayableName()}));//$NON-NLS-1$
                        } else{ 
                            clientgui.systemMessage(Messages.getString("MechDisplay.willSwitchAtEnd", new Object[]{m.getName(),m.pendingMode().getDisplayableName()}));//$NON-NLS-1$
                        }
                    }
                }
            }
        }
    }

    // ActionListener
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("dump")) { //$NON-NLS-1$
            Mounted m = getSelectedEquipment();
            boolean bOwner = (clientgui.getClient().getLocalPlayer() == en.getOwner());
            if (m == null || !bOwner || !(m.getType() instanceof AmmoType) ||
                        m.getShotsLeft() <= 0) {
                return;
            }

            boolean bDumping;
            boolean bConfirmed = false;

            if (m.isPendingDump()) {
                bDumping = false;
                String title = Messages.getString("MechDisplay.CancelDumping.title"); //$NON-NLS-1$
                String body = Messages.getString("MechDisplay.CancelDumping.message", new Object[]{m.getName()}); //$NON-NLS-1$
                bConfirmed = clientgui.doYesNoDialog(title, body);
            }
            else {
                bDumping = true;
                String title = Messages.getString("MechDisplay.Dump.title"); //$NON-NLS-1$
                String body = Messages.getString("MechDisplay.Dump.message", new Object[]{m.getName()}); //$NON-NLS-1$
                bConfirmed = clientgui.doYesNoDialog(title, body);
            }

            if (bConfirmed) {
                m.setPendingDump(bDumping);
                clientgui.getClient().sendModeChange(en.getId(), en.getEquipmentNum(m), bDumping ? 1 : 0);
            }
        }
    }

    private void setBackGround(){
        Image tile = getToolkit().getImage(IMAGE_DIR+"/tile.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        int b = BackGroundDrawer.TILING_BOTH;
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_HORIZONTAL |
                BackGroundDrawer.VALIGN_TOP;
        tile = getToolkit().getImage(IMAGE_DIR+"/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_HORIZONTAL |
                BackGroundDrawer.VALIGN_BOTTOM;
        tile = getToolkit().getImage(IMAGE_DIR+"/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_VERTICAL |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(IMAGE_DIR+"/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_VERTICAL |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage(IMAGE_DIR+"/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));


            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(IMAGE_DIR+"/tl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(IMAGE_DIR+"/bl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage(IMAGE_DIR+"/tr_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage(IMAGE_DIR+"/br_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

    }

}


/**
 * This class shows information about a unit that doesn't belong elsewhere.
 */
class ExtraPanel
    extends BufferedPanel
    implements ItemListener, ActionListener
{

    private static final String IMAGE_DIR = "data/images/widgets";
    
    private TransparentLabel  narcLabel, unusedL, carrysL, heatL, sinksL, targSysL;
    public TextArea unusedR, carrysR, heatR, sinksR;
    public Button sinks2B;
    public java.awt.List narcList;
    private int myMechId;
    
//    private Prompt prompt;
    private Slider prompt;
    
    private int sinks;
    private boolean dontChange;
    
    private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayLargeFontSize")); //$NON-NLS-1$

    private ClientGUI clientgui;

    public ExtraPanel(ClientGUI clientgui) {
        super();

        this.clientgui = clientgui;
        prompt = null;
        
        FontMetrics fm = getFontMetrics(FONT_VALUE);

        narcLabel = new TransparentLabel
            (Messages.getString("MechDisplay.AffectedBy"), fm, Color.white,TransparentLabel.CENTER); //$NON-NLS-1$

        narcList = new List(3, false);
        narcList.addKeyListener(clientgui.menuBar);

        // transport stuff
        //unusedL = new Label( "Unused Space:", Label.CENTER );

        unusedL = new TransparentLabel
            (Messages.getString("MechDisplay.UnusedSpace"), fm, Color.white,TransparentLabel.CENTER); //$NON-NLS-1$
        unusedR = new TextArea("", 2, 25, TextArea.SCROLLBARS_VERTICAL_ONLY); //$NON-NLS-1$
        unusedR.setEditable(false);
        unusedR.addKeyListener(clientgui.menuBar);

        carrysL = new TransparentLabel
            ( Messages.getString("MechDisplay.Carryng"), fm, Color.white,TransparentLabel.CENTER); //$NON-NLS-1$
        carrysR = new TextArea("", 4, 25, TextArea.SCROLLBARS_VERTICAL_ONLY); //$NON-NLS-1$
        carrysR.setEditable(false);
        carrysR.addKeyListener(clientgui.menuBar);

        sinksL = new TransparentLabel
            ( Messages.getString("MechDisplay.activeSinksLabel"), fm, Color.white, TransparentLabel.CENTER);
        sinksR = new TextArea ("", 2, 25, TextArea.SCROLLBARS_VERTICAL_ONLY);
        sinksR.setEditable(false);
        sinksR.addKeyListener(clientgui.menuBar);
        
        sinks2B = new Button (Messages.getString("MechDisplay.configureActiveSinksLabel"));
        sinks2B.setActionCommand("changeSinks");
        sinks2B.addActionListener(this);
        
        heatL = new TransparentLabel
            ( Messages.getString("MechDisplay.HeatEffects"), fm, Color.white, TransparentLabel.CENTER); //$NON-NLS-1$
        heatR = new TextArea ("", 4, 25, TextArea.SCROLLBARS_VERTICAL_ONLY); //$NON-NLS-1$
        heatR.setEditable(false);
        heatR.addKeyListener(clientgui.menuBar);

        targSysL = new TransparentLabel((Messages.getString("MechDisplay.TargSysLabel")).concat(" "), fm, Color.white, TransparentLabel.CENTER);

        // layout choice panel
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        gridbag = new GridBagLayout();
        c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(15, 9, 1, 9);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1.0;


        c.weighty = 0.0;
        gridbag.setConstraints(narcLabel, c);
        add(narcLabel);


        c.insets = new Insets(1, 9, 1, 9);
        c.weighty = 1.0;
        gridbag.setConstraints(narcList, c);
        add(narcList);

        c.weighty = 0.0;
        gridbag.setConstraints(unusedL, c);
        add(unusedL);

        c.weighty = 1.0;
        gridbag.setConstraints(unusedR, c);
        add(unusedR);

        c.weighty = 0.0;
        gridbag.setConstraints(carrysL, c);
        add(carrysL);

        c.insets = new Insets(1, 9, 1, 9);
        c.weighty = 1.0;
        gridbag.setConstraints(carrysR, c);
        add(carrysR);

        c.weighty = 0.0;
        gridbag.setConstraints(sinksL, c);
        add(sinksL);

        c.insets = new Insets(1, 9, 1, 9);
        c.weighty = 1.0;
        gridbag.setConstraints(sinksR, c);
        add(sinksR);
        
        c.weighty = 0.0;
        gridbag.setConstraints(sinks2B, c);
        add(sinks2B);

        c.weighty = 0.0;
        gridbag.setConstraints(heatL, c);
        add(heatL);

        c.insets = new Insets(1, 9, 18, 9);
        c.weighty = 1.0;
        gridbag.setConstraints(heatR, c);
        add(heatR);

        gridbag.setConstraints(targSysL, c);
        add(targSysL);

        setBackGround();


    }

   private void setBackGround(){
        Image tile = getToolkit().getImage(IMAGE_DIR+"/tile.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        int b = BackGroundDrawer.TILING_BOTH;
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_HORIZONTAL |
                BackGroundDrawer.VALIGN_TOP;
        tile = getToolkit().getImage(IMAGE_DIR+"/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_HORIZONTAL |
                BackGroundDrawer.VALIGN_BOTTOM;
        tile = getToolkit().getImage(IMAGE_DIR+"/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_VERTICAL |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(IMAGE_DIR+"/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_VERTICAL |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage(IMAGE_DIR+"/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));


            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(IMAGE_DIR+"/tl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(IMAGE_DIR+"/bl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage(IMAGE_DIR+"/tr_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage(IMAGE_DIR+"/br_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer (tile,b));

    }

    /**
     * updates fields for the specified mech
     */
    public void displayMech(Entity en) {

        // Clear the "Affected By" list.
        narcList.removeAll();
        sinks=0;
        myMechId = en.getId();
        if (clientgui.getClient().getLocalPlayer().getId() != en.getOwnerId()) {
            sinks2B.setEnabled(false);
            dontChange=true;
        } else {
            sinks2B.setEnabled(true);
            dontChange=false;
        }
        // Walk through the list of teams.  There
        // can't be more teams than players.
        StringBuffer buff = null;
        Enumeration loop = clientgui.getClient().game.getPlayers();
        while ( loop.hasMoreElements() ) {
            Player player = (Player) loop.nextElement();
            int team = player.getTeam();
            if ( en.isNarcedBy( team ) &&
                 !player.isObserver()) {
                buff = new StringBuffer( Messages.getString("MechDisplay.NARCedBy") ); //$NON-NLS-1$
                buff.append( player.getName() );
                buff.append( " [" ) //$NON-NLS-1$
                    .append( Player.teamNames[team] )
                    .append( "]" ); //$NON-NLS-1$
                narcList.add( buff.toString() );
            }
            if ( en.isINarcedBy( team ) &&
                    !player.isObserver()) {
                   buff = new StringBuffer( Messages.getString("MechDisplay.INarcHoming") ); //$NON-NLS-1$
                   buff.append( player.getName() );
                   buff.append( " [" ) //$NON-NLS-1$
                       .append( Player.teamNames[team] )
                       .append( "] ") //$NON-NLS-1$
                       .append(Messages.getString("MechDisplay.attached")) //$NON-NLS-1$
                       .append("."); //$NON-NLS-1$
                   narcList.add( buff.toString() );
            }
        }
        if ( en.isINarcedWith(INarcPod.ECM)) {
               buff = new StringBuffer( Messages.getString("MechDisplay.iNarcECMPodAttached") ); //$NON-NLS-1$
               narcList.add( buff.toString() );
        }
        if ( en.isINarcedWith(INarcPod.HAYWIRE)) {
               buff = new StringBuffer( Messages.getString("MechDisplay.iNarcHaywirePodAttached") ); //$NON-NLS-1$
               narcList.add( buff.toString() );
        }
        if ( en.isINarcedWith(INarcPod.NEMESIS)) {
               buff = new StringBuffer( Messages.getString("MechDisplay.iNarcNemesisPodAttached") ); //$NON-NLS-1$
               narcList.add( buff.toString() );
        }

        // Show inferno track.
        if ( en.infernos.isStillBurning() ) {
            buff = new StringBuffer( Messages.getString("MechDisplay.InfernoBurnRemaining") ); //$NON-NLS-1$
            buff.append( en.infernos.getTurnsLeftToBurn() );
            narcList.add( buff.toString() );
        }

        // Show ECM affect.
        Coords pos = en.getPosition();
        if ( Compute.isAffectedByECM( en, pos, pos ) ) {
            narcList.add( Messages.getString("MechDisplay.InEnemyECMField") ); //$NON-NLS-1$
        }

        // Show Turret Locked.
        if ( en instanceof Tank &&
             !( (Tank) en ).hasNoTurret() &&
             !en.canChangeSecondaryFacing() ) {
            narcList.add( Messages.getString("MechDisplay.Turretlocked") ); //$NON-NLS-1$
        }

        // Show jammed weapons.
        Enumeration weaps = en.getWeapons();
        while ( weaps.hasMoreElements() ) {
            Mounted weapon = (Mounted) weaps.nextElement();
            if ( weapon.isJammed() ) {
                buff = new StringBuffer( weapon.getName() );
                buff.append( Messages.getString("MechDisplay.isJammed") ); //$NON-NLS-1$
                narcList.add( buff.toString() );
            }
        }

        // Show breached locations.
        for ( int loc = 0; loc < en.locations(); loc++ ) {
            if ( ILocationExposureStatus.BREACHED == en.getLocationStatus(loc) ) {
                buff = new StringBuffer( en.getLocationName(loc) );
                buff.append( Messages.getString("MechDisplay.Breached") ); //$NON-NLS-1$
                narcList.add( buff.toString() );
            }
        }

        // transport values
        String unused = en.getUnusedString();
        if ( unused.equals("") ) unused = Messages.getString("MechDisplay.None"); //$NON-NLS-1$ //$NON-NLS-2$
        this.unusedR.setText( unused );
        Enumeration iter = en.getLoadedUnits().elements();
        carrysR.setText( null );
        boolean hasText = false;
        while ( iter.hasMoreElements() ) {
            carrysR.append( ((Entity)iter.nextElement()).getShortName() );
            hasText = true;
            if ( iter.hasMoreElements() ) {
                carrysR.append( "\n" ); //$NON-NLS-1$
            }
        }

        // Show club.
        Mounted club = Compute.clubMechHas( en );
        if ( null != club ) {
            if ( hasText ) {
                carrysR.append( "\r\n" ); //$NON-NLS-1$
            }
            carrysR.append( club.getName() );
        }

        // Show searchlight
        if(en.hasSpotlight()) {
            if(en.isUsingSpotlight())
                carrysR.append(Messages.getString("MechDisplay.SearchlightOn")); //$NON-NLS-1$
            else
                carrysR.append(Messages.getString("MechDisplay.SearchlightOff")); //$NON-NLS-1$
        }

        // Show Heat Effects, but only for Mechs.
        heatR.setText(""); //$NON-NLS-1$
        sinksR.setText("");
        
        if (en instanceof Mech) {
            Mech m = (Mech)en;
            
            sinks2B.setEnabled(!dontChange);
            sinks = m.getActiveSinks();
            if (m.hasDoubleHeatSinks()) {
                sinksR.append(Messages.getString("MechDisplay.activeSinksTextDouble", new Object[]{new Integer(sinks), new Integer(sinks*2)}));
            } else {
                sinksR.append(Messages.getString("MechDisplay.activeSinksTextSingle", new Object[]{new Integer(sinks)}));
            }
            
            boolean hasTSM = false;
            boolean mtHeat = false;
            if (((Mech)en).hasTSM()) hasTSM = true;

            if (clientgui.getClient().game.getOptions().booleanOption("maxtech_heat")) {
                mtHeat = true;
            }
            heatR.append(HeatEffects.getHeatEffects(en.heat, mtHeat, hasTSM));
        } else {
            // Non-Mechs cannot configure their heatsinks
            sinks2B.setEnabled(false);
        }

        targSysL.setText((Messages.getString("MechDisplay.TargSysLabel")).concat(" ").concat(MiscType.getTargetSysName(en.getTargSysType())));
    } // End public void displayMech( Entity )

    public void itemStateChanged(ItemEvent ev) {
    }
    
    public void actionPerformed(ActionEvent ae) {
        if (ae.getActionCommand().equals("changeSinks") && !dontChange) { //$NON-NLS-1$
            prompt = new Slider (clientgui.frame, Messages.getString("MechDisplay.changeSinks"), Messages.getString("MechDisplay.changeSinks"),
                                 sinks, 0, ((Mech)clientgui.getClient().game.getEntity(myMechId)).getNumberOfSinks());
            if (!prompt.showDialog()) return;
            clientgui.menuBar.actionPerformed(ae);
            int helper = prompt.getValue();

            ((Mech)clientgui.getClient().game.getEntity(myMechId)).setActiveSinksNextRound(helper);
            clientgui.getClient().sendUpdateEntity(clientgui.getClient().game.getEntity(myMechId));
        }
    }
} // End class ExtraPanel extends Panel

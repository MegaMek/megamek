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

package megamek.client;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Enumeration;

import megamek.client.util.widget.*;
import megamek.common.*;

/**
 * Displays the info for a mech.  This is also a sort
 * of interface for special movement and firing actions.
 */
public class MechDisplay extends BufferedPanel


{
    // buttons & gizmos for top level

    MechPanelTabStrip tabStrip;


    public BufferedPanel        displayP;
    public MovementPanel        mPan;
    public ArmorPanel           aPan;
    public WeaponPanel          wPan;
    public SystemPanel          sPan;
    public ExtraPanel           ePan;
    private ClientGUI           clientgui;
    private Client				client;

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
        displayP.add("movement", mPan);
        aPan = new ArmorPanel();
        displayP.add("armor", aPan);
        wPan = new WeaponPanel(clientgui);
        displayP.add("weapons", wPan);
        sPan = new SystemPanel(clientgui);
        displayP.add("systems", sPan);
        ePan = new ExtraPanel(clientgui);
        displayP.add("extras", ePan);

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

        ((CardLayout)displayP.getLayout()).show(displayP, "movement");
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

        // 2003-12-30, nemchenk
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
        if (s == "movement") {
            tabStrip.setTab(0);
        } else if (s == "armor") {
            tabStrip.setTab(1);
        } else if (s == "weapons") {
            tabStrip.setTab(3);
        } else if (s == "systems") {
            tabStrip.setTab(2);
        } else if (s == "extras") {
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
      int dx = (int) Math.round((w - r.width)/2);
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
    private int minTopMargin = 0;
    private int minLeftMargin = 0;
    private int minBottomMargin = 0;
    private int minRightMargin = 0;

    private static final int minTankTopMargin = 8;
    private static final int minTankLeftMargin = 8;
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
    }

    public void onResize(){
      Rectangle r = getContentBounds();
      if( r == null) return;
      int w = (int) Math.round((getSize().width - r.width)/2);
      int h = (int) Math.round((getSize().height - r.height)/2);
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
        DisplayMapSet ams = (DisplayMapSet) mech;
        removeAll();
        if (en instanceof Mech) {
            ams = (DisplayMapSet) mech;
            minLeftMargin = minMechLeftMargin;
            minTopMargin = minMechTopMargin;
            minBottomMargin = minMechBottomMargin;
            minRightMargin = minMechRightMargin;
        } else if (en instanceof Tank) {
            ams = (DisplayMapSet) tank;
            minLeftMargin = minTankLeftMargin;
            minTopMargin = minTankTopMargin;
            minBottomMargin = minTankTopMargin;
            minRightMargin = minTankLeftMargin;
        } else if (en instanceof BattleArmor) {
            ams = (DisplayMapSet) battleArmor;
            minLeftMargin = minInfLeftMargin;
            minTopMargin = minInfTopMargin;
            minBottomMargin = minInfTopMargin;
            minRightMargin = minInfLeftMargin;

        } else if (en instanceof Infantry) {
            ams = (DisplayMapSet) infantry;
            minLeftMargin = minInfLeftMargin;
            minTopMargin = minInfTopMargin;
            minBottomMargin = minInfTopMargin;
            minRightMargin = minInfLeftMargin;

        } else if (en instanceof Protomech) {
            ams = (DisplayMapSet) proto;
            minLeftMargin = minTankLeftMargin;
            minTopMargin = minTankTopMargin;
            minBottomMargin = minTankTopMargin;
            minRightMargin = minTankLeftMargin;
        }
        if (null == ams) {
            System.err.println("The armor panel is null.");
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

    private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN, Settings.mechDisplayMediumFontSize);

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

        wAmmo = new TransparentLabel("Ammo", fm, clr, TransparentLabel.LEFT);
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

        currentHeatBuildupL = new TransparentLabel("Heat Buildup: ", fm, clr, TransparentLabel.RIGHT);
        currentHeatBuildupR = new TransparentLabel("--", fm, clr, TransparentLabel.LEFT);

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
        wNameL = new TransparentLabel("Name", fm, clr, TransparentLabel.CENTER);
        wHeatL = new TransparentLabel("Heat", fm, clr, TransparentLabel.CENTER);
        wDamL =  new TransparentLabel("Damage", fm, clr, TransparentLabel.CENTER);
        wNameR = new TransparentLabel("", fm, clr, TransparentLabel.CENTER);
        wHeatR = new TransparentLabel("--", fm, clr, TransparentLabel.CENTER);
        wDamR =  new TransparentLabel("--", fm, clr, TransparentLabel.CENTER);


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
        wMinL = new TransparentLabel("Min", fm, clr, TransparentLabel.CENTER);
        wShortL = new TransparentLabel("Short", fm, clr, TransparentLabel.CENTER);
        wMedL = new TransparentLabel("Med", fm, clr, TransparentLabel.CENTER);
        wLongL = new TransparentLabel("Long", fm, clr, TransparentLabel.CENTER);
        wExtL = new TransparentLabel("Ext", fm, clr, TransparentLabel.CENTER);
        wMinR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER);
        wShortR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER);
        wMedR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER);
        wLongR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER);
        wExtR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER);
        
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
        wTargetL = new TransparentLabel("Target:", fm, clr, TransparentLabel.CENTER);
        wRangeL = new TransparentLabel("Range:", fm, clr, TransparentLabel.CENTER);
        wToHitL = new TransparentLabel("To Hit:", fm, clr, TransparentLabel.CENTER);

        wTargetR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER);
        wRangeR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER);
        wToHitR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER);

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
        toHitText = new TextArea("", 2, 20, TextArea.SCROLLBARS_VERTICAL_ONLY);
        toHitText.setEditable(false);
        toHitText.addKeyListener(client.menuBar);

        c.insets = new Insets(1, 9, 15, 9);
        c.gridx = 0; c.gridy = 10; c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout) getLayout()).setConstraints(toHitText, c);
        add(toHitText);


        setBackGround();


    }

    private void setBackGround(){
        Image tile = getToolkit().getImage("data/widgets/tile.gif");
        PMUtil.setImage(tile, (Component) this);
        int b = BackGroundDrawer.TILING_BOTH;
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_HORIZONTAL |
                BackGroundDrawer.VALIGN_TOP;
        tile = getToolkit().getImage("data/widgets/h_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_HORIZONTAL |
                BackGroundDrawer.VALIGN_BOTTOM;
        tile = getToolkit().getImage("data/widgets/h_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_VERTICAL |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage("data/widgets/v_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_VERTICAL |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage("data/widgets/v_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));


            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage("data/widgets/tl_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage("data/widgets/bl_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage("data/widgets/tr_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage("data/widgets/br_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

    }


    /**
     * updates fields for the specified mech
     *
     * fix the ammo when it's added
     */
    public void displayMech(Entity en) {

        // Grab a copy of the game.
        Game game = client.getClient().game;

        // update pointer to weapons
        this.weapons = en.getWeaponList();
        this.entity = en;

        int currentHeatBuildup = en.heat // heat from last round
            + en.getEngineCritHeat() // heat engine crits will add
            + en.heatBuildup; // heat we're building up this round
        if ( en instanceof Mech && en.infernos.isStillBurning() ) { // hit with inferno ammo
            currentHeatBuildup += en.infernos.getHeat();
        }
        Coords position = entity.getPosition();
        if (!en.isOffBoard()) {
            if ( position != null
                    && game.getBoard().getHex(position).levelOf(Terrain.FIRE) == 2 ) {
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
            // TODO : make this a StringBuffer.
            String wn = mounted.getDesc()
                        + " [" + en.getLocationAbbr(mounted.getLocation()) + "]";
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

                wn += " (" + shotsLeft + "/" + totalShotsLeft + ")";
            }

            // Fire Mode - lots of things have variable modes
            if (wtype.hasModes()) {
                wn += " " + mounted.curMode();
            }
            weaponList.add(wn);
            if (mounted.isUsedThisRound()
                && game.getPhase() == Game.PHASE_FIRING) {
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
          heatCapacityStr = heatCap + " [" + heatCapWater + "]";
        }
        // end duplicate block

        String heatText = Integer.toString(currentHeatBuildup);
        if (currentHeatBuildup > en.getHeatCapacityWithWater()) {
            heatText += "*"; // overheat indication
        }
        this.currentHeatBuildupR.setText(heatText + " (" + heatCapacityStr + ")");

        // If MaxTech range rules are in play, display the extreme range.
        if (game.getOptions().booleanOption("maxtech_range")) {
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
            wNameR.setText("");
            wHeatR.setText("--");
            wDamR.setText("--");
            wMinR.setText("---");
            wShortR.setText("---");
            wMedR.setText("---");
            wLongR.setText("---");
            wExtR.setText("---");
            return;
        }
        Mounted mounted = (Mounted)weapons.elementAt(weaponList.getSelectedIndex());
        WeaponType wtype = (WeaponType)mounted.getType();
        // update weapon display
        wNameR.setText(mounted.getDesc());
        wHeatR.setText(wtype.getHeat() + "");
        if(wtype.getDamage() == WeaponType.DAMAGE_MISSILE) {
            wDamR.setText("Missile");
        } else if(wtype.getDamage() == WeaponType.DAMAGE_VARIABLE) {
            wDamR.setText("Variable");
        } else if(wtype.getDamage() == WeaponType.DAMAGE_SPECIAL) {
            wDamR.setText("Special");
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
        if ( Entity.LOC_WET == entity.getLocationStatus(mounted.getLocation()) ) {
            shortR = wtype.getWShortRange();
            mediumR = wtype.getWMediumRange();
            longR = wtype.getWLongRange();
            extremeR = wtype.getWExtremeRange();
        }
        if(wtype.getMinimumRange() > 0) {
            wMinR.setText(Integer.toString(wtype.getMinimumRange()));
        } else {
            wMinR.setText("---");
        }
        if(shortR > 1) {
            wShortR.setText("1 - " + shortR);
        } else {
            wShortR.setText("" + shortR);
        }
        if(mediumR - shortR > 1) {
            wMedR.setText((shortR + 1) + " - " + mediumR);
        } else {
            wMedR.setText("" + mediumR);
        }
        if(longR - mediumR > 1) {
            wLongR.setText((mediumR + 1) + " - " + longR);
        } else {
            wLongR.setText("" + longR);
        }
        if(extremeR - longR > 1) {
            wExtR.setText((longR + 1) + " - " + extremeR);
        } else {
            wExtR.setText("" + extremeR);
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
        int ammoIndex = m.getDesc().indexOf("Ammo");
        int loc = m.getLocation();
        if (loc != Entity.LOC_NONE) {
            sb.append("[").append(entity.getLocationAbbr(loc)).append("] ");
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
            if(atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE)
            {
                wMinR.setText("4");
                wShortR.setText("1 - 9");
                wMedR.setText("10 - 18");
                wLongR.setText("19 - 27");
                wExtR.setText("28 - 36");
            }
            else if(atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE)
            {
                wMinR.setText("---");
                wShortR.setText("1 - 3");
                wMedR.setText("4 - 6");
                wLongR.setText("7 - 9");
                wExtR.setText("10 - 12");
            }
            else
            {
                wMinR.setText("4");
                wShortR.setText("1 - 5");
                wMedR.setText("6 - 10");
                wLongR.setText("11 - 15");
                wExtR.setText("16 - 20");
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
    private static Object SYSTEM = new Object();

    private TransparentLabel locLabel, slotLabel, modeLabel;
    public java.awt.List slotList;
    public java.awt.List locList;

    public Choice m_chMode;
    public Button m_bDumpAmmo;
    //public Label modeLabel;
    private ClientGUI clientgui;

    private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN, Settings.mechDisplayLargeFontSize);

    Entity en;

    public SystemPanel(ClientGUI clientgui) {
        super();

        FontMetrics fm = getFontMetrics(FONT_VALUE);

        this.clientgui = clientgui;
        locLabel = new TransparentLabel("Location", fm, Color.white,TransparentLabel.CENTER);
        slotLabel = new TransparentLabel("Slot", fm, Color.white,TransparentLabel.CENTER);

        locList = new List(8, false);
        locList.addItemListener(this);
        locList.addKeyListener(clientgui.menuBar);

        slotList = new List(12, false);
        slotList.addItemListener(this);
        slotList.addKeyListener(clientgui.menuBar);
        //slotList.setEnabled(false);

        m_chMode = new Choice();
        m_chMode.add("   ");
        m_chMode.setEnabled(false);
        m_chMode.addItemListener(this);
        m_chMode.addKeyListener(clientgui.menuBar);

        m_bDumpAmmo = new Button("Dump");
        m_bDumpAmmo.setEnabled(false);
        m_bDumpAmmo.setActionCommand("dump");
        m_bDumpAmmo.addActionListener(this);
        m_bDumpAmmo.addKeyListener(clientgui.menuBar);

        modeLabel = new TransparentLabel("Mode", fm, Color.white,TransparentLabel.RIGHT);
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
                sb.append("---");
            } else {
                switch(cs.getType()) {
                case CriticalSlot.TYPE_SYSTEM :
                    sb.append(cs.isDestroyed() ? "*" : "")
                        .append(cs.isBreached() ? "x" : "");
                    // Protomechs have different systme names.
                    if ( en instanceof Protomech ) {
                        sb.append(Protomech.systemNames[cs.getIndex()]);
                    } else {
                        sb.append(Mech.systemNames[cs.getIndex()]);
                    }
                    break;
                case CriticalSlot.TYPE_EQUIPMENT :
                    Mounted m = en.getEquipment(cs.getIndex());
                    sb.append(cs.isDestroyed() ? "*" : "").append(cs.isBreached() ? "x" : "").append(m.getDesc());
                    if (m.getType().hasModes()) {
                        sb.append(" (").append(m.curMode()).append(")");
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
                        sb.append(m.isDestroyed() ? "*" : "").append(m.isBreached() ? "x" : "").append(m.getDesc());
                        if (m.getType().hasModes()) {
                            sb.append(" (").append(m.curMode()).append(")");
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
                 && Game.PHASE_DEPLOYMENT != clientgui.getClient().game.getPhase()
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
                String[] saModes = m.getType().getModes();
                for (int x = 0; x < saModes.length; x++) {
                    m_chMode.add(saModes[x]);
                }
                m_chMode.select(m.curMode());
            }
        }
        else if (ev.getItemSelectable() == m_chMode) {
            Mounted m = getSelectedEquipment();
            if (m != null && m.getType().hasModes()) {
                int nMode = m.setMode(m_chMode.getSelectedItem());


                // send the event to the server
				clientgui.getClient().sendModeChange(en.getId(), en.getEquipmentNum(m), nMode);

                // notify the player
                if (m.getType().hasInstantModeSwitch()) {
					clientgui.systemMessage("Switched " + m.getName() + " to " + m.curMode());
                }
                else {
                    if (Game.PHASE_DEPLOYMENT == clientgui.getClient().game.getPhase() ) {
                         clientgui.systemMessage(m.getName() + " will switch to " + m.pendingMode() +
                            " at start of game.");
                    } else clientgui.systemMessage(m.getName() + " will switch to " + m.pendingMode() +
                            " at end of turn.");
                }
                displaySlots();
            }
        }
    }

    // ActionListener
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("dump")) {
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
                String title = "Cancel Dumping Ammo?";
                String body = "Do you want to cancel dumping " + m.getName() + "?";
                bConfirmed = clientgui.doYesNoDialog(title, body);
            }
            else {
                bDumping = true;
                String title = "Dump Ammo?";
                String body = "Do you want to dump " + m.getName() + "?\n\n"
                    + "Ammo dumping will start at the beginning of the next game turn.\n"
                    + "You will not be able to use the ammo while it is being dumped,\n"
                    + "but it can still be critically hit or explode from heat buildup.\n"
                    + "It will also explode as the result of any rear torso hit that\n"
                    + "turn.  Additionally, you will not be able to run or jump while\n"
                    + "you are dumping ammo.";
                bConfirmed = clientgui.doYesNoDialog(title, body);
            }

            if (bConfirmed) {
                m.setPendingDump(bDumping);
				clientgui.getClient().sendModeChange(en.getId(), en.getEquipmentNum(m), bDumping ? 1 : 0);
            }
        }
    }

    private void setBackGround(){
        Image tile = getToolkit().getImage("data/widgets/tile.gif");
        PMUtil.setImage(tile, (Component) this);
        int b = BackGroundDrawer.TILING_BOTH;
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_HORIZONTAL |
                BackGroundDrawer.VALIGN_TOP;
        tile = getToolkit().getImage("data/widgets/h_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_HORIZONTAL |
                BackGroundDrawer.VALIGN_BOTTOM;
        tile = getToolkit().getImage("data/widgets/h_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_VERTICAL |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage("data/widgets/v_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_VERTICAL |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage("data/widgets/v_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));


            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage("data/widgets/tl_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage("data/widgets/bl_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage("data/widgets/tr_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage("data/widgets/br_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

    }

}


/**
 * This class shows information about a unit that doesn't belong elsewhere.
 */
class ExtraPanel
    extends BufferedPanel
{
    private TransparentLabel  narcLabel, unusedL, carrysL, heatL;
    public TextArea unusedR, carrysR, heatR;
    public java.awt.List narcList;

    private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN, Settings.mechDisplayLargeFontSize);

    private ClientGUI clientgui;

    public ExtraPanel(ClientGUI clientgui) {
        super();

        this.clientgui = clientgui;

        FontMetrics fm = getFontMetrics(FONT_VALUE);

        narcLabel = new TransparentLabel
            ("Affected By:", fm, Color.white,TransparentLabel.CENTER);

        narcList = new List(3, false);
        narcList.addKeyListener(clientgui.menuBar);

        // transport stuff
        //unusedL = new Label( "Unused Space:", Label.CENTER );

        unusedL = new TransparentLabel
            ("Unused Space:", fm, Color.white,TransparentLabel.CENTER);
        unusedR = new TextArea("", 2, 25, TextArea.SCROLLBARS_VERTICAL_ONLY);
        unusedR.setEditable(false);
        unusedR.addKeyListener(clientgui.menuBar);

        carrysL = new TransparentLabel
            ( "Carrying:", fm, Color.white,TransparentLabel.CENTER);
        carrysR = new TextArea("", 4, 25, TextArea.SCROLLBARS_VERTICAL_ONLY);
        carrysR.setEditable(false);
        carrysR.addKeyListener(clientgui.menuBar);

        heatL = new TransparentLabel
            ( "Heat Effects:", fm, Color.white, TransparentLabel.CENTER);
        heatR = new TextArea ("", 4, 25, TextArea.SCROLLBARS_VERTICAL_ONLY);
        heatR.setEditable(false);
        heatR.addKeyListener(clientgui.menuBar);

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

        c.insets = new Insets(1, 9, 18, 9);
        c.weighty = 1.0;
        gridbag.setConstraints(carrysR, c);
        add(carrysR);

        c.weighty = 0.0;
        gridbag.setConstraints(heatL, c);
        add(heatL);

        c.insets = new Insets(1, 9, 18, 9);
        c.weighty = 1.0;
        gridbag.setConstraints(heatR, c);
        add(heatR);

        setBackGround();


    }



   private void setBackGround(){
        Image tile = getToolkit().getImage("data/widgets/tile.gif");
        PMUtil.setImage(tile, (Component) this);
        int b = BackGroundDrawer.TILING_BOTH;
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_HORIZONTAL |
                BackGroundDrawer.VALIGN_TOP;
        tile = getToolkit().getImage("data/widgets/h_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_HORIZONTAL |
                BackGroundDrawer.VALIGN_BOTTOM;
        tile = getToolkit().getImage("data/widgets/h_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_VERTICAL |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage("data/widgets/v_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.TILING_VERTICAL |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage("data/widgets/v_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));


            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage("data/widgets/tl_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage("data/widgets/bl_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage("data/widgets/tr_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

            b = BackGroundDrawer.NO_TILING |
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage("data/widgets/br_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));

    }


    /**
     * updates fields for the specified mech
     */
    public void displayMech(Entity en) {

        // Clear the "Affected By" list.
        narcList.removeAll();

        // Walk through the list of teams.  There
        // can't be more teams than players.
        StringBuffer buff = null;
        Enumeration loop = clientgui.getClient().game.getPlayers();
        while ( loop.hasMoreElements() ) {
            Player player = (Player) loop.nextElement();
            int team = player.getTeam();
            if ( en.isNarcedBy( team ) &&
                 !player.isObserver()) {
                buff = new StringBuffer( "NARCed by " );
                buff.append( player.getName() );
                buff.append( " [" )
                    .append( Player.teamNames[team] )
                    .append( "]" );
                narcList.add( buff.toString() );
            }
            if ( en.isINarcedBy( team ) &&
                    !player.isObserver()) {
                   buff = new StringBuffer( "iNarc Homing Pod from " );
                   buff.append( player.getName() );
                   buff.append( " [" )
                       .append( Player.teamNames[team] )
                       .append( "] attached." );
                   narcList.add( buff.toString() );
            }
        }
        if ( en.isINarcedWith(INarcPod.ECM)) {
               buff = new StringBuffer( "iNarc ECM Pod attached." );
               narcList.add( buff.toString() );
        }
        if ( en.isINarcedWith(INarcPod.HAYWIRE)) {
               buff = new StringBuffer( "iNarc Haywire Pod attached." );
               narcList.add( buff.toString() );
        }
        if ( en.isINarcedWith(INarcPod.NEMESIS)) {
               buff = new StringBuffer( "iNarc Nemesis Pod attached." );
               narcList.add( buff.toString() );
        }

        // Show inferno track.
        if ( en.infernos.isStillBurning() ) {
            buff = new StringBuffer( "Inferno burn remaining: " );
            buff.append( en.infernos.getTurnsLeftToBurn() );
            narcList.add( buff.toString() );
        }

        // Show ECM affect.
        Coords pos = en.getPosition();
        if ( Compute.isAffectedByECM( en, pos, pos ) ) {
            narcList.add( "In enemy ECM field." );
        }

        // Show Turret Locked.
        if ( en instanceof Tank &&
             !( (Tank) en ).hasNoTurret() &&
             !en.canChangeSecondaryFacing() ) {
            narcList.add( "Turret locked" );
        }

        // Show jammed weapons.
        Enumeration weaps = en.getWeapons();
        while ( weaps.hasMoreElements() ) {
            Mounted weapon = (Mounted) weaps.nextElement();
            if ( weapon.isJammed() ) {
                buff = new StringBuffer( weapon.getName() );
                buff.append( " is Jammed" );
                narcList.add( buff.toString() );
            }
        }

        // Show breached locations.
        for ( int loc = 0; loc < en.locations(); loc++ ) {
            if ( Entity.LOC_BREACHED == en.getLocationStatus(loc) ) {
                buff = new StringBuffer( en.getLocationName(loc) );
                buff.append( " Breached" );
                narcList.add( buff.toString() );
            }
        }

        // transport values
        String unused = en.getUnusedString();
        if ( unused.equals("") ) unused = "None";
        this.unusedR.setText( unused );
        Enumeration iter = en.getLoadedUnits().elements();
        carrysR.setText( null );
        boolean hasText = false;
        while ( iter.hasMoreElements() ) {
            carrysR.append( ((Entity)iter.nextElement()).getShortName() );
            hasText = true;
            if ( iter.hasMoreElements() ) {
                carrysR.append( "\n" );
            }
        }

        // Show club.
        Mounted club = Compute.clubMechHas( en );
        if ( null != club ) {
            if ( hasText ) {
                carrysR.append( "\n" );
            }
            carrysR.append( club.getName() );
        }

        // Show Heat Effects, but only for Mechs.
        heatR.setText("");
        if (en instanceof Mech) {
            boolean moreHeatMove = false, moreHeatFire = false;
            boolean hasTSM = false;
            if (((Mech)en).hasTSM()) hasTSM = true;

            if (en.heat>=30) {
                heatR.append("Was automaticly shut down\n");
                moreHeatMove = true;
            }
            if (en.heat >=25 && !moreHeatMove) {
                if (hasTSM) {
                    heatR.append ("-4 Movement Points\n");
                } else {
                    heatR.append("-5 Movement Points\n");
                }
                moreHeatMove = true;
            }
            if (en.heat >= 24) {
                heatR.append("+4 Modifier to Fire\n");
                moreHeatFire = true;
            }
            if (en.heat >= 20 && !moreHeatMove) {
                if (hasTSM) {
                    heatR.append ("-3 Movement Points\n");
                } else {
                    heatR.append ("-4 Movement Points\n");
                }
                moreHeatMove = true;
            }
            if (en.heat >= 17 && !moreHeatFire) {
                heatR.append ("+3 Modifier to Fire\n");
                moreHeatFire = true;
            }
            if (en.heat >= 15 && !moreHeatMove) {
                if (hasTSM) {
                    heatR.append ("-2 Movement Points\n");
                } else {
                    heatR.append ("-3 Movement Points\n");
                }
                moreHeatMove = true;
            }
            if (en.heat >= 13 && !moreHeatFire) {
                heatR.append ("+2 Modifier to Fire\n");
                moreHeatFire = true;
            }
            if (en.heat >= 10 && !moreHeatMove) {
                if (hasTSM) {
                    heatR.append ("-1 Movement Point\n");
                } else {
                    heatR.append ("-2 Movement Points\n");
                }
                moreHeatMove = true;
            }
            if (en.heat == 9 && !moreHeatMove && hasTSM) {
                heatR.append ("+1 Movement Point (TSM)\n");
                moreHeatMove = true;
            }
            if (en.heat >= 8 && !moreHeatFire) {
                heatR.append ("+1 Modifier to Fire\n");
                moreHeatFire = true;
            }
            if (en.heat >= 5 && !moreHeatMove) {
                heatR.append ("-1 Movement Point\n");
                moreHeatMove = true;
            }
            if (en.heat <=4) {
                heatR.append ("None\n");
            }
        }
    } // End public void displayMech( Entity )

} // End class ExtraPanel extends Panel

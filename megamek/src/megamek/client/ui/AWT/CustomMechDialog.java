/*
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

/*
 * CustomMechDialog.java
 *
 * Created on March 18, 2002, 2:56 PM
 */

package megamek.client;

import java.awt.*;

import megamek.common.*;

import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import megamek.common.options.IOptionGroup;
import megamek.common.options.IOption;
import megamek.common.options.PilotOptions;
import megamek.common.preference.PreferenceManager;

/**
 * A dialog that a player can use to customize his mech before battle.  
 * Currently, changing pilots, setting up C3 networks, changing ammunition,
 * deploying artillery offboard, setting MGs to rapidfire, setting auto-eject
 * is supported.
 *
 * @author  Ben
 * @version 
 */
public class CustomMechDialog 
extends ClientDialog implements ActionListener, DialogOptionListener { 
    
    private Label labName = new Label(Messages.getString("CustomMechDialog.labName"), Label.RIGHT); //$NON-NLS-1$
    private TextField fldName = new TextField(20);
    private Label labGunnery = new Label(Messages.getString("CustomMechDialog.labGunnery"), Label.RIGHT);; //$NON-NLS-1$
    private TextField fldGunnery = new TextField(3);
    private Label labPiloting = new Label(Messages.getString("CustomMechDialog.labPiloting"), Label.RIGHT);; //$NON-NLS-1$
    private TextField fldPiloting = new TextField(3);
    private Label labC3 = new Label(Messages.getString("CustomMechDialog.labC3"), Label.RIGHT);; //$NON-NLS-1$
    private Choice choC3 = new Choice();
    private int[] entityCorrespondance;
    private Label labCallsign = new Label(Messages.getString("CustomMechDialog.labCallsign"), Label.CENTER);; //$NON-NLS-1$
    private Label labUnitNum = new Label(Messages.getString("CustomMechDialog.labUnitNum"), Label.CENTER);; //$NON-NLS-1$
    private Choice choUnitNum = new Choice();
    private Vector entityUnitNum = new Vector();
    private Label labDeployment = new Label(Messages.getString("CustomMechDialog.labDeployment"), Label.RIGHT); //$NON-NLS-1$
    private Choice choDeployment = new Choice();
    private Label labAutoEject = new Label(Messages.getString("CustomMechDialog.labAutoEject"), Label.RIGHT); //$NON-NLS-1$
    private Checkbox chAutoEject = new Checkbox();
    
    private Label labOffBoard = new Label(Messages.getString("CustomMechDialog.labOffBoard"), Label.RIGHT); //$NON-NLS-1$
    private Checkbox chOffBoard = new Checkbox();
    private Label labOffBoardDirection = new Label(Messages.getString("CustomMechDialog.labOffBoardDirection"), Label.RIGHT); //$NON-NLS-1$
    private Choice choOffBoardDirection = new Choice();
    private Label labOffBoardDistance = new Label(Messages.getString("CustomMechDialog.labOffBoardDistance"), Label.RIGHT); //$NON-NLS-1$
    private TextField fldOffBoardDistance = new TextField(4);
    private Button butOffBoardDistance = new Button ("0");

    private Label labTargSys = new Label(Messages.getString("CustomMechDialog.labTargSys"), Label.RIGHT);
    private Choice choTargSys = new Choice();

    private Panel panButtons = new Panel();
    private Button butOkay = new Button(Messages.getString("Okay")); //$NON-NLS-1$
    private Button butCancel = new Button(Messages.getString("Cancel")); //$NON-NLS-1$
    private Button butNext = new Button(Messages.getString("Next"));
    private Button butPrev = new Button(Messages.getString("Previous"));
    
    private Vector m_vMunitions = new Vector();
    private Panel panMunitions = new Panel();
    private Vector m_vMGs = new Vector();
    private Panel panRapidfireMGs = new Panel();
        
    private Entity entity;
    private boolean okay = false;
    private ClientGUI clientgui;
    private Client client;

    private PilotOptions options;
    
    private Vector optionComps = new Vector();
    
    private Panel panOptions = new Panel();
    private ScrollPane scrOptions = new ScrollPane();
    
    private ScrollPane scrAll = new ScrollPane();

    private TextArea texDesc = new TextArea(Messages.getString("CustomMechDialog.texDesc"), 3, 35, TextArea.SCROLLBARS_VERTICAL_ONLY); //$NON-NLS-1$

    private boolean editable;
    
    private int direction = -1;
    private int distance = 17;
    
    /** Creates new CustomMechDialog */
    public CustomMechDialog(ClientGUI clientgui, Client client, Entity entity, boolean editable) {
        super(clientgui.frame, Messages.getString("CustomMechDialog.title"), true); //$NON-NLS-1$
        
        Panel tempPanel = new Panel();
        this.entity = entity;
        this.clientgui = clientgui;
        this.client = client;
        this.options = entity.getCrew().getOptions();
        this.editable = editable;
        
        texDesc.setEditable(false);
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        tempPanel.setLayout(gridbag);
            
        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(5, 5, 5, 5);
            
        c.weightx = 1.0;    c.weighty = 1.0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labName, c);
        tempPanel.add(labName);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(fldName, c);
        tempPanel.add(fldName);
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labGunnery, c);
        tempPanel.add(labGunnery);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(fldGunnery, c);
        tempPanel.add(fldGunnery);
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labPiloting, c);
        tempPanel.add(labPiloting);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(fldPiloting, c);
        tempPanel.add(fldPiloting);
        
        if (entity instanceof Mech) {
            Mech mech = (Mech)entity;
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(labAutoEject, c);
            tempPanel.add(labAutoEject);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(chAutoEject, c);
            tempPanel.add(chAutoEject);
            chAutoEject.setState(!mech.isAutoEject());
        }
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labDeployment, c);
        tempPanel.add(labDeployment);
      
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(choDeployment, c);
        tempPanel.add(choDeployment);
        refreshDeployment();

        if ( clientgui.getClient().game.getOptions().booleanOption("pilot_advantages") ) { //$NON-NLS-1$
          scrOptions.add(panOptions);
        
          c.weightx = 1.0;    c.weighty = 1.0;
          c.fill = GridBagConstraints.BOTH;
          c.gridwidth = GridBagConstraints.REMAINDER;
          gridbag.setConstraints(scrOptions, c);
          tempPanel.add(scrOptions);
  
          c.weightx = 1.0;    c.weighty = 0.0;
          gridbag.setConstraints(texDesc, c);
          tempPanel.add(texDesc);
        }
        
        if (entity.hasC3() || entity.hasC3i()) {        
          c.gridwidth = 1;
          c.anchor = GridBagConstraints.EAST;
          gridbag.setConstraints(labC3, c);
          tempPanel.add(labC3);
        
          c.gridwidth = GridBagConstraints.REMAINDER;
          c.anchor = GridBagConstraints.WEST;
          gridbag.setConstraints(choC3, c);
          tempPanel.add(choC3);
          refreshC3();
        }
        boolean eligibleForOffBoard = false;
        for (Enumeration i = entity.getWeapons(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            WeaponType wtype = (WeaponType)mounted.getType();
            if (wtype.hasFlag(WeaponType.F_ARTILLERY)) {
                eligibleForOffBoard = true;
            }
        }
        if (eligibleForOffBoard) {
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(labOffBoard, c);
            tempPanel.add(labOffBoard);
          
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(chOffBoard, c);
            tempPanel.add(chOffBoard);
            chOffBoard.setState(entity.isOffBoard());
            
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(labOffBoardDirection, c);
            tempPanel.add(labOffBoardDirection);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(choOffBoardDirection, c);
            choOffBoardDirection.add(Messages.getString("CustomMechDialog.North")); //$NON-NLS-1$
            choOffBoardDirection.add(Messages.getString("CustomMechDialog.South")); //$NON-NLS-1$
            choOffBoardDirection.add(Messages.getString("CustomMechDialog.East")); //$NON-NLS-1$
            choOffBoardDirection.add(Messages.getString("CustomMechDialog.West")); //$NON-NLS-1$
            direction = entity.getOffBoardDirection();
            if ( Entity.NONE == direction ) {
                direction = Entity.NORTH;
            }
            choOffBoardDirection.select( direction );
            tempPanel.add(choOffBoardDirection);
            
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(labOffBoardDistance, c);
            tempPanel.add(labOffBoardDistance);

            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            /*
            gridbag.setConstraints(fldOffBoardDistance, c);
            */
            
            butOffBoardDistance.addActionListener(this);
            gridbag.setConstraints(butOffBoardDistance, c);
            butOffBoardDistance.setLabel(Integer.toString(distance));
            tempPanel.add(butOffBoardDistance);
        }

        if (!(entity.hasTargComp()) && (clientgui.getClient().game.getOptions().booleanOption("allow_level_3_targsys")) && (entity instanceof Mech))
        {
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(labTargSys, c);
            tempPanel.add(labTargSys);

            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            choTargSys.add(MiscType.getTargetSysName(MiscType.T_TARGSYS_STANDARD));
            choTargSys.add(MiscType.getTargetSysName(MiscType.T_TARGSYS_LONGRANGE));
            choTargSys.add(MiscType.getTargetSysName(MiscType.T_TARGSYS_SHORTRANGE));
            choTargSys.add(MiscType.getTargetSysName(MiscType.T_TARGSYS_ANTI_AIR));
//            choTargSys.add(MiscType.getTargetSysName(MiscType.T_TARGSYS_MULTI_TRAC));
            gridbag.setConstraints(choTargSys, c);
            tempPanel.add(choTargSys);

            choTargSys.select(MiscType.getTargetSysName(entity.getTargSysType()));
        }

        if ( entity instanceof Protomech ) {
            // All Protomechs have a callsign.
            StringBuffer callsign = new StringBuffer(Messages.getString("CustomMechDialog.Callsign")); //$NON-NLS-1$
            callsign.append(": ");  //$NON-NLS-1$
            callsign.append( (char) (this.entity.getUnitNumber() +
                    PreferenceManager.getClientPreferences().getUnitStartChar()) )
                .append( '-' )
                .append( this.entity.getId() );
            labCallsign.setText( callsign.toString() );
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.CENTER;
            gridbag.setConstraints(labCallsign, c);
            tempPanel.add(labCallsign);

            // Get the Protomechs of this entity's player
            // that *aren't* in the entity's unit.
            Enumeration otherUnitEntities = client.game.getSelectedEntities
                ( new EntitySelector() {
                        private final int ownerId =
                            CustomMechDialog.this.entity.getOwnerId();
                        private final char unitNumber =
                            CustomMechDialog.this.entity.getUnitNumber();
                        public boolean accept( Entity entity ) {
                            if ( entity instanceof Protomech &&
                                 ownerId == entity.getOwnerId() &&
                                 unitNumber != entity.getUnitNumber() )
                                return true;
                            return false;
                        }
                    } );

            // If we got any other entites, show the unit number controls.
            if ( otherUnitEntities.hasMoreElements() ) {
                c.gridwidth = 1;
                c.anchor = GridBagConstraints.EAST;
                gridbag.setConstraints(labUnitNum, c);
                tempPanel.add(labUnitNum);

                c.gridwidth = GridBagConstraints.REMAINDER;
                c.anchor = GridBagConstraints.WEST;
                gridbag.setConstraints(choUnitNum, c);
                tempPanel.add(choUnitNum);
                refreshUnitNum(otherUnitEntities);
            }
        }

System.err.println("???Here...");
        // Can't set up munitions on infantry.
        if ( !(entity instanceof Infantry) ) {
System.err.println("???Here!!!");
            setupMunitions();
            c.anchor = GridBagConstraints.CENTER;
            gridbag.setConstraints(panMunitions, c);
            tempPanel.add(panMunitions);
        }
        
        // Set up rapidfire mg
        if (clientgui.getClient().game.getOptions().booleanOption("maxtech_burst")) { //$NON-NLS-1$
            setupRapidfireMGs();
            c.anchor = GridBagConstraints.CENTER;
            gridbag.setConstraints(panRapidfireMGs, c);
            tempPanel.add(panRapidfireMGs);
        }
        
        setupButtons();
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(panButtons, c);
        tempPanel.add(panButtons);
        
        fldName.setText(entity.getCrew().getName());
        fldName.addActionListener(this);
        fldGunnery.setText(new Integer(entity.getCrew().getGunnery()).toString());
        fldGunnery.addActionListener(this);
        fldPiloting.setText(new Integer(entity.getCrew().getPiloting()).toString());
        fldPiloting.addActionListener(this);
        
        if (!editable) {
            fldName.setEnabled(false);
            fldGunnery.setEnabled(false);
            fldPiloting.setEnabled(false);
            choC3.setEnabled(false);
            choDeployment.setEnabled(false);
            chAutoEject.setEnabled(false);
            disableMunitionEditing();
            disableMGSetting();
            chOffBoard.setEnabled(false);
            choOffBoardDirection.setEnabled(false);
            fldOffBoardDistance.setEnabled(false);
        }
        scrAll.add(tempPanel);

        // add the scrollable panel
        this.add(scrAll);
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { setVisible(false); }
        });

        pack();
        setLocationAndSize(tempPanel.getSize());
    }
    
    private void setupButtons() {
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);
        butNext.addActionListener(this);
        butPrev.addActionListener(this);
        
        // layout

        panButtons.setLayout(new GridLayout(1, 4, 10, 0));
        panButtons.add(butPrev);
        panButtons.add(butOkay);
        panButtons.add(butCancel);
        panButtons.add(butNext);
     }
    
    private void setupRapidfireMGs() {
        GridBagLayout gbl = new GridBagLayout();
        panMunitions.setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();
        
        int row = 0;
        for (Enumeration e = entity.getWeapons(); e.hasMoreElements(); ) {
            Mounted m = (Mounted)e.nextElement();
            WeaponType wtype = (WeaponType)m.getType();
            if (wtype.hasFlag(WeaponType.F_MG)) {
                gbc.gridy = row++;
                RapidfireMGPanel rmp = new RapidfireMGPanel(m);                
                gbl.setConstraints(rmp, gbc);
                panRapidfireMGs.add(rmp);
                m_vMGs.addElement(rmp);
            }
        }
    }
    
    private void setupMunitions() {
        
        GridBagLayout gbl = new GridBagLayout();
        panMunitions.setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();
        
        int row = 0;
        for (Enumeration e = entity.getAmmo(); e.hasMoreElements(); ) {
            Mounted m = (Mounted)e.nextElement();
            AmmoType at = (AmmoType)m.getType();
            Vector vTypes = new Vector();
            Vector vAllTypes = AmmoType.getMunitionsFor(at.getAmmoType());
            if (vAllTypes == null) {
                continue;
            }

            for (int x = 0, n = vAllTypes.size(); x < n; x++) {
                AmmoType atCheck = (AmmoType)vAllTypes.elementAt(x);
                boolean bTechMatch = TechConstants.isLegal(entity.getTechLevel(), atCheck.getTechLevel());
                                
                // allow all lvl2 IS units to use level 1 ammo
                // lvl1 IS units don't need to be allowed to use lvl1 ammo,
                // because there is no special lvl1 ammo, therefore it doesn't
                // need to show up in this display.
                if (!bTechMatch && entity.getTechLevel() == TechConstants.T_IS_LEVEL_2 &&
                    atCheck.getTechLevel() == TechConstants.T_IS_LEVEL_1) {
                    bTechMatch = true;
                }
                
                // if is_eq_limits is unchecked allow l1 guys to use l2 stuff
                if (!clientgui.getClient().game.getOptions().booleanOption("is_eq_limits") //$NON-NLS-1$
                    && entity.getTechLevel() == TechConstants.T_IS_LEVEL_1
                    && atCheck.getTechLevel() == TechConstants.T_IS_LEVEL_2) {
                    bTechMatch = true;
                }

                // Possibly allow level 3 ammos, possibly not.
                if (clientgui.getClient().game.getOptions().booleanOption("allow_level_3_ammo")) {
                    if (!clientgui.getClient().game.getOptions().booleanOption("is_eq_limits")) {
                        if (entity.getTechLevel() == TechConstants.T_CLAN_LEVEL_2
                                && atCheck.getTechLevel() == TechConstants.T_CLAN_LEVEL_3) {
                            bTechMatch = true;
                        }
                        if (((entity.getTechLevel() == TechConstants.T_IS_LEVEL_1) || (entity.getTechLevel() == TechConstants.T_IS_LEVEL_2))
                                && (atCheck.getTechLevel() == TechConstants.T_IS_LEVEL_3)) {
                            bTechMatch = true;
                        }
                    }
                } else if ((atCheck.getTechLevel() == TechConstants.T_IS_LEVEL_3) || (atCheck.getTechLevel() == TechConstants.T_CLAN_LEVEL_3)) {
                    bTechMatch = false;
                }
                
                //allow mixed Tech Mechs to use both IS and Clan ammo of any
                // level (since mixed tech is always level 3)
                if (entity.isMixedTech()) {
                    bTechMatch = true;
                }

                // If clan_ignore_eq_limits is unchecked,
                // do NOT allow Clans to use IS-only ammo.
                // N.B. play bit-shifting games to allow "incendiary"
                //      to be combined to other munition types.
                int muniType = atCheck.getMunitionType();
                muniType &= ~AmmoType.M_INCENDIARY;
                if ( !clientgui.getClient().game.getOptions().booleanOption("clan_ignore_eq_limits") //$NON-NLS-1$
                     && entity.isClan()
                     && ( muniType == AmmoType.M_SEMIGUIDED ||
                          muniType == AmmoType.M_THUNDER_AUGMENTED ||
                          muniType == AmmoType.M_THUNDER_INFERNO   ||
                          muniType == AmmoType.M_THUNDER_VIBRABOMB ||
                          muniType == AmmoType.M_THUNDER_ACTIVE ||
                          muniType == AmmoType.M_INFERNO_IV ||
                          muniType == AmmoType.M_VIBRABOMB_IV)) {
                    bTechMatch = false;
                }

                if ( !clientgui.getClient().game.getOptions().booleanOption("minefields") && //$NON-NLS-1$
                     AmmoType.canDeliverMinefield(atCheck) ) {
                    continue;
                }

                // Only Protos can use Proto-specific ammo
                if ( atCheck.hasFlag(AmmoType.F_PROTOMECH) &&
                     !(entity instanceof Protomech) ) {
                    continue;
                }

                // When dealing with machine guns, Protos can only
                //  use proto-specific machine gun ammo
                if ( entity instanceof Protomech &&
                     atCheck.hasFlag(AmmoType.F_MG) &&
                     !atCheck.hasFlag(AmmoType.F_PROTOMECH) ) {
                    continue;
                }

                // Battle Armor ammo can't be selected at all.
                // All other ammo types need to match on rack size and tech.
                if ( bTechMatch &&
                     atCheck.getRackSize() == at.getRackSize() &&
                     !atCheck.hasFlag(AmmoType.F_BATTLEARMOR) &&
                     atCheck.getTonnage(entity) == at.getTonnage(entity) ) {
                    vTypes.addElement(atCheck);
                }
            }
            if (vTypes.size() < 2 && !client.game.getOptions().booleanOption("lobby_ammo_dump")) { //$NON-NLS-1$
                continue;
            }
            
            gbc.gridy = row++;
            // Protomechs need special choice panels.
            MunitionChoicePanel mcp = null;
            if ( entity instanceof Protomech ) {
                mcp = new ProtomechMunitionChoicePanel(m, vTypes);
            } else {
                mcp = new MunitionChoicePanel(m, vTypes);
            }
            gbl.setConstraints(mcp, gbc);
            panMunitions.add(mcp);
            m_vMunitions.addElement(mcp);
        }
    }
    
    class MunitionChoicePanel extends Panel {
        private Vector m_vTypes;
        private Choice m_choice;
        private Mounted m_mounted;
        
        protected Label labDump = new Label(Messages.getString("CustomMechDialog.labDump")); //$NON-NLS-1$
        protected Checkbox chDump = new Checkbox();
                
        public MunitionChoicePanel(Mounted m, Vector vTypes) {
            m_vTypes = vTypes;
            m_mounted = m;
            AmmoType curType = (AmmoType)m.getType();
            m_choice = new Choice();
            Enumeration e = m_vTypes.elements();
            for (int x = 0; e.hasMoreElements(); x++) {
                AmmoType at = (AmmoType)e.nextElement();
                m_choice.add(at.getName());
                if (at.getMunitionType() == curType.getMunitionType()) {
                    m_choice.select(x);
                }
            }
            int loc;
            setLayout(new GridLayout(2, 2));
            if (m.getLocation() == Entity.LOC_NONE) {
                // oneshot weapons don't have a location of their own
                Mounted linkedBy = m.getLinkedBy();
                loc = linkedBy.getLocation();
            } else {
                loc = m.getLocation();
            }
            String sDesc = "(" + entity.getLocationAbbr(loc) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
            add(new Label(sDesc));
            add(m_choice);
            if (clientgui.getClient().game.getOptions().booleanOption("lobby_ammo_dump") ) { //$NON-NLS-1$
                add(labDump);
                add(chDump);
            }
        }

        public void applyChoice() {
            int n = m_choice.getSelectedIndex();
            AmmoType at = (AmmoType)m_vTypes.elementAt(n);
            m_mounted.changeAmmoType(at);
            if (chDump.getState()) {
                m_mounted.setShotsLeft(0);
            }
        }

        public void setEnabled(boolean enabled) {
            m_choice.setEnabled(enabled);
        }

        /**
         * Get the number of shots in the mount.
         *
         * @return      the <code>int</code> number of shots in the mount.
         */
        /* package */ int getShotsLeft() {
            return m_mounted.getShotsLeft();
        }

        /**
         * Set the number of shots in the mount.
         *
         * @param shots the <code>int</code> number of shots for the mount.
         */
        /* package */ void setShotsLeft( int shots ) {
            m_mounted.setShotsLeft( shots );
        }
    }

    /**
     * When a Protomech selects ammo, you need to adjust the shots on the
     * unit for the weight of the selected munition.
     */
    class ProtomechMunitionChoicePanel extends MunitionChoicePanel {
        private final float m_origShotsLeft;
        private final AmmoType m_origAmmo;

        public ProtomechMunitionChoicePanel(Mounted m, Vector vTypes) {
            super( m, vTypes );
            m_origAmmo = (AmmoType) m.getType();
            m_origShotsLeft = m.getShotsLeft();
        }

        /**
         * All ammo must be applied in ratios to the starting load.
         */
        public void applyChoice() {
            super.applyChoice();

            // Calculate the number of shots for the new ammo.
            // N.B. Some special ammos are twice as heavy as normal
            // so they have half the number of shots (rounded down).
            setShotsLeft( Math.round( getShotsLeft() * m_origShotsLeft /
                                      m_origAmmo.getShots() ) );
            if (chDump.getState()) {
                setShotsLeft(0);
            }            
        }
    }
    
    class RapidfireMGPanel extends Panel {
        private Mounted m_mounted;
        
        protected Checkbox chRapid = new Checkbox();
                
        public RapidfireMGPanel(Mounted m) {
            m_mounted = m;
            int loc;
            setLayout(new GridLayout(2, 2));
            loc = m.getLocation();            
            add(new Label(Messages.getString("CustomMechDialog.switchToRapidFire", new Object[]{entity.getLocationAbbr(loc)}))); //$NON-NLS-1$
            chRapid.setState(m.isRapidfire());
            add(chRapid);
        }

        public void applyChoice() {
            boolean b = chRapid.getState();
            m_mounted.setRapidfire(b);
        }

        public void setEnabled(boolean enabled) {
            chRapid.setEnabled(enabled);
        }
    }

    public void disableMunitionEditing() {
        for (int i = 0; i < m_vMunitions.size(); i++) {
            ((MunitionChoicePanel)m_vMunitions.elementAt(i)).setEnabled(false);
        }
    }
    
    public void disableMGSetting() {
        for (int i = 0; i < m_vMGs.size(); i++) {
            ((MunitionChoicePanel)m_vMGs.elementAt(i)).setEnabled(false);
        }
    }

    public void setOptions() {
      IOption option;
      
      for (Enumeration i = optionComps.elements(); i.hasMoreElements();) {
        DialogOptionComponent comp = (DialogOptionComponent)i.nextElement();
        
        option = comp.getOption();
        
        if ( (comp.getValue() == Messages.getString("CustomMechDialog.None")) ) { //NON-NLS-$1
            entity.getCrew().getOptions().getOption(option.getName()).setValue("None"); //NON-NLS-$1
        } else entity.getCrew().getOptions().getOption(option.getName()).setValue(comp.getValue());
      }
    }
    
    public void resetOptions() {
      IOption option;
      
      for (Enumeration i = optionComps.elements(); i.hasMoreElements();) {
        DialogOptionComponent comp = (DialogOptionComponent)i.nextElement();
        
        option = comp.getOption();
        option.setValue(false);
                
        entity.getCrew().getOptions().getOption(option.getName()).setValue(comp.getValue());
      }
    }
    
    public void refreshOptions() {
        panOptions.removeAll();
        optionComps = new Vector();
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panOptions.setLayout(gridbag);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        c.ipadx = 0;    c.ipady = 0;
        
        for (Enumeration i = options.getGroups(); i.hasMoreElements();) {
            IOptionGroup group = (IOptionGroup)i.nextElement();
            
            addGroup(group, gridbag, c);
            
            for (Enumeration j = group.getOptions(); j.hasMoreElements();) {
                IOption option = (IOption)j.nextElement();

                addOption(option, gridbag, c, editable);
            }
        }
        
        validate();
    }
    
    private void addGroup(IOptionGroup group, GridBagLayout gridbag, GridBagConstraints c) {
        Label groupLabel = new Label(group.getDisplayableName());
        
        gridbag.setConstraints(groupLabel, c);
        panOptions.add(groupLabel);
    }
    
    private void addOption(IOption option, GridBagLayout gridbag, GridBagConstraints c, boolean editable) {
        DialogOptionComponent optionComp = new DialogOptionComponent(this, option, editable);

        if (option.getName().equals("weapon_specialist")) { //$NON-NLS-1$
            optionComp.addValue(Messages.getString("CustomMechDialog.None")); //$NON-NLS-1$
            Hashtable uniqueWeapons = new Hashtable();
            for (int i = 0; i < entity.getWeaponList().size(); i++) {
                Mounted m = (Mounted)entity.getWeaponList().elementAt(i);
                uniqueWeapons.put(m.getName(),new Boolean(true));
            }
            String enumValue;
            for (Enumeration e = uniqueWeapons.keys(); e.hasMoreElements(); ) {
                optionComp.addValue((String)e.nextElement());
            }
            optionComp.setSelected(option.stringValue());
        }
        
        gridbag.setConstraints(optionComp, c);
        panOptions.add(optionComp);
        
        optionComps.addElement(optionComp);
    }
    
    public void showDescFor(IOption option) {
        texDesc.setText(option.getDescription());
    }

    // TODO : implement me!!!
    public void optionClicked( DialogOptionComponent comp,
                               IOption option, boolean state ) {
    }

    public boolean isOkay() {
        return okay;
    }

    private void refreshDeployment() {
      choDeployment.removeAll();
      choDeployment.add(Messages.getString("CustomMechDialog.StartOfGame")); //$NON-NLS-1$
      
      if ( entity.getDeployRound() < 1 )
        choDeployment.select(0);
        
      for ( int i = 1; i <= 15; i++ ) {
        choDeployment.add(Messages.getString("CustomMechDialog.AfterRound") + i); //$NON-NLS-1$
        
        if ( entity.getDeployRound() == i )
          choDeployment.select(i);
      }
    }
    
    private void refreshC3() {
        choC3.removeAll();
        int listIndex = 0;
        entityCorrespondance = new int[client.game.getNoOfEntities() + 2];

        if(entity.hasC3i()) {
            choC3.add(Messages.getString("CustomMechDialog.CreateNewNetwork")); //$NON-NLS-1$
            if(entity.getC3Master() == null) choC3.select(listIndex);
            entityCorrespondance[listIndex++] = entity.getId();
        }
        else if ( entity.hasC3MM() ) {
            int mNodes = entity.calculateFreeC3MNodes();
            int sNodes = entity.calculateFreeC3Nodes();

            choC3.add(Messages.getString("CustomMechDialog.setCompanyMaster", new Object[]{new Integer(mNodes), new Integer(sNodes)})); //$NON-NLS-1$
              
            if(entity.C3MasterIs(entity)) choC3.select(listIndex);
            entityCorrespondance[listIndex++] = entity.getId();

            choC3.add(Messages.getString("CustomMechDialog.setIndependentMaster", new Object[]{new Integer(sNodes)})); //$NON-NLS-1$
            if(entity.getC3Master() == null) choC3.select(listIndex);
            entityCorrespondance[listIndex++] = -1;

        }

        else if(entity.hasC3M()) {
            int nodes = entity.calculateFreeC3Nodes();

            choC3.add(Messages.getString("CustomMechDialog.setCompanyMaster1", new Object[]{new Integer(nodes)})); //$NON-NLS-1$
            if(entity.C3MasterIs(entity)) choC3.select(listIndex);
            entityCorrespondance[listIndex++] = entity.getId();

            choC3.add(Messages.getString("CustomMechDialog.setIndependentMaster", new Object[]{new Integer(nodes)})); //$NON-NLS-1$
            if(entity.getC3Master() == null) choC3.select(listIndex);
            entityCorrespondance[listIndex++] = -1;

        }
        for (Enumeration i = client.getEntities(); i.hasMoreElements();) {
            final Entity e = (Entity)i.nextElement();
            // ignore enemies or self
            if(entity.isEnemyOf(e) || entity.equals(e)) {
                continue;
            }
            // c3i only links with c3i
            if (entity.hasC3i() != e.hasC3i()) {
                continue;
            }
            int nodes = e.calculateFreeC3Nodes();
            if ( e.hasC3MM() && entity.hasC3M() && e.C3MasterIs(e) ) {
                nodes = e.calculateFreeC3MNodes();
            }
            if (entity.C3MasterIs(e) && !entity.equals(e)) {
                nodes++;
            }
            if (entity.hasC3i() && (entity.onSameC3NetworkAs(e) || entity.equals(e))) {
                nodes++;
            }
            if (nodes == 0) {
                continue;
            }
            if(e.hasC3i()) {
                if (entity.onSameC3NetworkAs(e)) {
                    choC3.add(Messages.getString("CustomMechDialog.join1", new Object[]{e.getDisplayName(), e.getC3NetId(), new Integer(nodes - 1)})); //$NON-NLS-1$
                    choC3.select(listIndex);
                }
                else {
                    choC3.add(Messages.getString("CustomMechDialog.join2", new Object[]{e.getDisplayName(), e.getC3NetId(), new Integer(nodes)})); //$NON-NLS-1$
                }
                entityCorrespondance[listIndex++] = e.getId();
            }
            else if ( e.C3MasterIs(e) && e.hasC3MM()) {
                // Company masters with 2 computers can have
                // *both* sub-masters AND slave units.
                choC3.add(Messages.getString("CustomMechDialog.connect2", new Object[]{e.getDisplayName(), e.getC3NetId(), new Integer(nodes)})); //$NON-NLS-1$
                entityCorrespondance[listIndex] = e.getId();
                if (entity.C3MasterIs(e)) {
                    choC3.select(listIndex);
                }
                listIndex++;
            }
            else if ( e.C3MasterIs(e) != entity.hasC3M() ) {
                // If we're a slave-unit, we can only connect to sub-masters,
                // not main masters likewise, if we're a master unit, we can
                // only connect to main master units, not sub-masters.
            }
            else if (entity.C3MasterIs(e)) {
                choC3.add(Messages.getString("CustomMechDialog.connect1", new Object[]{e.getDisplayName(), e.getC3NetId(), new Integer(nodes-1)})); //$NON-NLS-1$
                choC3.select(listIndex);
                entityCorrespondance[listIndex++] = e.getId();
            }
            else {
                choC3.add(Messages.getString("CustomMechDialog.connect2", new Object[]{e.getDisplayName(), e.getC3NetId(), new Integer(nodes)})); //$NON-NLS-1$
                entityCorrespondance[listIndex++] = e.getId();
            }
        }
    }

    /**
     * Populate the list of entities in other units from the given enumeration.
     *
     * @param   others the <code>Enumeration</code> containing entities in
     *          other units.
     */    
    private void refreshUnitNum( Enumeration others ) {
        // Clear the list of old values
        choUnitNum.removeAll();
        entityUnitNum.removeAllElements();

        // Make an entry for "no change".
        choUnitNum.add( Messages.getString("CustomMechDialog.doNotSwapUnits") ); //$NON-NLS-1$
        entityUnitNum.addElement( this.entity );

        // Walk through the other entities.
        while ( others.hasMoreElements() ) {
            // Track the position of the next other entity.
            final Entity other = (Entity) others.nextElement();
            entityUnitNum.addElement( other );

            // Show the other entity's name and callsign.
            StringBuffer callsign = new StringBuffer( other.getDisplayName() );
            callsign.append( " (" ) //$NON-NLS-1$
                .append( (char) (other.getUnitNumber() +
                                 PreferenceManager.getClientPreferences().getUnitStartChar()) )
                .append( '-' )
                .append( other.getId() )
                .append( ')' );
            choUnitNum.add( callsign.toString() );
        }
        choUnitNum.select(0);
    }

    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        if (actionEvent.getSource() == butOffBoardDistance) {
            Slider sl = new Slider (clientgui.frame, Messages.getString("CustomMechDialog.offboardDistanceTitle"), Messages.getString("CustomMechDialog.offboardDistanceQuestion"),
                                    entity.getOffBoardDistance(), 17, 170);
            if (!sl.showDialog()) return;
            distance = sl.getValue();
            butOffBoardDistance.setLabel(Integer.toString(distance));
            // butOffBoardDistance = new Button (Integer.toString(sl.getValue()));
            // butOffBoardDistance.addActionListener(this);
            return;
        }
        if (actionEvent.getSource() != butCancel) {
            // get values
            String name = fldName.getText();
            int gunnery;
            int piloting;
            int offBoardDistance;
            boolean autoEject = chAutoEject.getState();
            try {
                gunnery = Integer.parseInt(fldGunnery.getText());
                piloting =  Integer.parseInt(fldPiloting.getText());
            } catch (NumberFormatException e) {
                new AlertDialog(clientgui.frame, Messages.getString("CustomMechDialog.NumberFormatError"), Messages.getString("CustomMechDialog.EnterValidSkills")).show(); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            
            // keep these reasonable, please
            if (gunnery < 0 || gunnery > 7 || piloting < 0 || piloting > 7) {
                new AlertDialog(clientgui.frame, Messages.getString("CustomMechDialog.NumberFormatError"), Messages.getString("CustomMechDialog.EnterSkillsBetween0_7")).show(); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            if (chOffBoard.getState()){
                try {
                    offBoardDistance = distance;
                } catch (NumberFormatException e) {
                    new AlertDialog(clientgui.frame, Messages.getString("CustomMechDialog.NumberFormatError"), Messages.getString("CustomMechDialog.EnterValidSkills")).show(); //$NON-NLS-1$ //$NON-NLS-2$
                    return;
                }
                if (offBoardDistance < 17) {
                    new AlertDialog(clientgui.frame, Messages.getString("CustomMechDialog.NumberFormatError"), Messages.getString("CustomMechDialog.OffboardDistance")).show(); //$NON-NLS-1$ //$NON-NLS-2$
                    return;
                }
                entity.setOffBoard( offBoardDistance,
                                    choOffBoardDirection.getSelectedIndex() );
            }
            else {
                entity.setOffBoard( 0, Entity.NONE );
            }

            // change entity
            entity.setCrew(new Pilot(name, gunnery, piloting));
            if (entity instanceof Mech) {
                Mech mech = (Mech)entity;
                mech.setAutoEject(!autoEject);
            }
            if(entity.hasC3() && choC3.getSelectedIndex() > -1) {
                Entity chosen = client.getEntity
                    ( entityCorrespondance[choC3.getSelectedIndex()] );
                int entC3nodeCount = 
                    client.game.getC3SubNetworkMembers( entity ).size();
                int choC3nodeCount = 
                    client.game.getC3NetworkMembers( chosen ).size();
                if ( entC3nodeCount + choC3nodeCount <= Entity.MAX_C3_NODES ) {
                    entity.setC3Master( chosen );
                }
                else {
                    String message = Messages.getString("CustomMechDialog.NetworkTooBig.message", new Object[]{ //$NON-NLS-1$
                            entity.getShortName(), chosen.getShortName(), 
                            new Integer(entC3nodeCount), new Integer(choC3nodeCount),
                            new Integer(Entity.MAX_C3_NODES)});
                    clientgui.doAlertDialog( Messages.getString("CustomMechDialog.NetworkTooBig.title"), //$NON-NLS-1$
                                          message);
                    refreshC3();
                }
            }
            else if(entity.hasC3i() && choC3.getSelectedIndex() > -1) {
                entity.setC3NetId(client.getEntity(entityCorrespondance[choC3.getSelectedIndex()]));
            }

            // Update the entity's targetting system type.
            if (!(entity.hasTargComp()) && (clientgui.getClient().game.getOptions().booleanOption("allow_level_3_targsys"))) {
                int targSysIndex = MiscType.getTargetSysType(choTargSys.getSelectedItem());
                if (targSysIndex >= 0)
                    entity.setTargSysType(targSysIndex);
                else {
                    System.err.println("Illegal targetting system index: "+targSysIndex);
                    entity.setTargSysType(MiscType.T_TARGSYS_STANDARD);
                }
            }

            // If the player wants to swap unit numbers, update both
            // entities and send an update packet for the other entity.
            if ( !entityUnitNum.isEmpty() &&
                 choUnitNum.getSelectedIndex() > 0 ) {
                Entity other =  (Entity) this.entityUnitNum.elementAt
                    ( choUnitNum.getSelectedIndex() );
                char temp = this.entity.getUnitNumber();
                this.entity.setUnitNumber( other.getUnitNumber() );
                other.setUnitNumber( temp );
                client.sendUpdateEntity( other );
            }

            // Set the entity's deployment round.
            entity.setDeployRound(choDeployment.getSelectedIndex());

            // update munitions selections
            for (Enumeration e = m_vMunitions.elements(); e.hasMoreElements(); ) {
                ((MunitionChoicePanel)e.nextElement()).applyChoice();
            }
            // update MG rapid fire settings
            for (Enumeration e = m_vMGs.elements(); e.hasMoreElements(); ) {
                ((RapidfireMGPanel)e.nextElement()).applyChoice();
            }
            
            setOptions();
            
            okay = true;
        }
        this.setVisible(false);
        Entity nextOne = null;
        if (actionEvent.getSource() == butOkay) {
        } else if (actionEvent.getSource() == butPrev) {
            nextOne = client.game.getPreviousEntity(entity);
        } else if (actionEvent.getSource() == butNext) {
            nextOne = client.game.getNextEntity(entity);
        }
        if (nextOne!=null) {
            clientgui.chatlounge.refreshEntities();
            clientgui.chatlounge.customizeMech(nextOne);
        }
    }
}

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
import megamek.common.options.OptionGroup;
import megamek.common.options.GameOption;

/**
 * A dialog that a player can use to customize his mech before battle.  
 * Currently, only changing pilots is supported.
 *
 * @author  Ben
 * @version 
 */
public class CustomMechDialog 
extends Dialog implements ActionListener, DialogOptionListener { 
    
    private Label labName = new Label("Name: ", Label.RIGHT);
    private TextField fldName = new TextField(20);
    private Label labGunnery = new Label("Gunnery: ", Label.RIGHT);;
    private TextField fldGunnery = new TextField(3);
    private Label labPiloting = new Label("Piloting: ", Label.RIGHT);;
    private TextField fldPiloting = new TextField(3);
    private Label labC3 = new Label("C3 Network: ", Label.RIGHT);;
    private Choice choC3 = new Choice();
    private int[] entityCorrespondance;
    private Label labCallsign = new Label("Callsign: ", Label.CENTER);;
    private Label labUnitNum = new Label("Swap Units With: ", Label.CENTER);;
    private Choice choUnitNum = new Choice();
    private Vector entityUnitNum = new Vector();
    private Label labDeployment = new Label("Deployment Round: ", Label.RIGHT);
    private Choice choDeployment = new Choice();
    private Label labAutoEject = new Label("Disable Automatic ejection? ", Label.RIGHT);
    private Checkbox chAutoEject = new Checkbox();
    
    private Label labOffBoard = new Label("Deploy Offboard?", Label.RIGHT);
    private Checkbox chOffBoard = new Checkbox();
    private Label labOffBoardDirection = new Label("Offboard Direction:", Label.RIGHT);
    private Choice choOffBoardDirection = new Choice();
    private Label labOffBoardDistance = new Label("Offboard Distance in hexes:", Label.RIGHT);
    private TextField fldOffBoardDistance = new TextField(4);
    
    private Panel panButtons = new Panel();
    private Button butOkay = new Button("Okay");
    private Button butCancel = new Button("Cancel");

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

    private TextArea texDesc = new TextArea("Mouse over an option to see a description.", 3, 35, TextArea.SCROLLBARS_VERTICAL_ONLY);

    private boolean editable;
    
    /** Creates new CustomMechDialog */
    public CustomMechDialog(ClientGUI clientgui, Client client, Entity entity, boolean editable) {
        super(clientgui.frame, "Customize pilot/mech stats...", true);
        
        this.entity = entity;
        this.clientgui = clientgui;
        this.client = client;
        this.options = entity.getCrew().getOptions();
        this.editable = editable;
        
        texDesc.setEditable(false);
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
            
        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(5, 5, 5, 5);
            
        c.weightx = 1.0;    c.weighty = 1.0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labName, c);
        add(labName);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(fldName, c);
        add(fldName);
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labGunnery, c);
        add(labGunnery);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(fldGunnery, c);
        add(fldGunnery);
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labPiloting, c);
        add(labPiloting);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(fldPiloting, c);
        add(fldPiloting);
        
        if (entity instanceof Mech) {
            Mech mech = (Mech)entity;
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(labAutoEject, c);
            add(labAutoEject);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(chAutoEject, c);
            add(chAutoEject);
            chAutoEject.setState(!mech.isAutoEject());
        }
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labDeployment, c);
        add(labDeployment);
      
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(choDeployment, c);
        add(choDeployment);
        refreshDeployment();

        if ( clientgui.getClient().game.getOptions().booleanOption("pilot_advantages") ) {
          scrOptions.add(panOptions);
        
          c.weightx = 1.0;    c.weighty = 1.0;
          c.fill = GridBagConstraints.BOTH;
          c.gridwidth = GridBagConstraints.REMAINDER;
          gridbag.setConstraints(scrOptions, c);
          add(scrOptions);
  
          c.weightx = 1.0;    c.weighty = 0.0;
          gridbag.setConstraints(texDesc, c);
          add(texDesc);
        }
        
        if(entity.hasC3() || entity.hasC3i())
        {        
          c.gridwidth = 1;
          c.anchor = GridBagConstraints.EAST;
          gridbag.setConstraints(labC3, c);
          add(labC3);
        
          c.gridwidth = GridBagConstraints.REMAINDER;
          c.anchor = GridBagConstraints.WEST;
          gridbag.setConstraints(choC3, c);
          add(choC3);
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
            add(labOffBoard);
          
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(chOffBoard, c);
            add(chOffBoard);
            chOffBoard.setState(entity.isOffBoard());
            
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(labOffBoardDirection, c);
            add(labOffBoardDirection);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(choOffBoardDirection, c);
            choOffBoardDirection.add("North");
            choOffBoardDirection.add("South");
            choOffBoardDirection.add("East");
            choOffBoardDirection.add("West");
            int direction = entity.getOffBoardDirection();
            if ( Entity.NONE == direction ) {
                direction = Entity.NORTH;
            }
            choOffBoardDirection.select( direction );
            add(choOffBoardDirection);
            
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(labOffBoardDistance, c);
            add(labOffBoardDistance);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(fldOffBoardDistance, c);
            fldOffBoardDistance.setText(Integer.toString(entity.getOffBoardDistance()));
            add(fldOffBoardDistance);
        }

        if ( entity instanceof Protomech )
        {
            // All Protomechs have a callsign.
            StringBuffer callsign = new StringBuffer( "Callsign: " );
            callsign.append( (char) (this.entity.getUnitNumber() +
                                     Settings.unitStartChar) )
                .append( '-' )
                .append( this.entity.getId() );
            labCallsign.setText( callsign.toString() );
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.CENTER;
            gridbag.setConstraints(labCallsign, c);
            add(labCallsign);

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
                add(labUnitNum);

                c.gridwidth = GridBagConstraints.REMAINDER;
                c.anchor = GridBagConstraints.WEST;
                gridbag.setConstraints(choUnitNum, c);
                add(choUnitNum);
                refreshUnitNum(otherUnitEntities);
            }
        }

        // Can't set up munitions on infantry.
        if ( !(entity instanceof Infantry) ) {
            setupMunitions();
            c.anchor = GridBagConstraints.CENTER;
            gridbag.setConstraints(panMunitions, c);
            add(panMunitions);
        }
        
        // Set up rapidfire mg
        if (clientgui.getClient().game.getOptions().booleanOption("maxtech_burst")) {
            setupRapidfireMGs();
            c.anchor = GridBagConstraints.CENTER;
            gridbag.setConstraints(panRapidfireMGs, c);
            add(panRapidfireMGs);
        }

        setupButtons();
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(panButtons, c);
        add(panButtons);
        
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
            disableMunitionEditing();
            disableMGSetting();
            chOffBoard.setEnabled(false);
            choOffBoardDirection.setEnabled(false);
            fldOffBoardDistance.setEnabled(false);
        }
        
        addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) { setVisible(false); }
  });

        pack();
        setResizable(false);
        setLocation(clientgui.frame.getLocation().x + clientgui.frame.getSize().width/2 - getSize().width/2,
                    clientgui.frame.getLocation().y + clientgui.frame.getSize().height/2 - getSize().height/2);
    }
    
    private void setupButtons() {
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panButtons.setLayout(gridbag);
            
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 0, 0);
        c.weightx = 1.0;    c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        c.ipadx = 20;    c.ipady = 5;
        c.gridwidth = 1;
        gridbag.setConstraints(butOkay, c);
        panButtons.add(butOkay);
            
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butCancel, c);
        panButtons.add(butCancel);
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
                boolean bTechMatch = (entity.getTechLevel() == atCheck.getTechType());
                                
                // allow all lvl2 IS units to use level 1 ammo
                // lvl1 IS units don't need to be allowed to use lvl1 ammo,
                // because there is no special lvl1 ammo, therefore it doesn't
                // need to show up in this display.
                if (!bTechMatch && entity.getTechLevel() == TechConstants.T_IS_LEVEL_2 &&
                    atCheck.getTechType() == TechConstants.T_IS_LEVEL_1) {
                    bTechMatch = true;
                }
                
                // if is_eq_limits is unchecked allow l1 guys to use l2 stuff
                if (!clientgui.getClient().game.getOptions().booleanOption("is_eq_limits")
                    && entity.getTechLevel() == TechConstants.T_IS_LEVEL_1
                    && atCheck.getTechType() == TechConstants.T_IS_LEVEL_2) {
                    bTechMatch = true;
                }
                
		        //allow mixed Tech Mechs to use both IS and Clan Ammo
                if (entity.getTechLevel() == TechConstants.T_MIXED_BASE_CLAN_LEVEL_2) {
                   bTechMatch = TechConstants.T_CLAN_LEVEL_2 > atCheck.getTechType();                       
                }
                
                if (entity.getTechLevel() == TechConstants.T_MIXED_BASE_IS_LEVEL_2) {
                   bTechMatch = TechConstants.T_IS_LEVEL_2 >= atCheck.getTechType();
                }                

		        // If clan_ignore_eq_limits is unchecked,
                // do NOT allow Clans to use IS-only ammo.
                // N.B. play bit-shifting games to allow "incendiary"
                //      to be combined to other munition types.
                int muniType = atCheck.getMunitionType();
                muniType &= ~AmmoType.M_INCENDIARY;
                if ( !clientgui.getClient().game.getOptions().booleanOption("clan_ignore_eq_limits")
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
		        
                if ( !clientgui.getClient().game.getOptions().booleanOption("minefields") &&
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
            if (vTypes.size() < 2 && !client.game.getOptions().booleanOption("lobby_ammo_dump")) {
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
        
        protected Label labDump = new Label("Dump this ammobin");
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
            String sDesc = "(" + entity.getLocationAbbr(loc) + ")";
            add(new Label(sDesc));
            add(m_choice);
            if (clientgui.getClient().game.getOptions().booleanOption("lobby_ammo_dump") ) {
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
            String sDesc = "Machine Gun (" + entity.getLocationAbbr(loc) + ")";
            add(new Label(sDesc + " Switch to rapid-fire mode"));
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
      GameOption option;
      
      for (Enumeration i = optionComps.elements(); i.hasMoreElements();) {
        DialogOptionComponent comp = (DialogOptionComponent)i.nextElement();
        
        option = comp.getOption();
        
        entity.getCrew().getOptions().getOption(option.getShortName()).setValue(comp.getValue());
      }
    }
    
    public void resetOptions() {
      GameOption option;
      
      for (Enumeration i = optionComps.elements(); i.hasMoreElements();) {
        DialogOptionComponent comp = (DialogOptionComponent)i.nextElement();
        
        option = comp.getOption();
        option.setValue(false);
                
        entity.getCrew().getOptions().getOption(option.getShortName()).setValue(comp.getValue());
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
        
        for (Enumeration i = options.groups(); i.hasMoreElements();) {
            OptionGroup group = (OptionGroup)i.nextElement();
            
            addGroup(group, gridbag, c);
            
            for (Enumeration j = group.options(); j.hasMoreElements();) {
                GameOption option = (GameOption)j.nextElement();

                addOption(option, gridbag, c, editable);
            }
        }
        
        validate();
    }
    
    private void addGroup(OptionGroup group, GridBagLayout gridbag, GridBagConstraints c) {
        Label groupLabel = new Label(group.getName());
        
        gridbag.setConstraints(groupLabel, c);
        panOptions.add(groupLabel);
    }
    
    private void addOption(GameOption option, GridBagLayout gridbag, GridBagConstraints c, boolean editable) {
        DialogOptionComponent optionComp = new DialogOptionComponent(this, option, editable);

        if (option.getShortName().equals("weapon_specialist")) {
            optionComp.addValue("None");
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
    
    public void showDescFor(GameOption option) {
        texDesc.setText(option.getDesc());
    }

    // TODO : implement me!!!
    public void optionClicked( DialogOptionComponent comp,
                               GameOption option, boolean state ) {
    }

    public boolean isOkay() {
        return okay;
    }

    private void refreshDeployment() {
      choDeployment.removeAll();
      choDeployment.add("Start of game");
      
      if ( entity.getDeployRound() < 1 )
        choDeployment.select(0);
        
      for ( int i = 1; i <= 15; i++ ) {
        choDeployment.add("After round " + i);
        
        if ( entity.getDeployRound() == i )
          choDeployment.select(i);
      }
    }
    
    private void refreshC3() {
        choC3.removeAll();
        int listIndex = 0;
        entityCorrespondance = new int[client.game.getNoOfEntities() + 2];

        if(entity.hasC3i()) {
            choC3.add("Create new network (6 free)");
            if(entity.getC3Master() == null) choC3.select(listIndex);
            entityCorrespondance[listIndex++] = entity.getId();
        }
        else if ( entity.hasC3MM() ) {
            int mNodes = entity.calculateFreeC3MNodes();
            int sNodes = entity.calculateFreeC3Nodes();

            choC3.add("Set as company-level master (" + mNodes + "M / " 
                      + sNodes + "S free)");
            if(entity.C3MasterIs(entity)) choC3.select(listIndex);
            entityCorrespondance[listIndex++] = entity.getId();

            choC3.add("Set as independant master (" + sNodes + " free)");
            if(entity.getC3Master() == null) choC3.select(listIndex);
            entityCorrespondance[listIndex++] = -1;

        }

        else if(entity.hasC3M()) {
            int nodes = entity.calculateFreeC3Nodes();

            choC3.add("Set as company-level master (" + nodes + " free)");
            if(entity.C3MasterIs(entity)) choC3.select(listIndex);
            entityCorrespondance[listIndex++] = entity.getId();

            choC3.add("Set as independant master (" + nodes + " free)");
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
                    choC3.add("Join " + e.getDisplayName() + " [netid " + e.getC3NetId() + ": " + (nodes - 1)  + " free]");
                    choC3.select(listIndex);
                }
                else {
                    choC3.add("Join " + e.getDisplayName() + " (netid " + e.getC3NetId() + ": " + nodes + " free)");
                }
                entityCorrespondance[listIndex++] = e.getId();
            }
            else if ( e.C3MasterIs(e) && e.hasC3MM()) {
                // Company masters with 2 computers can have
                // *both* sub-masters AND slave units.
                choC3.add( "Connect to " + e.getDisplayName() +
                           " (" + nodes + " free)" );
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
                choC3.add("Connect to " + e.getDisplayName() + " [" + (nodes - 1) + " free]");
                choC3.select(listIndex);
                entityCorrespondance[listIndex++] = e.getId();
            }
            else {
                choC3.add("Connect to " + e.getDisplayName() + " (" + nodes + " free)");
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
        choUnitNum.add( "-- Do Not Swap Units --" );
        entityUnitNum.addElement( this.entity );

        // Walk through the other entities.
        while ( others.hasMoreElements() ) {
            // Track the position of the next other entity.
            final Entity other = (Entity) others.nextElement();
            entityUnitNum.addElement( other );

            // Show the other entity's name and callsign.
            StringBuffer callsign = new StringBuffer( other.getDisplayName() );
            callsign.append( " (" )
                .append( (char) (other.getUnitNumber() +
                                 Settings.unitStartChar) )
                .append( '-' )
                .append( other.getId() )
                .append( ')' );
            choUnitNum.add( callsign.toString() );
        }
        choUnitNum.select(0);
    }

    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
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
                new AlertDialog(clientgui.frame, "Number Format Error", "Please enter valid numbers for skill values.").show();
                return;
            }
            
            // keep these reasonable, please
            if (gunnery < 0 || gunnery > 7 || piloting < 0 || piloting > 7) {
                new AlertDialog(clientgui.frame, "Number Format Error", "Please enter values between 0 and 7 for the skill values.").show();
                return;
            }
            if (chOffBoard.getState()){
                try {
                    offBoardDistance = Integer.parseInt(fldOffBoardDistance.getText());
                } catch (NumberFormatException e) {
                    new AlertDialog(clientgui.frame, "Number Format Error", "Please enter valid numbers for skill values.").show();
                    return;
                }
                if (offBoardDistance < 17) {
                    new AlertDialog(clientgui.frame, "Number Format Error", "Offboard units need to be at least one mapsheet (17 hexes) away.").show();
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
                    StringBuffer message = new StringBuffer();
                    message.append( entity.getShortName() )
                        .append( " can't join the C3 network of\n" )
                        .append( chosen.getShortName() )
                        .append( " because that would add " )
                        .append( entC3nodeCount )
                        .append( "\nunits to a network that already has " )
                        .append( choC3nodeCount )
                        .append( ",\nwhich is more than the maximum of " )
                        .append( Entity.MAX_C3_NODES )
                        .append( "." );
                    clientgui.doAlertDialog( "C3 Network Too Big",
                                          message.toString() );
                    refreshC3();
                }
            }
            else if(entity.hasC3i() && choC3.getSelectedIndex() > -1) {
                entity.setC3NetId(client.getEntity(entityCorrespondance[choC3.getSelectedIndex()]));
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
    }
}

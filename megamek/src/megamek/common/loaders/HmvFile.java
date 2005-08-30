/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.common.loaders;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.Enumeration;
import java.util.Hashtable;

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IEntityMovementMode;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.TroopSpace;
import megamek.common.VTOL;

/**
 * Based on the hmpread.c program and the MtfFile object.  This class
 * can not load any Mixed tech or Level 3 vehicles.
 *
 * @author <a href="mailto:mnewcomb@sourceforge.net">Michael Newcomb</a>
 */
public class HmvFile
  implements IMechLoader
{
  private String name;
  private String model;

  private HMVMovementType movementType;
  private int rulesLevel;
  private int year;
  private boolean isOmni = false;
  private HMVTechType techType;

  private int engineRating;
  private HMVEngineType engineType;

  private int cruiseMP;
  private int jumpMP;

  private int heatSinks;
  private HMVArmorType armorType;

  private int roundedInternalStructure;

  private int turretArmor;
  private int frontArmor;
  private int leftArmor;
  private int rightArmor;
  private int rearArmor;

  private Hashtable equipment = new Hashtable();

  private int troopSpace = 0;

  public HmvFile(InputStream is)
    throws EntityLoadingException
  {
    try
    {
      DataInputStream dis = new DataInputStream(is);

      byte[] buffer = new byte[5];
      dis.read(buffer);
      String version = new String(buffer);

      // ??
      dis.skipBytes(2);

      int type = readUnsignedShort(dis);
      movementType = HMVMovementType.getType( type );
      if ( null == movementType ) {
          throw new EntityLoadingException
              ( "Could not locate movement type for " + type + "." );
      }

      // ??
      dis.skipBytes(12);

      buffer = new byte[readUnsignedShort(dis)];
      dis.read(buffer);
      name = new String(buffer);

      buffer = new byte[readUnsignedShort(dis)];
      dis.read(buffer);
      model = new String(buffer);

      rulesLevel = readUnsignedShort(dis);

      year = readUnsignedShort(dis);

      // ??
      dis.skipBytes(32);

      // The "bf2" buffer contains the word "omni" for OmniVehicles.
      int bf2Length = readUnsignedShort(dis);
      byte[] bf2Buffer = new byte[ bf2Length ];
      dis.read( bf2Buffer );
      isOmni = containsOmni( bf2Buffer );

      techType = HMVTechType.getType(readUnsignedShort(dis));

      // ??
      dis.skipBytes(4);

      engineRating = readUnsignedShort(dis);
      engineType = HMVEngineType.getType(readUnsignedShort(dis));

      cruiseMP = readUnsignedShort(dis);
      jumpMP = readUnsignedShort(dis);

      heatSinks = readUnsignedShort(dis);
      armorType = HMVArmorType.getType(readUnsignedShort(dis));

      roundedInternalStructure = readUnsignedShort(dis);

      turretArmor = readUnsignedShort(dis);

      // internal structure again ??
      dis.skipBytes(2);

      frontArmor = readUnsignedShort(dis);

      // internal structure again ??
      dis.skipBytes(2);

      leftArmor = readUnsignedShort(dis);

      // internal structure again ??
      dis.skipBytes(2);

      rightArmor = readUnsignedShort(dis);

      // internal structure again ??
      dis.skipBytes(2);

      rearArmor = readUnsignedShort(dis);
        // ??
        if (isOmni) {
            // Skip 12 bytes for OmniVehicles
            dis.skipBytes(12);

            // Decide whether or not the turret is a fixed weight
            int lockedTurret = readUnsignedShort(dis);

            if (lockedTurret == 2) {
                // Skip something else?...
                dis.skipBytes(12);
            }
        } else {
            // Skip 14 bytes for non-OmniVehicles
            dis.skipBytes(14);
        }

      int weapons = readUnsignedShort(dis);
      for (int i = 1; i <= weapons; i++)
      {
        int weaponCount = readUnsignedShort(dis);
        int weaponType = readUnsignedShort(dis);

        // manufacturer name
        dis.skipBytes(readUnsignedShort(dis));

        HMVWeaponLocation weaponLocation =
          HMVWeaponLocation.getType(readUnsignedShort(dis));

        int weaponAmmo = readUnsignedShort(dis);

        EquipmentType equipmentType = getEquipmentType(weaponType, techType);
        if (equipmentType != null)
        {
            addEquipmentType(equipmentType, weaponCount, weaponLocation);

            if (weaponAmmo > 0)
            {
                AmmoType ammoType = getAmmoType(weaponType, techType);

                if (ammoType != null)
                {
                    // Need to play games for half ton MG ammo.
                    if ( weaponAmmo < ammoType.getShots() ||
                         weaponAmmo % ammoType.getShots() > 0 ) {
                        switch ( ammoType.getAmmoType() ) {
                        case AmmoType.T_MG:
                            if ( ammoType.getTechLevel() ==
                                 TechConstants.T_IS_LEVEL_1 ) {
                                ammoType = (AmmoType) EquipmentType
                                    .get( "ISMG Ammo (100)" );
                            } else {
                                ammoType = (AmmoType) EquipmentType
                                    .get( "CLMG Ammo (100)" );
                            }
                            break;
                        case AmmoType.T_MG_LIGHT:
                            ammoType = (AmmoType) EquipmentType
                                .get( "CLLightMG Ammo (100)" );
                            break;
                        case AmmoType.T_MG_HEAVY:
                            ammoType = (AmmoType) EquipmentType
                                .get( "CLHeavyMG Ammo (50)" );
                            break;
                        default:
                            // Only MG ammo comes in half ton lots.
                            throw new EntityLoadingException
                                ( ammoType.getName() +
                                  " has " + ammoType.getShots() +
                                  " shots per ton, but " + name + " " + model +
                                  " wants " + weaponAmmo + " shots." );
                        }
                    }

                    // Add as many copies of the AmmoType as needed.
                    addEquipmentType(ammoType,
                                     weaponAmmo / ammoType.getShots(),
                                     HMVWeaponLocation.BODY);

                } // End found-ammoType

            } // End have-rounds-of-ammo

        } // End found-equipmentType

        // ??
        dis.skipBytes(4);

      } // Handle the next piece of equipment

      // Read the amount of troop/cargo bays.
      int bayCount = readUnsignedShort(dis);
      for ( int loop = 0; loop < bayCount; loop++ ) {

          // Read the size of this bay.
          dis.skipBytes(2);
          int baySizeCode = readUnsignedShort(dis);

          // manufacturer name
          dis.skipBytes(readUnsignedShort(dis));

          // Add the troopSpace of this bay to our running total.
          switch ( baySizeCode ) {
          case 0x3F80:  // 1 ton
          case 0x4020:  // 1 ton???
              troopSpace += 1;
              break;
          case 0x4040:  // 3 tons
          case 0x4060:  // 3.5 tons
              troopSpace += 3;
              break;
          case 0x4080:  // 4 tons
              troopSpace += 4;
              break;
          case 0x40A0:  // 5 tons
              troopSpace += 5;
              break;
          case 0x40C0:  // 6 tons
              troopSpace += 6;
              break;
          case 0x40F0:  // 7.5 tons
              troopSpace += 7;
              break;
          case 0x4100:  // 8 tons???
              troopSpace += 8;
              break;
          }

      } // Handle the next bay.

      dis.close();
    }
    catch (IOException ex)
    {
        ex.printStackTrace();
        throw new EntityLoadingException("I/O Error reading file");
    }
  }

  private short readUnsignedByte(DataInputStream dis)
    throws IOException
  {
    short b = dis.readByte();
    b += b < 0 ? 256 : 0;
    return b;
  }

  private int readUnsignedShort(DataInputStream dis)
    throws IOException
  {
    int b2 = readUnsignedByte(dis);

    int b1 = readUnsignedByte(dis);
    b1 <<= 8;

    return b1 + b2;
  }

  /**
     * Determine if the buffer contains the "is omni" flag.
     *
     * @param   buffer the array of <code>byte</code>s to be scanned.
     * @return  <code>true</code> if the buffer contains the "is omni" flag.
     */
    private boolean containsOmni( byte[] buffer ) {
        int index;

        // Look for the 4 byte flag.
        for ( index = buffer.length - 4; index >= 0; index-- ) {
            if ( 0x6f == buffer[index]   &&  // 'o'
                 0x6d == buffer[index+1] &&  // 'm'
                 0x6e == buffer[index+2] &&  // 'n'
                 0x69 == buffer[index+3] ) { // 'i'

                // We found it!
                return true;
            }
        }

        // We didn't find the "is omni" flag;
        return false;
    }

    public Entity getEntity() throws EntityLoadingException {
        try  {
            Entity entity = null;
            
            if (movementType == HMVMovementType.TRACKED ||
                    movementType == HMVMovementType.WHEELED ||
                    movementType == HMVMovementType.HOVER ||
                    movementType == HMVMovementType.DISPLACEMENT_HULL ||
                    movementType == HMVMovementType.HYDROFOIL) {
                Tank tank = new Tank();
                entity = tank;
    
                tank.setChassis(name);
                tank.setModel(model);
                tank.setYear(year);
                tank.setOmni(isOmni);
                tank.setEngineType(engineType.getId());
                int techLevel = TechConstants.T_IS_LEVEL_3;
                if (rulesLevel == 1) {
                    techLevel = TechConstants.T_IS_LEVEL_1;
                } else if (rulesLevel == 2) {
                    techLevel = techType == HMVTechType.CLAN ? TechConstants.T_CLAN_LEVEL_2 : TechConstants.T_IS_LEVEL_2;
                } else if (techType == HMVTechType.CLAN) {
                     techLevel = TechConstants.T_CLAN_LEVEL_3;
                }
                    
                tank.setTechLevel(techLevel);
    
                int suspensionFactor = getSuspensionFactor(roundedInternalStructure, movementType);
                tank.setWeight((engineRating + suspensionFactor) / cruiseMP);
    
                tank.setMovementMode(
                    movementType == HMVMovementType.DISPLACEMENT_HULL ? IEntityMovementMode.NAVAL :
                    movementType == HMVMovementType.HYDROFOIL ? IEntityMovementMode.HYDROFOIL :
                    movementType == HMVMovementType.HOVER ? IEntityMovementMode.HOVER :
                    movementType == HMVMovementType.WHEELED ? IEntityMovementMode.WHEELED :
                    IEntityMovementMode.TRACKED);
    
                tank.setOriginalWalkMP(cruiseMP);
                tank.setOriginalJumpMP(jumpMP);
    
                // hmmm...
                tank.setHasNoTurret(turretArmor == 0);
    
                tank.autoSetInternal();
                tank.setArmorType(armorType.getId());

                tank.initializeArmor(frontArmor, Tank.LOC_FRONT);
                tank.initializeArmor(leftArmor, Tank.LOC_LEFT);
                tank.initializeArmor(rightArmor, Tank.LOC_RIGHT);
                tank.initializeArmor(rearArmor, Tank.LOC_REAR);
                if (!tank.hasNoTurret()) {
                    tank.initializeArmor(turretArmor, Tank.LOC_TURRET);
                }

                addEquipment(tank, HMVWeaponLocation.FRONT, Tank.LOC_FRONT);
                addEquipment(tank, HMVWeaponLocation.LEFT, Tank.LOC_LEFT);
                addEquipment(tank, HMVWeaponLocation.RIGHT, Tank.LOC_RIGHT);
                addEquipment(tank, HMVWeaponLocation.REAR, Tank.LOC_REAR);
                if (!tank.hasNoTurret()) {
                    addEquipment(tank, HMVWeaponLocation.TURRET, Tank.LOC_TURRET);
                }

                addEquipment(tank, HMVWeaponLocation.BODY, Tank.LOC_BODY);

                // Do we have any infantry/cargo bays?
                if ( troopSpace > 0 ) {
                    entity.addTransporter( new TroopSpace( troopSpace ) );
                }
            } else if (movementType == HMVMovementType.VTOL) {
                VTOL vtol = new VTOL();
                entity = vtol;
    
                entity.setChassis(name);
                entity.setModel(model);
                entity.setYear(year);
                entity.setOmni( isOmni );
    
                int techLevel = rulesLevel == 1 ? TechConstants.T_IS_LEVEL_1 :
                    techType == HMVTechType.CLAN ? TechConstants.T_CLAN_LEVEL_2 :
                    TechConstants.T_IS_LEVEL_2;
                entity.setTechLevel(techLevel);
    
                int suspensionFactor = getSuspensionFactor(roundedInternalStructure, movementType);
                entity.setWeight((engineRating + suspensionFactor) / cruiseMP);
    
                entity.setMovementMode(IEntityMovementMode.VTOL);
    
                entity.setOriginalWalkMP(cruiseMP);
                entity.setOriginalJumpMP(jumpMP);
    
                // hmmm...
                vtol.setHasNoTurret(turretArmor == 0);
    
                entity.autoSetInternal();
    
                entity.initializeArmor(frontArmor, Tank.LOC_FRONT);
                entity.initializeArmor(leftArmor, Tank.LOC_LEFT);
                entity.initializeArmor(rightArmor, Tank.LOC_RIGHT);
                entity.initializeArmor(rearArmor, Tank.LOC_REAR);
                entity.initializeArmor(turretArmor, VTOL.LOC_ROTOR);

                addEquipment(vtol, HMVWeaponLocation.FRONT, Tank.LOC_FRONT);
                addEquipment(vtol, HMVWeaponLocation.LEFT, Tank.LOC_LEFT);
                addEquipment(vtol, HMVWeaponLocation.RIGHT, Tank.LOC_RIGHT);
                addEquipment(vtol, HMVWeaponLocation.REAR, Tank.LOC_REAR);
                if (!vtol.hasNoTurret()) {
                    addEquipment(vtol, HMVWeaponLocation.TURRET, Tank.LOC_TURRET);
                }

                addEquipment(vtol, HMVWeaponLocation.BODY, Tank.LOC_BODY);

                // Do we have any infantry/cargo bays?
                if ( troopSpace > 0 ) {
                    entity.addTransporter( new TroopSpace( troopSpace ) );
                }
            } else {
                throw new EntityLoadingException
                    ( "Unsupported vehicle movement type:" + movementType );
            }
            return entity;
        }
            catch (Exception e) {
                e.printStackTrace();
                throw new EntityLoadingException(e.getMessage());
        }
    }

  private void addEquipmentType(EquipmentType equipmentType, int weaponCount,
                                HMVWeaponLocation weaponLocation)
  {
    Hashtable equipmentAtLocation = (Hashtable) equipment.get(weaponLocation);
    if (equipmentAtLocation == null)
    {
      equipmentAtLocation = new Hashtable();
      equipment.put(weaponLocation, equipmentAtLocation);
    }
    Integer prevCount = (Integer) equipmentAtLocation.get( equipmentType );
    if ( null != prevCount ) {
        weaponCount += prevCount.intValue();
    }
    equipmentAtLocation.put(equipmentType, new Integer(weaponCount));
  }

  private void addEquipment(Tank tank, HMVWeaponLocation weaponLocation,
                            int location)
    throws Exception
  {
    Hashtable equipmentAtLocation = (Hashtable) equipment.get(weaponLocation);
    if (equipmentAtLocation != null)
    {
      for (Enumeration e = equipmentAtLocation.keys(); e.hasMoreElements();)
      {
        EquipmentType equipmentType = (EquipmentType) e.nextElement();
        Integer count = (Integer) equipmentAtLocation.get(equipmentType);

        for (int i = 0; i < count.intValue(); i++)
        {
          tank.addEquipment(equipmentType, location);
        }
      }
    }
  }

  private static int getSuspensionFactor(int roundedInternalStructure,
                                         HMVMovementType movementType)
  {
    int suspensionFactor = 0;
    if (movementType == HMVMovementType.WHEELED)
    {
      suspensionFactor = 20;
    }
    else if (movementType == HMVMovementType.DISPLACEMENT_HULL ||
             movementType == HMVMovementType.SUBMARINE)
    {
      suspensionFactor = 30;
    }
    else if (movementType == HMVMovementType.VTOL)
    {
      switch (roundedInternalStructure)
      {
        case 1:
          suspensionFactor = 50;
          break;
        case 2:
          suspensionFactor = 95;
          break;
        case 3:
          suspensionFactor = 140;
          break;
      }
    }
    else if (movementType == HMVMovementType.HOVER)
    {
      switch (roundedInternalStructure)
      {
        case 1:
          suspensionFactor = 40;
          break;
        case 2:
          suspensionFactor = 85;
          break;
        case 3:
          suspensionFactor = 130;
          break;
        case 4:
          suspensionFactor = 175;
          break;
        case 5:
          suspensionFactor = 235;
          break;
      }
    }
    else if (movementType == HMVMovementType.HYDROFOIL)
    {
      switch (roundedInternalStructure)
      {
        case 1:
          suspensionFactor = 60;
          break;
        case 2:
          suspensionFactor = 105;
          break;
        case 3:
          suspensionFactor = 150;
          break;
        case 4:
          suspensionFactor = 195;
          break;
        case 5:
          suspensionFactor = 255;
          break;
        case 6:
          suspensionFactor = 300;
          break;
        case 7:
          suspensionFactor = 345;
          break;
        case 8:
          suspensionFactor = 390;
          break;
        case 9:
          suspensionFactor = 435;
          break;
        case 10:
          suspensionFactor = 480;
          break;
      }
    }

    return suspensionFactor;
  }

  private static final Hashtable EQUIPMENT = new Hashtable();
  private static final Hashtable AMMO = new Hashtable();
  static
  {
    // inner sphere equipment
    //
    Hashtable isEquipment = new Hashtable();
    EQUIPMENT.put(HMVTechType.INNER_SPHERE, isEquipment);
    isEquipment.put(new Long(0x0A), "ISDouble Heat Sink");
    isEquipment.put(new Long(0x12), "ISTargeting Computer");
    isEquipment.put(new Long(0x14), "Endo Steel");
    isEquipment.put(new Long(0x15), "Ferro-Fibrous");
    isEquipment.put(new Long(0x17), "ISMASC");
    isEquipment.put(new Long(0x18), "ISArtemisIV");
    isEquipment.put(new Long(0x19), "ISCASE");
    isEquipment.put(new Long(0x33), "ISERLargeLaser");
    isEquipment.put(new Long(0x34), "ISERPPC");
    isEquipment.put(new Long(0x35), "ISFlamer");
    isEquipment.put(new Long(0x39), "ISSmallLaser");
    isEquipment.put(new Long(0x37), "ISLargeLaser");
    isEquipment.put(new Long(0x38), "ISMediumLaser");
    isEquipment.put(new Long(0x3A), "ISPPC");
    isEquipment.put(new Long(0x3B), "ISLargePulseLaser");
    isEquipment.put(new Long(0x3C), "ISMediumPulseLaser");
    isEquipment.put(new Long(0x3D), "ISSmallPulseLaser");
    isEquipment.put(new Long(0x3E), "ISAC2");
    isEquipment.put(new Long(0x3F), "ISAC5");
    isEquipment.put(new Long(0x40), "ISAC10");
    isEquipment.put(new Long(0x41), "ISAC20");
    isEquipment.put(new Long(0x42), "ISAntiMissileSystem");
    isEquipment.put(new Long(0x46), "ISLightGaussRifle");
    isEquipment.put(new Long(0x47), "ISGaussRifle");
    isEquipment.put(new Long(0x4B), "ISLBXAC2");
    isEquipment.put(new Long(0x4C), "ISLBXAC5");
    isEquipment.put(new Long(0x4D), "ISLBXAC10");
    isEquipment.put(new Long(0x4E), "ISLBXAC20");
    isEquipment.put(new Long(0x4F), "ISMachine Gun");
    isEquipment.put(new Long(0x50), "ISLAC2");
    isEquipment.put(new Long(0x51), "ISLAC5");
    isEquipment.put(new Long(0x54), "ISUltraAC2");
    isEquipment.put(new Long(0x55), "ISUltraAC5");
    isEquipment.put(new Long(0x56), "ISUltraAC10");
    isEquipment.put(new Long(0x57), "ISUltraAC20");
    isEquipment.put(new Long(0x5A), "ISERMediumLaser");
    isEquipment.put(new Long(0x5B), "ISERSmallLaser");
    isEquipment.put(new Long(0x5C), "ISAntiPersonnelPod");
    isEquipment.put(new Long(0x60), "ISLRM5");
    isEquipment.put(new Long(0x61), "ISLRM10");
    isEquipment.put(new Long(0x62), "ISLRM15");
    isEquipment.put(new Long(0x63), "ISLRM20");
    isEquipment.put(new Long(0x66), "ISImprovedNarc");
    isEquipment.put(new Long(0x67), "ISSRM2");
    isEquipment.put(new Long(0x68), "ISSRM4");
    isEquipment.put(new Long(0x69), "ISSRM6");
    isEquipment.put(new Long(0x6A), "ISStreakSRM2");
    isEquipment.put(new Long(0x6B), "ISStreakSRM4");
    isEquipment.put(new Long(0x6C), "ISStreakSRM6");
    isEquipment.put(new Long(0x71), "ISArrowIVSystem");
    isEquipment.put(new Long(0x72), "ISAngelECMSuite");
    isEquipment.put(new Long(0x73), "ISBeagleActiveProbe");
    isEquipment.put(new Long(0x74), "ISBloodhoundActiveProbe");
    isEquipment.put(new Long(0x75), "ISC3MasterComputer");
    isEquipment.put(new Long(0x76), "ISC3SlaveUnit");
    isEquipment.put(new Long(0x77), "ISImprovedC3CPU");
    isEquipment.put(new Long(0x78), "ISGuardianECM");
    isEquipment.put(new Long(0x79), "ISNarcBeacon");
    isEquipment.put(new Long(0x7A), "ISTAG");
    isEquipment.put(new Long(0x7B), "ISLRM5 (OS)");
    isEquipment.put(new Long(0x7C), "ISLRM10 (OS)");
    isEquipment.put(new Long(0x7D), "ISLRM15 (OS)");
    isEquipment.put(new Long(0x7E), "ISLRM20 (OS)");
    isEquipment.put(new Long(0x7F), "ISSRM2 (OS)");
    isEquipment.put(new Long(0x80), "ISSRM4 (OS)");
    isEquipment.put(new Long(0x81), "ISSRM6 (OS)");
    isEquipment.put(new Long(0x82), "ISStreakSRM2 (OS)");
    isEquipment.put(new Long(0x83), "ISStreakSRM4 (OS)");
    isEquipment.put(new Long(0x84), "ISStreakSRM6 (OS)");
    isEquipment.put(new Long(0x85), "ISFlamer (Vehicle)");
    isEquipment.put(new Long(0x87), "ISSniperArtillery");
    isEquipment.put(new Long(0x88), "ISThumperArtillery");
    isEquipment.put(new Long(0x89), "ISMRM10");
    isEquipment.put(new Long(0x8A), "ISMRM20");
    isEquipment.put(new Long(0x8B), "ISMRM30");
    isEquipment.put(new Long(0x8C), "ISMRM40");
    isEquipment.put(new Long(0x8E), "ISMRM10 (OS)");
    isEquipment.put(new Long(0x8F), "ISMRM20 (OS)");
    isEquipment.put(new Long(0x90), "ISMRM30 (OS)");
    isEquipment.put(new Long(0x91), "ISMRM40 (OS)");
    isEquipment.put(new Long(0x92), "ISLRTorpedo5");
    isEquipment.put(new Long(0x93), "ISLRTorpedo10");
    isEquipment.put(new Long(0x94), "ISLRTorpedo15");
    isEquipment.put(new Long(0x95), "ISLRTorpedo20");
    isEquipment.put(new Long(0x96), "ISSRTorpedo2");
    isEquipment.put(new Long(0x97), "ISSRTorpedo4");
    isEquipment.put(new Long(0x98), "ISSRTorpedo6");
    isEquipment.put(new Long(0x108), "ISTHBLBXAC2");
    isEquipment.put(new Long(0x109), "ISTHBLBXAC5");
    isEquipment.put(new Long(0x10A), "ISTHBLBXAC20");
    isEquipment.put(new Long(0x10B), "ISUltraAC2 (THB)");
    isEquipment.put(new Long(0x10C), "ISUltraAC10 (THB)");
    isEquipment.put(new Long(0x10D), "ISUltraAC20 (THB)");
    isEquipment.put(new Long(0x11e), "ISTHBBloodhoundActiveProbe");
    isEquipment.put(new Long(0x121), "ISRotaryAC2");
    isEquipment.put(new Long(0x122), "ISRotaryAC5");
    isEquipment.put(new Long(0x123), "ISHeavyGaussRifle");
    isEquipment.put(new Long(0x12B), "ISRocketLauncher10");
    isEquipment.put(new Long(0x12C), "ISRocketLauncher15");
    isEquipment.put(new Long(0x12D), "ISRocketLauncher20");
    isEquipment.put(new Long(0x01ce), "ISAC2 Ammo");
    isEquipment.put(new Long(0x01d0), "ISAC10 Ammo");
    isEquipment.put(new Long(0x01d2), "ISAMS Ammo");
    isEquipment.put(new Long(0x01cf), "ISAC5 Ammo");
    isEquipment.put(new Long(0x01d1), "ISAC20 Ammo");
    isEquipment.put(new Long(0x01d6), "ISLightGauss Ammo");
    isEquipment.put(new Long(0x01d7), "ISGauss Ammo");
    isEquipment.put(new Long(0x01db), "ISLBXAC2 Ammo");
    isEquipment.put(new Long(0x01dc), "ISLBXAC5 Ammo");
    isEquipment.put(new Long(0x01dd), "ISLBXAC10 Ammo");
    isEquipment.put(new Long(0x01de), "ISLBXAC20 Ammo");
    isEquipment.put(new Long(0x01df), "ISMG Ammo (200)");
    isEquipment.put(new Long(0x01e0), "ISLAC2 Ammo");
    isEquipment.put(new Long(0x01e1), "ISLAC5 Ammo");
    isEquipment.put(new Long(0x01e4), "ISUltraAC2 Ammo");
    isEquipment.put(new Long(0x01e5), "ISUltraAC5 Ammo");
    isEquipment.put(new Long(0x01e6), "ISUltraAC10 Ammo");
    isEquipment.put(new Long(0x01e7), "ISUltraAC20 Ammo");
    isEquipment.put(new Long(0x01f0), "ISLRM5 Ammo");
    isEquipment.put(new Long(0x01f1), "ISLRM10 Ammo");
    isEquipment.put(new Long(0x01f2), "ISLRM15 Ammo");
    isEquipment.put(new Long(0x01f3), "ISLRM20 Ammo");
    isEquipment.put(new Long(0x01f6), "ISiNarc Pods");
    isEquipment.put(new Long(0x01fa), "ISStreakSRM2 Ammo");
    isEquipment.put(new Long(0x01fb), "ISStreakSRM4 Ammo");
    isEquipment.put(new Long(0x01fc), "ISStreakSRM6 Ammo");
    isEquipment.put(new Long(0x0201), "ISArrowIV Ammo");
    isEquipment.put(new Long(0x0209), "ISNarc Pods");
    isEquipment.put(new Long(0x0215), "ISVehicleFlamer Ammo");
    isEquipment.put(new Long(0x0217), "ISSniper Ammo");
    isEquipment.put(new Long(0x0218), "ISThumper Ammo");
    isEquipment.put(new Long(0x0219), "ISMRM10 Ammo");
    isEquipment.put(new Long(0x021a), "ISMRM20 Ammo");
    isEquipment.put(new Long(0x021b), "ISMRM30 Ammo");
    isEquipment.put(new Long(0x021c), "ISMRM40 Ammo");
    isEquipment.put(new Long(0x02b1), "ISRotaryAC2 Ammo");
    isEquipment.put(new Long(0x02b2), "ISRotaryAC5 Ammo");
    isEquipment.put(new Long(0x02b3), "ISHeavyGauss Ammo");
    isEquipment.put(new Long(0x01f7), "ISSRM2 Ammo");
    isEquipment.put(new Long(0x01f8), "ISSRM4 Ammo");
    isEquipment.put(new Long(0x01f9), "ISSRM6 Ammo");
    isEquipment.put(new Long(0x0224), "ISLRTorpedo15 Ammo");
    isEquipment.put(new Long(0x0225), "ISLRTorpedo20 Ammo");
    isEquipment.put(new Long(0x0222), "ISLRTorpedo5 Ammo");
    isEquipment.put(new Long(0x0223), "ISLRTorpedo10 Ammo");
    isEquipment.put(new Long(0x0227), "ISSRTorpedo4 Ammo");
    isEquipment.put(new Long(0x0226), "ISSRTorpedo2 Ammo");
    isEquipment.put(new Long(0x0228), "ISSRTorpedo6 Ammo");
    isEquipment.put(new Long(0x298), "ISLBXAC2 Ammo (THB)");
    isEquipment.put(new Long(0x299), "ISLBXAC5 Ammo (THB)");
    isEquipment.put(new Long(0x29A), "ISLBXAC20 Ammo (THB)");
    isEquipment.put(new Long(0x29B), "IS Ultra AC/2 Ammo (THB)");
    isEquipment.put(new Long(0x29C), "IS Ultra AC/10 Ammo (THB)");
    isEquipment.put(new Long(0x29D), "IS Ultra AC/20 Ammo (THB)");

    Hashtable isAmmo = new Hashtable();
    AMMO.put(HMVTechType.INNER_SPHERE, isAmmo);
    isAmmo.put(new Long(0x60), "ISLRM5 Ammo");
    isAmmo.put(new Long(0x61), "ISLRM10 Ammo");
    isAmmo.put(new Long(0x62), "ISLRM15 Ammo");
    isAmmo.put(new Long(0x63), "ISLRM20 Ammo");
    isAmmo.put(new Long(0x3E), "ISAC2 Ammo");
    isAmmo.put(new Long(0x3F), "ISAC5 Ammo");
    isAmmo.put(new Long(0x40), "ISAC10 Ammo");
    isAmmo.put(new Long(0x41), "ISAC20 Ammo");
    isAmmo.put(new Long(0x42), "ISAMS Ammo");
    isAmmo.put(new Long(0x46), "ISLightGauss Ammo");
    isAmmo.put(new Long(0x47), "ISGauss Ammo");
    isAmmo.put(new Long(0x123), "ISHeavyGauss Ammo");
    isAmmo.put(new Long(0x4B), "ISLBXAC2 Ammo");
    isAmmo.put(new Long(0x4C), "ISLBXAC5 Ammo");
    isAmmo.put(new Long(0x4D), "ISLBXAC10 Ammo");
    isAmmo.put(new Long(0x4E), "ISLBXAC20 Ammo");
    isAmmo.put(new Long(0x4F), "ISMG Ammo (200)");
    isAmmo.put(new Long(0x54), "ISUltraAC2 Ammo");
    isAmmo.put(new Long(0x55), "ISUltraAC5 Ammo");
    isAmmo.put(new Long(0x56), "ISUltraAC10 Ammo");
    isAmmo.put(new Long(0x57), "ISUltraAC20 Ammo");
    isAmmo.put(new Long(0x66), "ISiNarc Pods");
    isAmmo.put(new Long(0x6A), "ISStreakSRM2 Ammo");
    isAmmo.put(new Long(0x6B), "ISStreakSRM4 Ammo");
    isAmmo.put(new Long(0x6C), "ISStreakSRM6 Ammo");
    isAmmo.put(new Long(0x71), "ISArrowIV Ammo");
    isAmmo.put(new Long(0x79), "ISNarc Pods");
    isAmmo.put(new Long(0x35), "ISVehicleFlamer Ammo");
    isAmmo.put(new Long(0x87), "ISSniper Ammo");
    isAmmo.put(new Long(0x88), "ISThumper Ammo");
    isAmmo.put(new Long(0x89), "ISMRM10 Ammo");
    isAmmo.put(new Long(0x8A), "ISMRM20 Ammo");
    isAmmo.put(new Long(0x8B), "ISMRM30 Ammo");
    isAmmo.put(new Long(0x8C), "ISMRM40 Ammo");
    isAmmo.put(new Long(0x121), "ISRotaryAC2 Ammo");
    isAmmo.put(new Long(0x122), "ISRotaryAC5 Ammo");
    isAmmo.put(new Long(0x67), "ISSRM2 Ammo");
    isAmmo.put(new Long(0x68), "ISSRM4 Ammo");
    isAmmo.put(new Long(0x69), "ISSRM6 Ammo");
    isAmmo.put(new Long(0x92), "ISLRTorpedo5 Ammo");
    isAmmo.put(new Long(0x93), "ISLRTorpedo10 Ammo");
    isAmmo.put(new Long(0x94), "ISLRTorpedo15 Ammo");
    isAmmo.put(new Long(0x95), "ISLRTorpedo20 Ammo");
    isAmmo.put(new Long(0x96), "ISSRTorpedo4 Ammo");
    isAmmo.put(new Long(0x97), "ISSRTorpedo2 Ammo");
    isAmmo.put(new Long(0x98), "ISSRTorpedo6 Ammo");

    // clan criticals
    //
    Hashtable clanEquipment = new Hashtable();
    EQUIPMENT.put(HMVTechType.CLAN, clanEquipment);
    clanEquipment.put(new Long(0x0A), "CLDouble Heat Sink");
    clanEquipment.put(new Long(0x12), "CLTargeting Computer");
    clanEquipment.put(new Long(0x14), "Endo Steel");
    clanEquipment.put(new Long(0x15), "Ferro-Fibrous");
    clanEquipment.put(new Long(0x17), "CLMASC");
    clanEquipment.put(new Long(0x18), "CLArtemisIV");
    clanEquipment.put(new Long(0x33), "CLERLargeLaser");
    clanEquipment.put(new Long(0x34), "CLERMediumLaser");
    clanEquipment.put(new Long(0x35), "CLERSmallLaser");
    clanEquipment.put(new Long(0x36), "CLERPPC");
    clanEquipment.put(new Long(0x39), "CLSmallLaser");
    clanEquipment.put(new Long(0x37), "CLFlamer");
    clanEquipment.put(new Long(0x38), "CLMediumLaser");
    clanEquipment.put(new Long(0x3A), "CLPPC");
    clanEquipment.put(new Long(0x3C), "CLLargePulseLaser");
    clanEquipment.put(new Long(0x3D), "CLMediumPulseLaser");
    clanEquipment.put(new Long(0x3E), "CLSmallPulseLaser");
    clanEquipment.put(new Long(0x3F), "CLAngelECMSuite");
    clanEquipment.put(new Long(0x40), "CLAntiMissileSystem");
    clanEquipment.put(new Long(0x41), "CLGaussRifle");
    clanEquipment.put(new Long(0x42), "CLLBXAC2");
    clanEquipment.put(new Long(0x43), "CLLBXAC5");
    clanEquipment.put(new Long(0x44), "CLLBXAC10");
    clanEquipment.put(new Long(0x45), "CLLBXAC20");
    clanEquipment.put(new Long(0x46), "CLMG");
    clanEquipment.put(new Long(0x47), "CLUltraAC2");
    clanEquipment.put(new Long(0x48), "CLUltraAC5");
    clanEquipment.put(new Long(0x49), "CLUltraAC10");
    clanEquipment.put(new Long(0x4A), "CLUltraAC20");
    clanEquipment.put(new Long(0x4B), "CLLRM5");
    clanEquipment.put(new Long(0x4C), "CLLRM10");
    clanEquipment.put(new Long(0x4D), "CLLRM15");
    clanEquipment.put(new Long(0x4E), "CLLRM20");
    clanEquipment.put(new Long(0x4F), "CLSRM2");
    clanEquipment.put(new Long(0x50), "CLSRM4");
    clanEquipment.put(new Long(0x51), "CLSRM6");
    clanEquipment.put(new Long(0x52), "CLStreakSRM2");
    clanEquipment.put(new Long(0x53), "CLStreakSRM4");
    clanEquipment.put(new Long(0x54), "CLStreakSRM6");
    clanEquipment.put(new Long(0x55), "CLArrowIVSystem");
    clanEquipment.put(new Long(0x56), "CLAntiPersonnelPod");
    clanEquipment.put(new Long(0x57), "CLActiveProbe");
    clanEquipment.put(new Long(0x58), "CLECMSuite");
    clanEquipment.put(new Long(0x59), "CLNarcBeacon");
    clanEquipment.put(new Long(0x5A), "CLTAG");
    clanEquipment.put(new Long(0x5B), "CLERMicroLaser");
    clanEquipment.put(new Long(0x5C), "CLLRM5 (OS)");
    clanEquipment.put(new Long(0x5D), "CLLRM10 (OS)");
    clanEquipment.put(new Long(0x5E), "CLLRM15 (OS)");
    clanEquipment.put(new Long(0x5F), "CLLRM20 (OS)");
    clanEquipment.put(new Long(0x60), "CLSRM2 (OS)");
    clanEquipment.put(new Long(0x61), "CLSRM4 (OS)");
    clanEquipment.put(new Long(0x62), "CLSRM6 (OS)");
    clanEquipment.put(new Long(0x63), "CLStreakSRM2 (OS)");
    clanEquipment.put(new Long(0x64), "CLStreakSRM4 (OS)");
    clanEquipment.put(new Long(0x65), "CLStreakSRM6 (OS)");
    clanEquipment.put(new Long(0x66), "CLVehicleFlamer");
    clanEquipment.put(new Long(0x67), "CLSRM2");
    clanEquipment.put(new Long(0x68), "CLSniperArtillery");
    clanEquipment.put(new Long(0x69), "CLThumperArtillery");
    clanEquipment.put(new Long(0x6A), "CLLRTorpedo5");
    clanEquipment.put(new Long(0x6B), "CLLRTorpedo10");
    clanEquipment.put(new Long(0x6C), "CLLRTorpedo15");
    clanEquipment.put(new Long(0x6D), "CLLRTorpedo20");
    clanEquipment.put(new Long(0x6E), "CLSRTorpedo2");
    clanEquipment.put(new Long(0x6F), "CLSRTorpedo4");
    clanEquipment.put(new Long(0x70), "CLSRTorpedo6");
    clanEquipment.put(new Long(0x7B), "CLLRM5 (OS)");
    clanEquipment.put(new Long(0x7C), "CLLRM10 (OS)");
    clanEquipment.put(new Long(0x7D), "CLLRM15 (OS)");
    clanEquipment.put(new Long(0x7E), "CLLRM20 (OS)");
    clanEquipment.put(new Long(0x7F), "CLSRM2 (OS)");
    clanEquipment.put(new Long(0x80), "CLHeavyLargeLaser");
    clanEquipment.put(new Long(0x81), "CLHeavyMediumLaser");
    clanEquipment.put(new Long(0x82), "CLHeavySmallLaser");
    clanEquipment.put(new Long(0x85), "CLFlamer (Vehicle)"); //?
    clanEquipment.put(new Long(0x92), "CLLRTorpedo5");
    clanEquipment.put(new Long(0x93), "CLLRTorpedo10");
    clanEquipment.put(new Long(0x94), "CLLRTorpedo15");
    clanEquipment.put(new Long(0x95), "CLLRTorpedo20");
    clanEquipment.put(new Long(0x96), "CLSRTorpedo2");
    clanEquipment.put(new Long(0x97), "CLSRTorpedo4");
    clanEquipment.put(new Long(0x98), "CLSRTorpedo6");
    clanEquipment.put(new Long(0xA8), "CLMicroPulseLaser");
    clanEquipment.put(new Long(0xAD), "CLLightMG");
    clanEquipment.put(new Long(0xAE), "CLHeavyMG");
    clanEquipment.put(new Long(0xAF), "CLLightActiveProbe");
    clanEquipment.put(new Long(0xB4), "CLLightTAG");
    clanEquipment.put(new Long(0xFC), "CLATM3");
    clanEquipment.put(new Long(0xFD), "CLATM6");
    clanEquipment.put(new Long(0xFE), "CLATM9");
    clanEquipment.put(new Long(0xFF), "CLATM12");
    clanEquipment.put(new Long(0x01f0), "CLLRM5 Ammo");
    clanEquipment.put(new Long(0x01f1), "CLLRM10 Ammo");
    clanEquipment.put(new Long(0x01f2), "CLLRM15 Ammo");
    clanEquipment.put(new Long(0x01f3), "CLLRM20 Ammo");
    clanEquipment.put(new Long(0x01ce), "CLAC2 Ammo");
    clanEquipment.put(new Long(0x01d0), "CLAMS Ammo");
    clanEquipment.put(new Long(0x01cf), "CLAC5 Ammo");
    clanEquipment.put(new Long(0x01d1), "CLGauss Ammo");
    clanEquipment.put(new Long(0x01d2), "CLLBXAC2 Ammo");
    clanEquipment.put(new Long(0x01d3), "CLLBXAC5 Ammo");
    clanEquipment.put(new Long(0x01d4), "CLLBXAC10 Ammo");
    clanEquipment.put(new Long(0x01d5), "CLLBXAC20 Ammo");
    clanEquipment.put(new Long(0x01d6), "CLMG Ammo (200)");
    clanEquipment.put(new Long(0x01d7), "CLUltraAC2 Ammo");
    clanEquipment.put(new Long(0x01d8), "CLUltraAC5 Ammo");
    clanEquipment.put(new Long(0x01d9), "CLUltraAC10 Ammo");
    clanEquipment.put(new Long(0x01da), "CLUltraAC20 Ammo");
    clanEquipment.put(new Long(0x01db), "CLLRM5 Ammo");
    clanEquipment.put(new Long(0x01dc), "CLLRM10 Ammo");
    clanEquipment.put(new Long(0x01dd), "CLLRM15 Ammo");
    clanEquipment.put(new Long(0x01de), "CLLRM20 Ammo");
    clanEquipment.put(new Long(0x01df), "CLSRM2 Ammo");
    clanEquipment.put(new Long(0x01e0), "CLSRM4 Ammo");
    clanEquipment.put(new Long(0x01e1), "CLSRM6 Ammo");
    clanEquipment.put(new Long(0x01e2), "CLStreakSRM2 Ammo");
    clanEquipment.put(new Long(0x01e3), "CLStreakSRM4 Ammo");
    clanEquipment.put(new Long(0x01e4), "CLStreakSRM6 Ammo");
    clanEquipment.put(new Long(0x01e5), "CLArrowIV Ammo");
    clanEquipment.put(new Long(0x01e9), "CLNarc Pods");
    clanEquipment.put(new Long(0x0215), "CLFlamer Ammo");
    clanEquipment.put(new Long(0x023d), "CLLightMG Ammo (200)");
    clanEquipment.put(new Long(0x023e), "CLHeavyMG Ammo (100)");
    clanEquipment.put(new Long(0x01f6), "CLFlamer (Vehicle) Ammo");
    clanEquipment.put(new Long(0x01f7), "CLSRM2 Ammo");
    clanEquipment.put(new Long(0x01f8), "CLSniper Ammo");
    clanEquipment.put(new Long(0x01f9), "CLThumper Ammo");
    clanEquipment.put(new Long(0x01fa), "CLLRTorpedo5 Ammo");
    clanEquipment.put(new Long(0x01fb), "CLLRTorpedo10 Ammo");
    clanEquipment.put(new Long(0x01fc), "CLLRTorpedo15 Ammo");
    clanEquipment.put(new Long(0x01fd), "CLLRTorpedo20 Ammo");
    clanEquipment.put(new Long(0x01fe), "CLSRTorpedo2 Ammo");
    clanEquipment.put(new Long(0x01ff), "CLSRTorpedo4 Ammo");
    clanEquipment.put(new Long(0x0200), "CLSRTorpedo6 Ammo");
    clanEquipment.put(new Long(0x0224), "CLLRTorpedo15 Ammo");
    clanEquipment.put(new Long(0x0225), "CLLRTorpedo20 Ammo");
    clanEquipment.put(new Long(0x0222), "CLLRTorpedo5 Ammo");
    clanEquipment.put(new Long(0x0223), "CLLRTorpedo10 Ammo");
    clanEquipment.put(new Long(0x0227), "CLSRTorpedo4 Ammo");
    clanEquipment.put(new Long(0x0226), "CLSRTorpedo2 Ammo");
    clanEquipment.put(new Long(0x0228), "CLSRTorpedo6 Ammo");
    clanEquipment.put(new Long(0x028c), "CLATM3 Ammo");
    clanEquipment.put(new Long(0x028d), "CLATM6 Ammo");
    clanEquipment.put(new Long(0x028e), "CLATM9 Ammo");
    clanEquipment.put(new Long(0x028f), "CLATM12 Ammo");

    Hashtable clAmmo = new Hashtable();
    AMMO.put(HMVTechType.CLAN, clAmmo);
    clAmmo.put(new Long(0x40), "CLAMS Ammo");
    clAmmo.put(new Long(0x41), "CLGauss Ammo");
    clAmmo.put(new Long(0x42), "CLLBXAC2 Ammo Ammo");
    clAmmo.put(new Long(0x43), "CLLBXAC5 Ammo");
    clAmmo.put(new Long(0x44), "CLLBXAC10 Ammo");
    clAmmo.put(new Long(0x45), "CLLBXAC20 Ammo");
    clAmmo.put(new Long(0x46), "CLMG Ammo (200)");
    clAmmo.put(new Long(0x47), "CLUltraAC2 Ammo");
    clAmmo.put(new Long(0x48), "CLUltraAC5 Ammo");
    clAmmo.put(new Long(0x49), "CLUltraAC10 Ammo");
    clAmmo.put(new Long(0x4A), "CLUltraAC20 Ammo");
    clAmmo.put(new Long(0x4B), "CLLRM5 Ammo");
    clAmmo.put(new Long(0x4C), "CLLRM10 Ammo");
    clAmmo.put(new Long(0x4D), "CLLRM15 Ammo");
    clAmmo.put(new Long(0x4E), "CLLRM20 Ammo");
    clAmmo.put(new Long(0x4F), "CLSRM2 Ammo");
    clAmmo.put(new Long(0x50), "CLSRM4 Ammo");
    clAmmo.put(new Long(0x51), "CLSRM6 Ammo");
    clAmmo.put(new Long(0x52), "CLStreakSRM2 Ammo");
    clAmmo.put(new Long(0x53), "CLStreakSRM4 Ammo");
    clAmmo.put(new Long(0x54), "CLStreakSRM6 Ammo");
    clAmmo.put(new Long(0x55), "CLArrowIVSystem Ammo");
    clAmmo.put(new Long(0x66), "CLVehicleFlamer Ammo");
    clAmmo.put(new Long(0x67), "CLSRM2 Ammo");
    clAmmo.put(new Long(0x68), "CLSniperArtillery Ammo");
    clAmmo.put(new Long(0x69), "CLThumperArtillery Ammo");
    clAmmo.put(new Long(0x6A), "CLTorpedoLRM5 Ammo");
    clAmmo.put(new Long(0x6B), "CLTorpedoLRM10 Ammo");
    clAmmo.put(new Long(0x6C), "CLTorpedoLRM15 Ammo");
    clAmmo.put(new Long(0x6D), "CLTorpedoLRM20 Ammo");
    clAmmo.put(new Long(0x6E), "CLTorpedoSRM2 Ammo");
    clAmmo.put(new Long(0x6F), "CLTorpedoSRM4 Ammo");
    clAmmo.put(new Long(0x70), "CLTorpedoSRM6 Ammo");
    clAmmo.put(new Long(0x85), "CLVehicleFlamer Ammo"); //?
    clAmmo.put(new Long(0x92), "CLTorpedoLRM5 Ammo");
    clAmmo.put(new Long(0x93), "CLTorpedoLRM10 Ammo");
    clAmmo.put(new Long(0x94), "CLTorpedoLRM15 Ammo");
    clAmmo.put(new Long(0x95), "CLTorpedoLRM20 Ammo");
    clAmmo.put(new Long(0x96), "CLTorpedoSRM2 Ammo");
    clAmmo.put(new Long(0x97), "CLTorpedoSRM4 Ammo");
    clAmmo.put(new Long(0x98), "CLTorpedoSRM6 Ammo");
    clAmmo.put(new Long(0xAD), "CLLightMG Ammo (200)");
    clAmmo.put(new Long(0xAE), "CLHeavyMG Ammo (100)");
    clAmmo.put(new Long(0xFC), "CLATM3 Ammo");
    clAmmo.put(new Long(0xFD), "CLATM6 Ammo");
    clAmmo.put(new Long(0xFE), "CLATM9 Ammo");
    clAmmo.put(new Long(0xFF), "CLATM12 Ammo");
  }

  private String getEquipmentName(long equipment, HMVTechType techType)
  {
    return getEquipmentName(new Long(equipment), techType);
  }

  private String getEquipmentName(Long equipment, HMVTechType techType)
  {
    if (equipment.longValue() > Short.MAX_VALUE)
    {
      equipment = new Long(equipment.longValue() & 0xFFFF);
    }
    final long value = equipment.longValue();

    String equipName = (String) EQUIPMENT.get(equipment);
    if (equipName == null)
    {
      Hashtable techEquipment = (Hashtable) EQUIPMENT.get(techType);
      if (techEquipment != null)
      {
        equipName = (String) techEquipment.get(equipment);
      }
    }

    // Report unexpected parsing failures.
    if (equipName == null &&
        value != 0  &&     // 0x00 Empty
        value != 7  &&     // 0x07 Lower Leg Actuator (on a quad)
        value != 8  &&     // 0x08 Foot Actuator (on a quad)
        value != 15)
    {    // 0x0F Fusion Engine
      System.out.print("unknown critical: 0x");
      System.out.print(Integer.toHexString(equipment.intValue())
                              .toUpperCase());
      System.out.print( " (" );
      System.out.print( techType );
      System.out.println( ")" );
    }

    return equipName;
  }

  private EquipmentType getEquipmentType(long equipment, HMVTechType techType)
  {
    EquipmentType equipmentType = null;

    String equipmentName = getEquipmentName(equipment, techType);
    if (equipmentName != null)
    {
      equipmentType = EquipmentType.get(equipmentName);
    }

    return equipmentType;
  }

  private String getAmmoName(long ammo, HMVTechType techType)
  {
    return getAmmoName(new Long(ammo), techType);
  }

  private String getAmmoName(Long ammo, HMVTechType techType)
  {
    if (ammo.longValue() > Short.MAX_VALUE)
    {
      ammo= new Long(ammo.longValue() & 0xFFFF);
    }
    final long value = ammo.longValue();

    String ammoName = (String) AMMO.get(equipment);
    if (ammoName == null)
    {
      Hashtable techAmmo = (Hashtable) AMMO.get(techType);
      if (techAmmo != null)
      {
        ammoName = (String) techAmmo.get(ammo);
      }
    }

    // Report unexpected parsing failures.
    if (ammoName == null &&
        value != 0)
    {
      System.out.print("unknown critical: 0x");
      System.out.print(Integer.toHexString(ammo.intValue())
                              .toUpperCase());
      System.out.print( " (" );
      System.out.print( techType );
      System.out.println( ")" );
    }

    return ammoName;
  }

  private AmmoType getAmmoType(long ammo, HMVTechType techType)
  {
    AmmoType ammoType = null;

    String ammoName = getAmmoName(ammo, techType);
    if (ammoName != null)
    {
      ammoType = (AmmoType) EquipmentType.get(ammoName);
    }

    return ammoType;
  }
    /*
  public static void main(String[] args)
    throws Exception
  {
    for (int i = 0; i < args.length; i++)
    {
      HmvFile hmvFile = new HmvFile(new FileInputStream(args[i]));
      System.out.println(new megamek.client.ui.AWT.MechView(hmvFile.getEntity()).getMechReadout());
    }
  }
    */
}

abstract class HMVType
{
    private String name;
    private int id;

    protected HMVType(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String toString() {
        return name;
    }

    public boolean equals( Object other ) {

        // Assume the other object doesn't equal this one.
        boolean result = false;

        // References to the same object are equal.
        if ( this == other ) {
            result = true;
        }

        // If the other object is an instance of
        // this object's class, then recast it.
        else if ( this.getClass().isInstance(other) ) {
            HMVType cast = (HMVType) other;

            // The two objects match if their names and IDs match.
            if ( this.name.equals(cast.name) &&
                 this.id == cast.id ) {
                result = true;
            }
        }

        // Return the result
        return result;
    }

    public int getId() {
        return id;
    }
}

class HMVEngineType
  extends HMVType
{
  public static final Hashtable types = new Hashtable();

  public static final HMVEngineType ICE = new HMVEngineType("I.C.E", EquipmentType.T_ENGINE_ICE);
  public static final HMVEngineType FUSION = new HMVEngineType("Fusion", EquipmentType.T_ENGINE_FUSION);
  public static final HMVEngineType XLFUSION = new HMVEngineType("XL Fusion", EquipmentType.T_ENGINE_XL);
  public static final HMVEngineType XXLFUSION = new HMVEngineType("XXL Fusion", EquipmentType.T_ENGINE_XXL);
  public static final HMVEngineType LIGHTFUSION = new HMVEngineType("Light Fusion", EquipmentType.T_ENGINE_LIGHT);

  private HMVEngineType(String name, int id)
  {
    super(name, id);
    types.put(new Integer(id), this);
  }

  public static HMVEngineType getType(int i)
  {
    return (HMVEngineType) types.get(new Integer(i));
  }
}

class HMVArmorType
  extends HMVType
{
  public static final Hashtable types = new Hashtable();

  public static final HMVArmorType STANDARD = new HMVArmorType(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_STANDARD), EquipmentType.T_ARMOR_STANDARD);
  public static final HMVArmorType FERRO = new HMVArmorType(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS), EquipmentType.T_ARMOR_FERRO_FIBROUS);
//  public static final HMVArmorType COMPACT = new HMVArmorType("Compact", 2);
//  public static final HMVArmorType LASER = new HMVArmorType("Laser", 3);

  private HMVArmorType(String name, int id)
  {
    super(name, id);
    types.put(new Integer(id), this);
  }

  public static HMVArmorType getType(int i)
  {
    return (HMVArmorType) types.get(new Integer(i));
  }
}

class HMVTechType
  extends HMVType
{
  public static final Hashtable types = new Hashtable();

  public static final HMVTechType INNER_SPHERE = new HMVTechType("Inner Sphere", 0);
  public static final HMVTechType CLAN = new HMVTechType("Clan", 1);
  public static final HMVTechType MIXED = new HMVTechType("Mixed", 2);

  private HMVTechType(String name, int id)
  {
    super(name, id);
    types.put(new Integer(id), this);
  }

  public static HMVTechType getType(int i)
  {
    return (HMVTechType) types.get(new Integer(i));
  }
}

class HMVMovementType
  extends HMVType
{
  public static final Hashtable types = new Hashtable();

  public static final HMVMovementType TRACKED =
    new HMVMovementType("Tracked", 8);
  public static final HMVMovementType WHEELED =
    new HMVMovementType("Wheeled", 16);
  public static final HMVMovementType HOVER =
    new HMVMovementType("Hover", 32);
  public static final HMVMovementType VTOL =
    new HMVMovementType("V.T.O.L", 64);
  public static final HMVMovementType HYDROFOIL =
    new HMVMovementType("Hydrofoil", 128);
  public static final HMVMovementType SUBMARINE =
    new HMVMovementType("Submarine", 256);
  public static final HMVMovementType DISPLACEMENT_HULL =
    new HMVMovementType("Displacement Hull", 512);

  private HMVMovementType(String name, int id)
  {
    super(name, id);
    types.put(new Integer(id), this);
  }

  public static HMVMovementType getType(int i)
  {
      // Only pay attention to the movement type bits.
      i &= 1016;
      return (HMVMovementType) types.get(new Integer(i));
  }

}

class HMVWeaponLocation
  extends HMVType
{
  public static final Hashtable types = new Hashtable();

  public static final HMVWeaponLocation TURRET =
    new HMVWeaponLocation("Turret", 0);
  public static final HMVWeaponLocation FRONT =
    new HMVWeaponLocation("Front", 1);
  public static final HMVWeaponLocation LEFT =
    new HMVWeaponLocation("Left", 2);
  public static final HMVWeaponLocation RIGHT =
    new HMVWeaponLocation("Right", 3);
  public static final HMVWeaponLocation REAR =
    new HMVWeaponLocation("Rear", 4);
  public static final HMVWeaponLocation BODY =
    new HMVWeaponLocation("Body", 5);

  private HMVWeaponLocation(String name, int id)
  {
    super(name, id);
    types.put(new Integer(id), this);
  }

  public static HMVWeaponLocation getType(int i)
  {
    return (HMVWeaponLocation) types.get(new Integer(i));
  }
}

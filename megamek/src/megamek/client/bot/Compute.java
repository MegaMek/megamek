/*
 * Compute.java
 *
 * Created on May 12, 2002, 12:02 PM
 */

package megamek.client.bot;

import megamek.*;
import megamek.common.*;
import megamek.common.actions.*;

import java.util.*;

/**
 *
 * @author  Steve Hawkins
 */

// How to account for spotters?
public class Compute extends megamek.common.Compute {
  
  public static double getExpectedDamage(WeaponType weap) {
    if (weap.getDamage() != WeaponType.DAMAGE_MISSILE) {
      // normal weapon
      return  weap.getDamage();
    } else {
      // hard-coded expected missile numbers, based on
      // missile-hit chart
      if (weap.getAmmoType() == AmmoType.T_SRM) {
        switch (weap.getRackSize()) {
          case 2:return 1.41666*2;
          case 4:return 2.63888*2;
          case 6:return 4*2;
        }
      } else {
        switch (weap.getRackSize()) {
          case 5:return 3.16666;
          case 10:return 6.30555;
          case 15:return 9.5;
          case 20:return 12.69444;
        }
      }
    }
    return 0;
  }
  
  public static int getFiringAngle(final Coords dest, int dest_facing, final Coords src) {
    int fa = dest.degree(src) - (dest_facing%6) * 60;
    if (fa < 0) {
      fa += 360;
    } else if (fa >= 360) {
      fa -= 360;
    }
    return fa;
  }
  
  public static int getThreatHitArc(Coords dest, int dest_facing, Coords src) {
    //could throw an illegal argument exception for incorrect facings...
    int fa = Compute.getFiringAngle(dest, dest_facing, src);
    if (fa >= 300 || fa <= 60) return CEntity.SIDE_FRONT;
    if (fa >= 60 && fa <= 120) return CEntity.SIDE_RIGHT;
    if (fa >= 240 && fa <= 300) return CEntity.SIDE_LEFT;
    else return CEntity.SIDE_REAR;
  }
  
  public final static int LEFT = 1;
  public final static int RIGHT = 2;
  
  public static int getAdjustedFacing(int facing, int movement) {
    if (movement == RIGHT) {
      return (facing + 1) % 6;
    } else if(movement == LEFT) {
      return (facing + 5) % 6;
    }
    return facing;
  }
  
  public static boolean isInFiringArc(Coords src, int facing, Coords dest, int arc) {
    // calculate firing angle
    int fa = src.degree(dest) - facing * 60;
    if (fa < 0) {
      fa += 360;
    } else if (fa > 360) {
      fa -= 360;
    }
    // is it in the specifed arc?
    switch(arc) {
      case ARC_FORWARD :
        return fa >= 300 || fa <= 60;
      case Compute.ARC_RIGHTARM :
        return fa >= 60 && fa <= 120;
      case Compute.ARC_LEFTARM :
        return fa >= 240 && fa <= 300;
      case ARC_REAR :
        return fa > 120 && fa < 240;
      case ARC_360 :
        return true;
      default:
        return false;
    }
  }
  
  public static int firingArcToHitArc(int arc) {
    switch (arc) {
      case Compute.ARC_FORWARD:
        return ToHitData.SIDE_FRONT;
        
      case Compute.ARC_LEFTARM:
        return ToHitData.SIDE_LEFT;
        
      case Compute.ARC_RIGHTARM:
        return ToHitData.SIDE_RIGHT;
        
      case Compute.ARC_REAR:
        return ToHitData.SIDE_REAR;
    }
    return 0;
  }
  
  public static void randomize(Object[] A) {
    for (int i = 0; i < A.length; ++i) {
      int j = Compute.random.nextInt(A.length);
      Object temp = A[i];
      A[i] = A[j];
      A[j] = temp;
    }
  } 
}
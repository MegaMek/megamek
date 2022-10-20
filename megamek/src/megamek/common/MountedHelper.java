package megamek.common;

import megamek.common.annotations.Nullable;

/** 
 * Helper methods for Mounted to clean up checks like "is this an Artemis IV".
 * 
 * @author Juliez
 */
public class MountedHelper {
    
    /** Returns true if the given Mounted m is a Coolant Pod. */
    public static boolean isCoolantPod(@Nullable Mounted m) {
        return (m != null) && (m.getType() instanceof AmmoType)
                && (((AmmoType) m.getType()).getAmmoType() == AmmoType.T_COOLANT_POD);
    }
    
    /** Returns true if the given Mounted m is an Artemis IV system (IS/C). */
    public static boolean isArtemisIV(@Nullable Mounted m) {
        return (m != null) && (m.getType() instanceof MiscType) 
                && m.getType().hasFlag(MiscType.F_ARTEMIS);
    }
    
    /** Returns true if the given Mounted m is an Artemis V system. */
    public static boolean isArtemisV(@Nullable Mounted m) {
        return (m != null) && (m.getType() instanceof MiscType) 
                && m.getType().hasFlag(MiscType.F_ARTEMIS_V);
    }
    
    /** Returns true if the given Mounted m is a Proto Artemis system. */
    public static boolean isArtemisProto(@Nullable Mounted m) {
        return (m != null) && (m.getType() instanceof MiscType) 
                && m.getType().hasFlag(MiscType.F_ARTEMIS_PROTO);
    }
    
    /** Returns true if the given Mounted m is any Artemis system (IV, V, Proto, IS/C). */
    public static boolean isAnyArtemis(@Nullable Mounted m) {
        return isArtemisIV(m) || isArtemisV(m) || isArtemisProto(m);
    }
    
}

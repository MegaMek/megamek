package megamek.client.generator;

import java.util.HashSet;

/**
 * Bare data class to pass around and store configuration-influencing info
 */
public class ReconfigurationParameters {

    // Game settings
    public boolean enemiesVisible = true;
    public int allowedYear = 3151;

    // Map settings
    public boolean darkEnvironment = false;

    // Enemy stats
    public long enemyCount = 0;
    public long enemyFliers = 0;
    public long enemyBombers = 0;
    public long enemyInfantry = 0;
    public long enemyBattleArmor = 0;
    public long enemyVehicles = 0;
    public long enemyMeks = 0;
    public long enemyEnergyBoats = 0;
    public long enemyMissileBoats = 0;
    public long enemyAdvancedArmorCount = 0;
    public long enemyReflectiveArmorCount = 0;
    public long enemyFireproofArmorCount = 0;
    public long enemyFastMovers = 0;
    public long enemyOffBoard = 0;
    public long enemyECMCount = 0;
    public HashSet<String> enemyFactions = new HashSet<String>();

    // Friendly stats
    public String friendlyFaction = "";
    public long friendlyCount = 0;
    public long friendlyTAGs = 0;
    public long friendlyNARCs = 0;
    public long friendlyEnergyBoats = 0;
    public long friendlyMissileBoats = 0;
    public long friendlyOffBoard = 0;
    public long friendlyECMCount = 0;
    public long friendlyInfantry = 0;
    public long friendlyBattleArmor = 0;

    // User-selected directives
    // Nukes may be banned for a given team but allowed in general (boo, hiss)
    public boolean nukesBannedForMe = true;

    // Datatype for passing around game parameters the Loadout Generator cares about
    ReconfigurationParameters() {
    }
}

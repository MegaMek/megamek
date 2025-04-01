package megamek.client.bot.common;

import megamek.client.ratgenerator.MissionRole;
import megamek.client.ratgenerator.ModelRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.common.Entity;

import java.util.EnumSet;
import java.util.Set;

public class UnitClassifier {

    private static final RATGenerator ratGenerator = RATGenerator.getInstance();

    private UnitClassifier() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Check if the unit is a transport
     * @param unit The unit to check
     * @return True if the unit is a transport, false otherwise
     */
    public static boolean isTransport(Entity unit) {
        return containsRole(unit, MissionRole.CARGO);
    }

    /**
     * Check if the unit is a VIP
     * @param unit The unit to check
     * @return True if the unit is a VIP, false otherwise
     */
    public static boolean isVIP(Entity unit) {
        return containsRole(unit, MissionRole.COMMAND)
              || unit.isC3CompanyCommander()
              || unit.isC3IndependentMaster()
              || unit.hasC3M()
              || unit.hasC3MM()
              || unit.hasC3i();
    }

    /**
     * Determines the primary role of a unit based on its capabilities.
     * @param unit The unit to analyze
     * @return The determined role
     */
    public static Set<MissionRole> determineUnitMissionRole(Entity unit) {
        return ratGenerator.getModelList().stream()
              .filter(e -> e.getModel().equals(unit.getModel()))
              .map(ModelRecord::getRoles).findFirst().orElse(EnumSet.of(MissionRole.CIVILIAN));
    }

    /**
     * Check if the unit has any of the specified roles
     * @param unit The unit to check
     * @param roles The roles to check for
     * @return True if the unit has any of the specified roles, false otherwise
     */
    public static boolean containsAnyRole(Entity unit, Set<MissionRole> roles) {
        return determineUnitMissionRole(unit).stream().anyMatch(roles::contains);
    }

    /**
     * Check if the unit has any of the specified roles
     * @param unit The unit to check
     * @param role The role to check for
     * @return True if the unit has any of the specified roles, false otherwise
     */
    public static boolean containsRole(Entity unit, MissionRole role) {
        return determineUnitMissionRole(unit).contains(role);
    }
}

package megamek.common.scenario;

public interface ScenarioShortInfo2 {

    String getName();

    String getDescription();

    String getFileName();

    default String getPlanet() {
        return "";
    };
}

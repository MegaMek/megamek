package megamek.common.options;

import megamek.server.victory.BVDestroyedVictoryCondition;
import megamek.server.victory.BVRatioVictoryCondition;
import megamek.server.victory.EnemyCmdrDestroyedVictory;
import megamek.server.victory.KillCountVictory;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class StaticGameOptions implements IGameOptions {

    private static final Map<String, IOption> tableInfo = new HashMap<>();

    private StaticGameOptions(Map<String, IOption> optionsHash) {
        tableInfo.putAll(optionsHash);
    }

    public static final StaticGameOptions EMPTY = empty();

    public static StaticGameOptions empty() {
        return new StaticGameOptions(Map.of());
    }

    public static StaticGameOptions create(IGameOptions gameOptions) {
        if (gameOptions instanceof AbstractOptions abstractOptions) {
            return new StaticGameOptions(abstractOptions.getOptionsHash());
        }
        return EMPTY;
    }

    @Override
    public int count() {
        return tableInfo.size();
    }

    @Override
    public int count(String groupKey) {
        return tableInfo.size();
    }

    @Override
    public int intOption(String name) {
        var option = this.getOption(name);

        if (option != null) {
            option.intValue();
        }
        return 0;
    }

    @Override
    public boolean booleanOption(String name) {
        var option = this.getOption(name);

        if (option != null) {
            option.booleanValue();
        }

        return switch (name) {
            case OptionsConstants.VICTORY_USE_BV_DESTROYED,
                 OptionsConstants.VICTORY_USE_BV_RATIO,
                 OptionsConstants.VICTORY_USE_KILL_COUNT,
                 OptionsConstants.VICTORY_COMMANDER_KILLED -> false;
            default -> true;
        };
    }

    @Override
    public IOption getOption(String name) {
        if (tableInfo == null) {
            return null;
        }
        return tableInfo.get(name);
    }

    @Override
    public Enumeration<IOptionGroup> getGroups() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IOptionsInfo getOptionsInfo() {
        throw new UnsupportedOperationException();
    }

}

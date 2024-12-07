package megamek.common.options;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class StaticGameOptions implements IGameOptions {

    private static final Map<String, IOption> tableInfo = new HashMap<>();

    private StaticGameOptions(Map<String, IOption> optionsHash) {
        tableInfo.putAll(optionsHash);
    }

    public static StaticGameOptions empty() {
        return new StaticGameOptions(Map.of());
    }

    public static StaticGameOptions create(IGameOptions gameOptions) {
        return new StaticGameOptions(gameOptions.getOptionsHash());
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
    public boolean booleanOption(String name) {
        return true;
    }

    @Override
    public IOption getOption(String name) {
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

    @Override
    public Map<String, IOption> getOptionsHash() {
        return tableInfo;
    }

}

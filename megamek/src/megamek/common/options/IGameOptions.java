package megamek.common.options;

import megamek.common.annotations.Nullable;

import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;

public interface IGameOptions {

    /**
     * Returns a count of all options in this object.
     * @return Option count.
     */
    int count();

    /**
     * Returns a count of all options in this object with the given group key.
     * @param groupKey the group key to filter on. Null signifies to return all options indiscriminately.
     * @return Option count.
     */
    int count(String groupKey);

    /**
     * Returns the <code>Enumeration</code> of the option groups in thioptions container.
     *
     * @return <code>Enumeration</code> of the <code>IOptionGroup</code>
     */
    Enumeration<IOptionGroup> getGroups();

    IOptionsInfo getOptionsInfo();

    Map<String, IOption> getOptionsHash();

    /**
     * Returns a string of all the quirk "codes" for this entity, using sep as
     * the separator
     * @param separator The separator to insert between codes, in addition to a space
     */
    default String getOptionList(String separator) {
        return getOptionListString(separator, null);
    }

    /**
     * Returns a string of all the quirk "codes" for this entity, using sep as
     * the separator, filtering on a specific group key.
     * @param separator The separator to insert between codes, in addition to a space
     * @param groupKey The group key to use to filter options. Null signifies to return all options indiscriminately.
     */
    default String getOptionListString(String separator, String groupKey) {
        StringBuilder listBuilder = new StringBuilder();

        if (null == separator) {
            separator = "";
        }

        for (Enumeration<IOptionGroup> i = getGroups(); i.hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            if ((groupKey != null) && !group.getKey().equalsIgnoreCase(groupKey)) {
                continue;
            }

            for (Enumeration<IOption> j = group.getOptions(); j
                .hasMoreElements();) {
                IOption option = j.nextElement();
                if (option != null && option.booleanValue()) {
                    if (!listBuilder.isEmpty()) {
                        listBuilder.append(separator);
                    }
                    listBuilder.append(option.getName());
                    if ((option.getType() == IOption.STRING)
                        || (option.getType() == IOption.CHOICE)
                        || (option.getType() == IOption.INTEGER)) {
                        listBuilder.append(" ").append(option.stringValue());
                    }
                }
            }
        }
        return listBuilder.toString();
    }


    /**
     * Returns the option by name or <code>null</code> if there is no such option
     *
     * @param name option name
     * @return the option or <code>null</code> if there is no such option
     */
    @Nullable IOption getOption(String name);

    /**
     * Returns an Optional option by name or <code>Optional empty</code> if there is no such option
     *
     * @param name option name
     * @return the Optional option or <code>empty</code> if there is no such option
     */
    default Optional<IOption> getOptionOpt(String name) {
        return Optional.ofNullable(getOption(name));
    }

    /**
     * Returns the value of the desired option as the <code>boolean</code>
     *
     * @param name option name
     * @return the value of the desired option as the <code>boolean</code>
     */
    default boolean booleanOption(String name) {
        IOption opt = getOption(name);
        if (opt == null) {
            return false;
        } else {
            return opt.booleanValue();
        }
    }

    /**
     * Returns the value of the desired option as the <code>int</code>
     *
     * @param name option name
     * @return the value of the desired option as the <code>int</code>
     */
    default int intOption(String name) {
        return getOption(name).intValue();
    }

    /**
     * Returns the value of the desired option as the <code>float</code>
     *
     * @param name option name
     * @return the value of the desired option as the <code>float</code>
     */
    default float floatOption(String name) {
        return getOption(name).floatValue();
    }

    /**
     * Returns the value of the desired option as the <code>String</code>
     *
     * @param name option name
     * @return the value of the desired option as the <code>String</code>
     */
    default String stringOption(String name) {
        return getOption(name).stringValue();
    }

}

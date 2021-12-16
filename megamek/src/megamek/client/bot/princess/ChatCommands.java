package megamek.client.bot.princess;

/**
 * @author Deric Page (deric.page@nisc.coop) (ext 2335)
 * @version %Id%
 * @since 10/24/2014 9:57 AM
 */
public enum ChatCommands {
    FLEE("fl", "princessName: flee", "Causes princess-controlled units to start fleeing the board, regardless of " +
                                     "damage level or Forced Withdrawal setting."),
    VERBOSE("ve", "princessName: verbose : <error/warning/info/debug>", "Sets princess's verbosity level."),
    BEHAVIOR("be", "princessName: behavior : behaviorName", "Change's princess's behavior to the named behavior."),
    CAUTION("ca", "princessName: caution : <+/->", "Modifies princess's Piloting Caution setting. Each '+' increases " +
                                                   "it by 1 and each '-' decreases it by one."),
    AVOID("av", "princessName: avoid : <+/->", "Modifies princess's Self Preservation setting. Each '+' increases it " +
                                               "by 1 and each '-' decreases it by one."),
    AGGRESSION("ag", "princessName: aggression : <+/->", "Modifies princess's Aggression setting. Each '+' increases " +
                                                         "it by 1 and each '-' decreases it by one."),
    HERDING("he", "princessName: herd : <+/->", "Modifies princess's Herding setting. Each '+' increases it by 1 and " +
                                                "each '-' decreases it by one."),
    BRAVERY("br", "princessName: brave : <+/->", "Modifies princess's Bravery setting. Each '+' increases it by 1 " +
                                                 "and each '-' decreases it by one."),
    TARGET("ta", "princessName: target : hexNumber", "Adds the specified hex to princess's list of Strategic Targets."),
    PRIORITIZE("pr", "princessName: prioritize : unitId", "Adds the specified unit to princess's Priority Targets " +
                                                          "list."),
    SHOW_BEHAVIOR("sh", "princessName: showBehavior", "Princess will state the name of her current behavior."),
    LIST__COMMANDS("li", "princessName: listCommands", "Displays this list of commands."),
    IGNORE_TARGET("ig", "princessName: ignoreTarget: unitId", "Will not fire on the entity with this ID.");

    private final String abbreviation;
    private final String syntax;
    private final String description;

    ChatCommands(String abbreviation, String syntax, String description) {
        this.abbreviation = abbreviation;
        this.syntax = syntax;
        this.description = description;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getSyntax() {
        return syntax;
    }

    public String getDescription() {
        return description;
    }
}

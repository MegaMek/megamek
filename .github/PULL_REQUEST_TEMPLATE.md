<!-- TITLE: include the issue number, and start with "Fix" for a bug fix or "Close" for
     anything else:

         Fix #1234: Cheese has the wrong colour gradient
         Close #1234: Added 6 new cheese gradients

     More than one issue is fine: "Close #111, #5329: Added unit history tracking".
     Release notes are generated automatically from pull request titles, so the title is
     what players end up reading. -->

## What this changes

<!-- The effect someone actually sees, in plain language. One or two sentences is fine. -->

<!-- Keep this line as well as the number in the title. GitHub only closes an issue from a
     keyword in the description - a number in the title alone does not close anything.

     Any of these close an issue when this merges:
         Close / Closes / Closed
         Fix / Fixes / Fixed
         Resolve / Resolves / Resolved

     Closing more than one needs the keyword repeated in front of each number:
         Fixes #1234, fixes #5678          closes both
         Fixes #1234, #5678                closes only #1234

     For an issue in another repository, qualify it: Fixes MegaMek/mekhq#1234 -->
Fixes #

## Testing

<!-- What you ran. Unit tests? Did you play a game and exercise this path?
     Once it has been tested in game, add the "AI ready for Review" label. -->

## What is not proven yet

<!-- What you did not test, or could not. "Nothing" is a valid answer - say so explicitly. -->

---

- [ ] This PR is focused on one issue or RFE. Large refactoring or accessibility work is in its own PR
- [ ] Every file in the diff has a deliberate change (no stray formatting, no unrelated files)
- [ ] Tests added or updated, if this implements a rule or changes game state
- [ ] Javadoc literals use `{@code true}` / `{@code null}` rather than bare or quoted text
- [ ] Dev team only: if AI tools were used, the **AI Assisted Development** label is applied, and I can
      explain and have verified the result

<!-- ABOUT THAT LAST BOX, IF YOU ARE NOT ON THE DEV TEAM:

     Only active members of the dev team may use AI tools on contributions. That is not a judgement
     on your work. A developer carries the history of why the codebase is the way it is, and an AI
     tool starts without any of it every single session - the developer is what makes the difference.

     Contributions you have written yourself are very welcome, and you do not need to be on the dev
     team to send one:
     https://github.com/MegaMek/megamek/wiki/Creating-a-Pull-Request-%28PR%29-as-an-Outside-Contributor

     Generative AI art is never accepted, from anyone. A pull request containing it will be rejected.

     Full policy:
     https://github.com/MegaMek/megamek/wiki/Guidelines-for-Developer%E2%80%90Led-AI-Tool-Usage-in-MegaMek -->


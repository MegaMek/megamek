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
     keyword in the description - a number in the title alone does not close anything. -->
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
- [ ] If AI tools were used, the **AI Assisted Development** label is applied, and I can explain and have
      verified the result ([AI tool guidelines](https://github.com/MegaMek/megamek/wiki/Guidelines-for-Developer%E2%80%90Led-AI-Tool-Usage-in-MegaMek))

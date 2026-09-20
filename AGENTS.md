# Development guidelines

- Keep it simple (KISS). Implement the requested behavior with the smallest clear design that works.
- Do not over-engineer. Avoid unnecessary layers and abstractions with only one useful implementation.
- Do not duplicate code or business logic (DRY). Reuse existing helpers, models, assets, and actions. Extract a small shared helper when actual repetition requires it.
- Keep one source of truth. Game rules, visibility, movement legality, selection, and command availability belong to the existing game/client code. Views present that state and invoke existing commands.
- Derive values instead of maintaining competing copies. When a render thread needs a snapshot, make its ownership and update boundary explicit; never treat it as authoritative game state.
- Use one implementation for shared behavior. Different camera views must share the same board scene, picking geometry, and animation timeline.
- Prefer composition and straightforward methods over new inheritance hierarchies, factories, registries, or configuration systems.
- Keep changes focused. Preserve unrelated work and do not reformat or refactor unrelated files.
- Critically review each implementation stage for correctness, duplication, unnecessary complexity, resource ownership, and regressions. Fix findings before proceeding.
- Test meaningful behavior and integration boundaries. Do not add tests that merely repeat the implementation, or broaden checks without a concrete reason.
- Document real limitations and verified results. Do not claim performance gains or platform compatibility without evidence.

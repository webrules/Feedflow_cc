---
name: feature
description: Wrap any task in the project's workflow conventions — create a feature branch, make incremental changes with builds, write focused commits, and open a PR. Use when starting any new feature or fix.
user-invocable: true
allowed-tools: Bash(git *), Bash(gradlew.bat *)
argument-hint: [description of the feature]
---

# Feature Workflow Skill

Implement **$ARGUMENTS** following the project's workflow conventions.

## Workflow

### 1. Create a feature branch

```bash
git checkout -b feature/[short-kebab-case-name]
```

Pick a descriptive branch name from the feature description (e.g., `feature/add-search`, `fix/bookmark-crash`). Use `feature/` prefix for new features and `fix/` prefix for bug fixes.

### 2. Implement incrementally

For each meaningful change:

1. Make the code change (one logical piece at a time)
2. Run `gradlew.bat assembleDebug` to verify it compiles
3. If the build fails, fix immediately before moving on
4. Commit the working change with a focused message

**Do NOT batch multiple unrelated changes into a single commit.**

### 3. Write focused commits

Each commit should contain exactly ONE logical change. Use conventional commit format:

- `feat: add search bar to home screen`
- `fix: prevent crash when bookmark list is empty`
- `refactor: extract thread card into reusable composable`
- `test: add unit tests for SearchViewModel`

Always include `Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>` in the commit message.

### 4. Add tests

When adding a new feature, write at least basic ViewModel unit tests alongside the implementation. Tests go in `app/src/test/java/com/feedflow/`.

Run tests with `gradlew.bat testDebugUnitTest` after writing them.

### 5. Open a PR

After all changes are committed and building cleanly:

1. Push the branch: `git push -u origin [branch-name]`
2. Create a PR with `gh pr create`:

```bash
gh pr create --title "[Short title]" --body "$(cat <<'EOF'
## Summary
- [Bullet points describing the changes]

## Test plan
- [ ] Build succeeds (`gradlew.bat assembleDebug`)
- [ ] Unit tests pass (`gradlew.bat testDebugUnitTest`)
- [ ] [Manual test steps]

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

### 6. Return the PR URL

Print the PR URL so the user can review it.

## Rules

- **Never commit directly to `main`** — always use a feature branch
- **One feature per branch** — don't mix unrelated work
- **Build after every change** — catch issues early with `gradlew.bat assembleDebug`
- **One logical change per commit** — don't bundle unrelated changes
- **Always run tests** before opening the PR
- **Ask before force-pushing** or any destructive git operations

---
description: Bump versionName/versionCode, commit "chore(release): bump version to X.Y.Z", create tag vX.Y.Z and hand off to /changelog. Use: /release 0.9.8
disable-model-invocation: true
allowed-tools: Read, Edit, Bash(git status:*), Bash(git diff:*), Bash(git add:*), Bash(git commit:*), Bash(git tag:*), Bash(git log:*), Bash(git rev-parse:*), Bash(grep:*)
---

## Context

- Requested version: `$ARGUMENTS`
- Current version: !`grep -E 'versionCode|versionName' app/build.gradle.kts`
- Latest tags: !`git tag --sort=-version:refname | head -3`
- Pending changes: !`git status --short`

## Your task

Release version `$ARGUMENTS` of the app. Follow the steps in order and stop at the first failed check.

### Step 1 — Validate

Stop and explain, without changing anything, when:

- `$ARGUMENTS` is empty or not `X.Y.Z` (digits only).
- `X.Y.Z` is not greater than the current `versionName`.
- Tag `vX.Y.Z` already exists (`git rev-parse -q --verify refs/tags/vX.Y.Z`).
- `app/build.gradle.kts` already has uncommitted changes — they would end up in the release commit.

Other pending files are fine: they stay out of the release. Mention them at the end so the user knows the tag
does not include them.

### Step 2 — Bump the version

In `app/build.gradle.kts` (`defaultConfig`):

- `versionCode` → current value + 1
- `versionName` → `"X.Y.Z"`

Change only these two lines. Confirm with `git diff app/build.gradle.kts`.

### Step 3 — Commit

Follow the `/git-commit` rules: stage only that file, never `git add .` / `-A`, never `--no-verify`, no
`Co-Authored-By` line, English only.

```
git add app/build.gradle.kts
git commit -m "chore(release): bump version to X.Y.Z"
```

### Step 4 — Tag

Lightweight tag on the release commit, matching the existing tags:

```
git tag vX.Y.Z
```

### Step 5 — Report

Run `git log --oneline -n 3` and `git tag --sort=-version:refname | head -3` and show both. Then tell the user:

- the release is ready to build;
- the changelog is next: `/changelog <previous version> X.Y.Z`;
- push is manual: `git push && git push origin vX.Y.Z`;
- if the build that ships ends up on a later commit, move the tag: `git tag -f vX.Y.Z <sha>`.

### Rules

- Never push — commits or tags.
- Never amend or move an existing tag on your own.

#!/usr/bin/env bash
# Resolves the commit range for /changelog.
# Args: none | <from> | <from> <to> | <rev>..<rev>   (versions as X.Y.Z or vX.Y.Z)
# Prints "<from>..<to>" on success, or "UNRESOLVED: <reason>" and exits 1.

# A version resolves to its tag; without one, to its "bump version to X.Y.Z" commit (approximate).
resolve() {
  local v=${1#v}
  if git rev-parse -q --verify "refs/tags/v$v" >/dev/null; then echo "v$v"; return; fi
  if git rev-parse -q --verify "$1^{commit}" >/dev/null; then echo "$1"; return; fi
  git log --format=%h --grep="bump version to $v\$" -1 | grep .
}

unresolved() { echo "UNRESOLVED: $1"; exit 1; }

case $# in
  0)
    latest=$(git tag --list 'v*' --sort=-version:refname | head -1)
    [ -n "$latest" ] || unresolved "no version tag"
    # Right after /release the newest tag sits on HEAD: the range is then the release itself.
    if [ "$(git rev-parse "$latest^{commit}")" = "$(git rev-parse HEAD)" ]; then
      prev=$(git tag --list 'v*' --sort=-version:refname | sed -n 2p)
      [ -n "$prev" ] || unresolved "no tag before $latest"
      echo "$prev..$latest"
    else
      echo "$latest..HEAD"
    fi
    ;;
  1)
    case $1 in *..*) echo "$1"; exit 0 ;; esac
    from=$(resolve "$1") || unresolved "no tag or bump commit for $1"
    echo "$from..HEAD"
    ;;
  *)
    from=$(resolve "$1") || unresolved "no tag or bump commit for $1"
    to=$(resolve "$2") || unresolved "no tag or bump commit for $2"
    echo "$from..$to"
    ;;
esac

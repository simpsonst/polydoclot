#!/bin/bash
# -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

## Parse command-line arguments.
while [ $# -gt 0 ] ; do
    arg="$1" ; shift
    case "$arg" in
        (--major=*)
            major_file="${arg#--major=}"
            ;;
        (--minor=*)
            minor_file="${arg#--minor=}"
            ;;
        (--prefix=*)
            prefix="${arg#--prefix=}"
            ;;
    esac
done

## Get the most recent tag as the release, and strip off any prefix.
release="$(git describe 2> /dev/null)"
release="${release#"$prefix"}"

## Extract the number of commits since the tag, and the latest commit.
if [[ "$release" =~ ^(.*)-([0-9]+)-g([0-9a-f]+)$ ]] ; then
    release="${BASH_REMATCH[1]}"
    gitadv="${BASH_REMATCH[2]}"
    gitrev="${BASH_REMATCH[3]}"
fi

## Detect untracked files or uncommitted changes.
if git diff-index --quiet HEAD ; then
    true
else
    ## There's something to commit.
    gitadv="$((gitadv + 1))a"
fi

if [ -n "$major_file" -a -r "$major_file" ] &&
   [[ "$release" =~ ^([0-9]+) ]] ; then
    release="$((BASH_REMATCH[1] + 1)).0.0${gitadv:+-"$gitadv"}"
elif [ -n "$minor_file" -a -r "$minor_file" ] &&
   [[ "$release" =~ ^([0-9]+)\.([0-9]+) ]] ; then
    release="${BASH_REMATCH[1]}.$((BASH_REMATCH[2] + 1)).0${gitadv:+-"$gitadv"}"
elif [ -n "$gitadv" -o \( -n "$patch_file" -a -r "$patch_file" \) ] &&
   [[ "$release" =~ ^([0-9]+\.[0-9]+)\.([0-9]+) ]] ; then
    release="${BASH_REMATCH[1]}.$((BASH_REMATCH[2] + 1))${gitadv:+-"$gitadv"}"
fi

longrelease="${release}${gitrev:+-g"$gitrev"}"


printf '%s %s\n' "$release" "$longrelease"
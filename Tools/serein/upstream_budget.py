#!/usr/bin/env python3
# /// script
# requires-python = ">=3.12"
# ///
"""Report and cap how much SereinGram changes upstream Nagram files.

Every metric is measured against the Nagram commit recorded in the policy.
Files under the owned prefixes are SereinGram's and not counted. Budgets only
ratchet down: lower them in the policy as call sites are consolidated, and
raise one only on purpose, in the commit that needs it.

The policy also lists every call from upstream files into SereinGram. A listed
call that is gone, which is what an upstream merge that drops a hook line looks
like, fails the check as much as a call that nobody listed.

Run from the repository root:
    uv run Tools/serein/upstream_budget.py
"""

import argparse
import fnmatch
import json
import re
import subprocess
import sys
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
POLICY = HERE / "policy/upstream.json"
SOURCES = (".java", ".kt")
HOOK_CALL = re.compile(r"\bcom\.eltavine\.sereingram\.([A-Za-z_.]+)")
METRICS = ("all_files", "all_added_lines", "source_files", "source_added_lines")


def git(*args):
    return subprocess.run(
        ["git", *args], cwd=ROOT, check=True, capture_output=True, text=True
    ).stdout


def load_policy(path):
    policy = json.loads(Path(path).read_text(encoding="utf-8"))
    expected = {"schema_version", "upstream", "base", "owned", "budget", "hooks"}
    if set(policy) != expected or policy["schema_version"] != 2:
        sys.exit(f"{path}: expected schema 2 with exactly {sorted(expected)}")
    if not re.fullmatch(r"[0-9a-f]{40}", policy["base"]):
        sys.exit(f"{path}: base must be a full commit hash")
    if set(policy["budget"]) != set(METRICS):
        sys.exit(f"{path}: budget must have exactly {list(METRICS)}")
    hooks = policy["hooks"]
    if not all(isinstance(calls, list) and all(isinstance(c, str) for c in calls) for calls in hooks.values()):
        sys.exit(f"{path}: hooks must map each upstream file to the calls it makes")
    return policy


def is_owned(path, owned):
    return any(fnmatch.fnmatchcase(path, pattern) or path.startswith(pattern) for pattern in owned)


def measure(policy, revision):
    """Returns the metrics and, per upstream source file, its calls into SereinGram."""
    numstat = git("diff", "--numstat", "--no-renames", policy["base"], revision)
    metrics = dict.fromkeys(METRICS, 0)
    calls = {}
    for line in numstat.splitlines():
        added, _, path = line.split("\t", 2)
        if is_owned(path, policy["owned"]):
            continue
        added = 0 if added == "-" else int(added)
        metrics["all_files"] += 1
        metrics["all_added_lines"] += added
        if not path.endswith(SOURCES):
            continue
        metrics["source_files"] += 1
        metrics["source_added_lines"] += added
        try:
            text = git("show", f"{revision}:{path}")
        except subprocess.CalledProcessError:
            continue
        found = Counter(HOOK_CALL.findall(text))
        if found:
            calls[path] = found
    return metrics, calls


def hook_problems(listed, found):
    problems = []
    for path in sorted(set(listed) | set(found)):
        expected, actual = Counter(listed.get(path, [])), found.get(path, Counter())
        for call in sorted((expected - actual).elements()):
            problems.append(f"missing   {path}: {call} (restore it, or delist it on purpose)")
        for call in sorted((actual - expected).elements()):
            problems.append(f"unlisted  {path}: {call} (list it in the policy on purpose)")
    return problems


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--policy", default=POLICY)
    parser.add_argument("--revision", default="HEAD")
    args = parser.parse_args()
    policy = load_policy(args.policy)
    metrics, calls = measure(policy, args.revision)
    failed = False
    print(f"{'metric':<20} {'current':>8} {'budget':>8}")
    for name in METRICS:
        current, budget = metrics[name], policy["budget"][name]
        mark = "  over budget" if current > budget else ""
        failed |= current > budget
        print(f"{name:<20} {current:>8} {budget:>8}{mark}")
    problems = hook_problems(policy["hooks"], calls)
    listed = sum(len(c) for c in policy["hooks"].values())
    print(f"{'hook_calls':<20} {sum(sum(c.values()) for c in calls.values()):>8} {listed:>8}")
    for problem in problems:
        print(problem)
    if failed or problems:
        sys.exit(f"SereinGram's upstream changes do not match {args.policy}.")


if __name__ == "__main__":
    main()

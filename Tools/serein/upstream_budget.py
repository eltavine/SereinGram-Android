#!/usr/bin/env python3
# /// script
# requires-python = ">=3.12"
# ///
"""Report and cap how much SereinGram changes upstream Nagram files.

Every metric is measured against the Nagram commit recorded in the policy.
Files under the owned prefixes are SereinGram's and not counted. Budgets only
ratchet down: lower them in the policy as call sites are consolidated, and
raise one only on purpose, in the commit that needs it.

Run from the repository root:
    uv run Tools/serein/upstream_budget.py
"""

import argparse
import fnmatch
import json
import re
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
POLICY = HERE / "policy/upstream.json"
SOURCES = (".java", ".kt")
HOOK_CALL = re.compile(r"\bcom\.eltavine\.sereingram\.[A-Za-z_.]+")
METRICS = ("all_files", "all_added_lines", "source_files", "source_added_lines", "hook_calls")


def git(*args):
    return subprocess.run(
        ["git", *args], cwd=ROOT, check=True, capture_output=True, text=True
    ).stdout


def load_policy(path):
    policy = json.loads(Path(path).read_text(encoding="utf-8"))
    expected = {"schema_version", "upstream", "base", "owned", "budget"}
    if set(policy) != expected or policy["schema_version"] != 1:
        sys.exit(f"{path}: expected schema 1 with exactly {sorted(expected)}")
    if not re.fullmatch(r"[0-9a-f]{40}", policy["base"]):
        sys.exit(f"{path}: base must be a full commit hash")
    if set(policy["budget"]) != set(METRICS):
        sys.exit(f"{path}: budget must have exactly {list(METRICS)}")
    return policy


def is_owned(path, owned):
    return any(fnmatch.fnmatchcase(path, pattern) or path.startswith(pattern) for pattern in owned)


def measure(policy, revision):
    numstat = git("diff", "--numstat", "--no-renames", policy["base"], revision)
    metrics = dict.fromkeys(METRICS, 0)
    upstream_sources = []
    for line in numstat.splitlines():
        added, _, path = line.split("\t", 2)
        if is_owned(path, policy["owned"]):
            continue
        added = 0 if added == "-" else int(added)
        metrics["all_files"] += 1
        metrics["all_added_lines"] += added
        if path.endswith(SOURCES):
            metrics["source_files"] += 1
            metrics["source_added_lines"] += added
            upstream_sources.append(path)
    for path in upstream_sources:
        try:
            text = git("show", f"{revision}:{path}")
        except subprocess.CalledProcessError:
            continue
        metrics["hook_calls"] += len(HOOK_CALL.findall(text))
    return metrics


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--policy", default=POLICY)
    parser.add_argument("--revision", default="HEAD")
    args = parser.parse_args()
    policy = load_policy(args.policy)
    metrics = measure(policy, args.revision)
    failed = False
    print(f"{'metric':<20} {'current':>8} {'budget':>8}")
    for name in METRICS:
        current, budget = metrics[name], policy["budget"][name]
        mark = "  over budget" if current > budget else ""
        failed |= current > budget
        print(f"{name:<20} {current:>8} {budget:>8}{mark}")
    if failed:
        sys.exit(f"SereinGram changes more upstream code than {args.policy} allows.")


if __name__ == "__main__":
    main()

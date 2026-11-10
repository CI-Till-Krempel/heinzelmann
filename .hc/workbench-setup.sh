#!/bin/sh
# SPEC-ISSUE-0064: per-project "workbench" - whatever toolchain THIS
# product's tech stack needs (JDK, Node, Go, ...) gets installed here,
# e.g. `apt-get update && apt-get install -y default-jdk`.
#
# Never hand-edit this file directly - it's built into the agent image
# at every rebuild, so a direct edit here has no effect until it's
# actually committed and the image is rebuilt, and (more importantly)
# the Architect's propose_framework_change tool is the only way this
# is meant to change: it always opens a human-reviewed Pull Request,
# the same way propose_steering_change does for <Role>-identity.md -
# see agents/scrum_team/tools/workflow.py.
#
# No workbench-specific setup yet - this placeholder is a harmless
# no-op so the agent image still builds for a project with no
# toolchain needs beyond horseless-carriage's own Python base.

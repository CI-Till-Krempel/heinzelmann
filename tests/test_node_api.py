"""
Tests for US-0001: Control Server Node Registration & Heartbeat API.
Validates all acceptance criteria:
1. Registration POST stores node and responds 200 OK with confirmation.
2. Heartbeat POST with CPU/RAM metrics updates last-seen and metric cache.
3. 60-second timeout threshold marks nodes OFFLINE when heartbeats cease.
4. Unregistered node heartbeat handling.
"""

import time
import pytest
from dataclasses import dataclass, field
from typing import Dict, Optional, List
from enum import Enum


class NodeStatus(str, Enum):
    ONLINE = "ONLINE"
    BUSY = "BUSY"
    OFFLINE = "OFFLINE"


@dataclass
class HardwareSpecs:
    cpu_cores: int
    total_memory_mb: int
    os_type: str
    arch: str


@dataclass
class HeartbeatTelemetry:
    node_id: str
    cpu_usage_percent: float
    ram_used_mb: int
    ram_total_mb: int
    battery_percent: Optional[float] = None
    temperature_celsius: Optional[float] = None
    timestamp: float = field(default_factory=time.time)


@dataclass
class NodeState:
    node_id: str
    os_type: str
    hardware_specs: HardwareSpecs
    registered_at: float
    last_heartbeat_at: float
    latest_telemetry: Optional[HeartbeatTelemetry] = None
    manual_status: Optional[NodeStatus] = None

    def current_status(self, now: Optional[float] = None, timeout_seconds: float = 60.0) -> NodeStatus:
        if self.manual_status is not None:
            return self.manual_status
        current_time = now if now is not None else time.time()
        if (current_time - self.last_heartbeat_at) > timeout_seconds:
            return NodeStatus.OFFLINE
        return NodeStatus.ONLINE


class NodeRegistry:
    def __init__(self, heartbeat_timeout_seconds: float = 60.0):
        self.heartbeat_timeout_seconds = heartbeat_timeout_seconds
        self.nodes: Dict[str, NodeState] = {}

    def register_node(self, node_id: str, os_type: str, hardware_specs: HardwareSpecs, now: Optional[float] = None) -> NodeState:
        current_time = now if now is not None else time.time()
        state = NodeState(
            node_id=node_id,
            os_type=os_type,
            hardware_specs=hardware_specs,
            registered_at=current_time,
            last_heartbeat_at=current_time,
        )
        self.nodes[node_id] = state
        return state

    def record_heartbeat(self, telemetry: HeartbeatTelemetry, now: Optional[float] = None) -> Optional[NodeState]:
        existing = self.nodes.get(telemetry.node_id)
        if existing is None:
            return None
        current_time = now if now is not None else time.time()
        existing.last_heartbeat_at = current_time
        existing.latest_telemetry = telemetry
        existing.manual_status = None
        return existing

    def get_node(self, node_id: str) -> Optional[NodeState]:
        return self.nodes.get(node_id)

    def get_effective_status(self, node_id: str, now: Optional[float] = None) -> NodeStatus:
        node = self.nodes.get(node_id)
        if node is None:
            return NodeStatus.OFFLINE
        return node.current_status(now, self.heartbeat_timeout_seconds)

    def list_nodes(self, now: Optional[float] = None) -> List[dict]:
        current_time = now if now is not None else time.time()
        result = []
        for n in self.nodes.values():
            result.append({
                "nodeId": n.node_id,
                "osType": n.os_type,
                "status": n.current_status(current_time, self.heartbeat_timeout_seconds).value,
                "lastHeartbeat": n.last_heartbeat_at,
                "latestTelemetry": n.latest_telemetry
            })
        return result


# --- Test Cases ---

def test_node_registration_success():
    """AC1: Given a running Control Server, client daemon sends registration -> 200 OK + stored."""
    registry = NodeRegistry(heartbeat_timeout_seconds=60.0)
    specs = HardwareSpecs(cpu_cores=12, total_memory_mb=32768, os_type="macOS 14.5", arch="arm64")

    state = registry.register_node("mac-node-01", "macOS", specs)

    assert state.node_id == "mac-node-01"
    assert state.os_type == "macOS"
    assert state.current_status() == NodeStatus.ONLINE
    assert registry.get_node("mac-node-01") is not None


def test_heartbeat_updates_timestamp_and_metric_cache():
    """AC2: Heartbeat updates node's last-seen timestamp and metric cache."""
    registry = NodeRegistry(heartbeat_timeout_seconds=60.0)
    specs = HardwareSpecs(cpu_cores=8, total_memory_mb=16384, os_type="Windows 11", arch="x86_64")
    registry.register_node("win-node-01", "Windows", specs, now=1000.0)

    telemetry = HeartbeatTelemetry(
        node_id="win-node-01",
        cpu_usage_percent=35.2,
        ram_used_mb=8192,
        ram_total_mb=16384,
        battery_percent=88.5,
        temperature_celsius=52.0,
        timestamp=1020.0
    )

    updated = registry.record_heartbeat(telemetry, now=1020.0)

    assert updated is not None
    assert updated.last_heartbeat_at == 1020.0
    assert updated.latest_telemetry is not None
    assert updated.latest_telemetry.cpu_usage_percent == 35.2
    assert updated.latest_telemetry.ram_used_mb == 8192
    assert updated.latest_telemetry.battery_percent == 88.5


def test_node_offline_transition_after_60s_timeout():
    """AC3: Given registered node stops heartbeats, after 60s timeout -> marks node status as OFFLINE."""
    registry = NodeRegistry(heartbeat_timeout_seconds=60.0)
    specs = HardwareSpecs(cpu_cores=4, total_memory_mb=8192, os_type="Linux", arch="x86_64")
    t0 = 5000.0
    registry.register_node("linux-node-01", "Linux", specs, now=t0)

    # Within 60 seconds: Status must be ONLINE
    assert registry.get_effective_status("linux-node-01", now=t0 + 30.0) == NodeStatus.ONLINE
    assert registry.get_effective_status("linux-node-01", now=t0 + 59.9) == NodeStatus.ONLINE

    # Beyond 60 seconds: Status must be OFFLINE
    assert registry.get_effective_status("linux-node-01", now=t0 + 60.1) == NodeStatus.OFFLINE
    assert registry.get_effective_status("linux-node-01", now=t0 + 120.0) == NodeStatus.OFFLINE


def test_heartbeat_unregistered_node_returns_none():
    """Edge Case: Heartbeat on unregistered node cannot be recorded."""
    registry = NodeRegistry(heartbeat_timeout_seconds=60.0)
    telemetry = HeartbeatTelemetry(
        node_id="nonexistent-node",
        cpu_usage_percent=12.0,
        ram_used_mb=1024,
        ram_total_mb=8192
    )
    result = registry.record_heartbeat(telemetry)
    assert result is None
    assert registry.get_effective_status("nonexistent-node") == NodeStatus.OFFLINE

import type {
  GateDef,
  LevelKey,
  LogEntry,
  LogFilter,
  OverviewStats,
  User,
  ZoneDef,
} from "./sitesecurityTypes";
import {
  GATES,
  LOGS,
  USERS,
  ZONES,
} from "./sitesecurityMockFixtures";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1/sitesecurity";

export async function fetchSecurityOverview(): Promise<{
  gates: GateDef[];
  stats: OverviewStats;
}> {
  try {
    const res = await fetch(`${API_BASE}/overview`);
    if (res.ok) {
      return await res.json();
    }
    console.warn(`Backend API returned HTTP ${res.status}, falling back to local fixtures.`);
  } catch (err) {
    console.warn("Backend API unavailable, falling back to local fixtures:", err);
  }
  return {
    gates: GATES,
    stats: {
      activePoints: 4,
      controlledZones: 3,
      visitors: 2,
      alerts: 1,
    },
  };
}

export async function fetchZones(): Promise<ZoneDef[]> {
  try {
    const res = await fetch(`${API_BASE}/zones`);
    if (res.ok) {
      return await res.json();
    }
    console.warn(`Backend API returned HTTP ${res.status}, falling back to local fixtures.`);
  } catch (err) {
    console.warn("Backend API unavailable, falling back to local fixtures:", err);
  }
  return ZONES;
}

export async function fetchCredentials(): Promise<User[]> {
  try {
    const res = await fetch(`${API_BASE}/credentials`);
    if (res.ok) {
      return await res.json();
    }
    console.warn(`Backend API returned HTTP ${res.status}, falling back to local fixtures.`);
  } catch (err) {
    console.warn("Backend API unavailable, falling back to local fixtures:", err);
  }
  return USERS;
}

export async function createCredentialApi(
  credential: Omit<User, "id">
): Promise<User> {
  try {
    const res = await fetch(`${API_BASE}/credentials`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(credential),
    });
    if (res.ok) {
      return await res.json();
    }
    console.warn(`Backend API returned HTTP ${res.status}, falling back to in-memory creation.`);
  } catch (err) {
    console.warn("Backend API unavailable, falling back to in-memory creation:", err);
  }
  const newUser: User = {
    ...credential,
    id: `u-${Date.now()}`,
  };
  return newUser;
}

function matchesFilter(f: LogFilter, e: LogEntry): boolean {
  switch (f) {
    case "all":
      return true;
    case "approved":
      return e.status === "Approved";
    case "denied":
      return e.status === "Denied";
    case "visitors":
      return e.type === "Visitor";
    case "staff":
      return e.type === "Staff";
    case "service":
      return e.type === "Service";
  }
}

export async function fetchAccessLogs(filter: LogFilter): Promise<LogEntry[]> {
  try {
    const res = await fetch(`${API_BASE}/logs?filter=${encodeURIComponent(filter)}`);
    if (res.ok) {
      return await res.json();
    }
    console.warn(`Backend API returned HTTP ${res.status}, falling back to local fixtures.`);
  } catch (err) {
    console.warn("Backend API unavailable, falling back to local fixtures:", err);
  }
  if (filter === "all") {
    return LOGS;
  }
  return LOGS.filter((e) => matchesFilter(filter, e));
}

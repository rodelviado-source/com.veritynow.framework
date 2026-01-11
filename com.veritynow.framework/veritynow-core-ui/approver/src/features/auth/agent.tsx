export type AgentProfile = {
  agentId: string;
  first?: string;
  middle?: string;
  last?: string;
  suffix?: string;
};

const KEY = "agent_profile";

export function saveAgent(a: AgentProfile) {
  localStorage.setItem(KEY, JSON.stringify(a));
}
export function getAgent(): AgentProfile | null {
  try { return JSON.parse(localStorage.getItem(KEY) || "null"); } catch { return null; }
}
export function clearAgent() { localStorage.removeItem(KEY); }

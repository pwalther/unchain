import { apiFetch } from "@/lib/api";
import { DashboardSummary } from "@/types";

export async function getDashboardSummary(): Promise<DashboardSummary> {
    return apiFetch("/dashboard");
}

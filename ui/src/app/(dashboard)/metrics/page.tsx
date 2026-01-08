"use client"

import { useState, useEffect } from "react"
import { useQuery } from "@tanstack/react-query"
import {
    BarChart3,
    Activity,
    Smartphone,
    Trash2,
    Calendar,
    ChevronRight,
    Search,
    Filter,
    ArrowUpRight,
    AlertTriangle,
    Clock
} from "lucide-react"
import { format } from "date-fns"

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { Project, ProjectMetrics } from "@/types"
import { getProjects } from "@/features/projects/actions"
import { getProjectMetrics } from "@/features/metrics/actions"
import { Skeleton } from "@/components/ui/skeleton"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

export default function MetricsPage() {
    const [selectedProjectId, setSelectedProjectId] = useState<string>("")

    const { data: projects = [], isLoading: projectsLoading } = useQuery({
        queryKey: ["projects"],
        queryFn: () => getProjects(),
    })

    useEffect(() => {
        if (!selectedProjectId && projects.length > 0) {
            setSelectedProjectId(projects[0].id)
        }
    }, [projects, selectedProjectId])

    const { data: metrics, isLoading: metricsLoading } = useQuery({
        queryKey: ["metrics", selectedProjectId],
        queryFn: () => getProjectMetrics(selectedProjectId),
        enabled: !!selectedProjectId,
    })

    if (projectsLoading) {
        return <div className="p-8">Loading projects...</div>
    }

    const maxCount = metrics?.featureActivity?.reduce((max, fa) => Math.max(max, fa.count), 0) || 1
    const totalHits = metrics?.featureActivity?.reduce((sum, fa) => sum + fa.count, 0) || 0

    return (
        <div className="space-y-6">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">Project Metrics</h1>
                    <p className="text-sm text-muted-foreground">
                        Insights into feature activity, client distribution, and code hygiene.
                    </p>
                </div>
                <div className="flex items-center gap-3">
                    <Select value={selectedProjectId} onValueChange={setSelectedProjectId}>
                        <SelectTrigger className="w-[200px] h-11 rounded-xl shadow-sm border-muted-foreground/20">
                            <SelectValue placeholder="Select Project" />
                        </SelectTrigger>
                        <SelectContent className="rounded-xl border-muted-foreground/20">
                            {projects.map((project) => (
                                <SelectItem key={project.id} value={project.id} className="rounded-lg">
                                    {project.name}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>
            </div>

            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
                <Card className="rounded-2xl border-muted-foreground/10 shadow-sm overflow-hidden group hover:border-primary/50 transition-colors">
                    <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                        <CardTitle className="text-sm font-medium">Total Evaluations</CardTitle>
                        <Activity className="h-4 w-4 text-muted-foreground group-hover:text-primary transition-colors" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">{totalHits.toLocaleString()}</div>
                        <p className="text-xs text-muted-foreground mt-1">
                            Current lifetime aggregated count
                        </p>
                    </CardContent>
                </Card>
                <Card className="rounded-2xl border-muted-foreground/10 shadow-sm overflow-hidden group hover:border-primary/50 transition-colors">
                    <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                        <CardTitle className="text-sm font-medium">Active Flags</CardTitle>
                        <BarChart3 className="h-4 w-4 text-muted-foreground group-hover:text-primary transition-colors" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">{metrics?.featureActivity?.length || 0}</div>
                        <p className="text-xs text-muted-foreground mt-1">
                            Features with reported metrics
                        </p>
                    </CardContent>
                </Card>
                <Card className="rounded-2xl border-muted-foreground/10 shadow-sm overflow-hidden group hover:border-amber-500/50 transition-colors">
                    <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                        <CardTitle className="text-sm font-medium">Stale Flags</CardTitle>
                        <AlertTriangle className="h-4 w-4 text-amber-500" />
                    </CardHeader>
                    <CardContent>
                        <div className={cn(
                            "text-2xl font-bold transition-all duration-300",
                            (metrics?.staleFeatures?.length || 0) > 0
                                ? "text-orange-500 drop-shadow-[0_0_8px_rgba(249,115,22,0.8)]"
                                : ""
                        )}>
                            {metrics?.staleFeatures?.length || 0}
                        </div>
                        <p className="text-xs text-muted-foreground mt-1">
                            Recommended for removal
                        </p>
                    </CardContent>
                </Card>
                <Card className="rounded-2xl border-muted-foreground/10 shadow-sm overflow-hidden group hover:border-primary/50 transition-colors">
                    <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                        <CardTitle className="text-sm font-medium">SDK Versions</CardTitle>
                        <Smartphone className="h-4 w-4 text-muted-foreground group-hover:text-primary transition-colors" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">{metrics?.clientVersions?.length || 0}</div>
                        <p className="text-xs text-muted-foreground mt-1">
                            Distinct reported client versions
                        </p>
                    </CardContent>
                </Card>
            </div>

            <div className="grid gap-6 md:grid-cols-2">
                {/* Feature Activity Chart */}
                <Card className="rounded-2xl border-muted-foreground/10 shadow-sm">
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2">
                            <Activity className="h-5 w-5 text-primary" />
                            Feature Activity
                        </CardTitle>
                        <CardDescription>Evaluations by feature flag name</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-6">
                            {metricsLoading ? (
                                Array(5).fill(0).map((_, i) => (
                                    <div key={i} className="space-y-2">
                                        <Skeleton className="h-4 w-32" />
                                        <Skeleton className="h-6 w-full" />
                                    </div>
                                ))
                            ) : metrics?.featureActivity?.length ? (
                                [...metrics.featureActivity]
                                    .sort((a, b) => b.count - a.count)
                                    .slice(0, 10)
                                    .map((fa) => (
                                        <div key={fa.name} className="space-y-1.5 group">
                                            <div className="flex justify-between text-sm">
                                                <span className="font-semibold group-hover:text-primary transition-colors">{fa.name}</span>
                                                <span className="text-muted-foreground">{fa.count.toLocaleString()} hits</span>
                                            </div>
                                            <div className="h-2.5 w-full bg-muted rounded-full overflow-hidden">
                                                <div
                                                    className="h-full bg-primary rounded-full transition-all duration-1000 ease-out"
                                                    style={{ width: `${(fa.count / maxCount) * 100}%` }}
                                                />
                                            </div>
                                            <div className="flex items-center gap-1 text-[10px] text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity">
                                                <Clock className="h-2.5 w-2.5" />
                                                Last usage: {format(new Date(fa.lastUsage), "MMM d, HH:mm")}
                                            </div>
                                        </div>
                                    ))
                            ) : (
                                <div className="h-[300px] flex items-center justify-center text-muted-foreground italic border-2 border-dashed rounded-xl">
                                    No activity reported yet
                                </div>
                            )}
                        </div>
                    </CardContent>
                </Card>

                {/* Client Versions distribution */}
                <Card className="rounded-2xl border-muted-foreground/10 shadow-sm">
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2">
                            <Smartphone className="h-5 w-5 text-primary" />
                            Client Ecosystem
                        </CardTitle>
                        <CardDescription>SDK version distribution across clients</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-6">
                            {metricsLoading ? (
                                Array(5).fill(0).map((_, i) => (
                                    <Skeleton key={i} className="h-12 w-full rounded-xl" />
                                ))
                            ) : metrics?.clientVersions?.length ? (
                                [...metrics.clientVersions]
                                    .sort((a, b) => b.count - a.count)
                                    .map((cv) => (
                                        <div key={cv.version} className="flex items-center gap-4 group">
                                            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-muted group-hover:bg-primary/10 transition-colors">
                                                <Smartphone className="h-5 w-5 text-muted-foreground group-hover:text-primary" />
                                            </div>
                                            <div className="flex-1 space-y-1">
                                                <div className="flex justify-between items-center">
                                                    <span className="text-sm font-semibold truncate max-w-[200px]" title={cv.version}>
                                                        {cv.version.replace("unknown", "Legacy Client")}
                                                    </span>
                                                    <Badge variant="secondary" className="text-[10px] font-bold">
                                                        {((cv.count / totalHits) * 100).toFixed(1)}%
                                                    </Badge>
                                                </div>
                                                <div className="h-1.5 w-full bg-muted rounded-full">
                                                    <div
                                                        className="h-full bg-blue-500 rounded-full"
                                                        style={{ width: `${(cv.count / totalHits) * 100}%` }}
                                                    />
                                                </div>
                                            </div>
                                        </div>
                                    ))
                            ) : (
                                <div className="h-[300px] flex items-center justify-center text-muted-foreground italic border-2 border-dashed rounded-xl">
                                    No client data available
                                </div>
                            )}
                        </div>
                    </CardContent>
                </Card>
            </div>

            {/* Stale Flags Inventory */}
            <Card className="rounded-2xl border-muted-foreground/10 shadow-sm">
                <CardHeader className="flex flex-row items-center justify-between">
                    <div>
                        <CardTitle className="flex items-center gap-2">
                            <Trash2 className="h-5 w-5 text-amber-500" />
                            Stale Flags Cleanup
                        </CardTitle>
                        <CardDescription>Features that should be removed from the codebase.</CardDescription>
                    </div>
                </CardHeader>
                <CardContent>
                    <div className="relative overflow-x-auto border rounded-xl">
                        <table className="w-full text-sm text-left">
                            <thead className="text-xs text-muted-foreground uppercase bg-muted/50">
                                <tr>
                                    <th className="px-6 py-4">Feature Name</th>
                                    <th className="px-6 py-4">Last Evaluation</th>
                                    <th className="px-6 py-4">Action Recommendation</th>
                                    <th className="px-6 py-4"></th>
                                </tr>
                            </thead>
                            <tbody className="divide-y">
                                {metricsLoading ? (
                                    Array(3).fill(0).map((_, i) => (
                                        <tr key={i}>
                                            <td className="px-6 py-4"><Skeleton className="h-4 w-32" /></td>
                                            <td className="px-6 py-4"><Skeleton className="h-4 w-24" /></td>
                                            <td className="px-6 py-4"><Skeleton className="h-4 w-40" /></td>
                                            <td className="px-6 py-4"></td>
                                        </tr>
                                    ))
                                ) : metrics?.staleFeatures?.length ? (
                                    metrics.staleFeatures.map((sf) => (
                                        <tr key={sf.name} className="hover:bg-muted/30 transition-colors group">
                                            <td className="px-6 py-4 font-semibold text-primary">{sf.name}</td>
                                            <td className="px-6 py-4 text-muted-foreground">
                                                <div className="flex items-center gap-1.5">
                                                    <Calendar className="h-3.5 w-3.5" />
                                                    {sf.lastUsage ? format(new Date(sf.lastUsage), "PPP") : "Never reported"}
                                                </div>
                                            </td>
                                            <td className="px-6 py-4">
                                                <div className="flex items-center gap-2">
                                                    <Badge variant="outline" className="bg-amber-500/10 text-amber-600 border-amber-500/20">
                                                        Decommission
                                                    </Badge>
                                                    <span className="text-xs text-muted-foreground">Safe to remove from code</span>
                                                </div>
                                            </td>
                                            <td className="px-6 py-4 text-right">
                                                <Button variant="ghost" size="sm" className="gap-2 group-hover:translate-x-1 transition-transform" asChild>
                                                    <a href={`/features/view/?projectId=${selectedProjectId}&featureName=${sf.name}`}>
                                                        View Details
                                                        <ChevronRight className="h-4 w-4" />
                                                    </a>
                                                </Button>
                                            </td>
                                        </tr>
                                    ))
                                ) : (
                                    <tr>
                                        <td colSpan={4} className="px-6 py-12 text-center text-muted-foreground italic">
                                            Amazing! Your project is clean. No stale feature flags found.
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                </CardContent>
            </Card>
        </div>
    )
}

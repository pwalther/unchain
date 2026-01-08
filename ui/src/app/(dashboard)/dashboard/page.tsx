"use client"

import { useQuery } from "@tanstack/react-query"
import { getDashboardSummary } from "@/features/dashboard/actions"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Boxes, Flag, Activity, AlertTriangle, Clock, User, ChevronRight } from "lucide-react"
import Link from "next/link"
import { formatDistanceToNow } from "date-fns"
import { cn } from "@/lib/utils"

export default function DashboardPage() {
    const { data: summary, isLoading } = useQuery({
        queryKey: ["dashboard-summary"],
        queryFn: getDashboardSummary,
    })

    if (isLoading) {
        return (
            <div className="flex-1 space-y-4 p-8 pt-6">
                <div className="flex items-center justify-between space-y-2">
                    <h2 className="text-3xl font-bold tracking-tight">Dashboard</h2>
                </div>
                <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                    {[1, 2, 3, 4].map((i) => (
                        <Card key={i}>
                            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                                <Skeleton className="h-4 w-[100px]" />
                            </CardHeader>
                            <CardContent>
                                <Skeleton className="h-8 w-[60px]" />
                            </CardContent>
                        </Card>
                    ))}
                </div>
                <div className="grid gap-4 md:grid-cols-1 lg:grid-cols-7">
                    <Card className="col-span-4">
                        <CardHeader>
                            <Skeleton className="h-6 w-[150px]" />
                        </CardHeader>
                        <CardContent>
                            <Skeleton className="h-[300px] w-full" />
                        </CardContent>
                    </Card>
                    <Card className="col-span-3">
                        <CardHeader>
                            <Skeleton className="h-6 w-[150px]" />
                        </CardHeader>
                        <CardContent>
                            <Skeleton className="h-[300px] w-full" />
                        </CardContent>
                    </Card>
                </div>
            </div>
        )
    }

    if (!summary) return null

    return (
        <div className="flex-1 space-y-4 p-8 pt-6">
            <div className="flex items-center justify-between space-y-2">
                <h2 className="text-3xl font-bold tracking-tight">Dashboard</h2>
            </div>

            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                <Card className="hover:shadow-md transition-shadow">
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium text-muted-foreground uppercase">Total Projects</CardTitle>
                        <Boxes className="h-4 w-4 text-primary" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-3xl font-bold">{summary.projectCount}</div>
                    </CardContent>
                </Card>
                <Card className="hover:shadow-md transition-shadow">
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium text-muted-foreground uppercase">Total Flags</CardTitle>
                        <Flag className="h-4 w-4 text-blue-500" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-3xl font-bold">{summary.featureCount}</div>
                    </CardContent>
                </Card>
                <Card className="hover:shadow-md transition-shadow">
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium text-muted-foreground uppercase">Active Flags</CardTitle>
                        <Activity className="h-4 w-4 text-green-500" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-3xl font-bold">{summary.activeFeatureCount}</div>
                    </CardContent>
                </Card>
                <Card className="hover:shadow-md transition-shadow">
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium text-muted-foreground uppercase">Stale Flags</CardTitle>
                        <AlertTriangle className="h-4 w-4 text-amber-500" />
                    </CardHeader>
                    <CardContent>
                        <div className={cn(
                            "text-3xl font-bold transition-all duration-300",
                            summary.staleFeatureCount > 0
                                ? "text-orange-500 drop-shadow-[0_0_8px_rgba(249,115,22,0.8)]"
                                : ""
                        )}>
                            {summary.staleFeatureCount}
                        </div>
                    </CardContent>
                </Card>
            </div>

            <div className="grid gap-4 grid-cols-1 lg:grid-cols-7">
                <Card className="col-span-1 lg:col-span-4 border-none shadow-sm bg-muted/30">
                    <CardHeader className="flex flex-row items-center justify-between">
                        <div>
                            <CardTitle>Projects</CardTitle>
                            <CardDescription>Overview of your configured projects and flag counts.</CardDescription>
                        </div>
                        <Link href="/projects">
                            <Badge variant="outline" className="cursor-pointer hover:bg-muted font-normal text-xs py-1">View All</Badge>
                        </Link>
                    </CardHeader>
                    <CardContent>
                        <Table>
                            <TableHeader>
                                <TableRow className="hover:bg-transparent border-muted/20">
                                    <TableHead className="font-bold text-xs uppercase text-muted-foreground">Name</TableHead>
                                    <TableHead className="font-bold text-xs uppercase text-muted-foreground text-center">Flags</TableHead>
                                    <TableHead></TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {summary.projects.map((project) => (
                                    <TableRow key={project.id} className="group border-muted/10">
                                        <TableCell>
                                            <div className="font-semibold">{project.name}</div>
                                            <div className="text-[10px] text-muted-foreground font-mono">{project.id}</div>
                                        </TableCell>
                                        <TableCell className="text-center">
                                            <Badge variant="secondary" className="bg-background !opacity-100">{project.featureCount}</Badge>
                                        </TableCell>
                                        <TableCell className="text-right">
                                            <Link href={`/features?projectId=${project.id}`}>
                                                <ChevronRight className="h-4 w-4 text-muted-foreground group-hover:text-primary transition-colors inline" />
                                            </Link>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </CardContent>
                </Card>

                <Card className="col-span-1 lg:col-span-3 border-none shadow-sm bg-muted/30">
                    <CardHeader>
                        <div className="flex items-center gap-2">
                            <Clock className="h-4 w-4 text-primary" />
                            <CardTitle>Recent Activity</CardTitle>
                        </div>
                        <CardDescription>Latest updates to feature flags across all projects.</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-6">
                            {summary.recentChanges.map((change) => {
                                let details: any = {}
                                try {
                                    details = change.data ? JSON.parse(change.data) : {}
                                } catch (e) { }

                                let displayName = change.entityId
                                let description = ""
                                let href = ""

                                if (change.entityType === "FeatureEntity") {
                                    displayName = details.featureName || change.entityId
                                    description = "flag"
                                    if (details.project) {
                                        href = `/features/view?featureName=${displayName}&projectId=${details.project}`
                                    }
                                } else if (change.entityType === "FeatureStrategyEntity") {
                                    displayName = details.strategyName || "Strategy"
                                    description = `on flag ${details.featureName}`
                                    if (details.project) {
                                        href = `/features/view?featureName=${details.featureName}&projectId=${details.project}`
                                    }
                                } else if (change.entityType === "ProjectEntity") {
                                    displayName = details.name || change.entityId
                                    description = "project"
                                    href = "/projects"
                                } else if (change.entityType === "ChangeRequestEntity") {
                                    displayName = details.title || `Change Request #${change.entityId}`
                                    description = "change request"
                                    if (details.project) {
                                        href = `/change-requests/view?id=${change.entityId}&projectId=${details.project}`
                                    }
                                }

                                return (
                                    <div key={change.id} className="flex items-start gap-4">
                                        <div className="mt-1">
                                            <div className={cn(
                                                "p-2 rounded-full",
                                                change.action === 'CREATED' ? 'bg-green-100 text-green-600' :
                                                    change.action === 'DELETED' ? 'bg-red-100 text-red-600' :
                                                        'bg-blue-100 text-blue-600'
                                            )}>
                                                <Flag className="h-3 w-3" />
                                            </div>
                                        </div>
                                        <div className="flex-1 space-y-1">
                                            <div className="text-sm font-medium">
                                                {href ? (
                                                    <Link href={href} className="font-bold text-primary hover:underline">
                                                        {displayName}
                                                    </Link>
                                                ) : (
                                                    <span className="font-bold text-primary">{displayName}</span>
                                                )}
                                                {description && <span className="text-muted-foreground ml-1">{description}</span>}
                                                <span className="text-muted-foreground mx-1">was</span>
                                                <Badge variant="outline" className="text-[10px] py-0 h-4 lowercase">{change.action}</Badge>
                                            </div>
                                            <div className="flex items-center gap-2 text-[10px] text-muted-foreground">
                                                <User className="h-3 w-3" />
                                                <span>{change.changedBy}</span>
                                                <span>•</span>
                                                <span>{formatDistanceToNow(new Date(change.changedAt), { addSuffix: true })}</span>
                                            </div>
                                        </div>
                                    </div>
                                )
                            })}
                            {summary.recentChanges.length === 0 && (
                                <div className="text-center py-8 text-muted-foreground text-sm italic">
                                    No recent activity recorded.
                                </div>
                            )}
                        </div>
                    </CardContent>
                </Card>
            </div>
        </div>
    )
}

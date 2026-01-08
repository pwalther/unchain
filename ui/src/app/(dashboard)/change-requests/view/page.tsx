"use client"

import { useState, Suspense } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { useSearchParams, useRouter } from "next/navigation"
import {
    ClipboardList,
    CheckCircle2,
    Clock,
    XCircle,
    User,
    ArrowLeft,
    Calendar,
    Activity,
    Shield,
    Terminal,
    AlertTriangle,
    Check
} from "lucide-react"
import Link from "next/link"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import { cn } from "@/lib/utils"

import { getChangeRequest, approveChangeRequest, updateChangeRequestState } from "@/features/change-requests/actions"

function ChangeRequestDetailContent() {
    const searchParams = useSearchParams()
    const router = useRouter()
    const queryClient = useQueryClient()

    const projectId = searchParams.get("projectId")
    const idStr = searchParams.get("id")
    const id = idStr ? parseInt(idStr) : 0

    const { data: request, isLoading } = useQuery({
        queryKey: ["change-request", projectId, id],
        queryFn: () => (projectId && id) ? getChangeRequest(projectId, id) : null,
        enabled: !!(projectId && id),
    })

    const approveMutation = useMutation({
        mutationFn: () => approveChangeRequest(projectId!, id),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["change-request", projectId, id] })
            queryClient.invalidateQueries({ queryKey: ["environments"] })
            queryClient.invalidateQueries({ queryKey: ["environment", request?.environment] })
            toast.success("Change request approved")
        },
        onError: (error: any) => toast.error(error.message || "Failed to approve change request")
    })

    const applyMutation = useMutation({
        mutationFn: () => updateChangeRequestState(projectId!, id, "Applied"),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["change-request", projectId, id] })
            queryClient.invalidateQueries({ queryKey: ["environments"] })
            queryClient.invalidateQueries({ queryKey: ["environment", request?.environment] })
            toast.success("Change request applied")
        },
        onError: (error: any) => toast.error(error.message || "Failed to apply changes")
    })

    const rejectMutation = useMutation({
        mutationFn: () => updateChangeRequestState(projectId!, id, "Rejected"),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["change-request", projectId, id] })
            toast.success("Change request rejected")
        },
        onError: (error: any) => toast.error(error.message || "Failed to reject change request")
    })

    if (!projectId || !id) {
        return (
            <div className="flex flex-col items-center justify-center py-20 space-y-4">
                <XCircle className="h-12 w-12 text-muted-foreground/50" />
                <h2 className="text-xl font-semibold">No change request specified</h2>
                <Button variant="outline" onClick={() => router.push("/change-requests")}>Go Back</Button>
            </div>
        )
    }

    if (isLoading) {
        return (
            <div className="space-y-8 animate-in fade-in duration-500">
                <div className="flex items-center gap-4">
                    <Skeleton className="h-10 w-10 rounded-xl" />
                    <div className="space-y-2">
                        <Skeleton className="h-8 w-64" />
                        <Skeleton className="h-4 w-32" />
                    </div>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <Skeleton className="h-40 col-span-2 rounded-2xl" />
                    <Skeleton className="h-40 rounded-2xl" />
                </div>
            </div>
        )
    }

    if (!request) {
        return (
            <div className="flex flex-col items-center justify-center py-20 space-y-4">
                <XCircle className="h-12 w-12 text-muted-foreground/50" />
                <h2 className="text-xl font-semibold">Change Request not found</h2>
                <Button variant="outline" onClick={() => router.push("/change-requests")}>Go Back</Button>
            </div>
        )
    }

    return (
        <div className="space-y-8 animate-in fade-in duration-500">
            <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
                <div className="flex items-start gap-4">
                    <Link href="/change-requests" className="mt-1">
                        <Button variant="ghost" size="icon" className="h-10 w-10 hover:bg-muted-foreground/10 rounded-xl border border-muted-foreground/20 shadow-sm transition-all">
                            <ArrowLeft className="h-5 w-5" />
                        </Button>
                    </Link>
                    <div className="space-y-1">
                        <div className="flex items-center gap-3">
                            <h1 className="text-3xl font-black italic tracking-tight">{request.title}</h1>
                            <Badge
                                variant="secondary"
                                className={cn(
                                    "gap-1 transition-all h-6",
                                    request.state === "Approved" && "bg-emerald-500/10 text-emerald-600 border-emerald-500/20",
                                    request.state === "In review" && "bg-amber-500/10 text-amber-600 border-amber-500/20",
                                    request.state === "Applied" && "bg-blue-500/10 text-blue-600 border-blue-500/20",
                                    request.state === "Rejected" && "bg-rose-500/10 text-rose-600 border-rose-500/20",
                                    request.state === "Draft" && "bg-muted text-muted-foreground border-muted-foreground/20"
                                )}
                            >
                                {request.state === "In review" && <Clock className="h-3 w-3" />}
                                {request.state === "Approved" && <CheckCircle2 className="h-3 w-3" />}
                                {request.state === "Applied" && <CheckCircle2 className="h-3 w-3" />}
                                {request.state === "Rejected" && <XCircle className="h-3 w-3" />}
                                {request.state}
                            </Badge>
                        </div>
                        <p className="text-muted-foreground flex items-center gap-2">
                            <span className="font-bold uppercase tracking-widest text-xs">CR-{request.id}</span>
                            <Separator orientation="vertical" className="h-3" />
                            <span className="text-sm italic">Created by {request.createdBy.username}</span>
                        </p>
                    </div>
                </div>
                <div className="flex items-center gap-3">
                    {request.state === "Draft" && (
                        <Button
                            className="h-11 px-8 bg-black hover:bg-neutral-800 text-white shadow-lg shadow-black/20"
                            onClick={() => updateChangeRequestState(projectId!, id, "In review").then(() => queryClient.invalidateQueries({ queryKey: ["change-request", projectId, id] }))}
                        >
                            <User className="mr-2 h-4 w-4" />
                            Submit for Review
                        </Button>
                    )}
                    {request.state === "In review" && (
                        <>
                            <Button
                                variant="outline"
                                className="h-11 px-6 border-rose-500/20 text-rose-600 hover:bg-rose-500/5"
                                onClick={() => rejectMutation.mutate()}
                                disabled={rejectMutation.isPending}
                            >
                                <XCircle className="mr-2 h-4 w-4" />
                                Reject
                            </Button>
                            <Button
                                className="h-11 px-8 bg-emerald-600 hover:bg-emerald-700 shadow-lg shadow-emerald-500/20"
                                onClick={() => approveMutation.mutate()}
                                disabled={approveMutation.isPending}
                            >
                                <CheckCircle2 className="mr-2 h-4 w-4" />
                                Approve
                            </Button>
                        </>
                    )}
                    {request.state === "Approved" && (
                        <Button
                            className="h-11 px-10 bg-blue-600 hover:bg-blue-700 shadow-lg shadow-blue-500/20"
                            onClick={() => applyMutation.mutate()}
                            disabled={applyMutation.isPending}
                        >
                            <Activity className="mr-2 h-4 w-4" />
                            Apply Changes
                        </Button>
                    )}
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-2 space-y-8">
                    <Card className="overflow-hidden border-muted-foreground/10">
                        <CardHeader className="bg-muted/30 pb-4">
                            <CardTitle className="text-lg flex items-center gap-2">
                                <Terminal className="h-5 w-5 text-primary" />
                                Proposed Changes
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="p-0">
                            {request.features.map((feature, featureIdx) => (
                                <div key={featureIdx} className="border-b last:border-0 border-muted-foreground/10">
                                    <div className="px-6 py-4 bg-muted/10 flex items-center gap-2">
                                        <Shield className="h-4 w-4 text-muted-foreground" />
                                        <span className="font-bold text-sm tracking-tight italic">Feature: {feature.name}</span>
                                    </div>
                                    <div className="divide-y divide-muted-foreground/5">
                                        {feature.changes.map((change, changeIdx) => (
                                            <div key={changeIdx} className="px-6 py-6 group hover:bg-primary/[0.02] transition-colors">
                                                <div className="flex items-start justify-between mb-4">
                                                    <Badge variant="outline" className="text-[10px] uppercase font-bold tracking-widest bg-background">
                                                        {change.action}
                                                    </Badge>
                                                </div>
                                                <div className="bg-muted/50 rounded-xl p-4 font-mono text-sm border border-muted-foreground/5 shadow-inner">
                                                    <pre className="whitespace-pre-wrap break-all opacity-80">
                                                        {JSON.stringify(change.payload, null, 2)}
                                                    </pre>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            ))}
                        </CardContent>
                    </Card>
                </div>

                <div className="space-y-6">
                    <Card className="border-muted-foreground/10">
                        <CardHeader>
                            <CardTitle className="text-lg italic">Context</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-6">
                            <div className="space-y-2">
                                <p className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">Environment</p>
                                <div className="flex items-center gap-3 p-3 rounded-xl bg-card border border-muted-foreground/10">
                                    <Activity className="h-4 w-4 text-primary" />
                                    <span className="text-sm font-semibold uppercase">{request.environment}</span>
                                </div>
                            </div>

                            <Separator />

                            <div className="space-y-2">
                                <p className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">Scheduled For</p>
                                <div className="flex items-center gap-3 p-3 rounded-xl bg-card border border-muted-foreground/10">
                                    <Calendar className="h-4 w-4 text-primary" />
                                    <span className="text-sm font-semibold">
                                        {request.scheduledAt
                                            ? new Date(request.scheduledAt).toLocaleString()
                                            : "Immediately after approval"}
                                    </span>
                                </div>
                            </div>

                            <Separator />

                            <div className="space-y-2">
                                <p className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">Min Approvals Required</p>
                                <div className="flex items-center gap-3 p-3 rounded-xl bg-card border border-muted-foreground/10">
                                    <Shield className="h-4 w-4 text-emerald-500" />
                                    <span className="text-sm font-bold">{request.minApprovals} approval{request.minApprovals !== 1 ? 's' : ''}</span>
                                </div>
                            </div>
                        </CardContent>
                    </Card>

                    {request.state === "Draft" && (
                        <div className="p-6 rounded-2xl bg-muted/50 border border-muted-foreground/10 space-y-4">
                            <div className="flex items-center gap-2 text-muted-foreground">
                                <AlertTriangle className="h-5 w-5" />
                                <p className="font-bold text-sm italic">Status: Draft</p>
                            </div>
                            <p className="text-xs text-muted-foreground leading-relaxed">
                                This change request is still being prepared. You can add more changes to it from the feature pages. Submit it for review when you're ready.
                            </p>
                        </div>
                    )}

                    {request.state === "In review" && (
                        <div className="p-6 rounded-2xl bg-amber-500/5 border border-amber-500/20 space-y-4">
                            <div className="flex items-center gap-2 text-amber-600">
                                <AlertTriangle className="h-5 w-5" />
                                <p className="font-bold text-sm italic italic">Status: Pending Review</p>
                            </div>
                            <p className="text-xs text-amber-600/80 leading-relaxed">
                                This change request requires {request.minApprovals} approval(s) before it can be applied to the {request.environment} environment.
                            </p>
                        </div>
                    )}
                </div>
            </div>
        </div >
    )
}

export default function ChangeRequestDetailPage() {
    return (
        <Suspense fallback={<div className="space-y-8 animate-in fade-in duration-500"><Skeleton className="h-10 w-10 rounded-xl" /><Skeleton className="h-8 w-64" /></div>}>
            <ChangeRequestDetailContent />
        </Suspense>
    )
}

"use client"

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { CheckCircle2, Clock, XCircle, User, ArrowRight, Calendar, Activity } from "lucide-react"
import { getChangeRequests, approveChangeRequest } from "@/features/change-requests/actions"
import { getProjects } from "@/features/projects/actions"
import { Button } from "@/components/ui/button"
import { DataTable } from "@/components/data-table"
import { ColumnDef } from "@tanstack/react-table"
import { ChangeRequest, Project } from "@/types"
import { Badge } from "@/components/ui/badge"
import { toast } from "sonner"
import { cn } from "@/lib/utils"
import Link from "next/link"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { useState, useEffect } from "react"

export default function ChangeRequestsPage() {
    const queryClient = useQueryClient()
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

    const { data: changeRequests = [], isLoading } = useQuery({
        queryKey: ["change-requests", selectedProjectId],
        queryFn: () => getChangeRequests(selectedProjectId),
        enabled: !!selectedProjectId,
    })

    const approveMutation = useMutation({
        mutationFn: (id: number) => approveChangeRequest(selectedProjectId, id),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["change-requests", selectedProjectId] })
            toast.success("Change request approved")
        },
        onError: () => {
            toast.error("Failed to approve change request")
        }
    })

    const columns: ColumnDef<ChangeRequest>[] = [
        {
            accessorKey: "title",
            header: "Title",
            cell: ({ row }) => (
                <div className="flex flex-col">
                    <span className="font-semibold text-foreground italic">{row.getValue("title")}</span>
                    <span className="text-xs text-muted-foreground uppercase tracking-widest font-bold">CR-{row.original.id}</span>
                </div>
            )
        },
        {
            accessorKey: "state",
            header: "Status",
            cell: ({ row }) => {
                const state = row.getValue("state") as string
                return (
                    <Badge
                        variant="secondary"
                        className={cn(
                            "gap-1 transition-all",
                            state === "Approved" && "bg-emerald-500/10 text-emerald-600 border-emerald-500/20",
                            state === "In review" && "bg-amber-500/10 text-amber-600 border-amber-500/20",
                            state === "Applied" && "bg-blue-500/10 text-blue-600 border-blue-500/20",
                            state === "Rejected" && "bg-rose-500/10 text-rose-600 border-rose-500/20",
                            state === "Draft" && "bg-muted text-muted-foreground border-muted-foreground/20"
                        )}
                    >
                        {state === "In review" && <Clock className="h-3 w-3" />}
                        {state === "Approved" && <CheckCircle2 className="h-3 w-3" />}
                        {state === "Applied" && <CheckCircle2 className="h-3 w-3" />}
                        {state === "Rejected" && <XCircle className="h-3 w-3" />}
                        {state === "Draft" && <Activity className="h-3 w-3" />}
                        {state}
                    </Badge>
                )
            }
        },
        {
            accessorKey: "environment",
            header: "Environment",
            cell: ({ row }) => (
                <Badge variant="outline" className="italic font-medium uppercase text-[10px]">
                    {row.getValue("environment")}
                </Badge>
            )
        },
        {
            accessorKey: "createdBy",
            header: "Owner",
            cell: ({ row }) => (
                <div className="flex items-center gap-2">
                    <div className="h-6 w-6 rounded-full bg-primary/10 flex items-center justify-center">
                        <User className="h-3 w-3 text-primary" />
                    </div>
                    <span className="text-sm font-medium">{row.original.createdBy.username}</span>
                </div>
            )
        },
        {
            accessorKey: "scheduledAt",
            header: "Scheduled",
            cell: ({ row }) => {
                const scheduledAt = row.original.scheduledAt
                if (!scheduledAt) return <span className="text-xs text-muted-foreground italic">Immediately</span>
                return (
                    <div className="flex items-center gap-2 text-xs">
                        <Calendar className="h-3 w-3 text-muted-foreground" />
                        <span className="font-medium">{new Date(scheduledAt).toLocaleString()}</span>
                    </div>
                )
            }
        },
        {
            id: "actions",
            cell: ({ row }) => (
                <div className="flex items-center justify-end gap-2">
                    {row.original.state === "In review" && (
                        <Button
                            variant="default"
                            size="sm"
                            className="bg-emerald-600 hover:bg-emerald-700 h-8 shadow-md shadow-emerald-600/20 animate-in fade-in slide-in-from-right-2 duration-300"
                            onClick={() => approveMutation.mutate(row.original.id)}
                            disabled={approveMutation.isPending}
                        >
                            Approve
                        </Button>
                    )}
                    <Link href={`/change-requests/view?projectId=${selectedProjectId}&id=${row.original.id}`}>
                        <Button variant="ghost" size="icon" className="h-8 w-8 hover:bg-primary/5 transition-colors group">
                            <ArrowRight className="h-4 w-4 text-muted-foreground group-hover:text-primary transition-colors" />
                        </Button>
                    </Link>
                </div>
            )
        }
    ]

    return (
        <div className="space-y-8 animate-in fade-in duration-500">
            <div className="flex items-center justify-between">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Change Requests</h1>
                    <p className="text-sm text-muted-foreground">Review and approve changes to protected environments.</p>
                </div>
                <Select value={selectedProjectId} onValueChange={setSelectedProjectId}>
                    <SelectTrigger className="w-[200px] bg-card border-muted-foreground/20">
                        <SelectValue placeholder="Select project" />
                    </SelectTrigger>
                    <SelectContent>
                        {projects.map((p: Project) => (
                            <SelectItem key={p.id} value={p.id}>{p.name}</SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
                <div className="p-4 rounded-2xl bg-card border border-muted-foreground/10 space-y-2">
                    <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">Pending Review</p>
                    <p className="text-3xl font-black">{changeRequests.filter(r => r.state === "In review").length}</p>
                </div>
                <div className="p-4 rounded-2xl bg-card border border-muted-foreground/10 space-y-2">
                    <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">Approved</p>
                    <p className="text-3xl font-black">{changeRequests.filter(r => r.state === "Approved").length}</p>
                </div>
                <div className="p-4 rounded-2xl bg-card border border-muted-foreground/10 space-y-2">
                    <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">Applied</p>
                    <p className="text-3xl font-black">{changeRequests.filter(r => r.state === "Applied").length}</p>
                </div>
                <div className="p-4 rounded-2xl bg-card border border-muted-foreground/10 space-y-2">
                    <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">Rejected</p>
                    <p className="text-3xl font-black">{changeRequests.filter(r => r.state === "Rejected").length}</p>
                </div>
            </div>

            <DataTable
                columns={columns}
                data={changeRequests}
                isLoading={isLoading}
                searchKey="title"
            />
        </div>
    )
}


"use client"

import { useState, useEffect, Suspense } from "react"
import Link from "next/link"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { Plus, Trash, AlertTriangle } from "lucide-react"
import { ColumnDef } from "@tanstack/react-table"
import { toast } from "sonner"
import { useSearchParams } from "next/navigation"

import { Button } from "@/components/ui/button"
import { DataTable } from "@/components/data-table"
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { getEnvironments } from "@/features/environments/actions"
import { createChangeRequest } from "@/features/change-requests/actions"
import { getFeatures, deleteFeature } from "@/features/features/actions"
import { getProjects } from "@/features/projects/actions"
import { Feature, Project, Environment } from "@/types"
import { Badge } from "@/components/ui/badge"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { CreateFeatureDrawer } from "@/features/features/components/create-feature-drawer"

export default function FeaturesPage() {
    return (
        <Suspense fallback={<div className="p-8 text-center text-muted-foreground animate-pulse">Loading project data...</div>}>
            <FeaturesPageContent />
        </Suspense>
    )
}

function FeaturesPageContent() {
    const searchParams = useSearchParams()
    const urlProjectId = searchParams.get("projectId")

    const queryClient = useQueryClient()
    const [selectedProjectId, setSelectedProjectId] = useState<string>("")
    const [drawerOpen, setDrawerOpen] = useState(false)

    const { data: projects = [], isLoading: projectsLoading } = useQuery({
        queryKey: ["projects"],
        queryFn: () => getProjects(),
    })

    // Set initial project when projects load or URL param changes
    useEffect(() => {
        if (urlProjectId) {
            setSelectedProjectId(urlProjectId)
        } else if (!selectedProjectId && projects.length > 0) {
            setSelectedProjectId(projects[0].id)
        }
    }, [projects, selectedProjectId, urlProjectId])

    const { data: features = [], isLoading, isError, error, refetch } = useQuery({
        queryKey: ["features", selectedProjectId],
        queryFn: () => getFeatures(selectedProjectId),
        enabled: !!selectedProjectId,
    })

    // If features are empty but projects aren't, maybe we're looking at a project with no features.
    // That's fine. But if there's an error, we should show it.
    useEffect(() => {
        if (isError) {
            toast.error(`Failed to load features: ${(error as any)?.message || "Unknown error"}`)
        }
    }, [isError, error])


    const [archiveConfirmOpen, setArchiveConfirmOpen] = useState(false)
    const [featureToArchive, setFeatureToArchive] = useState<Feature | null>(null)

    const { data: envs = [] } = useQuery<Environment[]>({
        queryKey: ["environments"],
        queryFn: getEnvironments,
    })

    const getProtectionInfo = (feature: Feature | null) => {
        if (!feature || !feature.environments) return { isProtected: false, protectedEnv: null }

        const protectedEnvs = envs.filter(e => e.requiredApprovals && e.requiredApprovals > 0)
        const activeInProtectedEnv = feature.environments.find(fe => {
            const envConfig = protectedEnvs.find(e => e.name === fe.name)
            if (!envConfig) return false
            // Enabled OR has strategies
            return fe.enabled || (fe.strategies && fe.strategies.length > 0)
        })

        return {
            isProtected: !!activeInProtectedEnv,
            protectedEnv: activeInProtectedEnv?.name || protectedEnvs[0]?.name || null
        }
    }

    const { isProtected, protectedEnv } = getProtectionInfo(featureToArchive)

    const archiveMutation = useMutation({
        mutationFn: async (feature: Feature) => {
            const { isProtected, protectedEnv } = getProtectionInfo(feature)
            if (isProtected && protectedEnv) {
                return createChangeRequest(selectedProjectId, {
                    title: `Archive feature: ${feature.name}`,
                    description: `Automatic change request for archiving protected feature flag ${feature.name}`,
                    environment: protectedEnv,
                    changes: [
                        {
                            type: "DELETE_FEATURE",
                            featureName: feature.name
                        }
                    ]
                })
            } else {
                return deleteFeature(selectedProjectId, feature.name)
            }
        },
        onSuccess: (data) => {
            if (typeof data === 'object' && 'id' in data) {
                toast.success("Change request created for flag archival")
            } else {
                toast.success("Flag successfully archived")
            }
            queryClient.invalidateQueries({ queryKey: ["features", selectedProjectId] })
            setArchiveConfirmOpen(false)
        },
        onError: (error: any) => {
            toast.error(`Error: ${error.message || "Failed to archive flag"}`)
        }
    })

    const columns: ColumnDef<Feature>[] = [
        {
            accessorKey: "name",
            header: "Name",
            cell: ({ row }) => (
                <div className="flex flex-col">
                    <Link
                        href={`/features/view?featureName=${row.original.name}&projectId=${selectedProjectId}`}
                        className="font-semibold text-primary hover:underline"
                    >
                        {row.original.name}
                    </Link>
                    <span className="text-[10px] text-muted-foreground font-mono">{row.original.type}</span>
                </div>
            ),
        },
        {
            accessorKey: "environments",
            header: "Environments",
            cell: ({ row }) => (
                <div className="flex flex-wrap gap-1">
                    {row.original.environments?.map((env) => (
                        <Badge
                            key={env.name}
                            variant={env.enabled ? "default" : "outline"}
                            className="text-[10px] py-0 h-4"
                        >
                            {env.name}
                        </Badge>
                    ))}
                </div>
            ),
        },
        {
            accessorKey: "stale",
            header: "Status",
            cell: ({ row }) => (
                row.original.stale ? (
                    <Badge variant="destructive" className="flex items-center gap-1 text-[10px] py-0 h-4">
                        <AlertTriangle className="h-2 w-2" /> Stale
                    </Badge>
                ) : (
                    <Badge variant="secondary" className="text-[10px] py-0 h-4">Active</Badge>
                )
            ),
        },
        {
            accessorKey: "lastSeenAt",
            header: "Last Seen",
            cell: ({ row }) => (
                <span className="text-[10px] text-muted-foreground">
                    {row.original.lastSeenAt ? new Date(row.original.lastSeenAt).toLocaleString() : "Never"}
                </span>
            ),
        },
        {
            id: "actions",
            cell: ({ row }) => (
                <div className="flex justify-end gap-2">
                    <Link href={`/features/view?featureName=${row.original.name}&projectId=${selectedProjectId}`}>
                        <Button variant="ghost" size="sm" className="h-8 text-xs">View</Button>
                    </Link>
                    <Button
                        variant="ghost"
                        size="sm"
                        className="h-8 w-8 p-0 text-destructive hover:text-destructive hover:bg-destructive/10"
                        onClick={() => {
                            setFeatureToArchive(row.original)
                            setArchiveConfirmOpen(true)
                        }}
                    >
                        <Trash className="h-4 w-4" />
                    </Button>
                </div>
            ),
        },
    ]

    return (
        <div className="flex flex-col gap-8 p-8 max-w-7xl mx-auto animate-in fade-in slide-in-from-bottom-4 duration-700">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-3xl font-bold tracking-tight">Feature Flags</h1>
                    <p className="text-muted-foreground">
                        Manage your feature toggles and their release strategies.
                    </p>
                </div>

                <div className="flex items-center gap-3">
                    <div className="flex items-center gap-2 bg-muted/30 p-1.5 rounded-lg border border-muted/50">
                        <span className="text-xs font-semibold text-muted-foreground px-2">Project:</span>
                        <Select value={selectedProjectId} onValueChange={setSelectedProjectId}>
                            <SelectTrigger className="w-[180px] h-9 bg-background border-none shadow-none focus:ring-0">
                                <SelectValue placeholder="Select project" />
                            </SelectTrigger>
                            <SelectContent>
                                {projects.map((p) => (
                                    <SelectItem key={p.id} value={p.id}>{p.name}</SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    <Button onClick={() => setDrawerOpen(true)} className="h-10 gap-2 shadow-sm">
                        <Plus className="h-4 w-4" /> Create Flag
                    </Button>
                </div>
            </div>

            <DataTable columns={columns} data={features} isLoading={isLoading} />

            <CreateFeatureDrawer
                projectId={selectedProjectId}
                projects={projects}
                open={drawerOpen}
                onOpenChange={setDrawerOpen}
                onSuccess={() => {
                    queryClient.invalidateQueries({ queryKey: ["features", selectedProjectId] })
                    queryClient.invalidateQueries({ queryKey: ["dashboard-summary"] })
                    queryClient.invalidateQueries({ queryKey: ["projects"] })
                }}
            />

            <AlertDialog open={archiveConfirmOpen} onOpenChange={setArchiveConfirmOpen}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>{isProtected ? "Request Flag Archival?" : "Archive Flag?"}</AlertDialogTitle>
                        <AlertDialogDescription>
                            {isProtected ? (
                                <>
                                    The flag <strong>{featureToArchive?.name}</strong> is currently active or enabled in a protected environment (<strong>{protectedEnv}</strong>).
                                    Archiving it requires a <strong>Change Request</strong> and manual approval.
                                </>
                            ) : (
                                <>
                                    Are you sure you want to archive <strong>{featureToArchive?.name}</strong>?
                                    This will disable the flag in all environments and hide it from the main list.
                                </>
                            )}
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction
                            onClick={() => featureToArchive && archiveMutation.mutate(featureToArchive)}
                            className={isProtected ? "" : "bg-destructive text-destructive-foreground hover:bg-destructive/90"}
                        >
                            {isProtected ? "Create Change Request" : "Archive Flag"}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    )
}

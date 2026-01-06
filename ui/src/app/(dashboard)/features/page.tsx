"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { Plus, Trash } from "lucide-react"
import { ColumnDef } from "@tanstack/react-table"
import { toast } from "sonner"

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
    const queryClient = useQueryClient()
    const [selectedProjectId, setSelectedProjectId] = useState<string>("")
    const [drawerOpen, setDrawerOpen] = useState(false)

    const { data: projects = [], isLoading: projectsLoading } = useQuery({
        queryKey: ["projects"],
        queryFn: () => getProjects(),
    })

    // Set initial project when projects load
    useEffect(() => {
        if (!selectedProjectId && projects.length > 0) {
            setSelectedProjectId(projects[0].id)
        }
    }, [projects, selectedProjectId])

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
                    environment: protectedEnv,
                    changes: [{
                        feature: feature.name,
                        action: "archive-feature",
                        payload: {}
                    }]
                })
            } else {
                return deleteFeature(selectedProjectId, feature.name)
            }
        },
        onSuccess: (_, feature) => {
            const { isProtected } = getProtectionInfo(feature)
            if (isProtected) {
                toast.success(`Change request created for archiving ${feature.name}`)
            } else {
                toast.success(`Feature ${feature.name} archived`)
            }
            queryClient.invalidateQueries({ queryKey: ["environments"] })
            queryClient.invalidateQueries({ queryKey: ["change-requests", selectedProjectId] })
            refetch()
            setArchiveConfirmOpen(false)
        },
        onError: (error: any) => {
            toast.error(error.message || "Failed to archive feature")
        }
    })

    function handleDeleteClick(feature: Feature) {
        setFeatureToArchive(feature)
        setArchiveConfirmOpen(true)
    }

    const columns: ColumnDef<Feature>[] = [
        {
            accessorKey: "name",
            header: "Name",
            cell: ({ row }) => (
                <div className="flex flex-col">
                    <Link
                        href={`/features/view?projectId=${selectedProjectId}&featureName=${row.getValue("name")}`}
                        className="font-medium text-foreground hover:underline underline-offset-4"
                    >
                        {row.getValue("name")}
                    </Link>
                    <span className="text-xs text-muted-foreground">{row.original.type}</span>
                </div>
            ),
        },
        {
            id: "environments",
            header: "Environments",
            cell: ({ row }) => {
                const enabledEnvironments = row.original.environments?.filter(e => e.enabled) || []
                return (
                    <div className="flex flex-wrap gap-1.5">
                        {enabledEnvironments.length > 0 ? (
                            enabledEnvironments.map((env) => (
                                <Badge
                                    key={env.name}
                                    variant="outline"
                                    className="px-2 py-0 h-5 text-[10px] font-bold bg-green-500/10 text-green-600 border-green-500/20 gap-1 capitalize"
                                >
                                    <div className="h-1 w-1 rounded-full bg-green-500 shadow-[0_0_4px_rgba(34,197,94,0.4)]" />
                                    {env.name}
                                </Badge>
                            ))
                        ) : (
                            <span className="text-[10px] font-bold text-muted-foreground/50 uppercase tracking-widest px-1">Inactive</span>
                        )}
                    </div>
                )
            }
        },
        {
            accessorKey: "stale",
            header: "Status",
            cell: ({ row }) => (
                <Badge variant={row.getValue("stale") ? "outline" : "secondary"}>
                    {row.getValue("stale") ? "Stale" : "Active"}
                </Badge>
            ),
        },
        {
            accessorKey: "createdAt",
            header: "Created",
            cell: ({ row }) => (
                <span className="text-muted-foreground">
                    {new Date(row.getValue("createdAt")).toLocaleDateString()}
                </span>
            ),
        },
        {
            id: "actions",
            cell: ({ row }) => {
                const feature = row.original
                return (
                    <div className="flex items-center justify-end gap-2">
                        <Link href={`/features/view?projectId=${selectedProjectId}&featureName=${feature.name}`}>
                            <Button variant="ghost" size="sm" className="h-8 gap-1.5 text-muted-foreground hover:text-primary transition-colors">
                                View
                            </Button>
                        </Link>
                        <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 gap-1.5 text-muted-foreground hover:text-destructive hover:bg-destructive/5 transition-all"
                            onClick={() => handleDeleteClick(feature)}
                        >
                            <Trash className="h-3.5 w-3.5" />
                            Archive
                        </Button>
                    </div>
                )
            },
        },
    ]

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">Features</h1>
                    <p className="text-sm text-muted-foreground">
                        Monitor and control your feature toggles.
                    </p>
                </div>
                <div className="flex items-center gap-3">
                    <Select value={selectedProjectId} onValueChange={setSelectedProjectId}>
                        <SelectTrigger className="w-[180px]">
                            <SelectValue placeholder="Select project" />
                        </SelectTrigger>
                        <SelectContent>
                            {projects.map((p: Project) => (
                                <SelectItem key={p.id} value={p.id}>{p.name}</SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                    <Button onClick={() => setDrawerOpen(true)}>
                        <Plus className="mr-2 h-4 w-4" />
                        New Feature
                    </Button>
                </div>
            </div>

            <DataTable
                columns={columns}
                data={features}
                searchKey="name"
                isLoading={isLoading}
            />

            <CreateFeatureDrawer
                open={drawerOpen}
                onOpenChange={setDrawerOpen}
                projectId={selectedProjectId}
                projects={projects}
            />

            <AlertDialog open={archiveConfirmOpen} onOpenChange={setArchiveConfirmOpen}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>
                        <AlertDialogDescription>
                            {isProtected ? (
                                <>
                                    Archiving <span className="font-bold text-foreground italic">{featureToArchive?.name}</span> requires a Change Request because it is active in protected environment <span className="font-bold text-amber-600 uppercase">({protectedEnv})</span>.
                                    <br /><br />
                                    This will create a draft Change Request that must be approved before the feature is archived.
                                </>
                            ) : (
                                <>
                                    This will permanently archive the feature <span className="font-bold text-foreground italic">{featureToArchive?.name}</span> and remove it from all environments. This action cannot be undone.
                                </>
                            )}
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction
                            onClick={() => featureToArchive && archiveMutation.mutate(featureToArchive)}
                            className={isProtected ? "bg-amber-600 hover:bg-amber-700" : "bg-destructive hover:bg-destructive/90"}
                        >
                            {isProtected ? "Create Change Request" : "Archive Feature"}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    )
}

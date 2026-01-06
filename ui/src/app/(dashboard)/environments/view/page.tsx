"use client"

import { useState, Suspense } from "react"
import { useSearchParams, useRouter } from "next/navigation"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import {
    ChevronLeft,
    Settings,
    Shield,
    Zap,
    Database,
    CheckCircle2,
    AlertCircle,
    Activity,
    Info,
    Calendar,
    ArrowRight
} from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle
} from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { getEnvironment, updateEnvironment } from "@/features/environments/actions"
import { EditEnvironmentDrawer } from "@/features/environments/components/edit-environment-drawer"
import { Skeleton } from "@/components/ui/skeleton"

function EnvironmentDetailsContent() {
    const searchParams = useSearchParams()
    const router = useRouter()
    const queryClient = useQueryClient()
    const name = searchParams.get("name")
    const [isEditDrawerOpen, setIsEditDrawerOpen] = useState(false)

    const { data: environment, isLoading, isError } = useQuery({
        queryKey: ["environment", name],
        queryFn: () => name ? getEnvironment(name) : null,
        enabled: !!name,
    })

    const updateEnvMutation = useMutation({
        mutationFn: (data: { type: string }) => updateEnvironment(name!, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["environment", name] })
            queryClient.invalidateQueries({ queryKey: ["environments"] })
            toast.success("Environment updated successfully")
            setIsEditDrawerOpen(false)
        },
        onError: () => {
            toast.error("Failed to update environment")
        }
    })

    if (!name) {
        return (
            <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
                <AlertCircle className="h-12 w-12 text-destructive opacity-50" />
                <h2 className="text-2xl font-bold tracking-tight">No Environment Specified</h2>
                <Button variant="outline" onClick={() => router.push("/environments")}>
                    Back to Environments
                </Button>
            </div>
        )
    }

    if (isLoading) {
        return (
            <div className="p-8 space-y-8 animate-pulse">
                <div className="flex items-center gap-4">
                    <Skeleton className="h-8 w-8 rounded-full" />
                    <Skeleton className="h-10 w-64" />
                </div>
                <div className="grid grid-cols-3 gap-6">
                    <Skeleton className="h-32 rounded-xl" />
                    <Skeleton className="h-32 rounded-xl" />
                    <Skeleton className="h-32 rounded-xl" />
                </div>
                <Skeleton className="h-96 rounded-xl w-full" />
            </div>
        )
    }

    if (isError || !environment) {
        return (
            <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
                <AlertCircle className="h-12 w-12 text-destructive opacity-50" />
                <h2 className="text-2xl font-bold tracking-tight">Environment Not Found</h2>
                <p className="text-muted-foreground">The environment you're looking for doesn't exist or you don't have access.</p>
                <Button variant="outline" onClick={() => router.push("/environments")}>
                    Back to Environments
                </Button>
            </div>
        )
    }

    return (
        <div className="p-8 space-y-8 max-w-7xl mx-auto">
            {/* Header */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 pb-2">
                <div className="space-y-1">
                    <Button
                        variant="ghost"
                        size="sm"
                        className="pl-0 gap-1 text-muted-foreground hover:text-primary transition-colors"
                        onClick={() => router.push("/environments")}
                    >
                        <ChevronLeft className="h-4 w-4" />
                        Back to Environments
                    </Button>
                    <div className="flex items-center gap-3">
                        <div className="p-2.5 bg-primary/10 rounded-xl">
                            <Database className="h-6 w-6 text-primary" />
                        </div>
                        <div>
                            <h1 className="text-3xl font-bold tracking-tight flex items-center gap-3 italic">
                                {environment.name}
                                {environment.protected && (
                                    <Badge variant="secondary" className="gap-1 font-bold text-[10px] uppercase tracking-wider bg-amber-500/10 text-amber-600 border-amber-500/20">
                                        <Shield className="h-3 w-3" />
                                        Protected
                                    </Badge>
                                )}
                            </h1>
                            <p className="text-muted-foreground mt-1">Manage environment configuration and settings.</p>
                        </div>
                    </div>
                </div>
                <div className="flex items-center gap-3 self-end md:self-center">
                    <Button
                        onClick={() => setIsEditDrawerOpen(true)}
                        className="gap-2 bg-primary hover:bg-primary/90 shadow-lg shadow-primary/20"
                    >
                        <Settings className="h-4 w-4" />
                        Edit Settings
                    </Button>
                </div>
            </div>

            {/* Stats / Overview Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <Card className="border-none bg-primary/5 shadow-none group hover:bg-primary/10 transition-colors cursor-default overflow-hidden relative">
                    <CardHeader className="pb-2">
                        <CardDescription className="text-primary/70 font-semibold uppercase text-[10px] tracking-widest">Environment Type</CardDescription>
                        <CardTitle className="text-3xl flex items-center gap-3">
                            <Zap className="h-6 w-6 text-amber-500" />
                            <span className="capitalize">{environment.type}</span>
                        </CardTitle>
                    </CardHeader>
                    <div className="absolute -right-4 -bottom-4 opacity-5 group-hover:opacity-10 transition-opacity">
                        <Zap className="h-24 w-24 transform -rotate-12" />
                    </div>
                </Card>

                <Card className="border-none bg-blue-500/5 shadow-none group hover:bg-blue-500/10 transition-colors cursor-default overflow-hidden relative">
                    <CardHeader className="pb-2">
                        <CardDescription className="text-blue-500/70 font-semibold uppercase text-[10px] tracking-widest">Active Toggles</CardDescription>
                        <CardTitle className="text-3xl flex items-center gap-3">
                            <CheckCircle2 className="h-6 w-6 text-blue-500" />
                            {environment.enabledToggleCount || 0}
                        </CardTitle>
                    </CardHeader>
                    <div className="absolute -right-4 -bottom-4 opacity-5 group-hover:opacity-10 transition-opacity">
                        <CheckCircle2 className="h-24 w-24 transform -rotate-12" />
                    </div>
                </Card>

                <Card className="border-none bg-purple-500/5 shadow-none group hover:bg-purple-500/10 transition-colors cursor-default overflow-hidden relative">
                    <CardHeader className="pb-2">
                        <CardDescription className="text-purple-500/70 font-semibold uppercase text-[10px] tracking-widest">Project Usage</CardDescription>
                        <CardTitle className="text-3xl flex items-center gap-3">
                            <Activity className="h-6 w-6 text-purple-500" />
                            {environment.projectCount || 0}
                        </CardTitle>
                    </CardHeader>
                    <div className="absolute -right-4 -bottom-4 opacity-5 group-hover:opacity-10 transition-opacity">
                        <Activity className="h-24 w-24 transform -rotate-12" />
                    </div>
                </Card>
            </div>

            {/* Content Tabs / Main Info */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-2 space-y-6">
                    <Card className="border-muted-foreground/10 overflow-hidden bg-card/50 backdrop-blur-sm">
                        <CardHeader className="border-b border-muted-foreground/10 py-4">
                            <div className="flex items-center gap-2">
                                <Info className="h-4 w-4 text-muted-foreground" />
                                <CardTitle className="text-lg font-bold italic">Information</CardTitle>
                            </div>
                        </CardHeader>
                        <CardContent className="p-0">
                            <div className="divide-y divide-muted-foreground/10">
                                <div className="p-4 flex items-center justify-between group hover:bg-muted/30 transition-colors">
                                    <div className="space-y-1">
                                        <p className="text-[10px] uppercase font-bold text-muted-foreground tracking-widest">Status</p>
                                        <div className="flex items-center gap-2">
                                            <div className={`h-2 w-2 rounded-full ${environment.enabled ? 'bg-emerald-500' : 'bg-rose-500'} animate-pulse`} />
                                            <p className="font-semibold">{environment.enabled ? 'Active' : 'Disabled'}</p>
                                        </div>
                                    </div>
                                    <ArrowRight className="h-4 w-4 text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity" />
                                </div>
                                <div className="p-4 flex items-center justify-between group hover:bg-muted/30 transition-colors">
                                    <div className="space-y-1">
                                        <p className="text-[10px] uppercase font-bold text-muted-foreground tracking-widest">Required Approvals</p>
                                        <p className="font-semibold">{environment.requiredApprovals || 0} required</p>
                                    </div>
                                    <ArrowRight className="h-4 w-4 text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity" />
                                </div>
                            </div>
                        </CardContent>
                    </Card>

                    <Card className="border-muted-foreground/10 bg-card/50 backdrop-blur-sm">
                        <CardHeader>
                            <CardTitle className="text-lg font-bold italic">Danger Zone</CardTitle>
                            <CardDescription>Actions that cannot be undone.</CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            <div className="flex items-center justify-between p-4 rounded-xl border border-destructive/20 bg-destructive/5">
                                <div className="space-y-1">
                                    <p className="font-bold">Delete Environment</p>
                                    <p className="text-sm text-muted-foreground">This will permanently delete the environment and all associated data.</p>
                                </div>
                                <Button variant="destructive" disabled={environment.protected}>Delete</Button>
                            </div>
                        </CardContent>
                    </Card>
                </div>

                <div className="space-y-6">
                    <Card className="border-muted-foreground/10 bg-card/50 backdrop-blur-sm">
                        <CardHeader>
                            <div className="flex items-center gap-2">
                                <Calendar className="h-4 w-4 text-muted-foreground" />
                                <CardTitle className="text-lg font-bold italic">Timeline</CardTitle>
                            </div>
                        </CardHeader>
                        <CardContent className="space-y-6">
                            <div className="relative pl-6 border-l border-muted-foreground/10 space-y-8">
                                <div className="relative">
                                    <div className="absolute -left-[30px] top-1 h-4 w-4 rounded-full border-2 border-primary bg-background" />
                                    <p className="text-xs font-bold uppercase text-primary/70 mb-1">Created</p>
                                    <p className="text-sm font-semibold">Environment initialized</p>
                                </div>
                                <div className="relative">
                                    <div className="absolute -left-[30px] top-1 h-4 w-4 rounded-full border-2 border-muted-foreground/20 bg-background" />
                                    <p className="text-xs font-bold uppercase text-muted-foreground mb-1">Configuration</p>
                                    <p className="text-sm font-semibold text-muted-foreground">Type set to <span className="text-primary italic">"{environment.type}"</span></p>
                                </div>
                            </div>
                        </CardContent>
                    </Card>
                </div>
            </div>

            <EditEnvironmentDrawer
                open={isEditDrawerOpen}
                onOpenChange={setIsEditDrawerOpen}
                environment={environment}
                onSubmit={(data: { type: string }) => updateEnvMutation.mutate(data)}
                isLoading={updateEnvMutation.isPending}
            />
        </div>
    )
}

export default function EnvironmentDetailsPage() {
    return (
        <Suspense fallback={<div className="p-8 space-y-8 animate-pulse"><Skeleton className="h-10 w-64" /></div>}>
            <EnvironmentDetailsContent />
        </Suspense>
    )
}

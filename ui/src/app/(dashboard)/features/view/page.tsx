"use client"

import { useState, useEffect, Suspense, useCallback } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { useSearchParams, useRouter } from "next/navigation"
import { ArrowLeft, Plus, Power, Settings, Trash, Info, Check, Cloud, Package, User, Terminal, Shield, Filter, Activity, Clock, ChevronRight, AlertTriangle, XCircle, Pencil } from "lucide-react"
import { toast } from "sonner"
import { apiFetch, ApiError } from "@/lib/api"
import Link from "next/link"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Badge } from "@/components/ui/badge"
import { Switch } from "@/components/ui/switch"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
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
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { Textarea } from "@/components/ui/textarea"

import { getFeature, toggleFeature } from "@/features/features/actions"
import { getEnvironments, updateEnvironment } from "@/features/environments/actions"
import { getStrategies, createStrategyForFeature, deleteStrategyFromFeature, updateStrategyForFeature, getStrategyForFeature } from "@/features/strategies/actions"
import { createChangeRequest, getChangeRequests, addChangesToRequest } from "@/features/change-requests/actions"
import { Feature, Strategy, StrategyDefinition, Environment, Variant } from "@/types"
import { StrategyDialog } from "@/features/features/components/strategy-dialog"
import { EditEnvironmentDrawer } from "@/features/environments/components/edit-environment-drawer"
import { FeatureVariantsDialog } from "@/features/features/components/feature-variants-dialog"
import { getContexts } from "@/features/contexts/actions"

function FeatureDetailsContent() {
    const searchParams = useSearchParams()
    const router = useRouter()
    const queryClient = useQueryClient()

    const projectId = searchParams.get("projectId")
    const featureName = searchParams.get("featureName")

    const [activeTab, setActiveTab] = useState<string>("")
    const [addStrategyEnv, setAddStrategyEnv] = useState<string | null>(null)
    const [editStrategy, setEditStrategy] = useState<{ env: string, strategy: Strategy } | null>(null)
    const [editEnv, setEditEnv] = useState<Environment | null>(null)
    const [pendingProductionEnable, setPendingProductionEnable] = useState<string | null>(null)
    const [crData, setCrData] = useState<{ env: string, changes: { feature: string, action: string, payload: unknown }[] } | null>(null)
    const [crTitle, setCrTitle] = useState("")
    const [crScheduledAt, setCrScheduledAt] = useState("")
    const [existingDraftId, setExistingDraftId] = useState<number | null>(null)
    const [archiveConfirmOpen, setArchiveConfirmOpen] = useState(false)
    const [shimmerEnv, setShimmerEnv] = useState<string | null>(null)
    const [editDescriptionOpen, setEditDescriptionOpen] = useState(false)
    const [editDescriptionValue, setEditDescriptionValue] = useState("")
    const [editVariantsOpen, setEditVariantsOpen] = useState(false)

    const { data: feature, isLoading: featureLoading } = useQuery({
        queryKey: ["feature", projectId, featureName],
        queryFn: () => (projectId && featureName) ? getFeature(projectId, featureName) : null,
        enabled: !!(projectId && featureName),
    })

    const { data: allEnvironments = [], isLoading: environmentsLoading } = useQuery({
        queryKey: ["environments"],
        queryFn: getEnvironments,
    })

    const { data: strategyDefinitions = [] } = useQuery({
        queryKey: ["strategies", "definitions"],
        queryFn: getStrategies,
    })

    const { data: featureChangeRequests = [] } = useQuery({
        queryKey: ["change-requests", projectId],
        queryFn: () => projectId ? getChangeRequests(projectId) : [],
        enabled: !!projectId,
    })

    const { data: fetchedStrategy, isLoading: strategyLoading } = useQuery({
        queryKey: ["strategy", projectId, featureName, editStrategy?.env, editStrategy?.strategy?.id],
        queryFn: () => (projectId && featureName && editStrategy) ? getStrategyForFeature(projectId, featureName, editStrategy.env, editStrategy.strategy.id!) : null,
        enabled: !!(projectId && featureName && editStrategy?.strategy?.id),
    })

    const { data: contextFields = [] } = useQuery({
        queryKey: ["context-fields"],
        queryFn: getContexts,
    })

    useEffect(() => {
        if (!activeTab && allEnvironments.length > 0) {
            setActiveTab(allEnvironments[0].name)
        }
    }, [allEnvironments, activeTab])

    const updateFeatureMutation = useMutation({
        mutationFn: (data: Partial<Feature>) => apiFetch(`/projects/${projectId}/features/${featureName}`, {
            method: 'PATCH',
            body: JSON.stringify(data)
        }),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["feature", projectId, featureName] })
            toast.success("Feature updated")
            setEditDescriptionOpen(false)
        },
        onError: () => toast.error("Failed to update feature")
    })

    const triggerExcitement = useCallback((envName: string) => {
        setShimmerEnv(envName)
        // Reset shimmer after animation completes (0.8s as defined in CSS)
        setTimeout(() => setShimmerEnv(null), 1000)
    }, [])

    useEffect(() => {
        if (crData) {
            const drafts = featureChangeRequests.filter(cr => cr.state === "Draft" && cr.environment === crData.env)
            if (drafts.length > 0) {
                setExistingDraftId(drafts[0].id)
            } else {
                setExistingDraftId(null)
            }
        }
    }, [crData, featureChangeRequests])

    const toggleMutation = useMutation({
        mutationFn: ({ env, enabled }: { env: string; enabled: boolean }) =>
            toggleFeature(projectId!, featureName!, env, enabled),
        onSuccess: (_, variables) => {
            queryClient.invalidateQueries({ queryKey: ["feature", projectId, featureName] })
            queryClient.invalidateQueries({ queryKey: ["environments"] })
            queryClient.invalidateQueries({ queryKey: ["environment", variables.env] })
            queryClient.invalidateQueries({ queryKey: ["dashboard-summary"] })
            toast.success("Feature environment status updated")

            if (variables.enabled) {
                const env = allEnvironments.find((e) => e.name === variables.env)
                if (env && !env.protected) {
                    triggerExcitement(variables.env)
                }
            }
        },
        onError: () => toast.error("Failed to update feature status")
    })

    const crMutation = useMutation({
        mutationFn: (data: { title?: string; environment: string; changes: { feature: string; action: string; payload: unknown }[]; scheduledAt?: string; id?: number }) => {
            if (data.id) {
                return addChangesToRequest(projectId!, data.id, { changes: data.changes })
            }
            return createChangeRequest(projectId!, {
                title: data.title || "New Change Request",
                environment: data.environment,
                changes: data.changes,
                scheduledAt: data.scheduledAt
            })
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["change-requests", projectId] })
            toast.success(existingDraftId ? "Changes added to draft" : "Change request created")
            setCrData(null)
            setCrTitle("")
            setCrScheduledAt("")
            setExistingDraftId(null)
            setArchiveConfirmOpen(false)
        },
        onError: () => toast.error("Failed to process change request")
    })

    const addStrategyMutation = useMutation({
        mutationFn: ({ env, strategy }: { env: string; strategy: Partial<Strategy> }) =>
            createStrategyForFeature(projectId!, featureName!, env, strategy),
        onSuccess: (_, variables) => {
            queryClient.invalidateQueries({ queryKey: ["feature", projectId, featureName] })
            queryClient.invalidateQueries({ queryKey: ["environments"] })
            queryClient.invalidateQueries({ queryKey: ["environment", variables.env] })
            toast.success("Strategy added")
            setAddStrategyEnv(null)
        },
        onError: () => toast.error("Failed to add strategy")
    })

    const updateStrategyMutation = useMutation({
        mutationFn: ({ env, strategyId, strategy }: { env: string; strategyId: string; strategy: Partial<Strategy> }) =>
            updateStrategyForFeature(projectId!, featureName!, env, strategyId, strategy),
        onSuccess: (_, variables) => {
            queryClient.invalidateQueries({ queryKey: ["feature", projectId, featureName] })
            queryClient.invalidateQueries({ queryKey: ["environments"] })
            queryClient.invalidateQueries({ queryKey: ["environment", variables.env] })
            toast.success("Strategy updated")
            setEditStrategy(null)
        },
        onError: () => toast.error("Failed to update strategy")
    })

    const removeStrategyMutation = useMutation({
        mutationFn: ({ env, strategyId }: { env: string; strategyId: string }) =>
            deleteStrategyFromFeature(projectId!, featureName!, env, strategyId),
        onSuccess: (_, variables) => {
            queryClient.invalidateQueries({ queryKey: ["feature", projectId, featureName] })
            queryClient.invalidateQueries({ queryKey: ["environments"] })
            queryClient.invalidateQueries({ queryKey: ["environment", variables.env] })
            toast.success("Strategy removed")
        },
        onError: () => toast.error("Failed to remove strategy")
    })

    const updateEnvMutation = useMutation({
        mutationFn: (data: Partial<Environment>) => updateEnvironment(editEnv!.name, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["environments"] })
            queryClient.invalidateQueries({ queryKey: ["feature", projectId, featureName] })
            toast.success("Environment updated")
            setEditEnv(null)
        },
        onError: () => toast.error("Failed to update environment")
    })

    const updateVariantsMutation = useMutation({
        mutationFn: (variants: Variant[]) => apiFetch(`/projects/${projectId}/features/${featureName}`, {
            method: 'PATCH',
            body: JSON.stringify({ variants })
        }),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["feature", projectId, featureName] })
            toast.success("Feature variants updated")
            setEditVariantsOpen(false)
        },
        onError: () => toast.error("Failed to update variants")
    })

    const archiveMutation = useMutation({
        mutationFn: async () => {
            const protectedEnvs = allEnvironments.filter(e => e.requiredApprovals && e.requiredApprovals > 0)
            const activeInProtectedEnv = feature?.environments?.find(fe => {
                const envConfig = protectedEnvs.find(e => e.name === fe.name)
                if (!envConfig) return false
                return fe.enabled || (fe.strategies && fe.strategies.length > 0)
            })

            const isArchiveProtected = !!activeInProtectedEnv
            const archiveProtectedEnv = activeInProtectedEnv?.name || protectedEnvs[0]?.name || null

            if (isArchiveProtected && archiveProtectedEnv) {
                return createChangeRequest(projectId!, {
                    title: `Archive feature: ${featureName}`,
                    environment: archiveProtectedEnv,
                    changes: [{
                        feature: featureName!,
                        action: "archive-feature",
                        payload: {}
                    }]
                })
            } else {
                return queryClient.ensureQueryData({
                    queryKey: ["archiveFeature", projectId, featureName],
                    queryFn: () => (projectId && featureName) ? apiFetch(`/projects/${projectId}/features/${featureName}`, { method: 'DELETE' }) : null
                })
            }
        },
        onSuccess: () => {
            const protectedEnvs = allEnvironments.filter(e => e.requiredApprovals && e.requiredApprovals > 0)
            const activeInProtectedEnv = feature?.environments?.find(fe => {
                const envConfig = protectedEnvs.find(e => e.name === fe.name)
                if (!envConfig) return false
                return fe.enabled || (fe.strategies && fe.strategies.length > 0)
            })

            if (!!activeInProtectedEnv) {
                toast.success(`Change request created for archiving ${featureName}`)
                setArchiveConfirmOpen(false)
            } else {
                queryClient.invalidateQueries({ queryKey: ["dashboard-summary"] })
                toast.success(`Feature ${featureName} archived`)
                router.push("/features")
            }
        },
        onError: (error: Error) => {
            toast.error(error.message || "Failed to archive feature")
        }
    })

    const getProtectionStatus = () => {
        const protectedEnvs = allEnvironments.filter(e => e.requiredApprovals && e.requiredApprovals > 0)
        const activeInProtectedEnv = feature?.environments?.find(fe => {
            const envConfig = protectedEnvs.find(e => e.name === fe.name)
            if (!envConfig) return false
            return fe.enabled || (fe.strategies && fe.strategies.length > 0)
        })

        return {
            isProtected: !!activeInProtectedEnv,
            protectedEnv: activeInProtectedEnv?.name || protectedEnvs[0]?.name || null
        }
    }

    const { isProtected: isArchiveProtected, protectedEnv: archiveProtectedEnv } = getProtectionStatus()

    if (!projectId || !featureName) {
        return (
            <div className="flex flex-col items-center justify-center py-20">
                <h2 className="text-xl font-semibold text-muted-foreground">No feature specified</h2>
                <Button variant="link" onClick={() => router.push("/features")}>Return to Features</Button>
            </div>
        )
    }

    if (featureLoading || environmentsLoading) {
        return (
            <div className="space-y-6 px-6 py-8">
                <Skeleton className="h-8 w-64" />
                <Skeleton className="h-[400px] w-full" />
            </div>
        )
    }

    if (!feature) {
        return (
            <div className="flex flex-col items-center justify-center py-20">
                <h2 className="text-xl font-semibold text-muted-foreground">Feature not found</h2>
                <Button variant="link" onClick={() => router.push("/features")}>Return to Features</Button>
            </div>
        )
    }

    return (
        <div className="space-y-6 px-6 py-8">
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <Button variant="ghost" size="icon" onClick={() => router.push("/features")}>
                        <ArrowLeft className="h-4 w-4" />
                    </Button>
                    <div>
                        <div className="flex items-center gap-2">
                            <h1 className="text-2xl font-semibold tracking-tight">{feature.name}</h1>
                            <Badge
                                variant={feature.stale ? "outline" : "secondary"}
                                className={feature.stale ? "bg-amber-500/10 text-amber-600 border-amber-500/20 gap-1" : "gap-1"}
                            >
                                {feature.stale ? (
                                    <>
                                        <AlertTriangle className="h-3 w-3" />
                                        Stale
                                    </>
                                ) : (
                                    <>
                                        <div className="h-1.5 w-1.5 rounded-full bg-current" />
                                        Active
                                    </>
                                )}
                            </Badge>
                        </div>
                        <div className="flex items-center gap-2 group">
                            <p className="text-sm text-muted-foreground">{feature.description || "No description provided"}</p>
                            <Button
                                variant="ghost"
                                size="icon"
                                className="h-6 w-6 opacity-0 group-hover:opacity-100 transition-opacity"
                                onClick={() => {
                                    setEditDescriptionValue(feature.description || "")
                                    setEditDescriptionOpen(true)
                                }}
                            >
                                <Pencil className="h-3 w-3" />
                            </Button>
                        </div>
                    </div>
                </div>
                <Button
                    variant="outline"
                    className="text-muted-foreground hover:text-destructive hover:bg-destructive/5 transition-all gap-2"
                    onClick={() => setArchiveConfirmOpen(true)}
                >
                    <Trash className="h-4 w-4" />
                    Archive Feature
                </Button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <Card className="md:col-span-2">
                    <CardHeader>
                        <CardTitle>Activation Strategies</CardTitle>
                        <CardDescription>
                            Configure how this feature is enabled across different environments.
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        {allEnvironments.length === 0 ? (
                            <div className="py-12 text-center border border-dashed rounded-lg bg-muted/20">
                                <p className="text-sm text-muted-foreground">No environments configured.</p>
                                <Button variant="link" onClick={() => router.push("/environments")}>
                                    Go to Environments
                                </Button>
                            </div>
                        ) : (
                            <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-4">
                                <TabsList className="bg-muted/50 p-1">
                                    {allEnvironments.map((env) => {
                                        const isEnabled = feature.environments?.find(fe => fe.name === env.name)?.enabled ?? false
                                        return (
                                            <TabsTrigger key={env.name} value={env.name} className="gap-2 px-4">
                                                <div className={`h-1.5 w-1.5 rounded-full transition-all ${isEnabled ? "bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.4)]" : "bg-muted-foreground/30"}`} />
                                                {env.name}
                                            </TabsTrigger>
                                        )
                                    })}

                                </TabsList>

                                {allEnvironments.map((env) => {
                                    const featureEnv = feature.environments?.find(fe => fe.name === env.name)
                                    const isShimmering = shimmerEnv === env.name

                                    return (
                                        <TabsContent key={env.name} value={env.name} className="space-y-4 pt-4">
                                            <div className={`flex items-center justify-between p-4 border rounded-xl bg-muted/30 transition-all duration-300 ${isShimmering ? "animate-card-shimmer border-green-500/50 bg-green-500/5" : ""}`}>
                                                <div className="flex items-center gap-3">
                                                    <div className={`p-2 rounded-full transition-all duration-300 ${featureEnv?.enabled ? "bg-green-500/20 text-green-600 shadow-[0_0_15px_rgba(34,197,94,0.4)] border border-green-500/20" : "bg-muted text-muted-foreground border border-transparent"}`}>
                                                        <Power className="h-5 w-5" />
                                                    </div>
                                                    <div>
                                                        <div className="flex items-center gap-2">
                                                            <p className="font-medium">Feature Flag status</p>
                                                            <Button
                                                                variant="ghost"
                                                                size="icon"
                                                                className="h-6 w-6 text-muted-foreground hover:text-primary"
                                                                onClick={() => setEditEnv(env)}
                                                            >
                                                                <Settings className="h-3.5 w-3.5" />
                                                            </Button>
                                                        </div>
                                                        <p className="text-xs text-muted-foreground">
                                                            {featureEnv?.enabled ? "Enabled in this environment" : "Disabled in this environment"}
                                                        </p>
                                                    </div>
                                                </div>
                                                <Switch
                                                    className="transition-all duration-300 data-[state=checked]:shadow-[0_0_12px_rgba(34,197,94,0.4)] data-[state=checked]:border-green-500/30 hover:shadow-[0_0_12px_rgba(59,130,246,0.3)] data-[state=checked]:hover:shadow-[0_0_18px_rgba(34,197,94,0.6)]"
                                                    checked={featureEnv?.enabled ?? false}
                                                    onCheckedChange={(checked: boolean) => {
                                                        if (env.protected) {
                                                            const changes: { feature: string; action: string; payload: unknown }[] = [{
                                                                feature: featureName!,
                                                                action: checked ? 'enable' : 'disable',
                                                                payload: { enabled: checked }
                                                            }]

                                                            // If enabling and no strategies, add default strategy
                                                            if (checked && (!featureEnv?.strategies || featureEnv.strategies.length === 0)) {
                                                                changes.push({
                                                                    feature: featureName!,
                                                                    action: 'add-strategy',
                                                                    payload: {
                                                                        name: 'default',
                                                                        parameters: {},
                                                                        constraints: []
                                                                    }
                                                                })
                                                            }

                                                            setCrData({ env: env.name, changes })
                                                            setCrTitle(`${checked ? 'Enable' : 'Disable'} ${featureName} in ${env.name}`)
                                                        } else if (checked && env.type === 'production') {
                                                            setPendingProductionEnable(env.name)
                                                        } else {
                                                            toggleMutation.mutate({ env: env.name, enabled: checked })
                                                        }
                                                    }}
                                                />
                                            </div>

                                            <div className="space-y-4 mt-6">
                                                <div className="flex items-center justify-between">
                                                    <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">Applied Strategies</h3>
                                                    <Button
                                                        size="sm"
                                                        variant="outline"
                                                        className="h-8 gap-2 border-primary/20 hover:bg-primary/5 hover:text-primary transition-colors"
                                                        onClick={() => setAddStrategyEnv(env.name)}
                                                    >
                                                        <Plus className="h-3.5 w-3.5" />
                                                        Add Strategy
                                                    </Button>
                                                </div>

                                                <div className="space-y-3">
                                                    {(featureEnv?.strategies?.length ?? 0) === 0 ? (
                                                        <div className="text-center py-10 border border-dashed rounded-xl bg-muted/10">
                                                            <p className="text-sm text-semi-bold">No strategies defined.</p>
                                                            <p className="text-xs text-muted-foreground mt-1">
                                                                {featureEnv?.enabled
                                                                    ? "The feature is enabled but has no strategies (default behavior applies)."
                                                                    : "You can define strategies before enabling the feature."}
                                                            </p>
                                                            <Button
                                                                variant="outline"
                                                                size="sm"
                                                                className="mt-4"
                                                                onClick={() => setAddStrategyEnv(env.name)}
                                                            >
                                                                Add your first strategy
                                                            </Button>
                                                        </div>
                                                    ) : (
                                                        featureEnv?.strategies?.map((strategy) => (
                                                            <Card key={strategy.id} className="group relative overflow-hidden transition-all hover:shadow-md border-muted-foreground/10">
                                                                <div className="p-4 flex items-center justify-between bg-card">
                                                                    <div className="space-y-1">
                                                                        <div className="flex items-center gap-2">
                                                                            <p className="font-bold text-sm text-primary">{strategy.name}</p>
                                                                            {strategy.disabled && <Badge variant="secondary" className="text-[10px]">Disabled</Badge>}
                                                                        </div>
                                                                        <div className="flex flex-wrap gap-2 mt-2">
                                                                            {strategy.parameters?.map((p) => (
                                                                                <Badge key={p.name} variant="secondary" className="text-[10px] font-medium bg-blue-500/10 text-blue-700 hover:bg-blue-500/20 border-none">
                                                                                    <span className="opacity-70 mr-1">{p.name}:</span>
                                                                                    {p.value}
                                                                                </Badge>
                                                                            ))}
                                                                            {strategy.constraints?.map((c, i) => (
                                                                                <Badge key={i} variant="outline" className="text-[10px] font-medium bg-background border-primary/20 hover:bg-muted/50 transition-colors h-6 px-2 gap-1.5 group">
                                                                                    <Filter className="h-2.5 w-2.5 text-primary/60" />
                                                                                    <div className="flex items-center gap-1">
                                                                                        {c.inverted && <span className="text-[9px] font-bold text-destructive">NOT</span>}
                                                                                        <span className="font-bold text-foreground">{c.contextName}</span>
                                                                                        <span className="text-[9px] text-muted-foreground uppercase tracking-tighter opacity-70 font-bold px-1 rounded bg-muted">
                                                                                            {c.operator.replace(/_/g, ' ')}
                                                                                        </span>
                                                                                        <span className="text-primary truncate max-w-[150px]">
                                                                                            {c.values?.join(', ')}
                                                                                        </span>
                                                                                    </div>
                                                                                </Badge>
                                                                            ))}
                                                                            {strategy.variants?.map((v, i) => (
                                                                                <Badge key={i} variant="outline" className="text-[10px] font-medium bg-purple-500/10 text-purple-700 border-purple-200 h-6 px-2 gap-1.5">
                                                                                    <Package className="h-2.5 w-2.5" />
                                                                                    {v.name} ({v.weight / 10}%)
                                                                                </Badge>
                                                                            ))}
                                                                        </div>
                                                                    </div>
                                                                    <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                                                                        <Button
                                                                            variant="ghost"
                                                                            size="icon"
                                                                            className="h-8 w-8 hover:bg-primary/5 hover:text-primary"
                                                                            onClick={() => setEditStrategy({ env: env.name, strategy })}
                                                                        >
                                                                            <Settings className="h-4 w-4" />
                                                                        </Button>
                                                                        <Button
                                                                            variant="ghost"
                                                                            size="icon"
                                                                            className="h-8 w-8 text-destructive hover:bg-destructive/10 hover:text-destructive"
                                                                            onClick={() => {
                                                                                if (env.protected) {
                                                                                    setCrData({
                                                                                        env: env.name,
                                                                                        changes: [{
                                                                                            feature: featureName!,
                                                                                            action: 'delete-strategy',
                                                                                            payload: { id: strategy.id }
                                                                                        }]
                                                                                    })
                                                                                    setCrTitle(`Remove ${strategy.name} strategy from ${featureName} in ${env.name}`)
                                                                                } else {
                                                                                    removeStrategyMutation.mutate({ env: env.name, strategyId: strategy.id! })
                                                                                }
                                                                            }}
                                                                        >
                                                                            <Trash className="h-4 w-4" />
                                                                        </Button>
                                                                    </div>
                                                                </div>
                                                            </Card>
                                                        ))
                                                    )}
                                                </div>
                                            </div>

                                        </TabsContent>
                                    )
                                })}
                            </Tabs>
                        )}
                    </CardContent>
                </Card>

                <Card className="md:col-span-2">
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <div>
                            <CardTitle>Feature Variants</CardTitle>
                            <CardDescription>
                                Variants defined here apply to all environments unless overridden by strategy variants.
                            </CardDescription>
                        </div>
                        <Button
                            size="sm"
                            variant="outline"
                            className="h-8 gap-2 border-primary/20 hover:bg-primary/5 hover:text-primary transition-colors"
                            onClick={() => setEditVariantsOpen(true)}
                        >
                            <Pencil className="h-3.5 w-3.5" />
                            Manage Variants
                        </Button>
                    </CardHeader>
                    <CardContent>
                        {(feature.variants?.length ?? 0) === 0 ? (
                            <div className="py-6 text-center border border-dashed rounded-xl bg-muted/10">
                                <p className="text-xs text-muted-foreground">No feature-level variants defined.</p>
                            </div>
                        ) : (
                            <div className="flex flex-wrap gap-3">
                                {feature.variants?.map((v, i) => (
                                    <Badge key={i} variant="outline" className="py-2 px-3 gap-2 bg-purple-500/5 border-purple-200 text-purple-700">
                                        <Package className="h-4 w-4" />
                                        <div className="flex flex-col items-start gap-0.5">
                                            <span className="font-bold text-xs">{v.name}</span>
                                            <span className="text-[10px] opacity-70">{v.weight / 10}% weight</span>
                                        </div>
                                    </Badge>
                                ))}
                            </div>
                        )}
                    </CardContent>
                </Card>

                <div className="space-y-6">
                    <Card>
                        <CardHeader>
                            <CardTitle className="text-lg">Metadata</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                    <Activity className="h-4 w-4" />
                                    <span>Type</span>
                                </div>
                                <Badge variant="secondary" className="uppercase font-bold tracking-wider text-[10px]">{feature.type}</Badge>
                            </div>
                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                    <Clock className="h-4 w-4" />
                                    <span>Created</span>
                                </div>
                                <span className="text-sm font-medium">{new Date(feature.createdAt).toLocaleDateString()}</span>
                            </div>
                            <Separator />
                            <div>
                                <p className="text-[10px] font-bold text-muted-foreground mb-2 uppercase tracking-widest">Project</p>
                                <Link
                                    href={`/projects`}
                                    className="flex items-center justify-between p-3 rounded-xl bg-muted/50 border border-transparent hover:border-primary/20 hover:bg-primary/5 transition-all group"
                                >
                                    <span className="text-sm font-semibold">{projectId}</span>
                                    <ChevronRight className="h-4 w-4 relative -left-1 group-hover:left-0 transition-all text-primary" />
                                </Link>
                            </div>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader>
                            <CardTitle className="text-lg">Impression Data</CardTitle>
                            <CardDescription className="text-xs text-balance">
                                When enabled, SDKs will emit events when this feature is evaluated.
                            </CardDescription>
                        </CardHeader>
                        <CardContent>
                            <div className="flex items-center justify-between p-3 rounded-xl bg-muted/30">
                                <span className="text-sm font-semibold">{feature.impressionData ? "Active" : "Inactive"}</span>
                                <Switch
                                    checked={feature.impressionData}
                                    onCheckedChange={(checked: boolean) => updateFeatureMutation.mutate({ impressionData: checked })}
                                />
                            </div>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader>
                            <CardTitle className="text-lg">Status</CardTitle>
                            <CardDescription className="text-xs text-balance">
                                Mark this feature as stale when it is no longer needed and should be removed.
                            </CardDescription>
                        </CardHeader>
                        <CardContent>
                            <div className="flex items-center justify-between p-3 rounded-xl bg-muted/30">
                                <span className="text-sm font-semibold">{feature.stale ? "Stale" : "Active"}</span>
                                <Switch
                                    checked={feature.stale}
                                    onCheckedChange={(checked: boolean) => updateFeatureMutation.mutate({ stale: checked })}
                                />
                            </div>
                        </CardContent>
                    </Card>
                </div>
            </div>

            <StrategyDialog
                open={!!addStrategyEnv}
                onOpenChange={(open) => !open && setAddStrategyEnv(null)}
                strategyDefinitions={strategyDefinitions}
                onSave={(strategy) => {
                    const env = allEnvironments.find(e => e.name === addStrategyEnv)
                    if (env?.protected) {
                        const changes = [{
                            feature: featureName!,
                            action: 'add-strategy',
                            payload: strategy
                        }]
                        setCrData({ env: addStrategyEnv!, changes })
                        setCrTitle(`Add ${strategy.name} strategy to ${featureName} in ${addStrategyEnv}`)
                        setAddStrategyEnv(null)
                    } else {
                        addStrategyMutation.mutate({ env: addStrategyEnv!, strategy })
                    }
                }}
            />

            <StrategyDialog
                open={!!editStrategy}
                onOpenChange={(open) => !open && setEditStrategy(null)}
                strategyDefinitions={strategyDefinitions}
                strategy={fetchedStrategy || editStrategy?.strategy}
                loading={strategyLoading}
                onSave={(strategy) => {
                    const env = allEnvironments.find(e => e.name === editStrategy!.env)
                    if (env?.protected) {
                        setCrData({
                            env: editStrategy!.env,
                            changes: [{
                                feature: featureName!,
                                action: 'update-strategy',
                                payload: { id: editStrategy!.strategy.id, ...strategy }
                            }]
                        })
                        setCrTitle(`Update ${strategy.name} strategy for ${featureName} in ${editStrategy!.env}`)
                        setEditStrategy(null)
                    } else {
                        updateStrategyMutation.mutate({
                            env: editStrategy!.env,
                            strategyId: editStrategy!.strategy.id!,
                            strategy
                        })
                    }
                }}
            />

            {feature && (
                <FeatureVariantsDialog
                    open={editVariantsOpen}
                    onOpenChange={setEditVariantsOpen}
                    feature={feature}
                    contextFields={contextFields}
                    onSave={(variants) => updateVariantsMutation.mutate(variants)}
                />
            )}

            {editEnv && (
                <EditEnvironmentDrawer
                    open={!!editEnv}
                    onOpenChange={(open) => !open && setEditEnv(null)}
                    environment={editEnv}
                    onSubmit={(values) => updateEnvMutation.mutate(values)}
                    isLoading={updateEnvMutation.isPending}
                />
            )}
            <AlertDialog open={!!pendingProductionEnable} onOpenChange={(open) => !open && setPendingProductionEnable(null)}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle className="flex items-center gap-2 text-red-600">
                            <AlertTriangle className="h-5 w-5" />
                            Enable in Production?
                        </AlertDialogTitle>
                        <AlertDialogDescription>
                            You are about to enable this feature in the <strong>{pendingProductionEnable}</strong> environment.
                            This will make the feature live for your users. Are you sure you want to proceed?
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction
                            className="bg-red-600 hover:bg-red-700 focus:ring-red-600"
                            onClick={() => {
                                if (pendingProductionEnable) {
                                    toggleMutation.mutate({ env: pendingProductionEnable, enabled: true })
                                    setPendingProductionEnable(null)
                                }
                            }}
                        >
                            Enable Feature
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            <Dialog open={!!crData} onOpenChange={(open) => !open && setCrData(null)}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Create Change Request</DialogTitle>
                        <DialogDescription>
                            This environment is protected. Changes must be approved via a change request.
                        </DialogDescription>
                    </DialogHeader>
                    <div className="space-y-4 py-4">
                        {featureChangeRequests.filter(cr => cr.state === "Draft" && cr.environment === crData?.env).length > 0 ? (
                            <div className="space-y-3">
                                <Label>Select Change Request</Label>
                                <RadioGroup
                                    value={existingDraftId?.toString() || "new"}
                                    onValueChange={(val: string) => setExistingDraftId(val === "new" ? null : parseInt(val))}
                                    className="gap-2"
                                >
                                    <div className="flex items-center space-x-3 space-y-0 rounded-xl border p-3 hover:bg-muted/50 transition-colors cursor-pointer">
                                        <RadioGroupItem value="new" id="new-cr" />
                                        <Label htmlFor="new-cr" className="flex-1 cursor-pointer font-medium">Create New Change Request</Label>
                                    </div>
                                    {featureChangeRequests
                                        .filter(cr => cr.state === "Draft" && cr.environment === crData?.env)
                                        .map((cr) => (
                                            <div key={cr.id} className="flex items-center space-x-3 space-y-0 rounded-xl border p-3 hover:bg-muted/50 transition-colors cursor-pointer">
                                                <RadioGroupItem value={cr.id.toString()} id={`cr-${cr.id}`} />
                                                <Label htmlFor={`cr-${cr.id}`} className="flex-1 cursor-pointer">
                                                    <div className="font-medium">{cr.title}</div>
                                                    <div className="text-[10px] text-muted-foreground uppercase tracking-wider">CR-{cr.id} • Draft</div>
                                                </Label>
                                            </div>
                                        ))}
                                </RadioGroup>
                            </div>
                        ) : null}

                        {!existingDraftId && (
                            <>
                                <div className="space-y-2">
                                    <Label htmlFor="title">Reason for change</Label>
                                    <Input
                                        id="title"
                                        placeholder="E.g. Enabling feature for beta testing"
                                        value={crTitle}
                                        onChange={(e) => setCrTitle(e.target.value)}
                                    />
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="scheduledAt" className="flex items-center gap-1.5">
                                        <Clock className="h-3.5 w-3.5 text-muted-foreground" />
                                        Scheduled Apply (Optional)
                                    </Label>
                                    <div className="flex gap-2">
                                        <Input
                                            id="scheduledAt"
                                            type="datetime-local"
                                            value={crScheduledAt}
                                            min={new Date(new Date().getTime() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 16)}
                                            onChange={(e) => setCrScheduledAt(e.target.value)}
                                            className="flex-1"
                                        />
                                        {crScheduledAt && (
                                            <Button
                                                variant="outline"
                                                size="icon"
                                                onClick={() => setCrScheduledAt("")}
                                                className="shrink-0"
                                                title="Clear scheduled time"
                                            >
                                                <XCircle className="h-4 w-4" />
                                            </Button>
                                        )}
                                    </div>
                                    {crScheduledAt && new Date(crScheduledAt) > new Date(new Date().setMonth(new Date().getMonth() + 11)) && (
                                        <p className="text-[10px] text-amber-600 font-medium flex items-center gap-1 pl-1">
                                            <AlertTriangle className="h-3 w-3" />
                                            Warning: Scheduled more than 11 months in the future.
                                        </p>
                                    )}
                                    <p className="text-[10px] text-muted-foreground italic pl-1">
                                        Leave empty to apply immediately after approval. Uses your local browser time ({Intl.DateTimeFormat().resolvedOptions().timeZone}).
                                    </p>
                                </div>
                            </>
                        )}
                        <div className="p-3 bg-muted rounded-lg text-xs space-y-1">
                            <p className="font-semibold">Planned Changes:</p>
                            {crData?.changes.map((c, i) => (
                                <p key={i}>
                                    {c.action === 'enable' ? 'Enable' : c.action === 'disable' ? 'Disable' : c.action}
                                    {c.feature === featureName ? '' : ` feature ${c.feature}`}
                                    {c.action === 'add-strategy' ? ` strategy ${(c.payload as { name: string }).name}` : ''}
                                    {c.action === 'delete-strategy' ? ` strategy` : ''}
                                </p>
                            ))}
                            <p className="mt-2">Environment: <code className="font-bold">{crData?.env}</code></p>
                        </div>
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setCrData(null)}>Cancel</Button>
                        <Button
                            disabled={(!existingDraftId && !crTitle) || crMutation.isPending}
                            onClick={() => {
                                if (crData) {
                                    if (crScheduledAt) {
                                        const scheduledDate = new Date(crScheduledAt)
                                        const now = new Date()
                                        // Allow 1 minute grace period for client/server sync
                                        now.setMinutes(now.getMinutes() - 1)

                                        if (isNaN(scheduledDate.getTime())) {
                                            toast.error("Invalid date format")
                                            return
                                        }

                                        if (scheduledDate <= now) {
                                            toast.error("Scheduled time must be in the future")
                                            return
                                        }
                                    }
                                    crMutation.mutate({
                                        title: crTitle,
                                        environment: crData.env,
                                        changes: crData.changes,
                                        scheduledAt: crScheduledAt ? new Date(crScheduledAt).toISOString() : undefined,
                                        id: existingDraftId || undefined
                                    })
                                }
                            }}
                        >
                            {crMutation.isPending ? "Processing..." : (existingDraftId ? "Add to Draft" : "Create Draft")}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
            <AlertDialog open={archiveConfirmOpen} onOpenChange={setArchiveConfirmOpen}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>
                        <AlertDialogDescription>
                            {isArchiveProtected ? (
                                <>
                                    Archiving <span className="font-bold text-foreground italic">{featureName}</span> requires a Change Request because it is active in protected environment <span className="font-bold text-amber-600 uppercase">({archiveProtectedEnv})</span>.
                                    <br /><br />
                                    This will create a draft Change Request that must be approved before the feature is archived.
                                </>
                            ) : (
                                <>
                                    This will permanently archive the feature <span className="font-bold text-foreground italic">{featureName}</span> and remove it from all environments. This action cannot be undone.
                                </>
                            )}
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction
                            onClick={() => archiveMutation.mutate()}
                            className={isArchiveProtected ? "bg-amber-600 hover:bg-amber-700" : "bg-destructive hover:bg-destructive/90"}
                        >
                            {isArchiveProtected ? "Create Change Request" : "Archive Feature"}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
            <Dialog open={editDescriptionOpen} onOpenChange={setEditDescriptionOpen}>
                <DialogContent className="sm:max-w-[500px]">
                    <DialogHeader>
                        <DialogTitle className="flex items-center gap-2 italic">
                            <Pencil className="h-4 w-4 text-primary" />
                            Edit Description
                        </DialogTitle>
                        <DialogDescription>
                            Update the description for the feature flag <span className="font-bold text-foreground italic">{featureName}</span>. This change will be logged in the audit trail.
                        </DialogDescription>
                    </DialogHeader>
                    <div className="py-4 space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="description" className="text-xs font-bold uppercase tracking-widest text-muted-foreground">Description</Label>
                            <Textarea
                                id="description"
                                placeholder="Describe what this feature flag does..."
                                value={editDescriptionValue}
                                onChange={(e) => setEditDescriptionValue(e.target.value)}
                                className="min-h-[120px] resize-none border-muted-foreground/20 focus:ring-primary/20"
                            />
                        </div>
                    </div>
                    <DialogFooter>
                        <Button variant="ghost" onClick={() => setEditDescriptionOpen(false)}>Cancel</Button>
                        <Button
                            onClick={() => updateFeatureMutation.mutate({ description: editDescriptionValue })}
                            className="bg-primary hover:bg-primary/90 shadow-lg shadow-primary/20"
                            disabled={updateFeatureMutation.isPending}
                        >
                            {updateFeatureMutation.isPending ? "Saving..." : "Save Changes"}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    )
}

export default function FeatureDetailsPage() {
    return (
        <Suspense fallback={
            <div className="space-y-6 px-6 py-8">
                <Skeleton className="h-8 w-64" />
                <Skeleton className="h-[400px] w-full" />
            </div>
        }>
            <FeatureDetailsContent />
        </Suspense>
    )
}

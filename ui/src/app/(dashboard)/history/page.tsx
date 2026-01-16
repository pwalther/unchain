"use client"

import { useState, useEffect } from "react"
import { useSearchParams, useRouter } from "next/navigation"
import { format } from "date-fns"
import { Calendar as CalendarIcon, Filter, Download, CheckCircle2, AlertTriangle, XCircle, Minus, RefreshCw, Eye } from "lucide-react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Calendar } from "@/components/ui/calendar"
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from "@/components/ui/popover"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { DataTable } from "@/components/data-table"
import { ColumnDef } from "@tanstack/react-table"
import { Badge } from "@/components/ui/badge"
import { apiFetch } from "@/lib/api"
import { useQuery } from "@tanstack/react-query"
import { Skeleton } from "@/components/ui/skeleton"
import { getProjects } from "@/features/projects/actions"
import { getEnvironments } from "@/features/environments/actions"
import { Project, Environment } from "@/types"
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
} from "@/components/ui/dialog"
import { Suspense } from "react"

interface AuditLogItem {
    id: number
    entityType: string
    displayName?: string
    entityId: string
    action: string
    changedBy: string
    changedAt: string
    data: string
    environment?: string
    signatureValid?: boolean
    chainValid?: boolean
}

function HistoryContent() {
    const router = useRouter()
    const searchParams = useSearchParams()

    // State for filters
    const [selectedProject, setSelectedProject] = useState<string>(searchParams.get("project") || "")
    const [selectedEnv, setSelectedEnv] = useState<string>(searchParams.get("environment") || "all")
    const [selectedFeature, setSelectedFeature] = useState<string>(searchParams.get("feature") || "")
    const [date, setDate] = useState<Date | undefined>(undefined)
    const [viewItem, setViewItem] = useState<AuditLogItem | null>(null)

    // Fetch projects for selector
    const { data: projects = [] } = useQuery({
        queryKey: ["projects"],
        queryFn: getProjects
    })

    // Fetch environments for selector
    const { data: environments = [] } = useQuery({
        queryKey: ["environments"],
        queryFn: getEnvironments
    })

    // Auto-select project if only one exists
    useEffect(() => {
        if (projects.length === 1 && !selectedProject) {
            const projectId = projects[0].id
            setSelectedProject(projectId)
            const params = new URLSearchParams(searchParams.toString())
            params.set("project", projectId)
            router.replace(`/history?${params.toString()}`)
        }
    }, [projects, selectedProject, router, searchParams])

    // Construct API URL
    const buildApiUrl = () => {
        if (!selectedProject) return null

        const params = new URLSearchParams()
        if (selectedEnv && selectedEnv !== "all") params.append("environment", selectedEnv)
        if (selectedFeature) params.append("feature", selectedFeature)

        if (date) {
            params.append("from", date.toISOString())
        }

        return `/projects/${selectedProject}/history?${params.toString()}`
    }

    const apiUrl = buildApiUrl()

    const { data: historyData = [], isLoading, refetch, isFetching } = useQuery({
        queryKey: ["history", selectedProject, selectedEnv, selectedFeature, date],
        queryFn: () => apiFetch<AuditLogItem[]>(apiUrl!),
        enabled: !!apiUrl,
        refetchOnWindowFocus: true
    })

    // Columns
    const columns: ColumnDef<AuditLogItem>[] = [
        {
            accessorKey: "changedAt",
            header: "Time",
            cell: ({ row }: { row: any }) => {
                const date = new Date(row.getValue("changedAt"))
                return new Intl.DateTimeFormat(undefined, {
                    dateStyle: 'medium',
                    timeStyle: 'medium'
                }).format(date)
            },
        },

        {
            accessorKey: "action",
            header: "Action",
            cell: ({ row }: { row: any }) => {
                const action = row.getValue("action") as string
                return (
                    <Badge variant={
                        action === "CREATED" ? "default" :
                            action === "DELETED" ? "destructive" :
                                action === "APPLIED" ? "secondary" : "outline"
                    }>
                        {action}
                    </Badge>
                )
            }
        },
        {
            accessorKey: "environment",
            header: "Environment",
            cell: ({ row }: { row: any }) => {
                const env = row.getValue("environment") as string | null
                return env ? <span className="font-mono text-sm">{env}</span> : <span className="text-muted-foreground">-</span>
            }
        },
        {
            accessorKey: "displayName",
            header: "Entity Type",
            cell: ({ row }: { row: any }) => {
                return row.getValue("displayName") || row.original.entityType
            }
        },
        {
            accessorKey: "integrity",
            header: "Integrity",
            cell: ({ row }: { row: any }) => {
                const signatureValid = row.original.signatureValid
                const chainValid = row.original.chainValid

                // If both are undefined, integrity checking is disabled
                if (signatureValid === undefined && chainValid === undefined) {
                    return (
                        <div className="flex items-center gap-1 text-muted-foreground" title="Integrity checking disabled">
                            <Minus className="h-4 w-4" />
                        </div>
                    )
                }

                // If signature is invalid
                if (signatureValid === false) {
                    return (
                        <div className="flex items-center gap-1 text-destructive" title="Invalid signature - entry may have been tampered with">
                            <XCircle className="h-4 w-4" />
                            <span className="text-xs">Tampered</span>
                        </div>
                    )
                }

                // If chain is broken
                if (chainValid === false) {
                    return (
                        <div className="flex items-center gap-1 text-yellow-600" title="Chain break detected - possible deletion before this entry">
                            <AlertTriangle className="h-4 w-4" />
                            <span className="text-xs">Chain Break</span>
                        </div>
                    )
                }

                // All valid
                return (
                    <div className="flex items-center gap-1 text-green-600" title="Signature and chain verified">
                        <CheckCircle2 className="h-4 w-4" />
                    </div>
                )
            }
        },
        {
            accessorKey: "entityId",
            header: "Entity ID",
            cell: ({ row }: { row: any }) => {
                const entityType = row.original.entityType;
                if (entityType === "ChangeRequestEntity" && row.original.action === "APPLIED") {
                    return "Summary"
                }
                return row.getValue("entityId")
            }
        },
        {
            accessorKey: "data",
            header: "Details",
            cell: ({ row }) => {
                try {
                    const data = JSON.parse(row.getValue("data") as string)
                    if (Array.isArray(data)) {
                        // This is the summary log
                        return (
                            <div className="space-y-1">
                                {data.map((change: any, idx: number) => (
                                    <div key={idx} className="text-sm">
                                        <span className="font-semibold">{change.action}</span> on <span className="font-mono">{change.feature}</span>
                                    </div>
                                ))}
                            </div>
                        )
                    }
                    // Regular log, show a summary or raw
                    return <pre className="text-xs whitespace-pre-wrap max-w-xs overflow-hidden text-muted-foreground">{JSON.stringify(data, null, 2)}</pre>
                } catch (e) {
                    return <span className="text-muted-foreground italic">Invalid Data</span>
                }
            }
        },
        {
            id: "actions",
            header: "Actions",
            cell: ({ row }) => {
                return (
                    <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => setViewItem(row.original)}
                        title="View Details"
                    >
                        <Eye className="h-4 w-4" />
                    </Button>
                )
            }
        }
    ]

    const updateFilter = (key: string, value: string) => {
        const params = new URLSearchParams(searchParams.toString())
        if (value && value !== "all") {
            params.set(key, value)
        } else {
            params.delete(key)
        }
        router.push(`/history?${params.toString()}`)

        if (key === "project") setSelectedProject(value)
        if (key === "environment") setSelectedEnv(value)
    }

    const downloadAuditLog = () => {
        if (!historyData || historyData.length === 0) {
            return
        }

        const dataStr = JSON.stringify(historyData, null, 2)
        const dataBlob = new Blob([dataStr], { type: 'application/json' })
        const url = URL.createObjectURL(dataBlob)
        const link = document.createElement('a')
        link.href = url

        // Generate filename with current filters
        const timestamp = new Date().toISOString().split('T')[0]
        const filters = [
            selectedProject,
            selectedEnv !== 'all' ? selectedEnv : null,
            selectedFeature,
        ].filter(Boolean).join('_')

        link.download = `audit-log_${filters}_${timestamp}.json`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)
    }

    return (
        <div className="flex-1 space-y-4 p-8 pt-6">
            <div className="flex items-center justify-between space-y-2">
                <h2 className="text-3xl font-bold tracking-tight">Audit History</h2>
                <div className="flex items-center gap-2">
                    <Button
                        onClick={() => refetch()}
                        variant="outline"
                        size="sm"
                        className="gap-2"
                        disabled={isLoading || isFetching}
                    >
                        <RefreshCw className={cn("h-4 w-4", (isLoading || isFetching) && "animate-spin")} />
                        Refresh
                    </Button>
                    {historyData && historyData.length > 0 && (
                        <Button
                            onClick={downloadAuditLog}
                            variant="outline"
                            size="sm"
                            className="gap-2"
                        >
                            <Download className="h-4 w-4" />
                            Download JSON
                        </Button>
                    )}
                </div>
            </div>

            <div className="flex flex-col gap-4 md:flex-row md:items-center">
                {/* Project Selector - Required First */}
                <div className="w-[200px]">
                    <label className="text-sm font-medium mb-1 block">Project</label>
                    <Select value={selectedProject} onValueChange={(val) => updateFilter("project", val)}>
                        <SelectTrigger>
                            <SelectValue placeholder="Select Project" />
                        </SelectTrigger>
                        <SelectContent>
                            {projects.map((p) => (
                                <SelectItem key={p.id} value={p.id}>
                                    {p.name}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>

                {/* Filters showing only if project selected? or always? 
                    "Allow filtering by project... (project selection first)" implies others depend on it or appear after.
                */}
                {selectedProject && (
                    <>
                        <div className="w-[200px]">
                            <label className="text-sm font-medium mb-1 block">Environment</label>
                            <Select value={selectedEnv} onValueChange={(val) => updateFilter("environment", val)}>
                                <SelectTrigger>
                                    <SelectValue placeholder="All Environments" />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="all">
                                        All Environments
                                    </SelectItem>
                                    {environments.map((e) => (
                                        <SelectItem key={e.name} value={e.name}>
                                            {e.name}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="flex-1 max-w-sm">
                            <label className="text-sm font-medium mb-1 block">Date (From)</label>
                            <div className="flex flex-wrap gap-2">
                                <Popover>
                                    <PopoverTrigger asChild>
                                        <Button
                                            variant={"outline"}
                                            className={cn(
                                                "w-[240px] justify-start text-left font-normal",
                                                !date && "text-muted-foreground"
                                            )}
                                        >
                                            <CalendarIcon className="mr-2 h-4 w-4" />
                                            {date ? format(date, "PPP") : <span>Pick a date</span>}
                                        </Button>
                                    </PopoverTrigger>
                                    <PopoverContent className="w-auto p-0" align="start">
                                        <Calendar
                                            mode="single"
                                            selected={date}
                                            onSelect={setDate}
                                            initialFocus
                                        />
                                    </PopoverContent>
                                </Popover>
                                <Button
                                    variant="secondary"
                                    onClick={() => refetch()}
                                    className="gap-2"
                                >
                                    <Filter className="h-4 w-4" />
                                    Search
                                </Button>
                            </div>
                        </div>
                    </>
                )}
            </div>

            {selectedProject ? (
                <div className="rounded-md border bg-card">
                    <DataTable
                        columns={columns}
                        data={historyData || []}
                        isLoading={isLoading || isFetching}
                    />
                </div>
            ) : (
                <div className="flex h-[400px] items-center justify-center rounded-md border border-dashed">
                    <p className="text-muted-foreground">Please select a project to view history.</p>
                </div>
            )}
            <Dialog open={!!viewItem} onOpenChange={(open) => !open && setViewItem(null)}>
                <DialogContent className="max-w-2xl max-h-[80vh] overflow-hidden flex flex-col">
                    <DialogHeader>
                        <DialogTitle>Audit Log Entry Details</DialogTitle>
                        <DialogDescription>
                            Full data for log ID {viewItem?.id}
                        </DialogDescription>
                    </DialogHeader>
                    {viewItem && (
                        <div className="flex-1 overflow-auto bg-muted p-4 rounded-md">
                            <pre className="text-xs font-mono whitespace-pre-wrap">
                                {JSON.stringify(JSON.parse(viewItem.data), null, 2)}
                            </pre>
                        </div>
                    )}
                </DialogContent>
            </Dialog>
        </div>
    )
}

export default function HistoryPage() {
    return (
        <Suspense fallback={<div>Loading...</div>}>
            <HistoryContent />
        </Suspense>
    )
}

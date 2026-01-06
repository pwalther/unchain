"use client"

import { useState } from "react"
import { useQuery, useQueryClient } from "@tanstack/react-query"
import { Zap, Code, Plus } from "lucide-react"
import { ColumnDef } from "@tanstack/react-table"

import { DataTable } from "@/components/data-table"
import { getStrategies } from "@/features/strategies/actions"
import { StrategyDefinition } from "@/types"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { CreateStrategyDefinitionDrawer } from "@/features/strategies/components/create-strategy-definition-drawer"
import { StrategyDetailsDrawer } from "@/features/strategies/components/strategy-details-drawer"

export default function StrategiesPage() {
    const queryClient = useQueryClient()
    const [isCreateOpen, setIsCreateOpen] = useState(false)
    const [selectedStrategyName, setSelectedStrategyName] = useState<string | null>(null)

    const { data: strategies = [], isLoading } = useQuery({
        queryKey: ["strategies", "definitions"],
        queryFn: getStrategies,
    })

    const selectedStrategy = strategies.find(s => s.name === selectedStrategyName) || null

    const columns: ColumnDef<StrategyDefinition>[] = [
        {
            accessorKey: "name",
            header: "Name",
            cell: ({ row }) => (
                <div className="flex items-center gap-3">
                    <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-500/10 text-amber-600">
                        <Zap className="h-4 w-4" />
                    </div>
                    <span className="font-medium text-foreground">{row.getValue("name")}</span>
                </div>
            ),
        },
        {
            accessorKey: "description",
            header: "Description",
            cell: ({ row }) => (
                <span className="text-muted-foreground line-clamp-1">
                    {row.getValue("description") || "No description provided"}
                </span>
            ),
        },
        {
            id: "parameters",
            header: "Parameters",
            cell: ({ row }) => {
                const params = row.original.parameters || []
                return (
                    <div className="flex gap-1 flex-wrap">
                        {params.length > 0 ? params.map(p => (
                            <Badge key={p.name} variant="secondary" className="text-[10px] capitalize">
                                {p.name} ({p.type})
                            </Badge>
                        )) : <span className="text-xs text-muted-foreground italic">None</span>}
                    </div>
                )
            }
        }
    ]

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">Strategy Definitions</h1>
                    <p className="text-sm text-muted-foreground">
                        Custom activation strategies available for your features.
                    </p>
                </div>
                <Button onClick={() => setIsCreateOpen(true)} className="gap-2">
                    <Plus className="h-4 w-4" />
                    New Strategy
                </Button>
            </div>

            <DataTable
                columns={columns}
                data={strategies}
                searchKey="name"
                isLoading={isLoading}
                onRowClick={(row) => setSelectedStrategyName(row.name)}
            />

            <CreateStrategyDefinitionDrawer
                open={isCreateOpen}
                onOpenChange={setIsCreateOpen}
                onSuccess={() => queryClient.invalidateQueries({ queryKey: ["strategies", "definitions"] })}
            />

            <StrategyDetailsDrawer
                strategy={selectedStrategy}
                open={!!selectedStrategy}
                onOpenChange={(open) => !open && setSelectedStrategyName(null)}
                onSuccess={() => queryClient.invalidateQueries({ queryKey: ["strategies", "definitions"] })}
            />
        </div>
    )
}

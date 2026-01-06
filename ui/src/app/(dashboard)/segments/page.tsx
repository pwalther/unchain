"use client"

import { useQuery } from "@tanstack/react-query"
import { Users, Filter } from "lucide-react"
import { ColumnDef } from "@tanstack/react-table"

import { DataTable } from "@/components/data-table"
import { getSegments } from "@/features/segments/actions"
import { Segment } from "@/types"
import { Badge } from "@/components/ui/badge"

export default function SegmentsPage() {
    const { data: segments = [], isLoading } = useQuery({
        queryKey: ["segments"],
        queryFn: getSegments,
    })

    const columns: ColumnDef<Segment>[] = [
        {
            accessorKey: "name",
            header: "Name",
            cell: ({ row }) => (
                <div className="flex items-center gap-3">
                    <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-500/10 text-indigo-600">
                        <Users className="h-4 w-4" />
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
                    {row.getValue("description") || "Reusable user segment"}
                </span>
            ),
        },
        {
            id: "constraints",
            header: "Constraints",
            cell: ({ row }) => {
                const constraintsCount = row.original.constraints?.length || 0
                return (
                    <div className="flex items-center gap-2">
                        <Filter className="h-3 w-3 text-muted-foreground" />
                        <span className="text-sm">{constraintsCount} rules</span>
                    </div>
                )
            }
        }
    ]

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">Segments</h1>
                    <p className="text-sm text-muted-foreground">
                        Define reusable groups of users based on custom constraints.
                    </p>
                </div>
            </div>

            <DataTable
                columns={columns}
                data={segments}
                searchKey="name"
                isLoading={isLoading}
            />
        </div>
    )
}

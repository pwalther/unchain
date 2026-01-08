"use client"

import { useState } from "react"
import Link from "next/link"
import { useQuery } from "@tanstack/react-query"
import { Globe, BadgeCheck, ShieldAlert, Plus, Lock, LockOpen } from "lucide-react"
import { ColumnDef } from "@tanstack/react-table"

import { Button } from "@/components/ui/button"
import { DataTable } from "@/components/data-table"
import { getEnvironments } from "@/features/environments/actions"
import { Environment } from "@/types"
import { Badge } from "@/components/ui/badge"
import { CreateEnvironmentDrawer } from "@/features/environments/components/create-environment-drawer"

export default function EnvironmentsPage() {
    const [drawerOpen, setDrawerOpen] = useState(false)

    const { data: environments = [], isLoading } = useQuery({
        queryKey: ["environments"],
        queryFn: getEnvironments,
    })

    const columns: ColumnDef<Environment>[] = [
        {
            accessorKey: "name",
            header: "Name",
            cell: ({ row }) => (
                <Link
                    href={`/environments/view?name=${row.getValue("name")}`}
                    className="flex items-center gap-3 hover:opacity-80 transition-opacity"
                >
                    <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 text-primary uppercase text-[10px] font-bold">
                        {row.getValue<string>("name").substring(0, 2)}
                    </div>
                    <span className="font-medium text-foreground underline-offset-4 hover:underline decoration-primary/30">
                        {row.getValue("name")}
                    </span>
                </Link>
            ),
        },
        {
            accessorKey: "type",
            header: "Type",
            cell: ({ row }) => (
                <Badge variant="outline" className="capitalize">
                    {row.getValue("type")}
                </Badge>
            ),
        },
        {
            accessorKey: "enabled",
            header: "State",
            cell: ({ row }) => (
                <div className="flex items-center gap-2">
                    {row.getValue("enabled") ? (
                        <>
                            <BadgeCheck className="h-4 w-4 text-green-500" />
                            <span className="text-sm">Enabled</span>
                        </>
                    ) : (
                        <>
                            <ShieldAlert className="h-4 w-4 text-muted-foreground" />
                            <span className="text-sm text-muted-foreground">Disabled</span>
                        </>
                    )
                    }
                </div>
            ),
        },
        {
            accessorKey: "protected",
            header: "Protection",
            cell: ({ row }) => {
                const isProtected = row.getValue("protected")
                return (
                    <Badge variant={isProtected ? "secondary" : "outline"} className="gap-1">
                        {isProtected ? <Lock className="h-3 w-3" /> : <LockOpen className="h-3 w-3" />}
                        {isProtected ? "Protected" : "None"}
                    </Badge>
                )
            },
        },
        {
            accessorKey: "enabledToggleCount",
            header: "Active Toggles",
            cell: ({ row }) => (
                <div className="flex items-center gap-2">
                    <span className="text-sm font-medium">{row.getValue("enabledToggleCount")}</span>
                    <span className="text-[10px] text-muted-foreground uppercase tracking-widest font-bold">Toggles</span>
                </div>
            ),
        },

    ]

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">Environments</h1>
                    <p className="text-sm text-muted-foreground">
                        Configure and manage your deployment environments.
                    </p>
                </div>
                <Button onClick={() => setDrawerOpen(true)}>
                    <Plus className="mr-2 h-4 w-4" />
                    New Environment
                </Button>
            </div>

            <DataTable
                columns={columns}
                data={environments}
                searchKey="name"
                isLoading={isLoading}
            />

            <CreateEnvironmentDrawer
                open={drawerOpen}
                onOpenChange={setDrawerOpen}
            />
        </div>
    )
}


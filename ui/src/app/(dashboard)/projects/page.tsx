"use client"

import { useState } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { Plus, MoreHorizontal, Trash, Folder } from "lucide-react"
import { ColumnDef } from "@tanstack/react-table"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { DataTable } from "@/components/data-table"
import { getProjects, deleteProject } from "@/features/projects/actions"
import { Project } from "@/types"
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { CreateProjectDrawer } from "@/features/projects/components/create-project-drawer"

export default function ProjectsPage() {
    const queryClient = useQueryClient()
    const [drawerOpen, setDrawerOpen] = useState(false)

    const { data: projects = [], isLoading } = useQuery({
        queryKey: ["projects"],
        queryFn: getProjects,
    })

    const deleteMutation = useMutation({
        mutationFn: deleteProject,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["projects"] })
            toast.success("Project deleted")
        },
        onError: () => toast.error("Failed to delete project")
    })

    const columns: ColumnDef<Project>[] = [
        {
            accessorKey: "name",
            header: "Name",
            cell: ({ row }) => (
                <div className="flex items-center gap-3">
                    <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
                        <Folder className="h-4 w-4" />
                    </div>
                    <div className="flex flex-col">
                        <span className="font-medium text-foreground">{row.getValue("name")}</span>
                        <span className="text-xs text-muted-foreground">{row.original.id}</span>
                    </div>
                </div>
            ),
        },
        {
            accessorKey: "description",
            header: "Description",
            cell: ({ row }) => (
                <span className="text-muted-foreground line-clamp-1 max-w-[300px]">
                    {row.getValue("description") || "No description"}
                </span>
            ),
        },
        {
            accessorKey: "updatedAt",
            header: "Last Updated",
            cell: ({ row }) => (
                <span className="text-muted-foreground text-xs">
                    {row.getValue("updatedAt") ? new Date(row.getValue("updatedAt") as string).toLocaleDateString() : "Never"}
                </span>
            ),
        },
        {
            id: "actions",
            cell: ({ row }) => {
                const project = row.original
                return (
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button variant="ghost" className="h-8 w-8 p-0">
                                <span className="sr-only">Open menu</span>
                                <MoreHorizontal className="h-4 w-4" />
                            </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                            <DropdownMenuLabel>Actions</DropdownMenuLabel>
                            <DropdownMenuItem onClick={() => navigator.clipboard.writeText(project.id)}>
                                Copy ID
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem
                                className="text-destructive hocus:text-destructive"
                                onClick={() => deleteMutation.mutate(project.id)}
                            >
                                <Trash className="mr-2 h-4 w-4" /> Delete project
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                )
            },
        },
    ]

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">Projects</h1>
                    <p className="text-sm text-muted-foreground">
                        Manage your projects and their feature flags.
                    </p>
                </div>
                <Button onClick={() => setDrawerOpen(true)}>
                    <Plus className="mr-2 h-4 w-4" />
                    New Project
                </Button>
            </div>

            <DataTable
                columns={columns}
                data={projects}
                searchKey="name"
                isLoading={isLoading}
            />

            <CreateProjectDrawer
                open={drawerOpen}
                onOpenChange={setDrawerOpen}
            />
        </div>
    )
}

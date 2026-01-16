"use client"

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { getContexts, deleteContext } from "../actions"
import { DataTable } from "@/components/data-table"
import { ColumnDef } from "@tanstack/react-table"
import { ContextField } from "@/types"
import { Button } from "@/components/ui/button"
import { ArrowUpDown, Trash2, Check, X } from "lucide-react"
import { toast } from "sonner"
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogTrigger,
} from "@/components/ui/alert-dialog"

export function ContextList() {
    const queryClient = useQueryClient()

    const { data: contexts = [], isLoading } = useQuery({
        queryKey: ["contexts"],
        queryFn: getContexts
    })

    const deleteMutation = useMutation({
        mutationFn: deleteContext,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["contexts"] })
            toast.success("Context deleted")
        },
        onError: (error: any) => {
            if (error?.status === 409) {
                toast.error("Cannot delete context: field is in use by one or more strategies")
            } else {
                toast.error("Failed to delete context")
            }
        }
    })

    const columns: ColumnDef<ContextField>[] = [
        {
            accessorKey: "name",
            header: ({ column }) => {
                return (
                    <Button
                        variant="ghost"
                        onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
                    >
                        Name
                        <ArrowUpDown className="ml-2 h-4 w-4" />
                    </Button>
                )
            },
        },
        {
            accessorKey: "description",
            header: "Description",
        },
        {
            accessorKey: "stickiness",
            header: "Stickiness",
            cell: ({ row }) => (
                row.original.stickiness ? <Check className="h-4 w-4 text-green-500" /> : <X className="h-4 w-4 text-muted-foreground" />
            )
        },

        {
            id: "actions",
            cell: ({ row }) => {
                return (
                    <AlertDialog>
                        <AlertDialogTrigger asChild>
                            <Button variant="ghost" size="icon" className="text-destructive hover:text-destructive hover:bg-destructive/10">
                                <Trash2 className="h-4 w-4" />
                            </Button>
                        </AlertDialogTrigger>
                        <AlertDialogContent>
                            <AlertDialogHeader>
                                <AlertDialogTitle>Delete Context Field?</AlertDialogTitle>
                                <AlertDialogDescription>
                                    This action cannot be undone. This will permanently delete the context field
                                    <span className="font-bold text-foreground"> {row.original.name}</span>.
                                    Existing strategies using this field may break.
                                </AlertDialogDescription>
                            </AlertDialogHeader>
                            <AlertDialogFooter>
                                <AlertDialogCancel>Cancel</AlertDialogCancel>
                                <AlertDialogAction onClick={() => deleteMutation.mutate(row.original.name)} className="bg-destructive hover:bg-destructive/90">
                                    Delete
                                </AlertDialogAction>
                            </AlertDialogFooter>
                        </AlertDialogContent>
                    </AlertDialog>
                )
            }
        }
    ]

    return (
        <DataTable
            columns={columns}
            data={contexts}
            searchKey="name"
            isLoading={isLoading}
        />
    )
}

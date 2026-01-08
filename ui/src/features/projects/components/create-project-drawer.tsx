"use client"

import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { toast } from "sonner"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import {
    Sheet,
    SheetContent,
    SheetDescription,
    SheetHeader,
    SheetTitle,
    SheetFooter
} from "@/components/ui/sheet"
import { Button } from "@/components/ui/button"
import {
    Form,
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { createProject } from "@/features/projects/actions"

const projectSchema = z.object({
    id: z.string().min(1, "ID is required").regex(/^[a-zA-Z0-9-]+$/, "ID must be URL-friendly"),
    name: z.string().min(1, "Name is required"),
    description: z.string().optional(),
})

type ProjectFormValues = z.infer<typeof projectSchema>

interface CreateProjectDrawerProps {
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function CreateProjectDrawer({ open, onOpenChange }: CreateProjectDrawerProps) {
    const queryClient = useQueryClient()
    const form = useForm<ProjectFormValues>({
        resolver: zodResolver(projectSchema),
        defaultValues: {
            id: "",
            name: "",
            description: "",
        },
    })

    const mutation = useMutation({
        mutationFn: createProject,
        onSuccess: () => {
            toast.success("Project created successfully")
            queryClient.invalidateQueries({ queryKey: ["projects"] })
            queryClient.invalidateQueries({ queryKey: ["dashboard-summary"] })
            onOpenChange(false)
            form.reset()
        },
        onError: (error: any) => {
            toast.error(error.message || "Failed to create project")
        }
    })

    async function onSubmit(values: ProjectFormValues) {
        mutation.mutate(values)
    }

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent className="sm:max-w-md">
                <SheetHeader>
                    <SheetTitle>Create Project</SheetTitle>
                    <SheetDescription>
                        Add a new project to organize your feature flags.
                    </SheetDescription>
                </SheetHeader>
                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 py-6">
                        <FormField
                            control={form.control}
                            name="id"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Project ID</FormLabel>
                                    <FormControl>
                                        <Input placeholder="e.g. my-awesome-project" {...field} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name="name"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Name</FormLabel>
                                    <FormControl>
                                        <Input placeholder="Project Name" {...field} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name="description"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Description</FormLabel>
                                    <FormControl>
                                        <Input placeholder="Optional description" {...field} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <SheetFooter>
                            <Button type="submit" disabled={mutation.isPending}>
                                {mutation.isPending ? "Creating..." : "Create Project"}
                            </Button>
                        </SheetFooter>
                    </form>
                </Form>
            </SheetContent>
        </Sheet>
    )
}


"use client"

import { useEffect } from "react"
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
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { createFeature } from "@/features/features/actions"

import { Project } from "@/types"

const featureSchema = z.object({
    name: z.string().min(1, "Name is required").regex(/^[a-zA-Z0-9-]+$/, "Name must be URL-friendly"),
    type: z.string().min(1, "Type is required"),
    projectId: z.string().min(1, "Project is required"),
    description: z.string().optional(),
})

type FeatureFormValues = z.infer<typeof featureSchema>

interface CreateFeatureDrawerProps {
    open: boolean
    onOpenChange: (open: boolean) => void
    projectId: string
    projects: Project[]
}

export function CreateFeatureDrawer({ open, onOpenChange, projectId, projects }: CreateFeatureDrawerProps) {
    const queryClient = useQueryClient()
    const form = useForm<FeatureFormValues>({
        resolver: zodResolver(featureSchema),
        defaultValues: {
            name: "",
            type: "release",
            projectId: projectId || "default",
            description: "",
        },
    })

    // Update form when projectId changes or drawer opens
    useEffect(() => {
        if (open) {
            form.setValue("projectId", projectId || "default")
        }
    }, [open, projectId, form])


    const mutation = useMutation({
        mutationFn: (values: FeatureFormValues) => createFeature(values.projectId, values),
        onSuccess: (_, variables) => {
            toast.success("Feature created successfully")
            queryClient.invalidateQueries({ queryKey: ["features", variables.projectId] })
            queryClient.invalidateQueries({ queryKey: ["environments"] })
            onOpenChange(false)
            form.reset({
                ...form.getValues(),
                name: "",
                description: ""
            })
        },
        onError: (error: any) => {
            toast.error(error.message || "Failed to create feature")
        }
    })

    async function onSubmit(values: FeatureFormValues) {
        mutation.mutate(values)
    }

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent className="sm:max-w-md">
                <SheetHeader>
                    <SheetTitle>New Feature Toggle</SheetTitle>
                    <SheetDescription>
                        Create a new feature toggle in your project.
                    </SheetDescription>
                </SheetHeader>
                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 py-6">
                        <FormField
                            control={form.control}
                            name="name"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Feature Name</FormLabel>
                                    <FormControl>
                                        <Input placeholder="e.g. beta-users-only" {...field} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name="projectId"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Project</FormLabel>
                                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                                        <FormControl>
                                            <SelectTrigger>
                                                <SelectValue placeholder="Select project" />
                                            </SelectTrigger>
                                        </FormControl>
                                        <SelectContent>
                                            {projects.map((p) => (
                                                <SelectItem key={p.id} value={p.id}>{p.name}</SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name="type"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Type</FormLabel>
                                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                                        <FormControl>
                                            <SelectTrigger>
                                                <SelectValue placeholder="Select type" />
                                            </SelectTrigger>
                                        </FormControl>
                                        <SelectContent>
                                            <SelectItem value="release">Release</SelectItem>
                                            <SelectItem value="experiment">Experiment</SelectItem>
                                            <SelectItem value="operational">Operational</SelectItem>
                                            <SelectItem value="kill-switch">Kill Switch</SelectItem>
                                            <SelectItem value="permission">Permission</SelectItem>
                                        </SelectContent>
                                    </Select>
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
                                {mutation.isPending ? "Creating..." : "Create Toggle"}
                            </Button>
                        </SheetFooter>
                    </form>
                </Form>
            </SheetContent>
        </Sheet>
    )
}



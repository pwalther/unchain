"use client"

import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog"
import {
    Form,
    FormControl,
    FormDescription,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { createContext } from "../actions"
import { toast } from "sonner"
import { ContextField } from "@/types"

const formSchema = z.object({
    name: z.string().min(1, "Name is required").regex(/^[a-zA-Z0-9_-]+$/, "Name implies a variable, keep it simple (alphanumeric, -, _)"),
    description: z.string().optional(),
    stickiness: z.boolean().default(false),
    sortOrder: z.coerce.number().optional()
})

interface ContextDialogProps {
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function ContextDialog({ open, onOpenChange }: ContextDialogProps) {
    const queryClient = useQueryClient()

    const form = useForm({
        resolver: zodResolver(formSchema),
        defaultValues: {
            name: "",
            description: "",
            stickiness: false,
            sortOrder: 100
        },
    })

    const mutation = useMutation({
        mutationFn: createContext,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["contexts"] })
            toast.success("Context field created")
            onOpenChange(false)
            form.reset()
        },
        onError: () => {
            toast.error("Failed to create context field")
        }
    })

    function onSubmit(values: z.infer<typeof formSchema>) {
        mutation.mutate(values as ContextField)
    }

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Create Context Field</DialogTitle>
                    <DialogDescription>
                        Define a new context field available for strategies.
                    </DialogDescription>
                </DialogHeader>

                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                        <FormField
                            control={form.control}
                            name="name"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Name</FormLabel>
                                    <FormControl>
                                        <Input placeholder="region, userId, tenantId..." {...field} />
                                    </FormControl>
                                    <FormDescription>
                                        The key used in the application code.
                                    </FormDescription>
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
                                        <Input placeholder="Brief description..." {...field} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name="stickiness"
                            render={({ field }) => (
                                <FormItem className="flex flex-row items-start space-x-3 space-y-0 rounded-md border p-4">
                                    <FormControl>
                                        <Checkbox
                                            checked={field.value}
                                            onCheckedChange={field.onChange}
                                        />
                                    </FormControl>
                                    <div className="space-y-1 leading-none">
                                        <FormLabel>
                                            Available for Stickiness
                                        </FormLabel>
                                        <FormDescription>
                                            Allow this field to be used for consistent variant distribution (e.g. A/B testing).
                                        </FormDescription>
                                    </div>
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name="sortOrder"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Sort Order</FormLabel>
                                    <FormControl>
                                        <Input type="number" {...field} value={field.value as string | number | undefined ?? ''} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <DialogFooter>
                            <Button type="submit" disabled={mutation.isPending}>
                                {mutation.isPending ? "Creating..." : "Create"}
                            </Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    )
}

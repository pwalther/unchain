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
    SheetFooter,
} from "@/components/ui/sheet"
import { Button } from "@/components/ui/button"
import {
    Form,
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
    FormDescription,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { createEnvironment } from "@/features/environments/actions"

const environmentSchema = z.object({
    name: z.string().min(1, "Name is required").regex(/^[a-zA-Z0-9-]+$/, "Name must be URL-friendly"),
    type: z.string().min(1, "Type is required"),
    requiredApprovals: z.number().int("Must be a whole number").min(0, "Must be 0 or more").max(10, "Must be 10 or less"),
})

type EnvironmentFormValues = z.infer<typeof environmentSchema>

interface CreateEnvironmentDrawerProps {
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function CreateEnvironmentDrawer({ open, onOpenChange }: CreateEnvironmentDrawerProps) {
    const queryClient = useQueryClient()
    const form = useForm<EnvironmentFormValues>({
        resolver: zodResolver(environmentSchema),
        defaultValues: {
            name: "",
            type: "development",
            requiredApprovals: 0,
        },
    })

    const mutation = useMutation({
        mutationFn: createEnvironment,
        onSuccess: () => {
            toast.success("Environment created successfully")
            queryClient.invalidateQueries({ queryKey: ["environments"] })
            onOpenChange(false)
            form.reset()
        },
        onError: (error: any) => {
            toast.error(error.message || "Failed to create environment")
        }
    })

    async function onSubmit(values: EnvironmentFormValues) {
        mutation.mutate(values)
    }

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent className="sm:max-w-[470px] overflow-y-auto">
                <SheetHeader>
                    <SheetTitle>Create Environment</SheetTitle>
                    <SheetDescription>
                        Add a new environment to manage your feature flags across different stages.
                    </SheetDescription>
                </SheetHeader>
                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 py-6">
                        <FormField
                            control={form.control}
                            name="name"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Environment Name</FormLabel>
                                    <FormControl>
                                        <Input placeholder="e.g. production, staging, dev" {...field} />
                                    </FormControl>
                                    <FormDescription>
                                        A unique name for the environment.
                                    </FormDescription>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name="requiredApprovals"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Required Approvals</FormLabel>
                                    <FormControl>
                                        <Input
                                            type="number"
                                            min="0"
                                            max="10"
                                            step="1"
                                            {...field}
                                            onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                                                const val = parseInt(e.target.value);
                                                field.onChange(isNaN(val) ? 0 : Math.min(10, Math.max(0, val)));
                                            }}
                                        />
                                    </FormControl>
                                    <FormDescription>
                                        Number of approvals required for change requests.
                                    </FormDescription>
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
                                                <SelectValue placeholder="Select a type" />
                                            </SelectTrigger>
                                        </FormControl>
                                        <SelectContent>
                                            <SelectItem value="development">Development</SelectItem>
                                            <SelectItem value="test">Test</SelectItem>
                                            <SelectItem value="preproduction">Pre-production</SelectItem>
                                            <SelectItem value="production">Production</SelectItem>
                                        </SelectContent>
                                    </Select>
                                    <FormDescription>
                                        The type of environment helps with organization and defaults.
                                    </FormDescription>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <SheetFooter className="mt-8">
                            <Button type="submit" className="w-full" disabled={mutation.isPending}>
                                {mutation.isPending ? "Creating..." : "Create Environment"}
                            </Button>
                        </SheetFooter>
                    </form>
                </Form>
            </SheetContent>
        </Sheet>
    )
}

"use client"

import { useState } from "react"
import { useForm, useFieldArray } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { Plus, Trash, Zap, Type, Hash, Percent, ListIcon, CheckCircle2 } from "lucide-react"
import { toast } from "sonner"

import {
    Sheet,
    SheetContent,
    SheetDescription,
    SheetHeader,
    SheetTitle,
    SheetFooter
} from "@/components/ui/sheet"
import {
    Form,
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
    FormDescription
} from "@/components/ui/form"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue
} from "@/components/ui/select"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import { createStrategyDefinition } from "@/features/strategies/actions"

const parameterSchema = z.object({
    name: z.string().min(1, "Parameter name is required"),
    type: z.enum(['string', 'percentage', 'list', 'number', 'boolean']),
    description: z.string().optional(),
    required: z.boolean(),
})

const formSchema = z.object({
    name: z.string().min(1, "Strategy name is required"),
    description: z.string().optional(),
    parameters: z.array(parameterSchema),
})

type FormValues = z.infer<typeof formSchema>

interface CreateStrategyDefinitionDrawerProps {
    open: boolean
    onOpenChange: (open: boolean) => void
    onSuccess: () => void
}

export function CreateStrategyDefinitionDrawer({
    open,
    onOpenChange,
    onSuccess
}: CreateStrategyDefinitionDrawerProps) {
    const [isPending, setIsPending] = useState(false)

    const form = useForm<FormValues>({
        resolver: zodResolver(formSchema),
        defaultValues: {
            name: "",
            description: "",
            parameters: [],
        },
    })

    const { fields, append, remove } = useFieldArray({
        control: form.control,
        name: "parameters",
    })

    async function onSubmit(values: FormValues) {
        setIsPending(true)
        try {
            await createStrategyDefinition(values)
            toast.success("Strategy definition created successfully")
            form.reset()
            onOpenChange(false)
            onSuccess()
        } catch (error) {
            toast.error("Failed to create strategy definition")
            console.error(error)
        } finally {
            setIsPending(false)
        }
    }

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent className="sm:max-w-[540px] overflow-y-auto">
                <SheetHeader>
                    <SheetTitle className="flex items-center gap-2">
                        <Zap className="h-5 w-5 text-amber-500" />
                        Create Strategy Definition
                    </SheetTitle>
                    <SheetDescription>
                        Define a new custom activation strategy. Definitions are global and can be used across all features.
                    </SheetDescription>
                </SheetHeader>

                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8 py-6">
                        <div className="space-y-4">
                            <FormField
                                control={form.control}
                                name="name"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Name</FormLabel>
                                        <FormControl>
                                            <Input placeholder="e.g. cityRollout" {...field} />
                                        </FormControl>
                                        <FormDescription>
                                            A unique name for your strategy.
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
                                            <Input placeholder="What does this strategy do?" {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>

                        <Separator />

                        <div className="space-y-4">
                            <div className="flex items-center justify-between">
                                <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">Parameters</h3>
                                <Button
                                    type="button"
                                    variant="outline"
                                    size="sm"
                                    className="gap-2"
                                    onClick={() => append({ name: "", type: "string", description: "", required: false })}
                                >
                                    <Plus className="h-4 w-4" />
                                    Add Parameter
                                </Button>
                            </div>

                            {fields.length === 0 ? (
                                <div className="text-center py-8 border rounded-lg bg-muted/10 border-dashed">
                                    <p className="text-sm text-muted-foreground italic">No parameters defined yet.</p>
                                </div>
                            ) : (
                                <div className="space-y-6">
                                    {fields.map((field, index) => (
                                        <div key={field.id} className="relative p-4 border rounded-xl bg-muted/5 space-y-4 animate-in fade-in slide-in-from-right-2 duration-300">
                                            <Button
                                                type="button"
                                                variant="ghost"
                                                size="icon"
                                                className="absolute top-2 right-2 h-8 w-8 text-destructive hover:bg-destructive/10"
                                                onClick={() => remove(index)}
                                            >
                                                <Trash className="h-4 w-4" />
                                            </Button>

                                            <div className="grid grid-cols-2 gap-4 pt-2">
                                                <FormField
                                                    control={form.control}
                                                    name={`parameters.${index}.name`}
                                                    render={({ field }) => (
                                                        <FormItem>
                                                            <FormLabel className="text-[11px] font-bold uppercase">Param Name</FormLabel>
                                                            <FormControl>
                                                                <Input placeholder="percentage" {...field} className="h-9" />
                                                            </FormControl>
                                                            <FormMessage />
                                                        </FormItem>
                                                    )}
                                                />

                                                <FormField
                                                    control={form.control}
                                                    name={`parameters.${index}.type`}
                                                    render={({ field }) => (
                                                        <FormItem>
                                                            <FormLabel className="text-[11px] font-bold uppercase">Type</FormLabel>
                                                            <Select onValueChange={field.onChange} defaultValue={field.value}>
                                                                <FormControl>
                                                                    <SelectTrigger className="h-9">
                                                                        <SelectValue />
                                                                    </SelectTrigger>
                                                                </FormControl>
                                                                <SelectContent>
                                                                    <SelectItem value="string">
                                                                        <div className="flex items-center gap-2">
                                                                            <Type className="h-3 w-3" /> <span>String</span>
                                                                        </div>
                                                                    </SelectItem>
                                                                    <SelectItem value="percentage">
                                                                        <div className="flex items-center gap-2">
                                                                            <Percent className="h-3 w-3" /> <span>Percentage</span>
                                                                        </div>
                                                                    </SelectItem>
                                                                    <SelectItem value="list">
                                                                        <div className="flex items-center gap-2">
                                                                            <ListIcon className="h-3 w-3" /> <span>List</span>
                                                                        </div>
                                                                    </SelectItem>
                                                                    <SelectItem value="number">
                                                                        <div className="flex items-center gap-2">
                                                                            <Hash className="h-3 w-3" /> <span>Number</span>
                                                                        </div>
                                                                    </SelectItem>
                                                                    <SelectItem value="boolean">
                                                                        <div className="flex items-center gap-2">
                                                                            <CheckCircle2 className="h-3 w-3" /> <span>Boolean</span>
                                                                        </div>
                                                                    </SelectItem>
                                                                </SelectContent>
                                                            </Select>
                                                            <FormMessage />
                                                        </FormItem>
                                                    )}
                                                />
                                            </div>

                                            <FormField
                                                control={form.control}
                                                name={`parameters.${index}.description`}
                                                render={({ field }) => (
                                                    <FormItem>
                                                        <FormLabel className="text-[11px] font-bold uppercase">Description (optional)</FormLabel>
                                                        <FormControl>
                                                            <Input placeholder="What is this used for?" {...field} className="h-9" />
                                                        </FormControl>
                                                        <FormMessage />
                                                    </FormItem>
                                                )}
                                            />

                                            <FormField
                                                control={form.control}
                                                name={`parameters.${index}.required`}
                                                render={({ field }) => (
                                                    <FormItem className="flex flex-row items-start space-x-2 space-y-0 rounded-md border p-3 bg-card">
                                                        <FormControl>
                                                            <Checkbox
                                                                checked={field.value}
                                                                onCheckedChange={field.onChange}
                                                            />
                                                        </FormControl>
                                                        <div className="space-y-1 leading-none">
                                                            <FormLabel className="text-sm font-medium">
                                                                Mark as required
                                                            </FormLabel>
                                                            <p className="text-[10px] text-muted-foreground">
                                                                Forces the user to provide a value when using this strategy.
                                                            </p>
                                                        </div>
                                                    </FormItem>
                                                )}
                                            />
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>

                        <SheetFooter>
                            <Button
                                type="submit"
                                className="w-full h-11 transition-all hover:shadow-lg hover:shadow-primary/20"
                                disabled={isPending}
                            >
                                {isPending ? "Creating..." : "Create Strategy Definition"}
                            </Button>
                        </SheetFooter>
                    </form>
                </Form>
            </SheetContent>
        </Sheet>
    )
}

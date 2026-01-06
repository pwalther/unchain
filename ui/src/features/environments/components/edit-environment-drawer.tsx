"use client"

import { useEffect } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { Settings, Info } from "lucide-react"

import {
    Sheet,
    SheetContent,
    SheetDescription,
    SheetHeader,
    SheetTitle,
    SheetFooter,
} from "@/components/ui/sheet"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
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
import { Slider } from "@/components/ui/slider"
import { Environment } from "@/types"

const editSchema = z.object({
    type: z.string().min(1, "Type is required"),
    requiredApprovals: z.number().int("Must be a whole number").min(0, "Must be 0 or more").max(10, "Must be 10 or less"),
})

type EditFormValues = z.infer<typeof editSchema>

interface EditEnvironmentDrawerProps {
    open: boolean
    onOpenChange: (open: boolean) => void
    environment: Environment
    onSubmit: (values: EditFormValues) => void
    isLoading?: boolean
}

export function EditEnvironmentDrawer({
    open,
    onOpenChange,
    environment,
    onSubmit,
    isLoading
}: EditEnvironmentDrawerProps) {
    const form = useForm<EditFormValues>({
        resolver: zodResolver(editSchema),
        defaultValues: {
            type: environment.type,
            requiredApprovals: environment.requiredApprovals || 0,
        },
    })

    // Update form when environment data changes or drawer opens
    useEffect(() => {
        if (open) {
            form.reset({
                type: environment.type,
                requiredApprovals: environment.requiredApprovals || 0,
            })
        }
    }, [open, environment, form])

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent className="sm:max-w-[470px] border-l border-muted-foreground/10 bg-background/95 backdrop-blur-md">
                <SheetHeader className="space-y-4">
                    <SheetTitle className="flex items-center gap-2 text-2xl italic font-bold">
                        <Settings className="h-5 w-5 text-primary" />
                        Edit Environment
                    </SheetTitle>
                    <SheetDescription className="text-sm">
                        Update the configuration for <span className="font-bold text-primary italic">"{environment.name}"</span>.
                    </SheetDescription>
                </SheetHeader>

                <div className="mt-8 p-4 bg-primary/5 rounded-xl border border-primary/10 flex gap-3 text-sm text-primary/80">
                    <Info className="h-5 w-5 shrink-0" />
                    <p>Some properties like <strong>Name</strong> and <strong>Protection Status</strong> cannot be changed here for safety reasons.</p>
                </div>

                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6 py-8">
                        <FormField
                            control={form.control}
                            name="type"
                            render={({ field }) => (
                                <FormItem className="space-y-3">
                                    <div className="flex items-center justify-between">
                                        <FormLabel className="text-sm font-bold uppercase tracking-widest text-muted-foreground">Environment Type</FormLabel>
                                    </div>
                                    <Select onValueChange={field.onChange} defaultValue={field.value} value={field.value}>
                                        <FormControl>
                                            <SelectTrigger className="h-12 border-muted-foreground/20 focus:ring-primary/20">
                                                <SelectValue placeholder="Select a type" />
                                            </SelectTrigger>
                                        </FormControl>
                                        <SelectContent>
                                            <SelectItem value="development" className="italic">Development</SelectItem>
                                            <SelectItem value="test" className="italic">Test</SelectItem>
                                            <SelectItem value="preproduction" className="italic">Pre-production</SelectItem>
                                            <SelectItem value="production" className="italic">Production</SelectItem>
                                        </SelectContent>
                                    </Select>
                                    <FormDescription className="text-xs italic">
                                        The type helps organize environments and apply default behaviors.
                                    </FormDescription>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="requiredApprovals"
                            render={({ field }) => (
                                <FormItem className="space-y-4">
                                    <div className="flex items-center justify-between">
                                        <FormLabel className="text-sm font-bold uppercase tracking-widest text-muted-foreground">Required Approvals</FormLabel>
                                        <Badge variant="secondary" className="font-mono text-primary bg-primary/10 border-primary/20">
                                            {field.value}
                                        </Badge>
                                    </div>
                                    <FormControl>
                                        <div className="py-2">
                                            <Slider
                                                min={0}
                                                max={10}
                                                step={1}
                                                value={[field.value]}
                                                onValueChange={(vals) => field.onChange(vals[0])}
                                                className="cursor-pointer"
                                            />
                                        </div>
                                    </FormControl>
                                    <FormDescription className="text-xs italic">
                                        Number of approvals required for change requests in this environment (0-10).
                                    </FormDescription>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <SheetFooter className="absolute bottom-6 left-6 right-6 gap-2 sm:gap-0">
                            <Button
                                type="button"
                                variant="ghost"
                                className="flex-1"
                                onClick={() => onOpenChange(false)}
                            >
                                Cancel
                            </Button>
                            <Button
                                type="submit"
                                className="flex-1 bg-primary hover:bg-primary/90 shadow-lg shadow-primary/20 transition-all active:scale-95"
                                disabled={isLoading}
                            >
                                {isLoading ? "Updating..." : "Save Changes"}
                            </Button>
                        </SheetFooter>
                    </form>
                </Form>
            </SheetContent>
        </Sheet>
    )
}

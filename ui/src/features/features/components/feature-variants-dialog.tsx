"use client"

import { useForm, useFieldArray } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { Plus, Trash2, Package } from "lucide-react"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog"
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
import { Variant, Feature } from "@/types"

const variantSchema = z.object({
    name: z.string().min(1, "Name is required"),
    weight: z.number().min(0).max(1000),
    stickiness: z.string().nullable().optional(),
    payload: z.object({
        type: z.enum(["string", "json", "number"]),
        value: z.string()
    }).nullable().optional()
})

const formSchema = z.object({
    variants: z.array(variantSchema)
})

type FormValues = z.infer<typeof formSchema>

interface FeatureVariantsDialogProps {
    open: boolean
    onOpenChange: (open: boolean) => void
    feature: Feature
    onSave: (variants: Variant[]) => void
    contextFields: { name: string }[]
}

export function FeatureVariantsDialog({ open, onOpenChange, feature, onSave, contextFields }: FeatureVariantsDialogProps) {
    const form = useForm<FormValues>({
        resolver: zodResolver(formSchema),
        defaultValues: {
            variants: feature.variants || []
        }
    })

    const { fields, append, remove } = useFieldArray({
        control: form.control,
        name: "variants"
    })

    function onSubmit(values: FormValues) {
        onSave(values.variants as Variant[])
        onOpenChange(false)
    }

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-y-auto">
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2">
                        <Package className="h-5 w-5 text-primary" />
                        Manage Feature Variants
                    </DialogTitle>
                    <DialogDescription>
                        Define variants for this feature toggle. These will be used for all environments unless overridden.
                    </DialogDescription>
                </DialogHeader>

                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6 py-4">
                        <div className="space-y-4">
                            {fields.map((field, index) => (
                                <div key={field.id} className="p-4 rounded-xl border bg-muted/30 space-y-4 relative group">
                                    <Button
                                        type="button"
                                        variant="ghost"
                                        size="icon"
                                        className="absolute top-2 right-2 h-8 w-8 opacity-0 group-hover:opacity-100 transition-opacity text-destructive"
                                        onClick={() => remove(index)}
                                    >
                                        <Trash2 className="h-4 w-4" />
                                    </Button>

                                    <div className="grid grid-cols-2 gap-4">
                                        <FormField
                                            control={form.control}
                                            name={`variants.${index}.name`}
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel className="text-xs font-bold uppercase text-muted-foreground">Variant Name</FormLabel>
                                                    <FormControl>
                                                        <Input {...field} placeholder="e.g. control" />
                                                    </FormControl>
                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />
                                        <FormField
                                            control={form.control}
                                            name={`variants.${index}.weight`}
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel className="text-xs font-bold uppercase text-muted-foreground">Weight (0-1000)</FormLabel>
                                                    <FormControl>
                                                        <Input
                                                            {...field}
                                                            type="number"
                                                            onChange={e => field.onChange(parseInt(e.target.value))}
                                                        />
                                                    </FormControl>
                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />
                                    </div>

                                    <FormField
                                        control={form.control}
                                        name={`variants.${index}.stickiness`}
                                        render={({ field }) => (
                                            <FormItem>
                                                <FormLabel className="text-xs font-bold uppercase text-muted-foreground">Stickiness Override</FormLabel>
                                                <Select
                                                    onValueChange={field.onChange}
                                                    defaultValue={field.value || ""}
                                                    value={field.value || ""}
                                                >
                                                    <FormControl>
                                                        <SelectTrigger>
                                                            <SelectValue placeholder="Default (userId)" />
                                                        </SelectTrigger>
                                                    </FormControl>
                                                    <SelectContent>
                                                        <SelectItem value="default">Default (userId)</SelectItem>
                                                        {contextFields.map(cf => (
                                                            <SelectItem key={cf.name} value={cf.name}>{cf.name}</SelectItem>
                                                        ))}
                                                    </SelectContent>
                                                </Select>
                                            </FormItem>
                                        )}
                                    />
                                </div>
                            ))}

                            <Button
                                type="button"
                                variant="outline"
                                className="w-full border-dashed gap-2"
                                onClick={() => append({ name: "", weight: 0, stickiness: "default", payload: null })}
                            >
                                <Plus className="h-4 w-4" />
                                Add Variant
                            </Button>
                        </div>

                        <DialogFooter className="pt-4 border-t">
                            <Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>
                                Cancel
                            </Button>
                            <Button type="submit">
                                Save Variants
                            </Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    )
}

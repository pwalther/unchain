"use client"

import { useForm, useFieldArray } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { Plus, Trash2, Package, Type, Hash, Code2 } from "lucide-react"
import { JsonEditor } from "@/components/ui/json-editor"
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
}).superRefine((data, ctx) => {
    if (data.payload?.type === 'json' && data.payload.value) {
        try {
            JSON.parse(data.payload.value)
        } catch (e) {
            ctx.addIssue({
                code: z.ZodIssueCode.custom,
                message: "Invalid JSON format",
                path: ['payload', 'value']
            })
        }
    }
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

                                    <div className="grid grid-cols-2 gap-4">
                                        <FormField
                                            control={form.control}
                                            name={`variants.${index}.stickiness`}
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground/70">Stickiness Override</FormLabel>
                                                    <Select
                                                        onValueChange={field.onChange}
                                                        defaultValue={field.value || ""}
                                                        value={field.value || ""}
                                                    >
                                                        <FormControl>
                                                            <SelectTrigger className="h-10 bg-background/50">
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

                                        <FormField
                                            control={form.control}
                                            name={`variants.${index}.payload.type`}
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground/70">Payload Type</FormLabel>
                                                    <Select
                                                        onValueChange={(val) => {
                                                            field.onChange(val)
                                                            if (!form.getValues(`variants.${index}.payload.value`)) {
                                                                form.setValue(`variants.${index}.payload.value`, val === 'json' ? '{}' : '')
                                                            }
                                                        }}
                                                        defaultValue={field.value || "string"}
                                                        value={field.value || "string"}
                                                    >
                                                        <FormControl>
                                                            <SelectTrigger className="h-10 bg-background/50">
                                                                <SelectValue placeholder="Select type" />
                                                            </SelectTrigger>
                                                        </FormControl>
                                                        <SelectContent>
                                                            <SelectItem value="string">
                                                                <div className="flex items-center gap-2"><Type className="h-3.5 w-3.5" /> String</div>
                                                            </SelectItem>
                                                            <SelectItem value="json">
                                                                <div className="flex items-center gap-2"><Code2 className="h-3.5 w-3.5" /> JSON</div>
                                                            </SelectItem>
                                                            <SelectItem value="number">
                                                                <div className="flex items-center gap-2"><Hash className="h-3.5 w-3.5" /> Number</div>
                                                            </SelectItem>
                                                        </SelectContent>
                                                    </Select>
                                                </FormItem>
                                            )}
                                        />
                                    </div>

                                    <FormField
                                        control={form.control}
                                        name={`variants.${index}.payload.value`}
                                        render={({ field }) => (
                                            <FormItem>
                                                <FormLabel className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground/70">Payload Value</FormLabel>
                                                <FormControl>
                                                    {form.watch(`variants.${index}.payload.type`) === 'json' ? (
                                                        <JsonEditor
                                                            value={field.value || "{}"}
                                                            onChange={field.onChange}
                                                            className="min-h-[100px]"
                                                        />
                                                    ) : (
                                                        <Input
                                                            {...field}
                                                            value={field.value ?? ""}
                                                            type={form.watch(`variants.${index}.payload.type`) === 'number' ? 'number' : 'text'}
                                                            placeholder={form.watch(`variants.${index}.payload.type`) === 'json' ? '{"key": "value"}' : 'Enter payload value'}
                                                            className="h-10 bg-background/50"
                                                        />
                                                    )}
                                                </FormControl>
                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />
                                </div>
                            ))}

                            <Button
                                type="button"
                                variant="outline"
                                className="w-full border-dashed gap-2"
                                onClick={() => append({
                                    name: "",
                                    weight: 0,
                                    stickiness: "default",
                                    payload: { type: "string", value: "" }
                                })}
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

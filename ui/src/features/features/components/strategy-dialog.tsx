"use client"

import { useState, useRef, useEffect } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { X, Plus, Hash, Percent, ListIcon, Type, CheckCircle2, Settings, Filter, Trash2, Calendar, Globe, User, Code2, Clock } from "lucide-react"
import { JsonEditor } from "@/components/ui/json-editor"

import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogFooter
} from "@/components/ui/dialog"
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
import { StrategyDefinition, StrategyParameterDefinition, Strategy, Constraint, ContextField, Variant } from "@/types"
import { useQuery } from "@tanstack/react-query"
import { getContexts } from "@/features/contexts/actions"
import { Switch } from "@/components/ui/switch"
import { Label } from "@/components/ui/label"
import { useFieldArray } from "react-hook-form"

const formSchema = z.object({
    name: z.string().min(1, "Strategy name is required"),
    parameters: z.record(z.string(), z.union([z.string(), z.boolean(), z.array(z.string())])),
    constraints: z.array(z.object({
        contextName: z.string().min(1, "Context name is required"),
        operator: z.string().min(1, "Operator is required"),
        values: z.array(z.string()).min(1, "At least one value is required"),
        caseInsensitive: z.boolean(),
        inverted: z.boolean(),
    })).superRefine((data, ctx) => {
        data.forEach((constraint, index) => {
            if (constraint.operator === 'IN_TIME_WINDOW') {
                const value = constraint.values[0] || "";
                // Format: HH:mm-HH:mm or HH:mm-HH:mm|Region/City
                const parts = value.split('|');
                const timePart = parts[0];
                const timeRangeRegex = /^([0-1]?[0-9]|2[0-3]):[0-5][0-9]-([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/;

                if (!timePart.match(timeRangeRegex)) {
                    ctx.addIssue({
                        code: z.ZodIssueCode.custom,
                        message: "Invalid time window format (HH:mm-HH:mm)",
                        path: [index, 'values', 0]
                    });
                }
            } else if (['DATE_AFTER', 'DATE_BEFORE'].includes(constraint.operator)) {
                const value = constraint.values[0] || "";
                if (!Date.parse(value)) {
                    ctx.addIssue({
                        code: z.ZodIssueCode.custom,
                        message: "Invalid date format",
                        path: [index, 'values', 0]
                    });
                }
            }
        });
    }),
    variants: z.array(z.object({
        name: z.string().min(1, "Variant name is required"),
        weight: z.number().min(0).max(1000),
        stickiness: z.string().optional().nullable(),
        payload: z.object({
            type: z.enum(["string", "json", "number"]),
            value: z.string()
        }).optional().nullable()
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
    })),
})

type FormValues = z.infer<typeof formSchema>

interface StrategyDialogProps {
    open: boolean
    onOpenChange: (open: boolean) => void
    strategyDefinitions: StrategyDefinition[]
    strategy?: Strategy | null
    onSave: (values: any) => void
    loading?: boolean
}

export function StrategyDialog({
    open,
    onOpenChange,
    strategyDefinitions,
    strategy,
    onSave,
    loading
}: StrategyDialogProps) {
    const isEdit = !!strategy
    const [selectedDef, setSelectedDef] = useState<StrategyDefinition | null>(null)

    const form = useForm<FormValues>({
        resolver: zodResolver(formSchema),
        defaultValues: {
            name: "",
            parameters: {},
            constraints: [],
            variants: [],
        },
    })

    const { fields: variantFields, append: appendVariant, remove: removeVariant } = useFieldArray({
        control: form.control,
        name: "variants",
    })

    const { fields: constraintFields, append: appendConstraint, remove: removeConstraint } = useFieldArray({
        control: form.control,
        name: "constraints",
    })

    const { data: contextFields = [] } = useQuery({
        queryKey: ["context-fields"],
        queryFn: () => getContexts(),
    })

    useEffect(() => {
        if (open) {
            if (strategy) {
                const def = strategyDefinitions.find(d => d.name === strategy.name) || null
                setSelectedDef(def)

                // Parse parameters back to form values based on definition types
                const parameters: Record<string, string | boolean | string[]> = {}
                def?.parameters.forEach(p => {
                    const strategyParam = strategy.parameters?.find(sp => sp.name === p.name)
                    const val = strategyParam?.value
                    if (p.type === 'boolean') {
                        parameters[p.name] = val === 'true'
                    } else if (p.type === 'list') {
                        parameters[p.name] = val ? val.split(',') : []
                    } else {
                        parameters[p.name] = val || ""
                    }
                })

                form.reset({
                    name: strategy.name,
                    parameters,
                    constraints: strategy.constraints || [],
                    variants: strategy.variants || []
                })
            } else {
                setSelectedDef(null)
                form.reset({
                    name: "",
                    parameters: {},
                    constraints: [],
                    variants: []
                })
            }
        }
    }, [open, strategy, strategyDefinitions, form])

    function onSubmit(values: FormValues) {
        const parameters: Record<string, string> = {}

        Object.entries(values.parameters).forEach(([key, value]) => {
            if (Array.isArray(value)) {
                parameters[key] = value.join(',')
            } else if (typeof value === 'boolean') {
                parameters[key] = value ? 'true' : 'false'
            } else if (value !== undefined && value !== null) {
                parameters[key] = String(value)
            }
        })

        onSave({
            name: values.name,
            parameters: parameters,
            constraints: values.constraints as Constraint[],
            variants: values.variants as Variant[],
        })
        onOpenChange(false)
    }

    const handleStrategyChange = (name: string) => {
        const def = strategyDefinitions.find(d => d.name === name) || null
        setSelectedDef(def)
        form.setValue("name", name)

        // Initialize parameters map with defaults based on type
        const initialParams: Record<string, string | boolean | string[]> = {}
        def?.parameters.forEach(p => {
            if (p.type === 'boolean') initialParams[p.name] = false
            else if (p.type === 'list') initialParams[p.name] = []
            else if (p.type === 'number' || p.type === 'percentage') initialParams[p.name] = ""
            else initialParams[p.name] = ""
        })
        form.setValue("parameters", initialParams)
        form.setValue("constraints", [])
        form.setValue("variants", [])
    }

    const renderParameterInput = (param: StrategyParameterDefinition, field: { value: any, onChange: (val: any) => void }) => {
        switch (param.type) {
            case 'boolean':
                return (
                    <div className="flex items-center space-x-2 pt-2">
                        <FormControl>
                            <Checkbox
                                checked={!!field.value}
                                onCheckedChange={field.onChange}
                            />
                        </FormControl>
                        <FormLabel className="text-sm font-normal cursor-pointer">
                            Enabled
                        </FormLabel>
                    </div>
                )

            case 'list':
                const values = Array.isArray(field.value) ? field.value : []
                return (
                    <div className="space-y-2">
                        <div className="flex gap-2">
                            <Input
                                placeholder="Type and press Enter..."
                                onKeyDown={(e) => {
                                    if (e.key === 'Enter') {
                                        e.preventDefault()
                                        const val = e.currentTarget.value.trim()
                                        if (val && !values.includes(val)) {
                                            field.onChange([...values, val])
                                            e.currentTarget.value = ""
                                        }
                                    }
                                }}
                            />
                        </div>
                        <div className="flex flex-wrap gap-1.5 mt-2 min-h-[32px] p-2 border rounded-md bg-muted/20">
                            {values.length === 0 && <span className="text-xs text-muted-foreground italic px-1">No items added</span>}
                            {values.map((v: string) => (
                                <Badge key={v} variant="secondary" className="gap-1 pl-2 pr-1 h-6">
                                    {v}
                                    <button
                                        type="button"
                                        onClick={() => field.onChange(values.filter((item: string) => item !== v))}
                                        className="hover:bg-muted rounded-full p-0.5"
                                    >
                                        <X className="h-3 w-3" />
                                    </button>
                                </Badge>
                            ))}
                        </div>
                    </div>
                )

            case 'percentage':
                return (
                    <div className="relative">
                        <FormControl>
                            <Input
                                {...field}
                                type="number"
                                min="0"
                                max="100"
                                placeholder="0-100"
                                onChange={(e) => {
                                    const val = parseInt(e.target.value)
                                    if (isNaN(val)) field.onChange("")
                                    else field.onChange(Math.min(100, Math.max(0, val)).toString())
                                }}
                            />
                        </FormControl>
                        <span className="absolute right-3 top-2.5 text-muted-foreground">%</span>
                    </div>
                )

            case 'number':
                return (
                    <FormControl>
                        <Input
                            {...field}
                            type="number"
                            placeholder={param.description || "Enter a number"}
                        />
                    </FormControl>
                )

            default: // string
                return (
                    <FormControl>
                        <Input
                            {...field}
                            maxLength={100}
                            placeholder={param.description || `Max 100 characters`}
                            onChange={(e) => {
                                field.onChange(e.target.value.substring(0, 100))
                            }}
                        />
                    </FormControl>
                )
        }
    }

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[640px] max-h-[90vh] flex flex-col p-0 gap-0 overflow-hidden">
                <DialogHeader className="p-6 pb-2">
                    <DialogTitle className="flex items-center gap-2 text-xl">
                        {isEdit ? (
                            <div className="p-2 rounded-lg bg-primary/10 text-primary">
                                <Settings className="h-5 w-5" />
                            </div>
                        ) : (
                            <div className="p-2 rounded-lg bg-primary/10 text-primary">
                                <Plus className="h-5 w-5" />
                            </div>
                        )}
                        {isEdit ? "Edit Strategy" : "Add Strategy"}
                    </DialogTitle>
                    <DialogDescription className="text-sm">
                        {isEdit
                            ? "Correct the configuration for this activation strategy."
                            : "Configure a new activation strategy for your feature flag."}
                    </DialogDescription>
                </DialogHeader>

                <div className="flex-1 overflow-y-auto px-6 py-2">
                    <Form {...form}>
                        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8 pb-6 relative">
                            {loading && (
                                <div className="absolute inset-0 bg-background/50 backdrop-blur-[1px] z-10 flex items-center justify-center rounded-lg">
                                    <div className="flex flex-col items-center gap-2">
                                        <div className="h-5 w-5 border-2 border-primary border-t-transparent rounded-full animate-spin" />
                                        <span className="text-xs font-medium text-muted-foreground">Fetching details...</span>
                                    </div>
                                </div>
                            )}
                            <FormField
                                control={form.control}
                                name="name"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel className="text-sm font-semibold">Strategy Definition</FormLabel>
                                        <Select
                                            onValueChange={handleStrategyChange}
                                            defaultValue={field.value}
                                            value={field.value}
                                            disabled={isEdit}
                                        >
                                            <FormControl>
                                                <SelectTrigger className="h-11 bg-muted/30">
                                                    <SelectValue placeholder="Select a strategy definition" />
                                                </SelectTrigger>
                                            </FormControl>
                                            <SelectContent className="max-w-[400px]">
                                                {strategyDefinitions.map((def) => (
                                                    <SelectItem key={def.name} value={def.name}>
                                                        <span className="font-semibold text-sm">{def.name}</span>
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            {selectedDef && (
                                <div className="space-y-6 animate-in fade-in slide-in-from-top-2 duration-300">
                                    <div className="flex items-center gap-4">
                                        <span className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground whitespace-nowrap">Parameters</span>
                                        <div className="h-px w-full bg-border" />
                                    </div>

                                    {selectedDef.parameters.length === 0 ? (
                                        <div className="text-center py-8 border border-dashed rounded-xl bg-muted/5 text-muted-foreground">
                                            <p className="text-sm italic">This strategy has no parameters to configure.</p>
                                        </div>
                                    ) : (
                                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-6">
                                            {selectedDef.parameters.map((param) => (
                                                <FormField
                                                    key={param.name}
                                                    control={form.control}
                                                    name={`parameters.${param.name}`}
                                                    render={({ field }) => (
                                                        <FormItem className={`space-y-2 ${param.type === 'string' || param.type === 'list' ? 'sm:col-span-2' : ''}`}>
                                                            <div className="flex items-center justify-between">
                                                                <FormLabel className="flex items-center gap-1.5 font-semibold text-xs">
                                                                    {param.name}
                                                                    {param.required && <span className="text-destructive">*</span>}
                                                                </FormLabel>
                                                                <Badge variant="outline" className="text-[9px] uppercase tracking-wider py-0 px-1.5 h-4 gap-1 font-bold text-muted-foreground bg-muted/30 border-none">
                                                                    {param.type === 'string' && <Type className="h-2.5 w-2.5" />}
                                                                    {param.type === 'percentage' && <Percent className="h-2.5 w-2.5" />}
                                                                    {param.type === 'list' && <ListIcon className="h-2.5 w-2.5" />}
                                                                    {param.type === 'number' && <Hash className="h-2.5 w-2.5" />}
                                                                    {param.type === 'boolean' && <CheckCircle2 className="h-2.5 w-2.5" />}
                                                                    {param.type}
                                                                </Badge>
                                                            </div>

                                                            <div className={`${param.type === 'percentage' || param.type === 'number' ? 'max-w-[120px]' : ''}`}>
                                                                {renderParameterInput(param, field)}
                                                            </div>

                                                            {param.description && (
                                                                <FormDescription className="text-[10px] leading-relaxed italic opacity-80">
                                                                    {param.description}
                                                                </FormDescription>
                                                            )}
                                                            <FormMessage className="text-[10px]" />
                                                        </FormItem>
                                                    )}
                                                />
                                            ))}
                                        </div>
                                    )}
                                </div>
                            )}

                            {selectedDef && (
                                <div className="space-y-6 animate-in fade-in slide-in-from-top-2 duration-300">
                                    <div className="flex items-center gap-4">
                                        <span className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground whitespace-nowrap">Constraints</span>
                                        <div className="h-px w-full bg-border" />
                                    </div>

                                    <div className="space-y-4">
                                        {constraintFields.map((field, index) => (
                                            <div key={field.id} className="p-5 rounded-2xl border border-muted-foreground/10 bg-muted/10 space-y-5 relative group transition-all hover:border-primary/20">
                                                <Button
                                                    type="button"
                                                    variant="ghost"
                                                    size="icon"
                                                    className="absolute top-2 right-2 h-7 w-7 opacity-0 group-hover:opacity-100 transition-all hover:bg-destructive/10 hover:text-destructive"
                                                    onClick={() => removeConstraint(index)}
                                                >
                                                    <Trash2 className="h-4 w-4" />
                                                </Button>

                                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                                    <FormField
                                                        control={form.control}
                                                        name={`constraints.${index}.contextName`}
                                                        render={({ field }) => (
                                                            <FormItem>
                                                                <FormLabel className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground/70">Context Field</FormLabel>
                                                                <Select onValueChange={field.onChange} defaultValue={field.value} value={field.value}>
                                                                    <FormControl>
                                                                        <SelectTrigger className="h-10 bg-background/50">
                                                                            <SelectValue placeholder="Select field" />
                                                                        </SelectTrigger>
                                                                    </FormControl>
                                                                    <SelectContent>
                                                                        {['appName', 'environment', 'userId', 'sessionId', 'remoteAddress'].map(f => (
                                                                            <SelectItem key={f} value={f}>{f}</SelectItem>
                                                                        ))}
                                                                        {contextFields.filter(cf => !['appName', 'environment', 'userId', 'sessionId', 'remoteAddress'].includes(cf.name)).map((cf) => (
                                                                            <SelectItem key={cf.name} value={cf.name}>{cf.name}</SelectItem>
                                                                        ))}
                                                                    </SelectContent>
                                                                </Select>
                                                            </FormItem>
                                                        )}
                                                    />
                                                    <FormField
                                                        control={form.control}
                                                        name={`constraints.${index}.operator`}
                                                        render={({ field }) => (
                                                            <FormItem>
                                                                <FormLabel className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground/70">Operator</FormLabel>
                                                                <Select onValueChange={field.onChange} defaultValue={field.value} value={field.value}>
                                                                    <FormControl>
                                                                        <SelectTrigger className="h-10 bg-background/50">
                                                                            <SelectValue placeholder="Select operator" />
                                                                        </SelectTrigger>
                                                                    </FormControl>
                                                                    <SelectContent>
                                                                        {['IN', 'NOT_IN', 'STR_ENDS_WITH', 'STR_STARTS_WITH', 'STR_CONTAINS', 'NUM_EQ', 'NUM_GT', 'NUM_LT', 'DATE_AFTER', 'DATE_BEFORE', 'IN_TIME_WINDOW', 'SEMVER_EQ', 'SEMVER_GT', 'SEMVER_LT'].map(op => (
                                                                            <SelectItem key={op} value={op}>{op}</SelectItem>
                                                                        ))}
                                                                    </SelectContent>
                                                                </Select>
                                                            </FormItem>
                                                        )}
                                                    />
                                                </div>

                                                <FormField
                                                    control={form.control}
                                                    name={`constraints.${index}.values`}
                                                    render={({ field }) => {
                                                        const operator = form.watch(`constraints.${index}.operator`);
                                                        const currentValue = field.value?.[0] || "";

                                                        if (['DATE_AFTER', 'DATE_BEFORE'].includes(operator)) {
                                                            return (
                                                                <FormItem className="space-y-2">
                                                                    <div className="flex items-center justify-between">
                                                                        <FormLabel className="text-[10px] uppercase font-bold tracking-widest text-muted-foreground/70">Date</FormLabel>
                                                                    </div>
                                                                    <div className="relative">
                                                                        <Calendar className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
                                                                        <Input
                                                                            type="datetime-local"
                                                                            className="pl-9 h-10 bg-background/50"
                                                                            value={currentValue}
                                                                            onChange={(e) => field.onChange([e.target.value])}
                                                                        />
                                                                    </div>
                                                                    <FormMessage className="text-[10px]" />
                                                                </FormItem>
                                                            );
                                                        }

                                                        if (operator === 'IN_TIME_WINDOW') {
                                                            // Parse value: HH:mm-HH:mm|Timezone
                                                            const [range, timezone] = (currentValue || "-").split('|');
                                                            const [start, end] = range.split('-');

                                                            return (
                                                                <FormItem className="space-y-2">
                                                                    <div className="flex items-center justify-between">
                                                                        <FormLabel className="text-[10px] uppercase font-bold tracking-widest text-muted-foreground/70">Time Window & Zone</FormLabel>
                                                                    </div>
                                                                    <div className="flex gap-2">
                                                                        <div className="relative flex-1">
                                                                            <Clock className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
                                                                            <Input
                                                                                type="time"
                                                                                className="pl-9 h-10 bg-background/50 text-xs"
                                                                                value={start || ""}
                                                                                onChange={(e) => {
                                                                                    const newStart = e.target.value;
                                                                                    const newRange = `${newStart}-${end || ''}`;
                                                                                    const newValue = timezone ? `${newRange}|${timezone}` : newRange;
                                                                                    field.onChange([newValue]);
                                                                                }}
                                                                            />
                                                                        </div>
                                                                        <span className="self-center text-muted-foreground">-</span>
                                                                        <div className="relative flex-1">
                                                                            <Clock className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
                                                                            <Input
                                                                                type="time"
                                                                                className="pl-9 h-10 bg-background/50 text-xs"
                                                                                value={end || ""}
                                                                                onChange={(e) => {
                                                                                    const newEnd = e.target.value;
                                                                                    const newRange = `${start || ''}-${newEnd}`;
                                                                                    const newValue = timezone ? `${newRange}|${timezone}` : newRange;
                                                                                    field.onChange([newValue]);
                                                                                }}
                                                                            />
                                                                        </div>
                                                                    </div>

                                                                    <div className="relative">
                                                                        <Globe className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
                                                                        <Select
                                                                            value={timezone || "local"}
                                                                            onValueChange={(val) => {
                                                                                const newTz = val === "local" ? "" : val;
                                                                                const newValue = newTz ? `${range}|${newTz}` : range;
                                                                                field.onChange([newValue]);
                                                                            }}
                                                                        >
                                                                            <FormControl>
                                                                                <SelectTrigger className="pl-9 h-10 bg-background/50">
                                                                                    <SelectValue placeholder="User Local Time" />
                                                                                </SelectTrigger>
                                                                            </FormControl>
                                                                            <SelectContent className="max-h-[200px]">
                                                                                <SelectItem value="local">
                                                                                    User Local Time
                                                                                </SelectItem>
                                                                                {Intl.supportedValuesOf('timeZone').map(tz => (
                                                                                    <SelectItem key={tz} value={tz}>{tz}</SelectItem>
                                                                                ))}
                                                                            </SelectContent>
                                                                        </Select>
                                                                    </div>
                                                                    <FormMessage className="text-[10px]" />
                                                                </FormItem>
                                                            );
                                                        }

                                                        return (
                                                            <FormItem className="space-y-2">
                                                                <div className="flex items-center justify-between">
                                                                    <FormLabel className="text-[10px] uppercase font-bold tracking-widest text-muted-foreground/70">Values</FormLabel>
                                                                    <span className="text-[9px] text-muted-foreground italic px-2 py-0.5 rounded bg-muted">
                                                                        {['IN', 'NOT_IN'].includes(operator || "")
                                                                            ? "Enter to add multiple"
                                                                            : "Single value only"}
                                                                    </span>
                                                                </div>
                                                                <div className="space-y-3">
                                                                    <div className="flex gap-2">
                                                                        <Input
                                                                            className="h-10 bg-background/50"
                                                                            placeholder={field.value?.length > 0 && !['IN', 'NOT_IN'].includes(operator || "")
                                                                                ? "Value already set"
                                                                                : "Type value..."
                                                                            }
                                                                            disabled={field.value?.length > 0 && !['IN', 'NOT_IN'].includes(operator || "")}
                                                                            onKeyDown={(e) => {
                                                                                if (e.key === 'Enter') {
                                                                                    e.preventDefault()
                                                                                    const val = e.currentTarget.value.trim()
                                                                                    const currentValues = field.value || []
                                                                                    if (val && !currentValues.includes(val)) {
                                                                                        field.onChange([...currentValues, val])
                                                                                        e.currentTarget.value = ""
                                                                                    }
                                                                                }
                                                                            }}
                                                                        />
                                                                        <Button
                                                                            type="button"
                                                                            variant="secondary"
                                                                            size="icon"
                                                                            className="h-10 w-10 shrink-0 bg-background shadow-sm hover:shadow-md transition-all"
                                                                            disabled={field.value?.length > 0 && !['IN', 'NOT_IN'].includes(operator || "")}
                                                                            onClick={(e) => {
                                                                                const input = e.currentTarget.previousElementSibling as HTMLInputElement
                                                                                const val = input.value.trim()
                                                                                const currentValues = field.value || []
                                                                                if (val && !currentValues.includes(val)) {
                                                                                    field.onChange([...currentValues, val])
                                                                                    input.value = ""
                                                                                }
                                                                            }}
                                                                        >
                                                                            <Plus className="h-4 w-4 text-primary" />
                                                                        </Button>
                                                                    </div>
                                                                    <div className="flex flex-wrap gap-1.5 min-h-[24px]">
                                                                        {field.value?.map((v: string) => (
                                                                            <Badge key={v} variant="secondary" className="gap-1 pl-2 pr-1.5 h-6 text-[10px] font-medium bg-background border border-muted-foreground/10">
                                                                                {v}
                                                                                <button
                                                                                    type="button"
                                                                                    onClick={() => field.onChange(field.value.filter((item: string) => item !== v))}
                                                                                    className="hover:bg-muted rounded-full p-0.5 text-muted-foreground hover:text-foreground transition-colors"
                                                                                >
                                                                                    <X className="h-3 w-3" />
                                                                                </button>
                                                                            </Badge>
                                                                        ))}
                                                                    </div>
                                                                </div>
                                                                <FormMessage className="text-[10px]" />
                                                            </FormItem>
                                                        );
                                                    }}
                                                />

                                                <div className="flex items-center gap-8 pt-1 border-t border-muted-foreground/5 mt-2">
                                                    <FormField
                                                        control={form.control}
                                                        name={`constraints.${index}.inverted`}
                                                        render={({ field }) => (
                                                            <div className="flex items-center space-x-2">
                                                                <FormControl>
                                                                    <Switch
                                                                        checked={field.value}
                                                                        onCheckedChange={field.onChange}
                                                                        className="scale-75 data-[state=checked]:bg-destructive"
                                                                    />
                                                                </FormControl>
                                                                <Label className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground/70 cursor-pointer select-none">
                                                                    Inverted
                                                                </Label>
                                                            </div>
                                                        )}
                                                    />
                                                    <FormField
                                                        control={form.control}
                                                        name={`constraints.${index}.caseInsensitive`}
                                                        render={({ field }) => (
                                                            <div className="flex items-center space-x-2">
                                                                <FormControl>
                                                                    <Switch
                                                                        checked={field.value}
                                                                        onCheckedChange={field.onChange}
                                                                        className="scale-75"
                                                                    />
                                                                </FormControl>
                                                                <Label className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground/70 cursor-pointer select-none">
                                                                    Case Insensitive
                                                                </Label>
                                                            </div>
                                                        )}
                                                    />
                                                </div>
                                            </div>
                                        ))}

                                        <Button
                                            type="button"
                                            variant="outline"
                                            size="sm"
                                            className="w-full border-dashed border-primary/20 hover:bg-primary/5 hover:border-primary/40 transition-all h-11 gap-2 rounded-xl text-primary font-medium"
                                            onClick={() => appendConstraint({
                                                contextName: "appName",
                                                operator: "IN",
                                                values: [],
                                                caseInsensitive: false,
                                                inverted: false
                                            })}
                                        >
                                            <Filter className="h-4 w-4" />
                                            Add Constraint
                                        </Button>
                                    </div>

                                    <div className="space-y-6 animate-in fade-in slide-in-from-top-2 duration-300">
                                        <div className="flex items-center gap-4">
                                            <span className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground whitespace-nowrap">Variants</span>
                                            <div className="h-px w-full bg-border" />
                                        </div>

                                        <div className="space-y-4">
                                            {variantFields.map((field, index) => (
                                                <div key={field.id} className="p-5 rounded-2xl border border-muted-foreground/10 bg-muted/10 space-y-5 relative group transition-all hover:border-primary/20">
                                                    <Button
                                                        type="button"
                                                        variant="ghost"
                                                        size="icon"
                                                        className="absolute top-2 right-2 h-7 w-7 opacity-0 group-hover:opacity-100 transition-all hover:bg-destructive/10 hover:text-destructive"
                                                        onClick={() => removeVariant(index)}
                                                    >
                                                        <Trash2 className="h-4 w-4" />
                                                    </Button>

                                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                                        <FormField
                                                            control={form.control}
                                                            name={`variants.${index}.name`}
                                                            render={({ field }) => (
                                                                <FormItem>
                                                                    <FormLabel className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground/70">Variant Name</FormLabel>
                                                                    <FormControl>
                                                                        <Input {...field} className="h-10 bg-background/50" placeholder="e.g. control" />
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
                                                                    <FormLabel className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground/70">Weight (0-1000)</FormLabel>
                                                                    <FormControl>
                                                                        <Input
                                                                            {...field}
                                                                            type="number"
                                                                            className="h-10 bg-background/50"
                                                                            onChange={e => field.onChange(parseInt(e.target.value))}
                                                                        />
                                                                    </FormControl>
                                                                    <FormMessage />
                                                                </FormItem>
                                                            )}
                                                        />
                                                    </div>

                                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
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
                                                size="sm"
                                                className="w-full border-dashed border-primary/20 hover:bg-primary/5 hover:border-primary/40 transition-all h-11 gap-2 rounded-xl text-primary font-medium"
                                                onClick={() => appendVariant({
                                                    name: "",
                                                    weight: 0,
                                                    stickiness: null,
                                                    payload: null
                                                })}
                                            >
                                                <Plus className="h-4 w-4" />
                                                Add Variant
                                            </Button>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </form>
                    </Form>
                </div>

                <DialogFooter className="p-6 pt-2 gap-3 sm:gap-2 border-t bg-muted/5">
                    <Button type="button" variant="ghost" onClick={() => onOpenChange(false)} className="h-11 px-6">
                        Cancel
                    </Button>
                    <Button
                        type="submit"
                        disabled={!selectedDef}
                        onClick={form.handleSubmit(onSubmit)}
                        className="h-11 px-10 font-bold transition-all hover:translate-y-[-1px] hover:shadow-lg active:translate-y-[0px]"
                    >
                        {isEdit ? "Update Strategy" : "Add Strategy"}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}

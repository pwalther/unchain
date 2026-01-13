"use client"

import { useState, useEffect } from "react"
import { Zap, Pencil, X, Save, Trash, AlertTriangle, Plus } from "lucide-react"
import { toast } from "sonner"

import {
    Sheet,
    SheetContent,
    SheetDescription,
    SheetHeader,
    SheetTitle,
    SheetFooter
} from "@/components/ui/sheet"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Checkbox } from "@/components/ui/checkbox"
import { StrategyDefinition, StrategyParameterDefinition } from "@/types"
import { updateStrategyDefinition } from "../actions"

interface StrategyDetailsDrawerProps {
    strategy: StrategyDefinition | null
    open: boolean
    onOpenChange: (open: boolean) => void
    onEdit?: (strategy: StrategyDefinition) => void
    onSuccess?: () => void
}

export function StrategyDetailsDrawer({
    strategy,
    open,
    onOpenChange,
    onEdit,
    onSuccess
}: StrategyDetailsDrawerProps) {
    const [isEditing, setIsEditing] = useState(false)
    const [editDescription, setEditDescription] = useState("")
    const [editParams, setEditParams] = useState<StrategyParameterDefinition[]>([])

    // Reset state when strategy changes or drawer opens
    useEffect(() => {
        if (strategy) {
            setEditDescription(strategy.description || "")
            // Deep copy parameters to avoid mutating prop directly before save
            setEditParams(strategy.parameters ? JSON.parse(JSON.stringify(strategy.parameters)) : [])
        }
    }, [strategy, open])

    if (!strategy) return null

    const handleEditStart = () => {
        setEditDescription(strategy.description || "")
        setEditParams(strategy.parameters ? JSON.parse(JSON.stringify(strategy.parameters)) : [])
        setIsEditing(true)
    }

    const handleCancelEdit = () => {
        setIsEditing(false)
        // Reset to original
        setEditDescription(strategy.description || "")
        setEditParams(strategy.parameters || [])
    }

    const handleSave = async () => {
        try {
            await updateStrategyDefinition(strategy.name, {
                parameters: editParams
            })
            toast.success("Strategy updated successfully")
            setIsEditing(false)
            onSuccess?.()
        } catch (error: any) {
            // Check for conflict (409)
            // Assuming apiFetch throws an error with status or we can detect it
            // If the error object from apiFetch contains status:
            if (error?.status === 409 || error?.message?.includes("409") || error?.message?.includes("Conflict")) {
                toast.error("Cannot update strategy parameters", {
                    description: "This strategy is currently in use by one or more features. You cannot modify parameters of an active strategy."
                })
            } else {
                toast.error("Failed to update strategy", {
                    description: "An unexpected error occurred."
                })
                console.error(error)
            }
        }
    }

    const addParameter = () => {
        setEditParams([...editParams, { name: "", type: "string", description: "", required: false }])
    }

    const removeParameter = (index: number) => {
        const newParams = [...editParams]
        newParams.splice(index, 1)
        setEditParams(newParams)
    }

    const updateParameter = (index: number, field: keyof StrategyParameterDefinition, value: any) => {
        const newParams = [...editParams]
        newParams[index] = { ...newParams[index], [field]: value }
        setEditParams(newParams)
    }

    return (
        <Sheet open={open} onOpenChange={(val) => {
            if (!val) setIsEditing(false)
            onOpenChange(val)
        }}>
            <SheetContent className="sm:max-w-[600px] overflow-y-auto">
                <SheetHeader>
                    <div className="flex items-center justify-between">
                        <SheetTitle className="flex items-center gap-2">
                            <Zap className="h-5 w-5 text-amber-500" />
                            {strategy.name}
                        </SheetTitle>
                        {strategy.editable && !isEditing && (
                            <Button variant="ghost" size="sm" onClick={handleEditStart}>
                                <Pencil className="h-4 w-4 mr-2" />
                                Edit
                            </Button>
                        )}
                    </div>
                    <SheetDescription>
                        Configuration for the {strategy.name} strategy.
                    </SheetDescription>
                </SheetHeader>

                <div className="py-6 space-y-6">
                    <div className="space-y-4">
                        <div>
                            <Label className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Description</Label>
                            {isEditing ? (
                                <Textarea
                                    value={editDescription}
                                    onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setEditDescription(e.target.value)}
                                    className="mt-2"
                                    disabled // Description update not supported by API yet
                                    placeholder="Description update is not supported yet."
                                />
                            ) : (
                                <p className="mt-2 text-sm text-foreground/90 leading-relaxed">
                                    {strategy.description || <span className="text-muted-foreground italic">No description provided</span>}
                                </p>
                            )}
                        </div>

                        <Separator />

                        <div>
                            <div className="flex items-center justify-between mb-4">
                                <Label className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Parameters</Label>
                                {isEditing && (
                                    <Button size="sm" variant="outline" onClick={addParameter} className="h-7 gap-1">
                                        <Plus className="h-3 w-3" /> Add
                                    </Button>
                                )}
                            </div>

                            <div className="space-y-3">
                                {!isEditing && (strategy.parameters || []).length === 0 ? (
                                    <p className="text-sm text-muted-foreground italic">This strategy defines no parameters.</p>
                                ) : isEditing ? (
                                    // Editing Mode
                                    <div className="space-y-4">
                                        {editParams.map((param, index) => (
                                            <div key={index} className="p-3 bg-muted/30 rounded-lg border border-border/50 space-y-3 relative group">
                                                <Button
                                                    variant="ghost"
                                                    size="icon"
                                                    className="absolute top-2 right-2 h-6 w-6 text-muted-foreground hover:text-destructive"
                                                    onClick={() => removeParameter(index)}
                                                >
                                                    <X className="h-3 w-3" />
                                                </Button>

                                                <div className="grid grid-cols-2 gap-3">
                                                    <div className="space-y-1">
                                                        <Label className="text-[10px] uppercase">Name</Label>
                                                        <Input
                                                            value={param.name}
                                                            onChange={(e) => updateParameter(index, 'name', e.target.value)}
                                                            className="h-8 text-xs"
                                                            placeholder="paramName"
                                                        />
                                                    </div>
                                                    <div className="space-y-1">
                                                        <Label className="text-[10px] uppercase">Type</Label>
                                                        <Select
                                                            value={param.type}
                                                            onValueChange={(val) => updateParameter(index, 'type', val)}
                                                        >
                                                            <SelectTrigger className="h-8 text-xs">
                                                                <SelectValue />
                                                            </SelectTrigger>
                                                            <SelectContent>
                                                                <SelectItem value="string">String</SelectItem>
                                                                <SelectItem value="percentage">Percentage</SelectItem>
                                                                <SelectItem value="list">List</SelectItem>
                                                                <SelectItem value="number">Number</SelectItem>
                                                                <SelectItem value="boolean">Boolean</SelectItem>
                                                            </SelectContent>
                                                        </Select>
                                                    </div>
                                                </div>

                                                <div className="flex items-center space-x-2">
                                                    <Checkbox
                                                        checked={param.required}
                                                        onCheckedChange={(checked: boolean) => updateParameter(index, 'required', checked)}
                                                        id={`required-${index}`}
                                                    />
                                                    <Label htmlFor={`required-${index}`} className="text-xs font-normal">Required</Label>
                                                </div>
                                            </div>
                                        ))}
                                        {editParams.length === 0 && <p className="text-sm text-muted-foreground italic">No parameters.</p>}
                                    </div>
                                ) : (
                                    // View Mode
                                    strategy.parameters?.map((param) => (
                                        <div key={param.name} className="p-3 bg-muted/30 rounded-lg border border-border/50">
                                            <div className="flex items-center justify-between mb-1">
                                                <div className="flex items-center gap-2">
                                                    <span className="font-semibold text-sm">{param.name}</span>
                                                    <Badge variant="outline" className="text-[10px] uppercase">{param.type}</Badge>
                                                </div>
                                                {param.required && <Badge variant="secondary" className="text-[10px]">Required</Badge>}
                                            </div>
                                            <p className="text-xs text-muted-foreground">
                                                {param.description || "No parameter description"}
                                            </p>
                                        </div>
                                    ))
                                )}
                            </div>
                        </div>

                        {/* Removed the warning block */}
                    </div>
                </div>

                <SheetFooter>
                    {isEditing ? (
                        <div className="flex gap-2 w-full">
                            <Button variant="outline" className="flex-1" onClick={handleCancelEdit}>Cancel</Button>
                            <Button onClick={handleSave} className="flex-1">Save Changes</Button>
                        </div>
                    ) : (
                        <Button variant="outline" className="w-full" onClick={() => onOpenChange(false)}>Close</Button>
                    )}
                </SheetFooter>
            </SheetContent>
        </Sheet>
    )
}

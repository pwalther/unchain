"use client"

import React, { useState, useEffect } from "react"
import Editor from "react-simple-code-editor"
// @ts-ignore
import { highlight, languages } from "prismjs/components/prism-core"
import "prismjs/components/prism-json"
// Use global CSS for syntax highlighting to support dark mode
// import "prismjs/themes/prism.css" 

import { cn } from "@/lib/utils"

interface JsonEditorProps {
    value: string
    onChange: (value: string) => void
    onError?: (error: string | null) => void
    className?: string
}

export const JsonEditor = React.forwardRef<
    HTMLDivElement,
    JsonEditorProps
>(({ value, onChange, onError, className }, ref) => {
    const [localValue, setLocalValue] = useState(value)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        setLocalValue(value)
    }, [value])

    const handleValueChange = (code: string) => {
        setLocalValue(code)
        onChange(code)

        try {
            if (code.trim()) {
                JSON.parse(code)
            }
            setError(null)
            onError?.(null)
        } catch (e: any) {
            const msg = e.message
            setError(msg)
            onError?.(msg)
        }
    }

    return (
        <div className={cn("relative rounded-md border bg-background font-mono text-sm", error && "border-destructive", className)}>
            <div className="max-h-[200px] overflow-auto">
                <Editor
                    value={localValue}
                    onValueChange={handleValueChange}
                    highlight={(code: string) => highlight(code, languages.json)}
                    padding={12}
                    style={{
                        fontFamily: '"Fira code", "Fira Mono", monospace',
                        fontSize: 12,
                        minHeight: "80px",
                    }}
                    className="outline-none"
                    textareaId="json-editor-textarea"
                />
            </div>
            {error && (
                <div className="absolute bottom-0 left-0 right-0 bg-destructive/10 px-3 py-1 text-[10px] text-destructive border-t border-destructive/20 animate-in fade-in slide-in-from-bottom-1">
                    Invalid JSON: {error}
                </div>
            )}
        </div>
    )
})

JsonEditor.displayName = "JsonEditor"

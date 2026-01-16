"use client"

import { useState } from "react"
import { ContextList } from "@/features/contexts/components/context-list"
import { ContextDialog } from "@/features/contexts/components/context-dialog"
import { Button } from "@/components/ui/button"
import { Plus } from "lucide-react"

export default function ContextsPage() {
    const [open, setOpen] = useState(false)

    return (
        <div className="flex-1 space-y-4 p-8 pt-6">
            <div className="flex items-center justify-between space-y-2">
                <h2 className="text-3xl font-bold tracking-tight">Contexts</h2>
                <div className="flex items-center space-x-2">
                    <Button onClick={() => setOpen(true)}>
                        <Plus className="mr-2 h-4 w-4" /> Create Context
                    </Button>
                </div>
            </div>
            <ContextList />
            <ContextDialog open={open} onOpenChange={setOpen} />
        </div>
    )
}

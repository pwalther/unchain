"use client"

import { useState, useEffect } from "react"
import { X, Info, WifiOff } from "lucide-react"
import { BANNER_CONFIG } from "@/config/banner"
import { useAuth } from "@/hooks/use-auth"

export function Banner() {
    const [isVisible, setIsVisible] = useState(false)
    const { status, failureCount, isFetching } = useAuth()
    const isError = status === 'error' || (failureCount > 0 && isFetching)

    useEffect(() => {
        const lastDismissedMessage = localStorage.getItem("banner-message-dismissed")
        // Check if config exists and has a non-empty message
        if (BANNER_CONFIG.enabled && BANNER_CONFIG.message && BANNER_CONFIG.message.trim() !== "") {
            if (lastDismissedMessage !== BANNER_CONFIG.message) {
                setIsVisible(true)
            }
        }
    }, [])

    const handleDismiss = () => {
        setIsVisible(false)
        localStorage.setItem("banner-message-dismissed", BANNER_CONFIG.message)
    }

    if (isError) {
        return (
            <div className="relative w-full bg-destructive text-destructive-foreground py-2 px-4 shadow-lg border-b border-destructive/20 overflow-hidden z-[100] animate-in fade-in slide-in-from-top duration-300">
                <div className="max-w-7xl mx-auto flex items-center justify-center gap-2">
                    <WifiOff size={16} className="shrink-0 animate-pulse" />
                    <p className="text-sm font-semibold tracking-wide">
                        Connection Interrupted: Unable to reach the server.
                    </p>
                </div>
            </div>
        )
    }

    if (!isVisible) return null

    return (
        <div className="relative w-full bg-linear-to-r from-indigo-600 via-purple-600 to-blue-600 text-white py-2 px-4 shadow-lg border-b border-white/10 overflow-hidden z-[100]">
            {/* Subtle glass effect/shine */}
            <div className="absolute inset-0 bg-white/5 pointer-events-none" />

            <div className="max-w-7xl mx-auto flex items-center justify-between gap-4">
                <div className="flex items-center gap-2 flex-1 justify-center">
                    <Info size={16} className="text-white/80 shrink-0 hidden sm:block" />
                    <p className="text-sm font-medium tracking-wide">
                        {BANNER_CONFIG.message}
                    </p>
                </div>

                <button
                    onClick={handleDismiss}
                    className="p-1 rounded-full hover:bg-white/20 transition-all duration-200 active:scale-95 shrink-0 cursor-pointer"
                    aria-label="Dismiss banner"
                >
                    <X size={18} />
                </button>
            </div>
        </div>
    )
}

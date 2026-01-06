"use client"

import * as React from "react"
import * as SliderPrimitive from "@radix-ui/react-slider"

import { cn } from "@/lib/utils"

function Slider({
    className,
    defaultValue,
    value,
    onValueChange,
    ...props
}: React.ComponentProps<typeof SliderPrimitive.Root>) {
    return (
        <SliderPrimitive.Root
            data-slot="slider"
            defaultValue={defaultValue}
            value={value}
            onValueChange={onValueChange}
            className={cn(
                "relative flex w-full touch-none select-none items-center",
                className
            )}
            {...props}
        >
            <SliderPrimitive.Track
                data-slot="slider-track"
                className="bg-muted relative h-2 w-full grow overflow-hidden rounded-full"
            >
                <SliderPrimitive.Range
                    data-slot="slider-range"
                    className="bg-primary absolute h-full transition-all duration-300 shadow-[0_0_10px_rgba(var(--primary),0.3)]"
                />
            </SliderPrimitive.Track>
            <SliderPrimitive.Thumb
                data-slot="slider-thumb"
                className="border-primary bg-background ring-ring/50 block size-5 rounded-full border-2 transition-all duration-200 outline-none focus-visible:ring-[3px] disabled:pointer-events-none disabled:opacity-50 hover:scale-110 active:scale-95 shadow-lg shadow-primary/20"
            />
        </SliderPrimitive.Root>
    )
}

export { Slider }

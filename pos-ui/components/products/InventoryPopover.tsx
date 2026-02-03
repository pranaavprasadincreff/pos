"use client"

import { useEffect, useRef, useState } from "react"
import type { ProductData } from "@/services/types"
import * as PopoverPrimitive from "@radix-ui/react-popover"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { toast } from "sonner"
import { updateInventory } from "@/services/productService"
import { cn } from "@/lib/utils"
import { Hint } from "@/components/shared/Hint"

interface Props {
    product: ProductData
    onUpdated: (updated: ProductData) => void
}

export default function InventoryPopover({ product, onUpdated }: Props) {
    const previousValue = useRef(product.inventory)
    const [value, setValue] = useState<number | null>(product.inventory)
    const [error, setError] = useState("")
    const [open, setOpen] = useState(false)
    const inputRef = useRef<HTMLInputElement>(null)
    const [isDirty, setIsDirty] = useState(false)

    function validate(val: number | null) {
        if (val === null) return false
        if (val < 0) {
            setError("Inventory cannot be negative")
            return false
        }
        setError("")
        return true
    }

    async function handleUpdate() {
        if (value === null) {
            setError("Inventory is required")
            return
        }
        if (!validate(value)) return

        try {
            const updated = await updateInventory(product.id, value)
            previousValue.current = updated.inventory
            setIsDirty(false)
            setOpen(false)
            toast.success("Inventory updated")
            onUpdated(updated)
        } catch (e: unknown) {
            toast.error(e instanceof Error ? e.message : "Inventory update failed")
        }
    }

    function handleClose() {
        setValue(previousValue.current)
        setError("")
        setOpen(false)
    }

    useEffect(() => {
        setIsDirty(value !== previousValue.current)
    }, [value])

    return (
        <PopoverPrimitive.Root
            open={open}
            onOpenChange={(o) => {
                if (!o) handleClose()
                else setOpen(true)
            }}
        >
            <Hint text="Update inventory">
                <PopoverPrimitive.Trigger asChild>
                    <Button variant="outline" className="w-full justify-between">
                        Inventory
                        <span className="font-semibold">{product.inventory}</span>
                    </Button>
                </PopoverPrimitive.Trigger>
            </Hint>

            {/* ✅ Portal ensures popover never affects card height */}
            <PopoverPrimitive.Portal>
                <PopoverPrimitive.Content
                    side="bottom"
                    align="start"
                    sideOffset={8}
                    collisionPadding={12}
                    className={cn(
                        "z-50 w-64 rounded-md border bg-popover p-4 text-popover-foreground shadow-md outline-none space-y-3",
                        error && "border-red-500"
                    )}
                    onOpenAutoFocus={(e) => {
                        e.preventDefault()
                        inputRef.current?.focus()
                    }}
                    onInteractOutside={(e) => {
                        // optional: allow clicking outside to close normally
                        // (Radix default is fine; keeping this here in case you want to tweak)
                    }}
                >
                    <p className="text-sm font-medium">Update Inventory</p>

                    <div className="flex gap-2">
                        <Button
                            variant="outline"
                            onClick={() => {
                                const nv = (value ?? 0) - 1
                                validate(nv)
                                setValue(nv)
                            }}
                        >
                            –
                        </Button>

                        <Input
                            ref={inputRef}
                            type="text"
                            inputMode="numeric"
                            value={value === null ? "" : value}
                            onFocus={(e) => e.target.select()}
                            onWheel={(e) => e.currentTarget.blur()}
                            onChange={(e) => {
                                const raw = e.target.value
                                if (raw === "") {
                                    setValue(null)
                                    setError("")
                                    return
                                }
                                if (!/^-?\d+$/.test(raw)) return
                                const num = Number(raw)
                                setValue(num)
                                validate(num)
                            }}
                            className={cn("text-center", error && "border-red-500 focus-visible:ring-red-500")}
                        />

                        <Button
                            variant="outline"
                            onClick={() => {
                                const nv = (value ?? 0) + 1
                                validate(nv)
                                setValue(nv)
                            }}
                        >
                            +
                        </Button>
                    </div>

                    {error && <p className="text-xs text-red-600">{error}</p>}

                    <div className="flex justify-end gap-2 pt-2">
                        <Button variant="ghost" onClick={handleClose}>
                            Cancel
                        </Button>
                        <Button onClick={handleUpdate} disabled={!!error || !isDirty}>
                            Update
                        </Button>
                    </div>
                </PopoverPrimitive.Content>
            </PopoverPrimitive.Portal>
        </PopoverPrimitive.Root>
    )
}

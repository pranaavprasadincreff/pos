"use client"

import { useEffect, useRef, useState } from "react"
import { ProductData } from "@/services/types"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
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
            onUpdated(updated) // ✅ patch in parent, no reload
        } catch (e: unknown) {
            const message = e instanceof Error ? e.message : "Inventory update failed"
            toast.error(message)
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
        <Popover
            open={open}
            onOpenChange={(o) => {
                if (!o) handleClose()
                else setOpen(true)
            }}
        >
            <PopoverTrigger asChild>
                {/* Hint MUST wrap the actual trigger element */}
                <Hint text="Update inventory">
                    <Button variant="outline" className="w-full justify-between">
                        Inventory
                        <span className="font-semibold">{product.inventory}</span>
                    </Button>
                </Hint>
            </PopoverTrigger>

            <PopoverContent
                className={cn("w-64 space-y-3", error ? "border-red-500" : "")}
                onOpenAutoFocus={() => inputRef.current?.focus()}
            >
                <p className="text-sm font-medium">Update Inventory</p>

                <div className="flex gap-2">
                    <Button
                        variant="outline"
                        onClick={() =>
                            setValue((v) => {
                                const base = v ?? 0
                                const nv = base - 1
                                validate(nv)
                                return nv
                            })
                        }
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
                        onClick={() =>
                            setValue((v) => {
                                const base = v ?? 0
                                const nv = base + 1
                                validate(nv)
                                return nv
                            })
                        }
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
            </PopoverContent>
        </Popover>
    )
}

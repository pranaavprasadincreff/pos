"use client"

import { useEffect, useRef } from "react"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { X } from "lucide-react"
import { useOrderForm } from "@/hooks/useOrderForm"
import type { OrderData } from "@/services/types"

interface OrderFormModalProps {
    open: boolean
    onClose: () => void
    onSuccess: () => void
    orderToEdit?: OrderData | null
}

export default function OrderFormModal({
                                           open,
                                           onClose,
                                           onSuccess,
                                           orderToEdit,
                                       }: OrderFormModalProps) {
    const scrollRef = useRef<HTMLDivElement>(null)

    const {
        rows,
        loading,
        formError,
        addRow,
        removeRow,
        handleBarcodeChange,
        handleQuantityChange,
        handlePriceChange,
        handleSubmit,
        resetForm,
    } = useOrderForm({ onSuccess, orderToEdit })

    // ✅ reset whenever modal opens (fresh create OR load edit)
    useEffect(() => {
        if (open) resetForm()
    }, [open, resetForm])

    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight
        }
    }, [rows.length])

    const isEditMode = Boolean(orderToEdit)
    const title = isEditMode ? "Edit Order" : "Create Order"
    const submitText = loading
        ? isEditMode
            ? "Saving…"
            : "Creating…"
        : isEditMode
            ? "Save Changes"
            : "Create Order"

    const grandTotal = rows.reduce((acc, row) => acc + Number(row.sellingPrice || 0), 0)

    const submitAndClose = async () => {
        const ok = await handleSubmit()
        if (ok !== false) handleClose()
    }

    function handleClose() {
        // ✅ close from Cancel or X only
        onClose()
    }

    return (
        <Dialog open={open} onOpenChange={(v) => (!v ? handleClose() : undefined)}>
            <DialogContent
                className="max-w-5xl w-full animate-in fade-in zoom-in-95 duration-200"
                onInteractOutside={(e) => e.preventDefault()} // ✅ no close on backdrop click
                onEscapeKeyDown={(e) => e.preventDefault()} // ✅ no close on ESC
            >
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>
                </DialogHeader>

                {formError && (
                    <div className="rounded-md border border-red-200 bg-red-50/60 px-3 py-2 text-sm text-red-700">
                        {formError}
                    </div>
                )}

                {/* Headers */}
                <div className="grid grid-cols-12 gap-3 font-semibold text-sm text-muted-foreground mb-2 sticky top-0 bg-background z-10 pt-2">
                    <div className="col-span-3">Barcode</div>
                    <div className="col-span-3">Product Name</div>
                    <div className="col-span-2">Quantity</div>
                    <div className="col-span-3">Price</div>
                    <div className="col-span-1" />
                </div>

                {/* Items container */}
                <div className="rounded-xl border bg-muted/30 p-3 w-full overflow-x-hidden">
                    <div
                        ref={scrollRef}
                        className="overflow-y-auto w-full overflow-x-hidden pr-3"
                        style={{
                            maxHeight: "calc(3 * 3.5rem + 1rem)",
                            scrollbarGutter: "stable",
                        }}
                    >
                        <div className="space-y-3">
                            {rows.map((row, index) => (
                                <div key={index} className="grid grid-cols-12 gap-3 items-center w-full">
                                    <Input
                                        className="col-span-3 bg-background w-full"
                                        placeholder="Barcode"
                                        value={row.productBarcode}
                                        onChange={(e) =>
                                            handleBarcodeChange(index, e.target.value.toUpperCase())
                                        }
                                    />

                                    <Input
                                        className="col-span-3 bg-background w-full"
                                        value={row.productName}
                                        disabled
                                    />

                                    <Input
                                        className="col-span-2 bg-background w-full"
                                        inputMode="numeric"
                                        value={row.quantity}
                                        onChange={(e) => handleQuantityChange(index, e.target.value)}
                                    />

                                    <Input
                                        className="col-span-3 bg-background w-full"
                                        value={row.sellingPrice}
                                        onChange={(e) => handlePriceChange(index, e.target.value)}
                                    />

                                    <div className="col-span-1 flex items-center justify-end">
                                        {rows.length > 1 && (
                                            <Button
                                                type="button"
                                                size="icon"
                                                variant="ghost"
                                                onClick={() => removeRow(index)}
                                                aria-label="Remove item"
                                                className="h-9 w-9 rounded-md"
                                            >
                                                <X className="h-4 w-4" />
                                            </Button>
                                        )}
                                    </div>

                                    {row.error && (
                                        <div className="col-span-12 text-xs text-red-600">{row.error}</div>
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>
                </div>

                <div className="flex items-center justify-between pt-1">
                    <Button
                        type="button"
                        variant="outline"
                        onClick={addRow}
                        className="border-indigo-200 bg-indigo-50/40 text-indigo-700 hover:bg-indigo-50"
                    >
                        + Add Item
                    </Button>

                    <div className="font-semibold">Grand Total: ₹{grandTotal.toFixed(2)}</div>
                </div>

                <div className="flex justify-end gap-2 pt-2">
                    <Button type="button" variant="outline" onClick={handleClose} disabled={loading}>
                        Cancel
                    </Button>

                    <Button
                        type="button"
                        disabled={loading}
                        onClick={submitAndClose}
                        className="bg-indigo-600 hover:bg-indigo-700 text-white"
                    >
                        {submitText}
                    </Button>
                </div>
            </DialogContent>
        </Dialog>
    )
}

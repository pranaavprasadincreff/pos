"use client"

import { useEffect, useRef } from "react"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { X } from "lucide-react"
import { useOrderForm } from "@/hooks/useOrderForm"
import type { OrderData } from "@/services/types"
import { cn } from "@/lib/utils"

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
        handleBarcodeChange, // ✅ use the hook function you actually return
        handleQuantityChange,
        handlePriceChange,
        handleSubmit,
        resetForm,
    } = useOrderForm({ onSuccess, orderToEdit })

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

    // ✅ Grand total (based on FINAL selling price per row)
    const grandTotal = rows.reduce((acc, row) => {
        const total = Number(row.sellingPrice || 0)
        return acc + (Number.isFinite(total) ? total : 0)
    }, 0)

    const submitAndClose = async () => {
        const ok = await handleSubmit()
        if (ok !== false) handleClose()
    }

    function handleClose() {
        onClose()
    }

    return (
        <Dialog open={open} onOpenChange={(v) => (!v ? handleClose() : undefined)}>
            <DialogContent
                className="max-w-5xl w-full animate-in fade-in zoom-in-95 duration-200"
                onInteractOutside={(e) => e.preventDefault()}
                onEscapeKeyDown={(e) => e.preventDefault()}
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
                    <div className="col-span-3">Product</div>
                    <div className="col-span-2">Quantity</div>
                    <div className="col-span-3">Final Selling Price</div>
                    <div className="col-span-1" />
                </div>

                {/* Items */}
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
                            {rows.map((row, index) => {
                                const qty = Number(row.quantity || 0)
                                const sellingTotal = Number(row.sellingPrice || 0)

                                const isOverMrp =
                                    row.unitMrp != null &&
                                    qty > 0 &&
                                    !Number.isNaN(sellingTotal) &&
                                    sellingTotal > row.unitMrp * qty

                                const isInvalidBarcode =
                                    row.error?.trim().toLowerCase() === "invalid barcode"

                                return (
                                    <div key={index} className="grid grid-cols-12 gap-3 items-start w-full">
                                        {/* ✅ Barcode: input + fixed-height error line under it */}
                                        <div className="col-span-3 space-y-1">
                                            <Input
                                                className="bg-background w-full"
                                                placeholder="Barcode"
                                                maxLength={40}
                                                value={row.productBarcode}
                                                onChange={(e) =>
                                                    handleBarcodeChange(index, e.target.value.toUpperCase())
                                                }
                                            />

                                            {/* fixed height line = same as MRP line */}
                                            <div className="h-4 leading-4 text-[11px] font-medium">
                                                {isInvalidBarcode ? (
                                                    <span className="text-red-600">Invalid barcode</span>
                                                ) : (
                                                    // keep alignment even when empty
                                                    <span className="opacity-0">.</span>
                                                )}
                                            </div>
                                        </div>

                                        {/* ✅ Product name + fixed-height MRP line */}
                                        <div className="col-span-3 space-y-1">
                                            <Input className="bg-background w-full" value={row.productName} disabled />
                                            <div className="h-4 leading-4 text-[11px] text-muted-foreground">
                                                MRP{" "}
                                                {row.unitMrp != null ? (
                                                    <span className="font-medium text-foreground">
                            ₹{row.unitMrp.toFixed(2)}
                          </span>
                                                ) : (
                                                    <span className="italic">—</span>
                                                )}
                                            </div>
                                        </div>

                                        {/* Quantity */}
                                        <Input
                                            className="col-span-2 bg-background w-full"
                                            inputMode="numeric"
                                            value={row.quantity}
                                            onChange={(e) => handleQuantityChange(index, e.target.value)}
                                        />

                                        {/* Final selling price */}
                                        <div className="col-span-3 space-y-1">
                                            <Input
                                                className={cn(
                                                    "bg-background w-full",
                                                    isOverMrp && "border-red-300 focus-visible:ring-red-400"
                                                )}
                                                placeholder="Final amount"
                                                value={row.sellingPrice}
                                                onChange={(e) => handlePriceChange(index, e.target.value)}
                                            />
                                            {isOverMrp && (
                                                <div className="text-[11px] text-red-600 font-medium">
                                                    Exceeds MRP for selected quantity
                                                </div>
                                            )}
                                        </div>

                                        {/* Remove */}
                                        <div className="col-span-1 flex items-center justify-end pt-1">
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

                                        {/* ✅ other errors below row (skip invalid barcode because shown under barcode input) */}
                                        {row.error && !isInvalidBarcode && (
                                            <div className="col-span-12 text-xs text-red-600">
                                                {row.error}
                                            </div>
                                        )}
                                    </div>
                                )
                            })}
                        </div>
                    </div>
                </div>

                {/* Bottom bar */}
                <div className="flex items-center justify-between pt-1">
                    <Button
                        type="button"
                        variant="outline"
                        onClick={addRow}
                        className="border-indigo-200 bg-indigo-50/40 text-indigo-700 hover:bg-indigo-50"
                    >
                        + Add Item
                    </Button>

                    <div className="text-xs text-muted-foreground">
                        Selling price is the <span className="font-medium">final total</span> for the line item
                    </div>
                </div>

                {/* Footer */}
                <div className="flex items-center justify-between pt-2">
                    <div className="font-semibold">Grand Total: ₹{grandTotal.toFixed(2)}</div>

                    <div className="flex justify-end gap-2">
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
                </div>
            </DialogContent>
        </Dialog>
    )
}

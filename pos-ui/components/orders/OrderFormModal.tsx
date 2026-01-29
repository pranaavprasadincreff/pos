'use client'

import { useRef, useEffect } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { X } from 'lucide-react'
import { useOrderForm } from '@/hooks/useOrderForm'
import { OrderData } from '@/services/types'

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

    useEffect(() => {
        if (open) resetForm()
    }, [open, resetForm])

    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight
        }
    }, [rows.length])

    const isEditMode = !!orderToEdit
    const title = isEditMode ? 'Edit Order' : 'Create Order'
    const submitText = loading
        ? isEditMode ? 'Saving…' : 'Creating…'
        : isEditMode ? 'Save Changes' : 'Create Order'

    const grandTotal = rows.reduce(
        (acc, row) => acc + Number(row.sellingPrice || 0),
        0
    )

    const submitAndClose = async () => {
        const ok = await handleSubmit()
        if (ok !== false) onClose()
    }

    return (
        <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
            <DialogContent className="max-w-5xl w-full">
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>
                </DialogHeader>

                {formError && (
                    <div className="bg-red-50 p-3 text-sm text-red-700 rounded">
                        {formError}
                    </div>
                )}

                {/* Headers */}
                <div className="grid grid-cols-12 gap-3 font-semibold text-sm text-muted-foreground mb-2 sticky top-0 bg-background z-10 pt-2">
                    <div className="col-span-3">Barcode</div>
                    <div className="col-span-3">Product Name</div>
                    <div className="col-span-2">Quantity</div>
                    <div className="col-span-3">Price</div>
                    <div className="col-span-1"></div>
                </div>

                {/* Distinct items container */}
                <div className="border rounded-md bg-muted/40 p-3 mb-3 w-full overflow-x-hidden">
                    <div
                        ref={scrollRef}
                        className="overflow-y-auto w-full overflow-x-hidden"
                        style={{ maxHeight: 'calc(3 * 3.5rem + 1rem)' }}
                    >
                        <div className="space-y-3">
                            {rows.map((row, index) => (
                                <div key={index} className="grid grid-cols-12 gap-3 items-center w-full">
                                    <Input
                                        className="col-span-3 bg-background w-full"
                                        placeholder="Barcode"
                                        value={row.productBarcode}
                                        onChange={e =>
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
                                        onChange={e => handleQuantityChange(index, e.target.value)}
                                    />

                                    <Input
                                        className="col-span-3 bg-background w-full"
                                        value={row.sellingPrice}
                                        onChange={e => handlePriceChange(index, e.target.value)}
                                    />

                                    <div className="col-span-1 flex items-center">
                                        {rows.length > 1 && (
                                            <Button
                                                size="icon"
                                                variant="ghost"
                                                onClick={() => removeRow(index)}
                                            >
                                                <X className="h-4 w-4" />
                                            </Button>
                                        )}
                                    </div>

                                    {row.error && (
                                        <div className="col-span-12 text-xs text-red-600">
                                            {row.error}
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>
                </div>

                <Button variant="outline" onClick={addRow}>
                    + Add Item
                </Button>

                <div className="flex justify-between items-center mt-3">
                    <div className="font-semibold">
                        Grand Total: ₹{grandTotal.toFixed(2)}
                    </div>

                    <div className="flex gap-2">
                        <Button variant="outline" onClick={onClose}>
                            Cancel
                        </Button>

                        <Button disabled={loading} onClick={submitAndClose}>
                            {submitText}
                        </Button>
                    </div>
                </div>
            </DialogContent>
        </Dialog>
    )
}

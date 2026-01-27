'use client'

import { useState } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { X } from 'lucide-react'
import { AxiosError } from 'axios'

import { OrderCreateItemForm, ProductData } from '@/services/types'
import { create } from '@/services/orderService'
import { getProductByBarcode } from '@/services/productService'

interface CreateOrderModalProps {
    open: boolean
    onClose: () => void
    onSuccess: () => void
}

interface InputRow {
    productBarcode: string
    productName: string
    quantity: string
    sellingPrice: string
    error?: string
}

const emptyRow = (): InputRow => ({
    productBarcode: '',
    productName: '',
    quantity: '',
    sellingPrice: '',
})

export default function CreateOrderModal({
                                             open,
                                             onClose,
                                             onSuccess,
                                         }: CreateOrderModalProps) {
    const [rows, setRows] = useState<InputRow[]>([emptyRow()])
    const [loading, setLoading] = useState(false)
    const [formError, setFormError] = useState<string | null>(null)

    /* ---------------- barcode lookup ---------------- */
    const handleBarcodeChange = async (index: number, barcode: string) => {
        updateRow(index, 'productBarcode', barcode)
        updateRow(index, 'productName', '')
        updateRow(index, 'sellingPrice', '')
        updateRow(index, 'error', undefined)

        if (!barcode.trim()) return

        try {
            const product: ProductData = await getProductByBarcode(barcode.trim())

            setRows(prev =>
                prev.map((row, i) =>
                    i === index
                        ? {
                            ...row,
                            productName: product.name,
                            quantity: row.quantity || '1',
                            sellingPrice: product.mrp.toString(), // ✅ ALWAYS RESET
                            error: undefined,
                        }
                        : row
                )
            )
        } catch {
            setRows(prev =>
                prev.map((row, i) =>
                    i === index
                        ? {
                            ...row,
                            productName: '',
                            sellingPrice: '',
                            error: 'Product not found',
                        }
                        : row
                )
            )
        }
    }

    /* ---------------- helpers ---------------- */
    const updateRow = (
        index: number,
        field: keyof InputRow,
        value: string | undefined
    ) => {
        setRows(prev =>
            prev.map((row, i) =>
                i === index ? { ...row, [field]: value } : row
            )
        )
    }

    const addRow = () => setRows(prev => [...prev, emptyRow()])
    const removeRow = (index: number) =>
        setRows(prev => prev.filter((_, i) => i !== index))

    /* ---------------- validation ---------------- */
    const validateRows = (): boolean => {
        let valid = true

        const updated = rows.map(row => {
            let error = ''

            if (!row.productBarcode.trim()) error = 'Barcode required'
            else if (!row.productName) error = 'Invalid barcode'
            else if (!row.quantity || Number(row.quantity) <= 0)
                error = 'Quantity must be > 0'
            else if (!row.sellingPrice || Number(row.sellingPrice) < 0)
                error = 'Invalid price'

            if (error) valid = false
            return { ...row, error: error || undefined }
        })

        setRows(updated)
        return valid
    }

    /* ---------------- submit ---------------- */
    const handleCreateOrder = async () => {
        setFormError(null)

        if (!validateRows()) return

        const items: OrderCreateItemForm[] = rows.map(row => ({
            productBarcode: row.productBarcode.trim().toUpperCase(),
            quantity: Number(row.quantity),
            sellingPrice: Number(row.sellingPrice),
        }))

        try {
            setLoading(true)
            await create({ items })
            onSuccess()
            onClose()
            setRows([emptyRow()])
        } catch (err) {
            const error = err as AxiosError<{ message: string }>
            setFormError(error.response?.data?.message || 'Failed to create order')
        } finally {
            setLoading(false)
        }
    }

    const isCreateDisabled =
        loading ||
        rows.some(
            r =>
                !r.productBarcode ||
                !r.quantity ||
                !r.sellingPrice ||
                !!r.error
        )

    /* ---------------- render ---------------- */
    return (
        <Dialog open={open} onOpenChange={onClose}>
            <DialogContent className="max-w-3xl">
                <DialogHeader>
                    <DialogTitle>Create Order</DialogTitle>
                </DialogHeader>

                {formError && (
                    <div className="rounded-md bg-red-50 p-3 text-sm text-red-700">
                        {formError}
                    </div>
                )}

                <div className="space-y-4">
                    <div className="grid grid-cols-12 gap-3 text-sm font-medium">
                        <div className="col-span-4">Barcode</div>
                        <div className="col-span-3">Product</div>
                        <div className="col-span-2">Qty</div>
                        <div className="col-span-2">Price</div>
                        <div className="col-span-1" />
                    </div>

                    {rows.map((row, index) => (
                        <div key={index} className="space-y-1">
                            <div className="grid grid-cols-12 gap-3">
                                <Input
                                    className="col-span-4"
                                    placeholder="Scan / type barcode"
                                    value={row.productBarcode}
                                    onChange={e =>
                                        handleBarcodeChange(index, e.target.value.toUpperCase())
                                    }
                                />

                                <Input
                                    className="col-span-3"
                                    value={row.productName}
                                    placeholder="Product name"
                                    disabled
                                />

                                <Input
                                    className="col-span-2"
                                    type="number"
                                    min={1}
                                    value={row.quantity}
                                    onChange={e =>
                                        updateRow(index, 'quantity', e.target.value)
                                    }
                                />

                                <Input
                                    className="col-span-2"
                                    type="number"
                                    min={0}
                                    step="0.01"
                                    value={row.sellingPrice}
                                    onChange={e =>
                                        updateRow(
                                            index,
                                            'sellingPrice',
                                            e.target.value
                                        )
                                    }
                                />

                                <div className="col-span-1 flex items-center">
                                    {rows.length > 1 && (
                                        <Button
                                            variant="ghost"
                                            size="icon"
                                            onClick={() => removeRow(index)}
                                        >
                                            <X className="h-4 w-4" />
                                        </Button>
                                    )}
                                </div>
                            </div>

                            {row.error && (
                                <div className="text-xs text-red-600">
                                    {row.error}
                                </div>
                            )}
                        </div>
                    ))}

                    <Button variant="outline" onClick={addRow}>
                        + Add Item
                    </Button>
                </div>

                <div className="flex justify-end gap-2 pt-4">
                    <Button variant="outline" onClick={onClose}>
                        Cancel
                    </Button>
                    <Button disabled={isCreateDisabled} onClick={handleCreateOrder}>
                        {loading ? 'Creating...' : 'Create Order'}
                    </Button>
                </div>
            </DialogContent>
        </Dialog>
    )
}

'use client'

import { useState, useCallback, useEffect } from 'react'
import { AxiosError } from 'axios'
import { OrderCreateItemForm, ProductData, OrderData } from '@/services/types'
import { create, edit } from '@/services/orderService'
import { getProductByBarcode } from '@/services/productService'

export interface InputRow {
    productBarcode: string
    productName: string
    quantity: string
    sellingPrice: string
    unitPrice?: number
    priceLocked?: boolean
    error?: string
}

export const emptyRow = (): InputRow => ({
    productBarcode: '',
    productName: '',
    quantity: '',
    sellingPrice: '',
    unitPrice: undefined,
    priceLocked: false,
})

interface UseOrderFormProps {
    onSuccess: () => void
    orderToEdit?: OrderData | null
}

export function useOrderForm({ onSuccess, orderToEdit }: UseOrderFormProps) {
    const [rows, setRows] = useState<InputRow[]>([emptyRow()])
    const [loading, setLoading] = useState(false)
    const [formError, setFormError] = useState<string | null>(null)

    // -------------------- Initialize rows --------------------
    const initializeRows = useCallback(async () => {
        if (orderToEdit?.items.length) {
            const initializedRows: InputRow[] = await Promise.all(
                orderToEdit.items.map(async (item) => {
                    try {
                        const product: ProductData = await getProductByBarcode(item.productBarcode)
                        return {
                            productBarcode: item.productBarcode,
                            productName: product.name,
                            unitPrice: product.mrp,
                            quantity: item.quantity.toString(),
                            sellingPrice: item.sellingPrice.toString(),
                            priceLocked: true,
                        }
                    } catch {
                        return {
                            productBarcode: item.productBarcode,
                            productName: '',
                            unitPrice: undefined,
                            quantity: item.quantity.toString(),
                            sellingPrice: item.sellingPrice.toString(),
                            priceLocked: true,
                            error: 'Invalid barcode',
                        }
                    }
                })
            )
            setRows(initializedRows)
        } else {
            setRows([emptyRow()])
        }
        setFormError(null)
    }, [orderToEdit])

    // -------------------- Reset --------------------
    const resetForm = useCallback(() => {
        if (orderToEdit) initializeRows()
        else {
            setRows([emptyRow()])
            setFormError(null)
        }
    }, [orderToEdit, initializeRows])

    // -------------------- Row ops --------------------
    const updateRow = <K extends keyof InputRow>(index: number, field: K, value: InputRow[K]) => {
        setRows(prev => prev.map((row, i) => (i === index ? { ...row, [field]: value } : row)))
    }

    const addRow = () => setRows(prev => [...prev, emptyRow()])
    const removeRow = (index: number) => setRows(prev => prev.filter((_, i) => i !== index))

    // -------------------- Barcode autofill --------------------
    const handleBarcodeChange = async (index: number, value: string) => {
        updateRow(index, 'productBarcode', value)
        const barcode = value.trim()
        if (!barcode) {
            updateRow(index, 'productName', '')
            updateRow(index, 'unitPrice', undefined)
            updateRow(index, 'sellingPrice', '')
            updateRow(index, 'error', undefined)
            return
        }

        try {
            const product: ProductData = await getProductByBarcode(barcode)
            setRows(prev => {
                const row = prev[index]
                const qty = Number(row.quantity) || 1
                return prev.map((r, i) =>
                    i === index
                        ? {
                            ...r,
                            productName: product.name,
                            unitPrice: product.mrp,
                            quantity: row.quantity || '1',
                            sellingPrice: r.priceLocked ? r.sellingPrice : (product.mrp * qty).toString(),
                            error: undefined,
                        }
                        : r
                )
            })
        } catch {
            updateRow(index, 'productName', '')
            updateRow(index, 'unitPrice', undefined)
            updateRow(index, 'sellingPrice', '')
            updateRow(index, 'error', 'Invalid barcode')
        }
    }

    // -------------------- Quantity & price --------------------
    const handleQuantityChange = (index: number, value: string) => {
        if (!/^\d*$/.test(value)) return
        setRows(prev => {
            const row = prev[index]
            const qty = Number(value)
            const unit = row.unitPrice
            const locked = row.priceLocked

            return prev.map((r, i) =>
                i === index
                    ? {
                        ...r,
                        quantity: value,
                        sellingPrice: unit && qty > 0 && !locked ? (unit * qty).toString() : r.sellingPrice,
                    }
                    : r
            )
        })
    }

    const handlePriceChange = (index: number, value: string) => {
        if (!/^\d*\.?\d*$/.test(value)) return
        updateRow(index, 'sellingPrice', value)
        updateRow(index, 'priceLocked', true)
    }

    // -------------------- Validation --------------------
    const validateRows = (): boolean => {
        let valid = true
        const updated = rows.map(row => {
            let error = ''
            if (!row.productBarcode) error = 'Barcode required'
            else if (!row.unitPrice) error = 'Invalid product'
            else if (!row.quantity || Number(row.quantity) <= 0) error = 'Qty > 0 required'
            else if (!row.sellingPrice || Number(row.sellingPrice) <= 0) error = 'Price required'

            if (error) valid = false
            return { ...row, error: error || undefined }
        })
        setRows(updated)
        return valid
    }

    // -------------------- Submit (PATCHED) --------------------
    const handleSubmit = async (): Promise<boolean> => {
        setFormError(null)

        if (!validateRows()) return false

        const items: OrderCreateItemForm[] = rows.map(row => ({
            productBarcode: row.productBarcode,
            quantity: Number(row.quantity),
            sellingPrice: Number(row.sellingPrice),
        }))

        try {
            setLoading(true)

            if (orderToEdit?.orderReferenceId)
                await edit(orderToEdit.orderReferenceId, { items })
            else
                await create({ items })

            onSuccess()
            resetForm()
            return true   // ✅ SUCCESS SIGNAL
        } catch (err) {
            const error = err as AxiosError<{ message: string }>
            setFormError(error.response?.data?.message || 'Failed')
            return false  // ✅ FAILURE SIGNAL
        } finally {
            setLoading(false)
        }
    }

    // -------------------- Auto init --------------------
    useEffect(() => {
        if (orderToEdit) initializeRows()
    }, [orderToEdit, initializeRows])

    return {
        rows,
        loading,
        formError,
        resetForm,
        updateRow,
        addRow,
        removeRow,
        handleBarcodeChange,
        handleQuantityChange,
        handlePriceChange,
        handleSubmit,
    }
}

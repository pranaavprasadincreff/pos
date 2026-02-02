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

    // FINAL total selling price for this line item
    sellingPrice: string

    // per-unit MRP (from product)
    unitMrp?: number

    // per-unit selling (derived from sellingPrice/qty when user edits)
    unitSellingPrice?: number

    error?: string
}

export const emptyRow = (): InputRow => ({
    productBarcode: '',
    productName: '',
    quantity: '',
    sellingPrice: '',
    unitMrp: undefined,
    unitSellingPrice: undefined,
})

interface UseOrderFormProps {
    onSuccess: () => void
    orderToEdit?: OrderData | null
}

function toNumber(v: string | undefined) {
    if (!v) return NaN
    return Number(v)
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
                        const qty = item.quantity
                        const unitMrp = product.mrp

                        // NOTE: existing data might be per-unit depending on your backend.
                        // Here we interpret stored sellingPrice as "per-unit" in existing orders,
                        // so for UI we convert it to final total = perUnit * qty.
                        const assumedPerUnitSelling = item.sellingPrice
                        const sellingTotal = assumedPerUnitSelling * qty

                        return {
                            productBarcode: item.productBarcode,
                            productName: product.name,
                            unitMrp,
                            quantity: qty.toString(),
                            sellingPrice: sellingTotal.toString(),
                            unitSellingPrice: assumedPerUnitSelling,
                            error: undefined,
                        }
                    } catch {
                        return {
                            productBarcode: item.productBarcode,
                            productName: '',
                            unitMrp: undefined,
                            quantity: item.quantity.toString(),
                            sellingPrice: (item.sellingPrice * item.quantity).toString(),
                            unitSellingPrice: item.sellingPrice,
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
        setRows((prev) => prev.map((row, i) => (i === index ? { ...row, [field]: value } : row)))
    }

    const addRow = () => setRows((prev) => [...prev, emptyRow()])
    const removeRow = (index: number) => setRows((prev) => prev.filter((_, i) => i !== index))

    // -------------------- Barcode autofill --------------------
    const handleBarcodeChange = async (index: number, value: string) => {
        updateRow(index, 'productBarcode', value)

        const barcode = value.trim()
        if (!barcode) {
            setRows((prev) => {
                const r = prev[index]
                const next = [...prev]
                next[index] = {
                    ...r,
                    productName: '',
                    unitMrp: undefined,
                    unitSellingPrice: undefined,
                    quantity: '',
                    sellingPrice: '',
                    error: undefined,
                }
                return next
            })
            return
        }

        try {
            const product: ProductData = await getProductByBarcode(barcode)

            setRows((prev) => {
                const row = prev[index]
                const qty = Number(row.quantity) || 1
                const unitMrp = product.mrp

                // default: total selling = unitMrp * qty
                const unitSelling = row.unitSellingPrice ?? unitMrp
                const sellingTotal = qty > 0 ? unitSelling * qty : ''

                const next = [...prev]
                next[index] = {
                    ...row,
                    productName: product.name,
                    unitMrp,
                    quantity: row.quantity || '1',
                    unitSellingPrice: unitSelling,
                    sellingPrice: sellingTotal.toString(),
                    error: undefined,
                }
                return next
            })
        } catch {
            setRows((prev) => {
                const row = prev[index]
                const next = [...prev]
                next[index] = {
                    ...row,
                    productName: '',
                    unitMrp: undefined,
                    unitSellingPrice: undefined,
                    quantity: row.quantity || '',
                    sellingPrice: '',
                    error: 'Invalid barcode',
                }
                return next
            })
        }
    }

    // -------------------- Quantity & price --------------------
    const handleQuantityChange = (index: number, value: string) => {
        if (!/^\d*$/.test(value)) return

        setRows((prev) => {
            const row = prev[index]
            const qty = Number(value)

            // scale selling price total using per-unit selling price if possible
            let nextSelling = row.sellingPrice
            if (qty > 0) {
                const unitSelling =
                    row.unitSellingPrice ??
                    (() => {
                        // if user hasn't edited price yet, infer per-unit from current total
                        const currentTotal = toNumber(row.sellingPrice)
                        const currentQty = Number(row.quantity) || 0
                        if (currentQty > 0 && !Number.isNaN(currentTotal)) return currentTotal / currentQty
                        // else default to unit MRP
                        return row.unitMrp
                    })()

                if (unitSelling != null && !Number.isNaN(unitSelling)) {
                    nextSelling = (unitSelling * qty).toString()
                }
            }

            const next = [...prev]
            next[index] = {
                ...row,
                quantity: value,
                sellingPrice: nextSelling,
                error: undefined,
            }
            return next
        })
    }

    const handlePriceChange = (index: number, value: string) => {
        if (!/^\d*\.?\d*$/.test(value)) return

        setRows((prev) => {
            const row = prev[index]
            const qty = Number(row.quantity) || 0
            const total = Number(value)

            // derive per-unit selling so we can scale on qty change
            const unitSelling =
                qty > 0 && !Number.isNaN(total) ? total / qty : row.unitSellingPrice

            const next = [...prev]
            next[index] = {
                ...row,
                sellingPrice: value,
                unitSellingPrice: unitSelling,
                error: undefined,
            }
            return next
        })
    }

    // -------------------- Validation --------------------
    const validateRows = (): boolean => {
        let valid = true

        const updated = rows.map((row) => {
            let error = ''

            const qty = Number(row.quantity)
            const sellingTotal = Number(row.sellingPrice)

            if (!row.productBarcode) error = 'Barcode required'
            else if (!row.unitMrp) error = 'Invalid product'
            else if (!row.quantity || Number.isNaN(qty) || qty <= 0) error = 'Qty > 0 required'
            else if (!row.sellingPrice || Number.isNaN(sellingTotal) || sellingTotal <= 0)
                error = 'Final selling price required'

            // selling total should not exceed scaled mrp total
            if (!error && row.unitMrp != null && qty > 0) {
                const mrpTotal = row.unitMrp * qty
                if (sellingTotal > mrpTotal) {
                    error = 'Selling price cannot exceed MRP'
                }
            }

            if (error) valid = false
            return { ...row, error: error || undefined }
        })

        setRows(updated)
        return valid
    }

    // -------------------- Submit --------------------
    const handleSubmit = async (): Promise<boolean> => {
        setFormError(null)

        if (!validateRows()) return false

        // IMPORTANT:
        // Backend today often expects per-unit sellingPrice.
        // Since UI is now taking TOTAL selling price, we convert total -> per-unit here.
        // perUnit = total / qty
        const items: OrderCreateItemForm[] = rows.map((row) => {
            const qty = Number(row.quantity)
            const total = Number(row.sellingPrice)
            const perUnit = qty > 0 ? total / qty : total

            return {
                productBarcode: row.productBarcode,
                quantity: qty,
                sellingPrice: perUnit, // send per-unit to backend
            }
        })

        try {
            setLoading(true)

            if (orderToEdit?.orderReferenceId) await edit(orderToEdit.orderReferenceId, { items })
            else await create({ items })

            onSuccess()
            resetForm()
            return true
        } catch (err) {
            const error = err as AxiosError<{ message: string }>
            setFormError(error.response?.data?.message || 'Failed')
            return false
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

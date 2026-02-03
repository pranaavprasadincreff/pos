'use client'

import { useState, useCallback, useEffect } from 'react'
import { AxiosError } from 'axios'
import { OrderCreateItemForm, ProductData, OrderData } from '@/services/types'
import { create, edit } from '@/services/orderService'
import { getProductByBarcode } from '@/services/productService'
import { toast } from "sonner"
import { can, getSessionRole } from "@/utils/permissions"

export interface InputRow {
    productBarcode: string
    productName: string
    quantity: string
    sellingPrice: string // FINAL total for the line item
    unitMrp?: number
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

function extractApiMessage(err: unknown): string {
    const e = err as any
    const status = e?.response?.status
    const data = e?.response?.data

    const dataMsg =
        typeof data === "string"
            ? data
            : data?.message || data?.error || (data ? JSON.stringify(data) : "")

    const msg = dataMsg || e?.message || "Request failed"
    return status ? `${msg} (HTTP ${status})` : msg
}


export function useOrderForm({ onSuccess, orderToEdit }: UseOrderFormProps) {
    const role = getSessionRole()
    const isEditMode = Boolean(orderToEdit)

    const allowed = isEditMode ? can(role, "order_edit") : can(role, "order_create")

    const [rows, setRows] = useState<InputRow[]>([emptyRow()])
    const [loading, setLoading] = useState(false)
    const [formError, setFormError] = useState<string | null>(null)

    const initializeRows = useCallback(async () => {
        if (orderToEdit?.items.length) {
            const initializedRows: InputRow[] = await Promise.all(
                orderToEdit.items.map(async (item) => {
                    try {
                        const product: ProductData = await getProductByBarcode(item.productBarcode)
                        const qty = item.quantity
                        const unitMrp = product.mrp

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

    const resetForm = useCallback(() => {
        if (orderToEdit) initializeRows()
        else {
            setRows([emptyRow()])
            setFormError(null)
        }
    }, [orderToEdit, initializeRows])

    const updateRow = <K extends keyof InputRow>(index: number, field: K, value: InputRow[K]) => {
        setRows((prev) => prev.map((row, i) => (i === index ? { ...row, [field]: value } : row)))
    }

    const addRow = () => setRows((prev) => [...prev, emptyRow()])
    const removeRow = (index: number) => setRows((prev) => prev.filter((_, i) => i !== index))

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

    const handleQuantityChange = (index: number, value: string) => {
        if (!/^\d*$/.test(value)) return

        setRows((prev) => {
            const row = prev[index]
            const qty = Number(value)

            let nextSelling = row.sellingPrice
            if (qty > 0) {
                const unitSelling =
                    row.unitSellingPrice ??
                    (() => {
                        const currentTotal = toNumber(row.sellingPrice)
                        const currentQty = Number(row.quantity) || 0
                        if (currentQty > 0 && !Number.isNaN(currentTotal)) return currentTotal / currentQty
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

            const unitSelling = qty > 0 && !Number.isNaN(total) ? total / qty : row.unitSellingPrice

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

            if (!error && row.unitMrp != null && qty > 0) {
                const mrpTotal = row.unitMrp * qty
                if (sellingTotal > mrpTotal) error = 'Selling price cannot exceed MRP'
            }

            if (error) valid = false
            return { ...row, error: error || undefined }
        })

        setRows(updated)
        return valid
    }

    const handleSubmit = async (): Promise<boolean> => {
        setFormError(null)

        if (!allowed) {
            setFormError("Not allowed")
            toast.error("Not allowed")
            return false
        }

        if (!validateRows()) return false

        const items: OrderCreateItemForm[] = rows.map((row) => {
            const qty = Number(row.quantity)
            const total = Number(row.sellingPrice)
            const perUnit = qty > 0 ? total / qty : total

            return {
                productBarcode: row.productBarcode,
                quantity: qty,
                sellingPrice: perUnit, // send per-unit
            }
        })

        const toastId = toast.loading(isEditMode ? "Saving order..." : "Creating order...")

        try {
            setLoading(true)

            if (orderToEdit?.orderReferenceId) await edit(orderToEdit.orderReferenceId, { items })
            else await create({ items })

            toast.success(isEditMode ? "Order updated" : "Order created", { id: toastId })
            onSuccess()
            resetForm()
            return true
        } catch (err: unknown) {
            const msg = extractApiMessage(err)
            setFormError(msg)
            toast.error(msg || "Failed", { id: toastId })
            return false
        } finally {
            setLoading(false)
        }
    }

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

'use client'

import { useEffect, useMemo, useState } from 'react'
import { OrderItemData } from '@/services/types'
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/components/ui/table'
import { getProductByBarcode } from '@/services/productService'

interface OrderItemsTableProps {
    items: OrderItemData[]
}

type ProductLite = {
    name: string
    mrp: number
}

export default function OrderItemsTable({ items }: OrderItemsTableProps) {
    const [productByBarcode, setProductByBarcode] = useState<
        Record<string, ProductLite | null>
    >({})
    const [loadingByBarcode, setLoadingByBarcode] = useState<Record<string, boolean>>(
        {}
    )

    const barcodes = useMemo(() => {
        const uniq = new Set<string>()
        for (const it of items) {
            const b = (it.productBarcode || '').trim()
            if (b) uniq.add(b)
        }
        return Array.from(uniq)
    }, [items])

    useEffect(() => {
        let cancelled = false

        async function hydrate() {
            const toFetch = barcodes.filter(
                (b) => productByBarcode[b] === undefined && !loadingByBarcode[b]
            )
            if (toFetch.length === 0) return

            setLoadingByBarcode((prev) => {
                const next = { ...prev }
                for (const b of toFetch) next[b] = true
                return next
            })

            await Promise.all(
                toFetch.map(async (barcode) => {
                    try {
                        const p = await getProductByBarcode(barcode)
                        if (cancelled) return

                        setProductByBarcode((prev) => ({
                            ...prev,
                            [barcode]: { name: p.name, mrp: p.mrp },
                        }))
                    } catch {
                        if (cancelled) return
                        setProductByBarcode((prev) => ({
                            ...prev,
                            [barcode]: null,
                        }))
                    } finally {
                        if (cancelled) return
                        setLoadingByBarcode((prev) => ({
                            ...prev,
                            [barcode]: false,
                        }))
                    }
                })
            )
        }

        hydrate()
        return () => {
            cancelled = true
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [barcodes])

    return (
        <div className="rounded-md border bg-background">
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead>Barcode</TableHead>
                        <TableHead>Product</TableHead>
                        <TableHead>MRP</TableHead>
                        <TableHead>Quantity</TableHead>
                        <TableHead>Selling Price</TableHead>
                    </TableRow>
                </TableHeader>

                <TableBody>
                    {items.map((item, index) => {
                        const barcode = (item.productBarcode || '').trim()
                        const product = barcode ? productByBarcode[barcode] : undefined
                        const loading = barcode ? loadingByBarcode[barcode] : false

                        // ✅ backend has per-unit sellingPrice, but UI wants "actual user-entered total"
                        const qty = Number(item.quantity || 0)
                        const perUnit = Number(item.sellingPrice || 0)
                        const sellingTotal = qty > 0 ? perUnit * qty : 0

                        return (
                            <TableRow key={`${item.productBarcode}-${index}`}>
                                {/* Barcode */}
                                <TableCell className="font-medium">{item.productBarcode}</TableCell>

                                {/* Product name */}
                                <TableCell>
                                    {loading ? (
                                        <span className="text-muted-foreground">Loading…</span>
                                    ) : product ? (
                                        product.name
                                    ) : (
                                        <span className="text-muted-foreground">—</span>
                                    )}
                                </TableCell>

                                {/* MRP (per unit, NOT scaled) */}
                                <TableCell>
                                    {loading ? (
                                        <span className="text-muted-foreground">—</span>
                                    ) : product ? (
                                        <>₹ {product.mrp.toFixed(2)}</>
                                    ) : (
                                        <span className="text-muted-foreground">—</span>
                                    )}
                                </TableCell>

                                {/* Quantity */}
                                <TableCell>{item.quantity}</TableCell>

                                {/* ✅ Selling Price shown as total for the row */}
                                <TableCell>₹ {sellingTotal.toFixed(2)}</TableCell>
                            </TableRow>
                        )
                    })}
                </TableBody>
            </Table>
        </div>
    )
}

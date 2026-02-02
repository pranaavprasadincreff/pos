"use client"

import { useCallback, useEffect, useMemo, useState } from "react"

import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"

import OrderTable from "@/components/orders/OrderTable"
import CreateOrderModal from "@/components/orders/OrderFormModal"
import Pagination from "@/components/shared/Pagination"

import { getAllOrders, filterOrders } from "@/services/orderService"
import type { OrderData } from "@/services/types"

import { Hint } from "@/components/shared/Hint"
import { toast } from "sonner"

const STATUS_OPTIONS = [
    "ALL",
    "FULFILLABLE",
    "UNFULFILLABLE",
    "INVOICED",
    "CANCELLED",
] as const
type OrderStatus = (typeof STATUS_OPTIONS)[number]

const RANGE_OPTIONS = ["1d", "1w", "1m"] as const
type DateRange = (typeof RANGE_OPTIONS)[number]

const TIMEFRAME_MAP: Record<DateRange, "LAST_DAY" | "LAST_WEEK" | "LAST_MONTH"> = {
    "1d": "LAST_DAY",
    "1w": "LAST_WEEK",
    "1m": "LAST_MONTH",
}

const PAGE_SIZE = 9

export default function OrdersPage() {
    const [orders, setOrders] = useState<OrderData[]>([])
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(1)

    const [orderId, setOrderId] = useState("")
    const [status, setStatus] = useState<OrderStatus>("ALL")
    const [range, setRange] = useState<DateRange>("1m")

    const [modalOpen, setModalOpen] = useState(false)
    const [editingOrder, setEditingOrder] = useState<OrderData | null>(null)

    const [loading, setLoading] = useState(true)

    const hasAnyFilter = useMemo(() => {
        return Boolean(orderId.trim()) || status !== "ALL" || range !== "1m"
    }, [orderId, status, range])

    const fetchOrders = useCallback(
        async (pageToLoad: number) => {
            let toastId: string | number | undefined
            try {
                setLoading(true)
                toastId = toast.loading("Loading orders...")

                const trimmedRef = orderId.trim()
                const timeframe = TIMEFRAME_MAP[range]

                const res = hasAnyFilter
                    ? await filterOrders({
                        orderReferenceId: trimmedRef ? trimmedRef : undefined,
                        status: status === "ALL" ? undefined : status,
                        timeframe,
                        page: pageToLoad,
                        size: PAGE_SIZE,
                    })
                    : await getAllOrders(pageToLoad, PAGE_SIZE)

                setOrders(res.content)
                setTotalPages(res.totalPages || 1)
            } finally {
                setLoading(false)
                if (toastId) toast.dismiss(toastId)
            }
        },
        [orderId, status, range, hasAnyFilter]
    )

    // fetch when page changes
    useEffect(() => {
        fetchOrders(page)
    }, [page, fetchOrders])

    // reset to first page when filters change
    useEffect(() => {
        setPage(0)
    }, [orderId, status, range])

    const refreshCurrentPage = useCallback(async () => {
        await fetchOrders(page) // ✅ stays on same page for cancel/invoice/etc
    }, [fetchOrders, page])

    const handleEdit = (order: OrderData) => {
        setEditingOrder(order)
        setModalOpen(true)
    }

    return (
        <div className="space-y-6">
            {/* Sticky Header */}
            <div className="sticky top-0 z-30 -mt-6 bg-background border-b">
                <div className="max-w-6xl mx-auto px-6 py-5 space-y-4">
                    <div className="flex justify-between items-center">
                        <div>
                            <h1 className="text-2xl font-semibold">Orders</h1>
                            <p className="text-sm text-muted-foreground">
                                Create orders and manage lifecycle
                            </p>
                        </div>

                        <Hint text="Create a new order">
                            <Button
                                className="bg-indigo-600 hover:bg-indigo-700"
                                onClick={() => {
                                    setEditingOrder(null)
                                    setModalOpen(true)
                                }}
                            >
                                + Create Order
                            </Button>
                        </Hint>
                    </div>

                    {/* Filters (same layout as before) */}
                    <div className="flex flex-wrap items-center gap-2">
                        <Hint text="Filter by Order ID (contains)">
                            <Input
                                className="w-60 transition focus-visible:ring-2 focus-visible:ring-indigo-500"
                                placeholder="Search Order ID"
                                value={orderId}
                                onChange={(e) => setOrderId(e.target.value)}
                            />
                        </Hint>

                        <Hint text="Filter by order status">
                            <div>
                                <Select value={status} onValueChange={(v) => setStatus(v as OrderStatus)}>
                                    <SelectTrigger
                                        className="
                      w-44 transition
                      focus-visible:ring-2 focus-visible:ring-indigo-500
                      data-[state=open]:ring-2 data-[state=open]:ring-indigo-500
                    "
                                    >
                                        <SelectValue />
                                    </SelectTrigger>

                                    {/* ✅ make dropdown behave like ClientsPage */}
                                    <SelectContent
                                        side="bottom"
                                        align="start"
                                        sideOffset={4}
                                        avoidCollisions={false}
                                        position="popper"
                                    >
                                        {STATUS_OPTIONS.map((s) => (
                                            <SelectItem key={s} value={s}>
                                                {s}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>
                        </Hint>

                        <Hint text="Filter by order creation time">
                            <div>
                                <Select value={range} onValueChange={(v) => setRange(v as DateRange)}>
                                    <SelectTrigger
                                        className="
                      w-40 transition
                      focus-visible:ring-2 focus-visible:ring-indigo-500
                      data-[state=open]:ring-2 data-[state=open]:ring-indigo-500
                    "
                                    >
                                        <SelectValue />
                                    </SelectTrigger>

                                    {/* ✅ make dropdown behave like ClientsPage */}
                                    <SelectContent
                                        side="bottom"
                                        align="start"
                                        sideOffset={4}
                                        avoidCollisions={false}
                                        position="popper"
                                    >
                                        <SelectItem value="1d">Last 1 Day</SelectItem>
                                        <SelectItem value="1w">Last 1 Week</SelectItem>
                                        <SelectItem value="1m">Last 1 Month</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                        </Hint>

                        <Hint text="Clear current filters">
                            <Button
                                variant="outline"
                                className="transition focus-visible:ring-2 focus-visible:ring-indigo-500"
                                onClick={() => {
                                    setOrderId("")
                                    setStatus("ALL")
                                    setRange("1m")
                                    setPage(0)
                                }}
                            >
                                Clear
                            </Button>
                        </Hint>
                    </div>
                </div>
            </div>

            {/* Orders Table */}
            <div className="max-w-6xl mx-auto px-6 pt-6 space-y-6">
                <OrderTable
                    orders={orders}
                    loading={loading}
                    onInvoiceGenerated={refreshCurrentPage}
                    onEdit={handleEdit}
                />

                <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
            </div>

            {/* Create / Edit Modal */}
            <CreateOrderModal
                open={modalOpen}
                onClose={() => {
                    setModalOpen(false)
                    setEditingOrder(null)
                }}
                onSuccess={refreshCurrentPage}
                orderToEdit={editingOrder}
            />
        </div>
    )
}

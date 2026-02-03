"use client"

import { useCallback, useEffect, useMemo, useRef, useState } from "react"

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
import { can, getSessionRole } from "@/utils/permissions"

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
const ORDER_REFERENCE_ID_MAX = 50
const PAGINATION_LIFT_PX = 40

function useDebouncedValue<T>(value: T, delayMs: number) {
    const [debounced, setDebounced] = useState(value)
    useEffect(() => {
        const t = setTimeout(() => setDebounced(value), delayMs)
        return () => clearTimeout(t)
    }, [value, delayMs])
    return debounced
}

function ensureFullyVisible(container: HTMLElement, el: HTMLElement, padding = 12) {
    const cRect = container.getBoundingClientRect()
    const eRect = el.getBoundingClientRect()

    if (eRect.top >= cRect.top + padding && eRect.bottom <= cRect.bottom - padding) return

    const topDelta = eRect.top - (cRect.top + padding)
    const bottomDelta = eRect.bottom - (cRect.bottom - padding)

    if (topDelta < 0) container.scrollTop += topDelta
    else if (bottomDelta > 0) container.scrollTop += bottomDelta
}

export default function OrdersPage() {
    const role = getSessionRole()
    const canCreateOrder = useMemo(() => can(role, "order_create"), [role])

    const [orders, setOrders] = useState<OrderData[]>([])
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(1)

    const [orderId, setOrderId] = useState("")
    const [status, setStatus] = useState<OrderStatus>("ALL")
    const [range, setRange] = useState<DateRange>("1m")

    const [modalOpen, setModalOpen] = useState(false)
    const [editingOrder, setEditingOrder] = useState<OrderData | null>(null)

    const [loading, setLoading] = useState(true)

    const headerRef = useRef<HTMLDivElement | null>(null)
    const paginationRef = useRef<HTMLDivElement | null>(null)

    const scrollViewportRef = useRef<HTMLDivElement | null>(null)
    const tableRegionRef = useRef<HTMLDivElement | null>(null)

    const [paginationH, setPaginationH] = useState(64)
    const [headerH, setHeaderH] = useState(0)

    const debouncedOrderId = useDebouncedValue(orderId, 350)

    const hasAnyFilter = useMemo(() => {
        return Boolean(debouncedOrderId.trim()) || status !== "ALL" || range !== "1m"
    }, [debouncedOrderId, status, range])

    const fetchOrders = useCallback(
        async (pageToLoad: number) => {
            const toastId = toast.loading("Loading orders...")
            try {
                setLoading(true)

                const trimmedRef = debouncedOrderId.trim()
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
            } catch (e: unknown) {
                toast.error(e instanceof Error ? e.message : "Failed to load orders", { id: toastId })
                return
            } finally {
                setLoading(false)
                toast.dismiss(toastId)
            }
        },
        [debouncedOrderId, status, range, hasAnyFilter]
    )

    useEffect(() => {
        fetchOrders(page)
    }, [page, fetchOrders])

    useEffect(() => {
        setPage(0)
        if (scrollViewportRef.current) scrollViewportRef.current.scrollTop = 0
    }, [debouncedOrderId, status, range])

    const refreshCurrentPage = useCallback(async () => {
        await fetchOrders(page)
    }, [fetchOrders, page])

    const handleEdit = (order: OrderData) => {
        setEditingOrder(order)
        setModalOpen(true)
    }

    useEffect(() => {
        const headerEl = headerRef.current
        const paginationEl = paginationRef.current
        if (!headerEl || !paginationEl) return

        const measure = () => {
            const hH = headerEl.getBoundingClientRect().height
            const pH = paginationEl.getBoundingClientRect().height
            setHeaderH(Math.ceil(hH))
            setPaginationH(Math.ceil(pH))
        }

        measure()

        const ro = new ResizeObserver(() => measure())
        ro.observe(headerEl)
        ro.observe(paginationEl)

        window.addEventListener("resize", measure)
        return () => {
            window.removeEventListener("resize", measure)
            ro.disconnect()
        }
    }, [])

    useEffect(() => {
        const container = scrollViewportRef.current
        const region = tableRegionRef.current
        if (!container || !region) return

        let raf = 0
        const schedule = () => {
            cancelAnimationFrame(raf)
            raf = requestAnimationFrame(() => {
                const candidates = Array.from(
                    region.querySelectorAll<HTMLElement>(
                        ['[aria-expanded="true"]', '[data-state="open"]', '[data-expanded="true"]'].join(",")
                    )
                )
                if (candidates.length === 0) return

                const target = candidates[candidates.length - 1]
                const next = target.nextElementSibling as HTMLElement | null

                const expandedBlock =
                    next &&
                    (next.matches('[data-state="open"], [data-expanded="true"]') ||
                        next.getAttribute("role") === "row" ||
                        next.tagName.toLowerCase() === "tr")
                        ? next
                        : target

                ensureFullyVisible(container, expandedBlock, 12)
            })
        }

        const mo = new MutationObserver(() => schedule())
        mo.observe(region, {
            subtree: true,
            childList: true,
            attributes: true,
            attributeFilter: ["aria-expanded", "data-state", "data-expanded"],
        })

        return () => {
            cancelAnimationFrame(raf)
            mo.disconnect()
        }
    }, [orders])

    const scrollViewportHeight = `calc(100dvh - ${headerH}px - ${paginationH + PAGINATION_LIFT_PX}px)`

    return (
        <div className="h-[100dvh] overflow-hidden bg-background">
            <div className="h-full flex flex-col">
                <div ref={headerRef} className="sticky top-0 z-30 bg-background border-b shrink-0">
                    <div className="max-w-6xl mx-auto px-6 py-5 space-y-4">
                        <div className="flex justify-between items-center">
                            <div>
                                <h1 className="text-2xl font-semibold">Orders</h1>
                                <p className="text-sm text-muted-foreground">Create orders and manage lifecycle</p>
                            </div>

                            {canCreateOrder ? (
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
                            ) : (
                                <div className="h-10" />
                            )}
                        </div>

                        <div className="flex flex-wrap items-center gap-2">
                            <Hint text={`Filter by Order ID (contains). Max ${ORDER_REFERENCE_ID_MAX} characters.`}>
                                <Input
                                    className="w-60 transition focus-visible:ring-2 focus-visible:ring-indigo-500"
                                    placeholder="Search Order ID"
                                    value={orderId}
                                    maxLength={ORDER_REFERENCE_ID_MAX}
                                    onChange={(e) => {
                                        const v = e.target.value
                                        const next = v.replace(/^\s+/, "")
                                        if (next.length > ORDER_REFERENCE_ID_MAX) {
                                            toast.error(`Order ID cannot exceed ${ORDER_REFERENCE_ID_MAX} characters`)
                                            setOrderId(next.slice(0, ORDER_REFERENCE_ID_MAX))
                                            return
                                        }
                                        setOrderId(next)
                                    }}
                                />
                            </Hint>

                            <Hint text="Filter by order status">
                                <div>
                                    <Select value={status} onValueChange={(v) => setStatus(v as OrderStatus)}>
                                        <SelectTrigger className="w-44 transition focus-visible:ring-2 focus-visible:ring-indigo-500 data-[state=open]:ring-2 data-[state=open]:ring-indigo-500">
                                            <SelectValue />
                                        </SelectTrigger>

                                        <SelectContent side="bottom" align="start" sideOffset={4} avoidCollisions={false} position="popper">
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
                                        <SelectTrigger className="w-40 transition focus-visible:ring-2 focus-visible:ring-indigo-500 data-[state=open]:ring-2 data-[state=open]:ring-indigo-500">
                                            <SelectValue />
                                        </SelectTrigger>

                                        <SelectContent side="bottom" align="start" sideOffset={4} avoidCollisions={false} position="popper">
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
                                        if (scrollViewportRef.current) scrollViewportRef.current.scrollTop = 0
                                    }}
                                >
                                    Clear
                                </Button>
                            </Hint>
                        </div>
                    </div>
                </div>

                <div className="flex-1 min-h-0 relative">
                    <div
                        ref={scrollViewportRef}
                        style={{ height: scrollViewportHeight }}
                        className="w-full overflow-y-auto overscroll-none"
                    >
                        <div ref={tableRegionRef} className="max-w-6xl mx-auto px-6 py-6">
                            <OrderTable
                                orders={orders}
                                loading={loading}
                                onInvoiceGenerated={refreshCurrentPage}
                                onEdit={handleEdit}
                            />
                        </div>
                    </div>

                    <div
                        ref={paginationRef}
                        className="absolute left-0 right-0 z-20 bg-background border-t"
                        style={{ bottom: PAGINATION_LIFT_PX }}
                    >
                        <div className="max-w-6xl mx-auto px-6 py-3">
                            <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
                        </div>
                    </div>
                </div>

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
        </div>
    )
}

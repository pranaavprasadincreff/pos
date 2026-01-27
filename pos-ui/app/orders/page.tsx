'use client'

import { useEffect, useMemo, useState } from 'react'
import { subDays } from 'date-fns'

import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select'

import OrderTable from '@/components/orders/OrderTable'
import CreateOrderModal from '@/components/orders/CreateOrderModal'
import Pagination from '@/components/clients/Pagination'

import { getAllOrders } from '@/services/orderService'
import { OrderData } from '@/services/types'

const STATUS_OPTIONS = ['ALL', 'CREATED', 'INVOICED'] as const
type OrderStatus = typeof STATUS_OPTIONS[number]

const RANGE_OPTIONS = ['1d', '1w', '1m'] as const
type DateRange = typeof RANGE_OPTIONS[number]

const PAGE_SIZE = 9

export default function OrdersPage() {
    const [allOrders, setAllOrders] = useState<OrderData[]>([])
    const [page, setPage] = useState(0)

    const [orderId, setOrderId] = useState('')
    const [status, setStatus] = useState<OrderStatus>('ALL')
    const [range, setRange] = useState<DateRange>('1m')

    const [modalOpen, setModalOpen] = useState(false)

    /* ---------------- data fetch ---------------- */

    async function fetchOrders() {
        const collected: OrderData[] = []
        let p = 0
        let total = 1

        while (p < total) {
            const res = await getAllOrders(p, PAGE_SIZE)
            collected.push(...res.content)
            total = res.totalPages
            p++
        }

        setAllOrders(collected)
    }

    useEffect(() => {
        let cancelled = false

        async function load() {
            const collected: OrderData[] = []
            let p = 0
            let total = 1

            while (p < total) {
                const res = await getAllOrders(p, PAGE_SIZE)
                collected.push(...res.content)
                total = res.totalPages
                p++
            }

            if (!cancelled) {
                setAllOrders(collected)
            }
        }

        load()

        return () => {
            cancelled = true
        }
    }, [])

    /* ---------------- filters ---------------- */

    const startDate = useMemo(() => {
        const days =
            range === '1d' ? 1 : range === '1w' ? 7 : 30
        return subDays(new Date(), days)
    }, [range])

    const filteredOrders = useMemo(() => {
        return allOrders.filter((o) => {
            const orderDate = new Date(o.orderTime)

            return (
                (!orderId ||
                    o.orderReferenceId
                        .toLowerCase()
                        .includes(orderId.toLowerCase())) &&
                (status === 'ALL' || o.status === status) &&
                orderDate >= startDate
            )
        })
    }, [allOrders, orderId, status, startDate])

    const totalPages = Math.ceil(filteredOrders.length / PAGE_SIZE)

    const paginatedOrders = filteredOrders.slice(
        page * PAGE_SIZE,
        page * PAGE_SIZE + PAGE_SIZE
    )

    return (
        <div className="space-y-6">
            {/* ================= Sticky Header ================= */}
            <div
                className="
                    sticky top-0 z-30
                    -mt-6
                    bg-background
                    border-b
                "
            >
                <div className="max-w-6xl mx-auto px-6 py-4 space-y-4">
                    {/* Title + Action */}
                    <div className="flex justify-between items-center">
                        <div>
                            <h1 className="text-2xl font-semibold">Orders</h1>
                            <p className="text-sm text-muted-foreground">
                                Create orders and manage invoices
                            </p>
                        </div>

                        <Button
                            className="bg-indigo-600 hover:bg-indigo-700"
                            onClick={() => setModalOpen(true)}
                        >
                            + Create Order
                        </Button>
                    </div>

                    {/* Filters */}
                    <div className="flex flex-wrap items-center gap-2 max-w-4xl">
                        <Input
                            className="
                                w-56
                                focus-visible:ring-2
                                focus-visible:ring-indigo-500
                                transition
                            "
                            placeholder="Order ID"
                            value={orderId}
                            onChange={(e) => {
                                setOrderId(e.target.value)
                                setPage(0)
                            }}
                        />

                        <Select
                            value={status}
                            onValueChange={(v) => {
                                if (
                                    STATUS_OPTIONS.includes(
                                        v as OrderStatus
                                    )
                                ) {
                                    setStatus(v as OrderStatus)
                                    setPage(0)
                                }
                            }}
                        >
                            <SelectTrigger className="w-36">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="ALL">ALL</SelectItem>
                                <SelectItem value="CREATED">
                                    CREATED
                                </SelectItem>
                                <SelectItem value="INVOICED">
                                    INVOICED
                                </SelectItem>
                            </SelectContent>
                        </Select>

                        <Select
                            value={range}
                            onValueChange={(v) => {
                                if (
                                    RANGE_OPTIONS.includes(
                                        v as DateRange
                                    )
                                ) {
                                    setRange(v as DateRange)
                                    setPage(0)
                                }
                            }}
                        >
                            <SelectTrigger className="w-36">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="1d">
                                    Last 1 Day
                                </SelectItem>
                                <SelectItem value="1w">
                                    Last 1 Week
                                </SelectItem>
                                <SelectItem value="1m">
                                    Last 1 Month
                                </SelectItem>
                            </SelectContent>
                        </Select>

                        <Button
                            variant="outline"
                            className="
                                border-slate-900 text-slate-900
                                hover:bg-slate-900 hover:text-white
                            "
                            onClick={() => {
                                setOrderId('')
                                setStatus('ALL')
                                setRange('1m')
                                setPage(0)
                            }}
                        >
                            Clear
                        </Button>
                    </div>
                </div>
            </div>

            {/* ================= Content ================= */}
            <div className="max-w-6xl mx-auto px-6 pt-6 space-y-6">
                <OrderTable
                    orders={paginatedOrders}
                    loading={false}
                    onInvoiceGenerated={fetchOrders}
                />

                <Pagination
                    page={page}
                    totalPages={totalPages}
                    onPageChange={setPage}
                />
            </div>

            {/* ================= Modal ================= */}
            <CreateOrderModal
                open={modalOpen}
                onClose={() => setModalOpen(false)}
                onSuccess={fetchOrders}
            />
        </div>
    )
}

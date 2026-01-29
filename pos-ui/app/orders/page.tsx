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
import CreateOrderModal from '@/components/orders/OrderFormModal'
import Pagination from '@/components/clients/Pagination'

import { getAllOrders } from '@/services/orderService'
import { OrderData } from '@/services/types'

const STATUS_OPTIONS = [
    'ALL',
    'FULFILLABLE',
    'UNFULFILLABLE',
    'INVOICED',
    'CANCELLED',
] as const
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
    const [editingOrder, setEditingOrder] = useState<OrderData | null>(null)
    const [loading, setLoading] = useState(true)

    // ---------------- fetch orders ----------------
    const fetchOrders = async () => {
        setLoading(true)
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
        setLoading(false)
    }

    useEffect(() => {
        let isMounted = true // track if component is still mounted

        const fetch = async () => {
            setLoading(true)
            const collected: OrderData[] = []
            let p = 0
            let total = 1

            while (p < total) {
                const res = await getAllOrders(p, PAGE_SIZE)
                collected.push(...res.content)
                total = res.totalPages
                p++
            }

            if (isMounted) {
                setAllOrders(collected)
                setLoading(false)
            }
        }

        fetch()

        return () => {
            isMounted = false
        }
    }, [])

    // ---------------- filters ----------------
    const startDate = useMemo(() => {
        const days = range === '1d' ? 1 : range === '1w' ? 7 : 30
        return subDays(new Date(), days)
    }, [range])

    const filteredOrders = useMemo(() => {
        return allOrders.filter(o => {
            const orderDate = new Date(o.orderTime)
            return (
                (!orderId || o.orderReferenceId.toLowerCase().includes(orderId.toLowerCase())) &&
                (status === 'ALL' || o.status === status) &&
                orderDate >= startDate
            )
        })
    }, [allOrders, orderId, status, startDate])

    const totalPages = Math.max(1, Math.ceil(filteredOrders.length / PAGE_SIZE))
    const paginatedOrders = filteredOrders.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE)

    // ---------------- edit handler ----------------
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
                            <p className="text-sm text-muted-foreground">Create orders and manage lifecycle</p>
                        </div>
                        <Button className="bg-indigo-600 hover:bg-indigo-700" onClick={() => setModalOpen(true)}>
                            + Create Order
                        </Button>
                    </div>

                    {/* Filters */}
                    <div className="flex flex-wrap items-center gap-2">
                        <Input
                            className="w-60 focus-visible:ring-2 focus-visible:ring-indigo-500"
                            placeholder="Search Order ID"
                            value={orderId}
                            onChange={(e) => { setOrderId(e.target.value); setPage(0) }}
                        />
                        <Select value={status} onValueChange={(v) => { setStatus(v as OrderStatus); setPage(0) }}>
                            <SelectTrigger className="w-44"><SelectValue /></SelectTrigger>
                            <SelectContent>
                                {STATUS_OPTIONS.map(s => <SelectItem key={s} value={s}>{s}</SelectItem>)}
                            </SelectContent>
                        </Select>
                        <Select value={range} onValueChange={(v) => { setRange(v as DateRange); setPage(0) }}>
                            <SelectTrigger className="w-40"><SelectValue /></SelectTrigger>
                            <SelectContent>
                                <SelectItem value="1d">Last 1 Day</SelectItem>
                                <SelectItem value="1w">Last 1 Week</SelectItem>
                                <SelectItem value="1m">Last 1 Month</SelectItem>
                            </SelectContent>
                        </Select>
                        <Button variant="outline" onClick={() => { setOrderId(''); setStatus('ALL'); setRange('1m'); setPage(0) }}>
                            Clear
                        </Button>
                    </div>
                </div>
            </div>

            {/* Orders Table */}
            <div className="max-w-6xl mx-auto px-6 pt-6 space-y-6">
                <OrderTable
                    orders={paginatedOrders}
                    loading={loading}
                    onInvoiceGenerated={fetchOrders}
                    onEdit={handleEdit}
                />
                <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
            </div>

            {/* Create / Edit Modal */}
            <CreateOrderModal
                open={modalOpen}
                onClose={() => { setModalOpen(false); setEditingOrder(null) }}
                onSuccess={fetchOrders}
                orderToEdit={editingOrder}
            />
        </div>
    )
}

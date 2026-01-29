'use client'

import { Fragment, useState } from 'react'
import { OrderData } from '@/services/types'
import {
    Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import { Button } from '@/components/ui/button'
import {
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@/components/ui/tooltip'
import {
    ChevronDown,
    ChevronRight,
    Pencil,
    XCircle,
    RotateCcw,
    FileText,
    Download,
} from 'lucide-react'
import OrderItemsTable from './OrderItemsTable'
import { invoiceService } from '@/services/invoiceService'
import { toast } from 'sonner'
import { downloadBase64Pdf } from '@/utils/downloadPdf'
import { ORDER_STATUS_META } from '@/utils/orderStatus'
import { formatDate } from '@/utils/date'
import { cancel, retryOrder } from '@/services/orderService'

interface OrderTableProps {
    orders: OrderData[]
    loading: boolean
    onInvoiceGenerated: () => void
    onEdit: (order: OrderData) => void
}

export default function OrderTable({
                                       orders, loading, onInvoiceGenerated, onEdit,
                                   }: OrderTableProps) {
    const [expanded, setExpanded] = useState<Set<string>>(new Set())

    const toggle = (orderId: string) => {
        setExpanded(prev => {
            const copy = new Set(prev)
            copy.has(orderId) ? copy.delete(orderId) : copy.add(orderId)
            return copy
        })
    }

    const ICON_BTN = 'h-9 w-9 p-0 rounded-md'

    if (loading) return <div className="rounded-md border p-8 text-center text-muted-foreground">Loading orders…</div>
    if (orders.length === 0) return <div className="rounded-md border p-8 text-center text-muted-foreground">No orders match current filters</div>

    return (
        <TooltipProvider delayDuration={150}>
            <div className="rounded-md border bg-background">
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead />
                            <TableHead>Order ID</TableHead>
                            <TableHead>Created</TableHead>
                            <TableHead>Status</TableHead>
                            <TableHead className="text-right">Total</TableHead>
                            <TableHead className="text-right">Actions</TableHead>
                        </TableRow>
                    </TableHeader>

                    <TableBody>
                        {orders.map(order => {
                            const isOpen = expanded.has(order.orderReferenceId)
                            const total = order.items.reduce((sum, item) => sum + item.quantity * item.sellingPrice, 0)
                            const statusMeta = ORDER_STATUS_META[order.status]

                            return (
                                <Fragment key={order.orderReferenceId}>
                                    <TableRow className="hover:bg-muted/40">
                                        <TableCell className="w-8">
                                            <Button
                                                variant="ghost"
                                                size="icon"
                                                onClick={() => toggle(order.orderReferenceId)}
                                            >
                                                {isOpen
                                                    ? <ChevronDown className="h-4 w-4" />
                                                    : <ChevronRight className="h-4 w-4" />}
                                            </Button>
                                        </TableCell>

                                        <TableCell className="font-medium">
                                            {order.orderReferenceId}
                                        </TableCell>

                                        <TableCell>
                                            {formatDate(order.orderTime)}
                                        </TableCell>

                                        <TableCell>
                                            <span
                                                className={`inline-block px-2 py-1 rounded-md text-xs font-semibold ${statusMeta.badgeClass}`}
                                                style={{ minWidth: '88px', textAlign: 'center' }}
                                            >
                                                {statusMeta.label}
                                            </span>
                                        </TableCell>

                                        <TableCell className="text-right font-medium">
                                            ₹{total.toFixed(2)}
                                        </TableCell>

                                        {/* ACTION ICONS */}
                                        <TableCell className="text-right">
                                            <div className="flex gap-2 justify-end flex-wrap">

                                                {/* EDIT */}
                                                {['FULFILLABLE', 'UNFULFILLABLE'].includes(order.status) && (
                                                    <Tooltip>
                                                        <TooltipTrigger asChild>
                                                            <Button
                                                                className={`bg-yellow-400 hover:bg-yellow-500 text-black ${ICON_BTN}`}
                                                                onClick={() => onEdit(order)}
                                                            >
                                                                <Pencil className="h-4 w-4" />
                                                            </Button>
                                                        </TooltipTrigger>
                                                        <TooltipContent>Edit Order</TooltipContent>
                                                    </Tooltip>
                                                )}

                                                {/* CANCEL */}
                                                {['FULFILLABLE', 'UNFULFILLABLE'].includes(order.status) && (
                                                    <Tooltip>
                                                        <TooltipTrigger asChild>
                                                            <Button
                                                                className={`bg-red-600 hover:bg-red-700 text-white ${ICON_BTN}`}
                                                                onClick={async () => {
                                                                    try {
                                                                        await cancel(order.orderReferenceId)
                                                                        toast.success('Order cancelled')
                                                                        onInvoiceGenerated()
                                                                    } catch {
                                                                        toast.error('Failed to cancel')
                                                                    }
                                                                }}
                                                            >
                                                                <XCircle className="h-4 w-4" />
                                                            </Button>
                                                        </TooltipTrigger>
                                                        <TooltipContent>Cancel Order</TooltipContent>
                                                    </Tooltip>
                                                )}

                                                {/* GENERATE INVOICE — PRIMARY */}
                                                {order.status === 'FULFILLABLE' && (
                                                    <Tooltip>
                                                        <TooltipTrigger asChild>
                                                            <Button
                                                                className={`bg-emerald-600 hover:bg-emerald-700 text-white ${ICON_BTN}`}
                                                                onClick={async () => {
                                                                    try {
                                                                        await invoiceService.generate(order.orderReferenceId)
                                                                        toast.success('Invoice generated')
                                                                        onInvoiceGenerated()
                                                                    } catch {
                                                                        toast.error('Failed to generate invoice')
                                                                    }
                                                                }}
                                                            >
                                                                <FileText className="h-4 w-4" />
                                                            </Button>
                                                        </TooltipTrigger>
                                                        <TooltipContent>Generate Invoice</TooltipContent>
                                                    </Tooltip>
                                                )}

                                                {/* RETRY */}
                                                {order.status === 'UNFULFILLABLE' && (
                                                    <Tooltip>
                                                        <TooltipTrigger asChild>
                                                            <Button
                                                                className={`bg-indigo-600 hover:bg-indigo-700 text-white ${ICON_BTN}`}
                                                                onClick={async () => {
                                                                    try {
                                                                        await retryOrder(order)
                                                                        toast.success('Order retried successfully')
                                                                        onInvoiceGenerated()
                                                                    } catch {
                                                                        toast.error('Failed to retry order')
                                                                    }
                                                                }}
                                                            >
                                                                <RotateCcw className="h-4 w-4" />
                                                            </Button>
                                                        </TooltipTrigger>
                                                        <TooltipContent>Retry Order</TooltipContent>
                                                    </Tooltip>
                                                )}

                                                {/* DOWNLOAD — DISTINCT, LOW ATTENTION */}
                                                {order.status === 'INVOICED' && (
                                                    <Tooltip>
                                                        <TooltipTrigger asChild>
                                                            <Button
                                                                variant="outline"
                                                                className={`border-gray-400 text-gray-700 hover:bg-gray-100 ${ICON_BTN}`}
                                                                onClick={async () => {
                                                                    try {
                                                                        const res = await invoiceService.get(order.orderReferenceId)
                                                                        downloadBase64Pdf(
                                                                            res.data.pdfBase64,
                                                                            `invoice-${order.orderReferenceId}.pdf`
                                                                        )
                                                                    } catch {
                                                                        toast.error('Failed to download invoice')
                                                                    }
                                                                }}
                                                            >
                                                                <Download className="h-4 w-4" />
                                                            </Button>
                                                        </TooltipTrigger>
                                                        <TooltipContent>Download Invoice</TooltipContent>
                                                    </Tooltip>
                                                )}

                                            </div>
                                        </TableCell>
                                    </TableRow>

                                    {/* EXPANDED ITEMS ROW — grouped with parent */}
                                    {isOpen && (
                                        <TableRow>
                                            <TableCell colSpan={6} className="bg-muted/30">
                                                <OrderItemsTable items={order.items} />
                                            </TableCell>
                                        </TableRow>
                                    )}
                                </Fragment>
                            )
                        })}
                    </TableBody>
                </Table>
            </div>
        </TooltipProvider>
    )
}

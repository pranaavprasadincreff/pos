'use client'

import { Fragment, useState } from 'react'
import { OrderData } from '@/services/types'
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/components/ui/table'
import { Button } from '@/components/ui/button'
import { ChevronDown, ChevronRight } from 'lucide-react'
import OrderItemsTable from './OrderItemsTable'
import { invoiceService } from '@/services/invoiceService'
import { toast } from 'sonner'
import { downloadBase64Pdf } from '@/utils/downloadPdf'

interface OrderTableProps {
    orders: OrderData[]
    loading: boolean
    onInvoiceGenerated: () => void
}

export default function OrderTable({
                                       orders,
                                       loading,
                                       onInvoiceGenerated,
                                   }: OrderTableProps) {
    const [expanded, setExpanded] = useState<Set<string>>(new Set())

    const toggle = (orderId: string): void => {
        setExpanded(prev => {
            const copy = new Set(prev)
            copy.has(orderId) ? copy.delete(orderId) : copy.add(orderId)
            return copy
        })
    }

    if (loading) {
        return <div className="text-sm text-muted-foreground">Loading orders…</div>
    }

    if (orders.length === 0) {
        return <div className="text-sm text-muted-foreground">No orders found</div>
    }

    return (
        <div className="rounded-md border">
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead />
                        <TableHead>Order ID</TableHead>
                        <TableHead>Created At</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead className="text-right">Invoice</TableHead>
                    </TableRow>
                </TableHeader>

                <TableBody>
                    {orders.map(order => {
                        const isOpen = expanded.has(order.orderReferenceId)

                        return (
                            <Fragment key={order.orderReferenceId}>
                                <TableRow>
                                    <TableCell className="w-8">
                                        <Button
                                            variant="ghost"
                                            size="icon"
                                            onClick={() => toggle(order.orderReferenceId)}
                                        >
                                            {isOpen ? (
                                                <ChevronDown className="h-4 w-4" />
                                            ) : (
                                                <ChevronRight className="h-4 w-4" />
                                            )}
                                        </Button>
                                    </TableCell>

                                    <TableCell className="font-medium">
                                        {order.orderReferenceId}
                                    </TableCell>

                                    <TableCell>
                                        {new Date(order.orderTime).toLocaleString('en-IN')}
                                    </TableCell>

                                    <TableCell>
                                        <span
                                            className={
                                                order.status === 'INVOICED'
                                                    ? 'text-green-600 font-medium'
                                                    : 'text-yellow-600 font-medium'
                                            }
                                        >
                                            {order.status}
                                        </span>
                                    </TableCell>

                                    <TableCell className="text-right">
                                        {order.status === 'CREATED' ? (
                                            <Button
                                                size="sm"
                                                className="w-36"
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
                                                Generate Invoice
                                            </Button>
                                        ) : (
                                            <Button
                                                variant="outline"
                                                size="sm"
                                                className="w-36"
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
                                                Download Invoice
                                            </Button>
                                        )}
                                    </TableCell>
                                </TableRow>

                                {isOpen && (
                                    <TableRow>
                                        <TableCell colSpan={5} className="bg-muted/30">
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
    )
}

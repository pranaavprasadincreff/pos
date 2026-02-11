'use client'

import { Fragment, useMemo, useState } from 'react'
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
    Loader2,
} from 'lucide-react'
import OrderItemsTable from './OrderItemsTable'
import { invoiceService } from '@/services/invoiceService'
import { toast } from 'sonner'
import { downloadBase64Pdf } from '@/utils/downloadPdf'
import { ORDER_STATUS_META } from '@/utils/orderStatus'
import { formatDate } from '@/utils/date'
import { cancelOrder, retryOrder } from '@/services/orderService'
import { cn } from '@/lib/utils'
import {
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog'
import { formatINR } from "@/utils/currencyFormat"
import { can, getSessionRole } from "@/utils/permissions"

interface OrderTableProps {
    orders: OrderData[]
    loading: boolean
    onInvoiceGenerated: () => void
    onEdit: (order: OrderData) => void
}

export default function OrderTable({ orders, loading, onInvoiceGenerated, onEdit }: OrderTableProps) {
    const role = getSessionRole()

    const canEdit = useMemo(() => can(role, "order_edit"), [role])
    const canCancel = useMemo(() => can(role, "order_cancel"), [role])
    const canRetry = useMemo(() => can(role, "order_retry"), [role])
    const canGenerateInvoice = useMemo(() => can(role, "invoice_generate"), [role])
    const canDownloadInvoice = useMemo(() => can(role, "invoice_get"), [role])

    const [expanded, setExpanded] = useState<Set<string>>(new Set())

    const [confirmOpen, setConfirmOpen] = useState(false)
    const [pendingCancelRef, setPendingCancelRef] = useState<string | null>(null)
    const [cancelLoading, setCancelLoading] = useState(false)

    const toggle = (orderId: string) => {
        setExpanded((prev) => {
            const copy = new Set(prev)
            copy.has(orderId) ? copy.delete(orderId) : copy.add(orderId)
            return copy
        })
    }

    const ICON_BTN = 'h-9 w-9 p-0 rounded-md'

    const openCancelConfirm = (orderReferenceId: string) => {
        if (!canCancel) return
        setPendingCancelRef(orderReferenceId)
        setConfirmOpen(true)
    }

    const closeCancelConfirm = () => {
        if (cancelLoading) return
        setConfirmOpen(false)
        setPendingCancelRef(null)
    }

    const confirmCancel = async () => {
        if (!pendingCancelRef || cancelLoading || !canCancel) return
        setCancelLoading(true)
        try {
            await cancelOrder(pendingCancelRef)
            toast.success('Order cancelled')
            onInvoiceGenerated()
            setConfirmOpen(false)
            setPendingCancelRef(null)
        } catch (e: unknown) {
            toast.error(e instanceof Error ? e.message : 'Failed to cancel')
        } finally {
            setCancelLoading(false)
        }
    }

    if (loading) {
        return (
            <div className="rounded-md border p-8 text-center text-muted-foreground">
                Loading orders…
            </div>
        )
    }

    if (orders.length === 0) {
        return (
            <div className="rounded-md border p-8 text-center text-muted-foreground">
                No orders match current filters
            </div>
        )
    }

    return (
        <>
            {/* Confirm Cancel Modal */}
            <Dialog
                open={confirmOpen}
                onOpenChange={(open) => {
                    if (!open) return
                    setConfirmOpen(true)
                }}
            >
                <DialogContent
                    className="max-w-md"
                    onInteractOutside={(e) => e.preventDefault()}
                    onEscapeKeyDown={(e) => e.preventDefault()}
                >
                    <DialogHeader>
                        <DialogTitle>Cancel this order?</DialogTitle>
                    </DialogHeader>

                    <div className="text-sm text-muted-foreground">
                        {pendingCancelRef ? (
                            <>
                                You’re about to cancel{' '}
                                <span className="font-medium text-foreground">{pendingCancelRef}</span>.
                                This action cannot be undone.
                            </>
                        ) : (
                            <>You’re about to cancel this order. This action cannot be undone.</>
                        )}
                    </div>

                    <DialogFooter className="mt-2 flex gap-2 sm:justify-end">
                        <Button type="button" variant="outline" onClick={closeCancelConfirm} disabled={cancelLoading}>
                            Keep Order
                        </Button>

                        <Button
                            type="button"
                            className="bg-red-600 hover:bg-red-700 text-white"
                            onClick={confirmCancel}
                            disabled={cancelLoading || !pendingCancelRef}
                        >
                            {cancelLoading ? (
                                <span className="inline-flex items-center gap-2">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Cancelling…
                </span>
                            ) : (
                                'Confirm Cancel'
                            )}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            <TooltipProvider delayDuration={150}>
                <div className="rounded-md border bg-background">
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead />
                                <TableHead className="w-[220px]">Order ID</TableHead>
                                <TableHead className="text-center">Created</TableHead>
                                <TableHead className="text-center">Status</TableHead>
                                <TableHead className="text-right">Total</TableHead>
                                <TableHead className="text-right">Actions</TableHead>
                            </TableRow>
                        </TableHeader>

                        <TableBody>
                            {orders.map((order) => {
                                const isOpen = expanded.has(order.orderReferenceId)
                                const total = order.items.reduce((sum, item) => sum + item.quantity * item.sellingPrice, 0)
                                const statusMeta = ORDER_STATUS_META[order.status]

                                const canEditThis = canEdit && ['FULFILLABLE', 'UNFULFILLABLE'].includes(order.status)
                                const canCancelThis = canCancel && ['FULFILLABLE', 'UNFULFILLABLE'].includes(order.status)
                                const canGenerateThis = canGenerateInvoice && order.status === 'FULFILLABLE'
                                const canRetryThis = canRetry && order.status === 'UNFULFILLABLE'
                                const canDownloadThis = canDownloadInvoice && order.status === 'INVOICED'

                                return (
                                    <Fragment key={order.orderReferenceId}>
                                        <TableRow
                                            className={cn(
                                                'hover:bg-muted/40',
                                                isOpen && 'bg-indigo-50/40 border border-indigo-200 border-b-0'
                                            )}
                                        >
                                            <TableCell className="w-8">
                                                <Button variant="ghost" size="icon" onClick={() => toggle(order.orderReferenceId)}>
                                                    {isOpen ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
                                                </Button>
                                            </TableCell>

                                            <TableCell className="font-medium w-[220px] max-w-[220px] truncate" title={order.orderReferenceId}>
                                              {order.orderReferenceId}
                                            </TableCell>
                                            <TableCell className="text-center">{formatDate(order.createdAt)}</TableCell>

                                            <TableCell className="text-center">
                        <span
                            className={`inline-block px-2 py-1 rounded-md text-xs font-semibold ${statusMeta.badgeClass}`}
                            style={{ minWidth: '88px', textAlign: 'center' }}
                        >
                          {statusMeta.label}
                        </span>
                                            </TableCell>

                                            <TableCell className="text-right font-medium">₹{formatINR(total)}</TableCell>

                                            <TableCell className="text-right">
                                                <div className="flex gap-2 justify-end flex-wrap">
                                                    {canEditThis && (
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

                                                    {canCancelThis && (
                                                        <Tooltip>
                                                            <TooltipTrigger asChild>
                                                                <Button
                                                                    className={`bg-red-600 hover:bg-red-700 text-white ${ICON_BTN}`}
                                                                    onClick={() => openCancelConfirm(order.orderReferenceId)}
                                                                >
                                                                    <XCircle className="h-4 w-4" />
                                                                </Button>
                                                            </TooltipTrigger>
                                                            <TooltipContent>Cancel Order</TooltipContent>
                                                        </Tooltip>
                                                    )}

                                                    {canGenerateThis && (
                                                        <Tooltip>
                                                            <TooltipTrigger asChild>
                                                                <Button
                                                                    className={`bg-emerald-600 hover:bg-emerald-700 text-white ${ICON_BTN}`}
                                                                    onClick={async () => {
                                                                        const toastId = toast.loading("Generating invoice...")
                                                                        try {
                                                                            await invoiceService.generate(order.orderReferenceId)
                                                                            toast.success("Invoice generated", { id: toastId })
                                                                            onInvoiceGenerated()
                                                                        } catch (e: unknown) {
                                                                            toast.error(e instanceof Error ? e.message : "Failed to generate invoice", { id: toastId })
                                                                        }
                                                                    }}
                                                                >
                                                                    <FileText className="h-4 w-4" />
                                                                </Button>
                                                            </TooltipTrigger>
                                                            <TooltipContent>Generate Invoice</TooltipContent>
                                                        </Tooltip>
                                                    )}

                                                    {canRetryThis && (
                                                        <Tooltip>
                                                            <TooltipTrigger asChild>
                                                                <Button
                                                                    className={`bg-indigo-600 hover:bg-indigo-700 text-white ${ICON_BTN}`}
                                                                    onClick={async () => {
                                                                        const toastId = toast.loading("Retrying...")
                                                                        try {
                                                                            await retryOrder(order)
                                                                            toast.success("Order retried", { id: toastId })
                                                                            onInvoiceGenerated()
                                                                        } catch (e: unknown) {
                                                                            toast.error(e instanceof Error ? e.message : "Failed to retry order", { id: toastId })
                                                                        }
                                                                    }}
                                                                >
                                                                    <RotateCcw className="h-4 w-4" />
                                                                </Button>
                                                            </TooltipTrigger>
                                                            <TooltipContent>Retry Order</TooltipContent>
                                                        </Tooltip>
                                                    )}

                                                    {canDownloadThis && (
                                                        <Tooltip>
                                                            <TooltipTrigger asChild>
                                                                <Button
                                                                    variant="outline"
                                                                    className={`border-gray-400 text-gray-700 hover:bg-gray-100 ${ICON_BTN}`}
                                                                    onClick={async () => {
                                                                        const toastId = toast.loading("Downloading...")
                                                                        try {
                                                                            const res = await invoiceService.get(order.orderReferenceId)
                                                                            downloadBase64Pdf(res.data.pdfBase64, `invoice-${order.orderReferenceId}.pdf`)
                                                                            toast.success("Downloaded", { id: toastId })
                                                                        } catch (e: unknown) {
                                                                            toast.error(e instanceof Error ? e.message : "Failed to download invoice", { id: toastId })
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

                                        {isOpen && (
                                            <TableRow>
                                                <TableCell colSpan={6} className="border border-t-0 border-indigo-200 bg-indigo-50/40 rounded-b-md px-4 py-4">
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
        </>
    )
}

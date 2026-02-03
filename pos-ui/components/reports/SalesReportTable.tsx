"use client"

import { Fragment, useState } from "react"
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"
import { Button } from "@/components/ui/button"
import { ChevronDown, ChevronRight, Loader2 } from "lucide-react"
import { cn } from "@/lib/utils"
import type { SalesReportRowData, ReportRowType } from "@/services/salesReportService"
import {formatINR} from "@/utils/currencyFormat";

type ExpandFetch = (clientEmail: string) => Promise<SalesReportRowData[]>

export default function SalesReportTable({
                                             rowType,
                                             rows,
                                             loading,
                                             onExpandFetch,
                                         }: {
    rowType: ReportRowType
    rows: SalesReportRowData[]
    loading: boolean
    onExpandFetch?: ExpandFetch // only used for CLIENT table
}) {
    const [expanded, setExpanded] = useState<Set<string>>(new Set())
    const [loadingClient, setLoadingClient] = useState<Record<string, boolean>>({})
    const [childrenByClient, setChildrenByClient] = useState<Record<string, SalesReportRowData[]>>({})

    const toggle = async (clientEmail: string) => {
        setExpanded((prev) => {
            const next = new Set(prev)
            next.has(clientEmail) ? next.delete(clientEmail) : next.add(clientEmail)
            return next
        })

        // fetch only when opening and not already cached
        const isOpenNow = !expanded.has(clientEmail)
        if (!isOpenNow) return
        if (!onExpandFetch) return
        if (childrenByClient[clientEmail]) return
        if (loadingClient[clientEmail]) return

        setLoadingClient((prev) => ({ ...prev, [clientEmail]: true }))
        try {
            const childRows = await onExpandFetch(clientEmail)
            setChildrenByClient((prev) => ({ ...prev, [clientEmail]: childRows }))
        } finally {
            setLoadingClient((prev) => ({ ...prev, [clientEmail]: false }))
        }
    }

    if (loading) {
        return <div className="rounded-md border p-8 text-center text-muted-foreground">Loading report…</div>
    }

    if (!rows || rows.length === 0) {
        return <div className="rounded-md border p-8 text-center text-muted-foreground">No data for selected filters</div>
    }

    const isClientTable = rowType === "CLIENT"

    return (
        <div className="rounded-md border bg-background">
            <Table>
                <TableHeader>
                    <TableRow>
                        {isClientTable ? <TableHead className="w-8" /> : null}
                        <TableHead>{isClientTable ? "Client Email" : "Product Barcode"}</TableHead>
                        {isClientTable ? <TableHead className="w-[220px]"> </TableHead> : <TableHead>Client Email</TableHead>}
                        <TableHead className="text-right">Orders</TableHead>
                        <TableHead className="text-right">Items</TableHead>
                        <TableHead className="text-right">Revenue</TableHead>
                    </TableRow>
                </TableHeader>

                <TableBody>
                    {rows.map((r) => {
                        const key = isClientTable ? (r.clientEmail || "") : (r.productBarcode || "")
                        const clientEmail = r.clientEmail || ""
                        const isOpen = isClientTable ? expanded.has(clientEmail) : false

                        return (
                            <Fragment key={`${key}-${r.clientEmail ?? ""}-${r.productBarcode ?? ""}`}>
                                <TableRow
                                    className={cn(
                                        "hover:bg-muted/40",
                                        // keep the “flowing” connected feel
                                        isClientTable && isOpen && "bg-indigo-50/40 border border-indigo-200 border-b-0"
                                    )}
                                >
                                    {isClientTable ? (
                                        <TableCell className="w-8">
                                            <Button
                                                variant="ghost"
                                                size="icon"
                                                onClick={() => toggle(clientEmail)}
                                                disabled={!clientEmail}
                                            >
                                                {isOpen ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
                                            </Button>
                                        </TableCell>
                                    ) : null}

                                    <TableCell className="font-medium">
                                        {isClientTable ? r.clientEmail : r.productBarcode}
                                    </TableCell>

                                    {isClientTable ? (
                                        <TableCell className="text-sm text-muted-foreground">
                                            {loadingClient[clientEmail] ? (
                                                <span className="inline-flex items-center gap-2">
                          <Loader2 className="h-4 w-4 animate-spin" />
                          Loading product rows…
                        </span>
                                            ) : (
                                                <span />
                                            )}
                                        </TableCell>
                                    ) : (
                                        <TableCell className="text-sm">{r.clientEmail}</TableCell>
                                    )}

                                    <TableCell className="text-right">{r.ordersCount}</TableCell>
                                    <TableCell className="text-right">{r.itemsCount}</TableCell>
                                    <TableCell className="text-right font-medium">
                                        ₹{formatINR(r.totalRevenue || 0)}
                                    </TableCell>
                                </TableRow>

                                {isClientTable && isOpen ? (
                                    <TableRow>
                                        <TableCell
                                            colSpan={6}
                                            className="
                        border border-t-0 border-indigo-200
                        bg-indigo-50/40
                        rounded-b-md
                        px-4 py-4
                      "
                                        >
                                            {/* keep it connected to the expanded row; just make header more distinct */}
                                            <div className="rounded-md border border-indigo-200 bg-background overflow-hidden">
                                                <Table>
                                                    <TableHeader>
                                                        <TableRow
                                                            className={cn(
                                                                // distinct header
                                                                "bg-muted/60",
                                                                // separation from body
                                                                "border-b border-indigo-200/70",
                                                                // make header labels pop
                                                                "[&>th]:py-3 [&>th]:text-xs [&>th]:font-semibold [&>th]:uppercase [&>th]:tracking-wider [&>th]:text-foreground",
                                                                // subtle extra separation line (optional but nice)
                                                                "shadow-[inset_0_-1px_0_0_rgba(99,102,241,0.25)]"
                                                            )}
                                                        >
                                                            <TableHead>Product Barcode</TableHead>
                                                            <TableHead className="text-right">Orders</TableHead>
                                                            <TableHead className="text-right">Items</TableHead>
                                                            <TableHead className="text-right">Revenue</TableHead>
                                                        </TableRow>
                                                    </TableHeader>

                                                    <TableBody>
                                                        {(childrenByClient[clientEmail] || []).map((cr, idx) => (
                                                            <TableRow key={`${cr.productBarcode}-${idx}`} className="hover:bg-muted/30">
                                                                <TableCell className="font-medium">{cr.productBarcode}</TableCell>
                                                                <TableCell className="text-right">{cr.ordersCount}</TableCell>
                                                                <TableCell className="text-right">{cr.itemsCount}</TableCell>
                                                                <TableCell className="text-right font-medium">
                                                                    ₹{formatINR(cr.totalRevenue || 0)}
                                                                </TableCell>
                                                            </TableRow>
                                                        ))}

                                                        {childrenByClient[clientEmail] && childrenByClient[clientEmail].length === 0 && (
                                                            <TableRow>
                                                                <TableCell colSpan={4} className="text-muted-foreground text-center py-6">
                                                                    No product rows for this client
                                                                </TableCell>
                                                            </TableRow>
                                                        )}
                                                    </TableBody>
                                                </Table>
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                ) : null}
                            </Fragment>
                        )
                    })}
                </TableBody>
            </Table>
        </div>
    )
}

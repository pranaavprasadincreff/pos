"use client"

import { Fragment, useEffect, useState } from "react"
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
import type { SalesReportRowData, ReportRowType } from "@/services/reportService"
import { formatINR } from "@/utils/currencyFormat"
import { toast } from "sonner"
import axios from "axios"

type ExpandFetch = (clientEmail: string) => Promise<SalesReportRowData[]>

export default function SalesReportTable({
                                             rowType,
                                             rows,
                                             loading,
                                             onExpandFetch,
                                             cacheKey,
                                             expandContextKey,
                                         }: {
    rowType: ReportRowType
    rows: SalesReportRowData[]
    loading: boolean
    onExpandFetch?: ExpandFetch
    cacheKey: string
    expandContextKey: string
}) {
    const [expanded, setExpanded] = useState<Set<string>>(new Set())
    const [loadingClient, setLoadingClient] = useState<Record<string, boolean>>({})
    const [childrenByClient, setChildrenByClient] = useState<Record<string, SalesReportRowData[]>>({})

    useEffect(() => {
        setExpanded(new Set())
        setLoadingClient({})
        setChildrenByClient({})
    }, [expandContextKey])

    const explainError = (e: unknown) => {
        if (axios.isAxiosError(e)) {
            const status = e.response?.status
            const data = e.response?.data
            const msg =
                (data && typeof data === "object" && "message" in data && (data as any).message) ||
                (typeof data === "string" ? data : "") ||
                e.message

            return `Expand failed${status ? ` (${status})` : ""}: ${String(msg)}`
        }
        if (e instanceof Error) return `Expand failed: ${e.message}`
        return "Expand failed: Unknown error"
    }

    const toggle = async (clientEmail: string) => {
        if (!clientEmail) return

        const wasOpen = expanded.has(clientEmail)
        const willOpen = !wasOpen

        setExpanded((prev) => {
            const next = new Set(prev)
            if (next.has(clientEmail)) next.delete(clientEmail)
            else next.add(clientEmail)
            return next
        })

        if (!willOpen) {
            setChildrenByClient((prev) => {
                const next = { ...prev }
                delete next[clientEmail]
                return next
            })
            return
        }

        if (!onExpandFetch) {
            toast.error("Expand handler missing (onExpandFetch not provided).")
            return
        }
        if (childrenByClient[clientEmail]) return
        if (loadingClient[clientEmail]) return

        setLoadingClient((prev) => ({ ...prev, [clientEmail]: true }))

        try {
            // 🔎 DIAGNOSTIC: confirm exact key being sent
            console.log("[SalesReportTable] Expanding client:", clientEmail)

            const childRows = await onExpandFetch(clientEmail)

            // 🔎 DIAGNOSTIC: confirm response
            console.log("[SalesReportTable] Expand rows:", childRows)

            setChildrenByClient((prev) => ({ ...prev, [clientEmail]: childRows }))
        } catch (e) {
            const msg = explainError(e)
            console.error("[SalesReportTable] Expand error:", e)
            toast.error(msg)

            // if expand failed, collapse row (optional but avoids “open with empty content”)
            setExpanded((prev) => {
                const next = new Set(prev)
                next.delete(clientEmail)
                return next
            })
        } finally {
            setLoadingClient((prev) => ({ ...prev, [clientEmail]: false }))
        }
    }

    if (loading) {
        return (
            <div className="rounded-md border p-8 text-center text-muted-foreground">
                Loading report…
            </div>
        )
    }

    if (!rows || rows.length === 0) {
        return (
            <div className="rounded-md border p-8 text-center text-muted-foreground">
                No data for selected filters
            </div>
        )
    }

    const isClientTable = rowType === "CLIENT"

    return (
        <div className="rounded-md border bg-background">
            <Table>
                <TableHeader>
                    <TableRow>
                        {isClientTable ? <TableHead className="w-8" /> : null}
                        <TableHead>{isClientTable ? "Client Email" : "Product Barcode"}</TableHead>
                        {isClientTable ? (
                            <TableHead className="w-[220px]"> </TableHead>
                        ) : (
                            <TableHead>Client Email</TableHead>
                        )}
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
                                            className="border border-t-0 border-indigo-200 bg-indigo-50/40 rounded-b-md px-4 py-4"
                                        >
                                            <div className="rounded-md border border-indigo-200 bg-background overflow-hidden">
                                                <Table>
                                                    <TableHeader>
                                                        <TableRow
                                                            className={cn(
                                                                "bg-muted/60",
                                                                "border-b border-indigo-200/70",
                                                                "[&>th]:py-3 [&>th]:text-xs [&>th]:font-semibold [&>th]:uppercase [&>th]:tracking-wider [&>th]:text-foreground",
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

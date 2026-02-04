"use client"

import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Hint } from "@/components/shared/Hint"
import { toast } from "sonner"
import { cn } from "@/lib/utils"
import SalesReportTable from "@/components/reports/SalesReportTable"
import {
    getDailySalesReport,
    getRangeSalesReport,
    type SalesReportResponseData,
    type SalesReportRowData,
    type ReportRowType,
} from "@/services/reportService"
import { Loader2, ArrowRight, Calendar as CalendarIcon } from "lucide-react"
import { Calendar } from "@/components/ui/calendar"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { format } from "date-fns"
import Pagination from "@/components/shared/Pagination"

type Mode = "daily" | "range"

const CLIENT_EMAIL_MAX = 120
const EMAIL_RE = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/

const PAGE_SIZE = 9
const PAGINATION_LIFT_PX = 64
const EXPAND_CONCURRENCY = 4

function todayISO() {
    const d = new Date()
    const yyyy = d.getFullYear()
    const mm = String(d.getMonth() + 1).padStart(2, "0")
    const dd = String(d.getDate()).padStart(2, "0")
    return `${yyyy}-${mm}-${dd}`
}

function startOfTodayLocal() {
    const d = new Date()
    d.setHours(0, 0, 0, 0)
    return d
}

function isoToDate(iso?: string) {
    if (!iso) return undefined
    const d = new Date(iso + "T00:00:00")
    return Number.isNaN(d.getTime()) ? undefined : d
}

function dateToISO(d: Date) {
    const yyyy = d.getFullYear()
    const mm = String(d.getMonth() + 1).padStart(2, "0")
    const dd = String(d.getDate()).padStart(2, "0")
    return `${yyyy}-${mm}-${dd}`
}

function isoDaysBetweenInclusive(start: string, end: string) {
    const s = new Date(start + "T00:00:00")
    const e = new Date(end + "T00:00:00")
    const diff = Math.floor((e.getTime() - s.getTime()) / (1000 * 60 * 60 * 24))
    return diff + 1
}

function addDaysISO(iso: string, days: number) {
    const d = new Date(iso + "T00:00:00")
    d.setDate(d.getDate() + days)
    return dateToISO(d)
}

function listISOInclusive(start: string, end: string) {
    const out: string[] = []
    let cur = start
    while (cur <= end) {
        out.push(cur)
        cur = addDaysISO(cur, 1)
    }
    return out
}

function useDebouncedValue<T>(value: T, delayMs: number) {
    const [debounced, setDebounced] = useState(value)
    useEffect(() => {
        const t = setTimeout(() => setDebounced(value), delayMs)
        return () => clearTimeout(t)
    }, [value, delayMs])
    return debounced
}

function mergeProductRows(rows: SalesReportRowData[]) {
    const keyOf = (r: any) =>
        String(
            r.productId ??
            r.productSku ??
            r.sku ??
            r.barcode ??
            r.productBarcode ??
            r.productName ??
            r.name ??
            r.id
        )

    const merged = new Map<string, any>()

    for (const r of rows as any[]) {
        const k = keyOf(r)
        const existing = merged.get(k)
        if (!existing) {
            merged.set(k, { ...r })
            continue
        }

        for (const [field, val] of Object.entries(r)) {
            if (typeof val === "number" && Number.isFinite(val)) {
                const prev = existing[field]
                existing[field] =
                    typeof prev === "number" && Number.isFinite(prev) ? prev + val : val
            } else if (existing[field] == null) {
                existing[field] = val
            }
        }
    }

    return Array.from(merged.values()) as SalesReportRowData[]
}

export default function ReportsPage() {
    const [mode, setMode] = useState<Mode>("daily")

    const [dailyDate, setDailyDate] = useState<string>(() => todayISO())
    const [startDate, setStartDate] = useState<string>(() => todayISO())
    const [endDate, setEndDate] = useState<string>(() => todayISO())

    const [openDaily, setOpenDaily] = useState(false)
    const [openFrom, setOpenFrom] = useState(false)
    const [openTo, setOpenTo] = useState(false)

    // ✅ EXACT ClientsPage-style search
    const [clientEmail, setClientEmail] = useState<string>("")
    const [searchError, setSearchError] = useState<string | null>(null)
    const debouncedClientEmail = useDebouncedValue(clientEmail, 500)

    const clientInputImmediate = useMemo(
        () => clientEmail.trim().toLowerCase(),
        [clientEmail]
    )
    const clientInput = useMemo(
        () => debouncedClientEmail.trim().toLowerCase(),
        [debouncedClientEmail]
    )

    const isExactClientEmailImmediate = useMemo(
        () => Boolean(clientInputImmediate) && EMAIL_RE.test(clientInputImmediate),
        [clientInputImmediate]
    )
    const isExactClientEmail = useMemo(
        () => Boolean(clientInput) && EMAIL_RE.test(clientInput),
        [clientInput]
    )

    const effectiveRowType: ReportRowType = useMemo(() => {
        return isExactClientEmailImmediate ? "PRODUCT" : "CLIENT"
    }, [isExactClientEmailImmediate])

    const today = useMemo(() => startOfTodayLocal(), [])

    const [loading, setLoading] = useState(false)
    const [report, setReport] = useState<SalesReportResponseData | null>(null)

    const [page, setPage] = useState(0)

    const headerRef = useRef<HTMLDivElement | null>(null)
    const paginationRef = useRef<HTMLDivElement | null>(null)
    const scrollViewportRef = useRef<HTMLDivElement | null>(null)

    const [headerH, setHeaderH] = useState(0)
    const [paginationH, setPaginationH] = useState(56)

    // ✅ validate filters WITHOUT tying to keystrokes (debounced + dates only)
    const validateInputs = useCallback((): string | null => {
        if (searchError) return searchError

        const t = todayISO()

        if (mode === "daily") {
            if (!dailyDate) return "Date is required"
            if (dailyDate > t) return "Daily report date cannot be in the future"
            return null
        }

        if (!startDate || !endDate) return "Start and end date are required"
        if (startDate > endDate) return "Start date cannot be after end date"
        if (startDate > t) return "Start date cannot be in the future"
        if (endDate > t) return "End date cannot be in the future"

        const days = isoDaysBetweenInclusive(startDate, endDate)
        if (days > 92) return "Date range too large (max 92 days)"
        return null
    }, [searchError, mode, dailyDate, startDate, endDate])

    const fetchReport = useCallback(async () => {
        const err = validateInputs()
        if (err) {
            toast.error(err)
            return
        }

        // ✅ backend ONLY receives DEBOUNCED email if it's an exact email
        const backendClientEmail = isExactClientEmail ? clientInput : undefined

        try {
            setLoading(true)

            const res =
                mode === "daily"
                    ? await getDailySalesReport({
                        startDate: dailyDate,
                        clientEmail: backendClientEmail,
                    })
                    : await getRangeSalesReport({
                        startDate,
                        endDate,
                        clientEmail: backendClientEmail,
                    })

            setReport(res)
            setPage(0)
        } finally {
            setLoading(false)
        }
    }, [validateInputs, mode, dailyDate, startDate, endDate, clientInput, isExactClientEmail])

    // ✅ This will now only run after 500ms pause (because it depends on clientInput)
    useEffect(() => {
        fetchReport()
    }, [fetchReport])

    const filteredRows = useMemo(() => {
        if (!report?.rows) return []

        const rowType = report.rowType ?? effectiveRowType
        if (rowType !== "CLIENT") return report.rows

        if (!clientInputImmediate) return report.rows
        if (isExactClientEmailImmediate) return report.rows

        return report.rows.filter((r) =>
            (r.clientEmail || "").toLowerCase().includes(clientInputImmediate)
        )
    }, [report, effectiveRowType, clientInputImmediate, isExactClientEmailImmediate])

    const totalPages = useMemo(
        () => Math.max(1, Math.ceil(filteredRows.length / PAGE_SIZE)),
        [filteredRows.length]
    )

    const pagedRows = useMemo(() => {
        const start = page * PAGE_SIZE
        return filteredRows.slice(start, start + PAGE_SIZE)
    }, [filteredRows, page])

    useEffect(() => {
        setPage(0)
        if (scrollViewportRef.current) scrollViewportRef.current.scrollTop = 0
    }, [mode, dailyDate, startDate, endDate, clientInputImmediate, isExactClientEmailImmediate])

    const onExpandFetch = useCallback(
        async (email: string): Promise<SalesReportRowData[]> => {
            if (!email) return []

            if (mode === "daily") {
                const res = await getDailySalesReport({ startDate: dailyDate, clientEmail: email })
                return res.rows || []
            }

            const days = listISOInclusive(startDate, endDate)

            let idx = 0
            const collected: SalesReportRowData[] = []

            const workers = Array.from({ length: EXPAND_CONCURRENCY }).map(async () => {
                while (idx < days.length) {
                    const d = days[idx++]
                    const res = await getDailySalesReport({ startDate: d, clientEmail: email })
                    if (res.rows?.length) collected.push(...res.rows)
                }
            })

            await Promise.all(workers)
            return mergeProductRows(collected)
        },
        [mode, dailyDate, startDate, endDate]
    )

    function switchMode(next: Mode) {
        if (next === mode) return
        setMode(next)
        setReport(null)
        setPage(0)
    }

    function clearFilters() {
        const t = todayISO()
        setClientEmail("")
        setSearchError(null)
        setDailyDate(t)
        setStartDate(t)
        setEndDate(t)
        setPage(0)
    }

    const dailySelected = useMemo(() => isoToDate(dailyDate), [dailyDate])
    const fromSelected = useMemo(() => isoToDate(startDate), [startDate])
    const toSelected = useMemo(() => isoToDate(endDate), [endDate])

    useEffect(() => {
        const headerEl = headerRef.current
        const paginationEl = paginationRef.current
        if (!headerEl || !paginationEl) return

        const measure = () => {
            setHeaderH(Math.ceil(headerEl.getBoundingClientRect().height))
            setPaginationH(Math.ceil(paginationEl.getBoundingClientRect().height))
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

    const bottomOffset = `calc(env(safe-area-inset-bottom, 0px) + ${PAGINATION_LIFT_PX}px)`
    const scrollViewportHeight = `calc(100dvh - ${headerH}px - ${paginationH}px - ${PAGINATION_LIFT_PX}px - env(safe-area-inset-bottom, 0px))`

    const tableCacheKey = useMemo(() => {
        const viewMode = mode
        const d1 = mode === "daily" ? dailyDate : startDate
        const d2 = mode === "daily" ? dailyDate : endDate
        // include debounced exact email (because backend drill-down depends on it)
        const email = isExactClientEmail ? clientInput : ""
        const rowType = report?.rowType ?? effectiveRowType
        return `${viewMode}|${d1}|${d2}|${email}|${rowType}`
    }, [mode, dailyDate, startDate, endDate, isExactClientEmail, clientInput, report?.rowType, effectiveRowType])

    return (
        <div className="h-[100dvh] overflow-hidden bg-background">
            <div className="h-full flex flex-col">
                <div ref={headerRef} className="sticky top-0 z-30 bg-background border-b shrink-0">
                    <div className="max-w-6xl mx-auto px-6 py-5 space-y-4">
                        <div className="flex justify-between items-center">
                            <div>
                                <div className="flex items-center gap-2">
                                    <h1 className="text-2xl font-semibold">Sales Reports</h1>
                                    {loading ? <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" /> : null}
                                </div>
                                <p className="text-sm text-muted-foreground">
                                    Track sales by client and product (invoiced orders only)
                                </p>
                            </div>
                        </div>

                        <div className="relative inline-flex bg-muted rounded-full p-1 w-full max-w-md">
                            <div
                                className={cn(
                                    "absolute top-0 left-0 h-full w-1/2 bg-indigo-600 rounded-full transition-transform",
                                    mode === "range" && "translate-x-full"
                                )}
                            />
                            <button
                                type="button"
                                className={cn(
                                    "relative z-10 flex-1 py-2 font-medium text-sm",
                                    mode === "daily" ? "text-white" : "text-muted-foreground"
                                )}
                                onClick={() => switchMode("daily")}
                            >
                                Day-to-day
                            </button>
                            <button
                                type="button"
                                className={cn(
                                    "relative z-10 flex-1 py-2 font-medium text-sm",
                                    mode === "range" ? "text-white" : "text-muted-foreground"
                                )}
                                onClick={() => switchMode("range")}
                            >
                                Sales report
                            </button>
                        </div>

                        <div className="flex flex-wrap items-center gap-2">
                            {mode === "daily" ? (
                                <div className="flex items-center gap-2">
                                    <span className="text-sm text-muted-foreground">Date</span>

                                    <Hint text="Select a day (IST). Auto-refreshes on change.">
                                        <Popover open={openDaily} onOpenChange={setOpenDaily}>
                                            <PopoverTrigger asChild>
                                                <Button
                                                    type="button"
                                                    variant="outline"
                                                    disabled={loading}
                                                    className="w-44 justify-start text-left font-normal"
                                                >
                                                    <CalendarIcon className="mr-2 h-4 w-4" />
                                                    {dailySelected ? format(dailySelected, "dd MMM yyyy") : "Select date"}
                                                </Button>
                                            </PopoverTrigger>

                                            <PopoverContent className="w-auto p-0" align="start">
                                                <Calendar
                                                    mode="single"
                                                    selected={dailySelected}
                                                    disabled={{ after: today }}
                                                    captionLayout="dropdown"
                                                    fromYear={2000}
                                                    toYear={today.getFullYear()}
                                                    onSelect={(d) => {
                                                        if (!d) return
                                                        const iso = dateToISO(d)
                                                        const t = todayISO()
                                                        if (iso > t) {
                                                            toast.error("Daily report date cannot be in the future")
                                                            return
                                                        }
                                                        setDailyDate(iso)
                                                        setOpenDaily(false)
                                                    }}
                                                    initialFocus
                                                />
                                            </PopoverContent>
                                        </Popover>
                                    </Hint>
                                </div>
                            ) : (
                                <div className="flex items-center gap-2">
                                    <span className="text-sm text-muted-foreground">From</span>

                                    <Hint text="Start date (IST).">
                                        <Popover open={openFrom} onOpenChange={setOpenFrom}>
                                            <PopoverTrigger asChild>
                                                <Button
                                                    type="button"
                                                    variant="outline"
                                                    disabled={loading}
                                                    className="w-44 justify-start text-left font-normal"
                                                >
                                                    <CalendarIcon className="mr-2 h-4 w-4" />
                                                    {fromSelected ? format(fromSelected, "dd MMM yyyy") : "Start date"}
                                                </Button>
                                            </PopoverTrigger>

                                            <PopoverContent className="w-auto p-0" align="start">
                                                <Calendar
                                                    mode="single"
                                                    selected={fromSelected}
                                                    disabled={{ after: today }}
                                                    captionLayout="dropdown"
                                                    fromYear={2000}
                                                    toYear={today.getFullYear()}
                                                    onSelect={(d) => {
                                                        if (!d) return
                                                        const iso = dateToISO(d)
                                                        const t = todayISO()
                                                        if (iso > t) {
                                                            toast.error("Start date cannot be in the future")
                                                            return
                                                        }
                                                        setStartDate(iso)
                                                        if (endDate < iso) setEndDate(iso)
                                                        setOpenFrom(false)
                                                    }}
                                                    initialFocus
                                                />
                                            </PopoverContent>
                                        </Popover>
                                    </Hint>

                                    <ArrowRight className="h-4 w-4 text-muted-foreground" />

                                    <span className="text-sm text-muted-foreground">To</span>

                                    <Hint text="End date (IST).">
                                        <Popover open={openTo} onOpenChange={setOpenTo}>
                                            <PopoverTrigger asChild>
                                                <Button
                                                    type="button"
                                                    variant="outline"
                                                    disabled={loading}
                                                    className="w-44 justify-start text-left font-normal"
                                                >
                                                    <CalendarIcon className="mr-2 h-4 w-4" />
                                                    {toSelected ? format(toSelected, "dd MMM yyyy") : "End date"}
                                                </Button>
                                            </PopoverTrigger>

                                            <PopoverContent className="w-auto p-0" align="start">
                                                <Calendar
                                                    mode="single"
                                                    selected={toSelected}
                                                    disabled={{ after: today }}
                                                    captionLayout="dropdown"
                                                    fromYear={2000}
                                                    toYear={today.getFullYear()}
                                                    onSelect={(d) => {
                                                        if (!d) return
                                                        const iso = dateToISO(d)
                                                        const t = todayISO()
                                                        if (iso > t) {
                                                            toast.error("End date cannot be in the future")
                                                            return
                                                        }
                                                        setEndDate(iso)
                                                        setOpenTo(false)
                                                    }}
                                                    initialFocus
                                                />
                                            </PopoverContent>
                                        </Popover>
                                    </Hint>
                                </div>
                            )}

                            {/* ✅ Not disabled while loading, AND debounce triggers API calls */}
                            <Input
                                className="w-72 transition focus-visible:ring-2 focus-visible:ring-indigo-500"
                                placeholder="Filter by Client Email"
                                value={clientEmail}
                                maxLength={CLIENT_EMAIL_MAX}
                                onChange={(e) => {
                                    const v = e.target.value.replace(/^\s+/, "")
                                    setClientEmail(v)
                                    setPage(0)

                                    // ✅ immediate validation (like ClientsPage)
                                    if (v.trim().length > CLIENT_EMAIL_MAX) {
                                        setSearchError(`clientEmail cannot exceed ${CLIENT_EMAIL_MAX} characters`)
                                    } else {
                                        setSearchError(null)
                                    }
                                }}
                            />

                            <Hint text="Reset filters">
                                <Button
                                    variant="outline"
                                    className="transition focus-visible:ring-2 focus-visible:ring-indigo-500"
                                    onClick={clearFilters}
                                    disabled={loading}
                                >
                                    Clear
                                </Button>
                            </Hint>
                        </div>

                        {searchError ? <p className="text-sm text-red-500">{searchError}</p> : null}
                    </div>
                </div>

                <div className="flex-1 min-h-0 relative">
                    <div
                        ref={scrollViewportRef}
                        style={{ height: scrollViewportHeight }}
                        className="w-full overflow-y-auto overscroll-none"
                    >
                        <div className="max-w-6xl mx-auto px-6 py-6">
                            <SalesReportTable
                                cacheKey={tableCacheKey}
                                rowType={report?.rowType ?? effectiveRowType}
                                rows={pagedRows}
                                loading={loading}
                                onExpandFetch={(report?.rowType ?? effectiveRowType) === "CLIENT" ? onExpandFetch : undefined}
                            />
                        </div>
                    </div>

                    <div
                        ref={paginationRef}
                        className="absolute left-0 right-0 z-20 bg-background border-t"
                        style={{ bottom: bottomOffset }}
                    >
                        <div className="max-w-6xl mx-auto px-6 py-3">
                            <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}

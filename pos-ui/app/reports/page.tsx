"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
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
} from "@/services/salesReportService"
import { Loader2, ArrowRight, Calendar as CalendarIcon } from "lucide-react"
import { Calendar } from "@/components/ui/calendar"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { format } from "date-fns"

type Mode = "daily" | "range"

const CLIENT_EMAIL_MAX = 120
const EMAIL_RE = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/

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

// tiny debounce hook (no extra deps)
function useDebouncedValue<T>(value: T, delayMs: number) {
    const [debounced, setDebounced] = useState(value)
    useEffect(() => {
        const t = setTimeout(() => setDebounced(value), delayMs)
        return () => clearTimeout(t)
    }, [value, delayMs])
    return debounced
}

export default function ReportsPage() {
    const [mode, setMode] = useState<Mode>("daily")

    // Dates
    const [dailyDate, setDailyDate] = useState<string>(() => todayISO())
    const [startDate, setStartDate] = useState<string>(() => todayISO())
    const [endDate, setEndDate] = useState<string>(() => todayISO())

    // Calendar popover open states (keep open until date selected)
    const [openDaily, setOpenDaily] = useState(false)
    const [openFrom, setOpenFrom] = useState(false)
    const [openTo, setOpenTo] = useState(false)

    // Single input supports:
    // - exact email drill-down if valid email
    // - partial contains search for client rows if not a valid email
    const [clientEmail, setClientEmail] = useState<string>("")

    const [loading, setLoading] = useState(false)
    const [report, setReport] = useState<SalesReportResponseData | null>(null)

    const debouncedClientInput = useDebouncedValue(clientEmail, 300)
    const clientInput = useMemo(() => debouncedClientInput.trim().toLowerCase(), [debouncedClientInput])

    const isExactClientEmail = useMemo(() => {
        return Boolean(clientInput) && EMAIL_RE.test(clientInput)
    }, [clientInput])

    const effectiveRowType: ReportRowType = useMemo(() => {
        return isExactClientEmail ? "PRODUCT" : "CLIENT"
    }, [isExactClientEmail])

    const today = useMemo(() => startOfTodayLocal(), [])

    const validateInputs = useCallback((): string | null => {
        if (clientInput && clientInput.length > CLIENT_EMAIL_MAX) {
            return `clientEmail cannot exceed ${CLIENT_EMAIL_MAX} characters`
        }

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
    }, [mode, dailyDate, startDate, endDate, clientInput])

    const fetchReport = useCallback(async () => {
        const err = validateInputs()
        if (err) {
            toast.error(err)
            return
        }

        // Only send to backend if exact email (drill-down).
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
        } finally {
            setLoading(false)
        }
    }, [mode, dailyDate, startDate, endDate, clientInput, isExactClientEmail, validateInputs])

    // Auto-load on open and auto-refresh on filter changes
    useEffect(() => {
        fetchReport()
    }, [fetchReport])

    const onExpandFetch = useCallback(
        async (email: string): Promise<SalesReportRowData[]> => {
            if (!email) return []
            const res =
                mode === "daily"
                    ? await getDailySalesReport({ startDate: dailyDate, clientEmail: email })
                    : await getRangeSalesReport({ startDate, endDate, clientEmail: email })
            return res.rows || []
        },
        [mode, dailyDate, startDate, endDate]
    )

    function switchMode(next: Mode) {
        if (next === mode) return
        setMode(next)
        setReport(null)
    }

    function clearFilters() {
        const t = todayISO()
        setClientEmail("")
        setDailyDate(t)
        setStartDate(t)
        setEndDate(t)
    }

    // Local contains filter ONLY in CLIENT view
    const filteredRows = useMemo(() => {
        if (!report?.rows) return []
        if (report.rowType !== "CLIENT") return report.rows
        if (!clientInput) return report.rows
        if (isExactClientEmail) return report.rows

        const q = clientInput
        return report.rows.filter((r) => (r.clientEmail || "").toLowerCase().includes(q))
    }, [report, clientInput, isExactClientEmail])

    const dailySelected = useMemo(() => isoToDate(dailyDate), [dailyDate])
    const fromSelected = useMemo(() => isoToDate(startDate), [startDate])
    const toSelected = useMemo(() => isoToDate(endDate), [endDate])

    return (
        <div className="space-y-6">
            <div className="sticky top-0 z-30 -mt-6 bg-background border-b">
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

                    {/* Toggle FIRST */}
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

                    {/* Filters ALWAYS below toggle */}
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
                                                    setOpenDaily(false) // ✅ close ONLY after choosing a date
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
                                                    // keep end >= start (nice UX)
                                                    if (endDate < iso) setEndDate(iso)
                                                    setOpenFrom(false) // ✅ close ONLY after choosing a date
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
                                                    setOpenTo(false) // ✅ close ONLY after choosing a date
                                                }}
                                                initialFocus
                                            />
                                        </PopoverContent>
                                    </Popover>
                                </Hint>
                            </div>
                        )}

                        <Input
                            className="w-72 transition focus-visible:ring-2 focus-visible:ring-indigo-500"
                            placeholder="Filter by Client Email"
                            value={clientEmail}
                            maxLength={CLIENT_EMAIL_MAX}
                            onChange={(e) => {
                                const v = e.target.value.replace(/^\s+/, "")
                                if (v.length > CLIENT_EMAIL_MAX) {
                                    toast.error(`clientEmail cannot exceed ${CLIENT_EMAIL_MAX} characters`)
                                    setClientEmail(v.slice(0, CLIENT_EMAIL_MAX))
                                    return
                                }
                                setClientEmail(v)
                            }}
                            disabled={loading}
                        />

                        {/* Clear button on SAME LEVEL as filters */}
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
                </div>
            </div>

            <div className="max-w-6xl mx-auto px-6 pt-6 space-y-6">
                <SalesReportTable
                    rowType={report?.rowType ?? effectiveRowType}
                    rows={filteredRows}
                    loading={loading}
                    onExpandFetch={report?.rowType === "CLIENT" ? onExpandFetch : undefined}
                />
            </div>
        </div>
    )
}

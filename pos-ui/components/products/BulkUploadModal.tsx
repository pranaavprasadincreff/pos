"use client"

import { useEffect, useMemo, useRef, useState } from "react"
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"
import { Hint } from "@/components/shared/Hint"
import { CircleHelp, Download, Loader2 } from "lucide-react"
import { bulkAddProducts, bulkUpdateInventory } from "@/services/productService"

type Mode = "product" | "inventory"
type UploadSummary = "idle" | "success" | "few_errors" | "all_errors"

function decodeBase64ToText(b64: string) {
    return decodeURIComponent(escape(atob(b64)))
}
function encodeTextToBase64(text: string) {
    return btoa(unescape(encodeURIComponent(text)))
}
function downloadTextFile(filename: string, contents: string) {
    const blob = new Blob([contents], { type: "text/tab-separated-values" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
}

const SAMPLE_PRODUCT_TSV =
    "barcode\tclientEmail\tname\tmrp\timageUrl\n" +
    "SKU-001\tclient@example.com\tSample Product\t1999\thttps://example.com/image.jpg\n" +
    "SKU-002\tclient@example.com\tNo Image Product\t499\t\n"

const SAMPLE_INVENTORY_TSV =
    "barcode\tinventory\n" +
    "SKU-001\t10\n" +
    "SKU-002\t0\n"

export default function BulkUploadModal({
                                            isOpen,
                                            onClose,
                                            onSuccess,
                                        }: {
    isOpen: boolean
    onClose: () => void
    onSuccess: () => void
}) {
    const [mode, setMode] = useState<Mode>("product")
    const [loading, setLoading] = useState(false)

    const [fileName, setFileName] = useState<string>("")
    const [inputBase64, setInputBase64] = useState<string | null>(null)

    const [outputBase64, setOutputBase64] = useState<string | null>(null)
    const [summary, setSummary] = useState<UploadSummary>("idle")
    const [summaryText, setSummaryText] = useState<string>("")

    const fileInputRef = useRef<HTMLInputElement>(null)

    function reset() {
        setFileName("")
        setInputBase64(null)
        setOutputBase64(null)
        setSummary("idle")
        setSummaryText("")
        setLoading(false)
        if (fileInputRef.current) fileInputRef.current.value = ""
    }

    useEffect(() => {
        if (!isOpen) reset()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isOpen])

    function handleClose() {
        reset()
        onClose()
    }

    function switchMode(newMode: Mode) {
        if (newMode === mode) return
        setMode(newMode)
        reset()
    }

    async function onPickFile(file: File) {
        setFileName(file.name)
        setOutputBase64(null)
        setSummary("idle")
        setSummaryText("")

        const text = await file.text()
        const trimmed = text.trim()
        if (!trimmed) {
            setInputBase64(null)
            setSummary("idle")
            setSummaryText("Selected file is empty.")
            return
        }

        setInputBase64(encodeTextToBase64(trimmed))
    }

    const requirementsText = useMemo(() => {
        if (mode === "product") {
            return "Required columns: barcode, clientEmail, name, mrp. Optional: imageUrl."
        }
        return "Required columns: barcode, inventory."
    }, [mode])

    async function upload() {
        if (!inputBase64 || loading) return

        setLoading(true)
        setSummary("idle")
        setSummaryText("")

        try {
            const res =
                mode === "product"
                    ? await bulkAddProducts(inputBase64)
                    : await bulkUpdateInventory(inputBase64)

            setOutputBase64(res.file)

            const outText = decodeBase64ToText(res.file)
            const lines = outText.trim().split("\n")
            const rows = lines.slice(1)
            const errorCount = rows.filter((r) =>
                r.toLowerCase().includes("error")
            ).length

            if (rows.length === 0) {
                setSummary("success")
                setSummaryText("Upload completed.")
            } else if (errorCount === 0) {
                setSummary("success")
                setSummaryText("Upload successful. No errors.")
            } else if (errorCount === rows.length) {
                setSummary("all_errors")
                setSummaryText(
                    "Upload completed, but all rows have errors. Download output to see details."
                )
            } else {
                setSummary("few_errors")
                setSummaryText(
                    `Upload completed with some errors (${errorCount}/${rows.length}). Download output to see details.`
                )
            }

            onSuccess()

            setInputBase64(null)
            setFileName("")
            if (fileInputRef.current) fileInputRef.current.value = ""
        } catch (e: unknown) {
            const msg = e instanceof Error ? e.message : "Upload failed"
            setSummary("all_errors")
            setSummaryText(msg)
        } finally {
            setLoading(false)
        }
    }

    function downloadOutput() {
        if (!outputBase64) return
        const tsv = decodeBase64ToText(outputBase64)
        downloadTextFile("bulk-output.tsv", tsv)
    }

    function downloadSample() {
        if (mode === "product")
            downloadTextFile("sample-products.tsv", SAMPLE_PRODUCT_TSV)
        else downloadTextFile("sample-inventory.tsv", SAMPLE_INVENTORY_TSV)
    }

    const summaryStyle =
        summary === "success"
            ? "border-emerald-200 bg-emerald-50/40 text-emerald-800"
            : summary === "few_errors"
                ? "border-amber-200 bg-amber-50/40 text-amber-800"
                : summary === "all_errors"
                    ? "border-red-200 bg-red-50/40 text-red-800"
                    : "border-border bg-muted/30 text-muted-foreground"

    return (
        <Dialog open={isOpen} onOpenChange={(open) => (!open ? handleClose() : undefined)}>
            <DialogContent
                className="max-w-2xl w-full"
                onInteractOutside={(e) => e.preventDefault()}
                onEscapeKeyDown={(e) => e.preventDefault()}
            >
                <DialogHeader>
                    <DialogTitle>Bulk Upload</DialogTitle>
                </DialogHeader>

                <div className="relative inline-flex bg-muted rounded-full p-1 mb-3 w-full max-w-md">
                    <div
                        className={cn(
                            "absolute top-0 left-0 h-full w-1/2 bg-indigo-600 rounded-full transition-transform",
                            mode === "inventory" && "translate-x-full"
                        )}
                    />
                    <button
                        type="button"
                        className={cn(
                            "relative z-10 flex-1 py-2 font-medium text-sm",
                            mode === "product" ? "text-white" : "text-muted-foreground"
                        )}
                        onClick={() => switchMode("product")}
                    >
                        Add Products
                    </button>
                    <button
                        type="button"
                        className={cn(
                            "relative z-10 flex-1 py-2 font-medium text-sm",
                            mode === "inventory" ? "text-white" : "text-muted-foreground"
                        )}
                        onClick={() => switchMode("inventory")}
                    >
                        Update Inventory
                    </button>
                </div>

                {/* File pick + helpers */}
                <div className="mt-3 flex items-start gap-3">
                    <div className="flex-1">
                        <Input
                            ref={fileInputRef}
                            type="file"
                            accept=".tsv"
                            onChange={(e) => {
                                const file = e.target.files?.[0]
                                if (!file) return
                                onPickFile(file)
                            }}
                        />

                        <div className="mt-1 text-xs text-muted-foreground">
                            <div className="flex items-center gap-1">
                                <Hint text={requirementsText}>
                  <span className="inline-flex items-center gap-1 cursor-default">
                    <CircleHelp className="h-3.5 w-3.5" />
                    Required columns
                  </span>
                                </Hint>

                                <span className="mx-1 text-muted-foreground/60">•</span>

                                <span>
                  Limit: up to{" "}
                                    <span className="font-medium text-muted-foreground">
                    5,000
                  </span>{" "}
                                    rows per file
                </span>
                            </div>

                            {/* extra spacing added here */}
                            <div className="mt-2">
                                <Hint text="Download a sample TSV to see the expected format">
                                    <button
                                        type="button"
                                        onClick={downloadSample}
                                        className="text-indigo-700 hover:text-indigo-800 underline underline-offset-4"
                                    >
                                        Download sample TSV
                                    </button>
                                </Hint>
                            </div>
                        </div>
                    </div>

                    <Hint text={inputBase64 ? "Upload selected file" : "Choose a TSV file first"}>
                        <Button
                            type="button"
                            onClick={upload}
                            disabled={!inputBase64 || loading}
                            className="bg-indigo-600 hover:bg-indigo-700"
                        >
                            {loading ? (
                                <span className="inline-flex items-center gap-2">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Uploading…
                </span>
                            ) : (
                                "Upload"
                            )}
                        </Button>
                    </Hint>
                </div>

                {fileName && (
                    <p className="text-xs text-muted-foreground mt-2">
                        Selected: <span className="font-medium">{fileName}</span>
                    </p>
                )}

                {outputBase64 && (
                    <div className="mt-3">
                        <Hint text="Download output file (includes error details if any)">
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                className="border-indigo-200 bg-indigo-50/40 text-indigo-700 hover:bg-indigo-50"
                                onClick={downloadOutput}
                            >
                                <Download className="h-4 w-4 mr-2" />
                                Output TSV
                            </Button>
                        </Hint>
                    </div>
                )}

                {(summary !== "idle" || summaryText) && (
                    <div className={cn("mt-4 rounded-lg border px-3 py-2 text-sm", summaryStyle)}>
                        {summaryText || "Upload finished."}
                    </div>
                )}

                <DialogFooter className="mt-4">
                    <Button type="button" variant="outline" onClick={handleClose}>
                        Exit
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}

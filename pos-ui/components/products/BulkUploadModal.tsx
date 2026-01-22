'use client'

import { useState, useRef, useEffect } from 'react'
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { toast } from 'sonner'
import TsvPreviewTable, { ParsedRow } from './TsvPreviewTable'
import { cn } from '@/lib/utils'

type Mode = 'product' | 'inventory'
type ViewMode = 'all' | 'error'

export default function BulkUploadModal({
                                            isOpen,
                                            onClose,
                                            onSuccess,
                                        }: {
    isOpen: boolean
    onClose: () => void
    onSuccess: () => void
}) {
    const [mode, setMode] = useState<Mode>('product')
    const [viewMode, setViewMode] = useState<ViewMode>('all')
    const [loading, setLoading] = useState(false)

    const [inputHeaders, setInputHeaders] = useState<string[]>([])
    const [inputRows, setInputRows] = useState<ParsedRow[]>([])

    const [previewHeaders, setPreviewHeaders] = useState<string[]>([])
    const [previewRows, setPreviewRows] = useState<ParsedRow[]>([])

    const [outputBase64, setOutputBase64] = useState<string | null>(null)

    const fileInputRef = useRef<HTMLInputElement>(null)

    function reset() {
        setInputHeaders([])
        setInputRows([])
        setPreviewHeaders([])
        setPreviewRows([])
        setOutputBase64(null)
        setViewMode('all')
        setLoading(false)
        if (fileInputRef.current) fileInputRef.current.value = ''
    }

    useEffect(() => {
        if (!isOpen) reset()
    }, [isOpen])

    function switchMode(newMode: Mode) {
        if (newMode === mode) return
        setMode(newMode)
        reset()
    }

    async function parseFile(file: File) {
        const text = await file.text()
        const lines = text.trim().split('\n').map(l => l.split('\t'))
        if (!lines.length) return

        const headers = lines[0]
        const rows: ParsedRow[] = lines.slice(1).map(values => ({
            values,
            isError:
                values.length !== headers.length ||
                values.some(v => v.trim() === ''),
        }))

        setInputHeaders(headers)
        setInputRows(rows)
        setPreviewHeaders(headers)
        setPreviewRows(rows)
    }

    async function upload() {
        if (!inputHeaders.length || !inputRows.length) return

        setLoading(true)
        const toastId = toast.loading('Uploading TSV...')

        try {
            const tsv = [inputHeaders, ...inputRows.map(r => r.values)]
                .map(r => r.join('\t'))
                .join('\n')

            const base64 = window.btoa(unescape(encodeURIComponent(tsv)))

            const endpoint =
                mode === 'product'
                    ? '/api/product/bulk-add-products'
                    : '/api/inventory/bulk-inventory-update'

            const res = await fetch(`http://localhost:8080${endpoint}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ file: base64 }),
            })

            if (!res.ok) {
                const text = await res.text()
                console.error('Bulk upload failed:', text)
                throw new Error('Upload failed')
            }

            const data = await res.json()
            setOutputBase64(data.file)

            const outText = decodeURIComponent(escape(atob(data.file)))
            const outLines = outText.trim().split('\n').map(l => l.split('\t'))

            setPreviewHeaders(outLines[0])
            setPreviewRows(
                outLines.slice(1).map(values => ({
                    values,
                    isError: values.some(v =>
                        v.toLowerCase().includes('error')
                    ),
                }))
            )

            // 🔑 CRITICAL: Refresh product list in parent
            onSuccess()

            // Force re-upload if user wants again
            setInputHeaders([])
            setInputRows([])
            if (fileInputRef.current) fileInputRef.current.value = ''

            toast.success('Upload completed')
        } catch {
            toast.error('Upload failed')
        } finally {
            setLoading(false)
            toast.dismiss(toastId)
        }
    }

    function downloadOutput() {
        if (!outputBase64) return
        const tsv = decodeURIComponent(escape(atob(outputBase64)))
        const blob = new Blob([tsv], { type: 'text/tab-separated-values' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = 'bulk-output.tsv'
        a.click()
        URL.revokeObjectURL(url)
    }

    const filteredRows =
        viewMode === 'all'
            ? previewRows
            : previewRows.filter(r => r.isError)

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="max-w-7xl w-full">
                <DialogHeader>
                    <DialogTitle>Bulk Upload</DialogTitle>
                </DialogHeader>

                {/* Mode Toggle */}
                <div className="relative inline-flex bg-muted rounded-full p-1 mb-4 max-w-md w-full">
                    <div
                        className={cn(
                            'absolute top-0 left-0 h-full w-1/2 bg-primary rounded-full transition-transform',
                            mode === 'inventory' && 'translate-x-full'
                        )}
                    />
                    <button
                        className={cn(
                            'relative z-10 flex-1 py-2 font-medium',
                            mode === 'product'
                                ? 'text-white'
                                : 'text-muted-foreground'
                        )}
                        onClick={() => switchMode('product')}
                    >
                        Add Products
                    </button>
                    <button
                        className={cn(
                            'relative z-10 flex-1 py-2 font-medium',
                            mode === 'inventory'
                                ? 'text-white'
                                : 'text-muted-foreground'
                        )}
                        onClick={() => switchMode('inventory')}
                    >
                        Update Inventory
                    </button>
                </div>

                {/* File Input */}
                <div className="flex gap-3 mb-4">
                    <Input
                        ref={fileInputRef}
                        type="file"
                        accept=".tsv"
                        onChange={e => {
                            const file = e.target.files?.[0]
                            if (!file) return
                            setPreviewHeaders([])
                            setPreviewRows([])
                            setOutputBase64(null)
                            setViewMode('all')
                            parseFile(file)
                        }}
                    />
                    <Button
                        onClick={upload}
                        disabled={!inputRows.length || loading}
                    >
                        {loading ? 'Uploading...' : 'Upload'}
                    </Button>
                </div>

                {previewRows.length > 0 && (
                    <>
                        <div className="flex justify-between mb-2">
                            <div className="flex gap-2">
                                <Button
                                    size="sm"
                                    variant={viewMode === 'all' ? 'default' : 'outline'}
                                    onClick={() => setViewMode('all')}
                                >
                                    View All
                                </Button>
                                <Button
                                    size="sm"
                                    variant={viewMode === 'error' ? 'default' : 'outline'}
                                    onClick={() => setViewMode('error')}
                                >
                                    View Errors
                                </Button>
                            </div>

                            {outputBase64 && (
                                <Button
                                    size="sm"
                                    variant="outline"
                                    onClick={downloadOutput}
                                >
                                    Download Output TSV
                                </Button>
                            )}
                        </div>

                        <div className="max-h-[700px] overflow-auto border rounded-md">
                            <TsvPreviewTable
                                headers={previewHeaders}
                                rows={filteredRows}
                            />
                        </div>
                    </>
                )}

                <DialogFooter>
                    <Button variant="outline" onClick={onClose}>
                        Exit
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}

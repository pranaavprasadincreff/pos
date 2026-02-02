"use client"

import { useCallback, useEffect, useMemo, useRef, useState } from "react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"

import ProductCardGrid from "@/components/products/ProductCardGrid"
import ProductModal from "@/components/products/ProductModal"
import BulkUploadModal from "@/components/products/BulkUploadModal"

import { filterProducts, getAllProducts } from "@/services/productService"
import { getClientByEmail } from "@/services/clientService"
import type { ProductData } from "@/services/types"

import { Hint } from "@/components/shared/Hint"
import { Loader2 } from "lucide-react"
import { toast } from "sonner"

const PAGE_SIZE = 9

// backend-ish limits (keep just lengths; no “invalid email format while typing”)
const EMAIL_MAX = 40
const NAME_MAX = 30
const BARCODE_MAX = 40

type ProductFilterKey = "barcode" | "name" | "clientEmail"
type ProductUI = ProductData & { clientName?: string }

const PRODUCT_FILTERS: Record<
    ProductFilterKey,
    { label: string; placeholder: string; tooltip: string }
> = {
    name: {
        label: "Name",
        placeholder: "Search product name…",
        tooltip: "Filter products by product name",
    },
    barcode: {
        label: "Barcode",
        placeholder: "Search barcode…",
        tooltip: "Filter products by barcode",
    },
    clientEmail: {
        label: "Client Email",
        placeholder: "Search client email…",
        tooltip: "Filter products by client email",
    },
}

// small concurrency to hydrate display names (does NOT affect filtering)
const CLIENT_LOOKUP_CONCURRENCY = 4

export default function ProductsPage() {
    const [products, setProducts] = useState<ProductUI[]>([])
    const [page, setPage] = useState(0) // next page to load
    const [hasMore, setHasMore] = useState(true)

    const [loadingMore, setLoadingMore] = useState(false)
    const [resetLoading, setResetLoading] = useState(true)

    const [searchTerm, setSearchTerm] = useState("")
    const [searchBy, setSearchBy] = useState<ProductFilterKey>("barcode")
    const [searchError, setSearchError] = useState<string | null>(null)

    const [modalOpen, setModalOpen] = useState(false)
    const [bulkOpen, setBulkOpen] = useState(false)
    const [editingProduct, setEditingProduct] = useState<ProductData | null>(null)

    const observerRef = useRef<HTMLDivElement | null>(null)

    // request guard: only latest request mutates state
    const requestIdRef = useRef(0)

    // client email -> client name cache for display only
    const [clientNameByEmail, setClientNameByEmail] = useState<Record<string, string>>({})
    const clientNameByEmailRef = useRef<Record<string, string>>({})
    useEffect(() => {
        clientNameByEmailRef.current = clientNameByEmail
    }, [clientNameByEmail])

    const inFlightClientEmailsRef = useRef<Set<string>>(new Set())

    const placeholder = useMemo(
        () => PRODUCT_FILTERS[searchBy]?.placeholder ?? "Search…",
        [searchBy]
    )

    function validateFilterInput(term: string, by: ProductFilterKey) {
        const t = term.trim()
        if (!t) return null

        if (by === "name") return t.length > NAME_MAX ? "Name filter too long" : null
        if (by === "barcode") return t.length > BARCODE_MAX ? "Barcode filter too long" : null
        return t.length > EMAIL_MAX ? "Client email filter too long" : null
    }

    const enrichProducts = useCallback((list: ProductData[]): ProductUI[] => {
        const map = clientNameByEmailRef.current
        return list.map((p) => ({ ...p, clientName: map[p.clientEmail] }))
    }, [])

    // hydrate missing client names (display-only)
    const fetchMissingClientNames = useCallback(async (emails: string[]) => {
        const unique = Array.from(new Set(emails))
        const missing = unique.filter(
            (e) => !clientNameByEmailRef.current[e] && !inFlightClientEmailsRef.current.has(e)
        )
        if (missing.length === 0) return

        missing.forEach((e) => inFlightClientEmailsRef.current.add(e))

        let idx = 0
        const workers = Array.from({ length: CLIENT_LOOKUP_CONCURRENCY }).map(async () => {
            while (idx < missing.length) {
                const email = missing[idx++]
                try {
                    const client = await getClientByEmail(email)
                    setClientNameByEmail((prev) =>
                        prev[email] === client.name ? prev : { ...prev, [email]: client.name }
                    )
                } catch {
                    // ignore
                } finally {
                    inFlightClientEmailsRef.current.delete(email)
                }
            }
        })

        await Promise.all(workers)
    }, [])

    const fetchPage = useCallback(
        async (pageToLoad: number, mode: "append" | "replace") => {
            const myRequestId = ++requestIdRef.current
            const isLatest = () => requestIdRef.current === myRequestId

            const trimmed = searchTerm.trim()
            const err = validateFilterInput(trimmed, searchBy)
            setSearchError(err)
            if (err) return

            // --- no search => get all ---
            if (trimmed.length === 0) {
                const res = await getAllProducts(pageToLoad, PAGE_SIZE)
                if (!isLatest()) return

                const enriched = enrichProducts(res.content)

                setProducts((prev) => {
                    if (mode === "replace") return enriched
                    const map = new Map(prev.map((p) => [p.id, p]))
                    enriched.forEach((p) => map.set(p.id, p))
                    return Array.from(map.values())
                })

                setHasMore(pageToLoad + 1 < res.totalPages)
                setPage(pageToLoad + 1)
                fetchMissingClientNames(res.content.map((p) => p.clientEmail))
                return
            }

            // --- filtered ---
            let res
            if (searchBy === "name") {
                res = await filterProducts({ page: pageToLoad, size: PAGE_SIZE, name: trimmed })
            } else if (searchBy === "barcode") {
                res = await filterProducts({ page: pageToLoad, size: PAGE_SIZE, barcode: trimmed })
            } else {
                // clientEmail filter => backend expects { client: "<email>" }
                res = await filterProducts({ page: pageToLoad, size: PAGE_SIZE, client: trimmed })
            }

            if (!isLatest()) return

            const enriched = enrichProducts(res.content)

            setProducts((prev) => {
                if (mode === "replace") return enriched
                const map = new Map(prev.map((p) => [p.id, p]))
                enriched.forEach((p) => map.set(p.id, p))
                return Array.from(map.values())
            })

            setHasMore(pageToLoad + 1 < res.totalPages)
            setPage(pageToLoad + 1)

            fetchMissingClientNames(res.content.map((p) => p.clientEmail))
        },
        [searchBy, searchTerm, enrichProducts, fetchMissingClientNames]
    )

    // initial load
    useEffect(() => {
        ;(async () => {
            setResetLoading(true)
            setProducts([])
            setHasMore(true)
            setPage(0)
            try {
                await fetchPage(0, "replace")
            } finally {
                setResetLoading(false)
            }
        })()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    // debounce reset on search change
    useEffect(() => {
        const timer = setTimeout(async () => {
            setResetLoading(true)
            setProducts([])
            setHasMore(true)
            setPage(0)
            try {
                await fetchPage(0, "replace")
            } finally {
                setResetLoading(false)
            }
        }, 300)

        return () => clearTimeout(timer)
    }, [searchBy, searchTerm, fetchPage])

    // infinite scroll
    const loadNextPage = useCallback(async () => {
        if (loadingMore || resetLoading || !hasMore) return
        setLoadingMore(true)
        try {
            await fetchPage(page, "append")
        } finally {
            setLoadingMore(false)
        }
    }, [page, hasMore, loadingMore, resetLoading, fetchPage])

    useEffect(() => {
        if (!observerRef.current) return
        const observer = new IntersectionObserver(
            (entries) => {
                if (entries[0].isIntersecting) loadNextPage()
            },
            { rootMargin: "150px" }
        )
        observer.observe(observerRef.current)
        return () => observer.disconnect()
    }, [loadNextPage])

    const hardReload = async () => {
        setResetLoading(true)
        setProducts([])
        setHasMore(true)
        setPage(0)
        try {
            await fetchPage(0, "replace")
            toast.success("Updated")
        } catch (e) {
            toast.error(e instanceof Error ? e.message : "Failed")
        } finally {
            setResetLoading(false)
        }
    }

    // inventory update: patch in place (prevents scroll jump)
    const handleInventoryUpdated = (updated: ProductData) => {
        setProducts((prev) =>
            prev.map((p) =>
                p.id === updated.id
                    ? { ...p, ...updated, clientName: clientNameByEmailRef.current[updated.clientEmail] ?? p.clientName }
                    : p
            )
        )
        fetchMissingClientNames([updated.clientEmail])
    }

    return (
        <div className="space-y-6">
            <div className="sticky top-0 z-30 bg-background border-b">
                <div className="max-w-7xl mx-auto px-6 py-4 space-y-4">
                    <div className="flex justify-between items-center">
                        <div>
                            <h1 className="text-2xl font-semibold">Products</h1>
                            <p className="text-sm text-muted-foreground">Manage products and inventory</p>
                        </div>

                        <div className="flex gap-2">
                            <Hint text="Add a new product">
                                <Button
                                    className="bg-indigo-600 hover:bg-indigo-700"
                                    onClick={() => {
                                        setEditingProduct(null)
                                        setModalOpen(true)
                                    }}
                                >
                                    + Add Product
                                </Button>
                            </Hint>

                            <Hint text="Upload TSV to add products / update inventory">
                                <Button
                                    variant="outline"
                                    className="border-indigo-200 bg-indigo-50/40 text-indigo-700 hover:bg-indigo-50"
                                    onClick={() => setBulkOpen(true)}
                                >
                                    Bulk Upload
                                </Button>
                            </Hint>
                        </div>
                    </div>

                    <div className="max-w-2xl space-y-1">
                        <div className="flex items-center gap-2 flex-nowrap">
              <span className="text-sm text-muted-foreground whitespace-nowrap shrink-0">
                Filter by:
              </span>

                            <Hint text={PRODUCT_FILTERS[searchBy]?.tooltip ?? "Choose filter"}>
                                <div>
                                    <Select
                                        value={searchBy}
                                        onValueChange={(v) => {
                                            setSearchBy(v as ProductFilterKey)
                                            setSearchError(null)
                                        }}
                                    >
                                        <SelectTrigger
                                            className="
                        w-40 transition
                        focus-visible:ring-2 focus-visible:ring-indigo-500
                        data-[state=open]:ring-2 data-[state=open]:ring-indigo-500
                      "
                                        >
                                            <SelectValue />
                                        </SelectTrigger>

                                        <SelectContent
                                            side="bottom"
                                            align="start"
                                            sideOffset={4}
                                            avoidCollisions={false}
                                            position="popper"
                                        >
                                            <SelectItem value="name">Name</SelectItem>
                                            <SelectItem value="barcode">Barcode</SelectItem>
                                            <SelectItem value="clientEmail">Client Email</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                            </Hint>

                            <Input
                                placeholder={placeholder}
                                value={searchTerm}
                                onChange={(e) => {
                                    const v = e.target.value
                                    setSearchTerm(v)
                                    setSearchError(validateFilterInput(v, searchBy))
                                }}
                                className="focus-visible:ring-2 focus-visible:ring-indigo-500 transition"
                            />

                            {searchTerm && (
                                <Hint text="Clear current search">
                                    <Button
                                        variant="outline"
                                        className="border-indigo-200 bg-indigo-50/40 text-indigo-700 hover:bg-indigo-50"
                                        onClick={() => {
                                            setSearchTerm("")
                                            setSearchError(null)
                                        }}
                                    >
                                        Clear
                                    </Button>
                                </Hint>
                            )}
                        </div>

                        {searchError && <p className="text-sm text-red-500">{searchError}</p>}
                    </div>
                </div>
            </div>

            <div className="max-w-7xl mx-auto px-6 pt-6 space-y-6">
                <ProductCardGrid
                    products={products}
                    loading={resetLoading}
                    onEdit={(product) => {
                        setEditingProduct(product)
                        setModalOpen(true)
                    }}
                    onInventoryUpdated={handleInventoryUpdated}
                />

                {hasMore && (
                    <div
                        ref={observerRef}
                        className="h-16 flex items-center justify-center text-sm text-muted-foreground"
                    >
                        {loadingMore ? (
                            <div className="flex items-center gap-2">
                                <Loader2 className="h-4 w-4 animate-spin" />
                                Loading more products…
                            </div>
                        ) : null}
                    </div>
                )}
            </div>

            <ProductModal
                isOpen={modalOpen}
                initialData={editingProduct}
                onClose={() => setModalOpen(false)}
                onSuccess={hardReload}
            />

            <BulkUploadModal
                isOpen={bulkOpen}
                onClose={() => setBulkOpen(false)}
                onSuccess={hardReload}
            />
        </div>
    )
}

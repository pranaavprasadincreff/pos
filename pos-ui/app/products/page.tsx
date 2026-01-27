'use client'

import {
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
} from 'react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select'

import ProductCardGrid from '@/components/products/ProductCardGrid'
import ProductModal from '@/components/products/ProductModal'
import BulkUploadModal from '@/components/products/BulkUploadModal'

import { getAllProducts } from '@/services/productService'
import { ProductData } from '@/services/types'

/* ---------------- constants ---------------- */

const PAGE_SIZE = 9

const SEARCH_BY_OPTIONS = ['name', 'barcode', 'clientEmail'] as const
type SearchBy = typeof SEARCH_BY_OPTIONS[number]

/* ---------------- page ---------------- */

export default function ProductsPage() {
    const [products, setProducts] = useState<ProductData[]>([])
    const [page, setPage] = useState(0)
    const [hasMore, setHasMore] = useState(true)
    const [loading, setLoading] = useState(false)

    const [searchTerm, setSearchTerm] = useState('')
    const [searchBy, setSearchBy] = useState<SearchBy>('name')

    const [modalOpen, setModalOpen] = useState(false)
    const [bulkOpen, setBulkOpen] = useState(false)

    const observerRef = useRef<HTMLDivElement | null>(null)
    const [editingProduct, setEditingProduct] = useState<ProductData | null>(null)


    const fetchPage = useCallback(
        async (pageToLoad: number) => {
            const res = await getAllProducts(pageToLoad, PAGE_SIZE)

            setProducts(prev => {
                const map = new Map(prev.map(p => [p.id, p]))
                res.content.forEach(p => map.set(p.id, p))
                return Array.from(map.values())
            })

            setHasMore(pageToLoad + 1 < res.totalPages)
            setPage(pageToLoad + 1)
        },
        []
    )

    const loadNextPage = useCallback(async () => {
        if (loading || !hasMore) return

        setLoading(true)
        await fetchPage(page)
        setLoading(false)
    }, [page, hasMore, loading, fetchPage])

    useEffect(() => {
        fetchPage(0)
    }, [])

    useEffect(() => {
        if (!observerRef.current) return

        const observer = new IntersectionObserver(
            (entries) => {
                if (entries[0].isIntersecting) {
                    loadNextPage()
                }
            },
            { rootMargin: '150px' }
        )

        observer.observe(observerRef.current)
        return () => observer.disconnect()
    }, [loadNextPage])

    const filteredProducts = useMemo(() => {
        if (!searchTerm) return products

        return products.filter(p =>
            p[searchBy]
                ?.toString()
                .toLowerCase()
                .includes(searchTerm.toLowerCase())
        )
    }, [products, searchTerm, searchBy])

    const hardReload = async () => {
        setProducts([])
        setHasMore(true)
        setPage(0)
        await fetchPage(0)
    }

    return (
        <div className="space-y-6">
            <div className="sticky top-0 z-30 bg-background border-b">
                <div className="max-w-7xl mx-auto px-6 py-4 space-y-4">
                    <div className="flex justify-between items-center">
                        <div>
                            <h1 className="text-2xl font-semibold">Products</h1>
                            <p className="text-sm text-muted-foreground">
                                Manage products and inventory
                            </p>
                        </div>

                        <div className="flex gap-2">
                            <Button
                                className="bg-indigo-600 hover:bg-indigo-700"
                                onClick={() => {
                                    setEditingProduct(null)
                                    setModalOpen(true)
                                }}
                            >
                                + Add Product
                            </Button>

                            <Button
                                className="bg-emerald-600 hover:bg-emerald-700"
                                onClick={() => setBulkOpen(true)}
                            >
                                Bulk Upload
                            </Button>
                        </div>
                    </div>

                    <div className="flex gap-2 max-w-2xl">
                        <Select
                            value={searchBy}
                            onValueChange={v => setSearchBy(v as SearchBy)}
                        >
                            <SelectTrigger className="w-40">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="name">Name</SelectItem>
                                <SelectItem value="barcode">Barcode</SelectItem>
                                <SelectItem value="clientEmail">
                                    Client Email
                                </SelectItem>
                            </SelectContent>
                        </Select>

                        <Input
                            placeholder={`Search by ${searchBy}`}
                            value={searchTerm}
                            onChange={e => setSearchTerm(e.target.value)}
                        />
                    </div>
                </div>
            </div>

            <div className="max-w-7xl mx-auto px-6 pt-6 space-y-6">
                <ProductCardGrid
                    products={filteredProducts}
                    loading={false}
                    onEdit={(product) => {
                        setEditingProduct(product)
                        setModalOpen(true)
                    }}
                    onInventoryUpdated={hardReload}
                />

                {hasMore && (
                    <div
                        ref={observerRef}
                        className="h-16 flex items-center justify-center text-sm text-muted-foreground"
                    >
                        {loading && 'Loading more products…'}
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

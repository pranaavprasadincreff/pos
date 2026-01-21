'use client'

import { useEffect, useMemo, useState } from 'react'
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
import Pagination from '@/components/clients/Pagination'
import { getAllProducts } from '@/services/productService'
import { ProductData } from '@/services/types'
import { toast } from 'sonner'

export default function ProductsPage() {
    const [page, setPage] = useState(0)
    const [allProducts, setAllProducts] = useState<ProductData[]>([])
    const [modalOpen, setModalOpen] = useState(false)
    const [editingProduct, setEditingProduct] = useState<ProductData | null>(null)

    const [searchTerm, setSearchTerm] = useState('')
    const [searchBy, setSearchBy] = useState<'name' | 'barcode' | 'clientEmail'>('name')
    const [loading, setLoading] = useState(false)

    const pageSize = 9 // 3-column grid

    useEffect(() => {
        fetchAllProducts()
    }, [])

    async function fetchAllProducts() {
        let toastId
        try {
            setLoading(true)
            toastId = toast.loading('Loading products...')

            const collected: ProductData[] = []
            let currentPage = 0
            let totalPages = 1

            while (currentPage < totalPages) {
                const res = await getAllProducts(currentPage, pageSize)
                collected.push(...res.content)
                totalPages = res.totalPages
                currentPage++
            }

            setAllProducts(collected)
        } catch (e) {
            toast.error('Failed to load products')
        } finally {
            setLoading(false)
            if (toastId) toast.dismiss(toastId)
        }
    }

    const filteredProducts = useMemo(() => {
        if (!searchTerm) return allProducts
        return allProducts.filter((p) =>
            p[searchBy]
                ?.toString()
                .toLowerCase()
                .includes(searchTerm.toLowerCase())
        )
    }, [allProducts, searchTerm, searchBy])

    const totalPages = Math.ceil(filteredProducts.length / pageSize)

    const paginatedProducts = useMemo(() => {
        const start = page * pageSize
        return filteredProducts.slice(start, start + pageSize)
    }, [filteredProducts, page])

    useEffect(() => {
        if (page >= totalPages && totalPages > 0) {
            setPage(totalPages - 1)
        }
    }, [page, totalPages])

    return (
        <div className="max-w-7xl mx-auto space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-semibold">Products</h1>
                    <p className="text-sm text-muted-foreground">
                        Manage products and inventory
                    </p>
                </div>
                <Button
                    onClick={() => setModalOpen(true)}
                    className="bg-indigo-600 hover:bg-indigo-700 text-white"
                >
                    + Add Product
                </Button>
            </div>

            {/* Filters */}
            <div className="flex gap-2 max-w-2xl">
                <Select
                    value={searchBy}
                    onValueChange={(v) => setSearchBy(v as any)}
                >
                    <SelectTrigger className="w-[160px]">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="name">Name</SelectItem>
                        <SelectItem value="barcode">Barcode</SelectItem>
                        <SelectItem value="clientEmail">Client Email</SelectItem>
                    </SelectContent>
                </Select>

                <Input
                    placeholder={`Search by ${searchBy}`}
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                />
            </div>

            {/* Cards */}
            <ProductCardGrid
                products={paginatedProducts}
                loading={loading}
                onEdit={(p) => {
                    setEditingProduct(p)
                    setModalOpen(true)
                }}
                onInventoryUpdated={fetchAllProducts}
            />

            {/* Pagination */}
            <Pagination
                page={page}
                totalPages={totalPages}
                onPageChange={setPage}
            />

            {/* Modal */}
            <ProductModal
                isOpen={modalOpen}
                initialData={editingProduct}
                onClose={() => {
                    setModalOpen(false)
                    setEditingProduct(null)
                }}
                onSuccess={fetchAllProducts}
            />
        </div>
    )
}

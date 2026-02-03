import api from "@/services/api"
import type { PageResponse, ProductData, ProductForm, ProductUpdateForm } from "./types"

function extractApiError(err: unknown): never {
    const anyErr: any = err
    const data = anyErr?.response?.data
    throw new Error(
        data?.message ||
        data?.error ||
        anyErr?.response?.statusText ||
        anyErr?.message ||
        "Request failed"
    )
}

/**
 * ✅ Normalize backend product payloads so `id` is always present for frontend usage.
 * Many backends return `productId` (not `id`) — this keeps UI + inventory update stable.
 */
function normalizeProduct(p: any): ProductData {
    // Prefer explicit `id`, otherwise fall back to `productId`
    const normalizedId = p?.id ?? p?.productId ?? ""

    return {
        ...p,
        id: normalizedId, // ensure `id` exists for inventory update
    } as ProductData
}

function normalizePage(resData: any): PageResponse<ProductData> {
    const content = Array.isArray(resData?.content) ? resData.content.map(normalizeProduct) : []
    return { ...resData, content } as PageResponse<ProductData>
}

export async function getAllProducts(
    page: number,
    size: number
): Promise<PageResponse<ProductData>> {
    try {
        const res = await api.post(`/product/get-all-paginated`, { page, size })
        return normalizePage(res.data)
    } catch (e) {
        extractApiError(e)
    }
}

export async function filterProducts(params: {
    page: number
    size: number
    barcode?: string
    name?: string
    client?: string
}): Promise<PageResponse<ProductData>> {
    try {
        const res = await api.post(`/product/filter`, params)
        return normalizePage(res.data)
    } catch (e) {
        extractApiError(e)
    }
}

export async function getProductByBarcode(barcode: string): Promise<ProductData> {
    try {
        const res = await api.get(`/product/get-by-barcode/${encodeURIComponent(barcode)}`)
        return normalizeProduct(res.data)
    } catch (e) {
        extractApiError(e)
    }
}

export async function createProduct(form: ProductForm): Promise<ProductData> {
    try {
        const res = await api.post(`/product/add`, form)
        return normalizeProduct(res.data)
    } catch (e) {
        extractApiError(e)
    }
}

export async function updateProduct(form: ProductUpdateForm): Promise<ProductData> {
    try {
        const res = await api.put(`/product/update`, form)
        return normalizeProduct(res.data)
    } catch (e) {
        extractApiError(e)
    }
}

export async function updateInventory(barcode: string, quantity: number): Promise<ProductData> {
    try {
        const res = await api.patch(`/inventory/update`, { barcode, quantity })
        return normalizeProduct(res.data)
    } catch (e) {
        extractApiError(e)
    }
}

export async function bulkAddProducts(fileBase64: string): Promise<{ file: string }> {
    try {
        const res = await api.post(`/product/bulk-add-products`, { file: fileBase64 })
        return res.data
    } catch (e) {
        extractApiError(e)
    }
}

export async function bulkUpdateInventory(fileBase64: string): Promise<{ file: string }> {
    try {
        const res = await api.post(`/inventory/bulk-inventory-update`, { file: fileBase64 })
        return res.data
    } catch (e) {
        extractApiError(e)
    }
}


import axios from "axios"
import type { PageResponse, ProductData, ProductForm, ProductUpdateForm } from "./types"

const API = "http://localhost:8080/api"

function extractApiError(err: unknown): never {
    if (axios.isAxiosError(err)) {
        const data: any = err.response?.data
        throw new Error(
            data?.message ||
            data?.error ||
            err.response?.statusText ||
            err.message ||
            "Request failed"
        )
    }
    throw err instanceof Error ? err : new Error("Request failed")
}

export async function getAllProducts(
    page: number,
    size: number
): Promise<PageResponse<ProductData>> {
    try {
        const res = await axios.post(`${API}/product/get-all-paginated`, { page, size })
        return res.data
    } catch (e) {
        extractApiError(e)
    }
}

export async function filterProducts(params: {
    page: number
    size: number
    barcode?: string
    name?: string
    client?: string // backend expects "client" = clientEmail
}): Promise<PageResponse<ProductData>> {
    try {
        const res = await axios.post(`${API}/product/filter`, params)
        return res.data
    } catch (e) {
        extractApiError(e)
    }
}

export async function getProductByBarcode(barcode: string): Promise<ProductData> {
    try {
        const res = await axios.get(`${API}/product/get-by-barcode/${encodeURIComponent(barcode)}`)
        return res.data
    } catch (e) {
        extractApiError(e)
    }
}

export async function createProduct(form: ProductForm): Promise<ProductData> {
    try {
        const res = await axios.post(`${API}/product/add`, form)
        return res.data
    } catch (e) {
        extractApiError(e)
    }
}

export async function updateProduct(form: ProductUpdateForm): Promise<ProductData> {
    try {
        const res = await axios.put(`${API}/product/update`, form)
        return res.data
    } catch (e) {
        extractApiError(e)
    }
}

export async function updateInventory(productId: string, quantity: number): Promise<ProductData> {
    try {
        const res = await axios.patch(`${API}/inventory/update`, { productId, quantity })
        return res.data
    } catch (e) {
        extractApiError(e)
    }
}

export async function bulkAddProducts(fileBase64: string): Promise<{ file: string }> {
    try {
        const res = await axios.post(`${API}/product/bulk-add-products`, { file: fileBase64 })
        return res.data
    } catch (e) {
        extractApiError(e)
    }
}

export async function bulkUpdateInventory(fileBase64: string): Promise<{ file: string }> {
    try {
        const res = await axios.post(`${API}/product/bulk-update-inventory`, { file: fileBase64 })
        return res.data
    } catch (e) {
        extractApiError(e)
    }
}

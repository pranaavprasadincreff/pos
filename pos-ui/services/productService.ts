import axios from "axios"
import type { PageResponse, ProductData, ProductForm, ProductUpdateForm } from "./types"

const API = "http://localhost:8080/api"

export async function getAllProducts(
    page: number,
    size: number
): Promise<PageResponse<ProductData>> {
    const res = await axios.post(`${API}/product/get-all-paginated`, { page, size })
    return res.data
}

export async function filterProducts(params: {
    page: number
    size: number
    barcode?: string
    name?: string
    client?: string // backend expects "client" = clientEmail
}): Promise<PageResponse<ProductData>> {
    const res = await axios.post(`${API}/product/filter`, params)
    return res.data
}

export async function getProductByBarcode(barcode: string): Promise<ProductData> {
    const res = await axios.get(`${API}/product/get-by-barcode/${encodeURIComponent(barcode)}`)
    return res.data
}

export async function createProduct(form: ProductForm): Promise<ProductData> {
    const res = await axios.post(`${API}/product/add`, form)
    return res.data
}

export async function updateProduct(form: ProductUpdateForm): Promise<ProductData> {
    const res = await axios.put(`${API}/product/update`, form)
    return res.data
}

export async function updateInventory(productId: string, quantity: number): Promise<ProductData> {
    const res = await axios.patch(`${API}/inventory/update`, { productId, quantity })
    return res.data
}

export async function bulkAddProducts(fileBase64: string): Promise<{ file: string }> {
    const res = await axios.post(`${API}/product/bulk-add-products`, { file: fileBase64 })
    return res.data
}

export async function bulkUpdateInventory(fileBase64: string): Promise<{ file: string }> {
    const res = await axios.post(`${API}/product/bulk-update-inventory`, { file: fileBase64 })
    return res.data
}

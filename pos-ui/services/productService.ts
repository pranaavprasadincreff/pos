import axios from 'axios'
import {
    PageResponse,
    ProductData,
    ProductForm,
    ProductUpdateForm,
} from './types'

const API = 'http://localhost:8080/api'

export async function getAllProducts(
    page: number,
    size: number
): Promise<PageResponse<ProductData>> {
    const res = await axios.post(
        `${API}/product/get-all-paginated`,
        { page, size }
    )
    return res.data
}

export async function createProduct(
    form: ProductForm
): Promise<ProductData> {
    const res = await axios.post(
        `${API}/product/add`,
        form
    )
    return res.data
}

export async function updateProduct(
    form: ProductUpdateForm
): Promise<ProductData> {
    const res = await axios.put(
        `${API}/product/update`,
        form
    )
    return res.data
}

export async function updateInventory(
    productId: string,
    quantity: number
): Promise<ProductData> {
    const res = await axios.patch(
        `${API}/inventory/update`,
        { productId, quantity }
    )
    return res.data
}

export async function getProductByBarcode(barcode: string): Promise<ProductData> {
    const res = await axios.get<ProductData>(`${API}/product/get-by-barcode/${barcode}`)
    return res.data
}

export async function searchProducts(query: string, size = 10): Promise<ProductData[]> {
    if (!query.trim()) return []
    const res = await axios.post(`${API}/product/search`, { query, size })
    return res.data
}
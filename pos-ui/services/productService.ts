import axios from 'axios'
import { PageResponse, ProductData } from './types'

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


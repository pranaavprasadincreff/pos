import axios from 'axios'
import { OrderCreateForm, OrderData, PageResponse } from './types'

const API = 'http://localhost:8080/api'

export interface PageForm {
    page: number
    size: number
}

export async function getAllOrders(page: number, size: number): Promise<PageResponse<OrderData>> {
    const form: PageForm = { page, size }
    const res = await axios.post(`${API}/order/get-all-paginated`, form)
    return {
        content: res.data.content,
        totalPages: res.data.totalPages,
        totalElements: res.data.totalElements,
        number: res.data.number,
        size: res.data.size,
    }
}

export async function create(form: OrderCreateForm): Promise<OrderData> {
    const res = await axios.post(`${API}/order/create`, form)
    return res.data
}

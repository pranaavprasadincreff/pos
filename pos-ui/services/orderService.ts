import axios from 'axios'
import { OrderCreateForm, OrderData, PageResponse } from './types'

const API = 'http://localhost:8080/api'

export interface PageForm {
    page: number
    size: number
}

export async function getAllOrders(
    page: number,
    size: number
): Promise<PageResponse<OrderData>> {
    const res = await axios.post(`${API}/order/get-all-paginated`, {
        page,
        size,
    })
    return res.data
}

export async function getOrder(
    orderReferenceId: string
): Promise<OrderData> {
    const res = await axios.get(
        `${API}/order/get/${orderReferenceId}`
    )
    return res.data
}

export async function create(
    form: OrderCreateForm
): Promise<OrderData> {
    const res = await axios.post(`${API}/order/create`, form)
    return res.data
}

export async function edit(
    orderReferenceId: string,
    form: OrderCreateForm
): Promise<OrderData> {
    const res = await axios.put(
        `${API}/order/edit/${orderReferenceId}`,
        form
    )
    return res.data
}

export async function cancel(
    orderReferenceId: string
): Promise<OrderData> {
    const res = await axios.put(
        `${API}/order/cancel/${orderReferenceId}`
    )
    return res.data
}

export async function retryOrder(order: OrderData): Promise<OrderData> {
    // Map items into create API format
    const form: OrderCreateForm = {
        items: order.items.map(i => ({
            productBarcode: i.productBarcode,
            quantity: i.quantity,
            sellingPrice: i.sellingPrice,
        })),
    }

    const res = await edit(order.orderReferenceId, form)
    return res
}


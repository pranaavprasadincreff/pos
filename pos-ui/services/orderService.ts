import api from "@/services/api"
import { OrderCreateForm, OrderData, PageResponse } from "./types"

export type OrderFilterTimeframe = "LAST_DAY" | "LAST_WEEK" | "LAST_MONTH"

export interface OrderFilterForm {
    orderReferenceId?: string
    status?: string
    timeframe: OrderFilterTimeframe
    page: number
    size: number
}

function extractApiError(err: unknown): never {
    const anyErr: any = err
    const status = anyErr?.response?.status
    const data = anyErr?.response?.data

    const dataMsg =
        typeof data === "string"
            ? data
            : data?.message || data?.error || (data ? JSON.stringify(data) : "")

    const msg =
        dataMsg ||
        anyErr?.response?.statusText ||
        anyErr?.message ||
        "Request failed"

    throw new Error(status ? `${msg} (HTTP ${status})` : msg)
}


export async function getAllOrders(page: number, size: number): Promise<PageResponse<OrderData>> {
    try {
        const res = await api.post(`/order/get-all-paginated`, { page, size })
        return res.data
    } catch (e) {
        extractApiError(e)
    }
}

export async function filterOrders(form: OrderFilterForm): Promise<PageResponse<OrderData>> {
    try {
        const res = await api.post(`/order/filter`, form)
        return res.data
    } catch (e) {
        extractApiError(e)
    }
}

export async function getOrder(orderReferenceId: string): Promise<OrderData> {
    try {
        const res = await api.get(`/order/get/${orderReferenceId}`)
        return res.data
    } catch (e) {
        extractApiError(e)
    }
}

export async function create(form: OrderCreateForm): Promise<OrderData> {
    try {
        const res = await api.post(`/order/create`, form)
        return res.data
    } catch (e) {
        extractApiError(e)
    }
}

export async function edit(orderReferenceId: string, form: OrderCreateForm): Promise<OrderData> {
    try {
        const res = await api.put(`/order/edit/${orderReferenceId}`, form)
        return res.data
    } catch (e) {
        extractApiError(e)
    }
}

export async function cancel(orderReferenceId: string): Promise<OrderData> {
    try {
        const res = await api.put(`/order/cancel/${orderReferenceId}`)
        return res.data
    } catch (e) {
        extractApiError(e)
    }
}

export async function retryOrder(order: OrderData): Promise<OrderData> {
    const form: OrderCreateForm = {
        items: order.items.map((i) => ({
            productBarcode: i.productBarcode,
            quantity: i.quantity,
            sellingPrice: i.sellingPrice,
        })),
    }
    return await edit(order.orderReferenceId, form)
}

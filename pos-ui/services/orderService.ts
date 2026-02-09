import api from "@/services/api"
import type {
  OrderCreateForm,
  OrderData,
  PageResponse,
  OrderUpdateForm,
} from "./types"

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
    dataMsg || anyErr?.response?.statusText || anyErr?.message || "Request failed"

  throw new Error(status ? `${msg} (HTTP ${status})` : msg)
}

export async function createOrder(form: OrderCreateForm): Promise<OrderData> {
  try {
    const res = await api.post(`/order`, form)
    return res.data
  } catch (e) {
    extractApiError(e)
  }
}

export async function updateOrder(form: OrderUpdateForm): Promise<OrderData> {
  try {
    const res = await api.put(`/order`, form)
    return res.data
  } catch (e) {
    extractApiError(e)
  }
}

export async function cancelOrder(orderReferenceId: string): Promise<OrderData> {
  try {
    const res = await api.put(`/order/${encodeURIComponent(orderReferenceId)}`)
    return res.data
  } catch (e) {
    extractApiError(e)
  }
}

export async function getOrder(orderReferenceId: string): Promise<OrderData> {
  try {
    const res = await api.get(`/order/${encodeURIComponent(orderReferenceId)}`)
    return res.data
  } catch (e) {
    extractApiError(e)
  }
}

export async function searchOrders(form: OrderFilterForm): Promise<PageResponse<OrderData>> {
  try {
    const res = await api.post(`/order/search`, form)
    return res.data
  } catch (e) {
    extractApiError(e)
  }
}

export async function getAllOrders(page: number, size: number): Promise<PageResponse<OrderData>> {
  return await searchOrders({
    page,
    size,
    timeframe: "LAST_MONTH",
  })
}

export async function retryOrder(order: OrderData): Promise<OrderData> {
  const form: OrderUpdateForm = {
    orderReferenceId: order.orderReferenceId,
    items: order.items.map((i) => ({
      productBarcode: i.productBarcode,
      quantity: i.quantity,
      sellingPrice: i.sellingPrice,
    })),
  }
  return await updateOrder(form)
}

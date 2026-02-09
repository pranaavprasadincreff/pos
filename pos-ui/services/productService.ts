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

function normalizeProduct(p: any): ProductData {
  const normalizedId = p?.id ?? p?.productId ?? ""
  return { ...p, id: normalizedId } as ProductData
}

function normalizePage(resData: any): PageResponse<ProductData> {
  const content = Array.isArray(resData?.content)
    ? resData.content.map(normalizeProduct)
    : []
  return { ...resData, content } as PageResponse<ProductData>
}

export async function searchProducts(params: {
  page: number
  size: number
  barcode?: string
  name?: string
  client?: string
}): Promise<PageResponse<ProductData>> {
  try {
    const res = await api.post(`/product/search`, params)
    return normalizePage(res.data)
  } catch (e) {
    extractApiError(e)
  }
}

export async function getProductByBarcode(barcode: string): Promise<ProductData> {
  try {
    const res = await api.get(`/product/${encodeURIComponent(barcode)}`)
    return normalizeProduct(res.data)
  } catch (e) {
    extractApiError(e)
  }
}

export async function createProduct(form: ProductForm): Promise<ProductData> {
  try {
    const res = await api.post(`/product`, form)
    return normalizeProduct(res.data)
  } catch (e) {
    extractApiError(e)
  }
}

export async function updateProduct(form: ProductUpdateForm): Promise<ProductData> {
  try {
    const res = await api.put(`/product`, form)
    return normalizeProduct(res.data)
  } catch (e) {
    extractApiError(e)
  }
}

export async function updateInventory(barcode: string, quantity: number): Promise<ProductData> {
  try {
    const res = await api.put(`/inventory`, { barcode, quantity })
    return normalizeProduct(res.data)
  } catch (e) {
    extractApiError(e)
  }
}

export async function bulkAddProducts(file: string): Promise<{ file: string }> {
  try {
    const res = await api.post(`/product/bulk-add-products`, { file })
    return res.data
  } catch (e) {
    extractApiError(e)
  }
}

export async function bulkUpdateInventory(file: string): Promise<{ file: string }> {
  try {
    const res = await api.post(`/inventory/bulk-inventory-update`, { file })
    return res.data
  } catch (e) {
    extractApiError(e)
  }
}

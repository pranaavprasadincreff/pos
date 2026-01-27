export interface Client {
    id: string
    name: string
    email: string
}

export interface ClientForm {
    name: string
    email: string
}

export interface ClientUpdateForm {
    name: string
    oldEmail: string
    newEmail: string
}
export interface ProductData {
    id: string
    barcode: string
    clientEmail: string
    name: string
    mrp: number
    imageUrl: string
    inventory: number
}

export interface ProductForm {
    barcode: string
    clientEmail: string
    name: string
    mrp: number
    imageUrl?: string
}

export interface ProductUpdateForm {
    oldBarcode: string
    newBarcode: string
    clientEmail: string
    name: string
    mrp: number
    imageUrl?: string
}

export interface PageResponse<T> {
    content: T[]
    totalPages: number
    totalElements: number
    number: number // current page
    size: number
}

export interface OrderItemData {
    productBarcode: string
    quantity: number
    sellingPrice: number
}

export interface OrderData {
    orderReferenceId: string
    orderTime: string
    status: 'CREATED' | 'INVOICED'
    items: OrderItemData[]
}

export interface OrderCreateItemForm {
    productBarcode: string
    quantity: number
    sellingPrice: number
}

export interface OrderCreateForm {
    items: OrderCreateItemForm[]
}

export interface InvoiceData {
    orderReferenceId: string
    pdfBase64: string
    pdfPath?: string
}

export interface ApiError {
    message: string
}

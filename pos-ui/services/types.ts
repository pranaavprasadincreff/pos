export interface User {
    id: string
    name: string
    email: string
}

export interface UserForm {
    name: string
    email: string
}

export interface UserUpdateForm {
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
    barcode: string
    clientEmail: string
    name: string
    mrp: number
    imageUrl?: string
}
export interface InventoryUpdateForm {
    barcode: string
    quantity: number
}
export interface BulkUploadForm {
    tsv: string
}

export interface BulkUploadError {
    row: number
    message: string
}

export interface BulkUploadData {
    successCount: number
    errorCount: number
    errors: BulkUploadError[]
}

export interface PageResponse<T> {
    content: T[]
    totalPages: number
    totalElements?: number
}

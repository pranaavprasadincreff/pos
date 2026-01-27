'use client'

import { useEffect, useRef, useState } from 'react'
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { cn } from '@/lib/utils'
import { createProduct, updateProduct } from '@/services/productService'
import { ProductData } from '@/services/types'
import type { AxiosError } from 'axios'

/* ---------------- types ---------------- */

interface Props {
    isOpen: boolean
    onClose: () => void
    onSuccess: () => void
    initialData: ProductData | null
}

interface ApiErrorResponse {
    message?: string
}

type FieldErrors = Partial<{
    barcode: string
    clientEmail: string
    name: string
    mrp: string
    imageUrl: string
}>

/* ---------------- component ---------------- */

export default function ProductModal({
                                         isOpen,
                                         onClose,
                                         onSuccess,
                                         initialData,
                                     }: Props) {
    const isEdit = Boolean(initialData)

    const [barcode, setBarcode] = useState('')
    const [clientEmail, setClientEmail] = useState('')
    const [name, setName] = useState('')
    const [mrp, setMrp] = useState('')
    const [imageUrl, setImageUrl] = useState('')
    const [submitting, setSubmitting] = useState(false)

    const [errors, setErrors] = useState<FieldErrors>({})
    const barcodeRef = useRef<HTMLInputElement>(null)

    /* ---------------- prefill / reset ---------------- */

    useEffect(() => {
        if (initialData) {
            setBarcode(initialData.barcode)
            setClientEmail(initialData.clientEmail)
            setName(initialData.name)
            setMrp(String(initialData.mrp))
            setImageUrl(initialData.imageUrl ?? '')
        } else {
            setBarcode('')
            setClientEmail('')
            setName('')
            setMrp('')
            setImageUrl('')
        }
        setErrors({})
    }, [initialData, isOpen])

    /* ---------------- submit ---------------- */

    async function handleSubmit() {
        setSubmitting(true)
        setErrors({})

        try {
            if (isEdit && initialData) {
                await updateProduct({
                    oldBarcode: initialData.barcode,
                    newBarcode: barcode,
                    clientEmail,
                    name,
                    mrp: Number(mrp),
                    imageUrl,
                })
            } else {
                await createProduct({
                    barcode,
                    clientEmail,
                    name,
                    mrp: Number(mrp),
                    imageUrl,
                })
            }

            onSuccess()
            onClose()
        } catch (err: unknown) {
            let message = 'Invalid input'

            if (isAxiosError(err)) {
                message =
                    err.response?.data?.message ??
                    err.message ??
                    message
            } else if (err instanceof Error) {
                message = err.message
            }

            const lower = message.toLowerCase()

            if (lower.includes('barcode')) {
                setErrors({ barcode: message })
                barcodeRef.current?.focus()
            } else if (lower.includes('email')) {
                setErrors({ clientEmail: message })
            } else if (lower.includes('mrp')) {
                setErrors({ mrp: message })
            } else if (lower.includes('name')) {
                setErrors({ name: message })
            } else {
                setErrors({ barcode: message })
            }
        } finally {
            setSubmitting(false)
        }
    }

    /* ---------------- render ---------------- */

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="animate-in fade-in zoom-in-95 duration-200">
                <DialogHeader>
                    <DialogTitle>
                        {isEdit ? 'Edit Product' : 'Add Product'}
                    </DialogTitle>
                </DialogHeader>

                <div className="space-y-4">
                    {/* Barcode */}
                    <div className="space-y-1">
                        <Label>Barcode</Label>
                        <Input
                            ref={barcodeRef}
                            value={barcode}
                            onChange={(e) => {
                                setBarcode(e.target.value)
                                setErrors(p => ({ ...p, barcode: undefined }))
                            }}
                            placeholder="SKU-001"
                            className={cn(
                                'focus-visible:ring-1 focus-visible:ring-indigo-500',
                                errors.barcode &&
                                'border-red-400 ring-2 ring-red-400/30'
                            )}
                        />
                        {errors.barcode && (
                            <p className="text-sm text-red-500">
                                {errors.barcode}
                            </p>
                        )}
                    </div>

                    {/* Client Email */}
                    <div className="space-y-1">
                        <Label>Client Email</Label>
                        <Input
                            value={clientEmail}
                            onChange={(e) => {
                                setClientEmail(e.target.value)
                                setErrors(p => ({
                                    ...p,
                                    clientEmail: undefined,
                                }))
                            }}
                            placeholder="client@email.com"
                            className={cn(
                                'focus-visible:ring-1 focus-visible:ring-indigo-500',
                                errors.clientEmail &&
                                'border-red-400 ring-2 ring-red-400/30'
                            )}
                        />
                        {errors.clientEmail && (
                            <p className="text-sm text-red-500">
                                {errors.clientEmail}
                            </p>
                        )}
                    </div>

                    {/* Name */}
                    <div className="space-y-1">
                        <Label>Name</Label>
                        <Input
                            value={name}
                            onChange={(e) => {
                                setName(e.target.value)
                                setErrors(p => ({ ...p, name: undefined }))
                            }}
                            placeholder="Product name"
                            className={cn(
                                'focus-visible:ring-1 focus-visible:ring-indigo-500',
                                errors.name &&
                                'border-red-400 ring-2 ring-red-400/30'
                            )}
                        />
                        {errors.name && (
                            <p className="text-sm text-red-500">
                                {errors.name}
                            </p>
                        )}
                    </div>

                    {/* MRP */}
                    <div className="space-y-1">
                        <Label>MRP</Label>
                        <Input
                            type="number"
                            value={mrp}
                            onChange={(e) => {
                                setMrp(e.target.value)
                                setErrors(p => ({ ...p, mrp: undefined }))
                            }}
                            placeholder="79999"
                            className={cn(
                                'focus-visible:ring-1 focus-visible:ring-indigo-500',
                                errors.mrp &&
                                'border-red-400 ring-2 ring-red-400/30'
                            )}
                        />
                        {errors.mrp && (
                            <p className="text-sm text-red-500">
                                {errors.mrp}
                            </p>
                        )}
                    </div>

                    {/* Image URL */}
                    <div className="space-y-1">
                        <Label>Image URL</Label>
                        <Input
                            value={imageUrl}
                            onChange={(e) => {
                                setImageUrl(e.target.value)
                                setErrors(p => ({
                                    ...p,
                                    imageUrl: undefined,
                                }))
                            }}
                            placeholder="https://image.com/product.jpg"
                            className="focus-visible:ring-1 focus-visible:ring-indigo-500"
                        />
                    </div>
                </div>

                <DialogFooter>
                    <Button
                        variant="outline"
                        onClick={onClose}
                        disabled={submitting}
                    >
                        Cancel
                    </Button>

                    <Button
                        onClick={handleSubmit}
                        disabled={
                            submitting ||
                            !barcode ||
                            !clientEmail ||
                            !name ||
                            !mrp
                        }
                        className="bg-indigo-600 hover:bg-indigo-700 text-white"
                    >
                        {submitting ? 'Saving…' : 'Save'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}

function isAxiosError(
    error: unknown
): error is AxiosError<ApiErrorResponse> {
    return (
        typeof error === 'object' &&
        error !== null &&
        'isAxiosError' in error
    )
}

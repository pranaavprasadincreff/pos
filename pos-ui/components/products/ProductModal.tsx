"use client"

import { useEffect, useMemo, useRef, useState } from "react"
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { cn } from "@/lib/utils"
import { createProduct, updateProduct } from "@/services/productService"
import type { ProductData } from "@/services/types"
import { toast } from "sonner"
import axios from "axios"

const EMAIL_MAX = 40
const NAME_MAX = 30
const BARCODE_MAX = 40
const IMAGE_URL_MAX = 500
const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/

interface Props {
    isOpen: boolean
    onClose: () => void
    onSuccess: () => void
    initialData: ProductData | null
}

type ApiErrorResponse = { message?: string }

function vRequired(v: string, msg: string) {
    return v.trim() ? null : msg
}
function vMax(v: string, max: number, msg: string) {
    return v.trim().length > max ? msg : null
}
function vEmail(v: string) {
    const t = v.trim()
    if (!t) return "Client is required"
    if (t.length > EMAIL_MAX) return "Email too long"
    if (!EMAIL_REGEX.test(t)) return "Invalid email format"
    return null
}
function vMrp(v: string) {
    const t = v.trim()
    if (!t) return "Invalid MRP"
    const n = Number(t)
    if (!Number.isFinite(n) || n <= 0) return "Invalid MRP"
    return null
}
function vImageUrlOptional(v: string) {
    const t = v.trim()
    if (!t) return null
    if (t.length > IMAGE_URL_MAX) return "Image URL too long"
    return null
}

type FieldErrors = Partial<{
    barcode: string
    clientEmail: string
    name: string
    mrp: string
    imageUrl: string
}>

function extractApiMessage(err: unknown): string {
    if (axios.isAxiosError<ApiErrorResponse>(err)) {
        return err.response?.data?.message || err.message || "Request failed"
    }
    if (err instanceof Error) return err.message
    return "Request failed"
}

export default function ProductModal({ isOpen, onClose, onSuccess, initialData }: Props) {
    const isEditMode = Boolean(initialData)

    const [barcode, setBarcode] = useState("")
    const [clientEmail, setClientEmail] = useState("")
    const [name, setName] = useState("")
    const [mrp, setMrp] = useState("")
    const [imageUrl, setImageUrl] = useState("")
    const [submitting, setSubmitting] = useState(false)

    const [touched, setTouched] = useState<Record<string, boolean>>({})
    const [submitAttempted, setSubmitAttempted] = useState(false)

    const [serverErrors, setServerErrors] = useState<FieldErrors>({})

    const barcodeRef = useRef<HTMLInputElement | null>(null)

    function resetLocalState() {
        setBarcode("")
        setClientEmail("")
        setName("")
        setMrp("")
        setImageUrl("")
        setSubmitting(false)
        setTouched({})
        setSubmitAttempted(false)
        setServerErrors({})
    }

    useEffect(() => {
        if (!isOpen) return

        if (initialData) {
            setBarcode(initialData.barcode ?? "")
            setClientEmail(initialData.clientEmail ?? "")
            setName(initialData.name ?? "")
            setMrp(String(initialData.mrp ?? ""))
            setImageUrl(initialData.imageUrl ?? "")
        } else {
            setBarcode("")
            setClientEmail("")
            setName("")
            setMrp("")
            setImageUrl("")
        }

        setTouched({})
        setSubmitAttempted(false)
        setServerErrors({})
        setSubmitting(false)
    }, [initialData, isOpen])

    const errors: FieldErrors = useMemo(() => {
        const show = (key: string) => submitAttempted || Boolean(touched[key])
        const e: FieldErrors = { ...serverErrors }

        if (!e.barcode && show("barcode")) {
            e.barcode =
                vRequired(barcode, "Barcode cannot be empty") ||
                vMax(barcode, BARCODE_MAX, "Barcode too long") ||
                undefined
        }

        if (!e.clientEmail && show("clientEmail")) {
            e.clientEmail = vEmail(clientEmail) || undefined
        }

        if (!e.name && show("name")) {
            e.name =
                vRequired(name, "Product name cannot be empty") ||
                vMax(name, NAME_MAX, "Name too long") ||
                undefined
        }

        if (!e.mrp && show("mrp")) {
            e.mrp = vMrp(mrp) || undefined
        }

        if (!e.imageUrl && show("imageUrl")) {
            e.imageUrl = vImageUrlOptional(imageUrl) || undefined
        }

        return e
    }, [barcode, clientEmail, name, mrp, imageUrl, touched, submitAttempted, serverErrors])

    const isFormValid = useMemo(() => {
        return (
            !vRequired(barcode, "Barcode") &&
            !vMax(barcode, BARCODE_MAX, "Barcode") &&
            !vEmail(clientEmail) &&
            !vRequired(name, "Name") &&
            !vMax(name, NAME_MAX, "Name") &&
            !vMrp(mrp) &&
            !vImageUrlOptional(imageUrl)
        )
    }, [barcode, clientEmail, name, mrp, imageUrl])

    async function handleSubmit() {
        setSubmitAttempted(true)
        setServerErrors({})

        const guard: FieldErrors = {
            barcode:
                vRequired(barcode, "Barcode cannot be empty") ||
                vMax(barcode, BARCODE_MAX, "Barcode too long") ||
                undefined,
            clientEmail: vEmail(clientEmail) || undefined,
            name:
                vRequired(name, "Product name cannot be empty") ||
                vMax(name, NAME_MAX, "Name too long") ||
                undefined,
            mrp: vMrp(mrp) || undefined,
            imageUrl: vImageUrlOptional(imageUrl) || undefined,
        }

        const hasError = Boolean(
            guard.barcode || guard.clientEmail || guard.name || guard.mrp || guard.imageUrl
        )

        if (hasError) {
            if (guard.barcode) barcodeRef.current?.focus()
            return
        }

        setSubmitting(true)

        const toastId = toast.loading(isEditMode ? "Updating product..." : "Creating product...")

        const payload = {
            barcode: barcode.trim(),
            clientEmail: clientEmail.trim(),
            name: name.trim(),
            mrp: Number(mrp),
            imageUrl: imageUrl.trim() ? imageUrl.trim() : undefined,
        }

        try {
            if (isEditMode && initialData) {
                const oldBarcode = (initialData.barcode ?? "").trim()

                await updateProduct({
                    oldBarcode,
                    newBarcode: payload.barcode,
                    clientEmail: payload.clientEmail,
                    name: payload.name,
                    mrp: payload.mrp,
                    imageUrl: payload.imageUrl,
                })

                toast.success("Updated product details", { id: toastId })
            } else {
                await createProduct(payload)
                toast.success("Created product", { id: toastId })
            }

            onSuccess()
            resetLocalState()
            onClose()
        } catch (err: unknown) {
            const msg = extractApiMessage(err)
            toast.error(msg, { id: toastId })

            const lower = msg.toLowerCase()
            if (lower.includes("barcode")) {
                setServerErrors({ barcode: msg })
                barcodeRef.current?.focus()
            } else if (lower.includes("email") || lower.includes("client")) {
                setServerErrors({ clientEmail: msg })
            } else if (lower.includes("mrp")) {
                setServerErrors({ mrp: msg })
            } else if (lower.includes("name")) {
                setServerErrors({ name: msg })
            } else if (lower.includes("image")) {
                setServerErrors({ imageUrl: msg })
            } else {
                setServerErrors({ barcode: msg })
            }
        } finally {
            setSubmitting(false)
        }
    }

    function handleClose() {
        resetLocalState()
        onClose()
    }

    return (
        <Dialog open={isOpen} onOpenChange={(open) => (!open ? handleClose() : undefined)}>
            <DialogContent
                className="animate-in fade-in zoom-in-95 duration-200"
                onInteractOutside={(e) => e.preventDefault()}
                onEscapeKeyDown={(e) => e.preventDefault()}
            >
                <DialogHeader>
                    <DialogTitle>{isEditMode ? "Edit Product" : "Add Product"}</DialogTitle>
                </DialogHeader>

                <div className="space-y-4">
                    <div className="space-y-1">
                        <Label>Barcode</Label>
                        <Input
                            ref={barcodeRef}
                            value={barcode}
                            onChange={(e) => {
                                setBarcode(e.target.value)
                                if (serverErrors.barcode) setServerErrors((p) => ({ ...p, barcode: undefined }))
                            }}
                            onBlur={() => setTouched((p) => ({ ...p, barcode: true }))}
                            placeholder="SKU-001"
                            className={cn(
                                "focus-visible:ring-1 focus-visible:ring-indigo-500",
                                errors.barcode && "border-red-400 ring-2 ring-red-400/30"
                            )}
                        />
                        {errors.barcode && <p className="text-sm text-red-500">{errors.barcode}</p>}
                    </div>

                    <div className="space-y-1">
                        <Label>Client Email</Label>
                        <Input
                            value={clientEmail}
                            onChange={(e) => {
                                setClientEmail(e.target.value)
                                if (serverErrors.clientEmail) setServerErrors((p) => ({ ...p, clientEmail: undefined }))
                            }}
                            onBlur={() => setTouched((p) => ({ ...p, clientEmail: true }))}
                            placeholder="client@email.com"
                            className={cn(
                                "focus-visible:ring-1 focus-visible:ring-indigo-500",
                                errors.clientEmail && "border-red-400 ring-2 ring-red-400/30"
                            )}
                        />
                        {errors.clientEmail && <p className="text-sm text-red-500">{errors.clientEmail}</p>}
                    </div>

                    <div className="space-y-1">
                        <Label>Name</Label>
                        <Input
                            value={name}
                            onChange={(e) => {
                                setName(e.target.value)
                                if (serverErrors.name) setServerErrors((p) => ({ ...p, name: undefined }))
                            }}
                            onBlur={() => setTouched((p) => ({ ...p, name: true }))}
                            placeholder="Product name"
                            className={cn(
                                "focus-visible:ring-1 focus-visible:ring-indigo-500",
                                errors.name && "border-red-400 ring-2 ring-red-400/30"
                            )}
                        />
                        {errors.name && <p className="text-sm text-red-500">{errors.name}</p>}
                    </div>

                    <div className="space-y-1">
                        <Label>MRP</Label>
                        <Input
                            type="number"
                            value={mrp}
                            onChange={(e) => {
                                setMrp(e.target.value)
                                if (serverErrors.mrp) setServerErrors((p) => ({ ...p, mrp: undefined }))
                            }}
                            onBlur={() => setTouched((p) => ({ ...p, mrp: true }))}
                            placeholder="79999"
                            className={cn(
                                "focus-visible:ring-1 focus-visible:ring-indigo-500",
                                errors.mrp && "border-red-400 ring-2 ring-red-400/30"
                            )}
                        />
                        {errors.mrp && <p className="text-sm text-red-500">{errors.mrp}</p>}
                    </div>

                    <div className="space-y-1">
                        <Label>Image URL (optional)</Label>
                        <Input
                            value={imageUrl}
                            onChange={(e) => {
                                setImageUrl(e.target.value)
                                if (serverErrors.imageUrl) setServerErrors((p) => ({ ...p, imageUrl: undefined }))
                            }}
                            onBlur={() => setTouched((p) => ({ ...p, imageUrl: true }))}
                            placeholder="https://image.com/product.jpg"
                            className={cn(
                                "focus-visible:ring-1 focus-visible:ring-indigo-500",
                                errors.imageUrl && "border-red-400 ring-2 ring-red-400/30"
                            )}
                        />
                        {errors.imageUrl && <p className="text-sm text-red-500">{errors.imageUrl}</p>}
                    </div>
                </div>

                <DialogFooter>
                    <Button variant="outline" onClick={handleClose} disabled={submitting}>
                        Cancel
                    </Button>

                    <Button
                        onClick={handleSubmit}
                        disabled={submitting || !isFormValid}
                        className="bg-indigo-600 hover:bg-indigo-700 text-white"
                    >
                        {submitting ? "Saving…" : "Save"}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}

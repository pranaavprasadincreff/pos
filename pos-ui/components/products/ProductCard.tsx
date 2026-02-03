"use client"

import Image from "next/image"
import { useMemo, useState } from "react"
import { ProductData } from "@/services/types"
import { Button } from "@/components/ui/button"
import { Pencil, ImageOff } from "lucide-react"
import InventoryPopover from "./InventoryPopover"
import { formatINR } from "@/utils/currencyFormat"
import { Hint } from "@/components/shared/Hint"
import { can, Role } from "@/utils/permissions"

type ProductUI = ProductData & { clientName?: string }

interface Props {
    product: ProductUI
    onEdit: (p: ProductData) => void
    onInventoryUpdated: (updated: ProductData) => void
}

function isValidImageUrl(url?: string): boolean {
    if (!url) return false
    try {
        const u = new URL(url)
        return u.protocol === "http:" || u.protocol === "https:"
    } catch {
        return false
    }
}

export default function ProductCard({ product, onEdit, onInventoryUpdated }: Props) {
    const [imageError, setImageError] = useState(false)

    const role =
        typeof window !== "undefined"
            ? (sessionStorage.getItem("auth.role") as Role | null)
            : null

    const canEdit = useMemo(() => can(role, "product_edit"), [role])

    const canRenderImage = !imageError && isValidImageUrl(product.imageUrl)

    const clientLine = product.clientName
        ? `${product.clientName} - ${product.clientEmail}`
        : product.clientEmail

    return (
        <div className="group rounded-xl border bg-white shadow-sm hover:shadow-md transition">
            <div className="relative h-40 w-full rounded-t-xl overflow-hidden bg-slate-100">
                {canRenderImage ? (
                    <Image
                        src={product.imageUrl!}
                        alt={product.name}
                        fill
                        className="object-cover"
                        onError={() => setImageError(true)}
                    />
                ) : (
                    <div className="flex h-full flex-col items-center justify-center text-slate-400">
                        <ImageOff className="h-6 w-6 mb-1" />
                        <span className="text-xs">No image available</span>
                    </div>
                )}

                {/* ✅ Hide edit button for operators */}
                {canEdit ? (
                    <Hint text="Edit">
                        <Button
                            size="icon"
                            variant="secondary"
                            onClick={() => onEdit(product)}
                            className="
                absolute top-2 right-2
                h-8 w-8 rounded-full
                opacity-0 group-hover:opacity-100
                transition
                shadow-sm
              "
                        >
                            <Pencil className="h-4 w-4" />
                        </Button>
                    </Hint>
                ) : null}
            </div>

            <div className="p-4 space-y-3">
                <div>
                    <p className="font-medium truncate">{product.name}</p>
                    <p className="text-sm text-muted-foreground truncate">{clientLine}</p>
                    <p className="text-xs text-muted-foreground">{product.barcode}</p>
                </div>

                <div className="flex items-center justify-between">
                    <p className="font-semibold">₹{formatINR(product.mrp)}</p>
                </div>

                {/* Inventory visible for both, control handled inside popover */}
                <InventoryPopover product={product} onUpdated={onInventoryUpdated} />
            </div>
        </div>
    )
}

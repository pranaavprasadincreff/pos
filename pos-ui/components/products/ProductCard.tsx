'use client'

import Image from 'next/image'
import { useState } from 'react'
import { ProductData } from '@/services/types'
import { Button } from '@/components/ui/button'
import { Pencil, ImageOff } from 'lucide-react'
import InventoryPopover from './InventoryPopover'

interface Props {
    product: ProductData
    onEdit: (p: ProductData) => void
    onInventoryUpdated: () => void
}

export default function ProductCard({ product, onEdit, onInventoryUpdated }: Props) {
    const [imageError, setImageError] = useState(false)

    return (
        <div className="group rounded-xl border bg-white shadow-sm hover:shadow-md transition">
            {/* Image */}
            <div className="relative h-40 w-full rounded-t-xl overflow-hidden bg-slate-100">
                {!imageError && product.imageUrl ? (
                    <Image
                        src={product.imageUrl}
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

                {/* Edit button overlay */}
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
            </div>

            {/* Content */}
            <div className="p-4 space-y-3">
                <div>
                    <p className="font-medium truncate">{product.name}</p>
                    <p className="text-sm text-muted-foreground truncate">
                        {product.clientEmail}
                    </p>
                    <p className="text-xs text-muted-foreground">
                        {product.barcode}
                    </p>
                </div>

                <div className="flex items-center justify-between">
                    <p className="font-semibold">
                        ₹{product.mrp.toLocaleString()}
                    </p>
                </div>

                <InventoryPopover
                    product={product}
                    onUpdated={onInventoryUpdated}
                />
            </div>
        </div>
    )
}

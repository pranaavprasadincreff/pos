import ProductCard from './ProductCard'
import { ProductData } from '@/services/types'
import { Skeleton } from '@/components/ui/skeleton'

interface Props {
    products: ProductData[]
    loading: boolean
    onEdit: (product: ProductData) => void
    onInventoryUpdated: () => void
}

export default function ProductCardGrid({
                                        products,
                                        loading,
                                        onEdit,
                                        onInventoryUpdated
                                    }: Props) {
    if (loading) {
        return (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {Array.from({ length: 6 }).map((_, i) => (
                    <Skeleton
                        key={i}
                        className="h-[320px] rounded-xl"
                    />
                ))}
            </div>
        )
    }

    if (!products.length) {
        return (
            <div className="text-center py-20 text-muted-foreground">
                No products found
            </div>
        )
    }

    return (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {products.map((product) => (
                <ProductCard
                    key={product.id}
                    product={product}
                    onEdit={() => onEdit(product)}
                    onInventoryUpdated={onInventoryUpdated}
                />
            ))}
        </div>
    )
}

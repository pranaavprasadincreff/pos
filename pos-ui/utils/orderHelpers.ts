import { OrderData, OrderItemData } from '@/services/types'

export function getOrderItemLineTotal(item: OrderItemData): number {
    return item.quantity * item.sellingPrice
}

export function getOrderTotal(order: OrderData): number {
    return order.items.reduce(
        (sum, item) => sum + getOrderItemLineTotal(item),
        0
    )
}

export function getOrderQuantityTotal(order: OrderData): number {
    return order.items.reduce((sum, i) => sum + i.quantity, 0)
}

export function getOrderItemCount(order: OrderData): number {
    return order.items.length
}

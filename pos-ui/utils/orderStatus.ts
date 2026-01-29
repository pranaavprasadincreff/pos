import { OrderStatus } from '@/services/types'

export const ORDER_STATUS_META: Record<
    OrderStatus,
    { label: string; badgeClass: string }
> = {
    FULFILLABLE: {
        label: 'Fulfillable',
        badgeClass:
            'bg-emerald-100 text-emerald-700 border border-emerald-200',
    },

    UNFULFILLABLE: {
        label: 'Unfulfillable',
        badgeClass:
            'bg-amber-100 text-amber-700 border border-amber-200',
    },

    INVOICED: {
        label: 'Invoiced',
        badgeClass:
            'bg-blue-100 text-blue-700 border border-blue-200',
    },

    CANCELLED: {
        label: 'Cancelled',
        badgeClass:
            'bg-red-100 text-red-700 border border-red-200',
    },
}

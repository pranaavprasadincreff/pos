import { OrderItemData } from '@/services/types'
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/components/ui/table'

interface OrderItemsTableProps {
    items: OrderItemData[]
}

export default function OrderItemsTable({ items }: OrderItemsTableProps) {
    return (
        <div className="rounded-md border bg-background">
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead>Barcode</TableHead>
                        <TableHead>Quantity</TableHead>
                        <TableHead>Selling Price</TableHead>
                    </TableRow>
                </TableHeader>

                <TableBody>
                    {items.map((item, index) => (
                        <TableRow key={`${item.productBarcode}-${index}`}>
                            <TableCell>{item.productBarcode}</TableCell>
                            <TableCell>{item.quantity}</TableCell>
                            <TableCell>₹ {item.sellingPrice.toFixed(2)}</TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </div>
    )
}

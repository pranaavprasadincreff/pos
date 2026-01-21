'use client'

import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog'
import {
    Tabs,
    TabsList,
    TabsTrigger,
    TabsContent,
} from '@/components/ui/tabs'
import { ProductData } from '@/services/types'

interface Props {
    isOpen: boolean
    onClose: () => void
    onSuccess: () => void
    initialData: ProductData | null
}

export default function ProductModal({
                                         isOpen,
                                         onClose,
                                         onSuccess,
                                         initialData,
                                     }: Props) {
    const isEdit = Boolean(initialData)

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="max-w-3xl">
                <DialogHeader>
                    <DialogTitle>
                        {isEdit ? 'Edit Product' : 'Add Product'}
                    </DialogTitle>
                </DialogHeader>

                <Tabs defaultValue="single" className="w-full">
                    <TabsList className="grid grid-cols-3 w-full">
                        <TabsTrigger value="single">
                            Single Product
                        </TabsTrigger>
                        <TabsTrigger value="bulk-product">
                            Bulk Product
                        </TabsTrigger>
                        <TabsTrigger value="bulk-inventory">
                            Bulk Inventory
                        </TabsTrigger>
                    </TabsList>

                    <TabsContent value="single">
                        <div className="py-6 text-sm text-muted-foreground">
                            Single product form coming next
                        </div>
                    </TabsContent>

                    <TabsContent value="bulk-product">
                        <div className="py-6 text-sm text-muted-foreground">
                            Bulk product upload coming next
                        </div>
                    </TabsContent>

                    <TabsContent value="bulk-inventory">
                        <div className="py-6 text-sm text-muted-foreground">
                            Bulk inventory upload coming next
                        </div>
                    </TabsContent>
                </Tabs>
            </DialogContent>
        </Dialog>
    )
}

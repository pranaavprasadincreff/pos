import api from "@/services/api"
import { InvoiceData } from "@/services/types"

export const invoiceService = {
    generate(orderReferenceId: string) {
        return api.post<InvoiceData>(`/pos/invoice/generate/${orderReferenceId}`)
    },

    get(orderReferenceId: string) {
        return api.get<InvoiceData>(`/pos/invoice/get/${orderReferenceId}`)
    },
}

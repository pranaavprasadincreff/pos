import axios from 'axios'
import { InvoiceData } from '@/services/types'

export const invoiceService = {
    generate(orderReferenceId: string) {
        return axios.post<InvoiceData>(
            `/api/pos/invoice/generate/${orderReferenceId}`
        )
    },

    get(orderReferenceId: string) {
        return axios.get<InvoiceData>(
            `/api/pos/invoice/get/${orderReferenceId}`
        )
    },
}
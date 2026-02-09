import api from "@/services/api"
import type { InvoiceData } from "@/services/types"

export const invoiceService = {
  generate(orderReferenceId: string) {
    return api.post<InvoiceData>(`/invoice/${encodeURIComponent(orderReferenceId)}`)
  },

  get(orderReferenceId: string) {
    return api.get<InvoiceData>(`/invoice/${encodeURIComponent(orderReferenceId)}`)
  },
}

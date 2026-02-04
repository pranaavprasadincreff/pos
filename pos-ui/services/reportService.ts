import api from "@/services/api"

export type ReportRowType = "CLIENT" | "PRODUCT"

export interface SalesReportRowData {
  clientEmail: string | null
  productBarcode: string | null
  ordersCount: number
  itemsCount: number
  totalRevenue: number
}

export interface SalesReportResponseData {
  reportKind: "DAILY" | "RANGE"
  startDate: string
  endDate: string
  clientEmail: string | null
  rowType: ReportRowType
  generatedAt: string
  rows: SalesReportRowData[]
}

export interface SalesReportForm {
  startDate: string
  endDate?: string
  clientEmail?: string
}

export async function getDailySalesReport(form: SalesReportForm): Promise<SalesReportResponseData> {
  const res = await api.post(`/reports/sales/daily`, form)
  return res.data
}

export async function getRangeSalesReport(form: SalesReportForm): Promise<SalesReportResponseData> {
  const res = await api.post(`/reports/sales/range`, form)
  return res.data
}

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

export interface DailySalesReportForm {
  date: string
  clientEmail?: string
}

export interface RangeSalesReportForm {
  startDate: string
  endDate: string
  clientEmail?: string
}

export async function getDailySalesReport(form: DailySalesReportForm) {
  const res = await api.post(`/reports/sales/daily`, form)
  return res.data
}

export async function getRangeSalesReport(form: RangeSalesReportForm) {
  const res = await api.post(`/reports/sales/range`, form)
  return res.data
}

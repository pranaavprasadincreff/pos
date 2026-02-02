export type ProductFilterKey = "name" | "barcode" | "client"

export const PRODUCT_FILTERS: Record<
    ProductFilterKey,
    { label: string; placeholder: string; tooltip: string; apiField: ProductFilterKey }
> = {
    name: {
        label: "Name",
        placeholder: "Search product name…",
        tooltip: "Filter products by product name",
        apiField: "name",
    },
    barcode: {
        label: "Barcode",
        placeholder: "Search barcode…",
        tooltip: "Filter products by barcode",
        apiField: "barcode",
    },
    client: {
        label: "Client",
        placeholder: "Search client (name or email)…",
        tooltip: "Search by product name OR client email (merged results)",
        apiField: "client",
    },
}

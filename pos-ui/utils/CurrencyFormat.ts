export const formatINR = (value: number | string) => {
    const num = typeof value === "string" ? Number(value) : value

    if (!Number.isFinite(num)) return "0.00"

    return new Intl.NumberFormat("en-IN", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(num)
}

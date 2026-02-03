export function formatDate(dateString: string): string {
    const d = new Date(dateString)

    const datePart = d.toLocaleDateString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
    })

    const timePart = d.toLocaleTimeString("en-IN", {
        hour: "2-digit",
        minute: "2-digit",
        hour12: false, // ✅ 24-hour clock
    })

    return ` ${timePart}, ${datePart}`
}

export function formatDate(dateString: string): string {
    const d = new Date(dateString)

    const day = d.getDate().toString().padStart(2, '0')
    const month = d.toLocaleString('en-IN', { month: 'short' })
    const year = d.getFullYear()

    return `${day} ${month}, ${year}`
}

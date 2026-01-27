export function downloadBase64Pdf(
    base64: string,
    fileName: string
): void {
    const byteCharacters = atob(base64)
    const byteNumbers = Array.from(byteCharacters, char =>
        char.charCodeAt(0)
    )

    const blob = new Blob([new Uint8Array(byteNumbers)], {
        type: 'application/pdf',
    })

    const url = URL.createObjectURL(blob)

    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    link.click()

    URL.revokeObjectURL(url)
}

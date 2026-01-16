import './globals.css'
import type { Metadata } from 'next'
import Sidebar from '@/components/layout/Sidebar'
import { Toaster } from '@/components/ui/sonner'

export const metadata: Metadata = {
    title: 'POS UI',
    description: 'POS Admin',
}

export default function RootLayout({
                                       children,
                                   }: {
    children: React.ReactNode
}) {
    return (
        <html lang="en">
            <body className="bg-muted/40">
                <div className="flex min-h-screen">
                    <Sidebar />
                    <main className="flex-1 p-6">{children}</main>
                </div>
                <Toaster
                    position="bottom-right"
                    richColors
                />
            </body>
        </html>
    )
}

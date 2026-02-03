import "./globals.css"
import type { Metadata } from "next"
import RootShell from "@/components/layout/RootShell"
import { Toaster } from "@/components/ui/sonner"
import { TooltipProvider } from "@/components/ui/tooltip"

export const metadata: Metadata = {
    title: "POS Console",
    description: "POS Console",
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
    return (
        <html lang="en">
        <body className="bg-muted/40 overflow-hidden">
        <TooltipProvider>
            <RootShell>{children}</RootShell>
            <Toaster position="top-right" richColors />
        </TooltipProvider>
        </body>
        </html>
    )
}

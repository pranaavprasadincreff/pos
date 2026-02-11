"use client"

import { useEffect, useMemo, useState } from "react"
import { usePathname, useRouter } from "next/navigation"
import Sidebar from "@/components/layout/Sidebar"
import api from "@/services/api"

const FIVE_MIN = 5 * 60 * 1000

export default function RootShell({ children }: { children: React.ReactNode }) {
    const router = useRouter()
    const pathname = usePathname()
    const [ready, setReady] = useState(false)

    const isLoginRoute = useMemo(() => pathname === "/login", [pathname])

    useEffect(() => {
        if (isLoginRoute) {
            setReady(true)
            return
        }

        const token = sessionStorage.getItem("auth.token")
        if (!token) {
            router.replace("/login")
            return
        }

        const lastCheck = Number(sessionStorage.getItem("auth.lastCheck") || "0")
        const needsCheck = Date.now() - lastCheck > FIVE_MIN

        if (!needsCheck) {
            setReady(true)
            return
        }

        api.get("/auth")
            .then((res) => {
                // keep email/role synced (nice to have)
                if (res?.data?.email) sessionStorage.setItem("auth.email", res.data.email)
                if (res?.data?.role) sessionStorage.setItem("auth.role", res.data.role)
                sessionStorage.setItem("auth.lastCheck", Date.now().toString())
                setReady(true)
            })
            .catch(() => {
                sessionStorage.clear()
                router.replace("/login")
            })
    }, [router, isLoginRoute])

    if (!ready) {
        return (
            <div className="flex h-screen items-center justify-center text-muted-foreground">
                Loading...
            </div>
        )
    }

    return (
        <div className="flex h-screen">
            {!isLoginRoute && <Sidebar />}
            <main className="flex-1 overflow-y-auto p-6">{children}</main>
        </div>
    )
}

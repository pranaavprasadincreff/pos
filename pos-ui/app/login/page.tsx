"use client"

import { useEffect, useMemo, useState } from "react"
import { useRouter } from "next/navigation"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { toast } from "sonner"
import api from "@/services/api"
import { Eye, EyeOff } from "lucide-react"

type Mode = "LOGIN" | "SIGNUP"

export default function LoginPage() {
    const router = useRouter()
    const [mode, setMode] = useState<Mode>("LOGIN")
    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const [showPassword, setShowPassword] = useState(false)
    const [submitting, setSubmitting] = useState(false)

    useEffect(() => {
        const token = sessionStorage.getItem("auth.token")
        if (token) router.replace("/")
    }, [router])

    const emailValid = useMemo(() => {
        const v = email.trim().toLowerCase()
        return /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/.test(v)
    }, [email])

    const canSubmit = emailValid && password.length >= 6 && !submitting

    async function submit() {
        if (!emailValid) return toast.error("Enter a valid email")
        if (password.length < 6) return toast.error("Password must be at least 6 characters")

        const normalizedEmail = email.trim().toLowerCase()

        let toastId: string | number | undefined
        try {
            setSubmitting(true)
            toastId = toast.loading(mode === "LOGIN" ? "Logging in..." : "Creating account...")

            const endpoint = mode === "LOGIN" ? "/auth/login" : "/auth/signup"
            const res = await api.post(endpoint, { email: normalizedEmail, password })

            const { token, role, email: resEmail } = res.data || {}
            if (!token || !role) {
                toast.error("Unexpected response from server")
                return
            }

            sessionStorage.setItem("auth.token", token)
            sessionStorage.setItem("auth.role", role)
            sessionStorage.setItem("auth.email", resEmail || normalizedEmail)
            sessionStorage.setItem("auth.lastCheck", Date.now().toString())

            toast.success(mode === "LOGIN" ? "Welcome back!" : "Account created!")
            router.replace("/")
        } catch (e: any) {
            const msg = e?.response?.data?.message || e?.message || "Something went wrong"
            toast.error(msg)
        } finally {
            if (toastId) toast.dismiss(toastId)
            setSubmitting(false)
        }
    }

    return (
        <div className="min-h-[100dvh] flex items-center justify-center bg-muted/40 p-6">
            <div className="w-full max-w-md">
                <Card className="shadow-sm">
                    <CardHeader className="space-y-2">
                        <CardTitle className="text-2xl font-semibold tracking-tight">
                            {mode === "LOGIN" ? "Sign in" : "Sign up"}
                        </CardTitle>
                        <p className="text-sm text-muted-foreground">
                            {mode === "LOGIN"
                                ? "Use your email and password to access the POS."
                                : "Supervisors can sign up only if their email is configured by the admin."}
                        </p>
                    </CardHeader>

                    <CardContent className="space-y-4">
                        <div className="space-y-2">
                            <label className="text-sm font-medium">Email</label>
                            <Input
                                placeholder="name@company.com"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                autoComplete="email"
                                className="transition focus-visible:ring-2 focus-visible:ring-indigo-500"
                            />
                        </div>

                        <div className="space-y-2">
                            <label className="text-sm font-medium">Password</label>

                            {/* password + eye */}
                            <div className="relative">
                                <Input
                                    type={showPassword ? "text" : "password"}
                                    placeholder="••••••••"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    autoComplete={mode === "LOGIN" ? "current-password" : "new-password"}
                                    className="pr-10 transition focus-visible:ring-2 focus-visible:ring-indigo-500"
                                    onKeyDown={(e) => {
                                        if (e.key === "Enter" && canSubmit) submit()
                                    }}
                                />

                                <button
                                    type="button"
                                    aria-label={showPassword ? "Hide password" : "Show password"}
                                    onClick={() => setShowPassword((v) => !v)}
                                    className="absolute right-2 top-1/2 -translate-y-1/2 rounded-md p-1 text-muted-foreground hover:text-foreground hover:bg-muted transition"
                                    tabIndex={-1}
                                >
                                    {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                                </button>
                            </div>

                            <p className="text-xs text-muted-foreground">Minimum 6 characters.</p>
                        </div>

                        <Button
                            onClick={submit}
                            disabled={!canSubmit}
                            className="w-full bg-indigo-600 hover:bg-indigo-700"
                        >
                            {submitting
                                ? mode === "LOGIN"
                                    ? "Signing in..."
                                    : "Signing up..."
                                : mode === "LOGIN"
                                    ? "Sign in"
                                    : "Create account"}
                        </Button>

                        <div className="flex items-center justify-between pt-1">
                            <button
                                type="button"
                                className="text-sm text-muted-foreground hover:text-foreground transition"
                                onClick={() => setMode(mode === "LOGIN" ? "SIGNUP" : "LOGIN")}
                            >
                                {mode === "LOGIN"
                                    ? "Need a supervisor account? Sign up"
                                    : "Already have an account? Sign in"}
                            </button>

                            <span className="text-xs text-muted-foreground">Invite-only operators</span>
                        </div>
                    </CardContent>
                </Card>

                <p className="mt-4 text-center text-xs text-muted-foreground">
                    Tip: Operators should use <span className="font-medium">Sign in</span>. Supervisors can use{" "}
                    <span className="font-medium">Sign up</span>.
                </p>
            </div>
        </div>
    )
}

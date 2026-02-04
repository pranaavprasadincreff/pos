"use client"

import { useEffect, useMemo, useRef, useState } from "react"
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { cn } from "@/lib/utils"

const EMAIL_MAX = 40
const PASSWORD_MIN = 6 // keep in sync with backend rule
const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/

export interface OperatorCreateForm {
    email: string
    password: string
}

interface Props {
    isOpen: boolean
    onClose: () => void
    onSubmit: (form: OperatorCreateForm) => Promise<void>
}

function validateEmailWhileTyping(email: string): string | null {
    const v = email.trim()
    if (!v) return "Email required"
    if (v.length > EMAIL_MAX) return "Email too long"
    return null
}
function validateEmailStrict(email: string): string | null {
    const basic = validateEmailWhileTyping(email)
    if (basic) return basic
    if (!EMAIL_REGEX.test(email.trim())) return "Invalid email format"
    return null
}
function validatePassword(pw: string): string | null {
    if (!pw) return "Password required"
    if (pw.length < PASSWORD_MIN) return `Password must be at least ${PASSWORD_MIN} characters`
    return null
}

export default function CreateOperatorModal({ isOpen, onClose, onSubmit }: Props) {
    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const [confirm, setConfirm] = useState("")
    const [submitting, setSubmitting] = useState(false)

    const [emailTouched, setEmailTouched] = useState(false)
    const [emailBlurred, setEmailBlurred] = useState(false)
    const [pwTouched, setPwTouched] = useState(false)
    const [confirmTouched, setConfirmTouched] = useState(false)
    const [submitAttempted, setSubmitAttempted] = useState(false)

    const emailRef = useRef<HTMLInputElement>(null)
    const pwRef = useRef<HTMLInputElement>(null)

    const [serverError, setServerError] = useState<string | null>(null)

    useEffect(() => {
        if (!isOpen) return
        setEmail("")
        setPassword("")
        setConfirm("")
        setSubmitting(false)
        setEmailTouched(false)
        setEmailBlurred(false)
        setPwTouched(false)
        setConfirmTouched(false)
        setSubmitAttempted(false)
        setServerError(null)
    }, [isOpen])

    const emailError = useMemo(() => {
        if (!isOpen) return null
        if (!emailTouched && !submitAttempted) return null
        if (!emailBlurred && !submitAttempted) return validateEmailWhileTyping(email)
        return validateEmailStrict(email)
    }, [email, emailTouched, emailBlurred, submitAttempted, isOpen])

    const pwError = useMemo(() => {
        if (!isOpen) return null
        if (!pwTouched && !submitAttempted) return null
        return validatePassword(password)
    }, [password, pwTouched, submitAttempted, isOpen])

    const confirmError = useMemo(() => {
        if (!isOpen) return null
        if (!confirmTouched && !submitAttempted) return null
        if (!confirm) return "Confirm password required"
        if (confirm !== password) return "Passwords do not match"
        return null
    }, [confirm, password, confirmTouched, submitAttempted, isOpen])

    const isFormValid = useMemo(() => {
        const eOk =
            !emailBlurred && !submitAttempted
                ? !validateEmailWhileTyping(email)
                : !validateEmailStrict(email)

        return eOk && !validatePassword(password) && confirm === password && confirm.length > 0
    }, [email, password, confirm, emailBlurred, submitAttempted])

    async function handleSubmit() {
        setSubmitAttempted(true)
        setEmailBlurred(true)
        setServerError(null)

        const eErr = validateEmailStrict(email)
        if (eErr) {
            setEmailTouched(true)
            emailRef.current?.focus()
            return
        }

        const pErr = validatePassword(password)
        if (pErr) {
            setPwTouched(true)
            pwRef.current?.focus()
            return
        }

        if (!confirm || confirm !== password) {
            setConfirmTouched(true)
            return
        }

        setSubmitting(true)
        try {
            await onSubmit({ email: email.trim(), password })
            onClose()
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : "Something went wrong"
            setServerError(msg)
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <Dialog open={isOpen} onOpenChange={(open) => (!open ? onClose() : undefined)}>
            <DialogContent
                className="animate-in fade-in zoom-in-95 duration-200"
                onInteractOutside={(e) => e.preventDefault()}
                onEscapeKeyDown={(e) => e.preventDefault()}
            >
                <DialogHeader>
                    <DialogTitle>Add Operator</DialogTitle>
                </DialogHeader>

                <div className="space-y-4">
                    {serverError && (
                        <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-md px-3 py-2">
                            {serverError}
                        </p>
                    )}

                    <div className="space-y-1">
                        <Label>Email</Label>
                        <Input
                            ref={emailRef}
                            value={email}
                            onChange={(e) => {
                                setEmail(e.target.value)
                                if (!emailTouched) setEmailTouched(true)
                                if (emailBlurred) setEmailBlurred(false)
                            }}
                            onBlur={() => {
                                setEmailTouched(true)
                                setEmailBlurred(true)
                            }}
                            placeholder="operator@email.com"
                            type="email"
                            className={cn(
                                "focus-visible:ring-1 focus-visible:ring-indigo-500",
                                emailError &&
                                "border-red-400 ring-2 ring-red-400/30 focus-visible:ring-red-400/40"
                            )}
                        />
                        {emailError ? (
                            <p className="text-sm text-red-500">{emailError}</p>
                        ) : (
                            <p className="text-xs text-muted-foreground">Max {EMAIL_MAX} characters</p>
                        )}
                    </div>

                    <div className="space-y-1">
                        <Label>Password</Label>
                        <Input
                            ref={pwRef}
                            value={password}
                            onChange={(e) => {
                                setPassword(e.target.value)
                                if (!pwTouched) setPwTouched(true)
                            }}
                            onBlur={() => setPwTouched(true)}
                            placeholder="••••••••"
                            type="password"
                            className={cn(
                                "focus-visible:ring-1 focus-visible:ring-indigo-500",
                                pwError &&
                                "border-red-400 ring-2 ring-red-400/30 focus-visible:ring-red-400/40"
                            )}
                        />
                        {pwError ? (
                            <p className="text-sm text-red-500">{pwError}</p>
                        ) : (
                            <p className="text-xs text-muted-foreground">Min {PASSWORD_MIN} characters</p>
                        )}
                    </div>

                    <div className="space-y-1">
                        <Label>Confirm Password</Label>
                        <Input
                            value={confirm}
                            onChange={(e) => {
                                setConfirm(e.target.value)
                                if (!confirmTouched) setConfirmTouched(true)
                            }}
                            onBlur={() => setConfirmTouched(true)}
                            placeholder="••••••••"
                            type="password"
                            className={cn(
                                "focus-visible:ring-1 focus-visible:ring-indigo-500",
                                confirmError &&
                                "border-red-400 ring-2 ring-red-400/30 focus-visible:ring-red-400/40"
                            )}
                        />
                        {confirmError ? (
                            <p className="text-sm text-red-500">{confirmError}</p>
                        ) : (
                            <p className="text-xs text-muted-foreground">Must match password</p>
                        )}
                    </div>
                </div>

                <DialogFooter>
                    <Button variant="outline" className="border-slate-300" onClick={onClose} disabled={submitting}>
                        Cancel
                    </Button>
                    <Button
                        onClick={handleSubmit}
                        disabled={submitting || !isFormValid}
                        className="bg-indigo-600 hover:bg-indigo-700 text-white shadow-sm"
                    >
                        {submitting ? "Saving..." : "Save"}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}

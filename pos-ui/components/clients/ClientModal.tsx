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
import { Client } from "@/services/types"
import { cn } from "@/lib/utils"

const EMAIL_MAX = 40
const NAME_MAX = 30
const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/

interface CreateClientForm {
    name: string
    email: string
}

interface EditClientForm {
    name: string
    oldEmail: string
    newEmail: string
}

type ClientForm = CreateClientForm | EditClientForm

interface Props {
    isOpen: boolean
    onClose: () => void
    onSubmit: (form: ClientForm) => Promise<void>
    initialData: Client | null
}

function validateName(name: string): string | null {
    const v = name.trim()
    if (!v) return "Name required"
    if (v.length > NAME_MAX) return "Name too long"
    return null
}

function validateEmailRequired(email: string): string | null {
    const v = email.trim()
    if (!v) return "Email required"
    if (v.length > EMAIL_MAX) return "Email too long"
    // keep this strict for client modal (client creation/update needs valid email)
    if (!EMAIL_REGEX.test(v)) return "Invalid email format"
    return null
}

export default function ClientModal({ isOpen, onClose, onSubmit, initialData }: Props) {
    const isEdit = Boolean(initialData)

    const [name, setName] = useState("")
    const [email, setEmail] = useState("")
    const [originalEmail, setOriginalEmail] = useState("")
    const [submitting, setSubmitting] = useState(false)

    // touched flags (show errors only after interaction)
    const [nameTouched, setNameTouched] = useState(false)
    const [emailTouched, setEmailTouched] = useState(false)
    const [submitAttempted, setSubmitAttempted] = useState(false)

    const nameRef = useRef<HTMLInputElement>(null)
    const emailRef = useRef<HTMLInputElement>(null)

    // backend errors can override field errors
    const [serverNameError, setServerNameError] = useState<string | null>(null)
    const [serverEmailError, setServerEmailError] = useState<string | null>(null)

    function resetLocalState() {
        setName("")
        setEmail("")
        setOriginalEmail("")
        setSubmitting(false)

        setNameTouched(false)
        setEmailTouched(false)
        setSubmitAttempted(false)

        setServerNameError(null)
        setServerEmailError(null)
    }

    useEffect(() => {
        if (!isOpen) return

        if (initialData) {
            setName(initialData.name ?? "")
            setEmail(initialData.email ?? "")
            setOriginalEmail(initialData.email ?? "")
        } else {
            setName("")
            setEmail("")
            setOriginalEmail("")
        }

        setSubmitting(false)
        setNameTouched(false)
        setEmailTouched(false)
        setSubmitAttempted(false)
        setServerNameError(null)
        setServerEmailError(null)
    }, [initialData, isOpen])

    const nameError = useMemo(() => {
        if (serverNameError) return serverNameError
        if (!isOpen) return null
        if (!nameTouched && !submitAttempted) return null
        return validateName(name)
    }, [name, nameTouched, submitAttempted, serverNameError, isOpen])

    const emailError = useMemo(() => {
        if (serverEmailError) return serverEmailError
        if (!isOpen) return null
        if (!emailTouched && !submitAttempted) return null
        return validateEmailRequired(email)
    }, [email, emailTouched, submitAttempted, serverEmailError, isOpen])

    const isFormValid = useMemo(() => {
        return !validateName(name) && !validateEmailRequired(email)
    }, [name, email])

    async function handleSubmit() {
        setSubmitAttempted(true)
        setServerNameError(null)
        setServerEmailError(null)

        const nErr = validateName(name)
        const eErr = validateEmailRequired(email)

        if (nErr) {
            setNameTouched(true)
            nameRef.current?.focus()
            return
        }
        if (eErr) {
            setEmailTouched(true)
            emailRef.current?.focus()
            return
        }

        setSubmitting(true)
        try {
            if (isEdit) {
                await onSubmit({
                    name: name.trim(),
                    oldEmail: originalEmail.trim(),
                    newEmail: email.trim(),
                })
            } else {
                await onSubmit({
                    name: name.trim(),
                    email: email.trim(),
                })
            }

            // close only after submit success
            resetLocalState()
            onClose()
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : "Something went wrong"

            if (msg.toLowerCase().includes("name")) {
                setServerNameError(msg)
                nameRef.current?.focus()
            } else {
                setServerEmailError(msg)
                emailRef.current?.focus()
            }
        } finally {
            setSubmitting(false)
        }
    }

    function handleClose() {
        // close from Cancel or X only
        resetLocalState()
        onClose()
    }

    return (
        <Dialog open={isOpen} onOpenChange={(open) => (!open ? handleClose() : undefined)}>
            <DialogContent
                className="animate-in fade-in zoom-in-95 duration-200"
                onInteractOutside={(e) => e.preventDefault()}
                onEscapeKeyDown={(e) => e.preventDefault()}
            >
                <DialogHeader>
                    <DialogTitle>{isEdit ? "Edit Client" : "Add Client"}</DialogTitle>
                </DialogHeader>

                <div className="space-y-4">
                    {/* Name */}
                    <div className="space-y-1">
                        <Label>Name</Label>
                        <Input
                            ref={nameRef}
                            value={name}
                            onChange={(e) => {
                                setName(e.target.value)
                                if (!nameTouched) setNameTouched(true)
                                if (serverNameError) setServerNameError(null)
                            }}
                            onBlur={() => setNameTouched(true)}
                            placeholder="Client name"
                            autoFocus
                            className={cn(
                                "focus-visible:ring-1 focus-visible:ring-indigo-500",
                                nameError &&
                                "border-red-400 ring-2 ring-red-400/30 focus-visible:ring-red-400/40"
                            )}
                        />
                        {nameError ? (
                            <p className="text-sm text-red-500">{nameError}</p>
                        ) : (
                            <p className="text-xs text-muted-foreground">Max {NAME_MAX} characters</p>
                        )}
                    </div>

                    {/* Email */}
                    <div className="space-y-1">
                        <Label>Email</Label>
                        <Input
                            ref={emailRef}
                            value={email}
                            onChange={(e) => {
                                setEmail(e.target.value)
                                if (!emailTouched) setEmailTouched(true)
                                if (serverEmailError) setServerEmailError(null)
                            }}
                            onBlur={() => setEmailTouched(true)}
                            placeholder="client@email.com"
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
                </div>

                <DialogFooter>
                    <Button
                        variant="outline"
                        className="border-slate-300"
                        onClick={handleClose}
                        disabled={submitting}
                    >
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

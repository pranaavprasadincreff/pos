'use client'

import { useEffect, useRef, useState } from 'react'
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Client } from '@/services/types'
import { cn } from '@/lib/utils'

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

export default function ClientModal({
                                        isOpen,
                                        onClose,
                                        onSubmit,
                                        initialData,
                                    }: Props) {
    const isEdit = Boolean(initialData)

    const [name, setName] = useState('')
    const [email, setEmail] = useState('')
    const [originalEmail, setOriginalEmail] = useState('')
    const [submitting, setSubmitting] = useState(false)

    // Field error state
    const [emailError, setEmailError] = useState<string | null>(null)
    const emailRef = useRef<HTMLInputElement>(null)

    useEffect(() => {
        if (initialData) {
            setName(initialData.name)
            setEmail(initialData.email)
            setOriginalEmail(initialData.email)
        } else {
            setName('')
            setEmail('')
            setOriginalEmail('')
        }
        setEmailError(null)
    }, [initialData, isOpen])

    async function handleSubmit() {
        setSubmitting(true)
        setEmailError(null)

        try {
            if (isEdit) {
                await onSubmit({
                    name,
                    oldEmail: originalEmail,
                    newEmail: email,
                })
            } else {
                await onSubmit({
                    name,
                    email,
                })
            }
        } catch (err: unknown) {
            setEmailError(
                err instanceof Error ? err.message : 'Invalid email'
            )
            emailRef.current?.focus()
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="animate-in fade-in zoom-in-95 duration-200">
                <DialogHeader>
                    <DialogTitle>
                        {isEdit ? 'Edit Client' : 'Add Client'}
                    </DialogTitle>
                </DialogHeader>

                <div className="space-y-4">
                    {/* Name */}
                    <div className="space-y-1">
                        <Label>Name</Label>
                        <Input
                            className="focus-visible:ring-1 focus-visible:ring-indigo-500"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            placeholder="Client name"
                            autoFocus
                        />
                    </div>

                    {/* Email */}
                    <div className="space-y-1">
                        <Label>Email</Label>
                        <Input
                            ref={emailRef}
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="client@email.com"
                            type="email"
                            className={cn(
                                'focus-visible:ring-1 focus-visible:ring-indigo-500',
                                emailError &&
                                'border-red-400 ring-2 ring-red-400/30 focus-visible:ring-red-400/40'
                            )}
                        />
                        {emailError && (
                            <p className="text-sm text-red-500">{emailError}</p>
                        )}
                    </div>
                </div>

                <DialogFooter>
                    <Button
                        variant="outline"
                        className="border-slate-300"
                        onClick={onClose}
                        disabled={submitting}
                    >
                        Cancel
                    </Button>

                    <Button
                        onClick={handleSubmit}
                        disabled={submitting || !name || !email}
                        className="bg-indigo-600 hover:bg-indigo-700 text-white shadow-sm"
                    >
                        {submitting ? 'Saving...' : 'Save'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}

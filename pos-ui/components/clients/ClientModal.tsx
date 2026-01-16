'use client'

import { useEffect, useState } from 'react'
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
import { User } from '@/services/types'

interface Props {
    isOpen: boolean
    onClose: () => void
    onSubmit: (form: any) => void
    initialData: User | null
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
    }, [initialData, isOpen])

    async function handleSubmit() {
        setSubmitting(true)

        try {
            if (isEdit) {
                onSubmit({
                    name,
                    oldEmail: originalEmail,
                    newEmail: email,
                })
            } else {
                onSubmit({
                    name,
                    email,
                })
            }
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>
                        {isEdit ? 'Edit Client' : 'Add Client'}
                    </DialogTitle>
                </DialogHeader>

                <div className="space-y-4">
                    <div className="space-y-1">
                        <Label>Name</Label>
                        <Input
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            placeholder="Client name"
                        />
                    </div>

                    <div className="space-y-1">
                        <Label>Email</Label>
                        <Input
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="client@email.com"
                        />
                    </div>
                </div>

                <DialogFooter>
                    <Button
                        variant="outline"
                        onClick={onClose}
                        disabled={submitting}
                    >
                        Cancel
                    </Button>
                    <Button
                        onClick={handleSubmit}
                        disabled={submitting || !name || !email}
                    >
                        {submitting ? 'Saving...' : 'Save'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}

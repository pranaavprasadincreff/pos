'use client'

import { useEffect, useMemo, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select'

import ClientTable from '@/components/clients/ClientTable'
import ClientModal from '@/components/clients/ClientModal'
import Pagination from '@/components/clients/Pagination'

import { getUsers, addUser, updateUser } from '@/services/userService'
import { User, UserForm, UserUpdateForm } from '@/services/types'
import { toast } from 'sonner'

export default function ClientsPage() {
    const [allClients, setAllClients] = useState<User[]>([])
    const [page, setPage] = useState(0)
    const [loading, setLoading] = useState(false)

    const [modalOpen, setModalOpen] = useState(false)
    const [editingClient, setEditingClient] = useState<User | null>(null)

    const [searchTerm, setSearchTerm] = useState('')
    const [searchBy, setSearchBy] = useState<'name' | 'email'>('name')

    const pageSize = 10

    useEffect(() => {
        fetchAllClients()
    }, [])

    async function fetchAllClients() {
        let toastId
        try {
            setLoading(true)
            toastId = toast.loading('Loading clients...')
            const collected: User[] = []

            let p = 0, total = 1
            while (p < total) {
                const res = await getUsers(p, pageSize)
                collected.push(...res.content)
                total = res.totalPages
                p++
            }

            setAllClients(collected)
        } finally {
            setLoading(false)
            toast.dismiss(toastId)
        }
    }

    async function handleSubmit(form: UserForm | UserUpdateForm) {
        editingClient
            ? await updateUser(form as UserUpdateForm)
            : await addUser(form as UserForm)

        setModalOpen(false)
        setEditingClient(null)
        fetchAllClients()
    }

    const filteredClients = useMemo(() => {
        if (!searchTerm) return allClients
        return allClients.filter(c =>
            c[searchBy].toLowerCase().includes(searchTerm.toLowerCase())
        )
    }, [allClients, searchTerm, searchBy])

    const totalPages = Math.ceil(filteredClients.length / pageSize)
    const paginatedClients = filteredClients.slice(
        page * pageSize,
        page * pageSize + pageSize
    )

    return (
        <div className="space-y-6">
            {/* Sticky Header */}
            <div className="sticky top-0 z-20 border-b
                bg-background/95 backdrop-blur
                supports-[backdrop-filter]:bg-background/80">
                <div className="max-w-6xl mx-auto px-6 py-4 space-y-4">
                    {/* Title + CTA */}
                    <div className="flex justify-between items-center">
                        <div>
                            <h1 className="text-2xl font-semibold">Clients</h1>
                            <p className="text-sm text-muted-foreground">
                                Manage registered clients
                            </p>
                        </div>
                        <Button
                            className="bg-indigo-600 hover:bg-indigo-700"
                            onClick={() => setModalOpen(true)}
                        >
                            + Add Client
                        </Button>
                    </div>

                    {/* Filters */}
                    <div className="flex items-center gap-2 max-w-xl">
                        <Select
                            value={searchBy}
                            onValueChange={(v) => {
                                if (v === 'name' || v === 'email') {
                                    setSearchBy(v)
                                }
                            }}
                        >
                            <SelectTrigger className="w-32">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="name">Name</SelectItem>
                                <SelectItem value="email">Email</SelectItem>
                            </SelectContent>
                        </Select>

                        <Input
                            placeholder={`Search by ${searchBy}`}
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="focus-visible:ring-2
                                       focus-visible:ring-indigo-500
                                       transition"
                        />

                        {searchTerm && (
                            <Button
                                variant="outline"
                                className="
            border-slate-900 text-slate-900
            hover:bg-slate-900 hover:text-white
            transition-colors
        "
                                onClick={() => {
                                    setSearchTerm('')
                                    setPage(0)
                                }}
                            >
                                Clear
                            </Button>
                        )}
                    </div>
                </div>
            </div>

            {/* Content */}
            <div className="max-w-6xl mx-auto px-6 space-y-6">
                <ClientTable
                    clients={paginatedClients}
                    loading={loading}
                    page={page}
                    pageSize={pageSize}
                    onEdit={(c) => {
                        setEditingClient(c)
                        setModalOpen(true)
                    }}
                />

                <Pagination
                    page={page}
                    totalPages={totalPages}
                    onPageChange={setPage}
                />
            </div>

            <ClientModal
                isOpen={modalOpen}
                initialData={editingClient}
                onClose={() => setModalOpen(false)}
                onSubmit={handleSubmit}
            />
        </div>
    )
}

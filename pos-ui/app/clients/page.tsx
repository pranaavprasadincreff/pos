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

import {
    getUsers,
    addUser,
    updateUser,
} from '@/services/userService'
import { User, UserForm, UserUpdateForm } from '@/services/types'
import { toast } from 'sonner'

export default function ClientsPage() {
    // const [clients, setClients] = useState<User[]>([])
    const [page, setPage] = useState(0)
    // const [totalPages, setTotalPages] = useState(0)
    const [allClients, setAllClients] = useState<User[]>([])

    const [modalOpen, setModalOpen] = useState(false)
    const [editingClient, setEditingClient] = useState<User | null>(null)

    // 🔍 search state
    const [searchTerm, setSearchTerm] = useState('')
    const [searchBy, setSearchBy] = useState<'name' | 'email'>('name')
    const [loading, setLoading] = useState(false)
    const isSearching = searchTerm.trim().length > 0

    const pageSize = 10

    useEffect(() => {
        fetchAllClients()
    }, [page])

    useEffect(() => {
        setPage(0)
    }, [searchTerm, searchBy])

    async function fetchAllClients() {
        let toastId: string | number | undefined

        try {
            setLoading(true)
            toastId = toast.loading('Loading clients...')

            const collected: User[] = []
            let currentPage = 0
            let totalPages = 1

            while (currentPage < totalPages) {
                const res = await getUsers(currentPage, pageSize)
                collected.push(...res.content)
                totalPages = res.totalPages
                currentPage++
            }

            setAllClients(collected)
        } catch (err) {
            toast.error('Failed to load clients')
        } finally {
            setLoading(false)
            if (toastId) toast.dismiss(toastId)
        }
    }

    async function handleSubmit(form: UserForm | UserUpdateForm) {
        try {
            if (editingClient) {
                await updateUser(form as UserUpdateForm)
                toast.success('Client updated successfully')
            } else {
                await addUser(form as UserForm)
                toast.success('Client added successfully')
                setPage(0)
            }

            setModalOpen(false)
            setEditingClient(null)
            fetchAllClients()
        } catch (err: unknown) {
            if (err instanceof Error) {
                toast.error(err.message)
                throw err
            } else {
                toast.error('Something went wrong')
                throw new Error('Something went wrong')
            }
        }
    }

    function openAddModal() {
        setEditingClient(null)
        setModalOpen(true)
    }

    function openEditModal(client: User) {
        setEditingClient(client)
        setModalOpen(true)
    }

    const filteredClients = useMemo(() => {
        if (!searchTerm) return allClients

        return allClients.filter((c) =>
            c[searchBy].toLowerCase().includes(searchTerm.toLowerCase())
        )
    }, [allClients, searchTerm, searchBy])

    const totalPages = Math.ceil(filteredClients.length / pageSize)

    const paginatedClients = useMemo(() => {
        const start = page * pageSize
        return filteredClients.slice(start, start + pageSize)
    }, [filteredClients, page, pageSize])

    useEffect(() => {
        if (page >= totalPages && totalPages > 0) {
            setPage(totalPages - 1)
        }
    }, [page, totalPages])

    return (
        <div className="max-w-6xl mx-auto space-y-8">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight text-slate-900">
                        Clients
                    </h1>
                    <p className="text-sm text-slate-500">
                        Manage and view all registered clients
                    </p>
                </div>
                <Button
                    onClick={openAddModal}
                    className="bg-indigo-600 hover:bg-indigo-700 text-white shadow-sm"
                >
                    + Add Client
                </Button>
            </div>

            {/* Search */}
            <div className="flex gap-2 max-w-xl">
                <Select
                    value={searchBy}
                    onValueChange={(v) => setSearchBy(v as 'name' | 'email')}
                >
                    <SelectTrigger className="w-[140px]">
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
                    className="focus-visible:ring-1 focus-visible:ring-indigo-500"
                />
            </div>

            {/* Table */}
            <ClientTable
                clients={paginatedClients}
                loading={loading}
                page={page}
                pageSize={pageSize}
                onEdit={openEditModal}
            />

            {/* Pagination */}
            <Pagination
                page={page}
                totalPages={totalPages}
                onPageChange={setPage}
            />

            {/* Modal */}
            <ClientModal
                isOpen={modalOpen}
                onClose={() => setModalOpen(false)}
                onSubmit={handleSubmit}
                initialData={editingClient}
            />
        </div>
    )
}

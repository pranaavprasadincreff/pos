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
    const [clients, setClients] = useState<User[]>([])
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)

    const [modalOpen, setModalOpen] = useState(false)
    const [editingClient, setEditingClient] = useState<User | null>(null)

    // 🔍 search state
    const [searchTerm, setSearchTerm] = useState('')
    const [searchBy, setSearchBy] = useState<'name' | 'email'>('name')
    const [loading, setLoading] = useState(false)
    const isSearching = searchTerm.trim().length > 0

    const pageSize = 10

    useEffect(() => {
        fetchClients()
    }, [page])

    async function fetchClients() {
        const showToast = clients.length > 0
        let toastId: string | number | undefined

        try {
            setLoading(true)

            if (showToast) {
                toastId = toast.loading('Loading clients...')
            }

            // 👇 TEST DELAY (remove later)
            // await new Promise(r => setTimeout(r, 1500))

            const res = await getUsers(page, pageSize)
            setClients(res.content)
            setTotalPages(res.totalPages)
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
                toast.success('Client added successfully')
            } else {
                await addUser(form as UserForm)
                toast.success('Client added successfully')
                setPage(0)
            }

            setModalOpen(false)
            setEditingClient(null)
            fetchClients()
        } catch (err: unknown) {
            if (err instanceof Error) {
                toast.error(err.message)
            } else {
                toast.error('Something went wrong')
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

    // 🔍 frontend filtering
    const filteredClients = useMemo(() => {
        if (!searchTerm) return clients

        return clients.filter((c) =>
            c[searchBy].toLowerCase().includes(searchTerm.toLowerCase())
        )
    }, [clients, searchTerm, searchBy])

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between">
                <h1 className="text-2xl font-semibold">Clients</h1>
                <Button onClick={openAddModal} className="bg-indigo-600 hover:bg-indigo-700 text-white">
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
                />
            </div>

            {/* Table */}
            <ClientTable
                clients={filteredClients}
                loading={loading}
                page={page}
                pageSize={pageSize}
                onEdit={openEditModal}
            />

            {/* Pagination */}
            {!isSearching && (
                <Pagination
                    page={page}
                    totalPages={totalPages}
                    onPageChange={setPage}
                />
            )}


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

"use client"

import { useEffect, useMemo, useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"

import ClientTable from "@/components/clients/ClientTable"
import ClientModal from "@/components/clients/ClientModal"
import Pagination from "@/components/shared/Pagination"

import {
    addClient,
    filterClients,
    getClients,
    updateClient,
} from "@/services/clientService"
import { Client, ClientForm, ClientUpdateForm } from "@/services/types"
import { toast } from "sonner"

import { CLIENT_FILTERS, ClientFilterKey } from "@/filters/clients"
import { Hint } from "@/components/shared/Hint"

// backend mirrors (search validation: length only)
const EMAIL_MAX = 40
const NAME_MAX = 30

export default function ClientsPage() {
    const [clients, setClients] = useState<Client[]>([])
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)
    const [loading, setLoading] = useState(false)

    const [modalOpen, setModalOpen] = useState(false)
    const [editingClient, setEditingClient] = useState<Client | null>(null)

    const [searchTerm, setSearchTerm] = useState("")
    const [searchBy, setSearchBy] = useState<ClientFilterKey>("name")
    const [searchError, setSearchError] = useState<string | null>(null)

    const pageSize = 10

    const placeholder = useMemo(() => {
        return CLIENT_FILTERS[searchBy]?.placeholder ?? "Search…"
    }, [searchBy])

    // ✅ length-only validation for search (no regex for email while typing)
    function validateSearch(term: string, by: ClientFilterKey) {
        const t = term.trim()
        if (!t) return null

        if (by === "name") {
            if (t.length > NAME_MAX) return "Name filter too long"
            return null
        }

        // by === "email"
        if (t.length > EMAIL_MAX) return "Email filter too long"
        return null
    }

    async function fetchClientsPage() {
        let toastId: string | number | undefined

        try {
            setLoading(true)

            const err = validateSearch(searchTerm, searchBy)
            setSearchError(err)
            if (err) return

            toastId = toast.loading("Loading clients...")

            const trimmed = searchTerm.trim()

            // No search => normal paginated
            if (trimmed.length === 0) {
                const res = await getClients(page, pageSize)
                setClients(res.content)
                setTotalPages(res.totalPages)
                return
            }

            // ✅ one-at-a-time filter based on dropdown
            const apiField = CLIENT_FILTERS[searchBy].apiField // "name" | "email"
            const res = await filterClients({
                page,
                size: pageSize,
                [apiField]: trimmed,
            })

            setClients(res.content)
            setTotalPages(res.totalPages)
        } finally {
            setLoading(false)
            if (toastId) toast.dismiss(toastId)
        }
    }

    useEffect(() => {
        fetchClientsPage()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [page, searchBy, searchTerm])

    async function handleSubmit(form: ClientForm | ClientUpdateForm) {
        if (editingClient) {
            await updateClient(form as ClientUpdateForm)
        } else {
            await addClient(form as ClientForm)
        }

        // ✅ close + clear edit state so "Create" opens fresh
        setModalOpen(false)
        setEditingClient(null)

        // reset to first page so update is visible
        setPage(0)
        fetchClientsPage()
    }

    // ✅ single close handler (also clears edit state)
    function handleCloseModal() {
        setModalOpen(false)
        setEditingClient(null)
    }

    return (
        <div className="space-y-6">
            {/* Sticky Header */}
            <div
                className="sticky top-0 z-20 border-b
        bg-background/95 backdrop-blur
        supports-[backdrop-filter]:bg-background/80"
            >
                <div className="max-w-6xl mx-auto px-6 py-4 space-y-4">
                    {/* Title + CTA */}
                    <div className="flex justify-between items-center">
                        <div>
                            <h1 className="text-2xl font-semibold">Clients</h1>
                            <p className="text-sm text-muted-foreground">
                                Manage registered clients
                            </p>
                        </div>

                        <Hint text="Add a new client">
                            <Button
                                className="bg-indigo-600 hover:bg-indigo-700"
                                onClick={() => {
                                    // ✅ important: ensure fresh "Create"
                                    setEditingClient(null)
                                    setModalOpen(true)
                                }}
                            >
                                + Add Client
                            </Button>
                        </Hint>
                    </div>

                    {/* Filters */}
                    <div className="max-w-xl space-y-1">
                        <div className="flex items-center gap-2 flex-nowrap">
              <span className="text-sm text-muted-foreground whitespace-nowrap shrink-0">
                Filter by:
              </span>

                            <Hint text={CLIENT_FILTERS[searchBy]?.tooltip ?? "Choose filter"}>
                                <div>
                                    <Select
                                        value={searchBy}
                                        onValueChange={(v) => {
                                            if (v === "name" || v === "email") {
                                                setSearchBy(v)
                                                setPage(0)
                                                setSearchError(null)
                                            }
                                        }}
                                    >
                                        <SelectTrigger
                                            className="
                        w-32 transition
                        focus-visible:ring-2 focus-visible:ring-indigo-500
                        data-[state=open]:ring-2 data-[state=open]:ring-indigo-500
                      "
                                        >
                                            <SelectValue />
                                        </SelectTrigger>

                                        <SelectContent
                                            side="bottom"
                                            align="start"
                                            sideOffset={4}
                                            avoidCollisions={false}
                                            position="popper"
                                        >
                                            {Object.entries(CLIENT_FILTERS).map(([key, meta]) => (
                                                <SelectItem key={key} value={key}>
                                                    {meta.label}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>
                            </Hint>

                            <Input
                                placeholder={placeholder}
                                value={searchTerm}
                                onChange={(e) => {
                                    const v = e.target.value
                                    setSearchTerm(v)
                                    setPage(0)
                                    setSearchError(validateSearch(v, searchBy))
                                }}
                                className="focus-visible:ring-2 focus-visible:ring-indigo-500 transition"
                            />

                            {searchTerm && (
                                <Hint text="Clear current search">
                                    <Button
                                        variant="outline"
                                        className="border-indigo-200 bg-indigo-50/40 text-indigo-700 hover:bg-indigo-50"
                                        onClick={() => {
                                            setSearchTerm("")
                                            setSearchError(null)
                                            setPage(0)
                                        }}
                                    >
                                        Clear
                                    </Button>
                                </Hint>
                            )}
                        </div>

                        {searchError && (
                            <p className="text-sm text-red-500">{searchError}</p>
                        )}
                    </div>
                </div>
            </div>

            {/* Content */}
            <div className="max-w-6xl mx-auto px-6 space-y-6">
                <ClientTable
                    clients={clients}
                    loading={loading}
                    page={page}
                    pageSize={pageSize}
                    onEdit={(c) => {
                        setEditingClient(c)
                        setModalOpen(true)
                    }}
                />

                <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
            </div>

            <ClientModal
                isOpen={modalOpen}
                initialData={editingClient}
                onClose={handleCloseModal}
                onSubmit={handleSubmit}
            />
        </div>
    )
}

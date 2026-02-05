import api from "@/services/api"
import { Client, ClientForm, PageResponse, ClientUpdateForm } from "./types"

export async function getClients(page: number, size: number): Promise<PageResponse<Client>> {
    const res = await api.post(`/client/get-all-paginated`, { page, size })
    return res.data
}

export async function filterClients(params: {
    page: number
    size: number
    name?: string
    email?: string
}): Promise<PageResponse<Client>> {
    const res = await api.post(`/client/search`, params)
    return res.data
}

export async function addClient(form: ClientForm) {
    const res = await api.post(`/client/add`, form)
    return res.data
}

export async function updateClient(form: ClientUpdateForm) {
    const res = await api.put(`/client/update`, form)
    return res.data
}

export async function getClientByEmail(email: string): Promise<Client> {
    const res = await api.get(`/client/get-by-email/${encodeURIComponent(email)}`)
    return res.data
}

import api from "@/services/api"
import { Client, ClientForm, PageResponse, ClientUpdateForm } from "./types"

export async function searchClients(params: {
    page: number
    size: number
    name?: string
    email?: string
}): Promise<PageResponse<Client>> {
    const res = await api.post(`/client/search`, params)
    return res.data
}

export async function addClient(form: ClientForm) {
    const res = await api.post(`/client`, form)
    return res.data
}

export async function updateClient(form: ClientUpdateForm) {
    const res = await api.put(`/client`, form)
    return res.data
}

export async function getClientByEmail(email: string): Promise<Client> {
    const res = await api.get(`/client/${encodeURIComponent(email)}`)
    return res.data
}

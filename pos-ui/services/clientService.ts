import { Client, ClientForm, PageResponse, ClientUpdateForm } from './types'

const BASE_URL = 'http://localhost:8080/api/client'

async function handleResponse<T>(res: Response): Promise<T> {
    if (!res.ok) {
        let message = 'Something went wrong'

        try {
            const errorBody = await res.json()
            if (typeof errorBody?.message === 'string') {
                message = errorBody.message
            }
        } catch {
            throw new Error(message)
        }

        throw new Error(message)
    }

    return res.json()
}

export async function getClients(
    page: number,
    size: number
): Promise<PageResponse<Client>> {
    const res = await fetch(`${BASE_URL}/get-all-paginated`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ page, size }),
    })

    return handleResponse(res)
}

export async function addClient(form: ClientForm) {
    const res = await fetch(`${BASE_URL}/add`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form), // ✅ correct
    })

    return handleResponse(res)
}

export async function updateClient(form: ClientUpdateForm) {
    const res = await fetch(`${BASE_URL}/update`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form), // ✅ FIXED
    })

    return handleResponse(res)
}

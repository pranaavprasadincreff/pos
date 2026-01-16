import {User, UserForm, PageResponse, UserUpdateForm} from './types';

const BASE_URL = 'http://localhost:8080/api/user';

export async function getUsers(
    page: number,
    size: number
): Promise<PageResponse<User>> {
    const res = await fetch(`${BASE_URL}/get-all-paginated`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ page, size }),
    });
    return res.json();
}

export async function addUser(form: UserForm) {
    const res = await fetch(`${BASE_URL}/add`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form),
    });

    return handleResponse(res);
}

export async function updateUser(form: UserUpdateForm) {
    const res = await fetch(`${BASE_URL}/update`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ form }),
    });

    return handleResponse(res);
}

async function handleResponse(res: Response) {
    if (!res.ok) {
        const error = await res.json();
        throw new Error(error.message || 'Request failed');
    }
    return res.json();
}

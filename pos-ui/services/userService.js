const BASE_URL = "http://localhost:8080/api/user";

export async function getUsers(page = 0, size = 10) {
    const res = await fetch(`${BASE_URL}/get-all-paginated`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ page, size }),
    });
    return res.json();
}

export async function addUser(user) {
    return fetch(`${BASE_URL}/add`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(user),
    }).then(res => res.json());
}

export async function updateUser(user) {
    return fetch(`${BASE_URL}/update`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(user),
    }).then(res => res.json());
}


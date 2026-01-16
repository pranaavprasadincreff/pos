export interface User {
    id: string;
    name: string;
    email: string;
}

export interface UserForm {
    name: string;
    email: string;
}

export interface UserUpdateForm {
    name: string;
    oldEmail: string;
    newEmail: string;
}

export interface PageResponse<T> {
    content: T[];
    totalPages: number;
}

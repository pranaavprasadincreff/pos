export type ClientFilterKey = "name" | "email"

export const CLIENT_FILTERS: Record<
    ClientFilterKey,
    { label: string; placeholder: string; tooltip?: string; apiField: "name" | "email" }
> = {
    name: {
        label: "Name",
        placeholder: "Search client name…",
        tooltip: "Filters clients by name",
        apiField: "name",
    },
    email: {
        label: "Email",
        placeholder: "Search client email…",
        tooltip: "Filters clients by email",
        apiField: "email",
    },
}

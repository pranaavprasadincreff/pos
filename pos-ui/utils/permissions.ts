// utils/permissions.ts
export type Role = "SUPERVISOR" | "OPERATOR"

// --------- Access maps (single source of truth) ---------

export const PAGE_ACCESS = {
    clients: ["SUPERVISOR", "OPERATOR"],
    products: ["SUPERVISOR", "OPERATOR"],
    orders: ["SUPERVISOR", "OPERATOR"],
    reports: ["SUPERVISOR"],
} as const

export const ACTION_ACCESS = {
    // ✅ Clients
    client_create: ["SUPERVISOR"],
    client_edit: ["SUPERVISOR"],

    // ✅ Products
    product_create: ["SUPERVISOR"],
    product_edit: ["SUPERVISOR"],
    product_bulk_upload: ["SUPERVISOR"],
    inventory_update: ["SUPERVISOR"],

    // ✅ Orders
    order_create: ["SUPERVISOR", "OPERATOR"],
    order_edit: ["SUPERVISOR", "OPERATOR"],
    order_retry: ["SUPERVISOR", "OPERATOR"],
    order_cancel: ["SUPERVISOR"],

    // ✅ Invoice
    invoice_generate: ["SUPERVISOR"],
    invoice_get: ["SUPERVISOR"],

    // ✅ Reports actions
    report_view: ["SUPERVISOR"],
} as const

// --------- Types derived from maps ---------

export type PageKey = keyof typeof PAGE_ACCESS
export type ActionKey = keyof typeof ACTION_ACCESS

// --------- Guards (runtime-safe) ---------

function isRole(v: unknown): v is Role {
    return v === "SUPERVISOR" || v === "OPERATOR"
}

function isActionKey(v: unknown): v is ActionKey {
    return typeof v === "string" && v in ACTION_ACCESS
}

function isPageKey(v: unknown): v is PageKey {
    return typeof v === "string" && v in PAGE_ACCESS
}

// --------- Public helpers ---------

export function canView(role: Role | null | undefined, page: PageKey): boolean {
    if (!role) return false
    // page is typed, but still defensive
    const allowed = (PAGE_ACCESS as Record<string, readonly Role[]>)[page]
    return Array.isArray(allowed) ? allowed.includes(role) : false
}

export function can(role: Role | null | undefined, action: ActionKey): boolean {
    if (!role) return false
    // action is typed, but still defensive
    const allowed = (ACTION_ACCESS as Record<string, readonly Role[]>)[action]
    return Array.isArray(allowed) ? allowed.includes(role) : false
}

export function canSafe(role: unknown, action: unknown): boolean {
    if (!isRole(role)) return false
    if (!isActionKey(action)) return false
    return ACTION_ACCESS[action].includes(role)
}

export function canViewSafe(role: unknown, page: unknown): boolean {
    if (!isRole(role)) return false
    if (!isPageKey(page)) return false
    return PAGE_ACCESS[page].includes(role)
}

export function getSessionRole(): Role | null {
    if (typeof window === "undefined") return null
    const raw = sessionStorage.getItem("auth.role")
    return isRole(raw) ? raw : null
}

export function getSessionEmail(): string {
    if (typeof window === "undefined") return ""
    return sessionStorage.getItem("auth.email") || ""
}

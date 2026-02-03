import { Navigate } from "react-router-dom"

export default function AuthGuard({ allowed, children }) {
    const role = sessionStorage.getItem("auth.role")

    if (!role) return <Navigate to="/login" />
    if (!allowed.includes(role)) return <Navigate to="/orders" />

    return children
}

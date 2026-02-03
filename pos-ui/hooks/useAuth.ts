export function useAuth() {
    const token = sessionStorage.getItem("auth.token")
    const role = sessionStorage.getItem("auth.role")
    const lastCheck = Number(sessionStorage.getItem("auth.lastCheck") || 0)

    const isLoggedIn = !!token
    const needsRefresh = Date.now() - lastCheck > 5 * 60 * 1000

    return { token, role, isLoggedIn, needsRefresh }
}

import axios from "axios"

const api = axios.create({
  baseURL: "http://localhost:8080/api",
})

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem("auth.token")
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err?.response?.status === 401) {
      sessionStorage.clear()
      window.location.href = "/login"
      return Promise.reject(err)
    }

    const data = err?.response?.data
    const backendMessage =
      (data && typeof data === "object" && data.message) ||
      (typeof data === "string" ? data : null)

    const message = backendMessage || err?.message || "Something went wrong"

    const status = err?.response?.status

    const normalizedError = new Error(message) as Error & { status?: number }
    normalizedError.status = status

    return Promise.reject(normalizedError)
  }
)

export default api

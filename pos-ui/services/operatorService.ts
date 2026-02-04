import api from "@/services/api"
import { AuthUserData, CreateOperatorForm } from "@/services/types"

export async function listOperators(): Promise<AuthUserData[]> {
    const res = await api.get(`/supervisor/operators`)
    return res.data
}

export async function createOperator(form: CreateOperatorForm): Promise<AuthUserData> {
    const res = await api.post(`/supervisor/operators`, form)
    return res.data
}

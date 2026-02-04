"use client"

import { useEffect, useMemo, useState } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { toast } from "sonner"
import { Hint } from "@/components/shared/Hint"
import { can, Role } from "@/utils/permissions"

import OperatorTable from "@/components/operators/OperatorTable"
import CreateOperatorModal from "@/components/operators/CreateOperatorModal"
import { createOperator, listOperators } from "@/services/operatorService"
import { AuthUserData } from "@/services/types"

export default function OperatorsPage() {
    const router = useRouter()

    const [operators, setOperators] = useState<AuthUserData[]>([])
    const [loading, setLoading] = useState(false)
    const [modalOpen, setModalOpen] = useState(false)

    const [role, setRole] = useState<Role | null>(null)
    useEffect(() => {
        const r = (sessionStorage.getItem("auth.role") as Role | null) ?? null
        setRole(r)

        // ✅ hard guard: operators should never see this page
        if (r !== "SUPERVISOR") router.replace("/")
    }, [router])

    const canManageOperators = useMemo(() => {
        if (!role) return false
        return role === "SUPERVISOR" && can(role, "operator_create")
    }, [role])

    async function fetchOperators() {
        let toastId: string | number | undefined
        try {
            setLoading(true)
            toastId = toast.loading("Loading operators...")
            const res = await listOperators()
            setOperators(res)
        } finally {
            setLoading(false)
            if (toastId) toast.dismiss(toastId)
        }
    }

    useEffect(() => {
        if (role === "SUPERVISOR") fetchOperators()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [role])

    async function handleCreate(form: { email: string; password: string }) {
        let toastId: string | number | undefined
        try {
            toastId = toast.loading("Creating operator...")
            await createOperator(form)
            toast.success("Operator created successfully")
            setModalOpen(false)
            fetchOperators()
        } catch (e: any) {
            throw new Error(
                e?.response?.data?.message ?? e?.message ?? "Failed to create operator"
            )
        } finally {
            if (toastId) toast.dismiss(toastId)
        }
    }

    // While redirecting, render nothing (prevents flash)
    if (role !== "SUPERVISOR") return null

    return (
        <div className="space-y-6">
            {/* Sticky Header */}
            <div className="sticky top-0 z-20 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80">
                <div className="max-w-6xl mx-auto px-6 py-4 space-y-4">
                    <div className="flex justify-between items-center min-h-[44px]">
                        <div>
                            <h1 className="text-2xl font-semibold">Operators</h1>
                            <p className="text-sm text-muted-foreground">
                                Create operator logins for POS usage
                            </p>
                        </div>

                        <div className="min-h-[40px] flex items-center">
                            {canManageOperators ? (
                                <Hint text="Add a new operator">
                                    <Button
                                        className="bg-indigo-600 hover:bg-indigo-700 h-10"
                                        onClick={() => setModalOpen(true)}
                                    >
                                        + Add Operator
                                    </Button>
                                </Hint>
                            ) : (
                                <div className="h-10 w-[140px]" />
                            )}
                        </div>
                    </div>
                </div>
            </div>

            {/* Content */}
            <div className="max-w-6xl mx-auto px-6 space-y-6">
                <OperatorTable operators={operators} loading={loading} />
            </div>

            {canManageOperators && (
                <CreateOperatorModal
                    isOpen={modalOpen}
                    onClose={() => setModalOpen(false)}
                    onSubmit={handleCreate}
                />
            )}
        </div>
    )
}

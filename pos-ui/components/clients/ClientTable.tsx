import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"
import { Button } from "@/components/ui/button"
import { Client } from "@/services/types"
import { Users } from "lucide-react"
import { LoadingIndicator } from "@/components/ui/LoadingIndicator"
import { Hint } from "@/components/shared/Hint"

interface Props {
    clients: Client[]
    loading: boolean
    page: number
    pageSize: number
    onEdit: (client: Client) => void
    canManage: boolean
}

export default function ClientTable({
                                        clients,
                                        loading,
                                        page,
                                        pageSize,
                                        onEdit,
                                        canManage,
                                    }: Props) {
    return (
        <div className="rounded-lg border bg-background">
            <Table>
                <TableHeader>
                    {/* ✅ fixed header height */}
                    <TableRow className="h-12">
                        <TableHead className="align-middle">S No</TableHead>
                        <TableHead className="align-middle">Name</TableHead>
                        <TableHead className="align-middle">Email</TableHead>
                        <TableHead className="text-right align-middle">Actions</TableHead>
                    </TableRow>
                </TableHeader>

                <TableBody>
                    {loading ? (
                        <TableRow className="h-12">
                            <TableCell colSpan={4} className="py-24 text-center">
                                <LoadingIndicator />
                            </TableCell>
                        </TableRow>
                    ) : clients.length === 0 ? (
                        <TableRow className="h-12">
                            <TableCell colSpan={4}>
                                <div className="flex flex-col items-center gap-2 py-12 text-muted-foreground">
                                    <Users className="h-10 w-10" />
                                    <p className="font-medium">No clients found</p>
                                    <p className="text-sm">
                                        Try adjusting your search{canManage ? " or add a new client" : ""}
                                    </p>
                                </div>
                            </TableCell>
                        </TableRow>
                    ) : (
                        clients.map((c, index) => (
                            // ✅ fixed row height, always
                            <TableRow key={c.email} className="h-12 hover:bg-slate-50 transition-colors">
                                <TableCell className="align-middle">
                                    {page * pageSize + index + 1}
                                </TableCell>

                                <TableCell className="align-middle font-medium text-slate-900">
                                    {c.name}
                                </TableCell>

                                <TableCell className="align-middle text-slate-500">
                                    {c.email}
                                </TableCell>

                                <TableCell className="text-right align-middle">
                                    {/* ✅ lock actions cell height */}
                                    <div className="min-h-[32px] flex items-center justify-end">
                                        {canManage ? (
                                            <Hint text="Edit this client">
                                                <Button
                                                    size="sm"
                                                    variant="ghost"
                                                    className="h-8 text-indigo-600 hover:text-indigo-700 hover:bg-indigo-50"
                                                    onClick={() => onEdit(c)}
                                                >
                                                    Edit
                                                </Button>
                                            </Hint>
                                        ) : (
                                            <span className="text-muted-foreground"></span>
                                        )}
                                    </div>
                                </TableCell>
                            </TableRow>
                        ))
                    )}
                </TableBody>
            </Table>
        </div>
    )
}

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
}

export default function ClientTable({
                                        clients,
                                        loading,
                                        page,
                                        pageSize,
                                        onEdit,
                                    }: Props) {
    return (
        <div className="rounded-lg border bg-background">
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead>S No</TableHead>
                        <TableHead>Name</TableHead>
                        <TableHead>Email</TableHead>
                        <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                </TableHeader>

                <TableBody>
                    {loading ? (
                        <TableRow>
                            <TableCell colSpan={4} className="py-24 text-center">
                                <LoadingIndicator />
                            </TableCell>
                        </TableRow>
                    ) : clients.length === 0 ? (
                        <TableRow>
                            <TableCell colSpan={4}>
                                <div className="flex flex-col items-center gap-2 py-12 text-muted-foreground">
                                    <Users className="h-10 w-10" />
                                    <p className="font-medium">No clients found</p>
                                    <p className="text-sm">Try adjusting your search or add a new client</p>
                                </div>
                            </TableCell>
                        </TableRow>
                    ) : (
                        clients.map((c, index) => (
                            <TableRow key={c.email} className="hover:bg-slate-50 transition-colors">
                                <TableCell>{page * pageSize + index + 1}</TableCell>
                                <TableCell className="font-medium text-slate-900">{c.name}</TableCell>
                                <TableCell className="text-slate-500">{c.email}</TableCell>
                                <TableCell className="text-right">
                                    <Hint text="Edit this client">
                                        <Button
                                            size="sm"
                                            variant="ghost"
                                            className="text-indigo-600 hover:text-indigo-700 hover:bg-indigo-50"
                                            onClick={() => onEdit(c)}
                                        >
                                            Edit
                                        </Button>
                                    </Hint>
                                </TableCell>
                            </TableRow>
                        ))
                    )}
                </TableBody>
            </Table>
        </div>
    )
}

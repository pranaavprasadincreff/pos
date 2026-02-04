import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"
import { Users } from "lucide-react"
import { LoadingIndicator } from "@/components/ui/LoadingIndicator"
import { AuthUserData } from "@/services/types"

interface Props {
    operators: AuthUserData[]
    loading: boolean
}

export default function OperatorTable({ operators, loading }: Props) {
    return (
        <div className="rounded-lg border bg-background">
            <Table>
                <TableHeader>
                    <TableRow className="h-12">
                        <TableHead className="align-middle">S No</TableHead>
                        <TableHead className="align-middle">Email</TableHead>
                        <TableHead className="align-middle">Role</TableHead>
                    </TableRow>
                </TableHeader>

                <TableBody>
                    {loading ? (
                        <TableRow className="h-12">
                            <TableCell colSpan={3} className="py-24 text-center">
                                <LoadingIndicator />
                            </TableCell>
                        </TableRow>
                    ) : operators.length === 0 ? (
                        <TableRow className="h-12">
                            <TableCell colSpan={3}>
                                <div className="flex flex-col items-center gap-2 py-12 text-muted-foreground">
                                    <Users className="h-10 w-10" />
                                    <p className="font-medium">No operators found</p>
                                    <p className="text-sm">Create one to allow logins</p>
                                </div>
                            </TableCell>
                        </TableRow>
                    ) : (
                        operators.map((u, idx) => (
                            <TableRow key={u.email} className="h-12 hover:bg-slate-50 transition-colors">
                                <TableCell className="align-middle">{idx + 1}</TableCell>
                                <TableCell className="align-middle font-medium text-slate-900">
                                    {u.email}
                                </TableCell>
                                <TableCell className="align-middle text-slate-500">
                                    {u.role}
                                </TableCell>
                            </TableRow>
                        ))
                    )}
                </TableBody>
            </Table>
        </div>
    )
}

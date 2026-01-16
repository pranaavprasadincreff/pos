import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/components/ui/table'
import { Button } from '@/components/ui/button'
import { User } from '@/services/types'
import { Users } from 'lucide-react'
import {LoadingIndicator} from "@/components/ui/LoadingIndicator";

interface Props {
    clients: User[]
    loading: boolean
    page: number
    pageSize: number
    onEdit: (client: User) => void
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
                        {/*<TableHead className="text-right">Action</TableHead>*/}
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
                                    <p className="text-sm">
                                        Try adjusting your search or add a new client
                                    </p>
                                </div>
                            </TableCell>
                        </TableRow>
                    ) : (
                        clients.map((c, index) => (
                            <TableRow key={c.email} className="hover:bg-muted/40 transition-colors">
                                <TableCell >{page * pageSize + index + 1}</TableCell>
                                <TableCell className="font-medium text-gray-900">{c.name}</TableCell>
                                <TableCell className="text-muted-foreground">{c.email}</TableCell>
                                <TableCell className="text-right">
                                    <Button
                                        size="sm"
                                        variant="ghost"
                                        onClick={() => onEdit(c)}
                                    >
                                        Edit
                                    </Button>
                                </TableCell>
                            </TableRow>
                        ))
                    )}
                </TableBody>
            </Table>
        </div>
    )
}

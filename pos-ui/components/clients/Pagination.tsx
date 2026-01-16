import { Button } from '@/components/ui/button'

interface Props {
    page: number
    totalPages: number
    onPageChange: (page: number) => void
}

export default function Pagination({
                                       page,
                                       totalPages,
                                       onPageChange,
                                   }: Props) {
    if (totalPages <= 1) return null

    return (
        <div className="flex justify-end gap-2">
            <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => onPageChange(page - 1)}
            >
                Previous
            </Button>

            <span className="px-2 text-sm self-center">
        Page {page + 1} of {totalPages}
      </span>

            <Button
                variant="outline"
                size="sm"
                disabled={page + 1 >= totalPages}
                onClick={() => onPageChange(page + 1)}
            >
                Next
            </Button>
        </div>
    )
}

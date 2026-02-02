"use client"

import { useEffect, useMemo, useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Hint } from "@/components/shared/Hint"
import { cn } from "@/lib/utils"

interface Props {
    page: number // 0-indexed
    totalPages: number
    onPageChange: (page: number) => void
}

export default function Pagination({ page, totalPages, onPageChange }: Props) {
    const [pageInput, setPageInput] = useState<string>("")

    useEffect(() => {
        setPageInput(String(page + 1))
    }, [page])

    const isPrevDisabled = page === 0
    const isNextDisabled = page + 1 >= totalPages

    const prevTooltip = useMemo(
        () => (isPrevDisabled ? "Already on first page" : "Go to previous page"),
        [isPrevDisabled]
    )
    const nextTooltip = useMemo(
        () => (isNextDisabled ? "Already on last page" : "Go to next page"),
        [isNextDisabled]
    )

    if (totalPages <= 1) return null

    function commitPageJump() {
        const trimmed = pageInput.trim()
        if (!trimmed) {
            setPageInput(String(page + 1))
            return
        }

        const num = Number(trimmed)
        if (!Number.isFinite(num) || !Number.isInteger(num)) {
            setPageInput(String(page + 1))
            return
        }

        let nextHuman = num
        if (nextHuman < 1) nextHuman = 1
        if (nextHuman > totalPages) nextHuman = totalPages

        const nextPage = nextHuman - 1
        setPageInput(String(nextHuman))
        if (nextPage !== page) onPageChange(nextPage)
    }

    return (
        <div className="flex justify-end items-center gap-3">
            <Hint text={prevTooltip}>
                <Button
                    variant="outline"
                    size="sm"
                    className={cn(
                        "w-24 border-indigo-200 bg-indigo-50/40",
                        "hover:bg-indigo-50 hover:border-indigo-300 transition-colors hover:text-indigo-700"
                    )}
                    disabled={isPrevDisabled}
                    onClick={() => onPageChange(page - 1)}
                >
                    Previous
                </Button>
            </Hint>

            <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <span>Page</span>

                <Hint text={`Enter a page number (1–${totalPages}) and press Enter`}>
                    <Input
                        value={pageInput}
                        onChange={(e) => setPageInput(e.target.value.replace(/[^\d]/g, ""))}
                        onBlur={commitPageJump}
                        onKeyDown={(e) => {
                            if (e.key === "Enter") {
                                e.currentTarget.blur()
                                commitPageJump()
                            } else if (e.key === "Escape") {
                                setPageInput(String(page + 1))
                                e.currentTarget.blur()
                            }
                        }}
                        className="h-7 w-9 px-1 text-center text-xs"
                        inputMode="numeric"
                        aria-label="Page number"
                    />
                </Hint>

                <span>of</span>
                <span className="tabular-nums text-slate-700">{totalPages}</span>
            </div>

            <Hint text={nextTooltip}>
                <Button
                    variant="outline"
                    size="sm"
                    className={cn(
                        "w-24 border-indigo-200 bg-indigo-50/40",
                        "hover:bg-indigo-50 hover:border-indigo-300 transition-colors hover:text-indigo-700"
                    )}
                    disabled={isNextDisabled}
                    onClick={() => onPageChange(page + 1)}
                >
                    Next
                </Button>
            </Hint>
        </div>
    )
}

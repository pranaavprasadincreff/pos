'use client'

import React from 'react'

export interface ParsedRow {
    values: string[]
    isError: boolean
}

interface Props {
    headers: string[]
    rows: ParsedRow[]
}

export default function TsvPreviewTable({ headers, rows }: Props) {
    return (
        <table className="w-full border-collapse text-sm">
            <thead className="sticky top-0 bg-gray-50 z-10">
            <tr>
                {headers.map((header, i) => (
                    <th
                        key={i}
                        className="border px-2 py-1 text-left bg-gray-50 font-medium"
                    >
                        {header}
                    </th>
                ))}
            </tr>
            </thead>
            <tbody>
            {rows.map((row, idx) => (
                <tr
                    key={idx}
                    className={row.isError ? 'bg-red-100' : 'bg-white'}
                >
                    {row.values.map((cell, i) => (
                        <td key={i} className="border px-2 py-1">
                            {cell}
                        </td>
                    ))}
                </tr>
            ))}
            </tbody>
        </table>
    )
}

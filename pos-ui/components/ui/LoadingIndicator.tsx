'use client'

import { useEffect, useState } from 'react'

const messages = [
    'Loading clients…',
    'Fetching data…',
    'Almost there…',
]

export function LoadingIndicator() {
    const [index, setIndex] = useState(0)

    useEffect(() => {
        const id = setInterval(() => {
            setIndex(i => (i + 1) % messages.length)
        }, 900)

        return () => clearInterval(id)
    }, [])

    return (
        <div className="flex flex-col items-center gap-3 text-muted-foreground">
            <div className="h-6 w-6 animate-spin rounded-full border-2 border-muted border-t-primary" />
            <span className="text-sm transition-opacity">
        {messages[index]}
      </span>
        </div>
    )
}
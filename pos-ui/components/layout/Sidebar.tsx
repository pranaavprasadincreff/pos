'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { Home, Users } from 'lucide-react'
import { cn } from '@/lib/utils'

const navItems = [
    {
        label: 'Home',
        href: '/',
        icon: Home,
    },
    {
        label: 'Clients',
        href: '/clients',
        icon: Users,
    },
]

export default function Sidebar() {
    const pathname = usePathname()

    return (
        <aside className="w-64 border-r bg-background">
            <div className="p-6 text-lg font-semibold">POS Admin</div>

            <nav className="space-y-1 px-2">
                {navItems.map((item) => {
                    const isActive = pathname === item.href
                    const Icon = item.icon

                    return (
                        <Link
                            key={item.href}
                            href={item.href}
                            className={cn(
                                'flex items-center gap-3 rounded-md px-3 py-2 text-sm transition-colors',
                                isActive
                                    ? 'bg-muted font-medium'
                                    : 'text-muted-foreground hover:bg-muted'
                            )}
                        >
                            <Icon className="h-4 w-4" />
                            {item.label}
                        </Link>
                    )
                })}
            </nav>
        </aside>
    )
}

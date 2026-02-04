"use client"

import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import {
    Home,
    Package,
    ShoppingCart,
    Users,
    BarChart3,
    LogOut,
    UserCircle,
} from "lucide-react"
import { cn } from "@/lib/utils"
import { canView, Role, PAGE_ACCESS } from "@/utils/permissions"
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogTrigger,
} from "@/components/ui/alert-dialog"

type PageKey = keyof typeof PAGE_ACCESS

type NavItem =
    | { label: string; href: string; icon: any; pageKey: null }
    | { label: string; href: string; icon: any; pageKey: PageKey }

const NAV_ITEMS = [
    { label: "Home", href: "/", icon: Home, pageKey: null },
    { label: "Clients", href: "/clients", icon: Users, pageKey: "clients" },
    { label: "Products", href: "/products", icon: Package, pageKey: "products" },
    { label: "Orders", href: "/orders", icon: ShoppingCart, pageKey: "orders" },
    { label: "Sales Reports", href: "/reports", icon: BarChart3, pageKey: "reports" },
    { label: "Operators", href: "/supervisor/operators", icon: Users, pageKey: "operators" },
] satisfies NavItem[]

export default function Sidebar() {
    const pathname = usePathname()
    const router = useRouter()

    const email = sessionStorage.getItem("auth.email") || ""
    const role = (sessionStorage.getItem("auth.role") || "") as Role

    const navItems = NAV_ITEMS.filter(
        (item) => item.pageKey === null || canView(role, item.pageKey)
    )

    function doLogout() {
        sessionStorage.clear()
        router.replace("/login")
    }

    return (
        <aside className="w-64 h-screen shrink-0 border-r bg-background sticky top-0 flex flex-col">
            {/* Brand */}
            <div className="p-6 text-lg font-semibold">POS Console</div>

            {/* Navigation */}
            <nav className="space-y-1 px-2 flex-1">
                {navItems.map((item) => {
                    const isActive = pathname === item.href
                    const Icon = item.icon

                    return (
                        <Link
                            key={item.href}
                            href={item.href}
                            className={cn(
                                "flex items-center gap-3 rounded-md px-3 py-2 text-sm transition-colors",
                                isActive
                                    ? "bg-muted font-medium text-foreground"
                                    : "text-muted-foreground hover:bg-muted hover:text-foreground"
                            )}
                        >
                            <Icon className="h-4 w-4" />
                            {item.label}
                        </Link>
                    )
                })}
            </nav>

            {/* Bottom area */}
            <div className="p-3 border-t space-y-3">
                {/* User block */}
                <div className="rounded-lg border bg-card px-3 py-3">
                    <div className="flex items-center gap-2">
                        <UserCircle className="h-5 w-5 text-muted-foreground shrink-0" />
                        <div className="text-sm font-medium text-foreground truncate">
                            {email}
                        </div>
                    </div>

                    <div className="mt-2">
                        <span
                            className={cn(
                                "inline-flex items-center rounded-full px-3 py-1.5 text-sm font-semibold",
                                "ring-1 ring-inset",
                                role === "SUPERVISOR"
                                    ? "bg-emerald-100 text-emerald-900 ring-emerald-200"
                                    : "bg-sky-100 text-sky-900 ring-sky-200"
                            )}
                        >
                            {role === "SUPERVISOR" ? "Supervisor" : "Operator"}
                        </span>
                    </div>
                </div>

                {/* Logout */}
                <AlertDialog>
                    <AlertDialogTrigger asChild>
                        <button
                            className="w-full flex items-center justify-center gap-2 rounded-md border px-3 py-2
                                       text-sm font-medium text-red-600 border-red-200
                                       hover:bg-red-50 hover:text-red-700 transition-colors"
                        >
                            <LogOut className="h-4 w-4" />
                            Logout
                        </button>
                    </AlertDialogTrigger>

                    <AlertDialogContent>
                        <AlertDialogHeader>
                            <AlertDialogTitle>Sign out?</AlertDialogTitle>
                            <AlertDialogDescription>
                                Are you sure you want to Logout?
                            </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                            <AlertDialogCancel>Cancel</AlertDialogCancel>
                            <AlertDialogAction
                                onClick={doLogout}
                                className="bg-red-600 hover:bg-red-700"
                            >
                                Logout
                            </AlertDialogAction>
                        </AlertDialogFooter>
                    </AlertDialogContent>
                </AlertDialog>
            </div>
        </aside>
    )
}

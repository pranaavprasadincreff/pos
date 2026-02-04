"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import { Card, CardContent } from "@/components/ui/card"
import { Users, Package, ShoppingCart, BarChart3, UserPlus } from "lucide-react"
import { canView, Role } from "@/utils/permissions"

export default function HomePage() {
    const [role, setRole] = useState<Role | null>(null)

    useEffect(() => {
        setRole((sessionStorage.getItem("auth.role") as Role | null) ?? null)
    }, [])

    return (
        <div className="max-w-5xl mx-auto py-20 space-y-8">
            <div className="text-center space-y-2">
                <h1 className="text-3xl font-semibold tracking-tight">POS Console</h1>
                <p className="text-muted-foreground">
                    Manage clients, products, orders and sales insights
                </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                {canView(role, "clients") && (
                    <Link href="/clients">
                        <Card className="hover:shadow-md transition cursor-pointer">
                            <CardContent className="flex items-center gap-4 p-6">
                                <Users className="h-8 w-8 text-indigo-600" />
                                <div>
                                    <p className="font-medium">Clients</p>
                                    <p className="text-sm text-muted-foreground">
                                        Manage registered clients
                                    </p>
                                </div>
                            </CardContent>
                        </Card>
                    </Link>
                )}

                {canView(role, "products") && (
                    <Link href="/products">
                        <Card className="hover:shadow-md transition cursor-pointer">
                            <CardContent className="flex items-center gap-4 p-6">
                                <Package className="h-8 w-8 text-emerald-600" />
                                <div>
                                    <p className="font-medium">Products</p>
                                    <p className="text-sm text-muted-foreground">
                                        Manage products & inventory
                                    </p>
                                </div>
                            </CardContent>
                        </Card>
                    </Link>
                )}

                {canView(role, "orders") && (
                    <Link href="/orders">
                        <Card className="hover:shadow-md transition cursor-pointer">
                            <CardContent className="flex items-center gap-4 p-6">
                                <ShoppingCart className="h-8 w-8 text-orange-500" />
                                <div>
                                    <p className="font-medium">Orders</p>
                                    <p className="text-sm text-muted-foreground">
                                        Create and manage orders
                                    </p>
                                </div>
                            </CardContent>
                        </Card>
                    </Link>
                )}

                {canView(role, "reports") && (
                    <Link href="/reports">
                        <Card className="hover:shadow-md transition cursor-pointer">
                            <CardContent className="flex items-center gap-4 p-6">
                                <BarChart3 className="h-8 w-8 text-yellow-500" />
                                <div>
                                    <p className="font-medium">Sales Reports</p>
                                    <p className="text-sm text-muted-foreground">
                                        Daily and range-wise analytics
                                    </p>
                                </div>
                            </CardContent>
                        </Card>
                    </Link>
                )}

                {/* ✅ Supervisor-only card, centered under the grid */}
                {canView(role, "operators") && (
                    <div className="sm:col-span-2 flex justify-center">
                        <Link href="/supervisor/operators" className="w-full sm:max-w-md">
                            <Card className="hover:shadow-md transition cursor-pointer">
                                <CardContent className="flex items-center gap-4 p-6">
                                    <UserPlus className="h-8 w-8 text-pink-600" />
                                    <div>
                                        <p className="font-medium">Operators</p>
                                        <p className="text-sm text-muted-foreground">
                                            Create operator logins and manage access
                                        </p>
                                    </div>
                                </CardContent>
                            </Card>
                        </Link>
                    </div>
                )}
            </div>
        </div>
    )
}

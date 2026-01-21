'use client'

import Link from 'next/link'
import { Card, CardContent } from '@/components/ui/card'
import { Users, Package } from 'lucide-react'

export default function HomePage() {
    return (
        <div className="max-w-5xl mx-auto py-20 space-y-8">
            <div className="text-center space-y-2">
                <h1 className="text-3xl font-semibold tracking-tight">
                    POS Dashboard
                </h1>
                <p className="text-muted-foreground">
                    Manage clients, products and inventory
                </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
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
            </div>
        </div>
    )
}

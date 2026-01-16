import Link from 'next/link'
import { Card, CardContent } from '@/components/ui/card'
import { Users } from 'lucide-react'

export default function HomePage() {
  return (
      <div className="space-y-6">
        <h1 className="text-2xl font-semibold">Welcome</h1>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Link href="/clients">
            <Card className="cursor-pointer hover:shadow-md transition-shadow">
              <CardContent className="flex items-center gap-4 p-6">
                <Users className="h-8 w-8 text-muted-foreground" />
                <div>
                  <p className="font-medium">Clients</p>
                  <p className="text-sm text-muted-foreground">
                    Manage your clients
                  </p>
                </div>
              </CardContent>
            </Card>
          </Link>
        </div>
      </div>
  )
}

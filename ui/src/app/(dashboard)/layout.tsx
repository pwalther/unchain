import { Sidebar } from "@/components/sidebar"

export default function DashboardLayout({
    children,
}: {
    children: React.ReactNode
}) {
    return (
        <div className="flex h-full overflow-hidden bg-background">
            <Sidebar />
            <main className="flex-1 overflow-y-auto overflow-x-hidden">
                <div className="container mx-auto max-w-7xl px-4 py-8 md:px-8">
                    {children}
                </div>
            </main>
        </div>
    )
}

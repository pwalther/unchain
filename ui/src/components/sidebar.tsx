"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { cn } from "@/lib/utils"
import {
    LayoutGrid,
    ClipboardList,
    Folder,
    Zap,
    Users,
    Globe,
    Settings,
    ChevronLeft,
    ChevronRight,
    Command
} from "lucide-react"
import { useState } from "react"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/hooks/use-auth"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { LogIn, LogOut } from "lucide-react"
import { getApiUrl } from "@/lib/api"

const navItems = [
    { name: "Features", href: "/features", icon: LayoutGrid },
    { name: "Change Requests", href: "/change-requests", icon: ClipboardList },
    { name: "Projects", href: "/projects", icon: Folder },
    { name: "Strategies", href: "/strategies", icon: Zap },
    // { name: "Segments", href: "/segments", icon: Users },
    { name: "Environments", href: "/environments", icon: Globe },
]

export function Sidebar() {
    const pathname = usePathname()
    const [collapsed, setCollapsed] = useState(false)
    const { data: user } = useAuth()

    return (
        <aside
            className={cn(
                "flex flex-col border-r bg-sidebar transition-all duration-300 ease-in-out",
                collapsed ? "w-[70px]" : "w-[260px]"
            )}
        >
            <div className="flex h-16 items-center px-6 border-b">
                <div className="flex items-center gap-3 overflow-hidden">
                    <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground shrink-0">
                        <Command className="h-5 w-5" />
                    </div>
                    {!collapsed && (
                        <span className="text-lg font-bold tracking-tight whitespace-nowrap">Unchain</span>
                    )}
                </div>
            </div>

            <nav className="flex-1 space-y-1 p-3">
                {navItems.map((item) => {
                    const isActive = pathname.startsWith(item.href)
                    return (
                        <Link
                            key={item.name}
                            href={item.href}
                            className={cn(
                                "flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all group",
                                isActive
                                    ? "bg-primary text-primary-foreground shadow-sm"
                                    : "text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
                            )}
                        >
                            <item.icon className={cn(
                                "h-5 w-5 shrink-0 transition-colors",
                                isActive ? "text-primary-foreground" : "text-muted-foreground group-hover:text-sidebar-accent-foreground"
                            )} />
                            {!collapsed && (
                                <span className="flex-1 opacity-100 transition-opacity duration-300">
                                    {item.name}
                                </span>
                            )}
                        </Link>
                    )
                })}
            </nav>

            <div className="p-3 border-t">
                {user?.authenticated ? (
                    <div className={cn(
                        "flex items-center gap-3 rounded-xl px-3 py-2.5 mb-2 transition-all bg-sidebar-accent/50",
                        collapsed ? "justify-center px-0" : ""
                    )}>
                        <Avatar className="h-8 w-8 shrink-0">
                            <AvatarImage src={user.picture} alt={user.name} />
                            <AvatarFallback>{user.username?.charAt(0).toUpperCase()}</AvatarFallback>
                        </Avatar>
                        {!collapsed && (
                            <div className="flex-1 overflow-hidden">
                                <p className="text-sm font-medium truncate">{user.name}</p>
                                <p className="text-xs text-muted-foreground truncate">{user.email}</p>
                            </div>
                        )}
                    </div>
                ) : (
                    <Button
                        variant="outline"
                        className={cn("w-full mb-2 gap-2", collapsed ? "px-0" : "justify-start")}
                        onClick={() => window.location.href = `${getApiUrl()}/oauth2/authorization/oidc`}
                    >
                        <LogIn className="h-4 w-4 shrink-0" />
                        {!collapsed && <span>Sign In</span>}
                    </Button>
                )}

                <Link
                    href="/settings"
                    className={cn(
                        "flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all group",
                        pathname.startsWith("/settings")
                            ? "bg-primary text-primary-foreground"
                            : "text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
                    )}
                >
                    <Settings className="h-5 w-5 shrink-0 text-muted-foreground group-hover:text-sidebar-accent-foreground" />
                    {!collapsed && <span>Settings</span>}
                </Link>
                <div className="mt-2 flex justify-end px-1">
                    <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 rounded-lg"
                        onClick={() => setCollapsed(!collapsed)}
                    >
                        {collapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
                    </Button>
                </div>
            </div>
        </aside >
    )
}

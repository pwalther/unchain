"use client"

import { useTheme } from "next-themes"
import { Moon, Sun, Monitor, Bell, Zap, User as UserIcon, Mail } from "lucide-react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import { Separator } from "@/components/ui/separator"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/hooks/use-auth"
import { Skeleton } from "@/components/ui/skeleton"

export default function SettingsPage() {
    const { theme, setTheme } = useTheme()
    const { data: user, isLoading } = useAuth()

    return (
        <div className="container max-w-4xl py-10 space-y-10 animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div className="space-y-1">
                <h1 className="text-3xl font-bold tracking-tight">Settings</h1>
                <p className="text-muted-foreground">Manage your application preferences and account settings.</p>
            </div>

            <div className="grid gap-8">
                {/* Visual Settings */}
                <Card className="border-none shadow-xl bg-card/50 backdrop-blur-sm overflow-hidden">
                    <div className="absolute top-0 right-0 p-6 opacity-5">
                        <Moon className="h-24 w-24 translate-x-8 -translate-y-8" />
                    </div>
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2">
                            <Sun className="h-5 w-5 text-primary" />
                            Appearance
                        </CardTitle>
                        <CardDescription>Customize how Unchain looks on your device.</CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-6">
                        <div className="flex items-center justify-between">
                            <div className="space-y-1">
                                <Label className="text-base">Dark Mode</Label>
                                <p className="text-sm text-muted-foreground">Toggle between light and dark themes.</p>
                            </div>
                            <Switch
                                checked={theme === "dark"}
                                onCheckedChange={(checked) => setTheme(checked ? "dark" : "light")}
                            />
                        </div>

                        <Separator />

                        <div className="flex items-center justify-between opacity-60">
                            <div className="space-y-1">
                                <Label className="text-base">Follow System Preference</Label>
                                <p className="text-sm text-muted-foreground">Automatically adjust theme based on your OS settings.</p>
                            </div>
                            <Switch
                                checked={theme === "system"}
                                onCheckedChange={(checked) => checked && setTheme("system")}
                            />
                        </div>

                        <div className="grid grid-cols-3 gap-4 pt-2">
                            <Button
                                variant={theme === 'light' ? 'default' : 'outline'}
                                className="flex flex-col gap-2 h-24 rounded-2xl transition-all hover:scale-105"
                                onClick={() => setTheme('light')}
                            >
                                <Sun className="h-6 w-6" />
                                <span className="text-xs font-semibold">Light</span>
                            </Button>
                            <Button
                                variant={theme === 'dark' ? 'default' : 'outline'}
                                className="flex flex-col gap-2 h-24 rounded-2xl transition-all hover:scale-105"
                                onClick={() => setTheme('dark')}
                            >
                                <Moon className="h-6 w-6" />
                                <span className="text-xs font-semibold">Dark</span>
                            </Button>
                            <Button
                                variant={theme === 'system' ? 'default' : 'outline'}
                                className="flex flex-col gap-2 h-24 rounded-2xl transition-all hover:scale-105"
                                onClick={() => setTheme('system')}
                            >
                                <Monitor className="h-6 w-6" />
                                <span className="text-xs font-semibold">System</span>
                            </Button>
                        </div>
                    </CardContent>
                </Card>

                {/* Account & Security */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <Card className="border-none shadow-lg bg-card/30 backdrop-blur-sm">
                        <CardHeader>
                            <CardTitle className="text-base flex items-center gap-2">
                                <UserIcon className="h-4 w-4 text-primary" />
                                User Information
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-6">
                            {isLoading ? (
                                <div className="space-y-4">
                                    <Skeleton className="h-10 w-full" />
                                    <Skeleton className="h-10 w-full" />
                                </div>
                            ) : (
                                <>
                                    <div className="space-y-2">
                                        <Label className="text-xs text-muted-foreground">Full Name</Label>
                                        <div className="flex items-center gap-3 p-2 rounded-md bg-background/50 border border-border/50">
                                            <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center text-primary">
                                                <UserIcon className="h-4 w-4" />
                                            </div>
                                            <span className="font-medium text-sm">{user?.name || "Unknown User"}</span>
                                        </div>
                                    </div>

                                    <div className="space-y-2">
                                        <Label className="text-xs text-muted-foreground">Email Address</Label>
                                        <div className="flex items-center gap-3 p-2 rounded-md bg-background/50 border border-border/50">
                                            <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center text-primary">
                                                <Mail className="h-4 w-4" />
                                            </div>
                                            <span className="font-medium text-sm">{user?.email || "No email provided"}</span>
                                        </div>
                                    </div>
                                </>
                            )}
                        </CardContent>
                    </Card>

                    <Card className="border-none shadow-lg bg-card/30 backdrop-blur-sm">
                        <CardHeader>
                            <CardTitle className="text-base flex items-center gap-2">
                                <Bell className="h-4 w-4 text-primary" />
                                Notifications
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            <div className="flex items-center justify-between text-sm">
                                <span className="text-muted-foreground flex items-center gap-2">
                                    <Zap className="h-4 w-4" /> MS Teams Alerts
                                </span>
                                <Switch disabled />
                            </div>
                            <p className="text-[10px] text-muted-foreground italic text-center">Connect MS Teams in integrations to enable</p>
                        </CardContent>
                    </Card>
                </div>

                <div className="flex justify-end pt-4">
                    <Button className="px-8 rounded-full shadow-lg shadow-primary/20 hover:scale-105 transition-all">
                        Save Preferences
                    </Button>
                </div>
            </div>
        </div>
    )
}

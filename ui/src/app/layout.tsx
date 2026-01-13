import type { Metadata } from "next";
import "./globals.css";
import "@fontsource/geist-sans/400.css";
import "@fontsource/geist-sans/500.css";
import "@fontsource/geist-sans/600.css";
import "@fontsource/geist-sans/700.css";
import "@fontsource/geist-mono/400.css";
import "@fontsource/geist-mono/700.css";
import { Providers } from "@/components/providers";
import { Banner } from "@/components/banner";

export const metadata: Metadata = {
  title: "Unchain | Feature Flag Management",
  description: "High-performance feature flag management system",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body
        className="antialiased"
        suppressHydrationWarning
      >
        <div className="flex flex-col h-screen">
          <Providers>
            <Banner />
            <div className="flex-1 overflow-hidden">
              {children}
            </div>
          </Providers>
        </div>
      </body>
    </html>
  );
}


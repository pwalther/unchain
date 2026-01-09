# Unchain UI
 
 The **Unchain UI** is the frontend dashboard for the Unchain feature flag management platform. It is built with Next.js 15, React 19, and Tailwind CSS.
 
 ## Configuration
 
 ### Global Banner
 
 The UI supports a global announcement banner that can be configured in `src/config/banner.ts`.
 
 **Features:**
 - **Show/Hide**: Controlled via the `enabled` boolean.
 - **Dynamic Content**: Displays any string defined in `message`.
 - **Smart Dismissal**: Remembers if a user has dismissed the banner. If the `message` is updated, the banner will automatically reappear for all users.
 
 **Example Configuration:**
 ```typescript
 export const BANNER_CONFIG = {
   message: "Planned maintenance on Saturday at 22:00 UTC",
   enabled: true,
 };
 ```
 
 ### API Connection
 
 The API endpoint is configured in `src/lib/api.ts` via the `NEXT_PUBLIC_API_URL` environment variable.
 
 ```typescript
 export const getApiUrl = () => process.env.NEXT_PUBLIC_API_URL || "http://localhost:3000";
 ```
 
 **Important Note for OIDC:**
 When using OpenID Connect (OIDC) authentication, the API URL must typically be an **HTTPS** address (e.g., `https://your-api.com`). This is because OIDC servers usually set "Secure" cookies which browsers will refuse to send over unencrypted HTTP connections. If you find yourself in a redirect loop or unauthenticated state with OIDC, verify you are using HTTPS.
 
 ## Getting Started

## Getting Started

First, run the development server:

```bash
npm run dev
# or
yarn dev
# or
pnpm dev
# or
bun dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

You can start editing the page by modifying `app/page.tsx`. The page auto-updates as you edit the file.

This project uses [`next/font/google`](https://nextjs.org/docs/app/building-your-application/optimizing/fonts) to automatically optimize and load [Geist](https://vercel.com/font).
> **Note for Offline/Proxy Builds:**  
> Next.js downloads these fonts at **build time** and self-hosts them. If you are building this application in an environment without internet access (e.g., behind a strict corporate proxy), the build may fail. In that case, you must manually download the `.woff2` font files, place them in `src/app/fonts/`, and switch to [`next/font/local`](https://nextjs.org/docs/app/building-your-application/optimizing/fonts#local-fonts) in `src/app/layout.tsx`.


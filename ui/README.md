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

This project uses [`next/font`](https://nextjs.org/docs/app/building-your-application/optimizing/fonts) to automatically optimize and load [Geist](https://vercel.com/font), a new font family for Vercel.

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js) - your feedback and contributions are welcome!

## Deploy on Vercel

The easiest way to deploy your Next.js app is to use the [Vercel Platform](https://vercel.com/new?utm_medium=default-template&filter=next.js&utm_source=create-next-app&utm_campaign=create-next-app-readme) from the creators of Next.js.

Check out our [Next.js deployment documentation](https://nextjs.org/docs/app/building-your-application/deploying) for more details.

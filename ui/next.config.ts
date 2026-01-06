import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  basePath: process.env.BASE_PATH,
  output: 'export',
  trailingSlash: true,
  images: {
    unoptimized: true,
  },
};

export default nextConfig;

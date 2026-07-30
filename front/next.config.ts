import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  reactCompiler: true,
  async rewrites() {
    return [
      {
        source: "/images/logo.png",
        destination: "/images/logo.svg",
      },
      {
        source: "/images/logo-horizontal.png",
        destination: "/images/logo-horizontal.svg",
      },
    ];
  },
};

export default nextConfig;

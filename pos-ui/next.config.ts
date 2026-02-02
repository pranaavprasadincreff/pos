import type { NextConfig } from "next"

const nextConfig: NextConfig = {
    images: {
        remotePatterns: [
            // allow https images from any host
            {
                protocol: "https",
                hostname: "**",
            },
            // allow http images from any host
            {
                protocol: "http",
                hostname: "**",
            },
        ],
    },

    devIndicators: false,

    async rewrites() {
        return [
            {
                source: "/api/:path*",
                destination: "http://localhost:8080/api/:path*",
            },
        ]
    },
}

export default nextConfig

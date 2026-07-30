"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const TABS = [
  { label: "공연", href: "/" },
  { label: "게시판", href: "/board" },
];

export default function PageTabs() {
  const pathname = usePathname();

  return (
    <div className="bg-white border-b border-gray-100">
      <div className="max-w-5xl mx-auto px-6 flex gap-0">
        {TABS.map(({ label, href }) => {
          const isActive =
            href === "/" ? pathname === "/" : pathname.startsWith(href);
          return (
            <Link
              key={href}
              href={href}
              className={`px-5 py-3 text-sm font-semibold border-b-2 transition-colors ${
                isActive
                  ? "border-blue-600 text-blue-600"
                  : "border-transparent text-gray-500 hover:text-gray-700"
              }`}
            >
              {label}
            </Link>
          );
        })}
      </div>
    </div>
  );
}

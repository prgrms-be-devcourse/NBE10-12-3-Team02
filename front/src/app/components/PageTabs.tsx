"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState, useEffect, useRef } from "react";

const TABS = [
  { label: "공연", href: "/" },
  { label: "게시판", href: "/board" },
];

export default function PageTabs() {
  const pathname = usePathname();
  const [visible, setVisible] = useState(true);
  const [tabHeight, setTabHeight] = useState(46);
  const tabRef = useRef<HTMLDivElement>(null);
  const lastScrollY = useRef(0);

  useEffect(() => {
    if (tabRef.current) {
      setTabHeight(tabRef.current.offsetHeight);
    }
  }, []);

  useEffect(() => {
    const handleScroll = () => {
      const currentY = window.scrollY;
      if (currentY < 10) {
        setVisible(true);
      } else if (currentY < lastScrollY.current) {
        setVisible(true);
      } else if (currentY > lastScrollY.current) {
        setVisible(false);
      }
      lastScrollY.current = currentY;
    };
    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  if (pathname === "/login" || pathname === "/signup") return null;

  return (
    <>
      <div
        ref={tabRef}
        className={`fixed top-16 left-0 right-0 z-40 bg-white border-b border-gray-100 transition-transform duration-300 ${
          visible ? "translate-y-0" : "-translate-y-full"
        }`}
      >
        <div className="max-w-5xl mx-auto px-6 flex">
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
      {/* fixed로 빠진 탭바 높이만큼 아래 콘텐츠가 가려지지 않도록 공간 확보 */}
      <div style={{ height: tabHeight }} aria-hidden="true" />
    </>
  );
}

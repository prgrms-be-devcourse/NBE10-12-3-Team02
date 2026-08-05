"use client";

import Image from "next/image";

const ICON_MAP: Record<string, string> = {
  KAKAO: "/icons/kakao.png",
  NAVER: "/icons/naver.png",
  GOOGLE: "/icons/google.png",
};

export function SocialBadge({
  provider,
  size = 18,
}: {
  provider: string;
  size?: number;
}) {
  const src = ICON_MAP[provider];
  if (!src) return null;
  return <Image src={src} alt={provider} width={size} height={size} unoptimized />;
}

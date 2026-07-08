const CONCERT_DETAIL_IMAGES: Record<string, string[]> = {
  PF232456: [
    "/images/concerts/details/PF232456-01.webp",
    "/images/concerts/details/PF232456-02.webp",
  ],
  PF232467: ["/images/concerts/details/PF232467-01.webp"],
  PF232471: ["/images/concerts/details/PF232471-01.webp"],
  PF232473: [
    "/images/concerts/details/PF232473-01.webp",
    "/images/concerts/details/PF232473-02.webp",
    "/images/concerts/details/PF232473-03.webp",
    "/images/concerts/details/PF232473-04.webp",
    "/images/concerts/details/PF232473-05.webp",
  ],
  PF233707: ["/images/concerts/details/PF233707-01.webp"],
  PF234436: ["/images/concerts/details/PF234436-01.webp"],
  PF235543: ["/images/concerts/details/PF235543-01.webp"],
  PF241378: ["/images/concerts/details/PF241378-01.webp"],
  PF244704: ["/images/concerts/details/PF244704-01.webp"],
  PF271736: ["/images/concerts/details/PF271736-01.webp"],
  PF282011: ["/images/concerts/details/PF282011-01.webp"],
  PF282014: ["/images/concerts/details/PF282014-01.webp"],
  PF282015: [
    "/images/concerts/details/PF282015-01.webp",
    "/images/concerts/details/PF282015-02.webp",
  ],
  PF282016: ["/images/concerts/details/PF282016-01.webp"],
  PF282031: [
    "/images/concerts/details/PF282031-01.webp",
    "/images/concerts/details/PF282031-02.webp",
    "/images/concerts/details/PF282031-03.webp",
  ],
  PF282176: ["/images/concerts/details/PF282176-01.webp"],
  PF283207: [
    "/images/concerts/details/PF283207-01.webp",
    "/images/concerts/details/PF283207-02.webp",
    "/images/concerts/details/PF283207-03.webp",
  ],
  PF283793: [
    "/images/concerts/details/PF283793-01.webp",
    "/images/concerts/details/PF283793-02.webp",
    "/images/concerts/details/PF283793-03.webp",
  ],
  PF283878: ["/images/concerts/details/PF283878-01.webp"],
  PF294720: [
    "/images/concerts/details/PF294720-01.webp",
    "/images/concerts/details/PF294720-02.webp",
    "/images/concerts/details/PF294720-03.webp",
    "/images/concerts/details/PF294720-04.webp",
  ],
  PF294721: ["/images/concerts/details/PF294721-01.webp"],
  PF294722: ["/images/concerts/details/PF294722-01.webp"],
  PF294723: ["/images/concerts/details/PF294723-01.webp"],
  PF294724: ["/images/concerts/details/PF294724-01.webp"],
  PF294726: ["/images/concerts/details/PF294726-01.webp"],
  PF294727: ["/images/concerts/details/PF294727-01.webp"],
  PF294728: ["/images/concerts/details/PF294728-01.webp"],
  PF294729: ["/images/concerts/details/PF294729-01.webp"],
  PF294730: ["/images/concerts/details/PF294730-01.webp"],
};

const CONCERT_POSTER_IMAGES: Record<string, string> = {
  PF232456: "/images/concerts/posters/PF232456.webp",
  PF232467: "/images/concerts/posters/PF232467.webp",
  PF232471: "/images/concerts/posters/PF232471.webp",
  PF232473: "/images/concerts/posters/PF232473.webp",
  PF233707: "/images/concerts/posters/PF233707.webp",
  PF234436: "/images/concerts/posters/PF234436.webp",
  PF235543: "/images/concerts/posters/PF235543.webp",
  PF241378: "/images/concerts/posters/PF241378.webp",
  PF244704: "/images/concerts/posters/PF244704.webp",
  PF271736: "/images/concerts/posters/PF271736.webp",
  PF282011: "/images/concerts/posters/PF282011.webp",
  PF282014: "/images/concerts/posters/PF282014.webp",
  PF282015: "/images/concerts/posters/PF282015.webp",
  PF282016: "/images/concerts/posters/PF282016.webp",
  PF282031: "/images/concerts/posters/PF282031.webp",
  PF282176: "/images/concerts/posters/PF282176.webp",
  PF283207: "/images/concerts/posters/PF283207.webp",
  PF283793: "/images/concerts/posters/PF283793.webp",
  PF283878: "/images/concerts/posters/PF283878.webp",
  PF294720: "/images/concerts/posters/PF294720.webp",
  PF294721: "/images/concerts/posters/PF294721.webp",
  PF294722: "/images/concerts/posters/PF294722.webp",
  PF294723: "/images/concerts/posters/PF294723.webp",
  PF294724: "/images/concerts/posters/PF294724.webp",
  PF294726: "/images/concerts/posters/PF294726.webp",
  PF294727: "/images/concerts/posters/PF294727.webp",
  PF294728: "/images/concerts/posters/PF294728.webp",
  PF294729: "/images/concerts/posters/PF294729.webp",
  PF294730: "/images/concerts/posters/PF294730.webp",
};

export function extractKopisId(url?: string | null) {
  if (!url) return null;

  return (
    url.match(/PF_PF(\d+)_/i)?.[1]?.replace(/^/, "PF") ??
    url.match(/PF\d+/i)?.[0]?.toUpperCase() ??
    null
  );
}

export function getLocalConcertPoster(urlPoster?: string | null) {
  const kopisId = extractKopisId(urlPoster);
  return kopisId ? CONCERT_POSTER_IMAGES[kopisId] ?? urlPoster ?? "" : urlPoster ?? "";
}

export function getConcertDetailImages(urlPoster: string) {
  const kopisId = extractKopisId(urlPoster);
  return kopisId ? CONCERT_DETAIL_IMAGES[kopisId] ?? [] : [];
}
const CONCERT_DETAIL_IMAGES: Record<string, string[]> = {
  PF232456: [
    "/images/concerts/details/PF232456-01.jpg",
    "/images/concerts/details/PF232456-02.jpg",
  ],
  PF232467: ["/images/concerts/details/PF232467-01.jpg"],
  PF232471: ["/images/concerts/details/PF232471-01.jpg"],
  PF232473: [
    "/images/concerts/details/PF232473-01.jpg",
    "/images/concerts/details/PF232473-02.jpg",
    "/images/concerts/details/PF232473-03.jpg",
    "/images/concerts/details/PF232473-04.jpg",
    "/images/concerts/details/PF232473-05.jpg",
  ],
  PF233707: ["/images/concerts/details/PF233707-01.png"],
  PF234436: ["/images/concerts/details/PF234436-01.jpg"],
  PF235543: ["/images/concerts/details/PF235543-01.jpg"],
  PF241378: ["/images/concerts/details/PF241378-01.jpg"],
  PF244704: ["/images/concerts/details/PF244704-01.jpg"],
  PF271736: ["/images/concerts/details/PF271736-01.jpg"],
  PF282011: ["/images/concerts/details/PF282011-01.png"],
  PF282014: ["/images/concerts/details/PF282014-01.png"],
  PF282015: [
    "/images/concerts/details/PF282015-01.jpg",
    "/images/concerts/details/PF282015-02.jpg",
  ],
  PF282016: ["/images/concerts/details/PF282016-01.jpg"],
  PF282031: [
    "/images/concerts/details/PF282031-01.jpg",
    "/images/concerts/details/PF282031-02.jpg",
    "/images/concerts/details/PF282031-03.jpg",
  ],
  PF282176: ["/images/concerts/details/PF282176-01.jpg"],
  PF283207: [
    "/images/concerts/details/PF283207-01.png",
    "/images/concerts/details/PF283207-02.png",
    "/images/concerts/details/PF283207-03.png",
  ],
  PF283793: [
    "/images/concerts/details/PF283793-01.png",
    "/images/concerts/details/PF283793-02.jpg",
    "/images/concerts/details/PF283793-03.png",
  ],
  PF283878: ["/images/concerts/details/PF283878-01.png"],
  PF294720: [
    "/images/concerts/details/PF294720-01.jpg",
    "/images/concerts/details/PF294720-02.jpg",
    "/images/concerts/details/PF294720-03.jpg",
    "/images/concerts/details/PF294720-04.jpg",
  ],
  PF294721: ["/images/concerts/details/PF294721-01.png"],
  PF294722: ["/images/concerts/details/PF294722-01.png"],
  PF294723: ["/images/concerts/details/PF294723-01.jpg"],
  PF294724: ["/images/concerts/details/PF294724-01.jpg"],
  PF294726: ["/images/concerts/details/PF294726-01.jpg"],
  PF294727: ["/images/concerts/details/PF294727-01.jpg"],
  PF294728: ["/images/concerts/details/PF294728-01.png"],
  PF294729: ["/images/concerts/details/PF294729-01.jpg"],
  PF294730: ["/images/concerts/details/PF294730-01.jpg"],
};

const CONCERT_POSTER_IMAGES: Record<string, string> = {
  PF232456: "/images/concerts/posters/PF232456.jpg",
  PF232467: "/images/concerts/posters/PF232467.jpg",
  PF232471: "/images/concerts/posters/PF232471.gif",
  PF232473: "/images/concerts/posters/PF232473.gif",
  PF233707: "/images/concerts/posters/PF233707.jpg",
  PF234436: "/images/concerts/posters/PF234436.gif",
  PF235543: "/images/concerts/posters/PF235543.gif",
  PF241378: "/images/concerts/posters/PF241378.gif",
  PF244704: "/images/concerts/posters/PF244704.jpg",
  PF271736: "/images/concerts/posters/PF271736.jpg",
  PF282011: "/images/concerts/posters/PF282011.jpg",
  PF282014: "/images/concerts/posters/PF282014.jpeg",
  PF282015: "/images/concerts/posters/PF282015.jpg",
  PF282016: "/images/concerts/posters/PF282016.jpg",
  PF282031: "/images/concerts/posters/PF282031.jpg",
  PF282176: "/images/concerts/posters/PF282176.jpg",
  PF283207: "/images/concerts/posters/PF283207.png",
  PF283793: "/images/concerts/posters/PF283793.png",
  PF283878: "/images/concerts/posters/PF283878.png",
  PF294720: "/images/concerts/posters/PF294720.jpg",
  PF294721: "/images/concerts/posters/PF294721.png",
  PF294722: "/images/concerts/posters/PF294722.png",
  PF294723: "/images/concerts/posters/PF294723.jpg",
  PF294724: "/images/concerts/posters/PF294724.jpg",
  PF294726: "/images/concerts/posters/PF294726.png",
  PF294727: "/images/concerts/posters/PF294727.jpg",
  PF294728: "/images/concerts/posters/PF294728.png",
  PF294729: "/images/concerts/posters/PF294729.jpg",
  PF294730: "/images/concerts/posters/PF294730.jpg",
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

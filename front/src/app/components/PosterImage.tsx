"use client";

import { useState } from "react";
import Image, { type ImageProps } from "next/image";

type PosterImageProps = Omit<ImageProps, "src" | "onError"> & { src: string };

export default function PosterImage({ src, alt, ...rest }: PosterImageProps) {
  const [error, setError] = useState(false);
  return (
    <Image
      {...rest}
      src={error ? "/default-poster.svg" : src}
      alt={alt}
      onError={() => setError(true)}
    />
  );
}
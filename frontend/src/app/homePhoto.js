// Landscape photo of the home page: Scott Blake on Unsplash (Unsplash License, free commercial use),
// 16:10 WebP derivatives in public/photos. index.html preloads it with the same srcset and sizes.

const WIDTHS = [960, 1440, 1600, 1920, 2560]

export const HOME_PHOTO_SRC = '/photos/orchard-1440.webp'

export const HOME_PHOTO_SRCSET = WIDTHS.map((width) => `/photos/orchard-${width}.webp ${width}w`).join(', ')

// Rendered with object-cover, so as wide as 1.6 times its height on tall windows. From 1024 px it
// fills the page; below, it fills the headline block: the window less the cards (about 13.5rem)
// on tablets, at most about 500 px high on phones.
export const HOME_PHOTO_SIZES =
  '(min-width: 64rem) max(100vw, 160vh), (min-width: 40rem) max(100vw, 160vh - 22rem), max(100vw, 50rem)'

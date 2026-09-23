# Frontend API Guide — Every Public Endpoint, Request and Response

> **Audience:** the KHI website frontend, the platform SPA, any client calling this backend ·
> **Base URL:** `https://khiarchiveplatformbackend-production.up.railway.app` ·
> **Covers:** all **29** endpoints under `/api/guest/**` — everything the public frontend calls ·
> **Auth:** none. No token, no header, no cookie.

Every endpoint below is documented the same way: **what you send**, **what comes back**, and a
copy-pasteable `curl` against the live server. Response bodies are complete — every field, with the
exact name and type the API emits.

Endpoints that need a staff token (content CRUD, trash, admin, analytics, maqam, physical media)
are **not** in this file; they live in [`../internal/`](../internal/README.md). Ask if you want
them written up in this same format.

---

## Contents

**[Part 0 — Before you start](#part-0--before-you-start)**
· [Base URL](#base-url) · [No authentication](#no-authentication) · [Rules that apply to every
response](#rules-that-apply-to-every-response) · [Pagination](#pagination) · [Errors](#errors)

**[Part 1 — Shared object shapes](#part-1--shared-object-shapes)**
· [PersonSummary](#personsummary) · [CategorySummary](#categorysummary) · [MediaHit](#mediahit)
· [Audio](#audio) · [Video](#video) · [Image](#image) · [Text](#text) · [Project](#project)
· [Category](#category) · [Person](#person)

**[Part 2 — Website search](#part-2--website-search) — start here**
1. [`GET /api/guest/media/search`](#1-get-apiguestmediasearch)
2. [`GET /api/guest/media/{type}/{code}`](#2-get-apiguestmediatypecode)

**[Part 3 — Discovery](#part-3--discovery)**
3. [`GET /api/guest/suggest`](#3-get-apiguestsuggest)
4. [`GET /api/guest/trending`](#4-get-apiguesttrending)
5. [`GET /api/guest/facets`](#5-get-apiguestfacets)
6. [`GET /api/guest/feed`](#6-get-apiguestfeed)
7. [`GET /api/guest/search`](#7-get-apiguestsearch)

**[Part 4 — Catalog](#part-4--catalog)**
8. [`GET /api/guest/projects`](#8-get-apiguestprojects) ·
9. [`/projects/{projectCode}`](#9-get-apiguestprojectsprojectcode) ·
10. [`/projects/{projectCode}/media`](#10-get-apiguestprojectsprojectcodemedia) ·
11. [`/categories`](#11-get-apiguestcategories) ·
12. [`/categories/{categoryCode}`](#12-get-apiguestcategoriescategorycode) ·
13. [`/categories/{categoryCode}/projects`](#13-get-apiguestcategoriescategorycodeprojects) ·
14. [`/persons`](#14-get-apiguestpersons) ·
15. [`/persons/{personCode}`](#15-get-apiguestpersonspersoncode) ·
16. [`/persons/{personCode}/projects`](#16-get-apiguestpersonspersoncodeprojects)

**[Part 5 — Media catalogs](#part-5--media-catalogs)**
17. [`/audios`](#17-get-apiguestaudios) · 18. [`/audios/{audioCode}`](#18-get-apiguestaudiosaudiocode) ·
19. [`/videos`](#19-get-apiguestvideos) · 20. [`/videos/{videoCode}`](#20-get-apiguestvideosvideocode) ·
21. [`/texts`](#21-get-apiguesttexts) · 22. [`/texts/{textCode}`](#22-get-apiguesttextstextcode) ·
23. [`/images`](#23-get-apiguestimages) · 24. [`/images/{imageCode}`](#24-get-apiguestimagesimagecode)

**[Part 6 — Files and playback](#part-6--files-and-playback)**
25. [`/audio/{code}/stream`](#25-get-apiguestaudioaudiocodestream) ·
26. [`/video/{code}/stream`](#26-get-apiguestvideovideocodestream) ·
27. [`/image/{code}/view`](#27-get-apiguestimageimagecodeview) ·
28. [`/text/{code}/read`](#28-get-apiguesttexttextcoderead) ·
29. [`/text/{code}/cover`](#29-get-apiguesttexttextcodecover)

**[Part 7 — Putting it together](#part-7--putting-it-together)**

---

# Part 0 — Before you start

## Base URL

```
Production   https://khiarchiveplatformbackend-production.up.railway.app
Local        http://localhost:8080
```

Every path in this file is appended to that. One place in your code should hold it:

```js
// src/lib/config.js
export const API_BASE = import.meta.env.VITE_API_BASE_URL
  ?? 'https://khiarchiveplatformbackend-production.up.railway.app';
```

A minimal client, used by every JavaScript example below:

```js
// src/lib/api.js
import { API_BASE } from './config';

export async function api(path, params = {}, options = {}) {
  const url = new URL(API_BASE + path);
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === '') continue;
    // Repeated arrays: ?tag=a&tag=b  — never ?tag[0]=a
    for (const v of [].concat(value)) url.searchParams.append(key, v);
  }
  const res = await fetch(url, options);
  if (!res.ok) throw Object.assign(new Error('API error'), { status: res.status, res });
  return res.json();
}
```

> **Array parameters repeat.** `tag`, `keyword`, `subject`, `genre`, `type` and `personType` are
> sent as `?tag=concert&tag=live`. If you use axios, set
> `paramsSerializer: { indexes: null }` — the default `tag[0]=concert` binds to nothing and your
> filter silently does nothing.

## No authentication

`/api/guest/**` is `permitAll()` and the JWT filter skips the prefix entirely. Sending a token
changes nothing; sending a broken one breaks nothing.

```bash
# This is the entire auth setup for every endpoint in this file:
curl -s "https://khiarchiveplatformbackend-production.up.railway.app/api/guest/trending"
```

**CORS.** Browser calls need your origin on the allowlist. Already allowed:
`http://localhost:5173`, `http://localhost:3000`,
`https://khi-archive-platform-frontend.vercel.app`,
`https://khi-archive-platform-frontend-a5p7.vercel.app`. Anything else — including every Vercel
preview URL — must be added to the backend's `CORS_ALLOWED_ORIGINS` env var **exactly**; wildcards
do not work. See [`../FRONTEND_INTEGRATION.md`](../FRONTEND_INTEGRATION.md).

## Rules that apply to every response

| Rule | What it means for your code |
|---|---|
| **`null` fields are omitted** | `hit.duration` is *absent*, not `null`, on an image. Use `?.` and `??` everywhere; never assume a key exists. |
| **Empty arrays are kept** | `"tags": []` is sent. `matchedIn`, `content`, `tags` are arrays or absent, never `null`. |
| **Timestamps are ISO-8601 UTC** | `"2015-05-01T00:00:00Z"` → `new Date(v)`. Plain dates (birth/death) are `"1921-03-15"`. |
| **Media paths are host-relative** | `"/api/guest/audio/AUD-0142/stream"` → prepend `API_BASE`. Never an S3 URL. |
| **Codes are the identity** | Route on `audioCode`/`projectCode`/…, not on `id`. Ids repeat across tables. |
| **`isTrending` serializes as `trending`** | The JSON key is `trending`. |
| **404 = "not available"** | Unknown code and hidden record are indistinguishable, on purpose. |

## Pagination

Every list endpoint takes `page` (0-based) and `size`, and returns Spring's `Page` envelope:

```jsonc
{
  "content": [ /* the rows */ ],
  "pageable": { "pageNumber": 0, "pageSize": 50, "offset": 0, "paged": true, "unpaged": false },
  "totalElements": 137,
  "totalPages": 3,
  "number": 0,             // current page, 0-based
  "size": 50,
  "numberOfElements": 50,
  "first": true,
  "last": false,
  "empty": false,
  "sort": { "sorted": false, "unsorted": true, "empty": true }
}
```

Read `number`, `totalPages`, `totalElements`, `first`, `last`. **`/api/guest/media/search` is the
one exception** — it returns a flat envelope with `page` instead of `number`, documented in
[Part 2](#1-get-apiguestmediasearch).

Default `size` is **50** on every list except `/categories` (**100**) and
`/media/search` (**24**, max 100).

> **Sorting: use `sortBy` + `sortDirection`.** A `sort=` parameter is echoed back in the envelope
> but does **not** reorder anything. An unrecognized `sortBy` is ignored silently.

## Errors

Every failure is one shape:

```jsonc
{
  "timestamp": "2026-09-04T09:12:44.183Z",
  "status": 404,
  "error": "AUDIO_NOT_FOUND",
  "category": "NOT_FOUND",
  "message": "Audio not found",
  "hint": "Check the audioCode, or the item may not be public.",
  "path": "/api/guest/audios/AUD-9999",
  "traceId": "b21f0a9c",
  "details": {}
}
```

`timestamp`, `status`, `error`, `message` and `path` are always present; `category`, `hint`,
`traceId` and `details` appear when they add something.

**Branch on `error`, never on `message`** — messages change, codes do not.

| Status | When | What to show |
|---|---|---|
| `200` | Success — including zero results | The results, or the empty state |
| `304` | Your cached copy is still valid (byte proxies only) | Nothing — the browser handles it |
| `206` | Partial content (audio/video seeking) | Nothing — the player handles it |
| `400` | A required parameter is missing (`q` on `/suggest` and `/search`), or a typed one is malformed (`gender`) | Fix the call — this is a client bug |
| `404` | Unknown code, **or** the item is not public | "Not available" |
| `500` | The file is missing from storage, or the archive is down | "Try again shortly" |

Zero results is **`200` with an empty list**, never a `404`. Full code list:
[`02-errors.md`](./02-errors.md).

---

# Part 1 — Shared object shapes

These objects appear inside many responses. Each endpoint below names the shape rather than
repeating 60 fields, so this is the section to keep open while you write types.

## PersonSummary

Embedded as `person` on every media object.

```jsonc
{
  "id": 1,
  "personCode": "PER-001",
  "fullName": "Hasan Zirak",
  "nickname": "Zirak",
  "romanizedName": "Hasan Zirak",
  "mediaPortrait": "/api/guest/image/IMG-0007/view"
}
```

## CategorySummary

Embedded as `categories[]` on every media object.

```jsonc
{ "id": 3, "categoryCode": "CAT-003", "name": "Music" }
```

## MediaHit

The flat card returned by `/api/guest/media/search`. **Identical for all four kinds** — this is
what lets one component render a mixed list.

```jsonc
{
  "type": "audio",
  "code": "AUD-0142",
  "id": 142,

  "title": "Hasan Zirak — Live in Sulaymaniyah",
  "subtitle": "حەسەن زیرەک — لایڤ",
  "titleInCentralKurdish": "حەسەن زیرەک — لایڤ",
  "romanizedTitle": "Hasan Zirak — Live",
  "description": "A 1971 concert recording…",

  "creator": "Hasan Zirak",
  "creatorRole": "singer",

  "projectCode": "PRJ-014",
  "projectName": "Radio Recordings",
  "person":     { /* PersonSummary */ },
  "categories": [ /* CategorySummary */ ],

  "language": "Kurdish",
  "dialect": "Sorani",
  "region": "Sulaymaniyah",
  "subject": ["Music"],
  "genre": ["Folk"],
  "tags": ["concert", "live"],
  "keywords": ["radio"],

  "duration": "04:12",
  "pageCount": 240,
  "documentType": "Book",

  "dateCreated": "1971-06-01T00:00:00Z",
  "datePublished": "1972-01-01T00:00:00Z",

  "mediaUrl": "/api/guest/audio/AUD-0142/stream",
  "thumbnailUrl": "/api/guest/image/IMG-0007/view",
  "detailUrl": "/api/guest/media/audio/AUD-0142",

  "score": 34.117,
  "matchedIn": ["title", "person", "creator"],

  "trending": true,
  "trendingRank": 3,
  "trendingScore": 18.5,

  "audio": null, "video": null, "image": null, "text": null
}
```

| Field | Type | Always? | Notes |
|---|---|---|---|
| `type` | string | yes | `audio` \| `video` \| `image` \| `text` |
| `code` | string | yes | The public code — use with `type` for routing |
| `id` | number | yes | Internal id. **Not unique across kinds** |
| `title` | string | yes | Best of: original → Central Kurdish → romanized → alternative → code |
| `subtitle` | string | no | The next-best title, when it differs from `title` |
| `titleInCentralKurdish` | string | no | Arabic-script Kurdish — render with `dir="auto"` |
| `romanizedTitle` | string | no | Always Latin script |
| `description` | string | no | **Already trimmed** to 320 chars on a word boundary, `…` when cut |
| `creator` | string | no | Singer/speaker (audio), director (video), photographer (image), author (text) |
| `creatorRole` | string | no | Which field `creator` came from — label it, don't guess |
| `projectCode`, `projectName` | string | no | The owning collection |
| `person` | object | no | [PersonSummary](#personsummary) |
| `categories` | array | no | [CategorySummary](#categorysummary)`[]` |
| `language`, `dialect`, `region` | string | no | |
| `subject`, `genre`, `tags`, `keywords` | string[] | no | |
| `duration` | string | audio/video | `"MM:SS"` or `"HH:MM:SS"` |
| `pageCount` | number | text | |
| `documentType` | string | text | e.g. `"Book"`, `"Manuscript"` |
| `dateCreated`, `datePublished` | ISO instant | no | When the work was made / published |
| `mediaUrl` | string | yes | Host-relative byte-proxy path |
| `thumbnailUrl` | string | **no** | Often absent for audio/video — design for that |
| `detailUrl` | string | yes | Ready to call |
| `score` | number | yes | Relevance; `0` when the request carried no `q` |
| `matchedIn` | string[] | yes | Closed vocabulary — see below. `[]` when no `q` |
| `trending` | boolean | yes | |
| `trendingRank` | number | no | 1-based, only when trending |
| `trendingScore` | number | no | |
| `audio`/`video`/`image`/`text` | object | no | Only with `include=full`; exactly one, matching `type` |

**`matchedIn` vocabulary** — the complete set, so you can map it to UI labels once:

`title` · `code` · `creator` · `person` · `project` · `category` · `tags` · `keywords` ·
`subject` · `genre` · `place` · `description`

## Audio

Returned by `/api/guest/audios`, `/api/guest/audios/{code}`, and inside
`/api/guest/media/audio/{code}`.

```jsonc
{
  "id": 142,
  "audioCode": "AUD-0142",

  "projectCode": "PRJ-014",
  "projectName": "Radio Recordings",
  "personMediaPortrait": "/api/guest/image/IMG-0007/view",
  "person":     { /* PersonSummary */ },
  "categories": [ /* CategorySummary */ ],

  "originTitle": "Hasan Zirak — Live in Sulaymaniyah",
  "alterTitle": "Live 1971",
  "centralKurdishTitle": "حەسەن زیرەک — لایڤ",
  "romanizedTitle": "Hasan Zirak — Live",

  "form": "Song",
  "typeOfBasta": "Basta",
  "typeOfMaqam": "Bayat",
  "subject": ["Music"],
  "genre": ["Folk"],

  "abstractText": "Short summary of the recording.",
  "description": "Full description…",

  "speaker": null,
  "singer": "Hasan Zirak",
  "producer": "Radio Baghdad",
  "composer": "Unknown",
  "poet": "Hejar",
  "contributors": ["Ensemble"],

  "language": "Kurdish",
  "dialect": "Sorani",
  "typeOfComposition": "Traditional",
  "typeOfPerformance": "Live",
  "lyrics": "…",

  "recordingVenue": "Radio Studio",
  "city": "Sulaymaniyah",
  "region": "Sulaymaniyah",
  "audience": "Public",

  "tags": ["concert", "live"],
  "keywords": ["radio"],

  "duration": "04:12",

  "dateCreated": "1971-06-01T00:00:00Z",
  "datePublished": "1972-01-01T00:00:00Z",
  "dateModified": "2026-02-11T10:04:00Z",

  "copyright": "KHI",
  "rightOwner": "KHI",
  "dateCopyrighted": "1972-01-01T00:00:00Z",
  "licenseType": "CC BY-NC",
  "availability": "Public",
  "owner": "KHI",
  "publisher": "Radio Baghdad",

  "audioFileUrl": "/api/guest/audio/AUD-0142/stream",

  "trending": true,
  "trendingRank": 3,
  "trendingScore": 18.5
}
```

Note the audio-only naming: **`originTitle`**, **`alterTitle`**, **`centralKurdishTitle`** — the
other three kinds use `originalTitle`, `alternativeTitle`, `titleInCentralKurdish`.

**Never returned:** S3 path, volume, directory, LCC classification, bit rate, bit depth, sample
rate, file size, version internals, audit and trash fields.

## Video

```jsonc
{
  "id": 31,
  "videoCode": "VID-0031",
  "projectCode": "PRJ-014",
  "projectName": "Radio Recordings",
  "personMediaPortrait": "/api/guest/image/IMG-0007/view",
  "person":     { /* PersonSummary */ },
  "categories": [ /* CategorySummary */ ],

  "originalTitle": "Hasan Zirak on Stage",
  "alternativeTitle": "Stage 1973",
  "titleInCentralKurdish": "حەسەن زیرەک لەسەر شانۆ",
  "romanizedTitle": "Hasan Zirak on Stage",

  "subject": ["Music"],
  "genre": ["Documentary"],
  "event": "Newroz Festival",
  "location": "Sulaymaniyah",
  "description": "…",

  "personShownInVideo": "Hasan Zirak",
  "colorOfVideo": ["Black and white"],

  "language": "Kurdish",
  "dialect": "Sorani",
  "region": "Sulaymaniyah",
  "subtitle": "English",

  "creatorArtistDirector": "Aram Ali",
  "producer": "KHI",
  "contributor": "Crew",
  "audience": "Public",

  "tags": ["stage"],
  "keywords": ["festival"],
  "whereThisVideoUsed": ["Exhibition 2019"],

  "duration": "12:40",

  "dateCreated": "1973-03-21T00:00:00Z",
  "dateModified": "2026-02-11T10:04:00Z",
  "datePublished": "1974-01-01T00:00:00Z",

  "copyright": "KHI",
  "rightOwner": "KHI",
  "dateCopyrighted": "1974-01-01T00:00:00Z",
  "licenseType": "CC BY-NC",
  "usageRights": "Non-commercial",
  "availability": "Public",
  "owner": "KHI",
  "publisher": "KHI",

  "videoFileUrl": "/api/guest/video/VID-0031/stream",

  "trending": false
}
```

## Image

```jsonc
{
  "id": 7,
  "imageCode": "IMG-0007",
  "projectCode": "PRJ-014",
  "projectName": "Radio Recordings",
  "personMediaPortrait": "/api/guest/image/IMG-0007/view",
  "person":     { /* PersonSummary */ },
  "categories": [ /* CategorySummary */ ],

  "originalTitle": "Hasan Zirak, portrait",
  "alternativeTitle": "Studio portrait",
  "titleInCentralKurdish": "وێنەی حەسەن زیرەک",
  "romanizedTitle": "Hasan Zirak, portrait",

  "subject": ["Portrait"],
  "form": "Photograph",
  "genre": ["Studio"],
  "event": null,
  "location": "Sulaymaniyah",
  "description": "…",

  "personShownInImage": "Hasan Zirak",
  "colorOfImage": ["Black and white"],

  "language": "Kurdish",
  "dialect": "Sorani",
  "region": "Sulaymaniyah",

  "manufacturer": "Nikon",
  "model": "F2",
  "lens": "50mm",

  "creatorArtistPhotographer": "Kamaran Sabir",
  "contributor": null,
  "audience": "Public",
  "photostory": null,

  "tags": ["portrait"],
  "keywords": ["studio"],
  "whereThisImageUsed": ["Book cover 2011"],

  "dateCreated": "1970-01-01T00:00:00Z",
  "dateModified": "2026-02-11T10:04:00Z",
  "datePublished": null,

  "copyright": "KHI",
  "rightOwner": "KHI",
  "dateCopyrighted": null,
  "licenseType": "CC BY-NC",
  "usageRights": "Non-commercial",
  "availability": "Public",
  "owner": "KHI",
  "publisher": "KHI",

  "imageFileUrl": "/api/guest/image/IMG-0007/view",

  "trending": false
}
```

**Never returned:** path, directory, LCC, DPI, pixel dimensions, bit depth, file size, version
internals, audit fields.

## Text

```jsonc
{
  "id": 56,
  "textCode": "TXT-0056",
  "projectCode": "PRJ-020",
  "projectName": "Printed Collection",
  "personMediaPortrait": null,
  "person":     { /* PersonSummary */ },
  "categories": [ /* CategorySummary */ ],

  "originalTitle": "Collected Songs of Hasan Zirak",
  "alternativeTitle": "Songbook",
  "titleInCentralKurdish": "گۆرانییەکانی حەسەن زیرەک",
  "romanizedTitle": "Goranîyekanî Hasan Zirak",

  "subject": ["Music"],
  "genre": ["Songbook"],
  "documentType": "Book",
  "description": "…",

  "script": "Arabic",
  "transcription": "…",
  "isbn": "978-0-000000-0-0",
  "edition": "2nd",
  "volume": "1",
  "series": "Kurdish Heritage",

  "language": "Kurdish",
  "dialect": "Sorani",
  "region": "Sulaymaniyah",

  "author": "Hejar",
  "contributors": "Editorial board",
  "printingHouse": "Sulaymaniyah Press",
  "audience": "General",

  "tags": ["songbook"],
  "keywords": ["lyrics"],

  "pageCount": 240,

  "dateCreated": "1985-01-01T00:00:00Z",
  "printDate": "1985-06-01T00:00:00Z",
  "dateModified": "2026-02-11T10:04:00Z",
  "datePublished": "1985-06-01T00:00:00Z",

  "copyright": "KHI",
  "rightOwner": "KHI",
  "dateCopyrighted": null,
  "licenseType": "CC BY-NC",
  "usageRights": "Non-commercial",
  "availability": "Public",
  "owner": "KHI",
  "publisher": "Sulaymaniyah Press",

  "textFileUrl": "/api/guest/text/TXT-0056/read",
  "coverImageUrl": "/api/guest/text/TXT-0056/cover",

  "trending": false
}
```

> **`coverImageUrl` is only present when a cover actually exists.** It is deliberately omitted
> otherwise, so you never render an `<img>` that is guaranteed to 404.

## Project

A project is a collection — the container media belongs to.

```jsonc
{
  "id": 14,
  "projectCode": "PRJ-014",
  "projectName": "Radio Recordings",
  "description": "Recordings preserved from radio archives.",
  "tags": ["radio"],
  "keywords": ["archive"],
  "person":     { /* PersonSummary */ },
  "categories": [ /* CategorySummary */ ],
  "mediaCounts": { "audios": 26, "videos": 4, "texts": 2, "images": 9 },
  "createdAt": "2026-01-04T08:00:00Z",
  "updatedAt": "2026-02-11T10:04:00Z",
  "trending": false
}
```

`mediaCounts` counts only **public, non-trashed** media, so it matches what a visitor can open.

## Category

```jsonc
{
  "id": 3,
  "categoryCode": "CAT-003",
  "name": "Music",
  "description": "Musical recordings and related material.",
  "keywords": ["music", "song"],
  "projectCount": 42,
  "createdAt": "2026-01-04T08:00:00Z",
  "trending": false
}
```

## Person

```jsonc
{
  "id": 1,
  "personCode": "PER-001",
  "mediaPortrait": "/api/guest/image/IMG-0007/view",
  "fullName": "Hasan Zirak",
  "nickname": "Zirak",
  "romanizedName": "Hasan Zirak",
  "gender": "MALE",
  "personType": ["Singer", "Performer"],
  "region": "Sulaymaniyah",
  "dateOfBirth": "1921-03-15",
  "dateOfBirthPrecision": "DAY",
  "placeOfBirth": "Bokan",
  "dateOfDeath": "1972-06-05",
  "dateOfDeathPrecision": "DAY",
  "placeOfDeath": "Sulaymaniyah",
  "description": "Kurdish singer…",
  "projectCount": 12,
  "trending": true,
  "trendingRank": 1,
  "trendingScore": 42.0
}
```

| Enum | Values |
|---|---|
| `gender` | `MALE` · `FEMALE` · `OTHER` · `UNKNOWN` |
| `dateOfBirthPrecision`, `dateOfDeathPrecision` | `DAY` · `MONTH` · `YEAR` · `DECADE` · `CENTURY` · `UNKNOWN` |

Precision tells you **how much of the date to print**: `YEAR` means show `1921`, not
`15 March 1921`.

---

# Part 2 — Website search

The two endpoints the search box uses. Start here.

## 1. `GET /api/guest/media/search`

One keyword across audio, video, image and text — merged into one ranked list, with per-kind counts
for the tab bar.

### Request

```
GET /api/guest/media/search
```

**Keyword**

| Param | Type | Default | Description |
|---|---|---|---|
| `q` | string | — | What the visitor typed. Matched across titles, codes, credits, project, person, categories, tags, keywords, subjects, genres, places and free text on all four kinds. Blank = browse the newest, not "no results". |

**Selection and shaping**

| Param | Type | Default | Description |
|---|---|---|---|
| `type` | repeatable / comma list | all four | `audio`, `video`, `image`, `text`. Aliases: `sound(s)`→audio, `photo(s)`→image, `file(s)`/`document(s)`→text. Selects `content` only — **never** `counts`. Unknown values are ignored, not fatal. |
| `sort` | string | `relevance` with `q`, else `newest` | `relevance` · `newest` · `oldest` · `title` · `trending` |
| `include` | string | `summary` | `full` attaches the complete kind-specific object |
| `groupBy` | string | `none` | `type` adds per-kind sections under `groups` |
| `facets` | boolean | `false` | `true` adds refine counts over the matched set |
| `page` | int | `0` | Zero-based |
| `size` | int | `24` | Clamped to `100` |

**Filters** — all optional, all compose with `q` and with each other.

| Param | Type | Matches |
|---|---|---|
| `projectCode` | string | Exact project code |
| `categoryCode` | string | Exact category code, via the owning project |
| `personCode` | string | Exact person code, via the owning project |
| `language` · `dialect` · `region` | string | Exact, case-insensitive |
| `subject` · `genre` · `tag` · `keyword` | repeatable | Any-match |
| `dateFrom` · `dateTo` | ISO date or instant | Inclusive range on `dateCreated`. A plain `2019-12-31` covers the whole day |
| `decade` | `1970` or `1970s` | That decade only — the value the `decades` facet reports |

Dates accept `2020-01-01T00:00:00Z`, `2020-01-01T00:00:00` (read as UTC) or `2020-01-01`. An
unparseable value is **ignored**, not rejected — a bad filter widens the search, never empties it.

```bash
curl -s "https://khiarchiveplatformbackend-production.up.railway.app/api/guest/media/search?q=Hasan%20Zirak&size=24&facets=true"
```

```js
const results = await api('/api/guest/media/search', {
  q: 'Hasan Zirak', size: 24, facets: true,
});
```

### Response `200`

```jsonc
{
  "query": "Hasan Zirak",
  "type": "all",
  "sort": "relevance",
  "order": ["audio", "video", "image", "text"],

  "counts": { "total": 41, "audio": 26, "video": 4, "image": 9, "text": 2 },

  "content": [
    {
      "type": "audio",
      "code": "AUD-0142",
      "id": 142,
      "title": "Hasan Zirak — Live in Sulaymaniyah",
      "subtitle": "حەسەن زیرەک — لایڤ",
      "description": "A 1971 concert recording…",
      "creator": "Hasan Zirak",
      "creatorRole": "singer",
      "projectCode": "PRJ-014",
      "projectName": "Radio Recordings",
      "person": { "id": 1, "personCode": "PER-001", "fullName": "Hasan Zirak",
                  "mediaPortrait": "/api/guest/image/IMG-0007/view" },
      "categories": [{ "id": 3, "categoryCode": "CAT-003", "name": "Music" }],
      "language": "Kurdish",
      "region": "Sulaymaniyah",
      "tags": ["concert", "live"],
      "duration": "04:12",
      "dateCreated": "1971-06-01T00:00:00Z",
      "mediaUrl": "/api/guest/audio/AUD-0142/stream",
      "thumbnailUrl": "/api/guest/image/IMG-0007/view",
      "detailUrl": "/api/guest/media/audio/AUD-0142",
      "score": 34.117,
      "matchedIn": ["title", "person", "creator"],
      "trending": true,
      "trendingRank": 3
    }
    // … 23 more
  ],

  "page": 0,
  "size": 24,
  "totalElements": 41,
  "totalPages": 2,
  "numberOfElements": 24,
  "first": true,
  "last": false,
  "empty": false,
  "hasNext": true,
  "hasPrevious": false,

  "truncated": false
}
```

| Field | Type | Description |
|---|---|---|
| `query` | string | The trimmed query that ran. `""` when browsing |
| `type` | string | `all`, or a comma list in public order |
| `sort` | string | **The sort actually applied** after defaulting — bind your dropdown to this |
| `order` | string[] | `["audio","video","image","text"]` — the fixed public order |
| `counts` | object | `total`, `audio`, `video`, `image`, `text`. **Always all four**, whatever `type` selects |
| `content` | [MediaHit](#mediahit)[] | The ranked page |
| `page`, `size`, `totalElements`, `totalPages`, `numberOfElements` | number | Note: `page`, **not** `number` — this endpoint does not use the Spring envelope |
| `first`, `last`, `empty`, `hasNext`, `hasPrevious` | boolean | |
| `groups` | object | Only with `groupBy=type` |
| `facets` | object | Only with `facets=true` |
| `truncated` | boolean | A kind hit the 500-row scan cap; counts are a floor |

> **`counts` vs `totalElements`.** `counts` covers all four kinds so the tab bar never changes when
> the tab changes. `totalElements` covers only the selected kinds. With `type=all` they agree, and a
> tab's count is always exactly the `totalElements` you get when you select that tab.

### With `groupBy=type`

```bash
curl -s ".../api/guest/media/search?q=Hasan%20Zirak&groupBy=type&size=6"
```

Adds, alongside `content`:

```jsonc
"groups": {
  "audio": {
    "kind": "audio",
    "content": [ /* MediaHit[] */ ],
    "page": 0, "size": 6,
    "totalElements": 26, "totalPages": 5, "numberOfElements": 6,
    "first": true, "last": false, "empty": false
  },
  "video": { "kind": "video", "content": [ … ], "totalElements": 4,  … },
  "image": { "kind": "image", "content": [ … ], "totalElements": 9,  … },
  "text":  { "kind": "text",  "content": [ … ], "totalElements": 2,  … }
}
```

Each section pages independently with the same `page`/`size`. Use this for a
"3 sounds · 2 videos · 4 photos" preview layout.

### With `facets=true`

```jsonc
"facets": {
  "languages": [{ "label": "Kurdish", "count": 31 }],
  "dialects":  [{ "label": "Sorani",  "count": 22 }],
  "regions":   [{ "label": "Sulaymaniyah", "count": 18 }],
  "subjects":  [{ "label": "Music", "count": 26 }],
  "genres":    [{ "label": "Folk",  "count": 14 }],
  "tags":      [{ "label": "concert", "count": 9 }],
  "keywords":  [{ "label": "live", "count": 7 }],
  "persons":   [{ "code": "PER-001", "label": "Hasan Zirak", "count": 33 }],
  "projects":  [{ "code": "PRJ-014", "label": "Radio Recordings", "count": 12 }],
  "decades":   [{ "label": "1950s", "count": 4 }, { "label": "1960s", "count": 19 }]
}
```

**How to send a facet back — the one rule that trips people up:**

| Facet | Send | As parameter |
|---|---|---|
| `languages` | `label` | `language` |
| `dialects` | `label` | `dialect` |
| `regions` | `label` | `region` |
| `subjects` | `label` | `subject` |
| `genres` | `label` | `genre` |
| `tags` | `label` | `tag` |
| `keywords` | `label` | `keyword` |
| `decades` | `label` | `decade` |
| **`persons`** | **`code`** | `personCode` |
| **`projects`** | **`code`** | `projectCode` |

Sending a person's *name* as `personCode` matches nothing and returns an empty page with **no
error** — a hard bug to spot. Max 30 buckets each, ordered by count desc then label asc; `decades`
is ordered oldest-first instead, because it reads as a timeline.

Facets cover the whole matched set for the selected kinds, not just the current page, so the
numbers hold steady while the visitor pages.

### With `include=full`

Each hit additionally carries the complete kind-specific object on the field matching its `type`:

```jsonc
{
  "type": "audio",
  "code": "AUD-0142",
  "title": "…",
  "audio": { /* the full Audio object — see Part 1 */ }
}
```

Roughly triples the payload. Use it only where the extra fields are actually rendered.

### How ranking works

With `sort=relevance` the order comes from a score built per hit:

1. The query splits into tokens: `Hasan Zirak` → `["hasan", "zirak"]`.
2. Each token scores against each field group as **group weight × match strength**:

| Group | Weight | | Match strength | Value |
|---|---|---|---|---|
| `title` | 10 | | field equals the token | 3.0 |
| `code` | 9 | | field starts with the token | 2.0 |
| `creator`, `person` | 8 | | a word inside starts with it | 1.6 |
| `project` | 5 | | any other substring | 1.0 |
| `tags`, `keywords`, `category` | 4 | | no match | 0 |
| `subject`, `genre` | 3 | | | |
| `place` | 2 | | | |
| `description` | 1 | | | |

3. Each token keeps its best group; the token scores are averaged.
4. Bonuses: all tokens matched `+6`; the whole phrase appears verbatim in a title `+8` (elsewhere
   `+2`); a top-20 trending item up to `+2`.
5. Ties fall back to the database's own fuzzy ranking (prefix → substring → trigram similarity).

The practical effect: a recording *titled* "Hasan Zirak — Live" beats a photograph whose
description merely mentions him, and one whose **person** is Hasan Zirak beats one that names him
once in a note — across kinds, in one list.

The other sorts ignore the score: `newest`/`oldest` by `dateCreated` (falling back to
`datePublished`), `title` case-insensitively, `trending` by `trendingScore`. All fall back to `code`
last, so paging is stable.

### Edge cases

| Situation | Response |
|---|---|
| No `q`, no filters | `200`, newest public media of every kind |
| Nothing matches | `200`, `content: []`, counts all zero, `empty: true` — never `404` |
| Unknown `type` value | `200`, search widens to all four kinds |
| Unparseable `dateFrom`/`dateTo`/`decade` | `200`, filter ignored |
| `size` over 100 | `200`, clamped to 100 |
| `page` past the end | `200`, empty `content`, correct `totalElements` |

### More examples

```bash
B=https://khiarchiveplatformbackend-production.up.railway.app

# Only his recordings, newest first
curl -s "$B/api/guest/media/search?q=Hasan%20Zirak&type=audio&sort=newest"

# Sounds and videos together, 1970s only
curl -s "$B/api/guest/media/search?q=Hasan%20Zirak&type=sounds,videos&decade=1970s"

# Everything for one person, no keyword
curl -s "$B/api/guest/media/search?personCode=PER-001&sort=newest&size=48"

# Narrow by language and two tags, page 2
curl -s "$B/api/guest/media/search?q=maqam&language=Kurdish&tag=concert&tag=live&page=1&size=24"

# Grouped preview with full payloads
curl -s "$B/api/guest/media/search?q=Hasan%20Zirak&groupBy=type&include=full&size=6"
```

---

## 2. `GET /api/guest/media/{type}/{code}`

Opens one result using the `type` + `code` pair its card already carries — no per-kind routing on
the frontend.

### Request

| Param | Where | Default | Description |
|---|---|---|---|
| `type` | path | — | `audio` · `video` · `image` · `text`, plus aliases `sound`, `photo`, `file`, `document` |
| `code` | path | — | The item's public code |
| `related` | query | `true` | `false` skips loading the rest of the project |

```bash
curl -s ".../api/guest/media/audio/AUD-0142"
curl -s ".../api/guest/media/text/TXT-0056?related=false"
```

### Response `200`

```jsonc
{
  "type": "audio",
  "code": "AUD-0142",

  "item": { /* MediaHit — the flat card, for the page header */ },

  "audio": { /* the full Audio object — identical to GET /api/guest/audios/AUD-0142 */ },

  "related": [ /* MediaHit[] — up to 12, this item excluded */ ]
}
```

| Field | Type | Description |
|---|---|---|
| `type` | string | The normalized kind — `sound` in becomes `audio` here |
| `code` | string | |
| `item` | [MediaHit](#mediahit) | The same flat card the search returns |
| `audio` \| `video` \| `image` \| `text` | object | **Exactly one is present**, the one naming `type` |
| `related` | [MediaHit](#mediahit)[] | Omitted entirely with `related=false` |

```js
const item = await api(`/api/guest/media/${type}/${code}`);
const payload = item[item.type];        // item.audio | item.video | item.image | item.text
```

`related` is the "more from this collection" rail. It is **interleaved kind by kind** — one audio,
one video, one image, one text, then round again — so the rail never fills with thirty photographs
from the same shoot. Empty when the item has no project, or the project holds nothing else public.

### Response `404`

Returned when the code is unknown, when `type` is not one of the four kinds, and when the item is
trashed or not public. **The three are deliberately indistinguishable** — the endpoint cannot be
used to probe for non-public records. Render one honest "not available" page for all three.

---

# Part 3 — Discovery

## 3. `GET /api/guest/suggest`

Autocomplete for the search box. Deliberately lighter than a full search — this is what should run
as the visitor types.

### Request

| Param | Type | Required | Default | Description |
|---|---|---|---|---|
| `q` | string | **yes** | — | The partial term. Blank returns `[]`; **omitting it entirely is a `400`** with `"error": "MISSING_PARAMETER"` |
| `limit` | int | no | `10` | Max rows. Capped at `50` |

```bash
curl -s ".../api/guest/suggest?q=hasan&limit=8"
```

### Response `200`

A **plain array** — not a page envelope:

```jsonc
[
  { "value": "Hasan Zirak",                  "kind": "person",   "code": "PER-001" },
  { "value": "Radio Recordings",             "kind": "project",  "code": "PRJ-014" },
  { "value": "Music",                        "kind": "category", "code": "CAT-003" },
  { "value": "Hasan Zirak — Live in Sulay…", "kind": "audio",    "code": "AUD-0142" },
  { "value": "Hasan Zirak on Stage",         "kind": "video",    "code": "VID-0031" },
  { "value": "Collected Songs of Hasan Z…",  "kind": "text",     "code": "TXT-0056" },
  { "value": "Hasan Zirak, portrait",        "kind": "image",    "code": "IMG-0007" }
]
```

| Field | Type | Description |
|---|---|---|
| `value` | string | The text to display — a name, project name, category name, or media title |
| `kind` | string | `project` · `category` · `person` · `audio` · `video` · `text` · `image` |
| `code` | string | The business code — route straight to that item |

**Order matters:** projects, categories and persons come first, then media fills the remaining
slots, split evenly across the four kinds. So the top of the dropdown is people and collections,
which is usually what a visitor means.

**Routing a suggestion.** `person`, `project` and `category` are not media, so they go to their own
pages:

```js
const ROUTE = {
  person:   (c) => `/person/${c}`,
  project:  (c) => `/collection/${c}`,
  category: (c) => `/category/${c}`,
  audio:    (c) => `/item/audio/${c}`,
  video:    (c) => `/item/video/${c}`,
  image:    (c) => `/item/image/${c}`,
  text:     (c) => `/item/text/${c}`,
};
navigate(ROUTE[s.kind](s.code));
```

Debounce **300 ms**, require 2+ characters, and abort the previous request. This endpoint writes
nothing to the trending logs, so calling it often is harmless.

---

## 4. `GET /api/guest/trending`

The home page's "Trending Now" and "Popular Searches". Cached server-side for 5 minutes.

### Request

No parameters.

```bash
curl -s ".../api/guest/trending"
```

### Response `200`

```jsonc
{
  "generatedAt": "2026-09-04T09:00:00Z",

  "trendingItems": [
    {
      "rank": 1,
      "score": 42.0,
      "kind": "audio",
      "code": "AUD-0142",
      "title": "Hasan Zirak — Live in Sulaymaniyah",
      "thumbnail": "/api/guest/image/IMG-0007/view",
      "projectCode": "PRJ-014",
      "projectName": "Radio Recordings",
      "personCode": "PER-001",
      "personName": "Hasan Zirak",
      "audio": { /* the full Audio object */ }
    }
    // … up to 20
  ],

  "trendingByType": {
    "audio": [ /* up to 5 TrendingItem */ ],
    "video": [ /* up to 5 */ ],
    "text":  [ /* up to 5 */ ],
    "image": [ /* up to 5 */ ]
  },

  "topSearches": [
    { "query": "hasan zirak", "count": 142 },
    { "query": "maqam",       "count": 37 }
  ]
}
```

| Field | Type | Description |
|---|---|---|
| `generatedAt` | ISO instant | When the snapshot was computed — drives "updated N minutes ago" |
| `trendingItems` | array | Top 20 across every kind, `rank` 1-based |
| `trendingByType` | object | Up to 5 per kind, for per-kind rails |
| `topSearches` | array | Top 10 queries of the last 24 hours |

**TrendingItem** carries a flat header (`rank`, `score`, `kind`, `code`, `title`, `thumbnail`,
`projectCode`, `projectName`, `personCode`, `personName`) **plus** the full object on the field
matching `kind` — one of `audio`, `video`, `text`, `image`, `project`, `person`. Note `kind` here
can also be `project` or `person`, which `/media/search` never returns.

**Scoring:** viewed in the last hour = 5 points, last 24 h = 2, last 7 days = 1. `topSearches`
counts `q` values sent to `/api/guest/search`, `/feed` and `/media/search` over 24 hours.

Frontend uses:

```js
trending.trendingItems.slice(0, 5)      // hero carousel
trending.trendingByType.audio           // "Trending Sounds" rail
trending.topSearches                    // "Popular Searches" chips
// any card whose code is in trendingItems with rank <= 10 → show a 🔥 badge
```

---

## 5. `GET /api/guest/facets`

Sidebar counts across the **whole public archive** — not scoped to a query. Use this for a browse
page's filter sidebar; for search-result facets use
[`/media/search?facets=true`](#with-facetstrue) instead, which counts only the matched set.

### Request

No parameters.

```bash
curl -s ".../api/guest/facets"
```

### Response `200`

```jsonc
{
  "mediaTypes": { "audios": 1240, "videos": 210, "texts": 340, "images": 2100, "projects": 180 },
  "categories": [{ "code": "CAT-003", "label": "Music", "count": 42 }],
  "persons":    [{ "code": "PER-001", "label": "Hasan Zirak", "count": 12 }],
  "languages":  [{ "label": "Kurdish", "count": 3100 }],
  "dialects":   [{ "label": "Sorani",  "count": 2400 }],
  "regions":    [{ "label": "Sulaymaniyah", "count": 900 }],
  "genres":     [{ "label": "Folk", "count": 610 }],
  "tags":       [{ "label": "concert", "count": 88 }],
  "keywords":   [{ "label": "radio", "count": 120 }]
}
```

Every bucket is `{ code?, label, count }`. `categories` and `persons` carry a `code` — send it as
`categoryCode` / `personCode`. Everything else sends `label`. Max 50 buckets per facet, ordered by
count desc then label asc.

---

## 6. `GET /api/guest/feed`

The browse page: the four media kinds, each in its own independently paged section. Use this when
there is **no keyword** and you want a balanced page; use `/media/search` when there is one.

### Request

| Param | Type | Default | Description |
|---|---|---|---|
| `q` | string | — | Free text across all four kinds |
| `types` | repeatable | all four | `image` · `audio` · `video` · `text` (aliases `photo`, `sound`) |
| `projectCode`, `categoryCode`, `personCode` | string | — | Exact codes |
| `language`, `dialect`, `region` | string | — | Exact, case-insensitive |
| `subject`, `genre`, `tag`, `keyword` | repeatable | — | Any-match |
| `dateFrom`, `dateTo` | ISO date | — | Inclusive range on `dateCreated` |
| `sortBy` | string | `relevance` with `q`, else `date` | `relevance` · `date` · `datePublished` · `title` |
| `sortDirection` | string | `desc` for dates, `asc` otherwise | `asc` · `desc` |
| `page`, `size` | int | `0`, `50` | **Applied to each section independently** |

```bash
curl -s ".../api/guest/feed?size=12&types=image&types=audio"
```

### Response `200`

```jsonc
{
  "order": ["image", "audio", "video", "text"],

  "images": {
    "kind": "image",
    "content": [ /* full Image objects */ ],
    "page": 0, "size": 12,
    "totalElements": 2100, "totalPages": 175, "numberOfElements": 12,
    "first": true, "last": false, "empty": false
  },
  "audios": { "kind": "audio", "content": [ /* full Audio objects */ ], … },
  "videos": { "kind": "video", "content": [ /* full Video objects */ ], … },
  "texts":  { "kind": "text",  "content": [ /* full Text objects  */ ], … },

  "totalElements": 3890,
  "page": 0,
  "size": 12,
  "hasNext": true,
  "hasPrevious": false
}
```

> **`size=12` returns up to 12 of *each* kind — 48 rows, not 12.** Pagination is per section, which
> is the whole point: it guarantees photographs cannot bury the sounds on page 0.

Sections carry the **full** kind-specific objects, not the flat card, so nothing kind-specific is
lost. The display order is fixed at photos → sounds → videos → texts; `sortBy` orders *within* each
section.

Unselected kinds still appear as empty sections. Projects and persons are never in the feed — they
have their own endpoints.

Calling this with a non-blank `q` writes to the trending log.

---

## 7. `GET /api/guest/search`

The original cross-entity search: seven sections, top-N each, including projects, categories and
persons. Kept for a "did you mean a person or a collection?" panel.

> For the website's search **results page**, use
> [`/api/guest/media/search`](#1-get-apiguestmediasearch) instead — this endpoint cannot page and
> its `total` is not a real total.

### Request

| Param | Type | Required | Default | Description |
|---|---|---|---|---|
| `q` | string | **yes** | — | Blank returns an empty result; **omitting it entirely is a `400`** with `"error": "MISSING_PARAMETER"` |
| `perSection` | int | no | `10` | Rows per section. Capped at `500` |

```bash
curl -s ".../api/guest/search?q=Hasan%20Zirak&perSection=5"
```

### Response `200`

```jsonc
{
  "query": "Hasan Zirak",
  "projects":   { "total": 3, "items": [ /* full Project objects  */ ] },
  "categories": { "total": 1, "items": [ /* full Category objects */ ] },
  "persons":    { "total": 1, "items": [ /* full Person objects   */ ] },
  "audios":     { "total": 5, "items": [ /* full Audio objects    */ ] },
  "videos":     { "total": 2, "items": [ /* full Video objects    */ ] },
  "texts":      { "total": 1, "items": [ /* full Text objects     */ ] },
  "images":     { "total": 5, "items": [ /* full Image objects    */ ] }
}
```

> **`total` is the number of items returned in that section, not the number that matched.** It can
> never exceed `perSection`. To show "41 results" or to page, call
> [`/media/search`](#1-get-apiguestmediasearch), whose `counts` are real.

Writes to the trending log when `q` is non-blank.

---

# Part 4 — Catalog

Projects (collections), categories and persons. All list endpoints return the standard
[Page envelope](#pagination); the `content` array holds the objects from
[Part 1](#part-1--shared-object-shapes).

## 8. `GET /api/guest/projects`

### Request

| Param | Type | Default | Description |
|---|---|---|---|
| `q` | string | — | Fuzzy search over project name, code, description, tags, keywords |
| `categoryCode` | string | — | Exact category code |
| `personCode` | string | — | Exact person code |
| `tag`, `keyword` | repeatable | — | Any-match |
| `sortBy` | string | — | `name` \| `code` \| `createdAt` \| `updatedAt` (aliases: `alpha`, `projectName`, `projectCode`, `created`, `added`, `updated`, `modified`) |
| `sortDirection` | string | `asc` | `asc` · `desc` |
| `page`, `size` | int | `0`, `50` | |

```bash
curl -s ".../api/guest/projects?q=radio&sortBy=name&size=20"
```

### Response `200`

```jsonc
{
  "content": [ /* Project objects — see Part 1 */ ],
  "totalElements": 12, "totalPages": 1, "number": 0, "size": 20,
  "numberOfElements": 12, "first": true, "last": true, "empty": false
}
```

## 9. `GET /api/guest/projects/{projectCode}`

One project by code. Logs a view for trending.

```bash
curl -s ".../api/guest/projects/PRJ-014"
```

**`200`** → a single [Project](#project) object.
**`404`** → unknown code, or the project is hidden or trashed.

## 10. `GET /api/guest/projects/{projectCode}/media`

Everything public inside one collection — the collection page.

| Param | Type | Default | Description |
|---|---|---|---|
| `type` | string | all | `audio` · `video` · `text` · `image` (plurals accepted). Anything else, or omitted, returns all four |

```bash
curl -s ".../api/guest/projects/PRJ-014/media"
curl -s ".../api/guest/projects/PRJ-014/media?type=audio"
```

### Response `200`

Not a page — a flat object with one array per kind. Keys for kinds you did not ask for are absent:

```jsonc
{
  "projectCode": "PRJ-014",
  "projectName": "Radio Recordings",
  "audios": [ /* full Audio objects */ ],
  "videos": [ /* full Video objects */ ],
  "texts":  [ /* full Text objects  */ ],
  "images": [ /* full Image objects */ ]
}
```

> **Unpaged — it returns the whole collection.** Fine for a normal project; for a very large one,
> prefer `/media/search?projectCode=PRJ-014`, which pages.

## 11. `GET /api/guest/categories`

| Param | Type | Default | Description |
|---|---|---|---|
| `q` | string | — | Fuzzy over name, code, description, keywords |
| `page`, `size` | int | `0`, **`100`** | Note the larger default |

```bash
curl -s ".../api/guest/categories?size=100"
```

**`200`** → Page of [Category](#category) objects.

## 12. `GET /api/guest/categories/{categoryCode}`

```bash
curl -s ".../api/guest/categories/CAT-003"
```

**`200`** → a single [Category](#category). **`404`** → unknown code.

## 13. `GET /api/guest/categories/{categoryCode}/projects`

Public projects filed under one category.

| Param | Type | Default |
|---|---|---|
| `page`, `size` | int | `0`, `50` |

```bash
curl -s ".../api/guest/categories/CAT-003/projects?page=0&size=24"
```

**`200`** → Page of [Project](#project) objects.

## 14. `GET /api/guest/persons`

| Param | Type | Default | Description |
|---|---|---|---|
| `q` | string | — | Fuzzy over full name, nickname, romanized name, code, description |
| `region` | string | — | Exact, case-insensitive |
| `gender` | enum | — | `MALE` · `FEMALE` · `OTHER` · `UNKNOWN` |
| `personType` | repeatable | — | Any-match, e.g. `personType=Singer&personType=Poet` |
| `page`, `size` | int | `0`, `50` | |

```bash
curl -s ".../api/guest/persons?q=zirak&personType=Singer"
```

**`200`** → Page of [Person](#person) objects.

> An invalid `gender` value is a **`400`** with `"error": "TYPE_MISMATCH"`, not a silent ignore — it
> is a typed enum. Send only the four values above. Every other filter on this endpoint is a plain
> string and is ignored when it matches nothing.

## 15. `GET /api/guest/persons/{personCode}`

```bash
curl -s ".../api/guest/persons/PER-001"
```

**`200`** → a single [Person](#person). Logs a view for trending. **`404`** → unknown code.

## 16. `GET /api/guest/persons/{personCode}/projects`

The person's collections — the backbone of a person page.

| Param | Type | Default |
|---|---|---|
| `page`, `size` | int | `0`, `50` |

```bash
curl -s ".../api/guest/persons/PER-001/projects"
```

**`200`** → Page of [Project](#project) objects.

To list a person's **media** rather than their collections, use
`/media/search?personCode=PER-001`.

---

# Part 5 — Media catalogs

Four per-kind catalogs with deep, kind-specific filters. Use these when you need a filter
`/media/search` does not expose (singer, ISBN, colour, photostory, recording venue…). For the
search page itself, `/media/search` is the better call.

**These four share a common core:**

| Param | Type | Default | Description |
|---|---|---|---|
| `q` | string | — | Fuzzy free text across every field and child collection of that kind |
| `projectCode`, `categoryCode`, `personCode` | string | — | Exact codes |
| `language`, `dialect`, `region` | string | — | Exact, case-insensitive |
| `subject`, `genre`, `tag`, `keyword` | repeatable | — | Any-match |
| `dateFrom`, `dateTo` | ISO date | — | Inclusive range on `dateCreated` |
| `publishedFrom`, `publishedTo` | ISO date | — | Inclusive range on `datePublished` |
| `sortBy` | string | — | `title` \| `code` \| `date` \| `published` \| `createdAt` (aliases below) |
| `sortDirection` | string | `asc` | `asc` · `desc` |
| `page`, `size` | int | `0`, `50` | |

**`sortBy` aliases**, identical on all four kinds:

| Sorts by | Accepted values |
|---|---|
| Title | `title` · `name` · `alpha` · `alphabet` · `originalTitle` (audio: `originTitle`) |
| Code | `code` · `audioCode`/`videoCode`/`textCode`/`imageCode` |
| Creation date | `date` · `dateCreated` |
| Publication date | `published` · `datePublished` |
| Upload date | `createdAt` · `created` · `added` |

Anything else is ignored and the relevance/natural order stands.

---

## 17. `GET /api/guest/audios`

Common core **plus** these audio-only filters:

| Param | Type | Match | Description |
|---|---|---|---|
| `singer` | string | contains | |
| `speaker` | string | contains | |
| `poet` | string | contains | |
| `composer` | string | contains | |
| `producer` | string | contains | |
| `contributor` | repeatable | any-match | |
| `form` | string | exact | e.g. `Song` |
| `typeOfBasta` | string | exact | |
| `typeOfMaqam` | string | exact | e.g. `Bayat` |
| `typeOfComposition` | string | exact | |
| `typeOfPerformance` | string | exact | e.g. `Live` |
| `recordingVenue` | string | contains | |
| `city` | string | exact | |
| `audience` | string | exact | |
| `lyrics` | string | contains | Searches inside the lyrics text |

```bash
curl -s ".../api/guest/audios?singer=Hasan%20Zirak&typeOfMaqam=Bayat&sortBy=date&sortDirection=desc"
```

**`200`** → Page of [Audio](#audio) objects.

```jsonc
{
  "content": [ /* Audio objects */ ],
  "totalElements": 26, "totalPages": 1, "number": 0, "size": 50,
  "numberOfElements": 26, "first": true, "last": true, "empty": false
}
```

## 18. `GET /api/guest/audios/{audioCode}`

```bash
curl -s ".../api/guest/audios/AUD-0142"
```

**`200`** → a single [Audio](#audio) object. Logs a view for trending.
**`404`** → unknown code, or not public.

Play it with `API_BASE + audio.audioFileUrl`.

---

## 19. `GET /api/guest/videos`

Common core **plus**:

| Param | Type | Match |
|---|---|---|
| `event` | string | exact |
| `location` | string | exact |
| `creatorArtistDirector` | string | contains |
| `producer` | string | contains |
| `contributor` | string | contains |
| `personShownInVideo` | string | contains |
| `subtitle` | string | contains |
| `audience` | string | exact |
| `provenance` | string | exact |
| `videoStatus` | string | exact |
| `publisher` | string | contains |
| `color` | repeatable | any-match — e.g. `color=Black and white` |
| `whereUsed` | repeatable | any-match |

```bash
curl -s ".../api/guest/videos?creatorArtistDirector=Aram&color=Black%20and%20white"
```

**`200`** → Page of [Video](#video) objects.

## 20. `GET /api/guest/videos/{videoCode}`

```bash
curl -s ".../api/guest/videos/VID-0031"
```

**`200`** → a single [Video](#video). Logs a view. **`404`** → unknown or not public.

---

## 21. `GET /api/guest/texts`

Common core **plus**:

| Param | Type | Match |
|---|---|---|
| `documentType` | string | exact — e.g. `Book`, `Manuscript` |
| `isbn` | string | contains |
| `author` | string | contains |
| `contributors` | string | contains |
| `script` | string | exact — e.g. `Arabic`, `Latin` |
| `series` | string | contains |
| `edition` | string | exact |
| `volume` | string | exact |
| `printingHouse` | string | contains |
| `audience` | string | exact |
| `provenance` | string | exact |
| `publisher` | string | contains |
| `printDateFrom`, `printDateTo` | ISO date | Inclusive range on `printDate` |

```bash
curl -s ".../api/guest/texts?author=Hejar&documentType=Book&printDateFrom=1980-01-01"
```

**`200`** → Page of [Text](#text) objects.

## 22. `GET /api/guest/texts/{textCode}`

```bash
curl -s ".../api/guest/texts/TXT-0056"
```

**`200`** → a single [Text](#text). Logs a view. **`404`** → unknown or not public.

---

## 23. `GET /api/guest/images`

Common core **plus**:

| Param | Type | Match |
|---|---|---|
| `event` | string | exact |
| `location` | string | exact |
| `creatorArtistPhotographer` | string | contains |
| `contributor` | string | contains |
| `personShownInImage` | string | contains |
| `audience` | string | exact |
| `provenance` | string | exact |
| `photostory` | string | contains |
| `imageStatus` | string | exact |
| `color` | repeatable | any-match |
| `whereUsed` | repeatable | any-match |

```bash
curl -s ".../api/guest/images?personShownInImage=Hasan&color=Black%20and%20white&size=48"
```

**`200`** → Page of [Image](#image) objects.

## 24. `GET /api/guest/images/{imageCode}`

```bash
curl -s ".../api/guest/images/IMG-0007"
```

**`200`** → a single [Image](#image). Logs a view. **`404`** → unknown or not public.

---

# Part 6 — Files and playback

These five return **bytes, not JSON**. Their URLs already arrive on the objects you fetched —
`audioFileUrl`, `videoFileUrl`, `imageFileUrl`, `textFileUrl`, `coverImageUrl`, and `mediaUrl` /
`thumbnailUrl` on a MediaHit. Prepend `API_BASE` and use them directly:

```jsx
<audio src={API_BASE + audio.audioFileUrl} controls preload="metadata" />
<video src={API_BASE + video.videoFileUrl} controls preload="metadata" playsInline />
<img   src={API_BASE + image.imageFileUrl} alt="" loading="lazy" />
```

> **Never build an S3 URL.** The bucket is not reachable from a browser and the object key is not in
> any response. Bytes are always proxied through the backend.
>
> **Never send an `Authorization` header.** A browser cannot attach headers to `<img src>` or
> `<audio src>` — which is exactly why these endpoints are anonymous.

## 25. `GET /api/guest/audio/{audioCode}/stream`

### Request

| Header | Required | Description |
|---|---|---|
| `Range` | no | e.g. `bytes=0-1048575`. The native player sends this automatically when seeking |

```bash
curl -sI ".../api/guest/audio/AUD-0142/stream"
curl -sI -H "Range: bytes=0-1023" ".../api/guest/audio/AUD-0142/stream"
```

### Response `200` (no `Range`) / `206` (with `Range`)

Body is the audio bytes. Headers:

| Header | Value |
|---|---|
| `Content-Type` | Inferred from the stored extension — `audio/mpeg`, `audio/wav`, … |
| `Accept-Ranges` | `bytes` |
| `Content-Length` | Bytes in **this** response, not the whole file |
| `Content-Range` | `bytes 0-1023/4218880` — only on a `206` |
| `Content-Disposition` | `inline; filename="…"; filename*=UTF-8''…` — Kurdish/Arabic filenames survive intact |
| `Cache-Control` | `public, max-age=300` |
| `X-Content-Type-Options` | `nosniff` |

`404` — unknown code, trashed, or the stored file is missing from storage.
`500` — storage failure (network, permissions, throttling).

## 26. `GET /api/guest/video/{videoCode}/stream`

Identical contract to the audio stream: `Range` in, `206` out, `Accept-Ranges: bytes`,
`Cache-Control: public, max-age=300`. `Content-Type` is `video/mp4`, `video/quicktime`, …

```bash
curl -sI -H "Range: bytes=0-1023" ".../api/guest/video/VID-0031/stream"
```

Seeking in `<video>` works because of the `206` support — no extra client code needed.

## 27. `GET /api/guest/image/{imageCode}/view`

Caching here is by **ETag**, not `Range`.

### Request

| Header | Required | Description |
|---|---|---|
| `If-None-Match` | no | The `ETag` from a previous response. The browser sends it for you |

```bash
curl -sI ".../api/guest/image/IMG-0007/view"
curl -sI -H 'If-None-Match: "a1b2c3d4"' ".../api/guest/image/IMG-0007/view"
```

### Response `200` or `304`

| Status | Body | When |
|---|---|---|
| `200` | The image bytes, with `ETag` and `Content-Type: image/jpeg` (or png, webp…) | First load |
| `304` | Empty | Your `If-None-Match` matched — the browser reuses its cached copy and **S3 is never touched** |

The `ETag` is derived from the image code and is stable, because image bytes never change after
upload. Set `loading="lazy"` on grid thumbnails and let the browser do the rest.

## 28. `GET /api/guest/text/{textCode}/read`

The document itself — usually a PDF.

| Header | Required | Description |
|---|---|---|
| `Range` | no | Supported, so pdf.js can fetch page ranges instead of the whole book |

```bash
curl -sI ".../api/guest/text/TXT-0056/read"
```

`200` / `206` with `Content-Type: application/pdf` (or the stored type),
`Accept-Ranges: bytes`, and `Content-Disposition: inline`. Range support is what makes a large
book open quickly in a viewer.

## 29. `GET /api/guest/text/{textCode}/cover`

The book cover, ETag-cached exactly like an image view.

```bash
curl -sI ".../api/guest/text/TXT-0056/cover"
```

`200` with `ETag`, or `304`.

> **Only call this when `coverImageUrl` is present on the Text object.** It is deliberately omitted
> when no cover exists, so an absent field means "there is no cover", not "try anyway".

---

# Part 7 — Putting it together

## Which endpoint for which page

| Page | Call |
|---|---|
| Search results | `GET /api/guest/media/search` |
| Autocomplete dropdown | `GET /api/guest/suggest` |
| Any item's detail page | `GET /api/guest/media/{type}/{code}` |
| Home — trending rails, popular searches | `GET /api/guest/trending` |
| Home — browse feed, no keyword | `GET /api/guest/feed` |
| Browse sidebar counts | `GET /api/guest/facets` |
| Collection page | `GET /api/guest/projects/{code}` + `/projects/{code}/media` |
| Category page | `GET /api/guest/categories/{code}` + `/categories/{code}/projects` |
| Person page | `GET /api/guest/persons/{code}` + `/persons/{code}/projects` |
| Person's media | `GET /api/guest/media/search?personCode={code}` |
| A kind-specific catalog with deep filters | `/audios` · `/videos` · `/texts` · `/images` |
| Playing or reading bytes | The five endpoints in [Part 6](#part-6--files-and-playback) |

## A complete search flow

```js
import { api } from '@/lib/api';
export const API_BASE = 'https://khiarchiveplatformbackend-production.up.railway.app';

// 1 — the visitor types (debounce 300 ms, 2+ chars)
const suggestions = await api('/api/guest/suggest', { q: 'hasan', limit: 8 });

// 2 — they submit
const results = await api('/api/guest/media/search', {
  q: 'Hasan Zirak', page: 0, size: 24, facets: true,
});

results.counts;          // { total: 41, audio: 26, video: 4, image: 9, text: 2 }  → tab bar
results.content;         // MediaHit[]                                            → result grid
results.facets;          // refine panel
results.totalPages;      // pagination

// 3 — they click the Sounds tab (page resets to 0; counts do NOT change)
const audioOnly = await api('/api/guest/media/search', {
  q: 'Hasan Zirak', type: 'audio', page: 0, size: 24, facets: true,
});

// 4 — they tick "Kurdish" in the refine panel (send bucket.label as `language`)
const refined = await api('/api/guest/media/search', {
  q: 'Hasan Zirak', type: 'audio', language: 'Kurdish', page: 0, size: 24, facets: true,
});

// 5 — they open a result
const hit  = results.content[0];                          // { type: 'audio', code: 'AUD-0142' }
const item = await api(`/api/guest/media/${hit.type}/${hit.code}`);
const full = item[item.type];                             // the complete Audio object

// 6 — they press play
const src = API_BASE + full.audioFileUrl;
```

## The eight mistakes that cost the most time

| Mistake | Symptom | Fix |
|---|---|---|
| `tag[0]=concert` instead of `tag=concert` | Filter silently does nothing | axios: `paramsSerializer: { indexes: null }` |
| Person facet sent as `label` | Empty results, no error | `persons` and `projects` send `code`, everything else sends `label` |
| Reading `data.number` on `/media/search` | `undefined` | That endpoint uses `page`; the others use `number` |
| Tab counts read from `totalElements` | Numbers jump when tabs change | Tabs read `counts`; `totalElements` is the selected kinds only |
| Filter changed without resetting `page` | Empty page after refining | Reset `page` to 0 on every change except paging |
| `key={hit.id}` in a mixed list | Cards swap content while paging | `key={`${hit.type}:${hit.code}`}` — ids repeat across kinds |
| Building an S3 URL, or `baseURL + path` where baseURL ends in `/api` | `/api/api/...`, broken media | Join against the **origin** |
| Treating `404` as a routing bug | "Page not found" for a private record | `404` also means "not public" — say "not available" |

## Also worth knowing

- **`thumbnailUrl` is often absent** on sounds and videos — they have no still of their own, so the
  API falls back to the project person's portrait, and there may not be one. Design the card for the
  no-image case first.
- **`description` on a MediaHit is already trimmed** to 320 characters on a word boundary. Do not
  fetch `include=full` just to get a longer one — render it as it arrives.
- **Kurdish and Arabic text**: put `dir="auto"` on every element showing an archive string, so the
  browser picks direction per field. A page-wide `dir="rtl"` is wrong for a mixed list. Add
  `lang="ckb"` on Central Kurdish so fonts and screen readers resolve.
- **Nothing is cached server-side** for search. A short client-side cache keyed on the full query
  string is worth having.
- **Every call carrying a non-blank `q`** to `/media/search`, `/search` or `/feed` is logged and
  feeds `GET /api/guest/trending`. That is where "Popular Searches" comes from.

---

## Related

- [`10-website-search.md`](./10-website-search.md) — the design rationale behind
  `/api/guest/media/search`: why it exists, how the ranking is built, what it replaces
- [`06-media.md`](./06-media.md) — the per-kind catalogs in the backend's own words
- [`07-streaming.md`](./07-streaming.md) — the byte proxies in more depth
- [`02-errors.md`](./02-errors.md) — the complete `ErrorCode` set
- [`01-conventions.md`](./01-conventions.md) — paging, dates, null omission, CORS
- [`../FRONTEND_INTEGRATION.md`](../FRONTEND_INTEGRATION.md) — the axios client, CORS allowlist,
  environment variables and local dev setup
- [`../internal/README.md`](../internal/README.md) — the staff endpoints that need a token

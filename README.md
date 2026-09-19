# Fast Radio Final 8

This build keeps the user-supplied CustomRadioPlayer station list completely separate from the Iran Radio/RadioBrowser list. The supplied `stations.txt` is imported as `app/src/main/assets/custom_imported_stations.json` with 113 entries and their supplied icon URLs where present. It is not merged into the Iran list.

## Panels
- Custom Radio: imported stations.txt, independent ▲/▼ scrolling, icons, Add and Import.
- Iran Radio: separate RadioBrowser-by-country list, independent ▲/▼ scrolling. No custom list is injected into it.
- Special Persian: BBC Persian and VOA Persian are separate from both lists.
- World Radio: opens from WORLD and uses RadioBrowser.

## Playback
Media3/ExoPlayer with 5–10 second buffer, HTTP/HTTPS redirect support, HLS and DASH modules, plus a compatibility player fallback.

## Low-data server
The app does not fake lower data usage with a client bitrate limit. If a real Fast Radio transcoder server is configured, it requests actual AMR-NB/Opus/MP3 output from FFmpeg. Without a configured server it plays the original stream.

## Recording
With the real server configured, REC starts server-side AMR-NB recording and STOP downloads the resulting `.amr` file to `Music/Fast Radio`.

## Current Persian sources
BBC Persian live stream was found in a current playlist source updated in September 2026; VOA Persian has an official radio page but its page currently reports no live internet stream, so the direct VOA HLS URL included here is treated as a third-party current stream source and may change. The app also keeps the official VOA homepage as the station homepage.


## Final 9 additions
- Online RadioBrowser search.
- Independent TV subtitle ticker with ON/OFF and previous/next.
- Independent news-source ticker with ON/OFF and previous/next.
- Yellow sun-light visual at a random position, gently fading in/out for about 10 seconds once per minute.
- BBC Persian and VOA Persian remain in a separate special list.

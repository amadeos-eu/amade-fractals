# amade-fractals – Architecture

Fractal explorer of amadeOS. It uses Java 21, Swing, Guice and [amade-commons](https://github.com/datazuul/amade-commons), is built with Gradle and licensed under the EUPL-1.2. The structure follows amade-imagescan.

Packaging:
- a Debian package
- an MSI and a portable app image for Windows, built with jpackage

Languages: English, German, French, Spanish and Italian. The language follows the amadeOS desktop.

## Key decisions

- **Values first, colours afterwards.** For each pixel, the calculation stores:
  - how its orbit ended (inside, escaped, converged to root *k*)
  - the iteration count
  - the last *z*
  - the closest approach to 0

  This is `FractalData`. `Colorizer` turns these values into RGB with one palette lookup per pixel. So palettes, density, shift, colouring method and colour cycling never calculate again.
- **The region is exact, the pixels are `double`.** `Region` keeps the centre as `BigDecimal`, rounded to the digits the zoom needs, and the height of the view as `double`. Zooming, framing and panning are exact operations on it. The width follows from the window, so one region fits any window or export size.
- **Deep zoom by perturbation with rebasing.**
  - Below a pixel size of about 4·10⁻¹⁵ the Mandelbrot set switches to `Perturbation`.
  - One reference orbit Z at the centre is calculated in `BigDecimal`, with the pixel's digits plus a margin.
  - Every pixel iterates dz ← (2Z + dz)·dz + dc in `double`.
  - Where |Z + dz| < |dz| or the reference ends, the pixel continues from the start of the reference with dz = Z + dz (Zhuoran's rebasing). No glitch detection and no second references are needed, and the result matches arbitrary precision (see `CalculationTest`).
  - The limit is the exponent of `double`, about 10⁻²⁹⁰.
  - Formulas without perturbation refuse to zoom past their precision instead of showing noise.
- **Every gesture answers at once.**
  - The canvas moves and scales the old picture with an `AffineTransform` while the presenter starts the new calculation. The transform ends when the new picture arrives, or never starts if the presenter refuses (limit reached).
  - A large picture is first calculated at a quarter of its size.
  - A newer view cancels the running calculation.
- **Rows to whichever core is free.** `Renderer` hands out rows one at a time from an atomic counter to one task per core, on daemon platform threads just below normal priority. Rows inside the set and rows at the boundary differ a lot in cost; a fixed split would leave cores idle.
- **Cheap inside points.**
  - The Mandelbrot main cardioid and period-2 bulb are recognised by formula.
  - Orbits that close on themselves are caught by a periodicity check (Brent-style, comparing with a saved value at doubling intervals). This is not done for Phoenix, whose state includes the previous value.
- **Exports in strips.** An export renders and colours 128 rows at a time into the final RGB picture. Each strip has its own region, with the same pixel size and its own centre. Memory then holds the picture plus one strip of values, not 21 bytes per pixel of the whole export.
- **Testable without a display.** The presenter, `Region`, the renderer and the colouring have no Swing code. Executors are injected, so tests run calculations and continuations in a deterministic queue (`QueueExecutor`).

## Packages

| Package | Contents |
|---|---|
| `bootstrap` | `AmadeFractalsApplication` (start through `AmadeApplication`), `FractalsModule` (Guice; worker pool, `RenderService`, texts) |
| `domain` | `Formula` (iteration, Julia form, home region, default seed), `Region` (centre as `BigDecimal`, height; zoom, frame, pan), `FractalSettings` (formula, power, Julia seed, iterations, bailout, region; limits of `double`), `ColorSettings`, `Coloring`, `Palette` (cyclic gradients as 1024-entry lookup tables; `DESKTOP` from the theme), `Place`, `Iterations` (iterations for a magnification), `ImageFormat` |
| `service.render` | `PointCalculator` (all formulas in `double`), `Perturbation` (reference orbit and deep pixels), `Renderer` (rows across the cores, cancellable, progress), `FractalData`, `RenderService` (background, preview, export in strips, results on the EDT) |
| `service.color` | `Colorizer` (smooth n + 1 − log(log\|z\|)/log d, bands, binary decomposition, orbit trap; Newton by root, darker with iterations) |
| `repository` | `ParameterFiles` (`.fractal`, UTF-8 properties, all digits of the centre), `ImageFiles` (PNG/JPEG, atomic writes), `FractalsPreferences` (`~/.config/amadeos/fractals/fractals.properties`: bars, automatic iterations, export size) |
| `ui.main` | `MainPresenter` (all commands and the history, no Swing), `MainView`, `MainWindow`, `MainMenuBar`, `MainToolBar`, `SettingsPanel` (fractal, location, colours), `FractalActions` (one action per command for menus and toolbar), `Formats` (magnification "1.5 × 10⁶", coordinates) |
| `ui.view` | `FractalCanvas` (picture, gestures, transform until the next picture, frame with the window's shape) |
| `ui.icons` | `ToolIcon` (toolbar icons drawn in Java2D in the theme's colours) |

## Flows

- **Start**
  1. The window opens.
  2. The presenter renders the Mandelbrot set for the size of the canvas, or opens the `.fractal` file named on the command line.
- **Change of view**
  1. Every change of `FractalSettings` (gesture, settings panel, menu, place, file) goes through `apply`. This puts the old settings on the back stack (100 steps), clears the forward stack, updates panel, menu, title and zoom, and renders.
  2. Zooming in with *Raise when zooming* on raises the iterations to `Iterations.suggested(magnification)`, never lowers them.
- **Rendering**
  1. `RenderService.render` runs on the virtual-thread executor from amade-commons, which hands the rows to the worker pool.
  2. A coarse preview at a quarter of the size comes first if the picture has more than 200 000 pixels.
  3. Progress is reported at most every 50 ms.
  4. Results reach the presenter through the `@Ui` executor, and are dropped if the task was cancelled meanwhile.
- **Colours**
  - Colour changes recolour the existing `FractalData` into the same `BufferedImage`.
  - Colour cycling is a 40 ms Swing timer that moves the palette shift.
  - The Desktop palette is recoloured when the theme changes (`AmadeUI.style` on the canvas).
- **Export**
  1. Size dialog (window size, Full HD, QHD, 4K, 8K, A4 at 300 dpi, square, or custom), then the file chooser.
  2. Rendering strip by strip in the background, then an atomic write.
  3. Exports are limited to 64 megapixels and 20 000 pixels a side. A running export is confirmed before quitting.
- **Theme and language**
  - Colours come from `AmadeUI` and follow the Style Manager.
  - Texts are in `messages*.properties`, checked by `TranslationCheck`, including a test against clashing mnemonics.

## Dependencies

- Guice, amade-commons, SLF4J/Logback.
- Everything else comes from the JDK: `BigDecimal`, ImageIO, Swing.

## Planned

- Zoom videos (frame sequences from the whole set down to a place)
- Extended-exponent numbers for zooms past 10⁻²⁹⁰, and perturbation for other formulas and Julia sets
- Distance estimation and stripe-average colouring, a palette editor
- Bookmarks of your own places

# amade-fractals

**amade-fractals** is the fractal explorer of the [amadeOS](https://github.com/datazuul/amadeos) desktop: render the Mandelbrot set and its relatives, zoom in with a click, a frame or the mouse wheel, follow a Julia set from any point, colour the result and export it as a large picture. In the spirit of [Fractal Zoomer](https://github.com/hrkalona/Fractal-Zoomer), built like the other amadeOS applications.

![License](https://img.shields.io/badge/license-EUPL--1.2-blue)
![Java](https://img.shields.io/badge/Java-21-orange)

## Features

- **Nine formulas**: Mandelbrot set (z² + c), Multibrot (zⁿ + c, n = 3 to 12), Burning Ship, Tricorn (Mandelbar), Celtic, Buffalo, Phoenix, Magnet and Newton's method for z³ − 1. Every escape-time formula also has a **Julia form**. *Ctrl+click* on a point of the Mandelbrot form shows the Julia set of that point, and the seed can also be typed in.
- **Navigation**:
  - click zooms in 2× at the point, right click zooms out
  - drag a frame to fill the window with it
  - the mouse wheel zooms at the pointer
  - drag with the right or middle button to move the picture
  - *Back* and *Forward* go through the history, *Home* shows the whole fractal again

  Each gesture answers at once: the old picture is moved and scaled while the new one is calculated, and a coarse preview comes first.
- **Deep zoom**: the Mandelbrot set zooms far below the precision of `double`, down to about 10⁻²⁹⁰, using **perturbation**. There is one reference orbit in arbitrary precision (`BigDecimal`), and every pixel follows its small distance from it in `double`. When the reference stops being any good for a pixel, the pixel goes back to the start of the reference (*rebasing*), so no glitch detection is needed. The centre keeps as many digits as the zoom needs. The other formulas stop where `double` ends and say so.
- **Iterations** rise automatically while zooming in (*Raise when zooming*, can be switched off), and can be doubled or halved with Ctrl+I / Ctrl+Shift+I.
- **Colouring**:
  - smooth (continuous iteration count), escape-time bands, binary decomposition, orbit trap
  - eight palettes; *Desktop Colours* follows the amadeOS colour theme
  - density and shift of the palette
  - **colour cycling**

  Changing colours never calculates again.
- **Places**: Seahorse Valley, Elephant Valley, Triple Spiral, Mini Mandelbrot, a deep zoom 10²⁰ times, Dendrite, Douady Rabbit and San Marco Dragon (Julia sets), the Burning Ship armada, Phoenix.
- **Files**:
  - *Save Image* stores the picture as shown (PNG or JPEG).
  - *Export Image* renders the view at any size up to 64 megapixels (Full HD, 4K, 8K, A4 at 300 dpi, or your own). Export works strip by strip, so memory holds little more than the finished picture.
  - Parameter files (`.fractal`) keep formula, place (all digits), iterations and colours. *Copy Parameters* puts them on the clipboard to share a place.
  - A `.fractal` file given on the command line is opened.
- Calculation on all processor cores; the rows go to whichever core is free.
- Five languages (English, Deutsch, Français, Español, Italiano), following the amadeOS desktop language; *Options → Language* overrides it.
- Theme colours, Motif look and 3D toolbar buttons from [amade-commons](https://github.com/datazuul/amade-commons). The toolbar icons are drawn in Java2D, so no icon theme is needed.

## Architecture

Java 21, Swing and Guice, built like [amade-imagescan](https://github.com/datazuul/amade-imagescan). Theme, language and start-up come from `AmadeApplication` in amade-commons. Packages, key decisions and flows are described in [ARCHITECTURE.md](ARCHITECTURE.md).

## Build and run

```sh
./gradlew run                              # start
./gradlew run --args=seahorse.fractal      # open a parameter file
./gradlew test                             # Swing tests need a display: xvfb-run ./gradlew test
./gradlew deb                              # build/distributions/amade-fractals_<version>_all.deb
./gradlew installToAmadeOs                 # the .deb into ../amadeos/config/packages.chroot/ for the ISO build
./gradlew windowsInstaller                 # on Windows: MSI with its own Java runtime (needs WiX Toolset 3)
./gradlew windowsInstaller -PinstallerType=app-image   # portable folder with amade-fractals.exe
```

If `../amade-commons` is checked out next to this repository, Gradle builds against that checkout instead of the JitPack version.

## Planned

- Zoom videos: a sequence of frames from the whole set down to a place
- Deep zoom past 10⁻²⁹⁰ (floating-point numbers with an extended exponent), and perturbation for the other formulas and Julia sets
- More colouring methods (distance estimation, stripe average) and a palette editor
- Bookmarks of your own places

## Licence

EUPL-1.2, see [LICENSE](LICENSE).

# Thumbs
JavaFX Scala GUI thumbnail browser.

Works with large directories.
### Navigation
```
Double click    Browse to a directory
Right click     Open image under pointer full size, any key to close
Arrows / Wheel  Scroll up and down
+ / -           Make thumbnails larger / smaller
Home            Return to top
```
### Reference
```
https://github.com/google/guava                 For caching.
https://github.com/haraldk/TwelveMonkeys        Awesome easy as pie plugins and extensions for Java ImageIO.
```
TODO: Maybe a smooth scroll. Work out where the columnar oddness at some sizes comes from on Windows 10.

sbt assembly to build an executable JAR, a bit fatter due to FX modules.

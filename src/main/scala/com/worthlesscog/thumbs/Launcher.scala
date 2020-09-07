package com.worthlesscog.thumbs

import java.io.{File, FilenameFilter}

import com.google.common.cache.CacheBuilder
import javafx.application.{Application, Platform}
import javafx.beans.property.{SimpleIntegerProperty => IntProp, SimpleObjectProperty => ObjProp}
import javafx.scene.control.Labeled
import javafx.scene.image.Image
import javafx.scene.input.{InputEvent, KeyCode, KeyEvent, MouseButton, MouseEvent, ScrollEvent}
import javafx.scene.layout.{Pane, StackPane, TilePane}
import javafx.scene.paint.Color
import javafx.stage.{DirectoryChooser, Modality, Screen, Stage}
import javax.imageio.ImageIO

import scala.collection.JavaConverters._

class Thumbs extends Application {

    val BACKGROUND_LOADING = true
    val CACHED_IMAGES = 8192
    val INITIAL_COLUMNS = 8
    val PRESERVE_ASPECT = true
    val SMOOTHER_THUMBNAILS = true
    val TITLE = "Thumbs"

    val bounds = Screen.getPrimary.getBounds
    val cache = CacheBuilder.newBuilder().maximumSize(8192).build[String, Image]()
    val formats = ImageIO.getReaderFormatNames.map("." +).toSet
    val imageFilter: FilenameFilter = (_, name) => formats exists { name endsWith }

    def start(stage: Stage): Unit =
        startImplicit(stage)

    def startImplicit(implicit stage: Stage) {
        implicit val status = defaultLabel("")
        implicit val stack = defaultStackPane()

        val cols = new IntProp()
        val dim = new ObjProp[Dim]()
        val dir = new ObjProp[File]()
        val files = new ObjProp[Array[File]](Array.empty)
        val pages = new Array[TilePane](2)
        val pane = new ObjProp[TilePane]()
        val panes = new Array[TilePane](2)
        val row = new IntProp()

        changeListener(setDimensions(row, dim)) |> cols.addListener
        changeListener(resize(pane, panes, pages, files)) |> dim.addListener
        changeListener(displayPane) |> pane.addListener
        changeListener(resetDirectory(row, files)) |> dir.addListener
        changeListener(initialLoad(dim, pane, panes, pages)) |> files.addListener

        val scene = defaultBorderPane(stack, bottom = status) |> defaultScene

        eventHandler(click(dir)) |> stack.setOnMouseClicked
        eventHandler(keyboard(dim, pane, panes, pages, files, row, cols)) |> scene.setOnKeyPressed
        eventHandler(scroll(dim, pane, panes, pages, files, row)) |> scene.setOnScroll

        cols.set(INITIAL_COLUMNS)

        stage.setResizable(false)
        stage.setScene(scene)
        stage.setTitle(TITLE)
        stage.show()
    }

    def setDimensions(row: IntProp, dim: ObjProp[Dim])(cols: Number) {
        reset(row)
        dimensions(cols.intValue) |> dim.set
    }

    def reset(row: IntProp) {
        cache.invalidateAll()
        row.set(0)
    }

    def dimensions(cols: Int) = {
        val tile = (((bounds.getWidth * 9) / 10) / cols).toInt
        val rows = (((bounds.getHeight * 9) / 10) / tile).toInt
        Dim(cols, rows, tile, tile)
    }

    def resize(pane: ObjProp[TilePane], panes: Array[TilePane], pages: Array[TilePane], files: ObjProp[Array[File]])(dim: Dim)(implicit stage: Stage, status: Labeled) {
        newPane(dim) |> pane.set
        panes(1) = newPane(dim)
        pages(1) = newPane(dim)

        stage.sizeToScene()
        stage.centerOnScreen()

        loadPanes(dim, pane.get, panes(1), pages(1), files.get)
    }

    def newPane(dim: Dim) = {
        val p = defaultTilePane(Color.DARKGRAY)
        p.setMinHeight(dim.height)
        p.setMinWidth(dim.width)
        p.setPrefColumns(dim.cols)
        p.setPrefRows(dim.rows)
        p.setPrefTileHeight(dim.tileHeight)
        p.setPrefTileWidth(dim.tileWidth)
        p
    }

    def loadPanes(dim: Dim, curr: TilePane, next: TilePane, page: TilePane, files: Array[File])(implicit status: Labeled) =
        Platform.runLater(() => {
            loadPane(dim, 0, files)(curr)
            loadPane(dim, 1, files)(next)
            loadPane(dim, dim.rows, files)(page)
        })

    def loadPane(dim: Dim, row: Int, files: Array[File])(p: TilePane)(implicit status: Labeled) = {
        p.getChildren.clear()
        loadTiles(dim, row, files).asJava |> p.getChildren.addAll
        p
    }

    def loadTiles(dim: Dim, row: Int, files: Array[File])(implicit status: Labeled) = {
        // files.slice(row * dim.cols, row * dim.cols + dim.tiles) map loadTile(dim.tileWidth, dim.tileHeight)
        val end = (row * dim.cols + dim.tiles) min files.length
        for (i <- row * dim.cols until end) yield files(i) |> loadTile(dim.tileWidth, dim.tileHeight)
    }

    def loadTile(w: Int, h: Int)(f: File)(implicit status: Labeled) = {
        val v = cache.get(f.getName, () => cacheImage(f, w, h)) |> defaultImageView
        eventHandler(statusUpdate(f)) |> v.setOnMouseEntered
        eventHandler(popup(f)) |> v.setOnMouseClicked
        defaultBorderPane(v)
    }

    def cacheImage(f: File, w: Int, h: Int) =
        new Image("file:" + f.getPath, w, h, PRESERVE_ASPECT, SMOOTHER_THUMBNAILS, BACKGROUND_LOADING)

    def statusUpdate(f: File)(e: MouseEvent)(implicit status: Labeled) =
        status.setText(f getPath)

    def popup(f: File)(e: MouseEvent) =
        if (e |> singleRightClick) {
            // TODO: scale down to display bounds if required or don't bother?
            val i = new Image("file:" + f.getPath) |> defaultImageView |> hbox |> defaultScene

            val s = new Stage()
            eventHandler(closeOnInput(s)) |> i.setOnKeyPressed
            eventHandler(closeOnInput(s)) |> i.setOnMouseClicked
            s.initModality(Modality.APPLICATION_MODAL)
            s.setResizable(false)
            s.setScene(i)
            s.setTitle(f getName)
            s.show()
        }

    def singleRightClick(e: MouseEvent) =
        e.getClickCount == 1 && e.getButton == MouseButton.SECONDARY

    def closeOnInput(s: Stage)(e: InputEvent) =
        s.close()

    def displayPane(pane: Pane)(implicit stack: StackPane) {
        stack.getChildren.clear()
        stack.getChildren.add(pane)
    }

    def resetDirectory(row: IntProp, files: ObjProp[Array[File]])(dir: File) {
        reset(row)
        dir.listFiles(imageFilter).sortWith(_.getName < _.getName) |> files.set
    }

    def initialLoad(dim: ObjProp[Dim], pane: ObjProp[TilePane], panes: Array[TilePane], pages: Array[TilePane])(files: Array[File])(implicit status: Labeled) =
        loadPanes(dim.get, pane.get, panes(1), pages(1), files)

    def click(last: ObjProp[File])(e: MouseEvent)(implicit stage: Stage) =
        if (e |> doubleClick) {
            val c = new DirectoryChooser()
            if (last.get != null)
                c.setInitialDirectory(last.get.getParentFile)
            c.showDialog(stage) match {
                case null =>
                case dir  => last.set(dir)
            }
        }

    def doubleClick(e: MouseEvent) =
        e.getClickCount == 2 && e.getButton == MouseButton.PRIMARY

    def keyboard(dim: ObjProp[Dim], pane: ObjProp[TilePane], panes: Array[TilePane], pages: Array[TilePane], files: ObjProp[Array[File]], row: IntProp, cols: IntProp)(e: KeyEvent)(implicit status: Labeled) =
        e.getCode match {
            case KeyCode.ADD =>
                if (cols.get > 2)
                    cols.set(cols.get - 1)

            case KeyCode.SUBTRACT =>
                if (cols.get < 25)
                    cols.set(cols.get + 1)

            case KeyCode.PAGE_DOWN =>
            // TODO

            case KeyCode.PAGE_UP =>
            // TODO

            case KeyCode.DOWN =>
                scrollDown(dim, pane, panes, files, row)

            case KeyCode.UP =>
                scrollUp(dim, pane, panes, files, row)

            case KeyCode.END =>
            // TODO

            case KeyCode.HOME =>
                if (row.get > 0) {
                    initialLoad(dim, pane, panes, pages)(files.get)
                    row.set(0)
                }

            case _ =>
        }

    def scrollDown(dim: ObjProp[Dim], pane: ObjProp[TilePane], panes: Array[TilePane], files: ObjProp[Array[File]], row: IntProp)(implicit status: Labeled) {
        val (d, f, r) = (dim.get, files.get, row.get)
        if (r * d.cols + d.tiles < f.length) {
            panes(0) = pane.get
            panes(1) |> pane.set
            panes(1) = loadNewPane(d, r + 2, f)
            // TODO - new page
            row.set(r + 1)
        }
    }

    def scrollUp(dim: ObjProp[Dim], pane: ObjProp[TilePane], panes: Array[TilePane], files: ObjProp[Array[File]], row: IntProp)(implicit status: Labeled) {
        val r = row.get
        if (r > 0) {
            panes(1) = pane.get
            panes(0) |> pane.set
            if (r > 1) {
                panes(0) = loadNewPane(dim.get, r - 2, files.get)
                // TODO - new page
            }
            row.set(r - 1)
        }
    }

    def scroll(dim: ObjProp[Dim], pane: ObjProp[TilePane], panes: Array[TilePane], pages: Array[TilePane], files: ObjProp[Array[File]], row: IntProp)(e: ScrollEvent)(implicit status: Labeled) =
        if (e.getDeltaY < 0)
            scrollDown(dim, pane, panes, files, row)
        else
            scrollUp(dim, pane, panes, files, row)

    def loadNewPane(dim: Dim, row: Int, files: Array[File])(implicit status: Labeled) = {
        val p = newPane(dim)
        Platform.runLater(() =>
            loadPane(dim, row, files)(p)
        )
        p
    }

}

object Launcher {

    def main(args: Array[String]) =
        Application.launch(classOf[Thumbs], args: _*)

}

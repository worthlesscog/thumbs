package com.worthlesscog

import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.scene.{Parent, Scene}
import javafx.scene.control.Label
import javafx.scene.layout.{HBox, StackPane}
// import javafx.concurrent.Task
import javafx.event.{Event, EventHandler}
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.image.{Image, ImageView}
import javafx.scene.layout.{Background, BackgroundFill, BorderPane, CornerRadii, TilePane}
import javafx.scene.paint.Color

package object thumbs {

    implicit class Pipe[A](a: A) {
        def |>[B](f: A => B): B = f(a)
    }

    case class Dim(cols: Int, rows: Int, tileWidth: Int, tileHeight: Int) {
        val height = tileHeight * rows
        val tiles = cols * rows
        val width = tileWidth * cols
    }

    // def backgroundTask[T](f: => T) = {
    //     val t = new Task[T] {
    //         def call = f
    //     }
    //     val thread = new Thread(t)
    //     thread.start()
    //     thread
    // }

    def changeListener[T](f: T => Unit) =
        new ChangeListener[T] {
            def changed(v: ObservableValue[_ <: T], oldV: T, newV: T) = f(newV)
        }

    def defaultBorderPane(centre: Node, top: Node = null, right: Node = null, bottom: Node = null, left: Node = null) = {
        val b = new BorderPane(centre)
        if (top != null) b.setTop(top)
        if (right != null) b.setRight(right)
        if (bottom != null) b.setBottom(bottom)
        if (left != null) b.setLeft(left)
        b
    }

    def defaultImageView(image: Image) =
        new ImageView(image)

    def defaultInsets() =
        new Insets(5)

    def defaultLabel(text: String) = {
        val l = new Label(text)
        l.setPadding(defaultInsets())
        l
    }

    def defaultScene(root: Parent) =
        new Scene(root)

    def defaultStackPane() =
        new StackPane()

    def defaultTilePane(colour: Color) = {
        val t = new TilePane()
        t.setBackground(flatBackground(colour))
        t
    }

    def eventHandler[T <: Event](h: T => Unit) =
        new EventHandler[T] {
            def handle(e: T) = h(e)
        }

    def flatBackground(colour: Color) =
        new Background(flatFill(colour))

    def flatFill(colour: Color) =
        new BackgroundFill(colour, CornerRadii.EMPTY, Insets.EMPTY)

    def hbox(n: Node) =
        new HBox(n)

    // def lruCache[K, V](capacity: Int) = new util.LinkedHashMap[K, V](capacity, 0.75f, true) {
    //     override def removeEldestEntry(eldest: util.Map.Entry[K, V]): Boolean = size > capacity
    // }

}

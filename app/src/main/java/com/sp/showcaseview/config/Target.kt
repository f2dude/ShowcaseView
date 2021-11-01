package com.sp.showcaseview.config

import android.graphics.Path
import android.graphics.RectF

/**
 * Class to draw the path around view container
 *
 * @author Saikrishna Pawar
 * @since 10/27/2021
 */
interface Target {

    /**
     * This path will be used when drawing the guide.
     * @return The path that will be drawn.
     */
    fun guidePath(): Path?

    /**
     * This rect is used when displaying the guide message.
     * If the guidePath is a circle then the bounding box should
     * be a square that contains the circle inside of it.
     * @return The rect that will used for positioning guide message.
     */
    fun boundingRect(): RectF?
}
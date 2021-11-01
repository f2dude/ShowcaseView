package com.sp.showcaseview.listener

import android.view.View

/**
 * Interface class to listen for view dismiss.
 *
 * @author Saikrishna Pawar
 * @since 10/27/2021
 */
interface GuideListener {

    /**
     * Called when the view is dismissed
     *
     * @param view View on which dismiss is called
     */
    fun onDismiss(view: View)
}
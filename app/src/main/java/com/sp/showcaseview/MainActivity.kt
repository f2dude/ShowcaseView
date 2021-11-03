package com.sp.showcaseview

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.sp.showcaseview.config.DismissType
import com.sp.showcaseview.config.ViewType
import com.sp.showcaseview.databinding.ActivityMainBinding
import com.sp.showcaseview.listener.GuideListener


class MainActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityMainBinding
    private var mShowcaseView: ShowcaseView? = null
    private var builder: ShowcaseView.Builder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        showIntro(mBinding.bottomNav)
    }

    private fun showIntro(currentView: View) {
        builder = ShowcaseView.Builder(this)
        builder?.let {
            it.setContentText("Get quick access to the main activities of your business")
            it.setContentTextSize(16)
            it.setDismissType(DismissType.ANYWHERE)
            it.setTargetView(currentView)
            it.setViewType(ViewType.BOTTOM_NAVIGATION)
            it.setContentBackgroundColor(ContextCompat.getColor(this, R.color.message_view_bg))
            it.setContentTextColor(ContextCompat.getColor(this, R.color.message_view_text_color))
            it.setGuideListener(object : GuideListener {
                override fun onDismiss(view: View) {
                    when (view.id) {
                        R.id.bottomNav -> {
                            it.setContentText("Quick access to all your chats.")
                            it.setTargetView(mBinding.tabView)
                            it.setViewType(ViewType.TAB_VIEW)
                        }
                        R.id.tabView -> {
                            it.setContentText(
                                "You’ll be notified every time a user requests to " +
                                        "chat with someone from your business."
                            )
                            it.setTargetView(mBinding.contactRequests)
                            it.setViewType(ViewType.CONTACTS_REQUESTS)
                        }
                        R.id.contactRequests -> {
                            return
                        }
                    }
                    mShowcaseView = it.build()
                    mShowcaseView?.show()
                }
            })
            mShowcaseView = it.build()
            mShowcaseView?.show()
        }
    }
}
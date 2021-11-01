package com.sp.showcaseview

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.sp.showcaseview.config.DismissType
import com.sp.showcaseview.config.PointerType
import com.sp.showcaseview.config.ViewType
import com.sp.showcaseview.databinding.ActivityMainBinding
import com.sp.showcaseview.listener.GuideListener


class MainActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityMainBinding
    private var mGuideView: GuideView? = null
    private var builder: GuideView.Builder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        showIntro(
            "Get quick access to the main activities of your business",
            mBinding.bottomNav
        )
    }

    private fun showIntro(text: String, currentView: View) {
        builder = GuideView.Builder(this)
        builder?.let {
            it.setContentText(text)
            it.setContentTextSize(16)
            it.setPointerType(PointerType.ARROW)
            it.setDismissType(DismissType.ANYWHERE)
            it.setTargetView(currentView)
            it.setViewType(ViewType.BOTTOM_NAVIGATION)
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
                    mGuideView = it.build()
                    mGuideView?.show()
                }
            })
            mGuideView = it.build()
            mGuideView?.show()
        }
    }
}
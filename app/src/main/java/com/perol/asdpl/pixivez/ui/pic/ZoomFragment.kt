package com.perol.asdpl.pixivez.ui.pic

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.viewpager.widget.ViewPager
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.BaseVBFragment
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.databinding.FragmentZoomBinding
import com.perol.asdpl.pixivez.ui.FragmentActivity

// zoom pic for viewing when clicked
class ZoomFragment : BaseVBFragment<FragmentZoomBinding>() {

    override fun loadData() {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as FragmentActivity).apply {
            hideAppBar()
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                            // Set the content to appear under the system bars so that the
                            // content doesn't resize when the system bars hide and show.
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            // Hide the nav bar and status bar
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
        val illust = requireArguments().getParcelable<Illust>("illust")!!
        val num = requireArguments().getInt("num", 0)
        val zoomPagerAdapter = ZoomPagerAdapter(requireContext(), illust)
        val size =
            if (illust.meta_pages.isEmpty()) {
                1
            } else {
                illust.meta_pages.size
            }
        binding.textviewZoom.text = getString(R.string.fractional, 1, size)
        binding.viewpageZoom.adapter = zoomPagerAdapter
        binding.viewpageZoom.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                binding.viewpageZoom.tag = position
                binding.textviewZoom.text = getString(R.string.fractional, position + 1, size)
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })
        binding.viewpageZoom.currentItem = num
    }

    companion object {
        fun start(mContext: Context, position: Int, illust: Illust) {
            FragmentActivity.start(mContext, "Zoom", "", Bundle().apply {
                putInt("num", position)
                putParcelable("illust", illust)
            })
        }
    }
}

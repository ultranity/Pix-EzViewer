package com.perol.asdpl.pixivez.fragments

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import java.util.*

abstract class BaseFragment<Layout : ViewDataBinding> : Fragment() {
    protected lateinit var rootView: View
    protected lateinit var binding: Layout
    protected var className = javaClass.simpleName + " "
    protected var mLayoutID = -1
    protected lateinit var mActivity: FragmentActivity
    protected lateinit var mContext: Context
    private var isVertical = false
    protected var isInit = false
    protected var uuid: String = UUID.randomUUID().toString()

    init {
        Log.d(className, " new Fragment instance uuid:$uuid")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            mActivity = requireActivity()
            mContext = requireContext()
            val bundle = arguments
            bundle?.let { initBundle(it) }
            val intent = mActivity.intent
            if (intent != null) {
                val activityBundle = intent.extras
                activityBundle?.let { initActivityBundle(it) }
            }
            initModel()

            //orientation
            val orientation = resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                isVertical = false
            } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                isVertical = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {
            isInit = true
            initLayout()
            if (mLayoutID != -1) {
                binding = DataBindingUtil.inflate(inflater, mLayoutID, container, false)
                rootView = binding.root
                initView()
                initData()
                return rootView
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        try {
            rootView.tag = uuid
            if (isVertical) {
                vertical()
            } else {
                horizon()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected abstract fun initLayout()
    protected fun initBundle(bundle: Bundle?) {}
    protected fun initActivityBundle(bundle: Bundle?) {}
    protected fun initView() {}
    protected fun initData() {}
    fun horizon() {}
    fun vertical() {}
    fun finish() {
        mActivity.finish()
    }

    fun initModel() {}
}
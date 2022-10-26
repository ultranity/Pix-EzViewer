/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
 * Copyright (c) 2019 Perol_Notsfsssf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 */

package com.perol.asdpl.pixivez.fragments.hellom

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.databinding.FragmentHelloMdynamicsBinding
import com.perol.asdpl.pixivez.objects.LazyFragment
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.viewmodel.RankingShareViewModel
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"

/**
 * A simple [Fragment] subclass.
 * Use the [HelloMTrendingFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class HelloMTrendingFragment : LazyFragment() {
    override fun loadData() {
    }

    private val modelist = arrayOf(
        "day", "day_male", "day_female", "week_original", "week_rookie", "week", "month", "day_r18", "day_male_r18", "day_female_r18", "week_r18", "week_r18g"
    )
    private val titles by lazy { resources.getStringArray(R.array.modellist) }
    private fun initView() {
        // viewpage_rankingm.adapter = RankingMAdapter(this, childFragmentManager)
        val shareModel =
            ViewModelProvider(requireActivity())[RankingShareViewModel::class.java]
        val isR18on = PxEZApp.instance.pre.getBoolean("r18on", false)
        for (i in modelist.indices) {
            if (!titles[i].contains("r18") or isR18on) {
                binding.tablayoutRankingm.addTab(
                    binding.tablayoutRankingm.newTab().setText(titles[i])
                )
            }
        }
        childFragmentManager.fragments.forEach {
            childFragmentManager.beginTransaction().remove(it).commit()
        }
        childFragmentManager.beginTransaction()
            .add(R.id.content_view, RankingMFragment.newInstance(modelist[0], 0)).commit()
        binding.tablayoutRankingm.getTabAt(0)!!.select()
        binding.tablayoutRankingm.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {}

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabSelected(tab: TabLayout.Tab) {
                childFragmentManager.beginTransaction().remove(childFragmentManager.fragments[0])
                    .add(
                        R.id.content_view,
                        RankingMFragment.newInstance(modelist[tab.position], tab.position)
                    )
                    .commit()
            }
        })
        // tablayout_rankingm.setupWithViewPager(viewpage_rankingm)
/*        TabLayoutMediator(tablayout_rankingm, viewpage_rankingm) { tab, position ->
            tab.text = resources.getStringArray(R.array.modellist)[position]
            viewpage_rankingm.setCurrentItem(tab.position, true)
        }.attach()*/
        val calendar = Calendar.getInstance()
        val yearNow = calendar.get(Calendar.YEAR)
        val monthNow = calendar.get(Calendar.MONTH) + 1
        val dayNow = calendar.get(Calendar.DAY_OF_MONTH)
        val dateNow = "$yearNow-$monthNow-$dayNow"
        binding.imageviewTriangle.apply {
            setOnClickListener {
                shareModel.apply {
                    val dateDialog = DatePickerDialog(
                        requireActivity(),
                        { p0, year1, month1, day1 ->
                            val monthR = month1 + 1
                            picDateShare.value = if ("$year1-$monthR-$day1" == dateNow) {
                                null
                            }
                            else {
                                "$year1-$monthR-$day1"
                            }
                            year.value = year1
                            month.value = month1
                            day.value = day1
                        },
                        year.value!!,
                        month.value!!,
                        day.value!!
                    )
                    dateDialog.datePicker.maxDate = System.currentTimeMillis()
                    dateDialog.show()
                }
            }
            setOnLongClickListener {
                shareModel.hideBookmarked.value = 1 - shareModel.hideBookmarked.value!!
                true
            }
        }
    }

    private var param1: String? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
        }
    }

    private lateinit var binding: FragmentHelloMdynamicsBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHelloMdynamicsBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @return A new instance of fragment HelloMDynamicsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String) =
            HelloMTrendingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}

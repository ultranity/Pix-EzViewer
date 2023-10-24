package com.chad.brvah.provider

import com.chad.brvah.BaseNodeAdapter
import com.chad.brvah.entity.node.BaseNode

abstract class BaseNodeProvider : BaseItemProvider<BaseNode>() {

    override fun getAdapter(): BaseNodeAdapter? {
        return super.getAdapter() as? BaseNodeAdapter
    }

}
package com.perol.asdpl.pixivez.ui.novel

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.AlignmentSpan
import android.text.style.ClickableSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.IntentActivity
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.data.model.Novel
import com.perol.asdpl.pixivez.data.model.NovelNaviItem
import com.perol.asdpl.pixivez.databinding.ActivityNovelBinding
import com.perol.asdpl.pixivez.databinding.ViewNovelChunkBinding
import com.perol.asdpl.pixivez.databinding.ViewNovelHeaderBinding
import com.perol.asdpl.pixivez.databinding.ViewNovelImageBinding
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.pic.PictureActivity
import com.perol.asdpl.pixivez.ui.user.UserMActivity

// 小说阅读页:头部(标题/作者/标签)+ 图文分块正文(标记渲染见 NovelMarkup),
// 字号记忆、收藏 toggle、系列上/下一篇
class NovelActivity : RinkActivity() {
    private lateinit var binding: ActivityNovelBinding
    private val viewModel: NovelViewModel by viewModels()
    private val adapter = NovelReaderAdapter(onJumpToPage = ::scrollToPage)
    private var novelId: Int = 0
    private var prev: NovelNaviItem? = null
    private var next: NovelNaviItem? = null
    private var textSize: Float = DEFAULT_TEXT_SIZE

    // 进度恢复只在首次数据就绪时执行一次
    private var progressRestored = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNovelBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.novel)

        novelId = intent.getIntExtra(EXTRA_NOVEL_ID, 0)
        textSize = PxEZApp.instance.pre.getFloat(PREF_TEXT_SIZE, DEFAULT_TEXT_SIZE)
        adapter.textSize = textSize
        binding.novelRecycler.adapter = adapter
        applyReaderTheme()

        initObserver()
        binding.novelRetry.setOnClickListener { viewModel.loadText(novelId) }
        viewModel.load(novelId)
    }

    // 背景主题:(背景色, 正文色);0=跟随主题走 XML/theme 默认
    private val bgThemes = listOf(
        0 to null,                                // follow(不覆盖)
        0xFFFAF7EF.toInt() to 0xFF333333.toInt(), // paper
        0xFFF0E4CC.toInt() to 0xFF5B4636.toInt(), // sepia
        0xFF000000.toInt() to 0xFFB0B0B0.toInt(), // black
    )

    private fun applyReaderTheme() {
        val bg = PxEZApp.instance.pre.getInt("novel_bg", 0)
        val (color, textColor) = bgThemes.getOrElse(bg) { bgThemes[0] }
        if (bg != 0) binding.root.setBackgroundColor(color)
        adapter.chunkTextColor = textColor
        adapter.lineSpacing = PxEZApp.instance.pre.getFloat("novel_line_space", 1.4f)
        adapter.notifyItemRangeChanged(1, adapter.chunks.size)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initObserver() {
        viewModel.bodyLoading.observe(this) { loading ->
            binding.novelLoading.visibility = if (loading) View.VISIBLE else View.GONE
            binding.novelRetry.isEnabled = !loading
        }
        viewModel.bodyFailed.observe(this) { failed ->
            binding.novelBodyError.visibility = if (failed) View.VISIBLE else View.GONE
        }
        viewModel.novel.observe(this) {
            adapter.novel = it
            adapter.notifyItemChanged(0)
        }
        viewModel.bookmarked.observe(this) { invalidateOptionsMenu() }
        viewModel.chunks.observe(this) {
            adapter.chunks = it.orEmpty()
            adapter.notifyDataSetChanged()
            if (!progressRestored && !it.isNullOrEmpty()) {
                progressRestored = true
                NovelProgressStore.load(novelId)?.let { p ->
                    (binding.novelRecycler.layoutManager as LinearLayoutManager)
                        .scrollToPositionWithOffset(p.position, p.offset)
                }
            }
        }
        viewModel.web.observe(this) { web ->
            prev = web?.seriesNavigation?.prevNovel
            next = web?.seriesNavigation?.nextNovel
            invalidateOptionsMenu()
        }
    }

    override fun onPause() {
        super.onPause()
        val lm = binding.novelRecycler.layoutManager as LinearLayoutManager
        val pos = lm.findFirstVisibleItemPosition()
        if (pos <= 0 || adapter.chunks.isEmpty()) return // header 处/未加载不记
        val offset = lm.findViewByPosition(pos)?.top ?: 0
        NovelProgressStore.save(novelId, pos, offset)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_novel, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // 收藏态用图标透明度区分(仅有一枚心形图标)
        menu.findItem(R.id.action_novel_bookmark).icon?.alpha =
            if (viewModel.bookmarked.value == true) 255 else 90
        menu.findItem(R.id.action_novel_prev).isVisible = prev != null
        menu.findItem(R.id.action_novel_next).isVisible = next != null
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_novel_bookmark -> viewModel.toggleBookmark(novelId)
            R.id.action_novel_font_larger -> changeFont(2f)
            R.id.action_novel_font_smaller -> changeFont(-2f)
            R.id.action_novel_prev -> prev?.let(::openNovel)
            R.id.action_novel_next -> next?.let(::openNovel)
            R.id.action_novel_bg -> {
                val labels = arrayOf(
                    getString(R.string.novel_bg_follow), getString(R.string.novel_bg_paper),
                    getString(R.string.novel_bg_sepia), getString(R.string.novel_bg_black)
                )
                val cur = PxEZApp.instance.pre.getInt("novel_bg", 0)
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.novel_background)
                    .setSingleChoiceItems(labels, cur) { d, i ->
                        PxEZApp.instance.pre.edit().putInt("novel_bg", i).apply()
                        d.dismiss()
                        // 跟随主题需要清掉已染的背景,直接重建最干净
                        recreate()
                    }.show()
            }
            R.id.action_novel_line_space -> {
                val values = floatArrayOf(1.2f, 1.4f, 1.8f)
                val labels = arrayOf("1.2", "1.4", "1.8")
                val cur = values.indexOfFirst {
                    it == PxEZApp.instance.pre.getFloat("novel_line_space", 1.4f)
                }.coerceAtLeast(0)
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.novel_line_space)
                    .setSingleChoiceItems(labels, cur) { d, i ->
                        PxEZApp.instance.pre.edit().putFloat("novel_line_space", values[i]).apply()
                        applyReaderTheme()
                        d.dismiss()
                    }.show()
            }
            R.id.action_novel_share -> share()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun changeFont(delta: Float) {
        textSize = (textSize + delta).coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE)
        adapter.textSize = textSize
        adapter.notifyItemRangeChanged(1, adapter.chunks.size)
        PxEZApp.instance.pre.edit().putFloat(PREF_TEXT_SIZE, textSize).apply()
    }

    // [jump:N] N 为 1 起页号;目标 = 首个 page==N-1 的块(header 占位 +1)
    private fun scrollToPage(page: Int) {
        val idx = adapter.chunks.indexOfFirst { it.page == page - 1 }
        if (idx < 0) return
        (binding.novelRecycler.layoutManager as LinearLayoutManager)
            .scrollToPositionWithOffset(1 + idx, 0)
    }

    private fun openNovel(item: NovelNaviItem) {
        if (!item.viewable) return
        start(this, item.id)
        finish()
    }

    private fun share() {
        val url = "https://www.pixiv.net/novel/show.php?id=$novelId"
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                },
                getString(R.string.share)
            )
        )
    }

    companion object {
        private const val EXTRA_NOVEL_ID = "novel_id"
        private const val PREF_TEXT_SIZE = "novel_text_size"
        private const val DEFAULT_TEXT_SIZE = 16f
        private const val MIN_TEXT_SIZE = 12f
        private const val MAX_TEXT_SIZE = 32f

        fun start(context: Context, novelId: Int) {
            context.startActivity(
                Intent(context, NovelActivity::class.java).putExtra(EXTRA_NOVEL_ID, novelId)
            )
        }
    }
}

// 正文分块适配器:0=头部,其后 Text/Image 两种块。
// Text 块 bind 时 tokenize→Spannable,测量摊到滚动过程,长文不阻塞主线程
private class NovelReaderAdapter(
    private val onJumpToPage: (Int) -> Unit
) : RecyclerView.Adapter<NovelReaderAdapter.VH>() {
    class VH(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)

    var novel: Novel? = null
    var chunks: List<NovelChunk> = emptyList()
    var textSize: Float = 16f

    // 背景主题联动的正文颜色/行距;null = 跟随 XML 默认
    var chunkTextColor: Int? = null
    var lineSpacing: Float = 1.4f

    override fun getItemViewType(position: Int) = when {
        position == 0 -> TYPE_HEADER
        chunks[position - 1] is NovelChunk.Image -> TYPE_IMAGE
        else -> TYPE_CHUNK
    }

    override fun getItemCount() = 1 + chunks.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        return VH(
            when (viewType) {
                TYPE_HEADER -> ViewNovelHeaderBinding.inflate(inflater, parent, false)
                TYPE_IMAGE -> ViewNovelImageBinding.inflate(inflater, parent, false)
                else -> ViewNovelChunkBinding.inflate(inflater, parent, false)
            }
        )
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        when (val b = holder.binding) {
            is ViewNovelHeaderBinding -> bindHeader(b)
            is ViewNovelImageBinding -> bindImage(b, chunks[position - 1] as NovelChunk.Image)
            is ViewNovelChunkBinding -> {
                b.novelChunk.textSize = textSize
                b.novelChunk.setLineSpacing(0f, lineSpacing)
                chunkTextColor?.let { b.novelChunk.setTextColor(it) }
                // 复用后的可选中 TextView 长按会失灵,重置一次可选中态恢复
                b.novelChunk.setTextIsSelectable(false)
                b.novelChunk.text = renderSpans((chunks[position - 1] as NovelChunk.Text).text)
                b.novelChunk.setTextIsSelectable(true)
                // 选中态之后设 movementMethod,链接与长按选择并存
                b.novelChunk.movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindHeader(b: ViewNovelHeaderBinding) {
        val n = novel ?: return
        b.novelTitle.text = n.title
        b.novelAuthor.text = n.user.name
        b.novelAuthor.setOnClickListener {
            UserMActivity.start(b.root.context, n.user.id)
        }
        b.novelMeta.text = "${n.text_length} · ♥ ${n.total_bookmarks}"
        b.novelSeries.visibility = if (n.series != null) View.VISIBLE else View.GONE
        b.novelSeries.text = n.series?.title
        b.novelCaption.visibility = if (n.caption.isBlank()) View.GONE else View.VISIBLE
        b.novelCaption.text = HtmlCompat.fromHtml(n.caption, HtmlCompat.FROM_HTML_MODE_LEGACY)
        // 每个标签一段 ClickableSpan → 小说搜索
        val sb = SpannableStringBuilder()
        for (tag in n.tags) {
            if (sb.isNotEmpty()) sb.append("  ")
            val start = sb.length
            sb.append("#${tag.name}")
            sb.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) =
                    NovelSearchResultActivity.start(widget.context, tag.name)
            }, start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        b.novelTags.text = sb
        b.novelTags.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun bindImage(b: ViewNovelImageBinding, chunk: NovelChunk.Image) {
        if (chunk.url == null) {
            // url 未解析:显示原始标记占位,不阻塞正文
            b.novelImage.visibility = View.GONE
            b.novelImageFallback.visibility = View.VISIBLE
            b.novelImageFallback.text =
                chunk.illustId?.let { "[pixivimage:$it]" } ?: "[uploadedimage]"
        } else {
            b.novelImage.visibility = View.VISIBLE
            b.novelImageFallback.visibility = View.GONE
            Glide.with(b.novelImage).load(chunk.url)
                .transition(withCrossFade()).into(b.novelImage)
        }
        b.root.setOnClickListener {
            chunk.illustId?.let { id -> PictureActivity.start(b.root.context, id) }
        }
    }

    // token → Spannable:chapter 加粗放大居中,ruby 括注,jumpuri/jump 可点
    private fun renderSpans(text: String): CharSequence {
        val sb = SpannableStringBuilder()
        for (t in tokenize(text)) when (t) {
            is NovelToken.Plain -> sb.append(t.text)
            is NovelToken.Ruby -> sb.append("${t.base}(${t.rt})")
            is NovelToken.Chapter -> {
                val append = chapterSpanAppend(
                    existingLength = sb.length,
                    existingEndsWithNewline = sb.isNotEmpty() && sb.last() == '\n',
                    title = t.title,
                )
                sb.append(append.text)
                sb.setSpan(StyleSpan(Typeface.BOLD), append.start, append.endExclusive, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(RelativeSizeSpan(1.25f), append.start, append.endExclusive, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(
                    AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                    append.start, append.endExclusive, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            is NovelToken.JumpUri -> {
                val start = sb.length
                sb.append(t.title.ifBlank { t.url })
                sb.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) = IntentActivity.start(widget.context, t.url)
                }, start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            is NovelToken.JumpPage -> {
                val start = sb.length
                sb.append("▶ P${t.page}")
                sb.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) = onJumpToPage(t.page)
                }, start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return sb
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CHUNK = 1
        private const val TYPE_IMAGE = 2
    }
}

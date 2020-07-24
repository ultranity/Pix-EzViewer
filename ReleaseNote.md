# TODO
- [ ] 批量下载
- [ ] feature: 搜索时尝试自动识别pid
- [ ] 批量识别pid重命名本地图片
- [ ] 用户页FAB位置移动至顶栏
- [ ] 用户页顶栏图片不隐藏/详情页显示
- [ ] 用户页顶栏图片下载
- [ ] 详情页面控件着色及主题
- [ ] 详情页面背景高斯模糊（可选）
- [ ] 新的布局极简布局，save、like 按钮合并为点击+长按
- [ ] pixiv画廊（登陆页面背景）
- [ ] 卡片单击、双击、三击（or长按？）完成下载图片，下载+收藏，下载+收藏+关注 
- [ ] 顶栏等尺寸细节调整
- [-] 瀑布流间距调整
- [ ] 瀑布流性能优化
- [ ] 更多图标（可开关）
- [ ] 画师TAB栏改为顶部展示卡片+详情
- [!] 允许用户使用pixivc等中转站api
- [ ] tag单独文件夹
- [ ] 下载管理修复：restart后UI更新只移除任务，需要重新进入
- [ ] 搜索记录tag长按删除卡顿优化-后台线程
- [ ] 去除部分平台兼容代码和实际无用的GMS，安装包体积可以降到10MB
- [ ] F-Droid 分发
- [] feature&update: 更多命名自定义字符串（tags,tag#,tag5,tagst)
# 更新日志：
## 1.6.0R:
- [x] feature: tag自定义连接符号 #11
- [x] feature: 查看收藏某图片的用户列表 #13
- [x] feature: 加入下载队列 Toast 开关 #14
- [x] update：搜索历史按时间降序 #18
- [x] update：网络请求&加载速度优化
- [x] update：捐赠记录
- [x] update：动态页面顶栏开关位置滑动
- [x] fix：tab每次刷新修复
- [x] feature:下载管理菜单增加：全部暂停
- [x] feature:下载管理菜单增加：重启
- [x] update&fix：其他调整

## 1.5.8W&1.5.8B:
- [x] fix: 部分设置下下载闪退
- [x] update: 更新样式
- [x] update: 自定义文件名中的模板微调
- [x] update&fix：自定义文件名中的模板示例选择后可修改
- [x] merge&update: 英文翻译补充及清理修复，感谢@namazso

## 1.5.8R:
- [x] feature: 动态页面加入屏蔽已收藏图片功能(开关切换)
- [x] feature: R18自动私密收藏
- [x] feature: 预加载增强
- [x] feature: 折叠布局中长按头像收藏
- [x] feature: 图片卡片长按显示更多信息
- [x] feature&update: 保存规则及策略优化，画师名称过长时删除无关后缀，tags去重，连接符改为#，尽可能储存更多信息
- [x] update: 收藏分类切换优化
- [x] update: 背景颜色优化（夜间模式）
- [x] update: 收藏分类切换优化
- [x] update&fix: stack DataHolder notifyDataSetChanged 修复网络较差时飞速滑动列表导致异步加载不一致error（很难达成的复现条件，嗯）
- [x] fix: 下载列表重复刷新闪退
- [x] update: 其他优化调整
## 1.5.7R:
- [x] feature: tab切换时可选择保留内容不刷新（设置内切换）
- [x] feature: R18独立保存文件夹（在保存路径下创建，可设置名称）
- [x] feature&update: 更多命名自定义字符串（tag，R18，account)
- [x] feature&update: fade in/out过渡动画
- [x] feature&update: 恢复未完成任务时自动刷新
- [x] update: 评论样式调整
- [x] update: crash Handler 代码优化
- [x] update: bugly 设置优化
- [x] update: 说明及 readme 明确只支持android5.0+
- [x] update: 进一步使用webp优化apk体积
- [x] update: 代码优化
- [x] update&fix: stack DataHolder
- [x] fix: lateinit property pixiVisionAdapter has not been initialized
- [x] fix: onTaskFail null 安全
- [x] fix: onTaskComplete file not found 检查

## 1.5.6B:
- [x] fix: 图片分享
- [x] fix: 自定义文件格式保存
- [x] fix: nullable layoutMananger 
## 1.5.6W:
- [x] fix: 自定义图片保存格式模板界面夜间模式背景异常
- [x] fix: constraintLayout_fold正确显示
- [x] fix: banner 加载更多
- [x] fix？: 尝试解决PagerAdapter出现IllegalStateException
- [x] update: 安装包体积优化（12MB,去除部分平台兼容代码和实际无用的GMS可以降到10MB？）
- [x] update: 强制关闭Bugly热更新避免部分机型bug
- [x] update&fix: 下载按钮夜间模式颜色可见度提升
## 1.5.6R:
- [x] update: 新的反馈邮箱：Pix-Ez@outlook.com
- [x] fix&update: 启动加载优化，解决部分环境下首页持续刷新中问题，首页加载速度X2+
- [x] fix&update: 图片详情横栏在部分复杂情况下的正确隐藏
- [x] feature: 自定义图片保存格式模板（覆盖旧设置，有改动的需要重设）
- [x] feature: 保存路径自动设置初始路径
- [x] feature: 新顶栏banner点击后变色
- [x] feature: 画师推荐展示的的预览图片可点击进入
- [x] update: 安装包体积优化
- [x] update: 阴影调整
- [x] update: 画师详情页面相关过渡动画优化
- [x] update: 用户头像缓存策略优化
- [x] fix: 参数、可读性、性能及安全优化
- [x] update: 使用DataHolder代替intent在activity传递图片数据，尽量减少查看时重新请求数据
## 1.5.5W:
- [x] update #1, 返回时直接退出搜索
- [x] fix&update: 暗黑模式优化：图标颜色修正#2, 占位图颜色修正，layout颜色修正#4
- [x] fix: 图片详情左右划动正确处理fragment状态避免加载失败
- [x] feature: 新的滑动式 pixivision 顶栏banner，减少交互层级（可设置切换），动画调整中，欢迎交流反馈or贡献代码
- [x] feature: 下载列表支持下拉刷新
- [x] feature: 新的中等尺寸图像展示卡片布局：仅保留作者小头像+简化标题，推荐用于首页、用户收藏、搜索结果
- [x] feature: 新布局长按用户头像关注
- [x] feature: gif允许下载首图
- [x] update: 图片加载性能优化
- [x] update: 查看图片过程过渡动画调整，更加流畅
- [x] fix: 正确处理gif加载过渡动画
- [x] update: 阴影效果调整
- [x] update: 使用对象传递避免查看时重新请求数据，减少卡顿(类似@4ed6740d8532f013ab40ab046cf47b530478cf4e,@132f882b9fc4bf6fbfa0fb9ecc9e48a862d64017)
- [x] fix: 参数及空指针安全优化
## 1.5.5R:
- [x] feature&fix: #1 由tag进入搜索界面后可点击顶部文字跳转至修改框
- [x] feature: 首页、用户收藏、搜索结果使用中等尺寸图像展示卡片布局：作者头像+作者昵称+标题（可在设置页面关闭）
- [x] feature: 图像展示卡片、图片下载按钮 长按显示更多详细信息
- [x] feature&fix: 在自己的用户页，不隐藏已收藏图片
- [x] update: 删除已完成任务卡顿优化-后台线程
- [x] update: 多任务下载卡顿优化-后台线程
- [x] feature: 下载管理增加：线程设置
- [x] update: 图片详情页面下载图标尺寸增大方便点击
- [x] feature: 新增命名格式改为：id(user)_title,（与MIUI相册兼容）
- [x] feature&fix: 标题中的"\t" 字符视为illegal
- [x] update: 高亮黄色调整：稍降亮度提高饱和度，不再刺眼
- [x] update: 图片详情未展开底栏同样使用黄色圆环标记已关注
- [x] update&fix: （部分）类重命名及代码可读性优化
- [x] update&fix: AS4.0 DataBinding 设置更新
## 1.5.4Y:
- [x] feature: 排行页未展开底栏同样使用黄色圆环标记已关注 ~~（首页推荐要不要加呢）~~
- [x] feature: 图片详情页面下载图标长按显示更多详情
- [x] feature: 命名格式新增： ~~id@user_title~~ （1.5.5R修改）, 与 id_title 格式下载文件已存在判断兼容
- [x] feature&fix: 下载页面resume尝试恢复异常型fail任务
- update&fix: 细节调整及其他优化

##1.5.4B
- [x] feature: 启动 app 时恢复先前未完成任务（可在设置页关闭）
- [x] update: 更新 aria2 框架
- [x] fix: 解决部分场景下网络异常切换时恢复任务失败而无限 wait 的问题
- update&fix: 其他优化
##1.5.4W
- [x] feature: 避免使用 windows 下禁用的 ascii 特殊字符（如 *，?）命名文件，使用 Unicode 类似字符替代
- [x] feature: 分别保存搜索页面与用户页面下隐藏已收藏图片功能的选择
- [x] feature: 下载管理器菜单 增加 移除已完成任务
- [x] feature: 启动 app 时仅移除已完成任务
- [x] update:更换 Bugly ID
# 架构

kmp compose, 支持 android、ios、desktop

## 目录

1. app 是启动入口
2. core 放与业务无关的公共代码以及UI控件
3. feature 放业务代码，每个业务一个模块，相互独立

## 技术选型

1. 分层架构，采用 MVVM 模式，数据流 Flow
2. 依赖注入：koin，网络：Ktor，数据库：Room，日志：Kermit，KV：Multiplatform Settings，Json：Kotlinx Serialization
3. 路由：Decompose，图片：Coil，组件：Material 3，图表：Koala Plot
4. 图片/文件选择器：FileKit，音视频：ComposeMediaPlayer(kdroidFilter 维护)，录音：kmp-audio-recorder-player，Picker：ImagePickerKMP
5. 文件读写：Okio，webview： compose-webview-multiplatform(KevinnZou 维护)
6. Crash和埋点：Firebase，自定义 Expect/Actual，仅需支持 android 和 ios 端，desktop 预留空实现
7. 后端：supabase-kt(io.github.jan-tennert.supabase), 配合 Room，离线优先 + 异步同步
8. 权限管理：MOKO Permissions

## 配色

Linsia: 取山海灵川潮汐之意，寓意学识如潮水源源不断，积微成博。

活力多巴胺（暖色调、娱乐、年轻化）

# Change Log

> 版本号定义遵循：[语义化版本 2.0.0](https://semver.org/lang/zh-CN/)

## 2.0.11 (2021-10-09)

- 修复：多线程构建错误问题（最常见就是产生异常`java.util.zip.ZipException: zip file is empty`）

    > 相关issue：[324](https://github.com/HujiangTechnology/gradle_plugin_android_aspectjx/issues/324)、[327](https://github.com/HujiangTechnology/gradle_plugin_android_aspectjx/issues/327)

- 修复：多变种构建时间随着变种数量增多暴涨问题

    > 相关issue：[305](https://github.com/HujiangTechnology/gradle_plugin_android_aspectjx/issues/305)

- 修复：aspectj织入发生错误时未终止构建

## 更多历史版本

[原作者版本信息](CHANGELOG-old.md)


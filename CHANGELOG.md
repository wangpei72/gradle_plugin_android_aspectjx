# Change Log

版本日志格式基于 [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)，版本号遵循 [语义化版本 2.0.0](https://semver.org/spec/v2.0.0.html)。

## [2.0.16] - 2022-09-06
### Fixed
- 修复：增量构建路径下构建失败的问题（[#28](https://github.com/wurensen/gradle_plugin_android_aspectjx/issues/28)）

## [2.0.15] - 2022-08-30
### Added

- 修改plugin引入相关逻辑，支持通过plugins方式引入（未发布到gradle插件仓库，仍然需要通过指定classpath的方式获取插件）

- 支持所有Android Plugin，包括：

  ```groovy
  'com.android.application',
  'com.android.feature',
  'com.android.dynamic-feature',
  'com.android.library',
  'android',
  'android-library'
  ```
### Changed

- AGP版本变更为4.1.3，gradle版本变更为6.5

### Fixed

- 修复：Kotlin项目禁用插件或不需要进行任何织入时，会发送类丢失的问题（[#26](https://github.com/wurensen/gradle_plugin_android_aspectjx/issues/26)）

## 2.0.14 (2021-12-07)

- 修复：Dump类不支持多线程可能导致的多线程错误问题

## 2.0.13 (2021-11-18)

- 修复：在增量构建场景下，打开被删除的文件导致构建异常的问题
- 优化：构建结束后关闭所有已打开的jar文件
- 优化：线程池使用结束后关闭

## 2.0.12 (2021-11-09)

- 修复：在增量构建场景下，打开被删除的文件导致构建异常的问题

## 2.0.11 (2021-10-12)

- 修复：多线程构建错误问题（最常见就是产生异常`java.util.zip.ZipException: zip file is empty`）

    > 相关issue：[#324](https://github.com/HujiangTechnology/gradle_plugin_android_aspectjx/issues/324)、[#327](https://github.com/HujiangTechnology/gradle_plugin_android_aspectjx/issues/327)

- 修复：多变种构建时间随着变种数量增多暴涨问题

    > 相关issue：[#305](https://github.com/HujiangTechnology/gradle_plugin_android_aspectjx/issues/305)

- 修复：aspectj织入发生错误时未终止构建

- 拷贝TABLESWITCH.java，修复循环i++后超过int最大值导致索引为负数的错误问题

## 更多历史版本

[原作者版本信息](CHANGELOG-old.md)


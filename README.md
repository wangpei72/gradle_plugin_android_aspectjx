AspectJX
===========

由于原作者不再维护，所以fork过来自己维护。

> [原作者说明文档](./README-old.md) 

## 最新版本（3.3.1）

接入或者升级前，请先[查看完整版本日志](CHANGELOG.md)

> 开发中的测试版本访问：[Sonatype's snapshot repository](https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/wurensen/gradle-android-plugin-aspectjx/)

## 如何使用

### 插件引用

**方式一：`apply`方式**

在项目根目录的`build.gradle`里依赖**AspectJX**

```groovy
// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        // 已发布到mavenCentral仓库
        mavenCentral()
    }

    dependencies {
        // aspectj插件
        classpath 'io.github.wurensen:gradle-android-plugin-aspectjx:<version>'
    }
}
```

在`app`项目的`build.gradle`里应用插件：

```groovy
// 3.0.0开始，id已变更：
apply plugin: 'io.github.wurensen.android-aspectjx'

// 3.0.0以下：
apply plugin: 'android-aspectjx'
//或者这样也可以
apply plugin: 'com.hujiang.android-aspectjx'
```

> 注意：为了保持plugins方式引入和apply plugin方式引入id一致，所以插件id已变更。

**方式二：`plugins`方式**

```groovy
plugins {
  // 3.0.0版本开始支持直接从gradlePluginPortal仓库拉取
  id "io.github.wurensen.android-aspectjx" version "version"
}
```

> 对于`3.0.0`以下版本想采用plugins方式，请用旧版本id，并且自定义拉取策略：[配置方式](https://github.com/wurensen/gradle_plugin_android_aspectjx/issues/27)

### AspectJX配置

所有配置项（该类成员对应`gradle`文件中的`aspectjx`规则）：

```kotlin
/**
 * 配置文件
 */
open class AJXExtension {

    /**
     * 包含规则
     */
    val includes = mutableListOf<String>()

    /**
     * 排除规则
     */
    val excludes = mutableListOf<String>()

    /**
     * aspectjtools.jar支持的参数，请误乱使用，否则可能会产生未知问题！！！
     */
    val ajcArgs = mutableListOf<String>()

    /**
     * 是否启用，默认启用
     */
    var enabled = true

    /**
     * 是否开启debug模式，输出调试相关信息，以便排查问题，默认关闭
     * @since 3.3.0
     */
    var debug = false
}
```

**AspectJX**默认会处理所有的二进制代码文件和库，为了提升编译效率及规避部分第三方库出现的编译兼容性问题，**AspectJX**提供`include`、`exclude`命令来过滤需要处理的文件及排除某些文件(包括class文件及jar文件)。

**支持包名匹配**

```groovy
aspectjx {
    // 排除所有package路径中包含`android.support`的class文件及库（jar文件）
    exclude 'android.support'
}
```

> **注意事项1：规则的描述尽量用比较具体的范围，防止exclude的范围超出预期**
>
> **注意事项2：注意apt编译期生成的类，比如项目的某个module模块用到glide注解生成类，生成的类会匹配上com.bumptech.glide导致整个module被过滤**
>
> **注意事项3：织入文件本身是需要当成待处理文件被自己处理的，所以排除规则注意避免把织入文件也排除了，否则可能会导致运行时异常**

**支持`*`和`**`匹配单独使用**

```groovy
aspectjx {
    // 忽略所有的class文件及jar文件，相当于AspectJX不生效
    exclude '*'
}
```

**提供enabled开关**

`enabled`默认为true，即默认**AspectJX**生效

```groovy
aspectjx {
    // 关闭AspectJX功能
    enabled false
}
```

**提供debug开关**
```groovy
aspectjx {
    // 是否debug，开启后会输出织入信息等相关日志（3.3.0版本新增）
    debug = true
}
```


## 适配情况

| 使用依赖 | 适配版本 | 兼容版本 |
| :-- | - | --- |
| Android Gradle Plugin | 7.2.2 | 4.1.3 |
| Gradle | 7.3.3 | 6.5 |
| org.aspectj:aspectjtools | 1.9.7 | 1.9.7 |
| org.aspectj:aspectjrt（引入插件会自动引入该依赖） | 1.9.7 | 1.9.7 |

> 适配AGP最新大版本和兼容上一个大版本

## 问题排查

从`3.2.0`版本开始，插件会把相关信息记录到对应`module`的`build/tmp/transformClassesWithAjxForVariantName`目录下：

| 目录/文件名称         | 说明                                                         |
| :-------------------- | :----------------------------------------------------------- |
| aspectFiles           | 该目录包含收集到的所有织入规则类                             |
| weaveTmp              | 该目录用于织入class类型文件时使用，其中input包含符合规则的class文件，output包含对input织入得到的结果class文件 |
| buildConfigCache.json | 该json文件用于增量构建，并记录AJX插件配置、织入规则文件信息、符合匹配规则的class文件或者jar、织入输出结果（包含costMillis字段，表示织入处理耗时，可以根据该耗时来进行调优，把确认不需要织入处理的class文件或者jar添加到排除规则中） |
| logs                  | 该目录用于存储日志，当织入失败时，日志文件会写入到该目录，可用于排查织入出错的场景 |

> 该目录在非增量构建时会被清除

从`3.3.0`版本开始，新增`debug`配置项，开启后构建过程会输出织入信息等相关日志，可以根据`“[:app:transformClassesWithAjxForDebug]”`作为前缀来查询日志。

## 常见问题

- 问：`library`模块引入插件，无法对第三方依赖库进行织入？

  答：`library`模块引入插件，只能对当前模块的class文件进行处理，无法对依赖库进行处理，只有`app`模块能对依赖库进行处理。

- 问：**AspectJX**是否支持`*.aj`文件的编译?

  答：不支持。目前**AspectJX**仅支持annotation的方式，具体可以参考[支持kotlin代码织入的AspectJ Demo](https://github.com/HujiangTechnology/AspectJ-Demo)

- 问：编译时会出现`can't determine superclass of missing type**`及其他编译错误怎么办？

  答：大部分情况下把出现问题相关的class文件或者依赖库（jar文件）过滤掉就可以搞定了

- 问：项目使用kotlin或kotlin协程，发生织入错误？

  答：请按以下方式exclude掉kotlin库

  ```groovy
  aspectjx {
      // 移除kotlin相关，编译错误和提升速度
      exclude 'kotlin.jvm', 'kotlin.internal'
      exclude 'kotlinx.coroutines.internal', 'kotlinx.coroutines.android'
  }
  ```

> 有任何问题可以提[Issues](https://github.com/wurensen/gradle_plugin_android_aspectjx/issues)或者[discussions](https://github.com/wurensen/gradle_plugin_android_aspectjx/discussions)

## License

```txt
Copyright 2022 LanceWu

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```


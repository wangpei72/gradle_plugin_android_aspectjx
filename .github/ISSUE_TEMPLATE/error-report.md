---
name: Error Report
about: 提供相关信息以便更好排查问题
title: "[Error]"
labels: ''
assignees: ''

---

请提供构建环境相关信息：
- AGP（Android Gradle Plugin）版本：
- Gradle版本：

发送构建错误时，请先确定是构建错误还是aspectj织入错误：
- 如果是aspectj织入发生异常，会在对应的`build/outputs/logs`目录下产生`ajcore`为前缀的日志文件，请提供该日志文件以便查找问题
- 如果是其它错误，请尽量提供完整的堆栈信息

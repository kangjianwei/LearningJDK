# JDK源码阅读笔记

> **Read The Fucking Source Code**　　---- [RTFM](https://en.wikipedia.org/wiki/RTFM)
>     
> **源码面前，了无秘密**　　---- [侯捷](https://zh.wikipedia.org/wiki/%E4%BE%AF%E4%BF%8A%E5%82%91_%28%E4%BD%9C%E5%AE%B6%29)


## 项目介绍

本项目主要整理/记录阅读`JDK`源码时的理解与体会，仅供参考。

项目中包含多个分支，主分支命名为`master`，测试分支命名为`test`，源码/笔记分支以`JDK-X`（**X是JDK版本**）命名。

* `master`分支不定期汇总源码笔记与测试代码的快照。

* `JDK-X`分支存放`JDK`的**源码**与**笔记**。阅读过程中产生的笔记以**注释**的形式直接写在源码文件中。

* `test`分支存放辅助理解的**测试代码**，可直接运行。
  * 注1：建议在`OracleJDK`/`OpenJDK` 11的环境下运行测试文件
  * 注2：不会为所有类/接口都写测试文件，有的是因为太简单，有的是因为已写过大量类似的，还有的是因为理解不到位


## 使用说明

1. 开箱即用。将项目克隆/下载到本地，然后使用`IntelliJ IDEA`打开即可。
    
2. 阅读源码时请切换到`JDK-X`分支，且**不需要**关联`JDK`。
    
   测试源码时请切换到`test`分支，此时需要关联`OracleJDK`/`OpenJDK`。
    
3. 该源码**不支持**直接编译。如想完整编译整个`JDK`项目，请参考官方教程[Building the JDK](https://hg.openjdk.java.net/jdk/jdk11/raw-file/tip/doc/building.html)。
    
4. 如果源码因缺失个别依赖文件而报错，请到谷歌搜索相关的jar包导入即可。或者可在[Github Issues](https://github.com/kangjianwei/LearningJDK/issues)提出反馈。
    
5. **欢迎在[Github Issues](https://github.com/kangjianwei/LearningJDK/issues)交流好的想法、建议、意见。**
    


## Commit图例

| 序号 |       emoji        |                           在本项目中的含义                            |       简写标记        |
| ---- | ------------------ | ------------------------------------------------------------------- | -------------------- |
| (0) | :tada:             | 初始化项目                                                           | `:tada:`             |
| (1) | :memo:             | 更新文档，包括但不限于README                                           | `:memo:`             |
| (2) | :bulb:             | 发布新的阅读笔记 <sub>**(注1)**</sub>                                 | `:bulb:`             |
| (3) | :sparkles:         | 增量更新阅读笔记                                                      | `:sparkles:`         |
| (4) | :recycle:          | 重构，主要指修改已有的阅读笔记，极少情形下会修改源码 <sub>**(注2)**</sub> | `:recycle:`          |
| (5) | :pencil2:          | 校对，主要指更正错别字、调整源码分组、修改源码排版等                      | `:pencil2:`          |
| (6) | :white_check_mark: | 发布测试文件                                                         | `:white_check_mark:` |
    
>     
> 注1：     
>      
> 关于某个源码当前的阅读进度，请参考[已阅代码清单_按功能排序](已阅代码清单_按功能排序.md)。    
>    
> 注2：涉及到修改源码的场景，包括但不限于：   
>      
>> 修改无意义的变量名为更易懂的变量名；        
>> 补全控制语句作用域上的花括号；    
>> 重构控制语句结构(如if语句的拆分，for/while的互换)；    
>> for循环和foreach循环的转换；    
>> 拆分过长且难读的调用链，将中间过程单独摘出来；      
>> 提取频繁出现的某段操作为单个方法；      
>> 将一个文件内的多个顶级类拆分到不同的文件中(内部类不拆分)；       
>> 匿名类与非匿名类的转换；    
>> 匿名类与函数表达式的转换；    
>> 函数式调用与普通调用的转换；            
>       
> 修改的原则是：尽量少地修改，且**不改变**原有的代码逻辑与运行结果（涉及到多线程的代码有些迷）    
> 修改的目的是：增强可读性，以及便于插入笔记    
    


## 相关链接
    
[Oracle JDK](https://www.oracle.com/technetwork/java/javase/archive-139210.html)    
    
[Open JDK](http://jdk.java.net/archive)    
    
    
## 脚注
    
Commit信息中的`emoji`参考来源：
    
* [Full Emoji List](https://unicode.org/emoji/charts/full-emoji-list.html)   
   
* [gitmoji](https://gitmoji.carloscuesta.me/)    

## 附录
   
#### [已阅代码清单_按功能排序](已阅代码清单_按功能排序.md)    
#### [已阅代码清单_按名称排序](已阅代码清单_按名称排序.md)    
#### [测试文件清单](测试文件清单.md)    

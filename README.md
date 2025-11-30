# CS209A 期末项目: StackOverflow数据分析程序

## 项目结构

**数据存储:** 包含数据读取类和数据库关联类
- TreadData 用于从Stack Overflow获取数据, 并封装为tread类
- DatabaseOperation 用于数据库相关操作

**分析方法:** 包含项目要求所需分析的实现

**前后端连接:** 包含前段与后端的交互
- FrontClient 用于向后端发送API请求
- BackServer 用于处理前段请求并将处理结果传递回前段

**资源文件:** 用于项目需要的资源文件
- html 存储网页画面所需的html
- json 存储网页需要呈现的图表

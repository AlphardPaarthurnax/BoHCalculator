# BoHCalculator

《司辰之书》合成路径计算器（开发中）。当前提供 Elements、Recipes、Verbs、Aspects、Cards、Crafts、Workstations、OtherRecipes、OtherVerbs 九个数据浏览页，以及带可视化进度的 Rowenarium SDE 生成器。

## 运行

需要 JDK 21：

```powershell
.\mvnw.cmd javafx:run
```

在“SDE 生成器”页点击“生成完整 SDE”。默认会复用已成功生成的数据，仅抓取新增或缺失页面；勾选“强制重新抓取所有页面”会重新获取全部页面。

生成结果位于 `src/main/resources/assets/sde`。仅 Aspects、Cards、Workstations 查找或下载图片；`generation-report.json` 会列出页面抓取失败、成功下载和仍缺失图片的准确文件名。

详细分类与数据结构见 [SDE 生成说明](docs/sde-generation.md)。

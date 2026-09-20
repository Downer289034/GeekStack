# 头像上传预览证据

- 用户：`avaprev_mu9sui2y`
- 媒体演示路径：`/media/avatar-preview/mu9sui2y.png`

| 步骤 | 截图 | 结果 |
|---|---|---|
| 上传前 | 01-settings-before-upload.png | pass |
| 上传后预览 | 02-settings-after-upload.png | pass |
| 保存后顶栏 | 03-topbar-after-save.png | pass |
| 刷新后顶栏 | 04-topbar-after-reload.png | pass |

```json
[
  {
    "id": "before-upload",
    "status": "pass",
    "detail": "previewImgCount=0（上传前应无图、字母 fallback）"
  },
  {
    "id": "after-upload-preview",
    "status": "pass",
    "detail": "toast=1; img=1; src=/media/avatar-preview/mu9sui2y.png"
  },
  {
    "id": "after-save-topbar",
    "status": "pass",
    "detail": "saveToast=1; navImg=1; navSrc=/media/avatar-preview/mu9sui2y.png; savedAvatar=/media/avatar-preview/mu9sui2y.png"
  },
  {
    "id": "after-reload",
    "status": "pass",
    "detail": "navImg=1"
  }
]
```

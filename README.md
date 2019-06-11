[TOC]

# Brandy AI图像sdk测试demo

本demo用于brandy ai能力图像sdk的效果展示和性能测试。



## 模型版本[sdk version2.0.1]

| Model       | Version | Description                            |
| ----------- | ------- | -------------------------------------- |
| det_face_tf | v9.0.0  | 人脸检测(检测人脸是否出现、人脸数量等) |
| cls_hand_tf | v1.0.7  | 举手检测(检测是否举手)                 |



## 对应Brandy Form的一些指标

- 人脸检测
  - 出/入框：出框或入框持续时间0.6秒后响应并输出出入框状态。
  - 签到：入框持续时间3秒后响应。
  - 长时出框：出框持续时间3.6秒后响应。
  - 人脸数量变化：人脸数量变化，数量保持不变持续时间5秒后，响应并输出人脸数量。
- 举手检测
  - 举手：举手持续时间0.5秒后响应并输出举手状态。


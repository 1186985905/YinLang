# YOLOv12 交通灯检测实现

本项目基于YOLOv3算法改进了一个YOLOv12模型，专门用于交通灯检测。YOLOv12在YOLOv3的基础上添加了注意力机制，提高了模型对小目标的检测能力和整体精度。

## 主要改进

相比YOLOv3，YOLOv12的主要改进包括：

1. **添加注意力机制**: 在特征提取网络中增加了通道注意力模块，使模型能够更好地关注重要特征
2. **增强的特征提取**: 通过特殊的注意力机制强化关键特征的提取能力
3. **更好的小目标检测**: 特别针对交通灯这类小目标物体进行了优化

## 项目文件结构

- `YOLOv12_implementation.py`: YOLOv12模型的完整实现
- `train_YOLOv12.py`: YOLOv12模型的训练脚本
- `predict_YOLOv12.py`: 使用YOLOv12模型进行预测的脚本

## 环境要求

- Python 3.6+
- PaddlePaddle 2.0+
- OpenCV
- NumPy
- Matplotlib
- PIL

## 使用方法

### 1. 训练模型

```bash
python train_YOLOv12.py
```

需要准备好训练数据集，格式应符合BOSCH交通灯数据集的组织形式。训练完成后会生成模型权重文件。

### 2. 模型预测

```bash
python predict_YOLOv12.py
```

默认使用训练完成的最后一个epoch的模型权重进行预测，你可以在脚本中修改`params_file_path`参数指定不同的模型权重文件。

### 3. 自定义数据集

如果要使用自己的数据集，请确保数据组织形式如下：

```
data/
  ├── train/
  │    ├── images/
  │    └── annotations/
  │         └── xmls/
  ├── var/
  │    ├── images/
  │    └── annotations/
  │         └── xmls/
  └── test/
       └── images/
```

XML文件格式应符合VOC数据集格式。

## YOLOv12的理论基础

YOLOv12是基于YOLOv3改进的一个模型，具有以下特点：

1. **骨干网络**: 使用DarkNet53作为特征提取骨干网络
2. **注意力机制**: 添加通道注意力机制，增强模型对关键特征的提取能力
3. **多尺度检测**: 保留了YOLOv3的多尺度检测机制，能够检测不同大小的目标
4. **损失函数**: 使用与YOLOv3相同的损失函数计算方式，包括边界框位置损失、目标概率损失和类别损失

## 交通灯类别

模型支持检测的交通灯类别包括：
- RedLeft (红左转灯)
- Red (红灯)
- RedRight (红右转灯)
- GreenLeft (绿左转灯)
- Green (绿灯)
- GreenRight (绿右转灯)
- Yellow (黄灯)
- off (灭灯)

## 注意事项

- 训练过程中建议使用GPU加速
- 默认训练10个epoch，可以根据需要在训练脚本中修改
- 模型的检测阈值可以在预测脚本中通过`draw_thresh`参数调整

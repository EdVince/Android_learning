项目名称：ncnnnanodetCameraX

功能：使用ncnn在安卓手机上进行目标检测

注：本项目是基于nihui的nanodetncnn项目的二次开发：https://github.com/nihui/ncnn-android-nanodet



特点：

1. 基于nihui的项目，将摄像头部分，从camera2 API和c++处理，改成了用CameraX API，实现上比较简单
2. 使用ncnn进行网络推理、使用opencv-mobile进行图像处理、使用CameraX API进行摄像头交互



基本测试：

测试机1：骁龙845+4G内存：大约11fps

测试机2：麒麟970+6G内存：大约4.5fps
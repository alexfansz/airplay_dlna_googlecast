+++
author = "Alfred"
title = "RK3326 Miracast"
date = "2024-12-09"
slug = "rk3326-Miracast"
description = "解决RK3326 SDK中Miracast 画面延迟很高问题"
tags = [
  "Miracast"
]
series = ["RK3326"]
+++

[RK3326](https://www.rock-chips.com/a/en/products/RK33_Series/2018/0514/900.html)发布的SDK中，支持Miracast，在实际产品测试中发现安卓手机通过Miracast 投屏后画面延迟很高。
<!--more-->

## 跟踪问题并解决
- WifiDisplaySink.cpp
这里处理rtcp 控制数据和rtp ts流，接收到ts流后交给RTPSink
```c
void WifiDisplaySink::onMessageReceived(const sp<AMessage> &msg) 
...
case ANetworkSession::kWhatBinaryData:
                {
                    CHECK(sUseTCPInterleaving);

                    int32_t channel;
                    CHECK(msg->findInt32("channel", &channel));

                    sp<ABuffer> data;
                    CHECK(msg->findBuffer("data", &data));

                    mRTPSink->injectPacket(channel == 0 /* isRTP */, data);
                    break;
                }
```
- RTPSink.cpp
接收rtp ts流,创建TunnelRenderer
````c
void RTPSink::onMessageReceived(const sp<AMessage> &msg)
                case ANetworkSession::kWhatDatagram:
                {
                    int32_t sessionID;
                    CHECK(msg->findInt32("sessionID", &sessionID));

                    sp<ABuffer> data;
                    CHECK(msg->findBuffer("data", &data));

                    status_t err;
                    if (msg->what() == kWhatRTPNotify) {
                        err = parseRTP(data);
                    } else {
                        err = parseRTCP(data);
                    }
                    break;
                }

status_t RTPSink::parseRTP(const sp<ABuffer> &buffer)
sp <AMessage> notifyLost = new AMessage(kWhatPacketLost, this);
                notifyLost->setInt32("ssrc", srcId);

                mRenderer = new TunnelRenderer(notifyLost);
                renderLooper->registerHandler(mRenderer);
````
- TunnelRenderer.cpp
```c
创建播放器,并把rtp ts流包装为播放器DataSource
sp<IServiceManager> sm = defaultServiceManager();
    sp<IBinder> binder = sm->getService(String16("media.player"));
    sp<IMediaPlayerService> service = interface_cast<IMediaPlayerService>(binder);
    CHECK(service.get() != NULL);

    mStreamSource = new StreamSource(this);
    mPlayerClient = new PlayerClient;

    mPlayer = service->create(mPlayerClient, AUDIO_SESSION_ALLOCATE);
    CHECK(mPlayer != NULL);
    CHECK_EQ(mPlayer->setDataSource((sp<IStreamSource>)mStreamSource),(status_t)OK);

	接下去就是播放器开解码并渲染了
	跟踪发现数据到这里并没有产生延迟，说明问题不在手机或网络传输
	既然数据已经及时到了,只是解码和渲染问题，那么理论上问题是一定可以解决的
```
- 问题解决
```
渲染采用gpu加速，发现延迟不在渲染这里

跟踪播放器,RK3326 使用两种播放器，一种是Nuplayer,一种是Ffmpeg
但Ffmpeg 并不支持StreamSource

SDK代码路径:
~/RockChip/3326/rk3326_sdk/frameworks/av/media/libmediaplayerservice/MediaPlayerFactory.cpp
class NuPlayerFactory : public MediaPlayerFactory::IFactory
...
virtual float scoreFactory(const sp<IMediaPlayer>& /*client*/,
                               const sp<DataSource>& /*source*/,
                               float /*curScore*/) {
        // Only NuPlayer supports setting a DataSource source directly.
        return 1.0;
    }

最终发现延迟发生在Nuplayer 的MPEG2TSExtractor

好在miracast 的ts 到pat pmt 都非常简单，就包了2个stream,很容易自己重新写歌简单的tsExtractor，最后完美解决 :) 
```
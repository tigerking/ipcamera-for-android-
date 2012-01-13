#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <deque>
#include "talk/base/thread.h"
#include "talk/base/messagequeue.h"
#include "ipcamera.h"
#include "mediabuffer.h"
#include "mediapak.h"


class MediaStreamer : public sigslot::has_slots<>, public talk_base::MessageHandler {  
public:
    MediaStreamer(int ifd, int ofd);
    ~MediaStreamer();
    void Start();
    void Stop();

protected:    
    virtual void OnMessage(talk_base::Message *msg);

    void doCapture();
    int checkSingleSliceNAL(const std::deque<unsigned char> &pattern , int &slice_type, unsigned int &frame_num);
    int fillBuffer(unsigned char *buf, unsigned int len);
    int flushBuffer(unsigned char *buf, unsigned int len);
    
    void doStreaming();

private:
    enum {
        MSG_BEGIN_CAPTURE_TASK,
        MSG_BEGIN_STREAMING_TASK,
    };

    int infd;
    int outfd;    
    int frame_num_length;

    talk_base::Thread *captureThread;
    talk_base::Thread *streamingThread;
public:
    static MediaBuffer *mediaBuffer;
    static MediaStreamer *mediaStreamer;    
};

MediaBuffer* MediaStreamer::mediaBuffer = NULL;
MediaStreamer* MediaStreamer::mediaStreamer = NULL;

int StartStreamingMedia(int infd, int outfd) {

    if ( MediaStreamer::mediaBuffer == NULL)
        MediaStreamer::mediaBuffer = new MediaBuffer(32, 120, MAX_VIDEO_PACKAGE, 1024); 
    MediaStreamer::mediaBuffer->Reset(); 

    if ( MediaStreamer::mediaStreamer != NULL) {
        delete MediaStreamer::mediaStreamer;
    }
    MediaStreamer::mediaStreamer = new MediaStreamer(infd, outfd);
    MediaStreamer::mediaStreamer->Start();

    return 1;
}

void StopStreamingMedia() {
    // release object in the begin of netxt request
    if ( MediaStreamer::mediaStreamer != NULL)  
         MediaStreamer::mediaStreamer->Stop();
}

MediaStreamer::MediaStreamer(int ifd, int ofd) {
    frame_num_length = -1;
    infd = ifd;
    outfd = ofd;
    
    captureThread = NULL;
    streamingThread = NULL;    
}

MediaStreamer::~MediaStreamer() {
    if ( streamingThread != NULL) {
        delete streamingThread;
    } 
    if ( captureThread != NULL) {
        delete captureThread;
    }
}

void MediaStreamer::Start() {
    captureThread = new talk_base::Thread();
    captureThread->Start();
    captureThread->Post(this, MSG_BEGIN_CAPTURE_TASK);
    
    streamingThread = new talk_base::Thread();
    streamingThread->Start();
    streamingThread->Post(this, MSG_BEGIN_STREAMING_TASK);
}

void MediaStreamer::Stop() {
    infd = 0;
    outfd = 0;
    
    if ( streamingThread != NULL)
        streamingThread->Quit();

    if ( captureThread != NULL)
        captureThread->Quit();
}

void MediaStreamer::OnMessage(talk_base::Message *msg) {
    switch( msg->message_id) {
        case MSG_BEGIN_CAPTURE_TASK:
            doCapture();        
            break;

        case MSG_BEGIN_STREAMING_TASK:
            doStreaming();
            break;

        default:
            break;
    }
}

void MediaStreamer::doCapture() {
    std::deque<unsigned char> video_check_pattern;
    video_check_pattern.resize(9, 0x00);
    
    unsigned char *buf;
    buf = new unsigned char[1024*512];

    /*
    FILE *fp = fopen ("/sdcard/streaming.flv", "wb");
    flvPackager->setParameter(640, 480, 30);
    flvPackager->addVideoHeader(&mediaInfo.sps_data[0], mediaInfo.sps_data.size(), &mediaInfo.pps_data[0], mediaInfo.pps_data.size());
    fwrite(flvPackager->getBuffer(), flvPackager->bufferLength(), 1, fp);
    flvPackager->resetBuffer();
    */

    LOGD("Native: Begin capture");

    unsigned int last_frame_num = 0;
    int frame_count = 0;
    while(1) {
        if ( infd < 0)
            break;

        // find video slice data from es streaming 
        unsigned char current_byte;
        if ( read(infd, &current_byte, 1) < 0)
            break;
        video_check_pattern.pop_front();
        video_check_pattern.push_back(current_byte);
        
        int slice_type;
        unsigned int frame_num;
        int nal_length = checkSingleSliceNAL( video_check_pattern, slice_type, frame_num );
        if ( nal_length > 0) {
            if ( (frame_num != 0) && (frame_num != (last_frame_num + 1) ) ) {
                LOGD("Error, wrong number, FIXME FIXME");
                {
                    char temp[512];
                    snprintf(temp, 512, "%d,  %d, %d, %d, 0x%02x%02x%02x%02x", slice_type, frame_num, nal_length, frame_num_length, 
                            video_check_pattern[5],video_check_pattern[6],video_check_pattern[7],video_check_pattern[8] );
                    LOGD(temp);
                }

                continue;
            }
            last_frame_num = frame_num;
            
                       
            for(int i = 0; i < (int)video_check_pattern.size(); i++) {
                buf[i] = video_check_pattern[i];
            }
            if ( fillBuffer( &buf[video_check_pattern.size()] , nal_length - (video_check_pattern.size() - 4) ) < 0)
                break;
            /*
            flvPackager->addVideoFrame( buf, nal_length + 4, slice_type, frame_count*30);
            fwrite(flvPackager->getBuffer(), flvPackager->bufferLength(), 1, fp);
            flvPackager->resetBuffer();
            */
            mediaBuffer->PushBuffer( buf, nal_length + 4, frame_count*33, slice_type ? MEDIA_TYPE_VIDEO_KEYFRAME : MEDIA_TYPE_VIDEO);

            frame_count++;
        }
    }
    delete buf;

}

int MediaStreamer::checkSingleSliceNAL(const std::deque<unsigned char> &pattern , int &slice_type, unsigned int &frame_num) {   
    
    // 1. first we check NAL's size, valid size should less than 192K 
    if ( pattern[0] != 0x00)
        return -1;                          
    if ( pattern[1] > 2)   
        return -1;

    // 2. check NAL header including NAL start and type,
    //    only nal_unit_type = 1 and 5 are selected
    //    nal_ref_idc > 0
    if (   (pattern[4] != 0x21)
        && (pattern[4] != 0x25)
        && (pattern[4] != 0x41)
        && (pattern[4] != 0x45)
        && (pattern[4] != 0x61)
        && (pattern[4] != 0x65) ) {
        return -1; 
    }  

    // 3. checking fist_mb (should be 0), slice type should be I or P, 
    //    frame_num should be continued. 
    // Only following pattens are supported. 
    //  
    // I Frame: b 1011 1***   (first_mb = 0, slice_type = 2, pps_id = 0)   
    // I Frame: b 1000 1000 1 (first_mb = 0, slice_type = 7, pps_id = 0)
    // P Frame: b 111* ****   (first_mb = 0, slice_type = 0, pps_id = 0)
    // P Frame: b 1001 101*   (first_mb = 0, slice_type = 5, pps_id = 0)
    // 
    int frame_num_skip = -1;
    if ( (pattern[5] & 0xF8 ) == 0xB8) {
        slice_type = 1;
        frame_num_skip = 5;
    } else if ( ((pattern[5] & 0xFF) == 0x88) 
                && ((pattern[6] & 0x80) == 0x80) ) {
        slice_type = 1;
        frame_num_skip = 9;
    } else if ( (pattern[5] & 0xE0) == 0xE0) {
        slice_type = 0;
        frame_num_skip = 3;
    } else if ( (pattern[5] & 0xFE) == 0x9A) {
        slice_type = 0;
        frame_num_skip = 7;
    }
    if ( slice_type == -1)
        return -1;

    if ( frame_num_length == -1) {
        frame_num_length = 0;
        frame_num = 0;
    } else if ( frame_num_length == 0) {
        unsigned int bits = (pattern[5] << 24) + (pattern[6] << 16) + (pattern[7] << 8) + pattern[8];
        bits = bits << frame_num_skip;
        for(int i = 0; i < (31 - frame_num_skip); i++) {
            if ( bits & 0x80000000 ) {
                frame_num_length = i + 1; 
                break;
            }
            bits = bits << 1;
        }
    }
    
    if ( frame_num_length > 0 ) {
        unsigned int bits = (pattern[5] << 24) + (pattern[6] << 16) + (pattern[7] << 8) + pattern[8];
        bits = bits << frame_num_skip;
        bits = bits >> ( 32 - frame_num_length );
        frame_num =  bits;
    }

    int nal_length = (pattern[1] << 16)  + (pattern[2] << 8) + pattern[3];
    return nal_length;
}

int MediaStreamer::fillBuffer(unsigned char *buf, unsigned int len) {
  while(len > 0) {
    int ret = recv(infd, buf, len, 0);
    
    if ( ret < 0)
        return -1;
    if ( ret == 0)
        continue;
    
    if ( infd < 0)
        return -1;

    len -= ret;
    buf += ret;
  }
  return len;
}

int MediaStreamer::flushBuffer(unsigned char *buf, unsigned int len) {
  while(len > 0) {
    int ret = send(outfd, buf, len, 0);
    
    if ( ret < 0)
        return -1;
    if ( ret == 0)
        continue;

    if ( outfd < 0)
        return -1;

    len -= ret;
    buf += ret;
  }
  return len;
}

void MediaStreamer::doStreaming() {

    LOGD("Native: Begin streaming");

    MediaPackage *media_package;
    FlashVideoPackager *flvPackager = new FlashVideoPackager();
    flvPackager->setParameter(640, 480, 30);
    flvPackager->addVideoHeader(&mediaInfo.sps_data[0], mediaInfo.sps_data.size(), &mediaInfo.pps_data[0], mediaInfo.pps_data.size());
 
    while(1) {
        if ( outfd < 0)
            break;

        media_package = NULL;
        if ( mediaBuffer->PullBuffer(&media_package, MEDIA_TYPE_VIDEO) == false) {
            talk_base::Thread::SleepMs(20);             // wait for 1/20 second
            continue;
        }
        
        flvPackager->addVideoFrame( media_package->data, 
                                    media_package->length,
                                    (media_package->media_type == MEDIA_TYPE_VIDEO_KEYFRAME),
                                    media_package->ts);
        int ret = flushBuffer(flvPackager->getBuffer(), flvPackager->bufferLength());
        if ( ret < 0)
            break;

        flvPackager->resetBuffer();
    }

    delete flvPackager;    
}

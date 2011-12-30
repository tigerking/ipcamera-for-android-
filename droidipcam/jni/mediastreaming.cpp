#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <deque>
#include "talk/base/thread.h"
#include "talk/base/messagequeue.h"
#include "ipcamera.h"
#include "mediabuffer.h"
#include "mediapak.h"

class StreamingTask : public talk_base::MessageHandler {  
public:
    StreamingTask(int ifd, int ofd);

public:    
    enum {
        MSG_BEGIN_TASK,
        MSG_END_TASK,    
    };

protected:    
    virtual void OnMessage(talk_base::Message *msg);
    void beginTask();
    int checkSingleSliceNAL(const std::deque<unsigned char> &pattern , int &slice_type, unsigned int &frame_num);
    int fillBuffer(unsigned char *buf, unsigned int len);

private:
    int infd;
    int outfd;    
    int frame_num_length;

public:
    static MediaBuffer *mediaBuffer;
    static StreamingTask *streamingTask;
    static talk_base::Thread *streamingThread;
};

MediaBuffer *StreamingTask::mediaBuffer;
StreamingTask *StreamingTask::streamingTask;
talk_base::Thread *StreamingTask::streamingThread;

int StartStreamingMedia(int infd, int outfd) {
    if ( StreamingTask::mediaBuffer == NULL)
        StreamingTask::mediaBuffer = new MediaBuffer(32, 120, MAX_VIDEO_PACKAGE, 1024); 

    StreamingTask::streamingThread = new talk_base::Thread();
    StreamingTask::streamingTask = new StreamingTask(infd, outfd);

    StreamingTask::streamingThread->Start();
    StreamingTask::streamingThread->Post(StreamingTask::streamingTask, StreamingTask::MSG_BEGIN_TASK);
    return 1;
}

void StopStreamingMedia() {
    return;
}

StreamingTask::StreamingTask(int ifd, int ofd) {
    frame_num_length = -1;
    infd = ifd;
    outfd = ofd;
    mediaBuffer->Reset(); 
}

void StreamingTask::OnMessage(talk_base::Message *msg) {
    switch( msg->message_id) {
        case MSG_BEGIN_TASK:
            beginTask();        
            break;

        case MSG_END_TASK:
            break;
    }
}

void StreamingTask::beginTask() {
    std::deque<unsigned char> video_check_pattern;
    video_check_pattern.resize(9, 0x00);
    
    unsigned char *buf;
    buf = new unsigned char[1024*512];

    FlashVideoPackager *flvPackager = new FlashVideoPackager();
    FILE *fp = fopen ("/sdcard/streaming.flv", "wb");
    flvPackager->setParameter(640, 480, 30);
    flvPackager->addVideoHeader(&mediaInfo.sps_data[0], mediaInfo.sps_data.size(), &mediaInfo.pps_data[0], mediaInfo.pps_data.size());
    fwrite(flvPackager->getBuffer(), flvPackager->bufferLength(), 1, fp);
    flvPackager->resetBuffer();

    unsigned int last_frame_num = 0;
    int frame_count = 0;
    while(1) {

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
                LOGD("Error, wrong number");
            }
            last_frame_num = frame_num;
            {
                char temp[128];
                snprintf(temp, 128, "%d,  %d, %d", slice_type, frame_num, nal_length);
                LOGD(temp);
            }

            for(int i = 0; i < (int)video_check_pattern.size(); i++) {
                buf[i] = video_check_pattern[i];
            }
            fillBuffer( &buf[video_check_pattern.size()] , nal_length - (video_check_pattern.size() - 4) );
            flvPackager->addVideoFrame( buf, nal_length + 4, slice_type, frame_count*30);
            fwrite(flvPackager->getBuffer(), flvPackager->bufferLength(), 1, fp);
            flvPackager->resetBuffer();
            frame_count++;
        }
    }
    delete buf;

}

int StreamingTask::checkSingleSliceNAL(const std::deque<unsigned char> &pattern , int &slice_type, unsigned int &frame_num) {   
    
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
    // P Frame: b 1001 011*   (first_mb = 0, slice_type = 5, pps_id = 0)
    // 
    int frame_num_skip = -1;
    if ( (pattern[5] & 0xF8 ) == 0xB8) {
        slice_type = 1;
        frame_num_skip = 5;
    } else if ( ((pattern[5] & 0xFF) == 0x80) 
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
    /*  
    {
        char temp[128];
        snprintf(temp, 128, "ftype = %d, frame num length = %d, num = %d", slice_type, frame_num_length, frame_num);
        LOGD(temp);
    }
    */
    return nal_length;
}

int StreamingTask::fillBuffer(unsigned char *buf, unsigned int len) {
  while(len > 0) {
    int ret = read(infd, buf, len);
    
    if ( ret < 0)
        return -1;
    if ( ret == 0)
        continue;

    len -= ret;
    buf += ret;
  }
  return len;
}


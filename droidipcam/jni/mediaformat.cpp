#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <deque>
#include "talk/base/thread.h"
#include "talk/base/messagequeue.h"
#include "ipcamera.h"
#include "mediabuffer.h"

const int MAX_VIDEO_PACKAGE = 384*1024;

class FormatTask : public talk_base::MessageHandler {  
public:
    FormatTask(int ifd, int ofd) {
        frame_num_length = -1;
        infd = ifd;
        outfd = ofd;
        media_buffer = new MediaBuffer(32, 120, MAX_VIDEO_PACKAGE, 1024); 
    }

public:
    enum {
        MSG_BEGIN_TASK,
        MSG_END_TASK,    
    };
    void doFormat();

protected:    
    virtual void OnMessage(talk_base::Message *msg);
    int checkSingleSliceNAL(const std::deque<unsigned char> &pattern );

private:
    int infd;
    int outfd;    

    int frame_num_length;

    MediaBuffer *media_buffer;
};

FormatTask *formatTask;
talk_base::Thread *formatThread;

int StartFormatingMedia(int infd, int outfd) {
    formatThread = new talk_base::Thread();
    formatTask = new FormatTask(infd, outfd);

    formatThread->Start();
    formatThread->Post(formatTask, FormatTask::MSG_BEGIN_TASK);
    return 1;
}

void StopFormatingMedia() {
    formatThread->Stop();
    return;
}

void FormatTask::OnMessage(talk_base::Message *msg) {
    switch( msg->message_id) {
        case MSG_BEGIN_TASK:
            doFormat();        
            break;

        case MSG_END_TASK:
            break;
    }
}

/*
static int fillBuffer(int sck, unsigned char *buf, unsigned int len) {
  while(len > 0) {
    int ret = read(sck, buf, len);
    if ( ret < 0)
        return -1;
    if ( ret == 0)
        continue;
    len -= ret;
    buf += ret;
  }
  return len;
}
*/

int FormatTask::checkSingleSliceNAL(const std::deque<unsigned char> &pattern ) {   
    
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

    // 3. checking slice type, first_mb, and frame_num
    // Only following pattens are supported. 
    //  
    // I Frame: b 1011 1***   (first_mb = 0, slice_type = 2, pps_id = 0)   
    // I Frame: b 1000 1000 1 (first_mb = 0, slice_type = 7, pps_id = 0)
    // P Frame: b 111* ****   (first_mb = 0, slice_type = 0, pps_id = 0)
    // P Frame: b 1001 011*   (first_mb = 0, slice_type = 5, pps_id = 0)
    // 
    int slice_type = -1;    // P = 0, I = 1
    int frame_num_skip = -1;
    int frame_num = -1;
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
        unsigned int bits = (pattern[5] << 24) + (pattern[6] << 16) + (pattern[8] << 8) + pattern[9];
        bits = bits << frame_num_skip;
        for(int i = 0; i < (31 - frame_num_skip); i++) {
            if ( bits & 0x80000000 ) {
                frame_num_length = i + 1;    
            }
            bits = bits << 1;
        }
    }
    
    {
        char temp[128];
        snprintf(temp, 128, "frame num length = %d, num = %d", frame_num_length, frame_num);
        LOGD(temp);
    }

    if ( slice_type == 0) {
        LOGD("Find new P frame....\n");
    } else {
        LOGD("Find new I frame....\n");
    }

    return 0;    
}

void FormatTask::doFormat() {
    std::deque<unsigned char> video_check_pattern;
    video_check_pattern.resize(8, 0x00);
    
    unsigned char *buf;
    buf = new unsigned char[1024*512];

    unsigned char current_byte;
    /*
    unsigned int vpkg_len = 0;
    unsigned int slice_len = 0;
    */

    while(1) {

        // find video slice data from es streaming 
        if ( read(infd, &current_byte, 1) < 0)
            break;
        video_check_pattern.pop_front();
        video_check_pattern.push_back(current_byte);

        checkSingleSliceNAL( video_check_pattern );

    }
    delete buf;

}



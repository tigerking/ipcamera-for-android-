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

private:
    int infd;
    int outfd;    

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

static int checkFirstSliceNAL(const std::deque<unsigned char> &pattern ) {   
    
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
    int frame_num = -1;
    int slice_type = -1;    // P = 0, I = 1
    if ( (pattern[5] & 0xF8 ) == 0xB8) {
        slice_type = 1;
    } else if ( (pattern[5] & 0xFF) == 0x80) {
        slice_type = 1;
    } else if ( (pattern[5] & 0xE0) == 0xE0) {
        slice_type = 0;
    } else if ( (pattern[5] & 0xFE) == 0x9A) {
        slice_type = 0;
    }
    if ( slice_type == -1)
        return -1;

    
}

void FormatTask::doFormat() {
    std::deque<unsigned char> video_check_pattern;
    video_check_pattern.resize(6, 0x00);
    
    unsigned char *buf;
    buf = new unsigned char[1024*512];

    unsigned char current_byte;
    unsigned int vpkg_len = 0;
    unsigned int slice_len = 0;
   
    while(1) {

        // find video slice data from es streaming 
        if ( read(infd, &current_byte, 1) < 0)
            break;
        video_check_pattern.pop_front();
        video_check_pattern.push_back(current_byte);

        vpkg_slice_len =  checkSliceNAL( video_check_pattern );

        if ( vpkg_len == 0)
            continue;
        
        if ( vpkg_len > (unsigned int)MAX_VIDEO_PACKAGE )
              LOGD("ERROR: Big video frame....");
        
        // reading video package.
        for(int i = 0; i < 6; i++) {
            buf[i] = video_check_pattern[i];
            video_check_pattern[i] = 0;
        }
        
        // this is first slice
        if ( fillBuffer(camera_sink_, &buf[6], vpkg_len - 2) < 0)
              break;
        vpkg_len += 4;

        // this is second slice , check it is valid?
        if ( fillBuffer(camera_sink_, &buf[vpkg_len], 6) < 0)
             break;
        second_slice_len = checkSecondSlice( &buf[vpkg_len] );
        if ( second_slice_len == 0)
            continue;
        
        if ( fillBuffer(camera_sink_, &buf[vpkg_len+6], second_slice_len - 2) < 0)
            break;

        vpkg_len += (second_slice_len + 4);
        
        vts++;
        SignalNewPackage(buf, vpkg_len, vts, MEDIA_TYPE_VIDEO);
    }
    delete buf;

}



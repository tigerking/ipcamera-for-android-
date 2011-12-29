#include <unistd.h>
#include <stdio.h>
#include <string.h>

#include "talk/base/thread.h"
#include "talk/base/messagequeue.h"
#include "ipcamera.h"
#include "mediabuffer.h"

class FormatTask : public talk_base::MessageHandler {  
public:
    FormatTask(int ifd, int ofd) {
        infd = ifd;
        outfd = ofd;
        media_buffer = new MediaBuffer(32, 120, 256*1024, 1024); 
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

void FormatTask::doFormat() {
    unsigned char tempbuf[1024*32];

    LOGD("Begin reading video data.\n");
    while(1) {
        int ret = read(infd, tempbuf, 1024);
        if ( ret > 0)
            LOGD("New Vieo Data...............\n");
        else if ( ret < 0)
            break;
    }
    LOGD("Video data is finish...\n");
}



#include <stdio.h>
#include <string.h>

#include "talk/base/thread.h"
#include "talk/base/messagequeue.h"
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
    
}



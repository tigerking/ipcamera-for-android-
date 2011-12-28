#include <stdio.h>
#include <string.h>

#include "talk/base/thread.h"
#include "talk/base/messagequeue.h"

class FormatTask : public talk_base::MessageHandler {  
public:
    FormatTask(int ifd, int ofd) {
        infd = ifd;
        outfd = ofd;
    }
    virtual void OnMessage(talk_base::Message *msg);
private:
    int infd;
    int outfd;    
};

FormatTask *formatTask;
talk_base::Thread *formatThread;

int StartFormatingMedia(int infd, int outfd) {
    formatThread = new talk_base::Thread();
    formatTask = new FormatTask(infd, outfd);

    formatThread->Start();
    formatThread->Post(formatTask, 0);
    return 1;
}

void StopFormatingMedia() {
    formatThread->Stop();
    return;
}

void formatingMedia(int ifd, int ofd) {
         
}

void FormatTask::OnMessage(talk_base::Message *msg) {
    formatingMedia(infd, outfd);        
}



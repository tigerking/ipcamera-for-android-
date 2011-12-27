#include <stdio.h>
#include <string.h>

#include "talk/base/thread.h"
#include "talk/base/messagequeue.h"
class JniTask : public talk_base::MessageHandler {  
    public:
        virtual void OnMessage(talk_base::Message *msg){
            printf("KAKA\n");
        }
};

int startServer() {
    talk_base::Thread *task_thread = new talk_base::Thread();
    JniTask *jni_task = new JniTask();


    task_thread->Start();
    task_thread->Post(jni_task, 0);


    //talk_base::Thread* main_thread = talk_base::Thread::Current(); 
    //main_thread->Run();

    return 0;
}
int StartFormatingMedia(int infd, int outfd) {
    
    startServer();

    return 0;
}

void StopFormatingMedia() {
    
    return;
}




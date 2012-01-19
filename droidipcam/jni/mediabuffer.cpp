#include <string.h>
#include "ipcamera.h"
#include "mediabuffer.h"


MediaBuffer::MediaBuffer(const unsigned int vpkg_number,
                         const unsigned int apkg_number,
	                     const unsigned int vpkg_size, 
                         const unsigned int apkg_size) {
  MediaPackage *pkg;

  vpkg_size_ = vpkg_size;
  apkg_size_ = apkg_size;

  for(unsigned int i = 0; i < vpkg_number; i++) {
    pkg = new MediaPackage(vpkg_size_);
    vpkg_pool_.push_back(pkg);   
  }
  vpkg_released = new MediaPackage(vpkg_size_);

  for(unsigned int i = 0; i < apkg_number; i++) {
    pkg = new MediaPackage(apkg_size_);
    apkg_pool_.push_back(pkg);   
  }

}

MediaBuffer::~MediaBuffer(){
  MediaPackage *pkg;
  for(unsigned int i = 0; i < vpkg_pool_.size(); i++) {
    pkg = vpkg_pool_[i];
    delete pkg;
  }
  vpkg_pool_.clear();

  for(unsigned int i = 0; i < apkg_pool_.size(); i++) {
    pkg = apkg_pool_[i];
    delete pkg;
  }
  apkg_pool_.clear();

}

void MediaBuffer::Reset() {
  MediaPackage *pkg;
  
  while(!vbuffer_.empty()) {
    pkg = vbuffer_.front();
    vbuffer_.pop_front();
    vpkg_pool_.push_back(pkg);
  } 
  vbuffer_.clear();

  while(!abuffer_.empty()) {
    pkg = abuffer_.front();
    abuffer_.pop_front();
    apkg_pool_.push_back(pkg);
  } 
  abuffer_.clear();
}

bool MediaBuffer::pushVideoPackage(const unsigned char *d, const unsigned int len, const unsigned int ts, const unsigned int isIntra) {
  static unsigned int global_seq_count = 0;
  MediaPackage *pkg = NULL;
  bool valid = false;
 
  global_seq_count ++;

  // 1. check memory space.
  if ( len > vpkg_size_ ) {
    return false;
  }

  if ( vpkg_pool_.size() == 0) {
    //LOGD("Media Buffer Overflow!"); 
    return false; 
  }

  // 2. check if it is contined frame or new intra frame
  {
    talk_base::CritScope lock(&mutex_);
    if ( vbuffer_.size() > 0)
      pkg = vbuffer_.back();
  }
  if ( pkg == NULL) {
      valid = true;                                     // first frame
  } else {
    if ( (pkg->seq + 1) == global_seq_count ){
      valid = true;                                     // continued frame
    } else if ( isIntra ){
      valid = true;                                     // new intra frame
    }
  }
  if (valid == false) {
    LOGD("Drop frames....");
    return false;
  }

  // 3. this is valid push to buffer  
  {
    talk_base::CritScope lock(&mutex_);
    pkg = vpkg_pool_.back();
    vpkg_pool_.pop_back();    
  }
  pkg->ts = ts;
  pkg->length = len;
  pkg->seq = global_seq_count;
  if ( isIntra )
    pkg->media_type = MEDIA_TYPE_VIDEO_KEYFRAME;
  else
    pkg->media_type = MEDIA_TYPE_VIDEO;

  memcpy(pkg->data, d, len);
  {
    talk_base::CritScope lock(&mutex_);
    vbuffer_.push_back(pkg);
  }
  return true;

}

bool MediaBuffer::pushAudioPackage(const unsigned char *d, const unsigned int len, const unsigned int ts) {
  MediaPackage *pkg;

  if ( len > apkg_size_ ) {
    return false;
  }

  if ( apkg_pool_.size() == 0) {
    return false; 
  }

  {
    talk_base::CritScope lock(&mutex_);
    pkg = apkg_pool_.back();
    apkg_pool_.pop_back();    
  }

  pkg->ts = ts;
  pkg->length = len;
  pkg->media_type = MEDIA_TYPE_AUDIO;
  memcpy(pkg->data, d, len);

  {
    talk_base::CritScope lock(&mutex_);
    abuffer_.push_back(pkg);
  }
  return true;

}

bool MediaBuffer::PushBuffer(const unsigned char *d, const unsigned int len, 
        unsigned int ts, const MEDIA_TYPE mt) {

  bool ret;
  if ( mt == MEDIA_TYPE_VIDEO ) {
      ret =  pushVideoPackage(d,len,ts,0);
  } else if ( mt == MEDIA_TYPE_VIDEO_KEYFRAME) {
      ret = pushVideoPackage(d,len,ts,1);
  } else {
      ret = pushAudioPackage(d,len,ts);
  }

  return ret;
}

bool MediaBuffer::PullBuffer(MediaPackage **ppkg,const MEDIA_TYPE mt) {
  std::list<MediaPackage *> *pBuffer;

  if ( mt == MEDIA_TYPE_AUDIO )
    pBuffer = &abuffer_;
  else
    pBuffer = &vbuffer_;

  if (pBuffer->size() == 0)
      return false;
  
  MediaPackage *pkg = pBuffer->front();
  
  vpkg_released->ts = pkg->ts;
  vpkg_released->length = pkg->length;
  vpkg_released->media_type = pkg->media_type;
  vpkg_released->seq = pkg->seq;
  memcpy(vpkg_released->data, pkg->data, vpkg_released->length);
  *ppkg = vpkg_released;

  releaseBuffer(pkg);

  return true;
}

void MediaBuffer::releaseBuffer(MediaPackage *pkg) {
  std::list<MediaPackage *> *pBuffer;
  std::vector<MediaPackage *> *pPool;

  if ( pkg->media_type == MEDIA_TYPE_AUDIO ) {
    pBuffer = &abuffer_;
    pPool = &apkg_pool_;
  } else {
    pBuffer = &vbuffer_;
    pPool = &vpkg_pool_;
  }

  { 
    talk_base::CritScope lock(&mutex_);
    pBuffer->pop_front();
    pPool->push_back(pkg);
  }
  return;
}


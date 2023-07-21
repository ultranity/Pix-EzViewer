package com.perol.asdpl.pixivez.services;

import com.arialyy.aria.core.scheduler.AptNormalTaskListener;
import com.arialyy.aria.core.task.DownloadTask;

/**
 * 该文件为Aria自动生成的代理文件，请不要修改该文件的任何代码！
 */
public final class PxEZApp$$DownloadListenerProxy extends AptNormalTaskListener<DownloadTask> {
  private PxEZApp obj;

  public PxEZApp$$DownloadListenerProxy() {
  }

  @Override
  public void onTaskComplete(final DownloadTask task) {
    obj.taskComplete((DownloadTask)task);
  }

  @Override
  public void setListener(final Object obj) {
    this.obj = (PxEZApp)obj;
  }
}

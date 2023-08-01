package com.perol.asdpl.pixivez.manager;

import com.arialyy.aria.core.scheduler.AptNormalTaskListener;
import com.arialyy.aria.core.task.DownloadTask;

/**
 * 该文件为Aria自动生成的代理文件，请不要修改该文件的任何代码！
 */
public final class DownloadManagerActivity$$DownloadListenerProxy extends AptNormalTaskListener<DownloadTask> {
  private DownloadManagerActivity obj;

  public DownloadManagerActivity$$DownloadListenerProxy() {
  }

  @Override
  public void onTaskPre(final DownloadTask task) {
    obj.onTaskPre(task);
  }

  @Override
  public void onTaskFail(final DownloadTask task, Exception e) {
    obj.onTaskFail(task);
  }

  @Override
  public void onTaskStop(final DownloadTask task) {
    obj.onTaskStop(task);
  }

  @Override
  public void onTaskComplete(final DownloadTask task) {
    obj.onTaskComplete(task);
  }

  @Override
  public void onTaskStart(final DownloadTask task) {
    obj.onTaskStart(task);
  }

  @Override
  public void onWait(final DownloadTask task) {
    obj.onWait(task);
  }

  @Override
  public void onPre(final DownloadTask task) {
    obj.onPre(task);
  }

  @Override
  public void onTaskCancel(final DownloadTask task) {
    obj.onTaskCancel(task);
  }

  @Override
  public void onTaskResume(final DownloadTask task) {
    obj.onTaskResume(task);
  }

  @Override
  public void onTaskRunning(final DownloadTask task) {
    obj.onTaskRunning(task);
  }

  @Override
  public void setListener(final Object obj) {
    this.obj = (DownloadManagerActivity)obj;
  }
}

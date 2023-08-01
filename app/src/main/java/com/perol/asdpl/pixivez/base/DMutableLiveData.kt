package com.perol.asdpl.pixivez.base

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

/** fix: MutableLiveData default value will be observed
 * Creates a MutableLiveData initialized with the given `default value`.
 * but skip first observe/expose currentVersion
 * @param defaultValue initial value
 */
class DMutableLiveData<T>(val defaultValue: T) : MutableLiveData<T>() {
    var currentVersion = 0
        private set

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner, observer)
        super.setValue(defaultValue)
    }

    fun observeAfterSet(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner, observer)
    }

    override fun getValue(): T? {
        if (isInitialized)
            return super.getValue()
        return defaultValue
    }

    override fun setValue(value: T?) {
        super.setValue(value)
        currentVersion++
    }
}

/* Java Impl
public final class DDMutableLiveData<T>  extends MutableLiveData<T>  {

   public DDMutableLiveData(T defaultValue) {
      this.defaultValue = defaultValue;
   }
   private int currentVersion;
   private final T defaultValue;

   public final T getDefaultValue() {
      return this.defaultValue;
   }
   public final int getCurrentVersion() {
      return this.currentVersion;
   }

   public void observe(@NotNull LifecycleOwner owner, @NotNull Observer<? super T> observer) {
      observer.onChanged(this.defaultValue);
      super.observe(owner, observer);
   }

   public final void observeAfterSet(@NotNull LifecycleOwner owner, @NotNull Observer<? super T> observer) {
      super.observe(owner, observer);
   }

   @Override
   @Nullable
   public final T getValue() {
      return this.isInitialized() ? super.getValue() : this.defaultValue;
   }

   @Override
   public final void setValue(@Nullable T value) {
      super.setValue(value);
      this.currentVersion++;
   }
}
*/
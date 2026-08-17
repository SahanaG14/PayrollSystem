import java.util.concurrent.*;

/** Debounced, daemon-backed persistence coordinator used by editable payroll controls. */
public final class AutoSaveService {
 private static final ScheduledExecutorService TIMER=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"Payroll-AutoSave");t.setDaemon(true);return t;});
 private static final ConcurrentHashMap<String,ScheduledFuture<?>> PENDING=new ConcurrentHashMap<>();
 private static volatile boolean dirty;
 public static void start(){TIMER.scheduleAtFixedRate(AutoSaveService::flushNow,30,30,TimeUnit.SECONDS);}
 public static void markDirty(){dirty=true;}
 public static void debounce(String key,Runnable work){markDirty();ScheduledFuture<?> old=PENDING.remove(key);if(old!=null)old.cancel(false);PENDING.put(key,TIMER.schedule(()->{try{work.run();}finally{PENDING.remove(key);}},1,TimeUnit.SECONDS));}
 public static synchronized void flushNow(){if(!dirty)return;CTCStore.flush();DeductionStore.flush();try{java.util.prefs.Preferences.userRoot().flush();}catch(Exception ignored){}dirty=false;}
 public static void shutdown(){flushNow();TIMER.shutdownNow();}
 private AutoSaveService(){}
}

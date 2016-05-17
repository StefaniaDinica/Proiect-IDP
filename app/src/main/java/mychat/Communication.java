package mychat;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.MessageContext;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusSignalHandler;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class Communication extends Service implements Observer {
    private static final String TAG = "chat.AllJoynService";
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()");
        return null;
    }

    public void onCreate() {
        Log.i(TAG, "onCreate()");
        startBusThread();
        mChatApplication = (Main)getApplication();
        mChatApplication.addObserver(this);

        CharSequence title = "AllJoyn";
        CharSequence message = "Chat Channel Hosting Service.";
        Intent intent = new Intent(this, Welcome.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Notification notification = builder.setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.kitty).setTicker(message).setWhen(System.currentTimeMillis())
                    .setAutoCancel(true).setContentTitle(title).setContentText(message).build();

        notification.flags |= Notification.DEFAULT_SOUND | Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

        Log.i(TAG, "onCreate(): startForeground()");
        startForeground(NOTIFICATION_ID, notification);

        mBackgroundHandler.connect();
        mBackgroundHandler.startDiscovery();
    }

    private static final int NOTIFICATION_ID = 0xdefaced;

    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        mBackgroundHandler.cancelDiscovery();
        mBackgroundHandler.disconnect();
        stopBusThread();
        mChatApplication.deleteObserver(this);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");
        return START_STICKY;
    }

    private Main mChatApplication = null;


    public synchronized void update(Observable o, Object arg) {
        Log.i(TAG, "update(" + arg + ")");
        String qualifier = (String)arg;

        if (qualifier.equals(Main.APPLICATION_QUIT_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_APPLICATION_QUIT_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(Main.USE_JOIN_CHANNEL_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_USE_JOIN_CHANNEL_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(Main.USE_LEAVE_CHANNEL_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_USE_LEAVE_CHANNEL_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(Main.HOST_INIT_CHANNEL_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_HOST_INIT_CHANNEL_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(Main.HOST_START_CHANNEL_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_HOST_START_CHANNEL_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(Main.HOST_STOP_CHANNEL_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_HOST_STOP_CHANNEL_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(Main.OUTBOUND_CHANGED_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_OUTBOUND_CHANGED_EVENT);
            mHandler.sendMessage(message);
        }
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_APPLICATION_QUIT_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): APPLICATION_QUIT_EVENT");
                    mBackgroundHandler.leaveSession();
                    mBackgroundHandler.cancelAdvertise();
                    mBackgroundHandler.unbindSession();
                    mBackgroundHandler.releaseName();
                    mBackgroundHandler.exit();
                    stopSelf();
                }
                break;
            case HANDLE_USE_JOIN_CHANNEL_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): USE_JOIN_CHANNEL_EVENT");
                    mBackgroundHandler.joinSession();
                }
                break;
            case HANDLE_USE_LEAVE_CHANNEL_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): USE_LEAVE_CHANNEL_EVENT");
                    mBackgroundHandler.leaveSession();
                }
                break;
            case HANDLE_HOST_INIT_CHANNEL_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): HOST_INIT_CHANNEL_EVENT");
                }
                break;
            case HANDLE_HOST_START_CHANNEL_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): HOST_START_CHANNEL_EVENT");
                    mBackgroundHandler.requestName();
                    mBackgroundHandler.bindSession();
                    mBackgroundHandler.advertise();
                }
                break;
            case HANDLE_HOST_STOP_CHANNEL_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): HOST_STOP_CHANNEL_EVENT");
                    mBackgroundHandler.cancelAdvertise();
                    mBackgroundHandler.unbindSession();
                    mBackgroundHandler.releaseName();
                }
                break;
            case HANDLE_OUTBOUND_CHANGED_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): OUTBOUND_CHANGED_EVENT");
                    mBackgroundHandler.sendMessages();
                }
                break;
            default:
                break;
            }
        }
    };

    private static final int HANDLE_APPLICATION_QUIT_EVENT = 0;
    private static final int HANDLE_USE_JOIN_CHANNEL_EVENT = 1;
    private static final int HANDLE_USE_LEAVE_CHANNEL_EVENT = 2;
    private static final int HANDLE_HOST_INIT_CHANNEL_EVENT = 3;
    private static final int HANDLE_HOST_START_CHANNEL_EVENT = 4;
    private static final int HANDLE_HOST_STOP_CHANNEL_EVENT = 5;
    private static final int HANDLE_OUTBOUND_CHANGED_EVENT = 6;

    public static enum BusAttachmentState {
        DISCONNECTED,
        CONNECTED,
        DISCOVERING
    }

    private BusAttachmentState mBusAttachmentState = BusAttachmentState.DISCONNECTED;

    public static enum HostChannelState {
        IDLE,
        NAMED,
        BOUND,
        ADVERTISED,
        CONNECTED
    }

    private HostChannelState mHostChannelState = HostChannelState.IDLE;

    public static enum UseChannelState {
        IDLE,
        JOINED,
    }

    private UseChannelState mUseChannelState = UseChannelState.IDLE;

    private final class BackgroundHandler extends Handler {
        public BackgroundHandler(Looper looper) {
            super(looper);
        }

        public void exit() {
            Log.i(TAG, "mBackgroundHandler.exit()");
            Message msg = mBackgroundHandler.obtainMessage(EXIT);
            mBackgroundHandler.sendMessage(msg);
        }

        public void connect() {
            Log.i(TAG, "mBackgroundHandler.connect()");
            Message msg = mBackgroundHandler.obtainMessage(CONNECT);
            mBackgroundHandler.sendMessage(msg);
        }

        public void disconnect() {
            Log.i(TAG, "mBackgroundHandler.disconnect()");
            Message msg = mBackgroundHandler.obtainMessage(DISCONNECT);
            mBackgroundHandler.sendMessage(msg);
        }

        public void startDiscovery() {
            Log.i(TAG, "mBackgroundHandler.startDiscovery()");
            Message msg = mBackgroundHandler.obtainMessage(START_DISCOVERY);
            mBackgroundHandler.sendMessage(msg);
        }

        public void cancelDiscovery() {
            Log.i(TAG, "mBackgroundHandler.stopDiscovery()");
            Message msg = mBackgroundHandler.obtainMessage(CANCEL_DISCOVERY);
            mBackgroundHandler.sendMessage(msg);
        }

        public void requestName() {
            Log.i(TAG, "mBackgroundHandler.requestName()");
            Message msg = mBackgroundHandler.obtainMessage(REQUEST_NAME);
            mBackgroundHandler.sendMessage(msg);
        }

        public void releaseName() {
            Log.i(TAG, "mBackgroundHandler.releaseName()");
            Message msg = mBackgroundHandler.obtainMessage(RELEASE_NAME);
            mBackgroundHandler.sendMessage(msg);
        }

        public void bindSession() {
            Log.i(TAG, "mBackgroundHandler.bindSession()");
            Message msg = mBackgroundHandler.obtainMessage(BIND_SESSION);
            mBackgroundHandler.sendMessage(msg);
        }

        public void unbindSession() {
            Log.i(TAG, "mBackgroundHandler.unbindSession()");
            Message msg = mBackgroundHandler.obtainMessage(UNBIND_SESSION);
            mBackgroundHandler.sendMessage(msg);
        }

        public void advertise() {
            Log.i(TAG, "mBackgroundHandler.advertise()");
            Message msg = mBackgroundHandler.obtainMessage(ADVERTISE);
            mBackgroundHandler.sendMessage(msg);
        }

        public void cancelAdvertise() {
            Log.i(TAG, "mBackgroundHandler.cancelAdvertise()");
            Message msg = mBackgroundHandler.obtainMessage(CANCEL_ADVERTISE);
            mBackgroundHandler.sendMessage(msg);
        }

        public void joinSession() {
            Log.i(TAG, "mBackgroundHandler.joinSession()");
            Message msg = mBackgroundHandler.obtainMessage(JOIN_SESSION);
            mBackgroundHandler.sendMessage(msg);
        }

        public void leaveSession() {
            Log.i(TAG, "mBackgroundHandler.leaveSession()");
            Message msg = mBackgroundHandler.obtainMessage(LEAVE_SESSION);
            mBackgroundHandler.sendMessage(msg);
        }

        public void sendMessages() {
            Log.i(TAG, "mBackgroundHandler.sendMessages()");
            Message msg = mBackgroundHandler.obtainMessage(SEND_MESSAGES);
            mBackgroundHandler.sendMessage(msg);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
            case CONNECT:
                doConnect();
                break;
            case DISCONNECT:
                doDisconnect();
                break;
            case START_DISCOVERY:
                doStartDiscovery();
                break;
            case CANCEL_DISCOVERY:
                doStopDiscovery();
                break;
            case REQUEST_NAME:
                doRequestName();
                break;
            case RELEASE_NAME:
                doReleaseName();
                break;
            case BIND_SESSION:
                doBindSession();
                break;
            case UNBIND_SESSION:
                doUnbindSession();
                break;
            case ADVERTISE:
                doAdvertise();
                break;
            case CANCEL_ADVERTISE:
                doCancelAdvertise();
                break;
            case JOIN_SESSION:
                doJoinSession();
                break;
            case LEAVE_SESSION:
                doLeaveSession();
                break;
            case SEND_MESSAGES:
                doSendMessages();
                break;
            case EXIT:
                getLooper().quit();
                break;
            default:
                break;
            }
        }
    }

    private static final int EXIT = 1;
    private static final int CONNECT = 2;
    private static final int DISCONNECT = 3;
    private static final int START_DISCOVERY = 4;
    private static final int CANCEL_DISCOVERY = 5;
    private static final int REQUEST_NAME = 6;
    private static final int RELEASE_NAME = 7;
    private static final int BIND_SESSION = 8;
    private static final int UNBIND_SESSION = 9;
    private static final int ADVERTISE = 10;
    private static final int CANCEL_ADVERTISE = 11;
    private static final int JOIN_SESSION = 12;
    private static final int LEAVE_SESSION = 13;
    private static final int SEND_MESSAGES = 14;


    private BackgroundHandler mBackgroundHandler = null;

    private void startBusThread() {
        HandlerThread busThread = new HandlerThread("BackgroundHandler");
        busThread.start();
        mBackgroundHandler = new BackgroundHandler(busThread.getLooper());
    }

    private void stopBusThread() {
        mBackgroundHandler.exit();
    }

    private BusAttachment mBus  = new BusAttachment(Main.PACKAGE_NAME, BusAttachment.RemoteMessage.Receive);

    private static final String NAME_PREFIX = "org.alljoyn.bus.samples.chat";

    private static final short CONTACT_PORT = 27;

    private static final String OBJECT_PATH = "/chatService";

    private class ChatBusListener extends BusListener {

        public void foundAdvertisedName(String name, short transport, String namePrefix) {
            Log.i(TAG, "mBusListener.foundAdvertisedName(" + name + ")");
            Main application = (Main)getApplication();
            application.addFoundChannel(name);
        }

        public void lostAdvertisedName(String name, short transport, String namePrefix) {
            Log.i(TAG, "mBusListener.lostAdvertisedName(" + name + ")");
            Main application = (Main)getApplication();
            application.removeFoundChannel(name);
        }
    }

    private ChatBusListener mBusListener = new ChatBusListener();

    private void doConnect() {
        Log.i(TAG, "doConnect()");
        org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(getApplicationContext());
        assert(mBusAttachmentState == BusAttachmentState.DISCONNECTED);
        mBus.useOSLogging(true);
        mBus.setDebugLevel("ALLJOYN_JAVA", 7);
        mBus.registerBusListener(mBusListener);

        Status status = mBus.registerBusObject(mChatService, OBJECT_PATH);
        if (Status.OK != status) {
            mChatApplication.alljoynError(Main.Module.HOST, "Unable to register the chat bus object: (" + status + ")");
            return;
        }

        status = mBus.connect();
        if (status != Status.OK) {
            mChatApplication.alljoynError(Main.Module.GENERAL, "Unable to connect to the bus: (" + status + ")");
            return;
        }

        status = mBus.registerSignalHandlers(this);
        if (status != Status.OK) {
            mChatApplication.alljoynError(Main.Module.GENERAL, "Unable to register signal handlers: (" + status + ")");
            return;
        }

        mBusAttachmentState = BusAttachmentState.CONNECTED;
    }

    private boolean doDisconnect() {
        Log.i(TAG, "doDisonnect()");
        assert(mBusAttachmentState == BusAttachmentState.CONNECTED);
        mBus.unregisterBusListener(mBusListener);
        mBus.disconnect();
        mBusAttachmentState = BusAttachmentState.DISCONNECTED;
        return true;
    }

    private void doStartDiscovery() {
        Log.i(TAG, "doStartDiscovery()");
        assert(mBusAttachmentState == BusAttachmentState.CONNECTED);
        Status status = mBus.findAdvertisedName(NAME_PREFIX);
        if (status == Status.OK) {
            mBusAttachmentState = BusAttachmentState.DISCOVERING;
            return;
        } else {
            mChatApplication.alljoynError(Main.Module.USE, "Unable to start finding advertised names: (" + status + ")");
            return;
        }
    }

    private void doStopDiscovery() {
        Log.i(TAG, "doStopDiscovery()");
        assert(mBusAttachmentState == BusAttachmentState.CONNECTED);
        mBus.cancelFindAdvertisedName(NAME_PREFIX);
        mBusAttachmentState = BusAttachmentState.CONNECTED;
    }

    private void doRequestName() {
        Log.i(TAG, "doRequestName()");
        int stateRelation = mBusAttachmentState.compareTo(BusAttachmentState.DISCONNECTED);
        assert (stateRelation >= 0);

        String wellKnownName = NAME_PREFIX + "." + mChatApplication.hostGetChannelName();
        Status status = mBus.requestName(wellKnownName, BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE);
        if (status == Status.OK) {
            mHostChannelState = HostChannelState.NAMED;
            mChatApplication.hostSetChannelState(mHostChannelState);
        } else {
            mChatApplication.alljoynError(Main.Module.USE, "Unable to acquire well-known name: (" + status + ")");
        }
    }

    private void doReleaseName() {
        Log.i(TAG, "doReleaseName()");
        int stateRelation = mBusAttachmentState.compareTo(BusAttachmentState.DISCONNECTED);
        assert (stateRelation >= 0);
        assert(mBusAttachmentState == BusAttachmentState.CONNECTED || mBusAttachmentState == BusAttachmentState.DISCOVERING);
        assert(mHostChannelState == HostChannelState.NAMED);

        String wellKnownName = NAME_PREFIX + "." + mChatApplication.hostGetChannelName();
        mBus.releaseName(wellKnownName);
        mHostChannelState = HostChannelState.IDLE;
        mChatApplication.hostSetChannelState(mHostChannelState);
    }

    private void doBindSession() {
        Log.i(TAG, "doBindSession()");

        Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);
        SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);

        Status status = mBus.bindSessionPort(contactPort, sessionOpts, new SessionPortListener() {
            public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
                Log.i(TAG, "SessionPortListener.acceptSessionJoiner(" + sessionPort + ", " + joiner + ", " + sessionOpts.toString() + ")");

                if (sessionPort == CONTACT_PORT) {
                    return true;
                }
                return false;
            }

            public void sessionJoined(short sessionPort, int id, String joiner) {
                Log.i(TAG, "SessionPortListener.sessionJoined(" + sessionPort + ", " + id + ", " + joiner + ")");
                mHostSessionId = id;
                SignalEmitter emitter = new SignalEmitter(mChatService, id, SignalEmitter.GlobalBroadcast.Off);
                mHostChatInterface = emitter.getInterface(MainInterface.class);
            }
        });

        if (status == Status.OK) {
            mHostChannelState = HostChannelState.BOUND;
            mChatApplication.hostSetChannelState(mHostChannelState);
        } else {
            mChatApplication.alljoynError(Main.Module.HOST, "Unable to bind session contact port: (" + status + ")");
            return;
        }
    }

    private void doUnbindSession() {
        Log.i(TAG, "doUnbindSession()");

        mBus.unbindSessionPort(CONTACT_PORT);
        mHostChatInterface = null;
        mHostChannelState = HostChannelState.NAMED;
        mChatApplication.hostSetChannelState(mHostChannelState);
    }

    int mHostSessionId = -1;
    boolean mJoinedToSelf = false;
    MainInterface mHostChatInterface = null;

    private void doAdvertise() {
        Log.i(TAG, "doAdvertise()");
        String wellKnownName = NAME_PREFIX + "." + mChatApplication.hostGetChannelName();
        Status status = mBus.advertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);

        if (status == Status.OK) {
            mHostChannelState = HostChannelState.ADVERTISED;
            mChatApplication.hostSetChannelState(mHostChannelState);
        } else {
            mChatApplication.alljoynError(Main.Module.HOST, "Unable to advertise well-known name: (" + status + ")");
            return;
        }
    }

    private void doCancelAdvertise() {
        Log.i(TAG, "doCancelAdvertise()");
        String wellKnownName = NAME_PREFIX + "." + mChatApplication.hostGetChannelName();
        Status status = mBus.cancelAdvertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);

        if (status != Status.OK) {
            mChatApplication.alljoynError(Main.Module.HOST, "Unable to cancel advertisement of well-known name: (" + status + ")");
            return;
        }

        mHostChannelState = HostChannelState.BOUND;
        mChatApplication.hostSetChannelState(mHostChannelState);
    }

    private void doJoinSession() {
        Log.i(TAG, "doJoinSession()");
        if (mHostChannelState != HostChannelState.IDLE) {
            if (mChatApplication.useGetChannelName().equals(mChatApplication.hostGetChannelName())) {
                mUseChannelState = UseChannelState.JOINED;
                mChatApplication.useSetChannelState(mUseChannelState);
                mJoinedToSelf = true;
                return;
            }
        }
        String wellKnownName = NAME_PREFIX + "." + mChatApplication.useGetChannelName();
        short contactPort = CONTACT_PORT;
        SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
        Mutable.IntegerValue sessionId = new Mutable.IntegerValue();

        Status status = mBus.joinSession(wellKnownName, contactPort, sessionId, sessionOpts, new SessionListener() {
            /**
             * This method is called when the last remote participant in the
             * chat session leaves for some reason and we no longer have anyone
             * to chat with.
             *
             * In the class documentation for the BusListener note that it is a
             * requirement for this method to be multithread safe.  This is
             * accomplished by the use of a monitor on the ChatApplication as
             * exemplified by the synchronized attribute of the removeFoundChannel
             * method there.
             */
            public void sessionLost(int sessionId, int reason) {
                Log.i(TAG, "BusListener.sessionLost(sessionId=" + sessionId + ",reason=" + reason + ")");
                mChatApplication.alljoynError(Main.Module.USE, "The chat session has been lost");
                mUseChannelState = UseChannelState.IDLE;
                mChatApplication.useSetChannelState(mUseChannelState);
            }
        });

        if (status == Status.OK) {
            Log.i(TAG, "doJoinSession(): use sessionId is " + mUseSessionId);
            mUseSessionId = sessionId.value;
        } else {
            mChatApplication.alljoynError(Main.Module.USE, "Unable to join chat session: (" + status + ")");
            return;
        }

        SignalEmitter emitter = new SignalEmitter(mChatService, mUseSessionId, SignalEmitter.GlobalBroadcast.Off);
        mChatInterface = emitter.getInterface(MainInterface.class);

        mUseChannelState = UseChannelState.JOINED;
        mChatApplication.useSetChannelState(mUseChannelState);
    }

    MainInterface mChatInterface = null;

    private void doLeaveSession() {
        Log.i(TAG, "doLeaveSession()");
        if (mJoinedToSelf == false) {
            mBus.leaveSession(mUseSessionId);
        }
        mUseSessionId = -1;
        mJoinedToSelf = false;
        mUseChannelState = UseChannelState.IDLE;
        mChatApplication.useSetChannelState(mUseChannelState);
    }

    int mUseSessionId = -1;

    private void doSendMessages() {
        Log.i(TAG, "doSendMessages()");

        String message;
        while ((message = mChatApplication.getOutboundItem()) != null) {
            Log.i(TAG, "doSendMessages(): sending message \"" + message + "\"");

            try {
                if (mJoinedToSelf) {
                    if (mHostChatInterface != null) {
                        mHostChatInterface.Chat(message);
                    }
                } else {
                    mChatInterface.Chat(message);
                }
            } catch (BusException ex) {
                mChatApplication.alljoynError(Main.Module.USE, "Bus exception while sending message: (" + ex + ")");
            }
        }
    }

    class ChatService implements MainInterface, BusObject {
        public void Chat(String str) throws BusException {
        }
    }

    private ChatService mChatService = new ChatService();

    @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "Chat")
    public void Chat(String string) {

        String uniqueName = mBus.getUniqueName();
        MessageContext ctx = mBus.getMessageContext();
        Log.i(TAG, "Chat(): use sessionId is " + mUseSessionId);
        Log.i(TAG, "Chat(): message sessionId is " + ctx.sessionId);

        if (ctx.sender.equals(uniqueName)) {
            Log.i(TAG, "Chat(): dropped our own signal received on session " + ctx.sessionId);
            return;
        }

        if (mJoinedToSelf == false && ctx.sessionId == mHostSessionId) {
            Log.i(TAG, "Chat(): dropped signal received on hosted session " + ctx.sessionId + " when not joined-to-self");
            return;
        }

        String nickname = "Peer"; //ctx.sender;
        //nickname = nickname.substring(nickname.length()-10, nickname.length());

        Log.i(TAG, "Chat(): signal " + string + " received from nickname " + nickname);
        mChatApplication.newRemoteUserMessage(nickname, string);
    }



    private String nickname;
    static {
        Log.i(TAG, "System.loadLibrary(\"alljoyn_java\")");
        System.loadLibrary("alljoyn_java");
    }
}

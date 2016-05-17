package mychat;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusSignal;

@BusInterface (name = "org.alljoyn.bus.samples.chat")
public interface MainInterface {
    @BusSignal
    public void Chat(String str) throws BusException;
}

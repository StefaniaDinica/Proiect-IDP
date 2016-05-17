package mychat;

import mychat.Observable;

public interface Observer {
    public void update(Observable o, Object arg);
}

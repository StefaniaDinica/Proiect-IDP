package mychat;

import mychat.Observer;

public interface Observable {
    public void addObserver(Observer obs);
    public void deleteObserver(Observer obs);
}

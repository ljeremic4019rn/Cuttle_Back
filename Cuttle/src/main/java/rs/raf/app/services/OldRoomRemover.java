package rs.raf.app.services;
import rs.raf.app.model.Room;
import java.util.Map;

public class OldRoomRemover extends Thread {

    private final Map<String, Room> activeRooms;

    public OldRoomRemover(Map<String, Room> activeRooms) {
        this.activeRooms = activeRooms;
    }

    @Override
    public void run() {
        while (true) {
            for (Map.Entry<String, Room> room : activeRooms.entrySet()) {
                if (room.getValue().getRoomLastUpdated() + 300000 < System.currentTimeMillis()) {
                    activeRooms.remove(room.getKey());
                }
            }
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}

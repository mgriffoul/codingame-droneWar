

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.Math.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {

    public static void main(String args[]) {


        Scanner in = new Scanner(System.in);


        int P = in.nextInt(); // number of players in the game (2 to 4 players)
        int ID = in.nextInt(); // ID of your player (0, 1, 2, or 3)
        int D = in.nextInt(); // number of drones in each team (3 to 11)
        int Z = in.nextInt(); // number of zones on the map (4 to 8)

        int numberOfPlayer = P;
        int numberOfDronePerTeam = D;
        int numberOfZone = Z;

        List<Zone> zonesInGame = new ArrayList<>();
        List<DronePlayer> dronePlayerList;
        DronePlayer me = new DronePlayer();

        for (int i = 0; i < numberOfZone; i++) {
            int X = in.nextInt(); // corresponds to the position of the center of a zone. A zone is a circle with a radius of 100 units.
            int Y = in.nextInt();
            //Récupération des zones de la partie
            Zone zone = new Zone(X, Y);
            zone.setId(i);
            zonesInGame.add(zone);
        }

        // game loop
        while (true) {


            //Réinitialisation des collections pour chaque tours
            dronePlayerList = new ArrayList<>();

            for (int i = 0; i < numberOfZone; i++) {
                int TID = in.nextInt(); // ID of the team controlling the zone (0, 1, 2, or 3) or -1 if it is not controlled. The zones are given in the same order as in the initialization.
                //Recupération du joueur qui controle la zone
                zonesInGame.get(i).setControllingPlayerId(TID);
            }

            for (int i = 0; i < numberOfPlayer; i++) {
                //Récupération des joueurs de la partie
                DronePlayer dronePlayer = new DronePlayer(i);
                dronePlayer.setPlayerId(i);
                dronePlayerList.add(dronePlayer);

                //Traitement des drones
                List<Drone> droneList = new ArrayList<>();
                for (int j = 0; j < numberOfDronePerTeam; j++) {
                    int coordX = in.nextInt(); // The first D lines contain the coordinates of drones of a player with the ID 0, the following D lines those of the drones of player 1, and thus it continues until the last player.
                    int coordY = in.nextInt();

                    //Récupération des drones de la partie et assignation aux joueurs
                    Drone drone = new Drone(coordX, coordY);
                    drone.setId(j);
                    drone.setOwner(dronePlayer);
                    droneList.add(drone);
                    dronePlayer.setDrones(droneList);

                    drone.setZonesByDistance(getTargetZonesSortedByDistance(drone, zonesInGame, ID));

                }

                if (ID == i) {
                    me = dronePlayer;
                }

            }

            analyseDronePositionOverZone(zonesInGame, dronePlayerList);
            affectDefenderFlag(zonesInGame, me.getDrones(), ID);
            affectTarget(zonesInGame, me.getDrones());

            for (Drone d : dronePlayerList.get(me.playerId).getDrones()) {
                System.out.println(d.targetCoord);
            }


        }

    }

    // Set la liste des drone au dessus d'une zone par id de joueur et le nombre de drone total au dessus d'une zone
    static void analyseDronePositionOverZone(List<Zone> zones, List<DronePlayer> dronePlayerList) {


        //System.err.println("analyse Drone position : ");
        for (Zone z : zones) {
            int numberTotalOfDrones = 0;
            // System.err.println("Pour la zone : "+z.getId());
            for (DronePlayer d : dronePlayerList) {
                // System.err.println("Pour le joueur : "+d.getPlayerId());
                int numberOfDrones = 0;
                for (Drone drone : d.getDrones()) {

                    if (calculateDistance(drone, z) <= 100) {
                        numberOfDrones++;
                        drone.setFlyingOverZone(z);
                    }
                }

                z.getPlayerIdNumberOfDroneOver().put(d.getPlayerId(), numberOfDrones);
                System.err.println("a été put pour le joueur " + d.getPlayerId() + " " + numberOfDrones + " drones dans la zone " + z.getId());
                numberTotalOfDrones += numberOfDrones;
            }
            // System.err.println("Nombre total Final de drones : " + numberTotalOfDrones);
            // System.err.println("///////////////////////////////////////////////////");
            z.setNumberOfDroneOver(numberTotalOfDrones);
        }

    }

    static int whosGettingMorePoints(List<Zone> zonesInGame, List<DronePlayer> dronePlayerList) {
        Map<Integer, Integer> playerScorePlayerIdTreeMap = new TreeMap<>();

        dronePlayerList.stream().forEach(
                player -> playerScorePlayerIdTreeMap.put(
                        (int) zonesInGame.stream().filter(zone -> zone.getControllingPlayerId() != player.getPlayerId()).count(),
                        player.getPlayerId()));

        if (playerScorePlayerIdTreeMap.isEmpty()) {
            return -1;
        } else {
            return ((TreeMap<Integer, Integer>) playerScorePlayerIdTreeMap).firstEntry().getValue();
        }
    }

    static void affectTarget(List<Zone> zones, List<Drone> myDrones) {

        myDrones.stream().forEach(drone -> {
                    if (drone.isDefendingZone()) {
                        drone.setTargetCoord(drone.getCoordX() + " " + drone.getCoordY());
                    } else if (!drone.isDefendingZone() && drone.getZonesByDistance() != null && !drone.getZonesByDistance().isEmpty()) {
                        drone.setTargetCoord(zones.get(drone.getZonesByDistance().get(0)).getCoord());
                    } else {
                        drone.setTargetCoord(drone.getCoordX() + " " + drone.getCoordY());
                    }
                }
        );

    }

    static void affectDefenderFlag(List<Zone> zones, List<Drone> myDrones, int myId) {

        myDrones = removeNotInZone(myDrones);
        myDrones = removeDronesWithoutConcurrence(myDrones);

        if (!myDrones.isEmpty()) {
            setDefendFlag(getZonesWhereIHaveSomeDrones(zones, myId), myDrones, myId);
        }

    }

    static List<Drone> removeNotInZone(List<Drone> drones) {

        return drones.stream().filter(drone -> drone.getFlyingOverZone() != null).collect(Collectors.toList());
    }

    static List<Drone> removeDronesWithoutConcurrence(List<Drone> drones) {
        return drones.stream().filter(drone -> drone.getFlyingOverZone().getNumberOfDroneOver() > 1).collect(Collectors.toList());
    }

    static void setDefendFlag(List<Zone> zones, List<Drone> myDrones, int myId) {

        for (Zone z : zones) {
            int numberOfMyDronesInZone = z.getPlayerIdNumberOfDroneOver().get(myId);
            int maxEnnemyDrone = getNumberMaxOfEnnemyDrone(z, myId);

            List<Drone> drones = filterDroneByZone(myDrones, z);

            if (!drones.isEmpty() && doesZoneNeedDefenders(z, numberOfMyDronesInZone, myId, maxEnnemyDrone)) {

                int i = 0;
                do {
                    drones.get(i).setDefendingZone(true);
                    i++;
                }while (i < maxEnnemyDrone -1);

            }
        }

    }

    static List<Drone> filterDroneByZone(List<Drone> drones, Zone z) {
        return drones.stream().filter(drone -> calculateDistance(drone, z)<=100).collect(Collectors.toList());
    }

    //TODO peux mieux faire !
    static int getNumberMaxOfEnnemyDrone(Zone z, int myId) {
        Map<Integer, Integer> mapWithoutMe = z.getPlayerIdNumberOfDroneOver();
        mapWithoutMe.remove(myId);

        return mapWithoutMe
                .values()
                .stream()
                .collect(Collectors.summarizingInt(Integer::intValue))
                .getMax();
    }

    static List<Zone> getZonesWhereIHaveSomeDrones(List<Zone> zones, int myId) {
        return zones.stream()
                .filter(
                        zone -> zone.getPlayerIdNumberOfDroneOver()
                                .keySet()
                                .contains(myId))
                .collect(Collectors.toList()
                );
    }

    static boolean doesZoneNeedDefenders(Zone z, int myDronesInZone, int myId, int maxEnnemyDrone) {

        for (int i : z.getPlayerIdNumberOfDroneOver().values()) {
            if (myDronesInZone > 0 && z.getControllingPlayerId() == myId && maxEnnemyDrone > 0) {
                return true;
            }
        }
        return false;
    }

    //Renvoie, pour chaque drone, la liste des zones par ordre croissant d'éloignement. On retire à la volé les zones déjà controllées.
    static List<Integer> getTargetZonesSortedByDistance(Drone drone, List<Zone> zones, int myId) {
        TreeMap<Integer, Integer> zoneDistanceZoneIndexTreeMap = new TreeMap<>();
        substractSafeZone(zones, myId).stream().forEach(z -> zoneDistanceZoneIndexTreeMap.put(calculateDistance(drone, z), z.getId()));
        return zoneDistanceZoneIndexTreeMap.values().stream().collect(Collectors.toList());
    }


    static List<Zone> substractSafeZone(List<Zone> zonesSortedByDistance, int myId) {
        return zonesSortedByDistance.stream().filter(ZoneInDanger(myId)).collect(Collectors.toList());
    }

    static Predicate<Zone> ZoneInDanger(int myId) {
        return z -> z.getControllingPlayerId() != myId;
    }


    static int calculateDistance(Drone drone, Zone zone) {
        return (int) sqrt(
                addExact(
                        (long) (pow((double) subtractExact(drone.getCoordX(), zone.getCoordX()), 2D)),
                        (long) (pow((double) subtractExact(drone.getCoordY(), zone.getCoordY()), 2D))
                )
        );
    }


    public static class Zone {

        private int coordX;
        private int coordY;
        private int numberOfDroneOver;
        private Map<Integer, Integer> playerIdNumberOfDroneOver;
        private int controllingPlayerId;
        private int id;

        public Zone(int coordX, int coordY) {
            this.coordX = coordX;
            this.coordY = coordY;
            this.playerIdNumberOfDroneOver = new HashMap<>();
        }

        Map<Integer, Integer> getPlayerIdNumberOfDroneOver() {
            return playerIdNumberOfDroneOver;
        }

        public void setPlayerIdNumberOfDroneOver(Map<Integer, Integer> playerIdNumberOfDroneOver) {
            this.playerIdNumberOfDroneOver = playerIdNumberOfDroneOver;
        }

        int getId() {
            return id;
        }

        void setId(int id) {
            this.id = id;
        }

        int getCoordX() {
            return coordX;
        }

        void setCoordX(int coordX) {
            this.coordX = coordX;
        }

        int getCoordY() {
            return coordY;
        }

        void setCoordY(int coordY) {
            this.coordY = coordY;
        }

        int getNumberOfDroneOver() {
            return numberOfDroneOver;
        }

        void setNumberOfDroneOver(int numberOfDroneOver) {
            this.numberOfDroneOver = numberOfDroneOver;
        }

        int getControllingPlayerId() {
            return controllingPlayerId;
        }

        void setControllingPlayerId(int controllingPlayerId) {
            this.controllingPlayerId = controllingPlayerId;
        }

        String getCoord() {
            return coordX + " " + coordY;
        }

        @Override
        public String toString() {
            return "Zone{" +
                    "coordX=" + coordX +
                    ", coordY=" + coordY +
                    ", numberOfDroneOver=" + numberOfDroneOver +
                    ", controllingPlayerId=" + controllingPlayerId +
                    ", id=" + id +
                    ", map size=" + playerIdNumberOfDroneOver.size() +
                    '}';
        }
    }

    static class DronePlayer {

        private int playerId;
        private List<Drone> drones;

        DronePlayer() {
        }

        DronePlayer(int playerId) {
            this.playerId = playerId;
        }

        int getPlayerId() {
            return playerId;
        }

        void setPlayerId(int playerId) {
            this.playerId = playerId;
        }

        List<Drone> getDrones() {
            return drones;
        }

        void setDrones(List<Drone> drones) {
            this.drones = drones;
        }
    }

    static class Drone {

        private int id;
        private int coordX;
        private int coordY;
        private DronePlayer owner;
        //List triée par ordre d'éloignement du drone à l'Id des différentes zones
        private List<Integer> zonesByDistance;
        private String targetCoord;
        private Zone flyingOverZone;
        private boolean defendingZone;

        Drone(int coordX, int coordY) {
            this.coordX = coordX;
            this.coordY = coordY;
        }

        public String getTargetCoord() {
            return targetCoord;
        }

        public void setTargetCoord(String targetCoord) {
            this.targetCoord = targetCoord;
        }

        boolean isDefendingZone() {
            return defendingZone;
        }

        void setDefendingZone(boolean defendingZone) {
            this.defendingZone = defendingZone;
        }

        Zone getFlyingOverZone() {
            return flyingOverZone;
        }

        void setFlyingOverZone(Zone flyingOverZone) {
            this.flyingOverZone = flyingOverZone;
        }

        List<Integer> getZonesByDistance() {
            return zonesByDistance;
        }

        void setZonesByDistance(List<Integer> zonesByDistance) {
            this.zonesByDistance = zonesByDistance;
        }

        int getId() {
            return id;
        }

        void setId(int id) {
            this.id = id;
        }

        int getCoordX() {
            return coordX;
        }

        void setCoordX(int coordX) {
            this.coordX = coordX;
        }

        int getCoordY() {
            return coordY;
        }

        void setCoordY(int coordY) {
            this.coordY = coordY;
        }

        DronePlayer getOwner() {
            return owner;
        }

        void setOwner(DronePlayer owner) {
            this.owner = owner;
        }

        @Override
        public String toString() {

            String zoneByDistance = "";
            if (this.zonesByDistance != null) {
                for (Integer i : this.zonesByDistance) {
                    zoneByDistance += i.toString() + " / ";
                }
            }


            return "id = " + this.id
                    + " coordX = " + this.coordX
                    + "coordY = " + this.coordY
                    + "owner = " + this.owner.getPlayerId()
                    + " ZoneByDistance =  " + zoneByDistance;
        }
    }

}

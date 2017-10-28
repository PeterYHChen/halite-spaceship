import hlt.*;

import java.util.*;

public class MyBot {

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("Tamagocchi");

        final ArrayList<Move> moveList = new ArrayList<>();
        for (;;) {
            moveList.clear();
            gameMap.updateMap(Networking.readLineIntoMetadata());

            Set<Planet> ownedPlanets = new HashSet<>();
            Set<Ship> availableShips = new HashSet<>();
            int ownedPlanetNum = 0;
            int remainEmptyPlanetNum = 0;
            int totalPlanetNum = gameMap.getAllPlanets().size();
            for (int id : gameMap.getAllPlanets().keySet()) {
                Planet planet = gameMap.getAllPlanets().get(id);
                if (planet.isOwned() && planet.getOwner() == gameMap.getMyPlayerId()) {
                    ownedPlanetNum++;
                    ownedPlanets.add(planet);
                }
                if (!planet.isOwned()) {
                    remainEmptyPlanetNum++;
                }
            }

            for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
                if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                    continue;
                }

                // docking process
                Map<Double, Entity> nearbyEntitiesByDistance = gameMap.nearbyEntitiesByDistance(ship);
                for (Double distance : nearbyEntitiesByDistance.keySet()) {
                    Entity entity = nearbyEntitiesByDistance.get(distance);
                    if (entity instanceof Planet) {
                        Planet planet = (Planet) entity;

                        // Check planet's owner
                        if (planet.isOwned()) {
                            if (remainEmptyPlanetNum > 2) {
                                continue;
                            } else {
                                // Not much remaining empty planets, start adding solders
                                // If other's planet, skip
                                if (planet.getOwner() != gameMap.getMyPlayerId()) {
                                    continue;
                                } else if (planet.getDockedShips().size() > planet.getDockingSpots() / 2) {
                                    // My planet, only skip if docked ships is enough
                                    continue;
                                }
                            }
                        }

                        if (ship.canDock(planet)) {
                            moveList.add(new DockMove(ship, planet));
                            break;
                        }

                        final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, planet, Constants.MAX_SPEED);
                        if (newThrustMove != null) {
                            moveList.add(newThrustMove);
                            availableShips.add(ship);
                            break;
                        }
                    }
                }
            }

            // No more planets, start attacking
            if (remainEmptyPlanetNum > 0) {
                Planet nearestPlanet = null;
                for (final Ship ship : availableShips) {
                    if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                        continue;
                    }

                    // For now, get the first nearest planet
                    if (nearestPlanet == null) {
                        Map<Double, Entity> nearbyEntitiesByDistance = gameMap.nearbyEntitiesByDistance(ship);
                        for (Double distance : nearbyEntitiesByDistance.keySet()) {
                            Entity entity = nearbyEntitiesByDistance.get(distance);
                            if (entity instanceof Planet) {
                                if (ownedPlanets.contains(entity)) {
                                    continue;
                                }
                                nearestPlanet = (Planet) entity;
                            }
                        }
                    }
                    
                    // Send out all available ships
                    if (ship.canDock(nearestPlanet)) {
                        moveList.add(new DockMove(ship, nearestPlanet));
                        break;
                    }

                    final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, nearestPlanet, Constants.MAX_SPEED);
                    if (newThrustMove != null) {
                        moveList.add(newThrustMove);
                        break;
                    }
                }
                Networking.sendMoves(moveList);
            }
        }
    }
}
